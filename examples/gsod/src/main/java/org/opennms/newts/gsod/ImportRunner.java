/*
 * Copyright 2014, The OpenNMS Group
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.opennms.newts.gsod;


import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.opennms.newts.gsod.FileObservable.fileTreeWalker;
import static org.opennms.newts.gsod.FileObservable.lines;
import static rx.exceptions.Exceptions.propagate;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.http.entity.ContentType;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.nio.client.methods.HttpAsyncMethods;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.opennms.newts.api.MetricType;
import org.opennms.newts.api.Sample;
import org.opennms.newts.api.SampleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Observable;
import rx.Observable.Operator;
import rx.Observer;
import rx.Subscriber;
import rx.Subscription;
import rx.apache.http.ObservableHttp;
import rx.apache.http.ObservableHttpResponse;
import rx.exceptions.Exceptions;
import rx.functions.Action0;
import rx.functions.Func1;
import rx.functions.Functions;
import rx.schedulers.Schedulers;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.Guice;
import com.google.inject.Injector;


public class ImportRunner {
    
    private int m_samplesPerBatch = 1000;
    private File m_source;
    private String m_restUrl = null;
    private SampleRepository m_repository;
    private int m_threadCount = 1;
    private int m_maxThreadQueueSize = 0;
    
    private void checkArgument(boolean check, String failureMessage) {
        if (!check) throw new IllegalArgumentException(failureMessage);
    }


    @Option(name="-n", aliases="--samples-per-batch", metaVar="sample-count", usage="the maxinum number of samples to include in each post to the repository (default: 1000)")
    public void setSamplesPerBatch(int samplesPerBatch) {
        checkArgument(samplesPerBatch > 0, "samples per batch must be greater than zero!");
        m_samplesPerBatch = samplesPerBatch;
    }
    
    @Option(name="-u", aliases="--url", metaVar="url", usage="publish data via a Newts REST server at the given url (default: use direct access via Newts API)")
    public void setURL(String url) {
        checkArgument(url != null && !url.isEmpty(), "the url must not be empty");
        m_restUrl = url;
    }
    
    @Option(name="-p", aliases="--parallelism", metaVar="thread-count", usage="when using direct the size of the thread pool that posts the results.  (defaults to 1 ie no parallelism)")
    public void setParallelism(int threadCount) {
        checkArgument(threadCount > 0, "thread count must be at least 1.");
        m_threadCount = threadCount;
    }
    
    @Option(name="-q", aliases="--max-work-queue-size", metaVar="batch-count", usage="when using direct the max size of the work-queue (defaults to thread-count * 3)")
    public void setMaxThreadQueueSize(int maxThreadQueueSize) {
        checkArgument(maxThreadQueueSize > 0, "max thread queue size must be at least 1.");
        m_maxThreadQueueSize = maxThreadQueueSize;
    }
    
    @Argument(metaVar="sourceDir", required=true, usage="the source directory that contains gsod data to import. These must be gzip'd files")
    public void setSource(File source) {
        checkArgument(source.exists(), "the source directory "+source+" does not exist");
        checkArgument(source.isDirectory(), "the source directory must be a directory");
        m_source = source;
    }

    private static final Logger LOG = LoggerFactory.getLogger(ImportRunner.class);

    public static void main(String... args) throws Exception {
        new ImportRunner().execute(args);

    }
    
    public void execute(String... args) throws Exception {

        CmdLineParser parser = new CmdLineParser(this);
        try {
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            // handling of wrong arguments
            System.err.println(e.getMessage());
            parser.printUsage(System.err);
            return;
        }

        // Setup the slf4j metrics reporter
        MetricRegistry metrics = new MetricRegistry();
        
        final long start = System.currentTimeMillis();
        metrics.register("elapsed-seconds", new Gauge<Double>() {

            @Override
            public Double getValue() {
                return (System.currentTimeMillis() - start)/1000.0;
            }
            
        });
        
        final ConsoleReporter reporter = ConsoleReporter.forRegistry(metrics)
                .outputTo(System.err)
                .convertRatesTo(SECONDS)
                .convertDurationsTo(MILLISECONDS)
                .build();

        reporter.start(10, SECONDS);



        LOG.debug("Scanning {} for GSOD data files...", m_source);
        
        // walk the files in the directory given
        Observable<Sample> samples = fileTreeWalker(m_source.toPath())
             .subscribeOn(Schedulers.io())
                
             // set up a meter for each file processed
            .map(meter(metrics.meter("files"), Path.class))
            
            // report file
            .map(reportFile())

            // read all the files and convert them into lines
            .mergeMap(lines())
            // excluding the header lines
            .filter(exclude("YEARMODA"))
            
            // turn each line into a list of samples
            .mergeMap(samples())
            
            // meter the samples
            .map(meter(metrics.meter("samples"), Sample.class))            
            ;
        
        
        Observable<List<Sample>> batches = samples
            // create batches each second or of size m_samplesPerBatch whichever comes first
            .buffer(m_samplesPerBatch)
            ;
        
        Observable<Boolean> doImport = m_restUrl != null ? restPoster(batches, metrics) : directPoster(batches, metrics);
        
        System.err.println("doImport = " + doImport);

        // GO!!!
        final AtomicReference<Subscription> subscription = new AtomicReference<>();
        final AtomicBoolean failed = new AtomicBoolean(false);
        
        final CountDownLatch latch = new CountDownLatch(1);
        
        Subscription s = doImport.subscribe(new Observer<Boolean>() {

            @Override
            public void onCompleted() {
                System.err.println("Finished Importing Everything!");
                reporter.report();
                latch.countDown();
                System.exit(0);
            }

            @Override
            public void onError(Throwable e) {
                failed.set(true);
                System.err.println("Error importing!");
                e.printStackTrace();
                try {
                    //latch.await();
                    Subscription s = subscription.get();
                    if (s != null) s.unsubscribe();

                } catch (Exception ex) {
                    System.err.println("Failed to close httpClient!");
                    ex.printStackTrace();
                } finally {
                    //dumpThreads();
                }
            }

            @Override
            public void onNext(Boolean t) {
                System.err.println("Received a boolen: " + t);
            }
        });
        
        subscription.set(s);
        if (failed.get()) {
            s.unsubscribe();
        }
        //latch.countDown();
        System.err.println("Return from Subscribe!");
        
        latch.await();
        
        //dumpThreads();
        

    }
    
    private SampleRepository repository() {
        if (m_repository == null) {
            Injector injector = Guice.createInjector(new Config());
            m_repository = injector.getInstance(SampleRepository.class);
        }
        return m_repository;
    }


    private Observable<Boolean> directPoster(Observable<List<Sample>> samples, MetricRegistry metrics) {

        final SampleRepository repository = repository();
        final Timer timer = metrics.timer("writes");
        final Meter completions = metrics.meter("samples-completed");
        

        Func1<List<Sample>, Boolean> insert = new Func1<List<Sample>, Boolean>() {

            @Override
            public Boolean call(List<Sample> s) {
                int sz = s.size();
                try (Context timerCtx = timer.time()) {
                    repository.insert(s);
                    return true;
                } finally {
                    completions.mark(sz);
                }
            }
        };
        
        
        return (m_threadCount == 1 ? samples.map(insert) : parMap(samples, metrics, insert)).all(Functions.<Boolean>identity());
        
        
    }


    private Observable<Boolean> parMap(Observable<List<Sample>> samples, MetricRegistry metrics, Func1<List<Sample>, Boolean> insert) {
        
        final Timer waitTime = metrics.timer("wait-time");
    
        
        @SuppressWarnings("serial")
        final BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<Runnable>(m_maxThreadQueueSize == 0 ? m_threadCount * 3 : m_maxThreadQueueSize) {

            @Override
            public boolean offer(Runnable r) {
                try (Context time = waitTime.time()) {
                    this.put(r);
                    return true;
                } catch (InterruptedException e) {
                    throw Exceptions.propagate(e);
                }
            }

            @Override
            public boolean add(Runnable r) {
                try (Context time = waitTime.time()) {
                    this.put(r);
                    return true;
                } catch (InterruptedException e) {
                    throw Exceptions.propagate(e);
                }
            }

            
        };
        final ThreadPoolExecutor executor = new ThreadPoolExecutor(m_threadCount, m_threadCount,
                                                                   0L, TimeUnit.MILLISECONDS,
                                                                   workQueue);
        
        metrics.register("active-threads", new Gauge<Integer>() {

            @Override
            public Integer getValue() {
                return executor.getActiveCount();
            }
            
        });
        
        metrics.register("pool-size", new Gauge<Integer>() {

            @Override
            public Integer getValue() {
                return executor.getPoolSize();
            }
            
        });
        metrics.register("largest-pool-size", new Gauge<Integer>() {

            @Override
            public Integer getValue() {
                return executor.getLargestPoolSize();
            }
            
        });
        
        metrics.register("work-queue-size", new Gauge<Integer>() {

            @Override
            public Integer getValue() {
                return workQueue.size();
            }
            
        });
        
        
        return parMap(samples, executor, metrics, insert);
    }
    
    private Observable<Boolean> parMap(Observable<List<Sample>> samples, ExecutorService executorSvc, final MetricRegistry metrics, final Func1<List<Sample>, Boolean> insert) {
        final ListeningExecutorService executor = MoreExecutors.listeningDecorator(executorSvc);
        
        Observable<Boolean> o = samples
                .lift(new Operator<ListenableFuture<Boolean>, List<Sample>>() {

            @Override
            public Subscriber<? super List<Sample>> call(final Subscriber<? super ListenableFuture<Boolean>> s) {
                return new Subscriber<List<Sample>>() {

                    @Override
                    public void onCompleted() {
                        if (!s.isUnsubscribed()) {
                            s.onCompleted();
                        }
                        executor.shutdown();
                    }

                    @Override
                    public void onError(Throwable e) {
                        if (!s.isUnsubscribed()) {
                            s.onError(e);
                        }
                    }

                    @Override
                    public void onNext(final List<Sample> t) {
                        if (!s.isUnsubscribed()) {
                            try {
                                ListenableFuture<Boolean> f = executor.submit(new Callable<Boolean>() {

                                    @Override
                                    public Boolean call() throws Exception {
                                        return insert.call(t);
                                    }

                                });
                                s.onNext(f);
                            } catch (Throwable ex) {
                                onError(ex);
                            }

                        
                        }
                    }
                };
            }
                
        })
        .observeOn(Schedulers.io())
        .map(new Func1<ListenableFuture<Boolean>, Boolean>() {

            @Override
            public Boolean call(ListenableFuture<Boolean> f) {
                try {
                    return f.get();
                } catch (Throwable e) {
                    throw Exceptions.propagate(e);
                }
            }
            
        });

        return o;
    }
    

    


    private Observable<Boolean> restPoster(Observable<List<Sample>> samples,  MetricRegistry metrics) {

        final CloseableHttpAsyncClient httpClient = HttpAsyncClients.createDefault();
        httpClient.start();


        return samples

                // turn each batch into json
                .map(toJSON())

                // meter them as the go into the post code
                .map(meter(metrics.meter("posts"), String.class))

                // post the json to the REST server
                .mergeMap(postJSON(m_restUrl, httpClient))

                // meter the responses
                .map(meter(metrics.meter("responses"), ObservableHttpResponse.class))
                
                // count sample completions
                .map(meter(metrics.meter("samples-completed"), m_samplesPerBatch, ObservableHttpResponse.class))

                // make sure every request has a successful return code
                .all(successful())
                
                .doOnCompleted(new Action0() {

                    @Override
                    public void call() {
                        try {
                            httpClient.close();
                        } catch (IOException e) {
                            System.err.println("Failed to close httpClient!");
                            e.printStackTrace();
                        }
                    }
                    
                });
    }

    private static Func1<? super Path, ? extends Path> reportFile() {
        return new Func1<Path, Path>() {

            @Override
            public Path call(Path file) {
                System.err.println("Begin Processing: " + file);
                return file;
            }
            
        };
    }

    public static Func1<String, Observable<Sample>> samples() {
        final LineParser parser = new LineParser();
        return new Func1<String, Observable<Sample>>() {

            @Override
            public Observable<Sample> call(String line) {
                try {
                    return Observable.from(parser.parseLine(line));
                } catch (ParseException e) {
                    throw propagate(e);
                }
            }

        };
    }

    private static boolean isNaN(Sample sample) {
        return (sample.getType() == MetricType.GAUGE) && Double.isNaN(sample.getValue().doubleValue());
    }

    public static Func1<List<Sample>, String> toJSON() {
        return new Func1<List<Sample>, String>() {
            @Override
            public String call(List<Sample> samples) {
                JSONBuilder bldr = new JSONBuilder();

                for(Sample sample : samples) {
                    if (isNaN(sample)) continue;
                    //System.err.println("Importing: " + sample);
                    bldr.newObject();
                    bldr.attr("timestamp", sample.getTimestamp().asMillis());
                    bldr.attr("resource", sample.getResource());
                    bldr.attr("name", sample.getName());
                    bldr.attr("type", sample.getType().name());
                    if (sample.getType() == MetricType.GAUGE) {
                        bldr.attr("value", sample.getValue().doubleValue());
                    } else {
                        bldr.attr("value", sample.getValue().longValue());
                    }
                }

                return bldr.toString();
            }
        };
    }
    
    private static Func1<ObservableHttpResponse, Boolean> successful() {
        return new Func1<ObservableHttpResponse, Boolean>() {

            @Override
            public Boolean call(ObservableHttpResponse response) {
                if (response.getResponse().getStatusLine().getStatusCode() >= 400) {
                    throw new RuntimeException("Failed to post samples: " + response.getResponse().getStatusLine());
                }
                return true;
            }
            
        };
    }

    public static Func1<String, Observable<ObservableHttpResponse>> postJSON(final String baseURL, final CloseableHttpAsyncClient httpClient) {

        final URI baseURI = URI.create(baseURL);

        return new Func1<String, Observable<ObservableHttpResponse>>() {
            @Override
            public Observable<ObservableHttpResponse> call(String json) {
                try {
                    return ObservableHttp.createRequest(HttpAsyncMethods.createPost(baseURI, json, ContentType.APPLICATION_JSON), httpClient).toObservable();
                } catch (UnsupportedEncodingException e) {
                    throw Exceptions.propagate(e);
                }
            }
        };
    }
    
    public static Func1<String, Boolean> exclude(final String pattern) {
        return new Func1<String, Boolean>() {

            @Override
            public Boolean call(String s) {
                return !s.contains(pattern);
            }
            
        };
    }
    
    public static <T> Func1<T, T> meter(final Meter meter, Class<T> clazz) {
        return meter(meter, 1, clazz);
    }
    
    public static <T> Func1<T, T> meter(final Meter meter, final int count, Class<T> clazz) {
        return new Func1<T, T>() {

            @Override
            public T call(T t) {
                meter.mark(count);
                return t;
            }
            
        };
    }

    

}

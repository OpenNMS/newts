package org.opennms.newts.gsod;


import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.ThreadPoolExecutor;

import javax.inject.Inject;

import org.opennms.newts.api.SampleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;


class FileVisitor extends SimpleFileVisitor<Path> {

    private static final Logger LOG = LoggerFactory.getLogger(FileVisitor.class);

    private final SampleRepository m_repository;
    private final ThreadPoolExecutor m_executor;
    private final MetricRegistry m_metrics;
    private final Counter m_numFiles;

    @Inject
    FileVisitor(SampleRepository repository, ThreadPoolExecutor executor, MetricRegistry metrics) {

        m_repository = repository;
        m_executor = executor;
        m_metrics = metrics;
        m_numFiles = m_metrics.counter("num-files");
        
        m_metrics.register("num-threads", new Gauge<Integer>() {

            @Override
            public Integer getValue() {
                return m_executor.getActiveCount();
            }
        });

    }

    void execute(Path path) throws FileNotFoundException, IOException {
        m_executor.execute(new FileImport(m_repository, m_metrics, path));
    }

    @Override
    public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) {

        LOG.debug("Found {}", path);

        if (attrs.isRegularFile()) {

            try {
                m_numFiles.inc();
                execute(path);
            }
            catch (IOException e) {
                e.printStackTrace();
            }

        }

        return FileVisitResult.CONTINUE;
    }

}

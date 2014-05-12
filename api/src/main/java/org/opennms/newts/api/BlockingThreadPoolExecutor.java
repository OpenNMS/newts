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
package org.opennms.newts.api;


import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Throwables;


public class BlockingThreadPoolExecutor extends ThreadPoolExecutor {

    public static class BlockingExecutionHandler implements RejectedExecutionHandler {

        @Override
        public void rejectedExecution(Runnable task, ThreadPoolExecutor executor) {

            BlockingQueue<Runnable> queue = executor.getQueue();

            while (true) {
                if (executor.isShutdown()) {
                    throw new RejectedExecutionException("ThreadPoolExecutor has shut down");
                }

                try {
                    if (queue.offer(task, 300, TimeUnit.MILLISECONDS)) {
                        break;
                    }
                }
                catch (InterruptedException e) {
                    throw Throwables.propagate(e);
                }
            }

        }

    }

    public BlockingThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, new LinkedBlockingQueue<Runnable>((int)(maximumPoolSize*1.5)));
        this.setRejectedExecutionHandler(new BlockingExecutionHandler());
    }

}

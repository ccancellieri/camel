/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.processor;

import java.util.Collection;
import java.util.Iterator;

import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.PollingConsumer;
import org.apache.camel.Processor;
import org.apache.camel.impl.LoggingExceptionHandler;
import org.apache.camel.impl.ServiceSupport;
import org.apache.camel.spi.ExceptionHandler;
import org.apache.camel.util.ServiceHelper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A base class for any kind of {@link Processor} which implements some kind of
 * batch processing.
 * 
 * @version $Revision$
 */
public class BatchProcessor extends ServiceSupport implements Runnable, Processor {
    public static final long DEFAULT_BATCH_TIMEOUT = 1000L;
    public static final int DEFAULT_BATCH_SIZE = 100;

    private static final transient Log LOG = LogFactory.getLog(BatchProcessor.class);
    private Endpoint endpoint;
    private Processor processor;
    private Collection<Exchange> collection;
    private long batchTimeout = DEFAULT_BATCH_TIMEOUT;
    private int batchSize = DEFAULT_BATCH_SIZE;
    private int outBatchSize;
    private PollingConsumer consumer;
    private ExceptionHandler exceptionHandler;

    public BatchProcessor(Endpoint endpoint, Processor processor, Collection<Exchange> collection) {
        this.endpoint = endpoint;
        this.processor = processor;
        this.collection = collection;
    }

    @Override
    public String toString() {
        return "BatchProcessor[to: " + processor + "]";
    }

    public void run() {
        LOG.debug("Starting thread for " + this);
        while (isRunAllowed()) {
            try {
                processBatch();
            } catch (Exception e) {
                getExceptionHandler().handleException(e);
            }
        }
        collection.clear();
    }

    // Properties
    // -------------------------------------------------------------------------
    public ExceptionHandler getExceptionHandler() {
        if (exceptionHandler == null) {
            exceptionHandler = new LoggingExceptionHandler(getClass());
        }
        return exceptionHandler;
    }

    public void setExceptionHandler(ExceptionHandler exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
    }

    public int getBatchSize() {
        return batchSize;
    }

    /**
     * Sets the <b>in</b> batch size. This is the number of incoming exchanges that this batch processor
     * will process before its completed. The default value is {@link #DEFAULT_BATCH_SIZE}.
     *
     * @param batchSize the size
     */
    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getOutBatchSize() {
        return outBatchSize;
    }

    /**
     * Sets the <b>out</b> batch size. If the batch processor holds more exchanges than this out size then
     * the completion is triggered. Can for instance be used to ensure that this batch is completed when
     * a certain number of exchanges has been collected. By default this feature is <b>not</b> enabled.
     *
     * @param outBatchSize the size
     */
    public void setOutBatchSize(int outBatchSize) {
        this.outBatchSize = outBatchSize;
    }

    public long getBatchTimeout() {
        return batchTimeout;
    }

    public void setBatchTimeout(long batchTimeout) {
        this.batchTimeout = batchTimeout;
    }

    public Endpoint getEndpoint() {
        return endpoint;
    }

    public Processor getProcessor() {
        return processor;
    }

    /**
     * A transactional method to process a batch of messages up to a timeout
     * period or number of messages reached.
     */
    protected synchronized void processBatch() throws Exception {
        long start = System.currentTimeMillis();
        long end = start + batchTimeout;
        for (int i = 0; !isBatchCompleted(i); i++) {
            long timeout = end - System.currentTimeMillis();
            if (timeout < 0L) {                
                if (LOG.isTraceEnabled()) {
                    LOG.trace("batch timeout expired at batch index: " + i);
                }
                break;
            }
            Exchange exchange = consumer.receive(timeout);
            if (exchange == null) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("receive with timeout: " + timeout + " expired at batch index: " + i);
                }
                break;
            }
            collection.add(exchange);
        }

        // we should NOT log the collection directly as it will invoke a toString() on collection
        // and it will call collection.iterator() where end-users might do stuff that would break
        // calling the iterator a 2nd time as below

        // lets send the batch
        Iterator<Exchange> iter = collection.iterator();
        while (iter.hasNext()) {
            Exchange exchange = iter.next();
            iter.remove();
            processExchange(exchange);
        }
    }

    /**
     * A strategy method to decide if the batch is completed the resulting exchanges should be sent
     */
    protected boolean isBatchCompleted(int index) {
        // out batch size is optional and we should only check if its enabled (> 0)
        if (outBatchSize > 0 && collection.size() >= outBatchSize) {
            return true;
        }
        // fallback yo regular batch size check
        return index >= batchSize;
    }

    /**
     * Strategy Method to process an exchange in the batch. This method allows
     * derived classes to perform custom processing before or after an
     * individual exchange is processed
     */
    protected void processExchange(Exchange exchange) throws Exception {
        processor.process(exchange);
    }

    protected void doStart() throws Exception {
        consumer = endpoint.createPollingConsumer();

        ServiceHelper.startServices(processor, consumer);

        Thread thread = new Thread(this, this + " Polling Thread");
        thread.start();
    }

    protected void doStop() throws Exception {
        ServiceHelper.stopServices(consumer, processor);
        collection.clear();
    }

    protected Collection<Exchange> getCollection() {
        return collection;
    }

    public void process(Exchange exchange) throws Exception {
        // empty since exchanges come from endpoint's polling consumer
    }
}

/*-
 * #%L
 * Elastic APM Java agent
 * %%
 * Copyright (C) 2018 - 2020 Elastic and contributors
 * %%
 * Licensed to Elasticsearch B.V. under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch B.V. licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * #L%
 */
package co.elastic.apm.plugin.log.collect;

import co.elastic.apm.agent.AbstractInstrumentationTest;
import co.elastic.apm.agent.impl.transaction.Transaction;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class Log4jListenerTest extends AbstractInstrumentationTest {

    private Transaction transaction;
    private static final Logger log = LogManager.getLogger(Log4jListenerTest.class);

    @BeforeEach
    void startTransaction() throws IOException {
        transaction = tracer.startRootTransaction(null);
        transaction.activate();
    }

    @AfterEach
    void endTransaction() {
        transaction.deactivate().end();
    }

    @Test
    public void logCollectTest() {
        log.warn("log4j warn, log collect");
        log.warn("log4j warn, stack", new RuntimeException("log runtime exception"));
    }


}

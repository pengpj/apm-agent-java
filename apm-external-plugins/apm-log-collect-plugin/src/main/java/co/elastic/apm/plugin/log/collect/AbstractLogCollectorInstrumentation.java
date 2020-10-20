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

import co.elastic.apm.plugin.log.collect.kafka.KafkaManager;
import co.elastic.apm.agent.sdk.ElasticApmInstrumentation;
import co.elastic.apm.api.ElasticApm;
import co.elastic.apm.api.Transaction;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.UUID;

import static co.elastic.apm.plugin.log.collect.LogGlobalVariable.LOG_COLLECT_LEVEL;

public abstract class AbstractLogCollectorInstrumentation extends ElasticApmInstrumentation {

    @Override
    public Collection<String> getInstrumentationGroupNames() {
        Collection<String> ret = new ArrayList<>();
        ret.add("log-collect");
        return ret;
    }

    public static boolean toCollect(LogLevelEnum current) {
        if (LOG_COLLECT_LEVEL == LogLevelEnum.OFF) {
            return false;
        }
        if (current.getValue() - LOG_COLLECT_LEVEL.getValue() >= 0) {
            return true;
        }
        return false;
    }

    public static String log(long sysTime,
                             String threadName,
                             String level,
                             String className,
                             String methodName,
                             String lineNumber,
                             String traceId,
                             String message,
                             String stack) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        Date date = new Date(sysTime);
        String format = dateFormat.format(date);

        StringBuilder builder = new StringBuilder();
        builder.append("[").append(format).append("]")
            .append("[").append(threadName).append("]")
            .append("[").append(level).append("]")
            .append("[").append(className).append(".").append(methodName).append(":").append(lineNumber).append("]")
            .append("[").append(traceId).append("]")
            .append(message).append("\n")
            .append(stack);
        String cutout = LogUtils.cutout(builder.toString(), LogGlobalVariable.LOG_MAX_LENGTH);
        KafkaManager.send(cutout);
        return cutout;
    }

    public static String traceId() {
        Transaction transaction = ElasticApm.currentTransaction();
        if (null == transaction) {
            return UUID.randomUUID().toString();
        } else {
            return transaction.getTraceId();
        }
    }


}

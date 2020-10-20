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

import co.elastic.apm.agent.sdk.state.GlobalState;

@GlobalState
public class LogCollectEnvConfig {

    public String getBootstrapServers() {
        return System.getenv("log_collect_bootstrap_servers");
    }

    public String getLogTopic() {
        String topic = System.getenv("log_collect_topic");
        if (NotExistEnv(topic)) {
            return "log-collect";
        }
        return topic;
    }

    public boolean syncSend() {
        String logCollectSyncSend = System.getenv("log_collect_sync_send");
        if (NotExistEnv(logCollectSyncSend)) {
            return false;
        }
        return Boolean.TRUE.equals(Boolean.parseBoolean(logCollectSyncSend));
    }

    public LogLevelEnum getLogLevel() {
        LogLevelEnum logLevelEnum;
        String logLevel = System.getenv("log_collect_level");
        if (NotExistEnv(logLevel)) {
            logLevelEnum = LogLevelEnum.OFF;
        } else {
            try {
                logLevelEnum = LogLevelEnum.valueOf(logLevel);
            } catch (IllegalArgumentException e) {
                logLevelEnum = LogLevelEnum.OFF;
            }
        }
        return logLevelEnum;
    }

    public int logMaxLength() {
        String logMaxLength = System.getenv("log_max_length");
        if (NotExistEnv(logMaxLength)) {
            return 1024 * 2;
        }
        try {
            return Integer.parseInt(logMaxLength);
        } catch (NumberFormatException e) {
            return 1024 * 2;
        }
    }

    private boolean NotExistEnv(String value) {
        return null == value || "".equals(value);
    }

}

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
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */

package co.elastic.apm.plugin.log.collect.kafka;

import co.elastic.apm.plugin.log.collect.LogCollectEnvConfig;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.serialization.StringSerializer;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class KafkaManager {

    public static final String DEFAULT_TIMEOUT_MILLIS = "30000";

    private static String topic;
    private static boolean syncSend;
    private static int timeoutMillis;

    private static final KafkaProducer<String, String> producer = initProducer();
    private static List<Header> headerList;

    static {
        LogCollectEnvConfig configuration = new LogCollectEnvConfig();
        topic = configuration.getLogTopic();
        syncSend = configuration.syncSend();
        timeoutMillis = Integer.parseInt(DEFAULT_TIMEOUT_MILLIS);

        headerList = new ArrayList<>();
        String ccloudID = System.getenv("CCLOUD_ID");
        String localIP = "";
        try {
            localIP = IPHelper.getLocalHostLANAddress().getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        if (ccloudID != null) {
            headerList.add(new RecordHeader("ccloudId", ccloudID.getBytes()));
        }
        headerList.add(new RecordHeader("localIp", localIP.getBytes()));
    }

    private static KafkaProducer<String, String> initProducer() {
        LogCollectEnvConfig configuration = new LogCollectEnvConfig();
        String bootstrapServers = configuration.getBootstrapServers();

        Properties config = new Properties();
        config.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        config.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        config.setProperty("batch.size", "0");
        return new KafkaProducer<>(config);

    }

    public static void send(final String msg) {
        if (producer != null) {
            final ProducerRecord<String, String> newRecord = new ProducerRecord<>(topic,
                null,
                null,
                null,
                msg,
                headerList);
            final Future<RecordMetadata> response = producer.send(newRecord);
            if (syncSend) {
                try {
                    response.get(timeoutMillis, TimeUnit.MILLISECONDS);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                producer.send(newRecord, new Callback() {
                    @Override
                    public void onCompletion(final RecordMetadata metadata, final Exception e) {
                        if (e != null) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        }
    }

}

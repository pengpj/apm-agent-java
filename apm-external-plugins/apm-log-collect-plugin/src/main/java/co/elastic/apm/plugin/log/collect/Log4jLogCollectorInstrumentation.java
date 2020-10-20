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

import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.log4j.Level;
import org.apache.log4j.spi.LocationInfo;
import org.apache.log4j.spi.LoggingEvent;

import java.util.Collection;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.not;

public class Log4jLogCollectorInstrumentation extends AbstractLogCollectorInstrumentation {

    private static final String LOG4J_EVENT = "org!apache!log4j!Category".replace("!", ".");
    private static final String SLF4J_LOGGER = "org!slf4j!Logger".replace('!', '.');

    @Override
    public ElementMatcher<? super TypeDescription> getTypeMatcher() {
        return named(LOG4J_EVENT)
            .and(not(named(SLF4J_LOGGER)))
            ;
    }

    @Override
    public ElementMatcher<? super MethodDescription> getMethodMatcher() {
        return named("callAppenders");
    }

    @Override
    public Collection<String> getInstrumentationGroupNames() {
        Collection<String> groupNames = super.getInstrumentationGroupNames();
        groupNames.add("log4j");
        return groupNames;
    }

    @Override
    public Class<?> getAdviceClass() {
        return Log4jAdvice.class;
    }

    public static class Log4jAdvice {

        /**
         * collect log4j log info
         */
        @Advice.OnMethodEnter(suppress = Throwable.class, inline = false)
        public static void onEnter(@Advice.Argument(value = 0, optional = true) LoggingEvent logEvent) {

            String traceId = traceId();
            long timestamp = System.currentTimeMillis();

            Level level = logEvent.getLevel();
            String logLevel = level.toString();
            LogLevelEnum currentLogLevel = LogLevelEnum.valueOf(logLevel);
            if (!toCollect(currentLogLevel)) {
                return;
            }
            Object message = logEvent.getMessage();
            LocationInfo locationInformation = logEvent.getLocationInformation();
            String className = locationInformation.getClassName();
            String methodName = locationInformation.getMethodName();
            String lineNumber = locationInformation.getLineNumber();
            String[] strRep = logEvent.getThrowableStrRep();

            if (className.startsWith("co.elastic.apm") && !className.endsWith("Test")) {
                return;
            }

            StringBuilder builder = new StringBuilder();
            for (String s : strRep) {
                builder.append(s).append(System.getProperty("line.separator"));
            }
            String stack = builder.toString();

            // write current log detail info


            // format log and send to kafka
            log(timestamp,
                Thread.currentThread().getName(),
                logLevel,
                className,
                methodName,
                lineNumber,
                traceId,
                message == null ? "" : message.toString(),
                stack
            );

        }
    }
}

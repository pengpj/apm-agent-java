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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

import java.util.Collection;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.not;

public class LogbackLogCollectorInstrumentation extends AbstractLogCollectorInstrumentation {

    private static final String LOGBACK_EVENT = "ch!qos!logback!classic!Logger".replace("!", ".");
    private static final String SLF4J_LOGGER = "org!slf4j!Logger".replace('!', '.');

    @Override
    public ElementMatcher<? super TypeDescription> getTypeMatcher() {
        return named(LOGBACK_EVENT)
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
        groupNames.add("logback");
        return groupNames;
    }

    @Override
    public Class<?> getAdviceClass() {
        return LogbackAdvice.class;
    }

    public static class LogbackAdvice {

        /**
         * collect logback log info
         */
        @Advice.OnMethodEnter(suppress = Throwable.class, inline = false)
        public static void onEnter(@Advice.Argument(value = 0, optional = true) ILoggingEvent param) {

            String traceId = traceId();
            long timestamp = System.currentTimeMillis();

            if (null == param) {
                nothing(param);
                return;
            }
            ilogbackEvent(traceId, timestamp, param);
        }

    }

    public static String nothing(Object param) {
        if (null == param) {
            return "null";
        }
        return param.getClass().getName();
    }


    public static void ilogbackEvent(String traceId, long timestamp, ILoggingEvent logEvent) {
        Level level = logEvent.getLevel();
        String logLevel = level.toString();
        LogLevelEnum currentLogLevel = LogLevelEnum.valueOf(logLevel);
        if (!toCollect(currentLogLevel)) {
            return;
        }
        Object message = logEvent.getFormattedMessage();
        String className = logEvent.getLoggerName();
        StackTraceElement[] callers = logEvent.getCallerData();
        String methodName = "";
        String lineNumber = "";
        for (StackTraceElement caller : callers) {
            methodName = caller.getMethodName();
            lineNumber = caller.getLineNumber() + "";
        }
        IThrowableProxy throwableProxy = logEvent.getThrowableProxy();

        StringBuilder builder = new StringBuilder();
        if (throwableProxy != null) {
            StackTraceElementProxy[] stackTraceElementProxyArray = throwableProxy.getStackTraceElementProxyArray();
            for (StackTraceElementProxy elementProxy : stackTraceElementProxyArray) {
                builder.append(elementProxy.getSTEAsString()).append(System.getProperty("line.separator"));
            }
        }

        if (className.startsWith("co.elastic.apm") && !className.endsWith("Test")) {
            return;
        }

        String stack = builder.toString();

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

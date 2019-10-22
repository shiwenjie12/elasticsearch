/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.indices.breaker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.common.breaker.CircuitBreaker;
import org.elasticsearch.common.component.AbstractLifecycleComponent;
import org.elasticsearch.common.settings.Settings;

/**
 * Interface for Circuit Breaker services, which provide breakers to classes
 * that load field data.
 * 断路器服务接口，为加载现场数据的类提供断路器。
 */
public abstract class CircuitBreakerService extends AbstractLifecycleComponent {
    private static final Logger logger = LogManager.getLogger(CircuitBreakerService.class);

    protected CircuitBreakerService(Settings settings) {
        super(settings);
    }

    /**
     * 允许注册定制断路器。
     */
    public abstract void registerBreaker(BreakerSettings breakerSettings);

    /**
     * @return可用于注册估计的断路器
     */
    public abstract CircuitBreaker getBreaker(String name);

    /**
     * 关于所有破坏者的@return统计数据
     */
    public abstract AllCircuitBreakerStats stats();

    /**
     * @return关于特定断路器的统计数据
     */
    public abstract CircuitBreakerStats stats(String name);

    @Override
    protected void doStart() {
    }

    @Override
    protected void doStop() {
    }

    @Override
    protected void doClose() {
    }

}

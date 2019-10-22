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
package org.elasticsearch.plugins;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.elasticsearch.common.io.stream.NamedWriteableRegistry;
import org.elasticsearch.common.network.NetworkService;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.BigArrays;
import org.elasticsearch.common.util.PageCacheRecycler;
import org.elasticsearch.common.util.concurrent.ThreadContext;
import org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.elasticsearch.http.HttpServerTransport;
import org.elasticsearch.indices.breaker.CircuitBreakerService;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.Transport;
import org.elasticsearch.transport.TransportInterceptor;

/**
 * 用于扩展网络和传输相关类的插件
 */
public interface NetworkPlugin {

    /**
     * 返回{@link TransportInterceptor}实例的列表，这些实例用于拦截传入和传出的传输（节点间）请求。这一定不能返回<code>null</code>
     *
     * @param namedWriteableRegistry registry of all named writeables registered
     * @param threadContext a {@link ThreadContext} of the current nodes or clients {@link ThreadPool} that can be used to set additional
     *                      headers in the interceptors
     */
    default List<TransportInterceptor> getTransportInterceptors(NamedWriteableRegistry namedWriteableRegistry,
                                                                ThreadContext threadContext) {
        return Collections.emptyList();
    }

    /**
     * 返回{@link Transport}供应商的地图。
     * 请参阅{@link org.elasticsearch.common.network.NetworkModule#TRANSPORT_TYPE_KEY}来配置特定的实现。
     */
    default Map<String, Supplier<Transport>> getTransports(Settings settings, ThreadPool threadPool, BigArrays bigArrays,
                                                           PageCacheRecycler pageCacheRecycler,
                                                           CircuitBreakerService circuitBreakerService,
                                                           NamedWriteableRegistry namedWriteableRegistry,
                                                           NetworkService networkService) {
        return Collections.emptyMap();
    }

    /**
     * 返回{@link HttpServerTransport}供应商的地图。
     * 请参阅{@link org.elasticsearch.common.network.NetworkModule#HTTP_TYPE_SETTING}来配置特定的实现。
     */
    default Map<String, Supplier<HttpServerTransport>> getHttpTransports(Settings settings, ThreadPool threadPool, BigArrays bigArrays,
                                                                         PageCacheRecycler pageCacheRecycler,
                                                                         CircuitBreakerService circuitBreakerService,
                                                                         NamedXContentRegistry xContentRegistry,
                                                                         NetworkService networkService,
                                                                         HttpServerTransport.Dispatcher dispatcher) {
        return Collections.emptyMap();
    }
}

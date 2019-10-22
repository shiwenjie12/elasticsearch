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

package org.elasticsearch.discovery.zen;

import org.elasticsearch.common.transport.TransportAddress;

import java.util.List;

/**
 * 可用于单播发现的单播主机列表的可插入提供程序。
 */
public interface UnicastHostsProvider {

    /**
     * 构建用于单播发现的单播主机的动态列表。
     */
    List<TransportAddress> buildDynamicHosts(HostsResolver hostsResolver);

    /**
     * 允许将主机列表解析为传输地址列表的Helper对象。
     * 每个主机都解析为一个传输地址（如果端口数大于一个，则解析为地址的集合）
      */
    interface HostsResolver {
        List<TransportAddress> resolveHosts(List<String> hosts, int limitPortCounts);
    }

}

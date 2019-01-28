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

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.function.Supplier;

import org.elasticsearch.cluster.routing.allocation.allocator.ShardsAllocator;
import org.elasticsearch.cluster.routing.allocation.decider.AllocationDecider;
import org.elasticsearch.common.settings.ClusterSettings;
import org.elasticsearch.common.settings.Settings;

/**
 * {@link Plugin}实现对客户群集管理行为的扩展点。
 */
public interface ClusterPlugin {

    /**
     * 返回决策者用于自定义分配分片的位置。
     *
     * @param settings Settings for the node
     * @param clusterSettings Settings for the cluster
     * @return Custom {@link AllocationDecider} instances
     */
    default Collection<AllocationDecider> createAllocationDeciders(Settings settings, ClusterSettings clusterSettings) {
        return Collections.emptyList();
    }

    /**
     * Return {@link ShardsAllocator} implementations added by this plugin.
     *
     * The key of the returned {@link Map} is the name of the allocator, and the value
     * is a function to construct the allocator.
     *
     * @param settings Settings for the node
     * @param clusterSettings Settings for the cluster
     * @return A map of allocator implementations
     */
    default Map<String, Supplier<ShardsAllocator>> getShardsAllocators(Settings settings, ClusterSettings clusterSettings) {
        return Collections.emptyMap();
    }

    /**
     * 在节点启动时调用
     */
    default void onNodeStarted() {
    }

}

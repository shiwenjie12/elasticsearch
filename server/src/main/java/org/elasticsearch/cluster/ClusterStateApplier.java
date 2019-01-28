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

package org.elasticsearch.cluster;

import org.elasticsearch.cluster.service.ClusterService;

/**
 * A component that is in charge of applying an incoming cluster state to the node internal data structures.
 * The single apply method is called before the cluster state becomes visible via {@link ClusterService#state()}.
 * 负责将传入群集状态应用于节点内部数据结构的组件。
 * 在通过{@link ClusterService＃state（）}显示集群状态之前，将调用单个apply方法。
 */
public interface ClusterStateApplier {

    /**
     * 在需要应用新的群集状态（{@link ClusterChangedEvent＃state（）}时调用
     */
    void applyClusterState(ClusterChangedEvent event);
}

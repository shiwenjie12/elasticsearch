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

package org.elasticsearch.cluster.routing.allocation.allocator;

import org.elasticsearch.cluster.routing.ShardRouting;
import org.elasticsearch.cluster.routing.allocation.AllocateUnassignedDecision;
import org.elasticsearch.cluster.routing.allocation.MoveDecision;
import org.elasticsearch.cluster.routing.allocation.RoutingAllocation;
import org.elasticsearch.cluster.routing.allocation.ShardAllocationDecision;

/**
 * <p>
 * A {@link ShardsAllocator} is the main entry point for shard allocation on nodes in the cluster.
 * The allocator makes basic decision where a shard instance will be allocated, if already allocated instances
 * need to relocate to other nodes due to node failures or due to rebalancing decisions.
 *
 * {@link ShardsAllocator}是群集中节点上分片分配的主要入口点。
 * 如果已分配的实例由于节点故障或由于重新平衡决策而需要重新定位到其他节点，则分配器将在分配分片实例的位置做出基本决策。
 * </p>
 */
public interface ShardsAllocator {

    /**
     * 将分片分配给群集中的节点。该方法的实现应该：
     *  - 分配未分配的分片
     *  - 重新定位不能再停留在节点上的分片
     *  - 重新定位分片以在群集中找到良好的分片平衡
     *
     * @param allocation current node allocation
     */
    void allocate(RoutingAllocation allocation);

    /**
     * 返回分片应驻留在集群中的位置的决策。如果分片未分配，则{@link AllocateUnassignedDecision}将为非null。
     * 如果分片未处于未分配状态，则{@link MoveDecision}将为非null。
     *
     * This method is primarily used by the cluster allocation explain API to provide detailed explanations
     * for the allocation of a single shard.  Implementations of the {@link #allocate(RoutingAllocation)} method
     * may use the results of this method implementation to decide on allocating shards in the routing table
     * to the cluster.
     *
     * If an implementation of this interface does not support explaining decisions for a single shard through
     * the cluster explain API, then this method should throw a {@code UnsupportedOperationException}.
     */
    ShardAllocationDecision decideShardAllocation(ShardRouting shard, RoutingAllocation allocation);
}

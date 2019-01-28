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

import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.unit.TimeValue;

// 具有回复行为的集群状态监听器
public interface AckedClusterStateTaskListener extends ClusterStateTaskListener {

    /**
     * 被调用以确定期望确认的节点。
     *
     * 由于此方法将被多次调用以确定acking节点集，因此返回一致结果至关重要：
     * 给定相同的侦听器实例和相同的节点参数，方法实现应返回相同的结果。
     *
     * @param discoveryNode a node
     * @return true if the node is expected to send ack back, false otherwise
     */
    boolean mustAck(DiscoveryNode discoveryNode);

    /**
     * Called once all the nodes have acknowledged the cluster state update request. Must be
     * very lightweight execution, since it gets executed on the cluster service thread.
     *
     * @param e optional error that might have been thrown
     */
    void onAllNodesAcked(@Nullable Exception e);

    /**
     * Called once the acknowledgement timeout defined by
     * {@link AckedClusterStateUpdateTask#ackTimeout()} has expired
     */
    void onAckTimeout();

    /**
     * Acknowledgement timeout, maximum time interval to wait for acknowledgements
     */
    TimeValue ackTimeout();

}

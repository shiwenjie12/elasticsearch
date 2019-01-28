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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.cluster.node.DiscoveryNodes;
import org.elasticsearch.common.component.AbstractLifecycleComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.lease.Releasable;
import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.util.concurrent.AbstractRunnable;
import org.elasticsearch.common.util.concurrent.ConcurrentCollections;
import org.elasticsearch.common.util.concurrent.FutureUtils;
import org.elasticsearch.common.util.concurrent.KeyedLock;
import org.elasticsearch.discovery.zen.MasterFaultDetection;
import org.elasticsearch.discovery.zen.NodesFaultDetection;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;

import static org.elasticsearch.common.settings.Setting.Property;
import static org.elasticsearch.common.settings.Setting.positiveTimeSetting;


/**
 * This component is responsible for connecting to nodes once they are added to the cluster state, and disconnect when they are
 * removed. Also, it periodically checks that all connections are still open and if needed restores them.
 * Note that this component is *not* responsible for removing nodes from the cluster if they disconnect / do not respond
 * to pings. This is done by {@link NodesFaultDetection}. Master fault detection
 * is done by {@link MasterFaultDetection}.
 * 此组件负责在节点添加到群集状态后连接到节点，并在删除它们时断开连接。 此外，它会定期检查所有连接是否仍处于打开状态，并在需要时进行恢复。
 * 请注意，如果组件断开/不响应ping，则此组件不负责从群集中删除节点。
 * 这是由{@link NodesFaultDetection}完成的。 主故障检测由{@link MasterFaultDetection}完成。
 */
public class NodeConnectionsService extends AbstractLifecycleComponent {
    private static final Logger logger = LogManager.getLogger(NodeConnectionsService.class);

    public static final Setting<TimeValue> CLUSTER_NODE_RECONNECT_INTERVAL_SETTING =
            positiveTimeSetting("cluster.nodes.reconnect_interval", TimeValue.timeValueSeconds(10), Property.NodeScope);
    private final ThreadPool threadPool;
    private final TransportService transportService;

    // map between current node and the number of failed connection attempts. 0 means successfully connected.
    // if a node doesn't appear in this list it shouldn't be monitored
    private ConcurrentMap<DiscoveryNode, Integer> nodes = ConcurrentCollections.newConcurrentMap();

    private final KeyedLock<DiscoveryNode> nodeLocks = new KeyedLock<>();

    private final TimeValue reconnectInterval;

    private volatile ScheduledFuture<?> backgroundFuture = null;

    @Inject
    public NodeConnectionsService(Settings settings, ThreadPool threadPool, TransportService transportService) {
        super(settings);
        this.threadPool = threadPool;
        this.transportService = transportService;
        this.reconnectInterval = NodeConnectionsService.CLUSTER_NODE_RECONNECT_INTERVAL_SETTING.get(settings);
    }

    public void connectToNodes(DiscoveryNodes discoveryNodes) {
        CountDownLatch latch = new CountDownLatch(discoveryNodes.getSize());
        for (final DiscoveryNode node : discoveryNodes) {
            final boolean connected;
            try (Releasable ignored = nodeLocks.acquire(node)) {
                nodes.putIfAbsent(node, 0);
                connected = transportService.nodeConnected(node); // 节点是否已经连接
            }
            if (connected) { // 连接成功
                latch.countDown();
            } else {
                // 产生到另一个线程并行执行
                threadPool.executor(ThreadPool.Names.MANAGEMENT).execute(new AbstractRunnable() {
                    @Override
                    public void onFailure(Exception e) {
                        // both errors and rejections are logged here. the service
                        // will try again after `cluster.nodes.reconnect_interval` on all nodes but the current master.
                        // On the master, node fault detection will remove these nodes from the cluster as their are not
                        // connected. Note that it is very rare that we end up here on the master.
                        logger.warn(() -> new ParameterizedMessage("failed to connect to {}", node), e);
                    }

                    @Override
                    protected void doRun() {
                        try (Releasable ignored = nodeLocks.acquire(node)) {
                            validateAndConnectIfNeeded(node);
                        }
                    }

                    @Override
                    public void onAfter() {
                        latch.countDown();
                    }
                });
            }
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 断开除了作为参数提供的节点之外的所有节点
     */
    public void disconnectFromNodesExcept(DiscoveryNodes nodesToKeep) {
        Set<DiscoveryNode> currentNodes = new HashSet<>(nodes.keySet());
        for (DiscoveryNode node : nodesToKeep) {
            currentNodes.remove(node);
        }
        for (final DiscoveryNode node : currentNodes) {
            try (Releasable ignored = nodeLocks.acquire(node)) {
                Integer current = nodes.remove(node);
                assert current != null : "node " + node + " was removed in event but not in internal nodes";
                try {
                    transportService.disconnectFromNode(node); // 断开连接
                } catch (Exception e) {
                    logger.warn(() -> new ParameterizedMessage("failed to disconnect to node [{}]", node), e);
                }
            }
        }
    }

    void validateAndConnectIfNeeded(DiscoveryNode node) {
        assert nodeLocks.isHeldByCurrentThread(node) : "validateAndConnectIfNeeded must be called under lock";
        if (lifecycle.stoppedOrClosed() ||
                nodes.containsKey(node) == false) { // we double check existence of node since connectToNode might take time...
            // nothing to do
        } else {
            try {
                // 连接到已连接的节点是noop
                transportService.connectToNode(node); // 连接节点
                nodes.put(node, 0);
            } catch (Exception e) {
                Integer nodeFailureCount = nodes.get(node);
                assert nodeFailureCount != null : node + " didn't have a counter in nodes map";
                nodeFailureCount = nodeFailureCount + 1;
                // log every 6th failure
                if ((nodeFailureCount % 6) == 1) {
                    final int finalNodeFailureCount = nodeFailureCount;
                    logger.warn(() -> new ParameterizedMessage(
                                "failed to connect to node {} (tried [{}] times)", node, finalNodeFailureCount), e);
                }
                nodes.put(node, nodeFailureCount);
            }
        }
    }

    // 连接检查
    class ConnectionChecker extends AbstractRunnable {

        @Override
        public void onFailure(Exception e) {
            logger.warn("unexpected error while checking for node reconnects", e);
        }

        protected void doRun() {
            for (DiscoveryNode node : nodes.keySet()) {
                try (Releasable ignored = nodeLocks.acquire(node)) {
                    validateAndConnectIfNeeded(node);
                }
            }
        }

        @Override
        public void onAfter() {
            if (lifecycle.started()) {
                backgroundFuture = threadPool.schedule(reconnectInterval, ThreadPool.Names.GENERIC, this);
            }
        }
    }

    @Override
    protected void doStart() {
        // 定时发送连接检查任务
        backgroundFuture = threadPool.schedule(reconnectInterval, ThreadPool.Names.GENERIC, new ConnectionChecker());
    }

    @Override
    protected void doStop() {
        FutureUtils.cancel(backgroundFuture);
    }

    @Override
    protected void doClose() {

    }
}

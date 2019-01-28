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

package org.elasticsearch.cluster.routing.allocation;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

import com.carrotsearch.hppc.ObjectLookupContainer;
import com.carrotsearch.hppc.cursors.ObjectObjectCursor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.ClusterInfo;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.DiskUsage;
import org.elasticsearch.cluster.block.ClusterBlockLevel;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.cluster.routing.RoutingNode;
import org.elasticsearch.cluster.routing.ShardRouting;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.common.settings.ClusterSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.set.Sets;

/**
 * 侦听节点以查看高水位线并启动空重新路由（如果有）。 还负责记录已通过磁盘水印的节点
 */
public class DiskThresholdMonitor {

    private static final Logger logger = LogManager.getLogger(DiskThresholdMonitor.class);

    private final DiskThresholdSettings diskThresholdSettings;
    private final Client client;
    private final Set<String> nodeHasPassedWatermark = Sets.newConcurrentHashSet();
    private final Supplier<ClusterState> clusterStateSupplier;
    private long lastRunNS;

    public DiskThresholdMonitor(Settings settings, Supplier<ClusterState> clusterStateSupplier, ClusterSettings clusterSettings,
                                Client client) {
        this.clusterStateSupplier = clusterStateSupplier;
        this.diskThresholdSettings = new DiskThresholdSettings(settings, clusterSettings);
        this.client = client;
    }

    /**
     * 如果已通过低水位或高水印，则警告给定的磁盘使用情况
     */
    private void warnAboutDiskIfNeeded(DiskUsage usage) {
        // Check absolute disk values
        if (usage.getFreeBytes() < diskThresholdSettings.getFreeBytesThresholdFloodStage().getBytes()) {
            logger.warn("flood stage disk watermark [{}] exceeded on {}, all indices on this node will be marked read-only",
                diskThresholdSettings.getFreeBytesThresholdFloodStage(), usage);
        } else if (usage.getFreeBytes() < diskThresholdSettings.getFreeBytesThresholdHigh().getBytes()) {
            logger.warn("high disk watermark [{}] exceeded on {}, shards will be relocated away from this node",
                diskThresholdSettings.getFreeBytesThresholdHigh(), usage);
        } else if (usage.getFreeBytes() < diskThresholdSettings.getFreeBytesThresholdLow().getBytes()) {
            logger.info("low disk watermark [{}] exceeded on {}, replicas will not be assigned to this node",
                diskThresholdSettings.getFreeBytesThresholdLow(), usage);
        }

        // Check percentage disk values
        if (usage.getFreeDiskAsPercentage() < diskThresholdSettings.getFreeDiskThresholdFloodStage()) {
            logger.warn("flood stage disk watermark [{}] exceeded on {}, all indices on this node will be marked read-only",
                Strings.format1Decimals(100.0 - diskThresholdSettings.getFreeDiskThresholdFloodStage(), "%"), usage);
        } else if (usage.getFreeDiskAsPercentage() < diskThresholdSettings.getFreeDiskThresholdHigh()) {
            logger.warn("high disk watermark [{}] exceeded on {}, shards will be relocated away from this node",
                Strings.format1Decimals(100.0 - diskThresholdSettings.getFreeDiskThresholdHigh(), "%"), usage);
        } else if (usage.getFreeDiskAsPercentage() < diskThresholdSettings.getFreeDiskThresholdLow()) {
            logger.info("low disk watermark [{}] exceeded on {}, replicas will not be assigned to this node",
                Strings.format1Decimals(100.0 - diskThresholdSettings.getFreeDiskThresholdLow(), "%"), usage);
        }
    }


    public void onNewInfo(ClusterInfo info) {
        ImmutableOpenMap<String, DiskUsage> usages = info.getNodeLeastAvailableDiskUsages();
        if (usages != null) {
            boolean reroute = false;
            String explanation = "";

            // Garbage collect nodes that have been removed from the cluster
            // from the map that tracks watermark crossing
            ObjectLookupContainer<String> nodes = usages.keys();
            for (String node : nodeHasPassedWatermark) {
                if (nodes.contains(node) == false) {
                    nodeHasPassedWatermark.remove(node);
                }
            }
            ClusterState state = clusterStateSupplier.get();
            Set<String> indicesToMarkReadOnly = new HashSet<>();
            for (ObjectObjectCursor<String, DiskUsage> entry : usages) {
                String node = entry.key;
                DiskUsage usage = entry.value;
                warnAboutDiskIfNeeded(usage); // 检查警告
                if (usage.getFreeBytes() < diskThresholdSettings.getFreeBytesThresholdFloodStage().getBytes() ||
                    usage.getFreeDiskAsPercentage() < diskThresholdSettings.getFreeDiskThresholdFloodStage()) { // 禁止写入
                    RoutingNode routingNode = state.getRoutingNodes().node(node); // 节点上的所有路由
                    if (routingNode != null) { // this might happen if we haven't got the full cluster-state yet?!
                        for (ShardRouting routing : routingNode) {
                            indicesToMarkReadOnly.add(routing.index().getName()); // 只允许读的索引
                        }
                    }
                } else if (usage.getFreeBytes() < diskThresholdSettings.getFreeBytesThresholdHigh().getBytes() ||
                    usage.getFreeDiskAsPercentage() < diskThresholdSettings.getFreeDiskThresholdHigh()) {
                    if ((System.nanoTime() - lastRunNS) > diskThresholdSettings.getRerouteInterval().nanos()) {// 重新路由
                        lastRunNS = System.nanoTime();
                        reroute = true;
                        explanation = "high disk watermark exceeded on one or more nodes";
                    } else {
                        logger.debug("high disk watermark exceeded on {} but an automatic reroute has occurred " +
                                "in the last [{}], skipping reroute",
                            node, diskThresholdSettings.getRerouteInterval());
                    }
                    nodeHasPassedWatermark.add(node);
                } else if (usage.getFreeBytes() < diskThresholdSettings.getFreeBytesThresholdLow().getBytes() ||
                    usage.getFreeDiskAsPercentage() < diskThresholdSettings.getFreeDiskThresholdLow()) {
                    nodeHasPassedWatermark.add(node);
                } else {
                    if (nodeHasPassedWatermark.contains(node)) {
                        // The node has previously been over the high or
                        // low watermark, but is no longer, so we should
                        // reroute so any unassigned shards can be allocated
                        // if they are able to be
                        if ((System.nanoTime() - lastRunNS) > diskThresholdSettings.getRerouteInterval().nanos()) {
                            lastRunNS = System.nanoTime();
                            reroute = true;
                            explanation = "one or more nodes has gone under the high or low watermark";
                            nodeHasPassedWatermark.remove(node);
                        } else {
                            logger.debug("{} has gone below a disk threshold, but an automatic reroute has occurred " +
                                    "in the last [{}], skipping reroute",
                                node, diskThresholdSettings.getRerouteInterval());
                        }
                    }
                }
            }
            if (reroute) {
                logger.info("rerouting shards: [{}]", explanation);
                reroute(); // 重新路由
            }
            indicesToMarkReadOnly.removeIf(index -> state.getBlocks().indexBlocked(ClusterBlockLevel.WRITE, index));
            if (indicesToMarkReadOnly.isEmpty() == false) {
                markIndicesReadOnly(indicesToMarkReadOnly);
            }
        }
    }

    protected void markIndicesReadOnly(Set<String> indicesToMarkReadOnly) {
        // 设置只读块但不阻止响应
        client.admin().indices().prepareUpdateSettings(indicesToMarkReadOnly.toArray(Strings.EMPTY_ARRAY)).
            setSettings(Settings.builder().put(IndexMetaData.SETTING_READ_ONLY_ALLOW_DELETE, true).build()).execute();
    }

    protected void reroute() {
        // 执行空重新路由，但不要阻止响应
        client.admin().cluster().prepareReroute().execute();
    }
}

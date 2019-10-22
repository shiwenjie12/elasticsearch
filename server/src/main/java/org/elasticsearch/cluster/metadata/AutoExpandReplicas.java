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
package org.elasticsearch.cluster.metadata;

import org.elasticsearch.cluster.node.DiscoveryNodes;
import org.elasticsearch.common.Booleans;
import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.common.settings.Setting.Property;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

/**
 * 此类充当{@code index.auto_expand_replicas}设置的功能包装器。
 * 此设置或更确切地说，它的值将扩展为最小值和最大值，这需要根据群集中的数据节点数进行特殊处理。此类处理所有解析并简化对这些值的访问。
 */
public final class AutoExpandReplicas {
    // 我们在“max”位置识别的值表示所有节点
    private static final String ALL_NODES_VALUE = "all";

    private static final AutoExpandReplicas FALSE_INSTANCE = new AutoExpandReplicas(0, 0, false);

    public static final Setting<AutoExpandReplicas> SETTING = new Setting<>(IndexMetaData.SETTING_AUTO_EXPAND_REPLICAS, "false",
        AutoExpandReplicas::parse, Property.Dynamic, Property.IndexScope);

    private static AutoExpandReplicas parse(String value) {
        final int min;
        final int max;
        if (Booleans.isFalse(value)) {
            return FALSE_INSTANCE;
        }
        final int dash = value.indexOf('-');
        if (-1 == dash) {
            throw new IllegalArgumentException("failed to parse [" + IndexMetaData.SETTING_AUTO_EXPAND_REPLICAS +
                "] from value: [" + value + "] at index " + dash);
        }
        final String sMin = value.substring(0, dash);
        try {
            min = Integer.parseInt(sMin);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("failed to parse [" + IndexMetaData.SETTING_AUTO_EXPAND_REPLICAS +
                "] from value: [" + value + "] at index " + dash, e);
        }
        String sMax = value.substring(dash + 1);
        if (sMax.equals(ALL_NODES_VALUE)) {
            max = Integer.MAX_VALUE;
        } else {
            try {
                max = Integer.parseInt(sMax);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("failed to parse [" + IndexMetaData.SETTING_AUTO_EXPAND_REPLICAS +
                    "] from value: [" + value + "] at index " + dash, e);
            }
        }
        return new AutoExpandReplicas(min, max, true);
    }

    private final int minReplicas;
    private final int maxReplicas;
    private final boolean enabled;

    private AutoExpandReplicas(int minReplicas, int maxReplicas, boolean enabled) {
        if (minReplicas > maxReplicas) {
            throw new IllegalArgumentException("[" + IndexMetaData.SETTING_AUTO_EXPAND_REPLICAS +
                "] minReplicas must be =< maxReplicas but wasn't " + minReplicas + " > " + maxReplicas);
        }
        this.minReplicas = minReplicas;
        this.maxReplicas = maxReplicas;
        this.enabled = enabled;
    }

    int getMinReplicas() {
        return minReplicas;
    }

    int getMaxReplicas(int numDataNodes) {
        return Math.min(maxReplicas, numDataNodes - 1);
    }

    // 获取期望的副本数
    private OptionalInt getDesiredNumberOfReplicas(int numDataNodes) {
        if (enabled) {
            final int min = getMinReplicas();
            final int max = getMaxReplicas(numDataNodes);
            int numberOfReplicas = numDataNodes - 1;
            if (numberOfReplicas < min) {
                numberOfReplicas = min;
            } else if (numberOfReplicas > max) {
                numberOfReplicas = max;
            }

            if (numberOfReplicas >= min && numberOfReplicas <= max) {
                return OptionalInt.of(numberOfReplicas);
            }
        }
        return OptionalInt.empty();
    }

    @Override
    public String toString() {
        return enabled ? minReplicas + "-" + maxReplicas : "false";
    }

    boolean isEnabled() {
        return enabled;
    }

    /**
     * 检查是否具有需要调整的自动扩展功能的副本。
     * 返回更新映射，将要更新的索引映射到所需的副本数。
     * 映射具有所需数量的副本作为键和要更新为值的索引，因为这允许将此方法的结果直接应用于RoutingTable.Builder＃updateNumberOfReplicas。
     */
    public static Map<Integer, List<String>> getAutoExpandReplicaChanges(MetaData metaData, DiscoveryNodes discoveryNodes) {
        // used for translating "all" to a number
        // 所有的数据节点
        final int dataNodeCount = discoveryNodes.getDataNodes().size();

        Map<Integer, List<String>> nrReplicasChanged = new HashMap<>();

        for (final IndexMetaData indexMetaData : metaData) {
            if (indexMetaData.getState() != IndexMetaData.State.CLOSE) {
                AutoExpandReplicas autoExpandReplicas = SETTING.get(indexMetaData.getSettings());
                autoExpandReplicas.getDesiredNumberOfReplicas(dataNodeCount).ifPresent(numberOfReplicas -> {
                    // 如果期望的副本数不符合索引的副本，则添加
                    if (numberOfReplicas != indexMetaData.getNumberOfReplicas()) {
                        nrReplicasChanged.computeIfAbsent(numberOfReplicas, ArrayList::new).add(indexMetaData.getIndex().getName());
                    }
                });
            }
        }
        return nrReplicasChanged;
    }
}



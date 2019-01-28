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

package org.elasticsearch.cluster.routing;


/**
 * 表示群集定义的{@link ShardRouting}的当前状态。
 * cluster.
 */
public enum ShardRoutingState {
    /**
     * 分片未分配给任何节点。
     */
    UNASSIGNED((byte) 1),
    /**
     * 分片正在初始化（可能从对等分片或网关恢复）。
     */
    INITIALIZING((byte) 2),
    /**
     * 碎片已启动。
     */
    STARTED((byte) 3),
    /**
     * 该分片正在重新定位。
     */
    RELOCATING((byte) 4);

    private byte value;

    ShardRoutingState(byte value) {
        this.value = value;
    }

    /**
     * Byte value of this {@link ShardRoutingState}
     * @return Byte value of this {@link ShardRoutingState}
     */
    public byte value() {
        return this.value;
    }

    public static ShardRoutingState fromValue(byte value) {
        switch (value) {
            case 1:
                return UNASSIGNED;
            case 2:
                return INITIALIZING;
            case 3:
                return STARTED;
            case 4:
                return RELOCATING;
            default:
                throw new IllegalStateException("No routing state mapped for [" + value + "]");
        }
    }
}

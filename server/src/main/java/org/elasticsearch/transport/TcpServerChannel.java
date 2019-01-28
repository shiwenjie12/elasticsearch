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

package org.elasticsearch.transport;

import org.elasticsearch.common.network.CloseableChannel;

import java.net.InetSocketAddress;


/**
 * This is a tcp channel representing a server channel listening for new connections. It is the server
 * channel abstraction used by the {@link TcpTransport} and {@link TransportService}. All tcp transport
 * implementations must return server channels that adhere to the required method contracts.
 * 这是一个tcp通道，表示侦听新连接的服务器通道。它是{@link TcpTransport}和{@link TransportService}使用的服务器通道抽象。
 * 所有tcp传输实现都必须返回符合所需方法契约的服务器通道。
 */
public interface TcpServerChannel extends CloseableChannel {

    /**
     * This returns the profile for this channel.
     */
    String getProfile();

    /**
     * Returns the local address for this channel.
     *
     * @return the local address of this channel.
     */
    InetSocketAddress getLocalAddress();

}

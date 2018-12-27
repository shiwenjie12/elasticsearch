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

package org.elasticsearch.common;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

/**
 * A registry from String to some class implementation. Used to ensure implementations are registered only once.
 * 由名称和实现组成的键值对的注册服务
 */
public class NamedRegistry<T> {
    private final Map<String, T> registry = new HashMap<>();
    private final String targetName;

    public NamedRegistry(String targetName) {
        this.targetName = targetName;
    }

    public Map<String, T> getRegistry() {
        return registry;
    }

    // 注册实现
    public void register(String name, T t) {
        requireNonNull(name, "name is required");
        requireNonNull(t, targetName + " is required");
        if (registry.putIfAbsent(name, t) != null) {
            throw new IllegalArgumentException(targetName + " for name [" + name + "] already registered");
        }
    }

    // 扩展注册插件
    public <P> void extractAndRegister(List<P> plugins, Function<P, Map<String, T>> lookup) {
        for (P plugin : plugins) {
            for (Map.Entry<String, T> entry : lookup.apply(plugin).entrySet()) {// 实现插件与服务的转换
                register(entry.getKey(), entry.getValue());
            }
        }
    }
}

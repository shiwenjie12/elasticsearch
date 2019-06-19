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

/**
 * 保持以下值之一：
 * a）隐式设置，例如通过一些默认值
 * b）明确设定，例如来自用户选择
 * 
 * 合并冲突的配置设置（如字段映射设置）时，最好保留一个明确的选择，而不是默认情况下隐式进行的选择。
 */
public class Explicit<T> {

    private final T value;
    private final boolean explicit;
    /**
     * Create a value with an indication if this was an explicit choice
     * @param value a setting value
     * @param explicit true if the value passed is a conscious decision, false if using some kind of default
     */
    public Explicit(T value, boolean explicit) {
        this.value = value;
        this.explicit = explicit;
    }

    public T value() {
        return this.value;
    }

    /**
     * 如果传递的值是有意识的决定，则@return为true，如果使用某种默认值，则为false
     */
    public boolean explicit() {
        return this.explicit;
    }
}

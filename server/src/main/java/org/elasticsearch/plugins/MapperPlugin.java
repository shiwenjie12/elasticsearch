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

package org.elasticsearch.plugins;

import org.elasticsearch.index.mapper.Mapper;
import org.elasticsearch.index.mapper.MetadataFieldMapper;

import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * {@link Plugin}实现的扩展点，用于添加自定义映射器
 */
public interface MapperPlugin {

    /**
     * 返回此插件添加的其他映射器实现。
     * <p>
     * The key of the returned {@link Map} is the unique name for the mapper which will be used
     * as the mapping {@code type}, and the value is a {@link Mapper.TypeParser} to parse the
     * mapper settings into a {@link Mapper}.
     */
    default Map<String, Mapper.TypeParser> getMappers() {
        return Collections.emptyMap();
    }

    /**
     * 返回此插件添加的其他元数据映射器实现。
     * <p>
     * The key of the returned {@link Map} is the unique name for the metadata mapper, which
     * is used in the mapping json to configure the metadata mapper, and the value is a
     * {@link MetadataFieldMapper.TypeParser} to parse the mapper settings into a
     * {@link MetadataFieldMapper}.
     */
    default Map<String, MetadataFieldMapper.TypeParser> getMetadataMappers() {
        return Collections.emptyMap();
    }

    /**
     * 返回给定索引名称返回谓词的函数，这些谓词必须匹配才能通过get mappings，get index，get field mappings和field capabilities API返回。
     * 用于过滤此API返回的字段。谓词接收字段名称作为输入参数，并且应该返回true以显示字段，并返回false以隐藏它。
     */
    default Function<String, Predicate<String>> getFieldFilter() {
        return NOOP_FIELD_FILTER;
    }

    /**
     * The default field predicate applied, which doesn't filter anything. That means that by default get mappings, get index
     * get field mappings and field capabilities API will return every field that's present in the mappings.
     */
    Predicate<String> NOOP_FIELD_PREDICATE = field -> true;

    /**
     * 应用了默认字段过滤器，但不过滤任何内容。这意味着默认情况下获取映射，
     * 获取索引获取字段映射和字段功能API将返回映射中存在的每个字段。
     */
    Function<String, Predicate<String>> NOOP_FIELD_FILTER = index -> NOOP_FIELD_PREDICATE;
}

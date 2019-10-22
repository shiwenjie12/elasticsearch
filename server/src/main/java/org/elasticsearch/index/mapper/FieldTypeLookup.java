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

package org.elasticsearch.index.mapper;

import org.elasticsearch.common.collect.CopyOnWriteHashMap;
import org.elasticsearch.common.regex.Regex;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 一个不可变的容器，用于按名称查找{@link MappedFieldType}。
 */
class FieldTypeLookup implements Iterable<MappedFieldType> {

    final CopyOnWriteHashMap<String, MappedFieldType> fullNameToFieldType;
    private final CopyOnWriteHashMap<String, String> aliasToConcreteName;

    FieldTypeLookup() {
        fullNameToFieldType = new CopyOnWriteHashMap<>();
        aliasToConcreteName = new CopyOnWriteHashMap<>();
    }

    private FieldTypeLookup(CopyOnWriteHashMap<String, MappedFieldType> fullNameToFieldType,
                            CopyOnWriteHashMap<String, String> aliasToConcreteName) {
        this.fullNameToFieldType = fullNameToFieldType;
        this.aliasToConcreteName = aliasToConcreteName;
    }

    /**
     * 返回一个新实例，该实例包含此实例的联合以及提供的映射器中的字段类型。
     * 如果某个字段已存在，则会更新其字段类型以使用给定字段映射器中的新类型。
     * 同样，如果别名已存在，则会更新它以引用新映射器中的字段类型。
     */
    public FieldTypeLookup copyAndAddAll(String type,
                                         Collection<FieldMapper> fieldMappers,
                                         Collection<FieldAliasMapper> fieldAliasMappers) {
        Objects.requireNonNull(type, "type must not be null");
        if (MapperService.DEFAULT_MAPPING.equals(type)) {
            throw new IllegalArgumentException("Default mappings should not be added to the lookup");
        }

        CopyOnWriteHashMap<String, MappedFieldType> fullName = this.fullNameToFieldType;
        CopyOnWriteHashMap<String, String> aliases = this.aliasToConcreteName;

        for (FieldMapper fieldMapper : fieldMappers) {
            MappedFieldType fieldType = fieldMapper.fieldType();
            MappedFieldType fullNameFieldType = fullName.get(fieldType.name());

            if (!Objects.equals(fieldType, fullNameFieldType)) {
                validateField(fullNameFieldType, fieldType, aliases);
                fullName = fullName.copyAndPut(fieldType.name(), fieldType);
            }
        }

        for (FieldAliasMapper fieldAliasMapper : fieldAliasMappers) {
            String aliasName = fieldAliasMapper.name();
            String path = fieldAliasMapper.path();

            validateAlias(aliasName, path, aliases, fullName);
            aliases = aliases.copyAndPut(aliasName, path);
        }

        return new FieldTypeLookup(fullName, aliases);
    }

    /**
     * 检查新字段类型是否有效。
     */
    private void validateField(MappedFieldType existingFieldType,
                               MappedFieldType newFieldType,
                               CopyOnWriteHashMap<String, String> aliasToConcreteName) {
        String fieldName = newFieldType.name();
        if (aliasToConcreteName.containsKey(fieldName)) {
            throw new IllegalArgumentException("The name for field [" + fieldName + "] has already" +
                " been used to define a field alias.");
        }

        if (existingFieldType != null) {
            List<String> conflicts = new ArrayList<>();
            existingFieldType.checkCompatibility(newFieldType, conflicts);
            if (conflicts.isEmpty() == false) {
                throw new IllegalArgumentException("Mapper for [" + fieldName +
                    "] conflicts with existing mapping:\n" + conflicts.toString());
            }
        }
    }

    /**
     * Checks that the new field alias is valid.
     *
     * Note that this method assumes that new concrete fields have already been processed, so that it
     * can verify that an alias refers to an existing concrete field.
     */
    private void validateAlias(String aliasName,
                               String path,
                               CopyOnWriteHashMap<String, String> aliasToConcreteName,
                               CopyOnWriteHashMap<String, MappedFieldType> fullNameToFieldType) {
        if (fullNameToFieldType.containsKey(aliasName)) {
            throw new IllegalArgumentException("The name for field alias [" + aliasName + "] has already" +
                " been used to define a concrete field.");
        }

        if (path.equals(aliasName)) {
            throw new IllegalArgumentException("Invalid [path] value [" + path + "] for field alias [" +
                aliasName + "]: an alias cannot refer to itself.");
        }

        if (aliasToConcreteName.containsKey(path)) {
            throw new IllegalArgumentException("Invalid [path] value [" + path + "] for field alias [" +
                aliasName + "]: an alias cannot refer to another alias.");
        }

        if (!fullNameToFieldType.containsKey(path)) {
            throw new IllegalArgumentException("Invalid [path] value [" + path + "] for field alias [" +
                aliasName + "]: an alias must refer to an existing field in the mappings.");
        }
    }

    /** Returns the field for the given field */
    public MappedFieldType get(String field) {
        String concreteField = aliasToConcreteName.getOrDefault(field, field);
        return fullNameToFieldType.get(concreteField);
    }

    /**
     * 返回简单匹配正则表达式的全名列表，如针对全名和索引名称的模式。
     */
    public Collection<String> simpleMatchToFullName(String pattern) {
        Set<String> fields = new HashSet<>();
        for (MappedFieldType fieldType : this) {
            if (Regex.simpleMatch(pattern, fieldType.name())) {
                fields.add(fieldType.name());
            }
        }
        for (String aliasName : aliasToConcreteName.keySet()) {
            if (Regex.simpleMatch(pattern, aliasName)) {
                fields.add(aliasName);
            }
        }
        return fields;
    }

    @Override
    public Iterator<MappedFieldType> iterator() {
        return fullNameToFieldType.values().iterator();
    }
}

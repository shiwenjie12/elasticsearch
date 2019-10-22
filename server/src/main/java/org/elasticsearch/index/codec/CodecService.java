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

package org.elasticsearch.index.codec;

import org.apache.logging.log4j.Logger;
import org.apache.lucene.codecs.Codec;
import org.apache.lucene.codecs.lucene50.Lucene50StoredFieldsFormat.Mode;
import org.apache.lucene.codecs.lucene80.Lucene80Codec;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.collect.MapBuilder;
import org.elasticsearch.index.mapper.MapperService;

import java.util.Map;

/**
 * 由于Lucene 4.0低级索引段是通过编解码器层读取和写入的，该编解码器层允许使用特定于用例的文件格式＆amp;
 * 每个字段的数据结构。 Elasticsearch通过此{@link CodecService}公开了完整的{@link Codec}功能。
 */
public class CodecService {

    private final Map<String, Codec> codecs;

    public static final String DEFAULT_CODEC = "default";
    public static final String BEST_COMPRESSION_CODEC = "best_compression";
    /** the raw unfiltered lucene default. useful for testing */
    public static final String LUCENE_DEFAULT_CODEC = "lucene_default";

    public CodecService(@Nullable MapperService mapperService, Logger logger) {
        final MapBuilder<String, Codec> codecs = MapBuilder.<String, Codec>newMapBuilder();
        if (mapperService == null) {
            codecs.put(DEFAULT_CODEC, new Lucene80Codec());
            codecs.put(BEST_COMPRESSION_CODEC, new Lucene80Codec(Mode.BEST_COMPRESSION));
        } else {
            codecs.put(DEFAULT_CODEC,
                    new PerFieldMappingPostingFormatCodec(Mode.BEST_SPEED, mapperService, logger));
            codecs.put(BEST_COMPRESSION_CODEC,
                    new PerFieldMappingPostingFormatCodec(Mode.BEST_COMPRESSION, mapperService, logger));
        }
        codecs.put(LUCENE_DEFAULT_CODEC, Codec.getDefault());
        for (String codec : Codec.availableCodecs()) {
            codecs.put(codec, Codec.forName(codec));
        }
        this.codecs = codecs.immutableMap();
    }

    public Codec codec(String name) {
        Codec codec = codecs.get(name);
        if (codec == null) {
            throw new IllegalArgumentException("failed to find codec [" + name + "]");
        }
        return codec;
    }

    /**
     * Returns all registered available codec names
     */
    public String[] availableCodecs() {
        return codecs.keySet().toArray(new String[0]);
    }
}

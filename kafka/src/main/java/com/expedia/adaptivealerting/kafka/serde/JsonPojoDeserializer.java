/*
 * Copyright 2018-2019 Expedia Group, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.expedia.adaptivealerting.kafka.serde;

import com.expedia.metrics.jackson.MetricsJavaModule;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;

import java.io.IOException;
import java.util.Map;

public final class JsonPojoDeserializer<T> implements Deserializer<T> {
    
    /**
     * JSON POJO class configuration key.
     */
    public static final String CK_JSON_POJO_CLASS = "JsonPojoClass";
    
    private ObjectMapper objectMapper;
    private Class<T> tClass;
    
    /**
     * Default constructor needed by Kafka
     */
    public JsonPojoDeserializer() {
        this.objectMapper = new ObjectMapper()
                .registerModule(new MetricsJavaModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public void configure(Map<String, ?> props, boolean isKey) {
        Object propClass = props.get(CK_JSON_POJO_CLASS);
        if (propClass instanceof Class) {
            tClass = (Class<T>) propClass;
        } else {
            try {
                tClass = (Class<T>) Class.forName(propClass.toString());
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }
    
    @Override
    public T deserialize(String topic, byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        try {
            return objectMapper.readValue(bytes, tClass);
        } catch (IOException e) {
            // TODO Is it correct to throw a SerializationException while deserializing? [WLW]
            //
            // Update:
            // It's not: https://kafka.apache.org/11/javadoc/org/apache/kafka/common/errors/SerializationException.html
            //
            // But Spring Kafka does the same thing:
            // https://github.com/spring-projects/spring-kafka/blob/master/spring-kafka/src/main/java/org/springframework/kafka/support/serializer/JsonDeserializer.java
            throw new SerializationException(e);
        }
    }
    
    @Override
    public void close() {
    }
}

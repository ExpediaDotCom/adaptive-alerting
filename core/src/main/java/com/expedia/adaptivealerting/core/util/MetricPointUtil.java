/*
 * Copyright 2018 Expedia Group, Inc.
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
package com.expedia.adaptivealerting.core.util;

import com.expedia.www.haystack.commons.entities.MetricPoint;
import com.expedia.www.haystack.commons.entities.MetricType;
import scala.Enumeration;
import scala.collection.immutable.Map;
import scala.collection.immutable.Map$;

import java.time.Instant;

/**
 * {@link MetricPoint} utilities.
 *
 * @author Willie Wheeler
 */
public final class MetricPointUtil {
    private static final Enumeration.Value DEFAULT_TYPE = MetricType.Gauge();
    private static final Map<String, String> DEFAULT_TAGS = Map$.MODULE$.<String, String>empty();
    
    /**
     * Prevent instantiation.
     */
    private MetricPointUtil() {
    }
    
    /**
     * {@link MetricPoint} factory method. Metric points have name "data", type gauge and no tags.
     *
     * @param instant Metric instant.
     * @param value   Metric value.
     * @return Metric point.
     */
    public static MetricPoint metricPoint(Instant instant, double value) {
        return metricPoint("data", instant, value);
    }
    
    /**
     * {@link MetricPoint} factory method. Metric points have type gauge and no tags.
     *
     * @param name    Metric name.
     * @param instant Metric instant.
     * @param value   Metric value.
     * @return Metric point.
     */
    public static MetricPoint metricPoint(String name, Instant instant, double value) {
        long epochSecond = instant.getEpochSecond();
        return new MetricPoint(name, DEFAULT_TYPE, DEFAULT_TAGS, (float) value, epochSecond);
    }
}

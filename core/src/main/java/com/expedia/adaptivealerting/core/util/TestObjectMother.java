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
package com.expedia.adaptivealerting.core.util;

import com.expedia.metrics.MetricData;
import com.expedia.metrics.MetricDefinition;

import java.time.Instant;

/**
 * <p>
 * Test object mother: http://wiki.c2.com/?ObjectMother
 * </p>
 * <p>
 * Ideally this would be part of the test source directory, but Maven doesn't allow us to declare test-only
 * dependencies. So we're putting this in the main source directory, considering that to be the lesser of two evils as
 * compared to duplicating this class in each module.
 * </p>
 */
public final class TestObjectMother {

    public static MetricDefinition metricDefinition() {
        return new MetricDefinition("my-metric");
    }

    public static MetricData metricData() {
        return new MetricData(metricDefinition(), 100.0, Instant.now().getEpochSecond());
    }
}

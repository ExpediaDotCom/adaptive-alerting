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

import com.expedia.adaptivealerting.kafka.util.TestObjectMother;
import com.expedia.metrics.MetricData;
import lombok.val;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class MetricDataMessagePackSerdeTest {
    private MetricDataMessagePackSerde serdeUnderTest;
    private MetricData metricData;

    @Before
    public void setUp() {
        this.serdeUnderTest = new MetricDataMessagePackSerde();
        this.metricData = TestObjectMother.metricData();
    }

    @Test
    public void coverageOnly() {
        serdeUnderTest.configure(null, false);
        serdeUnderTest.close();

        val serializer = new MetricDataMessagePackSerde.Ser();
        serializer.configure(null, false);
        serializer.close();

        val deserializer = new MetricDataMessagePackSerde.Deser();
        deserializer.configure(null, false);
        deserializer.close();
    }

    @Test
    public void testSerializeAndDeserialize() {
        val serResult = serdeUnderTest.serializer().serialize("some-topic", metricData);
        val deserResult = serdeUnderTest.deserializer().deserialize("some-topic", serResult);

        val expected = metricData.getMetricDefinition();
        val actual = deserResult.getMetricDefinition();

        // Comparing key and tags instead of comparing MetricDefinition directly because the deserializer uses a
        // MetricTank-specific MetricDefinition implementation. [WLW]
        assertEquals(expected.getKey(), actual.getKey());
        assertEquals(expected.getTags(), actual.getTags());
    }
}

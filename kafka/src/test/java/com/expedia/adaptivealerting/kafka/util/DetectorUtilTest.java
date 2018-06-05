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
package com.expedia.adaptivealerting.kafka.util;

import com.expedia.adaptivealerting.core.detector.ConstantThresholdOutlierDetector;
import com.expedia.adaptivealerting.core.detector.OutlierDetector;
import com.expedia.adaptivealerting.core.detector.OutlierResult;
import com.expedia.www.haystack.commons.entities.MetricPoint;
import com.expedia.www.haystack.commons.entities.MetricType;
import org.junit.Test;
import scala.collection.immutable.Map$;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static com.expedia.adaptivealerting.core.detector.ConstantThresholdOutlierDetector.RIGHT_TAILED;
import static com.expedia.adaptivealerting.core.detector.OutlierLevel.*;
import static org.junit.Assert.assertEquals;

public class DetectorUtilTest {
    private static final Function<String, OutlierDetector> DETECTOR_FACTORY =
            id -> new ConstantThresholdOutlierDetector(RIGHT_TAILED, 0.99f, 0.95f);

    @Test
    public void extractMetricIdForNullMetric() {
        assertEquals("haystack.null", DetectorUtil.extractMetricId(DetectorUtil.NULL_METRIC_POINT));
    }

    @Test
    public void classifyForNullMetric() {
        OutlierResult result = DetectorUtil.classify(
                DetectorUtil.NULL_METRIC_POINT,
                new HashMap<>(),
                DETECTOR_FACTORY
        );
        assertEquals(NORMAL, result.getOutlierLevel());
    }

    @Test
    public void classifyForWeakMetric() {
        OutlierResult result = DetectorUtil.classify(
                new MetricPoint("test", MetricType.Gauge(), Map$.MODULE$.empty(), 0.95f, 0),
                new HashMap<>(),
                DETECTOR_FACTORY
        );
        assertEquals(WEAK, result.getOutlierLevel());
    }

    @Test
    public void classifyForStrongMetric() {
        OutlierResult result = DetectorUtil.classify(
                new MetricPoint("test", MetricType.Gauge(), Map$.MODULE$.empty(), 0.99f, 0),
                new HashMap<>(),
                DETECTOR_FACTORY
        );
        assertEquals(STRONG, result.getOutlierLevel());
    }

    @Test
    public void classifyCreatesNewDetectorIfNotPresent() {
        Map<String, OutlierDetector> detectors = new HashMap<>();
        assertEquals(0, detectors.size());
        DetectorUtil.classify(
                DetectorUtil.NULL_METRIC_POINT,
                detectors,
                DETECTOR_FACTORY
        );
        assertEquals(1, detectors.size());
    }

    @Test
    public void evaluateMetricUsesDetectorIfPresent() {
        Map<String, OutlierDetector> detectors = Collections.singletonMap(
                DetectorUtil.extractMetricId(DetectorUtil.NULL_METRIC_POINT),
                DETECTOR_FACTORY.apply("")
        );
        DetectorUtil.classify(
                DetectorUtil.NULL_METRIC_POINT,
                detectors,
                null // Cannot create new detectors without factory.
        );
        assertEquals(1, detectors.size());
    }

    @Test
    public void evaluateMetricReusesDetectors() {
        Map<String, OutlierDetector> detectors = new HashMap<>();
        assertEquals(0, detectors.size());

        DetectorUtil.classify(
                DetectorUtil.NULL_METRIC_POINT,
                detectors,
                DETECTOR_FACTORY
        );

        Collection<OutlierDetector> firstDetectors = detectors.values();
        assertEquals(1, detectors.size());

        DetectorUtil.classify(
                DetectorUtil.NULL_METRIC_POINT,
                detectors,
                DETECTOR_FACTORY
        );
        Collection<OutlierDetector> secondDetectors = detectors.values();

        assertEquals(1, firstDetectors.size());
        assertEquals(1, secondDetectors.size());
        assertEquals(firstDetectors.toArray()[0], secondDetectors.toArray()[0]);
    }
}

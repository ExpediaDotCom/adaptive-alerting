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
package com.expedia.adaptivealerting.anomdetect.control;

import com.expedia.adaptivealerting.core.data.MappedMetricData;
import com.expedia.adaptivealerting.core.util.MathUtil;
import com.expedia.metrics.MetricData;
import com.expedia.metrics.MetricDefinition;
import com.opencsv.bean.CsvToBeanBuilder;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Instant;
import java.util.List;
import java.util.ListIterator;
import java.util.UUID;

import static com.expedia.adaptivealerting.anomdetect.NSigmasClassifier.DEFAULT_STRONG_SIGMAS;
import static com.expedia.adaptivealerting.anomdetect.NSigmasClassifier.DEFAULT_WEAK_SIGMAS;
import static junit.framework.TestCase.assertEquals;

/**
 * @author Willie Wheeler
 */
public class EwmaAnomalyDetectorTest {
    private static final double WEAK_SIGMAS = DEFAULT_WEAK_SIGMAS;
    private static final double STRONG_SIGMAS = DEFAULT_STRONG_SIGMAS;
    private static final double TOLERANCE = 0.001;
    
    private MetricDefinition metricDefinition;
    private long epochSecond;
    private UUID uuid;
    private static List<EwmaTestRow> data;
    
    @BeforeClass
    public static void setUpClass() throws IOException {
        readData_calInflow();
    }
    
    @Before
    public void setUp() {
        this.metricDefinition = new MetricDefinition("some-key");
        this.epochSecond = Instant.now().getEpochSecond();
        this.uuid = UUID.randomUUID();
    }
    
    @Test
    public void testDefaultConstructor() {
        final EwmaAnomalyDetector detector = new EwmaAnomalyDetector();
        assertEquals(0.15, detector.getAlpha());
        assertEquals(WEAK_SIGMAS, detector.getWeakThresholdSigmas());
        assertEquals(STRONG_SIGMAS, detector.getStrongThresholdSigmas());
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_alphaOutOfRange() {
        new EwmaAnomalyDetector(2.0, 100.0, 150.0, 0.0);
    }
    
    @Test
    public void testEvaluate() {
        
        // Params
        final double alpha = 0.05;
        
        final ListIterator<EwmaTestRow> testRows = data.listIterator();
        final EwmaTestRow testRow0 = testRows.next();
        final double observed0 = testRow0.getObserved();
        final EwmaAnomalyDetector detector = new EwmaAnomalyDetector(alpha, WEAK_SIGMAS, STRONG_SIGMAS, observed0);
    
        // Params
        assertEquals(alpha, detector.getAlpha());
        assertEquals(WEAK_SIGMAS, detector.getWeakThresholdSigmas());
        assertEquals(STRONG_SIGMAS, detector.getStrongThresholdSigmas());
        
        // Seed observation
        assertEquals(observed0, detector.getMean());
        assertEquals(0.0, detector.getVariance());
        
        while (testRows.hasNext()) {
            final EwmaTestRow testRow = testRows.next();
            final int observed = testRow.getObserved();
            
            final MappedMetricData mappedMetricData = toMappedMetricData(epochSecond, observed);
            detector.classify(mappedMetricData);
            
            assertApproxEqual(testRow.getKnownMean(), testRow.getMean());
            assertApproxEqual(testRow.getMean(), detector.getMean());
            assertApproxEqual(testRow.getVar(), detector.getVariance());
            // TODO Assert AnomalyLevel
        }
    }
    
    private static void readData_calInflow() {
        final InputStream is = ClassLoader.getSystemResourceAsStream("tests/cal-inflow-tests-ewma.csv");
        data = new CsvToBeanBuilder<EwmaTestRow>(new InputStreamReader(is))
                .withType(EwmaTestRow.class)
                .build()
                .parse();

    }
    
    private static void assertApproxEqual(double d1, double d2) {
        TestCase.assertTrue(MathUtil.isApproximatelyEqual(d1, d2, TOLERANCE));
    }
    
    private MappedMetricData toMappedMetricData(long epochSecond, double value) {
        final MetricData metricData = new MetricData(metricDefinition, value, epochSecond);
        return new MappedMetricData(metricData, uuid, "ewma-detector");
    }
}

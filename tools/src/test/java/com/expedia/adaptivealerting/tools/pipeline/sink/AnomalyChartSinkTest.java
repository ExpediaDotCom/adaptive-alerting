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
package com.expedia.adaptivealerting.tools.pipeline.sink;

import com.expedia.adaptivealerting.anomdetect.detect.MappedMetricData;
import com.expedia.adaptivealerting.anomdetect.detect.AnomalyLevel;
import com.expedia.adaptivealerting.anomdetect.detect.OutlierDetectorResult;
import com.expedia.adaptivealerting.anomdetect.detect.AnomalyThresholds;
import com.expedia.adaptivealerting.anomdetect.forecast.eval.PointForecastEvaluation;
import com.expedia.adaptivealerting.tools.util.TestObjectMother;
import com.expedia.adaptivealerting.tools.visualization.ChartSeries;
import com.expedia.metrics.MetricData;
import lombok.val;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.title.TextTitle;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.UUID;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

public final class AnomalyChartSinkTest {
    private AnomalyChartSink sinkUnderTest;

    @Mock
    private JFreeChart chart;

    @Mock
    private TextTitle textTitle;

    private ChartSeries chartSeries;
    private MappedMetricData strongAnomalyNoThresholds;
    private MappedMetricData weakAnomalyWithThresholds;
    private PointForecastEvaluation pointForecastEvaluation;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        initTestObjects();
        initDependencies();
        this.sinkUnderTest = new AnomalyChartSink(chart, chartSeries);
    }

    @Test
    public void coverageOnly() {
        assertNotNull(sinkUnderTest.getChart());
    }

    @Test
    public void testNext_strongAnomalyNoThresholds() {
        sinkUnderTest.next(strongAnomalyNoThresholds);
    }

    @Test
    public void testNext_weakAnomalyWithThresholds() {
        sinkUnderTest.next(weakAnomalyWithThresholds);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNext_nullAnomaly() {
        sinkUnderTest.next((MappedMetricData) null);
    }

    @Test
    public void testNext_modelEvaluation() {
        sinkUnderTest.next(pointForecastEvaluation);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNext_nullModelEvaluation() {
        sinkUnderTest.next((PointForecastEvaluation) null);
    }

    private void initTestObjects() {
        this.chartSeries = new ChartSeries();

        val metricDef = TestObjectMother.metricDefinition();
        val metricData = new MetricData(metricDef, 15.0, Instant.now().getEpochSecond());

        val anomalyResult_strong_noThresholds = new OutlierDetectorResult();
        anomalyResult_strong_noThresholds.setAnomalyLevel(AnomalyLevel.STRONG);

        val anomalyResult_weak_thresholds = new OutlierDetectorResult();
        anomalyResult_weak_thresholds.setAnomalyLevel(AnomalyLevel.WEAK);
        val thresholds = new AnomalyThresholds(100.0, 90.0, 20.0, 10.0);
        anomalyResult_weak_thresholds.setThresholds(thresholds);

        this.strongAnomalyNoThresholds = new MappedMetricData(metricData, UUID.randomUUID());
        this.strongAnomalyNoThresholds.setAnomalyResult(anomalyResult_strong_noThresholds);

        this.weakAnomalyWithThresholds = new MappedMetricData(metricData, UUID.randomUUID());
        this.weakAnomalyWithThresholds.setAnomalyResult(anomalyResult_weak_thresholds);

        this.pointForecastEvaluation = new PointForecastEvaluation("some-method", 0.0);
    }

    private void initDependencies() {
        when(textTitle.getText()).thenReturn("My Chart");
        when(chart.getTitle()).thenReturn(textTitle);
    }
}

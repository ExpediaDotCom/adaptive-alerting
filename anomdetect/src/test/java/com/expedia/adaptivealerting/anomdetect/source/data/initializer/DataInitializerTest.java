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
package com.expedia.adaptivealerting.anomdetect.source.data.initializer;

import com.expedia.adaptivealerting.anomdetect.detect.AnomalyType;
import com.expedia.adaptivealerting.anomdetect.detect.Detector;
import com.expedia.adaptivealerting.anomdetect.detect.MappedMetricData;
import com.expedia.adaptivealerting.anomdetect.detect.outlier.algo.forecasting.ForecastingDetector;
import com.expedia.adaptivealerting.anomdetect.forecast.interval.algo.multiplicative.MultiplicativeIntervalForecaster;
import com.expedia.adaptivealerting.anomdetect.forecast.interval.algo.multiplicative.MultiplicativeIntervalForecasterParams;
import com.expedia.adaptivealerting.anomdetect.forecast.point.algo.seasonalnaive.SeasonalNaivePointForecaster;
import com.expedia.adaptivealerting.anomdetect.forecast.point.algo.seasonalnaive.SeasonalNaivePointForecasterParams;
import com.expedia.adaptivealerting.anomdetect.source.data.DataSource;
import com.expedia.adaptivealerting.anomdetect.source.data.DataSourceResult;
import com.expedia.adaptivealerting.anomdetect.source.data.initializer.throttlegate.ThrottleGate;
import com.expedia.metrics.MetricData;
import com.expedia.metrics.MetricDefinition;
import com.typesafe.config.Config;
import lombok.val;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DataInitializerTest {
    @InjectMocks
    private DataInitializer initializerUnderTest;
    @Mock
    private DataSource dataSource;
    @Mock
    private Config config;
    @Mock
    private ThrottleGate throttleGate;

    private Detector detector;
    private MappedMetricData mappedMetricData;
    private List<DataSourceResult> dataSourceResults;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        initConfig();
        this.initializerUnderTest = spy(new DataInitializer(config, throttleGate, dataSource));
        initTestObjects();
        initDependencies();
    }

    @Test
    public void testInitializeDetectorThrottleGateOpen() {
        initThrottleGate(true);
        initializerUnderTest.initializeDetector(mappedMetricData, detector);
        verify(initializerUnderTest, atMost(1)).initializeDetector(any(MappedMetricData.class), any(Detector.class));
    }

    @Test(expected = DetectorDataInitializationThrottledException.class)
    public void testInitializeDetectorThrottleGateClosed() {
        initThrottleGate(false);
        initializerUnderTest.initializeDetector(mappedMetricData, detector);
        verify(initializerUnderTest, atMost(0)).initializeDetector(any(MappedMetricData.class), any(Detector.class));
    }

    private void initConfig() {
        when(config.getString(DataInitializer.BASE_URI)).thenReturn("http://graphite");
        when(config.getString(DataInitializer.EARLIEST_TIME)).thenReturn("7d");
        when(config.getInt(DataInitializer.MAX_DATA_POINTS)).thenReturn(2016);
    }

    private void initThrottleGate(boolean gateOpen) {
        when(config.getString(DataInitializer.THROTTLE_GATE_LIKELIHOOD)).thenReturn("0.4");
        when(throttleGate.isOpen()).thenReturn(gateOpen);
    }

    public void initTestObjects() {
        this.mappedMetricData = buildMappedMetricData();
        this.detector = buildDetector();
        this.dataSourceResults = new ArrayList<>();
        dataSourceResults.add(buildDataSourceResult(1.0, 1578307488));
        dataSourceResults.add(buildDataSourceResult(3.0, 1578307489));
    }

    private MappedMetricData buildMappedMetricData() {
        val mappedUuid = UUID.randomUUID();
        val metricDefinition = new MetricDefinition("metric-definition");
        val metricData = new MetricData(metricDefinition, 100.0, Instant.now().getEpochSecond());
        return new MappedMetricData(metricData, mappedUuid);
    }

    private Detector buildDetector() {
        val seasonalNaivePointForecaster = new SeasonalNaivePointForecaster(new SeasonalNaivePointForecasterParams().setCycleLength(22).setIntervalLength(11));
        val multiplicativeIntervalForecaster = new MultiplicativeIntervalForecaster(new MultiplicativeIntervalForecasterParams().setStrongMultiplier(3.0).setWeakMultiplier(1.0));
        return new ForecastingDetector(UUID.randomUUID(), seasonalNaivePointForecaster, multiplicativeIntervalForecaster, AnomalyType.TWO_TAILED, true, "seasonalnaive");
    }

    private void initDependencies() {
        when(dataSource.getMetricData(anyString(), anyInt(), anyString())).thenReturn(dataSourceResults);
    }

    private DataSourceResult buildDataSourceResult(Double value, long epochSecs) {
        val dataSourceResult = new DataSourceResult();
        dataSourceResult.setDataPoint(value);
        dataSourceResult.setEpochSecond(epochSecs);
        return dataSourceResult;
    }
}
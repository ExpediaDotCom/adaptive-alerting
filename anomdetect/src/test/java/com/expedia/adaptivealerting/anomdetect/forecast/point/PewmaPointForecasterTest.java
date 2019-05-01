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
package com.expedia.adaptivealerting.anomdetect.forecast.point;

import com.expedia.adaptivealerting.anomdetect.comp.legacy.EwmaParams;
import com.expedia.adaptivealerting.anomdetect.comp.legacy.PewmaParams;
import com.expedia.adaptivealerting.anomdetect.util.TestObjectMother;
import com.expedia.metrics.MetricData;
import com.expedia.metrics.MetricDefinition;
import com.opencsv.CSVReader;
import com.opencsv.bean.CsvToBeanBuilder;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Instant;
import java.util.List;

import static org.junit.Assert.assertEquals;

@Slf4j
public final class PewmaPointForecasterTest {
    private static final double DEFAULT_ALPHA = 0.05;
    private static final double TOLERANCE = 0.00001;

    private static final String SAMPLE_INPUT_PATH = "tests/pewma-sample-input.csv";
    private static final String CAL_INFLOW_PATH = "tests/cal-inflow-tests-pewma.csv";

    private MetricDefinition metricDef;
    private long epochSecond;

    @Before
    public void setUp() {
        this.metricDef = TestObjectMother.metricDefinition();
        this.epochSecond = Instant.now().getEpochSecond();
    }

    @Test
    public void testPewmaCloseToEwmaWithZeroBeta() throws IOException {
        val beta = 0.0;

        val testRows = readData_sampleInput().listIterator();
        val observed0 = Double.parseDouble(testRows.next()[0]);

        val ewmaParams = new EwmaParams()
                .setAlpha(DEFAULT_ALPHA)
                .setInitMeanEstimate(observed0);
        val ewmaPointForecaster = new EwmaPointForecaster(ewmaParams.toPointForecasterParams());

        val pewmaParams = new PewmaParams()
                .setAlpha(DEFAULT_ALPHA)
                .setBeta(beta)
                .setInitMeanEstimate(observed0);
        val pewmaPointForecaster = new PewmaPointForecaster(pewmaParams.toPointForecasterParams());

        int rowCount = 1;
        while (testRows.hasNext()) {
            val observed = Double.parseDouble(testRows.next()[0]);
            val metricData = new MetricData(metricDef, observed, rowCount);
            ewmaPointForecaster.forecast(metricData);
            pewmaPointForecaster.forecast(metricData);
            val threshold = 1.0 / rowCount; // results converge with more iterations
            assertEquals(ewmaPointForecaster.getMean(), pewmaPointForecaster.getMean(), threshold);
            rowCount++;
        }
    }

    @Test
    public void testForecast() {
        val testRows = readData_calInflow().listIterator();
        val observed0 = testRows.next().getObserved();

        val params = new PewmaPointForecaster.Params()
                .setAlpha(DEFAULT_ALPHA)
                .setBeta(0.5)
                .setInitMeanEstimate(observed0);
        val forecaster = new PewmaPointForecaster(params);

        assertEquals(params, forecaster.getParams());

        while (testRows.hasNext()) {
            val testRow = testRows.next();
            val observed = testRow.getObserved();
            val metricData = new MetricData(metricDef, observed, epochSecond);
            forecaster.forecast(metricData);

            assertEquals(testRow.getMean(), forecaster.getMean(), TOLERANCE);
            assertEquals(testRow.getStd(), forecaster.getStdDev(), TOLERANCE);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testForecast_nullMetricData() {
        val forecaster = new PewmaPointForecaster();
        forecaster.forecast(null);
    }

    @Test
    public void testValidate_valid() {
        new PewmaPointForecaster.Params()
                .setAlpha(0.15)
                .setBeta(0.35)
                .setWarmUpPeriod(25)
                .validate();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidate_invalidAlpha() {
        new PewmaPointForecaster.Params()
                .setAlpha(1.30)
                .setBeta(0.25)
                .setWarmUpPeriod(25)
                .validate();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidate_invalidBeta() {
        new PewmaPointForecaster.Params()
                .setAlpha(0.15)
                .setBeta(1.30)
                .setWarmUpPeriod(25)
                .validate();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidate_invalidWarmUpPeriod() {
        new PewmaPointForecaster.Params()
                .setAlpha(0.15)
                .setBeta(0.25)
                .setWarmUpPeriod(-8)
                .validate();
    }

    private static List<String[]> readData_sampleInput() throws IOException {
        val is = ClassLoader.getSystemResourceAsStream(SAMPLE_INPUT_PATH);
        val reader = new CSVReader(new InputStreamReader(is));
        return reader.readAll();
    }

    private static List<PewmaTestRow> readData_calInflow() {
        val is = ClassLoader.getSystemResourceAsStream(CAL_INFLOW_PATH);
        return new CsvToBeanBuilder<PewmaTestRow>(new InputStreamReader(is))
                .withType(PewmaTestRow.class)
                .build()
                .parse();
    }
}

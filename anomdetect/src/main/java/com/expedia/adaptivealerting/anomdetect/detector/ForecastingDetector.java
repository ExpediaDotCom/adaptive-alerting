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
package com.expedia.adaptivealerting.anomdetect.detector;

import com.expedia.adaptivealerting.anomdetect.comp.AnomalyClassifier;
import com.expedia.adaptivealerting.anomdetect.forecast.interval.IntervalForecast;
import com.expedia.adaptivealerting.anomdetect.forecast.interval.IntervalForecaster;
import com.expedia.adaptivealerting.anomdetect.forecast.interval.IntervalForecasterParams;
import com.expedia.adaptivealerting.anomdetect.forecast.point.PointForecaster;
import com.expedia.adaptivealerting.anomdetect.forecast.point.PointForecasterParams;
import com.expedia.adaptivealerting.core.anomaly.AnomalyResult;
import com.expedia.adaptivealerting.core.anomaly.AnomalyThresholds;
import com.expedia.adaptivealerting.core.anomaly.AnomalyType;
import com.expedia.metrics.MetricData;
import lombok.Data;
import lombok.Generated;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.val;

import java.util.UUID;

import static com.expedia.adaptivealerting.core.util.AssertUtil.notNull;

/**
 * <p>
 * {@link Detector} implementation based on underlying forecasters. The general approach is to generate a forecast and
 * compare the observed value to the forecast. If the observed value is too far from the forecast, then the detector
 * classifies the observation as an anomaly.
 * </p>
 * <p>
 * We actually generate two types of forecast: point and interval forecasts. These are based upon underlying
 * {@link PointForecaster} and {@link IntervalForecaster} implementations. Additionally we use {@link AnomalyType} to
 * apply either a one- or two-tailed test when generating the classification.
 * </p>
 *
 * @see PointForecaster
 * @see IntervalForecaster
 */
public class ForecastingDetector extends AbstractDetector {

    @Getter
    @Generated // https://reflectoring.io/100-percent-test-coverage/
    private PointForecaster pointForecaster;

    @Getter
    @Generated // https://reflectoring.io/100-percent-test-coverage/
    private IntervalForecaster intervalForecaster;

    @Getter
    @Generated // https://reflectoring.io/100-percent-test-coverage/
    private AnomalyType anomalyType;

    public ForecastingDetector(
            UUID uuid,
            PointForecaster pointForecaster,
            IntervalForecaster intervalForecaster,
            AnomalyType anomalyType) {

        super(uuid);

        notNull(pointForecaster, "pointForecaster can't be null");
        notNull(intervalForecaster, "intervalForecaster can't be null");
        notNull(anomalyType, "anomalyType can't be null");

        this.pointForecaster = pointForecaster;
        this.intervalForecaster = intervalForecaster;
        this.anomalyType = anomalyType;
    }

    @Override
    public AnomalyResult classify(MetricData metricData) {
        notNull(metricData, "metricData can't be null");

        val pointForecast = pointForecaster.forecast(metricData);
        val intervalForecast = intervalForecaster.forecast(metricData, pointForecast.getValue());
        val thresholds = toAnomalyThresholds(intervalForecast);
        val observed = metricData.getValue();
        val level = new AnomalyClassifier(anomalyType).classify(thresholds, observed);

        return new AnomalyResult(level)
                .setPredicted(pointForecast.getValue())
                .setThresholds(thresholds);
    }

    private AnomalyThresholds toAnomalyThresholds(IntervalForecast intervalForecast) {
        return new AnomalyThresholds(
                intervalForecast.getUpperStrong(),
                intervalForecast.getUpperWeak(),
                intervalForecast.getLowerWeak(),
                intervalForecast.getLowerStrong());
    }

    /**
     * {@link ForecastingDetector} configuration object.
     */
    @Data
    @Accessors(chain = true)
    public static final class Params extends AbstractDetectorConfig {
        private PointForecasterParams pointForecasterParams;
        private IntervalForecasterParams intervalForecasterParams;
        private AnomalyType anomalyType;

        @Override
        public void validate() {
            notNull(pointForecasterParams, "pointForecasterParams can't be null");
            notNull(intervalForecasterParams, "intervalForecasterParams can't be null");
            notNull(anomalyType, "anomalyType can't be null");
        }
    }
}

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
package com.expedia.adaptivealerting.anomdetect.forecast.point.algo.seasonalnaive;

import com.expedia.adaptivealerting.anomdetect.forecast.point.PointForecast;
import com.expedia.adaptivealerting.anomdetect.forecast.point.SeasonalPointForecaster;
import com.expedia.metrics.MetricData;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;

import java.util.UUID;

import static com.expedia.adaptivealerting.anomdetect.util.AssertUtil.notNull;

/**
 * Point forecaster based on the seasonal naive method described in
 * https://otexts.com/fpp2/simple-methods.html#simple-methods.
 */
@RequiredArgsConstructor
public class SeasonalNaivePointForecaster implements SeasonalPointForecaster {

    /**
     * Detector UUID.
     */
    @NonNull
    @Getter
    private UUID uuid;

    /**
     * Configuration parameters for the {@link SeasonalNaivePointForecaster}.
     */
    @NonNull
    @Getter
    private SeasonalNaivePointForecasterParams params;

    /**
     * Data store for previous seasonal values.
     */
    @NonNull
    @Getter
    private SeasonalBuffer buffer;

    /**
     * Creates a new forecaster from the given configuration parameters.
     * @param params Configuration parameters.
     */
    public SeasonalNaivePointForecaster(SeasonalNaivePointForecasterParams params) {
        notNull(params, "params can't be null");
        params.validate();
        this.params = params;
        this.buffer = new SeasonalBuffer(params.getCycleLength(), params.getIntervalLength(), params.getMissingValuePlaceholder());
    }

    /**
     * Forecasts the datapoint value for the given datapoint using seasonal naive algorithm.
     * @param metricData The datapoint.
     * @return Forecast for the given datapoint. Returns params.getMissingValuePlaceholder() for missing values.
     */
    @Override
    public PointForecast forecast(MetricData metricData) {
        notNull(metricData, "metricData can't be null");
        val oldValue = this.buffer.updateWhilePadding(metricData);
        return getPreviousValueOrNull(oldValue);
    }

    @Override
    public int getCycleLength() {
        return params.getCycleLength();
    }

    @Override
    public int getIntervalLength() {
        return params.getIntervalLength();
    }

    private PointForecast getPreviousValueOrNull(double oldValue) {
        if (isWarmingUp()) {
            return new PointForecast(Double.NaN, true);
        } else if (bufferContainedMissingValue(oldValue)) {
            return null;
        }
        return new PointForecast(oldValue, false);
    }

    private boolean isWarmingUp() {
        return !buffer.isReady();
    }

    private boolean bufferContainedMissingValue(double oldValue) {
        return this.params.getMissingValuePlaceholder().equals(oldValue);
    }
}

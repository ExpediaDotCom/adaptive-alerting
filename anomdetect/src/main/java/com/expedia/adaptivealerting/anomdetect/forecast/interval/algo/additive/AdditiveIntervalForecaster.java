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
package com.expedia.adaptivealerting.anomdetect.forecast.interval.algo.additive;

import com.expedia.adaptivealerting.anomdetect.forecast.interval.IntervalForecast;
import com.expedia.adaptivealerting.anomdetect.forecast.interval.IntervalForecaster;
import com.expedia.metrics.MetricData;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import static com.expedia.adaptivealerting.anomdetect.util.AssertUtil.notNull;

@RequiredArgsConstructor
public class AdditiveIntervalForecaster implements IntervalForecaster {

    @Getter
    @NonNull
    private AdditiveIntervalForecasterParams params;

    @Override
    public IntervalForecast forecast(MetricData metricData, double pointForecast) {
        notNull(metricData, "metricData can't be null");

        return new IntervalForecast(
                pointForecast + params.getStrongValue(),
                pointForecast + params.getWeakValue(),
                pointForecast - params.getWeakValue(),
                pointForecast - params.getStrongValue());
    }

}

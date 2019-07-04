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
package com.expedia.adaptivealerting.tools.pipeline.filter;

import com.expedia.adaptivealerting.anomdetect.detect.MappedMetricData;
import com.expedia.adaptivealerting.anomdetect.detect.OutlierDetectorResult;
import com.expedia.adaptivealerting.anomdetect.forecast.eval.PointForecastEvaluator;
import com.expedia.adaptivealerting.anomdetect.forecast.eval.PointForecastEvaluation;
import com.expedia.adaptivealerting.tools.pipeline.util.AnomalyResultSubscriber;
import com.expedia.adaptivealerting.tools.pipeline.util.ModelEvaluationSubscriber;
import lombok.val;

import java.util.LinkedList;
import java.util.List;

import static com.expedia.adaptivealerting.anomdetect.util.AssertUtil.notNull;

/**
 * Stream filter that applies model evaluator to metrics and publishes the score.
 */
public final class EvaluatorFilter implements AnomalyResultSubscriber {
    private final PointForecastEvaluator evaluator;
    private final List<ModelEvaluationSubscriber> subscribers = new LinkedList<>();

    public EvaluatorFilter(PointForecastEvaluator evaluator) {
        notNull(evaluator, "evaluator can't be null");
        this.evaluator = evaluator;
    }

    @Override
    public void next(MappedMetricData anomaly) {
        notNull(anomaly, "anomaly can't be null");
        // getPredicted() can return null during warm up; convert null to 0
        evaluator.update(anomaly.getMetricData().getValue(), getPredicted(anomaly));
        publish(evaluator.evaluate());
    }

    public void addSubscriber(ModelEvaluationSubscriber subscriber) {
        notNull(subscriber, "subscriber can't be null");
        subscribers.add(subscriber);
    }

    public void removeSubscriber(ModelEvaluationSubscriber subscriber) {
        notNull(subscriber, "subscriber can't be null");
        subscribers.remove(subscriber);
    }

    private Double getPredicted(MappedMetricData anomaly) {
        // getPredicted() can return null during warm up; convert null to 0
        val outlierResult = (OutlierDetectorResult) anomaly.getAnomalyResult();
        return (outlierResult.getPredicted() == null ? 0.0 : outlierResult.getPredicted());
    }

    private void publish(PointForecastEvaluation pointForecastEvaluation) {
        subscribers.stream().forEach(subscriber -> subscriber.next(pointForecastEvaluation));
    }
}

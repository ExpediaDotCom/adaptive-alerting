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
import com.expedia.adaptivealerting.anomdetect.detector.config.ConstantThresholdDetectorConfig;
import com.expedia.adaptivealerting.core.anomaly.AnomalyResult;
import com.expedia.metrics.MetricData;
import lombok.Getter;
import lombok.val;

import java.util.UUID;

import static com.expedia.adaptivealerting.core.util.AssertUtil.notNull;

/**
 * Anomaly detector with constant threshold for weak and strong anomalies. Supports both one- and two-tailed tests.
 */
public final class ConstantThresholdDetector extends AbstractDetector {

    @Getter
    private final ConstantThresholdDetectorConfig config;

    private final AnomalyClassifier classifier;

    public ConstantThresholdDetector(UUID uuid, ConstantThresholdDetectorConfig config) {
        super(uuid);
        notNull(config, "config can't be null");
        config.validate();

        this.config = config;
        this.classifier = new AnomalyClassifier(config.getType());
    }

    @Override
    public AnomalyResult classify(MetricData metricData) {
        notNull(metricData, "metricData can't be null");
        val thresholds = config.getThresholds();
        val level = classifier.classify(thresholds, metricData.getValue());
        return new AnomalyResult(level).setThresholds(thresholds);
    }

}

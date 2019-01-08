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
package com.expedia.adaptivealerting.anomdetect.perf;

import com.expedia.adaptivealerting.anomdetect.AnomalyDetector;
import com.expedia.adaptivealerting.core.anomaly.AnomalyResult;
import com.expedia.metrics.MetricData;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

import static com.expedia.adaptivealerting.core.util.AssertUtil.notNull;

/**
 * Wrapper around {@link AnomalyDetector}. It feeds the performance monitor with a listener and pushes the
 * classification outputs to the perfmon every time a new {@link MetricData} comes in.
 *
 * @author kashah
 */
@Data
@RequiredArgsConstructor
@ToString
@Slf4j
public final class MonitoredDetector implements AnomalyDetector {
    
    @NonNull
    private AnomalyDetector detector;
    
    @NonNull
    private PerformanceMonitor perfMonitor;
    
    @Override
    public UUID getUuid() {
        return detector.getUuid();
    }
    
    @Override
    public AnomalyResult classify(MetricData metricData) {
        notNull(metricData, "metricData can't be null");
        final AnomalyResult result = detector.classify(metricData);
        perfMonitor.evaluatePerformance(result);
        return result;
    }
    
    public static class PerfMonHandler implements PerfMonListener {
        
        @Override
        public void processScore(double score) {
            log.info("Performance score: {}", score);
        }
    }
}

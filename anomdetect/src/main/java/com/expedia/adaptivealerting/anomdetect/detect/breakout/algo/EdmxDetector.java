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
package com.expedia.adaptivealerting.anomdetect.detect.breakout.algo;

import com.expedia.adaptivealerting.anomdetect.detect.DetectorResult;
import com.expedia.adaptivealerting.anomdetect.detect.breakout.BreakoutDetector;
import com.expedia.metrics.MetricData;
import com.google.common.collect.EvictingQueue;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.time.Instant;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.expedia.adaptivealerting.anomdetect.util.AssertUtil.notNull;

@Slf4j
public final class EdmxDetector implements BreakoutDetector {

    @Getter
    private UUID uuid;

    @Getter
    private EdmxHyperparams hyperparams;

    private final EvictingQueue<MetricData> buffer;

    public EdmxDetector(UUID uuid, EdmxHyperparams hyperparams) {
        notNull(uuid, "uuid can't be null");
        notNull(hyperparams, "hyperparams can't be null");
        hyperparams.validate();

        log.info("Creating EdmxDetector: uuid={}, hyperparams={}", uuid, hyperparams);
        this.uuid = uuid;
        this.hyperparams = hyperparams;
        this.buffer = EvictingQueue.create(hyperparams.getBufferSize());
    }

    @Override
    public DetectorResult detect(MetricData metricData) {
        notNull(metricData, "metricData can't be null");
        buffer.add(metricData);

        val warmup = buffer.remainingCapacity() > 0;
        val result = new EdmxDetectorResult().setWarmup(warmup);

        if (warmup) {
            log.info("EdmxDetector warming up: uuid={}, size={}, toGo={}, metricData={}",
                    uuid, buffer.size(), buffer.remainingCapacity(), metricData);
            return result;
        }

        val mdValues = buffer.stream().mapToDouble(md -> md.getValue()).toArray();
        val estimate = EdmxEstimator.estimate(
                mdValues,
                hyperparams.getDelta(),
                hyperparams.getNumPerms(),
                hyperparams.getAlpha());

        val mdList = buffer.stream().collect(Collectors.toList());
        val location = estimate.getLocation();

        if (location == -1) {
            return result;
        }

        val epochSeconds = mdList.get(location).getTimestamp();
        val instant = Instant.ofEpochSecond(epochSeconds);

        return result
                .setTimestamp(instant)
                .setSignificant(estimate.isSignificant())
                .setEnergyDistance(estimate.getEnergyDistance())
                .setPreBreakoutMedian(estimate.getPreBreakoutMedian())
                .setPostBreakoutMedian(estimate.getPostBreakoutMedian())
                .setPValue(estimate.getPValue())
                .setAlpha(estimate.getAlpha());
    }
}

/*
 * Copyright 2018 Expedia Group, Inc.
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
package com.expedia.adaptivealerting.anomdetect;

import com.expedia.adaptivealerting.core.data.MappedMpoint;
import com.expedia.adaptivealerting.core.data.Metric;
import com.expedia.adaptivealerting.core.data.Mpoint;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.expedia.adaptivealerting.core.util.AssertUtil.notNull;

/**
 * Entry into the Adaptive Alerting runtime. Its job is find for any incoming {@link Mpoint} its set of mapped
 * detectors, creating the corresponding {@link MappedMpoint}s.
 *
 * @author David Sutherland
 * @author Willie Wheeler
 */
public final class AnomalyDetectorMapper {
    
    /**
     * Maps an {@link Mpoint} to its corresponding set of {@link MappedMpoint}s.
     *
     * @param mpoint Mpoint to map.
     * @return The corresponding set of {@link MappedMpoint}s: one per detector.
     */
    public Set<MappedMpoint> map(Mpoint mpoint) {
        notNull(mpoint, "mpoint can't be null");
        return createMappedMpoints(mpoint, findDetectors(mpoint.getMetric()));
    }
    
    private Set<DetectorMeta> findDetectors(Metric metric) {
        
        // TODO Resolve mappings using the model service instead of the following hardcoded logic. For now, only
        // bookings metrics get through. We can generalize/fix this when we switch over to using Mpoints generally.
        final Map<String, String> tags = metric.getTags();
        final Set<DetectorMeta> results = new HashSet<>();
        if ("bookings".equals(tags.get("what"))) {
            results.add(new DetectorMeta(UUID.fromString("636e13ed-6882-48cc-be75-56986a3b0179"), "aquila-detector"));
            results.add(new DetectorMeta(UUID.fromString("fac1a330-e5ad-4902-b17a-3d6068596a95"), "rcf-detector"));
        }
        return results;
    }
    
    private Set<MappedMpoint> createMappedMpoints(Mpoint mpoint, Set<DetectorMeta> detectors) {
        return detectors.stream()
                .map(detector -> new MappedMpoint(mpoint, detector.getUuid(), detector.getType()))
                .collect(Collectors.toSet());
    }
    
    private static class DetectorMeta {
        private UUID uuid;
        private String type;
    
        public DetectorMeta(UUID uuid, String type) {
            this.uuid = uuid;
            this.type = type;
        }
    
        public UUID getUuid() {
            return uuid;
        }
    
        public String getType() {
            return type;
        }
    }
}

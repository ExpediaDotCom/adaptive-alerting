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
package com.expedia.adaptivealerting.anomdetect.aquila;

import com.expedia.adaptivealerting.anomdetect.AnomalyDetector;
import com.expedia.adaptivealerting.core.anomaly.AnomalyLevel;
import com.expedia.adaptivealerting.core.anomaly.AnomalyResult;
import com.expedia.adaptivealerting.core.anomaly.AnomalyThresholds;
import com.expedia.metrics.MetricData;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Request;

import java.io.IOException;
import java.util.UUID;

import static com.expedia.adaptivealerting.core.util.AssertUtil.notNull;

// TODO Move this class to the Aquila repo. [WLW]

/**
 * You must register a {@link com.expedia.metrics.jackson.MetricsJavaModule} with the injected {@link ObjectMapper}.
 *
 * @author Willie Wheeler
 */
@Data
@RequiredArgsConstructor
@Slf4j
public final class AquilaAnomalyDetector implements AnomalyDetector {
    
    @NonNull
    private ObjectMapper objectMapper;
    
    @NonNull
    private String uri;
    
    @NonNull
    private UUID uuid;
    
    @Override
    public AnomalyResult classify(MetricData metricData) {
        notNull(metricData, "metricData can't be null");
        
        AnomalyLevel level;
        try {
            final AquilaRequest aquilaRequest = toAquilaRequest(metricData);
            final byte[] aquilaRequestBytes = objectMapper.writeValueAsBytes(aquilaRequest);
            final Content content = Request.Post(uri)
                    .addHeader("Content-Type", "application/json")
                    .bodyByteArray(aquilaRequestBytes)
                    .execute()
                    .returnContent();
            final AquilaResponse aquilaResponse = objectMapper.readValue(content.asBytes(), AquilaResponse.class);
            level = AnomalyLevel.valueOf(aquilaResponse.getLevel());
        } catch (IOException e) {
            log.error("Classification failed: " + e.getMessage(), e);
            level = AnomalyLevel.UNKNOWN;
        }
        
        return new AnomalyResult(uuid, metricData, level);
    }
    
    private AquilaRequest toAquilaRequest(MetricData metricData) {
        return new AquilaRequest(uuid.toString(), metricData.getTimestamp(), metricData.getValue());
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class AquilaRequest {
        private String detectorUuid;
        private Long epochSecond;
        private Double observed;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class AquilaResponse {
        private Double predicted;
        private AnomalyThresholds thresholds;
        private String level;
    }
}

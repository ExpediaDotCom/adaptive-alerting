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
import com.expedia.adaptivealerting.anomdetect.util.HttpClientWrapper;
import com.expedia.adaptivealerting.core.anomaly.AnomalyLevel;
import com.expedia.adaptivealerting.core.anomaly.AnomalyResult;
import com.expedia.metrics.MetricData;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.hateoas.hal.Jackson2HalModule;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.util.UUID;

import static com.expedia.adaptivealerting.core.util.AssertUtil.notNull;

// TODO Move this class to the Aquila repo. [WLW]

/**
 * @author Willie Wheeler
 */
@Data
@RequiredArgsConstructor
public final class AquilaAnomalyDetector implements AnomalyDetector {
    
    @NonNull
    private UUID uuid;
    
    private HttpClientWrapper httpClient = new HttpClientWrapper();
    
    // https://hdpe.me/post/spring-data-rest-hal-client/
    final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json()
            .modules(new Jackson2HalModule())
            .build();
    
    public AquilaAnomalyDetector() {
        this(UUID.randomUUID());
    }
    
    @Override
    public AnomalyResult classify(MetricData metricData) {
        notNull(metricData, "metricData can't be null");
        
        // FIXME Temporary so we can see whether we trip this detector
        return new AnomalyResult(uuid, metricData, AnomalyLevel.STRONG);
    }
}

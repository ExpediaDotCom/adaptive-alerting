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
package com.expedia.adaptivealerting.anomdetect.randomcutforest;

import com.expedia.adaptivealerting.anomdetect.AnomalyDetectorFactory;
import com.typesafe.config.Config;

import java.util.UUID;

import static com.expedia.adaptivealerting.core.util.AssertUtil.notNull;

/**
 * @author Willie Wheeler
 */
public final class RandomCutForestAnomalyDetectorFactory
        implements AnomalyDetectorFactory<RandomCutForestAnomalyDetector> {
    
    @Override
    public void init(Config appConfig) {
        notNull(appConfig, "appConfig can't be null");
    }
    
    @Override
    public RandomCutForestAnomalyDetector create(UUID uuid) {
        notNull(uuid, "uuid can't be null");
        
        // TODO Return different models for different metrics. [WLW]
        return new RandomCutForestAnomalyDetector();
    }
}

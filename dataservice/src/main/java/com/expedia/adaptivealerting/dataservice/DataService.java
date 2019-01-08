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
package com.expedia.adaptivealerting.dataservice;

import com.expedia.adaptivealerting.core.data.MetricFrame;
import com.expedia.metrics.MetricDefinition;
import com.typesafe.config.Config;

import java.time.Instant;

/**
 * Data service interface.
 *
 * @author Willie Wheeler
 */
public interface DataService {
    
    /**
     * Initializes the data service.
     *
     * @param config Data service configuration.
     */
    void init(Config config);
    
    /**
     * Gets the metric frame for a given date range.
     *
     * @param metricDefinition Metric.
     * @param startDate        Start date.
     * @param endDate          End date.
     * @return The corresponding metric frame.
     */
    MetricFrame getMetricFrame(MetricDefinition metricDefinition, Instant startDate, Instant endDate);
}

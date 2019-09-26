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
package com.expedia.adaptivealerting.modelservice.dto.metricprofiling;

import com.expedia.adaptivealerting.modelservice.dto.detectormapping.Detector;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class MatchedMetricResponse {
    private Map<Integer, List<Detector>> groupedDetectorsBySearchIndex;
    private long lookupTimeInMillis;
}

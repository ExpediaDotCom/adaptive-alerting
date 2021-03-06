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
package com.expedia.adaptivealerting.anomdetect;

import com.expedia.adaptivealerting.anomdetect.detect.DetectorContainer;
import com.expedia.adaptivealerting.anomdetect.detect.DetectorResult;
import com.expedia.metrics.MetricData;
import lombok.NonNull;

public interface DetectorExecutor {
    DetectorResult doDetection(DetectorContainer detector, @NonNull MetricData metricData);
    // I would have expected a method declaration here -- something related to the fact that this is an execution contract.
    //
    //Would it make sense to just bake pre- and post-filters into the overall processing pipeline (kind of like how servlets do)?
}

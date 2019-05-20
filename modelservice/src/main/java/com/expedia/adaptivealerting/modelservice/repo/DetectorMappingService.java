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
package com.expedia.adaptivealerting.modelservice.repo;

import com.expedia.adaptivealerting.modelservice.model.CreateDetectorMappingRequest;
import com.expedia.adaptivealerting.modelservice.model.DetectorMapping;
import com.expedia.adaptivealerting.modelservice.model.MatchingDetectorsResponse;
import com.expedia.adaptivealerting.modelservice.model.SearchMappingsRequest;

import java.util.List;
import java.util.Map;

public interface DetectorMappingService {

    MatchingDetectorsResponse findMatchingDetectorMappings(List<Map<String, String>> tagsList);

    String createDetectorMapping(CreateDetectorMappingRequest createDetectorMappingRequest);

    void deleteDetectorMapping(String id);

    DetectorMapping findDetectorMapping(String id);

    List<DetectorMapping> search(SearchMappingsRequest searchMappingsRequest);

    List<DetectorMapping> findLastUpdated(int timeInSeconds);

    void disableDetectorMapping(String id);
}

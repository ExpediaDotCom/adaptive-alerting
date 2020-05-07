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

import com.expedia.adaptivealerting.anomdetect.source.DetectorDocument;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Deprecated
public interface LegacyDetectorRepository {

    /**
     * Saves a detector document to the detector store. The detector UUID must be {@literal null}, as this method
     * assigns a UUID.
     *
     * @param document Detector document
     * @return Detector UUID assigned by this call
     */
    UUID createDetector(DetectorDocument document);

    void deleteDetector(String uuid);

    void updateDetector(String uuid, DetectorDocument document);

    void updateDetectorLastUsed(String uuid);

    DetectorDocument findByUuid(String uuid);

    List<DetectorDocument> findByCreatedBy(String user);

    void toggleDetector(String uuid, Boolean enabled);

    void trustDetector(String uuid, Boolean trusted);

    List<DetectorDocument> getLastUpdatedDetectors(long interval);
}

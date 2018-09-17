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
package com.expedia.adaptivealerting.anomdetect.cusum;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.S3Object;
import com.expedia.adaptivealerting.anomdetect.AnomalyDetectorFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.typesafe.config.Config;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import static com.expedia.adaptivealerting.core.util.AssertUtil.notNull;

/**
 * @author Willie Wheeler
 */
@Slf4j
public final class CusumFactory implements AnomalyDetectorFactory<CusumAnomalyDetector> {
    
    // TODO Move this AWS-specific code out of this factory.
    // The actual param load code will go in modelservice-s3. [WLW]
    private AmazonS3 s3;
    private String bucket;
    private String folder;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public void init(String type, Config config) {
        notNull(type, "type can't be null");
        notNull(config, "config can't be null");
        
        // TODO Reorganize the config here. We don't want every factory to have its own region and bucket. [WLW]
        final String region = config.getString("region");
        final String bucket = config.getString("bucket");
    
        notNull(region, "Property 'region' must be defined");
        notNull(bucket, "Property 'bucket' must be defined");
    
        this.s3 = AmazonS3ClientBuilder.standard()
                .withRegion(region)
                .build();
        this.bucket = bucket;
        this.folder = type;
        
        log.info("Initialized CusumFactory: region={}, bucket={}, folder={}", region, bucket, folder);
    }
    
    @Override
    public CusumAnomalyDetector create(UUID uuid) {
        notNull(uuid, "uuid can't be null");
        
        final String path = folder + "/" + uuid.toString() + ".json";
        final S3Object s3Obj = s3.getObject(bucket, path);
        final InputStream is = s3Obj.getObjectContent();
        
        CusumModel model;
        try {
            model = objectMapper.readValue(is, CusumModel.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        
        log.info("Loaded model: {}", model);
        return new CusumAnomalyDetector(uuid, model.getParams());
    }
}

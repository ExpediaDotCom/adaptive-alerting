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
package com.expedia.aquila.core.repo.file;

import com.expedia.aquila.core.repo.AbstractDetectorModelRepo;
import com.expedia.aquila.core.repo.PredictionModelRepo;
import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import static com.expedia.adaptivealerting.core.util.AssertUtil.notNull;

/**
 * @author Willie Wheeler
 */
public class FileDetectorModelRepo extends AbstractDetectorModelRepo {
    private static final Logger log = LoggerFactory.getLogger(FileDetectorModelRepo.class);
    
    private FilePredictionModelRepo predModelRepo;
    
    @Override
    public void init(Config config) {
        notNull(config, "config can't be null");
        final File baseDir = new File(config.getString("base.dir"));
        this.predModelRepo = new FilePredictionModelRepo(baseDir);
    }
    
    @Override
    protected PredictionModelRepo getPredictionModelRepo() {
        return predModelRepo;
    }
}

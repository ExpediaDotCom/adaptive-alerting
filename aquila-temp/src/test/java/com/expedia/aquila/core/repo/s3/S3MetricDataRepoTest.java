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
package com.expedia.aquila.core.repo.s3;

import com.expedia.adaptivealerting.aws.core.data.repo.S3MetricDataRepo;
import com.expedia.adaptivealerting.core.data.Metric;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Willie Wheeler
 * @author Karan Shah
 */
public final class S3MetricDataRepoTest {
    
    // Class under test
    private S3MetricDataRepo repo;
    
    // Test objects
    private Metric metric;
    
    @Before
    public void setUp() {
    }
    
    @Test
    public void testLoad() {
    }
}

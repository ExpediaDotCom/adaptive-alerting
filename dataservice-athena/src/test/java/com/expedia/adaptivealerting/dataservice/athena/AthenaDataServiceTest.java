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
package com.expedia.adaptivealerting.dataservice.athena;

import com.typesafe.config.Config;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

/**
 * @author Willie Wheeler
 */
public class AthenaDataServiceTest {
    
    // Class under test
    private AthenaDataService dataService;
    
    @Mock
    private Config config;
    
    @Before
    public void setUp() {
        this.dataService = new AthenaDataService();
        MockitoAnnotations.initMocks(this);
        initTestObjects();
    }
    
    @Test
    public void testInit() {
        dataService.init(config);
        assertNotNull(dataService.getAthena());
    }
    
    private void initTestObjects() {
        when(config.getString("region")).thenReturn("us-west-2");
        when(config.getString("database")).thenReturn("aa-datasets");
        when(config.getString("outputLocation")).thenReturn("aa-results");
        when(config.getInt("clientExecutionTimeout")).thenReturn(30000);
    }
}

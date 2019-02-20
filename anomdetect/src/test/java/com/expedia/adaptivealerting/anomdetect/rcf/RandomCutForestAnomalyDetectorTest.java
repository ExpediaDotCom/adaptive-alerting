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
package com.expedia.adaptivealerting.anomdetect.rcf;

import com.expedia.adaptivealerting.anomdetect.util.DetectorResource;
import com.expedia.adaptivealerting.anomdetect.util.ModelTypeResource;
import com.opencsv.CSVReader;
import com.opencsv.bean.CsvToBeanBuilder;
import org.junit.Before;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.UUID;

public class RandomCutForestAnomalyDetectorTest {
    private UUID detectorUUID;
    private DetectorResource detectorResource;
    
    @Before
    public void setUp() {
        this.detectorUUID = UUID.randomUUID();
        this.detectorResource = new DetectorResource(
                "a-b-c-d",
                new ModelTypeResource());
    }

    private static List<String[]> readCsv(String path) throws IOException {
        final InputStream is = ClassLoader.getSystemResourceAsStream(path);
        CSVReader reader = new CSVReader(new InputStreamReader(is));
        return reader.readAll();
    }

    private static List<RandomCutForestTestRow> readDataStream() {
        final InputStream is = ClassLoader.getSystemResourceAsStream("datasets/rcf-inflow.txt");
        return new CsvToBeanBuilder<RandomCutForestTestRow>(new InputStreamReader(is))
                .withType(RandomCutForestTestRow.class)
                .build()
                .parse();
    }
}

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
package com.expedia.adaptivealerting.anomdetect.source.data.graphite;

import com.expedia.adaptivealerting.anomdetect.source.data.DataSource;
import com.expedia.adaptivealerting.anomdetect.source.data.DataSourceResult;
import com.expedia.adaptivealerting.anomdetect.util.TimeConstantsUtil;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

import static java.time.Instant.ofEpochSecond;

@RequiredArgsConstructor
@Slf4j
public class GraphiteSource implements DataSource {

    public static final Double MISSING_VALUE = Double.NEGATIVE_INFINITY;

    /**
     * Client to load metric data from graphite.
     */
    @NonNull
    private GraphiteClient graphiteClient;

    @Override
    public List<DataSourceResult> getMetricData(long earliestTime, long latestTime, int intervalLength, String target) {
        int maxDataPoints = getMaxDataPointsPerDay(intervalLength);
        return buildDataSourceResult(earliestTime, latestTime, maxDataPoints, target);
    }

    private List<DataSourceResult> buildDataSourceResult(long earliestTime, long latestTime, int maxDataPoints, String metric) {
        List<DataSourceResult> results = new ArrayList<>();
        for (long i = earliestTime; i < latestTime; i += TimeConstantsUtil.SECONDS_PER_DAY) {
            List<GraphiteResult> graphiteResults = getDataFromGraphite(i, maxDataPoints, metric);
            if (graphiteResults.size() > 0) {
                String[][] dataPoints = graphiteResults.get(0).getDatapoints();
                //TODO Convert this to use JAVA stream
                for (String[] dataPoint : dataPoints) {
                    Double value = MISSING_VALUE;
                    if (dataPoint[0] != null) {
                        value = Double.parseDouble(dataPoint[0]);
                    }
                    long epochSeconds = Long.parseLong(dataPoint[1]);
                    DataSourceResult result = new DataSourceResult(value, epochSeconds);
                    results.add(result);
                }
            }
        }
        return results;
    }

    private List<GraphiteResult> getDataFromGraphite(long from, int maxDataPoints, String metric) {
        long until = from + TimeConstantsUtil.SECONDS_PER_DAY;
        log.debug("Fetching data from graphite for params: from={} ({}), until={} ({}), maxDataPoints={}, metric='{}'",
                from, ofEpochSecond(from), until, ofEpochSecond(until), maxDataPoints, metric);
        return graphiteClient.getData(from, until, maxDataPoints, metric);
    }

    private int getMaxDataPointsPerDay(int intervalLength) {
        return TimeConstantsUtil.SECONDS_PER_DAY / intervalLength;
    }
}
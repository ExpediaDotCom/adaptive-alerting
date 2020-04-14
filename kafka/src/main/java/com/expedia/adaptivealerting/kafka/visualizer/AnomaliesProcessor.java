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
package com.expedia.adaptivealerting.kafka.visualizer;

import com.expedia.adaptivealerting.anomdetect.detect.MappedMetricData;
import com.expedia.adaptivealerting.anomdetect.detect.outlier.OutlierDetectorResult;
import com.expedia.metrics.MetricData;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

@Slf4j
public class AnomaliesProcessor {

    public AnomaliesProcessor() {
    }

    public void processMetrics(ConsumerRecords<String, MappedMetricData> metricRecords, ExecutorService executorService) {

        List<AnomalyModel> anomalyModels = new ArrayList();
        for (ConsumerRecord<String, MappedMetricData> consumerRecord : metricRecords) {
            AnomalyModel anomalyModel = new AnomalyModel();
            MappedMetricData mappedMetricData = consumerRecord.value();
            if (mappedMetricData != null) {
                if (mappedMetricData.getDetectorUuid() != null) {
                    anomalyModel.setUuid(mappedMetricData.getDetectorUuid().toString());
                }
                MetricData metricData = mappedMetricData.getMetricData();
                if (metricData != null) {
                    anomalyModel.setTimestamp(Utility.convertToDate(metricData.getTimestamp()));
                    anomalyModel.setValue(metricData.getValue());
                    if (metricData.getMetricDefinition() != null) {
                        anomalyModel.setKey(metricData.getMetricDefinition().getKey());
                        anomalyModel.setTags(metricData.getMetricDefinition().getTags());
                    }
                }
                OutlierDetectorResult outlierDetectorResult = (OutlierDetectorResult) mappedMetricData.getAnomalyResult();
                if (outlierDetectorResult != null) {
                    anomalyModel.setLevel(outlierDetectorResult.getAnomalyLevel().toString());
                    anomalyModel.setAnomalyThresholds(outlierDetectorResult.getThresholds());
                }
            }
            anomalyModels.add(anomalyModel);
        }
        if (anomalyModels.size() > 0) {
            ElasticSearchBulkService elasticSearchBulkService = new ElasticSearchBulkService(anomalyModels);
            executorService.submit(elasticSearchBulkService);
            log.info("sending anomaly records to elasticsearch: {}", anomalyModels.size());
        }
    }
}

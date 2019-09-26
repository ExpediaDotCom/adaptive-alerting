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
package com.expedia.adaptivealerting.kafka;

import com.expedia.adaptivealerting.anomdetect.detect.AnomalyLevel;
import com.expedia.adaptivealerting.anomdetect.detect.MappedMetricData;
import com.expedia.adaptivealerting.anomdetect.detect.outlier.OutlierDetectorResult;
import com.expedia.adaptivealerting.kafka.alert.Alert;
import com.expedia.adaptivealerting.kafka.alert.AlertJsonSerde;
import lombok.Generated;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;

import java.util.HashMap;

// TODO Expand this to support breakouts? [WLW]

/**
 * @deprecated Remove alerting from AA.
 */
@Slf4j
public class KafkaAnomalyToAlertMapper extends AbstractStreamsApp {
    private static final String APP_ID = "a2a-mapper";

    private static final String VALUE = "value";
    // The metric definition key should be added as part of labels for subscription service to be create / apply subscription
    // on top of it.
    // If the tags contain `metric_key` as key, it will be overridden by metric definition key.
    private static final String METRIC_KEY = "metric_key";
    private static final String TIMESTAMP = "timestamp";
    private static final String ANOMALY_LEVEL = "anomalyLevel";

    // Cleaned code coverage
    // https://reflectoring.io/100-percent-test-coverage/
    @Generated
    public static void main(String[] args) {
        val tsConfig = new TypesafeConfigLoader(APP_ID).loadMergedConfig();
        val saConfig = new StreamsAppConfig(tsConfig);
        new KafkaAnomalyToAlertMapper(saConfig).start();
    }

    /**
     * Creates a new Kafka Streams adapter to map anomalies to alerts.
     *
     * @param config Streams app configuration.
     */
    public KafkaAnomalyToAlertMapper(StreamsAppConfig config) {
        super(config);
    }

    @Override
    protected Topology buildTopology() {
        val config = getConfig();
        val inboundTopic = config.getInputTopic();
        val outboundTopic = config.getOutputTopic();

        log.info("Initializing: inboundTopic={}, outboundTopic={}", inboundTopic, outboundTopic);

        val builder = new StreamsBuilder();
        final KStream<String, MappedMetricData> stream = builder.stream(inboundTopic);
        stream
                .filter((key, mappedMetricData) -> {
                    val outlierResult = (OutlierDetectorResult) mappedMetricData.getAnomalyResult();
                    val outlierLevel = outlierResult.getAnomalyLevel();
                    return AnomalyLevel.STRONG.equals(outlierLevel) || AnomalyLevel.WEAK.equals(outlierLevel);
                })
                .map((key, mappedMetricData) -> {
                    val metricData = mappedMetricData.getMetricData();
                    val metricDef = metricData.getMetricDefinition();
                    val tags = metricDef.getTags().getKv();
                    val outlierResult = (OutlierDetectorResult) mappedMetricData.getAnomalyResult();
                    val outlierLevel = outlierResult.getAnomalyLevel();
                    val value = metricData.getValue();
                    val timestamp = metricData.getTimestamp();

                    val labels = new HashMap<String, String>(tags);
                    labels.put(METRIC_KEY, metricDef.getKey());
                    labels.put(ANOMALY_LEVEL, outlierLevel.toString());

                    val annotations = new HashMap<String, String>();
                    annotations.put(VALUE, String.valueOf(value));
                    annotations.put(TIMESTAMP, String.valueOf(timestamp));

                    val alert = new Alert();
                    alert.setName(metricDef.getKey());
                    alert.setLabels(labels);
                    alert.setAnnotations(annotations);

                    return KeyValue.pair(mappedMetricData.getDetectorUuid().toString(), alert);
                })
                .to(outboundTopic, Produced.with(new Serdes.StringSerde(), new AlertJsonSerde()));

        return builder.build();
    }
}

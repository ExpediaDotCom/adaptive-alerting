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
package com.expedia.adaptivealerting.anomdetect;

import com.expedia.adaptivealerting.anomdetect.util.ModelServiceConnector;
import com.expedia.adaptivealerting.core.anomaly.AnomalyResult;
import com.expedia.adaptivealerting.core.data.MappedMetricData;
import com.expedia.adaptivealerting.core.util.ReflectionUtil;
import com.expedia.metrics.MetricData;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigObject;
import com.typesafe.config.ConfigValue;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.expedia.adaptivealerting.core.util.AssertUtil.notNull;

/**
 * Component that manages a given set of anomaly detectors.
 *
 * @author David Sutherland
 * @author Willie Wheeler
 */
@Slf4j
public class AnomalyDetectorManager {
    
    /**
     * Detectors configuration key.
     */
    private static final String CK_DETECTORS = "detectors";
    
    /**
     * Factories that know how to produce anomaly detectors on demand.
     */
    private final Map<String, AnomalyDetectorFactory> detectorFactories;
    
    /**
     * The managed detectors.
     */
    private final Map<UUID, AnomalyDetector> detectors = new HashMap<>();
    
    /**
     * Max samples required to evaluate performance
     */
    private static final int PERFMON_SAMPLE_SIZE = 100;
    
    @Getter
    private ModelServiceConnector modelServiceConnector;
    
    /**
     * Creates a new anomaly detector manager.
     *
     * @param config                Manager config
     * @param modelServiceConnector Model service connector.
     */
    public AnomalyDetectorManager(Config config, ModelServiceConnector modelServiceConnector) {
        notNull(config, "config can't be null");
        
        val detectorsConfig = config.getConfig(CK_DETECTORS);
        this.modelServiceConnector = modelServiceConnector;
        
        this.detectorFactories = new HashMap<>();
        
        // Calling detectorsConfig.entrySet() does a recursive traversal, which isn't what we want here.
        // Whereas detectorsConfig.root().entrySet() returns only the direct children.
        for (Map.Entry<String, ConfigValue> entry : detectorsConfig.root().entrySet()) {
            final String detectorType = entry.getKey();
            final Config detectorFactoryAndConfig = ((ConfigObject) entry.getValue()).toConfig();
            final String detectorFactoryClassname = detectorFactoryAndConfig.getString("factory");
            final Config detectorConfig = detectorFactoryAndConfig.getConfig("config");
            
            log.info("Initializing AnomalyDetectorFactory: type={}, className={}",
                    detectorType, detectorFactoryClassname);
            final AnomalyDetectorFactory factory =
                    (AnomalyDetectorFactory) ReflectionUtil.newInstance(detectorFactoryClassname);
            factory.init(detectorConfig, modelServiceConnector);
            log.info("Initialized AnomalyDetectorFactory: type={}, className={}",
                    detectorType, detectorFactoryClassname);
            
            detectorFactories.put(detectorType, factory);
        }
    }
    
    /**
     * <p>
     * Convenience method to classify the mapped metric point, performing detector lookup behind the scenes. Note that
     * this method has a side-effect in that it updates the passed mapped metric data itself.
     * </p>
     * <p>
     * Returns {@code null} if there's no detector defined for the given mapped metric data.
     * </p>
     *
     * @param mappedMetricData Mapped metric point.
     * @return The mapped metric point, or {@code null} if there's no associated detector.
     */
    public AnomalyResult classify(MappedMetricData mappedMetricData) {
        notNull(mappedMetricData, "mappedMetricData can't be null");
        
        log.info("Classifying mappedMetricData={}", mappedMetricData);
        final AnomalyDetector detector = detectorFor(mappedMetricData);
        if (detector == null) {
            log.warn("No detector for mappedMetricData={}", mappedMetricData);
            return null;
        }
        final MetricData metricData = mappedMetricData.getMetricData();
        return detector.classify(metricData);
    }
    
    /**
     * Gets the anomaly detector for the given metric point, creating it if absent. Returns {@code null} if there's no
     * {@link AnomalyDetectorFactory} defined for the mapped metric data's detector type.
     *
     * @param mappedMetricData Mapped metric point.
     * @return Anomaly detector for the given metric point, or {@code null} if there's some problem loading the
     * detector.
     */
    private AnomalyDetector detectorFor(MappedMetricData mappedMetricData) {
        notNull(mappedMetricData, "mappedMetricData can't be null");
        final UUID detectorUuid = mappedMetricData.getDetectorUuid();
        final AnomalyDetector detector = detectors.get(detectorUuid);
        if (detector == null) {
            final String detectorType = mappedMetricData.getDetectorType();
            final AnomalyDetectorFactory factory = detectorFactories.get(detectorType);
            if (factory == null) {
                log.warn("No AnomalyDetectorFactory registered for detectorType={}", detectorType);
            } else {
                log.info("Creating anomaly detector: uuid={}, type={}", detectorUuid, detectorType);
                final AnomalyDetector innerDetector = factory.create(detectorUuid);
                
                // TODO Temporarily commenting this out because it's causing a problem
                // for the RandomCutForest detector (NPE). We can reinstate after we
                // figure out how we want to address this. [WLW]
//                final PerformanceMonitor perfMonitor = new PerformanceMonitor(new PerfMonHandler(), new RmseEvaluator(), PERFMON_SAMPLE_SIZE);
//                detector = new MonitoredDetector(innerDetector, perfMonitor);
//                detectors.put(detectorUuid, detector);
                detectors.put(detectorUuid, innerDetector);
            }
        }
        return detector;
    }
}

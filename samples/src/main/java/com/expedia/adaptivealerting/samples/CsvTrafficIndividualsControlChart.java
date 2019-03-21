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
package com.expedia.adaptivealerting.samples;

import com.expedia.adaptivealerting.anomdetect.algo.IndividualsControlChartDetector;
import com.expedia.adaptivealerting.core.data.MetricFrame;
import com.expedia.adaptivealerting.core.data.MetricFrameLoader;
import com.expedia.adaptivealerting.core.evaluator.RmseEvaluator;
import com.expedia.adaptivealerting.tools.pipeline.filter.AnomalyDetectorFilter;
import com.expedia.adaptivealerting.tools.pipeline.filter.EvaluatorFilter;
import com.expedia.adaptivealerting.tools.pipeline.sink.AnomalyChartSink;
import com.expedia.adaptivealerting.tools.pipeline.source.MetricFrameMetricSource;
import com.expedia.adaptivealerting.tools.pipeline.util.PipelineFactory;
import com.expedia.metrics.MetricDefinition;

import java.io.InputStream;

import static com.expedia.adaptivealerting.tools.visualization.ChartUtil.createChartFrame;
import static com.expedia.adaptivealerting.tools.visualization.ChartUtil.showChartFrame;

public final class CsvTrafficIndividualsControlChart {

    public static void main(String[] args) throws Exception {

        // TODO Use the FileDataConnector rather than the MetricFrameLoader. [WLW]
        /* The individuals chart works best with data that have a steady mean and fluctuations could indicate outliers - like thread count,
             MemoryUtil of clusters, anything where deviation from runningMean could hint possible outliers, here suitable-dataset.csv is just
             a placeholder name to be replaced by applicable data.
        */
        final InputStream is = ClassLoader.getSystemResourceAsStream("samples/suitable-dataset.csv");
        final MetricFrame frame = MetricFrameLoader.loadCsv(new MetricDefinition("csv"), is, true);
        final MetricFrameMetricSource source = new MetricFrameMetricSource(frame, "data", 200L);

        final AnomalyDetectorFilter detectorFilter = new AnomalyDetectorFilter(new IndividualsControlChartDetector());
        final EvaluatorFilter evaluator = new EvaluatorFilter(new RmseEvaluator());
        final AnomalyChartSink chartWrapper = PipelineFactory.createChartSink("IndividualsControlChart");

        source.addSubscriber(detectorFilter);
        detectorFilter.addSubscriber(evaluator);
        detectorFilter.addSubscriber(chartWrapper);
        evaluator.addSubscriber(chartWrapper);

        showChartFrame(createChartFrame("Cal Inflow", chartWrapper.getChart()));
        source.start();
    }
}

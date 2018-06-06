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
package com.expedia.adaptivealerting.samples;

import com.expedia.adaptivealerting.anomdetect.EwmaAnomalyDetector;
import com.expedia.adaptivealerting.core.evaluator.RmseEvaluator;
import com.expedia.adaptivealerting.tools.pipeline.filter.AnomalyDetectorStreamFilter;
import com.expedia.adaptivealerting.tools.pipeline.filter.EvaluatorStreamFilter;
import com.expedia.adaptivealerting.tools.pipeline.source.WhiteNoiseMetricSource;
import com.expedia.adaptivealerting.tools.visualization.ChartSeries;

import static com.expedia.adaptivealerting.tools.visualization.ChartUtil.*;

/**
 * This is a sample pipeline to calculate RMSE
 *
 * @author Karan Shah
 */
public class RandomWhiteNoiseRmse {
    
    public static void main(String[] args) {
        final WhiteNoiseMetricSource source = new WhiteNoiseMetricSource("white-noise", 1000L, 0.0, 1.0);
        
        final AnomalyDetectorStreamFilter ewmaFilter = new AnomalyDetectorStreamFilter(new EwmaAnomalyDetector());
        source.addSubscriber(ewmaFilter);
        final EvaluatorStreamFilter evaluatorFilter = new EvaluatorStreamFilter(new RmseEvaluator());
        ewmaFilter.addSubscriber(evaluatorFilter);
        
        
        
        //final ChartSeries rmseSeries = new ChartSeries();
        //showChartFrame(createChartFrame("RMSE", createChart("RMSE", rmseSeries)));

        source.start();
    }
}

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
package com.expedia.adaptivealerting.anomdetect.cusum;

import com.expedia.adaptivealerting.anomdetect.AbstractAnomalyDetector;
import com.expedia.adaptivealerting.core.anomaly.AnomalyLevel;
import com.expedia.adaptivealerting.core.anomaly.AnomalyResult;
import com.expedia.metrics.MetricData;
import lombok.Data;
import lombok.NonNull;

import java.util.UUID;

import static com.expedia.adaptivealerting.core.anomaly.AnomalyLevel.*;
import static com.expedia.adaptivealerting.core.util.AssertUtil.notNull;

/**
 * <p>
 * Anomaly detector based on the cumulative sum. This is an online algorithm, meaning that it updates the thresholds
 * incrementally as new data comes in.
 * </p>
 * <p>
 * https://www.spcforexcel.com/knowledge/variable-control-charts/keeping-process-target-cusum-charts
 * </p>
 */
@Data
public final class CusumAnomalyDetector extends AbstractAnomalyDetector<CusumParams> {
    private static final double STD_DEV_DIVISOR = 1.128;
    
    @NonNull
    private CusumParams params;
    
    /**
     * Total number of data points seen so far.
     */
    private int totalDataPoints = 1;
    
    /**
     * Cumulative sum on the high side. SH
     */
    private double sumHigh = 0.0;
    
    /**
     * Cumulative sum on the low side. SL
     */
    private double sumLow = 0.0;
    
    /**
     * Moving range. Used to estimate the standard deviation.
     */
    private double movingRange = 0.0;
    
    /**
     * Previous value.
     */
    private double prevValue = 0.0;
    
    public CusumAnomalyDetector() {
        this(UUID.randomUUID(), new CusumParams());
    }
    
    public CusumAnomalyDetector(CusumParams params) {
        this(UUID.randomUUID(), params);
    }
    
    public CusumAnomalyDetector(UUID uuid, CusumParams params) {
        notNull(uuid, "uuid can't be null");
        notNull(params, "params can't be null");
        
        setUuid(uuid);
        loadParams(params);
    }

    @Override
    protected Class<CusumParams> getParamsClass() {
        return CusumParams.class;
    }

    @Override
    protected void loadParams(CusumParams params) {
        this.params = params;
        this.prevValue = params.getInitMeanEstimate();
    }
    
    @Override
    public AnomalyResult classify(MetricData metricData) {
        notNull(metricData, "metricData can't be null");
        
        final double observed = metricData.getValue();
        
        this.movingRange += Math.abs(this.prevValue - observed);
        
        final double stdDev = avgMovingRange() / STD_DEV_DIVISOR;
        final double slack = params.getSlackParam() * stdDev;
        final double weakDelta = params.getWeakSigmas() * stdDev;
        final double strongDelta = params.getStrongSigmas() * stdDev;
        
        this.sumHigh = Math.max(0, this.sumHigh + observed - (params.getTargetValue() + slack));
        this.sumLow = Math.min(0, this.sumLow + observed - (params.getTargetValue() - slack));
        
        this.prevValue = observed;
        
        // FIXME This eventually overflows. Realistically it won't happen, but would be nice to fix it anyway. [WLW]
        this.totalDataPoints++;
        
        Double upperStrong;
        Double upperWeak;
        Double lowerStrong;
        Double lowerWeak;
        AnomalyLevel level;
        
        if (totalDataPoints > params.getWarmUpPeriod()) {
            level = NORMAL;
            switch (params.getType()) {
                case LEFT_TAILED:
                    lowerWeak = -weakDelta;
                    lowerStrong = -strongDelta;
                    if (this.sumLow <= lowerStrong) {
                        level = STRONG;
                        // TODO Check whether this is really what we are supposed to do here. [WLW]
                        resetSums();
                    } else if (this.sumLow <= lowerWeak) {
                        level = WEAK;
                    }
                    break;
                case RIGHT_TAILED:
                    upperWeak = weakDelta;
                    upperStrong = strongDelta;
                    if (this.sumHigh >= upperStrong) {
                        level = STRONG;
                        // TODO Check whether this is really what we are supposed to do here. [WLW]
                        resetSums();
                    } else if (this.sumHigh > upperWeak) {
                        level = WEAK;
                    }
                    break;
                case TWO_TAILED:
                    if (this.sumHigh >= strongDelta || this.sumLow <= strongDelta) {
                        level = STRONG;
                        // TODO Check whether this is really what we are supposed to do here. [WLW]
                        resetSums();
                    } else if (this.sumHigh > weakDelta || this.sumLow <= weakDelta) {
                        level = WEAK;
                    }
                    break;
                default:
                    throw new IllegalStateException("Illegal type: " + params.getType());
            }
        } else {
            level = MODEL_WARMUP;
        }
        
        return new AnomalyResult(getUuid(), metricData, level);
    }
    
    private void resetSums() {
        this.sumHigh = 0.0;
        this.sumLow = 0.0;
    }
    
    private double avgMovingRange() {
        if (totalDataPoints > 1) {
            return movingRange / (totalDataPoints - 1);
        }
        return movingRange;
    }
}

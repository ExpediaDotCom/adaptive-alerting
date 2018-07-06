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
package com.expedia.adaptivealerting.tools.pipeline.source;

import com.expedia.adaptivealerting.core.data.MetricFrame;
import com.expedia.adaptivealerting.core.data.Mpoint;
import com.expedia.adaptivealerting.core.util.MetricUtil;
import com.expedia.www.haystack.commons.entities.MetricPoint;

import java.util.ListIterator;

/**
 * Metric source backed by a {@link MetricFrame}.
 *
 * @author Willie Wheeler
 */
public final class MetricFrameMetricSource extends AbstractMetricSource {
    private ListIterator<Mpoint> mpoints;
    
    public MetricFrameMetricSource(MetricFrame frame, String name, long period) {
        super(name, period);
        this.mpoints = frame.listIterator();
    }
    
    @Override
    public MetricPoint next() {
        if (mpoints.hasNext()) {
            final Mpoint mpoint = mpoints.next();
            final long epochSecond = mpoint.getEpochTimeInSeconds();
            final float value = mpoint.getValue().floatValue();
            return MetricUtil.metricPoint(epochSecond, value);
        } else {
            return null;
        }
    }
}

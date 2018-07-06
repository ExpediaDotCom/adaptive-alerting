package com.expedia.adaptivealerting.anomdetect.randomcutforest;


import com.expedia.adaptivealerting.core.data.MappedMpoint;
import com.expedia.adaptivealerting.core.util.MetricUtil;
import com.expedia.www.haystack.commons.entities.MetricPoint;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.util.Iterator;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MetricPointQueueTest {
    private Shingle queue;
    private Iterator<String> iterator;

     @Before
    public void setUp() {
        queue = null;
        iterator = null;
    }

    @Test
    public void emptyConstructorTest() {
        queue = new Shingle();

        assertFalse(queue.toValues().isPresent());
        assertFalse(queue.isReady());
    }

    @Test
    public void isReadyTest() {
        queue = new Shingle();


        queue.offer(toMappedMPoint(MetricUtil.metricPoint(Instant.now().getEpochSecond(), Double.valueOf(1))));
        assertFalse(queue.isReady());

        for (int i = 2; i <= 10; i++) {
            queue.offer(toMappedMPoint(MetricUtil.metricPoint(Instant.now().getEpochSecond(), Double.valueOf(i))));
        }
        assertTrue(queue.isReady());

        queue.offer(toMappedMPoint(MetricUtil.metricPoint(Instant.now().getEpochSecond(), Double.valueOf(11))));
        assertTrue(queue.isReady());
    }

    @Test
    public void toOutputFormatTest() {
        queue = new Shingle();

        for (int i = 1; i <= 10; i++) {
            queue.offer(toMappedMPoint(MetricUtil.metricPoint(Instant.now().getEpochSecond(), Double.valueOf(i))));
        }
        assertTrue(queue.isReady());
        String out = queue.toCsv().get();
        String expectedOut = "1.0,2.0,3.0,4.0,5.0,6.0,7.0,8.0,9.0,10.0";
        assertTrue(out.equals(expectedOut));
    }

    private MappedMpoint toMappedMPoint(MetricPoint metricPoint) {
        final MappedMpoint mappedMpoint = new MappedMpoint();
        mappedMpoint.setMpoint(MetricUtil.toMpoint(metricPoint));
        return mappedMpoint;
    }

}

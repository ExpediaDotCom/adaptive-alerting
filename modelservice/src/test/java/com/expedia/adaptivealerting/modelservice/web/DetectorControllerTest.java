package com.expedia.adaptivealerting.modelservice.web;

import com.expedia.adaptivealerting.modelservice.entity.ElasticsearchDetector;
import com.expedia.adaptivealerting.modelservice.repo.ElasticsearchDetectorRepository;
import com.expedia.adaptivealerting.modelservice.service.ElasticsearchService;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@Slf4j
@RunWith(MockitoJUnitRunner.class)
public class DetectorControllerTest {

    @Mock
    private ElasticsearchService elasticsearchService;

    @Mock
    private ElasticsearchDetectorRepository repo;

    @Mock
    private List<ElasticsearchDetector> detectors;

    @Before
    public void setUp() {
        when(elasticsearchService.getLastUpdatedDetectors(anyInt())).thenReturn(detectors);
    }

    @Test
    public void testToggleDetector() {
        elasticsearchService.toggleDetector("uuid", true);
        verify(elasticsearchService, atLeastOnce()).toggleDetector("uuid", true);
    }

    @Test
    public void testGetLastUpdatedDetectors() {
        int interval = 5;
        List<ElasticsearchDetector> actualDetectors = elasticsearchService.getLastUpdatedDetectors(interval);
        assertNotNull(detectors);
        assertSame(detectors, actualDetectors);
    }

}
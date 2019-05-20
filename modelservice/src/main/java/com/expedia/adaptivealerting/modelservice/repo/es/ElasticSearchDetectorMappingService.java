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
package com.expedia.adaptivealerting.modelservice.repo.es;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.expedia.adaptivealerting.modelservice.model.CreateDetectorMappingRequest;
import com.expedia.adaptivealerting.modelservice.model.Detector;
import com.expedia.adaptivealerting.modelservice.model.DetectorMapping;
import com.expedia.adaptivealerting.modelservice.model.DetectorMatchResponse;
import com.expedia.adaptivealerting.modelservice.model.MatchingDetectorsResponse;
import com.expedia.adaptivealerting.modelservice.model.SearchMappingsRequest;
import com.expedia.adaptivealerting.modelservice.repo.DetectorMappingService;
import com.expedia.adaptivealerting.modelservice.util.QueryUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsRequest;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsResponse;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.document.DocumentField;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.ConstantScoreQueryBuilder;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.percolator.PercolateQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.expedia.adaptivealerting.modelservice.repo.es.DetectorMappingEntity.LAST_MOD_TIME_KEYWORD;

@Component
@Slf4j
@SuppressWarnings({"PMD.ExcessiveImports", "PMD.AvoidThrowingRawExceptionTypes"})
public class ElasticSearchDetectorMappingService implements DetectorMappingService {
    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private ElasticSearchProperties elasticSearchProperties;

    @Autowired
    private ElasticSearchClient elasticSearchClient;

    private final Timer delayTimer;
    private final Counter exceptionCount;

    @Autowired
    public ElasticSearchDetectorMappingService(MetricRegistry metricRegistry) {
        this.delayTimer = metricRegistry.timer("es-lookup.time-delay");
        this.exceptionCount = metricRegistry.counter("es-lookup.exception");
    }

    @Override
    public MatchingDetectorsResponse findMatchingDetectorMappings(List<Map<String, String>> tagsList) {
        try {
            List<BytesReference> refList = new ArrayList<>();
            for (Map<String, String> tags : tagsList) {
                XContentBuilder xContent = XContentFactory.jsonBuilder();
                xContent.map(tags);
                refList.add(BytesReference.bytes(xContent));
            }
            PercolateQueryBuilder percolateQuery = new PercolateQueryBuilder(DetectorMappingEntity.QUERY_KEYWORD,
                    refList, XContentType.JSON);
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            ConstantScoreQueryBuilder constantScoreQueryBuilder = new ConstantScoreQueryBuilder(percolateQuery);
            searchSourceBuilder.query(constantScoreQueryBuilder);
            searchSourceBuilder.timeout(new TimeValue(elasticSearchProperties.getConfig().getConnectionTimeout()));
            //FIXME setting default result set size to 500.
            // This is for returning detectors matching for set of metrics
            searchSourceBuilder.size(500);
            return getDetectorMappings(searchSourceBuilder, tagsList);
        } catch (IOException e) {
            log.error("Error ES lookup", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public String createDetectorMapping(CreateDetectorMappingRequest createRequest) {
        Set<String> fields = createRequest.getFields();
        Set<String> newFieldMappings = removeFieldsHavingExistingMapping(fields);
        updateIndexMappings(newFieldMappings);
        final DetectorMappingEntity detectorMappingEntity = new DetectorMappingEntity()
                .setUser(createRequest.getUser())
                .setDetector(createRequest.getDetector())
                .setEnabled(true)
                .setLastModifiedTimeInMillis(System.currentTimeMillis())
                .setCreatedTimeInMillis(System.currentTimeMillis());
        final IndexRequest indexRequest = new IndexRequest(elasticSearchProperties.getIndexName(),
                elasticSearchProperties.getDocType());

        String json = null;
        try {
            json = objectMapper.writeValueAsString(detectorMappingEntity);
            indexRequest.source(json, XContentType.JSON);
            IndexResponse indexResponse = elasticSearchClient.index(indexRequest, RequestOptions.DEFAULT);
            return indexResponse.getId();
        } catch (IOException e) {
            log.error(String.format("Indexing mapping %s failed", json, e));
            throw new RuntimeException(e);
        }
    }

    private void updateIndexMappings(Set<String> newFieldMappings) {
        PutMappingRequest request = new PutMappingRequest(elasticSearchProperties.getIndexName());
        Map<String, Object> jsonMap = new HashMap<>();
        Map<String, Object> type = new HashMap<>();
        type.put("type", "keyword");

        Map<String, Object> properties = new HashMap<>();
        newFieldMappings.forEach(field -> {
            properties.put(field, type);
        });

        jsonMap.put("properties", properties);
        request.source(jsonMap);
        request.type(elasticSearchProperties.getDocType());

        try {
            elasticSearchClient.indices().putMapping(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            log.error("Error updating mappings", e);
            throw new RuntimeException(e);
        }
    }

    private Set<String> removeFieldsHavingExistingMapping(Set<String> fields) {
        GetMappingsRequest request = new GetMappingsRequest();
        request.indices(elasticSearchProperties.getIndexName());

        try {
            GetMappingsResponse mappingsResponse = elasticSearchClient.indices().getMapping(request, RequestOptions.DEFAULT);
            Map<String, String> mapProperties = ((Map<String, String>) mappingsResponse.getMappings()
                    .get(elasticSearchProperties.getIndexName())
                    .get(elasticSearchProperties.getDocType())
                    .sourceAsMap().get("properties"));

            Set<String> mappedFields = mapProperties.entrySet().stream()
                    .map(en -> en.getKey()).collect(Collectors.toSet());
            fields.removeAll(mappedFields);
            return fields;
        } catch (IOException e) {
            log.error("Error finding mappings", e);
            throw new RuntimeException(e);
        }
    }

    private void updateDetectorMapping(String index, DetectorMappingEntity detectorMappingEntity) {
        final IndexRequest indexRequest = new IndexRequest(elasticSearchProperties.getIndexName(),
                elasticSearchProperties.getDocType(), index);
        String json = null;
        try {
            json = objectMapper.writeValueAsString(detectorMappingEntity);
            indexRequest.source(json, XContentType.JSON);
            elasticSearchClient.index(indexRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            log.error("Update Index failed", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteDetectorMapping(String id) {
        final DeleteRequest deleteRequest = new DeleteRequest(elasticSearchProperties.getIndexName(),
                elasticSearchProperties.getDocType(), id);
        try {
            elasticSearchClient.delete(deleteRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            log.error(String.format("Deleting mapping %s failed", id), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void disableDetectorMapping(String id) {
        DetectorMapping detectorMapping = findDetectorMapping(id);
        if (detectorMapping.isEnabled()) {
            final DetectorMappingEntity detectorMappingEntity = new DetectorMappingEntity()
                    .setUser(detectorMapping.getUser())
                    .setDetector(detectorMapping.getDetector())
                    .setQuery(QueryUtil.buildQuery(detectorMapping.getExpression()))
                    .setEnabled(false)
                    .setLastModifiedTimeInMillis(System.currentTimeMillis())
                    .setCreatedTimeInMillis(detectorMapping.getCreatedTimeInMillis());
            updateDetectorMapping(id, detectorMappingEntity);
        }
    }

    @Override
    public DetectorMapping findDetectorMapping(String id) {
        GetRequest getRequest = new GetRequest(elasticSearchProperties.getIndexName(),
                elasticSearchProperties.getDocType(), id);
        try {
            GetResponse response = elasticSearchClient.get(getRequest, RequestOptions.DEFAULT);
            return getDetectorMapping(response.getSourceAsString(), response.getId(), Optional.empty());
        } catch (IOException e) {
            log.error(String.format("Get mapping %s failed", id), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<DetectorMapping> search(SearchMappingsRequest searchMappingsRequest) {
        SearchRequest searchRequest = new SearchRequest(elasticSearchProperties.getIndexName());
        searchRequest.types(elasticSearchProperties.getDocType());
        BoolQueryBuilder query = QueryBuilders.boolQuery();
        if (searchMappingsRequest.getUserId() != null) {
            query.must(userIdQuery(searchMappingsRequest));
        }
        if (searchMappingsRequest.getDetectorUuid() != null) {
            query.must(detectorIdQuery(searchMappingsRequest));
        }
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(query);
        //FIXME setting default result set size to 500 until we have pagination.
        searchSourceBuilder.size(500);
        searchRequest.source(searchSourceBuilder);
        List<DetectorMapping> result = getDetectorMappings(searchRequest);
        //FIXME - move this condition to search query.
        return result.stream().filter(detectorMapping -> detectorMapping.isEnabled()).collect(Collectors.toList());
    }

    @Override
    public List<DetectorMapping> findLastUpdated(int timeInSeconds) {
        final SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        final BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        long fromTime = System.currentTimeMillis() - timeInSeconds * 1000;
        boolQuery.must(new RangeQueryBuilder(LAST_MOD_TIME_KEYWORD).gt(fromTime));
        sourceBuilder.query(boolQuery);
        //FIXME setting default result set size to 500.
        sourceBuilder.size(500);
        final SearchRequest searchRequest =
                new SearchRequest()
                        .source(sourceBuilder)
                        .indices(elasticSearchProperties.getIndexName())
                        .types(elasticSearchProperties.getDocType());
        return getDetectorMappings(searchRequest);
    }

    private List<DetectorMapping> getDetectorMappings(SearchRequest searchRequest) {
        try {
            SearchResponse searchResponse = elasticSearchClient.search(searchRequest, RequestOptions.DEFAULT);
            SearchHits hits = searchResponse.getHits();
            return Arrays.asList(hits.getHits()).stream()
                    .map(hit -> getDetectorMapping(hit.getSourceAsString(), hit.getId(), Optional.empty()))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("Search failed", e);
            throw new RuntimeException("Search failed", e);
        }
    }

    private NestedQueryBuilder detectorIdQuery(SearchMappingsRequest searchMappingsRequest) {
        return QueryBuilders.nestedQuery(DetectorMappingEntity.DETECTOR_KEYWORD,
                QueryBuilders.boolQuery().must(QueryBuilders.matchPhraseQuery(
                        DetectorMappingEntity.DETECTOR_KEYWORD + "." + DetectorMappingEntity.DETECTOR_ID_KEYWORD,
                        searchMappingsRequest.getDetectorUuid().toString())),
                ScoreMode.None);
    }

    private NestedQueryBuilder userIdQuery(SearchMappingsRequest searchMappingsRequest) {
        return QueryBuilders.nestedQuery(DetectorMappingEntity.USER_KEYWORD,
                QueryBuilders.boolQuery().must(QueryBuilders.matchPhraseQuery(
                        DetectorMappingEntity.USER_KEYWORD + "." + DetectorMappingEntity.USER_ID_KEYWORD,
                        searchMappingsRequest.getUserId())),
                ScoreMode.None);
    }

    private MatchingDetectorsResponse getDetectorMappings(SearchSourceBuilder searchSourceBuilder,
                                                          List<Map<String, String>> tagsList) throws IOException {
        final SearchRequest searchRequest =
                new SearchRequest()
                        .source(searchSourceBuilder)
                        .indices(elasticSearchProperties.getIndexName());
        SearchResponse searchResponse = elasticSearchClient.search(searchRequest, RequestOptions.DEFAULT);
        delayTimer.update(searchResponse.getTook().getMillis(), TimeUnit.MILLISECONDS);
        SearchHit[] hits = searchResponse.getHits().getHits();
        //   return Arrays.asList(hits.getHits()).stream()
        //        .map(hit -> getDetectorMapping(hit.getSourceAsString(), hit.getId(), Optional.empty()))
        //        .collect(Collectors.toList());
        List<DetectorMapping> detectorMappings = Arrays.asList(hits).stream()
                .map(hit -> getDetectorMapping(hit.getSourceAsString(), hit.getId(), Optional.of(hit.getFields())))
                // .filter(detectorMapping -> detectorMapping.isEnabled()) //FIXME - move this condition into search query
                .collect(Collectors.toList());
        return convertToMatchingDetectorsResponse(new DetectorMatchResponse(detectorMappings,
                searchResponse.getTook().getMillis()));
    }

    private DetectorMapping getDetectorMapping(String json, String id,
                                               Optional<Map<String, DocumentField>> documentFieldMap) {
        DetectorMappingEntity detectorEntity = new DetectorMappingEntity();
        try {
            detectorEntity = objectMapper.readValue(json, DetectorMappingEntity.class);
        } catch (Exception e) {
            log.error("Deserilisation error", e);
        }
        DetectorMapping detectorMapping = new DetectorMapping()
                .setId(id)
                .setDetector(new Detector(detectorEntity.getDetector().getId()))
                .setExpression(QueryUtil.buildExpression(detectorEntity.getQuery()))
                .setEnabled(detectorEntity.isEnabled())
                .setCreatedTimeInMillis(detectorEntity.getCreatedTimeInMillis())
                .setLastModifiedTimeInMillis(detectorEntity.getLastModifiedTimeInMillis())
                .setUser(detectorEntity.getUser());
        documentFieldMap.ifPresent(dfm -> {
            List values = dfm.get("_percolator_document_slot").getValues();
            List<Integer> indexes = new ArrayList<>();
            values.forEach(index -> {
                indexes.add(Integer.valueOf(index.toString()));
            });
            detectorMapping.setSearchIndexes(indexes);
        });
        return detectorMapping;
    }

    private MatchingDetectorsResponse convertToMatchingDetectorsResponse(DetectorMatchResponse res) {
        Map<Integer, List<Detector>> groupedDetectorsByIndex = new HashMap<>();
        log.info("Mapping-Cache: found {} matching mappings", res.getDetectorMappings().size());
        res.getDetectorMappings().forEach(detectorMapping -> {
            detectorMapping.getSearchIndexes().forEach(searchIndex -> {
                groupedDetectorsByIndex.computeIfAbsent(searchIndex, index -> new ArrayList<>());
                groupedDetectorsByIndex.computeIfPresent(searchIndex, (index, list) -> {
                    list.add(detectorMapping.getDetector());
                    return list;
                });
            });

        });
        return new MatchingDetectorsResponse(groupedDetectorsByIndex, res.getLookupTimeInMillis());
    }
}

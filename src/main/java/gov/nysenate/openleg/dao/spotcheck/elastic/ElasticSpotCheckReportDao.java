package gov.nysenate.openleg.dao.spotcheck.elastic;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import com.google.common.eventbus.Subscribe;
import gov.nysenate.openleg.dao.base.ElasticBaseDao;
import gov.nysenate.openleg.dao.base.SearchIndex;
import gov.nysenate.openleg.dao.base.SpotCheckIndex;
import gov.nysenate.openleg.dao.spotcheck.SpotCheckContentIdMapper;
import gov.nysenate.openleg.dao.spotcheck.SpotCheckReportDao;
import gov.nysenate.openleg.model.search.ClearIndexEvent;
import gov.nysenate.openleg.model.search.RebuildIndexEvent;
import gov.nysenate.openleg.model.search.SearchResults;
import gov.nysenate.openleg.model.spotcheck.*;
import gov.nysenate.openleg.service.base.search.IndexedSearchService;
import gov.nysenate.openleg.util.OutputUtils;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.elasticsearch.index.query.QueryBuilders.*;

/**
 * Created by PKS on 7/25/16.
 */
@Repository
public class ElasticSpotCheckReportDao
        extends ElasticBaseDao
        implements SpotCheckReportDao {

    private static final Logger logger = LoggerFactory.getLogger(ElasticSpotCheckReportDao.class);

    protected static final String reportType = "reports";

    protected static final String observationType = "observations";

    @Autowired
    private List<SpotCheckContentIdMapper> contentIdMappers;

    private static Table<SpotCheckDataSource, SpotCheckContentType, SpotCheckContentIdMapper> contentIdMapperTable = HashBasedTable.create();


    @PostConstruct
    private void init() {
        contentIdMappers.forEach(
                contentIdMapper ->
                        contentIdMapperTable
                                .put(contentIdMapper.getDataSource(), contentIdMapper.getContentType(), contentIdMapper));
    }


    /** --- Implemented Methods --- */

    /**
     * {@inheritDoc}
     */
    @Override
    public <ContentKey> SpotCheckReport<ContentKey> getReport(SpotCheckReportId Id) throws DataAccessException {
        String[] spotcheckIndices = {getSpotcheckIndexName(Id.getReferenceType().getDataSource(),
                Id.getReferenceType().getContentType())};
        QueryBuilder query = matchQuery("reportId.reportDateTime", Id.getReportDateTime().toString());
        SearchResponse reportSearchResponse = getSearchRequest(spotcheckIndices, query, null, null, false, 0, reportType)
                .execute()
                .actionGet();

        if (reportSearchResponse.getHits().getTotalHits() > 0) {
            try {
                SpotCheckReport<ContentKey> report = getSpotcheckReportFrom(reportSearchResponse.getHits().getAt(0).getSource());
                String reportId = reportSearchResponse.getHits().getAt(0).getId();
                String reportIndex = reportSearchResponse.getHits().getAt(0).getIndex();
                Map<ContentKey, SpotCheckObservation<ContentKey>> observationMap = getObservationsForReport(reportIndex, reportId);
                report.setObservations(observationMap);
                return report;

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        throw new SpotCheckReportNotFoundEx(Id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <ContentKey> void saveReport(SpotCheckReport<ContentKey> report) {
        if (report == null) {
            throw new IllegalArgumentException("Supplied report cannot be null.");
        }
        if (report.getObservations() == null) {
            logger.warn("The observations have not been set on this report.");
            return;
        }

        ElasticReport elasticReport = new ElasticReport(report);

        String elasticReportJson = OutputUtils.toJson(elasticReport);

        BulkRequestBuilder bulkRequest = searchClient.prepareBulk();

        String spotcheckIndex = getSpotcheckIndexName(report.getReportId().getReferenceType().getDataSource(),
                report.getReportId().getReferenceType().getContentType());

        bulkRequest.add(getIndexRequest(spotcheckIndex, reportType, String.valueOf(report.hashCode()))
                .setSource(elasticReportJson));

        List<ElasticObservation> elasticObservations = setMismatchStatuses(spotcheckIndex,
                toElasticObservations(String.valueOf(report.hashCode()),
                        report.getObservations()
                                .values().stream()
                                .collect(Collectors.toList())));

        elasticObservations.forEach(elasticObservation ->
                bulkRequest.add(
                        getIndexRequest(spotcheckIndex, observationType,
                                elasticObservation.getMismatchId().toString())
                                .setSource(OutputUtils.toJson(elasticObservation))
                )
        );

        safeBulkRequestExecute(bulkRequest);
    }

    @Override
    public <ContentKey> List<SpotCheckReportSummary> getReportSummaries(Map<SpotCheckDataSource, Set<SpotCheckContentType>> dataSourceSetMap,
                                                                        LocalDateTime start,
                                                                        LocalDateTime end,
                                                                        gov.nysenate.openleg.dao.base.SortOrder dateOrder) {
        String[] spotcheckIndices = getSpotcheckIndexNames(dataSourceSetMap);
        QueryBuilder query = rangeQuery("reportId.reportDateTime").from(start.toString()).to(end.toString());
        SortBuilder sort = SortBuilders
                .fieldSort("reportId.reportDateTime")
                .order(SortOrder.valueOf(dateOrder.toString()))
                .unmappedType("date");
        SearchResponse reportSearchResponse = getSearchRequest(spotcheckIndices, query,
                null, Collections.singletonList(sort), true, 0, reportType)
                .execute().actionGet();
        List<SpotCheckReportSummary> spotCheckReportSummaries = getSpotcheckReportSummary(reportSearchResponse);

        return spotCheckReportSummaries;
    }

    @Override
    public <ContentKey> List<SpotCheckReportSummary> getReportSummaries(SpotCheckRefType refType, LocalDateTime start,
                                                                        LocalDateTime end, gov.nysenate.openleg.dao.base.SortOrder dateOrder) {
        String[] spotcheckIndices;
        if (refType == null)
            spotcheckIndices = getIndices().stream().toArray(String[]::new);
        else
            spotcheckIndices = Stream.of(getSpotcheckIndexName(refType.getDataSource(), refType.getContentType())).toArray(String[]::new);
        QueryBuilder query = rangeQuery("reportId.reportDateTime").from(start.toString()).to(end.toString());

        SortBuilder sort = SortBuilders
                .fieldSort("reportId.reportDateTime")
                .order(SortOrder.valueOf(dateOrder.toString()))
                .unmappedType("date");
        SearchResponse reportSearchResponse = getSearchRequest(spotcheckIndices, query,
                null, Collections.singletonList(sort), true, 0, reportType)
                .execute()
                .actionGet();
        List<SpotCheckReportSummary> spotCheckReportSummaries = getSpotcheckReportSummary(reportSearchResponse);
        return spotCheckReportSummaries;
    }

    @Override
    public <ContentKey> SpotCheckOpenMismatches<ContentKey> getOpenMismatches(OpenMismatchQuery query) {

        Set<String> indices = query.getRefTypes().stream()
                .map(SpotCheckIndex::getIndex)
                .map(SpotCheckIndex::getIndexName)
                .collect(Collectors.toSet());

        BoolQueryBuilder queryFilters = QueryBuilders.boolQuery();
        query.getMismatchTypes().forEach(spotCheckMismatchType -> {
            queryFilters.should(QueryBuilders.matchQuery("mismatchType", spotCheckMismatchType.toString()));
        });

        QueryBuilder esQuery = rangeQuery("observedDateTime")
                .from(query.getObservedAfter().toString())
                .to(query.getObservedBefore().toString());

        SortBuilder sort = SortBuilders.fieldSort(query.getOrderBy().getColName())
                .order(SortOrder.valueOf(query.getOrder().toString()));

        // Get search request
        SearchRequestBuilder searchRequest = getSearchRequest(indices, esQuery, queryFilters,
                null, null,
                Collections.singletonList(sort), query.getLimitOffset(), true);

        // Get search response
        SearchResponse searchResponse = searchRequest.execute().actionGet();

        // Get search results
        SearchResults<SpotCheckObservation<ContentKey>> searchResults =
                getSearchResults(searchResponse, query.getLimitOffset(),
                        this::getSpotcheckObservationFrom);

        // Extract observations from results
        Map<ContentKey, SpotCheckObservation<ContentKey>> observationMap =
                searchResults.getRawResults().stream()
                        .collect(Collectors.toMap(SpotCheckObservation::getKey, Function.identity()));

        return new SpotCheckOpenMismatches<>(query.getRefTypes(), observationMap, searchResults.getTotalResults());
//
//        String[] spotCheckIndexes = indexes.stream().toArray(String[]::new);
//        SearchResponse searchResponse = searchClient.prepareSearch()
//                .setIndices(spotCheckIndexes)
//                .setTypes(observationType)
//                .setPostFilter(queryFilters)
//                .addSort(query.getOrderBy().getColName(), SortOrder.valueOf(query.getOrder().toString()))
//                .setScroll(new TimeValue(60000))
//                .setSize(query.getLimitOffset().getLimit())
//                .execute().actionGet();
//        Map<ContentKey, SpotCheckObservation<ContentKey>> observations = new HashMap<>();
//        List<Integer> countList = new ArrayList<>();
//        Integer offset = query.getLimitOffset().getOffsetStart();
//        Integer scroll = 1;
//        do {
//            if (offset.equals(scroll)) {
//                searchResponse.getHits().forEach(observation -> {
//                    SpotCheckObservation<ContentKey> spotcheckObservation = getSpotcheckObservationFrom(observation);
//                    countList.add(spotcheckObservation.getMismatches().values().size());
//                    observations.put(spotcheckObservation.getKey(), spotcheckObservation);
//                });
//            }
//            scroll += 10;
//            searchResponse = searchClient.prepareSearchScroll(searchResponse.getScrollId())
//                    .setScroll(new TimeValue(60000))
//                    .execute()
//                    .actionGet();
//        }
//        while (searchResponse.getHits().getHits().length != 0 || observations.size() != query.getLimitOffset().getLimit());
//        Integer totalMismatches = countList.stream().mapToInt(Integer::intValue).sum();
//        return new SpotCheckOpenMismatches<>(query.getRefTypes(), observations, totalMismatches);
    }

    @Override
    public <ContentKey> SpotCheckOpenMismatches<ContentKey> getOpenMismatches(SpotCheckDataSource dataSource, SpotCheckContentType contentType, OpenMismatchQuery query) {
        List<SpotCheckRefType> spotCheckRefTypes = SpotCheckRefType.get(dataSource, contentType);
        query.setRefTypes(Sets.newHashSet(spotCheckRefTypes));
        return getOpenMismatches(query);
    }

    @Override
    public <ContentKey> OpenMismatchSummary getOpenMismatchSummary(Map<SpotCheckDataSource, Set<SpotCheckContentType>> dataSourceSetMap,
                                                                   LocalDateTime observedAfter) {
        String[] spotcheckIndexes = SpotCheckIndex.getIndices(dataSourceSetMap).stream().map(SpotCheckIndex::getIndexName).toArray(String[]::new);
        QueryBuilder query = QueryBuilders.boolQuery()
                .must(rangeQuery("observedDateTime").from(observedAfter.toString()));
        SearchResponse response = getSearchRequest(spotcheckIndexes, query,
                null, null, true, 0, observationType)
                .setTypes(observationType)
                .execute()
                .actionGet();
        Map<SpotCheckRefType, Collection<SpotCheckObservation<ContentKey>>> observationsMap = getRefTypeObservationMap(response);
        Set<SpotCheckRefType> refTypes = new HashSet<>();
        SpotCheckIndex.getIndices(dataSourceSetMap).forEach(spotCheckIndex ->
                refTypes.addAll(SpotCheckRefType.get(spotCheckIndex.getDataSource(), spotCheckIndex.getContentType()))
        );
        OpenMismatchSummary openMismatchSummary = new OpenMismatchSummary(refTypes, observedAfter);
        observationsMap.forEach((refType, observations) -> {
            openMismatchSummary.getRefTypeSummary(refType).addCountsFromObservations(observations);
        });
        return openMismatchSummary;
    }

    @Override
    public <ContentKey> OpenMismatchSummary getOpenMismatchSummary(Set<SpotCheckRefType> refTypes, LocalDateTime observedAfter) {
        List<String> indexes = new ArrayList<>();
        refTypes.forEach(spotCheckRefType -> {
            indexes.add(getSpotcheckIndexName(spotCheckRefType.getDataSource(), spotCheckRefType.getContentType()));
        });
        String[] spotCheckIndexes = indexes.stream().toArray(String[]::new);
        SearchResponse searchObservationsResponse = searchClient.prepareSearch()
                .setIndices(spotCheckIndexes)
                .setTypes(observationType)
                .setQuery(QueryBuilders.boolQuery()
                        .must(rangeQuery("observedDateTime").from(observedAfter.toString()))
                )
                .setScroll(new TimeValue(60000))
                .setSize(100)
                .execute().actionGet();
        Map<SpotCheckRefType, Collection<SpotCheckObservation<ContentKey>>> observationsMap = getRefTypeObservationMap(searchObservationsResponse);
        OpenMismatchSummary openMismatchSummary = new OpenMismatchSummary(refTypes, observedAfter);
        observationsMap.forEach((refType, observations) -> {
            openMismatchSummary.getRefTypeSummary(refType).addCountsFromObservations(observations);
        });
        return openMismatchSummary;
    }


    @Override
    public void deleteReport(SpotCheckReportId reportId) {
        String[] spotcheckIndices = {getSpotcheckIndexName(reportId.getReferenceType().getDataSource(),
                reportId.getReferenceType().getContentType())};
        QueryBuilder query = matchQuery("reportId.reportDateTime", reportId.getReportDateTime());
        SearchResponse reportSearchResponse = getSearchRequest(spotcheckIndices, query, null, null, false, 0, reportType)
                .execute()
                .actionGet();
        if (reportSearchResponse.getHits().getTotalHits() > 0) {
            String Id = reportSearchResponse.getHits().getAt(0).getId();
            String reportIndex = reportSearchResponse.getHits().getAt(0).getIndex();
            deleteEntry(reportIndex, reportType, Id);
        }
        //TODO: Delete observations for the report.
    }

    @Override
    public void setMismatchIgnoreStatus(SpotCheckDataSource dataSource, SpotCheckContentType contentType,
                                        int mismatchId, SpotCheckMismatchIgnore ignoreStatus) {
        String spotcheckIndex = getSpotcheckIndexName(dataSource, contentType);
        try {
            UpdateResponse response = getUpdateRequest(spotcheckIndex, observationType, String.valueOf(mismatchId))
                    .setDoc(jsonBuilder()
                            .startObject()
                            .field("mismatchIgnore", ignoreStatus)
                            .endObject())
                    .execute()
                    .actionGet();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void addIssueId(SpotCheckDataSource dataSource, SpotCheckContentType contentType, int mismatchId, String issueId) {
        String spotcheckIndex = getSpotcheckIndexName(dataSource, contentType);
        GetResponse response = getRequest(spotcheckIndex, observationType, String.valueOf(mismatchId))
                .execute().actionGet();
        List<String> issueIds = (List<String>) response.getSource().get("issueIds");
        issueIds.add(issueId);
        try {

            UpdateResponse updateResponse = getUpdateRequest(spotcheckIndex, observationType, String.valueOf(mismatchId))
                    .setDoc(jsonBuilder()
                            .startObject()
                            .array("issueIds", issueIds.toArray())
                            .endObject())
                    .execute()
                    .actionGet();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deleteIssueId(SpotCheckDataSource dataSource, SpotCheckContentType contentType, int mismatchId, String issueId) {
        String spotcheckIndex = getSpotcheckIndexName(dataSource, contentType);
        GetResponse response = getRequest(spotcheckIndex, observationType, String.valueOf(mismatchId))
                .execute().actionGet();
        List<String> issueIds = (List<String>) response.getSource().get("issueIds");
        issueIds.remove(issueId);
        try {
            UpdateResponse updateResponse = getUpdateRequest(spotcheckIndex, observationType, String.valueOf(mismatchId))
                    .setDoc(jsonBuilder()
                            .startObject()
                            .array("issueIds", issueIds.toArray())
                            .endObject())
                    .execute()
                    .actionGet();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected List<String> getIndices() {
        return Arrays.stream(SpotCheckIndex.values())
                .map(SpotCheckIndex::getIndexName)
                .collect(Collectors.toList());
    }

    /**
     * --- Internal Methods ---
     */


    private String[] getSpotcheckIndexNames(SpotCheckDataSource dataSource, Set<SpotCheckContentType> contentTypes) {
        return SpotCheckIndex.getIndices(dataSource, contentTypes)
                .stream()
                .map(SpotCheckIndex::getIndexName)
                .toArray(String[]::new);
    }

    private String getSpotcheckIndexName(SpotCheckDataSource dataSource, SpotCheckContentType contentType) {
        return SpotCheckIndex.getIndex(dataSource, contentType).getIndexName();
    }

    private String[] getSpotcheckIndexNames(Map<SpotCheckDataSource, Set<SpotCheckContentType>> dataSourceSetMap) {
        return SpotCheckIndex.getIndices(dataSourceSetMap)
                .stream()
                .map(SpotCheckIndex::getIndexName)
                .toArray(String[]::new);
    }

    /**
     * Looks at past observations for each checked content key and sets mismatch statuses in the current report
     * according to their presence in previous reports
     */
    private List<ElasticObservation> setMismatchStatuses(String spotcheckIndex, List<ElasticObservation> elasticObservations) {
        List<ElasticObservation> observations = new ArrayList<>();
        elasticObservations.forEach(elasticObservation -> {
            Map<String, String> observationKeyMap = elasticObservation.getObservationKey();
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
            observationKeyMap.forEach((k, v) -> {
                if (!v.isEmpty()) {
                    MatchQueryBuilder matchQuery = matchQuery("observationKey." + k, v);
                    boolQuery.must(matchQuery);
                }
            });
            BoolQueryBuilder filterQuery = QueryBuilders.boolQuery();
            filterQuery.must(matchQuery("mismatchType", elasticObservation.getMismatchType().toString()));
            /*if(!elasticObservation.getObservedData().isEmpty())
                filterQuery.must(matchQuery("observedData", elasticObservation.getObservedData()));
            if(!elasticObservation.getReferenceData().isEmpty())
                filterQuery.must(matchQuery("referenceData", elasticObservation.getReferenceData()));*/
            boolQuery.filter(filterQuery);
            SearchResponse searchResponse = searchClient.prepareSearch()
                    .setIndices(spotcheckIndex).setTypes(observationType)
                    .setQuery(boolQuery)
                    .execute().actionGet();
            if (searchResponse.getHits().totalHits() > 0) {
                elasticObservation.setMismatchStatus(SpotCheckMismatchStatus.EXISTING);
            }
            observations.add(elasticObservation);
        });
        return observations;
    }

    private <ContentKey> List<ElasticObservation> toElasticObservations(String reportId,
                                                                        List<SpotCheckObservation<ContentKey>> observations) {
        List<ElasticObservation> elasticObservations = new ArrayList<>();
        SpotCheckContentType contentType = observations.get(0).getReferenceId().getReferenceType().getContentType();
        SpotCheckDataSource dataSource = observations.get(0).getReferenceId().getReferenceType().getDataSource();
        observations.forEach(observation -> {
            observation.getMismatches().values().forEach(spotCheckMismatch -> {
                elasticObservations.add(new ElasticObservation(reportId, contentIdMapperTable
                        .get(dataSource, contentType)
                        .getMapFromKey(observation.getKey()),
                        observation, spotCheckMismatch));
            });
        });
        return elasticObservations;
    }

    private <ContentKey> Map<ContentKey, SpotCheckObservation<ContentKey>> getObservationsForReport(String spotcheckIndex, String reportId) {
        QueryBuilder query = matchQuery("spotcheckReportId", reportId);
        String[] spotcheckIndices = {spotcheckIndex};
        SearchResponse observationSearchResponse = getSearchRequest(spotcheckIndices, query,
                null, null, true, 0, observationType)
                .execute().actionGet();
        Map<ContentKey, SpotCheckObservation<ContentKey>> observationMap = new HashMap<>();
        do {

            observationSearchResponse.getHits().forEach(observation -> {
                SpotCheckObservation<ContentKey> spotcheckObservation = getSpotcheckObservationFrom(observation);
                if (observationMap.containsKey(spotcheckObservation.getKey())) {
                    SpotCheckObservation spotCheckObservation = observationMap.get(spotcheckObservation.getKey());
                    spotcheckObservation.getMismatches().putAll(spotCheckObservation.getMismatches());
                }
                observationMap.put(spotcheckObservation.getKey(), spotcheckObservation);
            });

            observationSearchResponse = searchClient.prepareSearchScroll(observationSearchResponse.getScrollId())
                    .setScroll(new TimeValue(60000))
                    .execute()
                    .actionGet();
        }
        while (observationSearchResponse.getHits().getHits().length != 0);
        return observationMap;
    }

    private <ContentKey> List<SpotCheckReportSummary> getSpotcheckReportSummary(SearchResponse reportSearchResponse) {
        List<SpotCheckReportSummary> spotCheckReportSummaries = new ArrayList<>();
        do {
            reportSearchResponse.getHits().forEach(report -> {
                String reportId = report.getId();
                String reportIndex = report.getIndex();
                SpotCheckReport<ContentKey> spotcheckReport = getSpotcheckReportFrom(report.getSource());
                Map<ContentKey, SpotCheckObservation<ContentKey>> observationMap =
                        getObservationsForReport(reportIndex, reportId);
                spotcheckReport.setObservations(observationMap);
                SpotCheckReportSummary summary = spotcheckReport.getSummary();
                summary.addCountsFromObservations(observationMap.values());
                spotCheckReportSummaries.add(spotcheckReport.getSummary());
            });

            reportSearchResponse = searchClient.prepareSearchScroll(reportSearchResponse.getScrollId())
                    .setScroll(new TimeValue(60000))
                    .execute()
                    .actionGet();
        }
        while (reportSearchResponse.getHits().getHits().length != 0);
        return spotCheckReportSummaries;
    }
    private <ContentKey> SpotCheckObservation<ContentKey> getSpotcheckObservationFrom(SearchHit observation){
        ElasticObservation elasticObservation = getElasticObservationFrom(observation);
        return elasticObservation
                .toSpotCheckObservation(
                        (ContentKey) contentIdMapperTable
                                .get(elasticObservation.getReferenceId().getReferenceType().getDataSource(),
                                        elasticObservation.getReferenceId().getReferenceType().getContentType())
                        .getKeyFromMap(elasticObservation.getObservationKey()),
                        observation.getId());
    }

    private <ContentKey> ElasticObservation getElasticObservationFrom(SearchHit observation){
        Map<String, Object> observationObjectMap = observation.getSource();
        TypeReference<ElasticObservation> elasticObservationTypeReference = new TypeReference<ElasticObservation>(){};
        return OutputUtils.getJsonMapper()
                .convertValue(observationObjectMap, elasticObservationTypeReference);
    }

    private <ContentKey> SpotCheckReport<ContentKey> getSpotcheckReportFrom(Map<String, Object> objectMap){
        TypeReference<ElasticReport> elasticReportTypeReference =
                new TypeReference<ElasticReport>(){};

        ElasticReport elasticReport = OutputUtils.getJsonMapper()
                .convertValue(objectMap,elasticReportTypeReference);

        return elasticReport.toSpotCheckReport();
    }

    private <ContentKey> Map<SpotCheckRefType, Collection<SpotCheckObservation<ContentKey>>> getRefTypeObservationMap(SearchResponse response){
        Map<SpotCheckRefType, Collection<SpotCheckObservation<ContentKey>>> observationsMap = new HashMap<>();
        do {
            response.getHits().forEach(observation -> {
                SpotCheckObservation<ContentKey> spotcheckObservation = getSpotcheckObservationFrom(observation);
                Collection<SpotCheckObservation<ContentKey>> observations =
                        observationsMap.get(spotcheckObservation.getReferenceId().getReferenceType());
                if (observations == null)
                    observations = new ArrayList<SpotCheckObservation<ContentKey>>();
                observations.add(spotcheckObservation);
                observationsMap.put(spotcheckObservation.getReferenceId().getReferenceType(), observations);
            });
            response = searchClient.prepareSearchScroll(response.getScrollId())
                    .setScroll(new TimeValue(60000))
                    .execute()
                    .actionGet();
        }
        while (response.getHits().getHits().length != 0);
        return observationsMap;
    }
}

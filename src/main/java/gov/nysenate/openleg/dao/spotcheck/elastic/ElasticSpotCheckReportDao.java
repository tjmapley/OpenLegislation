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
import gov.nysenate.openleg.model.spotcheck.*;
import gov.nysenate.openleg.service.base.search.IndexedSearchService;
import gov.nysenate.openleg.util.OutputUtils;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
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
import java.util.stream.Collectors;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.elasticsearch.index.query.QueryBuilders.*;

/**
 * Created by PKS on 7/25/16.
 */
@Repository
public class ElasticSpotCheckReportDao
        extends ElasticBaseDao
        implements SpotCheckReportDao{

    private static final Logger logger = LoggerFactory.getLogger(ElasticSpotCheckReportDao.class);

    protected static final String reportType = "reports";

    protected static final String observationType = "observations";

    @Autowired
    private List<SpotCheckContentIdMapper> contentIdMappers;

    private static Table<SpotCheckDataSource, SpotCheckContentType, SpotCheckContentIdMapper> contentIdMapperTable = HashBasedTable.create();


    @PostConstruct
    private void init(){
       contentIdMappers.forEach(
               contentIdMapper ->
                       contentIdMapperTable
                               .put(contentIdMapper.getDataSource(), contentIdMapper.getContentType(), contentIdMapper));
    }


    /** --- Implemented Methods --- */

    /** {@inheritDoc} */
    @Override
    public <ContentKey> SpotCheckReport<ContentKey> getReport(SpotCheckReportId Id) throws DataAccessException {
        String spotcheckIndex = getSpotcheckIndexName(Id.getReferenceType().getDataSource(), Id.getReferenceType().getContentType());
        SearchResponse reportSearchResponse = searchClient.prepareSearch()
                    .setIndices(spotcheckIndex)
                    .setTypes(reportType)
                    .setQuery(matchQuery("reportId.reportDateTime", Id.getReportDateTime().toString()))
                    .execute()
                    .actionGet();
        if(reportSearchResponse.getHits().getTotalHits() > 0) {
            try {
                SpotCheckReport<ContentKey> report = getSpotcheckReportFrom(reportSearchResponse.getHits().getAt(0).getSource());
                String reportId = reportSearchResponse.getHits().getAt(0).getId();
                Map<ContentKey, SpotCheckObservation<ContentKey>> observationMap = getObservationsForReport(spotcheckIndex, reportId);
                report.setObservations(observationMap);
                return report;

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        throw new SpotCheckReportNotFoundEx(Id);
    }

    /** {@inheritDoc} */
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

        bulkRequest.add(
                searchClient.prepareIndex()
                        .setIndex(spotcheckIndex)
                        .setType(reportType)
                        .setId(String.valueOf(report.hashCode()))
                        .setSource(elasticReportJson)
        );

        List<ElasticObservation> elasticObservations = setMismatchStatuses(spotcheckIndex,
                toElasticObservations(String.valueOf(report.hashCode()),
                        report.getObservations()
                                .values().stream()
                                .collect(Collectors.toList())));

        elasticObservations.forEach(elasticObservation ->
            bulkRequest.add(
                    searchClient.prepareIndex()
                            .setIndex(spotcheckIndex)
                            .setType(observationType)
                            .setId(elasticObservation.getMismatchId().toString())
                            .setSource(OutputUtils.toJson(elasticObservation))
            )
        );

        bulkRequest.execute().actionGet();
    }

    @Override
    public <ContentKey> List<SpotCheckReportSummary> getReportSummaries(SpotCheckRefType refType, LocalDateTime start,
                                                           LocalDateTime end, gov.nysenate.openleg.dao.base.SortOrder dateOrder) {
        String spotcheckIndex = getSpotcheckIndexName(refType.getDataSource(), refType.getContentType());
        SearchResponse reportSearchResponse = searchClient.prepareSearch()
                .setIndices(spotcheckIndex)
                .setTypes(reportType)
                .setSize(100)
                .setScroll(new TimeValue(60000))
                .setQuery(rangeQuery("reportId.reportDateTime").from(start.toString()).to(end.toString()))
                .addSort(SortBuilders
                                .fieldSort("reportId.reportDateTime")
                                .order(SortOrder.valueOf(dateOrder.toString()))
                                .unmappedType("date")
                )
                .execute()
                .actionGet();
        List<SpotCheckReportSummary> spotCheckReportSummaries = new ArrayList<>();
        do {
            reportSearchResponse.getHits().forEach(report -> {
                String reportId = report.getId();
                SpotCheckReport<ContentKey> spotcheckReport = getSpotcheckReportFrom(report.getSource());
                Map<ContentKey, SpotCheckObservation<ContentKey>> observationMap =
                        getObservationsForReport(spotcheckIndex, reportId);
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

    @Override
    public <ContentKey> SpotCheckOpenMismatches<ContentKey> getOpenMismatches(OpenMismatchQuery query) {

        String[] spotCheckIndexes = query.getRefTypes().stream().map(SpotCheckRefType::getRefName).toArray(String[]::new);

        BoolQueryBuilder queryFilters = QueryBuilders.boolQuery();
        query.getMismatchTypes().forEach(spotCheckMismatchType -> {
            queryFilters.should(QueryBuilders.matchQuery("mismatchType", spotCheckMismatchType.toString()));
        });
        SearchResponse searchResponse = searchClient.prepareSearch()
                .setIndices(spotCheckIndexes)
                .setTypes(observationType)
                .setQuery(
                        rangeQuery("observedDateTime")
                                .from(query.getObservedAfter().toString())
                                .to(query.getObservedBefore().toString())
                )
                .setPostFilter(queryFilters)
                .addSort(query.getOrderBy().getColName(), SortOrder.valueOf(query.getOrder().toString()))
                .setScroll(new TimeValue(60000))
                .setSize(query.getLimitOffset().getLimit())
                .execute().actionGet();
        Map<ContentKey, SpotCheckObservation<ContentKey>> observations = new HashMap<>();
        List<Integer> countList = new ArrayList<>();
        Integer offset = query.getLimitOffset().getOffsetStart();
        Integer scroll = 1;
        do {
            if (offset.equals(scroll)) {
                searchResponse.getHits().forEach(observation -> {
                    SpotCheckObservation<ContentKey> spotcheckObservation = getSpotcheckObservationFrom(observation);
                    countList.add(spotcheckObservation.getMismatches().values().size());
                    observations.put(spotcheckObservation.getKey(), spotcheckObservation);
                });
            }
            scroll += 10;
            searchResponse = searchClient.prepareSearchScroll(searchResponse.getScrollId())
                    .setScroll(new TimeValue(60000))
                    .execute()
                    .actionGet();
        }
        while(searchResponse.getHits().getHits().length != 0 || observations.size() != query.getLimitOffset().getLimit());
        Integer totalMismatches = countList.stream().mapToInt(Integer::intValue).sum();
        return new SpotCheckOpenMismatches<>(query.getRefTypes(),observations,totalMismatches);
    }

    @Override
    public <ContentKey> SpotCheckOpenMismatches<ContentKey> getOpenMismatches(SpotCheckDataSource dataSource, SpotCheckContentType contentType, OpenMismatchQuery query) {
        List<SpotCheckRefType> spotCheckRefTypes = SpotCheckRefType.get(dataSource, contentType);
        query.setRefTypes(Sets.newHashSet(spotCheckRefTypes));
        return getOpenMismatches(query);
    }

    @Override
    public <ContentKey> OpenMismatchSummary getOpenMismatchSummary(SpotCheckRefType refType, LocalDateTime observedAfter) {
        String index = refType.getRefName();
        SearchResponse searchResponse = searchClient.prepareSearch()
                .setIndices(index)
                .setTypes(observationType)
                .setQuery(QueryBuilders.boolQuery()
                                .must(rangeQuery("observedDateTime").from(observedAfter.toString()))
                )
                .setScroll(new TimeValue(60000))
                .setSize(100)
                .execute().actionGet();
        List<SpotCheckObservation<ContentKey>> observations = new ArrayList<>();
        do {
            searchResponse.getHits().forEach(observation -> {
                SpotCheckObservation<ContentKey> spotcheckObservation = getSpotcheckObservationFrom(observation);
                observations.add(spotcheckObservation);
            });
            searchResponse = searchClient.prepareSearchScroll(searchResponse.getScrollId())
                    .setScroll(new TimeValue(60000))
                    .execute()
                    .actionGet();
        }
        while(searchResponse.getHits().getHits().length != 0);

        OpenMismatchSummary openMismatchSummary = new OpenMismatchSummary(refType,observedAfter);
        openMismatchSummary.getRefTypeSummary(refType).addCountsFromObservations(observations);
        return openMismatchSummary;
    }


    @Override
    public <ContentKey> OpenMismatchSummary getOpenMismatchSummary(Set<SpotCheckRefType> refTypes, LocalDateTime observedAfter) {
        String[] spotCheckIndexes = refTypes.stream().map(SpotCheckRefType::getRefName).toArray(String[]::new);
        SearchResponse searchObservationsResponse = searchClient.prepareSearch()
                .setIndices(spotCheckIndexes)
                .setTypes(observationType)
                .setQuery(QueryBuilders.boolQuery()
                        .must(rangeQuery("observedDateTime").from(observedAfter.toString()))
                )
                .setScroll(new TimeValue(60000))
                .setSize(100)
                .execute().actionGet();
        Map<SpotCheckRefType, Collection<SpotCheckObservation<ContentKey>>>observationsMap = new HashMap<>();
        do {
            searchObservationsResponse.getHits().forEach(observation -> {
                SpotCheckObservation<ContentKey> spotcheckObservation = getSpotcheckObservationFrom(observation);
                Collection<SpotCheckObservation<ContentKey>> observations =
                        observationsMap.get(spotcheckObservation.getReferenceId().getReferenceType());
                if (observations == null)
                    observations = new ArrayList<SpotCheckObservation<ContentKey>>();
                observations.add(spotcheckObservation);
                observationsMap.put(spotcheckObservation.getReferenceId().getReferenceType(), observations);
            });
            searchObservationsResponse = searchClient.prepareSearchScroll(searchObservationsResponse.getScrollId())
                    .setScroll(new TimeValue(60000))
                    .execute()
                    .actionGet();
        }
        while (searchObservationsResponse.getHits().getHits().length != 0);
        OpenMismatchSummary openMismatchSummary = new OpenMismatchSummary(refTypes, observedAfter);
        observationsMap.forEach((refType, observations) ->{
            openMismatchSummary.getRefTypeSummary(refType).addCountsFromObservations(observations);
        });
        return openMismatchSummary;
    }


    @Override
    public void deleteReport(SpotCheckReportId reportId) {
        String spotcheckIndex = getSpotcheckIndexName(reportId.getReferenceType().getDataSource(),
                reportId.getReferenceType().getContentType());
        SearchResponse reportSearchResponse = searchClient.prepareSearch()
                .setIndices(spotcheckIndex)
                .setTypes(reportType)
                .setQuery(matchQuery("reportId.reportDateTime", reportId.getReportDateTime()))
                .execute()
                .actionGet();
        if(reportSearchResponse.getHits().getTotalHits() > 0){
            String id = reportSearchResponse.getHits().getAt(0).getId();
            searchClient.prepareDelete()
                    .setIndex(spotcheckIndex)
                    .setType(reportType)
                    .setId(id)
                    .execute()
                    .actionGet();
        }
    }

    @Override
    public void setMismatchIgnoreStatus(int mismatchId, SpotCheckMismatchIgnore ignoreStatus) {
        try {
            String spotcheckIndex = "";
            UpdateRequest updateRequest = new UpdateRequest(spotcheckIndex, observationType, String.valueOf(mismatchId))
                    .doc(jsonBuilder()
                            .startObject()
                            .field("mismatchIgnore", ignoreStatus)
                            .endObject());
            searchClient.update(updateRequest).actionGet();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void addIssueId(int mismatchId, String issueId) {
        String spotcheckIndex = "";
        GetResponse response = searchClient.prepareGet(spotcheckIndex, observationType, String.valueOf(mismatchId))
                .execute().actionGet();
        List<String> issueIds = (List<String>) response.getSource().get("issueIds");
        issueIds.add(issueId);
        try {
            UpdateRequest updateRequest = new UpdateRequest(spotcheckIndex, observationType, String.valueOf(mismatchId))
                    .doc(jsonBuilder()
                            .startObject()
                            .array("issueIds",issueIds.toArray())
                            .endObject());
            searchClient.update(updateRequest).actionGet();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deleteIssueId(int mismatchId, String issueId) {
        String spotcheckIndex = "";
        GetResponse response = searchClient.prepareGet(spotcheckIndex,observationType, String.valueOf(mismatchId))
                .execute().actionGet();
        List<String> issueIds = (List<String>) response.getSource().get("issueIds");
        issueIds.remove(issueId);
        try {
            UpdateRequest updateRequest = new UpdateRequest(spotcheckIndex, observationType, String.valueOf(mismatchId))
                    .doc(jsonBuilder()
                            .startObject()
                            .array("issueIds",issueIds.toArray())
                            .endObject());
            searchClient.update(updateRequest).actionGet();
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

    /** --- Internal Methods --- */


    private String getSpotcheckIndexName(SpotCheckDataSource dataSource, SpotCheckContentType contentType){
        return SpotCheckIndex
                .valueOf(dataSource, contentType)
                .getIndexName();
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
            observationKeyMap.forEach((k, v) ->{
                if (!v.isEmpty())  {
                MatchQueryBuilder matchQuery = matchQuery("observationKey."+ k, v);
                boolQuery.must(matchQuery);}
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
            if(searchResponse.getHits().totalHits() > 0){
                elasticObservation.setMismatchStatus(SpotCheckMismatchStatus.EXISTING);
            }
            observations.add(elasticObservation);
        });
        return observations;
    }

    private <ContentKey> List<ElasticObservation> toElasticObservations(String reportId,
                                                                        List<SpotCheckObservation<ContentKey>> observations){
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

    private <ContentKey> Map<ContentKey, SpotCheckObservation<ContentKey>> getObservationsForReport(String spotcheckIndex, String reportId){
        SearchResponse observationSearchResponse = searchClient.prepareSearch()
                .setIndices(spotcheckIndex)
                .setTypes(observationType)
                .setQuery(matchQuery("spotcheckReportId", reportId))
                .setScroll(new TimeValue(60000))
                .setSize(100)
                .execute()
                .actionGet();
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
}

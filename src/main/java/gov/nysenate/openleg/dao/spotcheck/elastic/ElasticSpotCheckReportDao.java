package gov.nysenate.openleg.dao.spotcheck.elastic;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.*;
import gov.nysenate.openleg.dao.base.ElasticBaseDao;
import gov.nysenate.openleg.dao.base.SpotCheckIndex;
import gov.nysenate.openleg.dao.base.ElasticSpotCheckType;
import gov.nysenate.openleg.dao.spotcheck.SpotCheckContentIdMapper;
import gov.nysenate.openleg.dao.spotcheck.SpotCheckReportDao;
import gov.nysenate.openleg.model.search.SearchResults;
import gov.nysenate.openleg.model.spotcheck.*;
import gov.nysenate.openleg.util.OutputUtils;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.*;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.filter.Filter;
import org.elasticsearch.search.aggregations.bucket.range.Range;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.support.IncludeExclude;
import org.elasticsearch.search.aggregations.metrics.tophits.TopHits;
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
import java.util.stream.IntStream;

import static gov.nysenate.openleg.dao.base.ElasticSpotCheckType.OBSERVATION;
import static gov.nysenate.openleg.dao.base.ElasticSpotCheckType.REPORT;
import static java.util.Arrays.asList;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.elasticsearch.index.query.QueryBuilders.*;
import static org.elasticsearch.search.aggregations.AggregationBuilders.*;

/**
 * Created by PKS on 7/25/16.
 */
@Repository
public class ElasticSpotCheckReportDao
        extends ElasticBaseDao
        implements SpotCheckReportDao {

    private static final Logger logger = LoggerFactory.getLogger(ElasticSpotCheckReportDao.class);

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
        Set<String> spotcheckIndices = Sets.newHashSet(getSpotcheckIndexName(Id.getReferenceType().getDataSource(),
                Id.getReferenceType().getContentType()));
        Set<String> types = Collections.singleton(REPORT.getName());
        QueryBuilder query = (Id.getReportDateTime() != null) ?
                matchQuery("reportId.reportDateTime", Id.getReportDateTime().toString())
                : matchQuery("reportId.referenceDateTime", Id.getReferenceDateTime().toString());

        SortBuilder sortBuilder = SortBuilders
                .fieldSort("reportId.reportDateTime")
                .order(SortOrder.DESC);

        SearchResponse reportSearchResponse = getSearchRequest(spotcheckIndices, types, query,
                null, Collections.singletonList(sortBuilder), false, 0)
                .execute()
                .actionGet();

        if (reportSearchResponse.getHits().getTotalHits() > 0) {
            try {
                SpotCheckReport<ContentKey> report = getSpotcheckReport(reportSearchResponse.getHits().getAt(0));
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

        List<ElasticObservation> elasticObservations = setMismatchStatuses(report);

        ElasticReport elasticReport = new ElasticReport(report);

        String elasticReportJson = OutputUtils.toJson(elasticReport);

        BulkRequestBuilder bulkRequest = searchClient.prepareBulk();

        String spotcheckIndex = getSpotcheckIndexName(report.getReportId().getReferenceType().getDataSource(),
                report.getReportId().getReferenceType().getContentType());

        bulkRequest.add(getIndexRequest(spotcheckIndex, REPORT.getName(), String.valueOf(report.hashCode()))
                .setSource(elasticReportJson));

        elasticObservations.forEach(elasticObservation ->
                bulkRequest.add(
                        getIndexRequest(spotcheckIndex, OBSERVATION.getName(),
                                elasticObservation.getMismatchId().toString())
                                .setSource(OutputUtils.toJson(elasticObservation))
                )
        );

        safeBulkRequestExecute(bulkRequest);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <ContentKey> List<SpotCheckReportSummary> getReportSummaries(Map<SpotCheckDataSource, Set<SpotCheckContentType>> dataSourceSetMap,
                                                                        LocalDateTime start,
                                                                        LocalDateTime end,
                                                                        gov.nysenate.openleg.dao.base.SortOrder dateOrder) {
        Set<String> spotcheckIndices = getSpotcheckIndexNames(dataSourceSetMap);
        Set<String> spotchecKTypes = Collections.singleton(REPORT.getName());
        QueryBuilder query = rangeQuery("reportId.reportDateTime").from(start.toString()).to(end.toString());
        SortBuilder sort = SortBuilders
                .fieldSort("reportId.reportDateTime")
                .order(SortOrder.valueOf(dateOrder.toString()))
                .unmappedType("date");
        SearchResponse reportSearchResponse = getSearchRequest(spotcheckIndices, spotchecKTypes, query,
                null, Collections.singletonList(sort), true, 0)
                .execute().actionGet();
        List<SpotCheckReportSummary> spotCheckReportSummaries = getSearchResults(reportSearchResponse, null,
                this::getSpotcheckReportSummary).getRawResults().stream().collect(Collectors.toList());

        return spotCheckReportSummaries;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <ContentKey> List<SpotCheckReportSummary> getReportSummaries(SpotCheckRefType refType, LocalDateTime start,
                                                                        LocalDateTime end, gov.nysenate.openleg.dao.base.SortOrder dateOrder) {
        Set<String> spotcheckIndices;
        if (refType == null)
            spotcheckIndices = Sets.newHashSet(getIndices());
        else
            spotcheckIndices = Collections.singleton(getSpotcheckIndexName(refType.getDataSource(), refType.getContentType()));

        Set<String> types = Sets.newHashSet(REPORT.getName());
        QueryBuilder query = rangeQuery("reportId.reportDateTime").from(start.toString()).to(end.toString());

        SortBuilder sort = SortBuilders
                .fieldSort("reportId.reportDateTime")
                .order(SortOrder.valueOf(dateOrder.toString()))
                .unmappedType("date");
        SearchResponse reportSearchResponse = getSearchRequest(spotcheckIndices, types, query,
                null, Collections.singletonList(sort), true, 0)
                .execute()
                .actionGet();
        List<SpotCheckReportSummary> spotCheckReportSummaries = getSearchResults(reportSearchResponse, null,
                this::getSpotcheckReportSummary).getRawResults().stream().collect(Collectors.toList());
        return spotCheckReportSummaries;
    }

    /**
     * {@inheritDoc}
     */
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

        QueryBuilder resolvedMismatchQuery = matchQuery("mismatchStatus", SpotCheckMismatchStatus.RESOLVED.toString());

        QueryBuilder regressionMismatchQuery = matchQuery("mismatchStatus", SpotCheckMismatchStatus.REGRESSION.toString());

        SearchRequestBuilder resolvedRequest = searchClient.prepareSearch()
                .setIndices(indices.stream().toArray(String[]::new))
                .setTypes(ElasticSpotCheckType.OBSERVATION.getName())
                .setQuery(resolvedMismatchQuery)
                .addSort("observedDateTime", SortOrder.DESC)
                .setSize(10000)
                .setFetchSource("groupByKey","");

        SearchRequestBuilder regressionRequest = searchClient.prepareSearch()
                .setIndices(indices.stream().toArray(String[]::new))
                .setTypes(ElasticSpotCheckType.OBSERVATION.getName())
                .setQuery(regressionMismatchQuery)
                .addSort("observedDateTime", SortOrder.DESC)
                .setSize(10000)
                .setFetchSource("groupByKey","");

        MultiSearchRequest preAggSearch = searchClient.prepareMultiSearch()
                .add(resolvedRequest)
                .add(regressionRequest).request();

        MultiSearchResponse searchResponse = searchClient.multiSearch(preAggSearch).actionGet();

        List<Set<String>> observationKeys = new ArrayList<>();

        searchResponse.forEach(item -> {
            if(item.getResponse().getHits().getTotalHits() > 0)
                observationKeys.add(Arrays.stream(item.getResponse().getHits().getHits())
                    .map(SearchHit::sourceAsMap)
                    .map(this::getObjectKey)
                    .collect(Collectors.toSet()));
        });

        Set<String> resolvedAndNotRegression = Sets.difference(observationKeys.get(0), observationKeys.get(1));

        String[] resolvedMismatchKeys = resolvedAndNotRegression.stream().toArray(String[]::new);

        IncludeExclude includeExclude = new IncludeExclude(null, resolvedMismatchKeys);

        QueryBuilder filterQuery = boolQuery().must(
                rangeQuery("observedDateTime")
                        .from(query.getObservedAfter())
                        .to(query.getObservedAfter())
        ).mustNot(
                boolQuery().must(
                        rangeQuery("observedDateTime")
                                .from(query.getObservedAfter())
                                .to(query.getObservedBefore().minusDays(1))
                ).must(termQuery("status", "RESOLVED"))
        );
        AggregationBuilder aggregationBuilder = filter("Filter",filterQuery)
                .subAggregation(
                        terms("Grouping").field("groupByKey")
                                .includeExclude(includeExclude)
                                .size(1000)
                                .subAggregation(
                                        topHits("GetHits")
                                                .size(1)
                                )
                );

        SortBuilder sort = SortBuilders.fieldSort(query.getOrderBy().getColName())
                .order(SortOrder.valueOf(query.getOrder().toString()));

        SearchResponse response = searchClient.prepareSearch()
                .setIndices(indices.stream().toArray(String[]::new))
                .setTypes(ElasticSpotCheckType.OBSERVATION.getName())
                .addAggregation(aggregationBuilder)
                .execute()
                .actionGet();

        List<SearchHit> searchHits = new ArrayList<>();


        Filter filterAgg = response.getAggregations().get("Filter");

        Terms termsAgg = filterAgg.getAggregations().get("Grouping");

        termsAgg.getBuckets().forEach(term ->{
            TopHits topHits = term.getAggregations().get("GetHits");
                searchHits.add(topHits.getHits().getAt(0));
        });

        List<SearchHit> hits = IntStream.range(query.getLimitOffset().getOffsetStart() - 1,
                query.getLimitOffset().getOffsetEnd())
                .mapToObj(searchHits::get)
                .collect(Collectors.toList());

        SearchResults<SpotCheckObservation<ContentKey>> searchResults =
                getSearchResults(searchHits, query.getLimitOffset(),
                        this::getSpotcheckObservation);

        Map<ContentKey, SpotCheckObservation<ContentKey>> observationMap = searchResults.getRawResults().stream()
                        .collect(Collectors.toMap(SpotCheckObservation::getKey, Function.identity()));

        return new SpotCheckOpenMismatches<>(query.getRefTypes(), observationMap, searchHits.size());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <ContentKey> SpotCheckOpenMismatches<ContentKey> getOpenMismatches(SpotCheckDataSource dataSource, SpotCheckContentType contentType, OpenMismatchQuery query) {
        List<SpotCheckRefType> spotCheckRefTypes = SpotCheckRefType.get(dataSource, contentType);
        query.setRefTypes(Sets.newHashSet(spotCheckRefTypes));
        return getOpenMismatches(query);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <ContentKey> OpenMismatchSummary getOpenMismatchSummary(Map<SpotCheckDataSource, Set<SpotCheckContentType>> dataSourceSetMap,
                                                                   LocalDateTime observedAfter, LocalDateTime observedBefore) {
        Set<String> spotcheckIndexes = SpotCheckIndex.getIndices(dataSourceSetMap).stream()
                .map(SpotCheckIndex::getIndexName)
                .collect(Collectors.toSet());
        QueryBuilder query = QueryBuilders.boolQuery()
                .must(rangeQuery("observedDateTime")
                        .from(observedAfter.toString())
                        .to(observedBefore.toString()));

        Set<String> types = Sets.newHashSet(OBSERVATION.getName());
        SearchResponse response = getSearchRequest(spotcheckIndexes, types, query,
                null, null, true, 0)
                .execute()
                .actionGet();
        Map<SpotCheckRefType, Collection<SpotCheckObservation<ContentKey>>> observationsMap = getRefTypeObservationMap(response);
        Set<SpotCheckRefType> refTypes = new HashSet<>();
        SpotCheckIndex.getIndices(dataSourceSetMap).forEach(spotCheckIndex ->
                refTypes.addAll(SpotCheckRefType.get(spotCheckIndex.getDataSource(), spotCheckIndex.getContentType()))
        );
        OpenMismatchSummary openMismatchSummary = new OpenMismatchSummary(refTypes, observedAfter, observedBefore);
        observationsMap.forEach((refType, observations) ->
            openMismatchSummary.getRefTypeSummary(refType).addCountsFromObservations(observations)
        );
        return openMismatchSummary;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <ContentKey> OpenMismatchSummary getOpenMismatchSummary(Set<SpotCheckRefType> refTypes, LocalDateTime observedAfter, LocalDateTime observedBefore) {
        Set<String> spotCheckIndexes = refTypes.stream()
                .map(SpotCheckIndex::getIndex)
                .map(SpotCheckIndex::getIndexName)
                .collect(Collectors.toSet());
        Set<String> types = Sets.newHashSet(OBSERVATION.getName());

        QueryBuilder query = boolQuery().must(
                rangeQuery("observedDateTime")
                        .from(observedAfter.toString())
                        .to(observedBefore.toString())
        );
        SearchResponse searchObservationsResponse = getSearchRequest(spotCheckIndexes, types, query, null, null, true,0)
                .execute()
                .actionGet();

        Map<SpotCheckRefType, Collection<SpotCheckObservation<ContentKey>>> observationsMap = getRefTypeObservationMap(searchObservationsResponse);
        OpenMismatchSummary openMismatchSummary = new OpenMismatchSummary(refTypes, observedAfter, observedBefore);
        observationsMap.forEach((refType, observations) ->
            openMismatchSummary.getRefTypeSummary(refType).addCountsFromObservations(observations)
        );
        return openMismatchSummary;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteReport(SpotCheckReportId reportId) {
        Set<String> spotcheckIndices = Sets.newHashSet(getSpotcheckIndexName(reportId.getReferenceType().getDataSource(),
                reportId.getReferenceType().getContentType()));
        QueryBuilder query = matchQuery("reportId.reportDateTime", reportId.getReportDateTime());
        Set<String> types = Sets.newHashSet(REPORT.getName());
        SearchResponse reportSearchResponse = getSearchRequest(spotcheckIndices, types, query, null, null, false, 0)
                .execute()
                .actionGet();
        if (reportSearchResponse.getHits().getTotalHits() > 0) {
            String Id = reportSearchResponse.getHits().getAt(0).getId();
            String reportIndex = reportSearchResponse.getHits().getAt(0).getIndex();
            QueryBuilder queryBuilder = matchQuery("spotcheckReportId", Id);
            SearchResponse searchResponse = getSearchRequest(Collections.singleton(reportIndex),
                    Collections.singleton(OBSERVATION.getName()),
                    queryBuilder, null,null,false,0
            ).execute().actionGet();
            List<String> observationIds = new ArrayList<>();
            searchResponse.getHits().forEach(observationHit -> observationIds.add(observationHit.getId()));
            deleteEntry(reportIndex, REPORT.getName(), Id);
            observationIds.forEach(observationId -> deleteEntry(reportIndex, OBSERVATION.getName(), observationId));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setMismatchIgnoreStatus(SpotCheckDataSource dataSource, SpotCheckContentType contentType,
                                        int mismatchId, SpotCheckMismatchIgnore ignoreStatus) {
        String spotcheckIndex = getSpotcheckIndexName(dataSource, contentType);
        try {
            UpdateResponse response = getUpdateRequest(spotcheckIndex, OBSERVATION.getName(), String.valueOf(mismatchId))
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

    /**
     * {@inheritDoc}
     */
    @Override
    public void addIssueId(SpotCheckDataSource dataSource, SpotCheckContentType contentType, int mismatchId, String issueId) {
        String spotcheckIndex = getSpotcheckIndexName(dataSource, contentType);
        GetResponse response = getRequest(spotcheckIndex, OBSERVATION.getName(), String.valueOf(mismatchId))
                .execute().actionGet();
        List<String> issueIds = (List<String>) response.getSource().get("issueIds");
        issueIds.add(issueId);
        try {

            UpdateResponse updateResponse = getUpdateRequest(spotcheckIndex, OBSERVATION.getName(), String.valueOf(mismatchId))
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

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteIssueId(SpotCheckDataSource dataSource, SpotCheckContentType contentType, int mismatchId, String issueId) {
        String spotcheckIndex = getSpotcheckIndexName(dataSource, contentType);
        GetResponse response = getRequest(spotcheckIndex, OBSERVATION.getName(), String.valueOf(mismatchId))
                .execute().actionGet();
        List<String> issueIds = (List<String>) response.getSource().get("issueIds");
        issueIds.remove(issueId);
        try {
            UpdateResponse updateResponse = getUpdateRequest(spotcheckIndex, OBSERVATION.getName(), String.valueOf(mismatchId))
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

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<String> getIndices() {
        return Arrays.stream(SpotCheckIndex.values())
                .map(SpotCheckIndex::getIndexName)
                .collect(Collectors.toList());
    }

    /**
     * --- Internal Methods ---
     */


    private Set<String> getSpotcheckIndexNames(SpotCheckDataSource dataSource, Set<SpotCheckContentType> contentTypes) {
        return SpotCheckIndex.getIndices(dataSource, contentTypes)
                .stream()
                .map(SpotCheckIndex::getIndexName)
                .collect(Collectors.toSet());
    }

    private String getSpotcheckIndexName(SpotCheckDataSource dataSource, SpotCheckContentType contentType) {
        return SpotCheckIndex.getIndex(dataSource, contentType).getIndexName();
    }

    private Set<String> getSpotcheckIndexNames(Map<SpotCheckDataSource, Set<SpotCheckContentType>> dataSourceSetMap) {
        return SpotCheckIndex.getIndices(dataSourceSetMap)
                .stream()
                .map(SpotCheckIndex::getIndexName)
                .collect(Collectors.toSet());
    }

    /**
     * Looks at past observations for each checked content key and sets mismatch statuses in the current report
     * according to their presence in previous reports
     */
    private <ContentKey> List<ElasticObservation> setMismatchStatuses(SpotCheckReport<ContentKey> currentReport) {

        SpotCheckReport<ContentKey> previousReport;
        try {
             previousReport = getReport(new SpotCheckReportId(currentReport.getReferenceType(),
                    currentReport.getReferenceDateTime(),
                    null));
        }catch (SpotCheckReportNotFoundEx ex){
            return getElasticObservations(currentReport);
        }

        Map<ContentKey, SpotCheckObservation<ContentKey>> latestObs = previousReport.getObservations();

        currentReport.getObservations().forEach((contentKey, reportObs) -> {
            if (latestObs.containsKey(contentKey)) { // Leave mismatch statuses as NEW if no prior occurrence
                latestObs.get(contentKey).getMismatches().forEach((type, lastMismatch) -> {
                    if (reportObs.getMismatches().containsKey(type)) { // if a past mismatch has occurred in this report
                        SpotCheckMismatch currentMismatch = reportObs.getMismatches().get(type);
                        switch (lastMismatch.getStatus()) {
                            case RESOLVED: currentMismatch.setStatus(SpotCheckMismatchStatus.REGRESSION); break;
                            default: currentMismatch.setStatus(SpotCheckMismatchStatus.EXISTING);
                        }
                        currentMismatch.setIssueIds(lastMismatch.getIssueIds());
                    } else if (!SpotCheckMismatchStatus.RESOLVED.equals(lastMismatch.getStatus())){
                        // If a past mismatch is not represented for this content in this report, it is resolved
                        lastMismatch.setStatus(SpotCheckMismatchStatus.RESOLVED);
                        reportObs.addMismatch(lastMismatch);
                        // Remove ignore status if the mismatch had a status of 'ignore until resolved'
                        if (lastMismatch.getIgnoreStatus() == SpotCheckMismatchIgnore.IGNORE_UNTIL_RESOLVED) {
                            setMismatchIgnoreStatus(currentReport.getReferenceType().getDataSource(),
                                    currentReport.getReferenceType().getContentType(),
                                    lastMismatch.getMismatchId(),
                                    SpotCheckMismatchIgnore.NOT_IGNORED);
                        }
                    }
                    if (lastMismatch.getIgnoreStatus() == SpotCheckMismatchIgnore.IGNORE_ONCE) {
                        setMismatchIgnoreStatus(currentReport.getReferenceType().getDataSource(),
                                currentReport.getReferenceType().getContentType(),
                                lastMismatch.getMismatchId(),
                                SpotCheckMismatchIgnore.NOT_IGNORED);
                    }
                });
            }
        });

        return getElasticObservations(currentReport);
    }

    private <ContentKey> List<ElasticObservation> getElasticObservations(SpotCheckReport<ContentKey> report) {
        List<ElasticObservation> elasticObservations = new ArrayList<>();
        SpotCheckContentType contentType = report.getReferenceType().getContentType();
        SpotCheckDataSource dataSource = report.getReferenceType().getDataSource();
        List<SpotCheckObservation<ContentKey>> observations = report.getObservations().values()
                .stream()
                .collect(Collectors.toList());
        String reportId = String.valueOf(report.hashCode());
        observations.forEach(observation -> {
            observation.getMismatches().values().forEach(spotCheckMismatch -> {
                elasticObservations.add(new ElasticObservation(reportId , contentIdMapperTable
                        .get(dataSource, contentType)
                        .getMapFromKey(observation.getKey()),
                        observation, spotCheckMismatch));
            });
        });
        return elasticObservations;
    }

    private <ContentKey> Map<ContentKey, SpotCheckObservation<ContentKey>> getObservationsForReport(String spotcheckIndex, String reportId) {
        QueryBuilder query = matchQuery("spotcheckReportId", reportId);
        Set<String> spotcheckIndices = Collections.singleton(spotcheckIndex);
        Set<String> types = Collections.singleton(OBSERVATION.getName());
        SearchResponse searchResponse = getSearchRequest(spotcheckIndices, types, query,
                null, null, true, 0)
                .execute()
                .actionGet();
        SearchResults<SpotCheckObservation<ContentKey>> searchResults =
                getSearchResults(searchResponse, null,
                        this::getSpotcheckObservation);

        List<SpotCheckObservation<ContentKey>> observations =
                searchResults.getRawResults().stream()
                        .collect(Collectors.toList());

        Map<ContentKey, SpotCheckObservation<ContentKey>> observationMap = new HashMap<>();
        observations.forEach(observation ->
                observationMap.put(observation.getKey(), observation)
        );
        return observationMap;
    }

    private <ContentKey> SpotCheckReportSummary getSpotcheckReportSummary(SearchHit report) {
        String reportId = report.getId();
        String reportIndex = report.getIndex();
        SpotCheckReport<ContentKey> spotcheckReport = getSpotcheckReport(report);
        Map<ContentKey, SpotCheckObservation<ContentKey>> observationMap =
                   getObservationsForReport(reportIndex, reportId);
        spotcheckReport.setObservations(observationMap);
        return spotcheckReport.getSummary();
    }
    private <ContentKey> SpotCheckObservation<ContentKey> getSpotcheckObservation(SearchHit observation){
        ElasticObservation elasticObservation = getElasticObservationFrom(observation);
        return elasticObservation
                .toSpotCheckObservation(
                        (ContentKey) contentIdMapperTable
                                .get(elasticObservation.getReferenceId().getReferenceType().getDataSource(),
                                        elasticObservation.getReferenceId().getReferenceType().getContentType())
                        .getKeyFromMap(elasticObservation.getObservationKey()),
                        observation.getId());
    }

    private <ContentKey> SpotCheckObservation<ContentKey> mergeObservations(
            Collection<SpotCheckObservation<ContentKey>> obsList) {
        Iterator<SpotCheckObservation<ContentKey>> obsItr = obsList.iterator();
        SpotCheckObservation<ContentKey> baseObs = obsItr.next();
        obsItr.forEachRemaining(obs -> {
            if (!Objects.equals(baseObs.getKey(), obs.getKey())) {
                throw new IllegalStateException("Attempt to merge observations with different keys: " +
                        baseObs.getKey() + "\t" + obs.getKey());
            }
            obs.getMismatches().values().stream()
                    .forEach(baseObs::addMismatch);
        });
        return baseObs;
    }

    private <ContentKey> ElasticObservation getElasticObservationFrom(SearchHit observation){
        Map<String, Object> observationObjectMap = observation.getSource();
        TypeReference<ElasticObservation> elasticObservationTypeReference = new TypeReference<ElasticObservation>(){};
        return OutputUtils.getJsonMapper()
                .convertValue(observationObjectMap, elasticObservationTypeReference);
    }

    private <ContentKey> SpotCheckReport<ContentKey> getSpotcheckReport(SearchHit searchHit){
        Map<String, Object> objectMap = searchHit.getSource();
        TypeReference<ElasticReport> elasticReportTypeReference =
                new TypeReference<ElasticReport>(){};
        ElasticReport elasticReport = OutputUtils.getJsonMapper()
                .convertValue(objectMap, elasticReportTypeReference);

        return elasticReport.toSpotCheckReport();
    }

    private <ContentKey> Map<SpotCheckRefType, Collection<SpotCheckObservation<ContentKey>>> getRefTypeObservationMap(SearchResponse response){
        Map<SpotCheckRefType, Collection<SpotCheckObservation<ContentKey>>> observationsMap = new HashMap<>();
        do {
            response.getHits().forEach(observation -> {
                SpotCheckObservation<ContentKey> spotcheckObservation = getSpotcheckObservation(observation);
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


    private String getObjectKey(Map<String, Object> objectMap){
        return (String) objectMap.get("groupByKey");
    }
}

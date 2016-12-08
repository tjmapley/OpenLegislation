package gov.nysenate.openleg.dao.base;

import gov.nysenate.openleg.model.spotcheck.SpotCheckContentType;
import gov.nysenate.openleg.model.spotcheck.SpotCheckDataSource;
import gov.nysenate.openleg.model.spotcheck.SpotCheckRefType;

import java.util.*;
import java.util.stream.Collectors;

import static gov.nysenate.openleg.model.spotcheck.SpotCheckContentType.*;
import static gov.nysenate.openleg.model.spotcheck.SpotCheckDataSource.*;


/**
 * Created by PKS on 8/24/16.
 */
public enum SpotCheckIndex {
    SENATE_SITE_CALENDAR("senate-site-calendars", NYSENATE_DOT_GOV, CALENDAR),
    SENATE_SITE_AGENDA("senate-site-agendas", NYSENATE_DOT_GOV, AGENDA),
    SENATE_SITE_BILL("senate-site-bills", NYSENATE_DOT_GOV, BILL),
    LDBC_BILL("ldbc-bills", OPENLEG, BILL),
    LDBC_CALENDAR("ldbc-calendars", OPENLEG, CALENDAR),
    LDBC_AGENDA("ldbc-agendas", OPENLEG, AGENDA)
    ;

    String indexName;
    SpotCheckDataSource dataSource;
    SpotCheckContentType contentType;

    SpotCheckIndex(String indexName, SpotCheckDataSource dataSource, SpotCheckContentType contentType) {
        this.indexName = indexName;
        this.dataSource = dataSource;
        this.contentType = contentType;
    }
    public String getIndexName() {
        return indexName;
    }

    public SpotCheckDataSource getDataSource(){
        return dataSource;
    }

    public SpotCheckContentType getContentType(){
        return contentType;
    }

    public static Set<SpotCheckIndex> getIndices(Map<SpotCheckDataSource, Set<SpotCheckContentType>> dataSourceSetMap){
        Set<SpotCheckIndex> indices = new HashSet<>();
        dataSourceSetMap.forEach((dataSource, contentTypes) ->
            contentTypes.forEach(contentType -> indices.add(getIndex(dataSource, contentType)))
        );
        return indices;
    }
    public static Set<SpotCheckIndex> getIndices(SpotCheckDataSource dataSource, Set<SpotCheckContentType> contentTypes){
         return Arrays.stream(SpotCheckIndex.values())
                    .filter(spotCheckIndex -> spotCheckIndex.getDataSource().equals(dataSource))
                    .filter(spotCheckIndex -> contentTypes.contains(spotCheckIndex.getContentType()))
                    .collect(Collectors.toSet());
    }

    public static SpotCheckIndex getIndex(SpotCheckDataSource dataSource, SpotCheckContentType contentType){
        return Arrays.stream(SpotCheckIndex.values())
                .filter(spotCheckIndex -> spotCheckIndex.getContentType().equals(contentType))
                .filter(spotCheckIndex ->  spotCheckIndex.getDataSource().equals(dataSource))
                .findFirst().get();
    }

    public static SpotCheckIndex getIndex(SpotCheckRefType refType){
        return getIndex(refType.getDataSource(), refType.getContentType());
    }
}

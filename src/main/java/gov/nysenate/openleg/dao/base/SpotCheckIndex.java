package gov.nysenate.openleg.dao.base;

import gov.nysenate.openleg.model.spotcheck.SpotCheckContentType;
import gov.nysenate.openleg.model.spotcheck.SpotCheckDataSource;
import gov.nysenate.openleg.model.spotcheck.SpotCheckRefType;

import java.util.Arrays;

import static gov.nysenate.openleg.model.spotcheck.SpotCheckContentType.*;
import static gov.nysenate.openleg.model.spotcheck.SpotCheckDataSource.*;


/**
 * Created by PKS on 8/24/16.
 */
public enum SpotCheckIndex {
    SENATE_SITE_CALENDAR("senate-site-calendar", NYSENATE_DOT_GOV, CALENDAR),
    SENATE_SITE_AGENDA("senate-site-agenda", NYSENATE_DOT_GOV, AGENDA),
    SENATE_SITE_BILL("senate-site-bill", NYSENATE_DOT_GOV, BILL),
    LDBC_BILL("ldbc-bill", OPENLEG, BILL),
    LDBC_CALENDAR("ldbc-calendar", OPENLEG, CALENDAR),
    LDBC_AGENDA("ldbc-agenda", OPENLEG, AGENDA)
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

    public static SpotCheckIndex valueOf(SpotCheckDataSource dataSource, SpotCheckContentType contentType){
        return Arrays.stream(SpotCheckIndex.values())
                .filter(spotCheckIndex -> spotCheckIndex.getContentType().equals(contentType))
                .filter(spotCheckIndex ->  spotCheckIndex.getDataSource().equals(dataSource))
                .findFirst().get();
    }
}

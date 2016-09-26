package gov.nysenate.openleg.dao.base;

/**
 * Created by PKS on 8/24/16.
 */
public enum SpotCheckIndex {
    SENATE_SITE_CALENDAR("senate-site-calendar"),
    SENATE_SITE_AGENDA("senate-site-agenda"),
    SENATE_SITE_BILL("senate-site-bill"),
    DEFAULT("")
    ;

    String indexName;

    SpotCheckIndex(String indexName) {
        this.indexName = indexName;
    }
    public String getIndexName() {
        return indexName;
    }
}

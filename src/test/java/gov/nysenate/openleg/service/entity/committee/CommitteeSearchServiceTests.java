package gov.nysenate.openleg.service.entity.committee;

import gov.nysenate.openleg.BaseTests;
import gov.nysenate.openleg.service.entity.committee.search.CommitteeSearchService;
import org.junit.Test;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.beans.factory.annotation.Autowired;

public class CommitteeSearchServiceTests extends BaseTests {
    private static final Logger logger = LogManager.getLogger();

    @Autowired
    CommitteeSearchService committeeSearchService;

    @Test
    public void purgeIndexTest() {
        committeeSearchService.clearIndex();
    }

    @Test
    public void indexAllTest() {
        committeeSearchService.rebuildIndex();
    }
}

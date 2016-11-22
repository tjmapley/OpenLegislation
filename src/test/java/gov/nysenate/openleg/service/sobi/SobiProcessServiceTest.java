package gov.nysenate.openleg.service.sobi;

import gov.nysenate.openleg.BaseTests;
import gov.nysenate.openleg.dao.base.LimitOffset;
import gov.nysenate.openleg.dao.base.SortOrder;
import gov.nysenate.openleg.model.sobi.SobiFragment;
import gov.nysenate.openleg.processor.sobi.SobiProcessService;
import org.junit.Test;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class SobiProcessServiceTest extends BaseTests
{
    private static final Logger logger = LogManager.getLogger();

    @Autowired
    private SobiProcessService sobiProcessService;

    @Test
    public void testCollateSobiFiles() throws Exception {
        sobiProcessService.collateSobiFiles();
    }

    @Test
    public void testGetPendingFragments() throws Exception {
        List<SobiFragment> fragments = sobiProcessService.getPendingFragments(SortOrder.ASC, LimitOffset.ALL);
        for (SobiFragment fragment : fragments) {
            logger.debug(fragment.getFragmentId());
        }
    }


}

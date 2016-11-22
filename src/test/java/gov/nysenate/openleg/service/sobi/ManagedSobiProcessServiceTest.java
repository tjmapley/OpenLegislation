package gov.nysenate.openleg.service.sobi;

import gov.nysenate.openleg.BaseTests;
import gov.nysenate.openleg.model.sobi.SobiProcessOptions;
import gov.nysenate.openleg.processor.sobi.SobiProcessService;
import org.junit.Test;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.beans.factory.annotation.Autowired;

public class ManagedSobiProcessServiceTest extends BaseTests
{
    private static final Logger logger = LogManager.getLogger();

    @Autowired
    private SobiProcessService sobiProcessService;

    @Test
    public void randomTest() throws Exception {

    }

    @Test
    public void testCollateSobiFiles() throws Exception {
        int collated = sobiProcessService.collateSobiFiles();
        logger.info("Collated {} sobis", collated);
    }

    @Test
    public void testGetPendingFragments() throws Exception {

    }

    @Test
    public void testProcessFragments() throws Exception {

    }

    @Test
    public void testProcessPendingFragments() throws Exception {
        SobiProcessOptions options = SobiProcessOptions.builder().build();
        sobiProcessService.processPendingFragments(options);
    }

    @Test
    public void testUpdatePendingProcessing() throws Exception {

    }
}

package gov.nysenate.openleg.processor.law;

import gov.nysenate.openleg.BaseTests;
import gov.nysenate.openleg.model.law.LawTree;
import gov.nysenate.openleg.service.law.data.LawDataService;
import org.junit.Test;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;

public class LawProcessorTests extends BaseTests
{
    private static final Logger logger = LogManager.getLogger();

    @Autowired
    private LawDataService lawDataService;

    @Test
    public void testProcess() throws Exception {
        LawTree lawTree = lawDataService.getLawTree("ABC", LocalDate.now());
        logger.info("{}", lawTree);
    }
}

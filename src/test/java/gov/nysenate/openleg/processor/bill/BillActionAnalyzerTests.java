package gov.nysenate.openleg.processor.bill;

import gov.nysenate.openleg.BaseTests;
import gov.nysenate.openleg.model.bill.BaseBillId;
import gov.nysenate.openleg.model.bill.Bill;
import gov.nysenate.openleg.service.bill.data.BillDataService;
import org.junit.Test;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

public class BillActionAnalyzerTests extends BaseTests
{
    private static final Logger logger = LogManager.getLogger();

    @Autowired BillDataService billData;

    @Test
    public void testMilestones() throws Exception {
        Bill bill = billData.getBill(new BaseBillId("S1234", 2009));
        BillActionAnalyzer analyzer = new BillActionAnalyzer(bill.getBaseBillId(), bill.getActions(), Optional.empty());
        analyzer.analyze();
        analyzer.getStatuses().forEach(s -> logger.info("{}", s));
        logger.info("{}","Milestones ========");
        analyzer.getMilestones().forEach(m -> logger.info("{}", m));

    }
}
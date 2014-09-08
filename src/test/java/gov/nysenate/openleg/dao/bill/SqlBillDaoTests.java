package gov.nysenate.openleg.dao.bill;

import gov.nysenate.openleg.BaseTests;
import gov.nysenate.openleg.dao.base.LimitOffset;
import gov.nysenate.openleg.model.base.SessionYear;
import gov.nysenate.openleg.model.bill.BaseBillId;
import gov.nysenate.openleg.util.OutputUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class SqlBillDaoTests extends BaseTests
{
    private static final Logger logger = LoggerFactory.getLogger(SqlBillDaoTests.class);

    @Autowired
    BillDao billDao;

    @Test
    public void testGetBill() throws Exception {
        StopWatch sw = new StopWatch();
        logger.info("{}", billDao.getBill(new BaseBillId("S1235", 2013)));
        sw.start();
        logger.info("{}", billDao.getBill(new BaseBillId("S1234", 2013)));
        logger.info("{}", billDao.getBill(new BaseBillId("S1234", 2013)));
        logger.info("{}", billDao.getBill(new BaseBillId("S1234", 2013)));
        logger.info("{}", billDao.getBill(new BaseBillId("S1234", 2013)));
        sw.stop();
        logger.info("Time elapsed: {}", sw.getTime());
    }

    @Test
    public void testGetBillIdsBySession() throws Exception {
        StopWatch sw = new StopWatch();
        List<BaseBillId> baseBillIds = billDao.getBillIds(SessionYear.current(), LimitOffset.FIFTY);

        sw.start();
        baseBillIds = billDao.getBillIds(SessionYear.current(), LimitOffset.THOUSAND);
        logger.info("{}", OutputUtils.toJson(baseBillIds.size()));
        sw.stop();
        logger.info("{}", sw.getTime());
    }

    @Test
    public void testCountAllBills() throws Exception {
        logger.info("{}", billDao.getBillCount(SessionYear.current()));
    }
}
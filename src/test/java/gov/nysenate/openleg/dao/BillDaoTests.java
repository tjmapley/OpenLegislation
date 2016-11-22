package gov.nysenate.openleg.dao;

import gov.nysenate.openleg.BaseTests;
import gov.nysenate.openleg.dao.bill.data.BillDao;
import gov.nysenate.openleg.model.bill.BillId;
import gov.nysenate.openleg.model.bill.BillInfo;
import gov.nysenate.openleg.util.OutputUtils;
import org.junit.Test;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.beans.factory.annotation.Autowired;

public class BillDaoTests extends BaseTests
{
    private static final Logger logger = LogManager.getLogger();

    @Autowired
    private BillDao billDao;

    @Test
    public void testActiveSessionRange() throws Exception {
        logger.info("{}", billDao.activeSessionRange());
    }
}

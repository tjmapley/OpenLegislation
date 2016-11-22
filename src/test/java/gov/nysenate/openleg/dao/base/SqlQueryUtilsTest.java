package gov.nysenate.openleg.dao.base;

import org.junit.Test;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import static gov.nysenate.openleg.dao.base.SqlQueryUtils.getOrderByClause;
import static gov.nysenate.openleg.dao.base.SqlQueryUtils.getSqlWithSchema;
import static org.junit.Assert.assertEquals;

public class SqlQueryUtilsTest
{
    private static final Logger logger = LogManager.getLogger();

    @Test
    public void testGetSqlWithSchema() throws Exception {
        String sql = getSqlWithSchema("SELECT 1 FROM ${schema}.test", "master", new OrderBy("id", SortOrder.ASC), new LimitOffset(20, 4));
        logger.info(sql);
    }

    @Test
    public void testGetLimitOffsetClause() throws Exception {

    }

    @Test
    public void testGetOrderByClause() throws Exception {
        assertEquals("ORDER BY id DESC, name DESC", getOrderByClause(
            new OrderBy("id", SortOrder.DESC, "name", SortOrder.DESC)).trim());
        assertEquals("ORDER BY id DESC", getOrderByClause(
                new OrderBy("id", SortOrder.DESC, "name", SortOrder.NONE)).trim());
        assertEquals("", getOrderByClause(null).trim());
    }
}

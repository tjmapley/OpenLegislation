package gov.nysenate.openleg.service.spotcheck.base;

import gov.nysenate.openleg.dao.base.SortOrder;
import gov.nysenate.openleg.dao.spotcheck.SpotCheckContentIdMapper;
import gov.nysenate.openleg.dao.spotcheck.SpotCheckReportDao;
import gov.nysenate.openleg.model.spotcheck.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Provides base functionality for implementors of SpotCheckReportService
 */
public abstract class BaseSpotCheckReportService<ContentKey> implements SpotCheckReportService<ContentKey> {

    /**
     * @return SpotCheckReportDao - the report dao that is used by the implementing report service
     */
    protected abstract SpotCheckContentIdMapper<ContentKey> getContentIdMapper();

    @Autowired private SpotCheckReportDao spotCheckReportDao;

    @Override
    public SpotCheckReportDao getReportDao(){
        return spotCheckReportDao;
    }

    /** {@inheritDoc} */
    @Override
    public SpotCheckReport<ContentKey> getReport(SpotCheckReportId reportId) throws SpotCheckReportNotFoundEx {
        if (reportId == null) {
            throw new IllegalArgumentException("Supplied reportId cannot be null");
        }
        try {
            return spotCheckReportDao.getReport(reportId);
        } catch (EmptyResultDataAccessException ex) {
            throw new SpotCheckReportNotFoundEx(reportId);
        }
    }

    /** {@inheritDoc} */
    @Override
    public List<SpotCheckReportSummary> getReportSummaries(SpotCheckRefType reportType,
                                                           LocalDateTime start, LocalDateTime end, SortOrder dateOrder) {
        return spotCheckReportDao.getReportSummaries(reportType, start, end, dateOrder);
    }

    /** {@inheritDoc} */
    @Override
    public SpotCheckOpenMismatches<ContentKey> getOpenObservations(OpenMismatchQuery query) {
        return spotCheckReportDao.getOpenMismatches(query);
    }

    /** {@inheritDoc} */
    @Override
    public SpotCheckOpenMismatches<ContentKey> getOpenObservations(
            SpotCheckDataSource dataSource, SpotCheckContentType contentType, OpenMismatchQuery query) {
        return spotCheckReportDao.getOpenMismatches(dataSource, contentType, query);
    }


    /** {@inheritDoc}
     * @param refTypes
     * @param observedAfter*/
    @Override
    public OpenMismatchSummary getOpenMismatchSummary(Set<SpotCheckRefType> refTypes, LocalDateTime observedAfter) {
        return spotCheckReportDao.getOpenMismatchSummary(refTypes, observedAfter);
    }

    /** {@inheritDoc} */
    @Override
    public void saveReport(SpotCheckReport<ContentKey> report) {
        spotCheckReportDao.saveReport(report);
    }

    /** {@inheritDoc} */
    @Override
    public void deleteReport(SpotCheckReportId reportId) {
        if (reportId == null) {
            throw new IllegalArgumentException("Supplied reportId to delete cannot be null");
        }
        spotCheckReportDao.deleteReport(reportId);
    }

    /** {@inheritDoc} */
    @Override
    public void setMismatchIgnoreStatus(SpotCheckDataSource dataSource, SpotCheckContentType contentType,
                                        int mismatchId, SpotCheckMismatchIgnore ignoreStatus) {
        spotCheckReportDao.setMismatchIgnoreStatus(dataSource, contentType, mismatchId, ignoreStatus);
    }

    @Override
    public void addIssueId(SpotCheckDataSource dataSource, SpotCheckContentType contentType, int mismatchId, String issueId) {
        spotCheckReportDao.addIssueId(dataSource, contentType, mismatchId, issueId);
    }

    @Override
    public void deleteIssueId(SpotCheckDataSource dataSource, SpotCheckContentType contentType, int mismatchId, String issueId) {
        spotCheckReportDao.deleteIssueId(dataSource, contentType, mismatchId, issueId);
    }
}

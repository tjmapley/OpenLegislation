package gov.nysenate.openleg.dao.spotcheck.elastic;

import gov.nysenate.openleg.model.spotcheck.SpotCheckReport;
import gov.nysenate.openleg.model.spotcheck.SpotCheckReportId;

/**
 * Created by PKS on 9/15/16.
 */
class ElasticReport<ContentKey> {

    protected SpotCheckReportId reportId;

    protected String notes;

    protected Integer observationsCount;


    public ElasticReport(){}

    public ElasticReport(SpotCheckReport<ContentKey> report){
        reportId = report.getReportId();
        notes = report.getNotes();
        observationsCount = report.getObservedCount();
    }

    public SpotCheckReportId getReportId(){
        return reportId;
    }

    public String getNotes(){
        return notes;
    }

    public Integer getObservationsCount(){
        return observationsCount;
    }

    public void setReportId(SpotCheckReportId Id){
        reportId = Id;
    }

    public void setNotes(String note){
        notes = note;
    }

    public void setObservationsCount(Integer count){
        observationsCount = count;
    }

    public SpotCheckReport<ContentKey> toSpotCheckReport(){
        return new SpotCheckReport<>(reportId,notes);
    }
}


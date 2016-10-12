package gov.nysenate.openleg.dao.spotcheck.elastic;

import com.google.common.base.Objects;
import gov.nysenate.openleg.model.spotcheck.SpotCheckReport;
import gov.nysenate.openleg.model.spotcheck.SpotCheckReportId;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (!(o instanceof ElasticReport)) return false;

        ElasticReport<?> that = (ElasticReport<?>) o;

        return new EqualsBuilder()
                .append(reportId, that.reportId)
                .append(notes, that.notes)
                .append(observationsCount, that.observationsCount)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(reportId)
                .append(notes)
                .append(observationsCount)
                .toHashCode();
    }
}


package gov.nysenate.openleg.dao.spotcheck.elastic;

import gov.nysenate.openleg.dao.spotcheck.elastic.AbstractSpotCheckReportElasticDao;
import gov.nysenate.openleg.model.spotcheck.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by PKS on 9/15/16.
 */
class ElasticObservation<ContentKey> {

    protected String spotcheckReportId;
    protected SpotCheckReferenceId referenceId;
    protected LocalDateTime observedDateTime;
    protected LocalDateTime createdDateTime;
    protected Map<String, String> observationKey;
    protected SpotCheckMismatchStatus mismatchStatus;
    protected SpotCheckMismatchType mismatchType;
    protected String observedData;
    protected String referenceData;
    protected String notes;
    protected SpotCheckMismatchIgnore mismatchIgnore;
    protected List<String> issueIds;

    public ElasticObservation(){}

    public ElasticObservation(String spotCheckReportId,Map<String, String> keyMap, SpotCheckObservation<ContentKey> observation,
                              SpotCheckMismatch spotCheckMismatch){
        spotcheckReportId = spotCheckReportId;
        referenceId = observation.getReferenceId();
        observedDateTime = observation.getObservedDateTime();
        createdDateTime = LocalDateTime.now();
        observationKey = keyMap;
        mismatchStatus = spotCheckMismatch.getStatus();
        mismatchType = spotCheckMismatch.getMismatchType();
        observedData = spotCheckMismatch.getObservedData();
        referenceData = spotCheckMismatch.getReferenceData();
        notes = spotCheckMismatch.getNotes();
        mismatchIgnore = spotCheckMismatch.getIgnoreStatus() != null ? spotCheckMismatch.getIgnoreStatus() : SpotCheckMismatchIgnore.NOT_IGNORED;
        issueIds = spotCheckMismatch.getIssueIds();
    }

    public String getSpotcheckReportId(){
        return spotcheckReportId;
    }

    public SpotCheckReferenceId  getReferenceId(){
        return referenceId;
    }

    public LocalDateTime getObservedDateTime(){
        return observedDateTime;
    }

    public LocalDateTime getcreatedDateTime(){
        return createdDateTime;
    }

    public Map<String, String> getObservationKey(){
        return observationKey;
    }

    public SpotCheckMismatchStatus getMismatchStatus(){
        return mismatchStatus;
    }

    public SpotCheckMismatchType getMismatchType(){
        return mismatchType;
    }

    public SpotCheckMismatchIgnore getMismatchIgnore(){
        return mismatchIgnore;
    }

    public List<String> getIssueIds(){
        return issueIds;
    }

    public String getNotes(){
        return notes;
    }

    public String getObservedData(){
        return observedData;
    }

    public String getReferenceData(){
        return referenceData;
    }

    public void setSpotcheckReportId(String reportId){
        spotcheckReportId = reportId;
    }

    public void setReferenceId(SpotCheckReferenceId Id){
        referenceId = Id;
    }

    public void setObservedDateTime(LocalDateTime dateTime){
        observedDateTime = dateTime;
    }

    public void setObervationkey(Map<String, String> key){
        observationKey = key;
    }

    public void setCreatedDateTime(LocalDateTime dateTime){
        createdDateTime = dateTime;
    }

    public void setMismatchStatus(SpotCheckMismatchStatus status){
        mismatchStatus = status;
    }

    public void setMismatchType(SpotCheckMismatchType type){
        mismatchType = type;
    }

    public void setObservedData(String data){
        observedData = data;
    }

    public void setReferenceData(String data){
        referenceData = data;
    }

    public void setNotes(String note){
        notes = note;
    }

    public void setIgnoreStatus(SpotCheckMismatchIgnore status){
        mismatchIgnore = status;
    }

    public void setIssueIds(List<String> Ids){
        issueIds = Ids;
    }

    public SpotCheckObservation<ContentKey> toSpotCheckObservation(ContentKey key){
        SpotCheckObservation<ContentKey> observation = new SpotCheckObservation<ContentKey>(referenceId, key);
        SpotCheckMismatch spotCheckMismatch = new SpotCheckMismatch(mismatchType, observedData, referenceData, notes);
        observation.setObservedDateTime(observedDateTime);
        spotCheckMismatch.setStatus(mismatchStatus);
        spotCheckMismatch.setIgnoreStatus((mismatchIgnore==null)? SpotCheckMismatchIgnore.NOT_IGNORED: mismatchIgnore);
        spotCheckMismatch.setIssueIds(issueIds);
        spotCheckMismatch.setMismatchId(this.hashCode());
        Map<SpotCheckMismatchType, SpotCheckMismatch> mismatchMap = new HashMap<>();
        mismatchMap.put(spotCheckMismatch.getMismatchType(), spotCheckMismatch);
        observation.setMismatches(mismatchMap);
        return observation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ElasticObservation)) return false;

        ElasticObservation<?> that = (ElasticObservation<?>) o;

        if (!spotcheckReportId.equals(that.spotcheckReportId)) return false;
        if (!referenceId.equals(that.referenceId)) return false;
        if (!observedDateTime.equals(that.observedDateTime)) return false;
        if (!createdDateTime.equals(that.createdDateTime)) return false;
        if (!observationKey.equals(that.observationKey)) return false;
        if (mismatchStatus != that.mismatchStatus) return false;
        if (mismatchType != that.mismatchType) return false;
        if (observedData != null ? !observedData.equals(that.observedData) : that.observedData != null) return false;
        if (referenceData != null ? !referenceData.equals(that.referenceData) : that.referenceData != null)
            return false;
        if (notes != null ? !notes.equals(that.notes) : that.notes != null) return false;
        if (mismatchIgnore != that.mismatchIgnore) return false;
        return issueIds != null ? issueIds.equals(that.issueIds) : that.issueIds == null;

    }

    @Override
    public int hashCode() {
        int result = spotcheckReportId.hashCode();
        result = 31 * result + referenceId.hashCode();
        result = 31 * result + observedDateTime.hashCode();
        result = 31 * result + createdDateTime.hashCode();
        result = 31 * result + observationKey.hashCode();
        result = 31 * result + (mismatchStatus != null ? mismatchStatus.hashCode() : 0);
        result = 31 * result + (mismatchType != null ? mismatchType.hashCode() : 0);
        result = 31 * result + (observedData != null ? observedData.hashCode() : 0);
        result = 31 * result + (referenceData != null ? referenceData.hashCode() : 0);
        result = 31 * result + (notes != null ? notes.hashCode() : 0);
        result = 31 * result + (mismatchIgnore != null ? mismatchIgnore.hashCode() : 0);
        result = 31 * result + (issueIds != null ? issueIds.hashCode() : 0);
        return result;
    }

}


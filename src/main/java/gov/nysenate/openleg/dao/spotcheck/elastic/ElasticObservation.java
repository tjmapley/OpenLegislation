package gov.nysenate.openleg.dao.spotcheck.elastic;

import gov.nysenate.openleg.model.spotcheck.*;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by PKS on 9/15/16.
 */
class ElasticObservation {

    protected String spotcheckReportId;
    protected SpotCheckReferenceId referenceId;
    protected LocalDateTime observedDateTime;
    protected LocalDateTime createdDateTime;
    protected Map<String, String> observationKey;
    protected String observationKeyString;

    public String getGroupByKey() {
        return groupByKey;
    }

    public void setGroupByKey(String groupByKey) {
        this.groupByKey = groupByKey;
    }

    protected String groupByKey;
    protected SpotCheckMismatchStatus mismatchStatus;
    protected SpotCheckMismatchType mismatchType;
    protected Integer mismatchId;
    protected String observedData;
    protected String referenceData;
    protected String notes;
    protected SpotCheckMismatchIgnore mismatchIgnore;
    protected List<String> issueIds;

    public ElasticObservation(){}

    public String getObservationKeyString() {
        return observationKeyString;
    }

    public void setObservationKeyString(String observationKeyString) {
        this.observationKeyString = observationKeyString;
    }

    public ElasticObservation(String spotCheckReportId, Map<String, String> keyMap, SpotCheckObservation observation,
                              SpotCheckMismatch spotCheckMismatch){
        spotcheckReportId = spotCheckReportId;
        referenceId = observation.getReferenceId();
        observedDateTime = observation.getObservedDateTime();
        createdDateTime = LocalDateTime.now();
        observationKey = keyMap;
        observationKeyString = observation.getKey().toString();
        groupByKey = observation.getKey().toString() + spotCheckMismatch.getMismatchType();
        mismatchStatus = spotCheckMismatch.getStatus();
        mismatchType = spotCheckMismatch.getMismatchType();
        observedData = spotCheckMismatch.getObservedData();
        referenceData = spotCheckMismatch.getReferenceData();
        notes = spotCheckMismatch.getNotes();
        mismatchIgnore = spotCheckMismatch.getIgnoreStatus() != null ? spotCheckMismatch.getIgnoreStatus() : SpotCheckMismatchIgnore.NOT_IGNORED;
        issueIds = spotCheckMismatch.getIssueIds();
        mismatchId = this.hashCode() + observation.hashCode() + spotCheckMismatch.hashCode();
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

    public Integer getMismatchId(){ return mismatchId; }

    public void setSpotcheckReportId(String reportId){
        spotcheckReportId = reportId;
    }

    public void setReferenceId(SpotCheckReferenceId Id){
        referenceId = Id;
    }

    public void setObservedDateTime(LocalDateTime dateTime){
        observedDateTime = dateTime;
    }

    public void setObservationKey(Map<String, String> key){
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

    public void setMismatchId(Integer mismatchid){ mismatchId = mismatchid; }

    public void setIssueIds(List<String> Ids){
        issueIds = Ids;
    }

    public <ContentKey> SpotCheckObservation<ContentKey> toSpotCheckObservation(ContentKey key, String mismatchid){
        SpotCheckObservation<ContentKey> observation = new SpotCheckObservation<ContentKey>(referenceId, key);
        SpotCheckMismatch spotCheckMismatch = new SpotCheckMismatch(mismatchType, observedData, referenceData, notes);
        observation.setObservedDateTime(observedDateTime);
        spotCheckMismatch.setStatus(mismatchStatus);
        spotCheckMismatch.setIgnoreStatus((mismatchIgnore==null)? SpotCheckMismatchIgnore.NOT_IGNORED: mismatchIgnore);
        spotCheckMismatch.setIssueIds(issueIds);
        spotCheckMismatch.setMismatchId(Integer.parseInt(mismatchid));
        Map<SpotCheckMismatchType, SpotCheckMismatch> mismatchMap = new HashMap<>();
        mismatchMap.put(spotCheckMismatch.getMismatchType(), spotCheckMismatch);
        observation.setMismatches(mismatchMap);
        return observation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (!(o instanceof ElasticObservation)) return false;

        ElasticObservation that = (ElasticObservation) o;

        return new EqualsBuilder()
                .append(spotcheckReportId, that.spotcheckReportId)
                .append(referenceId, that.referenceId)
                .append(observedDateTime, that.observedDateTime)
                .append(createdDateTime, that.createdDateTime)
                .append(observationKey, that.observationKey)
                .append(mismatchStatus, that.mismatchStatus)
                .append(mismatchType, that.mismatchType)
                .append(observedData, that.observedData)
                .append(referenceData, that.referenceData)
                .append(notes, that.notes)
                .append(mismatchIgnore, that.mismatchIgnore)
                .append(issueIds, that.issueIds)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(spotcheckReportId)
                .append(referenceId)
                .append(observedDateTime)
                .append(createdDateTime)
                .append(observationKey)
                .append(mismatchStatus)
                .append(mismatchType)
                .append(observedData)
                .append(referenceData)
                .append(notes)
                .append(mismatchIgnore)
                .append(issueIds)
                .toHashCode();
    }
}


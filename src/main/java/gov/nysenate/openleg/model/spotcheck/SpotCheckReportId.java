package gov.nysenate.openleg.model.spotcheck;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.ComparisonChain;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

public class SpotCheckReportId implements Comparable<SpotCheckReportId>
{
    /** The reference type used to validate data against. */
    protected SpotCheckRefType referenceType;

    /** The date that the reference was registered */
    protected LocalDateTime referenceDateTime;

    /** When this report was generated. */
    protected LocalDateTime reportDateTime;

    /** --- Constructor --- */

    protected SpotCheckReportId(){}

    public SpotCheckReportId(SpotCheckRefType referenceType, LocalDateTime reportDateTime) {
        this.referenceType = referenceType;
        this.reportDateTime = reportDateTime;
    }

    public SpotCheckReportId(SpotCheckRefType referenceType, LocalDateTime referenceDateTime, LocalDateTime reportDateTime) {
        this(referenceType, reportDateTime);
        this.referenceDateTime = referenceDateTime;
    }

    public SpotCheckReportId(SpotCheckReferenceId referenceId, LocalDateTime reportDateTime) {
        this(
                Optional.ofNullable(referenceId)
                        .map(SpotCheckReferenceId::getReferenceType)
                        .orElse(null),
                Optional.ofNullable(referenceId)
                        .map(SpotCheckReferenceId::getRefActiveDateTime)
                        .orElse(null),
                reportDateTime
        );
    }

    /** --- Functional Getters --- */
    @JsonIgnore
    public SpotCheckReferenceId getReferenceId() {
        return new SpotCheckReferenceId(referenceType, referenceDateTime);
    }

    /** --- Overrides --- */

    @Override
    public String toString() {
        return "SpotCheckReportId{" + "referenceType=" + referenceType + ", referenceDateTime=" + referenceDateTime +
                ", reportDateTime=" + reportDateTime + '}';
    }

    /** --- Basic Getters --- */

    public SpotCheckRefType getReferenceType() {
        return referenceType;
    }

    public LocalDateTime getReferenceDateTime() {
        return referenceDateTime;
    }

    public LocalDateTime getReportDateTime() {
        return reportDateTime;
    }

    @Override
    public int compareTo(SpotCheckReportId o) {
        return ComparisonChain.start()
            .compare(this.reportDateTime, o.reportDateTime)
            .result();
    }
}
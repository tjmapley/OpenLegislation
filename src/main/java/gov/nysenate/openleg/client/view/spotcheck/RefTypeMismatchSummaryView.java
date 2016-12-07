package gov.nysenate.openleg.client.view.spotcheck;

import gov.nysenate.openleg.model.spotcheck.RefTypeMismatchSummary;
import gov.nysenate.openleg.model.spotcheck.SpotCheckContentType;
import gov.nysenate.openleg.model.spotcheck.SpotCheckRefType;

public class RefTypeMismatchSummaryView extends SpotCheckSummaryView {

    protected SpotCheckRefType refType;

    protected SpotCheckContentType contentType;

    public RefTypeMismatchSummaryView(RefTypeMismatchSummary summary) {
        super(summary);
        this.refType = summary.getRefType();
        this.contentType = summary.getRefType().getContentType();
    }

    public SpotCheckRefType getRefType() {
        return refType;
    }
}

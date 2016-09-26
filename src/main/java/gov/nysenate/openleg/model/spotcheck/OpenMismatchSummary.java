package gov.nysenate.openleg.model.spotcheck;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * Contains mismatch/status count summaries for each spotcheck ref type included in a open mismatch query
 */
public class OpenMismatchSummary {

    protected Map<SpotCheckRefType, RefTypeMismatchSummary> summaryMap;

    protected LocalDateTime observedAfter;

    public OpenMismatchSummary(SpotCheckRefType refType, LocalDateTime observedAfter) {
        this.observedAfter = observedAfter;
        this.summaryMap = new HashMap<>();
        this.summaryMap.put(refType, new RefTypeMismatchSummary(refType, observedAfter)); //refTypes.stream()
                //.map(refType -> new RefTypeMismatchSummary(refType, observedAfter))
                //.collect(Collectors.toMap(RefTypeMismatchSummary::getRefType, Function.identity()));
    }

    /** --- Functional Getters / Setters --- */

    public RefTypeMismatchSummary getRefTypeSummary(SpotCheckRefType refType) {
        return summaryMap.get(refType);
    }

    /** --- Getters / Setters --- */

    public Map<SpotCheckRefType, RefTypeMismatchSummary> getSummaryMap() {
        return summaryMap;
    }

    public LocalDateTime getObservedAfter() {
        return observedAfter;
    }
}

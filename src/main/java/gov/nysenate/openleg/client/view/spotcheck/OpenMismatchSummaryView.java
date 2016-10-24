package gov.nysenate.openleg.client.view.spotcheck;

import gov.nysenate.openleg.client.view.base.ViewObject;
import gov.nysenate.openleg.model.spotcheck.OpenMismatchSummary;
import gov.nysenate.openleg.model.spotcheck.SpotCheckRefType;
import org.springframework.web.servlet.view.jasperreports.JasperReportsCsvView;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class OpenMismatchSummaryView implements ViewObject {

    protected Map<SpotCheckRefType, RefTypeMismatchSummaryView> summaryMap = new HashMap<>();
    protected LocalDateTime observedAfter;

    public OpenMismatchSummaryView(OpenMismatchSummary summary) {
        this.summaryMap = summary.getSummaryMap().values().stream()
                .map(RefTypeMismatchSummaryView::new)
                .collect(Collectors.toMap(RefTypeMismatchSummaryView::getRefType, Function.identity()));
        this.observedAfter = summary.getObservedAfter();
    }

    public OpenMismatchSummaryView(List<OpenMismatchSummary> summaries){
          summaries.forEach(summary -> {
                 this.summaryMap.putAll(
                           summary.getSummaryMap().values().stream()
                                   .map(RefTypeMismatchSummaryView::new)
                                   .collect(Collectors.toMap(RefTypeMismatchSummaryView::getRefType, Function.identity()))
            );
            this.observedAfter = summary.getObservedAfter();
        });
    }

    public Map<SpotCheckRefType, RefTypeMismatchSummaryView> getSummaryMap() {
        return summaryMap;
    }

    public LocalDateTime getObservedAfter() {
        return observedAfter;
    }

    @Override
    public String getViewType() {
        return "open-mismatch-summary";
    }
}

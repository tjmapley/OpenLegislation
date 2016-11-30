package gov.nysenate.openleg.dao.spotcheck.calendars;

import com.google.common.collect.ImmutableMap;
import gov.nysenate.openleg.dao.spotcheck.SpotCheckContentIdMapper;
import gov.nysenate.openleg.dao.spotcheck.elastic.ElasticSpotCheckReportDao;
import gov.nysenate.openleg.model.calendar.CalendarId;
import gov.nysenate.openleg.model.spotcheck.SpotCheckContentType;
import gov.nysenate.openleg.model.spotcheck.SpotCheckDataSource;
import gov.nysenate.openleg.model.spotcheck.SpotCheckRefType;
import org.springframework.stereotype.Repository;

import java.util.Map;

@Repository
public class CalendarAlertMapper implements SpotCheckContentIdMapper<CalendarId> {

    @Override
    public SpotCheckContentType getContentType() {
        return SpotCheckContentType.CALENDAR;
    }

    @Override
    public SpotCheckDataSource getDataSource() {
        return SpotCheckDataSource.OPENLEG;
    }

    @Override
    public CalendarId getKeyFromMap(Map<String, String> keyMap) {
        return new CalendarId(Integer.parseInt(keyMap.get("cal_no")), Integer.parseInt(keyMap.get("year")));
    }

    @Override
    public Map<String, String> getMapFromKey(CalendarId calendarId) {
        return ImmutableMap.<String, String>builder()
                .put("cal_no", String.valueOf(calendarId.getCalNo()))
                .put("year", String.valueOf(calendarId.getYear())).build();
    }

}

package gov.nysenate.openleg.client.view.calendar;

import gov.nysenate.openleg.model.calendar.Calendar;
import gov.nysenate.openleg.model.calendar.CalendarActiveList;
import gov.nysenate.openleg.model.calendar.CalendarSupplemental;
import gov.nysenate.openleg.service.bill.data.BillDataService;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CalendarViewFactory {
    private static final Logger logger = LogManager.getLogger();

    @Autowired
    BillDataService billDataService;

    public CalendarView getCalendarView(Calendar calendar) {
        return new CalendarView(calendar, billDataService);
    }

    public ActiveListView getActiveListView(CalendarActiveList activeList) {
        return new ActiveListView(activeList, billDataService);
    }

    public CalendarSupView getCalendarSupView(CalendarSupplemental calendarSupplemental) {
        return new CalendarSupView(calendarSupplemental, billDataService);
    }
}

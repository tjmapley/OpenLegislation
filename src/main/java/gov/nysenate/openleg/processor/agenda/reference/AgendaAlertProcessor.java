package gov.nysenate.openleg.processor.agenda.reference;

import gov.nysenate.openleg.dao.agenda.reference.AgendaAlertDao;
import gov.nysenate.openleg.model.spotcheck.agenda.AgendaAlertInfoCommittee;
import gov.nysenate.openleg.processor.base.ParseError;
import gov.nysenate.openleg.service.spotcheck.base.SpotCheckNotificationService;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Service
public class AgendaAlertProcessor {

    private static final Logger logger = LogManager.getLogger();

    @Autowired
    private AgendaAlertDao agendaAlertDao;

    @Autowired
    SpotCheckNotificationService notificationService;

    public int processAgendaAlerts() throws ParseError, IOException {
        int processedCount = 0;
        for (File alertFile : agendaAlertDao.getIncomingAgendaAlerts()) {
            logger.info("processing agenda alert {}", alertFile.getName());
            try {
                List<AgendaAlertInfoCommittee> references = AgendaAlertParser.parseAgendaAlert(alertFile);
                references.forEach(agendaAlertDao::updateAgendaAlertInfoCommittee);
                processedCount++;
            } catch (Exception ex) {
                notificationService.handleSpotcheckException(ex, false);
            } finally {
                logger.info("archiving agenda alert {}", alertFile.getName());
                agendaAlertDao.archiveAgendaAlert(alertFile);
            }
        }
        return processedCount;
    }

}

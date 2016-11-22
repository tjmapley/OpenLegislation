package gov.nysenate.openleg.service.spotcheck.agenda;

import gov.nysenate.openleg.dao.agenda.reference.AgendaAlertDao;
import gov.nysenate.openleg.model.spotcheck.SpotCheckRefType;
import gov.nysenate.openleg.processor.agenda.reference.AgendaAlertProcessor;
import gov.nysenate.openleg.service.spotcheck.base.BaseSpotcheckProcessService;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AgendaSpotcheckProcessService extends BaseSpotcheckProcessService {
    private static final Logger logger = LogManager.getLogger();

    @Autowired
    private AgendaAlertCheckMailService alertCheckMailService;

    @Autowired
    private CommAgendaAlertCheckMailService commAgendaAlertCheckMailService;

    @Autowired
    private AgendaAlertProcessor agendaAlertProcessor;

    @Autowired
    private AgendaAlertDao agendaAlertDao;

    /** --- Implemented Methods --- */

    @Override
    protected int doCollate() throws Exception {
        return alertCheckMailService.checkMail() + commAgendaAlertCheckMailService.checkMail();
    }

    @Override
    protected int doIngest() throws Exception {
        return agendaAlertProcessor.processAgendaAlerts();
    }

    @Override
    protected SpotCheckRefType getRefType() {
        return SpotCheckRefType.LBDC_AGENDA_ALERT;
    }

    @Override
    protected int getUncheckedRefCount() {
        return agendaAlertDao.getProdUncheckedAgendaAlertReferences().size() +
                agendaAlertDao.getUncheckedAgendaAlertReferences().size();
    }

    @Override
    public String getCollateType() {
        return "agenda alert";
    }
}

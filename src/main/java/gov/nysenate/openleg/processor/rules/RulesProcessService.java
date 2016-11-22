package gov.nysenate.openleg.processor.rules;

import com.google.common.eventbus.EventBus;
import gov.nysenate.openleg.config.Environment;
import gov.nysenate.openleg.processor.base.ProcessService;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
public class RulesProcessService implements ProcessService
{
    private static final Logger logger = LogManager.getLogger();

    @Autowired private Environment env;
    @Autowired private EventBus eventBus;

    @PostConstruct
    private void init() {
        eventBus.register(this);
    }

    /** {@inheritDoc} */
    @Override
    public int collate() {
        return 0;
    }

    /** {@inheritDoc} */
    @Override
    public int ingest() {
        return 0;
    }

    /** {@inheritDoc} */
    @Override
    public String getCollateType() {
        return null;
    }
}

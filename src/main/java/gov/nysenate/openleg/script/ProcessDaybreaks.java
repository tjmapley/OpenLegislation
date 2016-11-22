package gov.nysenate.openleg.script;

import gov.nysenate.openleg.processor.daybreak.DaybreakProcessService;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class ProcessDaybreaks extends BaseScript {

    private static final Logger logger = LogManager.getLogger();

    @Autowired
    private DaybreakProcessService daybreakProcessService;

    public static void main(String[] arg) throws Exception {
        AnnotationConfigApplicationContext ctx = BaseScript.init();
        ProcessDaybreaks processDaybreaks = ctx.getBean(ProcessDaybreaks.class);
        CommandLine cmd = getCommandLine(processDaybreaks.getOptions(), arg);
        processDaybreaks.execute(cmd);
        shutdown(ctx);
    }

    @Override
    protected Options getOptions() {
        Options options = new Options();
        options.addOption("c", "collate", false, "Will collate daybreak files without processing afterwards unless --process is set");
        options.addOption("p", "process", false, "Will process pending daybreak fragments without collating first unless --collate is set");
        return options;
    }

    @Override
    protected void execute(CommandLine opts) throws Exception {
        boolean collate = opts.hasOption("collate");
        boolean process = opts.hasOption("process");

        if(collate || !process) {
            logger.info("Collating daybreak files.");
            daybreakProcessService.collateDaybreakReports();
        }
        if(process || !collate) {
            logger.info("Processing pending daybreak fragments.");
            daybreakProcessService.processPendingFragments();
        }
        logger.info("done");
    }
}

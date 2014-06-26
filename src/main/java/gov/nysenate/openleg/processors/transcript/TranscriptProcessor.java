package gov.nysenate.openleg.processors.transcript;

import gov.nysenate.openleg.model.transcript.Transcript;
import gov.nysenate.openleg.util.ChangeLogger;
import gov.nysenate.openleg.util.Storage;
import org.apache.log4j.Logger;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TranscriptProcessor {
    private final Logger logger;

    public SimpleDateFormat TRANSCRIPT_DATE_PARSER = new SimpleDateFormat("MMMM dd, yyyy hh:mm aa");

    public TranscriptProcessor() {
        this.logger = Logger.getLogger(this.getClass());
    }

    public void process(File file, Storage storage) throws IOException {
        Transcript transcript = new Transcript();
        StringBuffer fullText = new StringBuffer();
        StringBuffer fullTextProcessed = new StringBuffer();

        String pLine = null;
        int locationLineIdx = 9;
        boolean checkedLineFour = false;

        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "latin1"));
        String line = null;
        logger.debug("Skipping: "+reader.readLine());
        while ((line = reader.readLine()) != null) {
            pLine = line.trim();

            if (pLine.startsWith("4") && (!checkedLineFour)) {
                if (pLine.indexOf("STENOGRAPHIC RECORD")==-1)
                    locationLineIdx = 10;

                checkedLineFour = true;
            }
            else if (transcript.getLocation() == null && pLine.startsWith(locationLineIdx+" ")) {
                pLine = pLine.trim();

                if (pLine.length() < 3)
                    locationLineIdx++; //location must be on the next line
                else {
                    //9                   ALBANY, NEW YORK
                    pLine = pLine.substring(2).trim();

                    transcript.setLocation(pLine);
                    logger.debug("got location: " + transcript.getLocation());
                }
            }
            else if (transcript.getTimeStamp() == null && pLine.startsWith((locationLineIdx+1)+" ")) {
                // 11                    August 7, 2009
                //  12                      10:00 a.m.
                pLine = pLine.substring(2).trim();

                logger.debug("got day: " + pLine);

                String nextLine = reader.readLine();
                nextLine = reader.readLine().trim();
                nextLine = nextLine.substring(2).trim();

                logger.debug("got time: " + nextLine);

                pLine += ' ' + nextLine;
                pLine = pLine.replace(".", "").toUpperCase();

                try {
                    Date tTime = TRANSCRIPT_DATE_PARSER.parse(pLine);
                    logger.debug(pLine+" -> "+tTime);
                    transcript.setTimeStamp(tTime);
                } catch (ParseException e) {
                    logger.error(file.getName()+": unable to parse transcript datetime " + pLine,e);
                }
            }
            else if (transcript.getType() == null && pLine.startsWith((locationLineIdx+5)+" ")) {
                // 15                    REGULAR SESSION
                pLine = pLine.substring(2);
                pLine = pLine.trim();

                transcript.setType(pLine);
            }

            fullText.append(line);
            fullText.append('\n');

            line = line.trim();

            if (line.length() > 2) {
                line = line.substring(2);
                fullTextProcessed.append(line);
                fullTextProcessed.append('\n');
            }
        }
        reader.close();

        transcript.setTranscriptText(fullText.toString());
        transcript.setTranscriptTextProcessed(fullTextProcessed.toString());
        String oid = transcript.getType().replaceAll(" ",  "-")+"-"+new SimpleDateFormat("MM-dd-yyyy_HH:mm").format(transcript.getTimeStamp());
        transcript.setId(oid);
        transcript.setModifiedDate(transcript.getTimeStamp());
        transcript.setPublishDate(transcript.getTimeStamp());

        // Save the transcript
        String key = transcript.getYear()+"/transcript/"+transcript.getId();
        storage.set(transcript);

        // Make an entry in the change log
        ChangeLogger.setContext(file, transcript.getTimeStamp());
        ChangeLogger.record(key, storage);
    }
}

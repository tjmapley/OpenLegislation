package gov.nysenate.openleg.dao.spotcheck;

import gov.nysenate.openleg.model.spotcheck.SpotCheckContentType;
import gov.nysenate.openleg.model.spotcheck.SpotCheckDataSource;

import java.util.Map;

public interface SpotCheckContentIdMapper<ContentKey> {

    SpotCheckContentType getContentType();

    SpotCheckDataSource getDataSource();

    ContentKey getKeyFromMap(Map<String, String> keyMap);

    Map<String, String> getMapFromKey(ContentKey key);
}

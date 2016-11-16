package gov.nysenate.openleg.service.spotcheck.base;

import gov.nysenate.openleg.model.spotcheck.SpotCheckContentType;

import java.util.Map;

public interface SpotCheckContentIdMapper<ContentKey> {

    SpotCheckContentType getContentType();

    ContentKey getKeyFromMap(Map<String, String> keyMap);

    Map<String, String> getMapFromKey(ContentKey key);
}

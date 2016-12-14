package gov.nysenate.openleg.dao.base;

/**
 * Created by senateuser on 12/12/16.
 */
public enum ElasticSpotCheckType {
    REPORT("reports"),
    OBSERVATION("observations"),
    ;

    String name;
    ElasticSpotCheckType(String name){
        this.name = name;
    }

    public String getName(){
        return name;
    }
}

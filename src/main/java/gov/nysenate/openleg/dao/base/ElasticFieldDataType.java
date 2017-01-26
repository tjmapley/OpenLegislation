package gov.nysenate.openleg.dao.base;

/**
 * Created by senateuser on 12/20/16.
 */
public enum ElasticFieldDataType {

    TEXT("text"),
    KEYWORD("keyword"),
    DATE("date"),
    LONG("long"),
    DOUBLE("double"),
    Boolean("boolean"),
    IP("ip"),
    OBJECT("object"),
    NESTED("nested"),
    GEO_POINT("geo_point"),
    GEO_SHAPE("geo_shape"),
    COMPLETION("completion"),
    ;

    private String name;

    ElasticFieldDataType(String name){
        this.name = name;
    }

    public String getName(){
        return name;
    }
}

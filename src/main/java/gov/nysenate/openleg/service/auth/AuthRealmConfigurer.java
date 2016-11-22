package gov.nysenate.openleg.service.auth;

import org.apache.shiro.realm.Realm;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;

@Component
public class AuthRealmConfigurer
{
    private static final Logger logger = LogManager.getLogger();

    @Autowired protected List<Realm> realmList;
    @Autowired protected DefaultWebSecurityManager securityManager;

    @PostConstruct
    public void setUp() {
        securityManager.setRealms(realmList);
    }
}

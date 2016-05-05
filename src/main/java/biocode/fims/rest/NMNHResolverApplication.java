package biocode.fims.rest;

/**
 * Jersey Application for NMNH Resolver Services
 */
public class NMNHResolverApplication extends FimsApplication {

    public NMNHResolverApplication() {
        super();
        packages("biocode.fims.rest.services.id");
    }
}

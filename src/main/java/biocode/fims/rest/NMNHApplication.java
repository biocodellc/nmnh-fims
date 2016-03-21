package biocode.fims.rest;

import org.glassfish.jersey.media.multipart.MultiPartFeature;

/**
 * class to register information for jersey app
 */
public class NMNHApplication extends FimsApplication {

    public NMNHApplication() {
        super();
        packages("biocode.fims.rest.services.rest");
        register(MultiPartFeature.class);
    }
}

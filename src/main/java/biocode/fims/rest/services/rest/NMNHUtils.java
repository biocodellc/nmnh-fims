package biocode.fims.rest.services.rest;

import biocode.fims.rest.FimsService;
import biocode.fims.rest.filters.Authenticated;
import biocode.fims.service.OAuthProviderService;
import biocode.fims.settings.SettingsManager;
import biocode.fims.utils.DashboardGenerator;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * NMNH-Fims utility services
 */
@Path("utils/")
public class NMNHUtils extends FimsService {

    @Autowired
    NMNHUtils(OAuthProviderService providerService, SettingsManager settingsManager) {
        super(providerService, settingsManager);
    }

    @GET
    @Authenticated
    @Path("/getDatasetDashboard")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDatasetDashboard() {
        DashboardGenerator dashboardGenerator = new DashboardGenerator();
        String dashboard = dashboardGenerator.getNMNHDashboard(user.getUsername());

        return Response.ok("{\"dashboard\": \"" + dashboard + "\"}").build();
    }
}

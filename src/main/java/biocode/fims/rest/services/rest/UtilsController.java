package biocode.fims.rest.services.rest;

import biocode.fims.rest.filters.Authenticated;
import biocode.fims.settings.SettingsManager;
import biocode.fims.utils.DashboardGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * NMNH-Fims utility services
 */
@Controller
@Path("utils/")
public class UtilsController extends FimsAbstractUtilsController {

    @Autowired
    UtilsController(SettingsManager settingsManager) {
        super(settingsManager);
    }

    @GET
    @Authenticated
    @Path("/getDatasetDashboard")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDatasetDashboard() {
        DashboardGenerator dashboardGenerator = new DashboardGenerator();
        String dashboard = dashboardGenerator.getNMNHDashboard(userContext.getUser().getUsername());

        return Response.ok("{\"dashboard\": \"" + dashboard + "\"}").build();
    }
}

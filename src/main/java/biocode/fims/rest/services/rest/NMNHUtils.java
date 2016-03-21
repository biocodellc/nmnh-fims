package biocode.fims.rest.services.rest;

import biocode.fims.rest.FimsService;
import biocode.fims.rest.filters.Authenticated;
import biocode.fims.utils.DashboardGenerator;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * NMNH-Fims utility services
 */
@Path("biocode/fims/utils/")
public class NMNHUtils extends FimsService {

    @GET
    @Authenticated
    @Path("/getDatasetDashboard")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDatasetDashboard() {
        DashboardGenerator dashboardGenerator = new DashboardGenerator();
        String dashboard = dashboardGenerator.getNMNHDashboard(username);

        return Response.ok("{\"dashboard\": \"" + dashboard + "\"}").build();
    }
}

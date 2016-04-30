package biocode.fims.rest.services.rest;

import biocode.fims.bcid.ExpeditionMinter;
import biocode.fims.bcid.ProjectMinter;
import biocode.fims.fimsExceptions.ForbiddenRequestException;
import biocode.fims.rest.FimsService;
import biocode.fims.rest.filters.Authenticated;
import biocode.fims.service.UserService;
import biocode.fims.settings.SettingsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

/**
 * REST services dealing with expeditions
 */
@Path("expeditions")
public class Expeditions extends FimsService {
    private static Logger logger = LoggerFactory.getLogger(Expeditions.class);

    @Autowired
    Expeditions(UserService userService, SettingsManager settingsManager) {
        super(userService, settingsManager);
    }

    /**
     * validateExpedition service checks the status of a new expedition code on the server and directing consuming
     * applications on whether this user owns the expedition and if it exists within an project or not.
     * Responses are error, update, or insert (first term followed by a colon)
     *
     * @param datasetCode
     * @param projectId
     *
     * @return
     */
    @GET
    @Authenticated
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    // THIS May mess things up later, jetty complaining about the APPLICATION_JSON mediatype
    @Produces(MediaType.TEXT_HTML)
    @Path("/validate/{projectId}/{datasetCode}")
    public Response validateExpedition(@PathParam("datasetCode") String datasetCode,
                                       @PathParam("projectId") Integer projectId) {
        ProjectMinter projectMinter = new ProjectMinter();
        ExpeditionMinter expedition = new ExpeditionMinter();

        // Decipher the expedition code
        try {
            datasetCode = URLDecoder.decode(datasetCode, "utf-8");
        } catch (UnsupportedEncodingException e) {
            logger.warn("UnsupportedEncodingException in expeditionService.mint method.", e);
        }

        //Check that the user exists in this project
        if (!projectMinter.userExistsInProject(userId, projectId)) {
            // If the user isn't in the project, then we can't update or create a new expedition
            throw new ForbiddenRequestException("User is not authorized to update/create expeditions in this project.");
        }

        // If specified, ignore the user.. simply figure out whether we're updating or inserting
        if (ignoreUser) {
            if (expedition.expeditionExistsInProject(datasetCode, projectId)) {
                return Response.ok("{\"update\": \"update this expedition\"}").build();
            } else {
                return Response.ok("{\"insert\": \"insert new expedition\"}").build();
            }
        }

        // Else, pay attention to what user owns the initial project
        else {
            if (expedition.userOwnsExpedition(userId, datasetCode, projectId)) {
                // If the user already owns the expedition, then great--- this is an update
                return Response.ok("{\"update\": \"user owns this expedition\"}").build();
                // If the expedition exists in the project but the user does not own the expedition then this means we can't
            } else if (expedition.expeditionExistsInProject(datasetCode, projectId)) {
                throw new ForbiddenRequestException("The dataset code '" + datasetCode +
                        "' exists in this project already and is owned by another user. " +
                        "Please choose another dataset code.");
            } else {
                return Response.ok("{\"insert\": \"the dataset code does not exist with project and nobody owns it\"}").build();
            }
        }
    }
}

package biocode.fims.rest.services.rest;

import biocode.fims.auth.LDAPAuthentication;
import biocode.fims.auth.NMNHAuthenticator;
import biocode.fims.auth.Authorizer;
import biocode.fims.bcid.BcidDatabase;
import biocode.fims.entities.User;
import biocode.fims.rest.FimsService;
import biocode.fims.service.UserService;
import biocode.fims.settings.SettingsManager;
import biocode.fims.utils.ErrorInfo;
import biocode.fims.utils.QueryParams;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;

/**
 * Authentication Services
 */
@Path("authenticationService")
public class NMNHAuthenticationService extends FimsService {

    private final UserService userService;
    @Autowired
    NMNHAuthenticationService(UserService userService, SettingsManager settingsManager) {
        super(userService, settingsManager);
        this.userService = userService;
    }

    /**
     * Service to log a user in with 2-factor authentication using LDAP & Entrust Identity Guard
     *
     * @param usr
     *
     * @throws IOException
     */
    @POST
    @Path("/loginLDAP")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response loginLDAP(@FormParam("username") String usr,
                              @FormParam("password") String pass,
                              @Context HttpServletRequest request) {
        LDAPAuthentication ldapAuthentication = new LDAPAuthentication();
        Integer numLdapAttemptsAllowed = Integer.parseInt(settingsManager.retrieveValue("ldapAttempts"));
        Integer ldapLockout = Integer.parseInt(settingsManager.retrieveValue("ldapLockedAccountTimeout"));

        if (!usr.isEmpty() && !pass.isEmpty()) {
            // ldap accounts lock after x # of attempts. We need to determine how many attempts the user currently has and inform
            // the user of a locked account
            Integer ldapAttempts = ldapAuthentication.getLoginAttempts(usr);

            if (ldapAttempts < numLdapAttemptsAllowed) {
                NMNHAuthenticator nmnhAuthenticator = new NMNHAuthenticator();
                String[] challengeQuestions;

                // attempt to login via ldap server. If ldap authentication is successful, then challenge questions are
                // retrieved from the Entrust IG server
                challengeQuestions = nmnhAuthenticator.loginLDAP(usr, pass, true);

                // if challengeQuestions is null, then ldap authentication failed
                if (challengeQuestions != null) {
                    //Construct query params
                    String queryParams = "?username=" + usr;
                    for (int i = 0; i < challengeQuestions.length; i++) {
                        queryParams += "&question_" + (i + 1) + "=" + challengeQuestions[i];
                    }
                    queryParams += "&" + request.getQueryString();

                    // need to return status 302 in order to pass SI vulnerabilities assessment
                    return Response.status(302).entity("{\"url\": \"" + appRoot + "entrustChallenge.jsp" + queryParams + "\"}")
                            .build();
                }

                // increase the number of attempts
                ldapAttempts += 1;
            }


            // if more then allowed number of ldap attempts, then the user is locked out of their account. We need to inform the user
            if (ldapAttempts >= numLdapAttemptsAllowed) {
                return Response.status(400)
                        .entity(new ErrorInfo("Your account is now locked for " + ldapLockout + " mins.", 400).toJSON())
                        .build();
            }

            return Response.status(400)
                    .entity(new ErrorInfo("Bad Credentials. " + (numLdapAttemptsAllowed - ldapAttempts) + " attempts remaining.",
                            400).toJSON())
                    .build();
        }
        return Response.status(400)
                .entity(new ErrorInfo("Empty Username or Password.", 400).toJSON())
                .build();
    }

    /**
     * Service to respond to Entrust IG challenge questions to complete authentication
     * @param returnTo
     * @param question1
     * @param question2
     * @param username
     * @return
     */
    @POST
    @Path("/entrustChallenge")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response entrustChallenge(@QueryParam("return_to") String returnTo,
                                     @FormParam("question_1") String question1,
                                     @FormParam("question_2") String question2,
                                     @FormParam("username") String username,
                                     @Context HttpServletRequest request) {
        String[] respChallenge = {question1, question2};

        if (username!= null && question1 != null && question2 != null) {
            NMNHAuthenticator authenticator = new NMNHAuthenticator();

            // verify with the entrust IG server that the correct responses were provided to the challenge questions
            // If so, then the user is logged in
            if (authenticator.entrustChallenge(username, respChallenge)) {
                User user = userService.getUser(username);
                // Place the user in the session
                session.setAttribute("user", user);
                Authorizer myAuthorizer = null;

                myAuthorizer = new Authorizer();

                // Check if the user is an admin for any projects
                if (myAuthorizer.userProjectAdmin(username)) {
                    session.setAttribute("projectAdmin", true);
                }

                // Redirect to return_to uri if provided
                if (returnTo != null) {

                    // check to see if oAuthLogin is in the session and set to true is so.
                    Object oAuthLogin = session.getAttribute("oAuthLogin");
                    if (oAuthLogin != null) {
                        session.setAttribute("oAuthLogin", true);
                    }

                    // need to return status 302 in order to pass SI vulnerabilities assessment
                    return Response.status(302).entity("{\"url\": \"" + returnTo +
                            new QueryParams().getQueryParams(request.getParameterMap(), true) + "\"}")
                            .build();
                } else {
                    // need to return status 302 in order to pass SI vulnerabilities assessment
                    return Response.status(302).entity("{\"url\": \"" + appRoot + "index.jsp\"}").build();
                }
            }
            return Response.status(500)
                    .entity(new ErrorInfo("Server Error", 500).toJSON())
                    .build();
        }
        return Response.status(400)
                .entity(new ErrorInfo("Bad Request", 400).toJSON())
                .build();
    }
}

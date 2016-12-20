package biocode.fims.auth;

import biocode.fims.bcid.BcidDatabase;
import biocode.fims.bcid.ProjectMinter;
import biocode.fims.fimsExceptions.ServerErrorException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * authentication methods needed for nmnh-fims
 */
public class NMNHAuthenticator {
    /**
     * Process 2-factor login as LDAP first and then entrust QA
     *
     * @param username
     * @param password
     * @param recognizeDemo
     *
     * @return
     */
    public String[] loginLDAP(String username, String password, Boolean recognizeDemo) {
        LDAPAuthentication ldapAuthentication = new LDAPAuthentication(username, password, recognizeDemo);

        // If ldap authentication is successful, then retrieve the challange questions from the entrust server
        if (ldapAuthentication.getStatus() == ldapAuthentication.SUCCESS) {
            EntrustIGAuthentication igAuthentication = new EntrustIGAuthentication();
            // get the challenge questions from entrust IG server
            String [] challengeQuestions = igAuthentication.getGenericChallenge(username);

            // challengeQuestions should never return null from here since the ldap authentication was successful.
            // However entrust IG server didn't provide any challenge questions, so throw an exception.
            if (challengeQuestions == null || challengeQuestions.length == 0) {
                throw new ServerErrorException("Server Error.", "No challenge questions provided");
            }

            return challengeQuestions;
        } else {
            // return null if the ldap authentication failed
            return null;
        }
    }

    /**
     * respond to a challenge from the Entrust Identity Guard Server
     * @param username
     * @param challengeResponse
     * @return
     */
    public boolean entrustChallenge(String username, String[] challengeResponse) {
        EntrustIGAuthentication igAuthentication = new EntrustIGAuthentication();
        // verify the user's responses to the challenge questions
        boolean isAuthenticated = igAuthentication.authenticateGenericChallange(username, challengeResponse);

        if (isAuthenticated) {
            if (!validUser(username)) {
                // If authentication is good and user doesn't exist in BcidDatabase, then insert account into database
                createLdapUser(username);

                // enable this user for all projects
                ProjectMinter projectMinter = new ProjectMinter();
                int userId = getUserId(username);
                // Loop projects and assign user to them
                ArrayList<Integer> projects = getAllProjectIds();
                for (Integer id: projects) {
                    projectMinter.addUserToProject(userId, id);
                }
            }
            return true;
        }

        return false;
    }

    /**
     * retrieve the user's hashed password from the BcidDatabase
     *
     * @return
     */
    private boolean validUser(String username) {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Connection conn = BcidDatabase.getConnection();
        try {
            String selectString = "SELECT count(*) FROM users WHERE username = ?";
            stmt = conn.prepareStatement(selectString);

            stmt.setString(1, username);
            rs = stmt.executeQuery();
            rs.next();
            return rs.getInt("count(*)") > 0;

        } catch (SQLException e) {
            throw new ServerErrorException(e);
        } finally {
            BcidDatabase.close(conn, stmt, rs);
        }
    }

    /**
     * return the userId given a username
     *
     * @param username
     *
     * @return
     */
    private Integer getUserId(String username) {
        Integer userId = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Connection conn = BcidDatabase.getConnection();
        try {
            String selectString = "SELECT userId FROM users WHERE username=?";
            stmt = conn.prepareStatement(selectString);

            stmt.setString(1, LDAPAuthentication.showShortUserName(username));

            rs = stmt.executeQuery();
            if (rs.next()) {
                userId = rs.getInt("userId");
            }

        } catch (SQLException e) {
            throw new ServerErrorException(e);
        } finally {
            BcidDatabase.close(conn, stmt, rs);
        }
        return userId;
    }

    /**
     * create a user given a username and password
     *
     * @param username
     *
     * @return
     */
    public Boolean createLdapUser(String username) {
        PreparedStatement stmt = null;
        Connection conn = BcidDatabase.getConnection();
        try {
            String insertString = "INSERT INTO users (username,hasSetPassword,institution,email,firstName,lastName,passwordResetToken,password,admin)" +
                    " VALUES(?,?,?,?,?,?,?,?,?)";
            stmt = conn.prepareStatement(insertString);

            stmt.setString(1, username);
            stmt.setInt(2, 1);
            stmt.setString(3, "Smithsonian Institution");
            stmt.setString(4, "");
            stmt.setString(5, "");
            stmt.setString(6, "");
            stmt.setString(7, "");
            stmt.setString(8, "");
            stmt.setInt(9, 0);

            stmt.execute();
            return true;
        } catch (SQLException e) {
            throw new ServerErrorException(e);
        } finally {
            BcidDatabase.close(conn, stmt, null);
        }
    }

    /**
     * List all the defined projects
     *
     * @return ArrauList<Integer> of all projectIds
     */
    public ArrayList<Integer> getAllProjectIds() {
        ArrayList<Integer> projects = new ArrayList<Integer>();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Connection conn = BcidDatabase.getConnection();

        try {
            String sql = "SELECT projectId FROM projects";
            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();
            while (rs.next()) {
                projects.add(rs.getInt("projectId"));
            }
            return projects;
        } catch (SQLException e) {
            throw new ServerErrorException("Trouble getting project List", e);
        } finally {
            BcidDatabase.close(conn, stmt, rs);
        }
    }
}

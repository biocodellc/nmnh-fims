package biocode.fims.utils;

import biocode.fims.bcid.ProjectMinter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.util.Iterator;

/**
 * class to generate the users dashboard
 */
public class DashboardGenerator {
    public DashboardGenerator() {
    }

    public String getNMNHDashboard(String username) {
        StringBuilder sb = new StringBuilder();
        int projectCounter = 1;
        int datasetCounter = 1;

        ProjectMinter projectMinter = new ProjectMinter();
        JSONObject projects = (JSONObject) JSONValue.parse(projectMinter.getMyTemplatesAndDatasets(username));

        sb.append("<h1>");
        sb.append(username);
        sb.append("'s Templates and Datasets</h1>\\n");

        // iterate over each project
        for (Iterator it = projects.keySet().iterator(); it.hasNext(); ) {
            String projectTitle = (String) it.next();
            sb.append("<br>\\n<a class='expand-content' id='");
            sb.append("project" + projectCounter);
            sb.append("' href='javascript:void(0);'>\\n");
            sb.append("\\t <img src='/images/right-arrow.png' id='arrow' class='img-arrow'>");
            sb.append(projectTitle);
            sb.append("</a>\\n");
            sb.append("<div class='toggle-content' id='");
            sb.append("project" + projectCounter);
            sb.append("'>");

            // iterate over each expedition
            for (Iterator it2 = ((JSONObject) projects.get(projectTitle)).keySet().iterator(); it2.hasNext(); ) {
                String expeditionTitle = (String) it2.next();
                JSONArray expeditionDatasets = (JSONArray) ((JSONObject) projects.get(projectTitle)).get(expeditionTitle);
                sb.append("<br>\\n<a class='expand-content' id='");
                sb.append("dataset" + datasetCounter);
                sb.append("' href='javascript:void(0);'>\\n");
                sb.append("\\t <img src='/images/right-arrow.png' id='arrow' class='img-arrow'>");
                sb.append(expeditionTitle);
                sb.append("</a>\\n");
                sb.append("<div class='toggle-content' id='");
                sb.append("dataset" + datasetCounter);
                sb.append("'>");
                sb.append("<table>\\n");
                sb.append("\\t<tr>\\n");
                sb.append("\\t\\t<th class='align_center'>Date</th>\\n");
                sb.append("\\t\\t<th>finalCopy</th>\\n");
                sb.append("\\t\\t<th class='align_center'>Dataset ARK</th>\\n");
                sb.append("\\t</tr>\\n");

                // inform the user that there is no datasets in the project
                if (expeditionDatasets.isEmpty()) {
                    sb.append("\\t<tr>\\n");
                    sb.append("\\t\\t<td colspan='4'>You have no datasets in this project.</td>\\n");
                    sb.append("\\t</tr>\\n");
                } else {

                    // iterate over the expeditions's datasets
                    for (Object d : expeditionDatasets) {
                        JSONObject dataset = (JSONObject) d;
                        sb.append("\\t<tr>\\n");

                        sb.append("\\t\\t<td>");
                        sb.append((String) dataset.get("ts"));
                        sb.append("</td>\\n");

                        sb.append("\\t\\t<td class='align_center'>");
                        if (dataset.get("finalCopy").equals("1")) {
                            sb.append("yes");
                        } else {
                            sb.append("no");
                        }
                        sb.append("</td>\\n");

                        // Direct Link
                        String identifier = (String) dataset.get("identifier");
                        if (identifier.contains("99999") || username.equalsIgnoreCase("demo")) {
                            sb.append("<td>not available for demonstration server or demo account</td>");
                        } else {
                            sb.append("<td><a href='");
                            sb.append("http://ezid.cdlib.org/id/" + identifier);
                            sb.append("'>");
                            sb.append("http://ezid.cdlib.org/id/" + identifier);
                            sb.append("</a></td>");
                        }

                        sb.append("\\t</tr>\\n");
                    }
                }
                sb.append("</table>\\n");
                sb.append("</div>\\n");
                datasetCounter ++;
            }

            sb.append("</div>\\n");
            projectCounter ++;
        }

        return sb.toString();
    }

    public static void main(String args[]) {
        DashboardGenerator dg = new DashboardGenerator();
        System.out.println(dg.getNMNHDashboard("demo"));
    }
}

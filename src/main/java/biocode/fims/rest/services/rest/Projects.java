package biocode.fims.rest.services.rest;

import biocode.fims.digester.*;
import biocode.fims.entities.Bcid;
import biocode.fims.rest.FimsService;
import biocode.fims.rest.filters.Authenticated;
import biocode.fims.run.ProcessController;
import biocode.fims.run.Process;
import biocode.fims.run.TemplateProcessor;
import biocode.fims.service.BcidService;
import biocode.fims.service.ExpeditionService;
import biocode.fims.service.UserService;
import biocode.fims.settings.SettingsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;
import java.util.List;

/**
 * REST services dealing with projects
 */
@Path("projects")
public class Projects extends FimsService {
    private static Logger logger = LoggerFactory.getLogger(Projects.class);

    private final ExpeditionService expeditionService;
    private final BcidService bcidService;

    @Autowired
    Projects(ExpeditionService expeditionService, BcidService bcidService,
             UserService userService, SettingsManager settingsManager) {
        super(userService, settingsManager);
        this.expeditionService = expeditionService;
        this.bcidService = bcidService;
    }

    @GET
    @Authenticated
    @Path("/{projectId}/getDefinition/{columnName}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDefinitions(@PathParam("projectId") int projectId,
                                   @PathParam("columnName") String columnName) {
        TemplateProcessor t = new TemplateProcessor(projectId, uploadPath(), true);
        StringBuilder output = new StringBuilder();

        Iterator attributes = t.getMapping().getAllAttributes(t.getMapping().getDefaultSheetName()).iterator();
        // Get a list of rules for the first digester.Worksheet instance
        Worksheet sheet = t.getValidation().getWorksheets().get(0);

        List<Rule> rules = sheet.getRules();


        while (attributes.hasNext()) {
            Attribute a = (Attribute) attributes.next();
            String column = a.getColumn();
            if (columnName.trim().equals(column.trim())) {
                // The column name
                output.append("<b>Column Name: " + columnName + "</b><p>");

                // URI
                if (a.getUri() != null) {
                    output.append("URI = " +
                            "<a href='" + a.getUri() + "' target='_blank'>" +
                            a.getUri() +
                            "</a><br>\n");
                }
                // Defined_by
                if (a.getDefined_by() != null) {
                    output.append("Defined_by = " +
                            "<a href='" + a.getDefined_by() + "' target='_blank'>" +
                            a.getDefined_by() +
                            "</a><br>\n");
                }

                // Definition
                if (a.getDefinition() != null && !a.getDefinition().trim().equals("")) {
                    output.append("<p>\n" +
                            "<b>Definition:</b>\n" +
                            "<p>" + a.getDefinition() + "\n");
                } else {
                    output.append("<p>\n" +
                            "<b>Definition:</b>\n" +
                            "<p>No custom definition available\n");
                }

                // Synonyms
                if (a.getSynonyms() != null && !a.getSynonyms().trim().equals("")) {
                    output.append("<p>\n" +
                            "<b>Synonyms:</b>\n" +
                            "<p>" + a.getSynonyms() + "\n");
                }

                // Synonyms
                if (a.getDataformat() != null && !a.getDataformat().trim().equals("")) {
                    output.append("<p>\n" +
                            "<b>Data Formatting Instructions:</b>\n" +
                            "<p>" + a.getDataformat() + "\n");
                }

                // Rules
                Iterator it = rules.iterator();
                StringBuilder ruleValidations = new StringBuilder();
                while (it.hasNext()) {

                    Rule r = (Rule) it.next();
                    r.setDigesterWorksheet(sheet);

                    if (r != null) {
                        biocode.fims.digester.List sList = t.getValidation().findList(r.getList());

                        // Convert to native state (without underscores)
                        String ruleColumn = r.getColumn();

                        if (ruleColumn != null) {
                            // Match column names with or without underscores
                            if (ruleColumn.replace("_", " ").equals(column) ||
                                    ruleColumn.equals(column)) {
                                ruleValidations.append(printRuleMetadata(r, sList));
                            }
                        }
                    }
                }
                if (!ruleValidations.toString().equals("")) {
                    output.append("<p>\n" +
                            "<b>Validation Rules:</b>\n<p>");
                    output.append(ruleValidations.toString());
                }

                return Response.ok(output.toString()).build();
            }
        }

        return Response.ok("No definition found for " + columnName).build();
    }

    /**
     * Print ruleMetadata
     *
     * @param sList We pass in a List of fields we want to associate with this rule
     *
     * @return
     */
    private String printRuleMetadata(Rule r, biocode.fims.digester.List sList) {
        StringBuilder output = new StringBuilder();
        output.append("<li>\n");
        //
        if (r.getType().equals("checkInXMLFields")) {
            r.setType("Lookup Value From List");
        }
        // Display the Rule type
        output.append("\t<li>type: " + r.getType() + "</li>\n");
        // Display warning levels
        output.append("\t<li>level: " + r.getLevel() + "</li>\n");
        // Display values
        if (r.getValue() != null) {
            try {
                output.append("\t<li>value: " + URLDecoder.decode(r.getValue(), "utf-8") + "</li>\n");
            } catch (UnsupportedEncodingException e) {
                output.append("\t<li>value: " + r.getValue() + "</li>\n");
                logger.warn("UnsupportedEncodingException", e);
            }
        }
        // Display fields
        // Convert XML Field values to a Stringified list
        List listFields;
        if (sList != null && sList.getFields().size() > 0) {
            listFields = sList.getFields();
        } else {
            listFields = r.getFields();
        }
        Iterator it;
        try {
            it = listFields.iterator();
        } catch (NullPointerException e) {
            logger.warn("NullPointerException", e);
            return output.toString();
        }
        // One or the other types of list need data
        if (!it.hasNext())
            return output.toString();

        output.append("\t<li>list: \n");

        // Look at the Fields
        output.append("\t\t<ul>\n");

        if (it != null) {
            while (it.hasNext()) {
                String field = ((Field) it.next()).getValue();
                //String field = (String) it.next();
                output.append("\t\t\t<li>" + field + "</li>\n");
            }
        }
        output.append("\t\t</ul>\n");
        output.append("\t</li>\n");

        output.append("</li>\n");
        return output.toString();
    }

    @GET
    @Authenticated
    @Path("/{projectId}/attributes")
    @Produces(MediaType.TEXT_HTML)
    public Response getAttributes(@PathParam("projectId") int projectId) {
        TemplateProcessor t = new TemplateProcessor(projectId, uploadPath(), true);
        LinkedList<String> requiredColumns = t.getRequiredColumns("error");
        LinkedList<String> desiredColumns = t.getRequiredColumns("warning");
        // Use TreeMap for natural sorting of groups
        Map<String, StringBuilder> groups = new TreeMap<String, StringBuilder>();

        //StringBuilder output = new StringBuilder();
        // A list of names we've already added
        ArrayList addedNames = new ArrayList();
        Iterator attributes = t.getMapping().getAllAttributes(t.getMapping().getDefaultSheetName()).iterator();
        while (attributes.hasNext()) {
            Attribute a = (Attribute) attributes.next();

            StringBuilder thisOutput = new StringBuilder();
            // Set the column name
            String column = a.getColumn();
            String group = a.getGroup();
            String uri = a.getUri();

            // Check that this name hasn't been read already.  This is necessary in some situations where
            // column names are repeated for different entities in the configuration file
            if (!addedNames.contains(column)) {
                // Set boolean to tell us if this is a requiredColumn
                Boolean aRequiredColumn = false, aDesiredColumn = false;
                if (requiredColumns == null) {
                    aRequiredColumn = false;
                } else if (requiredColumns.contains(a.getColumn())) {
                    aRequiredColumn = true;
                }
                if (desiredColumns == null) {
                    aDesiredColumn = false;
                } else if (desiredColumns.contains(a.getColumn())) {
                    aDesiredColumn = true;
                }


                // Construct the checkbox text
                thisOutput.append("<input type='checkbox' class='check_boxes' value='" + column + "' data-uri='");
                thisOutput.append(uri);
                thisOutput.append("'");

                // If this is a required column then make it checked (and immutable)
                if (aRequiredColumn)
                    thisOutput.append(" checked disabled");
                else if (aDesiredColumn)
                    thisOutput.append(" checked");

                // Close tag and insert Definition link
                thisOutput.append(">" + column + " \n" +
                        "<a href='#' class='def_link' name='" + column + "'>DEF</a>\n" + "<br>\n");

                // Fetch any existing content for this key
                if (group == null || group.equals("")) {
                    group = "Default Group";
                }
                StringBuilder existing = groups.get(group);

                // Append (not required) or Insert (required) the new content onto any existing in this key
                if (existing == null) {
                    existing = thisOutput;
                } else {
                    if (aRequiredColumn) {
                        existing.insert(0, thisOutput);
                    } else {
                        existing.append(thisOutput);
                    }
                }
                groups.put(group, existing);
            }

            // Now that we've added this to the output, add it to the ArrayList so we don't add it again
            addedNames.add(column);
        }

        // Iterate through any defined groups, which makes the template processor easier to navigate
        Iterator it = groups.entrySet().iterator();
        StringBuilder output = new StringBuilder();
        output.append("<a href='#' id='select_all'>Select ALL</a> | ");
        output.append("<a href='#' id='select_none'>Select NONE</a> | ");
        output.append("<a href='#' onclick='saveTemplateConfig()'>Save</a>");
        output.append("<script>" +
                "$('#select_all').click(function(event) {\n" +
                "      // Iterate each checkbox\n" +
                "      $(':checkbox').each(function() {\n" +
                "          this.checked = true;\n" +
                "      });\n" +
                "  });\n" +
                "$('#select_none').click(function(event) {\n" +
                "    $(':checkbox').each(function() {\n" +
                "       if (!$(this).is(':disabled')) {\n" +
                "          this.checked = false;}\n" +
                "      });\n" +
                "});" +
                "</script>");

        int count = 0;
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry) it.next();
            String groupName;

            try {
                groupName = pairs.getKey().toString();
            } catch (NullPointerException e) {
                groupName = "Default Group";
            }
            if (groupName.equals("") || groupName.equals("null")) {
                groupName = "Default Group";
            }

            // Anchors cannot have spaces in the name so we replace them with underscores
            String massagedGroupName = groupName.replaceAll(" ", "_");
            if (!pairs.getValue().toString().equals("")) {
                output.append("<div class=\"panel panel-default\">");
                output.append("<div class=\"panel-heading\"> " +
                        "<h4 class=\"panel-title\"> " +
                        "<a class=\"accordion-toggle\" data-toggle=\"collapse\" data-parent=\"#accordion\" href=\"#" + massagedGroupName + "\">" + groupName + "</a> " +
                        "</h4> " +
                        "</div>");
                output.append("<div id=\"" + massagedGroupName + "\" class=\"panel-collapse collapse");
                // Make the first element open initially
                if (count == 0) {
                    output.append(" in");
                }
                output.append("\">\n" +
                        "                <div class=\"panel-body\">\n" +
                        "                    <div id=\"" + massagedGroupName + "\" class=\"panel-collapse collapse in\">");
                output.append(pairs.getValue().toString());
                output.append("\n</div></div></div></div>");
            }

            it.remove(); // avoids a ConcurrentModificationException
            count++;
        }
        return Response.ok(output.toString()).build();
    }

    @POST
    @Authenticated
    @Path("/createExcel/")
    @Produces("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response createExcel(
            @FormParam("fields") List<String> fields,
            @FormParam("projectId") Integer projectId,
            @FormParam("accession_number") String accessionNumber,
            @FormParam("dataset_code") String datasetCode,
            @FormParam("operation") String operation) {

        if (accessionNumber == null || datasetCode == null) {
            return Response.status(400).entity("{\"error\": " +
                    "\"This is a NMNH project. Accession number and Dataset Code are required.}").build();
        }

        // Create the configuration file
        if (!datasetCode.matches("^\\w{4,50}$")) {
            return Response.status(400).entity("{\"error\": \"The Dataset Code must be an alphanumeric between" +
                    " 4 and 50 characters.").build();
        }
        if (!accessionNumber.matches("^\\d{6,20}$")) {
            return Response.status(400).entity("{\"error\": \"The Accession Number must be an integer with greater" +
                    " or equal to 6 numbers").build();
        }


        if (operation == null || !operation.equalsIgnoreCase("update")) {
            operation = "insert";
        }

        ProcessController processController = new ProcessController(projectId, datasetCode);
        processController.setAccessionNumber(accessionNumber);

        Process p = new Process(
                uploadPath(),
                processController,
                expeditionService);

        // Handle creating an expedition on template generation
        if (operation.equalsIgnoreCase("insert")) {
            processController.setUserId(user.getUserId());
            String expedition_title =
                    processController.getExpeditionCode() +
                    " spreadsheet" +
                    "(accession " + accessionNumber + ")";

            processController.setExpeditionTitle(expedition_title);
            p.runExpeditionCreate(bcidService);
        }

        // Create the template processor which handles all functions related to the template, reading, generation
        // Get the ARK associated with this dataset code
        // TODO: Resource may change in future... better to figure this out programatically at some point
        String identifier = null;
        try {
            Bcid rootBcid = expeditionService.getRootBcid(datasetCode, projectId, "Resource");
            identifier = String.valueOf(rootBcid.getIdentifier());
        } catch (EmptyResultDataAccessException e) {}

        // Construct the new templateProcessor
        TemplateProcessor templateProcessor = new TemplateProcessor(
                projectId,
                uploadPath(),
                true,
                accessionNumber,
                datasetCode,
                identifier,
                user.getUsername());

        // Set the default sheet-name
        String defaultSheetname = templateProcessor.getMapping().getDefaultSheetName();

        File file = templateProcessor.createExcelFile(defaultSheetname, uploadPath(), fields);

        // Catch a null file and return 204
        if (file == null)
            return Response.status(204).build();

        // Return response
        Response.ResponseBuilder response = Response.ok(file);
        response.header("Content-Disposition",
                "attachment; filename=" + file.getName());
        return response.build();
    }
}

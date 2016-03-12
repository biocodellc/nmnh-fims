package services;

import biocode.fims.bcid.*;
import biocode.fims.config.ConfigurationFileTester;
import biocode.fims.digester.Mapping;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.rest.FimsService;
import biocode.fims.rest.filters.Authenticated;
import biocode.fims.run.Process;
import biocode.fims.run.ProcessController;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.json.simple.JSONObject;
import tools.SIServerSideSpreadsheetTools;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.File;
import java.io.InputStream;

/**
 */
@Path("validate")
public class Validate extends FimsService {

    /**
     * service to validate a dataset against a project's rules
     *
     * @param projectId
     * @param expeditionCode
     * @param upload
     * @param is
     * @param fileData
     *
     * @return
     */
    @POST
    @Authenticated
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public String validate(@FormDataParam("projectId") Integer projectId,
                           @FormDataParam("expeditionCode") String expeditionCode,
                           @FormDataParam("upload") String upload,
                           @FormDataParam("finalCopy") String finalCopy,
                           @FormDataParam("dataset") InputStream is,
                           @FormDataParam("dataset") FormDataContentDisposition fileData) {
        StringBuilder retVal = new StringBuilder();
        Boolean removeController = true;
        Boolean deleteInputFile = true;
        String inputFile;

        // create a new processController
        ProcessController processController = new ProcessController(projectId, expeditionCode);

        // place the processController in the session here so that we can track the status of the validation process
        // by calling rest/validate/status
        session.setAttribute("processController", processController);


        // update the status
        processController.appendStatus("Initializing...<br>");
        processController.appendStatus("inputFilename = " + processController.stringToHTMLJSON(
                fileData.getFileName()) + "<br>");

        // Save the uploaded file
        String splitArray[] = fileData.getFileName().split("\\.");
        String ext;
        if (splitArray.length == 0) {
            // if no extension is found, then guess
            ext = "xls";
        } else {
            ext = splitArray[splitArray.length - 1];
        }
        inputFile = processController.saveTempFile(is, ext);
        // if input_file null, then there was an error saving the file
        if (inputFile == null) {
            throw new FimsRuntimeException("Server error saving file.", 500);
        }

        processController.setInputFilename(inputFile);

        // Create the process object --- this is done each time to orient the application
        Process p = new Process(
                uploadPath(),
                processController
        );

        // Test the configuration file to see that we're good to go...
        ConfigurationFileTester cFT = new ConfigurationFileTester();
        boolean configurationGood = true;

        cFT.init(p.configFile);

        if (!cFT.checkUniqueKeys()) {
            String message = "<br>CONFIGURATION FILE ERROR...<br>Please talk to your project administrator to fix the following error:<br>\t\n";
            message += cFT.getMessages();
            processController.setHasErrors(true);
            processController.setValidated(false);
            processController.appendStatus(message + "<br>");
            configurationGood = false;
            retVal.append("{\"done\": ");
            retVal.append(processController.getStatusSB().toString());
            retVal.append("}");
        }


        // Run the process only if the configuration is good.
        if (configurationGood) {
            processController.appendStatus("Validating...<br>");

            p.runValidation();

            // if there were validation errors, we can't upload
            if (processController.getHasErrors()) {
                retVal.append("{\"done\": ");
                retVal.append(processController.getMessages().toJSONString());
                retVal.append("}");

            } else if (upload != null && upload.equals("on")) {

                processController.setUserId(userId);

                // set final copy to true in processController if user wants it on
                if (finalCopy != null && finalCopy.equals("on")) {
                    processController.setFinalCopy(true);
                }

                // if there were vaildation warnings and user would like to upload, we need to ask the user to continue
                if (!processController.isValidated() && processController.getHasWarnings()) {
                    retVal.append("{\"continue\": ");
                    retVal.append(processController.getMessages().toJSONString());
                    retVal.append("}");

                    // there were no validation warnings and the user would like to upload, so continue
                } else {
                    retVal.append("{\"continue\": {\"message\": \"continue\"}}");
                }

                // don't delete the inputFile because we'll need it for uploading
                deleteInputFile = false;

                // don't remove the controller as we will need it later for uploading this file
                removeController = false;

            } else {
                retVal.append("{\"done\": ");
                retVal.append(processController.getMessages().toJSONString());
                retVal.append("}");
            }
        }

        if (deleteInputFile && inputFile != null) {
            new File(inputFile).delete();
        }
        if (removeController) {
            session.removeAttribute("processController");
        }

        return retVal.toString();
    }

    /**
     * Service to upload a dataset to an expedition. The validate service must be called before this service.
     *
     * @param createExpedition
     *
     * @return
     */
    @GET
    @Authenticated
    @Path("/continue")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public String upload(@QueryParam("createExpedition") @DefaultValue("false") Boolean createExpedition) {
        ProcessController processController = (ProcessController) session.getAttribute("processController");

        // if no processController is found, we can't do anything
        if (processController == null) {
            return "{\"error\": \"No process was detected.\"}";
        }

        // if the process controller was stored in the session, then the user wants to continue, set warning cleared
        processController.setClearedOfWarnings(true);
        processController.setValidated(true);

        // Create the process object --- this is done each time to orient the application
        Process p = new Process(
                uploadPath(),
                processController
        );

        // create this expedition if the user wants to
        if (createExpedition) {
            p.runExpeditionCreate();
        }

        if (!processController.isExpeditionAssignedToUserAndExists()) {
            p.runExpeditionCheck();
        }

        // Check to see if we need to create a new Expedition, if so we make a slight diversion
        if (processController.isExpeditionCreateRequired()) {
            // Ask the user if they want to create this expedition
            return "{\"continue\": {\"message\": \"The dataset code \\\"" + JSONObject.escape(processController.getExpeditionCode()) +
                    "\\\" does not exist.  " +
                    "Do you wish to create it now?<br><br>" +
                    "If you choose to continue, your data will be associated with this new dataset code.\"}}";
        }

        /*
         * Copy Spreadsheet to a standard location
        */
        // Set input and output files
        File inputFile = new File(processController.getInputFilename());

        File outputFile = new File(sm.retrieveValue("serverRoot") + inputFile.getName());

        // Run guidify, which adds a BCID to the spreadsheet

        // Get the mapping object so we can discern the column_internal fields
        Mapping mapping = processController.getMapping();

        // Represent the dataset by an ARK... In the Spreadsheet Uploader option this
        // gives us a way to track what spreadsheets are uploaded into the system as they can
        // be tracked in the mysql database.  They also get an ARK but that is probably not useful.

        boolean ezidRequest = Boolean.valueOf(sm.retrieveValue("ezidRequests"));

        // Mint the data group
        BcidMinter bcidMinter = new BcidMinter(ezidRequest);
        String identifier = bcidMinter.createEntityBcid(
                new Bcid(
                        processController.getUserId(),
                        "http://purl.org/dc/dcmitype/Dataset",
                        processController.getExpeditionCode() + " Dataset",
                        "",
                        inputFile.getName(),
                        null,
                        processController.getFinalCopy(),
                        false));

        // Associate the expeditionCode with this identifier
        ExpeditionMinter expedition = new ExpeditionMinter();
        expedition.attachReferenceToExpedition(processController.getExpeditionCode(), identifier, processController.getProjectId());


        // Get the BCID Root
        Resolver r = new Resolver(processController.getExpeditionCode(), processController.getProjectId(), "Resource");
        String bcidRoot = r.getIdentifier();
        // Smithsonian specific GUID to be attached to Sheet
        SIServerSideSpreadsheetTools siServerSideSpreadsheetTools = new SIServerSideSpreadsheetTools(
                inputFile,
                processController.getWorksheetName(),
                mapping.getDefaultSheetUniqueKey(),
                bcidRoot);

        // Write GUIDs
        siServerSideSpreadsheetTools.guidify();

        siServerSideSpreadsheetTools.addInternalRowToHeader(mapping, Boolean.valueOf(sm.retrieveValue("replaceHeader")));

        siServerSideSpreadsheetTools.write(outputFile);


        // delete the temporary file now that it has been uploaded
        new File(processController.getInputFilename()).delete();

        // Remove the processController from the session
        session.removeAttribute("processController");

        processController.appendStatus("<br><font color=#188B00>Successfully Uploaded!</font>");

        // This is the message the user sees after succesfully uploading a spreadsheet to the server
        return "{\"done\": {\"message\": \"Successfully uploaded your spreadsheet to the server!<br>" +
                "dataset code = " + processController.getExpeditionCode() + "<br>" +
                "dataset ARK = " + identifier + "<br>" +
                "resource ARK = " + bcidRoot + "<br>" +
                "Please maintain a local copy of your File!<br>" +
                "Your file will be processed soon for ingestion into RCIS.\"}}";
    }

    /**
     * Service used for getting the current status of the dataset validation/upload.
     *
     * @return
     */
    @GET
    @Path("/status")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public String status() {
        ProcessController processController = (ProcessController) session.getAttribute("processController");
        if (processController == null) {
            return "{\"error\": \"Waiting for validation to process...\"}";
        }

        return "{\"status\": \"" + processController.getStatusSB().toString() + "\"}";
    }
}



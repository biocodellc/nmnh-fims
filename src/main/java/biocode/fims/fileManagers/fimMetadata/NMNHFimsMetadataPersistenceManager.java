package biocode.fims.fileManagers.fimMetadata;

import biocode.fims.digester.Entity;
import biocode.fims.entities.Bcid;
import biocode.fims.fileManagers.fimsMetadata.FimsMetadataPersistenceManager;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.run.ProcessController;
import biocode.fims.service.ExpeditionService;
import biocode.fims.settings.SettingsManager;
import biocode.fims.tools.SIServerSideSpreadsheetTools;
import org.json.simple.JSONArray;

import java.io.File;

/**
 * @author RJ Ewing
 */
public class NMNHFimsMetadataPersistenceManager implements FimsMetadataPersistenceManager {
    private final SettingsManager settingsManager;
    private final ExpeditionService expeditionService;
    private ProcessController processController;

    public NMNHFimsMetadataPersistenceManager(SettingsManager settingsManager, ExpeditionService expeditionService) {
        this.settingsManager = settingsManager;
        this.expeditionService = expeditionService;
    }

    @Override
    public void upload(ProcessController processController, JSONArray fimsMetadata) {
        // only set the processController here. Actual "uploading" will happen in the writeSourceFile method
        this.processController = processController;
    }

    @Override
    public boolean validate(ProcessController processController) {
        return true;
    }

    @Override
    public String writeSourceFile(File sourceFile, int bcidId) {
        File outputFile = new File(settingsManager.retrieveValue("serverRoot") + sourceFile.getName());

        // Get the BCID Root
        Entity rootEntity = processController.getMapping().getRootEntity();
        Bcid rootEntityBcid = expeditionService.getEntityBcid(
                processController.getExpeditionCode(), processController.getProjectId(), rootEntity.getConceptAlias());

        if (rootEntityBcid == null) {
            throw new FimsRuntimeException("Server Error", "rootEntityBcid is null", 500);
        }

        rootEntity.setIdentifier(rootEntityBcid.getIdentifier());

        // Smithsonian specific GUID to be attached to Sheet
        SIServerSideSpreadsheetTools siServerSideSpreadsheetTools = new SIServerSideSpreadsheetTools(
                sourceFile,
                processController.getMapping().getDefaultSheetName(),
                processController.getMapping().getDefaultSheetUniqueKey(),
                String.valueOf(rootEntityBcid.getIdentifier()));

        // Write GUIDs
        siServerSideSpreadsheetTools.guidify();

        siServerSideSpreadsheetTools.addInternalRowToHeader(
                processController.getMapping(),
                Boolean.valueOf(settingsManager.retrieveValue("replaceHeader"))
        );

        siServerSideSpreadsheetTools.write(outputFile);

        return outputFile.getName();
    }

    @Override
    public String getWebAddress() {
        return null;
    }

    @Override
    public String getGraph() {
        return null;
    }

    @Override
    public JSONArray getDataset(ProcessController processController) {
        return null;
    }
}

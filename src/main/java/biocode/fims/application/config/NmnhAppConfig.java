package biocode.fims.application.config;

import biocode.fims.fileManagers.fimMetadata.NMNHFimsMetadataPersistenceManager;
import biocode.fims.fileManagers.fimsMetadata.FimsMetadataFileManager;
import biocode.fims.fileManagers.fimsMetadata.FimsMetadataPersistenceManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.*;

/**
 * Configuration class for Nmnh-Fims applications. Including cli and webapps
 */
@Configuration
@Import({FimsAppConfig.class})
// declaring this here allows us to override any properties that are also included in biocode-fims.props
@PropertySource(value = "classpath:biocode-fims.props", ignoreResourceNotFound = true)
@PropertySource("classpath:nmnh-fims.props")
public class NmnhAppConfig {
    @Autowired
    FimsAppConfig fimsAppConfig;
    @Autowired
    private MessageSource messageSource;

    @Bean
    @Scope("prototype")
    public FimsMetadataFileManager fimsMetadataFileManager() {
        FimsMetadataPersistenceManager persistenceManager = new NMNHFimsMetadataPersistenceManager(
                fimsAppConfig.settingsManager,
                fimsAppConfig.expeditionService
        );
        return new FimsMetadataFileManager(persistenceManager, fimsAppConfig.settingsManager,
                fimsAppConfig.expeditionService, fimsAppConfig.bcidService, messageSource);
    }
}


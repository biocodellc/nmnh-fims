package biocode.fims.application.config;

import biocode.fims.fileManagers.AuxilaryFileManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;

import java.util.ArrayList;
import java.util.List;


/**
 * configuration class for nmnh-fims webapp
 */
@Configuration
@Import({NmnhAppConfig.class, FimsWebAppConfig.class})
public class NmnhWebAppConfig {
    @Bean
    @Scope("prototype")
    public List<AuxilaryFileManager> fileManagers() {
        return new ArrayList<>();
    }

}

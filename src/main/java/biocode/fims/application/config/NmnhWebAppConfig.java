package biocode.fims.application.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;


/**
 * configuration class for nmnh-fims webapp
 */
@Configuration
@Import({NmnhAppConfig.class, FimsWebAppConfig.class})
public class NmnhWebAppConfig {

}

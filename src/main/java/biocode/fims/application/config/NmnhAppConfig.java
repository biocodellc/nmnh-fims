package biocode.fims.application.config;

import org.springframework.context.annotation.*;

/**
 * Configuration class for Nmnh-Fims applications. Including cli and webapps
 */
@Configuration
@Import({FimsAppConfig.class})
public class NmnhAppConfig {
}


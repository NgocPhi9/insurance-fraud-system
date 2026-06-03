package group102.insurancefraud.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.investigation")
@Getter
@Setter
public class AppConfig {
    private int maxClaimsPerInvestigator = 3;
    private int pageSize = 10;
}

package com.campussafety;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class DatasourceConfigurationTest {

    @Test
    void buildsMysqlUrlWithConfiguredTlsModeWhenNoFullUrlOverrideIsSet() {
        StandardEnvironment environment = applicationEnvironment(Map.of(
                "DB_HOST", "mysql.example.invalid",
                "DB_PORT", "18082",
                "DB_NAME", "defaultdb",
                "DB_SSL_MODE", "REQUIRED"));

        assertThat(environment.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:mysql://mysql.example.invalid:18082/defaultdb"
                        + "?sslMode=REQUIRED&allowPublicKeyRetrieval=true&serverTimezone=UTC");
    }

    @Test
    void allowsAnExplicitFullJdbcUrlToOverrideConstructedConnectionSettings() {
        String url = "jdbc:mysql://mysql.example.invalid:18082/defaultdb?sslMode=VERIFY_IDENTITY";
        StandardEnvironment environment = applicationEnvironment(Map.of("DB_URL", url));

        assertThat(environment.getProperty("spring.datasource.url")).isEqualTo(url);
    }

    private StandardEnvironment applicationEnvironment(Map<String, Object> runtimeProperties) {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application.yml"));

        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new PropertiesPropertySource(
                "applicationYaml", Objects.requireNonNull(yaml.getObject())));
        environment.getPropertySources().addFirst(new MapPropertySource("runtime", runtimeProperties));
        return environment;
    }
}

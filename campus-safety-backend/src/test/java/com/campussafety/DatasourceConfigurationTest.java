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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DatasourceConfigurationTest {

    @Test
    void buildsMysqlUrlWithTlsRequiredByDefault() {
        StandardEnvironment environment = applicationEnvironment(Map.of(
                "DB_HOST", "mysql.example.invalid",
                "DB_PORT", "18082",
                "DB_NAME", "defaultdb"));

        assertThat(environment.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:mysql://mysql.example.invalid:18082/defaultdb"
                        + "?sslMode=REQUIRED&allowPublicKeyRetrieval=true&serverTimezone=UTC");
    }

    @Test
    void honorsExplicitRequiredTlsMode() {
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

    @Test
    void failsClearlyWhenNeitherFullUrlNorRequiredHostSettingsAreAvailable() {
        StandardEnvironment environment = applicationEnvironment(Map.of());

        assertThatThrownBy(() -> environment.getProperty("spring.datasource.url"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Could not resolve placeholder 'DB_HOST'");
    }

    private StandardEnvironment applicationEnvironment(Map<String, Object> runtimeProperties) {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application.yml"));

        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
        environment.getPropertySources().remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
        environment.getPropertySources().addFirst(new PropertiesPropertySource(
                "applicationYaml", Objects.requireNonNull(yaml.getObject())));
        environment.getPropertySources().addFirst(new MapPropertySource("runtime", runtimeProperties));
        return environment;
    }
}

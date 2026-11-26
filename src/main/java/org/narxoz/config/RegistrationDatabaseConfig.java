package org.narxoz.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile(RegistrationUsers.PROFILE)
class RegistrationDatabaseConfig {
    @Bean
    FlywayMigrationStrategy registrationMigrationStrategy() {
        return labMigration -> {
            labMigration.migrate();
            // A separate history keeps the base Lab 5 profile valid on the same database.
            Flyway.configure()
                    .dataSource(labMigration.getConfiguration().getDataSource())
                    .schemas("registration")
                    .locations("classpath:db/registration")
                    .load()
                    .migrate();
        };
    }
}

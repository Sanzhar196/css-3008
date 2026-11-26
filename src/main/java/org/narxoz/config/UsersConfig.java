package org.narxoz.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

@Configuration
public class UsersConfig {

    /** Laptop only. Two classroom users with BCrypt hashes. */
    @Bean
    @Profile("dev & !registration & !prod")
    @Primary
    UserDetailsService devUsers(PasswordEncoder encoder) {
        return classroomUsers(encoder);
    }

    @Bean
    @Profile(RegistrationUsers.PROFILE)
    RegistrationUsers registrationUsers(PasswordEncoder encoder, JdbcTemplate jdbc) {
        return new RegistrationUsers(classroomUsers(encoder), encoder, jdbc);
    }

    private InMemoryUserDetailsManager classroomUsers(PasswordEncoder encoder) {
        return new InMemoryUserDetailsManager(
                User.withUsername("clerk")
                        .password(encoder.encode("clerk-dev"))
                        .roles("CLERK")
                        .build(),
                User.withUsername("manager")
                        .password(encoder.encode("manager-dev"))
                        .roles("MANAGER")
                        .build());
    }

    /** Prod ships no users in this lab: every login is 401. */
    @Bean
    @Profile("!dev | prod")
    UserDetailsService noUsers(Environment environment) {
        if (environment.matchesProfiles("dev & prod")) {
            throw new IllegalStateException("Activate either dev or prod, not both");
        }
        return new InMemoryUserDetailsManager();
    }
}

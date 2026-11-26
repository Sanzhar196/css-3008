package org.narxoz.config;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

import java.util.Locale;
import java.util.Optional;

/** Adds PostgreSQL-backed CLERK accounts to the classroom login service. */
public class RegistrationUsers implements UserDetailsService {
    public static final String PROFILE = "dev & registration & !prod";
    public static final String ROLE = "CLERK";

    private final InMemoryUserDetailsManager classroomUsers;
    private final PasswordEncoder encoder;
    private final JdbcTemplate jdbc;

    RegistrationUsers(InMemoryUserDetailsManager classroomUsers, PasswordEncoder encoder, JdbcTemplate jdbc) {
        this.classroomUsers = classroomUsers;
        this.encoder = encoder;
        this.jdbc = jdbc;
    }

    public Optional<String> register(String username, String password) {
        String key = username.toLowerCase(Locale.ROOT);
        if (classroomUsers.userExists(key)) {
            return Optional.empty();
        }
        int added = jdbc.update("""
                INSERT INTO registration.account (username, password_hash, role)
                VALUES (?, ?, ?)
                ON CONFLICT (username) DO NOTHING
                """, key, encoder.encode(password), ROLE);
        return added == 1 ? Optional.of(key) : Optional.empty();
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        String key = username.toLowerCase(Locale.ROOT);
        if (classroomUsers.userExists(key)) {
            return classroomUsers.loadUserByUsername(username);
        }
        UserDetails stored = jdbc.query("""
                SELECT password_hash, role
                FROM registration.account
                WHERE username = ?
                """, rows -> rows.next()
                ? User.withUsername(key)
                        .password(rows.getString("password_hash"))
                        .roles(rows.getString("role"))
                        .build()
                : null, key);
        if (stored == null) {
            throw new UsernameNotFoundException("Unknown user");
        }
        return stored;
    }
}

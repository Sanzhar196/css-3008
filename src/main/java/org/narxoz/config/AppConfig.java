package org.narxoz.config;

import org.narxoz.domain.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class AppConfig {

    @Bean
    public Rule rules() {
        return new RequestPolicy(List.of(new TransitionRule(), new OnlyApprovedCanCancelRule()));
    }

    @Bean
    CommandLineRunner demo(RequestService service) {
        return args -> System.out.println("DRAFT -> APPROVED: "
                + service.move(RequestStatus.DRAFT, RequestStatus.APPROVED));
    }
}
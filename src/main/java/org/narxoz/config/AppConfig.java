package org.narxoz.config;

import org.narxoz.client.DownstreamProperties;
import org.narxoz.domain.OnlyApprovedCanCancelRule;
import org.narxoz.domain.RequestId;
import org.narxoz.domain.RequestPolicy;
import org.narxoz.domain.RequestStatus;
import org.narxoz.domain.Rule;
import org.narxoz.domain.TransitionRule;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@EnableConfigurationProperties(DownstreamProperties.class)
public class AppConfig {

    @Bean
    public Rule rules() {
        return new RequestPolicy(List.of(new TransitionRule(), new OnlyApprovedCanCancelRule()));
    }

    @Bean
    CommandLineRunner demo(RequestService service) {
        return args -> System.out.println("DRAFT -> APPROVED: "
                + service.move(RequestId.newId(), RequestStatus.DRAFT, RequestStatus.APPROVED));
    }
}

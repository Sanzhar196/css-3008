package org.narxoz.handler;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class MeController {
    @GetMapping("/api/me")
    @Operation(summary = "Show the authenticated username and authorities")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Current authenticated user"),
            @ApiResponse(responseCode = "401", description = "Valid token required")
    })
    public MeResponse me(Authentication authentication) {
        return new MeResponse(authentication.getName(), authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).toList());
    }

    public record MeResponse(String username, List<String> authorities) { }
}

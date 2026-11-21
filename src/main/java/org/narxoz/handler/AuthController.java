package org.narxoz.handler;

import jakarta.validation.Valid;
import org.narxoz.config.TokenService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthenticationManager authentication;
    private final TokenService tokens;

    public AuthController(AuthenticationManager authentication, TokenService tokens) {
        this.authentication = authentication;
        this.tokens = tokens;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest body) {
        Authentication checked = authentication.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(body.username(), body.password()));
        List<String> roles = checked.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        TokenService.IssuedToken issued = tokens.issue(checked.getName(), roles);
        return new LoginResponse(issued.value(), "Bearer", issued.expiresInSeconds());
    }
}

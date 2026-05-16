package com.energiaclara.api.rest;

import com.energiaclara.api.rest.audit.Audited;
import com.energiaclara.api.rest.dto.LoginRequest;
import com.energiaclara.api.rest.dto.LoginResponse;
import com.energiaclara.api.rest.dto.RegisterRequest;
import com.energiaclara.application.dto.LoginCommand;
import com.energiaclara.application.dto.LoginResult;
import com.energiaclara.application.dto.RegisterUserCommand;
import com.energiaclara.application.port.in.LoginUseCase;
import com.energiaclara.application.port.in.RegisterUserUseCase;
import com.energiaclara.domain.model.vo.Email;
import com.energiaclara.domain.model.vo.TenantId;
import com.energiaclara.domain.model.vo.UserId;
import com.energiaclara.infrastructure.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final LoginUseCase loginUseCase;
    private final RegisterUserUseCase registerUserUseCase;

    public AuthController(LoginUseCase loginUseCase, RegisterUserUseCase registerUserUseCase) {
        this.loginUseCase = loginUseCase;
        this.registerUserUseCase = registerUserUseCase;
    }

    @PostMapping("/login")
    @Audited(action = "LOGIN", entity = "User")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginCommand command = new LoginCommand(
                Email.of(request.email()),
                request.password(),
                TenantId.of(request.tenantId())
        );
        LoginResult result = loginUseCase.login(command);
        return ResponseEntity.ok(new LoginResponse(
                result.token(), result.userId(), result.tenantId(), result.roles()
        ));
    }

    @PostMapping("/register")
    @PreAuthorize("hasRole('ADMIN_INSTITUCION')")
    @Audited(action = "REGISTER_USER", entity = "User", entityIdExpression = "#result.body['userId']")
    public ResponseEntity<Map<String, String>> register(
            @Valid @RequestBody RegisterRequest request,
            @AuthenticationPrincipal AuthenticatedUser current) {
        RegisterUserCommand command = new RegisterUserCommand(
                TenantId.of(request.tenantId()),
                Email.of(request.email()),
                request.fullName(),
                request.password(),
                request.roles(),
                UserId.of(current.userId())
        );
        UserId userId = registerUserUseCase.register(command);
        return ResponseEntity.ok(Map.of("userId", userId.toString()));
    }
}

package com.projetointegrador.seumentor.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.projetointegrador.seumentor.user.api.dtos.ForgotPasswordRequest;
import com.projetointegrador.seumentor.user.api.dtos.ResetPasswordRequest;
import com.projetointegrador.seumentor.user.api.UserCommand;

import com.projetointegrador.seumentor.security.auth.AuthenticationRequest;
import com.projetointegrador.seumentor.security.auth.AuthenticationResponse;
import com.projetointegrador.seumentor.security.auth.RegisterRequest;
import com.projetointegrador.seumentor.security.service.AuthenticationService;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Endpoints para registro, login e recuperação de senha")
public class AuthenticationController {

    private final AuthenticationService authenticationService;
    private final UserCommand userCommandService;
    private static final Logger log = LoggerFactory.getLogger(AuthenticationController.class);

    @PostMapping("/register")
    @Operation(summary = "Registra um novo usuário", description = "Cria uma nova conta de usuário no sistema.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Registro bem-sucedido",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AuthenticationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Erro no registro (ex: e-mail já existe, dados inválidos)",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AuthenticationResponse.class)))
    })
    public ResponseEntity<AuthenticationResponse> register(
            @RequestBody(description = "Dados necessários para o registro", required = true,
                    content = @Content(schema = @Schema(implementation = RegisterRequest.class)))
            @org.springframework.web.bind.annotation.RequestBody RegisterRequest request) {
        try {
            AuthenticationResponse response = authenticationService.register(request);

            log.info("Registration successful for email: {}", request.email());
            return ResponseEntity.ok(response);
        } catch (Throwable e) {
            log.error("Registration failed for email {}: {}", request.email(), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new AuthenticationResponse("Erro no registro: " + e.getMessage(), null));
        }
    }

    @PostMapping("/authenticate")
    @Operation(summary = "Autentica um usuário", description = "Verifica as credenciais (e-mail e senha) e retorna um token JWT e userId se forem válidas.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Autenticação bem-sucedida",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AuthenticationResponse.class))),
            @ApiResponse(responseCode = "401", description = "Credenciais inválidas (e-mail ou senha incorretos)",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AuthenticationResponse.class)))
    })
    public ResponseEntity<AuthenticationResponse> authenticate(
            @RequestBody(description = "Credenciais do usuário para login", required = true,
                    content = @Content(schema = @Schema(implementation = AuthenticationRequest.class)))
            @org.springframework.web.bind.annotation.RequestBody AuthenticationRequest request) {
        try {
            AuthenticationResponse response = authenticationService.authenticate(request);
            log.info("Authentication successful for email: {}", request.email());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.warn("Authentication failed for email {}: {}", request.email(), e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthenticationResponse("Erro na autenticação: " + e.getMessage(), null));
        }
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Solicita redefinição de senha", description = "Inicia o processo de recuperação de senha. Envia um e-mail com um link/token para o usuário, se o e-mail estiver cadastrado.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Solicitação processada. Um e-mail será enviado se o endereço estiver cadastrado.",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Se o email estiver cadastrado, um link de redefinição será enviado."))),
    })
    public ResponseEntity<?> forgotPassword(
            @RequestBody(description = "E-mail do usuário que esqueceu a senha", required = true,
                    content = @Content(schema = @Schema(implementation = ForgotPasswordRequest.class)))
            @org.springframework.web.bind.annotation.RequestBody ForgotPasswordRequest request) {
        try {
            userCommandService.requestPasswordReset(request.email());
            log.info("Password reset request processed for email: {}", request.email());
            return ResponseEntity.ok().body("Se o email estiver cadastrado, um link de redefinição será enviado.");
        } catch (Exception e) {
            log.error("Error during forgot password process for email {}: {}", request.email(), e.getMessage(), e);
            return ResponseEntity.ok().body("Se o email estiver cadastrado, um link de redefinição será enviado.");
        }
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Redefine a senha", description = "Define uma nova senha para o usuário usando o token recebido por e-mail.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Senha redefinida com sucesso.",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Senha redefinida com sucesso."))),
            @ApiResponse(responseCode = "400", description = "Erro ao redefinir senha (token inválido, expirado ou senha fora dos padrões).",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Erro ao redefinir senha: Token de redefinição inválido ou expirado.")))
    })
    public ResponseEntity<?> resetPassword(
            @RequestBody(description = "Token de redefinição e nova senha", required = true,
                    content = @Content(schema = @Schema(implementation = ResetPasswordRequest.class)))
            @org.springframework.web.bind.annotation.RequestBody ResetPasswordRequest request) {
        try {
            userCommandService.resetPassword(request.token(), request.newPassword());
            log.info("Password successfully reset using token (token not logged)");
            return ResponseEntity.ok().body("Senha redefinida com sucesso.");
        } catch (Exception e) {
            log.warn("Password reset failed using token (token not logged): {}", e.getMessage());
            return ResponseEntity.badRequest().body("Erro ao redefinir senha: " + e.getMessage());
        }
    }
}
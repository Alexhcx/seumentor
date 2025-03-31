package com.projetointegrador.seumentor.user.controller;

import org.springframework.http.HttpStatus; 
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
public class AuthenticationController {

    private final AuthenticationService authenticationService;
    private final UserCommand userCommandService; // Inject UserCommandService
    private static final Logger log = LoggerFactory.getLogger(AuthenticationController.class);


    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(
            @RequestBody RegisterRequest request) {
        try {
             AuthenticationResponse response = authenticationService.register(request);
             log.info("Registration successful for email: {}", request.getEmail());
             return ResponseEntity.ok(response);
        } catch (Throwable e) {
            log.error("Registration failed for email {}: {}", request.getEmail(), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(AuthenticationResponse.builder().token("Erro no registro: " + e.getMessage()).build());
        }
    }

    @PostMapping("/authenticate")
    public ResponseEntity<AuthenticationResponse> authenticate(
            @RequestBody AuthenticationRequest request) {
         try {
            AuthenticationResponse response = authenticationService.authenticate(request);
            log.info("Authentication successful for email: {}", request.getEmail());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
             log.warn("Authentication failed for email {}: {}", request.getEmail(), e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                           .body(AuthenticationResponse.builder().token("Erro na autenticação: " + e.getMessage()).build());
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        try {
            userCommandService.requestPasswordReset(request.email());
            log.info("Password reset request processed for email: {}", request.email());
            return ResponseEntity.ok().body("Se o email estiver cadastrado, um link de redefinição será enviado.");
        } catch (Exception e) {
             log.error("Error during forgot password process for email {}: {}", request.email(), e.getMessage(), e);
             return ResponseEntity.ok().body("Se o email estiver cadastrado, um link de redefinição será enviado.");
             // return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro interno ao processar a solicitação.");
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
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
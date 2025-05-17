package com.projetointegrador.seumentor.security.service;

import com.projetointegrador.seumentor.user.api.UserQuery;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.projetointegrador.seumentor.security.auth.AuthenticationRequest;
import com.projetointegrador.seumentor.security.auth.AuthenticationResponse;
import com.projetointegrador.seumentor.security.auth.RegisterRequest;
import com.projetointegrador.seumentor.user.api.UserCommand;
import com.projetointegrador.seumentor.user.api.dtos.UserRegistrationRequest;
import com.projetointegrador.seumentor.user.api.dtos.UserRepresentation;
import com.projetointegrador.seumentor.user.service.UsersDetailsAdapter;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UsersDetailsAdapter userDetailsAdapter;
    private final UserCommand userCommandService;
    private final UserQuery userQuery;

    public AuthenticationResponse register(RegisterRequest request) throws Throwable {
        UserRegistrationRequest registrationReq = new UserRegistrationRequest(
                request.firstName(),
                request.lastName(),
                request.email(),
                request.cpf(),
                request.phone(),
                request.password(),
                "USER");

        UserRepresentation createdUser = userCommandService.createUser(registrationReq);

        UserDetails userDetails = userDetailsAdapter.findByEmail(createdUser.email())
                .orElseThrow(
                        () -> new UsernameNotFoundException("Usuário recém-criado não encontrado (UserDetails): " + createdUser.email()));

        String jwtToken = jwtService.generateToken(userDetails);

        return new AuthenticationResponse(jwtToken, createdUser.id());
    }

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                ));

        UserDetails userDetails = userDetailsAdapter.findByEmail(request.email())
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuário não encontrado (UserDetails) após autenticação bem-sucedida: " + request.email()));

        UserRepresentation userRep = userQuery.findByEmail(request.email())
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuário não encontrado (UserRepresentation) após autenticação bem-sucedida: " + request.email()));

        String jwtToken = jwtService.generateToken(userDetails);

        return new AuthenticationResponse(jwtToken, userRep.id());
    }
}
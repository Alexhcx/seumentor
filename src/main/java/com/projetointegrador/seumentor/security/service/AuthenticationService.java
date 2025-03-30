package com.projetointegrador.seumentor.security.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.projetointegrador.seumentor.security.auth.AuthenticationRequest;
import com.projetointegrador.seumentor.security.auth.AuthenticationResponse;
import com.projetointegrador.seumentor.security.auth.RegisterRequest;
import com.projetointegrador.seumentor.user.api.UserCommand;
import com.projetointegrador.seumentor.user.api.UsersDetailsAdapter;
import com.projetointegrador.seumentor.user.api.dtos.UserRegistrationRequest;
import com.projetointegrador.seumentor.user.api.dtos.UserRepresentation;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

  private final AuthenticationManager authenticationManager;
  private final JwtService jwtService;
  private final UsersDetailsAdapter userDetailsAdapter;
  private final UserCommand userCommandService;

  public AuthenticationResponse register(RegisterRequest request) throws Throwable {

    UserRegistrationRequest registrationReq = new UserRegistrationRequest(
        request.getFirstName(),
        request.getLastName(),
        request.getEmail(),
        request.getPassword(),
        "USER");

    UserRepresentation createdUser = userCommandService.createUser(registrationReq);

    UserDetails userDetails = userDetailsAdapter.findByEmail(createdUser.email())
        .orElseThrow(
            () -> new UsernameNotFoundException("Usuário recém-criado não encontrado: " + createdUser.email()));
    String jwtToken = jwtService.generateToken(userDetails);

    return AuthenticationResponse.builder().token(jwtToken).build();
  }

  public AuthenticationResponse authenticate(AuthenticationRequest request) {
    authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(
            request.getEmail(),
            request.getPassword()));
    UserDetails userDetails = userDetailsAdapter.findByEmail(request.getEmail())
        .orElseThrow(() -> new UsernameNotFoundException(
            "Usuário não encontrado após autenticação bem-sucedida: " + request.getEmail()));

    String jwtToken = jwtService.generateToken(userDetails);

    return AuthenticationResponse.builder().token(jwtToken).build();
  }

}

package com.projetointegrador.seumentor.user.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.projetointegrador.seumentor.user.api.UserCommand;
import com.projetointegrador.seumentor.user.api.dtos.UserRegistrationRequest;
import com.projetointegrador.seumentor.user.api.dtos.UserRepresentation;
import com.projetointegrador.seumentor.user.repository.UserRepository;
import com.projetointegrador.seumentor.user.model.Role;
import com.projetointegrador.seumentor.user.model.UserModel;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor 
public class UserCommandService implements UserCommand {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder; 
  

  @Override
  @Transactional 
  public UserRepresentation createUser(UserRegistrationRequest request) throws Exception {

    if (userRepository.existsByEmail(request.email())) {
      throw new Exception("Email já cadastrado: " + request.email());
    }

    Role userRole;
    try {
      userRole = Role.valueOf(request.role().toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new Exception("Role inválida: " + request.role());
    }

    UserModel newUser = new UserModel();
    newUser.setFirstName(request.firstName());
    newUser.setLastName(request.lastName());
    newUser.setEmail(request.email());

    newUser.setPassword(passwordEncoder.encode(request.password()));

    newUser.setRole(userRole);

    UserModel savedUser = userRepository.save(newUser);

    return mapToRepresentation(savedUser);
  }

  private UserRepresentation mapToRepresentation(UserModel user) {
    return new UserRepresentation(user.getId(), user.getFirstName(), user.getLastName(), user.getEmail());
  }
}

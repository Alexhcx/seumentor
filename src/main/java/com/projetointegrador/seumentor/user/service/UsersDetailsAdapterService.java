package com.projetointegrador.seumentor.user.service;

import com.projetointegrador.seumentor.user.api.UsersDetailsAdapter;
import com.projetointegrador.seumentor.user.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UsersDetailsAdapterService implements UsersDetailsAdapter {

  private final UserRepository userRepository;

  @Override
  public Optional<UserDetails> findByEmail(String username) {
    // Busca UserModel e faz o cast para UserDetails, já que UserModel implementa
    // UserDetails
    return userRepository.findByEmail(username)
        .map(userModel -> (UserDetails) userModel);
    // Alternativamente: .map(userModel -> userModel); se o Optional<UserModel> for
    // aceito
  }

  @Override
  public Optional<UserDetails> loadUserDetailsById(Integer id) {
    return userRepository.findById(id)
        .map(userModel -> (UserDetails) userModel);
    // Alternativamente: .map(userModel -> userModel);
  }
}

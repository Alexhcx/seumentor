package com.projetointegrador.seumentor.user.service;

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
    return userRepository.findByEmail(username)
        .map(userModel -> (UserDetails) userModel);
  }

  @Override
  public Optional<UserDetails> loadUserDetailsById(Long id) {
    return userRepository.findById(id)
        .map(userModel -> (UserDetails) userModel);
  }
}

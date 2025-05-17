package com.projetointegrador.seumentor.user.service;

import org.springframework.security.core.userdetails.UserDetails;
import java.util.Optional;

public interface UsersDetailsAdapter {
    Optional<UserDetails> findByEmail(String username);
    Optional<UserDetails> loadUserDetailsById(Long id);
}

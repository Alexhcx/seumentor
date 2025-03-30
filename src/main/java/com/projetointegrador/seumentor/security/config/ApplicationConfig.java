package com.projetointegrador.seumentor.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.projetointegrador.seumentor.user.api.UsersDetailsAdapter;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class ApplicationConfig {

  private final UsersDetailsAdapter userQuery;

  @Bean
  public UserDetailsService userDetailsService() {
    return username -> userQuery.findByEmail(username)
        .orElseThrow(() -> new UsernameNotFoundException("user not found"));
  }
}

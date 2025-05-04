package com.projetointegrador.seumentor.user.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class FavoriteDisciplineNotFoundException extends RuntimeException {
  public FavoriteDisciplineNotFoundException(String message) {
    super(message);
  }
}

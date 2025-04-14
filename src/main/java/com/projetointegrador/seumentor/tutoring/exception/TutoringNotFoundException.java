package com.projetointegrador.seumentor.tutoring.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class TutoringNotFoundException extends RuntimeException {
    public TutoringNotFoundException(String message) {
        super(message);
    }
}

package com.projetointegrador.seumentor.course.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class DisciplineNotFoundException extends RuntimeException {
    public DisciplineNotFoundException(String message) {
        super(message);
    }
}

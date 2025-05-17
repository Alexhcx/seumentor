// src/main/java/com/projetointegrador/seumentor/tutoring/exception/TutoringOperationException.java
package com.projetointegrador.seumentor.tutoring.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class TutoringOperationException extends RuntimeException {
    public TutoringOperationException(String message) {
        super(message);
    }

    public TutoringOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
package com.projetointegrador.seumentor.course.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class CourseAreaNotFoundException extends RuntimeException {
    public CourseAreaNotFoundException(String message) {
        super(message);
    }
}

package com.projetointegrador.seumentor.user.api;

import com.projetointegrador.seumentor.user.api.dtos.UserRegistrationRequest;
import com.projetointegrador.seumentor.user.api.dtos.UserRepresentation;

public interface UserCommand {

  UserRepresentation createUser(UserRegistrationRequest registrationRequest) throws Throwable;

}

package com.projetointegrador.seumentor.user.api;

import com.projetointegrador.seumentor.user.api.dtos.ChangePasswordRequest;
import com.projetointegrador.seumentor.user.api.dtos.UserRegistrationRequest;
import com.projetointegrador.seumentor.user.api.dtos.UserRepresentation;

public interface UserCommand {

  UserRepresentation createUser(UserRegistrationRequest registrationRequest) throws Throwable;

  void requestPasswordReset(String email) throws Exception;

  void resetPassword(String token, String newPassword) throws Exception;

  void changeUserPassword(Long userId, ChangePasswordRequest request) throws Exception;

}

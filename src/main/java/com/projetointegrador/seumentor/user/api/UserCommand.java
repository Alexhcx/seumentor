package com.projetointegrador.seumentor.user.api;

import com.projetointegrador.seumentor.user.api.dtos.ChangePasswordRequest;
import com.projetointegrador.seumentor.user.api.dtos.UserRegistrationRequest;
import com.projetointegrador.seumentor.user.api.dtos.UserRepresentation;
import com.projetointegrador.seumentor.user.api.dtos.UserUpdateRequest;

public interface UserCommand {

  UserRepresentation createUser(UserRegistrationRequest registrationRequest) throws Throwable;

  UserRepresentation updateUser(Long userId, UserUpdateRequest request);

  void deleteUser(Long userId);

  void requestPasswordReset(String email) throws Exception;

  void resetPassword(String token, String newPassword) throws Exception;

  void changeUserPassword(Long userId, ChangePasswordRequest request) throws Exception;
  
  void promoteToMentor(Long userId);

}

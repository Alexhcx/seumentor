package com.projetointegrador.seumentor.user.api.dtos;

public record ResetPasswordRequest(String token, String newPassword ) {

}

package com.projetointegrador.seumentor.user.api.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados para atualização do perfil de um usuário. Todos os campos são opcionais.")
public record UserUpdateRequest(
        @Schema(description = "Nova URL da imagem de perfil do usuário", example = "https://example.com/new_profile.png")
        String profileImg,

        @Schema(description = "Nova data de nascimento do usuário (Formato YYYY-MM-DD)", example = "1991-06-20")
        String birthday,

        @Schema(description = "Novo número de telefone do usuário (apenas dígitos)", example = "11987654321")
        String phone,

        @Schema(description = "Nova cidade do usuário", example = "Rio de Janeiro")
        String city,

        @Schema(description = "Novo estado do usuário", example = "RJ")
        String state,

        @Schema(description = "Novo país do usuário", example = "Brasil")
        String country,

        @Schema(description = "Novo nome do curso do usuário", example = "Ciência da Computação")
        String courseName,

        @Schema(description = "Novo semestre atual do usuário", example = "6")
        String semester,

        @Schema(description = "Novo nome da universidade do usuário", example = "Universidade Federal ABC")
        String university
) {}

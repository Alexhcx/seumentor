package com.projetointegrador.seumentor.user.api.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Representação dos dados de um usuário")
public record UserRepresentation(
        @Schema(description = "ID único do usuário", example = "1")
        Long id,

        @Schema(description = "Primeiro nome do usuário", example = "João")
        String firstName,

        @Schema(description = "Sobrenome do usuário", example = "Silva")
        String lastName,

        @Schema(description = "Endereço de e-mail do usuário (utilizado para login)", example = "joao.silva@example.com")
        String email,

        @Schema(description = "URL da imagem de perfil do usuário", example = "https://example.com/profile.jpg")
        String profileImg,

        @Schema(description = "Data de nascimento do usuário", example = "1990-05-15")
        String birthday,

        @Schema(description = "Cidade do usuário", example = "São Paulo")
        String city,

        @Schema(description = "Estado do usuário", example = "SP")
        String state,

        @Schema(description = "País do usuário", example = "Brasil")
        String country,

        @Schema(description = "Nome do curso do usuário", example = "Engenharia de Software")
        String courseName,

        @Schema(description = "Semestre atual do usuário no curso", example = "5")
        String semester,

        @Schema(description = "Nome da universidade do usuário", example = "Universidade XYZ")
        String university
) {}

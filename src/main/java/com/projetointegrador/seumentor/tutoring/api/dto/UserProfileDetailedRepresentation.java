package com.projetointegrador.seumentor.tutoring.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Representação detalhada do perfil de um usuário, incluindo suas mentorias como mentor e como participante.")
public record UserProfileDetailedRepresentation(
        @Schema(description = "ID único do usuário", example = "1")
        Long id,

        @Schema(description = "Primeiro nome do usuário", example = "João")
        String firstName,

        @Schema(description = "Sobrenome do usuário", example = "Silva")
        String lastName,

        @Schema(description = "Endereço de e-mail do usuário", example = "joao.silva@example.com")
        String email,

        @Schema(description = "URL da imagem de perfil do usuário", example = "https://example.com/profile.jpg")
        String profileImg,

        @Schema(description = "Data de nascimento do usuário (Formato YYYY-MM-DD)", example = "1990-05-15")
        String birthday,

        @Schema(description = "Número do CPF do usuário (formatado)", example = "123.456.789-00")
        String cpf,

        @Schema(description = "Número de telefone do usuário", example = "11987654321")
        String phone,

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
        String university,

        @Schema(description = "Lista de mentorias em que o usuário é o mentor")
        List<TutoringRepresentation> tutoringMentor,

        @Schema(description = "Lista de mentorias em que o usuário participa (mentorado)")
        List<TutoringRepresentation> tutoringMentee
) {}

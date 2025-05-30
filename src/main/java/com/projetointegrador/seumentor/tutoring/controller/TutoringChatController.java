package com.projetointegrador.seumentor.tutoring.controller;


import com.projetointegrador.seumentor.tutoring.api.TutoringChatCommand;
import com.projetointegrador.seumentor.tutoring.api.dto.*;
import com.projetointegrador.seumentor.tutoring.exception.TutoringNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/tutoring")
@RequiredArgsConstructor
@Tag(name = "Mentorias (Tutoring)", description = "Endpoints para agendamento, gerenciamento e avaliação de mentorias")
@SecurityRequirement(name = "bearerAuth")
public class TutoringChatController {

  private final TutoringChatCommand tutoringCommandService;
  private static final Logger log = LoggerFactory.getLogger(TutoringController.class);

  @PatchMapping("/{tutoringId}/chat/enable")
  @PreAuthorize("hasAuthority('ADMIN') or @tutoringSecurityService.isMentorOfTutoring(authentication, #tutoringId)")
  @Operation(summary = "Habilita ou Desabilita o chat para uma mentoria", description = "Permite que o mentor da mentoria ou um ADMIN habilite/desabilite o chat.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Status do chat atualizado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TutoringRepresentation.class))),
      @ApiResponse(responseCode = "403", description = "Acesso negado"),
      @ApiResponse(responseCode = "404", description = "Mentoria não encontrada")
  })
  public ResponseEntity<TutoringRepresentation> toggleChatEnabled(
      @Parameter(description = "ID da mentoria") @PathVariable Long tutoringId,
      @RequestBody Map<String, Boolean> payload,
      Authentication authentication) {
    Boolean enable = payload.get("enable");
    if (enable == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payload deve conter a chave 'enable' (true/false).");
    }
    log.info("Controller: User {} solicitando {} o chat para a mentoria ID: {}", authentication.getName(),
        enable ? "habilitar" : "desabilitar", tutoringId);
    try {
      TutoringRepresentation updatedTutoring = tutoringCommandService.setChatEnabled(tutoringId, enable,
          authentication);
      return ResponseEntity.ok(updatedTutoring);
    } catch (TutoringNotFoundException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
    } catch (AccessDeniedException e) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e);
    } catch (Exception e) {
      log.error("Controller: Erro ao atualizar status do chat para mentoria {}: {}", tutoringId, e.getMessage(), e);
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao atualizar status do chat.", e);
    }
  }

  @PatchMapping("/{tutoringId}/chat/mentor-posting-only")
  @PreAuthorize("hasAuthority('ADMIN') or @tutoringSecurityService.isMentorOfTutoring(authentication, #tutoringId)")
  @Operation(summary = "Define se apenas o mentor pode postar no chat geral", description = "Permite que o mentor da mentoria ou um ADMIN restrinja/libere o envio de mensagens no chat geral.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Modo de postagem do chat atualizado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TutoringRepresentation.class))),
      @ApiResponse(responseCode = "403", description = "Acesso negado"),
      @ApiResponse(responseCode = "404", description = "Mentoria não encontrada")
  })
  public ResponseEntity<TutoringRepresentation> toggleMentorPostingOnly(
      @Parameter(description = "ID da mentoria") @PathVariable Long tutoringId,
      @RequestBody Map<String, Boolean> payload,
      Authentication authentication) {
    Boolean mentorOnly = payload.get("mentorOnly");
    if (mentorOnly == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Payload deve conter a chave 'mentorOnly' (true/false).");
    }
    log.info("Controller: User {} solicitando definir mentorPostingOnly={} para a mentoria ID: {}",
        authentication.getName(), mentorOnly, tutoringId);
    try {
      TutoringRepresentation updatedTutoring = tutoringCommandService.setMentorPostingOnly(tutoringId, mentorOnly,
          authentication);
      return ResponseEntity.ok(updatedTutoring);
    } catch (TutoringNotFoundException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
    } catch (AccessDeniedException e) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e);
    } catch (Exception e) {
      log.error("Controller: Erro ao atualizar modo de postagem do chat para mentoria {}: {}", tutoringId,
          e.getMessage(), e);
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao atualizar modo de postagem do chat.",
          e);
    }
  }
}

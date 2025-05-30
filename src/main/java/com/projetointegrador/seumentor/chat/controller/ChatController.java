package com.projetointegrador.seumentor.chat.controller;

import com.projetointegrador.seumentor.chat.dto.ChatOutputDTO;
import com.projetointegrador.seumentor.chat.service.ChatService;
import com.projetointegrador.seumentor.tutoring.exception.TutoringNotFoundException; // Se ChatService pode lançá-la diretamente
import com.projetointegrador.seumentor.tutoring.service.TutoringSecurityService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
@Tag(name = "Chat", description = "Endpoints para funcionalidades de chat, como histórico de mensagens")
@SecurityRequirement(name = "bearerAuth")
public class ChatController {

  private final ChatService chatService;
  private final TutoringSecurityService tutoringSecurityService;
  private static final Logger log = LoggerFactory.getLogger(ChatController.class);

  @GetMapping("/tutoring/{tutoringId}/history")
  @PreAuthorize("hasAuthority('ADMIN') or @tutoringSecurityService.isUserParticipantOrMentorOrAdmin(authentication, #tutoringId)")
  @Operation(summary = "Busca o histórico de mensagens de uma mentoria", description = "Retorna todas as mensagens de uma mentoria específica. Requer autenticação e que o usuário seja participante ou mentor da mentoria, ou ADMIN.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Histórico de mensagens retornado com sucesso", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ChatOutputDTO.class)))),
      @ApiResponse(responseCode = "403", description = "Acesso negado"),
      @ApiResponse(responseCode = "404", description = "Mentoria não encontrada"),
      @ApiResponse(responseCode = "500", description = "Erro interno no servidor")
  })
  public ResponseEntity<List<ChatOutputDTO>> getChatHistory(
      @Parameter(description = "ID da mentoria para buscar o histórico do chat", required = true) @PathVariable Long tutoringId,
      Authentication authentication) {
    log.info("ChatController: Usuário {} solicitando histórico de chat para mentoria ID: {}", authentication.getName(),
        tutoringId);
    try {
      if (!tutoringSecurityService.isUserParticipantOrMentorOrAdmin(authentication, tutoringId)) {
        throw new AccessDeniedException("Acesso negado ao histórico do chat desta mentoria.");
      }

      List<ChatOutputDTO> history = chatService.getChatHistory(tutoringId);
      return ResponseEntity.ok(history);
    } catch (TutoringNotFoundException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
    } catch (AccessDeniedException e) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e);
    } catch (Exception e) {
      log.error("ChatController: Erro ao buscar histórico de chat para mentoria {}: {}", tutoringId, e.getMessage(), e);
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao buscar histórico do chat.", e);
    }
  }
}
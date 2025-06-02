// src/main/java/com/projetointegrador/seumentor/cloudstorage/controller/StorageController.java
package com.projetointegrador.seumentor.cloudstorage.controller;

import com.projetointegrador.seumentor.chat.dto.ChatInput;
import com.projetointegrador.seumentor.chat.dto.ChatOutputDTO;
import com.projetointegrador.seumentor.chat.enums.MessageType;
import com.projetointegrador.seumentor.chat.service.ChatService;
import com.projetointegrador.seumentor.user.api.UserQuery;
import com.projetointegrador.seumentor.user.api.dtos.UserProfileImgIdRepresentation;
import com.projetointegrador.seumentor.user.exception.UserNotFoundException;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.projetointegrador.seumentor.cloudstorage.service.StorageService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cloudstorage/file")
@Tag(name = "Armazenamento em Nuvem (Cloud Storage)", description = "Endpoints para upload, download e gerenciamento de arquivos")
@SecurityRequirement(name = "bearerAuth")
public class StorageController {

    private final StorageService storageService;
    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserQuery userQuery;
    private static final Logger log = LoggerFactory.getLogger(StorageController.class);

    public StorageController(StorageService storageService,
            ChatService chatService,
            SimpMessagingTemplate messagingTemplate,
            UserQuery userQuery) {
        this.storageService = storageService;
        this.chatService = chatService;
        this.messagingTemplate = messagingTemplate;
        this.userQuery = userQuery;
    }

    @PostMapping("/upload/{mentoriaId}")
    @PreAuthorize("hasAuthority('ADMIN') or @tutoringSecurityService.isUserParticipantOrMentorOrAdmin(authentication, #mentoriaId)")
    @Operation(summary = "Upload de um arquivo para mentoria", description = "Faz o upload de um arquivo para uma mentoria específica, salva o link no chat e retorna uma URL assinada válida por 60 minutos. Requer que o usuário seja participante/mentor da mentoria ou ADMIN.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Arquivo enviado com sucesso e link enviado ao chat", content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE, schema = @Schema(type = "string", format = "url"))),
            @ApiResponse(responseCode = "400", description = "Requisição inválida"),
            @ApiResponse(responseCode = "401", description = "Não Autorizado"),
            @ApiResponse(responseCode = "403", description = "Acesso Negado"),
            @ApiResponse(responseCode = "404", description = "Recurso não encontrado (Mentoria ou Usuário)"),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor")
    })
    public ResponseEntity<String> uploadFile(
            @Parameter(description = "Arquivo a ser enviado", required = true, content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)) @RequestParam(value = "file") MultipartFile file,
            @Parameter(description = "ID da mentoria", required = true, example = "123") @PathVariable Long mentoriaId,
            Authentication authentication) {


        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("Upload de arquivo falhou: usuário não autenticado tentando fazer upload para mentoria {}", mentoriaId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuário não autenticado.");
        }

        String filePathS3;
        String fileUrl;

        try {
            filePathS3 = storageService.uploadMentoriaFile(file, String.valueOf(mentoriaId), authentication);
            fileUrl = storageService.generatePresignedUrl(filePathS3, Duration.ofMinutes(60));
            log.info("Arquivo {} enviado com sucesso para S3. URL assinada: {}", filePathS3, fileUrl);
        } catch (Exception e) {
            log.error("Erro ao fazer upload do arquivo para mentoria {}: {}", mentoriaId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro ao fazer upload do arquivo.");
        }

        try {
            String authenticatedUserEmail = authentication.getName();
            com.projetointegrador.seumentor.user.api.dtos.UserRepresentation sender = userQuery.findByEmail(authenticatedUserEmail)
                    .orElseThrow(() -> new UserNotFoundException("Usuário autenticado '" + authenticatedUserEmail + "' não encontrado."));
            Long senderId = sender.id();

            String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "arquivo";
            String chatMessageContent = "Arquivo anexado: " + originalFilename + "\nLink: " + fileUrl;

            ChatInput chatInput = new ChatInput(
                    mentoriaId, 
                    senderId,
                    chatMessageContent,
                    MessageType.FILE,
                    null
            );

            chatService.validateChatMessage(mentoriaId, chatInput);
            ChatOutputDTO outputDTO = chatService.saveMessageAndBuildOutput(chatInput);

            String destination = chatService.getDestinationTopic(
                    mentoriaId,
                    chatInput.type(),
                    chatInput.senderId(),
                    chatInput.receiverId()
            );
            
            messagingTemplate.convertAndSend(destination, outputDTO);
            log.info("Link do arquivo {} enviado para o chat da mentoria {} no tópico {}: {}", filePathS3, mentoriaId, destination, outputDTO);

        } catch (UserNotFoundException e) {
            log.error("Usuário autenticado não pôde ser encontrado para enviar a mensagem do arquivo ao chat: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (AccessDeniedException e) {
            log.warn("Acesso negado ao tentar enviar mensagem de arquivo para o chat da mentoria {}: {}", mentoriaId, e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            log.error("Erro ao enviar link do arquivo {} para o chat da mentoria {}: {}", filePathS3, mentoriaId, e.getMessage(), e);
        }
        
        return ResponseEntity.ok(fileUrl);
    }

    @PostMapping("/profile-image/{userId}")
    @PreAuthorize("authentication.principal.id == #userId or hasAuthority('ADMIN')")
    @Operation(summary = "Upload de imagem de perfil do usuário", description = "Faz o upload de uma imagem de perfil para um usuário específico. A imagem será armazenada em um caminho padronizado e apenas arquivos JPG e PNG são permitidos. Retorna a URL assinada da imagem. Requer que o usuário seja o dono do perfil ou ADMIN.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Imagem de perfil enviada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Requisição inválida"),
            @ApiResponse(responseCode = "401", description = "Não Autorizado"),
            @ApiResponse(responseCode = "403", description = "Acesso Negado"),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor")
    })
    public ResponseEntity<?> uploadProfileImage(
            @Parameter(description = "Arquivo de imagem (JPG ou PNG) a ser enviado", required = true, content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)) @RequestParam(value = "file") MultipartFile file,
            @Parameter(description = "ID do usuário", required = true, example = "123") @PathVariable Long userId,
            Authentication authentication) {
        try {
            String imageUrl = storageService.uploadProfileImage(file, String.valueOf(userId), authentication);
            return ResponseEntity.ok(imageUrl);
        } catch (IllegalArgumentException e) {
            log.warn("Bad request during profile image upload for user {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during profile image upload for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro interno ao processar o upload da imagem de perfil.");
        }
    }

    @GetMapping("/profile-image/{userId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Obter URL assinada para imagem de perfil", description = "Gera uma URL assinada temporária para acessar uma imagem de perfil específica do usuário.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "URL assinada gerada com sucesso", content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE, schema = @Schema(type = "string", format = "url"))),
            @ApiResponse(responseCode = "404", description = "Imagem não encontrada (arquivo ou usuário)", content = @Content),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor", content = @Content)
    })
    public ResponseEntity<String> getProfileImageUrl(
            @Parameter(description = "ID do usuário proprietário da imagem", required = true, example = "123") @PathVariable Long userId,
            Authentication authentication) {
        
        String key = "profile-images/" + userQuery.findProfileImgIdByUserId(userId).profileImgId() + userQuery.findProfileImgIdByUserId(userId).extension();
        String url = storageService.getProfileImageUrl(key, Duration.ofDays(7));
        return ResponseEntity.ok(url);
    }

    @GetMapping("/profile-images/{userId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Listar imagens de perfil do usuário", description = "Retorna todas as URLs assinadas das imagens de perfil associadas a um usuário específico.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de URLs de imagens obtida com sucesso"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado"),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor")
    })
    public ResponseEntity<List<String>> getUserProfileImages(
            @Parameter(description = "ID do usuário", required = true, example = "123") @PathVariable Long userId) {
        try {
            List<String> imageUrls = storageService.listUserProfileImages(userId);
            return ResponseEntity.ok(imageUrls);
        } catch (Exception e) {
            log.error("Erro ao listar imagens de perfil para o usuário {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/signed-url/{fileName:.+}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Gerar URL assinada para arquivo", description = "Gera uma URL assinada temporária para acessar um arquivo genérico no bucket. O nome do arquivo deve ser o caminho completo no bucket.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "URL assinada gerada com sucesso", content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE, schema = @Schema(type = "string", format = "url"))),
            @ApiResponse(responseCode = "404", description = "Arquivo não encontrado (chave inválida)", content = @Content),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor", content = @Content)
    })
    public ResponseEntity<String> generatePresignedUrl(
            @Parameter(description = "Nome completo do arquivo (chave) no bucket (ex: meu-diretorio/meuarquivo.pdf)", required = true) @PathVariable String fileName,
            @Parameter(description = "Tempo de expiração da URL em minutos", example = "60") @RequestParam(defaultValue = "60") Integer expirationMinutes) {
        String url = storageService.generatePresignedUrl(fileName, Duration.ofMinutes(expirationMinutes));
        return ResponseEntity.ok(url);
    }

    @GetMapping("/download/{fileName:.+}")
    @PreAuthorize("isAuthenticated()") 
    @Operation(summary = "Download de um arquivo (Autenticado)", description = "Faz o download de um arquivo do bucket S3. O nome do arquivo deve ser o caminho completo no bucket. Requer autenticação.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Arquivo baixado com sucesso", content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE)),
            @ApiResponse(responseCode = "404", description = "Arquivo não encontrado", content = @Content),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor durante o download", content = @Content)
    })
    public ResponseEntity<ByteArrayResource> downloadFile(
            @Parameter(description = "Nome completo do arquivo (chave) no bucket a ser baixado", required = true) @PathVariable String fileName) {
        byte[] data = storageService.downloadFile(fileName);
        if (data == null) {
            return ResponseEntity.notFound().build();
        }
        String downloadFileName = fileName.contains("/") ? fileName.substring(fileName.lastIndexOf("/") + 1) : fileName;
        ByteArrayResource resource = new ByteArrayResource(data);
        return ResponseEntity
                .ok()
                .contentLength(data.length)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + downloadFileName + "\"") 
                .body(resource);
    }

    @DeleteMapping("/delete/{fileName:.+}")
    @PreAuthorize("hasAuthority('ADMIN')") 
    @Operation(summary = "Excluir um arquivo (ADMIN)", description = "Remove um arquivo do bucket S3. O nome do arquivo deve ser o caminho completo no bucket. Requer permissão de ADMIN.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Arquivo removido com sucesso", content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE, schema = @Schema(type = "string", example = "meuarquivo.txt removed ..."))),
            @ApiResponse(responseCode = "404", description = "Arquivo não encontrado", content = @Content),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor durante a exclusão", content = @Content)
    })
    public ResponseEntity<String> deleteFile(
            @Parameter(description = "Nome completo do arquivo (chave) no bucket a ser excluído", required = true) @PathVariable String fileName) {
        return new ResponseEntity<>(storageService.deleteFile(fileName), HttpStatus.OK);
    }
}
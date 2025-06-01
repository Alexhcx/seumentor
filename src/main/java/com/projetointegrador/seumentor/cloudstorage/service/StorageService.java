package com.projetointegrador.seumentor.cloudstorage.service;

import com.projetointegrador.seumentor.tutoring.api.TutoringQuery; // Assuming you'll need this for mentor check
import com.projetointegrador.seumentor.tutoring.exception.TutoringNotFoundException;
import com.projetointegrador.seumentor.user.api.UserCommand;
import com.projetointegrador.seumentor.user.api.UserQuery;
import com.projetointegrador.seumentor.user.api.dtos.UserRepresentation;
import com.projetointegrador.seumentor.user.api.dtos.UserUpdateRequest;
import com.projetointegrador.seumentor.user.enums.Role;
import com.projetointegrador.seumentor.user.exception.UserNotFoundException;
import com.projetointegrador.seumentor.user.model.User; // Import User model
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException; // Import AccessDeniedException
import org.springframework.security.core.Authentication; // Import Authentication
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.*;

@Service
@Slf4j
public class StorageService {

    private static final String PROFILE_IMAGES_FOLDER = "profile-images/";
    private static final String ANEXOS_FOLDER = "anexos/";
    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(Arrays.asList(".jpg", ".jpeg", ".png"));

    @Value("${application.bucket.name}")
    private String bucketName;

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final UserCommand userCommandService;
    private final UserQuery userQuery;
    private final TutoringQuery tutoringQuery;

    public StorageService(S3Client s3Client, S3Presigner s3Presigner,
            UserCommand userCommandService, UserQuery userQuery,
            TutoringQuery tutoringQuery) { 
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.userCommandService = userCommandService;
        this.userQuery = userQuery;
        this.tutoringQuery = tutoringQuery; 
    }

    public String uploadMentoriaFile(MultipartFile file, String mentoriaIdStr, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Usuário não autenticado.");
        }
        Long mentoriaId;
        try {
            mentoriaId = Long.parseLong(mentoriaIdStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("ID da mentoria inválido: " + mentoriaIdStr);
        }

        String authenticatedUserEmail = authentication.getName();
        UserRepresentation authenticatedUser = userQuery.findByEmail(authenticatedUserEmail)
                .orElseThrow(() -> new UserNotFoundException(
                        "Usuário autenticado '" + authenticatedUserEmail + "' não encontrado."));

        com.projetointegrador.seumentor.tutoring.api.dto.TutoringRepresentation tutoring = tutoringQuery
                .findTutoringById(mentoriaId)
                .orElseThrow(() -> new TutoringNotFoundException("Mentoria não encontrada com ID: " + mentoriaId));

        if (!tutoring.mentorId().equals(authenticatedUser.id())) {
            log.warn("Usuário {} (ID: {}) tentou fazer upload para mentoria {} mas não é o mentor (Mentor ID: {}).",
                    authenticatedUserEmail, authenticatedUser.id(), mentoriaId, tutoring.mentorId());
            throw new AccessDeniedException("Apenas o mentor da mentoria pode enviar arquivos.");
        }
        log.info("Mentor {} (ID: {}) autorizado a fazer upload para mentoria {}", authenticatedUserEmail,
                authenticatedUser.id(), mentoriaId);

        File fileObj = convertMultiPartFileToFile(file);
        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "arquivo";
        // Ensure mentoriaId in path is the Long version for consistency if used
        // elsewhere
        String fileName = ANEXOS_FOLDER + mentoriaId + "/" + System.currentTimeMillis() + "_" + originalFilename;

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromFile(fileObj));
        fileObj.delete();

        return fileName;
    }

    public String uploadProfileImage(MultipartFile file, String userIdStr, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Usuário não autenticado.");
        }
        Long userId;
        try {
            userId = Long.parseLong(userIdStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("ID do usuário inválido: " + userIdStr);
        }

        String authenticatedUserEmail = authentication.getName();
        User authenticatedUserPrincipal = (User) authentication.getPrincipal();

        boolean isAdmin = authenticatedUserPrincipal.getRole() == Role.ADMIN;

        if (!authenticatedUserPrincipal.getId().equals(userId) && !isAdmin) {
            log.warn("Usuário {} (ID: {}) tentou fazer upload de imagem de perfil para o usuário ID {} sem permissão.",
                    authenticatedUserEmail, authenticatedUserPrincipal.getId(), userId);
            throw new AccessDeniedException("Você não tem permissão para alterar a imagem de perfil deste usuário.");
        }
        log.info("Usuário {} (ID: {}) autorizado a fazer upload de imagem de perfil para o usuário ID {} (isAdmin: {})",
                authenticatedUserEmail, authenticatedUserPrincipal.getId(), userId, isAdmin);

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        }

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Formato de arquivo não permitido. Apenas JPG e PNG são aceitos.");
        }

        File fileObj = convertMultiPartFileToFile(file);
        String standardizedFileName = "user-" + userId + "-seu-mentor-" + UUID.randomUUID().toString() + extension;
        String filePath = PROFILE_IMAGES_FOLDER + userId + "/" + standardizedFileName;

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(filePath)
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromFile(fileObj));
        fileObj.delete();

        String imageUrl = getProfileImageUrl(filePath, Duration.ofDays(7)); 

        try {
            userQuery.findById(userId) 
                    .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado com ID: " + userId));

            UserUpdateRequest updateRequest = new UserUpdateRequest(imageUrl, null, null, null, null, null, null, null,
                    null);
            userCommandService.updateUser(userId, updateRequest);
            log.info("Profile image URL saved for user ID: {}", userId);
        } catch (UserNotFoundException e) {
            log.warn("User not found with ID: {}. Profile image URL ({}) uploaded but not saved in user profile.",
                    userId, imageUrl, e);
            // Decide if this should be a critical error that stops the process or just a
            // warning.
            // If critical, rethrow or throw a specific exception.
        } catch (Exception e) {
            log.error("Error saving profile image URL for user ID {} to database: {}", userId, e.getMessage(), e);
        }
        return imageUrl;
    }

    public String getProfileImageUrl(String fileKey, Duration expiration) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(fileKey)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(expiration)
                .getObjectRequest(getObjectRequest)
                .build();

        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
        return presignedRequest.url().toString();
    }

    public String generatePresignedUrl(String fileKey, Duration expiration) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(fileKey)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(expiration)
                .getObjectRequest(getObjectRequest)
                .build();

        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
        return presignedRequest.url().toString();
    }

    public byte[] downloadFile(String fileKey) { 
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(fileKey)
                .build();

        ResponseInputStream<GetObjectResponse> response = s3Client.getObject(getObjectRequest);

        try (InputStream inputStream = response) {
            return inputStream.readAllBytes();
        } catch (IOException e) {
            log.error("Error downloading file {} from S3", fileKey, e);
            return null;
        }
    }

    public String deleteFile(String fileKey) { // Renamed fileName to fileKey
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(fileKey)
                .build();

        s3Client.deleteObject(deleteObjectRequest);
        return fileKey + " removed ...";
    }
    
    public List<String> listUserProfileImages(Long userId) {
        String prefix = PROFILE_IMAGES_FOLDER + userId + "/";
        List<String> imageUrls = new ArrayList<>();
        
        try {
            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(prefix)
                    .build();
                    
            ListObjectsV2Response listResponse = s3Client.listObjectsV2(listRequest);
            
            listResponse.contents().forEach(s3Object -> {
                String imageUrl = getProfileImageUrl(s3Object.key(), Duration.ofHours(23));
                imageUrls.add(imageUrl);
            });
            
            log.info("Found {} profile images for user ID: {}", imageUrls.size(), userId);
        } catch (Exception e) {
            log.error("Error listing profile images for user ID {}: {}", userId, e.getMessage(), e);
        }
        
        return imageUrls;
    }

    private File convertMultiPartFileToFile(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            originalFilename = "tempfile";
        }
        File convertedFile = new File(originalFilename);
        try (FileOutputStream fos = new FileOutputStream(convertedFile)) {
            fos.write(file.getBytes());
        } catch (IOException e) {
            log.error("Error converting multipartFile to file: {}", originalFilename, e);
        }
        return convertedFile;
    }
}
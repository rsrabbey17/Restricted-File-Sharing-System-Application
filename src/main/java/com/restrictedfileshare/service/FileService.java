package com.restrictedfileshare.service;

import com.restrictedfileshare.dto.ApiResponse;
import com.restrictedfileshare.dto.FileUploadResponse;
import com.restrictedfileshare.exception.BadRequestException;
import com.restrictedfileshare.exception.ResourceNotFoundException;
import com.restrictedfileshare.exception.UnauthorizedException;
import com.restrictedfileshare.model.FileAccess;
import com.restrictedfileshare.model.FileEntity;
import com.restrictedfileshare.model.User;
import com.restrictedfileshare.repository.FileAccessRepository;
import com.restrictedfileshare.repository.FileRepository;
import com.restrictedfileshare.repository.UserRepository;
import com.restrictedfileshare.validation.FileValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class FileService {

    private final FileRepository fileRepository;
    private final FileAccessRepository accessRepository;
    private final UserRepository userRepository;
    private final FileValidator fileValidator;

    @Value("${app.file.max-size-mb}")
    private int maxFileSizeMB;

    @Value("${app.file.upload-dir}")
    private String uploadDir;

    public FileUploadResponse uploadFile(MultipartFile file, List<String> sharedEmails, User uploader) {
        if (!uploader.isVerified()) {
            throw new UnauthorizedException("Only verified users can upload files");
        }

        fileValidator.validateFile(file);

        List<User> verifiedUsers = userRepository.findAllVerifiedByEmails(sharedEmails);

        List<String> nonVerifiedEmails = sharedEmails.stream()
                .filter(email -> verifiedUsers.stream().noneMatch(u -> u.getEmail().equals(email)))
                .collect(Collectors.toList());

        if (!nonVerifiedEmails.isEmpty()) {
            throw new BadRequestException(
                    "Cannot share with non-verified users: " + String.join(", ", nonVerifiedEmails)
            );
        }

        String storedFileName = saveFileToDisk(file);

        FileEntity fileEntity = new FileEntity();
        fileEntity.setOriginalName(file.getOriginalFilename());
        fileEntity.setStoredName(storedFileName);
        fileEntity.setPath(Paths.get(uploadDir).resolve(storedFileName).toString());
        fileEntity.setDownloadToken(UUID.randomUUID().toString());
        fileEntity.setSize(file.getSize());
        fileEntity.setMimeType(file.getContentType());
        fileEntity.setUploadedBy(uploader);
        fileEntity.setActive(true);

        fileRepository.save(fileEntity);

        for (User user : verifiedUsers) {
            if (!user.getId().equals(uploader.getId())) {
                FileAccess access = new FileAccess();
                access.setFile(fileEntity);
                access.setUser(user);
                accessRepository.save(access);
            }
        }

        FileAccess uploaderAccess = new FileAccess();
        uploaderAccess.setFile(fileEntity);
        uploaderAccess.setUser(uploader);
        accessRepository.save(uploaderAccess);

        List<String> successfullyShared = verifiedUsers.stream()
                .map(User::getEmail)
                .collect(Collectors.toList());

        String downloadLink = "/api/files/download/" + fileEntity.getDownloadToken();

        return new FileUploadResponse(
                fileEntity.getDownloadToken(),
                downloadLink,
                fileEntity.getOriginalName(),
                fileEntity.getSize(),
                successfullyShared
        );
    }

    private String saveFileToDisk(MultipartFile file) {
        try {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String fileName = System.currentTimeMillis() + "_" +
                    UUID.randomUUID().toString().substring(0, 8) + "_" +
                    file.getOriginalFilename().replaceAll("[^a-zA-Z0-9.-]", "_");

            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            return fileName;
        } catch (IOException e) {
            log.error("Failed to save file", e);
            throw new RuntimeException("Failed to save file: " + e.getMessage());
        }
    }

    public Resource downloadFile(String downloadToken, User requester) {
        FileEntity fileEntity = fileRepository.findByDownloadToken(downloadToken)
                .orElseThrow(() -> new ResourceNotFoundException("File not found"));

        if (!fileEntity.isActive()) {
            throw new ResourceNotFoundException("File no longer available");
        }

        boolean hasAccess = accessRepository.hasAccess(fileEntity.getId(), requester.getId());
        if (!hasAccess) {
            throw new UnauthorizedException("You do not have permission to download this file");
        }

        try {
            Path filePath = Paths.get(fileEntity.getPath());
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                throw new ResourceNotFoundException("File not found on server");
            }

            return resource;
        } catch (MalformedURLException e) {
            throw new RuntimeException("Error accessing file: " + e.getMessage());
        }
    }

    public List<FileUploadResponse> getUserFiles(User user) {
        List<FileEntity> files = fileRepository.findFilesAccessibleByUser(user.getId());

        return files.stream()
                .map(file -> {
                    List<String> sharedWith = accessRepository.findUserEmailsWithAccess(file.getId())
                            .stream()
                            .filter(email -> !email.equals(user.getEmail()))
                            .collect(Collectors.toList());

                    return new FileUploadResponse(
                            file.getDownloadToken(),
                            "/api/files/download/" + file.getDownloadToken(),
                            file.getOriginalName(),
                            file.getSize(),
                            sharedWith
                    );
                })
                .collect(Collectors.toList());
    }

    public ApiResponse<String> revokeAccess(Long fileId, String userEmail, User owner) {
        FileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found"));

        if (!file.getUploadedBy().getId().equals(owner.getId())) {
            throw new UnauthorizedException("Only file owner can revoke access");
        }

        User userToRevoke = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (userToRevoke.getId().equals(owner.getId())) {
            throw new BadRequestException("Cannot revoke owner's access");
        }

        accessRepository.deleteByFileAndUser(file, userToRevoke);

        return new ApiResponse<>(
                true,
                "Access revoked successfully for user: " + userEmail,
                null
        );
    }

    public ApiResponse<String> grantAccess(Long fileId, String userEmail, User owner) {
        FileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found"));

        if (!file.getUploadedBy().getId().equals(owner.getId())) {
            throw new UnauthorizedException("Only file owner can grant access");
        }

        User userToGrant = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!userToGrant.isVerified()) {
            throw new BadRequestException("Cannot grant access to non-verified user");
        }

        if (accessRepository.existsByFileAndUser(file, userToGrant)) {
            throw new BadRequestException("User already has access to this file");
        }

        FileAccess access = new FileAccess();
        access.setFile(file);
        access.setUser(userToGrant);
        accessRepository.save(access);

        return new ApiResponse<>(
                true,
                "Access granted successfully to user: " + userEmail,
                null
        );
    }

    public ApiResponse<String> deleteFile(Long fileId, User user) {
        FileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found"));

        if (!file.getUploadedBy().getId().equals(user.getId())) {
            throw new UnauthorizedException("Only file owner can delete files");
        }

        file.setActive(false);
        fileRepository.save(file);

        try {
            Path filePath = Paths.get(file.getPath());
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.warn("Failed to delete physical file: {}", file.getPath());
        }

        return new ApiResponse<>(true, "File deleted successfully", null);
    }
}
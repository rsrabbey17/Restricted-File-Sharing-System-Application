package com.restrictedfileshare.controller;

import com.restrictedfileshare.dto.ApiResponse;
import com.restrictedfileshare.dto.FileUploadResponse;
import com.restrictedfileshare.model.User;
import com.restrictedfileshare.service.FileService;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Validated
public class FileController {

    private final FileService fileService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<FileUploadResponse>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "sharedWith", required = false) String sharedWith,
            @AuthenticationPrincipal User user) {

        List<String> sharedEmails = sharedWith != null ?
                Arrays.asList(sharedWith.split(",")) :
                List.of();

        FileUploadResponse response = fileService.uploadFile(file, sharedEmails, user);

        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "File uploaded successfully",
                response
        ));
    }

    @GetMapping("/download/{token}")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable String token,
            @AuthenticationPrincipal User user) {

        Resource resource = fileService.downloadFile(token, user);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @GetMapping("/my-files")
    public ResponseEntity<ApiResponse<List<FileUploadResponse>>> getMyFiles(
            @AuthenticationPrincipal User user) {

        List<FileUploadResponse> files = fileService.getUserFiles(user);

        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Files retrieved successfully",
                files
        ));
    }

    @PostMapping("/{fileId}/grant-access")
    public ResponseEntity<ApiResponse<String>> grantAccess(
            @PathVariable Long fileId,
            @RequestParam @NotEmpty String userEmail,
            @AuthenticationPrincipal User user) {

        ApiResponse<String> response = fileService.grantAccess(fileId, userEmail, user);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{fileId}/revoke-access")
    public ResponseEntity<ApiResponse<String>> revokeAccess(
            @PathVariable Long fileId,
            @RequestParam @NotEmpty String userEmail,
            @AuthenticationPrincipal User user) {

        ApiResponse<String> response = fileService.revokeAccess(fileId, userEmail, user);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{fileId}")
    public ResponseEntity<ApiResponse<String>> deleteFile(
            @PathVariable Long fileId,
            @AuthenticationPrincipal User user) {

        ApiResponse<String> response = fileService.deleteFile(fileId, user);

        return ResponseEntity.ok(response);
    }
}
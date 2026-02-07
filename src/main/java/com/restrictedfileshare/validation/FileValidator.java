package com.restrictedfileshare.validation;

import com.restrictedfileshare.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

@Component
public class FileValidator {

    @Value("${app.file.max-size-mb}")
    private int maxFileSizeMB;

    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList(
            "application/pdf",
            "image/jpeg",
            "image/png",
            "image/gif",
            "text/plain",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/zip",
            "application/x-zip-compressed",
            "application/octet-stream"
    );

    public void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File cannot be empty");
        }

        long maxSizeBytes = maxFileSizeMB * 1024 * 1024;
        if (file.getSize() > maxSizeBytes) {
            throw new BadRequestException(
                    String.format("File size exceeds maximum limit of %d MB", maxFileSizeMB)
            );
        }

        if (file.getContentType() == null ||
                !ALLOWED_MIME_TYPES.contains(file.getContentType().toLowerCase())) {
            throw new BadRequestException(
                    "File type not allowed. Allowed types: PDF, Images, Text, Office documents, ZIP"
            );
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.contains("..")) {
            throw new BadRequestException("Invalid file name");
        }
    }
}
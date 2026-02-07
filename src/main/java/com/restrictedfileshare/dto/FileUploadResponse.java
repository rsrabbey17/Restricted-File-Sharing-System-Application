package com.restrictedfileshare.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadResponse {
    private String downloadToken;
    private String downloadLink;
    private String fileName;
    private long fileSize;
    private List<String> sharedWith;
}
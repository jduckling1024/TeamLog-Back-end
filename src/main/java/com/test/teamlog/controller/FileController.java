package com.test.teamlog.controller;

import com.test.teamlog.payload.FileDTO;
import com.test.teamlog.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.UnsupportedEncodingException;
import java.util.concurrent.TimeUnit;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Tag(name = "파일 다운로드 관리")
public class FileController {
    private final FileStorageService fileStorageService;

    @Operation(summary = "파일 다운로드")
    @GetMapping("/downloadFile/{storedFileName}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String storedFileName)
            throws UnsupportedEncodingException {
        // Load file as Resource
        FileDTO.FileResourceInfo resource = fileStorageService.loadFileAsFileResource(storedFileName);

        String contentType = resource.getContentType();

        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        String originalFileName = resource.getFileName();
        originalFileName =
                new String(originalFileName.getBytes("ISO-8859-1"), "euc-kr");
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .cacheControl(CacheControl.maxAge(10800, TimeUnit.SECONDS))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + originalFileName + "\"")
                .body(resource.getResource());
    }
}

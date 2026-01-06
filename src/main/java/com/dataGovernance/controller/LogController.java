package com.dataGovernance.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/logs")
public class LogController {

    @Value("${logging.file.name}")
    private String logFilePath;

    @GetMapping("/download")
    public ResponseEntity<Resource> downloadLogFile() {
        try {
            Path path = Paths.get(logFilePath).toAbsolutePath().normalize();
            Resource resource = new UrlResource(path.toUri());

            if (resource.exists() || resource.isReadable()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType("text/plain;charset=UTF-8")) // 明确指定UTF-8
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            }
            return ResponseEntity.notFound().build();
        } catch (IOException ex) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/view")
    public ResponseEntity<String> viewLogFile() {
        try {
            Path path = Paths.get(logFilePath).toAbsolutePath().normalize();
            String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8); // 使用UTF-8读取

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("text/plain;charset=UTF-8")) // 双重确保
                    .header("Content-Type", "text/plain;charset=UTF-8") // 显式设置header
                    .body(content);
        } catch (IOException ex) {
            return ResponseEntity.internalServerError()
                    .contentType(MediaType.parseMediaType("text/plain;charset=UTF-8"))
                    .body("无法读取日志文件");
        }
    }
}
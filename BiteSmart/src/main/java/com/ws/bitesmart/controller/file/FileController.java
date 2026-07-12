package com.ws.bitesmart.controller.file;

import com.ws.bitesmart.common.ResultVO;
import com.ws.bitesmart.config.FileConfig;
import com.ws.bitesmart.security.LoginUser;
import com.ws.bitesmart.service.file.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * 文件上传接口
 *
 * 任何登录用户都可以上传文件。
 * 上传后的文件路径存的是相对路径，前端用 /api/files/ 前缀访问。
 * 例如：后端存 /uploads/2026/07/09/xxx.jpg
 *       前端访问 http://localhost:8080/api/files/uploads/2026/07/09/xxx.jpg
 *
 * 实际上 Spring Boot 配置了静态资源映射，直接用 /uploads/ 也能访问。
 * 这里的 /api/files/** 在 SecurityConfig 的白名单里，所以不需要 Token 也能访问。
 */
@Slf4j
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;
    private final FileConfig fileConfig;

    /**
     * 上传文件
     *
     * POST /api/files/upload
     *
     * 请求方式：multipart/form-data
     * 参数：
     *   - file：文件（必填）
     *   - bizType：业务类型（必填，avatar/dish_image/license/review_image）
     *
     * 返回：
     *   {
     *     "code": 200,
     *     "message": "上传成功",
     *     "data": { "url": "/uploads/2026/07/09/xxx.jpg" }
     *   }
     */
    @PostMapping("/upload")
    public ResultVO<Map<String, String>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("bizType") String bizType,
            @AuthenticationPrincipal LoginUser loginUser) {

        if (loginUser == null) {
            return ResultVO.error(401, "未登录");
        }

        String fileUrl = fileService.uploadFile(file, bizType, loginUser.getUserId());
        Map<String, String> data = new HashMap<>();
        data.put("url", fileUrl);
        return ResultVO.success("上传成功", data);
    }

    /**
     * 文件下载/预览
     *
     * GET /api/files/download/{fileName}
     * GET /api/files/download/{date}/{fileName}
     *
     * 通过URL路径参数传递文件相对路径
     */
    @GetMapping("/download/**")
    public ResponseEntity<Resource> download(HttpServletRequest request) {
        try {
            String path = request.getRequestURI().replace("/api/files/download", "");
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            if (path.startsWith("uploads/")) {
                path = path.substring("uploads/".length());
            }

            File file = new File(fileConfig.getUploadDir(), path);
            if (!file.exists()) {
                log.warn("文件不存在: {}", file.getAbsolutePath());
                return ResponseEntity.notFound().build();
            }

            FileSystemResource resource = new FileSystemResource(file);
            String contentType = Files.probeContentType(Path.of(file.getAbsolutePath()));
            if (contentType == null) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getName() + "\"")
                    .body(resource);
        } catch (Exception e) {
            log.error("文件下载失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

}

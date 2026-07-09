package com.ws.bitesmart.service.file;

import com.ws.bitesmart.common.enums.ResultCodeEnum;
import com.ws.bitesmart.common.util.SnowflakeUtil;
import com.ws.bitesmart.config.FileConfig;
import com.ws.bitesmart.entity.file.FileUploadRecord;
import com.ws.bitesmart.exception.BusinessException;
import com.ws.bitesmart.mapper.file.FileUploadRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 文件上传服务
 *
 * 文件存储到本地磁盘，数据库只存相对路径。
 * 存储格式：/uploads/yyyy/MM/dd/随机UUID.扩展名
 *
 * 访问方式：直接通过 GET /api/files/uploads/yyyy/MM/dd/xxx.jpg 访问
 * （已在 SecurityConfig 中放行 /api/files/**）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

    private final FileConfig fileConfig;
    private final FileUploadRecordMapper fileUploadRecordMapper;

    /**
     * 上传文件
     *
     * @param file    上传的文件
     * @param bizType 业务类型（avatar/dish_image/license/review_image）
     * @param userId  上传用户ID
     * @return 文件访问的相对路径，如 /uploads/2026/07/09/xxx.jpg
     */
    @Transactional(rollbackFor = Exception.class)
    public String uploadFile(MultipartFile file, String bizType, Long userId) {
        // 1. 校验文件是否为空
        if (file.isEmpty()) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "上传文件不能为空");
        }

        // 2. 校验文件大小
        if (!fileConfig.isAllowedSize(file.getSize())) {
            throw new BusinessException(ResultCodeEnum.FILE_SIZE_EXCEED,
                    "文件大小不能超过 " + fileConfig.getMaxSize() / 1024 / 1024 + "MB");
        }

        // 3. 校验文件类型
        String mimeType = file.getContentType();
        if (mimeType == null || !fileConfig.isAllowedMimeType(mimeType)) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "不支持的文件类型");
        }

        try {
            // 4. 生成存储路径：按日期分目录，文件名用UUID避免冲突
            String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            String originalName = file.getOriginalFilename();
            // 过滤原始文件名中的特殊字符，防止路径穿越
            String safeName = originalName != null ? originalName.replaceAll("[^a-zA-Z0-9.\\-_]", "") : "";
            String ext = "";
            int dotIndex = safeName.lastIndexOf(".");
            if (dotIndex > 0) {
                ext = safeName.substring(dotIndex).toLowerCase();
            }
            String newFileName = UUID.randomUUID().toString().replace("-", "") + ext;

            // 相对路径（存到数据库和返回给前端的路径）
            String relativePath = "/uploads/" + dateDir + "/" + newFileName;

            // 磁盘上的完整路径
            Path fullPath = fileConfig.resolveUploadPath(dateDir + "/" + newFileName);

            // 5. 创建目录并保存文件
            Files.createDirectories(fullPath.getParent());
            Files.copy(file.getInputStream(), fullPath, StandardCopyOption.REPLACE_EXISTING);

            // 6. 保存上传记录到数据库
            FileUploadRecord record = new FileUploadRecord();
            record.setId(SnowflakeUtil.generate());
            record.setUserId(userId);
            record.setBizType(bizType);
            record.setFileName(originalName);
            record.setFileUrl(relativePath);
            record.setFileSize(file.getSize());
            record.setMimeType(mimeType);
            record.setStoragePath(fullPath.toString());
            fileUploadRecordMapper.insert(record);

            log.info("文件上传成功: {}, 大小={}, 类型={}", relativePath, file.getSize(), mimeType);

            return relativePath;

        } catch (IOException e) {
            log.error("文件上传失败: {}", e.getMessage());
            throw new BusinessException(ResultCodeEnum.FILE_UPLOAD_FAILED, "文件上传失败");
        }
    }

    /**
     * 删除文件
     *
     * @param fileUrl 文件的相对路径
     */
    public boolean deleteFile(String fileUrl) {
        try {
            // 找到对应的上传记录
            // 注意：这里简单处理，实际应该是根据ID来删除
            // 同时删除磁盘文件和数据库记录
            Path fullPath = fileConfig.resolveUploadPath(
                    fileUrl.replace("/uploads/", ""));
            return Files.deleteIfExists(fullPath);
        } catch (IOException e) {
            log.warn("删除文件失败: {}", e.getMessage());
            return false;
        }
    }

}

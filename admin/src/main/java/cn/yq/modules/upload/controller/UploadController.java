package cn.yq.modules.upload.controller;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.yq.common.api.ApiResponse;
import cn.yq.common.exception.BusinessException;
import cn.yq.modules.upload.domains.vo.req.UploadPresignReq;
import cn.yq.modules.upload.domains.vo.res.UploadPresignRes;
import cn.yq.modules.upload.service.TosService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 文件上传（OSS）接口
 */
@RestController
@RequestMapping("/api/upload")
@Slf4j
@Tag(name = "UploadController", description = "文件上传接口")
public class UploadController {

    @Resource
    private TosService tosService;

    /**
     * 上传图片/文件，返回 OSS 访问地址。
     * <p>
     * 默认路径：upload/yyyyMMdd/随机名-原文件名
     * 可选参数 path：业务自定义前缀（如 order/payment），则路径为
     *   {path}/yyyyMMdd/随机名-原文件名
     */
    @PostMapping("/file")
    @Operation(description = "上传文件，返回 OSS 地址")
    public ApiResponse<String> uploadFile(@RequestParam("file") MultipartFile file,
                                          @RequestParam(value = "path", required = false) String path) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的文件");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            originalFilename = "file";
        }
        // 业务自定义路径前缀（如 order/payment），否则默认 upload
        String basePath;
        if (path != null && !path.isBlank()) {
            String trimmed = path.trim();
            // 去掉首尾 /，避免出现 // 或以 / 开头导致根目录错乱
            trimmed = trimmed.replaceAll("^/+", "").replaceAll("/+$", "");
            basePath = trimmed.isEmpty() ? "upload" : trimmed;
        } else {
            basePath = "upload";
        }

        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String objectName = basePath + "/" + datePath + "/" + buildStoredFileName(originalFilename);

        try (InputStream inputStream = file.getInputStream()) {
            String fileUrl = tosService.uploadFile(objectName, inputStream);
            return ApiResponse.success("上传成功",fileUrl);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("上传文件失败 originalFilename={}", originalFilename, e);
            throw new BusinessException("上传失败：" + e.getMessage());
        }
    }

    @PostMapping("/presign")
    @Operation(description = "生成TOS直传预签名地址")
    public ApiResponse<UploadPresignRes> createUploadPresign(@RequestBody UploadPresignReq req) {
        if (req == null || req.getFileName() == null || req.getFileName().trim().isEmpty()) {
            throw new BusinessException("文件名不能为空");
        }
        String objectName = buildObjectName(req.getPath(), req.getFileName());
        UploadPresignRes res = tosService.createUploadPresign(objectName, req.getContentType());
        return ApiResponse.success(res);
    }

    private String buildObjectName(String path, String originalFilename) {
        String basePath;
        if (path != null && !path.isBlank()) {
            String trimmed = path.trim().replaceAll("^/+", "").replaceAll("/+$", "");
            basePath = trimmed.isEmpty() ? "upload" : trimmed;
        } else {
            basePath = "upload";
        }
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return basePath + "/" + datePath + "/" + buildStoredFileName(originalFilename);
    }

    /**
     * 存储层对象名不再拼接整段原始文件名，避免 key 和库中字段过长。
     * 原始文件名由业务表单单独存储用于展示。
     */
    private String buildStoredFileName(String originalFilename) {
        String ext = "";
        if (StrUtil.isNotBlank(originalFilename)) {
            int index = originalFilename.lastIndexOf('.');
            if (index >= 0 && index < originalFilename.length() - 1) {
                String rawExt = originalFilename.substring(index + 1).replaceAll("[^a-zA-Z0-9]", "");
                if (StrUtil.isNotBlank(rawExt)) {
                    ext = "." + rawExt.toLowerCase();
                }
            }
        }
        return IdUtil.simpleUUID() + RandomUtil.randomString("abcdefghijklmnopqrstuvwxyz0123456789", 6) + ext;
    }
}

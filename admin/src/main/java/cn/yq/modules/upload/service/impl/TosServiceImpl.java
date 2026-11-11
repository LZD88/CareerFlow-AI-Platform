package cn.yq.modules.upload.service.impl;

import cn.yq.common.exception.BusinessException;
import cn.yq.modules.upload.config.TosConfig;
import cn.yq.modules.upload.domains.vo.res.TosFileContentRes;
import cn.yq.modules.upload.domains.vo.res.UploadPresignRes;
import cn.yq.modules.upload.service.TosService;
import com.alibaba.fastjson2.JSONObject;
import com.volcengine.tos.TOSV2;
import com.volcengine.tos.TOSV2ClientBuilder;
import com.volcengine.tos.TosException;
import com.volcengine.tos.model.object.PreSignedURLInput;
import com.volcengine.tos.model.object.PreSignedURLOutput;
import com.volcengine.tos.model.object.PutObjectInput;
import com.volcengine.tos.model.object.PutObjectOutput;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class TosServiceImpl implements TosService {


    @Resource
    private TosConfig tosConfig;

    private TOSV2 buildClient() {
        if (tosConfig.getEndpoint() == null || tosConfig.getAccessKey() == null
                || tosConfig.getSecretKey() == null || tosConfig.getRegion() == null) {
            throw new BusinessException("OSS配置不完整");
        }
        return new TOSV2ClientBuilder().build(
                tosConfig.getRegion(),
                tosConfig.getEndpoint(),
                tosConfig.getAccessKey(),
                tosConfig.getSecretKey()
        );
    }

    @Override
    public String uploadFile(String objectName, InputStream inputStream) {
        // 参数校验
        if (objectName == null || objectName.trim().isEmpty()) {
            throw new BusinessException("对象名称不能为空");
        }
        if (inputStream == null) {
            throw new BusinessException("文件输入流不能为空");
        }
        try (TOSV2 tos = buildClient()) {
            PutObjectInput putObjectInput = new PutObjectInput()
                    .setBucket(tosConfig.getBucketName())
                    .setKey(objectName)
                    .setContent(inputStream);
            PutObjectOutput putObjectOutput = tos.putObject(putObjectInput);
            log.info("文件上传到OSS成功 putObjectOutput:{}", JSONObject.toJSONString(putObjectOutput));
            return buildFileUrl(objectName);
        }  catch (Exception e) {
            log.error("文件上传tos 异常", e);
            throw new BusinessException("上传失败，请稍后重试");
        }
    }

    @Override
    public UploadPresignRes createUploadPresign(String objectName, String contentType) {
        if (objectName == null || objectName.trim().isEmpty()) {
            throw new BusinessException("对象名称不能为空");
        }
        Map<String, String> headers = new HashMap<>();
        if (contentType != null && !contentType.isBlank()) {
            headers.put("Content-Type", contentType.trim());
        }
        try (TOSV2 tos = buildClient()) {
            PreSignedURLInput input = new PreSignedURLInput()
                    .setBucket(tosConfig.getBucketName())
                    .setKey(objectName)
                    .setHttpMethod("PUT")
                    .setExpires(15 * 60)
                    .setHeader(headers);
            PreSignedURLOutput output = tos.preSignedURL(input);
            UploadPresignRes res = new UploadPresignRes();
            res.setObjectKey(objectName);
            res.setUploadUrl(output.getSignedUrl());
            res.setFileUrl(buildFileUrl(objectName));
            // 浏览器直传时只返回前端真正需要显式设置的请求头，
            // 避免把 host / user-agent 这类受限头传回前端。
            res.setHeaders(headers.isEmpty() ? null : headers);
            return res;
        } catch (Exception e) {
            log.error("生成TOS预签名上传地址失败", e);
            throw new BusinessException("生成上传地址失败，请稍后重试");
        }
    }

    @Override
    public String createDownloadPresign(String objectName, long expiresSeconds) {
        if (objectName == null || objectName.trim().isEmpty()) {
            throw new BusinessException("对象名称不能为空");
        }
        long safeExpires = expiresSeconds > 0 ? expiresSeconds : 300L;
        try (TOSV2 tos = buildClient()) {
            PreSignedURLInput input = new PreSignedURLInput()
                    .setBucket(tosConfig.getBucketName())
                    .setKey(objectName)
                    .setHttpMethod("GET")
                    .setExpires(safeExpires);
            PreSignedURLOutput output = tos.preSignedURL(input);
            return output.getSignedUrl();
        } catch (TosException e) {
            log.error("生成TOS预签名下载地址失败 objectName={}", objectName, e);
            throw new BusinessException("生成下载地址失败，请稍后重试");
        } catch (Exception e) {
            log.error("生成TOS预签名下载地址失败 objectName={}", objectName, e);
            throw new BusinessException("生成下载地址失败，请稍后重试");
        }
    }

    @Override
    public TosFileContentRes getObjectContent(String objectName, boolean inlinePreview) {
        if (objectName == null || objectName.trim().isEmpty()) {
            throw new BusinessException("对象名称不能为空");
        }
        String contentType = guessContentType(objectName);
        String contentDisposition = (inlinePreview ? "inline" : "attachment") + "; filename=\"" + objectName.substring(objectName.lastIndexOf('/') + 1) + "\"";
        try (TOSV2 tos = buildClient();
             com.volcengine.tos.model.object.GetObjectV2Output output = tos.getObject(
                     new com.volcengine.tos.model.object.GetObjectV2Input()
                             .setBucket(tosConfig.getBucketName())
                             .setKey(objectName)
                             .setResponseContentDisposition(contentDisposition)
                             .setResponseContentType(contentType)
             )) {
            TosFileContentRes res = new TosFileContentRes();
            res.setContent(StreamUtils.copyToByteArray(output.getContent()));
            res.setContentType(contentType != null ? contentType : output.getContentType());
            res.setContentDisposition(contentDisposition);
            return res;
        }  catch (Exception e) {
            log.error("读取TOS文件失败 objectName={}", objectName, e);
            throw new BusinessException("读取文件失败，请稍后重试");
        }
    }

    private String buildFileUrl(String objectName) {
        return "https://" + tosConfig.getBucketName() + "." + tosConfig.getEndpoint() + "/" + objectName;
    }

    private String guessContentType(String objectName) {
        if (objectName == null) {
            return null;
        }
        String lower = objectName.toLowerCase();
        if (lower.endsWith(".pdf")) {
            return "application/pdf";
        }
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (lower.endsWith(".gif")) {
            return "image/gif";
        }
        if (lower.endsWith(".webp")) {
            return "image/webp";
        }
        if (lower.endsWith(".mp4")) {
            return "video/mp4";
        }
        if (lower.endsWith(".mov")) {
            return "video/quicktime";
        }
        return null;
    }
}

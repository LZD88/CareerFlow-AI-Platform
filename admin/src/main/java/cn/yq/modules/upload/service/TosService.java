package cn.yq.modules.upload.service;


import cn.yq.modules.upload.domains.vo.res.TosFileContentRes;
import cn.yq.modules.upload.domains.vo.res.UploadPresignRes;

import java.io.InputStream;

public interface TosService {

    String uploadFile(String objectName, InputStream inputStream);

    UploadPresignRes createUploadPresign(String objectName, String contentType);

    String createDownloadPresign(String objectName, long expiresSeconds);

    TosFileContentRes getObjectContent(String objectName, boolean inlinePreview);
}

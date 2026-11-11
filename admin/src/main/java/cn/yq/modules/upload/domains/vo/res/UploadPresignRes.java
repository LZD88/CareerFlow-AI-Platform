package cn.yq.modules.upload.domains.vo.res;

import lombok.Data;

import java.util.Map;

@Data
public class UploadPresignRes {

    private String objectKey;

    private String uploadUrl;

    private String fileUrl;

    private Map<String, String> headers;
}

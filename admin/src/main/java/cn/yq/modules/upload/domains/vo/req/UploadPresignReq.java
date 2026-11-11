package cn.yq.modules.upload.domains.vo.req;

import lombok.Data;

@Data
public class UploadPresignReq {

    private String fileName;

    private String contentType;

    private String path;
}

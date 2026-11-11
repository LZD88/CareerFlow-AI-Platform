package cn.yq.modules.upload.domains.vo.res;

import lombok.Data;

@Data
public class TosFileContentRes {

    /** 文件二进制内容 */
    private byte[] content;

    /** 响应 content-type */
    private String contentType;

    /** 响应 content-disposition */
    private String contentDisposition;
}

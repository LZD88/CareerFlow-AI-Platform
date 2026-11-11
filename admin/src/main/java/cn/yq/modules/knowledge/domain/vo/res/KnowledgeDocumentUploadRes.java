package cn.yq.modules.knowledge.domain.vo.res;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Markdown 知识文档批量上传结果。
 *
 * <p>一次上传会包含多个文件。后端按文件逐个保存到 MySQL，并根据 indexNow 决定是否立即调用
 * Python Agent 切片、向量化并写入 Milvus。单个文件失败不会中断整批上传。</p>
 */
@Data
public class KnowledgeDocumentUploadRes {

    /**
     * 本次请求收到的文件总数。
     */
    private Integer total = 0;

    /**
     * 成功保存到 MySQL 的文件数量。
     *
     * <p>如果 indexNow=true，Python/Milvus 入库失败的文件仍会保存到 MySQL，但状态会是 FAILED，
     * 这里仍计为成功处理，因为用户后续可以查看失败原因并重新入库。</p>
     */
    private Integer success = 0;

    /**
     * 文件读取、格式校验或 MySQL 保存失败的数量。
     */
    private Integer failed = 0;

    /**
     * 是否在上传后立即调用 Python Agent 写入 Milvus。
     *
     * <p>false 表示只保存 MySQL，文档状态为 PENDING，用户后续可手动点击“重入库”。</p>
     */
    private Boolean indexNow = Boolean.FALSE;

    /**
     * 每个上传文件的处理结果。
     */
    private List<Item> items = new ArrayList<>();

    /**
     * 单个 Markdown 文件的上传处理结果。
     */
    @Data
    public static class Item {

        /**
         * 用户上传时的原始文件名。
         */
        private String fileName;

        /**
         * 保存到 knowledge_document 后生成的 MySQL 主键。
         */
        private Long documentId;

        /**
         * 传给 Python/Milvus 使用的业务文档编码。
         */
        private String documentCode;

        /**
         * 文档当前状态：PENDING / INDEXING / INDEXED / FAILED / DISABLED。
         */
        private String status;

        /**
         * 已写入 Milvus 的 chunk 数量。
         *
         * <p>未立即入库或入库失败时通常为 0。</p>
         */
        private Integer chunkCount;

        /**
         * 单文件处理失败或 Python/Milvus 入库失败时的错误信息。
         */
        private String errorMessage;
    }
}

package cn.yq.modules.knowledge.domain.vo.res;

import lombok.Data;

import java.util.List;

@Data
public class PythonKnowledgeIngestTextRes {

    private Boolean ok;
    private String documentId;
    private Integer version;
    private String collection;
    private Integer chunkCount;
    private List<ChunkResult> chunks;

    @Data
    public static class ChunkResult {
        private String id;
        private String chunkId;
        private Integer contentLength;
    }
}

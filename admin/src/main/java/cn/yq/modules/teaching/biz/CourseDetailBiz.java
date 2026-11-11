package cn.yq.modules.teaching.biz;

import cn.yq.modules.teaching.domain.vo.req.CourseDetailCreateReq;
import org.springframework.web.multipart.MultipartFile;

public interface CourseDetailBiz {

    Long create(Long courseId, CourseDetailCreateReq request);

    void delete(Long courseId, Long id);

    Integer importExcel(Long courseId, MultipartFile file);
}

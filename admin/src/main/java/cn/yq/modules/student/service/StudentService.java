package cn.yq.modules.student.service;

import cn.yq.common.api.PageResult;
import cn.yq.modules.student.domain.entity.StudentEntity;
import cn.yq.modules.student.domain.vo.req.StudentCreateReq;
import cn.yq.modules.student.domain.vo.req.StudentQueryReq;
import cn.yq.modules.student.domain.vo.req.StudentUpdateReq;

public interface StudentService {

    PageResult<StudentEntity> page(StudentQueryReq request);

    StudentEntity detail(Long id);

    StudentEntity findByStudentCode(String studentCode);

    Long create(StudentCreateReq request);

    void update(Long id, StudentUpdateReq request);

    void delete(Long id);
}

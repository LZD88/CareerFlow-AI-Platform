package cn.yq.modules.studentportal.biz.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.yq.common.exception.BusinessException;
import cn.yq.config.MockInterviewProperties;
import cn.yq.modules.student.domain.entity.StudentEntity;
import cn.yq.modules.student.service.StudentService;
import cn.yq.modules.studentportal.biz.StudentPortalMockInterviewBiz;
import cn.yq.modules.studentportal.context.StudentPortalAccessContextHolder;
import cn.yq.modules.studentportal.domain.StudentPortalAccessContext;
import cn.yq.modules.studentportal.domain.entity.mockinterview.MockInterviewSessionEntity;
import cn.yq.modules.studentportal.domain.vo.req.mockinterview.MockInterviewCreateSessionReq;
import cn.yq.modules.studentportal.domain.vo.res.mockinterview.MockInterviewSessionRes;
import cn.yq.modules.studentportal.mapper.MockInterviewSessionMapper;
import cn.yq.modules.studentportal.util.MockInterviewRealtimeTokenUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class StudentPortalMockInterviewBizImpl implements StudentPortalMockInterviewBiz {

    private static final String STATUS_CREATED = "CREATED";

    private final StudentService studentService;
    private final MockInterviewSessionMapper mockInterviewSessionMapper;
    private final MockInterviewRealtimeTokenUtil mockInterviewRealtimeTokenUtil;
    private final MockInterviewProperties mockInterviewProperties;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MockInterviewSessionRes createSession(MockInterviewCreateSessionReq request) {
        StudentEntity student = requireCurrentStudent();
        Date now = new Date();

        MockInterviewSessionEntity entity = new MockInterviewSessionEntity();
        entity.setStudentId(student.getId());
        entity.setStudentCode(student.getStudentCode());
        entity.setStudentName(student.getStudentName());
        entity.setTitle(resolveTitle(request));
        entity.setInterviewRole(resolveInterviewRole(request));
        entity.setStatus(STATUS_CREATED);
        entity.setVolcSessionId(IdUtil.fastUUID());
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        mockInterviewSessionMapper.insert(entity);

        Instant expiresAt = Instant.now().plusSeconds(resolveTokenExpireSeconds());
        String token = mockInterviewRealtimeTokenUtil.createToken(
                entity.getId(),
                student.getId(),
                student.getStudentCode(),
                entity.getInterviewRole(),
                entity.getVolcSessionId(),
                expiresAt
        );
        entity.setRealtimeToken(token);
        entity.setRealtimeTokenExpiresAt(Date.from(expiresAt));
        mockInterviewSessionMapper.updateRealtimeToken(entity.getId(), entity.getRealtimeToken(), entity.getRealtimeTokenExpiresAt());
        return toRes(entity);
    }

    private StudentEntity requireCurrentStudent() {
        StudentPortalAccessContext accessContext = StudentPortalAccessContextHolder.get();
        if (accessContext == null || StrUtil.isBlank(accessContext.getStudentCode())) {
            throw new BusinessException(401, "未登录或登录状态失效");
        }
        StudentEntity student = studentService.findByStudentCode(accessContext.getStudentCode());
        if (student == null) {
            throw new BusinessException(404, "学员信息不存在");
        }
        return student;
    }

    private String resolveTitle(MockInterviewCreateSessionReq request) {
        String title = request == null ? null : StrUtil.trim(request.getTitle());
        if (StrUtil.isNotBlank(title)) {
            return title.length() <= 80 ? title : title.substring(0, 80);
        }
        return "AI模拟面试";
    }

    private String resolveInterviewRole(MockInterviewCreateSessionReq request) {
        String role = request == null ? null : StrUtil.trim(request.getInterviewRole());
        if (StrUtil.isNotBlank(role)) {
            return role.length() <= 100 ? role : role.substring(0, 100);
        }
        return "Java后端开发工程师";
    }

    private long resolveTokenExpireSeconds() {
        Long value = mockInterviewProperties.getRealtimeTokenExpireSeconds();
        return value == null || value <= 0 ? 600L : value;
    }

    private MockInterviewSessionRes toRes(MockInterviewSessionEntity entity) {
        MockInterviewSessionRes res = new MockInterviewSessionRes();
        res.setId(entity.getId());
        res.setTitle(entity.getTitle());
        res.setInterviewRole(entity.getInterviewRole());
        res.setStatus(entity.getStatus());
        res.setVolcSessionId(entity.getVolcSessionId());
        res.setRealtimeWsUrl(mockInterviewProperties.getPythonRealtimeWsUrl());
        res.setRealtimeToken(entity.getRealtimeToken());
        res.setRealtimeTokenExpiresIn(resolveTokenExpireSeconds());
        res.setRealtimeTokenExpiresAt(entity.getRealtimeTokenExpiresAt());
        res.setStartedAt(entity.getStartedAt());
        res.setEndedAt(entity.getEndedAt());
        res.setCreatedAt(entity.getCreatedAt());
        res.setUpdatedAt(entity.getUpdatedAt());
        return res;
    }
}

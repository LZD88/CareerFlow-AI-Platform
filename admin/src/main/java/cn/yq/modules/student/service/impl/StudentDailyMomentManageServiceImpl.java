package cn.yq.modules.student.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.yq.common.api.PageResult;
import cn.yq.modules.student.domain.vo.req.StudentDailyMomentAdminQueryReq;
import cn.yq.modules.student.domain.vo.res.StudentDailyMomentAdminItemRes;
import cn.yq.modules.student.service.StudentDailyMomentManageService;
import cn.yq.modules.teaching.domain.entity.StudentDailyMomentAdminEntity;
import cn.yq.modules.teaching.mapper.StudentDailyMomentMapper;
import cn.yq.modules.upload.service.TosService;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class StudentDailyMomentManageServiceImpl implements StudentDailyMomentManageService {

    private static final long DOWNLOAD_PRESIGN_EXPIRE_SECONDS = 60 * 60L;

    private final StudentDailyMomentMapper studentDailyMomentMapper;
    private final TosService tosService;

    @Override
    public PageResult<StudentDailyMomentAdminItemRes> page(StudentDailyMomentAdminQueryReq request) {
        Page<StudentDailyMomentAdminEntity> page = PageHelper.startPage(request.getPageNum(), request.getPageSize());
        List<StudentDailyMomentAdminItemRes> records = studentDailyMomentMapper.selectAdminPage(
                        normalize(request.getStudentCode()),
                        normalize(request.getStudentName()),
                        normalize(request.getTitle())
                ).stream()
                .map(this::toRes)
                .toList();
        return new PageResult<>(page.getTotal(), request.getPageNum(), request.getPageSize(), records);
    }

    private StudentDailyMomentAdminItemRes toRes(StudentDailyMomentAdminEntity entity) {
        StudentDailyMomentAdminItemRes res = new StudentDailyMomentAdminItemRes();
        res.setId(entity.getId());
        res.setStudentCode(entity.getStudentCode());
        res.setStudentName(entity.getStudentName());
        res.setClassId(entity.getClassId());
        res.setClassTerm(entity.getClassTerm());
        res.setCampusName(entity.getCampusName());
        res.setTitle(entity.getTitle());
        res.setContentText(entity.getContentText());
        res.setFileUrls(splitAndSign(entity.getFileUrl()));
        res.setRecordTime(entity.getRecordTime());
        res.setCreateTime(entity.getCreateTime());
        return res;
    }

    private List<String> splitAndSign(String fileUrl) {
        if (StrUtil.isBlank(fileUrl)) {
            return Collections.emptyList();
        }
        return StrUtil.splitTrim(fileUrl, ',').stream()
                .filter(StrUtil::isNotBlank)
                .map(this::resolveObjectKey)
                .filter(StrUtil::isNotBlank)
                .map(item -> tosService.createDownloadPresign(item, DOWNLOAD_PRESIGN_EXPIRE_SECONDS))
                .filter(Objects::nonNull)
                .toList();
    }

    private String resolveObjectKey(String fileUrlOrKey) {
        String trimmed = StrUtil.trim(fileUrlOrKey);
        if (StrUtil.isBlank(trimmed)) {
            return null;
        }
        String lower = trimmed.toLowerCase();
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
            return trimmed;
        }
        try {
            String path = URI.create(trimmed).getPath();
            return StrUtil.isBlank(path) ? null : StrUtil.removePrefix(path, "/");
        } catch (Exception ex) {
            return null;
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

package cn.yq.modules.studentportal.biz.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.yq.common.exception.BusinessException;
import cn.yq.modules.student.domain.entity.StudentEntity;
import cn.yq.modules.student.service.StudentService;
import cn.yq.modules.studentportal.biz.StudentPortalDailyMomentBiz;
import cn.yq.modules.studentportal.context.StudentPortalAccessContextHolder;
import cn.yq.modules.studentportal.domain.StudentPortalAccessContext;
import cn.yq.modules.studentportal.domain.vo.req.StudentDailyMomentCreateReq;
import cn.yq.modules.studentportal.domain.vo.req.StudentDailyMomentUploadPresignReq;
import cn.yq.modules.studentportal.domain.vo.res.StudentDailyMomentRes;
import cn.yq.modules.teaching.domain.entity.StudentDailyMomentEntity;
import cn.yq.modules.teaching.service.StudentDailyMomentService;
import cn.yq.modules.upload.domains.vo.res.UploadPresignRes;
import cn.yq.modules.upload.service.TosService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class StudentPortalDailyMomentBizImpl implements StudentPortalDailyMomentBiz {

    private static final DateTimeFormatter DATE_PATH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final long DOWNLOAD_PRESIGN_EXPIRE_SECONDS = 60 * 60L;

    private final StudentService studentService;
    private final StudentDailyMomentService studentDailyMomentService;
    private final TosService tosService;

    @Override
    public List<StudentDailyMomentRes> list() {
        StudentEntity student = requireCurrentStudent();
        return studentDailyMomentService.listByStudentNo(student.getStudentCode()).stream()
                .map(this::toRes)
                .toList();
    }

    @Override
    public UploadPresignRes createUploadPresign(StudentDailyMomentUploadPresignReq request) {
        StudentEntity student = requireCurrentStudent();
        String objectName = buildObjectName(student.getStudentCode(), request.getFileName());
        return tosService.createUploadPresign(objectName, request.getContentType());
    }

    @Override
    public Long create(StudentDailyMomentCreateReq request) {
        StudentEntity student = requireCurrentStudent();
        StudentDailyMomentEntity entity = new StudentDailyMomentEntity();
        entity.setStudentNo(student.getStudentCode());
        entity.setTitle(request.getTitle().trim());
        entity.setContentText(StrUtil.isBlank(request.getContentText()) ? null : request.getContentText().trim());
        entity.setFileUrl(joinFileUrls(request.getFileUrls()));
        entity.setRecordTime(request.getRecordTime());
        entity.setCreateTime(new Date());
        return studentDailyMomentService.create(entity);
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

    private String joinFileUrls(List<String> fileUrls) {
        if (fileUrls == null || fileUrls.isEmpty()) {
            return null;
        }
        List<String> normalized = fileUrls.stream()
                .filter(StrUtil::isNotBlank)
                .map(String::trim)
                .distinct()
                .toList();
        if (normalized.size() > 3) {
            throw new BusinessException("最多上传3个素材文件");
        }
        return normalized.isEmpty() ? null : String.join(",", normalized);
    }

    private String buildObjectName(String studentCode, String originalFilename) {
        String datePath = LocalDate.now().format(DATE_PATH_FORMATTER);
        return "student/daily-moments/" + studentCode + "/" + datePath + "/" + buildStoredFileName(originalFilename);
    }

    private StudentDailyMomentRes toRes(StudentDailyMomentEntity entity) {
        StudentDailyMomentRes res = new StudentDailyMomentRes();
        res.setId(entity.getId());
        res.setTitle(entity.getTitle());
        res.setContentText(entity.getContentText());
        res.setFileUrls(splitFileUrls(entity.getFileUrl()));
        res.setRecordTime(entity.getRecordTime());
        res.setCreateTime(entity.getCreateTime());
        return res;
    }

    private List<String> splitFileUrls(String fileUrl) {
        if (StrUtil.isBlank(fileUrl)) {
            return List.of();
        }
        return StrUtil.splitTrim(fileUrl, ',').stream()
                .filter(StrUtil::isNotBlank)
                .map(this::toAccessibleFileUrl)
                .filter(Objects::nonNull)
                .toList();
    }

    private String toAccessibleFileUrl(String fileUrlOrKey) {
        String objectKey = resolveObjectKey(fileUrlOrKey);
        if (StrUtil.isBlank(objectKey)) {
            return null;
        }
        return tosService.createDownloadPresign(objectKey, DOWNLOAD_PRESIGN_EXPIRE_SECONDS);
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
            String path = java.net.URI.create(trimmed).getPath();
            if (StrUtil.isBlank(path)) {
                return null;
            }
            return StrUtil.removePrefix(path, "/");
        } catch (Exception ex) {
            return null;
        }
    }

    private String buildStoredFileName(String originalFilename) {
        String ext = "";
        if (StrUtil.isNotBlank(originalFilename)) {
            int index = originalFilename.lastIndexOf('.');
            if (index >= 0 && index < originalFilename.length() - 1) {
                String rawExt = originalFilename.substring(index + 1).replaceAll("[^a-zA-Z0-9]", "");
                if (StrUtil.isNotBlank(rawExt)) {
                    ext = "." + rawExt.toLowerCase();
                }
            }
        }
        return IdUtil.simpleUUID() + RandomUtil.randomString("abcdefghijklmnopqrstuvwxyz0123456789", 6) + ext;
    }
}

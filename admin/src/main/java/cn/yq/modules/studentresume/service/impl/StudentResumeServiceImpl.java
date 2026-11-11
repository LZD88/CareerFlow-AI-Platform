package cn.yq.modules.studentresume.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.yq.common.api.PageResult;
import cn.yq.common.exception.BusinessException;
import cn.yq.modules.student.domain.entity.StudentEntity;
import cn.yq.modules.student.service.StudentService;
import cn.yq.modules.studentportal.context.StudentPortalAccessContextHolder;
import cn.yq.modules.studentportal.domain.StudentPortalAccessContext;
import cn.yq.modules.studentresume.domain.entity.StudentResumeEntity;
import cn.yq.modules.studentresume.domain.vo.req.StudentResumeQueryReq;
import cn.yq.modules.studentresume.domain.vo.req.StudentResumeSaveReq;
import cn.yq.modules.studentresume.domain.vo.res.StudentResumeRes;
import cn.yq.modules.studentresume.domain.vo.res.StudentResumeUploadRes;
import cn.yq.modules.studentresume.mapper.StudentResumeMapper;
import cn.yq.modules.studentresume.service.StudentResumeService;
import cn.yq.modules.upload.domains.vo.res.TosFileContentRes;
import cn.yq.modules.upload.service.TosService;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentResumeServiceImpl implements StudentResumeService {

    private static final int MAX_NAME_LENGTH = 80;
    private static final int MAX_ROLE_LENGTH = 80;
    private static final int MAX_RESUME_TEXT_LENGTH = 20000;
    private static final int MAX_SUMMARY_LENGTH = 4000;
    private static final long MAX_UPLOAD_SIZE = 10 * 1024 * 1024L;
    private static final long DOWNLOAD_PRESIGN_EXPIRE_SECONDS = 60 * 60L;
    private static final DateTimeFormatter DATE_PATH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final StudentResumeMapper studentResumeMapper;
    private final StudentService studentService;
    private final TosService tosService;

    @Override
    public List<StudentResumeRes> studentList() {
        StudentEntity student = requireCurrentStudent();
        return studentResumeMapper.selectByStudentId(student.getId()).stream().map(this::toRes).toList();
    }

    @Override
    public StudentResumeRes studentDetail(Long id) {
        StudentEntity student = requireCurrentStudent();
        StudentResumeEntity entity = studentResumeMapper.selectByIdAndStudentId(id, student.getId());
        if (entity == null) {
            throw new BusinessException("简历不存在");
        }
        return toRes(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StudentResumeRes create(StudentResumeSaveReq request) {
        StudentEntity student = requireCurrentStudent();
        Date now = new Date();
        StudentResumeEntity entity = new StudentResumeEntity();
        entity.setStudentId(student.getId());
        entity.setStudentCode(student.getStudentCode());
        entity.setStudentName(student.getStudentName());
        fillEditableFields(entity, request);
        entity.setParseStatus(StrUtil.isBlank(entity.getResumeSummary()) ? "PENDING" : "SUCCESS");
        entity.setStatus(1);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        if (Boolean.TRUE.equals(entity.getDefaultResume())) {
            studentResumeMapper.clearDefault(student.getId(), null);
        }
        studentResumeMapper.insert(entity);
        if (Boolean.TRUE.equals(entity.getDefaultResume())) {
            studentResumeMapper.clearDefault(student.getId(), entity.getId());
        }
        return toRes(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StudentResumeRes update(Long id, StudentResumeSaveReq request) {
        StudentEntity student = requireCurrentStudent();
        StudentResumeEntity entity = studentResumeMapper.selectByIdAndStudentId(id, student.getId());
        if (entity == null) {
            throw new BusinessException("简历不存在");
        }
        fillEditableFields(entity, request);
        entity.setParseStatus(StrUtil.isBlank(entity.getResumeSummary()) ? "PENDING" : "SUCCESS");
        if (Boolean.TRUE.equals(entity.getDefaultResume())) {
            studentResumeMapper.clearDefault(student.getId(), id);
        }
        studentResumeMapper.update(entity);
        return toRes(studentResumeMapper.selectByIdAndStudentId(id, student.getId()));
    }

    @Override
    public StudentResumeUploadRes uploadAndParse(MultipartFile file) {
        StudentEntity student = requireCurrentStudent();
        validateUploadFile(file);
        String originalFilename = StrUtil.blankToDefault(file.getOriginalFilename(), "resume");
        String ext = getExtension(originalFilename);
        try {
            byte[] bytes = file.getBytes();
            String objectName = buildObjectName(student.getStudentCode(), originalFilename);
            String fileUrl = tosService.uploadFile(objectName, new ByteArrayInputStream(bytes));
            String parsedText = "";
            String parseStatus = "SUCCESS";
            try {
                parsedText = limit(parseResumeText(bytes, ext), MAX_RESUME_TEXT_LENGTH);
                if (StrUtil.isBlank(parsedText)) {
                    parseStatus = "FAILED";
                }
            } catch (Exception ex) {
                parseStatus = "FAILED";
            }

            StudentResumeUploadRes res = new StudentResumeUploadRes();
            res.setOriginalFileName(originalFilename);
            res.setResumeFileUrl(fileUrl);
            res.setContentType(file.getContentType());
            res.setResumeText(parsedText);
            res.setParseStatus(parseStatus);
            return res;
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException("简历上传失败，请稍后重试");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefault(Long id) {
        StudentEntity student = requireCurrentStudent();
        StudentResumeEntity entity = studentResumeMapper.selectByIdAndStudentId(id, student.getId());
        if (entity == null) {
            throw new BusinessException("简历不存在");
        }
        studentResumeMapper.clearDefault(student.getId(), id);
        studentResumeMapper.setDefault(id, student.getId());
    }

    @Override
    public void studentDelete(Long id) {
        StudentEntity student = requireCurrentStudent();
        int affected = studentResumeMapper.softDeleteByStudent(id, student.getId());
        if (affected == 0) {
            throw new BusinessException("简历不存在");
        }
    }

    @Override
    public PageResult<StudentResumeRes> adminPage(StudentResumeQueryReq request) {
        Page<StudentResumeEntity> page = PageHelper.startPage(request.getPageNum(), request.getPageSize());
        List<StudentResumeEntity> records = studentResumeMapper.selectPage(
                trimToNull(request.getKeyword()),
                trimToNull(request.getTargetRole()),
                request.getDefaultResume()
        );
        return new PageResult<>(page.getTotal(), request.getPageNum(), request.getPageSize(), records.stream().map(this::toRes).toList());
    }

    @Override
    public StudentResumeRes adminDetail(Long id) {
        StudentResumeEntity entity = studentResumeMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("简历不存在");
        }
        return toRes(entity);
    }

    @Override
    public TosFileContentRes adminPreviewFile(Long id) {
        StudentResumeEntity entity = studentResumeMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("简历不存在");
        }
        String objectKey = resolveObjectKey(entity.getResumeFileUrl());
        if (StrUtil.isBlank(objectKey)) {
            throw new BusinessException("这份简历没有上传原文件");
        }
        return tosService.getObjectContent(objectKey, true);
    }

    @Override
    public void adminDelete(Long id) {
        int affected = studentResumeMapper.softDelete(id);
        if (affected == 0) {
            throw new BusinessException("简历不存在");
        }
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

    private void fillEditableFields(StudentResumeEntity entity, StudentResumeSaveReq request) {
        entity.setResumeName(limit(StrUtil.trim(request.getResumeName()), MAX_NAME_LENGTH));
        entity.setTargetRole(limit(trimToNull(request.getTargetRole()), MAX_ROLE_LENGTH));
        entity.setResumeText(limit(StrUtil.trim(request.getResumeText()), MAX_RESUME_TEXT_LENGTH));
        entity.setResumeSummary(limit(trimToNull(request.getResumeSummary()), MAX_SUMMARY_LENGTH));
        entity.setResumeFileUrl(limit(trimToNull(request.getResumeFileUrl()), 500));
        entity.setContentType(StrUtil.isBlank(entity.getResumeFileUrl()) ? "TEXT" : "FILE");
        entity.setDefaultResume(Boolean.TRUE.equals(request.getDefaultResume()));
        if (StrUtil.isBlank(entity.getResumeName())) {
            throw new BusinessException("简历名称不能为空");
        }
        if (StrUtil.isBlank(entity.getResumeText())) {
            throw new BusinessException("简历内容不能为空");
        }
    }

    private StudentResumeRes toRes(StudentResumeEntity entity) {
        StudentResumeRes res = new StudentResumeRes();
        res.setId(entity.getId());
        res.setStudentId(entity.getStudentId());
        res.setStudentCode(entity.getStudentCode());
        res.setStudentName(entity.getStudentName());
        res.setResumeName(entity.getResumeName());
        res.setTargetRole(entity.getTargetRole());
        res.setContentType(entity.getContentType());
        res.setResumeText(entity.getResumeText());
        res.setResumeSummary(entity.getResumeSummary());
        res.setResumeFileUrl(entity.getResumeFileUrl());
        res.setResumeFilePreviewUrl(signDownloadUrl(entity.getResumeFileUrl()));
        res.setParseStatus(entity.getParseStatus());
        res.setDefaultResume(Boolean.TRUE.equals(entity.getDefaultResume()));
        res.setCreatedAt(entity.getCreatedAt());
        res.setUpdatedAt(entity.getUpdatedAt());
        return res;
    }

    private String trimToNull(String value) {
        String trimmed = StrUtil.trim(value);
        return StrUtil.isBlank(trimmed) ? null : trimmed;
    }

    private String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private void validateUploadFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的简历文件");
        }
        if (file.getSize() > MAX_UPLOAD_SIZE) {
            throw new BusinessException("简历文件不能超过 10MB");
        }
        String originalFilename = StrUtil.trim(file.getOriginalFilename());
        String ext = getExtension(originalFilename);
        if (!List.of("pdf", "docx", "txt").contains(ext)) {
            throw new BusinessException("当前支持上传 PDF、DOCX、TXT 简历");
        }
    }

    private String parseResumeText(byte[] bytes, String ext) throws IOException {
        if ("pdf".equals(ext)) {
            try (PDDocument document = PDDocument.load(bytes)) {
                return StrUtil.trim(new PDFTextStripper().getText(document));
            }
        }
        if ("docx".equals(ext)) {
            try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(bytes));
                 XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
                return StrUtil.trim(extractor.getText());
            }
        }
        return StrUtil.trim(new String(bytes, StandardCharsets.UTF_8));
    }

    private String buildObjectName(String studentCode, String originalFilename) {
        String datePath = LocalDate.now().format(DATE_PATH_FORMATTER);
        return "student/resumes/" + studentCode + "/" + datePath + "/" + buildStoredFileName(originalFilename);
    }

    private String buildStoredFileName(String originalFilename) {
        String ext = getExtension(originalFilename);
        if (StrUtil.isBlank(ext)) {
            ext = "dat";
        }
        return IdUtil.simpleUUID() + RandomUtil.randomString("abcdefghijklmnopqrstuvwxyz0123456789", 6) + "." + ext;
    }

    private String getExtension(String originalFilename) {
        String fileName = StrUtil.trim(originalFilename);
        if (StrUtil.isBlank(fileName)) {
            return "";
        }
        int index = fileName.lastIndexOf('.');
        if (index < 0 || index >= fileName.length() - 1) {
            return "";
        }
        return fileName.substring(index + 1).replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
    }

    private String signDownloadUrl(String fileUrlOrKey) {
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
}

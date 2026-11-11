package cn.yq.modules.teaching.biz.impl;

import cn.yq.common.exception.BusinessException;
import cn.yq.modules.system.domain.entity.SysUserEntity;
import cn.yq.modules.system.mapper.SysUserMapper;
import cn.yq.modules.teaching.biz.DutyManagementBiz;
import cn.yq.modules.teaching.domain.entity.DutyAssignmentEntity;
import cn.yq.modules.teaching.domain.vo.req.ClassDutySaveReq;
import cn.yq.modules.teaching.domain.vo.req.GlobalDutySaveReq;
import cn.yq.modules.teaching.domain.vo.res.ClassDutyCandidateRecordRes;
import cn.yq.modules.teaching.domain.vo.res.ClassDutyItemRes;
import cn.yq.modules.teaching.domain.vo.res.DutyManagementDayRes;
import cn.yq.modules.teaching.domain.vo.res.DutyTeacherOptionRes;
import cn.yq.modules.teaching.domain.vo.res.GlobalDutyItemRes;
import cn.yq.modules.teaching.enums.DutyTypeEnum;
import cn.yq.modules.teaching.service.ClassService;
import cn.yq.modules.teaching.service.DutyAssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DutyManagementBizImpl implements DutyManagementBiz {

    private static final long GLOBAL_CLASS_ID = 0L;
    private static final String TEACHER_ROLE_CODE = "teacher";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private final DutyAssignmentService dutyAssignmentService;
    private final ClassService classService;
    private final SysUserMapper sysUserMapper;

    @Override
    public DutyManagementDayRes dayView(String dutyDate) {
        LocalDate targetDate = parseDate(dutyDate);
        DutyManagementDayRes res = new DutyManagementDayRes();
        res.setDutyDate(targetDate.format(DATE_FORMATTER));
        res.setClassDuties(dutyAssignmentService.listClassDutyCandidates(toDate(targetDate)).stream()
                .map(this::toClassDutyItem)
                .toList());
        res.setGlobalDuty(toGlobalDutyItem(dutyAssignmentService.getGlobalDuty(toDate(targetDate), DutyTypeEnum.GLOBAL_NIGHT_DUTY.getCode())));
        res.setTeacherOptions(listEnabledTeachers().stream().map(this::toTeacherOption).toList());
        return res;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveClassDuty(Long classId, ClassDutySaveReq request) {
        classService.detail(classId);
        LocalDate targetDate = parseDate(request.getDutyDate());
        validateTeacher(request.getTeacherId());
        DutyAssignmentEntity entity = buildDutyEntity(targetDate, DutyTypeEnum.CLASS_NIGHT_DUTY, classId, request.getTeacherId(), request.getRemark());
        dutyAssignmentService.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveGlobalDuty(GlobalDutySaveReq request) {
        LocalDate targetDate = parseDate(request.getDutyDate());
        validateTeacher(request.getTeacherId());
        DutyAssignmentEntity entity = buildDutyEntity(targetDate, DutyTypeEnum.GLOBAL_NIGHT_DUTY, GLOBAL_CLASS_ID, request.getTeacherId(), request.getRemark());
        dutyAssignmentService.save(entity);
    }

    private ClassDutyItemRes toClassDutyItem(ClassDutyCandidateRecordRes record) {
        ClassDutyItemRes item = new ClassDutyItemRes();
        item.setClassId(record.getClassId());
        item.setClassLabel((record.getCampusName() == null || record.getCampusName().isBlank() ? "" : record.getCampusName() + " ") + "第" + record.getClassTerm() + "期");
        item.setCampusName(record.getCampusName());
        item.setCourseName(record.getCourseName());
        item.setStartTime(formatDate(record.getStartTime()));
        item.setGraduateDate(formatDate(record.getGraduateDate()));
        item.setDutyLabel(DutyTypeEnum.CLASS_NIGHT_DUTY.getLabel());
        item.setDutyTimeRange(DutyTypeEnum.CLASS_NIGHT_DUTY.getStartTime() + "-" + DutyTypeEnum.CLASS_NIGHT_DUTY.getEndTime());
        item.setTeacherId(record.getTeacherId());
        item.setTeacherName(record.getTeacherName());
        item.setRemark(record.getRemark());
        return item;
    }

    private GlobalDutyItemRes toGlobalDutyItem(DutyAssignmentEntity entity) {
        GlobalDutyItemRes item = new GlobalDutyItemRes();
        item.setDutyLabel(DutyTypeEnum.GLOBAL_NIGHT_DUTY.getLabel());
        item.setDutyTimeRange(DutyTypeEnum.GLOBAL_NIGHT_DUTY.getStartTime() + "-" + DutyTypeEnum.GLOBAL_NIGHT_DUTY.getEndTime());
        if (entity != null) {
            item.setTeacherId(entity.getTeacherId());
            item.setTeacherName(entity.getTeacherName());
            item.setRemark(entity.getRemark());
        }
        return item;
    }

    private DutyTeacherOptionRes toTeacherOption(SysUserEntity teacher) {
        DutyTeacherOptionRes item = new DutyTeacherOptionRes();
        item.setTeacherId(teacher.getId());
        item.setTeacherName(resolveTeacherName(teacher));
        return item;
    }

    private List<SysUserEntity> listEnabledTeachers() {
        return sysUserMapper.selectPage(null, 1, TEACHER_ROLE_CODE);
    }

    private void validateTeacher(Long teacherId) {
        boolean exists = listEnabledTeachers().stream().anyMatch(item -> item.getId().equals(teacherId));
        if (!exists) {
            throw new BusinessException("所选值班老师不存在或已停用");
        }
    }

    private DutyAssignmentEntity buildDutyEntity(LocalDate dutyDate,
                                                 DutyTypeEnum dutyType,
                                                 Long classId,
                                                 Long teacherId,
                                                 String remark) {
        DutyAssignmentEntity entity = new DutyAssignmentEntity();
        entity.setDutyDate(toDate(dutyDate));
        entity.setDutyType(dutyType.getCode());
        entity.setClassId(classId);
        entity.setStartTime(dutyType.getStartTime() + ":00");
        entity.setEndTime(dutyType.getEndTime() + ":00");
        entity.setTeacherId(teacherId);
        entity.setRemark(remark);
        entity.setCreatedAt(new Date());
        entity.setUpdatedAt(new Date());
        return entity;
    }

    private String resolveTeacherName(SysUserEntity teacher) {
        if (teacher.getRealName() != null && !teacher.getRealName().isBlank()) {
            return teacher.getRealName();
        }
        if (teacher.getNickname() != null && !teacher.getNickname().isBlank()) {
            return teacher.getNickname();
        }
        return teacher.getUsername();
    }

    private String formatDate(Date date) {
        if (date == null) {
            return "-";
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().format(DATE_FORMATTER);
    }

    private LocalDate parseDate(String dutyDate) {
        try {
            return LocalDate.parse(dutyDate);
        } catch (DateTimeParseException ex) {
            throw new BusinessException("日期格式不正确");
        }
    }

    private Date toDate(LocalDate date) {
        return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}

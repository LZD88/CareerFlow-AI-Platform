package cn.yq.modules.teaching.domain.vo.res;

import lombok.Data;

import java.util.List;

@Data
public class DutyManagementDayRes {

    private String dutyDate;

    private List<ClassDutyItemRes> classDuties;

    private GlobalDutyItemRes globalDuty;

    private List<DutyTeacherOptionRes> teacherOptions;
}

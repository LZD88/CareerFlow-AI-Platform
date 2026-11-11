package cn.yq.modules.teaching.biz.impl;

import cn.yq.common.exception.BusinessException;
import cn.yq.modules.teaching.biz.ClassScheduleBiz;
import cn.yq.modules.teaching.domain.entity.ClassEntity;
import cn.yq.modules.teaching.domain.entity.ClassScheduleEntity;
import cn.yq.modules.teaching.domain.entity.CourseDetailEntity;
import cn.yq.modules.teaching.domain.vo.req.AssignStageTeacherReq;
import cn.yq.modules.teaching.domain.vo.req.ClassScheduleArrangeReq;
import cn.yq.modules.teaching.domain.vo.req.ClassScheduleChangeContentReq;
import cn.yq.modules.teaching.domain.vo.req.ClassScheduleChangeTeacherReq;
import cn.yq.modules.teaching.domain.vo.req.ClassScheduleDeleteReq;
import cn.yq.modules.teaching.domain.vo.req.ClassScheduleGenerateReq;
import cn.yq.modules.teaching.domain.vo.res.ClassStageTeacherRes;
import cn.yq.modules.teaching.domain.vo.res.ClassScheduleCalendarDayRes;
import cn.yq.modules.teaching.domain.vo.res.ClassScheduleCalendarRes;
import cn.yq.modules.teaching.domain.vo.res.HolidayInfoRes;
import cn.yq.modules.teaching.domain.vo.res.StageTeacherOptionRes;
import cn.yq.modules.teaching.domain.vo.res.TeacherScheduleBlockRes;
import cn.yq.modules.teaching.domain.vo.res.TeacherScheduleCalendarRes;
import cn.yq.modules.teaching.domain.vo.res.TeacherScheduleDateRes;
import cn.yq.modules.teaching.domain.vo.res.TeacherScheduleIdleTeacherRes;
import cn.yq.modules.teaching.domain.vo.res.TeacherScheduleRecordRes;
import cn.yq.modules.teaching.domain.vo.res.TeacherScheduleTeacherRowRes;
import cn.yq.modules.teaching.enums.ClassScheduleTypeEnum;
import cn.yq.modules.teaching.service.ClassScheduleService;
import cn.yq.modules.teaching.service.ClassService;
import cn.yq.modules.teaching.service.CourseDetailService;
import cn.yq.modules.system.domain.entity.SysUserEntity;
import cn.yq.modules.system.mapper.SysUserMapper;
import cn.yq.modules.teaching.support.HolidaySupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClassScheduleBizImpl implements ClassScheduleBiz {

    private static final String TEACHER_ROLE_CODE = "teacher";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private final ClassService classService;
    private final CourseDetailService courseDetailService;
    private final ClassScheduleService classScheduleService;
    private final SysUserMapper sysUserMapper;
    private final HolidaySupport holidaySupport;

    /**
     * 生成班级完整课表。
     * 生成过程会把正式上课、自习、休息都统一落表，后续所有调整都围绕这份日快照进行。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void generate(Long classId, ClassScheduleGenerateReq request) {
        ClassEntity classInfo = classService.detail(classId);
        if (classScheduleService.hasSchedule(classId)) {
            throw new BusinessException("该班级已生成课表");
        }

        List<CourseDetailEntity> details = courseDetailService.listByCourseId(classInfo.getCourseId());
        if (details.isEmpty()) {
            throw new BusinessException("当前课程还没有课程详情，无法生成课表");
        }

        LocalDate cursor = toLocalDate(request.getFirstCourseDate());
        // 给排课过程留一个安全上限，避免外部节假日数据异常时进入死循环。
        LocalDate maxDate = cursor.plusDays(details.size() * 5L + 365);
        List<ClassScheduleEntity> records = new ArrayList<>();
        Date now = new Date();
        int detailIndex = 0;
        while (detailIndex < details.size()) {
            if (cursor.isAfter(maxDate)) {
                throw new BusinessException("课表生成失败，请检查开始日期和课程内容");
            }

            HolidayInfoRes holidayInfo = holidaySupport.getMonthHolidayInfo(cursor.getYear(), cursor.getMonthValue()).get(cursor.toString());
            if (isRestDay(cursor, holidayInfo)) {
                records.add(buildRestRecord(classInfo, cursor, holidayInfo, now));
                cursor = cursor.plusDays(1);
                continue;
            }

            if (isSelfStudyDay(cursor)) {
                records.add(buildSelfStudyRecord(classInfo, cursor, now));
                cursor = cursor.plusDays(1);
                continue;
            }

            CourseDetailEntity detail = details.get(detailIndex);
            records.add(buildClassRecord(classInfo, detail, cursor, now));

            detailIndex += 1;
            cursor = cursor.plusDays(1);
        }

        classScheduleService.createBatch(records);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void arrange(Long classId, ClassScheduleArrangeReq request) {
        ClassEntity classInfo = classService.detail(classId);
        List<ClassScheduleEntity> schedules = getSortedSchedules(classId);

        LocalDate targetDate = toLocalDate(request.getScheduleDate());
        LocalDate firstDate = toLocalDate(schedules.get(0).getScheduleDate());
        LocalDate lastDate = toLocalDate(schedules.get(schedules.size() - 1).getScheduleDate());
        if (targetDate.isBefore(firstDate) || targetDate.isAfter(lastDate)) {
            throw new BusinessException("只能调整当前课表周期内的日期");
        }

        Map<LocalDate, ClassScheduleEntity> scheduleMap = toScheduleMap(schedules);
        ClassScheduleEntity currentRecord = requireSchedule(scheduleMap, targetDate, "当前日期没有课表记录，无法调整");

        ClassScheduleTypeEnum targetType = ClassScheduleTypeEnum.fromCode(request.getScheduleType());
        validateArrangeRequest(targetType, request);
        validateArrangeTeacher(classId, targetDate, targetType, request.getTeacherId());
        Date now = new Date();

        // “是否顺延”只取决于目标日期原来是不是正式上课日。
        // 比如：在休息/自习日加课，只占用当天；在正式上课日插入一节课，后续正式课程整体后移。
        List<ClassScheduleEntity> newTailRecords = shouldShiftTeaching(currentRecord, targetType)
                ? rebuildTailSchedules(classInfo, schedules, scheduleMap, targetDate, targetType, request.getStageName(), request.getCourseContent(), request.getTeacherId(), now)
                : rebuildDirectReplace(classInfo, schedules, targetDate, targetType, request.getStageName(), request.getCourseContent(), request.getTeacherId(), now);

        classScheduleService.replaceFromDate(classId, toDate(targetDate), newTailRecords);
    }

    /**
     * 老师选择下拉的数据来源。
     * 这里复用“按天查空闲讲师”的规则，既用于新增课程，也用于单天变更老师。
     */
    @Override
    public List<StageTeacherOptionRes> availableArrangeTeachers(Long classId, String scheduleDate) {
        classService.detail(classId);
        LocalDate targetDate = parseDate(scheduleDate);
        List<SysUserEntity> enabledUsers = listEnabledTeachers();
        List<ClassScheduleEntity> assignedSchedules = classScheduleService.listAssignedTeacherSchedules();
        return buildAvailableTeachersByDate(classId, targetDate, enabledUsers, assignedSchedules);
    }

    /**
     * 只更新当天课表的课程内容。
     * 这里故意不改 stageName / courseDetailId / courseDayNum，避免反向污染课程模板语义。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeContent(Long classId, ClassScheduleChangeContentReq request) {
        classService.detail(classId);
        ClassScheduleEntity targetSchedule = requireClassSchedule(classId, toLocalDate(request.getScheduleDate()), "当天没有可变更内容的上课安排");
        String courseContent = request.getCourseContent() == null ? null : request.getCourseContent().trim();
        if (courseContent == null || courseContent.isBlank()) {
            throw new BusinessException("课程内容不能为空");
        }
        classScheduleService.updateContentByClassAndDate(classId, targetSchedule.getScheduleDate(), courseContent);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeTeacher(Long classId, ClassScheduleChangeTeacherReq request) {
        classService.detail(classId);
        LocalDate targetDate = toLocalDate(request.getScheduleDate());
        ClassScheduleEntity targetSchedule = requireClassSchedule(classId, targetDate, "当天没有可变更老师的上课安排");
        validateArrangeTeacher(classId, targetDate, ClassScheduleTypeEnum.CLASS, request.getTeacherId());
        classScheduleService.updateTeacherByClassAndDate(classId, targetSchedule.getScheduleDate(), request.getTeacherId());
    }

    /**
     * 删除单天课表。
     * 这里会区分“固定日删除”和“正式上课日删除”两套路径，避免固定休息 / 自习日误触发前移。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSchedule(Long classId, ClassScheduleDeleteReq request) {
        ClassEntity classInfo = classService.detail(classId);
        List<ClassScheduleEntity> schedules = getSortedSchedules(classId);

        LocalDate targetDate = toLocalDate(request.getScheduleDate());
        Map<LocalDate, ClassScheduleEntity> scheduleMap = toScheduleMap(schedules);
        ClassScheduleEntity currentRecord = requireSchedule(scheduleMap, targetDate, "当前日期没有课表记录，无法删除");

        Date now = new Date();
        boolean targetIsFixedDay = isFixedDay(targetDate);
        List<ClassScheduleEntity> newTailRecords = !targetIsFixedDay && isClassSchedule(currentRecord)
                ? rebuildAfterDeleteClass(classInfo, schedules, scheduleMap, targetDate, now)
                : rebuildAfterDeleteFixed(classInfo, currentRecord, schedules, targetDate, now);
        classScheduleService.replaceFromDate(classId, toDate(targetDate), newTailRecords);
    }

    /**
     * 阶段老师分配页的数据装配。
     * 这里除了正常的统一老师状态，还会额外计算“已拆分授课”，让前端区分“未分配”和“阶段内单天改老师”。
     */
    @Override
    public List<ClassStageTeacherRes> stageTeachers(Long classId) {
        List<ClassScheduleEntity> currentSchedules = getSortedSchedules(classId);
        classService.detail(classId);
        Map<String, StageWindow> currentStageMap = buildStageWindowMap(classId, currentSchedules);
        if (currentStageMap.isEmpty()) {
            throw new BusinessException("当前班级还没有可分配老师的课程阶段");
        }

        List<SysUserEntity> enabledUsers = listEnabledTeachers();
        Map<Long, String> userNameMap = enabledUsers.stream()
                .collect(Collectors.toMap(SysUserEntity::getId, this::resolveTeacherName, (left, right) -> left));

        Map<String, Long> currentTeacherMap = buildCurrentStageTeacherMap(currentSchedules);
        Map<String, Boolean> stageSplitMap = buildStageSplitMap(currentSchedules);
        Map<String, String> stageTeacherSummaryMap = buildStageTeacherSummaryMap(currentSchedules, userNameMap);
        List<ClassScheduleEntity> assignedSchedules = classScheduleService.listAssignedTeacherSchedules();
        Map<Long, Map<String, StageWindow>> allStageMaps = buildStageWindowMapByClassId(currentStageMap, assignedSchedules, classId);

        List<ClassStageTeacherRes> result = new ArrayList<>();
        for (StageWindow window : currentStageMap.values()) {
            // 阶段弹窗要同时支持三种状态：
            // 1. 完全未分配：teacherId / teacherName 为空
            // 2. 整个阶段统一一个老师：teacherId / teacherName 有值
            // 3. 阶段内某几天被单独改过老师：splitAssigned = true，前端展示“已拆分授课”
            ClassStageTeacherRes item = new ClassStageTeacherRes();
            item.setStageName(window.getStageName());
            item.setDayCount(window.getDayCount());
            item.setStartDate(window.getStartDate().toString());
            item.setEndDate(window.getEndDate().toString());
            Long teacherId = currentTeacherMap.get(window.getStageName());
            item.setTeacherId(teacherId);
            item.setTeacherName(teacherId == null ? null : userNameMap.get(teacherId));
            item.setSplitAssigned(stageSplitMap.getOrDefault(window.getStageName(), Boolean.FALSE));
            item.setTeacherSummary(stageTeacherSummaryMap.get(window.getStageName()));
            item.setAvailableTeachers(buildAvailableTeacherOptions(classId, window, teacherId, enabledUsers, assignedSchedules, allStageMaps));
            result.add(item);
        }
        result.sort(Comparator.comparing(ClassStageTeacherRes::getStartDate));
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignStageTeachers(Long classId, AssignStageTeacherReq request) {
        classService.detail(classId);
        List<ClassScheduleEntity> currentSchedules = getSortedSchedules(classId);
        Map<String, StageWindow> currentStageMap = buildStageWindowMap(classId, currentSchedules);
        if (currentStageMap.isEmpty()) {
            throw new BusinessException("当前班级还没有可分配老师的课程阶段");
        }

        Map<String, Long> requestMap = request.getItems().stream()
                .filter(item -> item.getStageName() != null && !item.getStageName().isBlank())
                .collect(Collectors.toMap(AssignStageTeacherReq.StageTeacherAssignItem::getStageName,
                        AssignStageTeacherReq.StageTeacherAssignItem::getTeacherId,
                        (left, right) -> right));
        for (String stageName : requestMap.keySet()) {
            if (!currentStageMap.containsKey(stageName)) {
                throw new BusinessException("存在无效的课程阶段: " + stageName);
            }
        }

        List<SysUserEntity> enabledUsers = listEnabledTeachers();
        Set<Long> enabledUserIds = enabledUsers.stream().map(SysUserEntity::getId).collect(Collectors.toSet());
        List<ClassScheduleEntity> assignedSchedules = classScheduleService.listAssignedTeacherSchedules();
        Map<Long, Map<String, StageWindow>> allStageMaps = buildStageWindowMapByClassId(currentStageMap, assignedSchedules, classId);

        for (StageWindow window : currentStageMap.values()) {
            Long teacherId = requestMap.get(window.getStageName());
            if (teacherId != null && !enabledUserIds.contains(teacherId)) {
                throw new BusinessException("所选老师不存在或已停用");
            }
            if (teacherId != null && isTeacherOccupied(classId, window, teacherId, assignedSchedules, allStageMaps)) {
                throw new BusinessException(window.getStageName() + " 阶段所选老师在该时间段已有课");
            }
            classScheduleService.updateTeacherByClassAndStage(classId, window.getStageName(), teacherId);
        }
    }

    @Override
    public ClassScheduleCalendarRes calendar(Long classId, Integer year, Integer month) {
        ClassEntity classInfo = classService.detail(classId);
        int targetYear = year == null ? LocalDate.now().getYear() : year;
        int targetMonth = month == null ? LocalDate.now().getMonthValue() : month;

        LocalDate monthStart = LocalDate.of(targetYear, targetMonth, 1);
        LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());
        // 月视图按周展示，所以这里把查询范围补齐到周一开始、周日结束。
        LocalDate gridStart = monthStart.minusDays(monthStart.getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue());
        LocalDate gridEnd = monthEnd.plusDays(DayOfWeek.SUNDAY.getValue() - monthEnd.getDayOfWeek().getValue());
        Map<String, HolidayInfoRes> holidayMap = getHolidayInfoBetween(gridStart, gridEnd);

        List<ClassScheduleEntity> schedules = classScheduleService.listByClassIdAndDateRange(classId, toDate(gridStart), toDate(gridEnd));
        schedules.sort(Comparator.comparing(ClassScheduleEntity::getScheduleDate));
        Map<String, ClassScheduleEntity> scheduleMap = new HashMap<>();
        schedules.forEach(item -> scheduleMap.put(toLocalDate(item.getScheduleDate()).toString(), item));

        LocalDate firstClassDate = schedules.isEmpty() ? null : toLocalDate(schedules.get(0).getScheduleDate());
        LocalDate lastClassDate = schedules.isEmpty() ? null : toLocalDate(schedules.get(schedules.size() - 1).getScheduleDate());

        List<ClassScheduleCalendarDayRes> days = new ArrayList<>();
        LocalDate current = gridStart;
        while (!current.isAfter(gridEnd)) {
            ClassScheduleCalendarDayRes day = new ClassScheduleCalendarDayRes();
            day.setDate(current.toString());
            day.setDayOfMonth(current.getDayOfMonth());
            day.setCurrentMonth(current.getMonthValue() == targetMonth);

            ClassScheduleEntity schedule = scheduleMap.get(current.toString());
            HolidayInfoRes holiday = holidayMap.get(current.toString());
            if (schedule != null) {
                applyStoredSchedule(day, current, schedule, holiday);
                day.setCourseDayNum(schedule.getCourseDayNum());
                day.setCourseContent(schedule.getCourseContent());
                day.setStageName(schedule.getStageName());
                day.setTeacherId(schedule.getTeacherId());
                day.setTeacherName(schedule.getTeacherName());
            } else if (isRestDay(current, holiday)) {
                day.setLabel(ClassScheduleTypeEnum.REST.getLabel());
                day.setScheduleType(ClassScheduleTypeEnum.REST.getCode());
                day.setHoliday(Boolean.TRUE);
                day.setHolidayName(resolveHolidayName(current, holiday));
            } else if (firstClassDate != null && lastClassDate != null
                    && !current.isBefore(firstClassDate) && !current.isAfter(lastClassDate)
                    && isSelfStudyDay(current)) {
                // 落在课表周期内的周四固定展示为自习。
                day.setLabel(ClassScheduleTypeEnum.SELF_STUDY.getLabel());
                day.setScheduleType(ClassScheduleTypeEnum.SELF_STUDY.getCode());
            }
            days.add(day);
            current = current.plusDays(1);
        }

        ClassScheduleCalendarRes res = new ClassScheduleCalendarRes();
        res.setClassInfo(classInfo);
        res.setYear(targetYear);
        res.setMonth(targetMonth);
        res.setDays(days);
        return res;
    }

    @Override
    public TeacherScheduleCalendarRes teacherCalendar(String startDate, String endDate, String keyword, Long teacherId) {
        LocalDate start = startDate == null || startDate.isBlank()
                ? LocalDate.now().withDayOfMonth(1)
                : parseDate(startDate);
        LocalDate end = endDate == null || endDate.isBlank()
                ? start.withDayOfMonth(start.lengthOfMonth())
                : parseDate(endDate);
        if (end.isBefore(start)) {
            throw new BusinessException("结束日期不能早于开始日期");
        }

        List<SysUserEntity> enabledTeachers = listEnabledTeachers(keyword).stream()
                .filter(item -> teacherId == null || Objects.equals(item.getId(), teacherId))
                .sorted(Comparator.comparing(this::resolveTeacherDisplayName))
                .toList();
        List<Long> teacherIds = enabledTeachers.stream().map(SysUserEntity::getId).toList();
        List<TeacherScheduleRecordRes> records = classScheduleService.listTeacherSchedulesByDateRange(toDate(start), toDate(end), teacherIds);
        Map<Long, List<TeacherScheduleRecordRes>> recordsByTeacher = records.stream()
                .collect(Collectors.groupingBy(TeacherScheduleRecordRes::getTeacherId));

        TeacherScheduleCalendarRes res = new TeacherScheduleCalendarRes();
        res.setStartDate(start.format(DATE_FORMATTER));
        res.setEndDate(end.format(DATE_FORMATTER));
        res.setDateHeaders(buildTeacherDateHeaders(start, end));
        res.setTeachers(enabledTeachers.stream()
                .filter(item -> recordsByTeacher.containsKey(item.getId()))
                .map(item -> buildTeacherRow(item, recordsByTeacher.get(item.getId()), start))
                .toList());
        res.setIdleTeachers(enabledTeachers.stream()
                .filter(item -> !recordsByTeacher.containsKey(item.getId()))
                .map(this::buildIdleTeacher)
                .toList());
        return res;
    }

    private void applyStoredSchedule(ClassScheduleCalendarDayRes day,
                                     LocalDate current,
                                     ClassScheduleEntity schedule,
                                     HolidayInfoRes holiday) {
        String scheduleType = schedule.getScheduleType();
        if (ClassScheduleTypeEnum.SELF_STUDY.getCode().equals(scheduleType)) {
            day.setLabel(ClassScheduleTypeEnum.SELF_STUDY.getLabel());
            day.setScheduleType(ClassScheduleTypeEnum.SELF_STUDY.getCode());
            return;
        }
        if (ClassScheduleTypeEnum.REST.getCode().equals(scheduleType)) {
            day.setLabel(ClassScheduleTypeEnum.REST.getLabel());
            day.setScheduleType(ClassScheduleTypeEnum.REST.getCode());
            day.setHoliday(Boolean.TRUE);
            day.setHolidayName(resolveHolidayName(current, holiday));
            return;
        }
        if (ClassScheduleTypeEnum.EMPTY.getCode().equals(scheduleType)) {
            day.setLabel(null);
            day.setScheduleType(ClassScheduleTypeEnum.EMPTY.getCode());
            day.setHoliday(Boolean.FALSE);
            day.setHolidayName(null);
            return;
        }

        day.setLabel(ClassScheduleTypeEnum.CLASS.getLabel());
        day.setScheduleType(ClassScheduleTypeEnum.CLASS.getCode());
    }

    private List<ClassScheduleEntity> rebuildDirectReplace(ClassEntity classInfo,
                                                           List<ClassScheduleEntity> schedules,
                                                           LocalDate targetDate,
                                                           ClassScheduleTypeEnum targetType,
                                                           String stageName,
                                                           String courseContent,
                                                           Long teacherId,
                                                           Date now) {
        List<ClassScheduleEntity> records = new ArrayList<>();
        for (ClassScheduleEntity schedule : schedules) {
            LocalDate date = toLocalDate(schedule.getScheduleDate());
            if (date.isBefore(targetDate)) {
                continue;
            }
            if (date.equals(targetDate)) {
                // 不顺延场景只替换当天：后面的记录按原样复制。
                records.add(buildManualRecord(classInfo, targetDate, targetType, stageName, courseContent, teacherId, now));
                continue;
            }
            records.add(copyRecord(schedule, now));
        }
        return records;
    }

    private List<ClassScheduleEntity> rebuildAfterDeleteFixed(ClassEntity classInfo,
                                                              ClassScheduleEntity currentRecord,
                                                              List<ClassScheduleEntity> schedules,
                                                              LocalDate targetDate,
                                                              Date now) {
        List<ClassScheduleEntity> records = new ArrayList<>();
        records.add(buildRestoredFixedRecord(classInfo, currentRecord, targetDate, now));
        for (ClassScheduleEntity schedule : schedules) {
            LocalDate date = toLocalDate(schedule.getScheduleDate());
            if (date.isAfter(targetDate)) {
                records.add(copyRecord(schedule, now));
            }
        }
        return records;
    }

    private List<ClassScheduleEntity> rebuildAfterDeleteClass(ClassEntity classInfo,
                                                              List<ClassScheduleEntity> schedules,
                                                              Map<LocalDate, ClassScheduleEntity> scheduleMap,
                                                              LocalDate targetDate,
                                                              Date now) {
        LinkedList<ClassScheduleEntity> teachingQueue = new LinkedList<>();
        for (ClassScheduleEntity schedule : schedules) {
            LocalDate date = toLocalDate(schedule.getScheduleDate());
            if (date.isAfter(targetDate) && isClassSchedule(schedule)) {
                // 删除正式课程后，后面的正式课程需要依次前移补位。
                teachingQueue.add(cloneTeachingRecord(schedule, now));
            }
        }

        List<ClassScheduleEntity> records = new ArrayList<>();
        LocalDate cursor = targetDate;
        boolean targetFilled = false;
        while (!teachingQueue.isEmpty()) {
            ClassScheduleEntity existing = scheduleMap.get(cursor);
            if (existing != null && !isClassSchedule(existing)) {
                // 已存在的休息/自习日保持不动，正式课程只能跳过这些固定槽位继续往后补。
                records.add(copyRecord(existing, now));
                cursor = cursor.plusDays(1);
                continue;
            }
            if (existing == null && isFixedDay(cursor)) {
                // 课表尾部延伸到历史未落表的固定日时，需要自动补一条固定记录，保证时间线连续。
                records.add(buildAutoFixedRecord(classInfo, cursor, now));
                cursor = cursor.plusDays(1);
                continue;
            }

            ClassScheduleEntity teaching = teachingQueue.pollFirst();
            teaching.setClassId(classInfo.getId());
            teaching.setCourseId(classInfo.getCourseId());
            teaching.setScheduleDate(toDate(cursor));
            teaching.setCreatedAt(now);
            teaching.setUpdatedAt(now);
            records.add(teaching);
            targetFilled = true;
            cursor = cursor.plusDays(1);
        }

        if (!targetFilled) {
            // 理论上极少发生，但为了保证 replaceFromDate 后当天仍有一条记录，这里兜底补空白日。
            records.add(buildEmptyRecord(classInfo, targetDate, now));
            cursor = targetDate.plusDays(1);
        }

        for (ClassScheduleEntity schedule : schedules) {
            LocalDate date = toLocalDate(schedule.getScheduleDate());
            if (date.isAfter(targetDate) && date.isAfter(cursor.minusDays(1)) && !isClassSchedule(schedule)) {
                records.add(copyRecord(schedule, now));
            }
        }
        records.sort(Comparator.comparing(ClassScheduleEntity::getScheduleDate));
        return records;
    }

    private List<ClassScheduleEntity> rebuildTailSchedules(ClassEntity classInfo,
                                                           List<ClassScheduleEntity> schedules,
                                                           Map<LocalDate, ClassScheduleEntity> scheduleMap,
                                                           LocalDate targetDate,
                                                           ClassScheduleTypeEnum targetType,
                                                           String stageName,
                                                           String courseContent,
                                                           Long teacherId,
                                                           Date now) {
        LinkedList<ClassScheduleEntity> teachingQueue = new LinkedList<>();
        for (ClassScheduleEntity schedule : schedules) {
            LocalDate date = toLocalDate(schedule.getScheduleDate());
            if (!date.isBefore(targetDate) && isClassSchedule(schedule)) {
                // 从目标日期开始，把原有正式课程全部入队，后面再重新分配到新的日期槽位。
                teachingQueue.add(cloneTeachingRecord(schedule, now));
            }
        }

        List<ClassScheduleEntity> records = new ArrayList<>();
        // 目标日先放用户这次想插入/替换的记录，后面的正式课程再依次后移。
        records.add(buildManualRecord(classInfo, targetDate, targetType, stageName, courseContent, teacherId, now));

        LocalDate cursor = targetDate.plusDays(1);
        while (!teachingQueue.isEmpty()) {
            ClassScheduleEntity existing = scheduleMap.get(cursor);
            if (existing != null && !isClassSchedule(existing)) {
                // 固定休息/自习日不参与顺延占位，直接原样保留。
                records.add(copyRecord(existing, now));
                cursor = cursor.plusDays(1);
                continue;
            }

            if (existing == null && isFixedDay(cursor)) {
                // 顺延超出当前已落库范围时，也要把自然遇到的固定日补出来。
                records.add(buildAutoFixedRecord(classInfo, cursor, now));
                cursor = cursor.plusDays(1);
                continue;
            }

            ClassScheduleEntity teaching = teachingQueue.pollFirst();
            teaching.setClassId(classInfo.getId());
            teaching.setCourseId(classInfo.getCourseId());
            teaching.setScheduleDate(toDate(cursor));
            teaching.setCreatedAt(now);
            teaching.setUpdatedAt(now);
            records.add(teaching);
            cursor = cursor.plusDays(1);
        }

        return records;
    }

    private ClassScheduleEntity buildClassRecord(ClassEntity classInfo,
                                                 CourseDetailEntity detail,
                                                 LocalDate scheduleDate,
                                                 Date now) {
        ClassScheduleEntity record = initBaseRecord(classInfo, scheduleDate, ClassScheduleTypeEnum.CLASS, now);
        record.setCourseDetailId(detail.getId());
        record.setCourseDayNum(detail.getDayNum());
        record.setCourseContent(detail.getCourseContent());
        record.setStageName(detail.getStageName());
        return record;
    }

    private ClassScheduleEntity buildManualRecord(ClassEntity classInfo,
                                                  LocalDate scheduleDate,
                                                  ClassScheduleTypeEnum scheduleType,
                                                  String stageName,
                                                  String courseContent,
                                                  Long teacherId,
                                                  Date now) {
        if (scheduleType == ClassScheduleTypeEnum.CLASS) {
            ClassScheduleEntity record = initBaseRecord(classInfo, scheduleDate, scheduleType, now);
            // 手工加课不会反查课程模板，只把当前输入快照直接落到课表表中。
            record.setStageName(stageName == null ? null : stageName.trim());
            record.setCourseContent(courseContent == null ? null : courseContent.trim());
            record.setTeacherId(teacherId);
            return record;
        }

        if (scheduleType == ClassScheduleTypeEnum.SELF_STUDY) {
            return buildSelfStudyRecord(classInfo, scheduleDate, now);
        }

        HolidayInfoRes holidayInfo = holidaySupport.getMonthHolidayInfo(scheduleDate.getYear(), scheduleDate.getMonthValue()).get(scheduleDate.toString());
        return buildRestRecord(classInfo, scheduleDate, holidayInfo, now);
    }

    private ClassScheduleEntity buildSelfStudyRecord(ClassEntity classInfo, LocalDate scheduleDate, Date now) {
        ClassScheduleEntity record = initBaseRecord(classInfo, scheduleDate, ClassScheduleTypeEnum.SELF_STUDY, now);
        record.setCourseContent(ClassScheduleTypeEnum.SELF_STUDY.getLabel());
        return record;
    }

    private ClassScheduleEntity buildRestRecord(ClassEntity classInfo,
                                                LocalDate scheduleDate,
                                                HolidayInfoRes holidayInfo,
                                                Date now) {
        ClassScheduleEntity record = initBaseRecord(classInfo, scheduleDate, ClassScheduleTypeEnum.REST, now);
        record.setCourseContent(resolveHolidayName(scheduleDate, holidayInfo));
        return record;
    }

    private ClassScheduleEntity initBaseRecord(ClassEntity classInfo,
                                               LocalDate scheduleDate,
                                               ClassScheduleTypeEnum scheduleType,
                                               Date now) {
        ClassScheduleEntity record = new ClassScheduleEntity();
        record.setClassId(classInfo.getId());
        record.setCourseId(classInfo.getCourseId());
        record.setScheduleDate(toDate(scheduleDate));
        record.setScheduleType(scheduleType.getCode());
        record.setCreatedAt(now);
        record.setUpdatedAt(now);
        return record;
    }

    private ClassScheduleEntity buildAutoFixedRecord(ClassEntity classInfo, LocalDate scheduleDate, Date now) {
        HolidayInfoRes holidayInfo = holidaySupport.getMonthHolidayInfo(scheduleDate.getYear(), scheduleDate.getMonthValue()).get(scheduleDate.toString());
        if (isRestDay(scheduleDate, holidayInfo)) {
            return buildRestRecord(classInfo, scheduleDate, holidayInfo, now);
        }
        if (isSelfStudyDay(scheduleDate)) {
            return buildSelfStudyRecord(classInfo, scheduleDate, now);
        }
        throw new BusinessException("课表顺延失败，请检查目标日期");
    }

    private ClassScheduleEntity buildEmptyRecord(ClassEntity classInfo, LocalDate scheduleDate, Date now) {
        ClassScheduleEntity record = initBaseRecord(classInfo, scheduleDate, ClassScheduleTypeEnum.EMPTY, now);
        record.setCourseContent(null);
        record.setStageName(null);
        return record;
    }

    private ClassScheduleEntity buildRestoredFixedRecord(ClassEntity classInfo,
                                                         ClassScheduleEntity currentRecord,
                                                         LocalDate scheduleDate,
                                                         Date now) {
        if (isFixedDay(scheduleDate)) {
            return buildAutoFixedRecord(classInfo, scheduleDate, now);
        }
        if (Objects.equals(currentRecord.getScheduleType(), ClassScheduleTypeEnum.REST.getCode())) {
            HolidayInfoRes holidayInfo = holidaySupport.getMonthHolidayInfo(scheduleDate.getYear(), scheduleDate.getMonthValue()).get(scheduleDate.toString());
            return buildRestRecord(classInfo, scheduleDate, holidayInfo, now);
        }
        if (Objects.equals(currentRecord.getScheduleType(), ClassScheduleTypeEnum.SELF_STUDY.getCode())) {
            return buildSelfStudyRecord(classInfo, scheduleDate, now);
        }
        return buildEmptyRecord(classInfo, scheduleDate, now);
    }

    private Map<String, StageWindow> buildStageWindowMap(Long classId, List<ClassScheduleEntity> schedules) {
        Map<String, StageWindow> stageMap = new HashMap<>();
        schedules.stream()
                .filter(this::isClassSchedule)
                .filter(item -> item.getStageName() != null && !item.getStageName().isBlank())
                .forEach(item -> {
                    String stageName = item.getStageName().trim();
                    LocalDate date = toLocalDate(item.getScheduleDate());
                    // 阶段时间窗完全由“实际落库的正式课程日期”推导，
                    // 这样单天加课 / 删课 / 顺延之后，阶段可分配老师范围也会自动跟着真实课表走。
                    StageWindow window = stageMap.computeIfAbsent(stageName, key -> new StageWindow(classId, stageName, date, date));
                    window.include(date);
                });
        return stageMap;
    }

    private Map<String, Long> buildCurrentStageTeacherMap(List<ClassScheduleEntity> currentSchedules) {
        Map<String, List<Long>> teacherIdsByStage = currentSchedules.stream()
                .filter(this::isClassSchedule)
                .filter(item -> item.getStageName() != null && !item.getStageName().isBlank())
                .collect(Collectors.groupingBy(item -> item.getStageName().trim(),
                        Collectors.mapping(ClassScheduleEntity::getTeacherId, Collectors.toList())));

        Map<String, Long> result = new HashMap<>();
        for (Map.Entry<String, List<Long>> entry : teacherIdsByStage.entrySet()) {
            Set<Long> teacherIds = entry.getValue().stream().filter(Objects::nonNull).collect(Collectors.toSet());
            // 只有整个阶段每天都有老师，且所有天都是同一个老师，才认为这是“统一分配”状态。
            if (teacherIds.size() == 1 && entry.getValue().stream().noneMatch(Objects::isNull)) {
                result.put(entry.getKey(), teacherIds.iterator().next());
            }
        }
        return result;
    }

    private Map<String, Boolean> buildStageSplitMap(List<ClassScheduleEntity> currentSchedules) {
        Map<String, List<Long>> teacherIdsByStage = currentSchedules.stream()
                .filter(this::isClassSchedule)
                .filter(item -> item.getStageName() != null && !item.getStageName().isBlank())
                .collect(Collectors.groupingBy(item -> item.getStageName().trim(),
                        Collectors.mapping(ClassScheduleEntity::getTeacherId, Collectors.toList())));

        Map<String, Boolean> result = new HashMap<>();
        for (Map.Entry<String, List<Long>> entry : teacherIdsByStage.entrySet()) {
            Set<Long> teacherIds = entry.getValue().stream().filter(Objects::nonNull).collect(Collectors.toSet());
            boolean hasNull = entry.getValue().stream().anyMatch(Objects::isNull);
            // “已拆分授课”定义为：阶段内全部有老师，但不止一个老师。
            result.put(entry.getKey(), !hasNull && teacherIds.size() > 1);
        }
        return result;
    }

    private Map<String, String> buildStageTeacherSummaryMap(List<ClassScheduleEntity> currentSchedules,
                                                            Map<Long, String> userNameMap) {
        Map<String, List<Long>> teacherIdsByStage = currentSchedules.stream()
                .filter(this::isClassSchedule)
                .filter(item -> item.getStageName() != null && !item.getStageName().isBlank())
                .collect(Collectors.groupingBy(item -> item.getStageName().trim(),
                        Collectors.mapping(ClassScheduleEntity::getTeacherId, Collectors.toList())));

        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, List<Long>> entry : teacherIdsByStage.entrySet()) {
            Set<Long> teacherIds = entry.getValue().stream().filter(Objects::nonNull).collect(Collectors.toSet());
            boolean hasNull = entry.getValue().stream().anyMatch(Objects::isNull);
            if (!hasNull && teacherIds.size() > 1) {
                // 给前端一段可直接展示的摘要，减少前端再次拼接老师名的复杂度。
                String summary = teacherIds.stream()
                        .map(userNameMap::get)
                        .filter(Objects::nonNull)
                        .sorted()
                        .collect(Collectors.joining("、"));
                result.put(entry.getKey(), summary.isBlank() ? "已拆分授课" : "已拆分授课（" + summary + "）");
            }
        }
        return result;
    }

    private Map<Long, Map<String, StageWindow>> buildStageWindowMapByClassId(Map<String, StageWindow> currentStageMap,
                                                                              List<ClassScheduleEntity> assignedSchedules,
                                                                              Long currentClassId) {
        Set<Long> classIds = assignedSchedules.stream()
                .map(ClassScheduleEntity::getClassId)
                .filter(Objects::nonNull)
                .filter(id -> !Objects.equals(id, currentClassId))
                .collect(Collectors.toSet());
        Map<Long, Map<String, StageWindow>> result = new HashMap<>();
        if (!classIds.isEmpty()) {
            // 这里只拉“已经分配过老师的其他班级”，避免把全部班级课表都扫进来，减少不必要的装配开销。
            List<ClassScheduleEntity> allSchedules = classScheduleService.listByClassIds(new ArrayList<>(classIds));
            Map<Long, List<ClassScheduleEntity>> grouped = allSchedules.stream()
                    .collect(Collectors.groupingBy(ClassScheduleEntity::getClassId));
            for (Map.Entry<Long, List<ClassScheduleEntity>> entry : grouped.entrySet()) {
                result.put(entry.getKey(), buildStageWindowMap(entry.getKey(), entry.getValue()));
            }
        }
        result.put(currentClassId, currentStageMap);
        return result;
    }

    private List<StageTeacherOptionRes> buildAvailableTeacherOptions(Long classId,
                                                                     StageWindow currentWindow,
                                                                     Long currentTeacherId,
                                                                     List<SysUserEntity> enabledUsers,
                                                                     List<ClassScheduleEntity> assignedSchedules,
                                                                     Map<Long, Map<String, StageWindow>> allStageMaps) {
        List<StageTeacherOptionRes> result = new ArrayList<>();
        for (SysUserEntity user : enabledUsers) {
            // 当前已选老师需要保留在下拉中，否则前端无法显示当前值，也无法在不变更的情况下直接保存。
            if (!Objects.equals(currentTeacherId, user.getId())
                    && isTeacherOccupied(classId, currentWindow, user.getId(), assignedSchedules, allStageMaps)) {
                continue;
            }
            StageTeacherOptionRes item = new StageTeacherOptionRes();
            item.setTeacherId(user.getId());
            item.setTeacherName(resolveTeacherName(user));
            result.add(item);
        }
        result.sort(Comparator.comparing(StageTeacherOptionRes::getTeacherName));
        return result;
    }

    private List<StageTeacherOptionRes> buildAvailableTeachersByDate(Long classId,
                                                                     LocalDate targetDate,
                                                                     List<SysUserEntity> enabledUsers,
                                                                     List<ClassScheduleEntity> assignedSchedules) {
        List<StageTeacherOptionRes> result = new ArrayList<>();
        for (SysUserEntity user : enabledUsers) {
            // 单天维度的可选老师规则比阶段维度更简单：只看目标日期当天有没有被其他班级占用。
            if (isTeacherOccupiedOnDate(classId, targetDate, user.getId(), assignedSchedules)) {
                continue;
            }
            StageTeacherOptionRes item = new StageTeacherOptionRes();
            item.setTeacherId(user.getId());
            item.setTeacherName(resolveTeacherName(user));
            result.add(item);
        }
        result.sort(Comparator.comparing(StageTeacherOptionRes::getTeacherName));
        return result;
    }

    private boolean isTeacherOccupied(Long currentClassId,
                                      StageWindow currentWindow,
                                      Long teacherId,
                                      List<ClassScheduleEntity> assignedSchedules,
                                      Map<Long, Map<String, StageWindow>> allStageMaps) {
        for (ClassScheduleEntity schedule : assignedSchedules) {
            if (!Objects.equals(teacherId, schedule.getTeacherId())) {
                continue;
            }
            if (!isClassSchedule(schedule) || schedule.getStageName() == null || schedule.getStageName().isBlank()) {
                continue;
            }
            if (Objects.equals(currentClassId, schedule.getClassId())
                    && Objects.equals(currentWindow.getStageName(), schedule.getStageName().trim())) {
                // 当前班级当前阶段本身已经分配给这个老师时，不算冲突。
                continue;
            }
            StageWindow otherWindow = allStageMaps.getOrDefault(schedule.getClassId(), Map.of()).get(schedule.getStageName().trim());
            if (otherWindow == null) {
                continue;
            }
            if (currentWindow.overlaps(otherWindow)) {
                return true;
            }
        }
        return false;
    }

    private boolean isTeacherOccupiedOnDate(Long currentClassId,
                                            LocalDate targetDate,
                                            Long teacherId,
                                            List<ClassScheduleEntity> assignedSchedules) {
        // 当前班级允许同一天再次选择同一个老师，拦截的只是“其他班级”占用。
        return assignedSchedules.stream()
                .filter(this::isClassSchedule)
                .filter(item -> Objects.equals(teacherId, item.getTeacherId()))
                .filter(item -> Objects.equals(targetDate, toLocalDate(item.getScheduleDate())))
                .anyMatch(item -> !Objects.equals(currentClassId, item.getClassId()));
    }

    private String resolveTeacherName(SysUserEntity user) {
        if (user.getRealName() != null && !user.getRealName().isBlank()) {
            return user.getRealName();
        }
        if (user.getNickname() != null && !user.getNickname().isBlank()) {
            return user.getNickname();
        }
        return user.getUsername();
    }

    private List<ClassScheduleEntity> getSortedSchedules(Long classId) {
        List<ClassScheduleEntity> schedules = classScheduleService.listByClassId(classId);
        if (schedules.isEmpty()) {
            throw new BusinessException("当前班级还没有生成课表");
        }
        // 课表后续很多判断都依赖时间顺序，统一在这里排序，避免各处重复 sort。
        schedules.sort(Comparator.comparing(ClassScheduleEntity::getScheduleDate));
        return schedules;
    }

    private Map<LocalDate, ClassScheduleEntity> toScheduleMap(List<ClassScheduleEntity> schedules) {
        Map<LocalDate, ClassScheduleEntity> scheduleMap = new HashMap<>();
        schedules.forEach(item -> scheduleMap.put(toLocalDate(item.getScheduleDate()), item));
        return scheduleMap;
    }

    private ClassScheduleEntity requireSchedule(Map<LocalDate, ClassScheduleEntity> scheduleMap,
                                                LocalDate targetDate,
                                                String message) {
        ClassScheduleEntity schedule = scheduleMap.get(targetDate);
        if (schedule == null) {
            throw new BusinessException(message);
        }
        return schedule;
    }

    private ClassScheduleEntity requireClassSchedule(Long classId, LocalDate targetDate, String message) {
        // 单天变更内容 / 老师只允许作用于正式上课记录，自习和休息日不进入这条路径。
        return classScheduleService.listByClassId(classId).stream()
                .filter(this::isClassSchedule)
                .filter(item -> Objects.equals(targetDate, toLocalDate(item.getScheduleDate())))
                .findFirst()
                .orElseThrow(() -> new BusinessException(message));
    }

    private List<SysUserEntity> listEnabledTeachers() {
        return listEnabledTeachers(null);
    }

    private List<SysUserEntity> listEnabledTeachers(String keyword) {
        return sysUserMapper.selectPage(keyword, 1, TEACHER_ROLE_CODE);
    }

    private List<TeacherScheduleDateRes> buildTeacherDateHeaders(LocalDate startDate, LocalDate endDate) {
        List<TeacherScheduleDateRes> headers = new ArrayList<>();
        LocalDate cursor = startDate;
        while (!cursor.isAfter(endDate)) {
            TeacherScheduleDateRes item = new TeacherScheduleDateRes();
            item.setDate(cursor.format(DATE_FORMATTER));
            item.setDayLabel(String.format("%02d-%02d", cursor.getMonthValue(), cursor.getDayOfMonth()));
            item.setWeekdayLabel(resolveWeekdayLabel(cursor.getDayOfWeek()));
            headers.add(item);
            cursor = cursor.plusDays(1);
        }
        return headers;
    }

    private TeacherScheduleTeacherRowRes buildTeacherRow(SysUserEntity teacher,
                                                         List<TeacherScheduleRecordRes> records,
                                                         LocalDate startDate) {
        TeacherScheduleTeacherRowRes row = new TeacherScheduleTeacherRowRes();
        row.setTeacherId(teacher.getId());
        row.setTeacherName(resolveTeacherDisplayName(teacher));
        row.setBlocks(buildTeacherBlocks(records, startDate));
        return row;
    }

    private List<TeacherScheduleBlockRes> buildTeacherBlocks(List<TeacherScheduleRecordRes> records, LocalDate startDate) {
        List<TeacherScheduleRecordRes> sortedRecords = records.stream()
                .sorted(Comparator.comparing(TeacherScheduleRecordRes::getScheduleDate))
                .toList();
        List<TeacherScheduleBlockRes> blocks = new ArrayList<>();
        TeacherScheduleBlockRes currentBlock = null;
        LocalDate previousDate = null;
        for (TeacherScheduleRecordRes record : sortedRecords) {
            LocalDate scheduleDate = toLocalDate(record.getScheduleDate());
            if (currentBlock == null
                    || !Objects.equals(currentBlock.getClassId(), record.getClassId())
                    || !Objects.equals(currentBlock.getStageName(), record.getStageName())
                    || previousDate == null
                    || !scheduleDate.equals(previousDate.plusDays(1))) {
                currentBlock = new TeacherScheduleBlockRes();
                currentBlock.setStartDate(scheduleDate.format(DATE_FORMATTER));
                currentBlock.setEndDate(scheduleDate.format(DATE_FORMATTER));
                currentBlock.setStartIndex((int) ChronoUnit.DAYS.between(startDate, scheduleDate));
                currentBlock.setSpanDays(1);
                currentBlock.setClassId(record.getClassId());
                currentBlock.setClassTerm(record.getClassTerm());
                currentBlock.setClassLabel(resolveClassLabel(record.getClassTerm(), record.getClassId()));
                currentBlock.setStageName(record.getStageName());
                blocks.add(currentBlock);
            } else {
                currentBlock.setEndDate(scheduleDate.format(DATE_FORMATTER));
                currentBlock.setSpanDays(currentBlock.getSpanDays() + 1);
            }
            previousDate = scheduleDate;
        }
        return blocks;
    }

    private TeacherScheduleIdleTeacherRes buildIdleTeacher(SysUserEntity teacher) {
        TeacherScheduleIdleTeacherRes item = new TeacherScheduleIdleTeacherRes();
        item.setTeacherId(teacher.getId());
        item.setTeacherName(resolveTeacherDisplayName(teacher));
        return item;
    }

    private String resolveTeacherDisplayName(SysUserEntity teacher) {
        if (teacher.getRealName() != null && !teacher.getRealName().isBlank()) {
            return teacher.getRealName();
        }
        if (teacher.getNickname() != null && !teacher.getNickname().isBlank()) {
            return teacher.getNickname();
        }
        return teacher.getUsername();
    }

    private String resolveClassLabel(Integer classTerm, Long classId) {
        if (classTerm != null) {
            return "第" + classTerm + "期";
        }
        return "班级" + classId;
    }

    private String resolveWeekdayLabel(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> "周一";
            case TUESDAY -> "周二";
            case WEDNESDAY -> "周三";
            case THURSDAY -> "周四";
            case FRIDAY -> "周五";
            case SATURDAY -> "周六";
            case SUNDAY -> "周日";
        };
    }

    private static class StageWindow {
        private final Long classId;
        private final String stageName;
        private LocalDate startDate;
        private LocalDate endDate;
        private int dayCount;

        private StageWindow(Long classId, String stageName, LocalDate startDate, LocalDate endDate) {
            this.classId = classId;
            this.stageName = stageName;
            this.startDate = startDate;
            this.endDate = endDate;
            this.dayCount = 0;
        }

        private void include(LocalDate date) {
            if (date.isBefore(startDate)) {
                startDate = date;
            }
            if (date.isAfter(endDate)) {
                endDate = date;
            }
            dayCount += 1;
        }

        private boolean overlaps(StageWindow other) {
            return !endDate.isBefore(other.startDate) && !other.endDate.isBefore(startDate);
        }

        public Long getClassId() {
            return classId;
        }

        public String getStageName() {
            return stageName;
        }

        public LocalDate getStartDate() {
            return startDate;
        }

        public LocalDate getEndDate() {
            return endDate;
        }

        public int getDayCount() {
            return dayCount;
        }
    }

    private ClassScheduleEntity copyRecord(ClassScheduleEntity source, Date now) {
        ClassScheduleEntity record = new ClassScheduleEntity();
        record.setClassId(source.getClassId());
        record.setCourseId(source.getCourseId());
        record.setCourseDetailId(source.getCourseDetailId());
        record.setCourseDayNum(source.getCourseDayNum());
        record.setScheduleDate(source.getScheduleDate());
        record.setScheduleType(source.getScheduleType());
        record.setCourseContent(source.getCourseContent());
        record.setStageName(source.getStageName());
        record.setTeacherId(source.getTeacherId());
        record.setTeacherName(source.getTeacherName());
        record.setCreatedAt(now);
        record.setUpdatedAt(now);
        return record;
    }

    private ClassScheduleEntity cloneTeachingRecord(ClassScheduleEntity source, Date now) {
        ClassScheduleEntity record = new ClassScheduleEntity();
        record.setCourseDetailId(source.getCourseDetailId());
        record.setCourseDayNum(source.getCourseDayNum());
        record.setScheduleType(ClassScheduleTypeEnum.CLASS.getCode());
        record.setCourseContent(source.getCourseContent());
        record.setStageName(source.getStageName());
        record.setTeacherId(source.getTeacherId());
        record.setTeacherName(source.getTeacherName());
        record.setCreatedAt(now);
        record.setUpdatedAt(now);
        return record;
    }

    private LocalDate toLocalDate(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private Date toDate(LocalDate date) {
        return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private boolean isSelfStudyDay(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.THURSDAY;
    }

    private boolean isFixedDay(LocalDate date) {
        HolidayInfoRes holidayInfo = holidaySupport.getMonthHolidayInfo(date.getYear(), date.getMonthValue()).get(date.toString());
        return isRestDay(date, holidayInfo) || isSelfStudyDay(date);
    }

    private boolean isRestDay(LocalDate date, HolidayInfoRes holidayInfo) {
        // 仅法定节假日休息；非节假日的周六正常上课，周日继续休息。
        if (holidayInfo != null && Boolean.TRUE.equals(holidayInfo.getHoliday())) {
            return true;
        }
        return date.getDayOfWeek() == DayOfWeek.SUNDAY;
    }

    private Map<String, HolidayInfoRes> getHolidayInfoBetween(LocalDate start, LocalDate end) {
        Map<String, HolidayInfoRes> holidayMap = new HashMap<>();
        YearMonth current = YearMonth.from(start);
        YearMonth last = YearMonth.from(end);
        while (!current.isAfter(last)) {
            holidayMap.putAll(holidaySupport.getMonthHolidayInfo(current.getYear(), current.getMonthValue()));
            current = current.plusMonths(1);
        }
        return holidayMap;
    }

    private String resolveHolidayName(LocalDate date, HolidayInfoRes holiday) {
        if (holiday != null && holiday.getName() != null && !holiday.getName().isBlank()) {
            return holiday.getName();
        }
        if (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return "周日";
        }
        return "休息";
    }

    private boolean isClassSchedule(ClassScheduleEntity schedule) {
        return schedule != null && Objects.equals(schedule.getScheduleType(), ClassScheduleTypeEnum.CLASS.getCode());
    }

    private boolean shouldShiftTeaching(ClassScheduleEntity currentRecord, ClassScheduleTypeEnum targetType) {
        return isClassSchedule(currentRecord);
    }

    private void validateArrangeRequest(ClassScheduleTypeEnum scheduleType, ClassScheduleArrangeReq request) {
        if (scheduleType.isClassType()) {
            if (request.getStageName() == null || request.getStageName().isBlank()) {
                throw new BusinessException("加课时请选择课程阶段");
            }
            if (request.getCourseContent() == null || request.getCourseContent().isBlank()) {
                throw new BusinessException("加课时请输入课程内容");
            }
            if (request.getTeacherId() == null) {
                throw new BusinessException("加课时请选择授课老师");
            }
        }
    }

    private void validateArrangeTeacher(Long classId,
                                        LocalDate targetDate,
                                        ClassScheduleTypeEnum scheduleType,
                                        Long teacherId) {
        if (!scheduleType.isClassType()) {
            // 只有正式上课需要老师，自习/休息不做老师校验。
            return;
        }
        List<SysUserEntity> enabledUsers = listEnabledTeachers();
        boolean exists = enabledUsers.stream().anyMatch(item -> Objects.equals(item.getId(), teacherId));
        if (!exists) {
            throw new BusinessException("所选授课老师不存在或已停用");
        }
        List<ClassScheduleEntity> assignedSchedules = classScheduleService.listAssignedTeacherSchedules();
        if (isTeacherOccupiedOnDate(classId, targetDate, teacherId, assignedSchedules)) {
            throw new BusinessException("所选授课老师当天在其他班级已有课程安排");
        }
    }

    private LocalDate parseDate(String scheduleDate) {
        try {
            return LocalDate.parse(scheduleDate);
        } catch (DateTimeParseException ex) {
            throw new BusinessException("日期格式不正确");
        }
    }
}

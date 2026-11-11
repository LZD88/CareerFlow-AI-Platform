import { useEffect, useMemo, useState } from 'react';
import { LeftOutlined, RightOutlined } from '@ant-design/icons';
import { Button, DatePicker, Empty, Form, Input, Modal, Popconfirm, Select, Space, Spin, Tag, Typography, message } from 'antd';
import dayjs from 'dayjs';
import { useNavigate, useSearchParams } from 'react-router-dom';
import NoPermission from '../../components/NoPermission';
import PageContainer from '../../components/PageContainer';
import {
  arrangeClassSchedule,
  assignClassStageTeachers,
  changeClassScheduleContent,
  changeClassScheduleTeacher,
  deleteClassSchedule,
  getArrangeTeacherOptions,
  getClassScheduleCalendar,
  getClassStageTeachers
} from '../../api/class-schedules';
import { getClassPage } from '../../api/classes';
import { getCourseDetailList } from '../../api/course-details';
import { CLASS_SCHEDULE_LABELS, CLASS_SCHEDULE_TYPES, type ClassScheduleType } from '../../constants/class-schedule';
import type {
  AssignStageTeacherReq,
  ClassEntity,
  ClassScheduleArrangeReq,
  ClassScheduleCalendarDay,
  ClassScheduleCalendarRes,
  ClassScheduleChangeContentReq,
  ClassScheduleChangeTeacherReq,
  ClassStageTeacher,
  CourseDetailEntity,
  StageTeacherOption
} from '../../types/system';
import { hasPermission } from '../../utils/permission';

const weekLabels = ['一', '二', '三', '四', '五', '六', '日'];

const scheduleLegend = [
  { label: CLASS_SCHEDULE_LABELS[CLASS_SCHEDULE_TYPES.CLASS], type: CLASS_SCHEDULE_TYPES.CLASS },
  { label: CLASS_SCHEDULE_LABELS[CLASS_SCHEDULE_TYPES.SELF_STUDY], type: CLASS_SCHEDULE_TYPES.SELF_STUDY },
  { label: CLASS_SCHEDULE_LABELS[CLASS_SCHEDULE_TYPES.REST], type: CLASS_SCHEDULE_TYPES.REST }
];

export default function ClassSchedulePage() {
  const canView = hasPermission('menu:teaching:schedule');
  const canArrange = hasPermission('button:schedule:arrange');
  const canAssignTeacher = hasPermission('button:schedule:assign-teacher');
  const [arrangeForm] = Form.useForm<ClassScheduleArrangeFormValues>();
  const [assignTeacherForm] = Form.useForm<AssignTeacherFormValues>();
  const [changeContentForm] = Form.useForm<ChangeContentFormValues>();
  const [changeTeacherForm] = Form.useForm<ChangeTeacherFormValues>();
  const [searchParams, setSearchParams] = useSearchParams();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [classLoading, setClassLoading] = useState(false);
  const [arrangeLoading, setArrangeLoading] = useState(false);
  const [deleteLoading, setDeleteLoading] = useState(false);
  const [assignTeacherLoading, setAssignTeacherLoading] = useState(false);
  const [changeContentLoading, setChangeContentLoading] = useState(false);
  const [changeTeacherLoading, setChangeTeacherLoading] = useState(false);
  const [classOptions, setClassOptions] = useState<ClassEntity[]>([]);
  const [courseStages, setCourseStages] = useState<string[]>([]);
  const [arrangeTeacherOptions, setArrangeTeacherOptions] = useState<StageTeacherOption[]>([]);
  const [stageTeachers, setStageTeachers] = useState<ClassStageTeacher[]>([]);
  const [calendar, setCalendar] = useState<ClassScheduleCalendarRes>();
  const [selectedClassId, setSelectedClassId] = useState<number>();
  const [selectedDate, setSelectedDate] = useState<string>();
  const [detailOpen, setDetailOpen] = useState(false);
  const [arrangeOpen, setArrangeOpen] = useState(false);
  const [assignTeacherOpen, setAssignTeacherOpen] = useState(false);
  const [changeContentOpen, setChangeContentOpen] = useState(false);
  const [changeTeacherOpen, setChangeTeacherOpen] = useState(false);
  const [currentMonth, setCurrentMonth] = useState(() => {
    const now = dayjs();
    return { year: now.year(), month: now.month() + 1 };
  });
  const selectedArrangeType = Form.useWatch('scheduleType', arrangeForm);
  const selectedArrangeDate = Form.useWatch('scheduleDate', arrangeForm);

  const selectedClass = useMemo(
    () => classOptions.find((item) => item.id === selectedClassId),
    [classOptions, selectedClassId]
  );

  const weeks = useMemo(() => {
    const days = calendar?.days ?? [];
    const result: ClassScheduleCalendarDay[][] = [];
    // 后端返回的是平铺的日历格子，这里按每 7 天切成一周，方便渲染月视图。
    for (let i = 0; i < days.length; i += 7) {
      result.push(days.slice(i, i + 7));
    }
    return result;
  }, [calendar]);

  const selectedDay = useMemo(() => {
    if (!selectedDate) {
      return calendar?.days.find((item) => item.scheduleType === CLASS_SCHEDULE_TYPES.CLASS) ?? calendar?.days.find((item) => item.currentMonth);
    }
    return calendar?.days.find((item) => item.date === selectedDate);
  }, [calendar, selectedDate]);

  const onOpenDayDetail = (date: string) => {
    setSelectedDate(date);
    setDetailOpen(true);
  };

  const openArrangeModal = () => {
    const targetDate =
      selectedDate
      ?? calendar?.days.find((item) => item.currentMonth)?.date
      ?? dayjs().format('YYYY-MM-DD');
    arrangeForm.resetFields();
    arrangeForm.setFieldsValue({
      scheduleDate: dayjs(targetDate),
      scheduleType: CLASS_SCHEDULE_TYPES.CLASS
    });
    setArrangeOpen(true);
  };

  const openAssignTeacherModal = async () => {
    if (!selectedClassId) {
      return;
    }
    setAssignTeacherLoading(true);
    try {
      const result = await getClassStageTeachers(selectedClassId);
      setStageTeachers(result);
      assignTeacherForm.setFieldsValue({
        teachers: result.reduce<Record<string, number | undefined>>((acc, item) => {
          acc[item.stageName] = item.splitAssigned ? undefined : item.teacherId;
          return acc;
        }, {})
      });
      setAssignTeacherOpen(true);
    } finally {
      setAssignTeacherLoading(false);
    }
  };

  const openChangeTeacherModal = async () => {
    if (!selectedClassId || !selectedDay?.date) {
      return;
    }
    setChangeTeacherLoading(true);
    try {
      const result = await getArrangeTeacherOptions(selectedClassId, selectedDay.date);
      const currentTeacherOption = selectedDay.teacherId && selectedDay.teacherName
        ? [{ teacherId: selectedDay.teacherId, teacherName: selectedDay.teacherName }]
        : [];
      const merged = [...currentTeacherOption, ...result].filter(
        (item, index, arr) => arr.findIndex((candidate) => candidate.teacherId === item.teacherId) === index
      );
      setArrangeTeacherOptions(merged);
      changeTeacherForm.setFieldsValue({ teacherId: selectedDay.teacherId });
      setChangeTeacherOpen(true);
    } finally {
      setChangeTeacherLoading(false);
    }
  };

  const openChangeContentModal = () => {
    if (!selectedDay) {
      return;
    }
    changeContentForm.setFieldsValue({
      courseContent: selectedDay.courseContent
    });
    setChangeContentOpen(true);
  };

  const fetchClasses = async () => {
    setClassLoading(true);
    try {
      const result = await getClassPage({ pageNum: 1, pageSize: 1000 });
      setClassOptions(result.records);

      const queryClassId = Number(searchParams.get('classId'));
      const initialClassId = queryClassId || result.records[0]?.id;
      if (initialClassId) {
        setSelectedClassId(initialClassId);
      }
    } finally {
      setClassLoading(false);
    }
  };

  const fetchCalendar = async (classId: number, year: number, month: number) => {
    setLoading(true);
    try {
      const result = await getClassScheduleCalendar(classId, year, month);
      setCalendar(result);
      setSelectedDate((current) => {
        if (current && result.days.some((item) => item.date === current)) {
          return current;
        }
        return result.days.find((item) => item.scheduleType === CLASS_SCHEDULE_TYPES.CLASS)?.date ?? result.days.find((item) => item.currentMonth)?.date;
      });
    } catch {
      setCalendar(undefined);
    } finally {
      setLoading(false);
    }
  };

  const fetchCourseStages = async (courseId?: number) => {
    if (!courseId) {
      setCourseStages([]);
      return;
    }
    const result = await getCourseDetailList(courseId);
    setCourseStages(extractStageOptions(result));
  };

  useEffect(() => {
    if (!canView) {
      return;
    }
    fetchClasses();
  }, [canView]);

  useEffect(() => {
    if (!canView || !selectedClassId) {
      return;
    }
    setSearchParams({ classId: String(selectedClassId) });
    fetchCalendar(selectedClassId, currentMonth.year, currentMonth.month);
  }, [canView, selectedClassId, currentMonth.year, currentMonth.month]);

  useEffect(() => {
    fetchCourseStages(selectedClass?.courseId);
  }, [selectedClass?.courseId]);

  useEffect(() => {
    const fetchArrangeTeacherOptions = async () => {
      if (!arrangeOpen || !selectedClassId || selectedArrangeType !== CLASS_SCHEDULE_TYPES.CLASS || !selectedArrangeDate) {
        setArrangeTeacherOptions([]);
        return;
      }
      const result = await getArrangeTeacherOptions(selectedClassId, dayjs(selectedArrangeDate).format('YYYY-MM-DD'));
      setArrangeTeacherOptions(result);
      const currentTeacherId = arrangeForm.getFieldValue('teacherId');
      if (currentTeacherId && !result.some((item) => item.teacherId === currentTeacherId)) {
        arrangeForm.setFieldValue('teacherId', undefined);
      }
    };
    fetchArrangeTeacherOptions();
  }, [arrangeForm, arrangeOpen, selectedArrangeDate, selectedArrangeType, selectedClassId]);

  if (!canView) {
    return <NoPermission />;
  }

  const submitArrange = async () => {
    if (!selectedClassId) {
      return;
    }
    const values = await arrangeForm.validateFields();
    const payload: ClassScheduleArrangeReq = {
      scheduleDate: dayjs(values.scheduleDate).format('YYYY-MM-DD'),
      scheduleType: values.scheduleType,
      stageName: values.stageName,
      courseContent: values.courseContent?.trim(),
      teacherId: values.teacherId
    };

    setArrangeLoading(true);
    try {
      await arrangeClassSchedule(selectedClassId, payload);
      message.success('课表调整成功');
      setArrangeOpen(false);
      await fetchCalendar(selectedClassId, currentMonth.year, currentMonth.month);
      fetchClasses();
    } finally {
      setArrangeLoading(false);
    }
  };

  const submitDelete = async () => {
    if (!selectedClassId || !selectedDay) {
      return;
    }
    setDeleteLoading(true);
    try {
      await deleteClassSchedule(selectedClassId, {
        scheduleDate: selectedDay.date
      });
      message.success('课程删除成功');
      setDetailOpen(false);
      await fetchCalendar(selectedClassId, currentMonth.year, currentMonth.month);
      fetchClasses();
    } finally {
      setDeleteLoading(false);
    }
  };

  const submitAssignTeachers = async () => {
    if (!selectedClassId) {
      return;
    }
    const values = await assignTeacherForm.validateFields();
    const payload: AssignStageTeacherReq = {
      items: stageTeachers.map((item) => ({
        stageName: item.stageName,
        teacherId: values.teachers?.[item.stageName]
      }))
    };

    setAssignTeacherLoading(true);
    try {
      await assignClassStageTeachers(selectedClassId, payload);
      message.success('阶段老师分配成功');
      setAssignTeacherOpen(false);
    } finally {
      setAssignTeacherLoading(false);
    }
  };

  const submitChangeTeacher = async () => {
    if (!selectedClassId || !selectedDay?.date) {
      return;
    }
    const values = await changeTeacherForm.validateFields();
    const payload: ClassScheduleChangeTeacherReq = {
      scheduleDate: selectedDay.date,
      teacherId: values.teacherId as number
    };
    setChangeTeacherLoading(true);
    try {
      await changeClassScheduleTeacher(selectedClassId, payload);
      message.success('授课老师变更成功');
      setChangeTeacherOpen(false);
      setDetailOpen(false);
      await fetchCalendar(selectedClassId, currentMonth.year, currentMonth.month);
    } finally {
      setChangeTeacherLoading(false);
    }
  };

  const submitChangeContent = async () => {
    if (!selectedClassId || !selectedDay?.date) {
      return;
    }
    const values = await changeContentForm.validateFields();
    const payload: ClassScheduleChangeContentReq = {
      scheduleDate: selectedDay.date,
      courseContent: values.courseContent.trim()
    };
    setChangeContentLoading(true);
    try {
      await changeClassScheduleContent(selectedClassId, payload);
      message.success('课程内容变更成功');
      setChangeContentOpen(false);
      setDetailOpen(false);
      await fetchCalendar(selectedClassId, currentMonth.year, currentMonth.month);
    } finally {
      setChangeContentLoading(false);
    }
  };

  return (
    <PageContainer
      title="教学排课"
      description="按培训班级查看教学进度，安排课程、师资、休息和自习。"
    >
      <div className="schedule-toolbar">
        <div className="schedule-toolbar-main">
          <div className="schedule-class-picker">
            <Typography.Text type="secondary">选择班级：</Typography.Text>
            <Select
              value={selectedClassId}
              placeholder="请选择班级"
              loading={classLoading}
              style={{ width: 280 }}
              options={classOptions.map((item) => ({
                label: `第${item.classTerm}期 · ${item.courseName || '未关联课程'}`,
                value: item.id
              }))}
              onChange={(value) => setSelectedClassId(value)}
            />
          </div>

          {selectedClass ? (
            <div className="schedule-summary">
              <Tag color={selectedClass.hasSchedule ? 'blue' : 'default'} bordered={false}>
                {selectedClass.hasSchedule ? '已生成课表' : '未生成课表'}
              </Tag>
              <Typography.Text type="secondary">
                班主任：{selectedClass.headTeacherName || '-'}
              </Typography.Text>
              <Typography.Text type="secondary">
                课程：{selectedClass.courseName || '-'}
              </Typography.Text>
            </div>
          ) : null}
        </div>

        <Space>
          {canAssignTeacher ? (
            <Button disabled={!selectedClassId || !selectedClass?.hasSchedule} onClick={openAssignTeacherModal}>
              分配老师
            </Button>
          ) : null}
          {canArrange ? (
            <Button type="primary" ghost disabled={!selectedClassId || !selectedClass?.hasSchedule} onClick={openArrangeModal}>
              新增课程
            </Button>
          ) : null}
          <Button
            icon={<LeftOutlined />}
            onClick={() => {
              const target = dayjs(`${currentMonth.year}-${currentMonth.month}-01`).subtract(1, 'month');
              setCurrentMonth({ year: target.year(), month: target.month() + 1 });
            }}
          >
            上月
          </Button>
          <Select
            value={currentMonth.year}
            style={{ width: 120 }}
            options={Array.from({ length: 7 }).map((_, index) => {
              const year = dayjs().year() - 3 + index;
              return { label: `${year}年`, value: year };
            })}
            onChange={(value) => setCurrentMonth((prev) => ({ ...prev, year: value }))}
          />
          <Select
            value={currentMonth.month}
            style={{ width: 100 }}
            options={Array.from({ length: 12 }).map((_, index) => ({
              label: `${index + 1}月`,
              value: index + 1
            }))}
            onChange={(value) => setCurrentMonth((prev) => ({ ...prev, month: value }))}
          />
          <Button
            icon={<RightOutlined />}
            onClick={() => {
              const target = dayjs(`${currentMonth.year}-${currentMonth.month}-01`).add(1, 'month');
              setCurrentMonth({ year: target.year(), month: target.month() + 1 });
            }}
          >
            下月
          </Button>
        </Space>
      </div>

      {!selectedClassId ? (
        <Empty description="暂无可用班级" />
      ) : selectedClass && !selectedClass.hasSchedule ? (
        <Empty
          image={Empty.PRESENTED_IMAGE_SIMPLE}
          description={`第${selectedClass.classTerm}期班级还没有生成课表，请先回班级管理执行“生成课表”。`}
        >
          <Button type="primary" onClick={() => navigate('/classes')}>
            去班级管理
          </Button>
        </Empty>
      ) : (
        <Spin spinning={loading}>
          <div className="schedule-calendar-shell">
            <div className="schedule-week-header">
              {weekLabels.map((item) => (
                <div key={item} className="schedule-week-cell">
                  {item}
                </div>
              ))}
            </div>

            <div className="schedule-grid">
              {weeks.map((week) =>
                week.map((day) => (
                  <div
                    key={day.date}
                    // 不同的 scheduleType 用不同边框和标签颜色，让上课 / 自习 / 休息一眼区分开。
                    className={`schedule-day-card ${day.currentMonth ? '' : 'is-muted'} ${getScheduleCardClass(day.scheduleType)} ${selectedDate === day.date ? 'is-active' : ''}`}
                    onClick={() => onOpenDayDetail(day.date)}
                  >
                    <div className="schedule-day-head">
                      <span className="schedule-day-number">{day.dayOfMonth}</span>
                      {day.courseDayNum ? <span className="schedule-course-index">第 {day.courseDayNum} 天</span> : null}
                    </div>
                    <div className="schedule-day-body">
                      {day.label ? <span className={`schedule-pill ${getSchedulePillClass(day.scheduleType)}`}>{day.label}</span> : null}
                      {day.holidayName ? <span className="schedule-holiday-text">{day.holidayName}</span> : null}
                      {day.stageName ? <span className="schedule-stage-text">{day.stageName}</span> : null}
                      {!day.label ? <span className="schedule-empty-text">暂无安排</span> : null}
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>

          <div className="schedule-legend">
            {scheduleLegend.map((item) => (
              <div key={item.type} className="schedule-legend-item">
                <span className={`schedule-legend-dot ${getSchedulePillClass(item.type)}`} />
                <span>{item.label}</span>
              </div>
            ))}
          </div>
        </Spin>
      )}

      <Modal
        title={null}
        open={detailOpen && !!selectedDay}
        onCancel={() => setDetailOpen(false)}
        footer={
          canArrange && selectedDay ? [
            selectedDay.scheduleType === CLASS_SCHEDULE_TYPES.CLASS ? (
              <Button key="change-content" onClick={openChangeContentModal}>
                变更内容
              </Button>
            ) : null,
            selectedDay.scheduleType === CLASS_SCHEDULE_TYPES.CLASS ? (
              <Button key="change-teacher" onClick={openChangeTeacherModal}>
                变更老师
              </Button>
            ) : null,
            <Popconfirm
              key="delete"
              title="确认删除当天课程安排吗？"
              description={selectedDay.scheduleType === CLASS_SCHEDULE_TYPES.CLASS ? '删除上课会让后续课程前移。' : '删除休息或自习不会影响后续课程。'}
              onConfirm={submitDelete}
              okButtonProps={{ loading: deleteLoading, danger: true }}
            >
              <Button danger loading={deleteLoading}>
                删除课程
              </Button>
            </Popconfirm>
          ] : null
        }
        width={640}
        destroyOnClose
        className="schedule-detail-modal"
      >
        {selectedDay ? (
          <div className="schedule-detail-modal-body">
            <div className="schedule-detail-modal-hero">
              <div className="schedule-detail-date">{dayjs(selectedDay.date).format('YYYY年MM月DD日')}</div>
              <div className="schedule-detail-weekday">{dayjs(selectedDay.date).format('dddd')}</div>
            </div>

            <div className="schedule-detail-section">
              <span className="schedule-detail-section-label">当天状态</span>
              {selectedDay.label ? (
                <span className={`schedule-pill ${getSchedulePillClass(selectedDay.scheduleType)}`}>{selectedDay.label}</span>
              ) : (
                <span className="schedule-detail-fallback">暂无安排</span>
              )}
            </div>

            <div className="schedule-detail-meta-row">
              <div className="schedule-detail-meta-card">
                <span className="schedule-detail-meta-title">课程天数</span>
                <span className="schedule-detail-meta-value">
                  {selectedDay.courseDayNum ? `第 ${selectedDay.courseDayNum} 天` : '-'}
                </span>
              </div>
              <div className="schedule-detail-meta-card">
                <span className="schedule-detail-meta-title">所属阶段</span>
                <span className="schedule-detail-meta-value">{selectedDay.stageName || '-'}</span>
              </div>
              <div className="schedule-detail-meta-card">
                <span className="schedule-detail-meta-title">授课老师</span>
                <span className="schedule-detail-meta-value">{selectedDay.teacherName || '-'}</span>
              </div>
            </div>

            <div className="schedule-detail-section">
              <span className="schedule-detail-section-label">课程内容</span>
            </div>
            <div className="schedule-detail-content-card">
              {selectedDay.courseContent || selectedDay.holidayName || '当天暂无详细安排'}
            </div>
          </div>
        ) : null}
      </Modal>

      <Modal
        title="新增加课"
        open={arrangeOpen}
        onCancel={() => setArrangeOpen(false)}
        onOk={submitArrange}
        okText="保存"
        cancelText="取消"
        confirmLoading={arrangeLoading}
        width={640}
        destroyOnClose
      >
        <Form form={arrangeForm} layout="vertical">
          <Form.Item
            label="上课日期"
            name="scheduleDate"
            rules={[{ required: true, message: '请选择上课日期' }]}
          >
            <DatePicker style={{ width: '100%' }} format="YYYY-MM-DD" />
          </Form.Item>
          <Form.Item
            label="新增课程类型"
            name="scheduleType"
            rules={[{ required: true, message: '请选择新增课程类型' }]}
          >
            <Select
              options={[
                { label: '加课', value: CLASS_SCHEDULE_TYPES.CLASS },
                { label: '休息', value: CLASS_SCHEDULE_TYPES.REST },
                { label: '自习', value: CLASS_SCHEDULE_TYPES.SELF_STUDY }
              ]}
            />
          </Form.Item>
          {selectedArrangeType === CLASS_SCHEDULE_TYPES.CLASS ? (
            <Form.Item
              label="课程阶段"
              name="stageName"
              rules={[{ required: true, message: '请选择课程阶段' }]}
            >
              <Select
                placeholder="请选择当前课程阶段"
                options={courseStages.map((item) => ({ label: item, value: item }))}
              />
            </Form.Item>
          ) : null}
          {selectedArrangeType === CLASS_SCHEDULE_TYPES.CLASS ? (
            <Form.Item
              label="授课老师"
              name="teacherId"
              rules={[{ required: true, message: '请选择授课老师' }]}
            >
              <Select
                placeholder="请选择授课老师"
                options={arrangeTeacherOptions.map((item) => ({
                  label: item.teacherName,
                  value: item.teacherId
                }))}
              />
            </Form.Item>
          ) : null}
          <Form.Item
            label="课程内容"
            name="courseContent"
            rules={
              selectedArrangeType === CLASS_SCHEDULE_TYPES.CLASS
                ? [{ required: true, message: '请输入本节课的课程内容' }]
                : undefined
            }
          >
            <Input.TextArea
              rows={5}
              placeholder={
                selectedArrangeType === CLASS_SCHEDULE_TYPES.CLASS
                  ? '请输入本节课的课程内容'
                  : '休息 / 自习不填也可以，系统会自动带默认文案'
              }
            />
          </Form.Item>
          <Typography.Text type="secondary">
            上课日新增“加课”会把当天及后续正常课程顺延；休息/自习日新增“加课”只占用当天，不顺延后续课程。授课老师下拉只展示当天在其他班级没有课的讲师，当前班当天已有课不算冲突。
          </Typography.Text>
        </Form>
      </Modal>

      <Modal
        title="变更内容"
        open={changeContentOpen}
        onCancel={() => setChangeContentOpen(false)}
        onOk={submitChangeContent}
        okText="保存"
        cancelText="取消"
        confirmLoading={changeContentLoading}
        destroyOnClose
      >
        <Form form={changeContentForm} layout="vertical">
          <Form.Item label="上课日期">
            <Input value={selectedDay ? dayjs(selectedDay.date).format('YYYY-MM-DD') : ''} disabled />
          </Form.Item>
          <Form.Item
            label="课程内容"
            name="courseContent"
            rules={[{ required: true, message: '请输入课程内容' }]}
          >
            <Input.TextArea rows={6} placeholder="请输入当天实际上课内容" />
          </Form.Item>
          <Typography.Text type="secondary">
            这里只会修改当天课表记录的课程内容，不会影响课程模板、阶段、课程序号和后续班级。
          </Typography.Text>
        </Form>
      </Modal>

      <Modal
        title="变更老师"
        open={changeTeacherOpen}
        onCancel={() => setChangeTeacherOpen(false)}
        onOk={submitChangeTeacher}
        okText="保存"
        cancelText="取消"
        confirmLoading={changeTeacherLoading}
        destroyOnClose
      >
        <Form form={changeTeacherForm} layout="vertical">
          <Form.Item label="上课日期">
            <Input value={selectedDay ? dayjs(selectedDay.date).format('YYYY-MM-DD') : ''} disabled />
          </Form.Item>
          <Form.Item
            label="授课老师"
            name="teacherId"
            rules={[{ required: true, message: '请选择授课老师' }]}
          >
            <Select
              placeholder="请选择授课老师"
              loading={changeTeacherLoading}
              options={arrangeTeacherOptions.map((item) => ({
                label: item.teacherName,
                value: item.teacherId
              }))}
            />
          </Form.Item>
          <Typography.Text type="secondary">
            下拉框只展示当天在其他班级没有课的讲师，当前老师会保留在列表里，方便直接确认或重新选择。
          </Typography.Text>
        </Form>
      </Modal>

      <Modal
        title="分配老师"
        open={assignTeacherOpen}
        onCancel={() => setAssignTeacherOpen(false)}
        onOk={submitAssignTeachers}
        okText="确定"
        cancelText="取消"
        confirmLoading={assignTeacherLoading}
        width={960}
        destroyOnClose
      >
        <Typography.Paragraph type="secondary">
          这里根据当前班级关联的课程，展示该课程包含的所有阶段，并为每个阶段选择授课老师。每个阶段的下拉框会自动过滤：在该阶段时间范围内有课的老师不展示。
        </Typography.Paragraph>
        <Form form={assignTeacherForm} layout="vertical">
          {stageTeachers.map((item) => (
            <div key={item.stageName}>
              <Form.Item
                label={`${item.stageName}（${item.dayCount}天）`}
                name={['teachers', item.stageName]}
              >
                <Select
                  allowClear
                  placeholder={item.splitAssigned ? '已拆分授课' : '请选择老师'}
                  options={item.availableTeachers.map((teacher) => ({
                    label: teacher.teacherName,
                    value: teacher.teacherId
                  }))}
                />
              </Form.Item>
              {item.splitAssigned ? (
                <Typography.Text type="secondary" style={{ display: 'block', marginTop: -16, marginBottom: 24 }}>
                  {item.teacherSummary || '本阶段存在单天变更老师'}
                </Typography.Text>
              ) : null}
            </div>
          ))}
        </Form>
      </Modal>
    </PageContainer>
  );
}

interface ClassScheduleArrangeFormValues {
  scheduleDate: dayjs.Dayjs;
  scheduleType: ClassScheduleType;
  stageName?: string;
  courseContent?: string;
  teacherId?: number;
}

interface AssignTeacherFormValues {
  teachers?: Record<string, number | undefined>;
}

interface ChangeContentFormValues {
  courseContent: string;
}

interface ChangeTeacherFormValues {
  teacherId?: number;
}

function extractStageOptions(details: CourseDetailEntity[]) {
  return Array.from(
    new Set(
      details
        .map((item) => item.stageName?.trim())
        .filter((item): item is string => Boolean(item))
    )
  );
}

function getScheduleCardClass(type?: ClassScheduleType) {
  if (type === CLASS_SCHEDULE_TYPES.CLASS) {
    return 'is-class';
  }
  if (type === CLASS_SCHEDULE_TYPES.SELF_STUDY) {
    return 'is-self-study';
  }
  if (type === CLASS_SCHEDULE_TYPES.REST) {
    return 'is-rest';
  }
  return '';
}

function getSchedulePillClass(type?: ClassScheduleType) {
  if (type === CLASS_SCHEDULE_TYPES.CLASS) {
    return 'is-class';
  }
  if (type === CLASS_SCHEDULE_TYPES.SELF_STUDY) {
    return 'is-self-study';
  }
  if (type === CLASS_SCHEDULE_TYPES.REST) {
    return 'is-rest';
  }
  return '';
}

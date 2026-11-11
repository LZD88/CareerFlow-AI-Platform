import { useEffect, useState } from 'react';
import { CalendarOutlined, TeamOutlined } from '@ant-design/icons';
import { Button, DatePicker, Form, Input, InputNumber, Modal, Popconfirm, Select, Space, Table, Tag, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import { useNavigate } from 'react-router-dom';
import NoPermission from '../../components/NoPermission';
import PageContainer from '../../components/PageContainer';
import PanelTable from '../../components/PanelTable';
import { getCampusPage } from '../../api/campuses';
import { generateClassSchedule } from '../../api/class-schedules';
import { createClass, deleteClass, getClassDetail, getClassPage, updateClass } from '../../api/classes';
import { getCoursePage } from '../../api/courses';
import { getUserPage } from '../../api/users';
import type { CampusEntity, ClassCreateReq, ClassEntity, ClassQueryParams, ClassScheduleGenerateReq, ClassStatusCode, ClassUpdateReq, CourseEntity, UserEntity } from '../../types/system';
import { hasPermission } from '../../utils/permission';

interface SearchValues {
  keyword?: string;
  classStatus?: ClassStatusCode;
  campusId?: number;
  headTeacherId?: number;
  courseId?: number;
}

const classStatusOptions = [
  { label: '待开班', value: 'WAITING' },
  { label: '上课中', value: 'TEACHING' },
  { label: '已结课', value: 'FINISHED' }
] as const;

export default function ClassPage() {
  const navigate = useNavigate();
  const canView = hasPermission('menu:teaching:class');
  const canCreate = hasPermission('button:class:create');
  const canUpdate = hasPermission('button:class:update');
  const canDelete = hasPermission('button:class:delete');
  const canGenerateSchedule = hasPermission('button:class:generate-schedule');
  const canViewSchedule = hasPermission('menu:teaching:schedule');

  const [searchForm] = Form.useForm<SearchValues>();
  const [editForm] = Form.useForm<ClassCreateReq & ClassUpdateReq>();
  const [generateForm] = Form.useForm<ClassScheduleGenerateReq>();
  const [loading, setLoading] = useState(false);
  const [optionLoading, setOptionLoading] = useState(false);
  const [open, setOpen] = useState(false);
  const [generateOpen, setGenerateOpen] = useState(false);
  const [generateLoading, setGenerateLoading] = useState(false);
  const [isEdit, setIsEdit] = useState(false);
  const [currentId, setCurrentId] = useState<number>();
  const [currentGenerateClass, setCurrentGenerateClass] = useState<ClassEntity>();
  const [dataSource, setDataSource] = useState<ClassEntity[]>([]);
  const [campusOptions, setCampusOptions] = useState<CampusEntity[]>([]);
  const [teacherOptions, setTeacherOptions] = useState<UserEntity[]>([]);
  const [courseOptions, setCourseOptions] = useState<CourseEntity[]>([]);
  const [pageNum, setPageNum] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [total, setTotal] = useState(0);

  const fetchData = async () => {
    setLoading(true);
    try {
      const values = searchForm.getFieldsValue();
      const result = await getClassPage({ pageNum, pageSize, ...values } as ClassQueryParams);
      setDataSource(result.records);
      setTotal(result.total);
    } finally {
      setLoading(false);
    }
  };

  const fetchOptions = async () => {
    setOptionLoading(true);
    try {
      const [campusResult, userResult, courseResult] = await Promise.all([
        getCampusPage({ pageNum: 1, pageSize: 1000, status: 1 }),
        getUserPage({ pageNum: 1, pageSize: 1000, status: 1, roleCode: 'head_teacher' }),
        getCoursePage({ pageNum: 1, pageSize: 1000, status: 1 })
      ]);
      setCampusOptions(campusResult.records);
      setTeacherOptions(userResult.records);
      setCourseOptions(courseResult.records);
    } finally {
      setOptionLoading(false);
    }
  };

  const resetSearch = () => {
    searchForm.resetFields();
    if (pageNum !== 1) {
      setPageNum(1);
      return;
    }
    fetchData();
  };

  useEffect(() => {
    if (!canView) {
      return;
    }
    fetchData();
  }, [canView, pageNum, pageSize]);

  useEffect(() => {
    if (!canView) {
      return;
    }
    fetchOptions();
  }, [canView]);

  const onCreate = () => {
    setIsEdit(false);
    setCurrentId(undefined);
    editForm.resetFields();
    editForm.setFieldsValue({
      campusId: campusOptions[0]?.id,
      classStatus: 'WAITING',
      studentCount: 0,
      classTerm: 1
    } as Partial<ClassCreateReq>);
    setOpen(true);
  };

  const onEdit = async (record: ClassEntity) => {
    const detail = await getClassDetail(record.id);
    setIsEdit(true);
    setCurrentId(record.id);
    editForm.setFieldsValue({
      classTerm: detail.classTerm,
      campusId: detail.campusId,
      headTeacherId: detail.headTeacherId,
      classStatus: detail.classStatus,
      startTime: detail.startTime ? dayjs(detail.startTime) : undefined,
      courseId: detail.courseId,
      studentCount: detail.studentCount
    } as any);
    setOpen(true);
  };

  const onDelete = async (id: number) => {
    await deleteClass(id);
    message.success('删除成功');
    fetchData();
  };

  const onOpenGenerate = (record: ClassEntity) => {
    setCurrentGenerateClass(record);
    generateForm.resetFields();
    generateForm.setFieldsValue({
      // 默认带出班级开班时间，减少运营手动选日期的成本。
      firstCourseDate: record.startTime ? dayjs(record.startTime) : dayjs()
    } as any);
    setGenerateOpen(true);
  };

  const submitGenerate = async () => {
    if (!currentGenerateClass) {
      return;
    }
    const values = await generateForm.validateFields();
    setGenerateLoading(true);
    try {
      await generateClassSchedule(currentGenerateClass.id, {
        firstCourseDate: dayjs(values.firstCourseDate).format('YYYY-MM-DD')
      });
      message.success('课表生成成功');
      setGenerateOpen(false);
      fetchData();
      // 生成完成后直接跳去课表页，方便继续检查排课结果。
      navigate(`/schedules?classId=${currentGenerateClass.id}`);
    } finally {
      setGenerateLoading(false);
    }
  };

  const goSchedulePage = (record: ClassEntity) => {
    navigate(`/schedules?classId=${record.id}`);
  };

  const submitEdit = async () => {
    const values = await editForm.validateFields();
    const payload = {
      classTerm: values.classTerm,
      campusId: values.campusId,
      headTeacherId: values.headTeacherId,
      classStatus: values.classStatus,
      startTime: values.startTime ? dayjs(values.startTime).format('YYYY-MM-DD HH:mm:ss') : '',
      courseId: values.courseId,
      studentCount: values.studentCount
    } as ClassCreateReq;

    if (isEdit && currentId) {
      await updateClass(currentId, payload as ClassUpdateReq);
      message.success('更新成功');
    } else {
      await createClass(payload);
      message.success('创建成功');
    }
    setOpen(false);
    fetchData();
  };

  const columns: ColumnsType<ClassEntity> = [
    { title: 'ID', dataIndex: 'id', width: 80 },
    { title: '班级期数', dataIndex: 'classTerm', width: 120, render: (value) => `第 ${value} 期` },
    { title: '校区', dataIndex: 'campusName', width: 160, render: (value) => value || '-' },
    { title: '班主任', dataIndex: 'headTeacherName', width: 160, render: (value) => value || '-' },
    {
      title: '班级状态',
      dataIndex: 'classStatus',
      width: 120,
      render: (value: ClassStatusCode) => (
        <Tag color={getClassStatusColor(value)} bordered={false}>
          {getClassStatusLabel(value)}
        </Tag>
      )
    },
    {
      title: '开班时间',
      dataIndex: 'startTime',
      width: 180,
      render: (value) => (value ? dayjs(value).format('YYYY-MM-DD HH:mm:ss') : '-')
    },
    { title: '课程', dataIndex: 'courseName', width: 200, render: (value) => value || '-' },
    { title: '班级人数', dataIndex: 'studentCount', width: 120, render: (value) => `${value} 人` },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      width: 180,
      render: (value) => (value ? dayjs(value).format('YYYY-MM-DD HH:mm:ss') : '-')
    },
    {
      title: '操作',
      key: 'action',
      fixed: 'right',
      width: 260,
      render: (_, record) => (
        <Space>
          {canViewSchedule ? (
            <Button type="link" onClick={() => goSchedulePage(record)}>
              课表管理
            </Button>
          ) : null}
          {canGenerateSchedule && !record.hasSchedule ? (
            <Button type="link" onClick={() => onOpenGenerate(record)}>
              生成课表
            </Button>
          ) : null}
          {canUpdate ? (
            <Button type="link" onClick={() => onEdit(record)}>
              编辑
            </Button>
          ) : null}
          {canDelete ? (
            <Popconfirm title="确认删除该班级吗？" onConfirm={() => onDelete(record.id)}>
              <Button type="link" danger>
                删除
              </Button>
            </Popconfirm>
          ) : null}
        </Space>
      )
    }
  ];

  if (!canView) {
    return <NoPermission />;
  }

  return (
    <PageContainer
      title="培训班级"
      description="维护班级期数、班主任、班级状态、开班时间、课程和班级人数。"
      extra={canCreate ? <Button type="primary" onClick={onCreate}>新增班级</Button> : null}
    >
      <Form form={searchForm} layout="inline" className="search-form" onFinish={fetchData}>
        <Form.Item name="keyword">
          <Input placeholder="班级期数/校区/班主任/课程" allowClear style={{ width: 240 }} prefix={<TeamOutlined />} />
        </Form.Item>
        <Form.Item name="classStatus">
          <Select allowClear placeholder="班级状态" style={{ width: 150 }} options={classStatusOptions as any} />
        </Form.Item>
        <Form.Item name="campusId">
          <Select
            allowClear
            showSearch
            placeholder="校区"
            style={{ width: 180 }}
            loading={optionLoading}
            optionFilterProp="label"
            options={campusOptions.map((item) => ({
              label: item.campusName,
              value: item.id
            }))}
          />
        </Form.Item>
        <Form.Item name="headTeacherId">
          <Select
            allowClear
            showSearch
            placeholder="班主任"
            style={{ width: 200 }}
            loading={optionLoading}
            optionFilterProp="label"
            options={teacherOptions.map((item) => ({
              label: `${item.realName || item.nickname || item.username} (#${item.id})`,
              value: item.id
            }))}
          />
        </Form.Item>
        <Form.Item name="courseId">
          <Select
            allowClear
            showSearch
            placeholder="课程"
            style={{ width: 220 }}
            loading={optionLoading}
            optionFilterProp="label"
            options={courseOptions.map((item) => ({
              label: item.courseName,
              value: item.id
            }))}
          />
        </Form.Item>
        <Form.Item>
          <Space>
            <Button type="primary" htmlType="submit">
              查询
            </Button>
            <Button onClick={resetSearch}>
              重置
            </Button>
          </Space>
        </Form.Item>
      </Form>

      <PanelTable>
        <Table
          rowKey="id"
          loading={loading}
          columns={columns}
          dataSource={dataSource}
          scroll={{ x: 1600 }}
          pagination={{
            current: pageNum,
            pageSize,
            total,
            showSizeChanger: true,
            onChange: (page, size) => {
              setPageNum(page);
              setPageSize(size);
            }
          }}
        />
      </PanelTable>

      {canCreate || canUpdate ? (
        <Modal
          title={isEdit ? '编辑班级' : '新增班级'}
          open={open}
          onOk={submitEdit}
          onCancel={() => setOpen(false)}
          destroyOnClose
        >
          <Form form={editForm} layout="vertical">
            <Form.Item label="班级期数" name="classTerm" rules={[{ required: true, message: '请输入班级期数' }]}>
              <InputNumber min={1} precision={0} style={{ width: '100%' }} placeholder="例如：1" />
            </Form.Item>
            <Form.Item label="校区" name="campusId" rules={[{ required: true, message: '请选择校区' }]}>
              <Select
                showSearch
                placeholder="请选择校区"
                loading={optionLoading}
                optionFilterProp="label"
                options={campusOptions.map((item) => ({
                  label: item.campusName,
                  value: item.id
                }))}
              />
            </Form.Item>
            <Form.Item label="班主任" name="headTeacherId" rules={[{ required: true, message: '请选择班主任' }]}>
              <Select
                showSearch
                placeholder="请选择班主任"
                loading={optionLoading}
                optionFilterProp="label"
                options={teacherOptions.map((item) => ({
                  label: `${item.realName || item.nickname || item.username} (#${item.id})`,
                  value: item.id
                }))}
              />
            </Form.Item>
            <Form.Item label="班级状态" name="classStatus" rules={[{ required: true, message: '请选择班级状态' }]}>
              <Select options={classStatusOptions as any} />
            </Form.Item>
            <Form.Item label="开班时间" name="startTime" rules={[{ required: true, message: '请选择开班时间' }]}>
              <DatePicker
                showTime
                style={{ width: '100%' }}
                format="YYYY-MM-DD HH:mm:ss"
                placeholder="请选择开班时间"
                suffixIcon={<CalendarOutlined />}
              />
            </Form.Item>
            <Form.Item label="课程" name="courseId" rules={[{ required: true, message: '请选择课程' }]}>
              <Select
                showSearch
                placeholder="请选择课程"
                loading={optionLoading}
                optionFilterProp="label"
                options={courseOptions.map((item) => ({
                  label: item.courseName,
                  value: item.id
                }))}
              />
            </Form.Item>
            <Form.Item label="班级人数" name="studentCount" rules={[{ required: true, message: '请输入班级人数' }]}>
              <InputNumber min={0} precision={0} style={{ width: '100%' }} placeholder="请输入班级人数" />
            </Form.Item>
          </Form>
        </Modal>
      ) : null}

      {canGenerateSchedule ? (
        <Modal
          title={`生成课表${currentGenerateClass ? ` - 第${currentGenerateClass.classTerm}期` : ''}`}
          open={generateOpen}
          onOk={submitGenerate}
          onCancel={() => setGenerateOpen(false)}
          confirmLoading={generateLoading}
          destroyOnClose
        >
          <Form form={generateForm} layout="vertical">
            <Form.Item
              label="第一天课程日期"
              name="firstCourseDate"
              rules={[{ required: true, message: '请选择第一天课程日期' }]}
              extra="系统会从这一天开始，自动跳过 timor.tech 返回的休息日和法定节假日。"
            >
              <DatePicker style={{ width: '100%' }} format="YYYY-MM-DD" placeholder="请选择第一天上课日期" />
            </Form.Item>
          </Form>
        </Modal>
      ) : null}
    </PageContainer>
  );
}

function getClassStatusLabel(value: ClassStatusCode) {
  return classStatusOptions.find((item) => item.value === value)?.label ?? value;
}

function getClassStatusColor(value: ClassStatusCode) {
  if (value === 'WAITING') {
    return 'gold';
  }
  if (value === 'TEACHING') {
    return 'blue';
  }
  return 'green';
}

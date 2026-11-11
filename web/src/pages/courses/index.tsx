import { useEffect, useRef, useState } from 'react';
import { BookOutlined, LinkOutlined, PlusOutlined, UploadOutlined } from '@ant-design/icons';
import { Button, Form, Input, InputNumber, Modal, Popconfirm, Select, Space, Table, Tag, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import NoPermission from '../../components/NoPermission';
import PageContainer from '../../components/PageContainer';
import PanelTable from '../../components/PanelTable';
import StatusTag from '../../components/StatusTag';
import {
  createCourseDetail,
  deleteAllCourseDetails,
  deleteCourseDetail,
  getCourseDetailItem,
  getCourseDetailList,
  importCourseDetails,
  updateCourseDetail
} from '../../api/course-details';
import { createCourse, deleteCourse, getCourseDetail, getCoursePage, updateCourse } from '../../api/courses';
import type {
  CourseCreateReq,
  CourseDetailCreateReq,
  CourseDetailEntity,
  CourseDetailUpdateReq,
  CourseEntity,
  CourseUpdateReq
} from '../../types/system';
import { hasPermission } from '../../utils/permission';

interface SearchValues {
  keyword?: string;
  status?: number;
}

export default function CoursePage() {
  const canView = hasPermission('menu:teaching:course');
  const canCreate = hasPermission('button:course:create');
  const canUpdate = hasPermission('button:course:update');
  const canDelete = hasPermission('button:course:delete');
  const canManageDetail = hasPermission('button:course:detail');
  const canCreateDetail = hasPermission('button:course-detail:create');
  const canUpdateDetail = hasPermission('button:course-detail:update');
  const canDeleteDetail = hasPermission('button:course-detail:delete');
  const canDeleteAllDetail = hasPermission('button:course-detail:delete-all');
  const canImportDetail = hasPermission('button:course-detail:import');

  const [searchForm] = Form.useForm<SearchValues>();
  const [editForm] = Form.useForm<CourseCreateReq & CourseUpdateReq>();
  const [detailForm] = Form.useForm<CourseDetailCreateReq & CourseDetailUpdateReq>();
  const [loading, setLoading] = useState(false);
  const [open, setOpen] = useState(false);
  const [isEdit, setIsEdit] = useState(false);
  const [currentId, setCurrentId] = useState<number>();
  const [dataSource, setDataSource] = useState<CourseEntity[]>([]);
  const [detailLoading, setDetailLoading] = useState(false);
  const [detailOpen, setDetailOpen] = useState(false);
  const [detailEditOpen, setDetailEditOpen] = useState(false);
  const [detailEditId, setDetailEditId] = useState<number>();
  const [detailIsEdit, setDetailIsEdit] = useState(false);
  const [currentCourse, setCurrentCourse] = useState<CourseEntity>();
  const [detailDataSource, setDetailDataSource] = useState<CourseDetailEntity[]>([]);
  const [importing, setImporting] = useState(false);
  const [pageNum, setPageNum] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [total, setTotal] = useState(0);
  const importInputRef = useRef<HTMLInputElement | null>(null);

  const fetchData = async () => {
    setLoading(true);
    try {
      const values = searchForm.getFieldsValue();
      const result = await getCoursePage({ pageNum, pageSize, ...values });
      setDataSource(result.records);
      setTotal(result.total);
    } finally {
      setLoading(false);
    }
  };

  const fetchCourseDetails = async (courseId: number) => {
    setDetailLoading(true);
    try {
      const result = await getCourseDetailList(courseId);
      setDetailDataSource(result);
    } finally {
      setDetailLoading(false);
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

  const onCreate = () => {
    setIsEdit(false);
    setCurrentId(undefined);
    editForm.resetFields();
    // 新增时默认启用，减少重复操作。
    editForm.setFieldsValue({ status: 1, courseDays: 1 });
    setOpen(true);
  };

  const onEdit = async (record: CourseEntity) => {
    // 编辑前先拉详情，确保表单回填的是后端最新数据。
    const detail = await getCourseDetail(record.id);
    setIsEdit(true);
    setCurrentId(record.id);
    editForm.setFieldsValue({
      courseName: detail.courseName,
      courseDays: detail.courseDays,
      feishuMaterialPath: detail.feishuMaterialPath,
      status: detail.status
    });
    setOpen(true);
  };

  const onDelete = async (id: number) => {
    await deleteCourse(id);
    message.success('删除成功');
    fetchData();
  };

  const onManageDetail = async (record: CourseEntity) => {
    setCurrentCourse(record);
    setDetailOpen(true);
    await fetchCourseDetails(record.id);
  };

  const onCreateDetail = () => {
    detailForm.resetFields();
    setDetailIsEdit(false);
    setDetailEditId(undefined);
    detailForm.setFieldsValue({ dayNum: 1 });
    setDetailEditOpen(true);
  };

  const onEditDetail = async (record: CourseDetailEntity) => {
    if (!currentCourse) {
      return;
    }
    const detail = await getCourseDetailItem(currentCourse.id, record.id);
    setDetailIsEdit(true);
    setDetailEditId(record.id);
    detailForm.setFieldsValue({
      stageName: detail.stageName,
      dayNum: detail.dayNum,
      courseContent: detail.courseContent
    });
    setDetailEditOpen(true);
  };

  const onDeleteDetail = async (id: number) => {
    if (!currentCourse) {
      return;
    }
    await deleteCourseDetail(currentCourse.id, id);
    message.success('删除成功');
    fetchCourseDetails(currentCourse.id);
  };

  const onDeleteAllDetails = async () => {
    if (!currentCourse) {
      return;
    }
    await deleteAllCourseDetails(currentCourse.id);
    message.success('删除成功');
    fetchCourseDetails(currentCourse.id);
  };

  const onChooseImportFile = () => {
    importInputRef.current?.click();
  };

  const onImportFileChange = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    event.target.value = '';
    if (!file || !currentCourse) {
      return;
    }

    const fileName = file.name.toLowerCase();
    if (!fileName.endsWith('.xlsx') && !fileName.endsWith('.xls')) {
      message.error('请上传 xls 或 xlsx 格式的 Excel 文件');
      return;
    }

    setImporting(true);
    try {
      const count = await importCourseDetails(currentCourse.id, file);
      message.success(`导入成功，共处理 ${count} 条课程详情`);
      fetchCourseDetails(currentCourse.id);
    } finally {
      setImporting(false);
    }
  };

  const submitEdit = async () => {
    const values = await editForm.validateFields();
    if (isEdit && currentId) {
      await updateCourse(currentId, values as CourseUpdateReq);
      message.success('更新成功');
    } else {
      await createCourse(values as CourseCreateReq);
      message.success('创建成功');
    }
    setOpen(false);
    fetchData();
  };

  const submitDetailEdit = async () => {
    if (!currentCourse) {
      return;
    }
    const values = await detailForm.validateFields();
    if (detailIsEdit && detailEditId) {
      await updateCourseDetail(currentCourse.id, detailEditId, values as CourseDetailUpdateReq);
      message.success('更新成功');
    } else {
      await createCourseDetail(currentCourse.id, values as CourseDetailCreateReq);
      message.success('创建成功');
    }
    setDetailEditOpen(false);
    fetchCourseDetails(currentCourse.id);
  };

  const columns: ColumnsType<CourseEntity> = [
    { title: 'ID', dataIndex: 'id', width: 80 },
    { title: '课程名称', dataIndex: 'courseName', width: 180 },
    { title: '课程天数', dataIndex: 'courseDays', width: 120, render: (value) => `${value} 天` },
    {
      title: '飞书资料路径',
      dataIndex: 'feishuMaterialPath',
      width: 380,
      render: (value) => (
        <Typography.Text copyable={{ text: value }} ellipsis={{ tooltip: value }}>
          {value}
        </Typography.Text>
      )
    },
    { title: '状态', dataIndex: 'status', width: 100, render: (value) => <StatusTag value={value} /> },
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
      width: 240,
      render: (_, record) => (
        <Space>
          {canManageDetail ? (
            <Button type="link" onClick={() => onManageDetail(record)}>
              课程详情
            </Button>
          ) : null}
          {canUpdate ? (
            <Button type="link" onClick={() => onEdit(record)}>
              编辑
            </Button>
          ) : null}
          {canDelete ? (
            <Popconfirm title="确认删除该课程吗？" onConfirm={() => onDelete(record.id)}>
              <Button type="link" danger>
                删除
              </Button>
            </Popconfirm>
          ) : null}
        </Space>
      )
    }
  ];

  const detailColumns: ColumnsType<CourseDetailEntity> = [
    { title: '第几天', dataIndex: 'dayNum', width: 120 },
    {
      title: '课程内容',
      dataIndex: 'courseContent',
      render: (value) => (
        <Typography.Paragraph ellipsis={{ rows: 2, tooltip: value }} style={{ marginBottom: 0 }}>
          {value}
        </Typography.Paragraph>
      )
    },
    {
      title: '阶段',
      dataIndex: 'stageName',
      width: 140,
      render: (value) => (
        <Tag bordered={false} color="blue" className="course-stage-tag">
          {value}
        </Tag>
      )
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      width: 180,
      render: (value) => (value ? dayjs(value).format('YYYY-MM-DD HH:mm:ss') : '-')
    },
    {
      title: '操作',
      key: 'action',
      width: 160,
      fixed: 'right',
      render: (_, record) => (
        <Space>
          {canUpdateDetail ? (
            <Button type="link" onClick={() => onEditDetail(record)}>
              编辑
            </Button>
          ) : null}
          {canDeleteDetail ? (
            <Popconfirm title="确认删除该课程详情吗？" onConfirm={() => onDeleteDetail(record.id)}>
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
      title="课程体系"
      description="维护教学管理中的课程信息，包括课程名称、课程天数和飞书资料路径。"
      extra={canCreate ? <Button type="primary" onClick={onCreate}>新增课程</Button> : null}
    >
      <Form form={searchForm} layout="inline" className="search-form" onFinish={fetchData}>
        <Form.Item name="keyword">
          <Input placeholder="课程名称/飞书资料路径" allowClear style={{ width: 280 }} prefix={<BookOutlined />} />
        </Form.Item>
        <Form.Item name="status">
          <Select
            allowClear
            placeholder="状态"
            style={{ width: 140 }}
            options={[
              { label: '启用', value: 1 },
              { label: '禁用', value: 0 }
            ]}
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
          scroll={{ x: 1300 }}
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
          title={isEdit ? '编辑课程' : '新增课程'}
          open={open}
          onOk={submitEdit}
          onCancel={() => setOpen(false)}
          destroyOnClose
        >
          <Form form={editForm} layout="vertical">
            <Form.Item
              label="课程名称"
              name="courseName"
              rules={[{ required: true, message: '请输入课程名称' }]}
            >
              <Input placeholder="例如：Python 入门课" />
            </Form.Item>
            <Form.Item
              label="课程天数"
              name="courseDays"
              rules={[{ required: true, message: '请输入课程天数' }]}
            >
              <InputNumber min={1} precision={0} style={{ width: '100%' }} placeholder="请输入课程天数" />
            </Form.Item>
            <Form.Item
              label="飞书资料路径"
              name="feishuMaterialPath"
              rules={[{ required: true, message: '请输入飞书资料路径' }]}
            >
              <Input placeholder="例如：知识库/教学资料/Python 入门课" prefix={<LinkOutlined />} />
            </Form.Item>
            <Form.Item
              label="状态"
              name="status"
              rules={[{ required: true, message: '请选择状态' }]}
            >
              <Select
                options={[
                  { label: '启用', value: 1 },
                  { label: '禁用', value: 0 }
                ]}
              />
            </Form.Item>
          </Form>
        </Modal>
      ) : null}

      <Modal
        title={currentCourse ? `课程详情管理 - ${currentCourse.courseName}` : '课程详情管理'}
        open={detailOpen}
        onCancel={() => {
          setDetailOpen(false);
          setCurrentCourse(undefined);
          setDetailDataSource([]);
        }}
        footer={null}
        width={1500}
        destroyOnClose
      >
        <Space style={{ marginBottom: 16 }}>
          {canCreateDetail ? (
            <Button type="primary" icon={<PlusOutlined />} onClick={onCreateDetail}>
              新增一条
            </Button>
          ) : null}
          {canImportDetail ? (
            <Button icon={<UploadOutlined />} loading={importing} onClick={onChooseImportFile}>
              上传 Excel
            </Button>
          ) : null}
          {canDeleteAllDetail ? (
            <Popconfirm title="确认删除该课程的全部详情吗？" onConfirm={onDeleteAllDetails}>
              <Button danger ghost>
                删除所有
              </Button>
            </Popconfirm>
          ) : null}
          {detailDataSource.length ? (
            <Space size={8} style={{ marginLeft: 8 }}>
              <Tag bordered={false}>共 {detailDataSource.length} 条</Tag>
              <Tag bordered={false} color="processing">
                {groupCourseDetails(detailDataSource).length} 个阶段
              </Tag>
            </Space>
          ) : null}
        </Space>
        <input
          ref={importInputRef}
          type="file"
          accept=".xls,.xlsx"
          style={{ display: 'none' }}
          onChange={onImportFileChange}
        />
        <div className="course-outline-shell">
          {groupCourseDetails(detailDataSource).map((group, index) => (
            <section key={group.stageName} className="course-outline-section">
              <div className="course-outline-section-header">
                <div className="course-outline-stage">
                  <span className="course-outline-stage-index">{String(index + 1).padStart(2, '0')}</span>
                  <div>
                    <div className="course-outline-stage-name">{group.stageName}</div>
                    <div className="course-outline-stage-meta">
                      共 {group.records.length} 天
                      {group.records.length ? ` · 第 ${group.records[0].dayNum} 天 - 第 ${group.records[group.records.length - 1].dayNum} 天` : ''}
                    </div>
                  </div>
                </div>
              </div>
              <Table
                rowKey="id"
                loading={detailLoading}
                columns={detailColumns}
                dataSource={group.records}
                pagination={false}
                className="course-outline-table"
              />
            </section>
          ))}
          {!detailLoading && detailDataSource.length === 0 ? (
            <div className="course-outline-empty">
              <Typography.Text type="secondary">还没有课程详情，先新增一条或上传 Excel。</Typography.Text>
            </div>
          ) : null}
        </div>
      </Modal>

      <Modal
        title={detailIsEdit ? '编辑课程详情' : '新增课程详情'}
        open={detailEditOpen}
        onOk={submitDetailEdit}
        onCancel={() => setDetailEditOpen(false)}
        destroyOnClose
      >
        <Form form={detailForm} layout="vertical">
          <Form.Item
            label="所属阶段"
            name="stageName"
            rules={[{ required: true, message: '请输入所属阶段' }]}
          >
            <Input placeholder="例如：基础 / spring / 项目实战" />
          </Form.Item>
          <Form.Item
            label="第几天课"
            name="dayNum"
            rules={[{ required: true, message: '请输入第几天课' }]}
          >
            <InputNumber min={1} precision={0} style={{ width: '100%' }} placeholder="请输入第几天课" />
          </Form.Item>
          <Form.Item
            label="课程内容"
            name="courseContent"
            rules={[{ required: true, message: '请输入课程内容' }]}
          >
            <Input.TextArea rows={5} placeholder="请输入当天的课程内容" />
          </Form.Item>
        </Form>
      </Modal>
    </PageContainer>
  );
}

function groupCourseDetails(records: CourseDetailEntity[]) {
  const sorted = [...records].sort((a, b) => a.dayNum - b.dayNum || a.id - b.id);
  const groupMap = new Map<string, CourseDetailEntity[]>();

  sorted.forEach((item) => {
    const recordsInStage = groupMap.get(item.stageName);
    if (recordsInStage) {
      recordsInStage.push(item);
      return;
    }
    groupMap.set(item.stageName, [item]);
  });

  return Array.from(groupMap.entries()).map(([stageName, stageRecords]) => ({
    stageName,
    records: stageRecords
  }));
}

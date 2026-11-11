import {
  DeleteOutlined,
  EditOutlined,
  EyeOutlined,
  PlayCircleOutlined,
  PlusOutlined,
  StopOutlined,
  UploadOutlined
} from '@ant-design/icons';
import { Button, DatePicker, Form, Input, message, Modal, Popconfirm, Select, Space, Spin, Switch, Table, Tag, Upload } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import { useEffect, useMemo, useState } from 'react';
import NoPermission from '../../components/NoPermission';
import PageContainer from '../../components/PageContainer';
import PanelTable from '../../components/PanelTable';
import { getClassPage } from '../../api/classes';
import {
  createHomework,
  deleteHomework,
  getHomeworkDetail,
  getHomeworkPage,
  getHomeworkPublishContext,
  getHomeworkSubmissionPage,
  manageHomeworkAnswer,
  reviewHomeworkSubmission,
  updateHomework,
  updateHomeworkStatus,
  uploadHomeworkFile
} from '../../api/homeworks';
import type {
  ClassEntity,
  HomeworkAnswerManageReq,
  HomeworkAssignmentAdminItem,
  HomeworkAssignmentQueryParams,
  HomeworkAssignmentSaveReq,
  HomeworkPublishContextRes,
  HomeworkSubmissionAdminItem,
  HomeworkSubmissionAdminQueryParams,
  HomeworkSubmissionReviewReq,
  HomeworkSubmissionStatusCode,
  HomeworkStatusCode
} from '../../types/system';
import { hasPermission } from '../../utils/permission';

interface SearchValues {
  title?: string;
  classId?: number;
  status?: HomeworkStatusCode;
}

interface EditValues {
  classId?: number;
  homeworkDate?: dayjs.Dayjs;
  courseContentSnapshot?: string;
  title?: string;
  description?: string;
  deadline?: dayjs.Dayjs;
  allowResubmit?: boolean;
}

interface AnswerValues {
  publishAnswer?: boolean;
}

interface HomeworkFileState {
  name?: string;
  url?: string;
  downloadUrl?: string;
  type?: string;
}

interface SubmissionSearchValues {
  studentCode?: string;
  studentName?: string;
  status?: HomeworkSubmissionStatusCode;
}

interface ReviewValues {
  status: 'REVIEWED' | 'RESUBMIT_REQUIRED';
  score?: number;
  teacherComment?: string;
}

const homeworkStatusOptions = [
  { label: '进行中', value: 'PUBLISHED' },
  { label: '已关闭', value: 'CLOSED' }
] as const;

const submissionStatusOptions = [
  { label: '未提交', value: 'UNSUBMITTED' },
  { label: '待批改', value: 'SUBMITTED' },
  { label: '已批改', value: 'REVIEWED' },
  { label: '待重交', value: 'RESUBMIT_REQUIRED' }
] as const;

export default function HomeworkPage() {
  const canView = hasPermission('menu:teaching:homework');
  const canCreate = hasPermission('button:homework:create');
  const canUpdate = hasPermission('button:homework:update');
  const canDelete = hasPermission('button:homework:delete');
  const canToggleStatus = hasPermission('button:homework:toggle-status');
  const canViewSubmission = hasPermission('button:homework:submission-list');
  const canManageAnswer = hasPermission('button:homework:answer-manage');

  const [searchForm] = Form.useForm<SearchValues>();
  const [editForm] = Form.useForm<EditValues>();
  const [answerForm] = Form.useForm<AnswerValues>();
  const [submissionSearchForm] = Form.useForm<SubmissionSearchValues>();
  const [reviewForm] = Form.useForm<ReviewValues>();

  const [loading, setLoading] = useState(false);
  const [classLoading, setClassLoading] = useState(false);
  const [publishContextLoading, setPublishContextLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [answerSaving, setAnswerSaving] = useState(false);
  const [uploadingAttachment, setUploadingAttachment] = useState(false);
  const [uploadingAnswer, setUploadingAnswer] = useState(false);
  const [open, setOpen] = useState(false);
  const [answerOpen, setAnswerOpen] = useState(false);
  const [isEdit, setIsEdit] = useState(false);
  const [currentId, setCurrentId] = useState<number>();
  const [answerHomeworkId, setAnswerHomeworkId] = useState<number>();
  const [dataSource, setDataSource] = useState<HomeworkAssignmentAdminItem[]>([]);
  const [classOptions, setClassOptions] = useState<ClassEntity[]>([]);
  const [publishContext, setPublishContext] = useState<HomeworkPublishContextRes | null>(null);
  const [attachment, setAttachment] = useState<HomeworkFileState>({});
  const [answerAttachment, setAnswerAttachment] = useState<HomeworkFileState>({});
  const [previewOpen, setPreviewOpen] = useState(false);
  const [previewLoading, setPreviewLoading] = useState(false);
  const [previewTitle, setPreviewTitle] = useState('');
  const [previewContent, setPreviewContent] = useState('');
  const previewHtml = useMemo(() => renderMarkdownToHtml(previewContent), [previewContent]);
  const [pageNum, setPageNum] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [total, setTotal] = useState(0);
  const [submissionOpen, setSubmissionOpen] = useState(false);
  const [submissionLoading, setSubmissionLoading] = useState(false);
  const [submissionDataSource, setSubmissionDataSource] = useState<HomeworkSubmissionAdminItem[]>([]);
  const [submissionPageNum, setSubmissionPageNum] = useState(1);
  const [submissionPageSize, setSubmissionPageSize] = useState(10);
  const [submissionTotal, setSubmissionTotal] = useState(0);
  const [submissionHomework, setSubmissionHomework] = useState<HomeworkAssignmentAdminItem | null>(null);
  const [reviewOpen, setReviewOpen] = useState(false);
  const [reviewSaving, setReviewSaving] = useState(false);
  const [reviewRecord, setReviewRecord] = useState<HomeworkSubmissionAdminItem | null>(null);

  const classLabelMap = useMemo(
    () =>
      new Map(
        classOptions.map((item) => [
          item.id,
          `${item.campusName || '未命名校区'} 第${item.classTerm}期`
        ])
      ),
    [classOptions]
  );

  const fetchData = async () => {
    setLoading(true);
    try {
      const values = searchForm.getFieldsValue();
      const result = await getHomeworkPage({ pageNum, pageSize, ...values } as HomeworkAssignmentQueryParams);
      setDataSource(result.records);
      setTotal(result.total);
    } finally {
      setLoading(false);
    }
  };

  const fetchSubmissionData = async (assignmentId?: number) => {
    const targetId = assignmentId ?? submissionHomework?.id;
    if (!targetId) {
      return;
    }
    setSubmissionLoading(true);
    try {
      const values = submissionSearchForm.getFieldsValue();
      const result = await getHomeworkSubmissionPage(targetId, {
        pageNum: submissionPageNum,
        pageSize: submissionPageSize,
        ...values
      } as HomeworkSubmissionAdminQueryParams);
      setSubmissionDataSource(result.records);
      setSubmissionTotal(result.total);
    } finally {
      setSubmissionLoading(false);
    }
  };

  const fetchClassOptions = async () => {
    setClassLoading(true);
    try {
      const result = await getClassPage({ pageNum: 1, pageSize: 1000 });
      setClassOptions(result.records);
    } finally {
      setClassLoading(false);
    }
  };

  useEffect(() => {
    if (!canView) {
      return;
    }
    fetchData();
  }, [canView, pageNum, pageSize]);

  useEffect(() => {
    if (!submissionOpen || !submissionHomework) {
      return;
    }
    fetchSubmissionData();
  }, [submissionOpen, submissionHomework, submissionPageNum, submissionPageSize]);

  useEffect(() => {
    if (!canView) {
      return;
    }
    fetchClassOptions();
  }, [canView]);

  const resetSearch = () => {
    searchForm.resetFields();
    if (pageNum !== 1) {
      setPageNum(1);
      return;
    }
    fetchData();
  };

  const openCreateModal = () => {
    setIsEdit(false);
    setCurrentId(undefined);
    editForm.resetFields();
    editForm.setFieldsValue({ allowResubmit: false });
    setPublishContext(null);
    setAttachment({});
    setOpen(true);
  };

  const openEditModal = async (record: HomeworkAssignmentAdminItem) => {
    const detail = await getHomeworkDetail(record.id);
    setIsEdit(true);
    setCurrentId(record.id);
    editForm.setFieldsValue({
      classId: detail.classId,
      homeworkDate: detail.homeworkDate ? dayjs(detail.homeworkDate) : undefined,
      courseContentSnapshot: detail.courseContentSnapshot,
      title: detail.title,
      description: detail.description,
      deadline: detail.deadline ? dayjs(detail.deadline) : undefined,
      allowResubmit: detail.allowResubmit
    });
    setPublishContext({
      classId: detail.classId,
      classTerm: detail.classTerm,
      campusName: detail.campusName,
      courseId: detail.courseId,
      courseName: detail.courseName,
      courseDetailId: detail.courseDetailId,
      courseDayNum: detail.courseDayNum,
      courseContentSnapshot: detail.courseContentSnapshot,
      scheduleType: detail.scheduleType,
      homeworkDate: detail.homeworkDate
    });
    setAttachment({
      name: detail.attachmentName,
      url: detail.attachmentUrl,
      downloadUrl: detail.attachmentDownloadUrl,
      type: detail.attachmentType
    });
    setOpen(true);
  };

  const openAnswerModal = async (record: HomeworkAssignmentAdminItem) => {
    const detail = await getHomeworkDetail(record.id);
    setAnswerHomeworkId(record.id);
    answerForm.setFieldsValue({
      publishAnswer: detail.answerPublishStatus === 'PUBLISHED'
    });
    setAnswerAttachment({
      name: detail.answerAttachmentName,
      url: detail.answerAttachmentUrl,
      downloadUrl: detail.answerAttachmentDownloadUrl,
      type: detail.answerAttachmentType
    });
    setAnswerOpen(true);
  };

  const openSubmissionModal = async (record: HomeworkAssignmentAdminItem) => {
    setSubmissionHomework(record);
    submissionSearchForm.resetFields();
    setSubmissionPageNum(1);
    setSubmissionPageSize(10);
    setSubmissionOpen(true);
    await fetchSubmissionData(record.id);
  };

  const handleHomeworkDateOrClassChange = async () => {
    const classId = editForm.getFieldValue('classId');
    const homeworkDate = editForm.getFieldValue('homeworkDate');
    if (!classId || !homeworkDate) {
      setPublishContext(null);
      return;
    }
    setPublishContextLoading(true);
    try {
      const context = await getHomeworkPublishContext(classId, dayjs(homeworkDate).format('YYYY-MM-DD'));
      setPublishContext(context);
      editForm.setFieldsValue({
        courseContentSnapshot: context.courseContentSnapshot || editForm.getFieldValue('courseContentSnapshot')
      });
    } finally {
      setPublishContextLoading(false);
    }
  };

  const handleUpload = async (file: File, kind: 'assignment' | 'answer') => {
    if (!file.name.toLowerCase().endsWith('.md')) {
      message.error('当前仅支持上传 .md 文档');
      return false;
    }
    if (kind === 'assignment') {
      setUploadingAttachment(true);
    } else {
      setUploadingAnswer(true);
    }
    try {
      const uploadResult = await uploadHomeworkFile(file, kind === 'assignment' ? 'homework/assignment' : 'homework/answer');
      const url = normalizeUploadedUrl(uploadResult);
      if (!url) {
        throw new Error('上传接口未返回文件地址');
      }
      const nextValue: HomeworkFileState = {
        name: file.name,
        url,
        downloadUrl: url,
        type: 'text/markdown'
      };
      if (kind === 'assignment') {
        setAttachment(nextValue);
      } else {
        setAnswerAttachment(nextValue);
      }
      message.success('上传成功');
    } catch (error: any) {
      message.error(error?.message || '上传失败');
    } finally {
      if (kind === 'assignment') {
        setUploadingAttachment(false);
      } else {
        setUploadingAnswer(false);
      }
    }
    return false;
  };

  const submitEdit = async () => {
    const values = await editForm.validateFields();
    if (!publishContext?.courseId) {
      message.error('请先选择班级和作业日期，带出课程信息');
      return;
    }
    if (attachment.name && !attachment.url) {
      message.error('作业附件还没有拿到可用地址，请重新上传一次');
      return;
    }
    const payload: HomeworkAssignmentSaveReq = {
      classId: values.classId!,
      homeworkDate: dayjs(values.homeworkDate).format('YYYY-MM-DD'),
      courseId: publishContext.courseId,
      courseDetailId: publishContext.courseDetailId,
      courseContentSnapshot: values.courseContentSnapshot?.trim() || publishContext.courseContentSnapshot || '',
      title: values.title?.trim() || '',
      description: values.description?.trim(),
      attachmentName: attachment.name,
      attachmentUrl: attachment.url,
      attachmentType: attachment.type,
      deadline: values.deadline ? dayjs(values.deadline).format('YYYY-MM-DD HH:mm:ss') : undefined,
      allowResubmit: Boolean(values.allowResubmit)
    };
    setSaving(true);
    try {
      if (isEdit && currentId) {
        await updateHomework(currentId, payload);
        message.success('作业更新成功');
      } else {
        await createHomework(payload);
        message.success('作业发布成功');
      }
      setOpen(false);
      fetchData();
    } finally {
      setSaving(false);
    }
  };

  const submitAnswer = async () => {
    if (!answerHomeworkId) {
      return;
    }
    const values = await answerForm.validateFields();
    if (answerAttachment.name && !answerAttachment.url) {
      message.error('答案附件还没有拿到可用地址，请重新上传一次');
      return;
    }
    const payload: HomeworkAnswerManageReq = {
      answerAttachmentName: answerAttachment.name,
      answerAttachmentUrl: answerAttachment.url,
      answerAttachmentType: answerAttachment.type,
      publishAnswer: Boolean(values.publishAnswer)
    };
    setAnswerSaving(true);
    try {
      await manageHomeworkAnswer(answerHomeworkId, payload);
      message.success('答案保存成功');
      setAnswerOpen(false);
      fetchData();
    } finally {
      setAnswerSaving(false);
    }
  };

  const changeStatus = async (record: HomeworkAssignmentAdminItem, status: HomeworkStatusCode) => {
    await updateHomeworkStatus(record.id, { status });
    message.success(status === 'CLOSED' ? '作业已关闭' : '作业已开放');
    fetchData();
  };

  const removeHomework = async (id: number) => {
    await deleteHomework(id);
    message.success('删除成功');
    fetchData();
  };

  const openMarkdownPreview = async (name?: string, url?: string) => {
    if (!name || !url) {
      message.error('当前附件还没有可预览地址');
      return;
    }
    setPreviewTitle(name);
    setPreviewContent('');
    setPreviewOpen(true);
    setPreviewLoading(true);
    try {
      const response = await fetch(url);
      if (!response.ok) {
        throw new Error('预览内容加载失败');
      }
      setPreviewContent(await response.text());
    } catch (error: any) {
      message.error(error?.message || '预览内容加载失败');
    } finally {
      setPreviewLoading(false);
    }
  };

  const resetSubmissionSearch = () => {
    submissionSearchForm.resetFields();
    if (submissionPageNum !== 1) {
      setSubmissionPageNum(1);
      return;
    }
    fetchSubmissionData();
  };

  const openReviewModal = (record: HomeworkSubmissionAdminItem) => {
    if (!record.submissionId) {
      message.warning('该学员还没有提交作业');
      return;
    }
    setReviewRecord(record);
    reviewForm.setFieldsValue({
      status: record.status === 'RESUBMIT_REQUIRED' ? 'RESUBMIT_REQUIRED' : 'REVIEWED',
      score: record.score,
      teacherComment: record.teacherComment
    });
    setReviewOpen(true);
  };

  const submitReview = async () => {
    if (!submissionHomework?.id || !reviewRecord?.submissionId) {
      return;
    }
    const values = await reviewForm.validateFields();
    const payload: HomeworkSubmissionReviewReq = {
      status: values.status,
      score: values.status === 'REVIEWED' ? values.score : values.score,
      teacherComment: values.teacherComment?.trim()
    };
    setReviewSaving(true);
    try {
      await reviewHomeworkSubmission(submissionHomework.id, reviewRecord.submissionId, payload);
      message.success('批改成功');
      setReviewOpen(false);
      fetchSubmissionData();
      fetchData();
    } finally {
      setReviewSaving(false);
    }
  };

  const columns: ColumnsType<HomeworkAssignmentAdminItem> = [
    {
      title: '标题',
      dataIndex: 'title',
      width: 260,
      render: (value, record) => (
        <div>
          <div style={{ fontWeight: 700, color: '#2f241d', marginBottom: 8 }}>{value}</div>
          <div style={{ color: '#7f6a5f', whiteSpace: 'pre-wrap' }}>{record.description || '未填写作业说明'}</div>
        </div>
      )
    },
    {
      title: '班级',
      dataIndex: 'classId',
      width: 170,
      render: (_, record) => `${record.campusName || '-'} 第${record.classTerm || '-'}期`
    },
    {
      title: '当前课程',
      dataIndex: 'courseContentSnapshot',
      width: 280,
      render: (value, record) => (
        <div>
          <div style={{ fontWeight: 600, marginBottom: 6 }}>{value || '-'}</div>
          <div style={{ color: '#7f6a5f' }}>
            {record.courseDayNum ? `第 ${record.courseDayNum} 天` : '未匹配课次'}
            {record.courseName ? ` · ${record.courseName}` : ''}
          </div>
        </div>
      )
    },
    {
      title: '作业日期',
      dataIndex: 'homeworkDate',
      width: 130,
      render: (value) => (value ? dayjs(value).format('YYYY-MM-DD') : '-')
    },
    {
      title: '附件',
      dataIndex: 'attachmentName',
      width: 180,
      render: (_, record) => renderFileLink(record.attachmentName, record.attachmentDownloadUrl, openMarkdownPreview)
    },
    {
      title: '答案',
      dataIndex: 'answerAttachmentName',
      width: 220,
      render: (_, record) => (
        <Space direction="vertical" size={4}>
          {renderFileLink(record.answerAttachmentName, record.answerAttachmentDownloadUrl, openMarkdownPreview)}
          <Tag color={record.answerPublishStatus === 'PUBLISHED' ? 'green' : 'default'} bordered={false}>
            {record.answerPublishStatus === 'PUBLISHED' ? '已发布' : '未发布'}
          </Tag>
        </Space>
      )
    },
    {
      title: '截止时间',
      dataIndex: 'deadline',
      width: 170,
      render: (value) => (value ? dayjs(value).format('YYYY-MM-DD HH:mm:ss') : '-')
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 110,
      render: (value: HomeworkStatusCode) => (
        <Tag color={value === 'PUBLISHED' ? 'blue' : 'default'} bordered={false}>
          {value === 'PUBLISHED' ? '进行中' : '已关闭'}
        </Tag>
      )
    },
    {
      title: '提交情况',
      dataIndex: 'submittedCount',
      width: 120,
      render: (_, record) => `${record.submittedCount || 0}/${record.studentCountSnapshot || 0}`
    },
    {
      title: '发布人',
      dataIndex: 'publishTeacherName',
      width: 120,
      render: (value) => value || '-'
    },
    {
      title: '操作',
      key: 'action',
      width: 280,
      fixed: 'right',
      render: (_, record) => (
        <Space wrap size={[10, 6]}>
          {canViewSubmission ? (
            <Button type="link" icon={<EyeOutlined />} onClick={() => openSubmissionModal(record)}>
              提交列表
            </Button>
          ) : null}
          {canUpdate ? (
            <Button type="link" icon={<EditOutlined />} onClick={() => openEditModal(record)}>
              编辑
            </Button>
          ) : null}
          {canManageAnswer ? (
            <Button type="link" onClick={() => openAnswerModal(record)}>
              答案管理
            </Button>
          ) : null}
          {canToggleStatus ? (
            <Popconfirm
              title={record.status === 'PUBLISHED' ? '确认关闭这条作业？' : '确认重新开放这条作业？'}
              onConfirm={() => changeStatus(record, record.status === 'PUBLISHED' ? 'CLOSED' : 'PUBLISHED')}
            >
              <Button type="link" icon={record.status === 'PUBLISHED' ? <StopOutlined /> : <PlayCircleOutlined />}>
                {record.status === 'PUBLISHED' ? '关闭' : '开放'}
              </Button>
            </Popconfirm>
          ) : null}
          {canDelete ? (
            <Popconfirm title="确认删除这条作业？" onConfirm={() => removeHomework(record.id)}>
              <Button type="link" danger icon={<DeleteOutlined />}>
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
    <PageContainer title="作业考核" description="按班级与作业日期发布作业，并统一维护附件、答案和开放状态。">
      <Form form={searchForm} layout="inline" className="search-form" onFinish={fetchData}>
        <Form.Item name="title">
          <Input allowClear placeholder="作业标题" style={{ width: 220 }} />
        </Form.Item>
        <Form.Item name="classId">
          <Select
            allowClear
            loading={classLoading}
            placeholder="班级"
            style={{ width: 220 }}
            options={classOptions.map((item) => ({
              label: `${item.campusName || '-'} 第${item.classTerm}期`,
              value: item.id
            }))}
          />
        </Form.Item>
        <Form.Item name="status">
          <Select allowClear placeholder="状态" style={{ width: 180 }} options={homeworkStatusOptions as any} />
        </Form.Item>
        <Form.Item>
          <Space>
            <Button type="primary" htmlType="submit">
              查询
            </Button>
            <Button onClick={resetSearch}>重置</Button>
          </Space>
        </Form.Item>
        {canCreate ? (
          <Form.Item style={{ marginLeft: 'auto' }}>
            <Button type="primary" icon={<PlusOutlined />} onClick={openCreateModal}>
              发布作业
            </Button>
          </Form.Item>
        ) : null}
      </Form>

      <PanelTable>
        <Table
          rowKey="id"
          loading={loading}
          columns={columns}
          dataSource={dataSource}
          scroll={{ x: 1880 }}
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

      <Modal
        open={open}
        title={isEdit ? '编辑作业' : '发布作业'}
        onCancel={() => setOpen(false)}
        onOk={submitEdit}
        okText={isEdit ? '保存' : '确定'}
        confirmLoading={saving}
        destroyOnClose
        width={980}
      >
        <Form
          form={editForm}
          layout="vertical"
          onValuesChange={(changedValues) => {
            if ('classId' in changedValues || 'homeworkDate' in changedValues) {
              handleHomeworkDateOrClassChange();
            }
          }}
        >
          <Form.Item name="classId" label="班级" rules={[{ required: true, message: '请选择班级' }]}>
            <Select
              showSearch
              optionFilterProp="label"
              loading={classLoading}
              placeholder="请选择班级"
              options={classOptions.map((item) => ({
                label: classLabelMap.get(item.id) || `班级 ${item.id}`,
                value: item.id
              }))}
            />
          </Form.Item>

          <Form.Item name="homeworkDate" label="作业日期" rules={[{ required: true, message: '请选择作业日期' }]}>
            <DatePicker style={{ width: '100%' }} />
          </Form.Item>

          <div
            style={{
              marginBottom: 24,
              padding: 20,
              borderRadius: 16,
              background: '#faf7f4',
              border: '1px solid #f0e7df'
            }}
          >
            <div style={{ fontSize: 26, fontWeight: 700, marginBottom: 16 }}>课程信息</div>
            <div style={{ color: '#7f6a5f', marginBottom: 14 }}>
              {publishContextLoading ? '正在匹配课表中的课程内容...' : '请选择班级和作业日期后自动带出课程内容，老师也可以按实际情况微调。'}
            </div>
            <div style={{ marginBottom: 14, color: '#7f6a5f' }}>
              {publishContext
                ? `${publishContext.campusName || '-'} 第${publishContext.classTerm || '-'}期 · ${publishContext.courseName || '未命名课程'}${publishContext.courseDayNum ? ` · 第 ${publishContext.courseDayNum} 天` : ''}`
                : '暂未匹配到课程信息'}
            </div>
            <Form.Item name="courseContentSnapshot" label="课程内容" rules={[{ required: true, message: '请填写课程内容' }]} style={{ marginBottom: 0 }}>
              <Input.TextArea rows={4} placeholder="系统会先自动带出课程内容，老师也可以按实际情况修改" />
            </Form.Item>
          </div>

          <Form.Item name="title" label="作业标题" rules={[{ required: true, message: '请输入作业标题' }]}>
            <Input placeholder="例如：第 12 天作业" maxLength={200} />
          </Form.Item>

          <Form.Item name="description" label="作业说明">
            <Input.TextArea rows={5} placeholder="请输入作业要求、提交说明等" maxLength={5000} />
          </Form.Item>

          <Form.Item label="作业附件">
            <Space direction="vertical" size={10} style={{ width: '100%' }}>
              <Upload showUploadList={false} beforeUpload={(file) => handleUpload(file, 'assignment')} accept=".md">
                <Button icon={<UploadOutlined />} loading={uploadingAttachment}>
                  上传 Markdown
                </Button>
              </Upload>
              <div style={{ color: '#7f6a5f' }}>当前仅支持上传 `.md` 文档作为作业附件。</div>
              {attachment.name ? (
                <Space>
                  {renderFileLink(attachment.name, attachment.downloadUrl || attachment.url, openMarkdownPreview)}
                  <Button type="link" danger onClick={() => setAttachment({})}>
                    清除
                  </Button>
                </Space>
              ) : null}
            </Space>
          </Form.Item>

          <Form.Item name="deadline" label="截止时间">
            <DatePicker showTime style={{ width: '100%' }} placeholder="请选择日期" />
          </Form.Item>

          <Form.Item name="allowResubmit" label="允许重交" valuePropName="checked">
            <Switch />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        open={answerOpen}
        title="答案管理"
        onCancel={() => setAnswerOpen(false)}
        onOk={submitAnswer}
        okText="保存"
        confirmLoading={answerSaving}
        destroyOnClose
        width={760}
      >
        <Form form={answerForm} layout="vertical">
          <Form.Item label="答案附件">
            <Space direction="vertical" size={10} style={{ width: '100%' }}>
              <Upload showUploadList={false} beforeUpload={(file) => handleUpload(file, 'answer')} accept=".md">
                <Button icon={<UploadOutlined />} loading={uploadingAnswer}>
                  上传 Markdown
                </Button>
              </Upload>
              <div style={{ color: '#7f6a5f' }}>当前仅支持上传 `.md` 文档作为答案附件。</div>
              {answerAttachment.name ? (
                <Space>
                  {renderFileLink(answerAttachment.name, answerAttachment.downloadUrl || answerAttachment.url, openMarkdownPreview)}
                  <Button type="link" danger onClick={() => setAnswerAttachment({})}>
                    清除
                  </Button>
                </Space>
              ) : null}
            </Space>
          </Form.Item>

          <Form.Item name="publishAnswer" label="发布答案" valuePropName="checked">
            <Switch checkedChildren="已发布" unCheckedChildren="未发布" />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        open={previewOpen}
        title={previewTitle || 'Markdown 预览'}
        onCancel={() => setPreviewOpen(false)}
        footer={null}
        width={960}
        destroyOnClose
      >
        {previewLoading ? (
          <div style={{ padding: '60px 0', display: 'flex', justifyContent: 'center' }}>
            <Spin />
          </div>
        ) : (
          <div
            style={{
              maxHeight: '68vh',
              overflow: 'auto',
              padding: 16,
              borderRadius: 12,
              background: '#faf7f4',
              lineHeight: 1.8
            }}
          >
            <style>{`
              .homework-markdown-preview h1,
              .homework-markdown-preview h2,
              .homework-markdown-preview h3,
              .homework-markdown-preview h4,
              .homework-markdown-preview h5,
              .homework-markdown-preview h6 {
                margin: 0 0 16px;
                color: #2f241d;
                line-height: 1.35;
              }
              .homework-markdown-preview h1 { font-size: 30px; }
              .homework-markdown-preview h2 { font-size: 24px; }
              .homework-markdown-preview h3 { font-size: 20px; }
              .homework-markdown-preview p {
                margin: 0 0 16px;
                color: #41342b;
                font-size: 16px;
              }
              .homework-markdown-preview ul,
              .homework-markdown-preview ol {
                margin: 0 0 16px;
                padding-left: 24px;
                color: #41342b;
              }
              .homework-markdown-preview li {
                margin-bottom: 8px;
              }
              .homework-markdown-preview code {
                padding: 2px 6px;
                border-radius: 6px;
                background: #efe6de;
                font-family: Monaco, Menlo, Consolas, monospace;
                font-size: 14px;
              }
              .homework-markdown-preview pre {
                margin: 0 0 16px;
                padding: 16px;
                border-radius: 12px;
                background: #241d18;
                overflow: auto;
              }
              .homework-markdown-preview pre code {
                padding: 0;
                background: transparent;
                color: #f7efe9;
                font-size: 14px;
              }
              .homework-markdown-preview a {
                color: #1677ff;
                text-decoration: none;
              }
              .homework-markdown-preview strong {
                color: #2f241d;
              }
            `}</style>
            {previewContent ? (
              <div
                className="homework-markdown-preview"
                dangerouslySetInnerHTML={{ __html: previewHtml }}
              />
            ) : (
              '暂无可预览内容'
            )}
          </div>
        )}
      </Modal>

      <Modal
        open={submissionOpen}
        title={submissionHomework ? `${submissionHomework.title} · 提交列表` : '提交列表'}
        onCancel={() => setSubmissionOpen(false)}
        footer={null}
        destroyOnClose
        width={1220}
      >
        {submissionHomework ? (
          <>
            <div
              style={{
                marginBottom: 20,
                padding: 18,
                borderRadius: 16,
                background: '#faf7f4',
                border: '1px solid #f0e7df'
              }}
            >
              <div style={{ fontSize: 18, fontWeight: 700, marginBottom: 8 }}>{submissionHomework.title}</div>
              <Space size={24} wrap style={{ color: '#7f6a5f' }}>
                <span>{`${submissionHomework.campusName || '-'} 第${submissionHomework.classTerm || '-'}期`}</span>
                <span>{submissionHomework.courseContentSnapshot || '-'}</span>
                <span>截止时间：{submissionHomework.deadline ? dayjs(submissionHomework.deadline).format('YYYY-MM-DD HH:mm:ss') : '-'}</span>
                <span>提交情况：{submissionHomework.submittedCount || 0}/{submissionHomework.studentCountSnapshot || 0}</span>
              </Space>
            </div>

            <Form form={submissionSearchForm} layout="inline" className="search-form" onFinish={() => fetchSubmissionData()}>
              <Form.Item name="studentCode">
                <Input allowClear placeholder="学员编号" style={{ width: 180 }} />
              </Form.Item>
              <Form.Item name="studentName">
                <Input allowClear placeholder="学员姓名" style={{ width: 180 }} />
              </Form.Item>
              <Form.Item name="status">
                <Select allowClear placeholder="提交状态" style={{ width: 180 }} options={submissionStatusOptions as any} />
              </Form.Item>
              <Form.Item>
                <Space>
                  <Button type="primary" htmlType="submit">查询</Button>
                  <Button onClick={resetSubmissionSearch}>重置</Button>
                </Space>
              </Form.Item>
            </Form>

            <Table
              rowKey={(record) => String(record.submissionId || record.studentCode)}
              loading={submissionLoading}
              dataSource={submissionDataSource}
              pagination={{
                current: submissionPageNum,
                pageSize: submissionPageSize,
                total: submissionTotal,
                showSizeChanger: true,
                onChange: (page, size) => {
                  setSubmissionPageNum(page);
                  setSubmissionPageSize(size);
                }
              }}
              columns={[
                {
                  title: '学员',
                  dataIndex: 'studentName',
                  width: 220,
                  render: (_, record) => (
                    <div>
                      <div style={{ fontWeight: 700, marginBottom: 6 }}>{record.studentName || '-'}</div>
                      <div style={{ color: '#7f6a5f' }}>{record.studentCode}</div>
                      <div style={{ color: '#7f6a5f' }}>{record.school || '未填写学校'}</div>
                    </div>
                  )
                },
                {
                  title: '提交状态',
                  dataIndex: 'status',
                  width: 110,
                  render: (value?: HomeworkSubmissionStatusCode) => (
                    <Tag color={getSubmissionTagColor(value)} bordered={false}>
                      {getSubmissionStatusLabel(value)}
                    </Tag>
                  )
                },
                {
                  title: '提交说明',
                  dataIndex: 'submitContent',
                  width: 280,
                  render: (value?: string) => (
                    <div style={{ whiteSpace: 'pre-wrap', color: '#41342b' }}>
                      {value || '未填写提交说明'}
                    </div>
                  )
                },
                {
                  title: '提交附件',
                  dataIndex: 'attachmentName',
                  width: 180,
                  render: (_, record) => renderFileLink(record.attachmentName, record.attachmentDownloadUrl, openMarkdownPreview)
                },
                {
                  title: '提交时间',
                  dataIndex: 'submitTime',
                  width: 170,
                  render: (value?: string) => (value ? dayjs(value).format('YYYY-MM-DD HH:mm:ss') : '-')
                },
                {
                  title: '得分',
                  dataIndex: 'score',
                  width: 90,
                  render: (value?: number) => value ?? '-'
                },
                {
                  title: '评语',
                  dataIndex: 'teacherComment',
                  width: 220,
                  render: (value?: string) => value || '-'
                },
                {
                  title: '批改人',
                  dataIndex: 'reviewTeacherName',
                  width: 110,
                  render: (value?: string) => value || '-'
                },
                {
                  title: '批改时间',
                  dataIndex: 'reviewTime',
                  width: 170,
                  render: (value?: string) => (value ? dayjs(value).format('YYYY-MM-DD HH:mm:ss') : '-')
                },
                {
                  title: '操作',
                  key: 'action',
                  width: 120,
                  fixed: 'right',
                  render: (_, record) => (
                    <Space>
                      <Button
                        type="link"
                        disabled={!record.submissionId}
                        onClick={() => openReviewModal(record)}
                      >
                        批改
                      </Button>
                    </Space>
                  )
                }
              ]}
              scroll={{ x: 1600 }}
            />
          </>
        ) : null}
      </Modal>

      <Modal
        open={reviewOpen}
        title="批改作业"
        onCancel={() => setReviewOpen(false)}
        onOk={submitReview}
        okText="保存批改"
        confirmLoading={reviewSaving}
        destroyOnClose
        width={720}
      >
        {reviewRecord ? (
          <>
            <div
              style={{
                marginBottom: 20,
                padding: 16,
                borderRadius: 14,
                background: '#faf7f4',
                border: '1px solid #f0e7df'
              }}
            >
              <div style={{ fontWeight: 700, marginBottom: 8 }}>{reviewRecord.studentName}（{reviewRecord.studentCode}）</div>
              <div style={{ color: '#7f6a5f', whiteSpace: 'pre-wrap' }}>{reviewRecord.submitContent || '未填写提交说明'}</div>
              <div style={{ marginTop: 10 }}>
                {renderFileLink(reviewRecord.attachmentName, reviewRecord.attachmentDownloadUrl, openMarkdownPreview)}
              </div>
            </div>
            <Form form={reviewForm} layout="vertical">
              <Form.Item name="status" label="批改结果" rules={[{ required: true, message: '请选择批改结果' }]}>
                <Select
                  options={[
                    { label: '已批改', value: 'REVIEWED' },
                    { label: '待重交', value: 'RESUBMIT_REQUIRED' }
                  ]}
                />
              </Form.Item>
              <Form.Item
                noStyle
                shouldUpdate={(prev, next) => prev.status !== next.status}
              >
                {({ getFieldValue }) => (
                  <Form.Item
                    name="score"
                    label="得分"
                    rules={getFieldValue('status') === 'REVIEWED'
                      ? [{ required: true, message: '请填写得分' }]
                      : []}
                  >
                    <Input type="number" min={0} max={100} placeholder="0 - 100" />
                  </Form.Item>
                )}
              </Form.Item>
              <Form.Item name="teacherComment" label="评语">
                <Input.TextArea rows={6} maxLength={4000} showCount placeholder="请输入老师评语、修改建议或重交通知" />
              </Form.Item>
            </Form>
          </>
        ) : null}
      </Modal>
    </PageContainer>
  );
}

function getSubmissionStatusLabel(status?: HomeworkSubmissionStatusCode) {
  switch (status) {
    case 'SUBMITTED':
      return '待批改';
    case 'REVIEWED':
      return '已批改';
    case 'RESUBMIT_REQUIRED':
      return '待重交';
    default:
      return '未提交';
  }
}

function getSubmissionTagColor(status?: HomeworkSubmissionStatusCode) {
  switch (status) {
    case 'SUBMITTED':
      return 'gold';
    case 'REVIEWED':
      return 'green';
    case 'RESUBMIT_REQUIRED':
      return 'red';
    default:
      return 'default';
  }
}

function renderFileLink(name?: string, url?: string, onPreview?: (name?: string, url?: string) => void) {
  if (!name) {
    return <span>-</span>;
  }
  if (!url) {
    return <span>{name}</span>;
  }
  return (
    <Button type="link" style={{ padding: 0, height: 'auto' }} onClick={() => onPreview?.(name, url)}>
      {name}
    </Button>
  );
}

function normalizeUploadedUrl(value: unknown) {
  if (typeof value === 'string') {
    return value.trim();
  }
  if (value && typeof value === 'object') {
    const candidate = (value as Record<string, unknown>).fileUrl ?? (value as Record<string, unknown>).url;
    if (typeof candidate === 'string') {
      return candidate.trim();
    }
  }
  return '';
}

function renderMarkdownToHtml(markdown: string) {
  const normalized = markdown.replace(/\r\n/g, '\n').trim();
  if (!normalized) {
    return '';
  }

  const lines = normalized.split('\n');
  const html: string[] = [];
  let paragraphBuffer: string[] = [];
  let listItems: string[] = [];
  let orderedListItems: string[] = [];
  let codeBuffer: string[] = [];
  let inCodeBlock = false;

  const flushParagraph = () => {
    if (!paragraphBuffer.length) {
      return;
    }
    html.push(`<p>${renderInline(paragraphBuffer.join('<br />'))}</p>`);
    paragraphBuffer = [];
  };

  const flushList = () => {
    if (listItems.length) {
      html.push(`<ul>${listItems.map((item) => `<li>${renderInline(item)}</li>`).join('')}</ul>`);
      listItems = [];
    }
    if (orderedListItems.length) {
      html.push(`<ol>${orderedListItems.map((item) => `<li>${renderInline(item)}</li>`).join('')}</ol>`);
      orderedListItems = [];
    }
  };

  const flushCode = () => {
    if (!codeBuffer.length) {
      return;
    }
    html.push(`<pre><code>${escapeHtml(codeBuffer.join('\n'))}</code></pre>`);
    codeBuffer = [];
  };

  lines.forEach((line) => {
    const trimmed = line.trim();

    if (trimmed.startsWith('```')) {
      flushParagraph();
      flushList();
      if (inCodeBlock) {
        flushCode();
      }
      inCodeBlock = !inCodeBlock;
      return;
    }

    if (inCodeBlock) {
      codeBuffer.push(line);
      return;
    }

    if (!trimmed) {
      flushParagraph();
      flushList();
      return;
    }

    const headingMatch = trimmed.match(/^(#{1,6})\s+(.*)$/);
    if (headingMatch) {
      flushParagraph();
      flushList();
      const level = headingMatch[1].length;
      html.push(`<h${level}>${renderInline(headingMatch[2])}</h${level}>`);
      return;
    }

    const orderedMatch = trimmed.match(/^\d+\.\s+(.*)$/);
    if (orderedMatch) {
      flushParagraph();
      if (listItems.length) {
        flushList();
      }
      orderedListItems.push(orderedMatch[1]);
      return;
    }

    const unorderedMatch = trimmed.match(/^[-*]\s+(.*)$/);
    if (unorderedMatch) {
      flushParagraph();
      if (orderedListItems.length) {
        flushList();
      }
      listItems.push(unorderedMatch[1]);
      return;
    }

    paragraphBuffer.push(escapeHtml(line));
  });

  flushParagraph();
  flushList();
  flushCode();

  return html.join('');
}

function renderInline(text: string) {
  return text
    .replace(/`([^`]+)`/g, '<code>$1</code>')
    .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
    .replace(/\*([^*]+)\*/g, '<em>$1</em>')
    .replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2" target="_blank" rel="noreferrer">$1</a>');
}

function escapeHtml(text: string) {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

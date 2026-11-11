import { useEffect, useRef, useState } from 'react';
import { AudioOutlined, LinkOutlined, ReloadOutlined, FileTextOutlined, UploadOutlined, ReadOutlined, MoreOutlined } from '@ant-design/icons';
import { Button, Descriptions, Drawer, Dropdown, Form, Input, Modal, Progress, Select, Space, Table, Tag, Typography, Upload, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import type { MenuProps } from 'antd';
import type { RcFile, UploadFile } from 'antd/es/upload/interface';
import dayjs from 'dayjs';
import { generateInterviewQuestionsFromTask } from '../../api/interview-questions';
import { generateInterviewReview, getInterviewReviewDetail, getInterviewReviewPage, transcribeInterviewReview, uploadInterviewAudio, uploadInterviewResume } from '../../api/interview-reviews';
import NoPermission from '../../components/NoPermission';
import PageContainer from '../../components/PageContainer';
import PanelTable from '../../components/PanelTable';
import type { InterviewReviewQueryParams, InterviewReviewStatus, InterviewReviewTask, InterviewReviewUploadValues } from '../../types/interview-review';
import { hasPermission } from '../../utils/permission';

const { Dragger } = Upload;
const POLLING_INTERVAL_MS = 5000;
const MAX_POLLING_DURATION_MS = 3 * 60 * 1000;

const STATUS_OPTIONS: Array<{ label: string; value: InterviewReviewStatus }> = [
  { label: '已上传', value: 'UPLOADED' },
  { label: '转写中', value: 'TRANSCRIBING' },
  { label: '已转写', value: 'TRANSCRIBED' },
  { label: '复盘中', value: 'REVIEWING' },
  { label: '已完成', value: 'DONE' },
  { label: '失败', value: 'FAILED' }
];

const STATUS_META: Record<InterviewReviewStatus, { text: string; color: string }> = {
  UPLOADED: { text: '已上传', color: 'blue' },
  TRANSCRIBING: { text: '转写中', color: 'processing' },
  TRANSCRIBED: { text: '已转写', color: 'cyan' },
  REVIEWING: { text: '复盘中', color: 'processing' },
  DONE: { text: '已完成', color: 'green' },
  FAILED: { text: '失败', color: 'red' }
};

const QUESTION_STATUS_META: Record<string, { text: string; color: string }> = {
  NONE: { text: '未生成', color: 'default' },
  PROCESSING: { text: '生成中', color: 'processing' },
  DONE: { text: '已生成', color: 'green' },
  FAILED: { text: '失败', color: 'red' }
};

export default function InterviewReviewPage() {
  const canView = hasPermission('menu:system:interview-review');
  const canUpload = hasPermission('button:interview-review:upload');
  const canTranscribe = hasPermission('button:interview-review:transcribe');
  const canReview = hasPermission('button:interview-review:review') || canTranscribe;
  const canGenerateQuestion = hasPermission('button:interview-review:question-generate') || canReview;
  const [searchForm] = Form.useForm<InterviewReviewQueryParams>();
  const [uploadForm] = Form.useForm<InterviewReviewUploadValues>();
  const [loading, setLoading] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [resumeUploading, setResumeUploading] = useState(false);
  const [transcribingId, setTranscribingId] = useState<number>();
  const [reviewingId, setReviewingId] = useState<number>();
  const [questionGeneratingId, setQuestionGeneratingId] = useState<number>();
  const [uploadProgress, setUploadProgress] = useState(0);
  const [resumeUploadProgress, setResumeUploadProgress] = useState(0);
  const [uploadOpen, setUploadOpen] = useState(false);
  const [detailOpen, setDetailOpen] = useState(false);
  const [reviewOpen, setReviewOpen] = useState(false);
  const [transcriptOpen, setTranscriptOpen] = useState(false);
  const [fileList, setFileList] = useState<UploadFile[]>([]);
  const [resumeFileList, setResumeFileList] = useState<UploadFile[]>([]);
  const [records, setRecords] = useState<InterviewReviewTask[]>([]);
  const [detail, setDetail] = useState<InterviewReviewTask>();
  const [reviewDetail, setReviewDetail] = useState<InterviewReviewTask>();
  const [transcriptDetail, setTranscriptDetail] = useState<InterviewReviewTask>();
  const [pagination, setPagination] = useState({ current: 1, pageSize: 10, total: 0 });
  const pollingStartedAtRef = useRef<number>();
  const pollingStoppedNoticeRef = useRef(false);

  const fetchPage = async (pageNum = pagination.current, pageSize = pagination.pageSize) => {
    const values = searchForm.getFieldsValue();
    setLoading(true);
    try {
      const response = await getInterviewReviewPage({
        pageNum,
        pageSize,
        keyword: values.keyword?.trim(),
        status: values.status
      });
      setRecords(response.records);
      setPagination({
        current: response.pageNum,
        pageSize: response.pageSize,
        total: response.total
      });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (canView) {
      fetchPage(1, pagination.pageSize);
    }
  }, [canView]);

  useEffect(() => {
    if (!canView || !records.some((item) => item.status === 'TRANSCRIBING' || item.status === 'REVIEWING' || item.questionStatus === 'PROCESSING')) {
      pollingStartedAtRef.current = undefined;
      pollingStoppedNoticeRef.current = false;
      return;
    }
    if (!pollingStartedAtRef.current) {
      pollingStartedAtRef.current = Date.now();
      pollingStoppedNoticeRef.current = false;
    }
    const timer = window.setInterval(() => {
      const pollingStartedAt = pollingStartedAtRef.current;
      if (pollingStartedAt && Date.now() - pollingStartedAt >= MAX_POLLING_DURATION_MS) {
        window.clearInterval(timer);
        if (!pollingStoppedNoticeRef.current) {
          message.info('转写仍在后台运行，已停止自动刷新，请稍后手动刷新');
          pollingStoppedNoticeRef.current = true;
        }
        return;
      }
      fetchPage(pagination.current, pagination.pageSize);
      if (detailOpen && detail?.id) {
        getInterviewReviewDetail(detail.id).then(setDetail);
      }
      if (reviewOpen && reviewDetail?.id) {
        getInterviewReviewDetail(reviewDetail.id).then(setReviewDetail);
      }
      if (transcriptOpen && transcriptDetail?.id) {
        getInterviewReviewDetail(transcriptDetail.id).then(setTranscriptDetail);
      }
    }, POLLING_INTERVAL_MS);
    return () => window.clearInterval(timer);
  }, [
    canView,
    records,
    pagination.current,
    pagination.pageSize,
    detailOpen,
    detail?.id,
    reviewOpen,
    reviewDetail?.id,
    transcriptOpen,
    transcriptDetail?.id
  ]);

  const onSearch = () => {
    fetchPage(1, pagination.pageSize);
  };

  const onReset = () => {
    searchForm.resetFields();
    fetchPage(1, pagination.pageSize);
  };

  const onUploadSubmit = async () => {
    const values = await uploadForm.validateFields();
    const file = resolveSelectedFile(fileList[0]);
    const resumeFile = resolveSelectedFile(resumeFileList[0]);
    if (!file) {
      message.warning('请选择面试音频文件');
      return;
    }
    setUploading(true);
    setUploadProgress(0);
    try {
      await uploadInterviewAudio(file, values, setUploadProgress, resumeFile);
      message.success('上传成功，已创建复盘任务');
      setUploadOpen(false);
      uploadForm.resetFields();
      setFileList([]);
      setResumeFileList([]);
      fetchPage(1, pagination.pageSize);
    } finally {
      setUploading(false);
      setUploadProgress(0);
    }
  };

  const onOpenDetail = async (id: number) => {
    setDetailOpen(true);
    setDetail(undefined);
    const response = await getInterviewReviewDetail(id);
    setDetail(response);
  };

  const onOpenReview = async (id: number) => {
    setReviewOpen(true);
    setReviewDetail(undefined);
    const response = await getInterviewReviewDetail(id);
    setReviewDetail(response);
  };

  const onOpenTranscript = async (id: number) => {
    setTranscriptOpen(true);
    setTranscriptDetail(undefined);
    const response = await getInterviewReviewDetail(id);
    setTranscriptDetail(response);
  };

  const onUploadResumeForDetail = async (file: RcFile) => {
    if (!detail) {
      return Upload.LIST_IGNORE;
    }
    if (!isResumeFile(file)) {
      message.error(`${file.name} 不是支持的简历文件`);
      return Upload.LIST_IGNORE;
    }
    if (file.size > 20 * 1024 * 1024) {
      message.error('简历文件不能超过 20MB');
      return Upload.LIST_IGNORE;
    }
    setResumeUploading(true);
    setResumeUploadProgress(0);
    try {
      const response = await uploadInterviewResume(detail.id, file, setResumeUploadProgress);
      setDetail(response);
      message.success(detail.resumeFilename ? '简历已替换' : '简历已上传');
      fetchPage(pagination.current, pagination.pageSize);
    } finally {
      setResumeUploading(false);
      setResumeUploadProgress(0);
    }
    return Upload.LIST_IGNORE;
  };

  const onTranscribe = async (record: InterviewReviewTask, speakerDiarization = false) => {
    setTranscribingId(record.id);
    try {
      const response = await transcribeInterviewReview(record.id, speakerDiarization);
      message.success(speakerDiarization ? '已开始说话人转写' : '已开始快速转写');
      pollingStartedAtRef.current = Date.now();
      pollingStoppedNoticeRef.current = false;
      setDetail(response);
      if (detailOpen && detail?.id === record.id) {
        setDetail(response);
      }
      if (reviewOpen && reviewDetail?.id === record.id) {
        setReviewDetail(response);
      }
      fetchPage(pagination.current, pagination.pageSize);
    } finally {
      setTranscribingId(undefined);
    }
  };

  const onGenerateReview = async (record: InterviewReviewTask) => {
    setReviewingId(record.id);
    try {
      const response = await generateInterviewReview(record.id);
      message.success('已开始生成AI 面试复盘');
      pollingStartedAtRef.current = Date.now();
      pollingStoppedNoticeRef.current = false;
      if (detailOpen && detail?.id === record.id) {
        setDetail(response);
      }
      fetchPage(pagination.current, pagination.pageSize);
    } finally {
      setReviewingId(undefined);
    }
  };

  const onGenerateQuestions = async (record: InterviewReviewTask) => {
    setQuestionGeneratingId(record.id);
    try {
      await generateInterviewQuestionsFromTask(record.id);
      message.success('题库生成已开始，完成后会自动更新状态');
      pollingStartedAtRef.current = Date.now();
      pollingStoppedNoticeRef.current = false;
      fetchPage(pagination.current, pagination.pageSize);
    } finally {
      setQuestionGeneratingId(undefined);
    }
  };

  const buildActionItems = (record: InterviewReviewTask, allowReview: boolean): MenuProps['items'] => [
    {
      key: 'transcribe-fast',
      icon: <FileTextOutlined />,
      label: '快速转写',
      disabled: !['UPLOADED', 'TRANSCRIBED', 'FAILED'].includes(record.status)
    },
    {
      key: 'transcribe-speaker',
      icon: <AudioOutlined />,
      label: '说话人转写',
      disabled: !['UPLOADED', 'TRANSCRIBED', 'FAILED'].includes(record.status)
    },
    {
      key: 'generate-review',
      icon: <ReadOutlined />,
      label: '生成复盘',
      disabled: !allowReview || !['TRANSCRIBED', 'DONE'].includes(record.status)
    },
    {
      key: 'generate-questions',
      icon: <FileTextOutlined />,
      label: '生成题库',
      disabled: !canGenerateQuestion || questionGeneratingId === record.id || record.questionStatus === 'PROCESSING' || !['TRANSCRIBED', 'DONE'].includes(record.status)
    }
  ];

  const onActionMenuClick = (key: string, record: InterviewReviewTask) => {
    if (key === 'transcribe-fast') {
      onTranscribe(record, false);
      return;
    }
    if (key === 'transcribe-speaker') {
      onTranscribe(record, true);
      return;
    }
    if (key === 'generate-review') {
      onGenerateReview(record);
      return;
    }
    if (key === 'generate-questions') {
      onGenerateQuestions(record);
    }
  };

  const columns: ColumnsType<InterviewReviewTask> = [
    {
      title: '任务',
      dataIndex: 'title',
      render: (value: string, record) => (
        <Space direction="vertical" size={0}>
          <Button type="link" style={{ padding: 0 }} onClick={() => onOpenDetail(record.id)}>
            {value}
          </Button>
          <Typography.Text type="secondary">{record.originalFilename}</Typography.Text>
        </Space>
      )
    },
    {
      title: '候选人',
      dataIndex: 'candidateName',
      width: 140,
      render: (value?: string) => value || '-'
    },
    {
      title: '岗位',
      dataIndex: 'interviewRole',
      width: 120,
      render: (value?: string) => value || '-'
    },
    {
      title: '大小',
      dataIndex: 'audioSize',
      width: 110,
      render: (value: number) => formatFileSize(value)
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 120,
      render: (value: InterviewReviewStatus) => {
        const meta = STATUS_META[value] ?? { text: value, color: 'default' };
        return <Tag color={meta.color}>{meta.text}</Tag>;
      }
    },
    {
      title: '题库',
      dataIndex: 'questionStatus',
      width: 130,
      render: (value: string | undefined, record) => {
        const status = value || 'NONE';
        const meta = QUESTION_STATUS_META[status] ?? { text: status, color: 'default' };
        const text = status === 'DONE'
          ? `+${record.questionCreatedCount ?? 0} / 合并${record.questionMergedCount ?? 0}`
          : meta.text;
        return <Tag color={meta.color}>{text}</Tag>;
      }
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      width: 150,
      render: (value?: string) => formatTime(value)
    },
    {
      title: '操作',
      key: 'actions',
      width: 240,
      fixed: 'right',
      render: (_, record) => canTranscribe ? (
        <Space size={4}>
          <Button
            type="link"
            icon={<FileTextOutlined />}
            disabled={!['TRANSCRIBED', 'REVIEWING', 'DONE'].includes(record.status)}
            onClick={() => onOpenTranscript(record.id)}
          >
            转写
          </Button>
          <Button
            type="link"
            icon={<ReadOutlined />}
            disabled={!['REVIEWING', 'DONE'].includes(record.status)}
            onClick={() => onOpenReview(record.id)}
          >
            复盘
          </Button>
          <Dropdown
            menu={{ items: buildActionItems(record, canReview), onClick: ({ key }) => onActionMenuClick(key, record) }}
            trigger={['click']}
          >
            <Button type="text" icon={<MoreOutlined />} />
          </Dropdown>
        </Space>
      ) : null
    }
  ];

  if (!canView) {
    return <NoPermission />;
  }

  return (
    <PageContainer
      title="AI 面试复盘"
      description="上传学员面试录音，结合求职简历完成转写与 AI 复盘，整理改进建议和训练题目。"
      extra={canUpload ? (
        <Button type="primary" icon={<UploadOutlined />} onClick={() => setUploadOpen(true)}>
          上传音频
        </Button>
      ) : null}
    >
      <Form form={searchForm} layout="inline" className="search-form" onFinish={onSearch}>
        <Form.Item name="keyword" style={{ minWidth: 260 }}>
          <Input placeholder="标题 / 候选人 / 岗位 / 文件名" allowClear />
        </Form.Item>
        <Form.Item name="status" style={{ minWidth: 160 }}>
          <Select placeholder="状态" allowClear options={STATUS_OPTIONS} />
        </Form.Item>
        <Form.Item>
          <Space>
            <Button type="primary" htmlType="submit">
              查询
            </Button>
            <Button onClick={onReset}>重置</Button>
            <Button icon={<ReloadOutlined />} onClick={() => fetchPage()}>
              刷新
            </Button>
          </Space>
        </Form.Item>
      </Form>

      <PanelTable>
        <Table
          rowKey="id"
          loading={loading}
          columns={columns}
          dataSource={records}
          pagination={{
            current: pagination.current,
            pageSize: pagination.pageSize,
            total: pagination.total,
            showSizeChanger: true,
            showTotal: (total) => `共 ${total} 条`,
            onChange: (page, pageSize) => fetchPage(page, pageSize)
          }}
        />
      </PanelTable>

      <Modal
        title="上传面试音频"
        open={uploadOpen}
        confirmLoading={uploading}
        onOk={onUploadSubmit}
        onCancel={() => {
          setUploadOpen(false);
          uploadForm.resetFields();
          setFileList([]);
          setResumeFileList([]);
          setUploadProgress(0);
        }}
        okText="上传"
      >
        <Form form={uploadForm} layout="vertical">
          <Form.Item name="title" label="任务标题">
            <Input placeholder="默认使用文件名" maxLength={100} allowClear />
          </Form.Item>
          <Form.Item name="candidateName" label="候选人">
            <Input placeholder="可选" maxLength={50} allowClear />
          </Form.Item>
          <Form.Item name="interviewRole" label="面试岗位">
            <Input placeholder="可选" maxLength={50} allowClear />
          </Form.Item>
          <Dragger
            maxCount={1}
            accept=".mp3,.wav,.m4a,.aac,.flac,.ogg,.opus,.webm,.mp4,audio/*"
            fileList={fileList}
            beforeUpload={(file) => {
              if (!isAudioFile(file)) {
                message.error(`${file.name} 不是支持的音频文件`);
                return Upload.LIST_IGNORE;
              }
              if (file.size > 500 * 1024 * 1024) {
                message.error('音频文件不能超过 500MB');
                return Upload.LIST_IGNORE;
              }
              setFileList([file]);
              return false;
            }}
            onRemove={() => {
              setFileList([]);
            }}
          >
            <p className="ant-upload-drag-icon">
              <AudioOutlined />
            </p>
            <p className="ant-upload-text">拖拽或点击选择音频文件</p>
            <p className="ant-upload-hint">支持 mp3、wav、m4a、aac、flac、ogg、opus、webm、mp4，单文件不超过 500MB</p>
          </Dragger>
          <div style={{ marginTop: 16 }}>
            <Dragger
              maxCount={1}
              accept=".pdf,.doc,.docx,.txt,.md,application/pdf,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document,text/*"
              fileList={resumeFileList}
              beforeUpload={(file) => {
                if (!isResumeFile(file)) {
                  message.error(`${file.name} 不是支持的简历文件`);
                  return Upload.LIST_IGNORE;
                }
                if (file.size > 20 * 1024 * 1024) {
                  message.error('简历文件不能超过 20MB');
                  return Upload.LIST_IGNORE;
                }
                setResumeFileList([file]);
                return false;
              }}
              onRemove={() => {
                setResumeFileList([]);
              }}
            >
              <p className="ant-upload-drag-icon">
                <FileTextOutlined />
              </p>
              <p className="ant-upload-text">可选：上传候选人简历</p>
              <p className="ant-upload-hint">支持 pdf、doc、docx、txt、md，单文件不超过 20MB</p>
            </Dragger>
          </div>
          {uploading ? <Progress percent={uploadProgress} style={{ marginTop: 16 }} /> : null}
        </Form>
      </Modal>

      <Drawer
        title="复盘任务详情"
        open={detailOpen}
        width={720}
        onClose={() => setDetailOpen(false)}
      >
        {detail ? (
          <Space direction="vertical" size="large" style={{ width: '100%' }}>
            <Descriptions column={1} bordered size="small">
              <Descriptions.Item label="任务标题">{detail.title}</Descriptions.Item>
              <Descriptions.Item label="候选人">{detail.candidateName || '-'}</Descriptions.Item>
              <Descriptions.Item label="面试岗位">{detail.interviewRole || '-'}</Descriptions.Item>
              <Descriptions.Item label="原始文件">{detail.originalFilename}</Descriptions.Item>
              <Descriptions.Item label="对象 Key">{detail.audioObjectKey}</Descriptions.Item>
              <Descriptions.Item label="文件大小">{formatFileSize(detail.audioSize)}</Descriptions.Item>
              <Descriptions.Item label="简历文件">{detail.resumeFilename || '-'}</Descriptions.Item>
              <Descriptions.Item label="简历大小">{formatFileSize(detail.resumeSize)}</Descriptions.Item>
              <Descriptions.Item label="状态">
                <Tag color={STATUS_META[detail.status]?.color}>{STATUS_META[detail.status]?.text ?? detail.status}</Tag>
              </Descriptions.Item>
              <Descriptions.Item label="题库状态">
                <Space wrap>
                  <Tag color={QUESTION_STATUS_META[detail.questionStatus || 'NONE']?.color}>
                    {QUESTION_STATUS_META[detail.questionStatus || 'NONE']?.text ?? detail.questionStatus ?? '未生成'}
                  </Tag>
                  {detail.questionStatus === 'DONE' ? (
                    <Typography.Text type="secondary">
                      抽取 {detail.questionExtractedCount ?? 0} 个，新增 {detail.questionCreatedCount ?? 0} 个，合并 {detail.questionMergedCount ?? 0} 个
                    </Typography.Text>
                  ) : null}
                </Space>
              </Descriptions.Item>
              {detail.questionFailReason ? (
                <Descriptions.Item label="题库失败原因">{detail.questionFailReason}</Descriptions.Item>
              ) : null}
              <Descriptions.Item label="创建时间">{formatTime(detail.createdAt)}</Descriptions.Item>
              <Descriptions.Item label="更新时间">{formatTime(detail.updatedAt)}</Descriptions.Item>
            </Descriptions>
            <Space direction="vertical" size="small" style={{ width: '100%' }}>
              <Typography.Text strong>面试音频</Typography.Text>
              <audio controls src={detail.audioPreviewUrl || detail.audioUrl} style={{ width: '100%' }}>
                <track kind="captions" />
              </audio>
              <Button icon={<LinkOutlined />} href={detail.audioPreviewUrl || detail.audioUrl} target="_blank" rel="noreferrer">
                打开音频
              </Button>
            </Space>
            {detail.resumeFilename ? (
              <Space direction="vertical" size="small" style={{ width: '100%' }}>
                <Typography.Text strong>候选人简历</Typography.Text>
                <Space wrap>
                  <Button icon={<FileTextOutlined />} href={detail.resumePreviewUrl || detail.resumeUrl} target="_blank" rel="noreferrer">
                    打开简历
                  </Button>
                  {canUpload ? (
                    <Upload
                      showUploadList={false}
                      accept=".pdf,.doc,.docx,.txt,.md,application/pdf,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document,text/*"
                      beforeUpload={onUploadResumeForDetail}
                    >
                      <Button loading={resumeUploading} icon={<UploadOutlined />}>替换简历</Button>
                    </Upload>
                  ) : null}
                </Space>
                {resumeUploading ? <Progress percent={resumeUploadProgress} size="small" /> : null}
              </Space>
            ) : canUpload ? (
              <Space direction="vertical" size="small" style={{ width: '100%' }}>
                <Typography.Text strong>候选人简历</Typography.Text>
                <Upload
                  showUploadList={false}
                  accept=".pdf,.doc,.docx,.txt,.md,application/pdf,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document,text/*"
                  beforeUpload={onUploadResumeForDetail}
                >
                  <Button loading={resumeUploading} icon={<UploadOutlined />}>上传简历</Button>
                </Upload>
                {resumeUploading ? <Progress percent={resumeUploadProgress} size="small" /> : null}
              </Space>
            ) : null}
            {detail.transcriptText ? (
              <Button icon={<FileTextOutlined />} onClick={() => onOpenTranscript(detail.id)}>打开转写对话</Button>
            ) : null}
            {detail.reviewReport ? <Button icon={<ReadOutlined />} onClick={() => onOpenReview(detail.id)}>打开复盘报告</Button> : null}
            {detail.failReason ? (
              <Typography.Text type="danger">{detail.failReason}</Typography.Text>
            ) : null}
          </Space>
        ) : (
          <Typography.Text type="secondary">加载中...</Typography.Text>
        )}
      </Drawer>

      <Drawer
        title="AI 面试复盘报告"
        open={reviewOpen}
        width="72vw"
        onClose={() => setReviewOpen(false)}
      >
        {reviewDetail ? (
          <Space direction="vertical" size="large" style={{ width: '100%' }}>
            <Descriptions column={2} bordered size="small">
              <Descriptions.Item label="任务标题">{reviewDetail.title}</Descriptions.Item>
              <Descriptions.Item label="状态">
                <Tag color={STATUS_META[reviewDetail.status]?.color}>{STATUS_META[reviewDetail.status]?.text ?? reviewDetail.status}</Tag>
              </Descriptions.Item>
              <Descriptions.Item label="候选人">{reviewDetail.candidateName || '-'}</Descriptions.Item>
              <Descriptions.Item label="面试岗位">{reviewDetail.interviewRole || '-'}</Descriptions.Item>
            </Descriptions>
            {reviewDetail.reviewReport ? (
              <div style={{ fontSize: 15, lineHeight: 1.9 }}>
                {renderMarkdownReport(reviewDetail.reviewReport)}
              </div>
            ) : (
              <Typography.Text type="secondary">
                {reviewDetail.status === 'REVIEWING' ? '复盘报告生成中...' : '暂无复盘报告'}
              </Typography.Text>
            )}
          </Space>
        ) : (
          <Typography.Text type="secondary">加载中...</Typography.Text>
        )}
      </Drawer>

      <Drawer
        title="面试转写对话"
        open={transcriptOpen}
        width="68vw"
        onClose={() => setTranscriptOpen(false)}
      >
        {transcriptDetail ? (
          <Space direction="vertical" size="large" style={{ width: '100%' }}>
            <Descriptions column={2} bordered size="small">
              <Descriptions.Item label="任务标题">{transcriptDetail.title}</Descriptions.Item>
              <Descriptions.Item label="状态">
                <Tag color={STATUS_META[transcriptDetail.status]?.color}>{STATUS_META[transcriptDetail.status]?.text ?? transcriptDetail.status}</Tag>
              </Descriptions.Item>
              <Descriptions.Item label="候选人">{transcriptDetail.candidateName || '-'}</Descriptions.Item>
              <Descriptions.Item label="面试岗位">{transcriptDetail.interviewRole || '-'}</Descriptions.Item>
            </Descriptions>
            {transcriptDetail.transcriptText ? (
              <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
                {parseTranscriptLines(transcriptDetail.transcriptText).map((item, index) => (
                  <div
                    key={`${item.time}-${index}`}
                    style={{
                      alignSelf: item.speaker === '说话人1' ? 'flex-start' : 'flex-end',
                      maxWidth: '78%'
                    }}
                  >
                    <div style={{ marginBottom: 4, color: '#8c8c8c', fontSize: 12 }}>
                      {item.time ? `${item.time} · ` : null}{item.speaker || '未知说话人'}
                    </div>
                    <div
                      style={{
                        padding: '10px 14px',
                        borderRadius: 8,
                        background: item.speaker === '说话人1' ? '#f5f7fa' : '#e6f4ff',
                        border: '1px solid #edf0f5',
                        whiteSpace: 'pre-wrap',
                        lineHeight: 1.8
                      }}
                    >
                      {item.text}
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <Typography.Text type="secondary">暂无转写文本</Typography.Text>
            )}
          </Space>
        ) : (
          <Typography.Text type="secondary">加载中...</Typography.Text>
        )}
      </Drawer>
    </PageContainer>
  );
}

function isAudioFile(file: RcFile) {
  const lowerName = file.name.toLowerCase();
  const validExtension = ['.mp3', '.wav', '.m4a', '.aac', '.flac', '.ogg', '.opus', '.webm', '.mp4']
    .some((suffix) => lowerName.endsWith(suffix));
  return validExtension || file.type.startsWith('audio/');
}

function isResumeFile(file: RcFile) {
  const lowerName = file.name.toLowerCase();
  const validExtension = ['.pdf', '.doc', '.docx', '.txt', '.md'].some((suffix) => lowerName.endsWith(suffix));
  const validContentType = file.type === ''
    || file.type === 'application/pdf'
    || file.type === 'application/msword'
    || file.type === 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
    || file.type.startsWith('text/');
  return validExtension && validContentType;
}

function resolveSelectedFile(file?: UploadFile) {
  return file?.originFileObj ?? (file as RcFile | undefined);
}

function formatFileSize(value?: number) {
  if (!value) {
    return '-';
  }
  if (value < 1024 * 1024) {
    return `${(value / 1024).toFixed(1)} KB`;
  }
  return `${(value / 1024 / 1024).toFixed(1)} MB`;
}

function formatTime(value?: string) {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm:ss') : '-';
}

function renderMarkdownReport(markdown: string) {
  return markdown
    .split(/\n+/)
    .map((line, index) => {
      const text = line.trim();
      if (!text) {
        return null;
      }
      if (text.startsWith('## ')) {
        return (
          <Typography.Title key={index} level={4} style={{ marginTop: index === 0 ? 0 : 28, marginBottom: 12 }}>
            {renderInlineMarkdown(text.slice(3))}
          </Typography.Title>
        );
      }
      if (text.startsWith('### ')) {
        return (
          <Typography.Title key={index} level={5} style={{ marginTop: 20, marginBottom: 8 }}>
            {renderInlineMarkdown(text.slice(4))}
          </Typography.Title>
        );
      }
      const orderedMatch = text.match(/^(\d+)\.\s+(.*)$/);
      if (orderedMatch) {
        return (
          <Typography.Paragraph key={index} style={{ marginBottom: 8 }}>
            <Typography.Text strong>{orderedMatch[1]}.</Typography.Text> {renderInlineMarkdown(orderedMatch[2])}
          </Typography.Paragraph>
        );
      }
      if (text.startsWith('- ')) {
        return (
          <Typography.Paragraph key={index} style={{ marginBottom: 6, paddingLeft: 16 }}>
            <span style={{ marginLeft: -14, marginRight: 8 }}>•</span>
            {renderInlineMarkdown(text.slice(2))}
          </Typography.Paragraph>
        );
      }
      return (
        <Typography.Paragraph key={index} style={{ marginBottom: 10 }}>
          {renderInlineMarkdown(text)}
        </Typography.Paragraph>
      );
    });
}

function renderInlineMarkdown(text: string) {
  const parts = text.split(/(\*\*[^*]+\*\*)/g);
  return parts.map((part, index) => {
    if (part.startsWith('**') && part.endsWith('**')) {
      return <Typography.Text key={index} strong>{part.slice(2, -2)}</Typography.Text>;
    }
    return <span key={index}>{part}</span>;
  });
}

function parseTranscriptLines(text: string) {
  return text
    .split('\n')
    .map((line) => {
      const matched = line.match(/^\[([^\]]+)]\s*([^：:]+)[：:]\s*(.*)$/);
      if (matched) {
        return {
          time: matched[1],
          speaker: matched[2].trim(),
          text: matched[3].trim()
        };
      }
      return {
        time: '',
        speaker: '',
        text: line.trim()
      };
    })
    .filter((item) => item.text);
}

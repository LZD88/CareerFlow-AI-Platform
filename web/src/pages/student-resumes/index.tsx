import { useEffect, useMemo, useState } from 'react';
import { DeleteOutlined, DownloadOutlined, EyeOutlined, FilePdfOutlined, FileTextOutlined, ReloadOutlined } from '@ant-design/icons';
import { Button, Collapse, Descriptions, Drawer, Form, Input, Popconfirm, Select, Space, Table, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import {
  deleteStudentResume,
  getStudentResumeDetail,
  getStudentResumeFilePreview,
  getStudentResumePage
} from '../../api/student-resumes';
import NoPermission from '../../components/NoPermission';
import PageContainer from '../../components/PageContainer';
import PanelTable from '../../components/PanelTable';
import type { StudentResume, StudentResumeQueryParams } from '../../types/student-resume';
import { hasPermission } from '../../utils/permission';

const PARSE_STATUS_META: Record<string, { label: string; color: string }> = {
  PENDING: { label: '待解析', color: 'gold' },
  SUCCESS: { label: '已整理', color: 'green' },
  FAILED: { label: '解析失败', color: 'red' }
};

export default function StudentResumeAdminPage() {
  const canView = hasPermission('menu:student:resume') || hasPermission('student:resume:view');
  const canDelete = hasPermission('student:resume:delete');
  const [form] = Form.useForm<StudentResumeQueryParams>();
  const [loading, setLoading] = useState(false);
  const [records, setRecords] = useState<StudentResume[]>([]);
  const [pagination, setPagination] = useState({ current: 1, pageSize: 10, total: 0 });
  const [detailOpen, setDetailOpen] = useState(false);
  const [detail, setDetail] = useState<StudentResume>();
  const [previewUrl, setPreviewUrl] = useState<string>();
  const [previewLoading, setPreviewLoading] = useState(false);

  const stats = useMemo(() => ({
    defaultCount: records.filter((item) => item.defaultResume).length,
    roleCount: new Set(records.map((item) => item.targetRole).filter(Boolean)).size
  }), [records]);

  const fetchPage = async (pageNum = pagination.current, pageSize = pagination.pageSize) => {
    const values = form.getFieldsValue();
    setLoading(true);
    try {
      const response = await getStudentResumePage({
        pageNum,
        pageSize,
        keyword: values.keyword?.trim(),
        targetRole: values.targetRole?.trim(),
        defaultResume: values.defaultResume
      });
      setRecords(response.records);
      setPagination({ current: response.pageNum, pageSize: response.pageSize, total: response.total });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchPage(1, pagination.pageSize);
  }, []);

  useEffect(() => () => {
    if (previewUrl) {
      URL.revokeObjectURL(previewUrl);
    }
  }, [previewUrl]);

  const openDetail = async (id: number) => {
    setDetailOpen(true);
    setDetail(undefined);
    setPreviewUrl(undefined);
    const nextDetail = await getStudentResumeDetail(id);
    setDetail(nextDetail);
    if (isPdfResume(nextDetail)) {
      setPreviewLoading(true);
      try {
        const blob = await getStudentResumeFilePreview(id);
        setPreviewUrl(URL.createObjectURL(blob));
      } finally {
        setPreviewLoading(false);
      }
    }
  };

  const remove = async (id: number) => {
    await deleteStudentResume(id);
    await fetchPage();
  };

  const columns: ColumnsType<StudentResume> = [
    {
      title: '简历',
      dataIndex: 'resumeName',
      render: (value: string, record) => (
        <Button type="link" style={{ padding: 0, height: 'auto', whiteSpace: 'normal', textAlign: 'left' }} onClick={() => openDetail(record.id)}>
          {value}
        </Button>
      )
    },
    {
      title: '学员',
      width: 180,
      render: (_, record) => (
        <Space direction="vertical" size={0}>
          <Typography.Text strong>{record.studentName}</Typography.Text>
          <Typography.Text type="secondary">{record.studentCode}</Typography.Text>
        </Space>
      )
    },
    {
      title: '目标岗位',
      dataIndex: 'targetRole',
      width: 180,
      render: (value?: string) => value ? <Tag color="blue">{value}</Tag> : '-'
    },
    {
      title: '状态',
      width: 150,
      render: (_, record) => (
        <Space>
          {record.defaultResume ? <Tag color="gold">默认</Tag> : null}
          <Tag color={PARSE_STATUS_META[record.parseStatus]?.color || 'default'}>
            {PARSE_STATUS_META[record.parseStatus]?.label || record.parseStatus}
          </Tag>
        </Space>
      )
    },
    {
      title: '更新时间',
      dataIndex: 'updatedAt',
      width: 170,
      render: (value?: string) => value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-'
    },
    {
      title: '操作',
      width: 100,
      render: (_, record) => canDelete ? (
        <Popconfirm title="确认删除这份简历？" onConfirm={() => remove(record.id)}>
          <Button danger type="link" icon={<DeleteOutlined />}>删除</Button>
        </Popconfirm>
      ) : null
    }
  ];

  if (!canView) {
    return <NoPermission />;
  }

  return (
    <PageContainer
      title="求职简历"
      description="管理学员求职简历、目标岗位与项目经历，为就业辅导和 AI 面试训练提供依据。"
      extra={<FileTextOutlined style={{ fontSize: 24, color: '#1677ff' }} />}
    >
      <div className="resume-admin-summary">
        <div><span>当前页简历</span><strong>{records.length}</strong></div>
        <div><span>默认简历</span><strong>{stats.defaultCount}</strong></div>
        <div><span>岗位方向</span><strong>{stats.roleCount}</strong></div>
      </div>

      <Form form={form} layout="inline" className="search-form" onFinish={() => fetchPage(1, pagination.pageSize)}>
        <Form.Item name="keyword" style={{ minWidth: 280 }}>
          <Input placeholder="学员 / 学员编号 / 简历 / 内容" allowClear />
        </Form.Item>
        <Form.Item name="targetRole" style={{ minWidth: 220 }}>
          <Input placeholder="目标岗位" allowClear />
        </Form.Item>
        <Form.Item name="defaultResume" style={{ minWidth: 150 }}>
          <Select
            placeholder="默认状态"
            allowClear
            options={[
              { label: '默认简历', value: true },
              { label: '普通简历', value: false }
            ]}
          />
        </Form.Item>
        <Form.Item>
          <Space>
            <Button type="primary" htmlType="submit">查询</Button>
            <Button onClick={() => { form.resetFields(); fetchPage(1, pagination.pageSize); }}>重置</Button>
            <Button icon={<ReloadOutlined />} onClick={() => fetchPage()}>刷新</Button>
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

      <Drawer
        title="简历详情"
        open={detailOpen}
        width={900}
        onClose={() => {
          setDetailOpen(false);
          if (previewUrl) {
            URL.revokeObjectURL(previewUrl);
          }
          setPreviewUrl(undefined);
        }}
      >
        {detail ? (
          <Space direction="vertical" size="large" style={{ width: '100%' }}>
            <Descriptions column={2} bordered>
              <Descriptions.Item label="学员">{detail.studentName}</Descriptions.Item>
              <Descriptions.Item label="学员编号">{detail.studentCode}</Descriptions.Item>
              <Descriptions.Item label="简历名称">{detail.resumeName}</Descriptions.Item>
              <Descriptions.Item label="目标岗位">{detail.targetRole || '-'}</Descriptions.Item>
              <Descriptions.Item label="默认简历">{detail.defaultResume ? '是' : '否'}</Descriptions.Item>
              <Descriptions.Item label="更新时间">{dayjs(detail.updatedAt).format('YYYY-MM-DD HH:mm')}</Descriptions.Item>
            </Descriptions>
            {detail.resumeSummary ? (
              <section className="resume-admin-detail-block">
                <Typography.Title level={5}>简历摘要</Typography.Title>
                <Typography.Paragraph>{detail.resumeSummary}</Typography.Paragraph>
              </section>
            ) : null}
            {detail.resumeFileUrl ? (
              <section className="resume-admin-detail-block">
                <div className="resume-admin-preview-head">
                  <Typography.Title level={5}>
                    <FilePdfOutlined /> 原文件预览
                  </Typography.Title>
                  <Space>
                    <Button
                      icon={<EyeOutlined />}
                      disabled={!previewUrl && isPdfResume(detail)}
                      loading={previewLoading}
                      onClick={() => {
                        const url = previewUrl || detail.resumeFilePreviewUrl;
                        if (url) {
                          window.open(url, '_blank', 'noopener,noreferrer');
                        }
                      }}
                    >
                      新窗口打开
                    </Button>
                    <Button
                      icon={<DownloadOutlined />}
                      onClick={() => window.open(detail.resumeFilePreviewUrl, '_blank', 'noopener,noreferrer')}
                    >
                      下载/查看
                    </Button>
                  </Space>
                </div>
                {isPdfResume(detail) && previewUrl ? (
                  <iframe
                    className="resume-admin-pdf-preview"
                    title={`${detail.resumeName} 原文件预览`}
                    src={previewUrl}
                  />
                ) : isPdfResume(detail) && previewLoading ? (
                  <div className="resume-admin-file-fallback">
                    <FilePdfOutlined />
                    <span>正在加载 PDF 预览...</span>
                  </div>
                ) : (
                  <div className="resume-admin-file-fallback">
                    <FileTextOutlined />
                    <span>当前文件不是 PDF，浏览器不能直接嵌入预览，可以点击上方按钮打开或下载查看。</span>
                  </div>
                )}
              </section>
            ) : null}
            {detail.resumeFileUrl ? (
              <Collapse
                ghost
                items={[
                  {
                    key: 'resumeText',
                    label: '查看解析文本',
                    children: (
                      <section className="resume-admin-detail-block">
                        <pre>{detail.resumeText}</pre>
                      </section>
                    )
                  }
                ]}
              />
            ) : (
              <section className="resume-admin-detail-block">
                <Typography.Title level={5}>完整简历</Typography.Title>
                <pre>{detail.resumeText}</pre>
              </section>
            )}
          </Space>
        ) : (
          <Typography.Text type="secondary">加载中...</Typography.Text>
        )}
      </Drawer>
    </PageContainer>
  );
}

function isPdfResume(detail: StudentResume) {
  const url = detail.resumeFileUrl || detail.resumeFilePreviewUrl || '';
  return url.toLowerCase().includes('.pdf');
}

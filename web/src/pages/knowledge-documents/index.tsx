import { useEffect, useMemo, useState } from 'react';
import { DatabaseOutlined, EditOutlined, PlusOutlined, ReloadOutlined, StopOutlined, UploadOutlined } from '@ant-design/icons';
import { Button, Drawer, Form, Input, Modal, Popconfirm, Select, Space, Switch, Table, Tabs, Tag, Typography, Upload, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import type { RcFile, UploadFile } from 'antd/es/upload/interface';
import dayjs from 'dayjs';
import NoPermission from '../../components/NoPermission';
import PageContainer from '../../components/PageContainer';
import PanelTable from '../../components/PanelTable';
import { getCoursePage } from '../../api/courses';
import {
  createKnowledgeDocument,
  disableKnowledgeDocument,
  getKnowledgeDocumentChunks,
  getKnowledgeDocumentDetail,
  getKnowledgeDocumentPage,
  reindexKnowledgeDocument,
  updateKnowledgeDocument,
  uploadJsonKnowledgeDocuments,
  uploadMarkdownKnowledgeDocuments
} from '../../api/knowledge-documents';
import type { CourseEntity } from '../../types/system';
import type { KnowledgeDocumentChunk, KnowledgeDocumentCreateReq, KnowledgeDocumentEntity } from '../../types/knowledge-base';
import { hasPermission } from '../../utils/permission';

const statusOptions = [
  { label: '待入库', value: 'PENDING' },
  { label: '入库中', value: 'INDEXING' },
  { label: '已入库', value: 'INDEXED' },
  { label: '入库失败', value: 'FAILED' },
  { label: '已禁用', value: 'DISABLED' }
];

const statusColor: Record<string, string> = {
  PENDING: 'warning',
  INDEXING: 'processing',
  INDEXED: 'success',
  FAILED: 'error',
  DISABLED: 'default'
};

const statusText: Record<string, string> = {
  PENDING: '待入库',
  INDEXING: '入库中',
  INDEXED: '已入库',
  FAILED: '入库失败',
  DISABLED: '已禁用'
};

const studyModeOptions = [
  { label: '全部学员', value: 'ALL' },
  { label: '线上学员', value: 'ONLINE' },
  { label: '线下学员', value: 'OFFLINE' }
];

const studyModeText: Record<string, string> = {
  ALL: '全部学员',
  ONLINE: '线上学员',
  OFFLINE: '线下学员'
};

interface SearchValues {
  keyword?: string;
  status?: string;
  courseId?: number;
  studyMode?: string;
}

interface KnowledgeDocumentPageProps {
  mode?: KnowledgeMode;
}

type KnowledgeMode = 'course' | 'code' | 'dataSchema' | 'dataMetric';
type UploadKind = 'none' | 'markdown' | 'json';

const pageModeConfig = {
  course: {
    title: '知识库管理',
    description: '维护学生问答 RAG 知识文档，新增后会同步切片、向量化并写入 Milvus。',
    listTitle: '知识文档列表',
    tabLabel: '学生问答',
    canViewCode: 'menu:system:knowledge-document',
    permissionScope: 'course',
    sourceType: undefined,
    createSourceType: 'manual',
    createPermissionScope: 'course',
    showCourseFilters: true,
    uploadKind: 'none'
  },
  code: {
    title: '代码知识库',
    description: '维护日志诊断使用的 Java 代码知识文档，按方法切片并写入 Milvus。',
    listTitle: '代码文档列表',
    tabLabel: '代码诊断',
    canViewCode: 'menu:system:code-knowledge',
    permissionScope: 'log_diagnosis',
    sourceType: 'generated_java_md',
    createSourceType: 'generated_java_md',
    createPermissionScope: 'log_diagnosis',
    showCourseFilters: false,
    uploadKind: 'markdown'
  },
  dataSchema: {
    title: '数据分析表结构',
    description: '维护 Text-to-SQL 使用的表结构说明，后续按“一张表一个文档”写入向量库。',
    listTitle: '表结构文档列表',
    tabLabel: '数据分析表结构',
    canViewCode: 'menu:system:knowledge-document',
    permissionScope: 'data_analysis_schema',
    sourceType: 'data_analysis_schema_md',
    createSourceType: 'data_analysis_schema_md',
    createPermissionScope: 'data_analysis_schema',
    showCourseFilters: false,
    uploadKind: 'markdown'
  },
  dataMetric: {
    title: '数据分析业务知识',
    description: '维护数据分析指标口径 JSON，后续按“一个指标一个 Chunk”写入向量库。',
    listTitle: '业务指标文档列表',
    tabLabel: '数据分析业务知识',
    canViewCode: 'menu:system:knowledge-document',
    permissionScope: 'data_analysis_metric',
    sourceType: 'data_analysis_metric_json',
    createSourceType: 'data_analysis_metric_json',
    createPermissionScope: 'data_analysis_metric',
    showCourseFilters: false,
    uploadKind: 'json'
  }
} satisfies Record<KnowledgeMode, {
  title: string;
  description: string;
  listTitle: string;
  tabLabel: string;
  canViewCode: string;
  permissionScope: string;
  sourceType?: string;
  createSourceType: string;
  createPermissionScope: string;
  showCourseFilters: boolean;
  uploadKind: UploadKind;
}>;

export default function KnowledgeDocumentPage({ mode = 'course' }: KnowledgeDocumentPageProps) {
  // 多类知识库复用同一套表格/详情/Chunk 抽屉，通过固定过滤条件隔开业务数据。
  const [activeMode, setActiveMode] = useState<KnowledgeMode>(mode);
  const config = pageModeConfig[activeMode];
  const isCodeMode = activeMode === 'code';
  const isCourseMode = activeMode === 'course';
  const isMetricMode = activeMode === 'dataMetric';
  const canView = hasPermission(config.canViewCode);
  const canCreate = hasPermission(resolveUploadPermissionCode(config.uploadKind));
  const canUpdate = hasPermission('knowledge-document:update');
  const canReindex = hasPermission('knowledge-document:reindex');
  const canDisable = hasPermission('knowledge-document:disable');

  const [searchForm] = Form.useForm<SearchValues>();
  const [editForm] = Form.useForm<KnowledgeDocumentCreateReq>();
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [open, setOpen] = useState(false);
  const [editingId, setEditingId] = useState<number>();
  const [dataSource, setDataSource] = useState<KnowledgeDocumentEntity[]>([]);
  const [pageNum, setPageNum] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [total, setTotal] = useState(0);
  const [detailOpen, setDetailOpen] = useState(false);
  const [detail, setDetail] = useState<KnowledgeDocumentEntity>();
  const [chunkOpen, setChunkOpen] = useState(false);
  const [chunkDocument, setChunkDocument] = useState<KnowledgeDocumentEntity>();
  const [chunkLoading, setChunkLoading] = useState(false);
  const [chunkDataSource, setChunkDataSource] = useState<KnowledgeDocumentChunk[]>([]);
  const [chunkPageNum, setChunkPageNum] = useState(1);
  const [chunkPageSize, setChunkPageSize] = useState(10);
  const [chunkTotal, setChunkTotal] = useState(0);
  const [courseOptions, setCourseOptions] = useState<CourseEntity[]>([]);
  const [uploadOpen, setUploadOpen] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [uploadIndexNow, setUploadIndexNow] = useState(true);
  const [uploadFileList, setUploadFileList] = useState<UploadFile[]>([]);

  const courseSelectOptions = useMemo(
    () => courseOptions.map((item) => ({ label: `${item.courseName}（ID:${item.id}）`, value: item.id })),
    [courseOptions]
  );

  const courseNameMap = useMemo(
    () => new Map(courseOptions.map((item) => [item.id, item.courseName])),
    [courseOptions]
  );
  const detailMarkdownHtml = useMemo(
    () => (isMarkdownSource(detail?.sourceType) ? renderMarkdownToHtml(detail?.content || '') : ''),
    [detail]
  );

  const fetchData = async () => {
    setLoading(true);
    try {
      const values = searchForm.getFieldsValue();
      const result = await getKnowledgeDocumentPage({
        pageNum,
        pageSize,
        ...values,
        // 页面级固定过滤，避免代码知识库和学生课程知识库混在一个列表里。
        permissionScope: config.permissionScope,
        sourceType: config.sourceType
      });
      setDataSource(result.records);
      setTotal(result.total);
    } finally {
      setLoading(false);
    }
  };

  const fetchSelectOptions = async () => {
    const courseResult = await getCoursePage({ pageNum: 1, pageSize: 1000, status: 1 });
    setCourseOptions(courseResult.records);
  };

  useEffect(() => {
    if (!canView) {
      return;
    }
    // 只有学生问答知识库绑定课程，其他知识域避免多余的课程下拉请求。
    if (isCourseMode) {
      fetchSelectOptions();
    }
    fetchData();
  }, [canView, pageNum, pageSize, activeMode]);

  const onModeChange = (nextMode: string) => {
    setActiveMode(nextMode as KnowledgeMode);
    searchForm.resetFields();
    setPageNum(1);
    setPageSize(10);
    setDataSource([]);
    setTotal(0);
  };

  const onSearch = () => {
    if (pageNum !== 1) {
      setPageNum(1);
      return;
    }
    fetchData();
  };

  const onReset = () => {
    searchForm.resetFields();
    if (pageNum !== 1) {
      setPageNum(1);
      return;
    }
    fetchData();
  };

  const onCreate = () => {
    setEditingId(undefined);
    editForm.resetFields();
    editForm.setFieldsValue({
      sourceType: config.createSourceType,
      permissionScope: config.createPermissionScope,
      studyMode: 'ALL'
    });
    setOpen(true);
  };

  const onUploadSubmit = async () => {
    const files = uploadFileList
      .map((item) => item.originFileObj)
      .filter((item): item is RcFile => Boolean(item));
    if (!files.length) {
      message.warning(config.uploadKind === 'json' ? '请选择 JSON 文件' : '请选择 Markdown 文件');
      return;
    }
    setUploading(true);
    try {
      const result =
        config.uploadKind === 'json'
          ? await uploadJsonKnowledgeDocuments(files, uploadIndexNow, {
              sourceType: config.createSourceType,
              permissionScope: config.createPermissionScope
            })
          : await uploadMarkdownKnowledgeDocuments(files, uploadIndexNow, {
              sourceType: config.createSourceType,
              permissionScope: config.createPermissionScope
            });
      message.success(`上传完成：成功 ${result.success} 个，失败 ${result.failed} 个`);
      setUploadOpen(false);
      setUploadFileList([]);
      fetchData();
    } finally {
      setUploading(false);
    }
  };

  const onEdit = async (id: number) => {
    const result = await getKnowledgeDocumentDetail(id);
    setEditingId(id);
    editForm.setFieldsValue({
      title: result.title,
      content: result.content,
      courseId: result.courseId,
      studyMode: result.studyMode || 'ALL',
      sourceType: result.sourceType || 'manual',
      sourcePath: result.sourcePath,
      permissionScope: result.permissionScope || config.createPermissionScope
    });
    setOpen(true);
  };

  const onSubmit = async () => {
    const values = await editForm.validateFields();
    const payload: KnowledgeDocumentCreateReq = {
      ...values,
      sourceType: isCourseMode ? values.sourceType : config.createSourceType,
      permissionScope: config.createPermissionScope,
      courseId: isCourseMode ? values.courseId : undefined,
      studyMode: isCourseMode ? values.studyMode : 'ALL'
    };
    setSaving(true);
    try {
      if (editingId) {
        await updateKnowledgeDocument(editingId, payload);
        message.success('文档更新已提交入库，请查看状态');
      } else {
        await createKnowledgeDocument(payload);
        message.success('文档已提交入库，请查看状态');
      }
      setOpen(false);
      setEditingId(undefined);
      fetchData();
    } finally {
      setSaving(false);
    }
  };

  const onReindex = async (id: number) => {
    await reindexKnowledgeDocument(id);
    message.success('重新入库成功');
    fetchData();
  };

  const onDisable = async (id: number) => {
    await disableKnowledgeDocument(id);
    message.success('已禁用');
    fetchData();
  };

  const onDetail = async (id: number) => {
    const result = await getKnowledgeDocumentDetail(id);
    setDetail(result);
    setDetailOpen(true);
  };

  const fetchChunks = async (documentId: number, nextPageNum = chunkPageNum, nextPageSize = chunkPageSize) => {
    setChunkLoading(true);
    try {
      const result = await getKnowledgeDocumentChunks(documentId, {
        pageNum: nextPageNum,
        pageSize: nextPageSize
      });
      setChunkDataSource(result.records);
      setChunkTotal(result.total);
    } catch {
      message.error('获取 Chunk 详情失败');
    } finally {
      setChunkLoading(false);
    }
  };

  const onChunks = async (record: KnowledgeDocumentEntity) => {
    setChunkDocument(record);
    setChunkOpen(true);
    setChunkPageNum(1);
    setChunkPageSize(10);
    await fetchChunks(record.id, 1, 10);
  };

  const columns: ColumnsType<KnowledgeDocumentEntity> = [
    { title: 'ID', dataIndex: 'id', width: 80 },
    {
      title: '标题',
      dataIndex: 'title',
      width: 220,
      render: (value: string, record) => (
        <Button type="link" style={{ padding: 0 }} onClick={() => onDetail(record.id)}>
          {value}
        </Button>
      )
    },
    { title: '文档编码', dataIndex: 'documentCode', width: 220 },
    ...(isCourseMode
      ? [
          {
            title: '课程',
            dataIndex: 'courseId',
            width: 150,
            render: (value?: number) => (value ? `${courseNameMap.get(value) || '未知课程'}（${value}）` : '-')
          },
          {
            title: '适用范围',
            dataIndex: 'studyMode',
            width: 120,
            render: (value?: string) => studyModeText[value || 'ALL'] || value || '全部学员'
          }
        ]
      : [
          {
            title: isCodeMode ? '源码文件' : '来源路径',
            dataIndex: 'sourcePath',
            width: 260,
            render: (value?: string) => value || '-'
          }
        ]),
    { title: '版本', dataIndex: 'version', width: 80 },
    {
      title: 'Chunk',
      dataIndex: 'chunkCount',
      width: 90,
      render: (value: number, record) => (
        <Button type="link" style={{ padding: 0 }} disabled={!value} onClick={() => onChunks(record)}>
          {value || 0}
        </Button>
      )
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 110,
      render: (value: string) => <Tag color={statusColor[value] || 'default'}>{statusText[value] || value}</Tag>
    },
    {
      title: '最近入库',
      dataIndex: 'indexedAt',
      width: 170,
      render: (value?: string) => (value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-')
    },
    {
      title: '更新时间',
      dataIndex: 'updatedAt',
      width: 170,
      render: (value?: string) => (value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-')
    },
    {
      title: '操作',
      width: 250,
      fixed: 'right',
      render: (_, record) => (
        <Space>
          {canUpdate && !isCodeMode && !isMetricMode ? (
            <Button size="small" icon={<EditOutlined />} onClick={() => onEdit(record.id)}>
              编辑
            </Button>
          ) : null}
          {canReindex ? (
            <Popconfirm title="确认重新入库？" onConfirm={() => onReindex(record.id)}>
              <Button size="small" icon={<ReloadOutlined />}>
                重入库
              </Button>
            </Popconfirm>
          ) : null}
          {canDisable && record.status !== 'DISABLED' ? (
            <Popconfirm title="确认禁用？" onConfirm={() => onDisable(record.id)}>
              <Button size="small" danger icon={<StopOutlined />}>
                禁用
              </Button>
            </Popconfirm>
          ) : null}
        </Space>
      )
    }
  ];

  const chunkColumns: ColumnsType<KnowledgeDocumentChunk> = [
    { title: 'Chunk', dataIndex: 'chunkId', width: 120 },
    {
      title: '章节',
      dataIndex: 'headerPath',
      width: 220,
      render: (value?: string) => value || '-'
    },
    {
      title: '长度',
      dataIndex: 'contentLength',
      width: 90,
      render: (value: number | undefined, record) => value ?? record.content?.length ?? 0
    },
    {
      title: '范围',
      dataIndex: 'studyMode',
      width: 110,
      render: (value?: string) => studyModeText[value || 'ALL'] || value || '全部学员'
    },
    { title: '版本', dataIndex: 'version', width: 80 },
    {
      title: '向量',
      dataIndex: 'vectorDim',
      width: 120,
      render: (value: number | undefined, record) => (
        <span>{value || 0} 维{record.vectorPreview?.length ? ` / ${record.vectorPreview.length} 预览` : ''}</span>
      )
    }
  ];

  if (!canView) {
    return <NoPermission />;
  }

  return (
    <PageContainer title={config.title} description={config.description}>
      <Tabs
        activeKey={activeMode}
        onChange={onModeChange}
        items={(Object.keys(pageModeConfig) as KnowledgeMode[])
          .filter((item) => hasPermission(pageModeConfig[item].canViewCode))
          .map((item) => ({ key: item, label: pageModeConfig[item].tabLabel }))}
        style={{ marginBottom: 12 }}
      />
      <Form form={searchForm} layout="inline" className="search-form" onFinish={onSearch}>
        <Form.Item name="keyword">
          <Input placeholder="标题 / 文档编码 / 来源路径" allowClear style={{ width: 240 }} />
        </Form.Item>
        <Form.Item name="status">
          <Select placeholder="状态" allowClear options={statusOptions} style={{ width: 140 }} />
        </Form.Item>
        {isCourseMode ? (
          <>
            <Form.Item name="courseId">
              <Select
                placeholder="课程"
                allowClear
                showSearch
                optionFilterProp="label"
                options={courseSelectOptions}
                style={{ width: 220 }}
              />
            </Form.Item>
            <Form.Item name="studyMode">
              <Select placeholder="适用范围" allowClear options={studyModeOptions} style={{ width: 140 }} />
            </Form.Item>
          </>
        ) : null}
        <Form.Item>
          <Space>
            <Button type="primary" htmlType="submit">查询</Button>
            <Button onClick={onReset}>重置</Button>
            {canCreate ? (
              <>
                {config.uploadKind !== 'none' ? (
                  <Button icon={<UploadOutlined />} onClick={() => setUploadOpen(true)}>
                    {config.uploadKind === 'json' ? '批量上传 JSON' : '批量上传 MD'}
                  </Button>
                ) : (
                  <Button type="primary" icon={<PlusOutlined />} onClick={onCreate}>
                    新增文档
                  </Button>
                )}
              </>
            ) : null}
          </Space>
        </Form.Item>
      </Form>

      <Space style={{ width: '100%', justifyContent: 'space-between', marginTop: 16, marginBottom: 12 }}>
        <Typography.Title level={5} style={{ margin: 0 }}>
          {config.listTitle}
        </Typography.Title>
        <Tag icon={<DatabaseOutlined />} color="processing">Milvus</Tag>
      </Space>
      <PanelTable>
        <Table
          rowKey="id"
          loading={loading}
          columns={columns}
          dataSource={dataSource}
          scroll={{ x: 1350 }}
          pagination={{
            current: pageNum,
            pageSize,
            total,
            showSizeChanger: true,
            onChange: (current, size) => {
              setPageNum(current);
              setPageSize(size);
            }
          }}
        />
      </PanelTable>

      {config.uploadKind !== 'none' ? (
        <Modal
        title={config.uploadKind === 'json' ? '批量上传 JSON' : '批量上传 Markdown'}
        open={uploadOpen}
        width={720}
        confirmLoading={uploading}
        onOk={onUploadSubmit}
        onCancel={() => {
          setUploadOpen(false);
          setUploadFileList([]);
        }}
        destroyOnClose
      >
        <Space direction="vertical" size={16} style={{ width: '100%' }}>
          <Space>
            <Typography.Text>上传后立即入 Milvus</Typography.Text>
            <Switch checked={uploadIndexNow} onChange={setUploadIndexNow} />
          </Space>
          <Upload.Dragger
            multiple
            accept={config.uploadKind === 'json' ? '.json' : '.md,.markdown'}
            fileList={uploadFileList}
            beforeUpload={(file, nextFiles) => {
              const mergedCount = uploadFileList.length + nextFiles.length;
              const lowerName = file.name.toLowerCase();
              const isJsonFile = lowerName.endsWith('.json');
              const isMarkdownFile = lowerName.endsWith('.md') || lowerName.endsWith('.markdown');
              if (config.uploadKind === 'json' ? !isJsonFile : !isMarkdownFile) {
                message.error(`${file.name} 不是${config.uploadKind === 'json' ? ' JSON ' : ' Markdown '}文件`);
                return Upload.LIST_IGNORE;
              }
              if (mergedCount > 50) {
                message.error(`一次最多上传 50 个${config.uploadKind === 'json' ? ' JSON ' : ' Markdown '}文件`);
                return Upload.LIST_IGNORE;
              }
              return false;
            }}
            onChange={({ fileList }) => setUploadFileList(fileList.slice(0, 50))}
            onRemove={(file) => {
              setUploadFileList((current) => current.filter((item) => item.uid !== file.uid));
            }}
          >
            <p className="ant-upload-drag-icon">
              <UploadOutlined />
            </p>
            <p className="ant-upload-text">
              拖拽或点击选择{config.uploadKind === 'json' ? ' JSON ' : ' Markdown '}文件
            </p>
            <p className="ant-upload-hint">
              {config.uploadKind === 'json'
                ? '最多 50 个 .json 文件；数据分析业务指标会按一个指标一个 Chunk 入库；'
                : '最多 50 个 .md/.markdown 文件；'}
              {activeMode === 'dataSchema' ? '数据分析表结构会按一个文件一个 Chunk 入库；' : ''}
              关闭立即入库后可稍后点击“重入库”。
            </p>
          </Upload.Dragger>
        </Space>
        </Modal>
      ) : null}

      {!isCodeMode && !isMetricMode ? (
        <Modal
        title={editingId ? `编辑${config.listTitle.replace('列表', '')}` : `新增${config.listTitle.replace('列表', '')}`}
        open={open}
        width={860}
        confirmLoading={saving}
        onOk={onSubmit}
        onCancel={() => {
          setOpen(false);
          setEditingId(undefined);
        }}
        destroyOnClose
      >
        <Form form={editForm} layout="vertical">
          {!isCourseMode ? (
            <>
              <Form.Item name="sourceType" hidden>
                <Input />
              </Form.Item>
              <Form.Item name="permissionScope" hidden>
                <Input />
              </Form.Item>
            </>
          ) : null}
          <Form.Item name="title" label="标题" rules={[{ required: true, message: '请输入标题' }]}>
            <Input placeholder={isCourseMode ? '例如 Java HashMap 详解' : '例如 sale_order'} />
          </Form.Item>
          {isCourseMode ? (
            <Space size={16} style={{ width: '100%' }}>
              <Form.Item name="courseId" label="课程">
                <Select
                  allowClear
                  showSearch
                  optionFilterProp="label"
                  placeholder="选择课程"
                  options={courseSelectOptions}
                  style={{ width: 220 }}
                />
              </Form.Item>
              <Form.Item name="studyMode" label="适用范围" rules={[{ required: true, message: '请选择适用范围' }]}>
                <Select style={{ width: 160 }} options={studyModeOptions} />
              </Form.Item>
              <Form.Item name="sourceType" label="来源类型">
                <Select
                  style={{ width: 160 }}
                  options={[
                    { label: '手动录入', value: 'manual' },
                    { label: 'Markdown', value: 'markdown' },
                    { label: 'TXT', value: 'txt' }
                  ]}
                />
              </Form.Item>
            </Space>
          ) : null}
          <Form.Item name="sourcePath" label="来源路径">
            <Input placeholder={isCourseMode ? '可选，例如 docs/java/hashmap.md' : '可选，例如 data-analysis/schema-docs/sale_order.md'} />
          </Form.Item>
          <Form.Item name="content" label="正文" rules={[{ required: true, message: '请输入正文' }]}>
            <Input.TextArea rows={16} placeholder="粘贴 Markdown / 文本文档内容" />
          </Form.Item>
        </Form>
        </Modal>
      ) : null}

      <Modal
        title="知识文档详情"
        open={detailOpen}
        width={860}
        footer={null}
        onCancel={() => setDetailOpen(false)}
      >
        {detail ? (
          <Space direction="vertical" size={12} style={{ width: '100%' }}>
            <Space wrap>
              <Tag color={statusColor[detail.status] || 'default'}>{statusText[detail.status] || detail.status}</Tag>
              <Tag>版本 {detail.version}</Tag>
              <Tag>Chunk {detail.chunkCount}</Tag>
            </Space>
            <Typography.Paragraph><strong>文档编码：</strong>{detail.documentCode}</Typography.Paragraph>
            <Typography.Paragraph><strong>标题：</strong>{detail.title}</Typography.Paragraph>
            <Typography.Paragraph>
              <strong>课程/适用范围：</strong>{detail.courseId ?? '-'} / {studyModeText[detail.studyMode || 'ALL'] || '-'}
            </Typography.Paragraph>
            {detail.errorMessage ? (
              <Typography.Paragraph type="danger" style={{ whiteSpace: 'pre-wrap' }}>
                <strong>失败原因：</strong>{detail.errorMessage}
              </Typography.Paragraph>
            ) : null}
            <div
              style={{
                maxHeight: 420,
                overflow: 'auto',
                padding: 16,
                borderRadius: 12,
                background: '#faf7f4',
                lineHeight: 1.8
              }}
            >
              <style>{`
                .knowledge-document-markdown-preview h1,
                .knowledge-document-markdown-preview h2,
                .knowledge-document-markdown-preview h3,
                .knowledge-document-markdown-preview h4,
                .knowledge-document-markdown-preview h5,
                .knowledge-document-markdown-preview h6 {
                  margin: 0 0 16px;
                  color: #2f241d;
                  line-height: 1.35;
                }
                .knowledge-document-markdown-preview h1 { font-size: 28px; }
                .knowledge-document-markdown-preview h2 { font-size: 24px; }
                .knowledge-document-markdown-preview h3 { font-size: 20px; }
                .knowledge-document-markdown-preview p {
                  margin: 0 0 16px;
                  color: #41342b;
                  font-size: 16px;
                }
                .knowledge-document-markdown-preview ul,
                .knowledge-document-markdown-preview ol {
                  margin: 0 0 16px;
                  padding-left: 24px;
                  color: #41342b;
                }
                .knowledge-document-markdown-preview li {
                  margin-bottom: 8px;
                }
                .knowledge-document-markdown-preview code {
                  padding: 2px 6px;
                  border-radius: 6px;
                  background: #efe6de;
                  font-family: Monaco, Menlo, Consolas, monospace;
                  font-size: 14px;
                }
                .knowledge-document-markdown-preview pre {
                  margin: 0 0 16px;
                  padding: 16px;
                  border-radius: 12px;
                  background: #241d18;
                  overflow: auto;
                }
                .knowledge-document-markdown-preview pre code {
                  padding: 0;
                  background: transparent;
                  color: #f7efe9;
                  font-size: 14px;
                }
                .knowledge-document-markdown-preview a {
                  color: #1677ff;
                  text-decoration: none;
                }
                .knowledge-document-markdown-preview strong {
                  color: #2f241d;
                }
              `}</style>
              {isMarkdownSource(detail.sourceType) ? (
                <div
                  className="knowledge-document-markdown-preview"
                  dangerouslySetInnerHTML={{ __html: detailMarkdownHtml }}
                />
              ) : (
                <Typography.Paragraph style={{ whiteSpace: 'pre-wrap', marginBottom: 0 }}>
                  {detail.content}
                </Typography.Paragraph>
              )}
            </div>
          </Space>
        ) : null}
      </Modal>

      <Drawer
        title={chunkDocument ? `Chunk 详情 - ${chunkDocument.title}` : 'Chunk 详情'}
        open={chunkOpen}
        width={980}
        onClose={() => setChunkOpen(false)}
        destroyOnClose
      >
        {chunkDocument ? (
          <Space direction="vertical" size={12} style={{ width: '100%' }}>
            <Space wrap>
              <Tag>文档编码 {chunkDocument.documentCode}</Tag>
              <Tag>版本 {chunkDocument.version}</Tag>
              <Tag color="processing">Milvus</Tag>
              <Tag>共 {chunkTotal} 个 Chunk</Tag>
            </Space>
            <Table
              rowKey="id"
              loading={chunkLoading}
              columns={chunkColumns}
              dataSource={chunkDataSource}
              scroll={{ x: 820 }}
              expandable={{
                expandedRowRender: (record) => (
                  <Space direction="vertical" size={8} style={{ width: '100%' }}>
                    <Typography.Text type="secondary">{record.id}</Typography.Text>
                    {record.vectorPreview?.length ? (
                      <Typography.Paragraph style={{ marginBottom: 0 }}>
                        <strong>向量预览：</strong>{record.vectorPreview.map((item) => Number(item).toFixed(6)).join(', ')}
                      </Typography.Paragraph>
                    ) : null}
                    <Typography.Paragraph style={{ whiteSpace: 'pre-wrap', marginBottom: 0 }}>
                      {record.content || '-'}
                    </Typography.Paragraph>
                  </Space>
                )
              }}
              pagination={{
                current: chunkPageNum,
                pageSize: chunkPageSize,
                total: chunkTotal,
                showSizeChanger: true,
                onChange: async (current, size) => {
                  setChunkPageNum(current);
                  setChunkPageSize(size);
                  await fetchChunks(chunkDocument.id, current, size);
                }
              }}
            />
          </Space>
        ) : null}
      </Drawer>
    </PageContainer>
  );
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

function isMarkdownSource(sourceType?: string) {
  return sourceType === 'markdown' || sourceType === 'generated_java_md' || sourceType === 'data_analysis_schema_md';
}

function resolveUploadPermissionCode(uploadKind: UploadKind) {
  if (uploadKind === 'markdown') {
    return 'knowledge-document:upload-md';
  }
  if (uploadKind === 'json') {
    return 'knowledge-document:upload-json';
  }
  return 'knowledge-document:create';
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

import { useEffect, useState } from 'react';
import { Button, DatePicker, Descriptions, Form, Input, Modal, Popconfirm, Select, Space, Table, Tag, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import NoPermission from '../../components/NoPermission';
import PageContainer from '../../components/PageContainer';
import PanelTable from '../../components/PanelTable';
import type { RangePickerProps } from 'antd/es/date-picker';
import { getClassPage } from '../../api/classes';
import { getProductPage } from '../../api/products';
import { getStudentPermissionTemplatePage } from '../../api/student-permission-templates';
import { bindStudentPermissionTemplate, getStudentPage, getStudentPermissionBindingDetail, unbindStudentPermissionTemplate } from '../../api/students';
import type {
  ClassEntity,
  ProductEntity,
  StudentBindPermissionTemplateReq,
  StudentEntity,
  StudentPermissionBindingDetailRes,
  StudentPermissionTemplateEntity,
  StudentQueryParams
} from '../../types/system';
import { hasPermission } from '../../utils/permission';

interface SearchValues {
  keyword?: string;
  studyMode?: StudentEntity['studyMode'];
  classId?: number;
  productId?: number;
}

interface BindFormValues {
  templateId?: number;
  effectiveRange?: RangePickerProps['value'];
}

const studyModeOptions = [
  { label: '线上', value: 'ONLINE' },
  { label: '线下', value: 'OFFLINE' }
] as const;

export default function StudentPage() {
  const canView = hasPermission('menu:student:manage');
  const canBindTemplate = hasPermission('button:student:bind-permission-template');
  const canUnbindTemplate = hasPermission('button:student:unbind-permission-template');

  const [searchForm] = Form.useForm<SearchValues>();
  const [bindForm] = Form.useForm<BindFormValues>();
  const [loading, setLoading] = useState(false);
  const [optionLoading, setOptionLoading] = useState(false);
  const [dataSource, setDataSource] = useState<StudentEntity[]>([]);
  const [classOptions, setClassOptions] = useState<ClassEntity[]>([]);
  const [productOptions, setProductOptions] = useState<ProductEntity[]>([]);
  const [templateOptions, setTemplateOptions] = useState<StudentPermissionTemplateEntity[]>([]);
  const [bindOpen, setBindOpen] = useState(false);
  const [bindSubmitting, setBindSubmitting] = useState(false);
  const [currentStudent, setCurrentStudent] = useState<StudentEntity>();
  const [bindingDetail, setBindingDetail] = useState<StudentPermissionBindingDetailRes>();
  const [pageNum, setPageNum] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [total, setTotal] = useState(0);

  const fetchData = async () => {
    setLoading(true);
    try {
      const values = searchForm.getFieldsValue();
      const result = await getStudentPage({ pageNum, pageSize, ...values } as StudentQueryParams);
      setDataSource(result.records);
      setTotal(result.total);
    } finally {
      setLoading(false);
    }
  };

  const fetchOptions = async () => {
    setOptionLoading(true);
    try {
      const requests: Promise<any>[] = [
        getClassPage({ pageNum: 1, pageSize: 1000 }),
        getProductPage({ pageNum: 1, pageSize: 1000 })
      ];
      if (canBindTemplate || canUnbindTemplate) {
        requests.push(getStudentPermissionTemplatePage({ pageNum: 1, pageSize: 1000, status: 1 }));
      }
      const [classResult, productResult, templateResult] = await Promise.all(requests);
      setClassOptions(classResult.records);
      setProductOptions(productResult.records);
      setTemplateOptions(templateResult?.records ?? []);
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
  }, [canBindTemplate, canUnbindTemplate, canView]);

  const openBindModal = async (record: StudentEntity) => {
    const detail = await getStudentPermissionBindingDetail(record.id);
    setCurrentStudent(record);
    setBindingDetail(detail);
    bindForm.setFieldsValue({
      templateId: detail.manualBinding?.templateId,
      effectiveRange: detail.manualBinding
        ? [
            detail.manualBinding.effectiveStartTime ? dayjs(detail.manualBinding.effectiveStartTime) : null,
            detail.manualBinding.effectiveEndTime ? dayjs(detail.manualBinding.effectiveEndTime) : null
          ]
        : undefined
    });
    setBindOpen(true);
  };

  const closeBindModal = () => {
    setBindOpen(false);
    setCurrentStudent(undefined);
    setBindingDetail(undefined);
    bindForm.resetFields();
  };

  const submitBind = async () => {
    if (!currentStudent) {
      return;
    }
    const values = await bindForm.validateFields();
    const payload: StudentBindPermissionTemplateReq = {
      templateId: values.templateId!,
      effectiveStartTime: values.effectiveRange?.[0]?.toISOString(),
      effectiveEndTime: values.effectiveRange?.[1]?.toISOString(),
      status: 1
    };
    setBindSubmitting(true);
    try {
      await bindStudentPermissionTemplate(currentStudent.id, payload);
      message.success('绑定成功');
      closeBindModal();
      fetchData();
    } finally {
      setBindSubmitting(false);
    }
  };

  const handleUnbind = async () => {
    if (!currentStudent) {
      return;
    }
    setBindSubmitting(true);
    try {
      await unbindStudentPermissionTemplate(currentStudent.id);
      message.success('已恢复自动匹配');
      closeBindModal();
      fetchData();
    } finally {
      setBindSubmitting(false);
    }
  };

  const columns: ColumnsType<StudentEntity> = [
    { title: 'ID', dataIndex: 'id', width: 80 },
    { title: '学员编号', dataIndex: 'studentCode', width: 180 },
    { title: '姓名', dataIndex: 'studentName', width: 140 },
    { title: '手机号', dataIndex: 'phone', width: 160 },
    { title: '毕业届数', dataIndex: 'graduationSession', width: 120, render: (value) => value || '-' },
    { title: '学校', dataIndex: 'school', width: 220, render: (value) => value || '-' },
    { title: '学历', dataIndex: 'education', width: 120, render: (value) => value || '-' },
    {
      title: '学习方式',
      dataIndex: 'studyMode',
      width: 120,
      render: (value: StudentEntity['studyMode']) => (value === 'OFFLINE' ? '线下' : value === 'ONLINE' ? '线上' : '-')
    },
    { title: '课程商品', dataIndex: 'productName', width: 220, render: (value) => value || '-' },
    { title: '班级', dataIndex: 'className', width: 120, render: (value) => value || '-' },
    {
      title: '当前权限模板',
      dataIndex: 'currentPermissionTemplateName',
      width: 240,
      render: (_, record) => renderTemplateSummary(record)
    },
    {
      title: '加入时间',
      dataIndex: 'joinTime',
      width: 180,
      render: (value) => (value ? dayjs(value).format('YYYY-MM-DD HH:mm:ss') : '-')
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
      fixed: 'right',
      width: 180,
      render: (_, record) => (
        <Space>
          {canBindTemplate || canUnbindTemplate ? (
            <Button type="link" onClick={() => openBindModal(record)}>
              权限模板
            </Button>
          ) : null}
        </Space>
      )
    }
  ];

  if (!canView) {
    return <NoPermission />;
  }

  return (
    <PageContainer title="学员档案" description="查看已入班学员的基础档案、课程商品、培训班级和学习权益。">
      <Form form={searchForm} layout="inline" className="search-form" onFinish={fetchData}>
        <Form.Item name="keyword">
          <Input placeholder="学员编号/姓名/手机号/学校/课程商品" allowClear style={{ width: 280 }} />
        </Form.Item>
        <Form.Item name="studyMode">
          <Select allowClear placeholder="学习方式" style={{ width: 140 }} options={[...studyModeOptions]} />
        </Form.Item>
        <Form.Item name="productId">
          <Select
            allowClear
            showSearch
            optionFilterProp="label"
            loading={optionLoading}
            placeholder="课程商品"
            style={{ width: 220 }}
            options={productOptions.map((item) => ({ label: item.productName, value: item.id }))}
          />
        </Form.Item>
        <Form.Item name="classId">
          <Select
            allowClear
            showSearch
            optionFilterProp="label"
            loading={optionLoading}
            placeholder="班级"
            style={{ width: 180 }}
            options={classOptions.map((item) => ({ label: `第 ${item.classTerm} 期`, value: item.id }))}
          />
        </Form.Item>
        <Form.Item>
          <Space>
            <Button type="primary" htmlType="submit">
              查询
            </Button>
            <Button onClick={resetSearch}>重置</Button>
          </Space>
        </Form.Item>
      </Form>

      <PanelTable>
        <Table
          rowKey="id"
          loading={loading}
          columns={columns}
          dataSource={dataSource}
          scroll={{ x: 2140 }}
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

      {canBindTemplate || canUnbindTemplate ? (
        <Modal
          title="绑定学员权限模板"
          open={bindOpen}
          onOk={submitBind}
          okText="保存绑定"
          confirmLoading={bindSubmitting}
          onCancel={closeBindModal}
          destroyOnClose
          width={760}
          footer={
            <Space>
              {canUnbindTemplate && bindingDetail?.manualBinding ? (
                <Popconfirm title="确认解除当前手动绑定吗？" onConfirm={handleUnbind}>
                  <Button danger loading={bindSubmitting}>
                    解除绑定
                  </Button>
                </Popconfirm>
              ) : null}
              <Button onClick={closeBindModal}>取消</Button>
              {canBindTemplate ? (
                <Button type="primary" loading={bindSubmitting} onClick={submitBind}>
                  保存绑定
                </Button>
              ) : null}
            </Space>
          }
        >
          <Descriptions bordered size="small" column={1} styles={{ label: { width: 140 } }}>
            <Descriptions.Item label="学员">{currentStudent ? `${currentStudent.studentName} (${currentStudent.studentCode})` : '-'}</Descriptions.Item>
            <Descriptions.Item label="当前权限来源">{renderBindingSource(bindingDetail?.source)}</Descriptions.Item>
            <Descriptions.Item label="当前生效模板">
              {bindingDetail?.effectiveTemplate ? bindingDetail.effectiveTemplate.templateName : '未匹配'}
            </Descriptions.Item>
            <Descriptions.Item label="自动匹配模板">
              {bindingDetail?.autoMatchedTemplate ? bindingDetail.autoMatchedTemplate.templateName : '无'}
            </Descriptions.Item>
            <Descriptions.Item label="手动绑定模板">
              {bindingDetail?.manualBinding?.templateName ?? '无'}
            </Descriptions.Item>
          </Descriptions>

          {canBindTemplate ? (
            <Form form={bindForm} layout="vertical" style={{ marginTop: 16 }}>
              <Form.Item name="templateId" label="权限模板" rules={[{ required: true, message: '请选择权限模板' }]}>
                <Select
                  showSearch
                  optionFilterProp="label"
                  placeholder="请选择要绑定的权限模板"
                  options={templateOptions.map((item) => ({
                    label: `${item.templateName} (${item.templateCode})`,
                    value: item.id
                  }))}
                />
              </Form.Item>
              <Form.Item name="effectiveRange" label="生效时间">
                <DatePicker.RangePicker
                  showTime
                  style={{ width: '100%' }}
                  placeholder={['开始时间，为空表示立即生效', '结束时间，为空表示长期有效']}
                />
              </Form.Item>
            </Form>
          ) : null}
        </Modal>
      ) : null}
    </PageContainer>
  );
}

function renderTemplateSummary(record: StudentEntity) {
  if (!record.currentPermissionTemplateName) {
    return <Tag>未配置</Tag>;
  }
  const color = record.currentPermissionTemplateSource === 'MANUAL' ? 'processing' : 'default';
  const sourceText = record.currentPermissionTemplateSource === 'MANUAL' ? '手动' : '自动';
  return (
    <Space size={8}>
      <Tag color={color}>{sourceText}</Tag>
      <span>{record.currentPermissionTemplateName}</span>
    </Space>
  );
}

function renderBindingSource(source?: StudentPermissionBindingDetailRes['source']) {
  if (source === 'MANUAL') {
    return <Tag color="processing">手动绑定</Tag>;
  }
  if (source === 'AUTO') {
    return <Tag>自动匹配</Tag>;
  }
  return <Tag color="default">未匹配</Tag>;
}

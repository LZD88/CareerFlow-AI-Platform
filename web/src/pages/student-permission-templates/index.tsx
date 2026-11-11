import { useEffect, useMemo, useState } from 'react';
import { Badge, Button, Form, Input, Modal, Popconfirm, Select, Space, Table, Tag, Tree, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import type { DataNode } from 'antd/es/tree';
import dayjs from 'dayjs';
import NoPermission from '../../components/NoPermission';
import PageContainer from '../../components/PageContainer';
import PanelTable from '../../components/PanelTable';
import StatusTag from '../../components/StatusTag';
import { getStudentPermissionTree } from '../../api/permissions';
import {
  assignStudentPermissionTemplatePermissions,
  createStudentPermissionTemplate,
  deleteStudentPermissionTemplate,
  getStudentPermissionTemplateDetail,
  getStudentPermissionTemplatePage,
  updateStudentPermissionTemplate
} from '../../api/student-permission-templates';
import { getClassPage } from '../../api/classes';
import { getProductPage } from '../../api/products';
import type {
  ClassEntity,
  ProductEntity,
  StudentPermissionTemplateCreateReq,
  StudentPermissionTemplateEntity,
  StudentPermissionTemplateQueryParams,
  StudentPermissionTemplateUpdateReq,
  StudentPermissionTreeRes
} from '../../types/system';
import { hasPermission } from '../../utils/permission';

interface SearchValues {
  keyword?: string;
  status?: number;
  studyMode?: StudentPermissionTemplateEntity['studyMode'];
  productId?: number;
  classId?: number;
}

interface EditFormValues extends StudentPermissionTemplateCreateReq, StudentPermissionTemplateUpdateReq {}

const studyModeOptions = [
  { label: '线上', value: 'ONLINE' },
  { label: '线下', value: 'OFFLINE' }
] as const;

export default function StudentPermissionTemplatePage() {
  const canView = hasPermission('menu:system:student-permission-template');
  const canCreate = hasPermission('button:student-permission-template:create');
  const canUpdate = hasPermission('button:student-permission-template:update');
  const canDelete = hasPermission('button:student-permission-template:delete');
  const canAssignPermission = hasPermission('button:student-permission-template:assign-permission');

  const [searchForm] = Form.useForm<SearchValues>();
  const [editForm] = Form.useForm<EditFormValues>();
  const [loading, setLoading] = useState(false);
  const [optionLoading, setOptionLoading] = useState(false);
  const [open, setOpen] = useState(false);
  const [permissionOpen, setPermissionOpen] = useState(false);
  const [isEdit, setIsEdit] = useState(false);
  const [currentId, setCurrentId] = useState<number>();
  const [dataSource, setDataSource] = useState<StudentPermissionTemplateEntity[]>([]);
  const [permissionTree, setPermissionTree] = useState<StudentPermissionTreeRes[]>([]);
  const [checkedPermissionIds, setCheckedPermissionIds] = useState<number[]>([]);
  const [classOptions, setClassOptions] = useState<ClassEntity[]>([]);
  const [productOptions, setProductOptions] = useState<ProductEntity[]>([]);
  const [pageNum, setPageNum] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [total, setTotal] = useState(0);

  const productNameMap = useMemo(
    () => new Map(productOptions.map((item) => [item.id, item.productName])),
    [productOptions]
  );
  const classNameMap = useMemo(
    () => new Map(classOptions.map((item) => [item.id, `第 ${item.classTerm} 期`])),
    [classOptions]
  );

  const fetchData = async () => {
    setLoading(true);
    try {
      const values = searchForm.getFieldsValue();
      const result = await getStudentPermissionTemplatePage({ pageNum, pageSize, ...values } as StudentPermissionTemplateQueryParams);
      setDataSource(result.records);
      setTotal(result.total);
    } finally {
      setLoading(false);
    }
  };

  const fetchBaseData = async () => {
    setOptionLoading(true);
    try {
      const requests: Promise<any>[] = [
        getClassPage({ pageNum: 1, pageSize: 1000 }),
        getProductPage({ pageNum: 1, pageSize: 1000 })
      ];
      if (canAssignPermission) {
        requests.unshift(getStudentPermissionTree());
      }
      const results = await Promise.all(requests);
      if (canAssignPermission) {
        setPermissionTree(results[0]);
        setClassOptions(results[1].records);
        setProductOptions(results[2].records);
      } else {
        setClassOptions(results[0].records);
        setProductOptions(results[1].records);
      }
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
    fetchBaseData();
  }, [canAssignPermission, canView]);

  const onCreate = () => {
    setIsEdit(false);
    setCurrentId(undefined);
    editForm.resetFields();
    editForm.setFieldsValue({ status: 1 });
    setOpen(true);
  };

  const onEdit = async (record: StudentPermissionTemplateEntity) => {
    const detail = await getStudentPermissionTemplateDetail(record.id);
    setIsEdit(true);
    setCurrentId(record.id);
    editForm.setFieldsValue({
      templateCode: detail.template.templateCode,
      templateName: detail.template.templateName,
      studyMode: detail.template.studyMode,
      productId: detail.template.productId,
      classId: detail.template.classId,
      description: detail.template.description,
      status: detail.template.status
    });
    setOpen(true);
  };

  const onAssignPermissions = async (record: StudentPermissionTemplateEntity) => {
    const detail = await getStudentPermissionTemplateDetail(record.id);
    setCurrentId(record.id);
    setCheckedPermissionIds(detail.permissions.map((item) => item.id));
    setPermissionOpen(true);
  };

  const onDelete = async (id: number) => {
    await deleteStudentPermissionTemplate(id);
    message.success('删除成功');
    fetchData();
  };

  const submitEdit = async () => {
    const values = await editForm.validateFields();
    if (isEdit && currentId) {
      const payload: StudentPermissionTemplateUpdateReq = {
        templateName: values.templateName.trim(),
        studyMode: values.studyMode,
        productId: values.productId,
        classId: values.classId,
        description: values.description?.trim(),
        status: values.status
      };
      await updateStudentPermissionTemplate(currentId, payload);
      message.success('更新成功');
    } else {
      const payload: StudentPermissionTemplateCreateReq = {
        templateCode: values.templateCode.trim(),
        templateName: values.templateName.trim(),
        studyMode: values.studyMode,
        productId: values.productId,
        classId: values.classId,
        description: values.description?.trim(),
        status: values.status
      };
      await createStudentPermissionTemplate(payload);
      message.success('创建成功');
    }
    setOpen(false);
    fetchData();
  };

  const submitPermissions = async () => {
    if (!currentId) {
      return;
    }
    if (!checkedPermissionIds.length) {
      message.warning('请至少选择一个学员权限');
      return;
    }
    await assignStudentPermissionTemplatePermissions(currentId, checkedPermissionIds);
    message.success('权限分配成功');
    setPermissionOpen(false);
  };

  const columns: ColumnsType<StudentPermissionTemplateEntity> = [
    { title: 'ID', dataIndex: 'id', width: 80 },
    { title: '模板编码', dataIndex: 'templateCode', width: 220 },
    { title: '模板名称', dataIndex: 'templateName', width: 220 },
    {
      title: '学习方式',
      dataIndex: 'studyMode',
      width: 120,
      render: (value: StudentPermissionTemplateEntity['studyMode']) => (value === 'OFFLINE' ? '线下' : value === 'ONLINE' ? '线上' : '通用')
    },
    {
      title: '课程商品',
      dataIndex: 'productId',
      width: 220,
      render: (value?: number) => (value ? productNameMap.get(value) ?? `ID:${value}` : '通用')
    },
    {
      title: '班级',
      dataIndex: 'classId',
      width: 160,
      render: (value?: number) => (value ? classNameMap.get(value) ?? `ID:${value}` : '通用')
    },
    { title: '描述', dataIndex: 'description', width: 280, render: (value) => value || '-' },
    { title: '状态', dataIndex: 'status', width: 100, render: (value) => <StatusTag value={value} /> },
    {
      title: '更新时间',
      dataIndex: 'updatedAt',
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
          {canUpdate ? (
            <Button type="link" onClick={() => onEdit(record)}>
              编辑
            </Button>
          ) : null}
          {canAssignPermission ? (
            <Button type="link" onClick={() => onAssignPermissions(record)}>
              权限分配
            </Button>
          ) : null}
          {canDelete ? (
            <Popconfirm title="确认删除该模板吗？" onConfirm={() => onDelete(record.id)}>
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
      title="学员权益模板"
      description="管理学员端权限模板，并将学员权限点绑定到不同模板。"
      extra={canCreate ? <Button type="primary" onClick={onCreate}>新增模板</Button> : null}
    >
      <Form form={searchForm} layout="inline" className="search-form" onFinish={fetchData}>
        <Form.Item name="keyword">
          <Input placeholder="模板编码/模板名称" allowClear style={{ width: 240 }} />
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
          scroll={{ x: 1900 }}
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
          title={isEdit ? '编辑学员权益模板' : '新增学员权益模板'}
          open={open}
          onOk={submitEdit}
          onCancel={() => setOpen(false)}
          destroyOnClose
        >
          <Form form={editForm} layout="vertical">
            <Form.Item name="templateCode" label="模板编码" rules={[{ required: true, message: '请输入模板编码' }]}>
              <Input disabled={isEdit} />
            </Form.Item>
            <Form.Item name="templateName" label="模板名称" rules={[{ required: true, message: '请输入模板名称' }]}>
              <Input />
            </Form.Item>
            <Form.Item name="studyMode" label="学习方式">
              <Select allowClear placeholder="为空表示通用" options={[...studyModeOptions]} />
            </Form.Item>
            <Form.Item name="productId" label="课程商品">
              <Select
                allowClear
                showSearch
                optionFilterProp="label"
                placeholder="为空表示通用"
                options={productOptions.map((item) => ({ label: item.productName, value: item.id }))}
              />
            </Form.Item>
            <Form.Item name="classId" label="班级">
              <Select
                allowClear
                showSearch
                optionFilterProp="label"
                placeholder="为空表示通用"
                options={classOptions.map((item) => ({ label: `第 ${item.classTerm} 期`, value: item.id }))}
              />
            </Form.Item>
            <Form.Item name="description" label="描述">
              <Input.TextArea rows={3} />
            </Form.Item>
            <Form.Item name="status" label="状态" rules={[{ required: true, message: '请选择状态' }]}>
              <Select options={[{ label: '启用', value: 1 }, { label: '禁用', value: 0 }]} />
            </Form.Item>
          </Form>
        </Modal>
      ) : null}

      {canAssignPermission ? (
        <Modal
          title="学员权限分配"
          open={permissionOpen}
          onOk={submitPermissions}
          onCancel={() => setPermissionOpen(false)}
          width={760}
          destroyOnClose
        >
          <div className="assign-tree-toolbar">
            <Typography.Text strong>学员权限树</Typography.Text>
            <Badge count={checkedPermissionIds.length} showZero color="#0f6cbd" overflowCount={999} />
          </div>
          <div className="assign-tree-panel">
            <Tree
              checkable
              selectable={false}
              defaultExpandAll
              checkedKeys={checkedPermissionIds}
              treeData={convertPermissionTree(permissionTree)}
              onCheck={(checkedKeys) => {
                const values = Array.isArray(checkedKeys) ? checkedKeys : checkedKeys.checked;
                setCheckedPermissionIds(values as number[]);
              }}
            />
          </div>
        </Modal>
      ) : null}
    </PageContainer>
  );
}

function convertPermissionTree(data: StudentPermissionTreeRes[]): DataNode[] {
  return data.map((item) => ({
    key: item.id,
    title: (
      <div className="assign-tree-node">
        <span className="assign-tree-node-title">{item.permissionName}</span>
        <Space size={8}>
          <Tag color={getPermissionTypeColor(item.permissionType)}>{item.permissionType}</Tag>
          <Typography.Text type="secondary" className="assign-tree-node-code">
            {item.permissionCode}
          </Typography.Text>
        </Space>
      </div>
    ),
    children: convertPermissionTree(item.children ?? [])
  }));
}

function getPermissionTypeColor(type: string) {
  if (type === 'MENU') {
    return 'processing';
  }
  if (type === 'BUTTON') {
    return 'purple';
  }
  if (type === 'API') {
    return 'gold';
  }
  return 'default';
}

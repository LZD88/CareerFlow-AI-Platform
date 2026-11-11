import { useEffect, useMemo, useState } from 'react';
import {
  Button,
  Form,
  Input,
  InputNumber,
  Modal,
  Popconfirm,
  Segmented,
  Select,
  Space,
  Tag,
  Table,
  TreeSelect,
  Typography,
  message
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import NoPermission from '../../components/NoPermission';
import PageContainer from '../../components/PageContainer';
import PanelTable from '../../components/PanelTable';
import StatusTag from '../../components/StatusTag';
import {
  createPermission,
  deletePermission,
  getPermissionTree,
  getStudentPermissionTree,
  updatePermission
} from '../../api/permissions';
import type {
  PermissionCreateReq,
  PermissionEntity,
  PermissionTreeRes,
  PermissionUpdateReq,
  StudentPermissionTreeRes
} from '../../types/system';
import { hasPermission } from '../../utils/permission';

type PermissionScope = 'system' | 'student';

interface SearchValues {
  keyword?: string;
  permissionType?: string;
  status?: number;
}

interface PermissionTreeOption {
  title: string;
  value: number;
  key: number;
  children?: PermissionTreeOption[];
}

export default function PermissionPage() {
  const canView = hasPermission('menu:system:permission');
  const canCreate = hasPermission('button:permission:create');
  const canUpdate = hasPermission('button:permission:update');
  const canDelete = hasPermission('button:permission:delete');

  const [searchForm] = Form.useForm<SearchValues>();
  const [editForm] = Form.useForm<PermissionCreateReq & Partial<PermissionUpdateReq>>();
  const [loading, setLoading] = useState(false);
  const [open, setOpen] = useState(false);
  const [isEdit, setIsEdit] = useState(false);
  const [currentId, setCurrentId] = useState<number>();
  const [activeScope, setActiveScope] = useState<PermissionScope>('system');
  const [dataSource, setDataSource] = useState<PermissionTreeRes[]>([]);
  const [treeOptions, setTreeOptions] = useState<PermissionTreeRes[]>([]);
  const [studentDataSource, setStudentDataSource] = useState<StudentPermissionTreeRes[]>([]);

  const permissionMap = useMemo(() => buildPermissionMap(treeOptions), [treeOptions]);
  const studentPermissionMap = useMemo(() => buildPermissionMap(studentDataSource), [studentDataSource]);

  const fetchSystemData = async () => {
    setLoading(true);
    try {
      const result = await getPermissionTree();
      setTreeOptions(result);
      setDataSource(filterTree(result, searchForm.getFieldsValue()));
    } finally {
      setLoading(false);
    }
  };

  const fetchStudentData = async () => {
    setLoading(true);
    try {
      const result = await getStudentPermissionTree();
      setStudentDataSource(filterTree(result, searchForm.getFieldsValue()));
    } finally {
      setLoading(false);
    }
  };

  const fetchData = async () => {
    if (activeScope === 'system') {
      await fetchSystemData();
      return;
    }
    await fetchStudentData();
  };

  useEffect(() => {
    if (!canView) {
      return;
    }
    fetchData();
  }, [activeScope, canView]);

  const resetSearch = () => {
    searchForm.resetFields();
    fetchData();
  };

  const onCreate = () => {
    setIsEdit(false);
    setCurrentId(undefined);
    editForm.resetFields();
    editForm.setFieldsValue({ parentId: 0, status: 1, permissionType: 'MENU', sortNum: 1 });
    setOpen(true);
  };

  const onEdit = (record: PermissionEntity) => {
    setIsEdit(true);
    setCurrentId(record.id);
    editForm.setFieldsValue({
      parentId: record.parentId,
      permissionCode: record.permissionCode,
      permissionName: record.permissionName,
      permissionType: record.permissionType,
      sortNum: record.sortNum,
      description: record.description,
      status: record.status
    });
    setOpen(true);
  };

  const onDelete = async (id: number) => {
    await deletePermission(id);
    message.success('删除成功');
    fetchSystemData();
  };

  const submitEdit = async () => {
    const values = await editForm.validateFields();
    if (isEdit && currentId) {
      const payload: PermissionUpdateReq = {
        parentId: values.parentId,
        permissionName: values.permissionName,
        permissionType: values.permissionType,
        sortNum: values.sortNum,
        description: values.description,
        status: values.status
      };
      await updatePermission(currentId, payload);
      message.success('更新成功');
    } else {
      await createPermission(values as PermissionCreateReq);
      message.success('创建成功');
    }
    setOpen(false);
    fetchSystemData();
  };

  const systemColumns: ColumnsType<PermissionTreeRes> = [
    {
      title: '权限节点',
      dataIndex: 'permissionName',
      width: 460,
      render: (_, record) => renderPermissionNode(record)
    },
    {
      title: '权限类型',
      dataIndex: 'permissionType',
      width: 140,
      render: (value: string) => <Tag color={getPermissionTypeColor(value)}>{value}</Tag>
    },
    {
      title: '上级节点',
      dataIndex: 'parentId',
      width: 180,
      render: (value: number) => renderParentName(value, permissionMap)
    },
    { title: '排序', dataIndex: 'sortNum', width: 90 },
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
      width: 160,
      render: (_, record) => (
        <Space>
          {canUpdate ? (
            <Button type="link" onClick={() => onEdit(record)}>
              编辑
            </Button>
          ) : null}
          {canDelete ? (
            <Popconfirm title="确认删除该权限吗？" onConfirm={() => onDelete(record.id)}>
              <Button type="link" danger>
                删除
              </Button>
            </Popconfirm>
          ) : null}
        </Space>
      )
    }
  ];

  const studentColumns: ColumnsType<StudentPermissionTreeRes> = [
    {
      title: '权限节点',
      dataIndex: 'permissionName',
      width: 460,
      render: (_, record) => renderPermissionNode(record)
    },
    {
      title: '权限类型',
      dataIndex: 'permissionType',
      width: 140,
      render: (value: string) => <Tag color={getPermissionTypeColor(value)}>{value}</Tag>
    },
    {
      title: '上级节点',
      dataIndex: 'parentId',
      width: 180,
      render: (value: number) => renderParentName(value, studentPermissionMap)
    },
    {
      title: '路由/接口',
      width: 280,
      render: (_, record) => {
        if (record.routePath) {
          return <Typography.Text code>{record.routePath}</Typography.Text>;
        }
        if (record.apiPath) {
          return <Typography.Text code>{record.apiPath}</Typography.Text>;
        }
        return '-';
      }
    },
    { title: '排序', dataIndex: 'sortNum', width: 90 },
    { title: '状态', dataIndex: 'status', width: 100, render: (value) => <StatusTag value={value} /> },
    {
      title: '更新时间',
      dataIndex: 'updatedAt',
      width: 180,
      render: (value) => (value ? dayjs(value).format('YYYY-MM-DD HH:mm:ss') : '-')
    }
  ];

  if (!canView) {
    return <NoPermission />;
  }

  return (
    <PageContainer
      title="菜单与接口权限"
      description="维护后台权限，同时查看学员端权限点结构。"
      extra={activeScope === 'system' && canCreate ? <Button type="primary" onClick={onCreate}>新增权限</Button> : null}
    >
      <Segmented
        value={activeScope}
        onChange={(value) => setActiveScope(value as PermissionScope)}
        options={[
          { label: '后台权限', value: 'system' },
          { label: '学员权限', value: 'student' }
        ]}
        style={{ marginBottom: 16 }}
      />

      <Form form={searchForm} layout="inline" className="search-form" onFinish={fetchData}>
        <Form.Item name="keyword">
          <Input placeholder="权限编码/权限名称/路径" allowClear style={{ width: 240 }} />
        </Form.Item>
        <Form.Item name="permissionType">
          <Select
            allowClear
            placeholder="权限类型"
            style={{ width: 140 }}
            options={[
              { label: 'API', value: 'API' },
              { label: 'MENU', value: 'MENU' },
              { label: 'BUTTON', value: 'BUTTON' }
            ]}
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
          columns={activeScope === 'system' ? systemColumns : studentColumns}
          dataSource={activeScope === 'system' ? dataSource : studentDataSource}
          scroll={{ x: activeScope === 'system' ? 1180 : 1380 }}
          pagination={false}
          expandable={{ defaultExpandAllRows: true }}
        />
      </PanelTable>

      {activeScope === 'system' && (canCreate || canUpdate) ? (
        <Modal
          title={isEdit ? '编辑权限' : '新增权限'}
          open={open}
          onOk={submitEdit}
          onCancel={() => setOpen(false)}
          destroyOnClose
        >
          <Form form={editForm} layout="vertical">
            <Form.Item
              name="parentId"
              label="父权限"
              rules={[{ required: true, message: '请选择父权限' }]}
            >
              <TreeSelect
                allowClear
                treeDefaultExpandAll
                placeholder="请选择父权限"
                treeData={[
                  { title: '根节点', value: 0, key: 0, children: convertTreeOptions(treeOptions, currentId) }
                ]}
              />
            </Form.Item>
            <Form.Item
              name="permissionCode"
              label="权限编码"
              rules={[{ required: true, message: '请输入权限编码' }]}
            >
              <Input disabled={isEdit} />
            </Form.Item>
            <Form.Item
              name="permissionName"
              label="权限名称"
              rules={[{ required: true, message: '请输入权限名称' }]}
            >
              <Input />
            </Form.Item>
            <Form.Item
              name="permissionType"
              label="权限类型"
              rules={[{ required: true, message: '请选择权限类型' }]}
            >
              <Select
                options={[
                  { label: 'API', value: 'API' },
                  { label: 'MENU', value: 'MENU' },
                  { label: 'BUTTON', value: 'BUTTON' }
                ]}
              />
            </Form.Item>
            <Form.Item
              name="sortNum"
              label="排序"
              rules={[{ required: true, message: '请输入排序值' }]}
            >
              <InputNumber min={0} precision={0} style={{ width: '100%' }} />
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
    </PageContainer>
  );
}

function renderPermissionNode(record: PermissionEntity) {
  return (
    <div className="permission-node">
      <div className="permission-node-main">
        <span className="permission-node-title">{record.permissionName}</span>
        <Tag bordered={false} className="permission-node-id">
          ID {record.id}
        </Tag>
      </div>
      <div className="permission-node-meta">
        <Typography.Text code>{record.permissionCode}</Typography.Text>
        {record.description ? (
          <Typography.Text type="secondary" className="permission-node-desc">
            {record.description}
          </Typography.Text>
        ) : null}
      </div>
    </div>
  );
}

function renderParentName<T extends { permissionName: string }>(value: number, permissionMap: Map<number, T>) {
  if (value === 0) {
    return <Typography.Text type="secondary">根节点</Typography.Text>;
  }
  return permissionMap.get(value)?.permissionName ?? `#${value}`;
}

function filterTree<T extends PermissionTreeRes>(data: T[], filters: SearchValues): T[] {
  const keyword = filters.keyword?.trim();
  const permissionType = filters.permissionType;
  const status = filters.status;

  return data
    .map((item) => {
      const children = item.children ? filterTree(item.children as T[], filters) : [];
      const lowerKeyword = keyword?.toLowerCase();
      const matchedKeyword =
        !lowerKeyword ||
        item.permissionCode.toLowerCase().includes(lowerKeyword) ||
        item.permissionName.toLowerCase().includes(lowerKeyword) ||
        item.apiPath?.toLowerCase().includes(lowerKeyword) ||
        item.routePath?.toLowerCase().includes(lowerKeyword);
      const matchedType = !permissionType || item.permissionType === permissionType;
      const matchedStatus = status === undefined || item.status === status;
      if ((matchedKeyword && matchedType && matchedStatus) || children.length > 0) {
        return { ...item, children };
      }
      return null;
    })
    .filter(Boolean) as T[];
}

function convertTreeOptions(data: PermissionTreeRes[], currentId?: number): PermissionTreeOption[] {
  return data
    .filter((item) => item.id !== currentId)
    .map((item) => ({
      title: `${item.permissionName} (${item.permissionCode})`,
      value: item.id,
      key: item.id,
      children: convertTreeOptions(item.children ?? [], currentId)
    }));
}

function buildPermissionMap<T extends PermissionTreeRes>(data: T[]) {
  const map = new Map<number, T>();

  const walk = (nodes: T[]) => {
    nodes.forEach((node) => {
      map.set(node.id, node);
      if (node.children?.length) {
        walk(node.children as T[]);
      }
    });
  };

  walk(data);
  return map;
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

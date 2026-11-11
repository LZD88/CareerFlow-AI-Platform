import { useEffect, useState } from 'react';
import {
  Badge,
  Button,
  Form,
  Input,
  Modal,
  Popconfirm,
  Select,
  Space,
  Table,
  Tag,
  Tree,
  Typography,
  message
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import type { DataNode } from 'antd/es/tree';
import dayjs from 'dayjs';
import NoPermission from '../../components/NoPermission';
import PageContainer from '../../components/PageContainer';
import PanelTable from '../../components/PanelTable';
import StatusTag from '../../components/StatusTag';
import { assignRolePermissions, createRole, deleteRole, getRoleDetail, getRolePage, updateRole } from '../../api/roles';
import { getPermissionTree } from '../../api/permissions';
import type {
  PermissionEntity,
  PermissionTreeRes,
  RoleCreateReq,
  RoleEntity,
  RoleUpdateReq
} from '../../types/system';
import { hasPermission } from '../../utils/permission';

interface SearchValues {
  keyword?: string;
  status?: number;
}

export default function RolePage() {
  const canView = hasPermission('menu:system:role');
  const canCreate = hasPermission('button:role:create');
  const canUpdate = hasPermission('button:role:update');
  const canDelete = hasPermission('button:role:delete');
  const canAssignPermission = hasPermission('button:role:assign-permission');

  const [searchForm] = Form.useForm<SearchValues>();
  const [editForm] = Form.useForm<RoleCreateReq & Partial<RoleUpdateReq>>();
  const [loading, setLoading] = useState(false);
  const [open, setOpen] = useState(false);
  const [permissionOpen, setPermissionOpen] = useState(false);
  const [isEdit, setIsEdit] = useState(false);
  const [currentId, setCurrentId] = useState<number>();
  const [dataSource, setDataSource] = useState<RoleEntity[]>([]);
  const [permissionTree, setPermissionTree] = useState<PermissionTreeRes[]>([]);
  const [checkedPermissionIds, setCheckedPermissionIds] = useState<number[]>([]);
  const [pageNum, setPageNum] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [total, setTotal] = useState(0);

  const fetchData = async () => {
    setLoading(true);
    try {
      const values = searchForm.getFieldsValue();
      const result = await getRolePage({ pageNum, pageSize, ...values });
      setDataSource(result.records);
      setTotal(result.total);
    } finally {
      setLoading(false);
    }
  };

  const fetchPermissions = async () => {
    const result = await getPermissionTree();
    setPermissionTree(result);
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
    if (!canView || !canAssignPermission) {
      return;
    }
    fetchPermissions();
  }, [canAssignPermission, canView]);

  const onCreate = () => {
    setIsEdit(false);
    setCurrentId(undefined);
    editForm.resetFields();
    editForm.setFieldsValue({ status: 1 });
    setOpen(true);
  };

  const onEdit = (record: RoleEntity) => {
    setIsEdit(true);
    setCurrentId(record.id);
    editForm.setFieldsValue({
      roleCode: record.roleCode,
      roleName: record.roleName,
      description: record.description,
      status: record.status
    });
    setOpen(true);
  };

  const onAssignPermissions = async (record: RoleEntity) => {
    const detail = await getRoleDetail(record.id);
    setCurrentId(record.id);
    setCheckedPermissionIds(detail.permissions.map((item) => item.id));
    setPermissionOpen(true);
  };

  const onDelete = async (id: number) => {
    await deleteRole(id);
    message.success('删除成功');
    fetchData();
  };

  const submitEdit = async () => {
    const values = await editForm.validateFields();
    if (isEdit && currentId) {
      const payload: RoleUpdateReq = {
        roleName: values.roleName,
        description: values.description,
        status: values.status
      };
      await updateRole(currentId, payload);
      message.success('更新成功');
    } else {
      await createRole(values as RoleCreateReq);
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
      message.warning('请至少选择一个权限');
      return;
    }
    await assignRolePermissions(currentId, checkedPermissionIds);
    message.success('权限分配成功');
    setPermissionOpen(false);
  };

  const permissionTreeData = convertPermissionTree(permissionTree);

  const columns: ColumnsType<RoleEntity> = [
    { title: 'ID', dataIndex: 'id', width: 80 },
    { title: '角色编码', dataIndex: 'roleCode', width: 180 },
    { title: '角色名称', dataIndex: 'roleName', width: 180 },
    { title: '描述', dataIndex: 'description', width: 280 },
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
      width: 220,
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
            <Popconfirm title="确认删除该角色吗？" onConfirm={() => onDelete(record.id)}>
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
      title="岗位角色"
      description="管理角色定义，并将权限点绑定到角色。"
      extra={canCreate ? <Button type="primary" onClick={onCreate}>新增角色</Button> : null}
    >
      <Form form={searchForm} layout="inline" className="search-form" onFinish={fetchData}>
        <Form.Item name="keyword">
          <Input placeholder="角色编码/角色名称" allowClear style={{ width: 240 }} />
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
            <Button
              onClick={resetSearch}
            >
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
          scroll={{ x: 1280 }}
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
          title={isEdit ? '编辑角色' : '新增角色'}
          open={open}
          onOk={submitEdit}
          onCancel={() => setOpen(false)}
          destroyOnClose
        >
          <Form form={editForm} layout="vertical">
            <Form.Item name="roleCode" label="角色编码" rules={[{ required: true, message: '请输入角色编码' }]}>
              <Input disabled={isEdit} />
            </Form.Item>
            <Form.Item name="roleName" label="角色名称" rules={[{ required: true, message: '请输入角色名称' }]}>
              <Input />
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
          title="权限分配"
          open={permissionOpen}
          onOk={submitPermissions}
          onCancel={() => setPermissionOpen(false)}
          width={760}
          destroyOnClose
        >
          <div className="assign-tree-toolbar">
            <Typography.Text strong>权限树</Typography.Text>
            <Badge
              count={checkedPermissionIds.length}
              showZero
              color="#0f6cbd"
              overflowCount={999}
            />
          </div>
          <div className="assign-tree-panel">
            <Tree
              checkable
              selectable={false}
              defaultExpandAll
              checkedKeys={checkedPermissionIds}
              treeData={permissionTreeData}
              onCheck={(checkedKeys) => {
                const values = Array.isArray(checkedKeys)
                  ? checkedKeys
                  : checkedKeys.checked;
                setCheckedPermissionIds(values as number[]);
              }}
            />
          </div>
        </Modal>
      ) : null}
    </PageContainer>
  );
}

function convertPermissionTree(data: PermissionTreeRes[]): DataNode[] {
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

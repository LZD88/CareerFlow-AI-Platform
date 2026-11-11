import { useEffect, useState } from 'react';
import {
  Button,
  Form,
  Input,
  Modal,
  Popconfirm,
  Select,
  Space,
  Table,
  Tag,
  message
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import NoPermission from '../../components/NoPermission';
import PageContainer from '../../components/PageContainer';
import PanelTable from '../../components/PanelTable';
import StatusTag from '../../components/StatusTag';
import { assignUserRoles, createUser, deleteUser, getRoleOptions, getUserDetail, getUserPage, updateUser } from '../../api/users';
import type { RoleEntity, UserCreateReq, UserEntity, UserUpdateReq } from '../../types/system';
import { hasPermission } from '../../utils/permission';

interface SearchValues {
  keyword?: string;
  status?: number;
}

export default function UserPage() {
  const canView = hasPermission('menu:system:user');
  const canCreate = hasPermission('button:user:create');
  const canUpdate = hasPermission('button:user:update');
  const canDelete = hasPermission('button:user:delete');
  const canAssignRole = hasPermission('button:user:assign-role');

  const [searchForm] = Form.useForm<SearchValues>();
  const [editForm] = Form.useForm<UserCreateReq & Partial<UserUpdateReq>>();
  const [roleForm] = Form.useForm<{ roleIds: number[] }>();
  const [loading, setLoading] = useState(false);
  const [open, setOpen] = useState(false);
  const [roleOpen, setRoleOpen] = useState(false);
  const [isEdit, setIsEdit] = useState(false);
  const [currentId, setCurrentId] = useState<number>();
  const [dataSource, setDataSource] = useState<UserEntity[]>([]);
  const [roles, setRoles] = useState<RoleEntity[]>([]);
  const [pageNum, setPageNum] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [total, setTotal] = useState(0);

  const fetchData = async () => {
    setLoading(true);
    try {
      const values = searchForm.getFieldsValue();
      const result = await getUserPage({ pageNum, pageSize, ...values });
      setDataSource(result.records);
      setTotal(result.total);
    } finally {
      setLoading(false);
    }
  };

  const fetchRoles = async () => {
    const result = await getRoleOptions();
    setRoles(result.records);
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
    if (!canView || !canAssignRole) {
      return;
    }
    fetchRoles();
  }, [canAssignRole, canView]);

  const onCreate = () => {
    setIsEdit(false);
    setCurrentId(undefined);
    editForm.resetFields();
    editForm.setFieldsValue({ status: 1 });
    setOpen(true);
  };

  const onEdit = (record: UserEntity) => {
    setIsEdit(true);
    setCurrentId(record.id);
    editForm.setFieldsValue({
      username: record.username,
      nickname: record.nickname,
      realName: record.realName,
      phone: record.phone,
      email: record.email,
      status: record.status
    });
    setOpen(true);
  };

  const onAssignRoles = async (record: UserEntity) => {
    const detail = await getUserDetail(record.id);
    setCurrentId(record.id);
    roleForm.setFieldsValue({ roleIds: detail.roles.map((item) => item.id) });
    setRoleOpen(true);
  };

  const onDelete = async (id: number) => {
    await deleteUser(id);
    message.success('删除成功');
    fetchData();
  };

  const submitEdit = async () => {
    const values = await editForm.validateFields();
    if (isEdit && currentId) {
      const payload: UserUpdateReq = {
        nickname: values.nickname,
        realName: values.realName,
        phone: values.phone,
        email: values.email,
        status: values.status
      };
      await updateUser(currentId, payload);
      message.success('更新成功');
    } else {
      await createUser(values as UserCreateReq);
      message.success('创建成功');
    }
    setOpen(false);
    fetchData();
  };

  const submitRoles = async () => {
    const values = await roleForm.validateFields();
    if (!currentId) {
      return;
    }
    await assignUserRoles(currentId, values.roleIds);
    message.success('角色分配成功');
    setRoleOpen(false);
  };

  const columns: ColumnsType<UserEntity> = [
    { title: 'ID', dataIndex: 'id', width: 80 },
    { title: '用户名', dataIndex: 'username', width: 160 },
    { title: '昵称', dataIndex: 'nickname', width: 160 },
    { title: '真实姓名', dataIndex: 'realName', width: 160 },
    { title: '手机号', dataIndex: 'phone', width: 160 },
    { title: '邮箱', dataIndex: 'email', width: 220 },
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
          {canAssignRole ? (
            <Button type="link" onClick={() => onAssignRoles(record)}>
              分配角色
            </Button>
          ) : null}
          {canDelete ? (
            <Popconfirm title="确认删除该用户吗？" onConfirm={() => onDelete(record.id)}>
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
      title="员工账号"
      description="管理招生、教务、就业顾问和运营人员账号，按岗位分配角色。"
      extra={canCreate ? <Button type="primary" onClick={onCreate}>新增用户</Button> : null}
    >
      <Form form={searchForm} layout="inline" className="search-form" onFinish={fetchData}>
        <Form.Item name="keyword">
          <Input placeholder="用户名/昵称/真实姓名" allowClear style={{ width: 240 }} />
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
          scroll={{ x: 1480 }}
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
          title={isEdit ? '编辑用户' : '新增用户'}
          open={open}
          onOk={submitEdit}
          onCancel={() => setOpen(false)}
          destroyOnClose
        >
          <Form form={editForm} layout="vertical">
            <Form.Item name="username" label="用户名" rules={[{ required: true, message: '请输入用户名' }]}>
              <Input disabled={isEdit} />
            </Form.Item>
            {!isEdit ? (
              <Form.Item name="password" label="密码" rules={[{ required: true, message: '请输入密码' }]}>
                <Input.Password />
              </Form.Item>
            ) : null}
            <Form.Item name="nickname" label="昵称" rules={[{ required: true, message: '请输入昵称' }]}>
              <Input />
            </Form.Item>
            <Form.Item name="realName" label="真实姓名">
              <Input />
            </Form.Item>
            <Form.Item name="phone" label="手机号">
              <Input />
            </Form.Item>
            <Form.Item name="email" label="邮箱">
              <Input />
            </Form.Item>
            <Form.Item name="status" label="状态" rules={[{ required: true, message: '请选择状态' }]}>
              <Select options={[{ label: '启用', value: 1 }, { label: '禁用', value: 0 }]} />
            </Form.Item>
          </Form>
        </Modal>
      ) : null}

      {canAssignRole ? (
        <Modal
          title="分配角色"
          open={roleOpen}
          onOk={submitRoles}
          onCancel={() => setRoleOpen(false)}
          destroyOnClose
        >
          <Form form={roleForm} layout="vertical">
            <Form.Item name="roleIds" label="角色列表" rules={[{ required: true, message: '请选择至少一个角色' }]}>
              <Select
                mode="multiple"
                placeholder="请选择角色"
                options={roles.map((item) => ({
                  label: (
                    <Space>
                      <span>{item.roleName}</span>
                      <Tag>{item.roleCode}</Tag>
                    </Space>
                  ),
                  value: item.id
                }))}
              />
            </Form.Item>
          </Form>
        </Modal>
      ) : null}
    </PageContainer>
  );
}

import { useEffect, useState } from 'react';
import { BankOutlined } from '@ant-design/icons';
import { Button, Form, Input, Modal, Popconfirm, Select, Space, Table, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import NoPermission from '../../components/NoPermission';
import PageContainer from '../../components/PageContainer';
import PanelTable from '../../components/PanelTable';
import StatusTag from '../../components/StatusTag';
import { createCampus, deleteCampus, getCampusDetail, getCampusPage, updateCampus } from '../../api/campuses';
import { getUserPage } from '../../api/users';
import type { CampusCreateReq, CampusEntity, CampusQueryParams, CampusUpdateReq, UserEntity } from '../../types/system';
import { hasPermission } from '../../utils/permission';

interface SearchValues {
  keyword?: string;
  status?: number;
  principalUserId?: number;
}

export default function CampusPage() {
  const canView = hasPermission('menu:teaching:campus');
  const canCreate = hasPermission('button:campus:create');
  const canUpdate = hasPermission('button:campus:update');
  const canDelete = hasPermission('button:campus:delete');

  const [searchForm] = Form.useForm<SearchValues>();
  const [editForm] = Form.useForm<CampusCreateReq & CampusUpdateReq>();
  const [loading, setLoading] = useState(false);
  const [userLoading, setUserLoading] = useState(false);
  const [open, setOpen] = useState(false);
  const [isEdit, setIsEdit] = useState(false);
  const [currentId, setCurrentId] = useState<number>();
  const [dataSource, setDataSource] = useState<CampusEntity[]>([]);
  const [userOptions, setUserOptions] = useState<UserEntity[]>([]);
  const [pageNum, setPageNum] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [total, setTotal] = useState(0);

  const fetchData = async () => {
    setLoading(true);
    try {
      const values = searchForm.getFieldsValue();
      const result = await getCampusPage({ pageNum, pageSize, ...values } as CampusQueryParams);
      setDataSource(result.records);
      setTotal(result.total);
    } finally {
      setLoading(false);
    }
  };

  const fetchUsers = async () => {
    setUserLoading(true);
    try {
      // 负责人直接复用系统用户，因此这里拉一份启用中的用户做下拉选项。
      const result = await getUserPage({ pageNum: 1, pageSize: 1000, status: 1 });
      setUserOptions(result.records);
    } finally {
      setUserLoading(false);
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
    fetchUsers();
  }, [canView]);

  const onCreate = () => {
    setIsEdit(false);
    setCurrentId(undefined);
    editForm.resetFields();
    // 新增时默认启用，减少重复选择。
    editForm.setFieldsValue({ status: 1 });
    setOpen(true);
  };

  const onEdit = async (record: CampusEntity) => {
    // 编辑前先拉详情，确保表单回填的是后端最新数据。
    const detail = await getCampusDetail(record.id);
    setIsEdit(true);
    setCurrentId(record.id);
    editForm.setFieldsValue({
      campusName: detail.campusName,
      principalUserId: detail.principalUserId,
      address: detail.address,
      contactPhone: detail.contactPhone,
      remark: detail.remark,
      status: detail.status
    });
    setOpen(true);
  };

  const onDelete = async (id: number) => {
    await deleteCampus(id);
    message.success('删除成功');
    fetchData();
  };

  const submitEdit = async () => {
    const values = await editForm.validateFields();
    if (isEdit && currentId) {
      await updateCampus(currentId, values as CampusUpdateReq);
      message.success('更新成功');
    } else {
      await createCampus(values as CampusCreateReq);
      message.success('创建成功');
    }
    setOpen(false);
    fetchData();
  };

  const columns: ColumnsType<CampusEntity> = [
    { title: 'ID', dataIndex: 'id', width: 80 },
    { title: '校区名称', dataIndex: 'campusName', width: 180 },
    { title: '负责人', dataIndex: 'principalName', width: 160, render: (value) => value || '-' },
    { title: '负责人ID', dataIndex: 'principalUserId', width: 120 },
    { title: '校区地址', dataIndex: 'address', width: 260 },
    { title: '联系电话', dataIndex: 'contactPhone', width: 150, render: (value) => value || '-' },
    {
      title: '备注',
      dataIndex: 'remark',
      width: 220,
      render: (value) => value || '-'
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
      width: 160,
      render: (_, record) => (
        <Space>
          {canUpdate ? (
            <Button type="link" onClick={() => onEdit(record)}>
              编辑
            </Button>
          ) : null}
          {canDelete ? (
            <Popconfirm title="确认删除该校区吗？" onConfirm={() => onDelete(record.id)}>
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
      title="培训校区"
      description="维护职业培训校区、教学地址、负责人及联系方式。"
      extra={canCreate ? <Button type="primary" onClick={onCreate}>新增校区</Button> : null}
    >
      <Form form={searchForm} layout="inline" className="search-form" onFinish={fetchData}>
        <Form.Item name="keyword">
          <Input placeholder="校区名称/地址/负责人" allowClear style={{ width: 240 }} prefix={<BankOutlined />} />
        </Form.Item>
        <Form.Item name="principalUserId">
          <Select
            allowClear
            showSearch
            placeholder="负责人"
            style={{ width: 200 }}
            loading={userLoading}
            optionFilterProp="label"
            options={userOptions.map((item) => ({
              label: `${item.realName || item.nickname || item.username} (#${item.id})`,
              value: item.id
            }))}
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
          scroll={{ x: 1700 }}
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
          title={isEdit ? '编辑校区' : '新增校区'}
          open={open}
          onOk={submitEdit}
          onCancel={() => setOpen(false)}
          destroyOnClose
        >
          <Form form={editForm} layout="vertical">
            {/* 校区基础信息尽量保持扁平，便于后面继续扩展学段、教室等关联能力。 */}
            <Form.Item
              label="校区名称"
              name="campusName"
              rules={[{ required: true, message: '请输入校区名称' }]}
            >
              <Input placeholder="例如：浦东校区" />
            </Form.Item>
            <Form.Item
              label="负责人"
              name="principalUserId"
              rules={[{ required: true, message: '请选择负责人' }]}
            >
              <Select
                showSearch
                placeholder="请选择负责人"
                loading={userLoading}
                optionFilterProp="label"
                options={userOptions.map((item) => ({
                  label: `${item.realName || item.nickname || item.username} (#${item.id})`,
                  value: item.id
                }))}
              />
            </Form.Item>
            <Form.Item
              label="校区地址"
              name="address"
              rules={[{ required: true, message: '请输入校区地址' }]}
            >
              <Input.TextArea rows={3} placeholder="请输入详细地址" />
            </Form.Item>
            <Form.Item label="联系电话" name="contactPhone">
              <Input placeholder="例如：021-88886666" />
            </Form.Item>
            <Form.Item label="备注" name="remark">
              <Input.TextArea rows={3} placeholder="可填写校区说明、交通信息等" />
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
    </PageContainer>
  );
}

import { useEffect, useState } from 'react';
import { Button, Form, Input, Modal, Popconfirm, Space, Table, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import NoPermission from '../../components/NoPermission';
import PageContainer from '../../components/PageContainer';
import PanelTable from '../../components/PanelTable';
import { createConfig, deleteConfig, getConfigDetail, getConfigPage, updateConfig } from '../../api/configs';
import type { SysConfigCreateReq, SysConfigEntity, SysConfigUpdateReq } from '../../types/system';
import { hasPermission } from '../../utils/permission';

interface SearchValues {
  keyword?: string;
}

export default function ConfigPage() {
  const canView = hasPermission('menu:system:config');
  const canCreate = hasPermission('button:config:create');
  const canUpdate = hasPermission('button:config:update');
  const canDelete = hasPermission('button:config:delete');

  const [searchForm] = Form.useForm<SearchValues>();
  const [editForm] = Form.useForm<SysConfigCreateReq & SysConfigUpdateReq>();
  const [loading, setLoading] = useState(false);
  const [open, setOpen] = useState(false);
  const [isEdit, setIsEdit] = useState(false);
  const [currentId, setCurrentId] = useState<number>();
  const [dataSource, setDataSource] = useState<SysConfigEntity[]>([]);
  const [pageNum, setPageNum] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [total, setTotal] = useState(0);

  const fetchData = async () => {
    setLoading(true);
    try {
      const values = searchForm.getFieldsValue();
      const result = await getConfigPage({ pageNum, pageSize, ...values });
      setDataSource(result.records);
      setTotal(result.total);
    } finally {
      setLoading(false);
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

  const onCreate = () => {
    setIsEdit(false);
    setCurrentId(undefined);
    editForm.resetFields();
    setOpen(true);
  };

  const onEdit = async (record: SysConfigEntity) => {
    const detail = await getConfigDetail(record.id);
    setIsEdit(true);
    setCurrentId(record.id);
    editForm.setFieldsValue({ k: detail.k, v: detail.v });
    setOpen(true);
  };

  const onDelete = async (id: number) => {
    await deleteConfig(id);
    message.success('删除成功');
    fetchData();
  };

  const submitEdit = async () => {
    const values = await editForm.validateFields();
    if (isEdit && currentId) {
      await updateConfig(currentId, values as SysConfigUpdateReq);
      message.success('更新成功，配置已立即生效');
    } else {
      await createConfig(values as SysConfigCreateReq);
      message.success('创建成功');
    }
    setOpen(false);
    fetchData();
  };

  const columns: ColumnsType<SysConfigEntity> = [
    { title: 'ID', dataIndex: 'id', width: 80 },
    { title: '配置Key', dataIndex: 'k', width: 280 },
    {
      title: '配置Value',
      dataIndex: 'v',
      render: (value: string) => (
        <Typography.Text copyable={{ text: value }} style={{ whiteSpace: 'pre-wrap' }}>
          {value}
        </Typography.Text>
      )
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
            <Popconfirm title="确认删除该配置吗？" onConfirm={() => onDelete(record.id)}>
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
      title="平台参数"
      description="维护 sys_config 中的实时配置项，修改后会立即刷新后端缓存。"
      extra={canCreate ? <Button type="primary" onClick={onCreate}>新增配置</Button> : null}
    >
      <Form form={searchForm} layout="inline" className="search-form" onFinish={fetchData}>
        <Form.Item name="keyword">
          <Input placeholder="输入 key 或 value 查询" allowClear style={{ width: 280 }} />
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
          scroll={{ x: 920 }}
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
          title={isEdit ? '编辑平台参数' : '新增平台参数'}
          open={open}
          onOk={submitEdit}
          onCancel={() => setOpen(false)}
          destroyOnClose
        >
          <Form form={editForm} layout="vertical">
            <Form.Item
              label="配置Key"
              name="k"
              rules={[{ required: true, message: '请输入配置Key' }]}
            >
              <Input placeholder="例如：jwtExpireTime" />
            </Form.Item>
            <Form.Item
              label="配置Value"
              name="v"
              rules={[{ required: true, message: '请输入配置Value' }]}
            >
              <Input.TextArea rows={4} placeholder="例如：7200" />
            </Form.Item>
          </Form>
        </Modal>
      ) : null}
    </PageContainer>
  );
}

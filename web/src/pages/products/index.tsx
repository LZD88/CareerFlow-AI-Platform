import { useEffect, useState } from 'react';
import { AppstoreOutlined } from '@ant-design/icons';
import { Button, Form, Input, InputNumber, Modal, Popconfirm, Select, Space, Table, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import NoPermission from '../../components/NoPermission';
import PageContainer from '../../components/PageContainer';
import PanelTable from '../../components/PanelTable';
import { createProduct, deleteProduct, getProductDetail, getProductPage, updateProduct } from '../../api/products';
import type { ProductCreateReq, ProductEntity, ProductQueryParams, ProductUpdateReq } from '../../types/system';
import { hasPermission } from '../../utils/permission';

interface SearchValues {
  keyword?: string;
}

export default function ProductPage() {
  const canView = hasPermission('menu:sales:product');
  const canCreate = hasPermission('button:product:create');
  const canUpdate = hasPermission('button:product:update');
  const canDelete = hasPermission('button:product:delete');

  const [searchForm] = Form.useForm<SearchValues>();
  const [editForm] = Form.useForm<ProductCreateReq & ProductUpdateReq>();
  const [loading, setLoading] = useState(false);
  const [open, setOpen] = useState(false);
  const [isEdit, setIsEdit] = useState(false);
  const [currentId, setCurrentId] = useState<number>();
  const [dataSource, setDataSource] = useState<ProductEntity[]>([]);
  const [pageNum, setPageNum] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [total, setTotal] = useState(0);

  const fetchData = async () => {
    setLoading(true);
    try {
      const values = searchForm.getFieldsValue();
      const result = await getProductPage({ pageNum, pageSize, ...values } as ProductQueryParams);
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
    editForm.setFieldsValue({
      studyMode: 'ONLINE'
    } as any);
    setOpen(true);
  };

  const onEdit = async (record: ProductEntity) => {
    const detail = await getProductDetail(record.id);
    setIsEdit(true);
    setCurrentId(record.id);
    editForm.setFieldsValue({
      productCode: detail.productCode,
      productName: detail.productName,
      studyMode: detail.studyMode,
      productPrice: Number(detail.productPrice)
    } as any);
    setOpen(true);
  };

  const onDelete = async (id: number) => {
    await deleteProduct(id);
    message.success('删除成功');
    fetchData();
  };

  const submitEdit = async () => {
    const values = await editForm.validateFields();
    const payload = {
      productCode: values.productCode.trim(),
      productName: values.productName.trim(),
      studyMode: values.studyMode,
      productPrice: values.productPrice
    } as ProductCreateReq;

    if (isEdit && currentId) {
      await updateProduct(currentId, payload as ProductUpdateReq);
      message.success('更新成功');
    } else {
      await createProduct(payload);
      message.success('创建成功');
    }
    setOpen(false);
    fetchData();
  };

  const columns: ColumnsType<ProductEntity> = [
    { title: 'ID', dataIndex: 'id', width: 80 },
    { title: '课程商品编号', dataIndex: 'productCode', width: 180 },
    { title: '课程商品名称', dataIndex: 'productName', width: 220 },
    {
      title: '学习方式',
      dataIndex: 'studyMode',
      width: 120,
      render: (value) => (value === 'OFFLINE' ? '线下' : '线上')
    },
    {
      title: '课程商品价格',
      dataIndex: 'productPrice',
      width: 140,
      render: (value) => `¥${Number(value ?? 0).toFixed(2)}`
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
          {canUpdate ? (
            <Button type="link" onClick={() => onEdit(record)}>
              编辑
            </Button>
          ) : null}
          {canDelete ? (
            <Popconfirm title="确认删除该课程商品吗？" onConfirm={() => onDelete(record.id)}>
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
      title="课程商品"
      description="维护职业培训课程商品、授课方式与报名价格，作为招生报名和学员权益的依据。"
      extra={canCreate ? <Button type="primary" onClick={onCreate}>新增课程商品</Button> : null}
    >
      <Form form={searchForm} layout="inline" className="search-form" onFinish={fetchData}>
        <Form.Item name="keyword">
          <Input placeholder="课程商品编号/课程商品名称" allowClear style={{ width: 260 }} prefix={<AppstoreOutlined />} />
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
          scroll={{ x: 900 }}
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
          title={isEdit ? '编辑课程商品' : '新增课程商品'}
          open={open}
          onOk={submitEdit}
          onCancel={() => setOpen(false)}
          destroyOnClose
        >
          <Form form={editForm} layout="vertical">
            <Form.Item label="课程商品编号" name="productCode" rules={[{ required: true, message: '请输入课程商品编号' }]}>
              <Input placeholder="请输入课程商品编号" />
            </Form.Item>
            <Form.Item label="课程商品名称" name="productName" rules={[{ required: true, message: '请输入课程商品名称' }]}>
              <Input placeholder="请输入课程商品名称" />
            </Form.Item>
            <Form.Item label="学习方式" name="studyMode" rules={[{ required: true, message: '请选择学习方式' }]}>
              <Select
                placeholder="请选择学习方式"
                options={[
                  { label: '线上', value: 'ONLINE' },
                  { label: '线下', value: 'OFFLINE' }
                ]}
              />
            </Form.Item>
            <Form.Item label="课程商品价格" name="productPrice" rules={[{ required: true, message: '请输入课程商品价格' }]}>
              <InputNumber min={0} precision={2} style={{ width: '100%' }} placeholder="请输入课程商品价格" />
            </Form.Item>
          </Form>
        </Modal>
      ) : null}
    </PageContainer>
  );
}

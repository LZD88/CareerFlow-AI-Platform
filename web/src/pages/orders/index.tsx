import { useEffect, useMemo, useState } from 'react';
import { BarsOutlined } from '@ant-design/icons';
import { Alert, Button, DatePicker, Descriptions, Form, Input, InputNumber, Modal, Popconfirm, Select, Space, Table, Tag, message } from 'antd';
import { useSearchParams } from 'react-router-dom';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import NoPermission from '../../components/NoPermission';
import PageContainer from '../../components/PageContainer';
import PanelTable from '../../components/PanelTable';
import { getClassPage } from '../../api/classes';
import { createOrder, deleteOrder, enrollOrder, getOrderDetail, getOrderPage, updateOrder } from '../../api/orders';
import { getProductPage } from '../../api/products';
import type { ClassEntity, OrderCreateReq, OrderEntity, OrderQueryParams, OrderUpdateReq, ProductEntity } from '../../types/system';
import { hasPermission } from '../../utils/permission';
import CheckoutModal from './CheckoutModal';

interface SearchValues {
  keyword?: string;
  orderStatus?: OrderEntity['orderStatus'];
  fulfillmentStatus?: OrderEntity['fulfillmentStatus'];
}

interface EnrollmentFormValues {
  password: string;
  graduationSession: number;
  school: string;
  education: string;
  joinTime: dayjs.Dayjs;
}

const fulfillmentOptions = [
  { label: '待支付', value: 'PENDING_PAYMENT' },
  { label: '待入班', value: 'PENDING_ENROLLMENT' },
  { label: '已入班', value: 'ENROLLED' },
  { label: '已关闭', value: 'CLOSED' }
];

interface OrderFormValues extends OrderCreateReq, OrderUpdateReq {
  orderNo?: string;
  studentCode?: string;
  productAmount?: number;
  receivableAmount?: number;
  paidAmount?: number;
  orderStatus?: OrderEntity['orderStatus'];
}

const orderStatusOptions = [
  { label: '未支付', value: 'UNPAID' },
  { label: '部分支付', value: 'PARTIAL_PAID' },
  { label: '已支付', value: 'PAID' },
  { label: '已关闭', value: 'CLOSED' }
] as const;

export default function OrderPage() {
  const canView = hasPermission('menu:sales:order');
  const canCreate = hasPermission('button:order:create');
  const canUpdate = hasPermission('button:order:update');
  const canDelete = hasPermission('button:order:delete');
  const canEnroll = hasPermission('button:order:enroll');
  const canCollect = hasPermission('button:order:collect');
  const [collectingOrder, setCollectingOrder] = useState<OrderEntity>();
  const [searchParams] = useSearchParams();
  const [enrollmentForm] = Form.useForm<EnrollmentFormValues>();
  const [enrollingOrder, setEnrollingOrder] = useState<OrderEntity>();
  const [enrollmentSubmitting, setEnrollmentSubmitting] = useState(false);

  const [searchForm] = Form.useForm<SearchValues>();
  const [editForm] = Form.useForm<OrderFormValues>();
  const [loading, setLoading] = useState(false);
  const [optionLoading, setOptionLoading] = useState(false);
  const [open, setOpen] = useState(false);
  const [isEdit, setIsEdit] = useState(false);
  const [currentId, setCurrentId] = useState<number>();
  const [dataSource, setDataSource] = useState<OrderEntity[]>([]);
  const [productOptions, setProductOptions] = useState<ProductEntity[]>([]);
  const [classOptions, setClassOptions] = useState<ClassEntity[]>([]);
  const [pageNum, setPageNum] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [total, setTotal] = useState(0);

  useEffect(() => {
    searchForm.setFieldsValue({
      orderStatus: (searchParams.get('orderStatus') || undefined) as SearchValues['orderStatus'],
      fulfillmentStatus: (searchParams.get('fulfillmentStatus') || undefined) as SearchValues['fulfillmentStatus']
    });
    if (pageNum !== 1) setPageNum(1);
    else if (canView) fetchData();
  }, [searchParams]);

  const openEnrollment = (record: OrderEntity) => {
    enrollmentForm.resetFields();
    enrollmentForm.setFieldsValue({ joinTime: dayjs() });
    setEnrollingOrder(record);
  };

  const submitEnrollment = async () => {
    if (!enrollingOrder) return;
    const values = await enrollmentForm.validateFields();
    setEnrollmentSubmitting(true);
    try {
      await enrollOrder(enrollingOrder.id, {
        ...values,
        phone: enrollingOrder.phone,
        classId: enrollingOrder.classId,
        joinTime: values.joinTime.toISOString()
      });
      message.success('入班办理成功，已建立学员档案并关联课程商品和班级');
      setEnrollingOrder(undefined);
      enrollmentForm.resetFields();
      await fetchData();
    } finally {
      setEnrollmentSubmitting(false);
    }
  };

  const selectedProductId = Form.useWatch('productId', editForm);
  const selectedDiscountAmount = Form.useWatch('discountAmount', editForm);

  const selectedProduct = useMemo(
    () => productOptions.find((item) => item.id === selectedProductId),
    [productOptions, selectedProductId]
  );
  const selectedStudyMode = selectedProduct?.studyMode;

  useEffect(() => {
    const productAmount = Number(selectedProduct?.productPrice ?? 0);
    const discountAmount = Number(selectedDiscountAmount ?? 0);
    const receivableAmount = Math.max(productAmount - discountAmount, 0);
    editForm.setFieldsValue({
      productAmount,
      receivableAmount,
      paidAmount: editForm.getFieldValue('paidAmount') ?? 0,
      orderStatus: editForm.getFieldValue('orderStatus') ?? 'UNPAID'
    } as any);
  }, [selectedProduct, selectedDiscountAmount]);

  useEffect(() => {
    if (!selectedProductId || selectedStudyMode === 'ONLINE') {
      editForm.setFieldValue('classId', undefined);
    }
  }, [selectedProductId, selectedStudyMode, editForm]);

  const fetchData = async () => {
    setLoading(true);
    try {
      const values = searchForm.getFieldsValue();
      const result = await getOrderPage({ pageNum, pageSize, ...values } as OrderQueryParams);
      setDataSource(result.records);
      setTotal(result.total);
    } finally {
      setLoading(false);
    }
  };

  const fetchOptions = async () => {
    setOptionLoading(true);
    try {
      const [productResult, classResult] = await Promise.all([
        getProductPage({ pageNum: 1, pageSize: 1000 }),
        getClassPage({ pageNum: 1, pageSize: 1000 })
      ]);
      setProductOptions(productResult.records);
      setClassOptions(classResult.records);
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
  }, [canView]);

  const onCreate = () => {
    setIsEdit(false);
    setCurrentId(undefined);
    editForm.resetFields();
    editForm.setFieldsValue({
      discountAmount: 0,
      productAmount: 0,
      receivableAmount: 0,
      paidAmount: 0,
      orderStatus: 'UNPAID'
    } as any);
    setOpen(true);
  };

  const onEdit = async (record: OrderEntity) => {
    const detail = await getOrderDetail(record.id);
    setIsEdit(true);
    setCurrentId(record.id);
    editForm.setFieldsValue({
      orderNo: detail.orderNo,
      studentName: detail.studentName,
      phone: detail.phone,
      studentCode: detail.studentCode,
      productId: detail.productId,
      classId: detail.classId,
      discountAmount: Number(detail.discountAmount),
      productAmount: Number(detail.productAmount),
      receivableAmount: Number(detail.receivableAmount),
      paidAmount: Number(detail.paidAmount),
      orderStatus: detail.orderStatus
    } as any);
    setOpen(true);
  };

  const onDelete = async (id: number) => {
    await deleteOrder(id);
    message.success('删除成功');
    fetchData();
  };

  const submitEdit = async () => {
    try {
      const values = await editForm.validateFields();
      const payload = {
        studentName: values.studentName.trim(),
        phone: values.phone.trim(),
        productId: values.productId,
        discountAmount: values.discountAmount ?? 0,
        classId: selectedStudyMode === 'OFFLINE' ? values.classId : undefined
      } as OrderCreateReq;

      if (isEdit && currentId) {
        await updateOrder(currentId, payload as OrderUpdateReq);
        message.success('更新成功');
      } else {
        await createOrder(payload);
        message.success('创建成功');
      }
      setOpen(false);
      fetchData();
    } catch (error: any) {
      const firstError = error?.errorFields?.[0];
      if (firstError?.name) {
        editForm.scrollToField(firstError.name, { block: 'center' });
      } else {
        throw error;
      }
    }
  };

  const columns: ColumnsType<OrderEntity> = [
    { title: 'ID', dataIndex: 'id', width: 80 },
    { title: '订单编号', dataIndex: 'orderNo', width: 180 },
    { title: '学员姓名', dataIndex: 'studentName', width: 140 },
    { title: '手机号', dataIndex: 'phone', width: 160 },
    { title: '学员编号', dataIndex: 'studentCode', width: 160 },
    {
      title: '入班进度', dataIndex: 'fulfillmentStatus', width: 120,
      render: (value) => <Tag color={value === 'ENROLLED' ? 'green' : value === 'PENDING_ENROLLMENT' ? 'blue' : 'default'}>
        {fulfillmentOptions.find(item => item.value === value)?.label || '—'}
      </Tag>
    },
    { title: '课程商品', dataIndex: 'productName', width: 200, render: (value) => value || '-' },
    {
      title: '学习方式',
      dataIndex: 'productStudyMode',
      width: 120,
      render: (value) => (value === 'OFFLINE' ? '线下' : value === 'ONLINE' ? '线上' : '-')
    },
    {
      title: '班级期数',
      dataIndex: 'classTerm',
      width: 120,
      render: (value) => (value ? `第 ${value} 期` : '-')
    },
    { title: '课程商品金额(元)', dataIndex: 'productAmount', width: 140, render: renderMoney },
    { title: '优惠金额(元)', dataIndex: 'discountAmount', width: 140, render: renderMoney },
    { title: '应收金额(元)', dataIndex: 'receivableAmount', width: 140, render: renderMoney },
    { title: '已付金额(元)', dataIndex: 'paidAmount', width: 140, render: renderMoney },
    {
      title: '订单状态',
      dataIndex: 'orderStatus',
      width: 120,
      render: (value: OrderEntity['orderStatus']) => (
        <Tag color={getOrderStatusColor(value)} bordered={false}>
          {getOrderStatusLabel(value)}
        </Tag>
      )
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
      width: 250,
      render: (_, record) => (
        <Space>
          {canCollect && ['UNPAID', 'PARTIAL_PAID'].includes(record.orderStatus) ? (
            <Button type="link" onClick={() => setCollectingOrder(record)}>收款</Button>
          ) : null}
          {canEnroll && record.fulfillmentStatus === 'PENDING_ENROLLMENT' ? (
            <Button type="link" onClick={() => openEnrollment(record)}>办理入班</Button>
          ) : null}
          {canUpdate && record.orderStatus === 'UNPAID' ? (
            <Button type="link" onClick={() => onEdit(record)}>
              编辑
            </Button>
          ) : null}
          {canDelete && record.orderStatus === 'UNPAID' ? (
            <Popconfirm title="确认删除该订单吗？" onConfirm={() => onDelete(record.id)}>
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
      title="报名订单"
      description="管理培训报名与收款，跟踪支付结果，为已支付订单办理建档入班。"
      extra={canCreate ? <Button type="primary" onClick={onCreate}>新增报名</Button> : null}
    >
      <Form form={searchForm} layout="inline" className="search-form" onFinish={fetchData}>
        <Form.Item name="fulfillmentStatus">
          <Select allowClear placeholder="入班进度" style={{ width: 140 }} options={fulfillmentOptions} />
        </Form.Item>
        <Form.Item name="keyword">
          <Input placeholder="订单编号/学员姓名/手机号/学员编号/课程商品" allowClear style={{ width: 320 }} prefix={<BarsOutlined />} />
        </Form.Item>
        <Form.Item name="orderStatus">
          <Select allowClear placeholder="订单状态" style={{ width: 160 }} options={orderStatusOptions as any} />
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

      {canCreate || canUpdate ? (
        <Modal
          title={isEdit ? '编辑报名订单' : '新增报名'}
          open={open}
          onOk={submitEdit}
          onCancel={() => setOpen(false)}
          destroyOnClose
        >
          <Form form={editForm} layout="vertical">
            <Form.Item label="订单编号" name="orderNo">
              <Input disabled placeholder={isEdit ? '系统生成订单编号' : '保存后由系统自动生成'} />
            </Form.Item>
            <Form.Item label="学员姓名" name="studentName" rules={[{ required: true, message: '请输入学员姓名' }]}>
              <Input placeholder="请输入学员姓名" />
            </Form.Item>
            <Form.Item
              label="手机号"
              name="phone"
              rules={[
                { required: true, message: '请输入手机号' },
                { pattern: /^1\d{10}$/, message: '请输入正确的11位手机号' }
              ]}
            >
              <Input placeholder="请输入手机号" />
            </Form.Item>
            <Form.Item label="学员编号" name="studentCode">
              <Input disabled placeholder={isEdit ? '系统生成学员编号' : '保存后由系统自动生成'} />
            </Form.Item>
            <Form.Item label="课程商品" name="productId" rules={[{ required: true, message: '请选择课程商品' }]}>
              <Select
                showSearch
                placeholder="请选择课程商品"
                loading={optionLoading}
                optionFilterProp="label"
                options={productOptions.map((item) => ({
                  label: `${item.productName} (${item.productCode}) - ${item.studyMode === 'OFFLINE' ? '线下' : '线上'}`,
                  value: item.id
                }))}
              />
            </Form.Item>
            <Form.Item
              label="班级期数"
              name="classId"
              rules={selectedStudyMode === 'OFFLINE' ? [{ required: true, message: '线下课程商品必须选择班级期数' }] : []}
            >
              <Select
                showSearch
                allowClear
                disabled={selectedStudyMode !== 'OFFLINE'}
                placeholder={selectedStudyMode === 'OFFLINE' ? '请选择班级期数' : '线上课程商品无需选择班级期数'}
                loading={optionLoading}
                optionFilterProp="label"
                options={classOptions.map((item) => ({
                  label: `第 ${item.classTerm} 期${item.courseName ? ` - ${item.courseName}` : ''}${item.campusName ? ` / ${item.campusName}` : ''}`,
                  value: item.id
                }))}
              />
            </Form.Item>
            <Form.Item label="课程商品金额" name="productAmount">
              <InputNumber disabled min={0} precision={2} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item label="优惠金额" name="discountAmount" rules={[{ required: true, message: '请输入优惠金额' }]}>
              <InputNumber min={0} precision={2} style={{ width: '100%' }} placeholder="请输入优惠金额" />
            </Form.Item>
            <Form.Item label="应收金额" name="receivableAmount">
              <InputNumber disabled min={0} precision={2} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item label="已付金额" name="paidAmount">
              <InputNumber disabled min={0} precision={2} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item label="订单状态" name="orderStatus">
              <Select disabled options={orderStatusOptions as any} />
            </Form.Item>
          </Form>
        </Modal>
      ) : null}
      <CheckoutModal order={collectingOrder} onClose={() => setCollectingOrder(undefined)} onPaid={() => { message.success('付款已到账，订单已更新'); void fetchData(); }} />
      <Modal title="办理学员入班" open={Boolean(enrollingOrder)} onOk={submitEnrollment}
        confirmLoading={enrollmentSubmitting} okText="确认建档入班" cancelText="取消"
        onCancel={() => { setEnrollingOrder(undefined); enrollmentForm.resetFields(); }} destroyOnClose width={640}>
        <Alert type="info" showIcon message="依据已支付报名订单建立学员档案，课程商品和班级沿用订单信息。" style={{ marginBottom: 16 }} />
        <Descriptions size="small" column={2} bordered>
          <Descriptions.Item label="学员姓名">{enrollingOrder?.studentName}</Descriptions.Item>
          <Descriptions.Item label="手机号">{enrollingOrder?.phone}</Descriptions.Item>
          <Descriptions.Item label="课程商品" span={2}>{enrollingOrder?.productName}</Descriptions.Item>
          <Descriptions.Item label="入班安排" span={2}>{enrollingOrder?.productStudyMode === 'OFFLINE' ? '线下培训 · 第 ' + enrollingOrder?.classTerm + ' 期' : '线上培训'}</Descriptions.Item>
        </Descriptions>
        <Form form={enrollmentForm} layout="vertical" style={{ marginTop: 20 }}>
          <Form.Item name="password" label="学员初始登录密码" rules={[{ required: true, message: '请设置登录密码' }, { min: 8, max: 64, message: '密码长度为8至64位' }, { validator: (_, value) => !value || value === value.trim() ? Promise.resolve() : Promise.reject(new Error('密码首尾不能包含空格')) }]}>
            <Input.Password autoComplete="new-password" placeholder="设置8至64位密码" />
          </Form.Item>
          <div className="enrollment-form-grid">
            <Form.Item name="graduationSession" label="毕业年份" rules={[{ required: true, message: '请输入毕业年份' }]}>
              <InputNumber min={1950} max={2100} precision={0} style={{ width: '100%' }} placeholder="例如：2026" />
            </Form.Item>
            <Form.Item name="education" label="最高学历" rules={[{ required: true, message: '请选择最高学历' }]}>
              <Select options={['初中及以下', '高中 / 中专', '大专', '本科', '硕士', '博士', '其他'].map(label => ({ label, value: label }))} placeholder="请选择学历" />
            </Form.Item>
          </div>
          <Form.Item name="school" label="毕业院校" rules={[{ required: true, whitespace: true, message: '请输入毕业院校' }, { max: 128, message: '最多128个字符' }]}>
            <Input placeholder="请输入毕业院校" maxLength={128} />
          </Form.Item>
          <Form.Item name="joinTime" label="入班时间" rules={[{ required: true, message: '请选择入班时间' }]}>
            <DatePicker showTime style={{ width: '100%' }} />
          </Form.Item>
        </Form>
      </Modal>
    </PageContainer>
  );
}

function renderMoney(value?: number) {
  return `¥${Number(value ?? 0).toFixed(2)}`;
}

function getOrderStatusLabel(value?: OrderEntity['orderStatus']) {
  return orderStatusOptions.find((item) => item.value === value)?.label ?? value ?? '-';
}

function getOrderStatusColor(value?: OrderEntity['orderStatus']) {
  if (value === 'PAID') {
    return 'green';
  }
  if (value === 'PARTIAL_PAID') {
    return 'gold';
  }
  if (value === 'CLOSED') {
    return 'default';
  }
  return 'red';
}

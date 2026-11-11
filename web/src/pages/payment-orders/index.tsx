import { useEffect, useState } from 'react';
import { CreditCardOutlined } from '@ant-design/icons';
import { Button, Descriptions, Empty, Form, Input, Modal, Select, Space, Spin, Table, Tag, Tooltip, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import NoPermission from '../../components/NoPermission';
import PageContainer from '../../components/PageContainer';
import PanelTable from '../../components/PanelTable';
import { getPaymentChannelOrder, getPaymentOrderPage } from '../../api/payment-orders';
import type {
  PaymentChannelOrderEntity,
  PaymentOrderEntity,
  PaymentOrderQueryParams,
  PaymentOrderStatusCode
} from '../../types/system';
import { hasPermission } from '../../utils/permission';

interface SearchValues {
  keyword?: string;
  paymentStatus?: PaymentOrderStatusCode;
  paymentChannel?: string;
}

const paymentStatusOptions = [
  { label: '待发起', value: 'INIT' },
  { label: '下单中', value: 'PROCESSING' },
  { label: '已创建收银台单', value: 'UNIFIED_ORDER_CREATED' },
  { label: '支付成功', value: 'PAY_SUCCESS' },
  { label: '已关闭', value: 'CLOSED' },
  { label: '下单失败', value: 'FAILED' }
] as const;

const paymentChannelOptions = [
  { label: '易宝支付', value: 'YEEPAY' },
  { label: '本地演示', value: 'LOCAL_DEMO' },
  { label: '联调样本', value: 'LOCAL_FIXTURE' }
] as const;

export default function PaymentOrderPage() {
  const canView = hasPermission('menu:sales:payment-order');

  const [searchForm] = Form.useForm<SearchValues>();
  const [loading, setLoading] = useState(false);
  const [dataSource, setDataSource] = useState<PaymentOrderEntity[]>([]);
  const [channelOpen, setChannelOpen] = useState(false);
  const [channelLoading, setChannelLoading] = useState(false);
  const [currentPaymentOrderNo, setCurrentPaymentOrderNo] = useState<string>();
  const [channelOrder, setChannelOrder] = useState<PaymentChannelOrderEntity | null>();
  const [pageNum, setPageNum] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [total, setTotal] = useState(0);

  const fetchData = async () => {
    setLoading(true);
    try {
      const values = searchForm.getFieldsValue();
      const result = await getPaymentOrderPage({ pageNum, pageSize, ...values } as PaymentOrderQueryParams);
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

  const openChannelOrder = async (record: PaymentOrderEntity) => {
    setCurrentPaymentOrderNo(record.paymentOrderNo);
    setChannelOrder(undefined);
    setChannelOpen(true);
    setChannelLoading(true);
    try {
      const result = await getPaymentChannelOrder(record.paymentOrderNo);
      setChannelOrder(result);
    } finally {
      setChannelLoading(false);
    }
  };

  const columns: ColumnsType<PaymentOrderEntity> = [
    { title: 'ID', dataIndex: 'id', width: 80 },
    { title: '支付订单号', dataIndex: 'paymentOrderNo', width: 190 },
    { title: '业务订单号', dataIndex: 'orderNo', width: 180 },
    { title: '学员姓名', dataIndex: 'studentName', width: 130 },
    { title: '手机号', dataIndex: 'phone', width: 150 },
    { title: '学员编号', dataIndex: 'studentCode', width: 160 },
    { title: '课程商品', dataIndex: 'productName', width: 200, render: (value) => value || '-' },
    { title: '支付金额(元)', dataIndex: 'paymentAmount', width: 130, render: renderMoney },
    { title: '支付渠道', dataIndex: 'paymentChannel', width: 120, render: getPaymentChannelLabel },
    {
      title: '支付状态',
      dataIndex: 'paymentStatus',
      width: 150,
      render: (value: PaymentOrderStatusCode) => (
        <Tag color={getPaymentStatusColor(value)} bordered={false}>
          {getPaymentStatusLabel(value)}
        </Tag>
      )
    },
    { title: '易宝收款订单号', dataIndex: 'uniqueOrderNo', width: 180, render: (value) => value || '-' },
    {
      title: '最近错误',
      dataIndex: 'lastErrorMsg',
      width: 220,
      ellipsis: true,
      render: (value?: string) =>
        value ? (
          <Tooltip title={value}>
            <Typography.Text type="danger">{value}</Typography.Text>
          </Tooltip>
        ) : (
          '-'
        )
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      width: 180,
      render: (value) => (value ? dayjs(value).format('YYYY-MM-DD HH:mm:ss') : '-')
    },
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
      width: 120,
      render: (_, record) => (
        <Button type="link" onClick={() => openChannelOrder(record)}>
          扩展信息
        </Button>
      )
    }
  ];

  if (!canView) {
    return <NoPermission />;
  }

  return (
    <PageContainer title="支付流水" description="跟踪培训报名收款，查询支付单、渠道流水、回调结果与失败原因。">
      <Form form={searchForm} layout="inline" className="search-form" onFinish={fetchData}>
        <Form.Item name="keyword">
          <Input
            placeholder="支付订单号/业务订单号/学员/手机号/课程商品/渠道单号"
            allowClear
            style={{ width: 390 }}
            prefix={<CreditCardOutlined />}
          />
        </Form.Item>
        <Form.Item name="paymentStatus">
          <Select allowClear placeholder="支付状态" style={{ width: 180 }} options={paymentStatusOptions as any} />
        </Form.Item>
        <Form.Item name="paymentChannel">
          <Select allowClear placeholder="支付渠道" style={{ width: 140 }} options={paymentChannelOptions as any} />
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
          scroll={{ x: 2350 }}
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

      <Modal
        title="订单扩展信息"
        open={channelOpen}
        width={920}
        footer={null}
        destroyOnClose
        onCancel={() => setChannelOpen(false)}
      >
        <Spin spinning={channelLoading}>
          {channelOrder === undefined ? null : channelOrder ? (
            <Space direction="vertical" size={16} style={{ width: '100%' }}>
              <Descriptions bordered size="small" column={2}>
                <Descriptions.Item label="支付订单号">{valueOrDash(channelOrder.paymentOrderNo)}</Descriptions.Item>
                <Descriptions.Item label="易宝收款订单号">{valueOrDash(channelOrder.uniqueOrderNo)}</Descriptions.Item>
                <Descriptions.Item label="商户编号">{valueOrDash(channelOrder.merchantNo)}</Descriptions.Item>
                <Descriptions.Item label="商户收款请求号">{valueOrDash(channelOrder.orderId)}</Descriptions.Item>
                <Descriptions.Item label="渠道订单号">{valueOrDash(channelOrder.channelOrderId)}</Descriptions.Item>
                <Descriptions.Item label="渠道流水号">{valueOrDash(channelOrder.channelTrxId)}</Descriptions.Item>
                <Descriptions.Item label="银行订单号">{valueOrDash(channelOrder.bankOrderId)}</Descriptions.Item>
                <Descriptions.Item label="渠道类型">{valueOrDash(channelOrder.channel)}</Descriptions.Item>
                <Descriptions.Item label="订单金额">{renderMoney(channelOrder.orderAmount)}</Descriptions.Item>
                <Descriptions.Item label="支付金额">{renderMoney(channelOrder.payAmount)}</Descriptions.Item>
                <Descriptions.Item label="渠道状态">{valueOrDash(channelOrder.status)}</Descriptions.Item>
                <Descriptions.Item label="支付成功时间">{valueOrDash(channelOrder.paySuccessDate)}</Descriptions.Item>
                <Descriptions.Item label="失败码">{valueOrDash(channelOrder.failCode)}</Descriptions.Item>
                <Descriptions.Item label="失败原因">{valueOrDash(channelOrder.failReason)}</Descriptions.Item>
                <Descriptions.Item label="回调类型">{valueOrDash(channelOrder.callbackType)}</Descriptions.Item>
                <Descriptions.Item label="回调时间">{formatDateTime(channelOrder.callbackTime)}</Descriptions.Item>
                <Descriptions.Item label="创建时间">{formatDateTime(channelOrder.createdAt)}</Descriptions.Item>
                <Descriptions.Item label="更新时间">{formatDateTime(channelOrder.updatedAt)}</Descriptions.Item>
              </Descriptions>

              <TextBlock title="付款信息" value={channelOrder.payerInfo} />
              <TextBlock title="回调原文" value={channelOrder.callbackBody} />
            </Space>
          ) : (
            <Empty description={`未找到 ${currentPaymentOrderNo ?? ''} 对应的渠道扩展信息`} />
          )}
        </Spin>
      </Modal>
    </PageContainer>
  );
}

function renderMoney(value?: number) {
  return `¥${Number(value ?? 0).toFixed(2)}`;
}

function getPaymentChannelLabel(value?: string) {
  return paymentChannelOptions.find((item) => item.value === value)?.label ?? value ?? '-';
}

function getPaymentStatusLabel(value?: PaymentOrderStatusCode) {
  return paymentStatusOptions.find((item) => item.value === value)?.label ?? value ?? '-';
}

function getPaymentStatusColor(value?: PaymentOrderStatusCode) {
  if (value === 'PAY_SUCCESS') {
    return 'green';
  }
  if (value === 'UNIFIED_ORDER_CREATED' || value === 'PROCESSING') {
    return 'blue';
  }
  if (value === 'FAILED') {
    return 'red';
  }
  if (value === 'CLOSED') {
    return 'default';
  }
  return 'gold';
}

function valueOrDash(value?: string | number) {
  if (value === undefined || value === null || value === '') {
    return '-';
  }
  return value;
}

function formatDateTime(value?: string) {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm:ss') : '-';
}

function formatJsonText(value?: string) {
  if (!value) {
    return '';
  }
  try {
    return JSON.stringify(JSON.parse(value), null, 2);
  } catch {
    return value;
  }
}

function TextBlock(props: { title: string; value?: string }) {
  const text = formatJsonText(props.value);
  return (
    <div>
      <Typography.Text strong>{props.title}</Typography.Text>
      {text ? (
        <Input.TextArea value={text} readOnly autoSize={{ minRows: 4, maxRows: 10 }} style={{ marginTop: 8 }} />
      ) : (
        <div style={{ marginTop: 8 }}>
          <Typography.Text type="secondary">-</Typography.Text>
        </div>
      )}
    </div>
  );
}

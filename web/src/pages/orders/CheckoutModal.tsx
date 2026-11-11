import { useEffect, useRef, useState } from 'react';
import { Alert, Button, Descriptions, Grid, Modal, QRCode, Space, Spin, Tag, Typography, message } from 'antd';
import dayjs from 'dayjs';
import { createCheckout, getCheckout, type CheckoutView } from '../../api/checkout';
import type { OrderEntity } from '../../types/system';

const money = (value?: number) => '¥' + Number(value ?? 0).toFixed(2);
const openStatuses = ['INIT', 'PROCESSING', 'UNIFIED_ORDER_CREATED'];

export default function CheckoutModal({ order, onClose, onPaid }: {
  order?: OrderEntity; onClose: () => void; onPaid: () => void;
}) {
  const screens = Grid.useBreakpoint();
  const columnCount = screens.sm ? 2 : 1;
  const [view, setView] = useState<CheckoutView>();
  const [loading, setLoading] = useState(false);
  const [creating, setCreating] = useState(false);
  const [error, setError] = useState('');
  const generation = useRef(0);
  const paidNotified = useRef(false);
  const onPaidRef = useRef(onPaid);
  onPaidRef.current = onPaid;

  const apply = (result: CheckoutView) => {
    setView(result);
    if (result.orderStatus === 'PAID' && !paidNotified.current) {
      paidNotified.current = true; onPaidRef.current();
    }
  };

  useEffect(() => {
    const current = ++generation.current;
    paidNotified.current = false; setView(undefined); setError(''); setCreating(false);
    if (!order) return;
    setLoading(true);
    void getCheckout(order.id).then(result => {
      if (generation.current === current) apply(result);
    }).catch(err => {
      if (generation.current === current) setError(err instanceof Error ? err.message : '收款信息加载失败');
    }).finally(() => { if (generation.current === current) setLoading(false); });
    return () => { generation.current++; };
  }, [order?.id]);

  const refresh = async () => {
    if (!order) return;
    const current = generation.current;
    setLoading(true);
    try {
      const result = await getCheckout(order.id);
      if (generation.current === current) { apply(result); setError(''); }
    } catch (err) {
      if (generation.current === current) setError(err instanceof Error ? err.message : '查询失败');
    } finally { if (generation.current === current) setLoading(false); }
  };

  const generate = async () => {
    if (!order) return;
    const current = generation.current;
    setCreating(true); setError('');
    try {
      const result = await createCheckout(order.id);
      if (current === generation.current) apply(result);
    } catch (err) {
      if (current === generation.current) {
        const detail = err instanceof Error ? err.message : '收银台生成失败';
        try {
          const latest = await getCheckout(order.id, true);
          if (current === generation.current) apply(latest);
        } catch { /* 保留原始下单错误 */ }
        if (current === generation.current) setError(detail);
      }
    } finally { if (current === generation.current) setCreating(false); }
  };

  // 自动查询使用独立计时，单次完成后再排下次；窗口关闭即停止。
  useEffect(() => {
    if (!order || !view?.paymentOrderNo || !openStatuses.includes(view.paymentStatus || '')) return;
    let cancelled = false;
    let timer: ReturnType<typeof setTimeout>;
    let count = 0;
    const poll = async () => {
      try {
        const result = await getCheckout(order.id, true);
        if (cancelled) return;
        apply(result);
        if (!openStatuses.includes(result.paymentStatus || '') || result.orderStatus === 'PAID') return;
      } catch { return; }
      if (!cancelled && count++ < 75) timer = setTimeout(poll, 4000);
    };
    timer = setTimeout(poll, 4000);
    return () => { cancelled = true; clearTimeout(timer); };
  }, [order?.id, view?.paymentOrderNo]);

  const paid = view?.orderStatus === 'PAID';
  const canGenerate = view?.ready && !paid && view.orderStatus !== 'CLOSED'
    && Number(view.remainingAmount) > 0 && !openStatuses.includes(view.paymentStatus || '');
  const safeUrl = view?.cashierUrl?.startsWith('https://') ? view.cashierUrl : undefined;

  return <Modal title="报名订单收款" open={Boolean(order)} onCancel={onClose} footer={null} width={620} destroyOnClose>
    <Spin spinning={loading}>
      <Descriptions column={columnCount} size="small" bordered style={{ marginBottom: 16 }}>
        <Descriptions.Item label="订单编号" span={columnCount}>{order?.orderNo}</Descriptions.Item>
        <Descriptions.Item label="学员">{order?.studentName}</Descriptions.Item>
        <Descriptions.Item label="收款渠道">易宝支付 {view?.ready && <Tag color={view.sandbox ? 'gold' : 'blue'}>{view.sandbox ? '沙箱' : '正式'}</Tag>}</Descriptions.Item>
        <Descriptions.Item label="应收金额">{money(view?.receivableAmount ?? order?.receivableAmount)}</Descriptions.Item>
        <Descriptions.Item label="已付金额">{money(view?.paidAmount ?? order?.paidAmount)}</Descriptions.Item>
        <Descriptions.Item label="本次待收" span={columnCount}><Typography.Text strong style={{ fontSize: 22 }}>{money(view?.remainingAmount ?? Number(order?.receivableAmount || 0) - Number(order?.paidAmount || 0))}</Typography.Text></Descriptions.Item>
      </Descriptions>
      {view && !view.ready && <Alert type="warning" showIcon message="在线支付尚未开通" description="请联系管理员完成易宝商户接入后收款。" style={{ marginBottom: 16 }} />}
      {error && <Alert type="error" showIcon message={error} style={{ marginBottom: 16 }} />}
      {view?.paymentOrderNo && <>
        <Alert type={paid ? 'success' : 'info'} showIcon message={view.message} style={{ marginBottom: 16 }} />
        <Typography.Paragraph type="secondary">支付单号：{view.paymentOrderNo}</Typography.Paragraph>
      </>}
      {safeUrl && !paid && <div style={{ textAlign: 'center', marginBottom: 20 }}>
        <QRCode value={safeUrl} size={200} style={{ margin: '0 auto 12px' }} />
        <Typography.Paragraph type="secondary">扫码进入易宝收银台，选择可用支付方式</Typography.Paragraph>
        <Typography.Paragraph type="secondary">有效期至 {dayjs(view?.expiresAt).format('YYYY-MM-DD HH:mm:ss')}</Typography.Paragraph>
        <Space>
          <Button type="primary" href={safeUrl} target="_blank" rel="noopener noreferrer">打开收银台</Button>
          <Button onClick={async () => { try { await navigator.clipboard.writeText(safeUrl); message.success('收银台链接已复制'); } catch { message.error('复制失败，请直接打开收银台'); } }}>复制付款链接</Button>
        </Space>
      </div>}
      <Space wrap>
        {canGenerate && <Button type="primary" loading={creating} onClick={generate}>生成收银台 · {money(view?.remainingAmount)}</Button>}
        <Button onClick={refresh} loading={loading}>刷新支付结果</Button>
        <Button onClick={onClose}>{paid ? '完成' : '关闭'}</Button>
      </Space>
    </Spin>
  </Modal>;
}

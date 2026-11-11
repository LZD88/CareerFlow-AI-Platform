import { useEffect, useState } from 'react';
import { Alert, Button, Card, Descriptions, Result, Spin } from 'antd';
import { useSearchParams } from 'react-router-dom';
import { getPublicCheckoutStatus, type CheckoutView } from '../../api/checkout';

export default function PaymentReturnPage() {
  const [params] = useSearchParams();
  const id = Number(params.get('orderId'));
  const paymentNo = params.get('paymentOrderNo') || '';
  const [view, setView] = useState<CheckoutView>();
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [attempt, setAttempt] = useState(0);
  useEffect(() => {
    setView(undefined);
    if (!Number.isSafeInteger(id) || id < 1 || !/^[A-Za-z0-9_-]{6,64}$/.test(paymentNo)) {
      setError('付款返回链接不完整，请联系工作人员核实'); setLoading(false); return;
    }
    const controller = new AbortController();
    let timer: ReturnType<typeof setTimeout>;
    let count = 0;
    const poll = async () => {
      try {
        const result = await getPublicCheckoutStatus(id, paymentNo, controller.signal);
        if (controller.signal.aborted) return;
        setView(result); setError('');
        if (!['PAY_SUCCESS', 'FAILED', 'CLOSED'].includes(result.paymentStatus || '') && count++ < 60)
          timer = setTimeout(poll, 4000);
      } catch (err) {
        if (!controller.signal.aborted) setError(err instanceof Error ? err.message : '结果查询失败，请稍后刷新');
      } finally { if (!controller.signal.aborted) setLoading(false); }
    };
    setLoading(true); void poll();
    return () => { controller.abort(); clearTimeout(timer); };
  }, [id, paymentNo, attempt]);
  const paid = view?.paymentStatus === 'PAY_SUCCESS';
  const closed = view?.orderStatus === 'CLOSED' || ['FAILED', 'CLOSED'].includes(view?.paymentStatus || '');
  return <main style={{ minHeight: '100vh', background: '#f4f7fc', padding: '64px 16px' }}>
    <Card style={{ maxWidth: 640, margin: '0 auto' }}>
      <Spin spinning={loading}>
        <Result status={error ? 'warning' : paid ? 'success' : closed ? 'warning' : 'info'}
          title={error ? '暂时无法查询付款结果' : paid ? '付款已到账' : closed ? '请核实本次付款结果' : '正在确认付款结果'}
          subTitle={error || view?.message || '正在向平台查询支付结果，请稍候'} />
        {view && <Descriptions column={1} bordered size="small" style={{ marginBottom: 20 }}>
          <Descriptions.Item label="报名订单">{view.orderNo}</Descriptions.Item>
          <Descriptions.Item label="支付单号">{view.paymentOrderNo}</Descriptions.Item>
          <Descriptions.Item label="本次金额">¥{Number(view.paymentAmount || 0).toFixed(2)}</Descriptions.Item>
        </Descriptions>}
        {!paid && <Alert type="info" showIcon message="如已扣款，请等待结果确认，勿重复付款。" style={{ marginBottom: 20 }} />}
        <Button type="primary" onClick={() => setAttempt(v => v + 1)} loading={loading}>刷新付款结果</Button>
      </Spin>
    </Card>
  </main>;
}

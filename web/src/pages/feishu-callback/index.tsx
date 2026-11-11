import { Button, Result, Spin, message } from 'antd';
import { useEffect, useRef, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { bindFeishu, feishuLogin } from '../../api/auth';
import { setLoginSession } from '../../utils/auth';
import {
  consumeFeishuAction,
  consumeFeishuBindReturnTo,
  consumeFeishuBindState,
  saveFeishuBindStatus
} from '../../utils/feishu';

export default function FeishuCallbackPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [status, setStatus] = useState<'loading' | 'error'>('loading');
  const [errorMessage, setErrorMessage] = useState('');
  const handledRef = useRef(false);

  useEffect(() => {
    if (handledRef.current) {
      return;
    }
    handledRef.current = true;

    const code = searchParams.get('code');
    const state = searchParams.get('state');
    const error = searchParams.get('error');
    const errorDescription = searchParams.get('error_description');
    const action = consumeFeishuAction();

    if (error) {
      const messageText = errorDescription || '飞书授权已取消或失败';
      saveFeishuBindStatus('error', messageText);
      setStatus('error');
      setErrorMessage(messageText);
      return;
    }

    if (!code) {
      const messageText = '飞书回调中缺少 code 参数';
      saveFeishuBindStatus('error', messageText);
      setStatus('error');
      setErrorMessage(messageText);
      return;
    }

    const savedState = consumeFeishuBindState();
    if (!state || !savedState || state !== savedState) {
      const messageText = '飞书回调 state 校验失败，请重新发起绑定';
      saveFeishuBindStatus('error', messageText);
      setStatus('error');
      setErrorMessage(messageText);
      return;
    }

    const run = async () => {
      try {
        if (action === 'login') {
          const result = await feishuLogin(code);
          setLoginSession(result);
          saveFeishuBindStatus('success', '飞书登录成功');
          message.success('飞书登录成功');
        } else {
          await bindFeishu(code);
          saveFeishuBindStatus('success', '飞书绑定成功');
          message.success('飞书绑定成功');
        }
        navigate(consumeFeishuBindReturnTo(), { replace: true });
      } catch (errorObj: any) {
        const messageText = errorObj?.response?.data?.message || errorObj?.message || '飞书绑定失败';
        saveFeishuBindStatus('error', messageText);
        setStatus('error');
        setErrorMessage(messageText);
      }
    };

    run();
  }, [navigate, searchParams]);

  if (status === 'loading') {
    return (
      <div className="feishu-callback-page">
        <div className="feishu-callback-card">
          <Spin size="large" />
          <div className="feishu-callback-copy">
            正在处理飞书授权结果...
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="feishu-callback-page">
      <div className="feishu-callback-card">
        <Result
          status="error"
          title="飞书处理失败"
          subTitle={errorMessage}
          extra={[
            <Button key="back" type="primary" onClick={() => navigate(consumeFeishuBindReturnTo(), { replace: true })}>
              返回系统
            </Button>
          ]}
        />
      </div>
    </div>
  );
}

import { useEffect, useState } from 'react';
import { Button, Card, Divider, Form, Input, Typography, message } from 'antd';
import { CheckCircleOutlined, LinkOutlined, SolutionOutlined } from '@ant-design/icons';
import { useLocation, useNavigate } from 'react-router-dom';
import { login } from '../../api/auth';
import { platform } from '../../constants/platform';
import { setLoginSession } from '../../utils/auth';
import { buildFeishuAuthorizeUrl, canLaunchFeishuAuth, saveFeishuBindReturnTo } from '../../utils/feishu';
import type { LoginReq } from '../../types/auth';

export default function LoginPage() {
  const [form] = Form.useForm<LoginReq>();
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  const requestedPath = (location.state as { from?: string } | null)?.from;
  const redirectTo = requestedPath?.startsWith('/') && !requestedPath.startsWith('//') ? requestedPath : '/workbench';
  const feishuLoginEnabled = canLaunchFeishuAuth();
  useEffect(() => { document.title = '登录 · ' + platform.name; }, []);
  const onFinish = async () => {
    const values = await form.validateFields();
    setLoading(true);
    try {
      setLoginSession(await login(values));
      message.success('登录成功');
      navigate(redirectTo, { replace: true });
    } finally { setLoading(false); }
  };
  const onFeishuLogin = () => {
    const url = buildFeishuAuthorizeUrl('login');
    if (!url) { message.warning('飞书授权地址生成失败，请检查配置'); return; }
    saveFeishuBindReturnTo(redirectTo);
    window.location.href = url;
  };
  return (
    <div className="login-page">
      <section className="login-story">
        <div className="login-brand"><SolutionOutlined /> 职业培训 · 就业服务</div>
        <div>
          <div className="section-eyebrow">TRAINING & CAREER SERVICES</div>
          <h1>让技能成长<br />连接职业未来<span>。</span></h1>
          <p>{platform.description}</p>
          <div className="login-features">
            {['课程商品与报名支付', '学员入班与教学履约', '求职简历与 AI 面试复盘'].map(item => <div key={item}><CheckCircleOutlined /> {item}</div>)}
          </div>
        </div>
        <div className="login-story-footer">{platform.name}</div>
      </section>
      <div className="login-form-panel">
        <Card className="login-card" bordered={false}>
          <div className="section-eyebrow">工作，从这里开始</div>
          <Typography.Title level={2} className="login-title">登录运营平台</Typography.Title>
          <Typography.Paragraph className="login-subtitle">使用员工账号，协同开展培训与就业服务。</Typography.Paragraph>
          <Form form={form} layout="vertical" onFinish={onFinish}>
            <Form.Item name="username" label="员工账号" rules={[{ required: true, message: '请输入员工账号' }]}>
              <Input size="large" placeholder="请输入员工账号" autoComplete="username" />
            </Form.Item>
            <Form.Item name="password" label="登录密码" rules={[{ required: true, message: '请输入密码' }]}>
              <Input.Password size="large" placeholder="请输入登录密码" autoComplete="current-password" />
            </Form.Item>
            <Button type="primary" size="large" block loading={loading} htmlType="submit">登录平台</Button>
          </Form>
          <Divider plain>企业身份登录</Divider>
          <Button size="large" block icon={<LinkOutlined />} onClick={onFeishuLogin} disabled={!feishuLoginEnabled}>飞书登录</Button>
        </Card>
        <Typography.Text className="login-footer" type="secondary">招生 · 教学 · 学员 · 就业</Typography.Text>
      </div>
    </div>
  );
}

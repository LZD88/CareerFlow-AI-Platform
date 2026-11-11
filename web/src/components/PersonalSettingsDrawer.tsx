import {
  Button,
  Card,
  Drawer,
  Form,
  Input,
  Modal,
  Space,
  Tag,
  Typography,
  message
} from 'antd';
import {
  CheckCircleFilled,
  LinkOutlined,
  LockOutlined,
  ReloadOutlined,
  SafetyCertificateOutlined,
  UserOutlined
} from '@ant-design/icons';
import { useEffect, useState } from 'react';
import { changePassword, getAuthProfile, unbindFeishu } from '../api/auth';
import type { AuthProfileRes, ChangePasswordReq } from '../types/auth';
import { getLoginUser } from '../utils/auth';
import {
  buildFeishuAuthorizeUrl,
  canLaunchFeishuAuth,
  consumeFeishuBindStatus,
  saveFeishuBindReturnTo
} from '../utils/feishu';

interface PersonalSettingsDrawerProps {
  open: boolean;
  onClose: () => void;
}

export default function PersonalSettingsDrawer(props: PersonalSettingsDrawerProps) {
  const { open, onClose } = props;
  const loginUser = getLoginUser();
  const [passwordForm] = Form.useForm<ChangePasswordReq>();
  const [profile, setProfile] = useState<AuthProfileRes>();
  const [loading, setLoading] = useState(false);
  const [submitLoading, setSubmitLoading] = useState(false);
  const [statusMessageShown, setStatusMessageShown] = useState(false);
  const [passwordOpen, setPasswordOpen] = useState(false);

  const isBound = Boolean(profile?.unionId || loginUser?.unionId);
  const displayName = profile?.nickname || loginUser?.nickname || loginUser?.username || '管理员';
  const displayUsername = profile?.username || loginUser?.username || 'admin';

  const fetchProfile = async () => {
    setLoading(true);
    try {
      const result = await getAuthProfile();
      setProfile(result);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (!open) {
      return;
    }
    fetchProfile();
  }, [open]);

  useEffect(() => {
    if (!open || statusMessageShown) {
      return;
    }
    const bindStatus = consumeFeishuBindStatus();
    if (!bindStatus) {
      return;
    }
    if (bindStatus.status === 'success') {
      message.success(bindStatus.message);
      fetchProfile();
    } else {
      message.error(bindStatus.message);
    }
    setStatusMessageShown(true);
  }, [open, statusMessageShown]);

  useEffect(() => {
    if (!open) {
      setStatusMessageShown(false);
    }
  }, [open]);

  const handleLaunchFeishuAuth = () => {
    const url = buildFeishuAuthorizeUrl();
    if (!url) {
      message.warning('飞书授权地址生成失败，请检查配置');
      return;
    }
    saveFeishuBindReturnTo(window.location.pathname);
    window.location.href = url;
  };

  const handleUnbind = async () => {
    setSubmitLoading(true);
    try {
      await unbindFeishu();
      message.success('飞书解绑成功');
      fetchProfile();
    } finally {
      setSubmitLoading(false);
    }
  };

  const handleChangePassword = async () => {
    const values = await passwordForm.validateFields();
    setSubmitLoading(true);
    try {
      await changePassword(values);
      message.success('密码修改成功，请使用新密码登录');
      setPasswordOpen(false);
      passwordForm.resetFields();
    } finally {
      setSubmitLoading(false);
    }
  };

  return (
    <Drawer
      title="个人设置"
      placement="right"
      width={560}
      open={open}
      onClose={onClose}
      extra={<Button onClick={onClose}>关闭</Button>}
    >
      <div className="settings-shell">
        <Card
          className="settings-section-card"
          bordered={false}
          loading={loading}
          title={
            <div className="settings-section-title">
              <SafetyCertificateOutlined />
              <span>账号与安全</span>
            </div>
          }
        >
          <div className="settings-row">
            <div className="settings-row-label">登录账号</div>
            <div className="settings-row-value">
              <span className="settings-row-icon"><UserOutlined /></span>
              <span>{displayUsername}</span>
            </div>
          </div>

          <div className="settings-row">
            <div className="settings-row-label">显示名称</div>
            <div className="settings-row-value">
              <span>{displayName}</span>
            </div>
          </div>

          <div className="settings-row">
            <div className="settings-row-label">登录密码</div>
            <div className="settings-row-value">
              <span className="settings-muted">已设置</span>
              <Button type="link" icon={<LockOutlined />} onClick={() => setPasswordOpen(true)}>
                修改密码
              </Button>
            </div>
          </div>

          <div className="settings-divider" />

          <Typography.Title level={3} className="settings-subsection-title">
            第三方账号
          </Typography.Title>

          <div className="third-account-card">
            <div className="third-account-brand">
              <div className="third-account-logo">飞</div>
              <div>
                <Typography.Title level={4} className="third-account-name">
                  飞书
                </Typography.Title>
                <Typography.Text className="third-account-desc">
                  {isBound ? '已绑定飞书账号，可用于识别当前登录用户' : '绑定后可通过飞书授权自动识别当前账号'}
                </Typography.Text>
                {isBound && profile?.unionId ? (
                  <Typography.Text className="third-account-id">
                    union_id: {profile.unionId}
                  </Typography.Text>
                ) : null}
              </div>
            </div>

            <div className="third-account-actions">
              {isBound ? (
                <Space direction="vertical" size={12} align="end">
                  <Tag className="settings-status-tag settings-status-tag-success" icon={<CheckCircleFilled />}>
                    已连接
                  </Tag>
                  <Button onClick={handleUnbind} loading={submitLoading}>
                    解除绑定
                  </Button>
                </Space>
              ) : (
                <Space direction="vertical" size={12} align="end">
                  <Tag className="settings-status-tag settings-status-tag-pending">
                    待绑定
                  </Tag>
                  <Button
                    type="primary"
                    className="settings-bind-button"
                    icon={<LinkOutlined />}
                    onClick={handleLaunchFeishuAuth}
                    disabled={!canLaunchFeishuAuth()}
                  >
                    绑定飞书
                  </Button>
                </Space>
              )}
            </div>
          </div>
        </Card>

        <Card
          className="settings-section-card"
          bordered={false}
          title={
            <div className="settings-section-title">
              <LockOutlined />
              <span>安全提示</span>
            </div>
          }
          extra={<Button icon={<ReloadOutlined />} onClick={fetchProfile} loading={loading}>刷新状态</Button>}
        >
          <ul className="settings-tips">
            <li>请勿将登录密码告诉他人，管理员不会以任何方式索要你的密码。</li>
            <li>建议定期更换密码，并使用字母、数字与符号组合。</li>
            <li>绑定飞书后，请使用本人账号授权，避免绑定到错误账户。</li>
          </ul>
        </Card>
      </div>

      <Modal
        title="修改密码"
        open={passwordOpen}
        onOk={handleChangePassword}
        onCancel={() => {
          setPasswordOpen(false);
          passwordForm.resetFields();
        }}
        confirmLoading={submitLoading}
        destroyOnClose
      >
        <Form form={passwordForm} layout="vertical">
          <Form.Item
            label="旧密码"
            name="oldPassword"
            rules={[{ required: true, message: '请输入旧密码' }]}
          >
            <Input.Password placeholder="请输入旧密码" />
          </Form.Item>
          <Form.Item
            label="新密码"
            name="newPassword"
            rules={[
              { required: true, message: '请输入新密码' },
              { min: 6, message: '新密码长度不能少于6位' }
            ]}
          >
            <Input.Password placeholder="请输入新密码" />
          </Form.Item>
          <Form.Item
            label="确认新密码"
            name="confirmPassword"
            dependencies={['newPassword']}
            rules={[
              { required: true, message: '请再次输入新密码' },
              ({ getFieldValue }) => ({
                validator(_, value) {
                  if (!value || getFieldValue('newPassword') === value) {
                    return Promise.resolve();
                  }
                  return Promise.reject(new Error('两次输入的新密码不一致'));
                }
              })
            ]}
          >
            <Input.Password placeholder="请再次输入新密码" />
          </Form.Item>
        </Form>
      </Modal>
    </Drawer>
  );
}

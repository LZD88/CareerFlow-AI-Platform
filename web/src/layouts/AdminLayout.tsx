import { Avatar, Breadcrumb, Button, Layout, Menu, Space, Typography } from 'antd';
import { MenuFoldOutlined, MenuUnfoldOutlined, SolutionOutlined, UserOutlined } from '@ant-design/icons';
import { useEffect, useMemo, useState } from 'react';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import PersonalSettingsDrawer from '../components/PersonalSettingsDrawer';
import { menuItems } from '../constants/menu';
import { platform } from '../constants/platform';
import { clearLoginSession, getLoginUser } from '../utils/auth';
import { filterMenuItems } from '../utils/permission';

const { Header, Sider, Content } = Layout;

export default function AdminLayout() {
  const navigate = useNavigate();
  const { pathname } = useLocation();
  const loginUser = getLoginUser();
  const [settingsOpen, setSettingsOpen] = useState(false);
  const [collapsed, setCollapsed] = useState(false);
  const filteredMenuItems = useMemo(() => filterMenuItems(menuItems), [pathname]);
  const currentGroup = filteredMenuItems.find(item => item.children?.some(child => child.key === pathname));
  const currentPage = currentGroup?.children?.find(item => item.key === pathname)
    ?? filteredMenuItems.find(item => item.key === pathname);
  const [openKeys, setOpenKeys] = useState<string[]>(currentGroup ? [currentGroup.key] : ['/sales']);

  useEffect(() => {
    if (currentGroup) setOpenKeys(keys => keys.includes(currentGroup.key) ? keys : [...keys, currentGroup.key]);
    if (!currentPage) navigate('/workbench', { replace: true });
    document.title = (currentPage ? currentPage.label + ' · ' : '') + platform.name;
  }, [pathname, currentGroup, currentPage, navigate]);

  return (
    <Layout className="app-shell">
      <Sider width={248} collapsed={collapsed} breakpoint="lg" onBreakpoint={setCollapsed} className="app-sider">
        <div className="brand-block">
          <div className="brand-mark"><SolutionOutlined /></div>
          {!collapsed && <div className="brand-copy">
            <Typography.Text className="brand-title">职业培训 · 就业服务</Typography.Text>
            <Typography.Text className="brand-subtitle">一体化运营平台</Typography.Text>
          </div>}
        </div>
        <Menu theme="dark" mode="inline" selectedKeys={[pathname]}
          openKeys={collapsed ? [] : openKeys} onOpenChange={setOpenKeys}
          items={filteredMenuItems.map(({ description: _d, permissionCode: _p, ...item }) => ({
            ...item, children: item.children?.map(({ description: _cd, permissionCode: _cp, ...child }) => child)
          }))}
          onClick={({ key }) => navigate(key)}
        />
        {!collapsed && <div className="sider-footer">从技能成长，到职业启程</div>}
      </Sider>
      <Layout className="app-main">
        <Header className="app-header">
          <div className="header-bar">
            <Space size={14}>
              <Button type="text" aria-label={collapsed ? '展开导航' : '收起导航'}
                icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
                onClick={() => setCollapsed(!collapsed)} />
              <Breadcrumb items={[{ title: '运营平台' }, ...(currentGroup ? [{ title: currentGroup.label }] : []), { title: currentPage?.label || '业务工作台' }]} />
            </Space>
            <Space size={12}>
              <Avatar size="small" icon={<UserOutlined />} />
              <Typography.Text strong>{loginUser?.nickname || loginUser?.username || '管理员'}</Typography.Text>
              <Button type="text" onClick={() => setSettingsOpen(true)}>个人设置</Button>
              <Button type="text" onClick={() => { clearLoginSession(); navigate('/login', { replace: true }); }}>退出登录</Button>
            </Space>
          </div>
        </Header>
        <Content className="app-content"><Outlet /></Content>
      </Layout>
      <PersonalSettingsDrawer open={settingsOpen} onClose={() => setSettingsOpen(false)} />
    </Layout>
  );
}

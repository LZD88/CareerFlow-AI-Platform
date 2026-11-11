import {
  AppstoreOutlined, AudioOutlined, BankOutlined, BarChartOutlined, BarsOutlined,
  BookOutlined, CalendarOutlined, CreditCardOutlined, DashboardOutlined, DatabaseOutlined,
  EditOutlined, ProfileOutlined, SafetyCertificateOutlined, SearchOutlined, SettingOutlined,
  ShoppingOutlined, SlidersOutlined, SolutionOutlined, TeamOutlined, UserOutlined
} from '@ant-design/icons';
import type { ReactNode } from 'react';

export interface AppMenuItem {
  key: string;
  icon?: ReactNode;
  label: string;
  description?: string;
  permissionCode?: string;
  children?: AppMenuItem[];
}

// 沿用原接口、路由及权限编码，使现有角色授权继续生效。
export const menuItems: AppMenuItem[] = [
  { key: '/workbench', icon: <DashboardOutlined />, label: '业务工作台', description: '查看招生报名、教学履约与就业辅导的业务进展' },
  {
    key: '/sales', icon: <ShoppingOutlined />, label: '招生报名', children: [
      { key: '/products', icon: <AppstoreOutlined />, label: '课程商品', permissionCode: 'menu:sales:product' },
      { key: '/orders', icon: <BarsOutlined />, label: '报名订单', permissionCode: 'menu:sales:order' },
      { key: '/payment-orders', icon: <CreditCardOutlined />, label: '支付流水', permissionCode: 'menu:sales:payment-order' }
    ]
  },
  {
    key: '/teaching', icon: <BankOutlined />, label: '教学履约', children: [
      { key: '/campuses', icon: <BankOutlined />, label: '培训校区', permissionCode: 'menu:teaching:campus' },
      { key: '/courses', icon: <BookOutlined />, label: '课程体系', permissionCode: 'menu:teaching:course' },
      { key: '/classes', icon: <TeamOutlined />, label: '培训班级', permissionCode: 'menu:teaching:class' },
      { key: '/schedules', icon: <CalendarOutlined />, label: '教学排课', permissionCode: 'menu:teaching:schedule' },
      { key: '/teacher-schedules', icon: <CalendarOutlined />, label: '师资安排', permissionCode: 'menu:teaching:teacher-schedule' },
      { key: '/duties', icon: <CalendarOutlined />, label: '值班安排', permissionCode: 'menu:teaching:duty' },
      { key: '/homeworks', icon: <BookOutlined />, label: '作业考核', permissionCode: 'menu:teaching:homework' }
    ]
  },
  {
    key: '/student-management', icon: <TeamOutlined />, label: '学员档案', children: [
      { key: '/students', icon: <UserOutlined />, label: '学员档案', permissionCode: 'menu:student:manage' },
      { key: '/student-daily-moments', icon: <CalendarOutlined />, label: '学员动态', permissionCode: 'menu:student:daily-moment' }
    ]
  },
  {
    key: '/career-services', icon: <SolutionOutlined />, label: '就业服务', children: [
      { key: '/student-resumes', icon: <ProfileOutlined />, label: '求职简历', permissionCode: 'menu:student:resume' },
      { key: '/interview-reviews', icon: <AudioOutlined />, label: 'AI 面试复盘', permissionCode: 'menu:system:interview-review' },
      { key: '/interview-questions', icon: <BookOutlined />, label: '面试训练题库', permissionCode: 'menu:system:interview-review' }
    ]
  },
  {
    key: '/ai-support', icon: <DatabaseOutlined />, label: 'AI 支撑', children: [
      { key: '/knowledge-documents', icon: <DatabaseOutlined />, label: '培训知识库', permissionCode: 'menu:system:knowledge-document' },
      { key: '/data-analysis', icon: <BarChartOutlined />, label: '业务数据分析', permissionCode: 'menu:system:data-analysis' },
      { key: '/media-agent', icon: <EditOutlined />, label: '招生内容创作', permissionCode: 'menu:system:media-agent' }
    ]
  },
  {
    key: '/system', icon: <SettingOutlined />, label: '系统管理', children: [
      { key: '/users', icon: <UserOutlined />, label: '员工账号', permissionCode: 'menu:system:user' },
      { key: '/roles', icon: <TeamOutlined />, label: '岗位角色', permissionCode: 'menu:system:role' },
      { key: '/permissions', icon: <SafetyCertificateOutlined />, label: '菜单与接口权限', permissionCode: 'menu:system:permission' },
      { key: '/student-permission-templates', icon: <SafetyCertificateOutlined />, label: '学员权益模板', permissionCode: 'menu:system:student-permission-template' },
      { key: '/configs', icon: <SlidersOutlined />, label: '平台参数', permissionCode: 'menu:system:config' },
      { key: '/log-analysis', icon: <SearchOutlined />, label: '运行日志诊断', permissionCode: 'menu:system:log-analysis' }
    ]
  }
];

import { useEffect, useState } from 'react';
import { ArrowRightOutlined, BookOutlined, CheckCircleOutlined, FileTextOutlined, ReloadOutlined, SolutionOutlined, TeamOutlined } from '@ant-design/icons';
import { Alert, Button, Card, Empty, Skeleton, Space, Table, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useNavigate } from 'react-router-dom';
import dayjs from 'dayjs';
import { getClassPage } from '../../api/classes';
import { getOrderPage } from '../../api/orders';
import { getProductPage } from '../../api/products';
import { getStudentPage } from '../../api/students';
import { getStudentResumePage } from '../../api/student-resumes';
import { getInterviewReviewPage } from '../../api/interview-reviews';
import { businessStages, platform } from '../../constants/platform';
import type { OrderEntity } from '../../types/system';
import type { StudentResume } from '../../types/student-resume';
import { getLoginUser } from '../../utils/auth';
import { hasPermission } from '../../utils/permission';

interface Metric {
  key: string;
  label: string;
  detail: string;
  path: string;
  permission: string;
  value?: number;
  failed?: boolean;
}

const metrics: Metric[] = [
  { key: 'products', label: '课程商品', detail: '已配置的培训项目', path: '/products', permission: 'menu:sales:product' },
  { key: 'orders', label: '待支付报名', detail: '等待完成支付的订单', path: '/orders?orderStatus=UNPAID', permission: 'menu:sales:order' },
  { key: 'enrollment', label: '待办理入班', detail: '已支付且尚未完成入班', path: '/orders?fulfillmentStatus=PENDING_ENROLLMENT', permission: 'menu:sales:order' },
  { key: 'students', label: '学员档案', detail: '已完成建档的培训学员', path: '/students', permission: 'menu:student:manage' },
  { key: 'classes', label: '在训班级', detail: '正在开展教学的班级', path: '/classes', permission: 'menu:teaching:class' },
  { key: 'resumes', label: '求职简历', detail: '就业辅导中的简历档案', path: '/student-resumes', permission: 'menu:student:resume' },
  { key: 'interviews', label: '面试复盘', detail: '已创建的面试分析任务', path: '/interview-reviews', permission: 'menu:system:interview-review' }
];

const orderLabels: Record<string, string> = { UNPAID: '待支付', PARTIAL_PAID: '部分支付', PAID: '已支付', CLOSED: '已关闭' };
const fulfillmentLabels: Record<string, string> = { PENDING_PAYMENT: '待支付', PENDING_ENROLLMENT: '待入班', ENROLLED: '已入班', CLOSED: '已关闭' };

export default function WorkbenchPage() {
  const navigate = useNavigate();
  const [revision, setRevision] = useState(0);
  const [loading, setLoading] = useState(true);
  const [items, setItems] = useState<Metric[]>([]);
  const [orders, setOrders] = useState<OrderEntity[]>([]);
  const [resumes, setResumes] = useState<StudentResume[]>([]);
  const [ordersFailed, setOrdersFailed] = useState(false);
  const [resumesFailed, setResumesFailed] = useState(false);
  const user = getLoginUser();
  const canViewOrders = hasPermission('menu:sales:order');
  const canViewResumes = hasPermission('menu:student:resume');

  useEffect(() => {
    let active = true;
    setLoading(true);
    setOrdersFailed(false);
    setResumesFailed(false);
    const available = metrics.filter(item => hasPermission(item.permission));
    setItems(available);
    const page = { pageNum: 1, pageSize: 1 };
    const loaders: Record<string, () => Promise<{ total: number }>> = {
      products: () => getProductPage(page),
      orders: () => getOrderPage({ ...page, orderStatus: 'UNPAID' }),
      enrollment: () => getOrderPage({ ...page, fulfillmentStatus: 'PENDING_ENROLLMENT' }),
      students: () => getStudentPage(page),
      classes: () => getClassPage({ ...page, classStatus: 'TEACHING' }),
      resumes: () => getStudentResumePage(page),
      interviews: () => getInterviewReviewPage(page)
    };
    const requests = available.map(async item => {
      try {
        const result = await loaders[item.key]();
        if (active) setItems(current => current.map(metric => metric.key === item.key ? { ...metric, value: result.total } : metric));
      } catch {
        if (active) setItems(current => current.map(metric => metric.key === item.key ? { ...metric, failed: true } : metric));
      }
    });
    if (canViewOrders) requests.push(getOrderPage({ pageNum: 1, pageSize: 5 })
      .then(result => { if (active) setOrders(result.records); })
      .catch(() => { if (active) setOrdersFailed(true); }));
    if (canViewResumes) requests.push(getStudentResumePage({ pageNum: 1, pageSize: 4 })
      .then(result => { if (active) setResumes(result.records); })
      .catch(() => { if (active) setResumesFailed(true); }));
    Promise.allSettled(requests).then(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [revision, canViewOrders, canViewResumes]);

  const columns: ColumnsType<OrderEntity> = [
    { title: '报名学员', dataIndex: 'studentName', render: (value, record) => <div><strong>{value}</strong><div className="table-secondary">{record.orderNo}</div></div> },
    { title: '课程商品', dataIndex: 'productName', ellipsis: true, render: value => value || '—' },
    { title: '应收金额', dataIndex: 'receivableAmount', render: value => '¥' + Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2 }) },
    { title: '支付状态', dataIndex: 'orderStatus', render: value => <Tag color={value === 'PAID' ? 'green' : 'orange'}>{orderLabels[value] || value}</Tag> },
    { title: '入班进度', dataIndex: 'fulfillmentStatus', render: value => value ? <Tag color={value === 'ENROLLED' ? 'green' : 'default'}>{fulfillmentLabels[value] || value}</Tag> : '—' }
  ];
  return (
    <div className="workbench">
      <div className="workbench-heading">
        <div><div className="section-eyebrow">运营工作台 / OVERVIEW</div><Typography.Title level={3}>你好，{user?.nickname || user?.username || '管理员'}</Typography.Title><Typography.Text type="secondary">{dayjs().format('YYYY 年 M 月 D 日')}，一起关注学员从报名到就业的每一步。</Typography.Text></div>
        <Button icon={<ReloadOutlined />} loading={loading} onClick={() => setRevision(value => value + 1)}>刷新数据</Button>
      </div>
      <section className="workbench-hero">
        <div><div className="hero-kicker">从课程购买，到就业辅导</div><h1>{platform.name}</h1><p>{platform.description}</p><Space wrap>
          {canViewOrders && <Button type="primary" onClick={() => navigate('/orders')}>处理报名订单 <ArrowRightOutlined /></Button>}
          {canViewResumes && <Button onClick={() => navigate('/student-resumes')}>进入就业服务</Button>}
        </Space></div><div className="hero-symbol" aria-hidden="true"><SolutionOutlined /></div>
      </section>
      {items.length === 0 && <Alert type="info" showIcon message="当前账号尚未分配业务模块权限，请联系管理员配置岗位权限。" />}
      <div className="metric-grid">
        {items.map((item, index) => <Card key={item.key} className="metric-card" bordered={false} onClick={() => navigate(item.path)} hoverable>
          <div className="metric-label"><span>{item.label}</span><span className={'metric-dot tone-' + index % 4} /></div>
          {item.value === undefined && !item.failed ? <Skeleton.Input active size="small" /> : <div className="metric-value">{item.failed ? '暂不可用' : item.value?.toLocaleString()}</div>}
          <div className="metric-detail">{item.detail}<ArrowRightOutlined /></div>
        </Card>)}
      </div>
      <Card className="business-flow-card" bordered={false} title="培训与就业服务流程" extra={<Tag color="blue">全流程协同</Tag>}>
        <div className="business-flow">
          {businessStages.map((stage, index) => <button key={stage.path} className="business-stage" disabled={!hasPermission(stage.permission)} onClick={() => navigate(stage.path)}>
            <span className="stage-number">0{index + 1}</span><strong>{stage.title}</strong><span>{stage.description}</span><ArrowRightOutlined />
          </button>)}
        </div>
      </Card>
      <div className="workbench-bottom">
        {canViewOrders && <Card title={<Space><BookOutlined /> 最近报名</Space>} bordered={false} extra={<Button type="link" onClick={() => navigate('/orders')}>全部订单 <ArrowRightOutlined /></Button>}>
          {ordersFailed ? <Alert type="warning" message="报名数据加载失败，请刷新重试。" /> : <Table rowKey="id" loading={loading} columns={columns} dataSource={orders} pagination={false} size="middle" scroll={{ x: 640 }} locale={{ emptyText: <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无报名订单" /> }} />}
        </Card>}
        <Card title={<Space><TeamOutlined /> 就业辅导</Space>} bordered={false} className="career-card">
          <div className="career-intro">以简历和面试表现为依据，帮助学员做好求职准备。</div>
          {canViewResumes && (resumesFailed ? <Alert type="warning" message="简历数据加载失败，请刷新重试。" /> : loading ? <Skeleton active paragraph={{ rows: 2 }} /> : resumes.length ? <div className="resume-list">{resumes.map(resume =>
            <button className="resume-item" key={resume.id} onClick={() => navigate('/student-resumes')}>
              <span className="resume-icon"><FileTextOutlined /></span><span><strong>{resume.studentName} · {resume.resumeName}</strong><small>{resume.targetRole || '尚未填写目标岗位'}</small></span><ArrowRightOutlined />
            </button>)}</div> : <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无求职简历" />)}
          <div className="career-actions">
            {hasPermission('menu:system:interview-review') && <Button block onClick={() => navigate('/interview-reviews')} icon={<SolutionOutlined />}>AI 面试复盘</Button>}
            {hasPermission('menu:system:interview-review') && <Button block onClick={() => navigate('/interview-questions')} icon={<CheckCircleOutlined />}>面试训练题库</Button>}
            {!canViewResumes && !hasPermission('menu:system:interview-review') && <Typography.Text type="secondary">就业服务权限尚未开通</Typography.Text>}
          </div>
        </Card>
      </div>
    </div>
  );
}

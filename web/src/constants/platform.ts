export const platform = {
  name: '职业培训与就业服务一体化平台',
  shortName: '职业培训与就业服务',
  description: '连接课程报名、教学履约与就业辅导，让每一步成长都有据可循。'
};

export const businessStages = [
  { title: '课程商品', description: '配置培训项目、授课方式与报名价格', path: '/products', permission: 'menu:sales:product' },
  { title: '报名支付', description: '创建报名订单，跟踪收款与支付结果', path: '/orders', permission: 'menu:sales:order' },
  { title: '学员入班', description: '为已支付订单建档，关联课程商品和班级', path: '/orders?fulfillmentStatus=PENDING_ENROLLMENT', permission: 'menu:sales:order' },
  { title: '教学履约', description: '组织排课、师资安排与作业考核', path: '/classes', permission: 'menu:teaching:class' },
  { title: '就业辅导', description: '管理求职简历、面试复盘和训练题库', path: '/student-resumes', permission: 'menu:student:resume' }
];

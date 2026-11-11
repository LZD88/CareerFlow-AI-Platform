const { api } = require('./local-api.cjs');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const output = process.env.LOCAL_TEST_OUTPUT || path.resolve(__dirname, '../.local-run');

(async () => {
  const session = await api(null, 'POST', '/api/auth/login', { username: 'admin', password: 'password' });
  const page = { pageNum: 1, pageSize: 100 };
  const ensure = async (path, matches, data) => {
    const list = await api(session, 'GET', path, null, page);
    return list.records.find(matches)?.id || await api(session, 'POST', path, data);
  };
  const productId = await ensure('/api/sales/products', row => row.productCode === 'LOCAL_JAVA', {
    productCode: 'LOCAL_JAVA', productName: 'Java 后端开发就业班（本地联调）', studyMode: 'OFFLINE', productPrice: 9800
  });
  const campusId = await ensure('/api/teaching/campuses', row => row.campusName === '职业技能实训中心（本地联调）', {
    campusName: '职业技能实训中心（本地联调）', principalUserId: 1, address: '本地联调示例校区', contactPhone: '13800000000', status: 1
  });
  const courseId = await ensure('/api/teaching/courses', row => row.courseName === 'Java 后端就业实训（本地联调）', {
    courseName: 'Java 后端就业实训（本地联调）', courseDays: 3, feishuMaterialPath: '本地联调资料', status: 1
  });
  const classId = await ensure('/api/teaching/classes', row => row.classTerm === 202611, {
    classTerm: 202611, campusId, headTeacherId: 1, classStatus: 'TEACHING', startTime: '2026-11-11 09:00:00', courseId, studentCount: 0
  });
  const current = await api(session, 'GET', '/api/sales/orders', null, { ...page, keyword: '联调学员' });
  let orderId = current.records[0]?.id;
  if (!orderId) {
    orderId = await api(session, 'POST', '/api/sales/orders', {
      studentName: '联调学员', phone: '13800138000', productId, discountAmount: 800, classId
    });
  }
  const order = await api(session, 'GET', '/api/sales/orders/' + orderId);
  assert.equal(Number(order.receivableAmount), 9000);
  fs.mkdirSync(output, { recursive: true });
  fs.writeFileSync(output + '/fixture.json', JSON.stringify({ productId, campusId, courseId, classId, orderId, orderNo: order.orderNo, studentCode: order.studentCode, phone: order.phone }, null, 2));
  console.log('CREATED real API fixtures: course, campus, class, product, order #' + orderId);
  const checks = [
    ['/api/system/users', page], ['/api/system/roles', page], ['/api/system/permissions/tree'],
    ['/api/system/configs', page], ['/api/sales/payment-orders', page],
    ['/api/students', page], ['/api/student-resumes', page], ['/api/interview-reviews', page],
    ['/api/knowledge/documents', page], ['/api/teaching/duties', { dutyDate: '2026-11-11' }]
  ];
  const results = [];
  for (const [path, params] of checks) {
    try {
      await api(session, 'GET', path, null, params);
      results.push({ path, passed: true }); console.log('PASS ' + path);
    } catch (error) {
      results.push({ path, passed: false, error: error.message }); console.log('FAIL ' + error.message);
    }
  }
  fs.writeFileSync(output + '/api-checks.json', JSON.stringify(results, null, 2));
})().catch(error => { console.error(error.message); process.exitCode = 1; });

// 仅向独立本地库添加演示数据，不调用支付、短信、模型或云存储。
const { spawnSync } = require('node:child_process');
const fs = require('node:fs');
const path = require('node:path');
const mysql = process.env.LOCAL_MYSQL_BIN || 'mysql';
const mysqldump = process.env.LOCAL_MYSQLDUMP_BIN || (path.dirname(mysql) !== '.' ? path.join(path.dirname(mysql), process.platform === 'win32' ? 'mysqldump.exe' : 'mysqldump') : 'mysqldump');
const database = 'training_career_local';
const output = process.env.LOCAL_TEST_OUTPUT || path.resolve(__dirname, '../.local-run');
const env = { ...process.env, MYSQL_PWD: process.env.LOCAL_DB_PASSWORD || 'training-local-db' };
const args = ['-h', '127.0.0.1', '-P', '3306', '-u', process.env.LOCAL_DB_USER || 'training_local', '--default-character-set=utf8mb4', '--batch', '--raw', database];
function run(sql) {
  const result = spawnSync(mysql, args, { input: sql, encoding: 'utf8', env, maxBuffer: 16 * 1024 * 1024 });
  if (result.status !== 0) throw new Error(result.stderr || 'Local MySQL failed');
  return result.stdout;
}
const identity = run('SELECT DATABASE(),@@hostname;');
if (!identity.includes(database)) throw new Error('Unexpected database');
fs.mkdirSync(output, { recursive: true });
const stamp = new Date().toISOString().replace(/[:.]/g, '-');
const backup = path.join(output, 'before-demo-' + stamp + '.sql');
const dump = spawnSync(mysqldump, [...args.slice(0, 6), '--single-transaction', '--no-tablespaces', '--default-character-set=utf8mb4', '--result-file=' + backup, database], { encoding: 'utf8', env });
if (dump.status !== 0) throw new Error('Backup failed: ' + dump.stderr);

const sql = ["SET NAMES utf8mb4;", "CREATE TEMPORARY TABLE seed_assert (ok INT NOT NULL CHECK(ok=1));", "INSERT INTO seed_assert VALUES(GET_LOCK('training_career_demo_seed',10));", 'START TRANSACTION;'];
const ref = value => ({ raw: value });
const quote = value => value?.raw ? value.raw : value == null ? 'NULL' : value === '' ? "''" : typeof value === 'number' ? String(value) : "CONVERT(0x" + Buffer.from(String(value), 'utf8').toString('hex') + ' USING utf8mb4)';
let sequence = 0;
function ensure(table, keys, data, variable) {
  const condition = Object.entries(keys).map(([key, value]) => '`' + key + '` <=> ' + quote(value)).join(' AND ');
  const all = { ...keys, ...data };
  sql.push('INSERT INTO `' + table + '` (' + Object.keys(all).map(key => '`' + key + '`').join(',') + ') SELECT ' + Object.values(all).map(quote).join(',') + ' WHERE NOT EXISTS (SELECT 1 FROM `' + table + '` WHERE ' + condition + ');');
  if (!variable) variable = '@seed_' + (++sequence);
  sql.push('SET ' + variable + '=(SELECT id FROM `' + table + '` WHERE ' + condition + ' LIMIT 1);');
  return ref(variable);
}
// 后续重复执行沿用最初的日期锚点，避免日期变化造成课表重复或冲突。
const anchor = run("SELECT DATE_FORMAT(DATE_ADD(start_time,INTERVAL 7 DAY),'%Y-%m-%d') FROM edu_class WHERE class_term=20261101 LIMIT 1;").trim().split(/\r?\n/)[1];
const today = anchor ? new Date(anchor + 'T12:00:00') : new Date();
const date = (offset = 0, hour = null) => {
  const day = new Date(today.getFullYear(), today.getMonth(), today.getDate() + offset, 12);
  const text = [day.getFullYear(), String(day.getMonth() + 1).padStart(2, '0'), String(day.getDate()).padStart(2, '0')].join('-');
  return text + (hour == null ? '' : ' ' + String(hour).padStart(2, '0') + ':00:00');
};
const prefix = 'DEMO202611';
const password = 'Demo123456';
const opsRole = ensure('sys_role', { role_code: 'DEMO_OPERATIONS' }, { role_name: '平台运营（演示）', description: '本地演示运营账号，维护招生、教学与就业服务', status: 1 }, '@demo_ops_role');
sql.push('INSERT IGNORE INTO sys_role_permission(role_id,permission_id) SELECT ' + quote(opsRole) + ',id FROM sys_permission WHERE status=1;');
const teacherRole = ensure('sys_role', { role_code: 'teacher' }, { role_name: '授课讲师（演示）', description: '本地演示师资账号', status: 1 }, '@demo_teacher_role');
sql.push('INSERT IGNORE INTO sys_role_permission(role_id,permission_id) SELECT ' + quote(teacherRole) + ",id FROM sys_permission WHERE status=1 AND (permission_code LIKE 'menu:teaching%' OR permission_code LIKE 'menu:student%' OR permission_code LIKE 'homework:%' OR permission_code LIKE 'schedule:%' OR permission_code LIKE 'teacher-schedule:%' OR permission_code LIKE 'duty:%' OR permission_code IN ('class:view','course:view','campus:view','student:view','user:view','product:view')); ");
const operator = ensure('sys_user', { username: 'demo_ops' }, { password, nickname: '林晓·运营负责人', real_name: '林晓', phone: '19900009000', email: 'demo.ops@example.invalid', status: 1 }, '@demo_operator');
ensure('sys_user_role', { user_id: operator, role_id: opsRole }, {});
const teachers = ['李明','王芳','张伟','陈晨','刘洋','赵敏','周杰','吴婷'].map((name, index) => {
  const user = ensure('sys_user', { username: 'demo_teacher_' + (index + 1) }, { password, nickname: name + '老师', real_name: name, phone: '19900009' + String(index + 1).padStart(3, '0'), email: 'demo.teacher.' + (index + 1) + '@example.invalid', status: 1 }, '@teacher' + index);
  ensure('sys_user_role', { user_id: user, role_id: teacherRole }, {});
  return user;
});
const campuses = [
  ['杭州·滨江数字技能中心','杭州市滨江区数字产业园（演示地址）'],
  ['南京·软件谷实训中心','南京市雨花台区软件谷（演示地址）'],
  ['成都·高新就业服务中心','成都市高新区人才服务园（演示地址）']
].map(([name, address], index) => ensure('edu_campus', { campus_name: name + '（演示）' }, { principal_user_id: teachers[index], address, contact_phone: '1990000800' + index, remark: 'DEMO 本地业务展示数据', status: 1 }, '@campus' + index));
const programs = [
  { code: 'JAVA', name: 'Java 后端开发就业班', role: 'Java 后端开发工程师', price: 12800, mode: 'OFFLINE', topics: ['Java 集合与并发编程','Spring Boot 接口设计','MySQL 索引与事务','Redis 缓存与分布式锁','订单支付与回调幂等','微服务网关与权限控制','项目部署与监控','简历优化与模拟面试'] },
  { code: 'WEB', name: 'Web 前端工程实战班', role: '前端开发工程师', price: 9800, mode: 'OFFLINE', topics: ['HTML 与响应式布局','TypeScript 类型系统','React 状态与组件设计','工程化与 Vite 构建','接口联调与权限菜单','可视化图表与数据展示','前端性能优化','项目复盘与求职辅导'] },
  { code: 'AIGC', name: 'AIGC 应用开发实训班', role: 'AI 应用开发工程师', price: 15800, mode: 'OFFLINE', topics: ['提示词设计与结构化输出','大模型 API 与流式响应','文档解析与向量检索','RAG 召回与重排','Agent 工具调用','AI 任务状态与重试','效果评估与可观测性','AI 应用发布与展示'] },
  { code: 'DATA', name: '数据分析与商业决策班', role: '数据分析师', price: 8800, mode: 'OFFLINE', topics: ['SQL 查询与数据清洗','指标口径与数据建模','Python 数据处理','用户行为与留存分析','销售漏斗与营收分析','可视化与业务看板','分析报告与建议','业务案例答辩'] },
  { code: 'CLOUD', name: '云原生运维线上提升班', role: '云原生运维工程师', price: 3999, mode: 'ONLINE', topics: ['Linux 运维基础','容器镜像与 Docker','Kubernetes 编排','CI/CD 持续交付','日志与告警','发布策略与回滚','服务可用性保障','线上故障演练'] },
  { code: 'QA', name: '自动化测试线上实战班', role: '测试开发工程师', price: 2999, mode: 'ONLINE', topics: ['测试用例与边界设计','HTTP 与接口测试','Pytest 参数化','Playwright 浏览器测试','性能压测与分析','持续集成测试','缺陷定位与报告','质量保障项目实战'] }
];
for (let index = 0; index < programs.length; index++) {
  const item = programs[index];
  item.product = ensure('sale_product', { product_code: prefix + '_' + item.code }, { product_name: item.name + '（演示）', study_mode: item.mode, product_price: item.price }, '@product' + index);
  item.course = ensure('edu_course', { course_name: item.name + '·课程体系（演示）' }, { course_days: 20, feishu_material_path: '演示资料：请在课程详情查看教学大纲', status: 1 }, '@course' + index);
  item.details = [];
  for (let dayNum = 1; dayNum <= 20; dayNum++) {
    const title = item.topics[(dayNum - 1) % item.topics.length] + (dayNum > 8 ? ' · 进阶实训 ' + Math.ceil(dayNum / 8) : '');
    item.details.push(ensure('edu_course_detail', { course_id: item.course, day_num: dayNum }, { stage_name: dayNum <= 6 ? '基础能力' : dayNum <= 14 ? '项目实训' : '交付与就业', course_content: title }, '@detail' + index + '_' + dayNum));
  }
}
const classes = [];
for (let index = 0; index < 8; index++) {
  const program = programs[index % 4];
  const start = index === 6 ? 7 : index === 7 ? -50 : -7 - index;
  const state = index === 6 ? 'WAITING' : index === 7 ? 'FINISHED' : 'TEACHING';
  const id = ensure('edu_class', { class_term: 20261101 + index }, { campus_id: campuses[index % 3], head_teacher_id: teachers[index], class_status: state, start_time: date(start, 9), course_id: program.course, student_count: 0 }, '@class' + index);
  classes.push({ id, program, start, state, index });
  let dayNum = 0;
  for (let offset = 0; dayNum < 20; offset++) {
    const calendarDay = new Date(today.getFullYear(), today.getMonth(), today.getDate() + start + offset).getDay();
    const isRest = calendarDay === 0 || calendarDay === 6;
    if (!isRest) dayNum++;
    ensure('edu_class_schedule', { class_id: id, schedule_date: date(start + offset) }, {
      course_id: program.course, course_detail_id: isRest ? null : program.details[dayNum - 1], course_day_num: isRest ? null : dayNum,
      schedule_type: isRest ? 'REST' : 'CLASS', course_content: isRest ? '周末休息与自主复习（演示）' : program.topics[(dayNum - 1) % 8],
      stage_name: isRest ? null : dayNum <= 6 ? '基础能力' : dayNum <= 14 ? '项目实训' : '交付与就业', teacher_id: isRest ? null : teachers[index]
    });
  }
}
const surnames = ['陈','林','王','李','张','刘','周','吴','徐','赵','黄','孙','胡','朱','高','何'];
const given = ['宇航','雨桐','子涵','思远','佳宁'];
const schools = ['江南应用技术学院','滨江职业技术学院','南京工程学院','成都信息工程学院','金陵科技学院','浙江理工学院'];
const people = [];
for (let index = 0; index < 80; index++) {
  const number = String(index + 1).padStart(3, '0');
  const name = surnames[index % 16] + given[Math.floor(index / 16)];
  const program = programs[index % 6];
  const cohort = program.mode === 'ONLINE' ? null : classes.filter(item => item.program === program)[Math.floor(index / 6) % 2];
  const enroll = index < 48;
  const status = enroll ? 'PAID' : ['PAID','UNPAID','PAID','PARTIAL_PAID','PAID','CLOSED','UNPAID','UNPAID'][(index - 48) % 8];
  const discount = [0, 300, 500, 800][index % 4];
  const due = program.price - discount;
  const paid = status === 'PAID' ? due : status === 'PARTIAL_PAID' ? Math.floor(due / 2) : 0;
  const created = enroll ? date(cohort ? cohort.start - 10 : -30 - index % 10, 10) : date(-Math.floor((79 - index) / 8), 10);
  const orderNo = prefix + '_ORDER_' + number;
  const studentCode = prefix + '_STU_' + number;
  const phone = '19900010' + number;
  ensure('sale_order', { order_no: orderNo }, { student_name: name, phone, product_id: program.product, class_id: cohort?.id || null, student_code: studentCode, product_amount: program.price, discount_amount: discount, receivable_amount: due, paid_amount: paid, order_status: status, created_at: created, updated_at: created });
  if (paid > 0 || status === 'CLOSED') {
    ensure('sale_payment_order', { payment_order_no: prefix + '_PAY_' + number }, { order_no: orderNo, student_code: studentCode, student_name: name, phone, product_name: program.name + '（演示）', payment_amount: paid || due, payment_channel: 'LOCAL_DEMO', payment_status: paid ? 'PAY_SUCCESS' : 'CLOSED', unique_order_no: prefix + '_CHANNEL_' + number, last_error_msg: '演示流水，无真实扣款', created_at: created, updated_at: created });
  }
  if (enroll) {
    const joined = cohort ? date(cohort.start - 2, 9) : date(-18 - index % 10, 9);
    const student = ensure('edu_student', { student_code: studentCode }, { student_name: name, phone, password, graduation_session: 2024 + index % 3, school: schools[index % schools.length], education: index % 4 === 0 ? '大专' : '本科', study_mode: program.mode, class_id: cohort?.id || null, product_id: program.product, join_time: joined, created_at: joined, updated_at: date(-index % 4, 15) }, '@student' + index);
    people.push({ id: student, name, code: studentCode, cohort, program, index });
    if (index < 36) ensure('student_resume', { student_id: student, resume_name: program.role + '求职简历（演示）' }, {
      student_code: studentCode, student_name: name, target_role: program.role, content_type: 'TEXT', parse_status: 'SUCCESS', is_default: 1, status: 1,
      resume_text: '# ' + name + '\n\n目标岗位：' + program.role + '\n\n## 教育背景\n' + schools[index % schools.length] + '，计算机相关专业，' + (2024 + index % 3) + ' 届。\n\n## 技能\n' + program.topics.slice(0, 5).join('、') + '。\n\n## 实训项目\n职业培训与就业服务平台：完成需求分析、模块设计、接口联调和演示交付，能够解释业务流程及异常场景。\n\n## 个人优势\n重视沟通与文档，能根据反馈持续完善项目。\n\n（以上均为本地演示资料）',
      resume_summary: '已完成' + program.name + '实训，求职方向：' + program.role + '；待完善项目成果与量化描述。', created_at: date(-12 - index % 8, 11), updated_at: date(-Math.floor(index / 12), 16)
    });
    if (index < 40) ensure('edu_student_daily_moment', { student_no: studentCode, title: ['完成第一轮项目答辩','小组接口联调记录','模拟面试复盘收获','本周实训学习总结'][index % 4] + '（演示）' }, { content_text: '今天完成了' + program.topics[index % 8] + '练习。通过小组讨论定位问题，补充了边界用例，下一步继续完善项目文档。（本地演示动态）', record_time: date(-index % 6, 18), create_time: date(-index % 6, 18), update_time: date(-index % 6, 18) });
  }
}
for (const cohort of classes) {
  sql.push('UPDATE edu_class c SET student_count=(SELECT COUNT(*) FROM edu_student s WHERE s.class_id=c.id) WHERE c.id=' + quote(cohort.id) + ';');
  if (cohort.state !== 'TEACHING') continue;
  for (let shift = -2; shift <= 3; shift++) ensure('edu_duty_assignment', { duty_date: date(shift), duty_type: 'CLASS_NIGHT_DUTY', class_id: cohort.id }, { start_time: '19:00:00', end_time: '21:00:00', teacher_id: teachers[cohort.index], remark: '晚自习答疑、实训进度跟进（演示）' });
  const students = people.filter(person => person.cohort === cohort);
  for (let index = 0; index < 3; index++) {
    const lesson = Math.min(19, Math.max(0, -cohort.start - 2 + index));
    const assignment = ensure('homework_assignment', { class_id: cohort.id, title: '【演示】' + cohort.program.topics[index] + '·阶段作业' }, {
      description: '完成' + cohort.program.topics[index] + '实训任务，提交实现思路、关键代码和自测记录。验收关注功能完整性、异常边界与说明文档。', homework_date: date(-3 + index), course_id: cohort.program.course, course_detail_id: cohort.program.details[lesson], course_content_snapshot: cohort.program.topics[index],
      publish_teacher_id: teachers[cohort.index], publish_date: date(-3 + index, 15), deadline: date(index - 1, 22), status: index === 0 ? 'CLOSED' : 'PUBLISHED', answer_publish_status: 'UNPUBLISHED', allow_resubmit: 1, student_count_snapshot: students.length, deleted: 0,
      close_time: index === 0 ? date(-1, 23) : null, close_teacher_id: index === 0 ? teachers[cohort.index] : null
    });
    for (let position = 0; position < Math.max(0, students.length - index); position++) {
      const person = students[position], reviewed = (position + index) % 3 !== 0;
      ensure('homework_submission', { assignment_id: assignment, student_no: person.code }, {
        student_name_snapshot: person.name, submit_content: '已完成核心功能及测试，补充了输入校验和错误处理。实现说明：' + cohort.program.topics[index] + '。（演示提交）', submit_time: date(-2 + index, 17), status: reviewed ? 'REVIEWED' : 'SUBMITTED', score: reviewed ? 75 + (position * 7 + index * 3) % 24 : null,
        teacher_comment: reviewed ? '核心功能完成，建议补充异常场景测试，并在说明中解释设计取舍。（演示评语）' : null, review_teacher_id: reviewed ? teachers[cohort.index] : null, review_time: reviewed ? date(-1 + index, 19) : null, version: 1
      });
    }
  }
}
for (let shift = -2; shift <= 3; shift++) ensure('edu_duty_assignment', { duty_date: date(shift), duty_type: 'GLOBAL_NIGHT_DUTY', class_id: 0 }, { start_time: '21:00:00', end_time: '22:30:00', teacher_id: teachers[(shift + 2) % teachers.length], remark: '跨班问题协调与当日晚间巡查（演示）' });

const reviews = [];
for (let index = 0; index < 16; index++) {
  const person = people[index], done = index < 12;
  const transcript = '【演示面试文字记录】\n面试官：请介绍你的项目职责。\n学员：我负责职业培训平台的业务模块实现、接口联调和测试。\n面试官：你如何定位并处理异常？\n学员：先查看请求参数与日志，再复现边界场景，补充校验和测试。\n面试官：有哪些可以改进的地方？\n学员：需要进一步补充性能数据，并更清晰地解释方案取舍。';
  const report = '# 面试复盘报告（演示样本）\n\n> 本报告为人工构造的本地演示内容，未调用语音识别或模型生成。\n\n## 综合评价\n综合评分：' + (76 + index % 15) + '/100。表达清楚，基础知识掌握较好，项目细节仍需加强。\n\n## 做得较好\n- 能按业务流程说明个人职责。\n- 能结合日志与测试定位问题。\n\n## 需要改进\n- 补充压测方式和实际指标。\n- 解释异常重试、幂等及数据一致性。\n\n## 一周练习计划\n1. 梳理项目架构和关键接口。\n2. 练习事务、缓存和并发场景。\n3. 按问题、方案、结果的顺序重新录制项目介绍。';
  reviews.push(ensure('interview_review_task', { title: '【演示】' + person.name + '·' + person.program.role + '模拟面试' }, { candidate_name: person.name, interview_role: person.program.role, original_filename: '演示文字面试记录（无音频附件）', audio_object_key: '', audio_url: '', audio_size: 0, status: done ? 'DONE' : 'TRANSCRIBED', transcript_text: transcript, review_report: done ? report : null, question_status: done ? 'DONE' : 'NONE', question_extracted_count: done ? 2 : 0, question_created_count: done ? 2 : 0, question_merged_count: 0, created_by: operator, created_at: date(-index % 7, 14), updated_at: date(-index % 7, 15) }, '@review' + index));
}
const questions = [
  ['Java 基础','HashMap 与 ConcurrentHashMap 有什么区别？','HashMap 不保证线程安全。ConcurrentHashMap 支持并发访问，JDK 8 使用 CAS 和桶级同步等机制；需要结合使用场景说明迭代一致性和复合操作。'],
  ['Java 并发','如何理解线程池的核心参数？','从核心线程数、最大线程数、工作队列、空闲存活时间、线程工厂和拒绝策略说明任务接收流程，并依据任务耗时与资源限制配置。'],
  ['Spring','Spring 事务在哪些情况下会失效？','应结合代理机制说明自调用、方法可见性、异常类型、异常被捕获、事务传播与非 Spring 管理对象等场景，并通过集成测试验证。'],
  ['MySQL','如何分析一条慢 SQL？','先定位慢查询与执行频率，再用 EXPLAIN 检查访问路径、索引、扫描行数、排序与回表，结合数据分布优化并对比实际耗时。'],
  ['Redis','如何处理缓存穿透、击穿和雪崩？','穿透可用参数校验、空值缓存或布隆过滤；击穿可用互斥重建或逻辑过期；雪崩可用过期时间扰动、限流和分级缓存。'],
  ['分布式','支付回调如何保证幂等？','以渠道交易号或业务幂等键建立唯一约束，在事务内校验金额和状态，用条件更新或行锁推进状态，重复通知返回一致结果。'],
  ['前端工程','React 列表为什么需要稳定的 key？','key 帮助协调算法识别元素身份，稳定业务 ID 能避免重排时状态错位；动态列表一般不应使用数组下标作为 key。'],
  ['TypeScript','interface 和 type 的区别是什么？','二者都可描述对象结构。interface 支持声明合并和扩展；type 更适合联合、交叉、映射等类型表达，应根据 API 设计选用。'],
  ['AI 应用','RAG 检索链路包含哪些环节？','通常包括文档解析、切片、向量化、存储、查询改写、召回、重排、上下文拼接和生成，需评估召回质量及引用准确性。'],
  ['AI 应用','如何设计大模型流式响应接口？','定义开始、增量、完成和错误事件，处理超时与断连，保存完整回答，避免重复消费，前端需要增量渲染和取消能力。'],
  ['数据分析','如何定义和分析转化率？','先固定人群、事件、时间窗口、去重维度与分母口径，再分析整体和分群变化，避免不同统计粒度混用。'],
  ['SQL','WHERE 和 HAVING 有什么区别？','WHERE 在分组前过滤原始记录，HAVING 对分组聚合结果过滤。应说明执行顺序并检查聚合计算的业务口径。'],
  ['云原生','容器健康检查需要注意什么？','区分启动、就绪和存活探针，设置合理的超时与阈值；就绪失败应摘流，存活探针不能因外部依赖短暂异常频繁重启。'],
  ['测试开发','如何设计接口的边界测试？','覆盖必填、长度、取值边界、类型错误、权限、重复请求、并发与异常依赖，并检查响应和数据副作用。'],
  ['工程实践','如何处理接口调用超时和重试？','区分连接与读取超时，仅对可重试错误进行有界退避重试，写操作应有幂等保障，并记录请求链路与失败原因。'],
  ['项目经验','如何介绍自己在项目中的贡献？','按业务背景、个人责任、关键方案、验证方法和结果组织表达；明确自己实际实现和参与的部分，避免虚构性能数据。'],
  ['数据库','为什么需要数据库唯一约束？','唯一约束在并发写入时保护业务不变量，应用层先查后写存在竞争窗口；应处理冲突异常并返回清晰的业务结果。'],
  ['权限设计','菜单权限和接口权限如何配合？','前端菜单与按钮按权限控制展示，后端独立校验身份、接口操作和数据归属，服务端不能依赖前端是否显示入口。'],
  ['测试开发','端到端测试应如何选择关键场景？','优先覆盖登录、核心业务成功路径、关键失败路径及权限边界，使用稳定定位器和隔离数据，控制对外部依赖的影响。'],
  ['系统设计','如何设计任务状态与失败恢复？','明确允许的状态转换、幂等键、超时和重试上限，持久化任务与错误原因，避免进程重启后任务长期停留在处理中。']
];
questions.forEach(([category, question, answer], index) => {
  const task = reviews[index % 12];
  const bank = ensure('interview_question_bank', { normalized_question: question }, { category, tags: JSON.stringify([category, '演示题库', '求职训练']), standard_answer: answer, short_answer: answer, follow_up_questions: JSON.stringify(['结合你的项目举例说明。','如果出现并发或失败，你会如何验证？']), pitfalls: JSON.stringify(['只背概念，没有说明适用场景','没有说明边界和验证方式']), source_count: 1, last_source_task_id: task, created_at: date(-index % 7, 14), updated_at: date(-index % 7, 15) });
  ensure('interview_question_source', { question_id: bank, task_id: task, original_question: question }, { answer_context: '学员回答了主要概念，但缺少项目中的具体场景。（演示来源记录）', student_answer_quality: index % 3 === 0 ? 'GOOD' : 'PARTIAL', student_answer_score: 72 + index % 20, student_answer_analysis: '已覆盖基础概念，方案取舍和异常边界仍可补充。', improvement_suggestion: '用一个实际场景说明处理步骤，再补充验证方法。' });
});
sql.push("UPDATE interview_review_task t SET question_extracted_count=(SELECT COUNT(*) FROM interview_question_source s WHERE s.task_id=t.id),question_created_count=(SELECT COUNT(*) FROM interview_question_source s WHERE s.task_id=t.id) WHERE t.title LIKE '【演示】%' AND t.status='DONE';");
for (let index = 0; index < 12; index++) {
  const program = programs[index % 6];
  ensure('knowledge_document', { document_code: prefix + '_KB_' + String(index + 1).padStart(2, '0') }, { title: '【演示】' + program.name + (index < 6 ? '·学习路线与常见问题' : '·项目交付检查清单'), content: '# ' + program.name + '\n\n## 学习目标\n掌握' + program.topics.slice(0, 4).join('、') + '，完成可演示的业务实训。\n\n## 项目交付\n- 提交运行说明和接口说明。\n- 说明业务流程及权限边界。\n- 覆盖正常与异常用例。\n\n## 就业准备\n整理真实项目经历，完成简历与模拟面试。\n\n本资料为演示内容，尚未写入向量库。', source_type: 'manual', source_path: 'local-demo/' + program.code.toLowerCase(), course_id: program.course, study_mode: program.mode, version: 1, permission_scope: 'course', status: 'PENDING', chunk_count: 0, error_message: '', created_at: date(-index % 4, 9), updated_at: date(-index % 4, 9) });
}
// 事务提交前验证订单、学员、付款及班级的关联；任何失败会使 mysql 退出并回滚。
sql.push("INSERT INTO seed_assert SELECT IF(COUNT(*)=0,1,0) FROM edu_student s JOIN sale_order o ON o.student_code=s.student_code WHERE s.student_code LIKE 'DEMO202611_STU_%' AND (o.order_status<>'PAID' OR o.paid_amount<>o.receivable_amount OR s.product_id<>o.product_id OR NOT(s.class_id <=> o.class_id) OR s.phone<>o.phone);");
sql.push("INSERT INTO seed_assert SELECT IF(COUNT(*)=0,1,0) FROM sale_order o WHERE o.order_no LIKE 'DEMO202611_ORDER_%' AND (o.product_amount-o.discount_amount<>o.receivable_amount OR o.paid_amount>o.receivable_amount OR o.paid_amount<>(SELECT COALESCE(SUM(p.payment_amount),0) FROM sale_payment_order p WHERE p.order_no=o.order_no AND p.payment_status='PAY_SUCCESS'));");
sql.push('COMMIT;', "SELECT RELEASE_LOCK('training_career_demo_seed');");
fs.writeFileSync(path.join(output, 'demo-seed.sql'), sql.join('\n'));
run(sql.join('\n'));
const summarySql = [
  ['products', "SELECT COUNT(*) FROM sale_product WHERE product_code LIKE 'DEMO202611_%'"],
  ['campuses', "SELECT COUNT(*) FROM edu_campus WHERE remark='DEMO 本地业务展示数据'"],
  ['courses', "SELECT COUNT(*) FROM edu_course WHERE course_name LIKE '%课程体系（演示）'"],
  ['classes', 'SELECT COUNT(*) FROM edu_class WHERE class_term BETWEEN 20261101 AND 20261108'],
  ['orders', "SELECT COUNT(*) FROM sale_order WHERE order_no LIKE 'DEMO202611_ORDER_%'"],
  ['payments', "SELECT COUNT(*) FROM sale_payment_order WHERE payment_order_no LIKE 'DEMO202611_PAY_%'"],
  ['students', "SELECT COUNT(*) FROM edu_student WHERE student_code LIKE 'DEMO202611_STU_%'"],
  ['resumes', "SELECT COUNT(*) FROM student_resume WHERE student_code LIKE 'DEMO202611_STU_%'"],
  ['moments', "SELECT COUNT(*) FROM edu_student_daily_moment WHERE student_no LIKE 'DEMO202611_STU_%'"],
  ['schedules', 'SELECT COUNT(*) FROM edu_class_schedule s JOIN edu_class c ON c.id=s.class_id WHERE c.class_term BETWEEN 20261101 AND 20261108'],
  ['homeworks', "SELECT COUNT(*) FROM homework_assignment WHERE title LIKE '【演示】%'"],
  ['submissions', "SELECT COUNT(*) FROM homework_submission WHERE student_no LIKE 'DEMO202611_STU_%'"],
  ['reviews', "SELECT COUNT(*) FROM interview_review_task WHERE title LIKE '【演示】%'"],
  ['questions', "SELECT COUNT(*) FROM interview_question_bank WHERE tags LIKE '%演示题库%'"],
  ['knowledge', "SELECT COUNT(*) FROM knowledge_document WHERE document_code LIKE 'DEMO202611_KB_%'"]
].map(([name, query]) => 'SELECT ' + quote(name) + ' AS kind,(' + query + ') AS count').join(' UNION ALL ') + ';';
const summary = Object.fromEntries(run(summarySql).trim().split(/\r?\n/).slice(1).map(line => { const [key, count] = line.split('\t'); return [key, Number(count)]; }));
fs.writeFileSync(path.join(output, 'demo-summary.json'), JSON.stringify({ database, createdAt: new Date().toISOString(), counts: summary, operator: 'demo_ops', student: prefix + '_STU_001', backup }, null, 2));
console.log(JSON.stringify(summary, null, 2));
console.log('Demo account: demo_ops / ' + password);
console.log('Demo student: ' + prefix + '_STU_001 / ' + password);
console.log('Backup: ' + backup);

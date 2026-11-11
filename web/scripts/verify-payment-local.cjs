const { chromium } = require('playwright');
const fs = require('node:fs'), path = require('node:path'), assert = require('node:assert/strict');
const front = path.resolve(__dirname, '..');
const javaContainer = path.resolve(front, '../admin');
const java = process.env.LOCAL_JAVA_PROJECT || (fs.existsSync(path.join(javaContainer,'pom.xml')) ? javaContainer
  : fs.readdirSync(javaContainer).map(x=>path.join(javaContainer,x)).find(x=>fs.existsSync(path.join(x,'pom.xml'))));
const { api, headers, base } = require(path.join(java, 'scripts/local-api.cjs'));
const output = process.env.LOCAL_TEST_OUTPUT || path.join(front,'.local-run/payment');
fs.mkdirSync(output,{recursive:true});
const results = [], pageErrors = [];
function pass(kind, name) { results.push({kind,name}); console.log('PASS ['+kind+'] '+name); }
async function settledDialog(page) {
  await page.locator('.ant-modal:visible').evaluate(async el => {
    await new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve)));
    await Promise.all(el.getAnimations({subtree:true}).filter(a=>a.effect.getTiming().iterations!==Infinity).map(a=>a.finished.catch(()=>{})));
  });
}
async function raw(session, method, route, data) {
  const response = await fetch(base+route,{method,headers:headers(session,method,route,data),body:data?JSON.stringify(data):undefined});
  return response.json();
}
(async()=>{
  const browser = await chromium.launch({headless:true,...(process.env.PLAYWRIGHT_CHROME_PATH ? {executablePath:process.env.PLAYWRIGHT_CHROME_PATH} : {})});
  const page = await browser.newPage({viewport:{width:1440,height:1000}});
  page.on('pageerror',e=>pageErrors.push(e.message));
  try {
    await page.goto('http://127.0.0.1:5173/login');
    await page.getByLabel('员工账号',{exact:true}).fill('demo_ops');
    await page.getByLabel('登录密码',{exact:true}).fill('Demo123456');
    const loginResponse = page.waitForResponse(r=>r.url().endsWith('/api/auth/login')&&r.request().method()==='POST');
    await page.getByRole('button',{name:'登录平台',exact:true}).click();
    const session = (await (await loginResponse).json()).data;
    await page.waitForURL('**/workbench');
    assert(session.permissionCodes.includes('button:order:collect'));
    pass('真实接口','运营岗位获得收款按钮和接口权限');
    const orders = await api(session,'GET','/api/sales/orders',null,{pageNum:1,pageSize:1000});
    const partial = orders.records.find(x=>x.orderStatus==='PARTIAL_PAID');
    const unpaid = orders.records.find(x=>x.orderStatus==='UNPAID');
    const paid = orders.records.find(x=>x.orderStatus==='PAID');
    assert(partial && unpaid && paid,'先执行 seed-demo.cjs 准备演示订单');
    const partialPreview = await api(session,'GET','/api/payments/orders/'+partial.id+'/checkout');
    assert.equal(Number(partialPreview.remainingAmount),Number(partial.receivableAmount)-Number(partial.paidAmount));
    assert.equal(partialPreview.ready,false);
    assert(partialPreview.missingConfig.some(x=>x.includes('商户号')));
    assert(partialPreview.missingConfig.some(x=>x.includes('SDK')));
    pass('真实接口','未配置商户时明确不可收款，部分支付金额按余额计算');
    assert.equal((await raw(null,'GET','/api/payments/orders/'+partial.id+'/checkout')).code,401);
    pass('真实接口','未登录不能访问管理端收银台');
    const before = await api(session,'GET','/api/sales/payment-orders',null,{pageNum:1,pageSize:1});
    assert.equal((await raw(session,'POST','/api/payments/orders/'+partial.id+'/checkout',{amount:0.01})).code,503);
    const after = await api(session,'GET','/api/sales/payment-orders',null,{pageNum:1,pageSize:1});
    assert.equal(before.total,after.total);
    pass('真实接口','配置不齐禁止下单，不产生虚假支付记录');
    const teacher = await api(null,'POST','/api/auth/login',{username:'demo_teacher_1',password:'Demo123456'});
    assert.equal((await raw(teacher,'POST','/api/payments/orders/'+partial.id+'/checkout',{})).code,403);
    pass('真实接口','讲师岗位无收款权限');

    await page.goto('http://127.0.0.1:5173/orders?orderStatus=UNPAID');
    await page.locator('tr[data-row-key="'+unpaid.id+'"]').getByRole('button',{name:'收款',exact:true}).click();
    const dialog = page.getByRole('dialog');
    await dialog.getByText('在线支付尚未开通',{exact:true}).waitFor();
    await settledDialog(page);
    assert.equal(await dialog.getByRole('button',{name:/生成收银台/}).count(),0);
    assert.equal(await dialog.locator('canvas').count(),0);
    await dialog.screenshot({path:path.join(output,'checkout-unconfigured.png'),animations:'disabled'});
    pass('真实页面','未支付订单有收款入口，缺配置时不展示假收银台或二维码');
    await dialog.getByRole('button',{name:/关\s*闭/}).last().click();
    await page.goto('http://127.0.0.1:5173/orders?orderStatus=PARTIAL_PAID');
    await page.locator('tr[data-row-key="'+partial.id+'"]').getByRole('button',{name:'收款',exact:true}).click();
    await dialog.getByText('在线支付尚未开通',{exact:true}).waitFor();
    await settledDialog(page);
    const balance = '¥'+Number(partialPreview.remainingAmount).toFixed(2);
    assert((await dialog.innerText()).includes(balance));
    await dialog.screenshot({path:path.join(output,'checkout-partial.png'),animations:'disabled'});
    await page.setViewportSize({width:390,height:844});
    await settledDialog(page);
    const mobileBounds = await dialog.boundingBox();
    assert(mobileBounds.x >= 0 && mobileBounds.x + mobileBounds.width <= 391);
    await page.screenshot({path:path.join(output,'checkout-mobile.png'),animations:'disabled'});
    pass('真实页面','部分支付弹窗显示应收、已付、余额，移动端可查看');
    await page.setViewportSize({width:1440,height:1000});
    await dialog.getByRole('button',{name:/关\s*闭/}).last().click();

    const payments = await api(session,'GET','/api/sales/payment-orders',null,{pageNum:1,pageSize:1000,keyword:paid.orderNo});
    const sample = payments.records.find(x=>x.orderNo===paid.orderNo&&x.paymentStatus==='PAY_SUCCESS');
    assert(sample);
    const route = '/student-api/public/orders/'+paid.id+'/checkout-status?paymentOrderNo='+sample.paymentOrderNo;
    const status = await (await fetch(base+route)).json();
    assert.equal(status.code,200);
    assert.equal(status.data.paymentStatus,'PAY_SUCCESS');
    for(const key of ['token','accessToken','signSecret','studentName','phone']) assert(!Object.hasOwn(status.data,key));
    assert(!status.data.cashierUrl);
    const legacy = await (await fetch(base+'/student-api/public/orders/'+paid.id+'/payment-return-status?paymentOrderNo='+sample.paymentOrderNo)).json();
    for(const key of ['token','accessToken','signSecret','studentName','phone']) assert(!Object.hasOwn(legacy.data,key));
    pass('真实接口·既有演示样本','新旧公开付款状态接口均不返回登录凭证和个人资料');
    const invalid = await (await fetch(base+'/student-api/public/orders/'+partial.id+'/checkout-status?paymentOrderNo='+sample.paymentOrderNo)).json();
    assert.equal(invalid.code,404);
    pass('真实接口','公开查询拒绝与报名订单不匹配的支付单号');

    const context = await browser.newContext();
    const publicPage = await context.newPage();
    publicPage.on('pageerror',e=>pageErrors.push(e.message));
    await publicPage.goto('http://127.0.0.1:5173/payment/return?orderId='+paid.id+'&paymentOrderNo='+sample.paymentOrderNo);
    await publicPage.getByText('付款已到账',{exact:true}).waitFor();
    assert(publicPage.url().includes('/payment/return'));
    assert.equal(await publicPage.evaluate(()=>localStorage.length),0);
    await publicPage.screenshot({path:path.join(output,'payment-return-demo-sample.png'),fullPage:true});
    pass('真实页面·既有演示样本','付款返回页可匿名查询既有演示流水，不跳转管理员登录');
    await publicPage.goto('http://127.0.0.1:5173/payment/return');
    await publicPage.getByText('付款返回链接不完整，请联系工作人员核实',{exact:true}).waitFor();
    pass('真实页面','缺少返回参数时显示可理解的错误');
    await context.close();

    // 仅拦截收银台接口验证前端展示，不向易宝下单，不修改本地支付和报名订单。
    let uiState = {...partialPreview, ready:true, sandbox:true, missingConfig:[], paymentOrderNo:undefined, paymentStatus:undefined};
    await page.route('**/api/payments/orders/*/checkout',async route=>{
      if(route.request().method()==='POST') uiState = {...uiState,paymentOrderNo:'ZF_UI_CONTRACT_ONLY',paymentStatus:'UNIFIED_ORDER_CREATED',
        paymentAmount:uiState.remainingAmount,cashierUrl:'https://cashier.example.test/contract-only',expiresAt:new Date(Date.now()+300000).toISOString()};
      await route.fulfill({json:{code:200,data:uiState,message:'测试替身'}});
    });
    await page.locator('tr[data-row-key="'+partial.id+'"]').getByRole('button',{name:'收款',exact:true}).click();
    await dialog.getByRole('button',{name:/生成收银台/}).click();
    const link = dialog.getByRole('link',{name:'打开收银台',exact:true});
    await link.waitFor();
    assert.equal(await link.getAttribute('href'),'https://cashier.example.test/contract-only');
    assert.equal(await dialog.locator('canvas, svg.ant-qrcode-svg').count(),1);
    pass('前端契约·拦截响应','生成收银台后显示二维码、可复制链接及独立窗口链接');
    uiState = {...uiState,paymentStatus:'PAY_SUCCESS',orderStatus:'PAID',paidAmount:partial.receivableAmount,remainingAmount:0};
    await dialog.getByRole('button',{name:'刷新支付结果',exact:true}).click();
    await dialog.getByRole('button',{name:/完\s*成/}).waitFor();
    assert.equal(await dialog.getByRole('link',{name:'打开收银台',exact:true}).count(),0);
    pass('前端契约·拦截响应','成功状态会刷新订单并停止显示继续付款链接');
    assert.deepEqual(pageErrors,[]);
    const actual = await api(session,'GET','/api/sales/orders/'+partial.id);
    assert.equal(actual.orderStatus,'PARTIAL_PAID');
    assert.equal(Number(actual.paidAmount),Number(partial.paidAmount));
    pass('真实接口','界面契约测试没有改变真实订单和付款金额');
  } catch(error) {
    pageErrors.push(error.message);
    console.error(error.stack);
    await page.screenshot({path:path.join(output,'checkout-failure.png'),fullPage:true});
    process.exitCode=1;
  } finally {
    fs.writeFileSync(path.join(output,'payment-verification.json'),JSON.stringify({results,pageErrors,realChannelCharged:false},null,2));
    await browser.close();
  }
})().catch(error=>{console.error(error);process.exitCode=1});

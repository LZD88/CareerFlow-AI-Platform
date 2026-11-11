const crypto = require('node:crypto');
const base = 'http://127.0.0.1:8081/yq-admin';
function stable(value) {
  if (Array.isArray(value)) return '[' + value.map(stable).join(',') + ']';
  if (value && typeof value === 'object') return '{' + Object.entries(value).sort(([a], [b]) => a.localeCompare(b)).map(([key, val]) => JSON.stringify(key) + ':' + stable(val)).join(',') + '}';
  return JSON.stringify(value);
}
function headers(session, method, path, data, query = '') {
  if (!session) return { 'Content-Type': 'application/json' };
  const timestamp = String(Date.now()), nonce = crypto.randomUUID();
  const payload = method === 'GET' ? query : data ? stable(data) : '';
  const sign = crypto.createHmac('sha256', session.signSecret).update([timestamp, nonce, method, '/yq-admin' + path, payload].join('\n')).digest('hex');
  return { 'Content-Type': 'application/json', Authorization: 'Bearer ' + session.token, 'X-Timestamp': timestamp, 'X-Nonce': nonce, 'X-Sign': sign };
}
async function api(session, method, path, data, params, timeout = 20000) {
  const query = params ? Object.entries(params).filter(([, value]) => value !== undefined).map(([key, value]) => encodeURIComponent(key) + '=' + encodeURIComponent(value)).sort((a,b) => a.localeCompare(b)).join('&') : '';
  const response = await fetch(base + path + (query ? '?' + query : ''), { method, headers: headers(session, method, path, data, query), body: data ? JSON.stringify(data) : undefined, signal: AbortSignal.timeout(timeout) });
  const body = await response.json();
  if (body.code !== 200) throw new Error(method + ' ' + path + ' => ' + body.code + ' ' + body.message);
  return body.data;
}
module.exports = { api, headers, base };
if (require.main === module) {
  (async () => {
    const login = await api(null, 'POST', '/api/auth/login', { username: 'admin', password: 'password' });
    console.log('LOGIN OK: ' + login.username);
    const users = await api(login, 'GET', '/api/system/users', null, { pageNum: 1, pageSize: 10 });
    console.log('SIGNED GET OK: ' + users.total + ' users');
    console.log('PASSWORD EXCLUDED: ' + !Object.hasOwn(users.records[0], 'password'));
    const profile = await api(login, 'GET', '/api/auth/profile');
    console.log('PROFILE OK: ' + profile.username);
  })().catch(error => { console.error(error.message); process.exitCode = 1; });
}

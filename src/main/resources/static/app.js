let accessToken;
let correlationId = () => crypto.randomUUID();
const $ = id => document.getElementById(id);
const show = value => { $('result').textContent = typeof value === 'string' ? value : JSON.stringify(value, null, 2); };
$('businessKey').value = `PR-DEMO-${Date.now()}`;
async function api(url, options = {}) {
  const id = correlationId();
  const headers = {'X-Correlation-Id': id, ...(options.headers || {})};
  if (accessToken) headers.Authorization = `Bearer ${accessToken}`;
  const response = await fetch(url, {...options, headers});
  let body; try { body = await response.json(); } catch { body = null; }
  return {response, body, id};
}
$('login').addEventListener('submit', async event => {
  event.preventDefault(); accessToken = undefined;
  const {response, body} = await api('/api/v1/auth/login', {method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify({username:$('username').value,password:$('password').value})});
  if (!response.ok) {
    show({...body, httpStatus: response.status});
    return;
  }
  accessToken = body.accessToken; $('login').hidden = true; $('session').hidden = false;
  const me = await api('/api/me'); $('identity').textContent = `${me.body.username} · ${me.body.authorities.join(', ')} · token expires in ${body.expiresIn} seconds`;
  show({status:response.status, message:'Signed in. Token is kept only in memory.'});
});
$('create').addEventListener('click', async () => {
  const result = await api('/api/v1/purchase-requests', {method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({businessKey:$('businessKey').value,title:$('title').value})});
  if (result.body?.id) $('requestId').value = result.body.id;
  show({...result.body, httpStatus:result.response.status});
});
$('approve').addEventListener('click', async () => {
  const result = await api(`/api/v1/purchase-requests/${encodeURIComponent($('requestId').value)}/approve`, {method:'POST'});
  show({...result.body, httpStatus:result.response.status, note:result.response.status===403?'Request status was not changed.':''});
});
$('logout').addEventListener('click', async () => {
  accessToken = undefined;
  $('session').hidden = true;
  $('login').hidden = false;
  const id = $('requestId').value;
  const path = id ? `/api/v1/purchase-requests/${encodeURIComponent(id)}` : '/api/v1/purchase-requests';
  const result = await api(path);
  show({...result.body, httpStatus:result.response.status, message:result.body?.message || 'Logged out.'});
});

'use strict';
// LEGEND FPS relay with persistent account authentication and friend/world-invite presence.
// Configure DATABASE_URL with a PostgreSQL connection string before enabling account login.
const http = require('http');
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const { promisify } = require('util');
const WebSocket = require('ws');
const { Pool } = require('pg');

const scrypt = promisify(crypto.scrypt);
const PORT = Number(process.env.PORT || 8080);
const TOKEN = process.env.LEGEND_RELAY_TOKEN || '';
const DATA_FILE = process.env.DATA_FILE || path.join(__dirname, 'relay-data.json');
const DATABASE_URL = process.env.DATABASE_URL || '';
const MAX_FRIENDS = Number(process.env.MAX_FRIENDS || 50);
const USER_RE = /^[A-Za-z0-9_]{3,16}$/;
const MC_NAME_RE = /^[A-Za-z0-9_]{1,16}$/;
const ADDR_RE = /^[a-z0-9-]+(?:\.[a-z0-9-]+)*\.e4mc\.link(?::[0-9]{1,5})?$/i;
const SESSION_DAYS = 30;
const pool = DATABASE_URL ? new Pool({ connectionString: DATABASE_URL, ssl: process.env.PGSSLMODE === 'disable' ? false : { rejectUnauthorized: false } }) : null;
let dbReady = false;
let users = {};
const online = new Map();
const sessions = new Map(); // token hash -> { key, expires }
const invites = new Map();
const claims = new Map();

const hash = (s) => crypto.createHash('sha256').update(s).digest('hex');
const safeEqual = (a, b) => {
  const aa = Buffer.from(String(a)); const bb = Buffer.from(String(b));
  return aa.length === bb.length && crypto.timingSafeEqual(aa, bb);
};
const send = (ws, obj) => { if (ws && ws.readyState === WebSocket.OPEN) ws.send(JSON.stringify(obj)); };
const without = (arr, k) => arr.filter((x) => x !== k);
const disp = (k) => (users[k] ? users[k].name : k);
const validAddress = (value) => {
  if (!ADDR_RE.test(value)) return false;
  const port = value.match(/:(\d+)$/);
  return !port || (Number(port[1]) >= 1 && Number(port[1]) <= 65535);
};
function json(res, status, data) {
  res.writeHead(status, { 'content-type': 'application/json; charset=utf-8', 'cache-control': 'no-store', 'access-control-allow-origin': '*' });
  res.end(JSON.stringify(data));
}
function readBody(req) {
  return new Promise((resolve, reject) => {
    let raw = '';
    req.on('data', chunk => { raw += chunk; if (raw.length > 8192) { reject(new Error('Request too large')); req.destroy(); } });
    req.on('end', () => { try { resolve(JSON.parse(raw || '{}')); } catch { reject(new Error('Invalid JSON')); } });
    req.on('error', reject);
  });
}
async function createSession(key) {
  const token = crypto.randomBytes(32).toString('base64url');
  const tokenHash = hash(token);
  const expires = new Date(Date.now() + SESSION_DAYS * 86400000);
  await pool.query('INSERT INTO legend_sessions(token_hash, username_key, expires_at) VALUES($1,$2,$3)', [tokenHash, key, expires]);
  sessions.set(tokenHash, { key, expires: expires.getTime() });
  return { token, expiresAt: expires.toISOString() };
}
async function resolveSession(token) {
  if (!dbReady || typeof token !== 'string' || token.length < 30 || token.length > 200) return null;
  const tokenHash = hash(token);
  const cached = sessions.get(tokenHash);
  if (cached && cached.expires > Date.now()) return cached.key;
  const result = await pool.query('SELECT username_key FROM legend_sessions WHERE token_hash=$1 AND expires_at > NOW()', [tokenHash]);
  if (!result.rowCount) return null;
  sessions.set(tokenHash, { key: result.rows[0].username_key, expires: Date.now() + 60000 });
  return result.rows[0].username_key;
}
async function initDatabase() {
  if (!pool) return;
  await pool.query(`
    CREATE TABLE IF NOT EXISTS legend_accounts (
      username_key TEXT PRIMARY KEY,
      username TEXT NOT NULL,
      password_salt TEXT NOT NULL,
      password_hash TEXT NOT NULL,
      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );
    CREATE TABLE IF NOT EXISTS legend_sessions (
      token_hash TEXT PRIMARY KEY,
      username_key TEXT NOT NULL REFERENCES legend_accounts(username_key) ON DELETE CASCADE,
      expires_at TIMESTAMPTZ NOT NULL
    );
    CREATE INDEX IF NOT EXISTS legend_sessions_expiry_idx ON legend_sessions(expires_at);
    CREATE TABLE IF NOT EXISTS legend_social_users (
      username_key TEXT PRIMARY KEY REFERENCES legend_accounts(username_key) ON DELETE CASCADE,
      friends JSONB NOT NULL DEFAULT '[]'::jsonb,
      incoming JSONB NOT NULL DEFAULT '[]'::jsonb,
      outgoing JSONB NOT NULL DEFAULT '[]'::jsonb
    );
  `);
  const rows = await pool.query('SELECT a.username_key, a.username, s.friends, s.incoming, s.outgoing FROM legend_accounts a LEFT JOIN legend_social_users s USING(username_key)');
  for (const row of rows.rows) users[row.username_key] = {
    name: row.username,
    friends: row.friends || [],
    incoming: row.incoming || [],
    outgoing: row.outgoing || []
  };
  dbReady = true;
  console.log('LEGEND FPS PostgreSQL ready; loaded ' + rows.rowCount + ' accounts');
}
let saveTimer;
function save() {
  if (saveTimer) return;
  saveTimer = setTimeout(async () => {
    saveTimer = null;
    if (dbReady) {
      try {
        for (const [key, u] of Object.entries(users)) {
          await pool.query(`INSERT INTO legend_social_users(username_key,friends,incoming,outgoing)
            VALUES($1,$2::jsonb,$3::jsonb,$4::jsonb)
            ON CONFLICT(username_key) DO UPDATE SET friends=EXCLUDED.friends,incoming=EXCLUDED.incoming,outgoing=EXCLUDED.outgoing`,
            [key, JSON.stringify(u.friends), JSON.stringify(u.incoming), JSON.stringify(u.outgoing)]);
        }
      } catch (e) { console.error('database save failed:', e.message); }
    } else {
      try { fs.writeFileSync(DATA_FILE, JSON.stringify(users)); } catch (e) { console.error('file save failed:', e.message); }
    }
  }, 250);
}
function stateFor(k) {
  const u = users[k];
  return { type: 'state', friends: u.friends.map(f => ({ name: disp(f), online: online.has(f) })), incoming: u.incoming.map(disp), outgoing: u.outgoing.map(disp) };
}
const push = (k) => { if (online.has(k) && users[k]) send(online.get(k), stateFor(k)); };
const pushWithFriends = (k) => { push(k); (users[k]?.friends || []).forEach(push); };
function pushPublicPlayers() {
  const packet = { type: 'players', players: [...online.keys()].map(disp).sort((a,b) => a.localeCompare(b)) };
  for (const ws of online.values()) send(ws, packet);
}
function restoreFriendships(k) {
  for (const c of claims.get(k) || []) {
    if (c === k || !users[c] || !users[k]) continue;
    if (claims.get(c)?.has(k) && !users[k].friends.includes(c)) {
      users[k].friends.push(c); if (!users[c].friends.includes(k)) users[c].friends.push(k);
      users[k].incoming = without(users[k].incoming, c); users[k].outgoing = without(users[k].outgoing, c);
      users[c].incoming = without(users[c].incoming, k); users[c].outgoing = without(users[c].outgoing, k);
      save(); push(c);
    }
  }
}
async function registerAccount(body) {
  if (!dbReady) throw Object.assign(new Error('Account database is not configured yet'), { status: 503 });
  const username = String(body.username || '').trim();
  const password = String(body.password || '');
  if (!USER_RE.test(username)) throw Object.assign(new Error('Username must be 3–16 letters, numbers or underscores'), { status: 400 });
  if (password.length < 6 || password.length > 128) throw Object.assign(new Error('Password must be 6–128 characters'), { status: 400 });
  const key = username.toLowerCase();
  const salt = crypto.randomBytes(16).toString('hex');
  const derived = await scrypt(password, salt, 64);
  try {
    await pool.query('INSERT INTO legend_accounts(username_key,username,password_salt,password_hash) VALUES($1,$2,$3,$4)', [key, username, salt, derived.toString('hex')]);
  } catch (e) {
    if (e.code === '23505') throw Object.assign(new Error('That username is already registered'), { status: 409 });
    throw e;
  }
  users[key] = { name: username, friends: [], incoming: [], outgoing: [] };
  await pool.query('INSERT INTO legend_social_users(username_key) VALUES($1)', [key]);
  const session = await createSession(key);
  return { username, ...session };
}
async function loginAccount(body) {
  if (!dbReady) throw Object.assign(new Error('Account database is not configured yet'), { status: 503 });
  const username = String(body.username || '').trim();
  const password = String(body.password || '');
  if (!USER_RE.test(username) || password.length < 1 || password.length > 128) throw Object.assign(new Error('Invalid username or password'), { status: 401 });
  const result = await pool.query('SELECT username_key,username,password_salt,password_hash FROM legend_accounts WHERE username_key=$1', [username.toLowerCase()]);
  if (!result.rowCount) throw Object.assign(new Error('Invalid username or password'), { status: 401 });
  const account = result.rows[0];
  const derived = await scrypt(password, account.password_salt, 64);
  if (!safeEqual(derived.toString('hex'), account.password_hash)) throw Object.assign(new Error('Invalid username or password'), { status: 401 });
  return { username: account.username, ...(await createSession(account.username_key)) };
}
const server = http.createServer(async (req, res) => {
  if (req.method === 'OPTIONS') { res.writeHead(204, { 'access-control-allow-origin': '*', 'access-control-allow-methods': 'GET,POST,OPTIONS', 'access-control-allow-headers': 'content-type,authorization' }); return res.end(); }
  if (req.url === '/health' || req.url === '/') return json(res, 200, { service: 'LEGEND FPS relay', status: 'online', accounts: dbReady ? 'ready' : 'database-not-configured' });
  try {
    if (req.method === 'POST' && (req.url === '/api/register' || req.url === '/api/login')) {
      const body = await readBody(req);
      const result = req.url.endsWith('/register') ? await registerAccount(body) : await loginAccount(body);
      return json(res, 200, { ok: true, ...result });
    }
    if (req.method === 'POST' && req.url === '/api/logout') {
      const token = String(req.headers.authorization || '').replace(/^Bearer\s+/i, '');
      if (token && dbReady) {
        const tokenHash = hash(token);
        await pool.query('DELETE FROM legend_sessions WHERE token_hash=$1', [tokenHash]);
        sessions.delete(tokenHash);
      }
      return json(res, 200, { ok: true });
    }
    if (req.method === 'GET' && req.url === '/api/me') {
      const token = String(req.headers.authorization || '').replace(/^Bearer\s+/i, '');
      const key = await resolveSession(token);
      if (!key || !users[key]) return json(res, 401, { error: 'Session expired; please sign in again' });
      return json(res, 200, { username: users[key].name });
    }
    return json(res, 404, { error: 'Not found' });
  } catch (e) {
    console.error('HTTP request failed:', e.message);
    return json(res, e.status || 500, { error: e.message || 'Server error' });
  }
});
const wss = new WebSocket.Server({ server, path: '/ws', maxPayload: 4096 });
wss.on('connection', (ws) => {
  ws.key = '';
  ws.msgCount = 0;
  const rate = setInterval(() => { ws.msgCount = 0; }, 5000);
  ws.on('message', async (raw) => {
    if (++ws.msgCount > 40) return ws.close(1008, 'rate limit');
    let p;
    try { p = JSON.parse(raw.toString()); } catch { return send(ws, { type: 'error', message: 'Invalid JSON' }); }
    if (p.type === 'ping') return send(ws, { type: 'pong' });
    if (p.type === 'authenticate') {
      try {
        const key = await resolveSession(String(p.sessionToken || ''));
        if (!key || !users[key]) { send(ws, { type: 'error', message: 'Please sign in to LEGEND FPS' }); return ws.close(1008, 'authentication required'); }
        const old = online.get(key);
        if (old && old !== ws) { old.key = ''; old.close(1000, 'signed in elsewhere'); }
        ws.key = key; online.set(key, ws);
        if (Array.isArray(p.friends)) claims.set(key, new Set(p.friends.slice(0,200).map(String).filter(n => MC_NAME_RE.test(n)).map(n => n.toLowerCase())));
        send(ws, { type: 'registered', name: users[key].name });
        pushWithFriends(key); pushPublicPlayers();
      } catch (e) { send(ws, { type: 'error', message: 'Authentication service unavailable' }); ws.close(1011, 'database error'); }
      return;
    }
    // Compatibility for the previous build; new builds must use account sessions.
    if (p.type === 'register') {
      if (TOKEN && p.token !== TOKEN) return ws.close(1008, 'invalid relay token');
      const name = String(p.name || '').trim(), secret = String(p.secret || '');
      if (!MC_NAME_RE.test(name) || !/^[A-Za-z0-9-]{16,64}$/.test(secret)) return ws.close(1008, 'invalid identity');
      const k = name.toLowerCase();
      if (!users[k]) users[k] = { name, friends: [], incoming: [], outgoing: [] };
      const old = online.get(k); if (old && old !== ws) { old.key = ''; old.close(1000, 'replaced'); }
      ws.key = k; online.set(k, ws); save(); send(ws, { type: 'registered', name }); pushWithFriends(k); pushPublicPlayers(); return;
    }
    const k = ws.key;
    if (!k || !users[k]) return send(ws, { type: 'error', message: 'Sign in first' });
    const me = users[k];
    const target = (field) => {
      const t = String(p[field] || '').trim();
      if (!MC_NAME_RE.test(t)) { send(ws, { type: 'error', message: 'Invalid player name' }); return null; }
      return t.toLowerCase();
    };
    switch (p.type) {
      case 'list_players': return send(ws, { type: 'players', players: [...online.keys()].map(disp).sort((a,b) => a.localeCompare(b)) });
      case 'friend_request': {
        const t = target('to'); if (!t) return;
        if (t === k) return send(ws, { type: 'error', message: 'You cannot add yourself' });
        if (!users[t]) return send(ws, { type: 'error', message: disp(t) + ' has not used LEGEND FPS on this relay yet' });
        if (me.friends.includes(t)) return send(ws, { type: 'error', message: 'Already friends' });
        if (me.friends.length >= MAX_FRIENDS) return send(ws, { type: 'error', message: 'Friend list is full' });
        if (me.incoming.includes(t)) { me.incoming = without(me.incoming,t); users[t].outgoing = without(users[t].outgoing,k); me.friends.push(t); users[t].friends.push(k); save(); push(k); push(t); return send(ws,{type:'info',message:'You are now friends with '+disp(t)}); }
        if (!me.outgoing.includes(t)) { me.outgoing.push(t); users[t].incoming.push(k); }
        save(); push(k); push(t); return send(ws,{type:'info',message:'Friend request sent to '+disp(t)});
      }
      case 'friend_accept': {
        const f = target('from'); if (!f) return;
        if (!me.incoming.includes(f)) return send(ws,{type:'error',message:'No request from that player'});
        me.incoming=without(me.incoming,f); users[f].outgoing=without(users[f].outgoing,k);
        if(!me.friends.includes(f))me.friends.push(f); if(!users[f].friends.includes(k))users[f].friends.push(k);
        save();push(k);push(f);return send(ws,{type:'info',message:'You are now friends with '+disp(f)});
      }
      case 'friend_decline': { const f=target('from');if(!f)return;me.incoming=without(me.incoming,f);if(users[f])users[f].outgoing=without(users[f].outgoing,k);save();push(k);push(f);return; }
      case 'friend_cancel': { const t=target('to');if(!t)return;me.outgoing=without(me.outgoing,t);if(users[t])users[t].incoming=without(users[t].incoming,k);save();push(k);push(t);return; }
      case 'friend_remove': { const t=target('name');if(!t)return;me.friends=without(me.friends,t);if(users[t])users[t].friends=without(users[t].friends,k);save();push(k);push(t);return; }
      case 'invite': {
        const t=target('to');if(!t)return;const address=String(p.address||'').trim();
        if(!validAddress(address)||address.length>255)return send(ws,{type:'error',message:'Invalid e4mc address'});
        if(!me.friends.includes(t))return send(ws,{type:'error',message:disp(t)+' is not your friend'});
        const tw=online.get(t);if(!tw)return send(ws,{type:'error',message:disp(t)+' is offline'});
        const id=crypto.randomUUID();invites.set(id,{from:k,to:t,at:Date.now()});send(tw,{type:'invite',id,from:me.name,address});return send(ws,{type:'invite_sent',to:disp(t),id});
      }
      case 'invite_response': { const id=String(p.id||''),inv=invites.get(id);if(!inv||inv.to!==k)return;invites.delete(id);return send(online.get(inv.from),{type:'invite_result',from:me.name,accepted:!!p.accepted}); }
      default: return send(ws,{type:'error',message:'Unknown packet'});
    }
  });
  ws.on('close', () => { clearInterval(rate); if(ws.key&&online.get(ws.key)===ws){const k=ws.key;online.delete(k);pushWithFriends(k);pushPublicPlayers();} });
  ws.on('error', () => {});
});
setInterval(() => { const cut=Date.now()-10*60*1000;for(const[id,v]of invites)if(v.at<cut)invites.delete(id); },60000);
if (pool) initDatabase().catch(e => { console.error('PostgreSQL initialization failed:', e.message); process.exitCode = 1; });
else { try { users = JSON.parse(fs.readFileSync(DATA_FILE,'utf8')); } catch { users = {}; } }
server.listen(PORT,'0.0.0.0',()=>console.log('LEGEND FPS relay listening on '+PORT));

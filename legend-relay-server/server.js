'use strict';
// LEGEND FPS relay v2.1 - friends, friend requests, presence and world invites over WebSocket.
// Identity model: Minecraft name + per-install secret (trust-on-first-use). The first install to
// register a name owns it on this relay. This is NOT Mojang-verified identity; see README_AR.md.
const http = require('http');
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const WebSocket = require('ws');

const PORT = Number(process.env.PORT || 8080);
const TOKEN = process.env.LEGEND_RELAY_TOKEN || '';
const DATA_FILE = process.env.DATA_FILE || path.join(__dirname, 'relay-data.json');
const MAX_FRIENDS = Number(process.env.MAX_FRIENDS || 50);
const NAME_RE = /^[A-Za-z0-9_]{1,16}$/;
const SECRET_RE = /^[A-Za-z0-9-]{16,64}$/;
// Only real e4mc hostnames are accepted as invite addresses.
const ADDR_RE = /^[a-z0-9-]+(?:\.[a-z0-9-]+)*\.e4mc\.link(?::[0-9]{1,5})?$/i;

// users: key(lowercase name) -> { name, secretHash, friends:[keys], incoming:[keys], outgoing:[keys] }
let users = {};
try { users = JSON.parse(fs.readFileSync(DATA_FILE, 'utf8')); } catch { users = {}; }
let saveTimer = null;
function save() {
  if (saveTimer) return;
  saveTimer = setTimeout(() => {
    saveTimer = null;
    try { fs.writeFileSync(DATA_FILE, JSON.stringify(users)); } catch (e) { console.error('save failed', e.message); }
  }, 500);
}
const without = (arr, k) => arr.filter((x) => x !== k);
const hash = (s) => crypto.createHash('sha256').update(s).digest('hex');
const online = new Map(); // key -> ws
const invites = new Map(); // id -> { from, to, at }
// Friend claims sent by clients at sign-in. Free hosts wipe the disk when they sleep; a friendship is
// restored only when BOTH players claim each other, so nobody can force a friendship on someone else.
const claims = new Map(); // key -> Set of keys
function restoreFriendships(k) {
  for (const c of claims.get(k) || []) {
    if (c === k || !users[c] || !users[k]) continue;
    if (claims.get(c)?.has(k) && !users[k].friends.includes(c)) {
      users[k].friends.push(c);
      if (!users[c].friends.includes(k)) users[c].friends.push(k);
      users[k].incoming = without(users[k].incoming, c); users[k].outgoing = without(users[k].outgoing, c);
      users[c].incoming = without(users[c].incoming, k); users[c].outgoing = without(users[c].outgoing, k);
      save(); push(c);
    }
  }
}

const send = (ws, obj) => { if (ws && ws.readyState === WebSocket.OPEN) ws.send(JSON.stringify(obj)); };
const disp = (k) => (users[k] ? users[k].name : k);

function stateFor(k) {
  const u = users[k];
  return {
    type: 'state',
    friends: u.friends.map((f) => ({ name: disp(f), online: online.has(f) })),
    incoming: u.incoming.map(disp),
    outgoing: u.outgoing.map(disp),
  };
}
const push = (k) => { if (online.has(k) && users[k]) send(online.get(k), stateFor(k)); };
const pushWithFriends = (k) => { push(k); (users[k]?.friends || []).forEach(push); };

const server = http.createServer((req, res) => {
  res.writeHead(200, { 'content-type': 'text/plain' });
  res.end('LEGEND FPS relay v2.1 online');
});
const wss = new WebSocket.Server({ server, path: '/ws', maxPayload: 4096 });

wss.on('connection', (ws) => {
  ws.key = '';
  ws.msgCount = 0;
  const rate = setInterval(() => { ws.msgCount = 0; }, 5000);
  ws.on('message', (raw) => {
    if (++ws.msgCount > 40) return ws.close(1008, 'rate limit');
    let p;
    try { p = JSON.parse(raw.toString()); } catch { return send(ws, { type: 'error', message: 'Invalid JSON' }); }
    if (p.type === 'ping') return send(ws, { type: 'pong' });

    if (p.type === 'register') {
      if (TOKEN && p.token !== TOKEN) return ws.close(1008, 'invalid relay token');
      const name = String(p.name || '').trim();
      const secret = String(p.secret || '');
      if (!NAME_RE.test(name) || !SECRET_RE.test(secret)) return ws.close(1008, 'invalid identity');
      const k = name.toLowerCase();
      if (users[k]) {
        if (users[k].secretHash !== hash(secret)) {
          send(ws, { type: 'error', message: 'This name is already claimed by another install' });
          return ws.close(1008, 'name claimed');
        }
        users[k].name = name;
      } else {
        users[k] = { name, secretHash: hash(secret), friends: [], incoming: [], outgoing: [] };
      }
      const old = online.get(k);
      if (old && old !== ws) { send(old, { type: 'error', message: 'Signed in from another place' }); old.key = ''; old.close(1000, 'replaced'); }
      ws.key = k; online.set(k, ws); save();
      if (Array.isArray(p.friends)) {
        claims.set(k, new Set(p.friends.slice(0, 200).map((n) => String(n)).filter((n) => NAME_RE.test(n)).map((n) => n.toLowerCase())));
        restoreFriendships(k);
      }
      send(ws, { type: 'registered', name });
      return pushWithFriends(k);
    }
    const k = ws.key;
    if (!k) return send(ws, { type: 'error', message: 'Register first' });
    const me = users[k];
    const target = (field) => {
      const t = String(p[field] || '').trim();
      if (!NAME_RE.test(t)) { send(ws, { type: 'error', message: 'Invalid player name' }); return null; }
      return t.toLowerCase();
    };

    switch (p.type) {
      case 'friend_request': {
        const t = target('to'); if (!t) return;
        if (t === k) return send(ws, { type: 'error', message: 'You cannot add yourself' });
        if (!users[t]) return send(ws, { type: 'error', message: disp(t) + ' has not used LEGEND FPS on this relay yet' });
        if (me.friends.includes(t)) return send(ws, { type: 'error', message: 'Already friends' });
        if (me.friends.length >= MAX_FRIENDS) return send(ws, { type: 'error', message: 'Friend list is full' });
        if (me.incoming.includes(t)) { // they already asked us -> accept
          me.incoming = without(me.incoming, t); users[t].outgoing = without(users[t].outgoing, k);
          me.friends.push(t); users[t].friends.push(k);
          save(); push(k); push(t);
          return send(ws, { type: 'info', message: 'You are now friends with ' + disp(t) });
        }
        if (!me.outgoing.includes(t)) { me.outgoing.push(t); users[t].incoming.push(k); }
        save(); push(k); push(t);
        return send(ws, { type: 'info', message: 'Friend request sent to ' + disp(t) });
      }
      case 'friend_accept': {
        const f = target('from'); if (!f) return;
        if (!me.incoming.includes(f)) return send(ws, { type: 'error', message: 'No request from that player' });
        me.incoming = without(me.incoming, f); users[f].outgoing = without(users[f].outgoing, k);
        if (!me.friends.includes(f)) me.friends.push(f);
        if (!users[f].friends.includes(k)) users[f].friends.push(k);
        save(); push(k); push(f);
        return send(ws, { type: 'info', message: 'You are now friends with ' + disp(f) });
      }
      case 'friend_decline': {
        const f = target('from'); if (!f) return;
        me.incoming = without(me.incoming, f); if (users[f]) users[f].outgoing = without(users[f].outgoing, k);
        save(); push(k); push(f); return;
      }
      case 'friend_cancel': {
        const t = target('to'); if (!t) return;
        me.outgoing = without(me.outgoing, t); if (users[t]) users[t].incoming = without(users[t].incoming, k);
        save(); push(k); push(t); return;
      }
      case 'friend_remove': {
        const t = target('name'); if (!t) return;
        me.friends = without(me.friends, t); if (users[t]) users[t].friends = without(users[t].friends, k);
        save(); push(k); push(t); return;
      }
      case 'invite': {
        const t = target('to'); if (!t) return;
        const address = String(p.address || '').trim();
        if (!ADDR_RE.test(address) || address.length > 255) return send(ws, { type: 'error', message: 'Invalid e4mc address' });
        if (!me.friends.includes(t)) return send(ws, { type: 'error', message: disp(t) + ' is not your friend' });
        const tw = online.get(t);
        if (!tw) return send(ws, { type: 'error', message: disp(t) + ' is offline' });
        const id = crypto.randomUUID();
        invites.set(id, { from: k, to: t, at: Date.now() });
        send(tw, { type: 'invite', id, from: me.name, address });
        return send(ws, { type: 'invite_sent', to: disp(t), id });
      }
      case 'invite_response': {
        const inv = invites.get(String(p.id || ''));
        if (!inv || inv.to !== k) return;
        invites.delete(String(p.id));
        return send(online.get(inv.from), { type: 'invite_result', from: me.name, accepted: !!p.accepted });
      }
      default:
        return send(ws, { type: 'error', message: 'Unknown packet' });
    }
  });
  ws.on('close', () => {
    clearInterval(rate);
    if (ws.key && online.get(ws.key) === ws) { const k = ws.key; online.delete(k); pushWithFriends(k); }
  });
  ws.on('error', () => {});
});

setInterval(() => { const cut = Date.now() - 10 * 60 * 1000; for (const [id, v] of invites) if (v.at < cut) invites.delete(id); }, 60000);
server.listen(PORT, '0.0.0.0', () => console.log('LEGEND relay v2.1 listening on ' + PORT));

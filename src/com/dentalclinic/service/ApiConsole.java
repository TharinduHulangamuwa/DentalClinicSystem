package com.dentalclinic.service;

/**
 * A small browser console for exercising the REST API by hand.
 *
 * WHY THIS EXISTS
 * A browser address bar cannot send an Authorization header, so every route
 * except /api/health returns 401 when typed into the URL bar. This page signs
 * in, keeps the token in the browser, and attaches it to each request, which
 * makes the whole API explorable without PowerShell or a separate tool.
 *
 * SECURITY NOTE FOR THE REPORT
 * This is a development and demonstration aid, not a production feature. A
 * real clinic deployment would not ship it, because any page that helps a
 * visitor authenticate and browse patient records is an attack surface. It is
 * therefore controlled by ApiServer.CONSOLE_ENABLED, and the recommendation
 * is to set that flag to false before deployment.
 *
 * The page is a single self-contained string with no external stylesheet,
 * script or font, so it works with no internet connection.
 *
 * @author [Your Name]
 */
public final class ApiConsole {

    private ApiConsole() { }

    public static String html(int port) {
        return HTML.replace("__PORT__", String.valueOf(port));
    }

    private static final String HTML =
"<!DOCTYPE html>\n" +
"<html lang='en'>\n" +
"<head>\n" +
"<meta charset='utf-8'>\n" +
"<title>Sunrise Dental Clinic - API Console</title>\n" +
"<style>\n" +
"  * { box-sizing: border-box; }\n" +
"  body { margin:0; font-family:'Segoe UI',Arial,sans-serif; background:#f3f6fa; color:#1c232d; }\n" +
"  header { background:#132f4c; color:#fff; padding:18px 26px; }\n" +
"  header h1 { margin:0; font-size:19px; }\n" +
"  header p { margin:4px 0 0; font-size:12px; color:#96afc8; }\n" +
"  .wrap { display:flex; gap:18px; padding:18px 26px; align-items:flex-start; }\n" +
"  .panel { background:#fff; border:1px solid #d6dde7; border-radius:6px; padding:16px 18px; }\n" +
"  .left { width:320px; flex:none; }\n" +
"  .right { flex:1; min-width:0; }\n" +
"  h2 { font-size:13px; margin:0 0 10px; color:#132f4c; text-transform:uppercase;\n" +
"       letter-spacing:.4px; }\n" +
"  label { display:block; font-size:12px; margin:8px 0 3px; color:#707a88; }\n" +
"  input { width:100%; padding:7px 9px; border:1px solid #d6dde7; border-radius:4px;\n" +
"          font-size:13px; font-family:inherit; }\n" +
"  button { border:0; border-radius:4px; padding:8px 14px; font-size:13px; cursor:pointer;\n" +
"           font-family:inherit; }\n" +
"  .primary { background:#0082b4; color:#fff; font-weight:600; width:100%; margin-top:12px; }\n" +
"  .primary:hover { background:#006994; }\n" +
"  .ep { display:block; width:100%; text-align:left; background:#fff; color:#132f4c;\n" +
"        border:1px solid #d6dde7; margin-bottom:5px; padding:7px 10px; font-size:12px; }\n" +
"  .ep:hover { background:#eef4fa; border-color:#0082b4; }\n" +
"  .ep .m { display:inline-block; width:52px; font-weight:700; font-size:11px; }\n" +
"  .get{color:#157347;} .post{color:#0082b4;} .put{color:#b06a00;} .del{color:#b71c1c;}\n" +
"  .admin { border-left:3px solid #b06a00; }\n" +
"  pre { background:#0f1d2b; color:#d7e3ef; padding:14px; border-radius:5px;\n" +
"        font-family:Consolas,monospace; font-size:12.5px; line-height:1.5;\n" +
"        white-space:pre-wrap; word-break:break-word; max-height:60vh; overflow:auto;\n" +
"        margin:0; }\n" +
"  .status { display:inline-block; padding:2px 9px; border-radius:10px; font-size:12px;\n" +
"            font-weight:700; margin-left:8px; }\n" +
"  .ok { background:#d7f0e2; color:#157347; }\n" +
"  .bad{ background:#fadcdc; color:#b71c1c; }\n" +
"  .who { font-size:12px; color:#707a88; margin-top:10px; line-height:1.6; }\n" +
"  .warn { background:#fff4d6; border:1px solid #e0b955; color:#7a5c00; font-size:12px;\n" +
"          padding:9px 12px; border-radius:5px; margin:0 26px 4px; }\n" +
"  .bar { display:flex; align-items:center; margin-bottom:10px; }\n" +
"  .bar h2 { margin:0; }\n" +
"</style>\n" +
"</head>\n" +
"<body>\n" +
"<header>\n" +
"  <h1>Sunrise Dental Clinic &mdash; API Console</h1>\n" +
"  <p>REST web service on port __PORT__ &nbsp;|&nbsp; CIS6003 Advanced Programming</p>\n" +
"</header>\n" +
"<div class='warn'>Development aid. Every request below is a real HTTP call to the " +
"running Java web service. Disable this page before any production deployment.</div>\n" +
"<div class='wrap'>\n" +

"  <div class='panel left'>\n" +
"    <h2>1. Sign in</h2>\n" +
"    <label>Username</label><input id='u' value='admin'>\n" +
"    <label>Password</label><input id='p' type='password' value='admin123'>\n" +
"    <button class='primary' onclick='signIn()'>Sign in and get a token</button>\n" +
"    <div class='who' id='who'>Not signed in. Every route except /api/health " +
"needs a token.</div>\n" +
"    <button class='ep' style='margin-top:10px' onclick='signOut()'>" +
"<span class='m del'>POST</span>/api/logout</button>\n" +

"    <h2 style='margin-top:20px'>2. Try an endpoint</h2>\n" +
"    <button class='ep' onclick=\"call('GET','/api/health',0)\">" +
"<span class='m get'>GET</span>/api/health</button>\n" +
"    <button class='ep' onclick=\"call('GET','/api/dashboard',1)\">" +
"<span class='m get'>GET</span>/api/dashboard</button>\n" +
"    <button class='ep' onclick=\"call('GET','/api/appointments',1)\">" +
"<span class='m get'>GET</span>/api/appointments</button>\n" +
"    <button class='ep' onclick=\"call('GET','/api/appointments/APT1001',1)\">" +
"<span class='m get'>GET</span>/api/appointments/APT1001</button>\n" +
"    <button class='ep' onclick=\"call('GET','/api/appointments/next',1)\">" +
"<span class='m get'>GET</span>/api/appointments/next</button>\n" +
"    <button class='ep' onclick=\"call('GET','/api/treatments',1)\">" +
"<span class='m get'>GET</span>/api/treatments</button>\n" +
"    <button class='ep' onclick=\"call('GET','/api/reports/daily',1)\">" +
"<span class='m get'>GET</span>/api/reports/daily</button>\n" +
"    <button class='ep' onclick=\"call('GET','/api/sessions',1)\">" +
"<span class='m get'>GET</span>/api/sessions</button>\n" +
"    <button class='ep' onclick=\"call('POST','/api/reminders',1,'{}')\">" +
"<span class='m post'>POST</span>/api/reminders</button>\n" +
"    <button class='ep admin' onclick=\"call('GET','/api/users',1)\">" +
"<span class='m get'>GET</span>/api/users <em>(admin)</em></button>\n" +

"    <h2 style='margin-top:20px'>3. Prove the security</h2>\n" +
"    <button class='ep' onclick='noToken()'>Request with NO token &rarr; 401</button>\n" +
"    <button class='ep' onclick='badToken()'>Request with a FORGED token &rarr; 401</button>\n" +
"    <button class='ep' onclick='duplicate()'>Create a DOUBLE BOOKING &rarr; 409</button>\n" +
"    <button class='ep' onclick='invalid()'>Send INVALID data &rarr; 400</button>\n" +
"  </div>\n" +

"  <div class='panel right'>\n" +
"    <div class='bar'><h2 id='ttl'>Response</h2><span id='st'></span></div>\n" +
"    <pre id='out'>Sign in on the left, then choose an endpoint.\n\n" +
"Every button issues a genuine HTTP request to the Java web service and shows\n" +
"the raw JSON reply, exactly as the Swing client receives it.</pre>\n" +
"  </div>\n" +
"</div>\n" +

"<script>\n" +
"let token = null;\n" +
"\n" +
"function show(title, status, body) {\n" +
"  document.getElementById('ttl').textContent = title;\n" +
"  const s = document.getElementById('st');\n" +
"  s.textContent = status;\n" +
"  s.className = 'status ' + (status >= 200 && status < 300 ? 'ok' : 'bad');\n" +
"  try { body = JSON.stringify(JSON.parse(body), null, 2); } catch (e) { }\n" +
"  document.getElementById('out').textContent = body;\n" +
"}\n" +
"\n" +
"async function request(method, path, useToken, body, forced) {\n" +
"  const headers = {};\n" +
"  if (body) headers['Content-Type'] = 'application/json';\n" +
"  const t = forced !== undefined ? forced : (useToken ? token : null);\n" +
"  if (t) headers['Authorization'] = 'Bearer ' + t;\n" +
"  const r = await fetch(path, { method: method, headers: headers, body: body });\n" +
"  return { status: r.status, text: await r.text() };\n" +
"}\n" +
"\n" +
"async function call(method, path, useToken, body) {\n" +
"  if (useToken && !token) {\n" +
"    show(method + ' ' + path, 0, 'Sign in first - this route needs a token.');\n" +
"    return;\n" +
"  }\n" +
"  try {\n" +
"    const r = await request(method, path, useToken, body);\n" +
"    show(method + ' ' + path, r.status, r.text);\n" +
"  } catch (e) {\n" +
"    show(method + ' ' + path, 0, 'Could not reach the server. Is ApiServer running?');\n" +
"  }\n" +
"}\n" +
"\n" +
"async function signIn() {\n" +
"  const body = JSON.stringify({\n" +
"    username: document.getElementById('u').value,\n" +
"    password: document.getElementById('p').value,\n" +
"    rememberMe: false\n" +
"  });\n" +
"  try {\n" +
"    const r = await request('POST', '/api/login', false, body);\n" +
"    show('POST /api/login', r.status, r.text);\n" +
"    if (r.status === 200) {\n" +
"      const d = JSON.parse(r.text);\n" +
"      token = d.token;\n" +
"      document.getElementById('who').innerHTML =\n" +
"        'Signed in as <b>' + d.fullName + '</b> (' + d.role + ')<br>' +\n" +
"        'Token: <code>' + token.substring(0, 14) + '...</code><br>' +\n" +
"        'Sent as an Authorization: Bearer header on every request below.';\n" +
"    } else {\n" +
"      token = null;\n" +
"      document.getElementById('who').textContent = 'Sign-in failed.';\n" +
"    }\n" +
"  } catch (e) {\n" +
"    show('POST /api/login', 0, 'Could not reach the server. Is ApiServer running?');\n" +
"  }\n" +
"}\n" +
"\n" +
"async function signOut() {\n" +
"  if (!token) { show('POST /api/logout', 0, 'Not signed in.'); return; }\n" +
"  const r = await request('POST', '/api/logout', true, '{}');\n" +
"  show('POST /api/logout', r.status, r.text +\n" +
"       '\\n\\nThe token is now invalid server-side. Try an endpoint above: it " +
"will return 401 even though the browser still holds the same string.');\n" +
"  token = null;\n" +
"  document.getElementById('who').textContent = 'Signed out.';\n" +
"}\n" +
"\n" +
"async function noToken() {\n" +
"  const r = await request('GET', '/api/appointments', false, null, null);\n" +
"  show('GET /api/appointments  (no token)', r.status, r.text +\n" +
"       '\\n\\nThe server checks for the header BEFORE looking anything up.');\n" +
"}\n" +
"\n" +
"async function badToken() {\n" +
"  const r = await request('GET', '/api/appointments', true, null, 'NOTAREALTOKEN');\n" +
"  show('GET /api/appointments  (forged token)', r.status, r.text +\n" +
"       '\\n\\nA different message from the one above: this token got past the " +
"header check and failed validation against the sessions table. Two checks, " +
"two messages.');\n" +
"}\n" +
"\n" +
"async function duplicate() {\n" +
"  if (!token) { show('Double booking test', 0, 'Sign in first.'); return; }\n" +
"  const existing = await request('GET', '/api/appointments', true);\n" +
"  const list = JSON.parse(existing.text);\n" +
"  if (!list.length) { show('Double booking test', 0, 'No appointments to clash with.'); return; }\n" +
"  const a = list[0];\n" +
"  const clash = JSON.stringify({\n" +
"    appointmentNo: 'APT9999', patientName: 'Test Patient', address: 'Colombo',\n" +
"    contactNo: '0770000000', dentistName: a.dentistName,\n" +
"    treatmentType: a.treatmentType, appointmentDate: a.appointmentDate,\n" +
"    appointmentTime: a.appointmentTime\n" +
"  });\n" +
"  const r = await request('POST', '/api/appointments', true, clash);\n" +
"  show('POST /api/appointments  (deliberate clash)', r.status, r.text +\n" +
"       '\\n\\nBooking ' + a.dentistName + ' again at ' + a.appointmentDate +\n" +
"       ' ' + a.appointmentTime + ' is refused. This is the double-booking " +
"problem from the scenario, prevented by the server and by a UNIQUE constraint " +
"in the database.');\n" +
"}\n" +
"\n" +
"async function invalid() {\n" +
"  if (!token) { show('Validation test', 0, 'Sign in first.'); return; }\n" +
"  const bad = JSON.stringify({\n" +
"    appointmentNo: 'BADNUMBER', patientName: '12345', address: 'x',\n" +
"    contactNo: '123', dentistName: 'Dr. Silva', treatmentType: 'Filling',\n" +
"    appointmentDate: '2026-02-30', appointmentTime: '99:99'\n" +
"  });\n" +
"  const r = await request('POST', '/api/appointments', true, bad);\n" +
"  show('POST /api/appointments  (invalid data)', r.status, r.text +\n" +
"       '\\n\\nThe SERVER rejected this, not the Swing client. A server must " +
"never trust a client, because a request can be sent by hand exactly like this " +
"one was.');\n" +
"}\n" +
"</script>\n" +
"</body>\n" +
"</html>\n";
}

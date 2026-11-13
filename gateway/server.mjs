import http from 'node:http';
import { pathToFileURL } from 'node:url';
import { readFile } from 'node:fs/promises';

const uuid = /^[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$/;
const declarations = JSON.parse(await readFile(new URL('../app/src/main/assets/gemini-functions.v1.json', import.meta.url), 'utf8'));
const allowed = new Set(declarations.map(d => d.name));
class GatewayError extends Error { constructor(code, status = 502) { super(code); this.code = code; this.status = status; } }
export function validateRequest(body) {
  if (!body || !uuid.test(body.requestId) || !uuid.test(body.traceId) || typeof body.message !== 'string' || body.message.length < 1 || body.message.length > 8000) throw new GatewayError('INVALID_REQUEST',400);
  const ctx = body.context;
  if (!ctx || ctx.timeZone !== 'Asia/Ho_Chi_Minh' || ![ctx.referenceDate,ctx.selectedDate].every(d => typeof d === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(d))) throw new GatewayError('INVALID_CONTEXT',400);
  if (ctx.route !== undefined && !['TODAY','FINANCE','NOTES','SETTINGS'].includes(ctx.route)) throw new GatewayError('INVALID_CONTEXT',400);
  if (ctx.selectedPeriod !== undefined && !/^\d{4}-(0[1-9]|1[0-2])$/.test(ctx.selectedPeriod)) throw new GatewayError('INVALID_CONTEXT',400);
}
export async function interpret(body, { key, model, fetcher = fetch } = {}) {
  validateRequest(body);
  if (!key || !model || !/^[a-zA-Z0-9._-]+$/.test(model)) throw new GatewayError('CONFIGURATION',503);
  const system = await readFile(new URL('../data/gemini-system-prompt.vi.md', import.meta.url),'utf8');
  let response;
  try {
    response = await fetcher(`https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent`, {
      method: 'POST', redirect: 'error', signal: AbortSignal.timeout(25000), headers: { 'Content-Type':'application/json', 'x-goog-api-key':key },
      body: JSON.stringify({ systemInstruction: { parts:[{ text: system + '\nChỉ đề xuất một hàm trong danh sách. Không thực hiện hay báo đã lưu. Khi thiếu trường thì hỏi lại bằng tiếng Việt. Không tính số từ trí nhớ.' }] },
        contents:[{ role:'user', parts:[{ text: JSON.stringify({ message:body.message, context:body.context }) }] }],
        tools:[{ functionDeclarations:declarations }], generationConfig:{ temperature:0.1, maxOutputTokens:2048 } })
    });
  } catch (error) { throw new GatewayError(['TimeoutError','AbortError'].includes(error?.name) ? 'GATEWAY_TIMEOUT':'GATEWAY_UNAVAILABLE',504); }
  if (!response.ok) throw new GatewayError(response.status === 429 ? 'RATE_LIMITED' : 'UPSTREAM_REJECTED');
  const data = await response.json();
  const parts = data.candidates?.[0]?.content?.parts;
  if (!Array.isArray(parts)) throw new GatewayError('PROTOCOL');
  const calls = parts.filter(p => p.functionCall).map((p,i) => ({ callId:`call-${i+1}`, name:p.functionCall.name, args:p.functionCall.args }));
  if (calls.length > 1) throw new GatewayError('MULTI_STEP_UNSUPPORTED',422);
  if (calls.some(c => !allowed.has(c.name) || !c.args || Array.isArray(c.args))) throw new GatewayError('PROTOCOL');
  return { requestId:body.requestId, traceId:body.traceId, kind:calls.length ? 'PROPOSALS':'CLARIFY', calls, text:calls.length ? '' : parts.filter(p=>!p.thought).map(p=>p.text || '').join('').slice(0,2000) };
}
export function createGateway(config = {}) {
  let active = 0;
  return http.createServer(async (req,res) => {
    res.setHeader('Content-Type','application/json; charset=utf-8'); res.setHeader('Cache-Control','no-store');
    if (req.method === 'GET' && req.url === '/health') { res.end(JSON.stringify({ configured:Boolean(config.key && config.model) })); return; }
    if (req.method !== 'POST' || req.url !== '/v1/assistant/interpret') { res.writeHead(404); res.end('{"error":"NOT_FOUND"}'); return; }
    if (active >= 4) { res.writeHead(429); res.end('{"error":"RATE_LIMITED"}'); return; }
    active++;
    try {
      let bytes = 0; const chunks = [];
      for await (const chunk of req) { bytes += chunk.length; if (bytes > 32768) throw new GatewayError('REQUEST_TOO_LARGE',413); chunks.push(chunk); }
      let body; try { body = JSON.parse(Buffer.concat(chunks).toString('utf8')); } catch { throw new GatewayError('INVALID_JSON',400); }
      const result = await interpret(body,config); res.end(JSON.stringify(result));
    } catch (error) {
      // Never log HTTP bodies, key, model response, exception text, or raw chat.
      res.writeHead(error instanceof GatewayError ? error.status : 502);
      res.end(JSON.stringify({ error:error instanceof GatewayError ? error.code:'GATEWAY_UNAVAILABLE' }));
    } finally { active--; }
  });
}
if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  createGateway({ key:process.env.GEMINI_API_KEY, model:process.env.GEMINI_MODEL }).listen(8787,'127.0.0.1',() => process.stdout.write('Bep An gateway: http://127.0.0.1:8787 (loopback only)\n'));
}

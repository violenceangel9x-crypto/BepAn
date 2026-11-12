import test from 'node:test';
import assert from 'node:assert/strict';
import { randomUUID } from 'node:crypto';
import { interpret, validateRequest, createGateway } from './server.mjs';
const request = () => ({ requestId:randomUUID(),traceId:randomUUID(),message:'Ghi tiền mua rau 85 nghìn',context:{ timeZone:'Asia/Ho_Chi_Minh',referenceDate:'2026-11-12',selectedDate:'2026-11-30' } });
test('function calling returns proposals and keeps correlation',async () => {
  const body=request();
  const result=await interpret(body,{key:'synthetic-secret',model:'gemini-3.5-flash',fetcher:async (url,options) => {
    const parsed=JSON.parse(options.body); assert.equal(parsed.tools[0].functionDeclarations.length,5); assert.equal(parsed.contents[0].role,'user');
    return { ok:true,json:async () => ({ candidates:[{ content:{ parts:[{functionCall:{ name:'record_purchase',args:{date:'2026-11-12',amountVnd:85000,description:'Mua rau'} }}] } }] }) };
  }});
  assert.equal(result.traceId,body.traceId); assert.equal(result.calls[0].args.amountVnd,85000); assert.equal(result.kind,'PROPOSALS');
});
test('no key does not silently simulate',async () => { await assert.rejects(interpret(request(),{}),{message:'CONFIGURATION'}); });
test('unknown or compound commands rejected before app',async () => {
  await assert.rejects(interpret(request(),{key:'synthetic',model:'model',fetcher:async () => ({ok:true,json:async()=>({candidates:[{content:{parts:[{functionCall:{name:'delete_database',args:{}}}]}}]})})}),{message:'PROTOCOL'});
});
test('timeouts have safe codes',async () => {
  await assert.rejects(interpret(request(),{key:'synthetic',model:'model',fetcher:async()=>{throw Object.assign(new Error('SECRET_NOT_FOR_EXPORT'),{name:'TimeoutError'});}}),{message:'GATEWAY_TIMEOUT'});
});
test('request validation bounds input',() => { assert.throws(()=>validateRequest({...request(),message:'x'.repeat(8001)}),{message:'INVALID_REQUEST'}); });
test('HTTP errors redact provider and client secrets',async () => {
  const server=createGateway({key:'SYNTHETIC_API_SECRET',model:'model',fetcher:async()=>{throw new Error('PRIVATE_CHAT_AND_HTTP_HEADER');}});
  await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));
  try {
    const response=await fetch(`http://127.0.0.1:${server.address().port}/v1/assistant/interpret`,{method:'POST',body:JSON.stringify(request())});
    const text=await response.text(); assert.equal(response.status,504); assert.equal(text,'{"error":"GATEWAY_UNAVAILABLE"}');
  } finally { await new Promise(resolve=>server.close(resolve)); }
});

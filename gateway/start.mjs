import { createGateway } from './server.mjs';

async function promptKey() {
  if (!process.stdin.isTTY) throw new Error('CONFIGURATION');
  process.stdout.write('Gemini API key (hidden, kept in memory only): ');
  process.stdin.setRawMode(true);
  process.stdin.setEncoding('utf8');
  process.stdin.resume();
  try {
    return await new Promise((resolve, reject) => {
      let value = '';
      const onData = chunk => {
        for (const character of chunk) {
          if (character === '\u0003' || character === '\u0004') {
            process.stdin.off('data', onData);
            reject(new Error('CANCELLED'));
            return;
          }
          if (character === '\r' || character === '\n') {
            process.stdin.off('data', onData);
            resolve(value.trim());
            return;
          }
          if (character === '\u007f' || character === '\b') value = value.slice(0, -1);
          else if (character >= ' ' && character <= '~') value += character;
        }
      };
      process.stdin.on('data', onData);
    });
  } finally {
    process.stdin.setRawMode(false);
    process.stdin.pause();
    process.stdout.write('\n');
  }
}

try {
  const key = process.env.GEMINI_API_KEY?.trim() || await promptKey();
  const model = process.env.GEMINI_MODEL?.trim() || 'gemini-3.5-flash';
  if (!key || !/^[a-zA-Z0-9._-]+$/.test(model)) throw new Error('CONFIGURATION');
  const server = createGateway({ key, model });
  server.on('error', () => { process.stderr.write('GATEWAY_BIND_FAILED\n'); process.exitCode = 2; });
  server.listen(8787, '127.0.0.1', () => process.stdout.write(`Bep An gateway: http://127.0.0.1:8787, model ${model}\n`));
  process.once('SIGINT', () => server.close(() => process.exit(0)));
  process.once('SIGTERM', () => server.close(() => process.exit(0)));
} catch {
  process.stderr.write('Gateway needs a key in GEMINI_API_KEY or an interactive terminal.\n');
  process.exitCode = 2;
}

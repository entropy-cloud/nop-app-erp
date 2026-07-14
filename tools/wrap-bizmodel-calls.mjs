import { readFile, writeFile } from 'fs/promises';
import { execFile } from 'child_process';
import { promisify } from 'util';
import path from 'path';
import { fileURLToPath } from 'url';

const execFileAsync = promisify(execFile);
const __dirname = fileURLToPath(new URL('.', import.meta.url));
const rootDir = path.join(__dirname, '..');

async function findTestFiles() {
  const { stdout } = await execFileAsync('git', ['ls-files', '--', 
    'module-*/erp-*-service/src/test/java/**/*.java'], {
    cwd: rootDir,
    maxBuffer: 10 * 1024 * 1024,
  });
  return stdout.split(/\r?\n/).map(l => l.trim()).filter(Boolean);
}

function findMatchingParen(lines, startLineIdx, startCol) {
  let depth = 0;
  let inString = false;
  let inChar = false;
  let inLineComment = false;
  let inBlockComment = false;

  for (let i = startLineIdx; i < lines.length; i++) {
    const line = lines[i];
    const startJ = (i === startLineIdx) ? startCol : 0;

    for (let j = startJ; j < line.length; j++) {
      const ch = line[j];
      const nextCh = line[j + 1];

      if (inLineComment) { continue; }
      if (inBlockComment) {
        if (ch === '*' && nextCh === '/') { inBlockComment = false; j++; }
        continue;
      }
      if (inString) {
        if (ch === '\\') { j++; continue; }
        if (ch === '"') { inString = false; }
        continue;
      }
      if (inChar) {
        if (ch === '\\') { j++; continue; }
        if (ch === "'") { inChar = false; }
        continue;
      }

      if (ch === '/' && nextCh === '/') { inLineComment = true; continue; }
      if (ch === '/' && nextCh === '*') { inBlockComment = true; j++; continue; }
      if (ch === '"') { inString = true; continue; }
      if (ch === "'") { inChar = true; continue; }

      if (ch === '(') depth++;
      if (ch === ')') {
        depth--;
        if (depth === 0) {
          return { lineIdx: i, col: j };
        }
      }
    }
    inLineComment = false;
  }
  return null;
}

async function processFile(filePath) {
  const absolutePath = path.join(rootDir, filePath);
  let content = await readFile(absolutePath, 'utf8');

  if (!/IOrmTemplate\s+ormTemplate/.test(content) && !/extends\s+\w*(TestSupport|TestBase)/.test(content)) {
    return false;
  }

  if (filePath.includes('/report/') || filePath.includes('/dashboard/')) {
    return false;
  }

  const bizFieldMatches = [...content.matchAll(/IErp\w+Biz\s+(\w+)/g)];
  const callFieldMatches = [...content.matchAll(/(\w+Biz)\.\w+\s*\(/g)];
  const bizFields = new Set([
    ...bizFieldMatches.map(m => m[1]),
    ...callFieldMatches.map(m => m[1])
  ].filter(f => f !== 'IErp' && !f.startsWith('IErp')));
  if (bizFields.size === 0) return false;

  let lines = content.split('\n');
  const modifications = [];
  let wrappedCount = 0;

  for (let i = 0; i < lines.length; i++) {
    const line = lines[i];
    const trimmed = line.trim();

    if (trimmed.startsWith('//') || trimmed.startsWith('*') || trimmed.startsWith('/*')) continue;

    for (const field of bizFields) {
      const callPattern = new RegExp(`\\b${field}\\.(\\w+)\\(`);
      const match = line.match(callPattern);
      if (!match) continue;

      const methodStartCol = line.indexOf(`${field}.`);
      if (methodStartCol === -1) continue;

      if (line.substring(0, methodStartCol).includes('runInSession')) continue;

      const parenCol = line.indexOf('(', methodStartCol + field.length + 1);
      if (parenCol === -1) continue;

      const parenMatch = findMatchingParen(lines, i, parenCol);
      if (!parenMatch) continue;

      const beforeCall = line.substring(0, methodStartCol);

      let hasAssignment = /^\s*(?:\w[\w<>\[\],\s]*\s+)?\w+\s*=\s*$/.test(beforeCall) ||
                          /^\s*(?:\w[\w<>\[\],\s]*\s+)?\w+\s*=\s*$/.test(beforeCall);

      if (!hasAssignment && /^\s*$/.test(beforeCall) && i > 0) {
        const prevTrimmed = lines[i - 1].trim();
        if (prevTrimmed.endsWith('=') || /\w+\s*=\s*$/.test(prevTrimmed)) {
          hasAssignment = true;
        }
      }

      const closeLineText = lines[parenMatch.lineIdx];
      const stmtEnd = closeLineText.substring(parenMatch.col + 1);

      const isPureStandalone = /^\s*;\s*$/.test(stmtEnd) && !hasAssignment && /^\s*$/.test(beforeCall);

      const useFunction = !isPureStandalone;

      if (parenMatch.lineIdx === i) {
        const callText = line.substring(methodStartCol, parenMatch.col + 1);

        if (useFunction) {
          if (hasAssignment) {
            const eqIdx = beforeCall.lastIndexOf('=');
            const assignmentPart = beforeCall.substring(0, eqIdx + 1);
            lines[i] = `${assignmentPart} ormTemplate.runInSession(session -> ${callText})${stmtEnd}`;
          } else {
            lines[i] = `${beforeCall}ormTemplate.runInSession(session -> ${callText})${stmtEnd}`;
          }
        } else {
          lines[i] = `${beforeCall}ormTemplate.runInSession(() -> ${callText})${stmtEnd}`;
        }
        wrappedCount++;
      } else {
        if (useFunction) {
          if (hasAssignment) {
            const eqIdx = beforeCall.lastIndexOf('=');
            const assignmentPart = beforeCall.substring(0, eqIdx + 1);
            lines[i] = `${assignmentPart} ormTemplate.runInSession(session -> ${line.substring(methodStartCol)}`;
          } else {
            lines[i] = `${beforeCall}ormTemplate.runInSession(session -> ${line.substring(methodStartCol)}`;
          }
        } else {
          lines[i] = `${beforeCall}ormTemplate.runInSession(() -> ${line.substring(methodStartCol)}`;
        }

        const closeLine = lines[parenMatch.lineIdx];
        lines[parenMatch.lineIdx] = closeLine.substring(0, parenMatch.col + 1) + ')' + closeLine.substring(parenMatch.col + 1);

        wrappedCount++;
      }
      break;
    }
  }

  if (wrappedCount === 0) return false;

  await writeFile(absolutePath, lines.join('\n'), 'utf8');
  console.log(`  [wrapped] ${filePath} (${wrappedCount} calls)`);
  return true;
}

async function main() {
  const files = await findTestFiles();
  console.log(`Found ${files.length} test files in 9 target modules`);

  let modifiedCount = 0;
  for (const filePath of files) {
    try {
      const modified = await processFile(filePath);
      if (modified) modifiedCount++;
    } catch (e) {
      console.error(`  [error] ${filePath}: ${e.message}`);
    }
  }

  console.log(`\nDone: ${modifiedCount} files modified`);
}

main().catch(err => {
  console.error('Error:', err);
  process.exit(1);
});

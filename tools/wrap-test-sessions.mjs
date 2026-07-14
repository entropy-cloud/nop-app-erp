import { readFile, writeFile } from 'fs/promises';
import { execFile } from 'child_process';
import { promisify } from 'util';
import path from 'path';
import { fileURLToPath } from 'url';

const execFileAsync = promisify(execFile);
const __dirname = fileURLToPath(new URL('.', import.meta.url));
const rootDir = path.join(__dirname, '..');

const TARGET_MODULES = ['aps', 'assets', 'b2b', 'contract', 'finance', 'hr', 'inventory', 'logistics', 'projects'];

async function findTestFiles() {
  const globs = TARGET_MODULES.map(m => `module-${m}/erp-*-service/src/test/java/**/*.java`);
  const { stdout } = await execFileAsync('git', ['ls-files', '--', ...globs], {
    cwd: rootDir,
    maxBuffer: 10 * 1024 * 1024,
  });
  return stdout.split(/\r?\n/).map(l => l.trim()).filter(Boolean);
}

function findMatchingBrace(lines, startLineIdx, startCol) {
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
      
      if (ch === '{') depth++;
      if (ch === '}') {
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
  
  // Skip files that use IGraphQLEngine (already correct pattern)
  if (content.includes('IGraphQLEngine') || content.includes('graphQLEngine') || content.includes('executeRpc')) {
    return false;
  }
  
  // Must have direct BizModel calls
  if (!/\b\w+Biz\.\w+\s*\(/.test(content)) {
    return false;
  }
  
  // Must already inject IOrmTemplate
  if (!/IOrmTemplate\s+ormTemplate/.test(content)) {
    return false;
  }
  
  // Skip report/dashboard test files (they call BizModel findList/findPage for read, not mutations that need session)
  if (filePath.includes('/report/') || filePath.includes('/dashboard/')) {
    return false;
  }
  
  const lines = content.split('\n');
  const insertions = [];
  let wrappedCount = 0;
  
  for (let i = 0; i < lines.length; i++) {
    const trimmed = lines[i].trim();
    
    // Find @Test or @EnableSnapshot annotations
    if (trimmed !== '@Test' && !trimmed.startsWith('@Test(') && trimmed !== '@EnableSnapshot') {
      continue;
    }
    
    // Skip past annotations to find method declaration
    let methodLine = i + 1;
    while (methodLine < lines.length) {
      const t = lines[methodLine].trim();
      if (t === '' || t.startsWith('@')) {
        methodLine++;
        continue;
      }
      break;
    }
    
    if (methodLine >= lines.length) continue;
    
    // Find the opening brace of the method body
    let braceLine = methodLine;
    let braceCol = -1;
    while (braceLine < lines.length) {
      const col = lines[braceLine].indexOf('{');
      if (col !== -1) {
        braceCol = col;
        break;
      }
      braceLine++;
    }
    if (braceCol === -1) continue;
    
    // Find matching closing brace
    const match = findMatchingBrace(lines, braceLine, braceCol);
    if (!match) continue;
    
    const bodyStart = braceLine + 1;
    const bodyEnd = match.lineIdx;
    
    if (bodyEnd <= bodyStart) continue;
    
    const bodyContent = lines.slice(bodyStart, bodyEnd).join('\n');
    
    // Skip if body uses GraphQL engine (already correct pattern)
    if (bodyContent.includes('graphQLEngine') ||
        bodyContent.includes('executeRpc')) {
      continue;
    }
    
    // Must have BizModel calls in the body
    if (!/\b\w+Biz\.\w+\s*\(/.test(bodyContent)) {
      continue;
    }
    
    const indent = '        ';
    
    insertions.push({
      line: bodyStart,
      content: `${indent}ormTemplate.runInSession(() -> {`,
    });
    
    insertions.push({
      line: match.lineIdx,
      content: `${indent}});`,
    });
    wrappedCount++;
  }
  
  if (insertions.length === 0) return false;
  
  // Sort insertions in reverse order to not mess up line numbers
  insertions.sort((a, b) => b.line - a.line);
  
  const newLines = [...lines];
  for (const ins of insertions) {
    newLines.splice(ins.line, 0, ins.content);
  }
  
  await writeFile(absolutePath, newLines.join('\n'), 'utf8');
  console.log(`  [wrapped] ${filePath} (${wrappedCount} methods)`);
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

import { execFile } from 'child_process';
import { promisify } from 'util';
import { readFile, writeFile } from 'fs/promises';
import path from 'path';
import { fileURLToPath } from 'url';

const execFileAsync = promisify(execFile);
const __dirname = fileURLToPath(new URL('.', import.meta.url));
const rootDir = path.join(__dirname, '..');

async function findFiles() {
  const { stdout } = await execFileAsync('git', ['ls-files', '--', 'module-*/erp-*-service/src/main/java/**/*BizModel.java'], {
    cwd: rootDir,
    maxBuffer: 10 * 1024 * 1024,
  });
  return stdout.split(/\r?\n/).map(l => l.trim()).filter(Boolean);
}

async function processFile(filePath) {
  const absolutePath = path.join(rootDir, filePath);
  let content = await readFile(absolutePath, 'utf8');

  if (!content.includes('SingleSession')) return false;

  let changed = false;
  let lines = content.split('\n');

  // 1. Remove @SingleSession annotation lines (lines that are just whitespace + @SingleSession)
  lines = lines.filter(line => {
    const trimmed = line.trim();
    if (trimmed === '@SingleSession') {
      changed = true;
      return false;
    }
    return true;
  });

  // 2. Remove the import line for SingleSession
  lines = lines.filter(line => {
    if (line.includes('import') && line.includes('io.nop.api.core.annotations.orm.SingleSession')) {
      changed = true;
      return false;
    }
    return true;
  });

  // 3. Fix javadoc references to @SingleSession
  // Pattern: {@code @BizMutation}+{@code @SingleSession} -> {@code @BizMutation}
  // Pattern: {@code @SingleSession} -> {@code @BizMutation}
  lines = lines.map(line => {
    if (line.includes('SingleSession') && (line.includes('*') || line.includes('//'))) {
      const newLine = line
        .replace(/\+?\{@code @SingleSession\}/g, '')
        .replace(/\+{@code @BizMutation}/g, '{@code @BizMutation}')
        .replace(/\{@code @SingleSession\}/g, '{@code @BizMutation}');
      if (newLine !== line) {
        changed = true;
        return newLine;
      }
    }
    return line;
  });

  // 4. Remove any now-blank lines left by annotation removal (avoid double blanks)
  const result = [];
  for (let i = 0; i < lines.length; i++) {
    // Skip consecutive blank lines (keep at most one)
    if (lines[i].trim() === '' && result.length > 0 && result[result.length - 1].trim() === '') {
      continue;
    }
    result.push(lines[i]);
  }

  if (changed) {
    await writeFile(absolutePath, result.join('\n'), 'utf8');
    console.log(`  [modified] ${filePath}`);
  }
  return changed;
}

async function main() {
  const files = await findFiles();
  console.log(`Found ${files.length} BizModel files`);

  let modifiedCount = 0;
  for (const filePath of files) {
    const modified = await processFile(filePath);
    if (modified) modifiedCount++;
  }

  console.log(`\nDone: ${modifiedCount} files modified`);
}

main().catch(err => {
  console.error('Error:', err);
  process.exit(1);
});

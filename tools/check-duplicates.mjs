import { execFile } from 'child_process';
import { promisify } from 'util';
import { access, readFile, mkdir, rm } from 'fs/promises';
import path from 'path';
import { fileURLToPath } from 'url';

const execFileAsync = promisify(execFile);

const __dirname = fileURLToPath(new URL('.', import.meta.url));
const rootDir = path.join(__dirname, '..');
const toolsDir = __dirname;
const reportDir = path.join(rootDir, 'tmp', 'jscpd-report');
const reportFile = path.join(reportDir, 'jscpd-report.json');
const configFile = path.join(toolsDir, 'jscpd.config.json');
const THRESHOLD_PCT = 8;

const scanRoots = (process.env.AGE_DUPLICATE_SCAN_ROOTS || 'src,lib,packages,apps,tests,tools')
  .split(',')
  .map((value) => value.trim())
  .filter(Boolean);

async function getExistingScanRoots() {
  const roots = [];
  for (const root of scanRoots) {
    const absolutePath = path.join(rootDir, root);
    try {
      await access(absolutePath);
      roots.push(absolutePath);
    } catch {}
  }
  return roots;
}

async function main() {
  await mkdir(reportDir, { recursive: true });

  const existingScanRoots = await getExistingScanRoots();
  if (existingScanRoots.length === 0) {
    console.log('[check:duplicates] No configured scan roots found.');
    return;
  }

  const args = [
    'exec',
    'jscpd',
    ...existingScanRoots,
    '--config', configFile,
    '--reporters', 'json',
    '--output', reportDir,
    '--threshold', '0',
    '--silent',
  ];

  try {
    await execFileAsync('pnpm', args, {
      cwd: toolsDir,
      maxBuffer: 50 * 1024 * 1024,
    });
  } catch (error) {
    console.error('[check:duplicates] Failed to execute jscpd.');
    console.error(error.message);
  }

  let report;
  try {
    const raw = await readFile(reportFile, 'utf8');
    report = JSON.parse(raw);
  } catch {
    console.error('[check:duplicates] Failed to read jscpd JSON report');
    console.error('[check:duplicates] Make sure dependencies are installed in tools/: pnpm install');
    process.exit(1);
  } finally {
    try {
      await rm(reportDir, { recursive: true, force: true });
    } catch {}
  }

  const total = report.statistics.total;
  const pct = parseFloat(total?.percentage ?? '0');
  const clones = total?.clones ?? 0;
  const dupLines = total?.duplicatedLines ?? 0;
  const totalLines = total?.lines ?? 0;

  console.log(
    `[check:duplicates] ${clones} clones, ${dupLines}/${totalLines} duplicated lines (${pct}%), threshold: ${THRESHOLD_PCT}%`,
  );

  if (pct > THRESHOLD_PCT) {
    console.error(
      `[check:duplicates] ERROR: Duplicate ratio ${pct}% exceeds threshold ${THRESHOLD_PCT}%`,
    );
    process.exit(1);
  }

  console.log('[check:duplicates] OK');
}

main().catch((error) => {
  console.error('[check:duplicates] Error:', error.message);
  process.exit(1);
});

import { execFile } from 'child_process';
import { promisify } from 'util';
import { readFile } from 'fs/promises';
import path from 'path';
import { fileURLToPath } from 'url';

const execFileAsync = promisify(execFile);

const __dirname = fileURLToPath(new URL('.', import.meta.url));
const rootDir = path.join(__dirname, '..');

const ignoredPathParts = [
  'dist/',
  'node_modules/',
  'coverage/',
  'test-results/',
  '.opencode/',
];
const rootPrefixes = ['src/', 'lib/', 'tools/', 'tests/', 'packages/', 'apps/', 'docs/'];

const CODE_EXT = {
  '.ts': 'TypeScript',
  '.tsx': 'TypeScript (React)',
  '.js': 'JavaScript',
  '.jsx': 'JavaScript (React)',
  '.mjs': 'JavaScript (ESM)',
  '.css': 'CSS',
  '.scss': 'SCSS',
  '.json': 'JSON',
  '.md': 'Markdown',
  '.html': 'HTML',
  '.svg': 'SVG',
  '.sh': 'Shell',
  '.py': 'Python',
};

const CODE_ONLY_EXT = new Set(['.ts', '.tsx', '.js', '.jsx', '.mjs', '.css', '.scss']);

function isTracked(filePath) {
  if (!rootPrefixes.some((p) => filePath.startsWith(p))) return false;
  if (ignoredPathParts.some((p) => filePath.includes(p))) return false;
  return true;
}

async function getFiles() {
  const { stdout } = await execFileAsync('git', ['ls-files'], {
    cwd: rootDir,
    maxBuffer: 20 * 1024 * 1024,
  });
  return stdout.split('\n').filter(Boolean).filter(isTracked);
}

function countLines(content) {
  const lines = content.split('\n');
  const total = lines.length;
  let blank = 0;
  let comment = 0;
  let inBlock = false;

  for (const line of lines) {
    const trimmed = line.trim();
    if (trimmed === '') {
      blank++;
      continue;
    }
    if (inBlock) {
      comment++;
      if (trimmed.includes('*/')) inBlock = false;
      continue;
    }
    if (trimmed.startsWith('//') || trimmed.startsWith('*') || trimmed.startsWith('/*')) {
      comment++;
      if (trimmed.startsWith('/*') && !trimmed.includes('*/')) {
        inBlock = true;
      }
      continue;
    }
    if (trimmed.startsWith('#') || trimmed.startsWith('<!--')) {
      comment++;
      continue;
    }
  }

  return { total, blank, comment, code: total - blank - comment };
}

function getPackage(filePath) {
  const match = filePath.match(/^(apps|packages)\/([^/]+)/);
  return match ? `${match[1]}/${match[2]}` : filePath.split('/')[0];
}

function isTestFile(filePath) {
  return (
    filePath.includes('.test.') || filePath.includes('.spec.') || filePath.includes('__tests__')
  );
}

function analyzeTsComplexity(content, filePath) {
  const isTsx = filePath.endsWith('.tsx') || filePath.endsWith('.jsx');
  const patterns = [
    { re: /\bfunction\s+\w+/g, kind: 'function' },
    { re: /\bconst\s+\w+\s*=\s*(\([^)]*\)|[a-zA-Z_]\w*)\s*=>/g, kind: 'arrow' },
    { re: /\bconst\s+\w+\s*=\s*function/g, kind: 'function-expr' },
  ];

  if (isTsx) {
    patterns.push(
      { re: /\bexport\s+default\s+function\s+\w+/g, kind: 'component' },
      {
        re: /\bfunction\s+\w+\s*\([^)]*\)\s*(?::\s*[^{]+)?\{[^]*?return\s*[(<]/g,
        kind: 'component',
      },
    );
  }

  let functionCount = 0;
  for (const { re } of patterns) {
    const matches = content.match(re);
    if (matches) functionCount += matches.length;
  }

  const lines = content.split('\n');
  let codeLineCount = 0;
  for (const line of lines) {
    const t = line.trim();
    if (t && !t.startsWith('//') && !t.startsWith('*') && !t.startsWith('/*')) codeLineCount++;
  }

  const branches =
    (content.match(/\bif\s*\(/g) || []).length +
    (content.match(/\belse\s+if\b/g) || []).length +
    (content.match(/\bswitch\s*\(/g) || []).length +
    (content.match(/\bcase\s+/g) || []).length +
    (content.match(/\?\s*.+\s*:/g) || []).length;

  const imports = (content.match(/^import\s/gm) || []).length;
  const exports = (content.match(/^export\s/gm) || []).length;

  return { functionCount, codeLines: codeLineCount, branches, imports, exports };
}

function formatNumber(n) {
  return n.toLocaleString('en-US');
}

function bar(value, max, width = 30) {
  const filled = max > 0 ? Math.round((value / max) * width) : 0;
  return '\u2588'.repeat(filled) + '\u2591'.repeat(width - filled);
}

function printSection(title) {
  console.log(`\n${'='.repeat(70)}`);
  console.log(`  ${title}`);
  console.log('='.repeat(70));
}

async function main() {
  const files = await getFiles();
  const projectName = path.basename(rootDir);
  console.log(`\n  ${projectName} Code Statistics`);
  console.log(`  Generated: ${new Date().toISOString().slice(0, 10)}`);
  console.log(`  Tracked files analyzed: ${formatNumber(files.length)}`);

  const packageData = new Map();
  const langData = new Map();
  let totalLoc = 0,
    totalBlank = 0,
    totalComment = 0,
    totalCode = 0;
  let srcFiles = 0,
    testFiles = 0,
    otherFiles = 0;
  let srcCode = 0,
    testCode = 0,
    srcLoc = 0,
    testLoc = 0;
  const docsCategories = new Map();
  let docsTotalFiles = 0,
    docsTotalLoc = 0;
  const complexityAcc = {
    functionCount: 0,
    codeLines: 0,
    branches: 0,
    imports: 0,
    exports: 0,
  };
  const packageComplexity = new Map();
  const fileSizes = [];

  for (const filePath of files) {
    const ext = path.extname(filePath);
    const lang = CODE_EXT[ext];
    if (!lang) continue;

    let content;
    try {
      content = await readFile(path.join(rootDir, filePath), 'utf-8');
    } catch {
      continue;
    }

    const lc = countLines(content);
    const pkg = getPackage(filePath);
    const isTest = isTestFile(filePath);

    if (!packageData.has(pkg)) {
      packageData.set(pkg, {
        files: 0,
        loc: 0,
        blank: 0,
        comment: 0,
        code: 0,
        srcFiles: 0,
        testFiles: 0,
        otherFiles: 0,
        langs: new Map(),
      });
    }
    const pd = packageData.get(pkg);
    pd.files++;
    pd.loc += lc.total;
    pd.blank += lc.blank;
    pd.comment += lc.comment;
    pd.code += lc.code;
    if (isTest) pd.testFiles++;
    else if (CODE_ONLY_EXT.has(ext)) pd.srcFiles++;
    else pd.otherFiles++;

    const langKey = lang;
    if (!pd.langs.has(langKey)) pd.langs.set(langKey, { files: 0, code: 0 });
    pd.langs.get(langKey).files++;
    pd.langs.get(langKey).code += lc.code;

    if (!langData.has(langKey))
      langData.set(langKey, { files: 0, loc: 0, blank: 0, comment: 0, code: 0 });
    const ld = langData.get(langKey);
    ld.files++;
    ld.loc += lc.total;
    ld.blank += lc.blank;
    ld.comment += lc.comment;
    ld.code += lc.code;

    totalLoc += lc.total;
    totalBlank += lc.blank;
    totalComment += lc.comment;
    totalCode += lc.code;

    if (isTest) testFiles++;
    else if (CODE_ONLY_EXT.has(ext)) srcFiles++;
    else otherFiles++;

    if (isTest) {
      testCode += lc.code;
      testLoc += lc.total;
    } else if (CODE_ONLY_EXT.has(ext)) {
      srcCode += lc.code;
      srcLoc += lc.total;
    }

    if (ext === '.ts' || ext === '.tsx' || ext === '.js' || ext === '.jsx' || ext === '.mjs') {
      const cx = analyzeTsComplexity(content, filePath);
      complexityAcc.functionCount += cx.functionCount;
      complexityAcc.codeLines += cx.codeLines;
      complexityAcc.branches += cx.branches;
      complexityAcc.imports += cx.imports;
      complexityAcc.exports += cx.exports;

      if (!packageComplexity.has(pkg)) {
        packageComplexity.set(pkg, {
          functionCount: 0,
          codeLines: 0,
          branches: 0,
          imports: 0,
          exports: 0,
          files: 0,
        });
      }
      const pc = packageComplexity.get(pkg);
      pc.functionCount += cx.functionCount;
      pc.codeLines += cx.codeLines;
      pc.branches += cx.branches;
      pc.imports += cx.imports;
      pc.exports += cx.exports;
      pc.files++;
    }

    if (filePath.startsWith('docs/') && ext === '.md') {
      const parts = filePath.split('/');
      const category = parts.length > 2 ? parts[1] : 'root';
      if (!docsCategories.has(category)) {
        docsCategories.set(category, { files: 0, loc: 0, code: 0 });
      }
      const dc = docsCategories.get(category);
      dc.files++;
      dc.loc += lc.total;
      dc.code += lc.code;
      docsTotalFiles++;
      docsTotalLoc += lc.total;
    }

    fileSizes.push({ path: filePath, loc: lc.total, code: lc.code, ext });
  }

  printSection('Overall Summary');
  console.log(`
  Total files        : ${formatNumber(files.length)}
  Source files (code) : ${formatNumber(srcFiles)}
  Test files          : ${formatNumber(testFiles)}
  Other files         : ${formatNumber(otherFiles)}

  Total lines         : ${formatNumber(totalLoc)}
  Code lines          : ${formatNumber(totalCode)} (${totalLoc > 0 ? ((totalCode / totalLoc) * 100).toFixed(1) : '0'}%)
  Comment lines       : ${formatNumber(totalComment)} (${totalLoc > 0 ? ((totalComment / totalLoc) * 100).toFixed(1) : '0'}%)
  Blank lines         : ${formatNumber(totalBlank)} (${totalLoc > 0 ? ((totalBlank / totalLoc) * 100).toFixed(1) : '0'}%)

  Test-to-source ratio: ${srcFiles > 0 ? (testFiles / srcFiles).toFixed(2) : 'N/A'}
  Avg LOC per file    : ${files.length > 0 ? (totalLoc / files.length).toFixed(1) : '0'}
  Avg code per file   : ${files.length > 0 ? (totalCode / files.length).toFixed(1) : '0'}

  ── Source vs Test ───────────────────────────────────────
                    Files       LOC      Code
  Source files     : ${formatNumber(srcFiles).padStart(5)}  ${formatNumber(srcLoc).padStart(9)}  ${formatNumber(srcCode).padStart(9)}
  Test files       : ${formatNumber(testFiles).padStart(5)}  ${formatNumber(testLoc).padStart(9)}  ${formatNumber(testCode).padStart(9)}
  Test/Source(code): ${srcCode > 0 ? (testCode / srcCode).toFixed(2) : 'N/A'}

  ── Documentation ───────────────────────────────────────
  ${'Category'.padEnd(18)} ${'Files'.padStart(6)} ${'LOC'.padStart(8)} ${'Size (est.)'.padStart(12)}
  ${'─'.repeat(48)}
${[...docsCategories.entries()]
  .sort((a, b) => b[1].loc - a[1].loc)
  .map(([cat, d]) => {
    const kb = Math.round((d.loc * 45) / 1024);
    const sizeStr = kb >= 1024 ? `${(kb / 1024).toFixed(1)} MB` : `${formatNumber(kb)} KB`;
    return `  ${cat.padEnd(18)} ${formatNumber(d.files).padStart(6)} ${formatNumber(d.loc).padStart(8)} ${sizeStr.padStart(12)}`;
  })
  .join('\n')}
  ${'─'.repeat(48)}
  ${'TOTAL'.padEnd(18)} ${formatNumber(docsTotalFiles).padStart(6)} ${formatNumber(docsTotalLoc).padStart(8)} ${formatNumber(Math.round((docsTotalLoc * 45) / 1024)).padStart(10)} KB
  Docs/Source (LOC)  : ${srcLoc > 0 ? (docsTotalLoc / srcLoc).toFixed(2) : 'N/A'}`);

  printSection('By Language');
  const langRows = [...langData.entries()]
    .sort((a, b) => b[1].code - a[1].code)
    .map(([lang, d]) => ({ lang, ...d }));
  const maxLangCode = Math.max(1, ...langRows.map((r) => r.code));

  console.log(`
  ${'Language'.padEnd(22)} ${'Files'.padStart(7)} ${'LOC'.padStart(9)} ${'Code'.padStart(9)} ${'Comment'.padStart(9)} ${'Blank'.padStart(9)}  Distribution`);
  console.log('  ' + '-'.repeat(95));
  for (const r of langRows) {
    console.log(
      `  ${r.lang.padEnd(22)} ${formatNumber(r.files).padStart(7)} ${formatNumber(r.loc).padStart(9)} ${formatNumber(r.code).padStart(9)} ${formatNumber(r.comment).padStart(9)} ${formatNumber(r.blank).padStart(9)}  ${bar(r.code, maxLangCode, 20)}`,
    );
  }

  printSection('By Package (sorted by code lines)');
  const pkgRows = [...packageData.entries()].sort((a, b) => b[1].code - a[1].code);
  const maxPkgCode = Math.max(1, ...pkgRows.map(([, d]) => d.code));

  console.log(`
  ${'Package'.padEnd(42)} ${'Files'.padStart(5)} ${'Code'.padStart(8)} ${'Cmt'.padStart(6)} ${'Test'.padStart(5)} ${'Src'.padStart(5)} ${'Oth'.padStart(5)}  Distribution`);
  console.log('  ' + '-'.repeat(100));
  for (const [pkg, d] of pkgRows) {
    console.log(
      `  ${pkg.padEnd(42)} ${formatNumber(d.files).padStart(5)} ${formatNumber(d.code).padStart(8)} ${formatNumber(d.comment).padStart(6)} ${formatNumber(d.testFiles).padStart(5)} ${formatNumber(d.srcFiles).padStart(5)} ${formatNumber(d.otherFiles).padStart(5)}  ${bar(d.code, maxPkgCode, 15)}`,
    );
  }

  printSection('Complexity (TS/JS files only)');
  const avgFuncLen =
    complexityAcc.functionCount > 0
      ? (complexityAcc.codeLines / complexityAcc.functionCount).toFixed(1)
      : 'N/A';
  const branchDensity =
    complexityAcc.codeLines > 0
      ? ((complexityAcc.branches / complexityAcc.codeLines) * 1000).toFixed(1)
      : '0';

  console.log(`
  Functions/methods   : ${formatNumber(complexityAcc.functionCount)}
  Total code lines    : ${formatNumber(complexityAcc.codeLines)}
  Avg function length : ${avgFuncLen} lines
  Branch statements   : ${formatNumber(complexityAcc.branches)}
  Branch density      : ${branchDensity} per 1000 LOC
  Import statements   : ${formatNumber(complexityAcc.imports)}
  Export statements   : ${formatNumber(complexityAcc.exports)}
  Import/Export ratio : ${complexityAcc.exports > 0 ? (complexityAcc.imports / complexityAcc.exports).toFixed(2) : 'N/A'}`);

  printSection('Largest Files (by code lines, top 20)');
  const topFiles = fileSizes
    .filter((f) =>
      ['.ts', '.tsx', '.js', '.jsx', '.mjs'].includes(f.ext),
    )
    .filter((f) => !isTestFile(f.path))
    .sort((a, b) => b.code - a.code)
    .slice(0, 20);

  console.log(`\n  ${'File'.padEnd(65)} ${'LOC'.padStart(6)} ${'Code'.padStart(6)}`);
  console.log('  ' + '-'.repeat(80));
  for (const f of topFiles) {
    console.log(
      `  ${f.path.padEnd(65)} ${formatNumber(f.loc).padStart(6)} ${formatNumber(f.code).padStart(6)}`,
    );
  }

  console.log('\n');
}

main().catch((err) => {
  console.error('Error:', err.message);
  process.exit(1);
});

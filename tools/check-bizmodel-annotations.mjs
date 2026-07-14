import { execFile } from 'child_process';
import { promisify } from 'util';
import { readFile } from 'fs/promises';
import path from 'path';
import { fileURLToPath } from 'url';

const execFileAsync = promisify(execFile);

const __dirname = fileURLToPath(new URL('.', import.meta.url));
const rootDir = path.join(__dirname, '..');

const BIZMODEL_GLOB = 'module-*/erp-*-service/src/main/java/**/*BizModel.java';
const EXCLUDED = ['/_gen/', '/target/'];

const PROHIBITED_PAIRS = [
  { bizAnnotation: 'BizMutation', prohibited: 'SingleSession', message: '@BizMutation 方法不应叠加 @SingleSession（BizModel 管道已提供 ORM Session）' },
  { bizAnnotation: 'BizMutation', prohibited: 'Transactional', message: '@BizMutation 方法不应叠加 @Transactional（@BizMutation 已自动包装事务）' },
];

/**
 * Check if an intentional marker exists near the method's annotations.
 *
 * Marker syntax: a comment line containing "nop-check: allow" anywhere within
 * the 10 lines preceding the method signature (covers javadoc + annotations).
 *
 * Example:
 * <pre>
 *   // nop-check: allow @Transactional(REQUIRES_NEW) for posting isolation
 *   @BizMutation
 *   @Transactional(REQUIRES_NEW)
 *   public Long post(...) { ... }
 * </pre>
 */
const ALLOW_MARKER_RE = /nop-check:\s*allow/i;

function hasAllowMarker(lines, methodLine, contextLines = 10) {
  const start = Math.max(0, methodLine - contextLines);
  for (let i = start; i < methodLine; i++) {
    const trimmed = lines[i].trim();
    if (trimmed.startsWith('//') && ALLOW_MARKER_RE.test(trimmed)) {
      return true;
    }
    if (trimmed.startsWith('*') && ALLOW_MARKER_RE.test(trimmed)) {
      return true;
    }
  }
  return false;
}

/**
 * Check if a javadoc block preceding the method contains the allow marker.
 */
function hasAllowInJavadoc(lines, methodLine) {
  // Scan backwards for `/**` starting within 20 lines
  let javadocStart = -1;
  for (let i = methodLine - 1; i >= Math.max(0, methodLine - 30); i--) {
    const trimmed = lines[i].trim();
    if (trimmed.startsWith('/**')) {
      javadocStart = i;
      break;
    }
  }
  if (javadocStart < 0) return false;
  for (let i = javadocStart; i < methodLine; i++) {
    const trimmed = lines[i].trim();
    if (ALLOW_MARKER_RE.test(trimmed)) {
      return true;
    }
  }
  return false;
}

function shouldExclude(filePath) {
  return EXCLUDED.some(p => filePath.includes(p));
}

/**
 * Simplified Java annotation parser.
 * Returns array of { methodName, annotations: Set<string>, line: number }
 */
function parseMethodAnnotations(content) {
  const methods = [];
  const lines = content.split('\n');

  let inBlockComment = false;
  let currentAnnotations = [];
  let currentMethodName = null;
  let currentMethodLine = 0;

  for (let i = 0; i < lines.length; i++) {
    const line = lines[i];
    const lineNum = i + 1;

    // Track block comments
    if (!inBlockComment) {
      const blockStart = line.indexOf('/*');
      if (blockStart !== -1) {
        inBlockComment = true;
        if (line.includes('*/')) {
          inBlockComment = false;
        }
        continue;
      }
    } else {
      if (line.includes('*/')) {
        inBlockComment = false;
      }
      continue;
    }

    // Skip line comments and empty lines
    const trimmed = line.trim();
    if (trimmed.startsWith('//')) continue;
    if (trimmed === '') {
      // Non-empty line resets accumulation
      continue;
    }

    // Match annotations: @AnnotationName, @AnnotationName(...)
    const annotationMatch = trimmed.match(/^@(\w+)/);
    if (annotationMatch) {
      currentAnnotations.push(annotationMatch[1]);
      currentMethodLine = lineNum;
      continue;
    }

    if (trimmed.startsWith('@')) {
      currentAnnotations.push(trimmed.split(/[ (]/)[0].replace('@', ''));
      currentMethodLine = lineNum;
      continue;
    }

    // Match method declaration: qualifiers returnType methodName(...)
    // Skip field declarations, class declarations, etc.
    const methodMatch = trimmed.match(/^\s*(?:public|protected|private)\s+(?:\S+\s+)*?(\w+)\s*\(/);
    if (methodMatch && !trimmed.includes(' class ') && !trimmed.includes(' interface ')) {
      currentMethodName = methodMatch[1];
      if (currentAnnotations.length > 0) {
        methods.push({
          methodName: currentMethodName,
          annotations: new Set(currentAnnotations),
          line: currentMethodLine || lineNum,
        });
      }
      currentAnnotations = [];
      currentMethodName = null;
      currentMethodLine = 0;
      continue;
    }

    // Non-annotation, non-method line → reset annotation accumulation
    // (but only if we see something that looks like a declaration keyword or closing brace)
    if (trimmed.includes(' class ') || trimmed.includes(' interface ') || trimmed.startsWith('}')) {
      currentAnnotations = [];
      currentMethodName = null;
      currentMethodLine = 0;
    }
  }

  return methods;
}

async function findBizModelFiles() {
  const { stdout } = await execFileAsync('git', ['ls-files', '--', ...BIZMODEL_GLOB.split(' ')], {
    cwd: rootDir,
    maxBuffer: 10 * 1024 * 1024,
  });
  return stdout
    .split(/\r?\n/)
    .map(l => l.trim())
    .filter(Boolean)
    .filter(p => !shouldExclude(p));
}

async function main() {
  const files = await findBizModelFiles();
  let totalViolations = 0;
  const allReports = [];

  for (const filePath of files) {
    const absolutePath = path.join(rootDir, filePath);
    const content = await readFile(absolutePath, 'utf8');
    const methods = parseMethodAnnotations(content);

    const fileViolations = [];

    const lines = content.split('\n');

    for (const method of methods) {
      for (const rule of PROHIBITED_PAIRS) {
        if (method.annotations.has(rule.bizAnnotation) && method.annotations.has(rule.prohibited)) {
          const hasMarker = hasAllowMarker(lines, method.line - 1, 10)
                        || hasAllowInJavadoc(lines, method.line - 1);
          if (hasMarker) {
            continue;
          }
          fileViolations.push({
            method: method.methodName,
            line: method.line,
            rule,
          });
        }
      }
    }

    if (fileViolations.length > 0) {
      allReports.push({ file: filePath, violations: fileViolations });
      totalViolations += fileViolations.length;

      console.error(`\n[check-bizmodel-annotations] ${filePath}:`);
      for (const v of fileViolations) {
        console.error(`  L${v.line}  ${v.method} — ${v.rule.message}`);
      }
    }
  }

  if (totalViolations > 0) {
    console.error(`\n[check-bizmodel-annotations] FAIL: ${totalViolations} violation(s) across ${allReports.length} file(s)`);
    process.exit(1);
  }

  console.log(`[check-bizmodel-annotations] OK: ${files.length} BizModel files checked, 0 violations`);
}

main().catch((error) => {
  console.error('[check-bizmodel-annotations] Error:', error);
  process.exit(1);
});

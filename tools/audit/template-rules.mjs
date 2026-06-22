import { createResult, getLineNumber, getLineText, isTestFile, scanTopLevelLets } from './shared.mjs';

function scanEmptyCatchBlocks({ rule, relativePath, content }) {
  const results = [];
  const catchPattern = /\bcatch\s*(?:\([^)]*\))?\s*\{\s*\}/g;
  let match;

  while ((match = catchPattern.exec(content)) !== null) {
    const line = getLineNumber(content, match.index);
    results.push(createResult(rule, relativePath, line, getLineText(content, line), match[0]));
  }

  return results;
}

function scanThenWithoutCatch({ rule, relativePath, content }) {
  const results = [];
  const lines = content.split(/\r?\n/);

  for (let index = 0; index < lines.length; index += 1) {
    const lineText = lines[index] ?? '';
    if (!lineText.includes('.then(')) {
      continue;
    }
    if (lineText.includes('.catch(') || lineText.includes('return ')) {
      continue;
    }
    results.push(createResult(rule, relativePath, index + 1, lineText, 'then chain without catch'));
  }

  return results;
}

function scanTodoComments({ rule, relativePath, content }) {
  const results = [];
  const lines = content.split(/\r?\n/);

  for (let index = 0; index < lines.length; index += 1) {
    const lineText = lines[index] ?? '';
    if (/\b(?:TODO|FIXME|HACK)\b/.test(lineText)) {
      results.push(createResult(rule, relativePath, index + 1, lineText, 'TODO/FIXME/HACK'));
    }
  }

  return results;
}

export const genericAuditRules = [
  {
    id: 'empty-catch-block',
    severity: 'medium',
    description: 'catch block silently ignores failures',
    include: (filePath) => /\.(ts|tsx|js|jsx|mjs|cjs)$/.test(filePath) && !isTestFile(filePath),
    scanWithContent: scanEmptyCatchBlocks,
  },
  {
    id: 'then-chain-no-catch',
    severity: 'medium',
    description: 'promise chain appears to have no local catch path',
    include: (filePath) => /\.(ts|tsx|js|jsx|mjs|cjs)$/.test(filePath) && !isTestFile(filePath),
    scanWithContent: scanThenWithoutCatch,
  },
  {
    id: 'test-module-top-let',
    severity: 'medium',
    description: 'mutable module-top state in tests can leak across cases',
    include: (filePath) => isTestFile(filePath),
    scanWithContent: scanTopLevelLets,
  },
  {
    id: 'todo-comment-in-source',
    severity: 'info',
    description: 'leftover TODO/FIXME/HACK marker may indicate unfinished work',
    include: (filePath) => /\.(ts|tsx|js|jsx|mjs|cjs|css)$/.test(filePath),
    scanWithContent: scanTodoComments,
  },
];

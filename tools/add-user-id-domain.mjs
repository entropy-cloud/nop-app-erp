#!/usr/bin/env node
/**
 * Add stdDomain="userId" to user-type FK columns across all non-HR ORM files.
 */
import { readFileSync, writeFileSync, readdirSync, statSync } from 'fs';
import path from 'path';

const rootDir = process.argv[2] || process.cwd();

// User-type columns that represent login user references (not employee refs)
const USER_COLUMNS = new Set([
  'ownerId', 'approverId', 'operatorId', 'assignedToId', 'teamLeaderId',
  'handlerId', 'pickerId', 'interviewerId', 'closedBy', 'postedBy',
  'approvedById', 'reviewerId', 'fromStaffId', 'toStaffId', 'requesterId',
  'changedBy',
]);

function extract(s, name) {
  if (!s) return null;
  const e = name.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  const m = s.match(new RegExp(`${e}\\s*=\\s*"([^"]*)"`, 'i'));
  return m ? m[1] : null;
}

function main() {
  const files = [];
  for (const entry of readdirSync(rootDir)) {
    if (!entry.startsWith('module-')) continue;
    const modelDir = path.join(rootDir, entry, 'model');
    try {
      if (!statSync(modelDir).isDirectory()) continue;
      for (const f of readdirSync(modelDir).filter(f => f.endsWith('.orm.xml')))
        files.push({ fullPath: path.join(modelDir, f), module: entry.replace('module-', '') });
    } catch (e) {}
  }

  let totalUpdated = 0;

  for (const { fullPath, module: mod } of files) {
    // Skip HR module — user-type columns there are employee refs
    if (mod === 'hr') {
      console.log(`  [SKIP] ${mod} (HR module — employee refs)`);
      continue;
    }

    const text = readFileSync(fullPath, 'utf-8');
    let newText = text;
    let changed = false;

    // Find all <column> elements with user-type column names
    const colRegex = /<column\s+([^>]*?)(\/>|><\/column>)/g;
    let match;

    // Use while loop with manual lastIndex tracking to avoid infinite loop
    const matches = [];
    while ((match = colRegex.exec(text)) !== null) {
      const attrs = match[1];
      const colName = extract(attrs, 'name');
      if (!colName) continue;
      if (!USER_COLUMNS.has(colName)) continue;

      // Check if already has stdDomain or domain attribute
      const hasStdDomain = attrs.includes('stdDomain=');
      const hasDomain = attrs.includes('domain=');
      if (hasStdDomain || hasDomain) continue;

      matches.push({ index: match.index, length: match[0].length, attrs, colName });
    }

    // Apply replacements (in reverse order to preserve positions)
    for (const m of matches.reverse()) {
      const oldStr = text.substring(m.index, m.index + m.length);
      const newStr = oldStr.replace(`name="${m.colName}"`, `name="${m.colName}" stdDomain="userId"`);
      newText = newText.substring(0, m.index) + newStr + newText.substring(m.index + m.length);
      changed = true;
      totalUpdated++;
      console.log(`  [ADD] ${mod}: ${m.colName}`);
    }

    if (changed) {
      writeFileSync(fullPath, newText, 'utf-8');
      console.log(`  ✓ ${fullPath}`);
    }
  }

  console.log(`\n=== Total columns updated with stdDomain="userId": ${totalUpdated} ===`);
}

main();

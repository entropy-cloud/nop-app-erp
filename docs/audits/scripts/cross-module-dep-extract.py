#!/usr/bin/env python3
"""
Cross-module dependency audit for nop-app-erp.
Scans all module-*/model/*.orm.xml, extracts:
  - to-one / to-many refEntityName edges
  - <entity notGenCode="true"> external entity declarations
  - cross-module edges (where source domain != target domain)
Builds DAG, detects cycles, computes per-domain stats.
"""
import os
import re
import sys
import xml.etree.ElementTree as ET
from collections import defaultdict, OrderedDict

REPO = "/Users/abc/app/nop-app-erp"

# Map: module-<domain>/ -> (canonical_label, package_short_used_in_code)
# canonical_label is what we display (matches scope matrix column headers).
# package_short is the actual label appearing in `app.erp.<package_short>.dao.entity.*`.
# Most modules: canonical_label == package_short.
# Exception: module-contract uses package `app.erp.contract.dao.entity.*` (full word),
# but its entity prefix is `ErpCt*` and tableName `erp_ct_*`. We canonicalize to `ct`.
MODULES = [
    ("master-data",  "md",   "md"),
    ("inventory",    "inv",  "inv"),
    ("purchase",     "pur",  "pur"),
    ("sales",        "sal",  "sal"),
    ("finance",      "fin",  "fin"),
    ("assets",       "ast",  "ast"),
    ("projects",     "prj",  "prj"),
    ("manufacturing","mfg",  "mfg"),
    ("quality",      "qa",   "qa"),
    ("maintenance",  "mnt",  "mnt"),
    ("crm",          "crm",  "crm"),
    ("cs",           "cs",   "cs"),
    ("hr",           "hr",   "hr"),
    ("aps",          "aps",  "aps"),
    ("contract",     "ct",   "contract"),  # anomaly: package uses full word
    ("drp",          "drp",  "drp"),
    ("logistics",    "log",  "log"),
    ("b2b",          "b2b",  "b2b"),
    ("notify",       "notify","notify"),
]
SHORT_OF = {m[0]: m[1] for m in MODULES}        # module-dir-name -> canonical_label
PKG_OF   = {m[0]: m[2] for m in MODULES}        # module-dir-name -> package short used in code
CANON_OF_PKG = {m[2]: m[1] for m in MODULES}    # package short -> canonical_label
DOMAIN_OF_SHORT = {m[1]: m[0] for m in MODULES}

# Allowed cross-business-domain (non-master-data) ORM edges, per module-boundaries.md / data-dependency-matrix.md §5.6.2
ALLOWED_CROSS_BIZ = {
    # finance -> projects / assets
    ("fin", "prj"),
    ("fin", "ast"),
    # purchase / sales -> projects
    ("pur", "prj"),
    ("sal", "prj"),
    # hr -> projects (ErpPrjProject / ErpPrjTask)
    ("hr", "prj"),
    # manufacturing -> inventory (ErpInvBatch), approved R-only
    ("mfg", "inv"),
    # maintenance -> assets (ErpAstAsset), approved R-only
    ("mnt", "ast"),
    # drp -> inventory (ErpInvStockMove), approved R-only
    ("drp", "inv"),
}

# ORM files
orm_files = []
for d in sorted(os.listdir(REPO)):
    p = os.path.join(REPO, d)
    if d.startswith("module-") and os.path.isdir(p):
        dir_name = d[len("module-"):]
        canonical = SHORT_OF.get(dir_name)
        if canonical:
            mdir = os.path.join(p, "model")
            if os.path.isdir(mdir):
                for f in os.listdir(mdir):
                    if f.endswith(".orm.xml"):
                        orm_files.append((canonical, d, os.path.join(mdir, f)))
orm_files.sort()
print(f"=== ORM files found: {len(orm_files)} ===", file=sys.stderr)

# Collect
# edges: list of (src_short, tgt_short, src_entity, tgt_entity_full, rel_name, rel_kind, src_file, line_no)
edges = []
# external entity declarations: (declaring_short, declared_entity_full, tableName, file)
externals = defaultdict(list)  # declaring_short -> list of (full_name, tableName, file)
# entity declarations (own): set of entity className per short, with notGenCode flag
own_entities = defaultdict(set)  # short -> set of full class name that are GENERATED here (no notGenCode)
declared_extern_in_module = defaultdict(set)  # short -> set of full class names declared with notGenCode=true

ENTITY_NAME_RE = re.compile(r'app\.erp\.([a-z][a-z0-9]*)\.dao\.entity\.([A-Za-z0-9_]+)')

def canon_of_ref(ref_full):
    """Map a refEntityName like app.erp.X.dao.entity.Y to canonical module label.
    Returns (canon_label, entity_simple_name) or (None, None) if package unknown."""
    m = ENTITY_NAME_RE.match(ref_full)
    if not m:
        return (None, None)
    pkg = m.group(1)
    ent = m.group(2)
    return (CANON_OF_PKG.get(pkg), ent)

# We parse with ET but need line numbers; use iterparse with line-aware approach.
# Simpler: do regex-based pass to keep line numbers AND handle namespaces. But ET is safer for accuracy.
# Compromise: do two passes — ET for structure, regex for line numbers on declarations and to-one/to-many.

for src_short, src_dir, path in orm_files:
    with open(path, 'r', encoding='utf-8') as fh:
        text = fh.read()
    # Pre-compute line offsets for char->line
    # Use regex finditer with line counting via count('\n', end of preceding)
    # Entity tags may span multiple lines (className on line N, more attrs on N+1, closing > later).
    # Use DOTALL so [^>]*? matches newlines too (well, [^>] already crosses newlines; issue was
    # the python regex docs lie — [^>] DOES match newlines by default in Python's re).
    # Real issue: when className is the LAST attr before newline+more-attrs, the closing '>' is on
    # a later line and [^>]*? stops at first '>'. That works. So the real fix is: also accept when
    # entity tag has 'name' attr but NOT 'className' (pure external entity declaration uses name= only).
    ENT_RE = re.compile(r'<entity\b([^>]*?)>', re.DOTALL)
    for m in ENT_RE.finditer(text):
        attrs = m.group(1)
        if 'notGenCode' in attrs and re.search(r'\bnotGenCode\s*=\s*"true"', attrs):
            nm = re.search(r'\bname\s*=\s*"([^"]+)"', attrs)
            tn = re.search(r'\btableName\s*=\s*"([^"]+)"', attrs)
            if nm:
                full = nm.group(1)
                table = tn.group(1) if tn else ""
                line_no = text.count('\n', 0, m.start()) + 1
                externals[src_short].append((full, table, os.path.relpath(path, REPO), line_no))
                declared_extern_in_module[src_short].add(full)
        else:
            cm = re.search(r'\bclassName\s*=\s*"(app\.erp\.[a-z][a-z0-9]*\.dao\.entity\.[A-Za-z0-9_]+)"', attrs)
            if cm:
                own_entities[src_short].add(cm.group(1))
    # === relations: to-one / to-many ===
    # <to-one name="..." refEntityName="app.erp.X.dao.entity.Y" ...>
    for m in re.finditer(r'<(to-one|to-many)\b([^>]*?)>', text, re.DOTALL):
        kind = m.group(1)
        attrs = m.group(2)
        nm = re.search(r'\bname\s*=\s*"([^"]+)"', attrs)
        ref = re.search(r'\brefEntityName\s*=\s*"([^"]+)"', attrs)
        if not ref:
            continue
        ref_full = ref.group(1)
        tgt_canon, tgt_entity = canon_of_ref(ref_full)
        if tgt_canon is None:
            continue  # not a known module entity (e.g. platform NopSys*, NopAuth*)
        # find enclosing entity className — search backwards for last <entity className=...> or <entity name=...>
        prefix = text[:m.start()]
        enclosing = None
        for em2 in ENT_RE.finditer(prefix):
            a2 = em2.group(1)
            cm = re.search(r'\bclassName\s*=\s*"(app\.erp\.[a-z][a-z0-9]*\.dao\.entity\.[A-Za-z0-9_]+)"', a2)
            if cm:
                enclosing = cm.group(1)
        src_entity_full = enclosing if enclosing else "?"
        line_no = text.count('\n', 0, m.start()) + 1
        edges.append((src_short, tgt_canon, src_entity_full, ref_full,
                      nm.group(1) if nm else "?", kind, os.path.relpath(path, REPO), line_no))

# === Cross-module edges only (src_short != tgt_short) ===
cross_edges = [e for e in edges if e[0] != e[1]]

# === DAG cycle detection (cross-module graph only) ===
adj = defaultdict(set)
for s, t, *_ in cross_edges:
    adj[s].add(t)

WHITE, GRAY, BLACK = 0, 1, 2
color = {}
cycles = []

def dfs(node, stack):
    color[node] = GRAY
    stack.append(node)
    for nb in sorted(adj.get(node, ())):
        if color.get(nb, WHITE) == GRAY:
            # found cycle
            idx = stack.index(nb)
            cycles.append(stack[idx:] + [nb])
        elif color.get(nb, WHITE) == WHITE:
            dfs(nb, stack)
    stack.pop()
    color[node] = BLACK

for n in sorted(adj.keys()):
    if color.get(n, WHITE) == WHITE:
        dfs(n, [])

# dedupe cycles
seen_cycle = set()
uniq_cycles = []
for c in cycles:
    key = tuple(sorted(set(c)))
    if key not in seen_cycle:
        seen_cycle.add(key)
        uniq_cycles.append(c)

# === Per-domain stats ===
def short_to_label(s):
    return f"{s} ({DOMAIN_OF_SHORT.get(s, '?')})"

# Build maps for external entity names per domain (as base entity short name):
# We need to check completeness: every cross-module refEntityName should have a <entity notGenCode> declared in the SOURCE module.
# Group cross edges by (src, tgt_full)
edges_by_src = defaultdict(list)
for e in cross_edges:
    edges_by_src[e[0]].append(e)

# === Print results ===
print("# Cross-Module Dependency Audit — Automated Extraction")
print()
print(f"Repository: {REPO}")
print(f"ORM files scanned: {len(orm_files)}")
print()
print("## 1. Cross-module edge inventory (full list)")
print()
print("| # | src | src_entity | -> | tgt | tgt_entity | rel | kind | file:line |")
print("|---|-----|------------|----|-----|------------|-----|------|-----------|")
for i, (s, t, se, te, name, kind, f, ln) in enumerate(sorted(cross_edges, key=lambda x: (x[0], x[3], x[2], x[4])), 1):
    se_short = se.rsplit('.', 1)[-1] if se != "?" else "?"
    print(f"| {i} | {s} | {se_short} | -> | {t} | {te} | {name} | {kind} | {f}:{ln} |")
print()
print(f"**Total cross-module relations (edges):** {len(cross_edges)} (to-one + to-many combined)")
to_one_cnt = sum(1 for e in cross_edges if e[5] == 'to-one')
to_many_cnt = sum(1 for e in cross_edges if e[5] == 'to-many')
print(f"- to-one: **{to_one_cnt}**")
print(f"- to-many: **{to_many_cnt}** (within-module to-many excluded; these are cross-module to-many, e.g. finance referencing master-data M:N)")
print()

# dedup by (src, tgt_full) for "edges" in DAG terms
dag_edges = sorted({(s, t) for s, t, *_ in cross_edges})
print(f"**Unique DAG edges (src,tgt) pairs:** {len(dag_edges)}")
print()
print("## 2. DAG edges (src -> tgt)")
print()
print("| src | -> | tgt | count(relations) |")
print("|-----|----|-----|------------------|")
edge_count = defaultdict(int)
for s, t, *_ in cross_edges:
    edge_count[(s, t)] += 1
for (s, t), c in sorted(edge_count.items()):
    print(f"| {s} | -> | {t} | {c} |")
print()

# === Cycle detection results ===
print("## 3. Cycle detection")
print()
if uniq_cycles:
    print(f"**❌ CYCLES FOUND: {len(uniq_cycles)}**")
    for c in uniq_cycles:
        print(f"- {' -> '.join(c)}")
else:
    print("**✅ NO CYCLES** — DAG verified acyclic (cross-module graph).")
print()

# === DAG compliance: allowed cross-business-domain edges ===
print("## 4. DAG compliance (allowed vs forbidden cross-BIZ edges)")
print()
print("Note: edges to `md` (master-data, root) are ALWAYS allowed (one-way business -> master-data).")
print("Cross-business-domain edges (non-md) checked against module-boundaries.md allow-list:")
print()
print("| src | tgt | count | status |")
print("|-----|-----|-------|--------|")
allowed_ok = 0
forbidden = 0
for (s, t), c in sorted(edge_count.items()):
    if t == 'md':
        continue  # always allowed
    if s == t:
        continue
    if (s, t) in ALLOWED_CROSS_BIZ:
        print(f"| {s} | {t} | {c} | ✅ allowed |")
        allowed_ok += 1
    else:
        print(f"| {s} | {t} | {c} | ❌ FORBIDDEN |")
        forbidden += 1
print()
print(f"Allowed cross-biz edges: {allowed_ok}; forbidden: {forbidden}")
print()

# === External entity declarations (notGenCode) per module ===
print("## 5. External entity declarations (`<entity notGenCode=\"true\">`) per module")
print()
print("| declaring module | external entities declared |")
print("|------------------|-----------------------------|")
total_ext = 0
for s in sorted(externals.keys()):
    n = len(externals[s])
    total_ext += n
    print(f"| {s} | {n} |")
print(f"| **TOTAL** | **{total_ext}** |")
print()
print("Detail of every external entity declaration (declaring module, entity, tableName):")
print()
print("| declaring | entity | tableName | file:line |")
print("|-----------|--------|-----------|-----------|")
for s in sorted(externals.keys()):
    for full, table, f, ln in sorted(externals[s]):
        ent = full.rsplit('.', 1)[-1]
        print(f"| {s} | {ent} | {table} | {f}:{ln} |")
print()

# === Completeness matrix: every cross-module refEntityName must have a notGenCode declaration in the SOURCE module ===
print("## 6. External entity declaration completeness (Mechanism B)")
print()
print("Rule: every cross-module `<to-one>/<to-many refEntityName=...>` in module S must have a corresponding")
print("`<entity notGenCode=\"true\">` declaration in module S (declaring the external entity to skip codegen).")
print()
print("### 6.1 Per (src, refEntityName) — declaration status")
print()
print("| src | refEntityName | declared notGenCode? |")
print("|-----|---------------|----------------------|")
referenced_full_by_src = defaultdict(set)
for s, t, se, te_full, name, kind, f, ln in cross_edges:
    referenced_full_by_src[s].add(te_full)
missing = []
covered = 0
for s in sorted(referenced_full_by_src.keys()):
    declared = declared_extern_in_module.get(s, set())
    for full in sorted(referenced_full_by_src[s]):
        if full in declared:
            print(f"| {s} | {full.rsplit('.',1)[-1]} | ✅ yes |")
            covered += 1
        else:
            print(f"| {s} | {full.rsplit('.',1)[-1]} | ❌ MISSING |")
            missing.append((s, full))
print()
print(f"**Coverage: {covered} / {covered + len(missing)} referenced external entities have `<entity notGenCode>` declaration in source module.**")
if missing:
    print()
    print("MISSING declarations:")
    for s, full in missing:
        print(f"- {s} references {full} but no `<entity notGenCode=\"true\">` found")
print()

# === Per-domain to-one counts (matches owner doc format) ===
print("## 7. Per-domain cross-module to-one counts")
print()
print("Matches owner doc §5.6.2 / scope matrix §1.1 'cross-domain to-one' metric:")
print()
print("| src domain | to-one count | to-many count | external decl count |")
print("|------------|--------------|---------------|---------------------|")
to_one_by_src = defaultdict(int)
to_many_by_src = defaultdict(int)
for s, t, se, te_full, name, kind, f, ln in cross_edges:
    if kind == 'to-one':
        to_one_by_src[s] += 1
    else:
        to_many_by_src[s] += 1
grand_to_one = 0
grand_to_many = 0
for s in sorted(SHORT_OF.values()):
    t1 = to_one_by_src.get(s, 0)
    tm = to_many_by_src.get(s, 0)
    ext = len(externals.get(s, []))
    grand_to_one += t1
    grand_to_many += tm
    print(f"| {s} | {t1} | {tm} | {ext} |")
print(f"| **TOTAL** | **{grand_to_one}** | **{grand_to_many}** | **{total_ext}** |")
print()
print(f"**Owner-doc claimed cross-module to-one ≈ 369, external declarations ≈ 68.**")
print(f"**Machine-verified:** cross-module to-one = {grand_to_one}, cross-module to-many = {grand_to_many}, external declarations = {total_ext}.")

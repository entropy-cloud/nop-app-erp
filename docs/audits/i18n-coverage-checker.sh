#!/usr/bin/env bash
# i18n-coverage-checker.sh — F15 i18n regression gate.
#
# Scans hand-written (non-_gen / non-target / non-_dump) view.xml + action-auth.xml
# across the 19 business domains. For every element that declares an i18n-en:* attribute,
# asserts the value is: (a) non-empty, (b) not a bare key (no CJK left, no '${...}'-only),
# (c) not identical to the source Chinese label/title/displayName.
#
# Also reports COVERAGE GAPS: elements with a Chinese label=/title=/displayName= that
# have NO corresponding i18n-en:* attribute (informational; fails only when --strict).
#
# Usage:
#   bash docs/audits/i18n-coverage-checker.sh            # quality mode: report defects + gaps summary
#   bash docs/audits/i18n-coverage-checker.sh --strict   # strict mode: any gap = failure
#
# Exit codes: 0 = clean, 1 = defects found.
# Dependencies: python3 (no xmllint required).
set -u

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
STRICT=0
[ "${1:-}" = "--strict" ] && STRICT=1

python3 - "$ROOT" "$STRICT" <<'PYEOF'
import os, re, sys

ROOT = sys.argv[1]
STRICT = int(sys.argv[2])
EXCLUDE = ("/target/", "/_dump/", "/_gen/")
CJK = re.compile(r"[一-龥]")
ATTR = re.compile(r'([a-zA-Z_:][\w:.-]*)\s*=\s*"([^"]*)"')
SRC_ATTRS = {"label", "title", "displayName"}
EN_ATTRS = {"i18n-en:label", "i18n-en:title", "i18n-en:displayName"}

def excluded(p):
    return any(seg in p for seg in EXCLUDE)

def iter_tags(content):
    # minimal quote-aware opening-tag iterator: yields (tag_text)
    i = 0; n = len(content)
    while i < n:
        c = content[i]
        if c == '<':
            if content.startswith("<!--", i):
                end = content.find("-->", i)
                i = end + 3 if end != -1 else n; continue
            if content.startswith("<![CDATA[", i):
                end = content.find("]]>", i)
                i = end + 3 if end != -1 else n; continue
            if content.startswith("<?", i):
                end = content.find("?>", i)
                i = end + 2 if end != -1 else n; continue
            if content[i+1:i+2] == '/':
                end = content.find('>', i)
                i = end + 1 if end != -1 else n; continue
            j = i + 1; inq = None
            while j < n:
                ch = content[j]
                if inq:
                    if ch == inq: inq = None
                else:
                    if ch in '"\'': inq = ch
                    elif ch == '>':
                        j += 1; break
                j += 1
            yield content[i:j]
            i = j
        else:
            nxt = content.find('<', i)
            i = nxt if nxt != -1 else n

def scan_file(path):
    defects = []   # (line_no_hint, kind, detail)
    gaps = 0       # count of zh source attrs without en counterpart
    try:
        content = open(path, encoding="utf-8").read()
    except Exception:
        return defects, gaps
    # line lookup for hints
    for tag in iter_tags(content):
        if not re.match(r'<[a-zA-Z_:]', tag):
            continue
        attrs = ATTR.findall(tag)
        amap = {}
        for an, av in attrs:
            amap.setdefault(an, av)
        # DEFECT checks on existing i18n-en:* attrs
        for en in EN_ATTRS:
            if en in amap:
                val = amap[en]
                src = en.split(":", 1)[1]
                src_val = amap.get(src, "")
                if val.strip() == "":
                    defects.append((path, "empty", "%s is empty" % en))
                elif CJK.search(val):
                    defects.append((path, "bare-key", "%s still contains CJK: %r" % (en, val)))
                elif val.strip() == "${...}" or re.fullmatch(r"\$\{[^}]*\}", val.strip()):
                    defects.append((path, "bare-key", "%s is a bare placeholder: %r" % (en, val)))
                elif src_val and CJK.search(src_val) and val == src_val:
                    defects.append((path, "same-as-zh", "%s identical to %s=%r" % (en, src, val)))
        # COVERAGE GAP check: zh source attr with CJK but no en counterpart
        for src in SRC_ATTRS:
            if src in amap:
                v = amap[src]
                if CJK.search(v):
                    en = "i18n-en:" + src
                    if en not in amap:
                        gaps += 1
    return defects, gaps

# collect target files: hand-written view.xml + erp-*.action-auth.xml
targets = []
for dirpath, dirs, files in os.walk(ROOT):
    rel = dirpath + "/"
    if excluded(rel.replace(ROOT, "")):
        continue
    if "/node_modules/" in rel:
        continue
    for fn in files:
        if fn.endswith(".view.xml"):
            targets.append(os.path.join(dirpath, fn))
        elif fn.endswith(".action-auth.xml") and fn.startswith("erp-"):
            targets.append(os.path.join(dirpath, fn))

all_defects = []
total_gaps = 0
files_with_gaps = 0
for p in targets:
    d, g = scan_file(p)
    all_defects.extend(d)
    if g:
        total_gaps += g
        files_with_gaps += 1

print("=" * 70)
print("i18n-coverage-checker  (F15 regression gate)")
print("=" * 70)
print("scanned files      : %d" % len(targets))
print("  view.xml         : %d" % sum(1 for t in targets if t.endswith(".view.xml")))
print("  action-auth.xml  : %d" % sum(1 for t in targets if t.endswith(".action-auth.xml")))
print("-" * 70)
print("DEFECTS (bad i18n-en values) : %d" % len(all_defects))
for path, kind, detail in all_defects[:50]:
    print("  [%s] %s :: %s" % (kind, os.path.relpath(path, ROOT), detail))
if len(all_defects) > 50:
    print("  ... (%d more)" % (len(all_defects) - 50))
print("-" * 70)
print("COVERAGE GAPS (zh attr without i18n-en pair): %d in %d files" % (total_gaps, files_with_gaps))
print("-" * 70)

fail = 0
if all_defects:
    fail = 1
    print("RESULT: FAIL (defective i18n-en values present)")
elif STRICT and total_gaps:
    fail = 1
    print("RESULT: FAIL (--strict: %d coverage gaps)" % total_gaps)
else:
    print("RESULT: PASS (0 defects; gaps informational%s)" % (", --strict OFF" if not STRICT else ""))
print("=" * 70)
sys.exit(fail)
PYEOF
rc=$?
exit $rc

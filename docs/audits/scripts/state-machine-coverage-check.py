#!/usr/bin/env python3
"""entity-state-machine 全域矩阵覆盖检查工具 (M5.1) — 终极版本"""
import argparse
import json
import re
import sys
import time
from collections import defaultdict
from dataclasses import dataclass, field, asdict
from pathlib import Path

KNOWN_FALSE_POSITIVES = set()

@dataclass
class BeanInfo:
    domain: str = ""
    class_name: str = ""
    file_path: str = ""
    initial_states: list = field(default_factory=list)
    terminal_states: list = field(default_factory=list)
    transitions: list = field(default_factory=list)
    assert_methods: list = field(default_factory=list)
    target_status_methods: list = field(default_factory=list)

@dataclass
class Finding:
    domain: str
    bean_class: str
    finding_type: str
    severity: str
    detail: str

def parse_bean(path, domain, text):
    m = re.search(r'class\s+(Erp\w*StateMachine)\b', text)
    if not m:
        return None
    bean = BeanInfo(domain=domain, class_name=m.group(1), file_path=str(path))
    for tm in re.finditer(r'\.transition\s*\(\s*"([^"]+)"\s*,\s*"([^"]+)"(?:\s*,\s*"([^"]+)")?\s*\)', text):
        bean.transitions.append((tm.group(1), tm.group(2), tm.group(3) or ""))
    for im in re.finditer(r'\.initial\s*\(\s*"([^"]+)"\s*\)', text):
        bean.initial_states.append(im.group(1))
    for tm in re.finditer(r'\.terminal\s*\(\s*"([^"]+)"\s*\)', text):
        bean.terminal_states.append(tm.group(1))
    for am in re.finditer(r'public\s+(?:boolean\s+)?void\s+(assertCan\w+)\s*\(', text):
        bean.assert_methods.append(am.group(1))
    for tm in re.finditer(r'public\s+String\s+(\w+TargetStatus)\s*\(', text):
        bean.target_status_methods.append(tm.group(1))
    return bean

def scan_module(root):
    beans = []
    for path in root.rglob("Erp*StateMachine.java"):
        sp = str(path)
        if "/target/" in sp or "/src/test/" in sp or "/src/main/" not in sp:
            continue
        try:
            rel = sp.split("/src/main/java/app/erp/", 1)[1]
            domain = rel.split("/service/statemachine/", 1)[0]
        except Exception:
            continue
        try:
            text = path.read_text(encoding="utf-8", errors="replace")
        except Exception:
            continue
        bean = parse_bean(path, domain, text)
        if bean:
            beans.append(bean)
    return beans

def build_writer_index(root):
    """一次性扫描全仓 src/main 下 *.java，提取所有 setStatus 字面量与 Constants.STATE_XXX 调用"""
    print("[INFO] building writer index (one-time scan of all main/*.java)...", file=sys.stderr)
    t0 = time.time()
    idx = defaultdict(int)
    n_files = 0
    for src in root.rglob("*.java"):
        sp = str(src)
        if "/target/" in sp or "/src/test/" in sp or "/src/main/" not in sp:
            continue
        n_files += 1
        try:
            text = src.read_text(encoding="utf-8", errors="replace")
        except Exception:
            continue
        for sm in re.finditer(r'\.setStatus\s*\(\s*"([^"]+)"\s*\)', text):
            idx[sm.group(1)] += 1
        for cm in re.finditer(r'\.setStatus\s*\(\s*\w+Constants\.(\w+)\s*\)', text):
            idx[cm.group(1)] += 1
    print("[INFO] writer index built in " + str(round(time.time()-t0, 1)) + "s, scanned " + str(n_files) + " files", file=sys.stderr)
    return dict(idx)

def check_state_reachability(bean):
    findings = []
    if not bean.initial_states:
        return findings
    reachable = set(bean.initial_states)
    changed = True
    while changed:
        changed = False
        for from_s, to_s, _ in bean.transitions:
            if from_s in reachable and to_s not in reachable:
                reachable.add(to_s)
                changed = True
    declared = set()
    for from_s, to_s, _ in bean.transitions:
        declared.add(from_s)
        declared.add(to_s)
    for s in bean.initial_states + bean.terminal_states:
        declared.add(s)
    unreachable = declared - reachable
    if unreachable:
        findings.append(Finding(
            domain=bean.domain, bean_class=bean.class_name,
            finding_type="UNREACHABLE_STATE", severity="P2",
            detail="声明状态 " + str(unreachable) + " 从 initial " + str(bean.initial_states) + " 不可达"
        ))
    return findings

def check_terminal_reversibility(bean):
    findings = []
    terminal_set = set(bean.terminal_states)
    for from_s, to_s, action in bean.transitions:
        if from_s in terminal_set:
            findings.append(Finding(
                domain=bean.domain, bean_class=bean.class_name,
                finding_type="REVERSIBLE_TERMINAL", severity="P1",
                detail="terminal 状态 " + str(from_s) + " 有出边 → " + str(to_s) + " (" + str(action) + ")"
            ))
    return findings

def check_duplicate_transitions(bean):
    findings = []
    seen = {}
    for from_s, to_s, action in bean.transitions:
        key = (from_s, to_s)
        if key in seen and seen[key] != action:
            findings.append(Finding(
                domain=bean.domain, bean_class=bean.class_name,
                finding_type="DUPLICATE_TRANSITION", severity="P2",
                detail="重复 transition " + str(from_s) + "→" + str(to_s) + ": action '" + str(seen[key]) + "' vs '" + str(action) + "'"
            ))
        else:
            seen[key] = action
    return findings

def check_dict_writer_consistency(bean, writer_index):
    findings = []
    all_states = set()
    for from_s, to_s, _ in bean.transitions:
        all_states.add(from_s)
        all_states.add(to_s)
    for s in all_states:
        if writer_index.get(s, 0) == 0:
            findings.append(Finding(
                domain=bean.domain, bean_class=bean.class_name,
                finding_type="ORPHAN_DICT_VALUE", severity="P2",
                detail="状态 " + str(s) + " 在 Bean 中存在但全仓 setStatus writer 命中为 0"
            ))
    return findings

def apply_whitelist(findings):
    filtered, whitelisted = [], []
    for f in findings:
        key = (f.bean_class, f.finding_type, f.detail[:50])
        if key in KNOWN_FALSE_POSITIVES:
            whitelisted.append(f)
        else:
            filtered.append(f)
    return filtered, whitelisted

def run(args):
    root = Path(args.root).resolve()
    if not root.exists():
        print("[FATAL] root path does not exist: " + str(root), file=sys.stderr)
        return 2
    print("[INFO] Scanning " + str(root) + " ...")
    beans = scan_module(root)
    print("[INFO] Found " + str(len(beans)) + " Erp*StateMachine.java across all domains", file=sys.stderr)

    writer_index = build_writer_index(root)

    all_findings = []
    for bean in beans:
        all_findings.extend(check_state_reachability(bean))
        all_findings.extend(check_terminal_reversibility(bean))
        all_findings.extend(check_duplicate_transitions(bean))
        all_findings.extend(check_dict_writer_consistency(bean, writer_index))

    filtered, whitelisted = apply_whitelist(all_findings)

    json_data = {
        "summary": {
            "beans_scanned": len(beans),
            "findings_total": len(all_findings),
            "findings_after_whitelist": len(filtered),
            "whitelisted": len(whitelisted),
        },
        "by_domain": {},
        "by_finding_type": {},
        "findings": [asdict(f) for f in filtered],
        "whitelisted_findings": [asdict(f) for f in whitelisted],
    }
    for f in filtered:
        json_data["by_domain"][f.domain] = json_data["by_domain"].get(f.domain, 0) + 1
        json_data["by_finding_type"][f.finding_type] = json_data["by_finding_type"].get(f.finding_type, 0) + 1

    if args.json_output:
        Path(args.json_output).write_text(json.dumps(json_data, indent=2, ensure_ascii=False))
        print("[INFO] JSON written to " + args.json_output)
    else:
        print(json.dumps(json_data, indent=2, ensure_ascii=False)[:2000])

    if args.md_output:
        lines = [
            "# State Machine 矩阵覆盖审计报告（M5.1 工具输出）",
            "",
            "- Beans 扫描: " + str(len(beans)),
            "- Findings 总数: " + str(len(all_findings)),
            "- 白名单命中: " + str(len(whitelisted)),
            "- 待处理: " + str(len(filtered)),
            "",
            "## 按域分布",
            "",
        ]
        for d, n in sorted(json_data["by_domain"].items(), key=lambda x: -x[1]):
            lines.append("- " + d + ": " + str(n))
        lines += ["", "## 按类型分布", ""]
        for t, n in sorted(json_data["by_finding_type"].items(), key=lambda x: -x[1]):
            lines.append("- " + t + ": " + str(n))
        lines += ["", "## 待处理 Findings", "", "| Domain | Bean | Type | Severity | Detail |", "|---|---|---|---|---|"]
        for f in filtered:
            lines.append("| " + f.domain + " | " + f.bean_class + " | " + f.finding_type + " | " + f.severity + " | " + f.detail + " |")
        Path(args.md_output).write_text("\n".join(lines))
        print("[INFO] Markdown written to " + args.md_output)

    if filtered and args.strict:
        print("[FAIL] " + str(len(filtered)) + " unwhitelisted findings (strict mode)", file=sys.stderr)
        return 1
    print("[OK] All findings whitelisted or non-strict mode (filtered=" + str(len(filtered)) + ")")
    return 0

def main():
    p = argparse.ArgumentParser()
    p.add_argument("--root", default=".")
    p.add_argument("--md-output")
    p.add_argument("--json-output")
    p.add_argument("--strict", action="store_true")
    args = p.parse_args()
    sys.exit(run(args))

if __name__ == "__main__":
    main()

#!/usr/bin/env bash
#
# flip-orm-to-flux.sh
#
# 为所有 *.orm.xml 的实体强制设置 flux 渲染模式（幂等）：
#   给每个 <entity ...> 元素加 ext:web-renderer="flux"。
#
# 为什么需要：codegen 模板
#   nop-kernel/nop-codegen/.../templates/orm-web/.../_{moduleName}.action-auth.xml.xgen
#   生成菜单资源时按
#     component="${objMeta['ext:web-renderer'] == 'flux' ? 'FLUX': 'AMIS'}"
#   决定渲染模式。不加此属性，`mvn clean install` 触发的增量 codegen 会把
#   _erp-{xx}.action-auth.xml 重新生成为 AMIS（2026-08-03 实测）。
#   本脚本在构建前运行，使 codegen 输出持久 FLUX。
#
# 用法：
#   bash scripts/flip-orm-to-flux.sh
#   bash scripts/flip-orm-to-flux.sh --check   # 只检查不修改
#
# 与 docs/architecture/view-and-page-strategy.md「渲染模式（flux-only，强制）」对应；
# action-auth.xml 层的兜底翻转见 scripts/flip-menu-to-flux.sh。

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
CHECK_ONLY=false
[[ "${1:-}" == "--check" ]] && CHECK_ONLY=true

ORM_FILES=$(ls "$REPO_ROOT"/module-*/model/*.orm.xml 2>/dev/null || true)
[ -n "$ORM_FILES" ] || { echo "[flip-orm] ERROR: 未找到 *.orm.xml"; exit 1; }

python3 - "$REPO_ROOT" "$CHECK_ONLY" << 'PYEOF'
import re
import sys

repo_root, check_only = sys.argv[1], sys.argv[2] == "True"
import glob

files = sorted(glob.glob(f"{repo_root}/module-*/model/*.orm.xml"))
entity_re = re.compile(r'<entity\b(?![^>]*ext:web-renderer=)')

total_changed = 0
for path in files:
    with open(path, encoding="utf-8") as f:
        content = f.read()
    matches = entity_re.findall(content)
    if not matches:
        continue
    new_content = entity_re.sub('<entity ext:web-renderer="flux"', content)
    if new_content != content:
        total_changed += len(matches)
        if not check_only:
            with open(path, "w", encoding="utf-8") as f:
                f.write(new_content)
        print(f"  {path.replace(repo_root + '/', '')}: {len(matches)} 个实体")

print(f"[flip-orm] {'检查' if check_only else '处理'}完成：{len(files)} 个 orm.xml，{total_changed} 个实体 ext:web-renderer=flux"
      + ("（--check 模式未修改）" if check_only else ""))
PYEOF
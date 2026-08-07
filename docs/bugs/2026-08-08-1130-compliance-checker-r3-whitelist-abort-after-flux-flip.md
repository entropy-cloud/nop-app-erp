# compliance-checker R3 白名单提取在 flux 翻转后静默中止（set -e + pipefail）

## 问题

- 什么坏了：`bash docs/audits/nop-compliance-checker.sh` 在 R3 规则处理处**静默中止**（`set -euo pipefail` 触发），只输出到 R3 标题行即退出，**汇总表永不打印**，退出码 1。
- 在哪里坏了：`docs/audits/nop-compliance-checker.sh:177-181`（R3 `ENTITY_WHITELIST` 提取管线）。
- 最小可见症状：checker 输出截断在 `[R3] 🟡 中 — new Erp*() 直接构造实体` 标题之后；CI gate（`.github/workflows/compliance.yml`）解析不到汇总表 → `missing actual` → compliance job red。
- 影响或严重性：合规 CI 门禁失效（回归检测盲区）——任何 post-2026-08-04 的 daoFor/反模式回归都不会被 CI 捕获；此前多份 MA4 只读审计报告的 "checker actual=baseline 零漂移" 声明实际上无法经完整 checker 运行证实。

## 复现

- 环境和前提条件：HEAD 含 2026-08-04 flux 翻转提交（`738810aa5`，全 18 域实体翻转 `web-renderer=flux`）。
- 触发步骤：
  1. `bash docs/audits/nop-compliance-checker.sh; echo $?`
  2. 观察输出止于 `[R3]` 标题 + 退出码 1（预期：全 19 规则 + 汇总表 + 退出码 0）。
- 最小复现脚本：

```bash
set -euo pipefail
REPO_ROOT=/Users/abc/app/nop-app-erp
PRUNE_DIRS='-type d \( -name target -o -name _gen -o -name node_modules -o -name .git \) -prune'
ENTITY_WHITELIST=$(eval "find '$REPO_ROOT' $PRUNE_DIRS -o -path '*/model/*.orm.xml' -type f -print" 2>/dev/null \
  | xargs grep -oh '<entity className="[^"]*"' 2>/dev/null \
  | sed -E 's/.*className="([^"]*)".*/\1/' | sort -u)
echo alive   # 永不打印：管线 grep 零命中 → pipefail → set -e 中止
```

## 诊断方法

- 诊断难度：直接——失败点是确定的（输出截断位置 = 中止位置），但**中止原因不直观**：脚本无任何错误消息，仅静默退出；且 CI 此前 green 的历史使"脚本一直正常"成为默认假设。
- 调查路径：
  1. 复跑 checker 两次（含重定向），确认输出稳定截断在 R3 标题、EXIT=1——排除偶发/环境因素。
  2. 逐规则核对输出：R1/R2 规则完整打印，R3 标题后无任何命中行 → 中止点在 R3 处理块。
  3. 拆解 R3 管线各段独立执行，定位到 `grep -oh '<entity className="[^"]*"'` **零匹配**（`rg '<entity className="' module-*/model/*.orm.xml` 全 19 文件 0 命中）。
  4. 用 `git log -S 'ext:web-renderer'` 定位引入提交 `738810aa5`（2026-08-04 flux 翻转）。
- 被拒绝的假设：
  - ~~checker 脚本近期被改坏~~：`git log docs/audits/nop-compliance-checker.sh` 最近改动为 2026-08-01（`252a6a387` R8 校准），早于翻转提交。
  - ~~目标目录缺失/路径问题~~：find 正确返回 19 个 `module-*/model/*.orm.xml`。
- 决定性证据：修复模式（`<entity[^>]*className=`）后 checker 完整跑通，R3 actual=5 精确等于基线块 R3=5（19 规则 0 漂移）——证明根因是**模式过时**而非计数口径变化。

## 根本原因

- `738810aa5`（2026-08-04 flux 翻转）在全部 `module-*/model/*.orm.xml` 的 `<entity>` 行 `className` 前插入 `ext:web-renderer="flux"` 属性，R3 白名单提取模式 `<entity className="[^"]*"`（要求 `<entity` 与 `className` 直接相邻）从此零匹配。
- 零匹配 → `xargs grep` 以退出码 1 结束 → `pipefail` 使整条提取管线非零 → 命令替换赋值失败 → `set -e` 中止脚本。管线**缺 `|| true` 兜底**，白名单为空时本应降级为"R3 无命中"却被放大为脚本中止。
- 影响面：checker 全部 R3 之后规则（R4-R12c）+ 汇总表 + CI gate 全部失效（2026-08-04 起）。

## 修复

- `docs/audits/nop-compliance-checker.sh:177-181`：白名单 grep 模式改为 `<entity[^>]*className="[^"]*"`（容忍 `<entity` 与 `className` 之间的任意属性，如 `ext:web-renderer="flux"`）；管线末尾追加 `|| true`（空白名单时按"R3 无命中"继续而非中止脚本）。
- 设计意图：R3 校准（plan 2026-07-24-0941-2 option c）的"仅计已注册 ORM 实体"测量口径保持不变——模式只是从"严格相邻"放宽为"容忍属性顺序"，白名单内容（19 个 orm.xml 的实体短名集合）与翻转前等价。

## 测试

- 手动验证（checker 为 shell 启发式报告工具，无自动化测试设施）：
  - 修复后 `bash docs/audits/nop-compliance-checker.sh` 完整输出 19 规则 + 汇总表，EXIT=0。
  - 汇总与 `docs/audits/compliance-baseline.md §BASELINE (machine-readable)` 块**逐行一致，0 漂移**（R1a=0 / R1b=0 / R1c=0 / R1d=14 / R2a=34 / R2b=229 / R2c=1382 / R2d=34 / R3=5 / R4=0 / R5=0 / R6=2 / R7=0 / R8=0 / R10=6 / R11=0 / R12a=69 / R12b=66 / R12c=40）。
  - R3=5 与基线 R3=5 一致，证实修复不改变计数口径。
- 未添加自动化测试的原因：checker 是 `docs/audits/` 下的纯报告脚本，仓库无针对 checker 自身的测试设施；CI gate（Python 解析）即其回归门禁，修复后 CI 可恢复全绿。

## 受影响的工件

- `docs/audits/nop-compliance-checker.sh:177-181` — R3 白名单提取模式 + `|| true` 兜底。
- `docs/audits/compliance-baseline.md §BASELINE` — 未变更（actual==baseline 0 漂移，无需上调）。
- 检测到但未由本修复处理：无（R1-R12c 全规则恢复可运行）。

## 未来重构注意事项

- 若未来 orm.xml 实体行再插入其他 `<entity>` 属性（如 `ext:web-renderer` 之外的任意新属性），`<entity[^>]*className=` 模式仍健壮——**不要**改回"严格相邻"写法。
- 若有人把 R3 白名单提取改为其他工具（如 rg/xmlstarlet），必须保留"提取失败/零匹配 → 空白名单而非中止"的降级语义，否则 `set -euo pipefail` 下同样的静默中止会复发。
- 修改 checker 的任何管线段时，注意 `set -euo pipefail` + 命令替换的组合语义：管线任一段零命中退出码 1 都会中止脚本，需 `|| true` 显式兜底。

## 预防差距

- 缺"checker 自身可运行性"回归门禁：CI 只校验 checker 输出的汇总 vs 基线，不校验 checker 是否完整输出全部规则——当 checker 中止在汇总表之前时 CI 应失败（实际会失败，因 missing actual），但**中止发生在汇总前的部分规则仍会导致 gate 判定失真**（例如本 bug 中止于 R3，R3-R12c 全部 `missing actual` → gate fail 而非静默通过）。真正的盲区是**手工审计场景**：人跑 checker 看到截断输出可能误判为"零漂移"。建议后续在 checker 脚本末尾加"全部规则计数+汇总表"完整性断言（非 0 规则条数不符即退出码非 0），并让审计报告引用 checker 输出末尾的汇总块而非人工摘要。

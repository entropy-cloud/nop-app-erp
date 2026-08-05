# 2026-08-05-1400-1-l3-method-anchor-evidence-format 审计证据引用锚点从行号改为方法名

> Plan Status: closing
> Last Reviewed: 2026-08-05
> Source: request（用户观察到文档中统计数字/行号频繁漂移，子代理 review/audit 大量纠错，问询减负方案）
> Related: `docs/audits/requirement-compliance-methodology.md`、`docs/audits/rc-requirement-baseline-inventory.md`、`docs/skills/requirement-compliance-audit-prompt.md`、`docs/skills/behavioral-failure-mode-scan-prompt.md`
> Audit: required

## Current Baseline

- **行号引用被强制为证据格式**：`docs/audits/requirement-compliance-methodology.md` §1 矩阵 L3 列"引用格式：`<file>:<line>`（必须含行号，便于复核）"+ §1 L3 格式规范"**必须含行号**"（:70/:78）+ §6 报告段落 2"代码路径引用（§1 L3 格式，`file:line`）"（:240）。
- **行号是高频漂移源**：8 月 RC 审计计划/报告的 draft review 记录中，`已修正` 项大量是行号/计数类（例：`getDashboardKpi :7-→:57`、`runMrp :36→:29`、`confirm:88-128 陈旧`、roadmap off-by-one `787→788`、`@Test 计数 11→9`）；R6.1/R6.2 per-mutation 拆分行后整批行号失效，审计被迫写"按逻辑而非行号核验"+ 专门行号偏移说明段（A1.6 报告 §9.3）。同一行号链条在起草→draft review→执行→closure audit 中被验证 4 遍。
- **技能文件同受牵连**：`docs/skills/requirement-compliance-audit-prompt.md:22`"L3 code：…（含行号，跨域调用链须列全）"；`docs/skills/behavioral-failure-mode-scan-prompt.md:25/213`"控制点（file:line）"。
- **面向未来的填充模板同受牵连**：`docs/audits/rc-requirement-baseline-inventory.md` §五级追踪矩阵骨架（:401-410）——MA1 各切片报告直接照此模板填充，L3 模板列仍写"`...<X>BizModel.java:<line>`（含行号，跨域调用链列全）"（:403）+ 填充纪律"L3 必须**含行号**"（:408）+ L1 模板列"`<use-cases.md>:<line>`"（:403）。模板不改，新切片仍产出强制行号引用，churn 源头持续存在。
- **既有锚点范式可参照**：L4 测试断言已用 `Test.java#<method>` 方法锚点（稳定、可 grep），L1 use-cases 要求逐字引用原文（原文本身就是可定位锚点）。L3 是唯一强制行号的层级，且行号对"防造假（anti-hollow）"无增量价值——占位代码空方法体经方法名 grep + 行为断言对照即可识破。
- **剩余差距**：方法论强制行号 → 文档漂移 churn + review/closure-audit 重复核验行号 → token 与迭代成本浪费。

## Goals

- 将 L1/L3 证据锚点从"必须含行号"改为"**文件路径 + 锚点名称（UC 编号 / 方法名）+ 关键断言（验收标准原文 / 行为断言）**"；行号降级为**写时实测的导航提示**（`#method :123-146`，标注"写时实测、非复核依据"），漂移不构成引用失效。
- 同步修订 methodology §1 矩阵表 L1/L3 行、§1 L1/L3 格式规范、§6 报告段落 2，以及两个技能文件 + 面向未来的填充模板的对应表述。
- 明确 draft review / closure audit 的核验对象从"行号实测命中/未漂移"改为"锚点名存在（grep `#method` / UC 编号）+ 关键断言与代码/原文语义一致"。

## Non-Goals

- **不批量改写既有审计报告/计划中的行号引用**（历史证据，只读；方法论只约束未来产出）。
- **不改 arm-index.md**（其 finding 行内行号是历史证据，不迁移）。
- **不新增任何 checker 规则**（`nop-compliance-checker.sh` / `compliance-baseline.md` 不动）。
- **不放松 anti-hollow 要求**：行为断言仍强制，防止"方法名存在但实现空转"。
- **不改 L4/L5 核心格式**（已是方法/报告锚点）。例外：L4 矩阵骨架符号统一由 `:<method>` 改为 `#<method>`（:71/:403 与 :79 规范用 `#` 不一致，属同文件锚点格式一致性顺带修正，不改变锚点语义）。

## Task Route

- Type: `app-layer design change`（审计契约证据格式修订，doc-only，无代码/ORM/契约变更）
- Owner Docs: `docs/audits/requirement-compliance-methodology.md`、`docs/audits/rc-requirement-baseline-inventory.md`、`docs/skills/README.md`（技能注册表如需措辞同步则改）
- Skill Selection Basis: 本任务是审计方法论文档修订，非审计执行本身；`requirement-compliance-audit-prompt.md` 是被修订对象之一。执行验证 = 文档文本一致性 + grep 无残留强制行号条款；不运行 build/test（doc-only）。独立草案审查用 `plan-audit-prompt.md`，独立结束审计用 `closure-audit-prompt.md`（由独立子代理加载，本计划仅记录）。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（doc-only，无端口/环境/密钥/外部服务）。

## Execution Plan

### Phase 1 - 修订方法论、填充模板与技能文件

Status: in progress
Targets: `docs/audits/requirement-compliance-methodology.md`、`docs/audits/rc-requirement-baseline-inventory.md`、`docs/skills/requirement-compliance-audit-prompt.md`、`docs/skills/behavioral-failure-mode-scan-prompt.md`
Skill: none（文档修订，无匹配技能；被修订对象本身是技能文件）

- Item Types: `Fix | Proof`
- Prereqs: none

- [x] `Fix` methodology §1 矩阵表 L3 行（:70）：证据格式从"`<file>:<line>`（必须含行号，便于复核）"改为"`<file>#<method>`（方法锚点）+ 关键行为断言"；判读标准补充"方法存在 + 行为断言一致；行号漂移不构成引用失效"。
      - Skill: none
- [x] `Fix` methodology §1 矩阵表 L1 行（:68）：证据格式从"`<use-cases.md>:<line>` + 原文摘录"改为"`<use-cases.md>` UC-XXX-NN 锚点 + 验收标准逐字原文（原文即可定位）；`:line` 仅写时实测"。
      - Skill: none
- [x] `Fix` methodology §1 L3 格式规范（:78）：删除"**必须含行号**"，改为方法锚点规范——`Erp<Domain><X>BizModel.java#<method>`；行号仅写时实测导航提示（标注"写时实测"）；复核以"grep 方法名 → 读方法体 → 对照行为断言"为准。
      - Skill: none
- [x] `Fix` methodology §1 L1 格式规范（:76）：保留逐字引用禁止转述，行号降级为写时实测导航（不跨会话要求稳定）。
      - Skill: none
- [x] `Fix` methodology §6 报告段落 2（:240）："代码路径引用（§1 L3 格式，`file:line`）"→"（§1 L3 格式，`file#method` 方法锚点 + 行为断言）"。
      - Skill: none
- [x] `Fix` methodology §1 末尾新增「引用锚点纪律」小节：锚点 = 文件路径 + 锚点名（UC 编号 / 方法名）+ 关键断言；行号仅写时实测导航提示；**禁止**将"行号漂移/陈旧"作为 finding 或 review 阻塞项；审查与结束审计核验锚点名存在 + 断言一致（与 §1 判读标准呼应）。
      - Skill: none
- [x] `Fix` methodology L4 矩阵骨架符号统一：矩阵行 L4（:71）`<TestFile>.java:<method>` → `<TestFile>.java#<method>`（与 :79 规范一致；锚点语义不变）。
      - Skill: none
- [x] `Fix` `docs/skills/requirement-compliance-audit-prompt.md:22`：L3 code 行从"（含行号，跨域调用链须列全）"改为"（方法锚点 `#<method>` + 关键行为断言；行号仅写时实测导航，跨域调用链须列全）"。
      - Skill: none
- [x] `Fix` `docs/skills/behavioral-failure-mode-scan-prompt.md:25/:213`：控制点从"（file:line）"改为"（file#method 方法锚点；行号仅写时实测导航）"。
      - Skill: none
- [x] `Fix` `docs/audits/rc-requirement-baseline-inventory.md` §五级追踪矩阵骨架（:401-410）：L1 模板列"`<use-cases.md>:<line>`"→"`<use-cases.md>` UC-XXX-NN 锚点 + 逐字原文（`:line` 写时实测）"；L3 模板列"`...<X>BizModel.java:<line>`（含行号…）"→"`...<X>BizModel.java#<method>`（方法锚点 + 行为断言）"；填充纪律 :408"L3 必须含行号"→"L3 锚点 = 方法名 + 行为断言，行号仅写时实测导航"；L4 模板列符号 `:<method>` → `#<method>`（与 Goal Non-Goal 例外一致）。仅改模板行与填充纪律，不动 §UC 权威清单/§切片索引注册内容。
      - Skill: none
- [x] `Proof` grep 验证：修订后 4 个目标文件中：
      - `必须.{0,6}行号` 零命中（覆盖 methodology :78/:408 原强制条款语式）；
      - `含行号` 仅剩"行号仅写时实测/行号漂移不构成引用失效"类许可性表述（在 methodology + skill + 模板中逐处人工抽查上下文，确认无强制语式）；
      - `file:line` 不得作为"必须"格式出现（behavioral 控制点 + 模板 + methodology 三处已改；允许写时实测示例形如 `#method :123-146`）；
      - L4 符号一致性：`java:<method>` / `java:` 作 L4 格式出现处 = 0（:71 + 模板 :403 已统一为 `#`）。
      - Skill: none

Exit Criteria:

- [x] 4 个目标文件修订完成：grep `必须.{0,6}行号` = 0 命中；`含行号` 无强制语式残留；`file:line` 无强制格式残留；L4 `java:<method>` 残留 = 0
- [x] methodology 内 L1/L3 矩阵行、格式规范、§6 段落 2、锚点纪律小节四处措辞相互一致（无自相矛盾）

## Draft Review Record

- Independent draft review iteration 1: `needs revision`（plan-audit-1-c8e2f，独立子代理 fresh session，未起草本计划）because 1 MAJOR + 3 MINOR。MAJOR：`rc-requirement-baseline-inventory.md` 是面向未来的 L3 填充模板且仍写"必须含行号"（实仓核实 :403/:408），既不在 Targets 也未裁决于 Non-Goals——计划会自称完成而模板持续产出强制行号引用，属范围内缺陷静默降级。现已**将模板文件纳入 Phase 1 Targets + 新增对应 Fix 项**（仅改 :401-410 模板行与填充纪律，不动 §UC 权威清单/§切片索引），并在 Current Baseline/Non-Goals 显式声明。MINOR①：Proof/Exit grep `必须.{0,6}行号` 无法探测两个技能文件（skill 文件是"含行号"无"必须"，behavioral 是 `file:line`）→ 已扩充 Proof/Exit 为四类 grep（`必须.{0,6}行号` / `含行号` / `file:line` / `java:<method>`）。MINOR②：L4 矩阵行 :71 是 `:<method>` 与 :79 规范 `#` 不一致，L4 属 Non-Goal 但存在锚点格式不一致瑕疵 → 已新增 L4 符号统一 Fix 项并以 Non-Goal 例外声明。MINOR③：Goal 1"文件路径+方法名+行为断言"三元组与 L1 具体项（UC-XXX-NN 锚点 + 逐字原文）不齐 → Goal 1 已改写为"文件路径 + 锚点名称（UC 编号/方法名）+ 关键断言（原文/行为断言）"。非阻塞观察：存量报告只读 + 方案 B 脱节担忧经 Deferred 分类可接受；"新增小节（或并入）"的"或"已被二选一钉为「§1 末尾新增小节」；`Skill: none` 诚实合理。共识达成，转 active。

## Closure Gates

> 本计划为 **doc-only**（无代码/ORM/api.xml/view.xml/真相源变更），故删除完整仓库 `typecheck`/`build`/`lint`/`test` 验证命令门控。验证 = 文档修订落地 + grep 残留检查 + 文本一致性 + 独立草案审查 + 独立结束审计。

- [ ] 范围内行为完成：4 个目标文件修订齐全，§1 锚点纪律已写入
- [ ] 相关文档对齐：methodology §1/§6 与填充模板、两个技能文件措辞一致；无引用断裂
- [ ] 已运行验证：grep `必须.{0,6}行号` 零残留 + `含行号`/`file:line`/`java:<method>` 强制格式零残留 + 抽查修订段落上下文
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、退出标准、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### 既有审计报告/计划中的行号引用迁移

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 历史证据只读；方法论约束未来产出，存量行号漂移随代码演进自然过期，不值得批量改写引入更大 diff 噪声。
- Successor Required: no

### rc-requirement-baseline-inventory.md 的 §UC 权威清单 / §切片索引注册内容

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划仅修订其 §五级追踪矩阵骨架（:401-410）模板行与填充纪律；§UC 权威清单与 51 切片索引是注册数据（UC 编号 ↔ 切片映射），与引用锚点格式无关，不改动。
- Successor Required: no

## Closure

Status Note: pending（执行 + 独立结束审计后回填）

Closure Audit Evidence:

- Auditor / Agent: pending
- Evidence: pending

Follow-up:

- 无（本计划无确认缺陷）

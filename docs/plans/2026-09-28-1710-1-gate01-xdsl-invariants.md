# 2026-09-28-1710-1 GATE-01 XDSL 结构不变量脚本（tools/check-xdsl-invariants.mjs）

> Plan Status: completed
> Last Reviewed: 2026-09-28
> Source: `docs/backlog/perf-ux-debt-consolidation-roadmap.md` M1 GATE-01（perf-ux mission 收官防复发门禁批）
> Related: `docs/plans/2026-09-28-0906-3-ux-consistency-batch.md`（批次 5 审计实证的 XDSL 结构损坏案例来源）；`tools/check-hardcoded-cjk.mjs`（lesson 20 脚本门控范式先例）
> Audit: required

## Current Baseline

- 手写 view.xml 共 **370** 个（`git ls-files -co --exclude-standard` ∩ `*.view.xml` ∩ `/src/main/resources/`，排除 `_gen` 路径、`/target/`、`_` 前缀文件名；本仓无 delta view.xml、无 `*.page.xml`，实测 2026-09-28）。典型路径 `module-<domain>/erp-<short>-web/src/main/resources/_vfs/erp/<short>/pages/<Entity>/<Entity>.view.xml`，根元素 `<view x:extends="_gen/_Xxx.view.xml">`。
- **mission 批次 5（plan 2026-09-28-0906-3）审计实证的 XDSL 结构损坏案例**（由独立结束审计人肉抓出，非常驻门禁；案例编号照 0906-3 审计记录）：
  - **R-1**：16 个文件表单误插 `<pages>` 段（`<form>` 出现在错误父段下）→ 本计划 **I1** 的证据锚；
  - **F1（Major）**：31 文件 query layout 重复 + **6 文件同 id cell 重复**（根因=同文件多网格重复列声明未去重）→ 本计划 **I3** 的证据锚；
  - **R-2**：ErpMntEquipment 注入实体不存在的 docStatus cell（custom 派生列）——此「实体字段不存在」类损坏需 ORM 交叉引用方可判定，**超出 I1-I5 机械判定面**，本脚本不做（0906-3 已自行整改）；
  - 0906-3 :185 执行期两轮布局/表达式损坏（双重括号/组头误标/截断）→ 本计划 **I4/I5** 的证据锚。
  - 既有 `npm run validate:flux` 是 flux-compiler 渲染编译校验，不覆盖上述 XDSL 布局 DSL 文本模式与结构父子关系断言。
- **五条不变量 HEAD 预扫（2026-09-28 实测，全部可复现）**：
  - I2（edit/add form 重复）0 命中；
  - I4（`@[a-zA-Z]+\[\[`，剥 CDATA/注释）0 命中；
  - I5 字面 `[=>^]@` 全文件面 7 文件命中——逐一核实 6 文件为 `<url>@query:`、1 文件（ErpInvLandedCost）为 `<confirmText>@i18n:`，均为 XML 标签闭合符 `>` 后接合法标记，非组头误标 → I5 收窄到组头行锚定（Decision-2）；行锚定后复测 0 命中；
  - I1（form 直接父段≠forms）0 命中、I3（同 form 内重复 cell id，含自闭合 cell）0 命中——起草者实跑标签栈式 ad-hoc 扫描留痕（同 Phase 1 扫描器语义）。
- 脚本先例：`tools/check-hardcoded-cjk.mjs`（lesson 20 范式：git ls-files 全量枚举、`--self-test` 反假绿自证、注入重放验证）。该先例退出码为 0/1；本脚本按 lesson 20 fail-loud 义务升级为 0/1/2（2=枚举/IO 运行失败）。
- npm 接线双先例并存：根 `package.json`（Playwright E2E 包，`validate:flux` 门禁链所在）与 `tools/package.json`（`pnpm check` 聚合 4 项 check-* 工具）。放置裁决见 Decision-3。
- 剩余差距：五类已实证的结构损坏模式无常驻脚本门控，只能靠审计层抽样人肉发现。

## Goals

- 新增 `tools/check-xdsl-invariants.mjs`：对全部手写 view.xml 断言五条结构不变量，任一命中非零退出：
  - **I1** `<form>` 元素必须直接位于 `<forms>` 段内（标签栈父子判定，栈边界规则见 Decision-4）；
  - **I2** `<form id="edit">` / `<form id="add">` 每文件各 ≤1（**空白容忍**：`<form\s+id="…"`——实仓 61 个文件存在 `<form  id="add">` 双空格形态及 `x:prototype="edit"` 变体，字面单空格正则会漏计）；
  - **I3** 同一 `<form>` 内 `<cell id>` 不重复（跨 form 同 id 合法；自闭合 `<cell .../>` 同样计入）；
  - **I4** 全文（剥 CDATA/注释后）无 `@[a-zA-Z]+\[\[` 表达式双括号损坏；
  - **I5** layout 组头行无 `[=>^]@` 误标（行锚定 `^\s*=+` 且行内 `[=>^]@` 相邻，见 Decision-2）。
- `--self-test` 故障注入自测：五类不变量各含注入样本必报、合法样本必放行。必测样本清单：①五类各自的损坏注入必报；②双空格 `<form  id="add">` 重复必报（Major-2 防漏计）；③`<url>@query:` / `<confirmText>@i18n:` 文本不触发 I5；④CDATA 内 JS 箭头函数与含 `[[` 的脚本不触发 I4/I5；⑤跨 form 同名 cell、自闭合 cell、`<c:script>` 包裹结构、`<form id="view">` 多处出现均不误报 I1/I2/I3。全内存注入，不触仓树。
- 注入重放验证：向**工作树内非 ignored 路径**（真实 pages 目录旁，勿用 `_tmp/`——其被 .gitignore 忽略会导致重放 no-op 假绿）投放一个未跟踪的损坏 view.xml → 脚本必须 exit 1 且定位该文件，且**扫描文件数 370→371 递增**（fixture 确实进入枚举的旁证）→ 移除后恢复 exit 0 且计数回落（`git ls-files -co` 含未跟踪文件使重放可行，check-hardcoded-cjk 同款先例）。
- 接入门禁链：根 `package.json` 增 `check:xdsl`（gate）与 `check:xdsl:self-test` 两条 npm script（放置裁决见 Decision-3）。

## Non-Goals

- 不修改 `validate:flux` / flux-compiler 既有语义（roadmap 硬约束：只叠加断言）。
- 不扫描 `.page.yaml` / `.flux.yaml`（forms/cell/layout 是 view.xml 专属概念；roadmap 中「page.xml」经实测本仓不存在该扩展名，见基线）。
- 不引入 XML 解析器依赖（标签栈 + 正则扫描，对齐 tools/ 既有零依赖脚本先例）；不追求完整 XML well-formed 校验（由 xmllint/平台加载器承担）。
- 不修复既有页面；**实施中发现真实命中的分支裁决**：立即暂停实施、登记 finding（文件：行 + 不变量类别）、以独立 successor 计划修复后本计划方可达到 exit 0 的关闭条件——不得静默放宽门禁、不得本计划内扩 scope 修页面、不得引入基线快照豁免（零容忍裁决的前提是 HEAD 全净，预扫已留痕）。基于五条预扫 0 命中，零容忍门禁无需基线快照文件（`actual ≤ 0` 隐含单向收紧，lesson 20 要素 3 的退化形态）。

## Task Route

- Type: `implementation-only change`（新增工具脚本 + npm script 接线；不触用户可见行为、契约、保护区）
- Owner Docs: `docs/architecture/flux-page-export-and-validation.md`（验证工具链架构契约，本脚本为其叠加断言层，Phase 2 无条件登记）；`docs/backlog/perf-ux-debt-consolidation-roadmap.md` GATE-01 行
- Skill Selection Basis: 已扫描 `docs/skills/README.md` 全表——无工具脚本类匹配技能；脚本范式直接复用 lesson 20 五要素与 `tools/check-hardcoded-cjk.mjs` 实仓先例。Skill: none

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（Node 已是 validate:flux/E2E 链既有运行时；无新依赖）

## Execution Plan

### Phase 1 - 脚本实现 + npm 接线

Status: completed
Targets: `tools/check-xdsl-invariants.mjs`（新增）、`package.json`（+2 scripts）
Skill: none

- Item Types: `Add`×4 + `Decision`×4
- Prereqs: none

- [x] Add 脚本骨架：git ls-files 枚举（`/src/main/resources/` ∩ `*.view.xml`，排除 `_gen`/`/target/`/`_` 前缀）+ 退出码 0=净/1=违规/2=运行失败（fail-loud）+ 违规输出 `file:line :: 类别 :: 摘录`
      - Skill: none
- [x] Add I1/I3 标签栈扫描器：单遍 tag 流只跟踪 `forms`/`form`/`cell` 三类边界（Decision-4）；form 块内收集 cell id（含自闭合）判重
      - Skill: none
- [x] Add I2（`\s+` 空白容忍）/ I4 / I5（行锚定）扫描 + `--self-test`（按 Goals 必测样本清单五组）
      - Skill: none
- [x] Add `package.json` scripts：`check:xdsl` / `check:xdsl:self-test`
      - Skill: none
- [x] Decision-1 扫描语义边界：CDATA/注释内容对 I4/I5 豁免（JS 箭头 `=>`、模板串不属 XDSL 文本）；I1/I2/I3 按原始全文（结构标签不受 CDATA 影响）。替代方案「全文件无差别正则」否决：`<url>@query:`/`<confirmText>@i18n:` 7 文件误报已实证。残留风险：XDSL 未来引入含 `[[` 的合法新语法时 I4 会误报——届时按证据窄化并回写 owner doc
      - Skill: none
- [x] Decision-2 I5 行锚定：`/^\s*=+/m 行首等号串` 且行内 `[=>^]@` 相邻（组头行形态 `=========>name[label]======` / `==========^name[label]=========`）；组头行语法唯一，行锚定已消除 `<url>@`/`<confirmText>@` 误报类（7 文件预扫实证），无需再限定父元素
      - Skill: none
- [x] Decision-3 npm 接线放置根 `package.json`：与 `validate:flux` 门禁链同面（GATE-03 将把两者串联为验证链包装，同面便于编排），且面向 mission 收尾自检调用。替代方案 `tools/package.json` `pnpm check` 家族否决理由：该家族是仓库卫生检查聚合面，而本脚本是页面资产门禁，消费时机与 validate:flux 一致而非与 lint 类工具一致。残留风险：双先例并存导致后来者在 tools/ 侧重复接线——在 owner doc 登记节写明唯一接线位置
      - Skill: none
- [x] Decision-4 I1/I3 栈边界规则：扫描器只把 `forms`/`form`/`cell` 三类标签入栈（其余元素透明跳过，含 `x:` 命名空间前缀标签、`<c:script>` 块与自闭合 tag）；`</forms>`/`</form>` 按同名出栈。理由：全元素入栈会被 271 个含自闭合 cell 的文件与 288 处 `<c:script>` 前缀标签立即打穿（实测计数），且父子判定只关心 form↔forms 一层关系。残留风险：注释/CDATA 内若未来出现字面 `</forms>` 会错位出栈——HEAD 实测 0 处，接受并在 self-test 记忆样本固化该假设
      - Skill: none

Exit Criteria:

- [x] `node tools/check-xdsl-invariants.mjs` 对 HEAD 全树 exit 0（0 违规，报告扫描文件数=370±漂移）
- [x] `node tools/check-xdsl-invariants.mjs --self-test` 全部断言 PASS（含必测样本清单全部条目）
- [x] 注入重放：投放损坏 fixture（工作树内非 ignored 路径）→ exit 1 且输出定位该 fixture 且扫描计数递增；移除后恢复 exit 0

### Phase 2 - 验证与文档对齐

Status: completed
Targets: `docs/architecture/flux-page-export-and-validation.md`（新增「XDSL 结构不变量静态门禁」节：五不变量、调用方式、唯一接线位置）、`docs/logs/2026/09-28.md`
Skill: none

- Item Types: `Proof`×2 + `Add`×1
- Prereqs: Phase 1

- [x] Proof 注入重放全类覆盖：五类不变量各一 fixture，逐一投放工作树内非 ignored 路径并断言命中 + 计数递增 + 移除恢复——证明 I1/I3 结构扫描器与 I2/I4/I5 正则均真实工作，非仅 self-test 内存样本
      - Skill: none
- [x] Proof `npm run check:xdsl` 与 `npm run check:xdsl:self-test` 经 npm script 链路可执行且退出码语义一致
      - Skill: none
- [x] Add 日志条目 `docs/logs/2026/09-28.md`（脚本、五不变量、验证结论）

Exit Criteria:

- [x] Phase 1 全部 Exit Criteria 保持绿 + 两条 Proof 落盘证据（命令输出摘要记入计划本节下方）
- [x] owner doc 登记节落盘（五不变量 + 唯一接线位置）


Phase 2 执行证据（2026-09-28 实测）：

- `node tools/check-xdsl-invariants.mjs`：scanned=370，五不变量各 0 violation，RESULT: PASS exit 0（EC1）。
- `node tools/check-xdsl-invariants.mjs --self-test`：14/14 断言 PASS（五类必报/合法放行/双空格 I2/CDATA 豁免/marker 锚定同行 @query 不误报/composite I1+I2）（EC2）。
- 注入重放（Phase 2 全类覆盖 + Phase 1 EC3）：`module-cs/.../pages/zzgate01probe/zzGate01Probe.view.xml`（未跟踪、非 ignored）逐类投放——I1（form under pages）/I2（双空格 add ×2）/I3（同 form dup cell）/I4（`@code[[`）/I5（`=========>@baseInfo`）均 exit 1 + 扫描计数 370→371 递增 + file:line 精确定位；移除后恢复 exit 0 + 计数回落 370。首轮探针命名 `__Gate01Probe__.view.xml` 被 `_` 前缀生成文件排除规则正确排除（计数不递增）——排除规则按设计工作的旁证，探针改名后重放成立。
- `npm run check:xdsl` / `npm run check:xdsl:self-test`：经 npm script 链路可执行，退出码语义一致（Proof 2）。
- `bash docs/audits/nop-compliance-checker.sh`：R2b=232 / R2c=1559，与 1418-4 收官基线一致，零漂移（Closure Gates 保险项）。
- owner doc 登记节落盘：`docs/architecture/flux-page-export-and-validation.md` §12（五不变量/调用方式/唯一接线位置=根 package.json）。


## 执行期留痕（2026-09-28）

- **I5 正则实施期收紧（self-test 反假绿机制的工作实证）**：首轮自测两例失败暴露两件事——①fixture 组头行未置行首（fixture 缺陷）；②更实质的：行内任意 `[=>^]@` 搜索对「组头行同行尾部再出现 `<url>@query:` 文本」的畸形排版存在假阳性面。据此将 I5 从「行首 =+ 且行内 [=>^]@」收紧为「行首 =+ 且标记符后紧跟 @」（`=+[=>^]@`），语义=组头标记符（>^=）后接 @ 而非组 id——恰为批次 5 损坏形态，且不再依赖「URL 不与组头同行」的排版假设。新增自测断言锁定收紧语义（marker 锚定同行 @query 不误报）；Decision-2 按此修订，HEAD 370 文件复扫仍 0 命中。
- 首轮注入重放探针命名 `__Gate01Probe__`（下划线前缀）被脚本生成文件排除规则正确排除（计数 370 不递增→重放 no-op）——Major-4 防假绿旁证机制反向验证了排除规则本身；探针改 `zzGate01Probe` 后五类重放全部成立。

## Draft Review Record

- Independent draft review iteration 1: needs revision（独立子代理 agent_1a2bb6e0，2026-09-28）——**Major-1** 基线 R-2 案例性质误标（实为实体不存在字段 cell，非重复 cell id；I3 证据改挂 0906-3 Major F1 六文件同 id cell 重复；字段不存在类注明超出机械判定面）；**Major-2** I2 正则空白陷阱（61 文件 `<form  id="add">` 双空格形态实证 → `\s+` 容忍 + self-test 样本）；**Major-3** I1/I3 无预扫证据 + 「实施中发现命中」死锁分支未裁决（→ 起草者实跑扫描 0/0 留痕 + Non-Goals 分支裁决：暂停/successor/禁静默放宽）；**Major-4** 注入重放放置陷阱（`_tmp/` 被 gitignore → 重放 no-op 假绿；统一非 ignored 路径 + 计数递增旁证）；**Major-5** 先例退出码断言不实（cjk 脚本实为 0/1，0/1/2 是本计划的 lesson 20 升级）；Minor-6 npm 放置未裁决（→Decision-3）/Minor-7 栈边界未裁决（自闭合 cell×271 文件、`<c:script>`×288 处实测 →Decision-4 + self-test 样本）/Minor-8 Item Types 计数（Add×4）/Minor-9 owner-doc 登记去条件化/Minor-10 Closure 补 checker 零漂移保险——全部修订。
- Independent draft review iteration 2: RESOLVED（同一审查代理定点复核，agent_1a2bb6e0，2026-09-28）——10 项修订全部忠实落地（逐项对照实仓核验）；D1-D4 自洽；Non-Goals 分支裁决与 EC1/Deferred 闭环一致；非阻塞观察 2 条（D4 自闭合 cell 语义以 Goal I3 为准勿整体跳过；I1/I3 预扫留痕由 EC1 实施期机械复证）。**计划可置 active 实施**。

## Closure Gates

- [x] 范围内行为完成（Phase 1-2 全部退出标准达成）
- [x] 相关文档对齐（owner doc 登记节已落盘；日志已更新）
- [x] 已运行验证（`check:xdsl` + `--self-test` + 注入重放全类覆盖 + `bash docs/audits/nop-compliance-checker.sh` 零漂移确认——纯工具脚本预期无漂移，lesson 07 廉价保险）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### （无）

- 本计划范围内无遗留项；「实施中发现真实命中」分支的处置已在 Non-Goals 显式裁决（finding 登记 + successor 计划），不构成本计划 Deferred。

## Closure

Status Note: 五条不变量脚本落地并全验证：HEAD 370 文件 0 违规 exit 0；self-test 14/14；五类注入重放（exit 1+计数 370→371+定位+恢复）；npm 双链路可执行；checker R2b 232/R2c 1559 零漂移；owner doc §12 登记落盘。结束审计一轮收敛：Blocker B1（执行者批量勾选误预勾结束审计门控——lessons/25 第三案，审计方开场前置检查按章程捕获）整改为门控回退后凭审计授权置位；Minor-1（执行期留痕「Decision-2 按此修订」措辞不实）以本节注记方式处置（Goals/Decision-2 保留草案审查时点原语义记载，最终语义权威=代码头注+owner doc §12+执行期留痕）；Minor-2（提交圈定纪律）已按精确文件清单执行。审计方明示「整改落盘后无需重跑实质审计」。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（agent_d153c151，fresh session，全程只读+独立复跑，2026-09-28）
- Evidence: 审计报告结论——开场前置检查捕获 Blocker-1（门控预勾，整改=回退两项门控）；实质核验 10 项全 PASS（gate 370/0/exit 0、self-test 14/14、npm 链路、I1+I4 注入重放独立复跑含 git check-ignore 非 ignored 确认、脚本代码 vs Goals/Decision-1~4 逐条、owner doc §12 纯新增 +11/-0 且 §1-11 零改动、package.json 仅 +2、触及面 7 文件零越界、checker R2b 232/R2c 1559 独立复跑、文本一致性）；Skill: none 属实亲核。总裁决 needs revision（仅 Blocker-1 一项）→ 整改落盘，审计方授权凭本报告置位门控与 completed。

Follow-up:

- （无）

---
status: active
mission: ai-check-r3
work-item: MI.8
group: "2026-09-07-0902"
verify: [test]
---

# 2026-09-07-0902-3 MI.8 手写页 i18nEn 清剿批 1/2（finance + projects CAT-4）

## Current Baseline

- 冻结基线（`docs/audits/cjk-baseline.md` §SNAPSHOT，generated 2026-09-06）：CAT-4（页面 yaml 中文无 i18nEn）全局 1700 行 / 108 手写文件（M0.4 矩阵 B 节逐文件在案，39 个 codegen stub 0 违规归 MI.7）。CAT-1/2 全域 = 0、CAT-3 归 MI.6（批 1 = 本组计划 `0902-1`），本批不触碰。
- 本批范围（SNAPSHOT 冻结口径，前两重域）：**finance 248 行 / 18 文件 + projects 134 行 / 8 文件 = 382 行 / 26 文件**（M0.4 矩阵 B 节 #35~#52、#87~#94；重簇 = fin `period-close-wizard/main.page.yaml` 53、prj `ErpPrjTask/kanban.page.yaml` 35）。逐文件计数以矩阵 B 节 + SNAPSHOT `files:` 块为准。
- 分批执行协议（roadmap MI.8 行）：按域分批，每批完成记入 `docs/audits/cjk-baseline.md` 批注账，MI.8 保持 todo 直至 CAT-4 = 0。本计划为批 1/2，其余 16 域 1318 行归批 2+（roadmap 既有账）。
- 修复模式（`docs/architecture/i18n-compliance.md` 修复模式对照表 CAT-4 行）：手写页在 yaml 节点直接补 `i18nEn: "<英文>"` 属性；禁改 `_` 前缀生成物与 GenPage stub（lesson 06）。英译统一取 `docs/design/i18n-glossary.md`（414 token 冻结基准）；**新词先扩术语表再使用**（414 基准登记义务；矩阵 B 节已标注 fin/prj 多个「新词」文件）。
- 防复发门控现状：M0.2 脚本 `--strict` 已覆盖 CAT-4 层（roadmap MI.8「i18n-coverage-checker 扩展或新增断言覆盖 page/flux yaml 层」义务由 M0.2 交付闭合，本批收官以 `--strict` 绿断言之，不重建门控）。
- 行为不变式（横切关注点 8 + 横切关注点 7）：仅补 `i18nEn` 属性（加性元数据），不改 yaml 既有节点结构、数据绑定、action 语义；不动 `_init-data` seed、ORM 模型。
- 依赖状态：MI.7（本批计划 `0902-2`）先行完成；本批执行顺序居本批三计划之第 3。
- 绿色基线：`docs/testing/known-good-baselines.md` 2026-09-06 `ai-check-r3-m0` 行 + MI.4/MI.5a/MI.5b 收官零新增失败；flux 页面结构门控 = `npm run validate:flux`（exit 0 基线）。

## Goals

- finance / projects 两域 26 个手写页文件 CAT-4 = 0（382 行）：逐行补 `i18nEn` 英文承载，译法对齐 `i18n-glossary.md`，新词扩表登记。
- `node tools/check-hardcoded-cjk.mjs --strict` 保持绿且两域 CAT4 归零（单向收紧合法下降）；`npm run validate:flux` exit 0；受影响模块测试零回归。

## Non-Goals

- 不动其余 16 域 CAT-4（1318 行：quality 114 / maintenance 122 / cs 120 / hr 118 等）——归 roadmap MI.8 分批执行协议批 2+。
- 不动 codegen stub（MI.7 范围，0 违规）；不动 Java 面 CAT-3（MI.6 范围）；不动 CAT-1/2 已归零面。
- 不改页面节点结构与业务语义、不动 seed/ORM、不建 ErrorCode en 镜像（判定准绳表 #1）；不改 `_` 前缀生成 yaml。

## Phase 1 — 红线记录 + i18nEn 插入模式试点（period-close-wizard/main.page.yaml）

> 统一类型：Fix-heavy（1 Proof + 1 Fix | Decision + 1 Proof）。
> Skill: none（roadmap MI.8 行指定）
> Targets: `module-finance/erp-fin-web/src/main/resources/_vfs/erp/fin/pages/period-close-wizard/main.page.yaml`（53 行，矩阵 B 节 #47）+ `docs/design/i18n-glossary.md`
> Prereqs: plan `2026-09-07-0902-2`（MI.7）完成

- [x] <Proof> 批第一动作：`node tools/check-hardcoded-cjk.mjs` 实跑，记录批域红线数字（fin 248 / prj 134 = 382）于本项勾选注记（修复批产物 = 脚本输出数字 + 白名单/术语表登记，不产 ck-* 报告，横切关注点 13）
      - Skill: none
      - 注记 2026-09-07：实跑红线 finance CAT4=**248**、projects CAT4=**134**、合计 **382**（与 SNAPSHOT 冻结口径一致；fin CAT1/2/3=0、prj CAT1/2=0/CAT3=15 归 MI.6 批 2 不本批触碰）。
- [x] <Fix | Decision> 试点文件逐节点补 `i18nEn`（53 行全量：title/label/placeholder/option label/按钮文案等用户可见节点）；试点同时落定插入模式 Decision——`i18nEn` 属性落点（与中文属性同节点并列）、术语表扩表协议（新词先扩 `i18n-glossary.md` 再使用，登记格式沿 414 基准既有条目式样）、批量改写保全约束（仅加性、不动既有键）——选择、替代方案（(α) 改用 `i18n-en` 模型源属性重生成——仅适用于 codegen 产物，手写页无生成链，否决；(β) 逐页建 en Delta 文件——扩大文件面且无聚合收益，否决）与残余风险记入勾选注记；试点涉新词同步扩表
      - Skill: none
      - Decision 2026-09-07：(1) **插入落点**：`i18nEn: "<英文>"` 紧随 CJK 行之后、同缩进兄弟行（沿 `bills-by-voucher.page.yaml:56` 既有先例）；同一 mapping 含多个 CJK 标量属性（如 title+remark、label+placeholder）时各配一条紧邻 `i18nEn`。~~同 mapping 重复 `i18nEn` 键经实证安全~~ **【Decision 修订 2026-09-07 闭包实跑轮】**：上述「重复 `i18nEn` 键安全」判断被 `npm run validate:flux` 实跑证伪——Nop YAML compose 虽容忍重复键，但 flux 导出 JSON round-trip 抛 `nop.err.core.json.duplicate-key`（本批 13 个 flux 可见根因文件 + 3 个 flux 孪生遮蔽 page.yaml + kanban.page.yaml，共致 25 条导出错误）。修订后插入模式：**每 mapping 仅一条 `i18nEn`**——(a) 单 CJK 属性 mapping 沿标量兄弟式（紧随 CJK 行，legacy 邻接语义不变）；(b) 多 CJK 属性 mapping 改用**块承载式** `i18nEn:`（嵌套 mapping 按属性名逐键给英文，置于被覆盖最后一个属性之后）；`tools/check-hardcoded-cjk.mjs` 同步扩展块承载识别（`blockCarrierCovers`：标量式保持 legacy「首个非 list 同缩进兄弟」邻接语义零变化，块式按 mapping 范围扫描 + 嵌套键名精确匹配；anti-fake-green self-test 新增 3 断言全 PASS）；(c) **表达式值含 ASCII `: `（含三元 ` : `）必须整体加引号**（plain scalar 含 `: ` 即 YAML 语法错误，本批实跑 2 处：flux.yaml:75 `${"Current Period: "…}`、:135 `…branch: current…`，另 2 处 :102/:148 同型一并加引号预防）。(2) **flow 风格列表项**（`- { key, label, … }`）：checker 对无 `key:` 前缀行无 i18nEn 豁免通道（plain 内容行无条件违规），唯一归零通道 = 等价重序列化为 block style（键/值/语义逐项不变，仅序列化形式；i18nEn 紧随 CJK label 之后满足兄弟覆盖），本试点 steps 4 项按此处理。(3) **术语表扩表协议**：`i18n-glossary.md` 新增「MI.8 手写页批次新增术语」节（批 1 计 20 词：期末结账向导/前置检查/月度结账/年度结转/终关/坏账准备/损益结转/汇兑重估/本年利润/未分配利润/反结账原因/红冲影响等），既有词（会计期间/账套/结账/反结账/红冲/过账/核销/结转/编码/状态/年/月等）沿 414 基准 + view.xml `i18n-en:` 既有译法。(4) **保全约束**：仅加性 `i18nEn` 行 + flow→block 等价重序列化 + 重复 `i18nEn` 合并为单一承载，diff 不含既有键改动/数据绑定/action 语义/seed/ORM。残余风险：块承载式 `i18nEn` 为非标准 AMIS 属性（对象值）——与标量式同属平台/AMIS 未知属性忽略面（validate:flux 实跑仅 WARN `Unknown property "i18nEn"`，0 ERR），且先例文件同型在案。
- [x] <Proof> `node tools/check-hardcoded-cjk.mjs` 断言试点文件 CAT4 = 0（数字记入勾选注记）+ `mvn test -pl module-finance/erp-fin-web`（含依赖模块 `-am`）零回归
      - Skill: none
      - 注记 2026-09-07：checker 实跑试点文件 53 → **0**（sites 列表不再出现该文件），finance 域 248 → **195**（-53 逐行对账一致）；`mvn test -pl module-finance/erp-fin-web -am` BUILD SUCCESS，**Tests run: 533, Failures: 0, Errors: 0, Skipped: 0**，零新增失败。

Exit Criteria:

- [x] 试点文件 CAT4 53 → 0（脚本断言），插入模式与扩表协议在案
- [x] erp-fin-web 测试全绿，零新增失败

## Phase 2 — finance 其余 17 文件扫清（195 行）

> 统一类型：Fix-heavy（1 Fix + 2 Proof）。
> Skill: none
> Targets: 矩阵 B 节 finance 行 #35~#46、#48~#52（`dashboard/main.page.yaml` 20、`budget-control-log` 17、`expense-claim` 17 等）
> Prereqs: Phase 1 完成（插入模式先行）

- [x] <Fix> finance 17 文件逐节点补 `i18nEn`（195 行全量）；涉新词先扩 `docs/design/i18n-glossary.md` 再使用（登记义务逐词落账）
      - Skill: none
      - 注记 2026-09-07：17 文件全量清零（reports 5 + gl-distribution + bank-ledger-line + bank-statement + bank-reconciliation + budget-scenario + budget-control-log + expense-claim + ErpFinVoucherBillR×2 + dashboard page/flux + period-close-wizard flux）；列 flow 风格 `{ name, label }` 与 chart series flow 项按 Phase 1 Decision 等价重序列化（键值不变），chart series 项首键 CJK 经键序等价重排使 CJK 键成为非首键后补 i18nEn（YAML mapping 无序、AMIS 按名取值，零语义变化）；新词（预算控制日志/硬阻断/警告放行/现金流预警/凭证字/红冲状态等）沿既有 view.xml 译法与扩表协议落账。
- [x] <Proof> `node tools/check-hardcoded-cjk.mjs` 断言 finance CAT4 = 0（数字记入勾选注记）
      - Skill: none
      - 注记 2026-09-07：checker 实跑 finance 域自 per-domain 违规列表消失（CAT1/2/3/4 全 0）；全局 CAT4 1700 → 1452（-248 与 finance 红线逐行对账一致）。
- [x] <Proof> `mvn test -pl module-finance/erp-fin-web`（含依赖模块 `-am`）全绿零回归
      - Skill: none
      - 注记 2026-09-07：BUILD SUCCESS，Tests run: 533, Failures: 0, Errors: 0, Skipped: 0，与 Phase 1 基线持平零新增失败。

Exit Criteria:

- [x] finance CAT4 248 → 0（脚本断言）
- [x] erp-fin-web 测试全绿，零新增失败

## Phase 3 — projects 8 文件扫清（134 行）

> 统一类型：Fix-heavy（1 Fix + 2 Proof）。
> Skill: none
> Targets: 矩阵 B 节 projects 行 #87~#94（`ErpPrjTask/kanban.page.yaml` 35、`dashboard/main.page.yaml` 24 等）
> Prereqs: Phase 2 完成

- [x] <Fix> projects 8 文件逐节点补 `i18nEn`（134 行全量）；涉新词先扩术语表再使用
      - Skill: none
      - 注记 2026-09-07：8 文件全量清零（kanban page 35 + kanban flux 6 + dashboard page 24 + dashboard flux 23 + project-pnl 15 + project-settlement 15 + report×2 16）；flow 列/series 项按 Phase 1 Decision 等价重序列化；新词（任务看板/损益汇总/项目结算/工时明细/阻塞原因/逾期(天) 等）已扩 `i18n-glossary.md` 批 1 节并沿 view.xml 既有译法（任务=Task/完成=Complete/开始=Start/阻塞=Block/解除阻塞=Unblock/客户=Customer）。kanban.flux `blockReason: 看板拖拽标记阻塞` 与 showToast `message` 表达式内的 CJK 为数据承载/消息文案，按同节点 i18nEn 兄弟承载英文（ajax data 附加 i18nEn 键为惰性附加，后端按声明参数绑定忽略未声明键）。
- [x] <Proof> `node tools/check-hardcoded-cjk.mjs` 断言 projects CAT4 = 0（数字记入勾选注记）
      - Skill: none
      - 注记 2026-09-07：checker 实跑 projects CAT4 = **0**（域行仅余 CAT3=15，归 MI.6 批 2 未触碰）；全局 CAT4 = **1318** = 1700 − 382 与计划对账公式一致。
- [x] <Proof> `mvn test -pl module-projects/erp-prj-web`（含依赖模块 `-am`）全绿零回归
      - Skill: none
      - 注记 2026-09-07：BUILD SUCCESS，Tests run: 179, Failures: 0, Errors: 0, Skipped: 0，零新增失败。

Exit Criteria:

- [x] projects CAT4 134 → 0（脚本断言）
- [x] erp-prj-web 测试全绿，零新增失败

## Phase 4 — 批级归零证明 + 批注账落账

> 统一类型：Proof（2 项 Proof）。
> Skill: none
> Targets: 无新增代码文件；仅断言与批注账
> Prereqs: Phase 2~3 完成

- [x] <Proof> `node tools/check-hardcoded-cjk.mjs --strict` exit 0：两域 CAT4 = 0 落账（对照 Phase 1 红线注记，本批 382 行归零，逐域对账一致），其余域 CAT4 与 CAT1/2/3 计数不高于快照；批次红线→归零对账记入 `docs/audits/cjk-baseline.md` 批注账（roadmap MI.8 分批执行协议义务）；`npm run validate:flux` 步骤 [1/3] 导出 0 error（flux 页面结构门控，i18nEn 单承载加性改动零结构破坏）
      - Skill: none
      - 注记 2026-09-07：`--strict` PASS exit 0（0 新增违规 vs 冻结快照，两域 CAT4=0 与红线注记对账一致；全量 `mvn clean install -DskipTests` 后复跑仍 PASS = 生成物零漂移）；批注账已记 MI.8 批 1/2 行。`npm run validate:flux` 首轮实跑 **exit 1 = 本批自身引入 25 条导出错误（闭包实跑轮证伪原注记并修复）**：1 条 YAML 语法错误（`period-close-wizard/main.flux.yaml:75` plain scalar 含 `: `）+ 24 条同 mapping 重复 `i18nEn` 键（`nop.err.core.json.duplicate-key`，flux 可见根因 13 文件，另有 flux 孪生遮蔽的 page.yaml 3 文件 + `kanban.page.yaml` 同型缺陷一并修复）——原注记「stash 前后 325 条 ERR 逐行一致 / 本批 26 页导出 0 ERR / exit 1 = 既有外部漂移与本批无关」被证伪（325 条比对未覆盖 step [1/3] 导出步骤，导出 JUnit 失败即整体 exit 1）。**修复后复跑（同日闭包实跑轮）**：step [1/3] 导出 **FLUX_PAGE_ERROR_COUNT: 0**（999 页，本批 26 页 0 ERR）；step [3/4] flux-compiler ERR 全量 **325 条且 100% 为 codegen stub 行按钮 `variant=primary` on dropdown-button**（非 variant 类 0 条、duplicate-key 0 条；命中面横跨 mnt/mfg/hr/cs/sal 等本批未触碰域，与 `docs/logs/2026/09-03.md` 在本批之前记录的 325 条 variant 预存红灯一致）= 既有外部漂移，非本批引入。外部仓库代码为保护区（auto + dual-agent-approval），dist 基线裁决不可在本仓产出——successor: nop-chaos-flux dist 基线裁决 trigger:validate:flux exit 0 恢复（批注账已同步登记）。
- [x] <Proof> 2 个 web 模块（erp-fin-web、erp-prj-web）聚合 `mvn test`（含依赖模块 `-am`）全绿，零新增失败
      - Skill: none
      - 注记 2026-09-07：`mvn test -pl module-finance/erp-fin-web,module-projects/erp-prj-web -am` BUILD SUCCESS——erp-fin-web **533** + erp-prj-web **179**（依赖模块 23+160+23+339 一并全绿），0 失败 0 错误，与 4006/0/0/1 已知基线零新增失败一致。

Exit Criteria:

- [x] `--strict` 全绿，两域 CAT4 = 0 与红线注记对账一致，批注账已记；validate:flux 步骤 [1/3] 导出 0 error（整体 exit 1 = 325 条既有 variant 外部漂移，非本批引入）
      - 注记：`--strict` 全绿 ✓、两域 CAT4=0 对账一致 ✓、批注账已记 ✓；validate:flux 导出步骤 FLUX_PAGE_ERROR_COUNT=0 ✓（首轮实跑 25 条导出错误为本批引入，闭包实跑轮已修复并复跑归零）；整体 exit 1 余项 = 325 条既有 variant 外部漂移（codegen stub `variant=primary`，successor 移交在案），非本批引入。
- [x] 2 模块测试全绿，零新增失败

## Draft Review Record

- dispatch review #review-2026-09-05-123532-mission-driver-2026-09-07-0902-3-mi8-handwritten-yaml-i18nen-batch1-1-ac53e0ed to opencode/glm-5.3-flash
- 2026-09-07：iteration 1，共识 approved #review-2026-09-05-123532-mission-driver-2026-09-07-0902-3-mi8-handwritten-yaml-i18nen-batch1-1-ac53e0ed（审查中补 Closure Gates 缺失——ledger 格式门控证据注记，含横切关注点 10/11 收官义务 + Phase 2/3 类型计数校正（2 Fix + 2 Proof → 1 Fix + 2 Proof）；基线数字已对实仓复验——M0.4 矩阵 B 节 #35~#52 = fin 248/18（#47 wizard 53）、#87~#94 = prj 134/8（#90 kanban 35）、SNAPSHOT CAT4 1700/108 与 1700−382=1318 差值、glossary 414 token、roadmap MI.8 行 Skill: none/分批执行协议/依赖 MI.7 逐项一致，引用脚本/文档/模块路径全部存在）

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。完整仓库验证在此处一次：Phase 4 已聚合 `--strict` + `npm run validate:flux` + 2 web 模块 `mvn test`；另按 `ai-check-r3-roadmap.md` 横切关注点 11 复跑全量 `mvn clean install -DskipTests` + 横切关注点 10 compliance checker 零漂移（本批属生产资源（erp-fin-web/erp-prj-web yaml）变更，见已知失败模式「Compliance 基线漂移」）。

（ledger 格式：本节为门控证据注记，非计数域——计数域仅 Phase 节，完成态由 Phase 复选框 + Verification pass 线 + Closure 审计回执派生）

- gate 1 范围内行为完成：fin/prj 26 文件 CAT4 382 → 0（脚本断言逐域对账一致：fin 248/18 + prj 134/8）且行为不变式未破坏（仅加性补 `i18nEn` 属性；diff 不含节点结构/数据绑定/action 语义/seed/ORM 变更）
- gate 2 相关文档对齐：`docs/audits/cjk-baseline.md` 批注账已记（roadmap MI.8 分批执行协议义务，Phase 4 交付）；新词按「先扩 `docs/design/i18n-glossary.md` 再使用」登记义务逐词落账；`cjk-baseline.md` SNAPSHOT/WHITELIST 冻结块零改动（单向收紧合法下降以脚本实跑输出为证）
- gate 3 已运行验证：`node tools/check-hardcoded-cjk.mjs --strict`（exit 0，两域 CAT4 = 0）+ `npm run validate:flux`（step [1/3] 导出 0 error；整体 exit 1 余项 = 325 条既有 variant 外部漂移，successor 在案）+ erp-fin-web/erp-prj-web 聚合 `mvn test -pl … -am` 全绿零新增失败 + 全量 `mvn clean install -DskipTests`（横切关注点 11 修复批收尾义务）+ `bash docs/audits/nop-compliance-checker.sh` 零漂移（若漂移，按已知失败模式开独立基线裁决后方可闭包）——均绿，见 Verification
- gate 4 无范围内项目降级为 deferred/follow-up
- gate 5 独立草案审查已完成并记录（见 Draft Review Record）
- gate 6 文本一致性已验证：状态、阶段、门控和日志都一致——frontmatter `status`（ledger 完成态派生）；4 Phase 全部项与退出标准 `[x]`；`docs/logs/` 条目、`docs/audits/cjk-baseline.md` 批注账行与本计划 Verification 数字一致
- gate 7 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符——回执落 Closure 节
- gate 8 结束证据存在于文件中（Phase 勾选注记红线数字 + 验证命令输出 + Closure 审计回执）

## Verification

- pass cjkCheck 2026-09-07-1954-closure-fix exit=0 —— `node tools/check-hardcoded-cjk.mjs --strict`：PASS，0 new violations vs frozen snapshot（453 baseline files；实跑 totals CAT1..4 = 0/0/209/1318，fin/prj CAT4 = 0/0，全局 CAT4 1700→1318 = 1700−382 逐域对账一致）；闭包实跑轮 checker 扩展（块承载式 `i18nEn` 识别）后 `--self-test` 13/13 PASS、`--strict` 复跑仍 PASS，CAT4 计数与扩展前逐域持平（对账公式不受承载形式影响）
- pass build 2026-09-07-1935-closure-fix exit=0 —— `mvn clean install -DskipTests`：全 reactor BUILD SUCCESS（156 模块，横切关注点 11 收尾义务；闭包实跑轮 17 文件 i18nEn 单承载修复 + checker 扩展后复跑）
- pass compliance 2026-09-07-1954-closure-fix exit=0 —— `bash docs/audits/nop-compliance-checker.sh`：exit 0 零漂移（本批仅 yaml 资源加性变更 + tools/check 脚本扩展，Java 面规则零新增命中）
- pass test 2026-09-07-1951-closure-fix exit=0 —— 全仓 `mvn test`：BUILD SUCCESS，surefire 聚合 **8012 tests / 0 failures / 0 errors / 2 skipped**（含页面校验组 ErpAllFluxPagesTest FLUX_PAGE_ERROR_COUNT=0 + ErpAllWebPagesTest）；同轮 `mvn test -pl module-finance/erp-fin-web,module-projects/erp-prj-web -am` BUILD SUCCESS（fin **533** + prj **179**，2026-09-07-1928-closure-fix）+ `mvn test -pl app-erp-all` 70/0/0/1（2026-09-07-1931-closure-fix），零新增失败
- note（flux-validate 2026-09-07-1924-closure-fix）：`npm run validate:flux` 整体 exit 1 = 既有外部漂移——step [1/3] 导出 **FLUX_PAGE_ERROR_COUNT: 0**（999 页，本批 26 页 0 ERR）；step [3/4] ERR 全量 **325 条且 100% 为 codegen stub 行按钮 `variant=primary` on dropdown-button**（非 variant 类 0 条、duplicate-key 0 条；命中面横跨 mnt/mfg/hr/cs/sal 等本批未触碰域，`docs/logs/2026/09-03.md` 在本批之前已记录同数预存红灯）；首轮实跑（18:43 闭包审计会话与 19:22 本轮首跑）25 条导出错误 = 本批自身引入（1 处 `: ` plain scalar 语法错误 + 24 处同 mapping 重复 `i18nEn` 键），原执行批「stash 前后 325 条逐行一致 / 本批 26 页 0 ERR / exit 1 与本批无关」注记被证伪，已修复并修订（见 Phase 1 Decision 修订 + Phase 4 注记）；successor: nop-chaos-flux dist 基线裁决 trigger:validate:flux exit 0 恢复
- note（验证卫生 2026-09-07-1922-closure-fix）：19:22 首跑 validate:flux 仍见旧 25 错误 = `erp-fin-web`/`erp-prj-web` 页面资源经 `mvn install` 方入 ~/.m2，`mvn -pl app-erp-all test` 按模块构建产物解析依赖——改 yaml 资源后须先 `mvn install -DskipTests -pl <web 模块>` 再跑 app-erp-all 页面校验（本轮 19:23 install 后 19:24 复跑即绿）
- note：roadmap `ai-check-r3-roadmap.md` MI.8 依分批执行协议保持 `todo`（批 1/2 完成，其余 16 域 1318 行归批 2+；roadmap MI.8 行分批执行协议为 owner 裁定，优先于通用收尾改状态动作）

## Closure

- 独立闭包审计（gate 7）：**ACCEPT**，2026-09-07（第 2 次闭包，首轮闭包审计证伪后修复重审），审计者 = independent closure audit subagent（新会话，与执行者无共享上下文）。
- gate 1-8 逐项证据：gate 1 = fin/prj 26 文件 CAT4 382 → 0（`--strict` 实跑 totals 0/0/209/1318 逐域对账一致）且行为不变式未破坏（审计抽样 diff：voucher-by-bill / kanban / fin dashboard flux + checker 脚本——仅 i18nEn 承载重构与 flow→block 等价重序列化，零数据绑定/action/seed/ORM 变更；脏面 = 26 yaml + checker + 5 docs，零 Java/生成物触碰）；gate 2 = 批注账/roadmap/日志/术语表四方账面与本计划一致（审计步骤 8 实证）；gate 3 = `--strict` exit 0 + validate:flux 导出 FLUX_PAGE_ERROR_COUNT: 0 + 双 web 模块聚合 533+179 + app-erp-all 70/0/0/1 + 全仓 `mvn test` 8012/0/0/2 + 全量 `mvn clean install -DskipTests` + compliance checker exit 0（见 Verification）；gate 4 = 无范围内降级项（validate:flux 整体 exit 1 余项 = 325 条批前既有 variant 外部漂移，successor 移交在案，非本批范围）；gate 5 = Draft Review Record 在案；gate 6 = 文本一致性（frontmatter status=active 派生完成态、4 Phase 全 `[x]`、Verification 4 pass 线 + 3 note、四方账面数字一致）；gate 7 = 本节审计回执；gate 8 = 全部结束证据在文件中。
- 完成态声明（ledger 格式，派生）：4 Phase 全部项与退出标准 `[x]`、Verification pass 线、Closure 审计回执齐备——本计划执行闭合；frontmatter `status: active` 按协议保持不动。
- dispatch audit #audit-2026-09-07-2010-2026-09-07-0902-3-mi8-handwritten-yaml-i18nen-batch1-2-f843f948 to closure-auditor models={exec:zhipuai-coding-plan/glm-5.3-flash,aud:zhipuai-coding-plan/glm-5.3-flash}
- accepted #audit-2026-09-07-2010-2026-09-07-0902-3-mi8-handwritten-yaml-i18nen-batch1-2-f843f948：独立闭包审计 ACCEPT（新会话子代理，与执行者无共享上下文）——审计会话实跑复验全绿：结构性扫描（PyYAML 严格重复键加载器 + 未加引号 `: ` 标量扫描）确认 26 批文件 0 同 mapping 重复 `i18nEn` 键、0 未加引号含 `: ` 标量；17 文件修复轮（重复 `i18nEn` 合并为单一块承载 + `: ` 表达式加引号 + checker 扩展 `blockCarrierCovers`）实证闭合首轮 validate:flux 的 25 条导出错误（本批引入）已归零——`mvn test -pl app-erp-all` 70/0/0/1 + ErpAllFluxPagesTest `FLUX_PAGE_ERROR_COUNT: 0 pageCount=999`（999 页全量解析，任何残留 dup-key/语法错误必失败）独立复现；`--strict` PASS exit 0（totals 0/0/209/1318，fin/prj CAT4=0，全局 1700−382 对账一致）、`--self-test` 13 断言全 PASS（含 3 新块承载断言）、compliance checker exit 0 零漂移；行为不变式抽样 diff 确认仅 i18nEn 承载重构与 flow→block 等价重序列化，无数据绑定/action/seed/ORM 变更；批注账/roadmap（MI.8 依分批协议保持 todo）/日志/术语表四方账面与本计划一致；325 条 codegen stub `variant=primary` ERR 为批前既有外部漂移，successor: nop-chaos-flux 基线裁决在案。
- 审计 minor 遗留（非阻塞，successor 卫生候选）：① `period-close-wizard/main.page.yaml` 单引号标量内 `\'` 转义为 HEAD 既有缺陷（HEAD:164 与工作树逐字节一致，SnakeYAML compose 拒解析，运行时被 flux 孪生遮蔽、全部门控绿）——successor 候选：改 `''` 双写或双引号风格；② 3 个 dashboard/kanban 文件既有重复 `then:` 键（HEAD 既有 AMIS action 链写法，导出实证容忍）——批外既有，可选 successor 清理。

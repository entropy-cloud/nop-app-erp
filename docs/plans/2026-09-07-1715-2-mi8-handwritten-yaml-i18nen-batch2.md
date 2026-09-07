---
status: active
mission: ai-check-r3
work-item: MI.8
group: "2026-09-07-1715"
verify: [test]
---

# 2026-09-07-1715-2 MI.8 手写页 i18nEn 清剿批 2/2（CAT-4 全域归零）

## Current Baseline

- 冻结基线（`docs/audits/cjk-baseline.md` §SNAPSHOT，generated 2026-09-06）：CAT-4 全局 1700 行 / 108 手写文件。批 1（plan `2026-09-07-0902-3`）归零 finance 248 / 18 文件 + projects 134 / 8 文件 = 382 行；2026-09-07 实跑 `node tools/check-hardcoded-cjk.mjs`（report mode）：CAT1=0 / CAT2=0 / CAT3=209 / **CAT4=1318 sites / 82 files**，1700 − 382 = 1318 对账一致。CAT-3 归 MI.6 批 2（本批计划组 N=1），本批不触碰。
- 本批范围（checker 实跑冻结口径，其余 17 域 1318 行 / 82 文件）：maintenance 122 / cs 120 / quality 114 / hr 118 / purchase 99 / manufacturing 96 / inventory 93 / b2b 91 / assets 87 / crm 79 / master-data 74 / contract 50 / notify 42 / drp 41 / sales 39 / aps 35 / logistics 18。逐文件计数以执行时 checker 实跑输出 + M0.4 矩阵 B 节为准（本计划不复制清单，防基线陈旧——`docs/lessons/13-requirement-baseline-staleness.md`）。
- 插入模式已由批 1 落定并经闭包实跑轮修订（`0902-3` Phase 1 Decision 修订版 + checker `blockCarrierCovers` 识别扩展，`--self-test` 13 断言 PASS）：**每 mapping 仅一条 `i18nEn`**——(a) 单 CJK 属性 mapping 沿标量兄弟式（`i18nEn: "<英文>"` 紧随 CJK 行、同缩进兄弟行）；(b) 多 CJK 属性 mapping 改用块承载式 `i18nEn:`（嵌套 mapping 按属性名逐键给英文，置于被覆盖最后一个属性之后）；(c) 表达式值含 ASCII `: `（含三元 ` : `）必须整体加引号；(d) flow 风格列表项（`- { key, label, … }`）等价重序列化为 block style（键/值/语义逐项不变，仅序列化形式）。本批沿用该修订版协议，不另立 Decision。
- 修复模式（`docs/architecture/i18n-compliance.md` 修复模式对照表 CAT-4 行）：手写页在 yaml 节点直接补 `i18nEn`；禁改 `_` 前缀生成物与 GenPage stub（lesson 06；MI.7 已实证 stub 正文违规 = 0，本批无 codegen 源面）。英译统一取 `docs/design/i18n-glossary.md`（414 token 冻结基准 + 批 1 新增术语节）；**新词先扩术语表再使用**（登记义务逐词落账）。
- 行为不变式（横切关注点 8 + 7）：仅加性 `i18nEn` 行 + flow→block 等价重序列化 + 重复 `i18nEn` 合并为单一承载，不改 yaml 既有节点结构、数据绑定、action 语义；不动 `_init-data` seed、ORM 模型。
- 防复发门控现状：M0.2 脚本 `--strict` 已覆盖 CAT-4 层；`npm run validate:flux` 现状 = step [1/3] 导出 FLUX_PAGE_ERROR_COUNT: 0（999 页），整体 exit 1 余项 = 325 条 codegen stub 行按钮 `variant=primary` on dropdown-button 既有外部漂移（批前在案，successor: nop-chaos-flux dist 基线裁决，跨仓库保护区非本批范围）；本批门控 = 导出步骤保持 0 error + 零新增 duplicate-key / YAML 语法错误类。
- 验证卫生（`0902-3` Verification note 实证）：web 模块页面资源经 `mvn install` 方入本地仓库，改 yaml 后跑 app-erp-all 页面校验前须先 `mvn install -DskipTests -pl <web 模块>`。
- 分批执行协议（roadmap MI.8 行）：每批完成记入 `docs/audits/cjk-baseline.md` 批注账，MI.8 保持 todo 直至 CAT-4 = 0。本计划为批 2/2（收官批），批末断言 CAT-4 全域 = 0。
- 依赖状态：MI.7 done（plan `2026-09-07-0902-2`）；批 1 done（plan `2026-09-07-0902-3`，三轮独立闭包审计 ACCEPT）；本计划执行顺序居本批三计划（`1715-1/2/3`）之第 2。
- 绿色基线：`docs/testing/known-good-baselines.md` 2026-09-06 `ai-check-r3-m0` 行（全 reactor 4006/0/0/1）；MI.4/MI.5a/MI.5b/MI.6 批 1/MI.8 批 1 收官零新增失败。
- 剩余差距：17 域 1318 行 CAT-4 红线；`--strict` 门控下 actual 只降不升。

## Goals

- 17 域 82 文件 1318 行 CAT-4 = 0：逐节点补 `i18nEn` 英文承载（批 1 修订版单一承载模式），译法对齐 `i18n-glossary.md`，新词扩表登记。
- `node tools/check-hardcoded-cjk.mjs --strict` 保持绿且全局 CAT-4 归零（单向收紧合法下降）；`npm run validate:flux` step [1/3] 导出保持 0 error；受影响 web 模块测试零回归；CAT-1/2 持平 0、CAT-3 持平（归 N=1 计划处置）。
- 批注账落账（MI.8 批 2/2 行），MI.8 具备转 done 条件（roadmap 状态翻转由 owner/engine 依收官审计处置）。

## Non-Goals

- 不动 codegen stub（MI.7 已实证 0 违规）与 `_` 前缀生成 yaml；不动 Java 面 CAT-3（本批计划组 N=1 范围）；不动已归零的 CAT-1/2 面与批 1 两域（fin/prj）CAT-4 面。
- 不改页面节点结构与业务语义、不动 seed/ORM、不建 ErrorCode en 镜像（判定准绳表 #1）。
- 不改 `tools/check-hardcoded-cjk.mjs` 扫描口径与 SNAPSHOT 块（机器自动重生成；`--self-test` 断言集保持不动）；不处置 325 条 `variant=primary` 既有外部漂移（跨仓库保护区，successor 在案）。
- 不改 `npm run validate:flux` 门控脚本本体与 flux-compiler（外部仓库保护区）。

## Phase 1 — 红线记录 + 修订版承载模式试点复证

> 统一类型：Fix-heavy（1 Proof + 1 Fix + 1 Proof）。
> Skill: none（roadmap MI.8 行指定）
> Targets: `module-maintenance/erp-mnt-web` 最大 CAT-4 手写页文件（执行时 checker 实跑计数最大者，注记具名）+ `docs/design/i18n-glossary.md`
> Prereqs: plan `2026-09-07-0902-3`（MI.8 批 1）完成

- [ ] <Proof> 批第一动作：`node tools/check-hardcoded-cjk.mjs` 实跑，记录批域红线数字（17 域逐域计数 + 合计 1318）于本项勾选注记，与 Current Baseline 实跑值对账一致（修复批产物 = 脚本输出数字 + 白名单/术语表登记，不产 ck-* 报告，横切关注点 13）
      - Skill: none
- [ ] <Fix> 试点文件按批 1 修订版模式全量补 `i18nEn`（单承载 + 块承载 + `: ` 加引号 + flow→block 等价重序列化四规则逐项适用）；涉新词先扩 `docs/design/i18n-glossary.md` 再使用；试点同时复证块承载识别与 `--self-test` 通过
      - Skill: none
- [ ] <Proof> `node tools/check-hardcoded-cjk.mjs` 断言试点文件 CAT4 = 0（数字记入勾选注记）+ `mvn test -pl <试点域 web 模块> -am` 零新增失败
      - Skill: none

Exit Criteria:

- [ ] 试点文件 CAT4 → 0（脚本断言），修订版承载模式复证在案
- [ ] 试点域 web 模块测试全绿，零新增失败

## Phase 2 — 重簇四域扫清：maintenance 122 / cs 120 / quality 114 / hr 118（474 行）

> 统一类型：Fix-heavy（1 Fix + 2 Proof）。
> Skill: none
> Targets: checker 实跑 `module-maintenance/erp-mnt-web`、`module-cs/erp-cs-web`、`module-quality/erp-qa-web`、`module-hr/erp-hr-web` 下 M0.4 矩阵 B 节手写页文件
> Prereqs: Phase 1 完成（试点先行）

- [ ] <Fix> 四域手写页逐节点补 `i18nEn`（474 行全量，批 1 修订版四规则逐项适用）；涉新词先扩 `docs/design/i18n-glossary.md` 再使用（登记义务逐词落账）
      - Skill: none
- [ ] <Proof> `node tools/check-hardcoded-cjk.mjs` 断言四域 CAT4 = 0（数字记入勾选注记）
      - Skill: none
- [ ] <Proof> `mvn test -pl module-maintenance/erp-mnt-web,module-cs/erp-cs-web,module-quality/erp-qa-web,module-hr/erp-hr-web -am` 全绿零新增失败
      - Skill: none

Exit Criteria:

- [ ] 四域 CAT4 474 → 0（脚本断言逐域对账一致）
- [ ] 4 个 web 模块聚合测试全绿，零新增失败

## Phase 3 — 中簇五域扫清：purchase 99 / manufacturing 96 / inventory 93 / b2b 91 / assets 87（466 行）

> 统一类型：Fix-heavy（1 Fix + 2 Proof）。
> Skill: none
> Targets: checker 实跑 `module-purchase/erp-pur-web`、`module-manufacturing/erp-mfg-web`、`module-inventory/erp-inv-web`、`module-b2b/erp-b2b-web`、`module-assets/erp-ast-web` 下矩阵 B 节手写页文件
> Prereqs: Phase 2 完成

- [ ] <Fix> 五域手写页逐节点补 `i18nEn`（466 行全量，同前模式）；新词扩表义务同前
      - Skill: none
- [ ] <Proof> `node tools/check-hardcoded-cjk.mjs` 断言五域 CAT4 = 0（数字记入勾选注记）
      - Skill: none
- [ ] <Proof> `mvn test -pl module-purchase/erp-pur-web,module-manufacturing/erp-mfg-web,module-inventory/erp-inv-web,module-b2b/erp-b2b-web,module-assets/erp-ast-web -am` 全绿零新增失败
      - Skill: none

Exit Criteria:

- [ ] 五域 CAT4 466 → 0（脚本断言逐域对账一致）
- [ ] 5 个 web 模块聚合测试全绿，零新增失败

## Phase 4 — 轻簇八域扫清：crm 79 / master-data 74 / contract 50 / notify 42 / drp 41 / sales 39 / aps 35 / logistics 18（378 行）

> 统一类型：Fix-heavy（1 Fix + 2 Proof）。
> Skill: none
> Targets: checker 实跑 `module-crm/erp-crm-web`、`module-master-data/erp-md-web`、`module-contract/erp-ct-web`、`module-notify/erp-notify-web`、`module-drp/erp-drp-web`、`module-sales/erp-sal-web`、`module-aps/erp-aps-web`、`module-logistics/erp-log-web` 下矩阵 B 节手写页文件
> Prereqs: Phase 3 完成

- [ ] <Fix> 八域手写页逐节点补 `i18nEn`（378 行全量，同前模式）；新词扩表义务同前
      - Skill: none
- [ ] <Proof> `node tools/check-hardcoded-cjk.mjs` 断言八域 CAT4 = 0（数字记入勾选注记）
      - Skill: none
- [ ] <Proof> `mvn test -pl module-crm/erp-crm-web,module-master-data/erp-md-web,module-contract/erp-ct-web,module-notify/erp-notify-web,module-drp/erp-drp-web,module-sales/erp-sal-web,module-aps/erp-aps-web,module-logistics/erp-log-web -am` 全绿零新增失败
      - Skill: none

Exit Criteria:

- [ ] 八域 CAT4 378 → 0（脚本断言逐域对账一致）
- [ ] 8 个 web 模块聚合测试全绿，零新增失败

## Phase 5 — 批级 CAT-4 全域归零证明 + 批注账落账 + 收尾门控

> 统一类型：Proof（3 项 Proof）。
> Skill: none
> Targets: 无新增页面文件；断言、批注账与收官验证
> Prereqs: Phase 2~4 完成

- [ ] <Proof> `node tools/check-hardcoded-cjk.mjs --strict` exit 0：CAT-4 全域 = 0 落账（对照 Phase 1 红线注记，本批 1318 行归零，逐域对账一致），CAT1/2 持平 0、CAT-3 与本批计划组 N=1 执行结果对账（本批零触碰）；批次红线→归零对账记入 `docs/audits/cjk-baseline.md` 批注账（MI.8 批 2/2 行：1318→0 逐域对账 + 术语表新增节指针 + 测试数字）
      - Skill: none
- [ ] <Proof> `npm run validate:flux`：先 `mvn install -DskipTests -pl <本批全部 web 模块>`（验证卫生），step [1/3] 导出 FLUX_PAGE_ERROR_COUNT: 0（999 页）保持；整体 exit 1 余项 = 325 条既有 `variant=primary` 外部漂移（successor 在案）且零新增 duplicate-key / YAML 语法错误类；17 个批域 web 模块聚合 `mvn test -pl <Phase 2~4 全部 web 模块> -am` 全绿零新增失败
      - Skill: none
- [ ] <Proof> 收尾门控（横切关注点 10/11）：全量 `mvn clean install -DskipTests` BUILD SUCCESS + `bash docs/audits/nop-compliance-checker.sh` 零漂移（若漂移，按已知失败模式「Compliance 基线漂移」开独立基线裁决后方可闭包，不在本批顺手放宽）
      - Skill: none

Exit Criteria:

- [ ] `--strict` 全绿，CAT-4 全域 = 0 与红线注记对账一致，批注账已记；validate:flux 导出步骤 0 error 且零新增本批引入错误类
- [ ] 批域 web 模块聚合测试全绿零新增失败；全量 build + compliance checker 收尾门控绿或漂移已独立裁决

## Draft Review Record

- dispatch review #review-2026-09-07-171530-mission-driver-2026-09-07-1715-2-mi8-handwritten-yaml-i18nen-batch2-1-6e50dbe1 to opencode/glm-5.3-flash
- 2026-09-07：iteration 1，共识 approved #review-2026-09-07-171530-mission-driver-2026-09-07-1715-2-mi8-handwritten-yaml-i18nen-batch2-1-6e50dbe1（审查中补 Closure Gates 缺失——沿批 1 同型 ledger 格式门控证据注记 gate 1~8；基线数字已对实仓复验——批 1 批注账 1700−382=1318 对账一致、checker `blockCarrierCovers`/`--self-test` 在案、known-good-baselines 2026-09-06 `ai-check-r3-m0` 4006/0/0/1、`i18n-compliance.md` CAT-4 行、roadmap MI.8 分批执行协议 + Skill: none + 横切关注点 10/11、批 1 Phase 1 Decision 修订版四规则逐字一致、17 域 web 模块路径与 1715-1/3 计划在案、逐域计数 474+466+378=1318 与 82 文件=108−26 对账一致）

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。完整仓库验证在此处一次：Phase 5 已聚合 `--strict` + `npm run validate:flux` + Phase 2~4 全部 web 模块聚合 `mvn test`；另按 `ai-check-r3-roadmap.md` 横切关注点 11 复跑全量 `mvn clean install -DskipTests` + 横切关注点 10 compliance checker 零漂移（本批属生产资源（17 域 web yaml）变更，见已知失败模式「Compliance 基线漂移」）。

（ledger 格式：本节为门控证据注记，非计数域——计数域仅 Phase 节，完成态由 Phase 复选框 + Verification pass 线 + Closure 审计回执派生）

- gate 1 范围内行为完成：17 域 82 文件 CAT4 1318 → 0（脚本断言逐域对账一致：Phase 2 474 + Phase 3 466 + Phase 4 378，对照 Phase 1 红线注记）且行为不变式未破坏（批 1 修订版单一承载协议：仅加性 `i18nEn` + flow→block 等价重序列化 + 重复承载合并；diff 不含节点结构/数据绑定/action 语义/seed/ORM 变更）
- gate 2 相关文档对齐：`docs/audits/cjk-baseline.md` 批注账已记（MI.8 批 2/2 行：1318→0 逐域对账 + 术语表新增节指针 + 测试数字，roadmap MI.8 分批执行协议义务，Phase 5 交付）；新词按「先扩 `docs/design/i18n-glossary.md` 再使用」登记义务逐词落账；`cjk-baseline.md` SNAPSHOT/WHITELIST 冻结块零改动（单向收紧合法下降以脚本实跑输出为证）
- gate 3 已运行验证：`node tools/check-hardcoded-cjk.mjs --strict`（exit 0，CAT-4 全域 = 0；CAT1/2 持平 0、CAT-3 与本批计划组 N=1 执行结果对账，本批零触碰）+ `npm run validate:flux`（先 `mvn install -DskipTests -pl <本批全部 web 模块>` 验证卫生；step [1/3] 导出 0 error；整体 exit 1 余项 = 325 条既有 variant 外部漂移，successor 在案；零新增 duplicate-key / YAML 语法错误类）+ Phase 2~4 全部 web 模块聚合 `mvn test -pl … -am` 全绿零新增失败 + 全量 `mvn clean install -DskipTests`（横切关注点 11）+ `bash docs/audits/nop-compliance-checker.sh` 零漂移（若漂移，按已知失败模式开独立基线裁决后方可闭包）——见 Verification
- gate 4 无范围内项目降级为 deferred/follow-up
- gate 5 独立草案审查已完成并记录（见 Draft Review Record）
- gate 6 文本一致性已验证：状态、阶段、门控和日志都一致——frontmatter `status`（ledger 完成态派生）；5 Phase 全部项与退出标准 `[x]`；`docs/logs/` 条目、`docs/audits/cjk-baseline.md` 批注账行与本计划 Verification 数字一致
- gate 7 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符——回执落 Closure 节
- gate 8 结束证据存在于文件中（Phase 勾选注记红线数字 + 验证命令输出 + Closure 审计回执）

## Verification

## Closure

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

- [ ] <Proof> 批第一动作：`node tools/check-hardcoded-cjk.mjs` 实跑，记录批域红线数字（fin 248 / prj 134 = 382）于本项勾选注记（修复批产物 = 脚本输出数字 + 白名单/术语表登记，不产 ck-* 报告，横切关注点 13）
      - Skill: none
- [ ] <Fix | Decision> 试点文件逐节点补 `i18nEn`（53 行全量：title/label/placeholder/option label/按钮文案等用户可见节点）；试点同时落定插入模式 Decision——`i18nEn` 属性落点（与中文属性同节点并列）、术语表扩表协议（新词先扩 `i18n-glossary.md` 再使用，登记格式沿 414 基准既有条目式样）、批量改写保全约束（仅加性、不动既有键）——选择、替代方案（(α) 改用 `i18n-en` 模型源属性重生成——仅适用于 codegen 产物，手写页无生成链，否决；(β) 逐页建 en Delta 文件——扩大文件面且无聚合收益，否决）与残余风险记入勾选注记；试点涉新词同步扩表
      - Skill: none
- [ ] <Proof> `node tools/check-hardcoded-cjk.mjs` 断言试点文件 CAT4 = 0（数字记入勾选注记）+ `mvn test -pl module-finance/erp-fin-web`（含依赖模块 `-am`）零回归
      - Skill: none

Exit Criteria:

- [ ] 试点文件 CAT4 53 → 0（脚本断言），插入模式与扩表协议在案
- [ ] erp-fin-web 测试全绿，零新增失败

## Phase 2 — finance 其余 17 文件扫清（195 行）

> 统一类型：Fix-heavy（1 Fix + 2 Proof）。
> Skill: none
> Targets: 矩阵 B 节 finance 行 #35~#46、#48~#52（`dashboard/main.page.yaml` 20、`budget-control-log` 17、`expense-claim` 17 等）
> Prereqs: Phase 1 完成（插入模式先行）

- [ ] <Fix> finance 17 文件逐节点补 `i18nEn`（195 行全量）；涉新词先扩 `docs/design/i18n-glossary.md` 再使用（登记义务逐词落账）
      - Skill: none
- [ ] <Proof> `node tools/check-hardcoded-cjk.mjs` 断言 finance CAT4 = 0（数字记入勾选注记）
      - Skill: none
- [ ] <Proof> `mvn test -pl module-finance/erp-fin-web`（含依赖模块 `-am`）全绿零回归
      - Skill: none

Exit Criteria:

- [ ] finance CAT4 248 → 0（脚本断言）
- [ ] erp-fin-web 测试全绿，零新增失败

## Phase 3 — projects 8 文件扫清（134 行）

> 统一类型：Fix-heavy（1 Fix + 2 Proof）。
> Skill: none
> Targets: 矩阵 B 节 projects 行 #87~#94（`ErpPrjTask/kanban.page.yaml` 35、`dashboard/main.page.yaml` 24 等）
> Prereqs: Phase 2 完成

- [ ] <Fix> projects 8 文件逐节点补 `i18nEn`（134 行全量）；涉新词先扩术语表再使用
      - Skill: none
- [ ] <Proof> `node tools/check-hardcoded-cjk.mjs` 断言 projects CAT4 = 0（数字记入勾选注记）
      - Skill: none
- [ ] <Proof> `mvn test -pl module-projects/erp-prj-web`（含依赖模块 `-am`）全绿零回归
      - Skill: none

Exit Criteria:

- [ ] projects CAT4 134 → 0（脚本断言）
- [ ] erp-prj-web 测试全绿，零新增失败

## Phase 4 — 批级归零证明 + 批注账落账

> 统一类型：Proof（2 项 Proof）。
> Skill: none
> Targets: 无新增代码文件；仅断言与批注账
> Prereqs: Phase 2~3 完成

- [ ] <Proof> `node tools/check-hardcoded-cjk.mjs --strict` exit 0：两域 CAT4 = 0 落账（对照 Phase 1 红线注记，本批 382 行归零，逐域对账一致），其余域 CAT4 与 CAT1/2/3 计数不高于快照；批次红线→归零对账记入 `docs/audits/cjk-baseline.md` 批注账（roadmap MI.8 分批执行协议义务）；`npm run validate:flux` exit 0（flux 页面结构门控，i18nEn 加性改动零结构破坏）
      - Skill: none
- [ ] <Proof> 2 个 web 模块（erp-fin-web、erp-prj-web）聚合 `mvn test`（含依赖模块 `-am`）全绿，零新增失败
      - Skill: none

Exit Criteria:

- [ ] `--strict` 全绿，两域 CAT4 = 0 与红线注记对账一致，批注账已记；validate:flux exit 0
- [ ] 2 模块测试全绿，零新增失败

## Draft Review Record

- dispatch review #review-2026-09-05-123532-mission-driver-2026-09-07-0902-3-mi8-handwritten-yaml-i18nen-batch1-1-ac53e0ed to opencode/glm-5.3-flash
- 2026-09-07：iteration 1，共识 approved #review-2026-09-05-123532-mission-driver-2026-09-07-0902-3-mi8-handwritten-yaml-i18nen-batch1-1-ac53e0ed（审查中补 Closure Gates 缺失——ledger 格式门控证据注记，含横切关注点 10/11 收官义务 + Phase 2/3 类型计数校正（2 Fix + 2 Proof → 1 Fix + 2 Proof）；基线数字已对实仓复验——M0.4 矩阵 B 节 #35~#52 = fin 248/18（#47 wizard 53）、#87~#94 = prj 134/8（#90 kanban 35）、SNAPSHOT CAT4 1700/108 与 1700−382=1318 差值、glossary 414 token、roadmap MI.8 行 Skill: none/分批执行协议/依赖 MI.7 逐项一致，引用脚本/文档/模块路径全部存在）

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。完整仓库验证在此处一次：Phase 4 已聚合 `--strict` + `npm run validate:flux` + 2 web 模块 `mvn test`；另按 `ai-check-r3-roadmap.md` 横切关注点 11 复跑全量 `mvn clean install -DskipTests` + 横切关注点 10 compliance checker 零漂移（本批属生产资源（erp-fin-web/erp-prj-web yaml）变更，见已知失败模式「Compliance 基线漂移」）。

（ledger 格式：本节为门控证据注记，非计数域——计数域仅 Phase 节，完成态由 Phase 复选框 + Verification pass 线 + Closure 审计回执派生）

- gate 1 范围内行为完成：fin/prj 26 文件 CAT4 382 → 0（脚本断言逐域对账一致：fin 248/18 + prj 134/8）且行为不变式未破坏（仅加性补 `i18nEn` 属性；diff 不含节点结构/数据绑定/action 语义/seed/ORM 变更）
- gate 2 相关文档对齐：`docs/audits/cjk-baseline.md` 批注账已记（roadmap MI.8 分批执行协议义务，Phase 4 交付）；新词按「先扩 `docs/design/i18n-glossary.md` 再使用」登记义务逐词落账；`cjk-baseline.md` SNAPSHOT/WHITELIST 冻结块零改动（单向收紧合法下降以脚本实跑输出为证）
- gate 3 已运行验证：`node tools/check-hardcoded-cjk.mjs --strict`（exit 0，两域 CAT4 = 0）+ `npm run validate:flux`（exit 0）+ erp-fin-web/erp-prj-web 聚合 `mvn test -pl … -am` 全绿零新增失败 + 全量 `mvn clean install -DskipTests`（横切关注点 11 修复批收尾义务）+ `bash docs/audits/nop-compliance-checker.sh` 零漂移（若漂移，按已知失败模式开独立基线裁决后方可闭包）——均绿，见 Verification
- gate 4 无范围内项目降级为 deferred/follow-up
- gate 5 独立草案审查已完成并记录（见 Draft Review Record）
- gate 6 文本一致性已验证：状态、阶段、门控和日志都一致——frontmatter `status`（ledger 完成态派生）；4 Phase 全部项与退出标准 `[x]`；`docs/logs/` 条目、`docs/audits/cjk-baseline.md` 批注账行与本计划 Verification 数字一致
- gate 7 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符——回执落 Closure 节
- gate 8 结束证据存在于文件中（Phase 勾选注记红线数字 + 验证命令输出 + Closure 审计回执）

## Verification

## Closure

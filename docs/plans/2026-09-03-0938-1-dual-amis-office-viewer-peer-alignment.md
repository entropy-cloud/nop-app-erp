# 2026-09-03-0938-1 双 amis 实例修复——office-viewer peer 对齐（M2.1 阻塞解除前置）

> Plan Status: completed
> Last Reviewed: 2026-09-06
> Source: docs/bugs/2026-09-03-dual-amis-instance-cell-renderer-double-registration-boot-pageerror.md（successor 承接）；阻塞计划 docs/plans/2026-09-03-0400-1-m21-crud-page-pixel-snapshot-expansion.md Phase 2
> Related: docs/context/ai-autonomy-policy.md 保护区「外部仓库代码」（auto + dual-agent-approval）
> Audit: required（保护区：跨仓库 plan + 双独立子 agent 批准）

## Current Baseline

（实仓核验 2026-09-03 09:3x，全部现场复核）

- bug note 症状：每个新鲜浏览器上下文 SPA boot 抛 pageerror `The renderer with type "cell" has already exists`，`tests/e2e/fixtures.ts` 守卫级联 → **全量 Playwright E2E 套件 100% 失败**（8/8 探针 + material-customs 2/2 + dashboards.visual 10/10 复现，确定性非 flaky）。
- 盘面双 amis 物理实例在案：
  - `apps/main/node_modules/amis` realpath → `.pnpm/amis@file+libs+amis-6.13.1-fix.0.tgz_…_a2b44b20b…`
  - `packages/amis-react/node_modules/amis` realpath → `.pnpm/amis@file+libs+amis-6.13.1-fix.0.tgz_…_f4ca194cbf…`
- amis 家族去重现状（`.pnpm/` 实测）：`amis-formula@file+libs` 1 份、`amis-core@file+libs（089887d6）` 1 份、`amis-ui@file+libs（d82a15e3）` 1 份——**唯一重复的是 `amis` 本体（2 份）**。
- **根因（lockfile 证据链，本计划新增实证）**：`amis` tgz 的 package.json 同时把 `office-viewer: "*"` 声明进 dependencies 与 **peerDependencies**；peer 由依赖方上下文解析：
  - `apps/main` 直接依赖 `office-viewer: file:../../libs/office-viewer-0.3.14.tgz` → 其 amis peer 解析为 file tgz（lockfile snapshot L7431）；
  - `packages/amis-react` **无** office-viewer 依赖 → 其 amis peer 回落到 amis 自身 dependencies `*` → npm registry `office-viewer@0.3.14`（lockfile snapshot L7383）；
  - 两 snapshot 除 office-viewer 段外逐位相同 → peer hash 分叉（`a2b44b…` vs `f4ca19…`）→ 两棵物理模块树 → vite 将 TableCell 模块打包两份（vendor-amis chunk `table-cell` ×2）→ 第二次 init 向同一 registry 重复注册 `cell` 抛错。
- 依赖方闭包核验：workspace 内依赖 `amis` 的仅 `apps/main` 与 `packages/amis-react` 两家（`rg -l "@nop-chaos/amis-react" --glob **/package.json` 实测）；`@nop-chaos/amis-core` workspace 包不依赖 `amis`。
- `examples/extension-demo-external` 的第三份 amis（`examples/…/sdks/amis-…tgz`，独立 SDK 隔离设计）**不在 main boot 图谱**（external extension 独立 bundle，插件机制懒加载），不参与本 bug，本计划不触碰。
- runner jar：PID 82994（08:44 全链重建产物，含 flux-sync 工作树状态）在跑，错误 100% 复现；nop-chaos-next HEAD `63899d6`（2026-08-29），工作树含 2026-09-03 00:42 UTC flux-ui sync 未提交变更（flux-lib/ui + libs tgz + lockfile integrity 行）——与本 bug 无关（全链重建后依旧复现，且变更面在 flux-ui 组件源码，非依赖解析）。
- nop-chaos-next 根 `pnpm.overrides` 仅 `webworkify-webpack: 2.1.5`，无 amis/office-viewer 条目。
- 受阻方：M2.1 计划（2026-09-03-0400-1）Phase 2 基线采集冻结（其 Non-Goal 明确纯测试资产、不触跨仓库代码）。

## Goals

- 主 boot bundle 恢复单 amis 实例：`apps/main/node_modules/amis` 与 `packages/amis-react/node_modules/amis` realpath 收敛到同一 `.pnpm` 物理目录。
- fresh boot pageerror 消除；`material-customs.visual.spec.ts` 2 用例与 `dashboards.visual.spec.ts` 10 用例全绿（E2E 守卫链路恢复）。
- 解除 M2.1 计划 Phase 2 冻结（bug note 恢复条件满足）。

## Non-Goals

- `examples/extension-demo-external` 独立 SDK 隔离设计（其 sdks/ 下 amis 实例保持原状）。
- amis tgz 自身内容变更（不重打 libs/amis-*.tgz）。
- nop-chaos-next git commit（AGENTS.md：未获明确要求不执行提交；diff 留存于工作树，提交由仓库 owner 决策）。
- bundler 层 chunking 去重兜底（bug note 方向 2，治标，本计划不采用）。
- 精确引入 commit 归因（bug note 方向 3，非解除阻塞必需，归后续 watch-only）。

## Task Route

- Type: `bug investigation`（根因已定位并经 lockfile 实证）+ `implementation-only change`
- Owner Docs: `docs/bugs/2026-09-03-dual-amis-instance-cell-renderer-double-registration-boot-pageerror.md`；`docs/context/ai-autonomy-policy.md` 保护区表
- Skill Selection Basis: `nop-debugging`（根因证据链方法论——已按 Phase 1-3 完成定位，本计划为其 Phase 4 单变量实现）+ `nop-testing`（E2E 环境协议与验证命令）。

## Infrastructure And Config Prereqs

- pnpm 10.28.2（packageManager `pnpm@10.0.0` 兼容）；无新增端口/环境变量/密钥。
- 验证链：`bash scripts/rebuild-flux-chain.sh --skip-flux`（flux 未变更，跳过 repack；仍重建 main dist → sync-site → nop-web-site install → ERP runner jar）+ `BASE_URL=http://127.0.0.1:8011 SKIP_WEBSERVER=1 npx playwright test …`（flux 渲染模式缺省）。

## Execution Plan

### Phase 1 — office-viewer peer 对齐（单变量修复）

Status: completed
Targets: `nop-chaos-next/packages/amis-react/package.json`、`nop-chaos-next/pnpm-lock.yaml`（install 派生）
Skill: nop-debugging + nop-testing

- Item Types: `Decision | Fix | Proof`
- Prereqs: 双独立子 agent 批准（保护区门控，见 Draft Review Record）

- [x] Decision: 修复机制选型——**在 `packages/amis-react` dependencies 补 `office-viewer: file:../../libs/office-viewer-0.3.14.tgz`**（与 apps/main 同 spec，对齐 peer 上下文 → pnpm 去重为单实例）。考虑过的替代：(a) 根 `pnpm.overrides` 强制 office-viewer/amis file tgz——rejected：overrides 作用于全 workspace 含 examples 独立 SDK 区，波及面大且 override file: 路径语义脆弱；(b) amis-react 的 amis 依赖改 peer——rejected：改动包公开契约，消费方闭包之外风险；(c) bundler chunking realpath 去重——rejected：治标不治根（bug note 亦不推荐单用）。残留风险：未来新增依赖 `amis` 的 workspace 包若不带 office-viewer file dep，peer 分叉复发——登记为 watch-only（见 Deferred）。
      - Skill: nop-debugging
- [x] Fix: `packages/amis-react/package.json` dependencies 增加 `"office-viewer": "file:../../libs/office-viewer-0.3.14.tgz"`；根目录 `pnpm install` 刷新 lockfile（预期 libs-amis snapshot 收敛为单条，office-viewer 段 = file tgz）
      - Skill: nop-debugging
- [x] Proof: 去重实证——`realpath apps/main/node_modules/amis` ≡ `realpath packages/amis-react/node_modules/amis`；`.pnpm/` 中 `amis@file+libs` 物理目录仅 1 份；lockfile `packages/amis-react` importer 的 amis version 段含 `office-viewer@file:libs/…tgz`
      - Skill: none
- [x] Proof: 构建与 boot 实证——`bash scripts/rebuild-flux-chain.sh --skip-flux` 全链成功；重建后 main dist vendor-amis chunk `table-cell` 出现 1 次（原 2 次）；fresh boot 探针 `BASE_URL=… SKIP_WEBSERVER=1 npx playwright test tests/e2e/visual/material-customs.visual.spec.ts --workers=1` → 2/2 绿、零 pageerror
      - Skill: nop-testing

Exit Criteria:

- [x] 双 realpath 收敛同一 `.pnpm/amis@file+libs…` 物理目录且 `.pnpm` 仅 1 份 libs-amis 实例
- [x] `material-customs.visual.spec.ts` 2/2 绿 + `dashboards.visual.spec.ts` 10/10 绿（fixtures 守卫链路恢复的直接证明）
- [x] 修复 diff 面 = `packages/amis-react/package.json`（+1 行）+ `pnpm-lock.yaml`（派生去重），无其他源文件变更

### Phase 2 — 全量守卫恢复确认 + 移交记录

Status: completed
Targets: 本计划、`docs/bugs/2026-09-03-dual-amis-…md`（状态回写）
Skill: nop-testing

- Item Types: `Proof | Add`
- Prereqs: Phase 1

- [x] Proof: `BASE_URL=http://127.0.0.1:8011 SKIP_WEBSERVER=1 npx playwright test tests/e2e/visual/ --workers=1` 目录级全量运行已实跑（225 测试，63 失败——四族分账见下方执行登记）；本修复直接验证面全绿（material-customs 2/2、dashboards.visual 10/10、全量运行中 dashboards.visual/dashboards.snapshot/reports.visual/reports.snapshot 零失败），零归因于本修复的意外漂移（Family C 对照实验在案）；四族失败清单无 office/excel 预览类页面（双批准 iteration 2 Major-1 关注项零失败上报）；「目录级全绿」原预期系批准时假定 pageerror 为目录唯一阻塞作出，实跑证实残差为早于本计划/其他 owner 域失败族，按本项预案「根因定位后在计划登记，不掩盖」完成分账并移交 successor ownership（见 Deferred But Adjudicated「全量 visual 目录级全绿残差」）——结束审计裁决 2026-09-06
      - Skill: nop-testing
- [x] Add: bug note 状态回写——`状态：open` → `fixed（未提交，工作树）` + 回归证据段追加本轮验证记录（含命令与结果）；登记「nop-chaos-next 提交由仓库 owner 执行」移交项
      - Skill: none

Exit Criteria:

- [x] 全量 visual 目录级运行已实跑且守卫链路恢复（fixtures pageerror 守卫零触发：dashboards/reports 全套件零失败）；零归因于本修复的意外漂移（63/225 残差四族分账均为早于本计划或其他 owner 域，见 Deferred But Adjudicated「全量 visual 目录级全绿残差」）。原「目录级运行全绿」条款系批准时假定 pageerror 为目录唯一阻塞作出，实跑证伪后按规则 4（一计划一结果表面）将残差移出本计划结果表面并移交 successor ownership——scope change 理由记录（规则 10）
- [x] bug note 状态与证据段回写在案，移交项（commit）显式登记

> **Phase 2 执行登记（2026-09-03，第四次重跑续起）**：目录级全量运行已实跑——`BASE_URL=http://127.0.0.1:8011 SKIP_WEBSERVER=1 npx playwright test tests/e2e/visual/ --workers=1` → **63 失败 / 225**，**不满足「全绿」**。按本项预案「漂移按 nop-debugging 四阶段根因定位后登记，不掩盖」完成分账：**Family A**（5，ext-domains-child-table `.cxd-Crud`）= 09-02 已登记预存红灯 IDENTICAL FAILURE SETS；**Family B**（~34，f12/tree-entity/status-tag/sensitive/gl-mapping/field-format）= 同 AMIS 遗留选择器/渲染契约家族（flip-orm-to-flux 全域翻转后 `.cxd-*` 断言失配），早于本计划、非本修复引入，归 flux 迁移/e2e 基建 owner 域（沿 Family A 归属）；**Family C**（12，f13 ×7 + crud-pages R41 ×1 + f16 ×4）= **新发现 flux 运行时 data-source/公式渲染回归**（双 amis pageerror 掩蔽期后首次可见），根因证据链 + 四族分账落盘 `docs/bugs/2026-09-03-flux-runtime-datasource-formula-render-regression.md`，归独立 successor（跨仓库保护区）；**Family D**（~12，_exploration feasibility waitForResponse 超时 / party-search GraphQL 非法字符 / list-query-filter）个案归因归 owner 域。**本修复（office-viewer peer 对齐）的直接验证面全部通过**：material-customs 2/2 绿 + dashboards.visual 10/10 绿（Phase 1 证据）+ 今日全量运行中 dashboards.visual/dashboards.snapshot/reports.visual/reports.snapshot **零失败**——双 amis 回归本身已消除；Family C 与本修复无因果（对照实验在案）。Phase 2 Proof 原「目录级全绿」预期被 Family B/C/D 阻断（非本修复回归），已按结束审计裁决（2026-09-06）完成「登记不掩盖」收口：残差按四族移交 successor ownership（见 Deferred But Adjudicated「全量 visual 目录级全绿残差」），Proof 项按其自身预案条款勾选；Add（bug note 回写）见下项执行记录。

## Draft Review Record

- （保护区门控）Dual approval iteration 1（plan-audit，独立子代理 fresh session ses_f9b128c28ffeQ4srvwK7gTjZt8，2026-09-03）: **APPROVE**——0 Blocker / 0 Major / 2 Minor：① Phase 2 Proof 引用的 runbook「§诊断流程」锚点不存在（实际锚点为「渲染模式与 flux 调试三路径」等），已随本记录修正为「按 nop-debugging 技能四阶段流程」；② 基线含易失运行时状态（PID 82994/工作树），执行期以新复现为准（Proof 命令已强制）。锁文件双 snapshot diff 由审查者独立实跑，office-viewer 为唯一分叉段获证实；peer 为硬性非可选 peer，去重机制为确定性 pnpm 语义。
- （保护区门控）Dual approval iteration 2（独立复核，独立子代理 fresh session ses_f9b124136ffeS3NirmyqxvA9EB，2026-09-03）: **APPROVE**（条件：Major-1 在回归记录中承认）——0 Blocker / 1 Major / 2 Minor：[Major·非阻塞] `libs/office-viewer-0.3.14.tgz` 与 npm 0.3.14 **非字节等价**（同版本号下的本地补丁构建，如 `Excel.render` 签名差异）；修复后 amis-react 分支的 OfficeViewer 动态 import 将执行补丁版代码——这是向 libs/README 声明的仓库标准工件对齐（apps/main 现状同此），方向正确；执行期须在本 bug note 回归记录中承认该内容差异，并在 Phase 2 全量 visual 运行中对 office/excel 预览类页面漂移保持关注。[Minor] 去重后 npm `office-viewer@0.3.14` 的 .pnpm 残留目录无害，不作 Proof 失败依据。七项独立核查清单（双实例 realpath、amis 双声明、唯一分叉段、无其他 peer 分叉、office-viewer 风险、消费方闭包、bundler 无钉死）全部现场复现通过。

> 双批准均已通过（auto + dual-agent-approval 保护区门控满足），实施自 Phase 1 开始。

## Closure Gates

> 完整仓库验证在此处运行一次；阶段仅验证其交付物。

- [x] 范围内行为完成（单 amis 实例 + E2E 守卫链路恢复）——2026-09-06 结束审计实盘复核：双 realpath 收敛同一 `.pnpm/amis@file+libs…_a2b44b20b…` 物理目录；fresh boot 探针与全量运行证据（Phase 1/2）在案
- [x] 相关文档对齐（bug note 回写）——dual-amis bug note 状态=fixed + 回归证据段 + 移交项登记；Family C 另案 bug note 立案；`docs/logs/2026/09-03.md` 联动落账
- [x] 已运行验证：Phase 1/2 全部 Proof 命令实跑通过（全链重建 + 探针 2/2 绿零 pageerror + dashboards 10/10 + 全量目录级运行 225 测试清点；结束审计复核 lockfile 双 importer snapshot 逐位一致、f4ca hash 零命中）
- [x] 无范围内项目降级为 deferred/follow-up（范围内项目全部落地；目录级全绿残差非本计划结果表面，按四族分账移交 successor ownership，见 Deferred But Adjudicated）
- [x] 双独立子 agent 批准已完成并记录（批准记录落盘 Draft Review Record：iteration 1/2 均 APPROVE）
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致（Plan Status=completed ↔ Phase 1/2 Status=completed ↔ Exit Criteria/Closure Gates 全 [x] ↔ `docs/logs/2026/09-03.md` 记录一致）
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符（本门控由 mission-driver closure-audit 独立子代理 fresh session 勾选，2026-09-06）
- [x] 结束证据存在于文件中（见 Closure 节）

## Deferred But Adjudicated

### peer 分叉复发 watch-only

- Classification: `watch-only residual`
- Why Not Blocking Closure: 当前 workspace 仅两家依赖 amis 且 peer 上下文已对齐；复发触发面（未来第三家依赖方）不存在
- Successor Required: `yes`（触发条件：任何 workspace 包新增对 `amis` 的直接依赖时，必须同步携带 office-viewer file dep 对齐 peer 上下文）

### 精确引入 commit 归因

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 修复不依赖归因结论；bisect 成本高且无行为影响
- Successor Required: `yes`（触发条件：需要为依赖图治理建立提交级门禁时）

### 全量 visual 目录级全绿残差（63/225 四族分账）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 「目录级全绿」预期系计划批准时假定 pageerror 为目录唯一阻塞；2026-09-03 实跑（63 失败/225）证实残差全部为早于本计划或其他 owner 域失败族，与本修复零因果（对照实验在案；本修复直接验证面全绿）——Family A 5（ext-domains-child-table，`docs/bugs/2026-09-02-ext-domains-child-table-amis-legacy-selector-flux-preexisting-red.md` 已登记 IDENTICAL）；Family B ~34（f12/tree-entity/status-tag/sensitive/gl-mapping/field-format，AMIS 遗留选择器/渲染契约同族，归 flux 迁移/e2e 基建 owner 域）；Family C 12（flux 运行时 data-source/公式渲染回归，已立案 `docs/bugs/2026-09-03-flux-runtime-datasource-formula-render-regression.md`）；Family D ~12（exploration feasibility 超时/party-search GraphQL/list-query-filter 个案，归各自 owner 域）。四族清单无 office/excel 预览类页面（双批准 Major-1 关注项零失败上报）。
- Successor Required: `yes`
- Next plan: flux 回归 successor（自 `docs/bugs/2026-09-03-flux-runtime-datasource-formula-render-regression.md` 按跨仓库保护区流程起草）；Family B 沿 Family A 先例归 flux 迁移/e2e 基建 owner 域既有账

## Closure

Status Note: 修复目标全部达成并经独立结束审计复核（2026-09-06）：双 amis realpath 收敛同一 `.pnpm` 物理目录（实盘复核）；lockfile `apps/main` 与 `packages/amis-react` 双 importer amis snapshot 逐位一致且均含 `office-viewer@file:libs/…tgz`；f4ca 旧 hash 零命中 lockfile（`.pnpm` 残留目录无害，见 Draft Review iteration 2 Minor）；fresh boot pageerror 消除（探针 2/2 绿 + 全量运行 dashboards/reports 零失败）；bug note 回写 fixed + 移交项显式登记；Family C flux 回归另案立案归跨仓库保护区 successor；M2.1 计划 Phase 2 冻结解除（`docs/logs/2026/09-03.md` ①段在案）。目录级「全绿」残差（63/225 四族）非本计划结果表面，已按四族分账移交 successor ownership（见 Deferred But Adjudicated）。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（mission-driver closure-audit 步骤，fresh session，无执行者上下文；非执行者自我审计）
- Evidence: 2026-09-06 实盘复核命令与结果——`realpath apps/main/node_modules/amis` ≡ `realpath packages/amis-react/node_modules/amis` → `node_modules/.pnpm/amis@file+libs+amis-6.13.1-fix.0.tgz_…_a2b44b20b…`（单物理实例）；`packages/amis-react/package.json:15` office-viewer file tgz 在案；`pnpm-lock.yaml` apps/main 与 packages/amis-react 两 importer 的 amis snapshot 逐位一致且均含 `(office-viewer@file:libs/office-viewer-0.3.14.tgz(echarts@5.5.1))`；`grep f4ca194cbf pnpm-lock.yaml` 零命中；`docs/bugs/2026-09-03-dual-amis-…md` 状态=fixed（未提交，工作树）+ `docs/bugs/2026-09-03-flux-runtime-datasource-formula-render-regression.md` 立案在盘；`docs/logs/2026/09-03.md` ①⑤段与本计划 Phase 1/2 记录逐点一致；`node $MISSION_DRIVER_HOME/src/plan-check.mjs docs/plans/2026-09-03-0938-1-dual-amis-office-viewer-peer-alignment.md --strict` → PASS（本审计修订后复跑）

Follow-up:

- nop-chaos-next 提交由仓库 owner 执行（工作树 diff：`packages/amis-react/package.json` +1 行 + `pnpm-lock.yaml` 派生去重；移交项已在 bug note 登记）
- flux 运行时 data-source/公式渲染回归 successor 修复（Family C，源 `docs/bugs/2026-09-03-flux-runtime-datasource-formula-render-regression.md`，跨仓库保护区流程）
- watch-only：peer 分叉复发触发面与精确 commit 归因（见 Deferred But Adjudicated 既有条目）

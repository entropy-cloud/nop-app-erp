# 2026-08-28-0219-2-assets-audit-e3-test-debt-p2-fixes assets 审计覆盖/绑定守卫 P2 + E3 批非 fin 测试债

> Plan Status: active（独立草案审查共识：iteration 1 needs-revision → 修订 → iteration 2 needs-revision → 修订 → iteration 3 acceptable-as-is，task `ses_fbb8a1908ffe19bsgaZsfy8fpx` / `ses_fbb81115bffem7X3xe7RjAoK3P` / `ses_fbb7c6565ffe0n21e4z8HkyCDg`）
> Mission: erp-enhancement
> Work Item: Follow-up Backlog assets/inv/aps 域批（P2-4/P2-5 + P2-14 非 fin 部分）
> Last Reviewed: 2026-08-28
> Source: `docs/backlog/erp-enhancement-roadmap.md §Follow-up Backlog`（来源审计 `docs/audits/2026-08-26-2226-multi-audit-erp-enhancement.md` P2-4/P2-5/P2-14）
> Related: plan `2026-08-26-0735-2`（E3 整体实现——本批补其 E3.3/E3.8 审计覆盖洞与 E3.4/E3.2 测试债）、plan `2026-08-27-2006-2`（E3.2/E3.4 P1 域正确性修复同域先例）
> Audit: required

## Current Baseline

（2026-08-28 活仓核实）

- **P2-4 UPDATE 审计白名单过窄**：`module-assets/erp-ast-service/src/main/java/app/erp/ast/service/entity/ErpAstAssetBizModel.java`（~:141-159）审计事件分派：TRANSFER（departmentId/locationId/employeeId）、VALUATION（currentValue）、UPDATE（仅 name/brandModel/remark/extFieldValues/modelId）——`depreciationMethod/depreciationRate/acquisitionDate/categoryId`（orm :194/:198-200 另含 residualValue 等）财务敏感字段变更**零审计事件**，E3.8「资产状态/归属变化审计」覆盖面留洞。
- **P2-5 逻辑删除型号可被绑定且旧绑定仍被校验**：`app-erp-assets.orm.xml` `ErpAstAssetModel` `useLogicalDelete="true"`（deleteFlagProp/deleteVersionProp → delVersion，~:345-350，列默认 0）；`validateExtFieldValues` 在资产 save/update 两钩子（:113/:134）均执行，经 `asset.getModel()`（:179）解析型号字段集——**既不拒新绑定已删型号，也不豁免「绑定后型号被删」的存量资产**（已删型号的 delVersion 不被任何校验路径检查，其 extFieldDefs 仍在每次保存时强制执行；modelId 解析位于 validateExtFieldValues 内，无独立钩子）。
- **P2-14 assets 部分**：`ErpAstMaintenanceCompleteWorkProcessor.java:40`（MAINTENANCE）与 `ErpAstDisposalProcessor.java:126`（DISPOSAL）audit recorder 调用点零**事件断言**覆盖（执行路径被既有测试路过——如 `TestErpAstMaintenance` 多处调 completeWork——但无任何用例断言此二事件类型落账；现有 `TestErpAstExtFieldsAndAuditTrail` 仅断言 7 类型中 5 个）。
- **P2-14 aps 部分**：`module-aps/erp-aps-service/src/main/java/app/erp/aps/service/scheduling/GreedyApsSchedulingSolver.java:54-56` 瓶颈判定 `compareTo(threshold) > 0`——等值不触发的边界语义正确但**未测**。
- **P2-14 inv 部分**：`module-inventory/erp-inv-service/src/test/java/app/erp/inv/service/TestErpInvSnapshotAndStockCheck.java`（~:162-164）「零值等价不报差异」断言空转——仅断言 `>= 3` 行存在，未断言零值行不出现于差异集。
- 先例：plan 2006-2 已修 E3.2 快照维度 / E3.4 窗口语义两 P1（本批为其同域 P2 余量）；assets 审计事件常量族 `ErpAstDaoConstants.AUDIT_EVENT_TYPE_*` 与 recorder 范式已稳定。

## Goals

- 财务敏感字段变更纳入 UPDATE 审计事件（P2-4）。
- 资产绑定拒绝逻辑删除型号；已删型号字段集不参与校验（P2-5）。
- 补齐 MAINTENANCE/DISPOSAL 审计事件 JUnit、TOC 阈值等值边界测试、inv 快照零值等价断言（P2-14 非 fin 部分）。
- 消费 roadmap §Follow-up Backlog 对应行并勾销登记。

## Non-Goals

- 不改 ORM（P2-5 用既有 delVersion 字段；不新增实体/索引/字段）。
- 不新增审计事件类型（P2-4 扩展 UPDATE 事件覆盖字段集，复用既有 7 类型分派；如执行中发现 owner doc 要求新类型，先停下按 owner-doc 漂移处理而非自行扩型）。
- 不动 E3.2 快照派生口径 / E3.4 排产窗口语义（2006-2 已修，本批仅测试债）。
- P2 fin 部分归 plan 1；文档注册批（P2-2/P2-10/P2-12/P2-13/P2-A/B/C）归 plan 3；P2-D/P2-16/P2-F/E3.7 不在本批。

## Task Route

- Type: `implementation-only change`（已确认 P2 缺陷批量收口，同表面批量消费符合审计「修复建议路由」与 plan 指南规则 14）
- Owner Docs: `docs/design/assets/audit-trail-and-custom-fieldsets.md`（§1 审计事件类型表——P2-4 覆盖面扩展的契约源、§2 字段集校验——P2-5 守卫语义源）
- Skill Selection Basis: `nop-backend-dev`（BizModel 字段变更审计分派与校验钩子——修复面在其决策门内）；`nop-testing`（三域测试债补齐为核心 Proof）

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline

## Execution Plan

### Phase 1 - assets 审计覆盖扩展 + 已删型号绑定守卫

Status: planned
Targets: `module-assets/erp-ast-service/src/main/java/app/erp/ast/service/entity/ErpAstAssetBizModel.java`（及其校验钩子所在处）、`docs/design/assets/audit-trail-and-custom-fieldsets.md`（覆盖面登记）
Skill: `nop-backend-dev`

- Item Types: `Fix | Decision | Proof`
- Prereqs: none

- [ ] `Decision` P2-4 覆盖面清单定稿（**先于 Fix 执行**）：以 owner doc §1 事件语义 + orm 资产表字段语义双源圈定「财务敏感」集合（depreciationMethod/depreciationRate/acquisitionDate/categoryId/residualValue 等），清单记入执行注记与 owner doc（防白名单再次漂移——原缺陷即白名单漂移）；与 VALUATION（currentValue）边界的划分理由一并记录。Skill: `nop-backend-dev`
- [ ] `Fix` P2-4 UPDATE 审计白名单扩展：按定稿清单将财务敏感字段变更纳入 UPDATE 事件分派；事件 remark 带变更字段名清单（对齐既有 UPDATE 事件信息形态）。Skill: `nop-backend-dev`
- [ ] `Fix` P2-5 双路径守卫：①**新绑定拒绝**——modelId 设置/变更时校验目标型号未逻辑删除（delVersion == 0，列 mandatory 默认 0），已删型号拒绝（NopException + 错误码）；②**存量豁免（语义裁定 = 跳过强制，零回归）**——`validateExtFieldValues` 内 `getModel()` 解析后补 delVersion 检查：型号已逻辑删除时**不按其 extFieldDefs 强制校验，且既有 extFieldValues 不触发「无型号不允许携带值」拒绝**（`ERR_AST_EXT_FIELD_WITHOUT_MODEL` 分支对已删型号豁免）——理由：「绑定后型号被删」的存量资产今日对合规值可正常保存，修复不得引入存量保存被拒的可见回归；对立方案（拒绝并要求清值）为激进卫生化，对 P2 缺陷不成比例，否决。两路径均落地，无「或」分支。Skill: `nop-backend-dev`
- [ ] `Proof` assets 单测：财务字段变更产生 UPDATE 审计事件（含字段名 remark）；新绑定已删型号被拒（错误码断言）；**存量路径**——资产绑定型号并携带合规 extFieldValues 后型号被逻辑删除，资产后续保存（含原必填键缺失场景，值不再按已删 defs 校验）通过且不报 `ERR_AST_EXT_FIELD_WITHOUT_MODEL`。Skill: `nop-testing`

Exit Criteria:

- [ ] 财务敏感字段变更审计事件可观测（测试断言事件类型 + remark 字段名）
- [ ] 已删型号**双路径**可观测：新绑定拒绝（错误码断言）+ 存量保存豁免（已删字段集不强制、既有值不触发 `ERR_AST_EXT_FIELD_WITHOUT_MODEL`）
- [ ] 本地化验证：`mvn test -pl module-assets/erp-ast-service` 全绿

### Phase 2 - E3 批非 fin 测试债补齐 + 收口登记

Status: planned
Targets: `module-assets`/`module-aps`/`module-inventory` 测试树、`docs/backlog/erp-enhancement-roadmap.md`、`docs/logs/2026/`
Skill: `nop-testing`

- Item Types: `Proof | Fix`
- Prereqs: Phase 1

- [ ] `Proof` MAINTENANCE/DISPOSAL 审计事件 JUnit：经 `ErpAstMaintenanceCompleteWorkProcessor`/`ErpAstDisposalProcessor` 触达 recorder 调用点，断言事件类型/资产关联落账（7 事件类型全覆盖收口）。Skill: `nop-testing`
- [ ] `Proof` TOC 阈值等值边界测试：负荷率 == 阈值不触发瓶颈、> 阈值触发（`GreedyApsSchedulingSolver.java:54-56` 语义锁定）。Skill: `nop-testing`
- [ ] `Fix` inv 快照零值等价断言修复：`TestErpInvSnapshotAndStockCheck` 补断言零值行不出现在差异集（消除空转断言）。Skill: `nop-testing`
- [ ] `Proof` scoped 复跑：`mvn test -pl module-assets/erp-ast-service` + `mvn test -pl module-aps/erp-aps-service` + `mvn test -pl module-inventory/erp-inv-service` 全绿。Skill: `nop-testing`
- [ ] `Fix` roadmap §Follow-up Backlog 勾销：P2-4/P2-5 行注记本计划引用与日期；P2-14 为跨计划复合行（fin 部分归 plan 0219-1）——本计划补非 fin 部分注记，**若 0219-1 的 fin 部分注记已落地则整行勾销并双计划引用，否则仅注记本计划部分、整行勾销归后落地者**；compliance checker 复跑（actual ≤ baseline）。Skill: `none`
- [ ] `Proof` 日志条目（`docs/logs/2026/` 执行当日，含验证状态）。Skill: `none`

Exit Criteria:

- [ ] 7 审计事件类型全部有 JUnit 覆盖（MAINTENANCE/DISPOSAL 新增）
- [ ] TOC 边界与快照零值断言测试绿
- [ ] roadmap 对应行勾销、checker actual ≤ baseline、日志落盘

## Draft Review Record

- Independent draft review iteration 1: **needs revision**（task `ses_fbb8a1908ffe19bsgaZsfy8fpx`，fresh session）——7 组基线主张活仓复核全吻合，但 2 BLOCKER：①P2-5「随绑定拒绝自然不可达」备选分支事实性错误（`validateExtFieldValues` 在 save/update 两钩子均执行且经 `asset.getModel()` 解析，绑定后型号被删的存量资产路径在仅拒绝新绑定下仍可达——目标后半句无机制无证明）；②Closure Gates 漏全仓构建（指南模板要求，先例均全 reactor install）。
- Independent draft review iteration 2: **needs revision**（task `ses_fbb81115bffem7X3xe7RjAoK3P`，fresh session）——iteration-1 两 BLOCKER 全 resolved（逐项核verified）、6 MINOR 全 resolved；新 1 BLOCKER：P2-5 ②「走既有分支**或等价语义**」在语义层复活「或」分支——既有无型号分支（BizModel:172-177）对非空值抛 `ERR_AST_EXT_FIELD_WITHOUT_MODEL`，字面读法会使存量合规资产每次保存被拒（可见回归），与 Proof 示例互斥。
- Independent draft review iteration 3: **acceptable-as-is（零 BLOCKER 零 MINOR）**（task `ses_fbb7c6565ffe0n21e4z8HkyCDg`，fresh session）——iteration-2 BLOCKER resolved：P2-5 ②语义确定性裁定（跳过强制 + 既有值豁免，零回归，对立方案显式否决带理由），Proof/退出标准确定性断言；活仓实现可行性复核通过（delVersion 检查点 :179 之后可实现；平台逻辑删除过滤器不覆盖 to-one 引用加载——`asset.getModel()` 可解析已删型号，与基线主张一致；Proof 场景可构造：`dao().deleteEntity()` 即 UPDATE delVersion）；指南合规/反松弛/范围完整性全过。

## Closure Gates

- [ ] 范围内行为完成（Phase 1/2 全部项目落地，P2-4 覆盖面清单有记录）
- [ ] 相关文档对齐（owner doc §1 覆盖面登记——仅当实际改变 owner 行为契约时）
- [ ] 已运行验证（scoped mvn test 三模块 + 全 reactor `mvn clean install -DskipTests` + compliance checker——生产代码变更经 ast-web/app/app-erp-all 聚合下游消费，closure 需全仓构建证明）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

（无）

## Closure

Status Note: （待执行后填写）

Closure Audit Evidence:

- Auditor / Agent: （待独立结束审计）
- Evidence: （待填写）

Follow-up:

- （无预留）

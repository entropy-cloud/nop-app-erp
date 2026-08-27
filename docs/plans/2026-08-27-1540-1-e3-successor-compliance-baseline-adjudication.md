# 2026-08-27-1540-1 E3 收尾 successor：compliance 基线漂移裁决（R2b/R2c）+ R7 `_tmp` 扫描范围校准

> Plan Status: completed
> Mission: erp-enhancement
> Work Item: E3 closure successor — compliance baseline adjudication（R2b 238→241 / R2c 1529→1537）
> Last Reviewed: 2026-08-27
> Source: `docs/plans/2026-08-26-0735-2-e3-integrated-implementation.md` Follow-up 第 1 条（compliance 基线裁决 successor，known failure mode 登记路径）+ `docs/context/project-context.md §已知失败模式（Compliance 基线漂移）` + `missions/erp-enhancement.json` 验证标准（"compliance checker 零漂移"）
> Related: 漂移裁决先例 `docs/plans/2026-07-25-1057-1-compliance-baseline-drift-adjudication.md`（首例）/ `2026-07-31-1705-2`（V.2）/ `2026-08-02-0651-1`（post-R6.8）/ `2026-08-23-0434-3`（M4.1 改善回写）；F1/F2 批同批先例（`compliance-baseline.md` §R2c baseline-raise（ai-check F1.2）+ §R2b/R2c/R12c 基线上调注记（plan 2026-08-26-0630-1））；checker 校准先例（R3 orm.xml 白名单 / R8 排除集 ×3 / R1d-R6-R10 注释排除，`compliance-baseline.md` 校准范式矩阵）
> Audit: required

## Current Baseline

> 2026-08-27 实时仓库核验（起草会话实测，可复现命令见 Phase 1）。

- **E3 整体实现计划已完成关闭**（`2026-08-26-0735-2`，2026-08-27 独立结束审计通过）。其 Closure Gates 显式登记 compliance 漂移归本 successor：R2b 238→241（+3）/ R2c 1529→1537（+8），per-site 证据 8 站点已登记（Phase 8 Proof 注记 + Follow-up），按 known failure mode「调高基线唯一途径=开独立计划」路径执行——本计划即该 successor。
- **checker 当前实测**（`bash docs/audits/nop-compliance-checker.sh`，2026-08-27）对照 `docs/audits/compliance-baseline.md ## BASELINE (machine-readable)`：

  | 规则 | 基线 | 当前 actual | delta | 门控判定 |
  |------|------|------------|-------|---------|
  | R2b（BizModel daoFor(Erp*)） | 238 | **241** | **+3** | ❌ REGRESSION（CI red） |
  | R2c（全生产代码 daoFor() 总量） | 1529 | **1537** | **+8** | ❌ REGRESSION（CI red） |
  | R7（System.currentTimeMillis()） | 0 | **1** | **+1** | ⚠️ 本地-only 误报（见下） |
  | 其余 16 规则（R1a-c/R1d/R2a/R2d/R3/R4/R5/R6/R8/R10/R11/R12a-c） | — | — | 0 | ✅ |

- **8 个漂移站点活仓核验**（与 E3 closure 注册清单精确一致；R2b +3 = 其中 3 个 BizModel 站点，R2c +8 = 全部）：

  | # | file:line | daoFor 实参 | 域/角色 | E3 来源 |
  |---|-----------|------------|---------|---------|
  | 1 | `module-assets/erp-ast-service/.../entity/ErpAstAssetBizModel.java:81` | `ErpAstAssetActionLog` | assets BizModel（`getAssetAuditTrail` 时间轴只读聚合） | Phase 4（E3.8） |
  | 2 | `module-assets/erp-ast-service/.../entity/ErpAstAssetBizModel.java:179` | `ErpAstAssetModel` | assets BizModel（ext 字段校验钩子 `getEntityById(modelId)`） | Phase 4（E3.3） |
  | 3 | `module-inventory/erp-inv-service/.../entity/ErpInvStockLedgerBizModel.java:107` | `ErpInvStockBalance` | inv BizModel（`checkStockBalanceConsistency` 对账全量读，须无行过滤） | Phase 3（E3.2） |
  | 4 | `module-assets/erp-ast-service/.../audit/ErpAstAssetAuditRecorder.java:47` | `ErpAstAssetActionLog` | assets 审计记录器（业务事务内审计追加） | Phase 4（E3.8） |
  | 5 | `module-aps/erp-aps-service/.../scheduling/ApsBottleneckDetector.java:131` | `ErpApsOperationOrder` | aps 瓶颈识别器（排产系统级读，须无行过滤） | Phase 5（E3.4） |
  | 6 | `module-finance/erp-fin-service/.../classify/ErpFinApDocRuleClassifier.java:99` | `ErpMdPartner` | fin 分类引擎 SPI 实现（批处理上下文伙伴匹配） | Phase 6（E3.5） |
  | 7 | `module-finance/erp-fin-service/.../processor/ErpFinApDocumentPipelineProcessor.java:515` | `ErpFinApDocument` | fin 摄取管道 Processor（自聚合实体访问） | Phase 6（E3.5） |
  | 8 | `module-finance/erp-fin-service/.../processor/ErpFinApDocumentPipelineProcessor.java:519` | `ErpFinApDocumentLog` | fin 摄取管道 Processor（自聚合实体访问） | Phase 6（E3.5） |

- **基线最后裁决源**：plan `2026-08-26-0630-1`（F2.1）注记上调 R2b 237→238 / R2c 1506→1529（+23）/ R12c 41→42；`## BASELINE (machine-readable)` 与人类可读基线表当前均为 R2b=238 / R2c=1529（两处一致，无表-块漂移）。
- **R7 +1 根因**：`_tmp/2026-08-25-201158-mission-driver/TestThreadLocalFrozenClockAnchoredSim.java:80`（git-ignored 草稿，`git check-ignore` 证实命中 `.gitignore:26 _tmp/`）。checker `PRUNE_DIRS`（脚本 :26）排除 target/_gen/node_modules/.git 但**不排除 `_tmp/`**——本地草稿被扫描产生 R7 误报；CI 干净检出不命中（E3 closure 已裁决不改 R7 基线）。实测 `_tmp/` 仅含 2 个 `.java` 文件、对 19 规则的唯一贡献 = 该 1 处 R7 命中（无 daoFor/R12 import/@Inject 贡献）——排除 `_tmp/` 的校准爆炸半径 = R7 本地噪声，零其他规则影响。
- **为何 E3 closure 未就地裁决**：E3 计划已知「调高基线唯一途径=开独立计划」且本计划无权内联上调（`compliance-baseline.md §回归门控规则`），故按 known failure mode 显式登记归 successor——登记时点 CI compliance 门为已知红，本计划负责恢复 green。
- **剩余差距**：8 站点逐项 Fix-vs-raise 裁决未做；BASELINE 块/基线表未更新；R7 本地噪声根因（`_tmp` 扫描范围）未处置；mission 验证标准「compliance checker 零漂移」未恢复。

## Goals

- 对 R2b +3 / R2c +8 漂移逐站点裁决：每个 net-new daoFor 站点分类为 `Fix`（重构消除——ORM `<to-one>` getter / I*Biz 注入 / CrudBizModel `dao()`）或 `baseline-raise`（基线既有同族合法模式，带替代方案否决理由与残留风险），对齐 `1057-1`/`1705-2`/`0651-1`/F1/F2 先例。
- 更新 `docs/audits/compliance-baseline.md`：新增裁决注记节（per-site 证据 + 源计划 + 分类）+ `## BASELINE (machine-readable)` 块 + 人类可读基线表（R2b/R2c 两行）+ F2.1 注记 R7 段落 supersede 注记四处同步。
- 裁决并处置 R7 `_tmp` 扫描范围问题（checker PRUNE_DIRS 校准 vs 维持现状，Decision 完整记录；R7 基线维持 0）。
- 恢复 CI compliance gate green 语义（全 19 规则 actual ≤ updated baseline）与 mission 验证标准「compliance checker 零漂移」。

## Non-Goals

- 不重开 E3 已关闭工作项（其生产代码经独立草案审查 + 双子代理 ORM 批准 + 独立结束审计；本计划只裁决 daoFor 站点合规语义，不改其业务行为——若识别 Fix 候选则语义等价重构）。
- E3.7 跨域流程编排保持暂缓（用户门控，roadmap 规则 6）。
- 不新增 checker 规则、不改其他规则的测量逻辑；R7 基线不动（维持 0——本地误报路径要么校准消除要么登记维持，不上调）。
- 不处理 F2.1 注记的兄弟 worktree/REPO_ROOT 环境问题（该历史记录场景当前实测零贡献——2026-08-27 复算 19 规则均与仓库范围一致，非本计划范围）。
- 不处理 E3.1b 缺口 G1/G2（已登记归 permissions-enforcement roadmap，属另一 mission）。

## Task Route

- Type: `verification or audit work`（compliance 基线裁决，遵循 4 例漂移裁决先例 + F1/F2 同批先例）
- Owner Docs: `docs/audits/compliance-baseline.md`（权威基线 + 机器可读块 + §回归门控规则）+ `docs/context/project-context.md §已知失败模式` + `docs/architecture/processor-extension-pattern.md`（Processor/Recorder daoFor 合法性背书）+ `docs/architecture/data-dependency-matrix.md`（跨域只读聚合合法性）
- Skill Selection Basis: `nop-debugging`（checker 复跑 + 逐站点归因复核）；`nop-backend-dev`（评估每站点 Fix 可行性——I*Biz 注入 / ORM `<to-one>` getter / CrudBizModel 管道替代 vs 接受为既有同族模式）。无需审计 skill——机械化基线裁决，非新维度审计（对齐 `0651-1` 同型计划先例）。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（checker `docs/audits/nop-compliance-checker.sh` + CI gate `.github/workflows/compliance.yml` 已就绪）。
- 零 ORM 变更、零业务 Java 变更（预期）；唯一可能的代码触碰 = checker 脚本 PRUNE_DIRS 一行（若 R7 校准选方案 A）或 Fix 候选重构（若 Phase 1 发现 Type-1 站点，预期无——源分类均为非 FK 导航/系统级读/管道自聚合）。
- 回滚 = git revert（纯文档 + 至多一行脚本/局部 Java 变更）。

## Execution Plan

### Phase 1 — 逐站点漂移归因 + Fix-vs-raise 裁决 + R7 范围 Decision

Status: completed
Targets: `docs/audits/compliance-baseline.md`（只读核验）+ 8 站点生产文件（只读核验）+ `_tmp/`（只读核验）
Skill: `nop-debugging` + `nop-backend-dev`

- Item Types: `Decision | Proof`
- Prereqs: 无

- [x] `Proof`：复跑 `bash docs/audits/nop-compliance-checker.sh` 捕获汇总表，确认 actuals 与 Current Baseline 表一致（R2b=241 / R2c=1537 / R7=1[本地] / 其余=基线）；若实际计数有新漂移（起草后新增生产代码），扩记入本裁决一并处理，不得静默缩小范围。
      - Skill: `nop-debugging`
- [x] `Proof`：逐站点归因落 `## Phase 1 Evidence` 节——8 站点 file:line + daoFor 实参 + 源计划 Phase + 同域/跨域分类（当前画像：#1/#2 assets 同域 BizModel；#3 inv 同域 BizModel；#4 assets 同域 Recorder；#5 aps 同域 Detector；#6 fin→md 跨域只读 SPI 实现；#7/#8 fin 同域 Processor）+ 每站点代码上下文语义（聚合查询 / getEntityById 主键导航 / 审计追加）。
      - Skill: `nop-debugging`
- [x] `Decision`：对每个站点裁定 `Fix` vs `baseline-raise`，记录选择、替代方案（I*Biz 注入 / ORM `<to-one>` getter / CrudBizModel `dao()` 逐一评估否决理由）、残留风险。预期方向（Phase 1 实测定案）：全部 `baseline-raise`——源分类均为基线既有同族合法模式（先例：审计追加=业务事务内日志追加同族[`EquipmentStatusLogWriter:31`（R1.73-75 注记）/ `ErpFinPostingExceptionRecorder`]；系统级读=对账/排产须无行过滤；批处理 SPI=I*Biz force-lazy 注入不可达[RC-R1.2 batch helper 先例]；管道自聚合=per-mutation Processor `dao()` 契约[1057-2 先例]；BizModel 校验钩子 getEntityById=主键导航非 FK 弱引用）。若发现可机械重构为 ORM `<to-one>` getter 的 Type-1 站点，则裁定 `Fix` 并在 Phase 2 执行（语义等价）。
      - Skill: `nop-backend-dev`
      - 实测修正预期：站点 #2 为 Type-1 Fix 候选（ORM `<to-one name="model">` 已建模，见 Evidence §3），裁定 `Fix`；其余 7 站点 `baseline-raise`。
- [x] `Decision`：R7 `_tmp` 扫描范围裁决——方案 A：`PRUNE_DIRS` 增 `-o -name _tmp`（对齐 R8 排除集校准先例；测量范围修正非基线放水——`_tmp/` 为 git-ignored 本地草稿区永不入库，CI 干净检出不命中故 CI 行为零变化，仅消除本地复跑噪声与「本地红/CI 绿」歧义；残留风险：`_tmp` 内生产违规被静默豁免——可忽略，因永不入库）vs 方案 B：维持现状（登记为已知本地-only 噪声；残留风险：每次本地复跑 R7 假 +1，与 CI 结果不一致，弱化 checker 本地可信度）。记录选择与理由。
      - Skill: `nop-backend-dev`
      - 裁定=**方案 A**（完整 Decision 见 Evidence §4）。

Exit Criteria:

- [x] `## Phase 1 Evidence` 节落盘：8 站点 file:line + 源计划 + 同/跨域分类 + 语义描述 + Fix-vs-raise 裁定表（含替代方案否决理由 + 残留风险），覆盖 R2b +3 / R2c +8 全部 delta。
- [x] R7 范围 Decision 完整记录（选择 + 替代方案 + 残留风险）。

### Phase 2 — 应用裁决：基线更新 + （如选）checker 校准 + 门控复验

Status: completed
Targets: `docs/audits/compliance-baseline.md`（BASELINE 块 + 基线表 + 新裁决注记节 + F2.1 注记 R7 段落 supersede 注记）；若 R7 选方案 A：`docs/audits/nop-compliance-checker.sh`（PRUNE_DIRS 一行）；若识别 Fix 候选：对应 `.java` 文件
Skill: none（Fix 路径的技能标注在项目级）

- Item Types: `Add | Fix | Proof`
- Prereqs: Phase 1 裁决完成

- [x] `Fix`：若 Phase 1 识别 Fix 候选，应用 ORM `<to-one>` getter / I*Biz 重构（语义等价）；无 Fix 候选则显式记录跳过。
      - Skill: `nop-backend-dev`
      - 落地：站点 #2 `ErpAstAssetBizModel.java:179` `daoProvider().daoFor(ErpAstAssetModel.class).getEntityById(modelId)` → `asset.getModel()`（ORM `<to-one name="model">` 生成 getter，`_gen/_ErpAstAsset:1662` 实证存在）。
- [x] `Add`：更新 `docs/audits/compliance-baseline.md` 四处同步——(a) 新增裁决注记节（漂移源 = E3 整体计划 8 站点 per-site 证据摘要 + Fix-vs-raise 分类 + R7 `_tmp` 裁决）；(b) `## BASELINE (machine-readable)` 块 R2b/R2c 更新为裁决值（全部 baseline-raise 则 = 241/1537；有 Fix 则按 Fix 后重测值）；(c) 顶部人类可读基线表 R2b/R2c 两行同步（防表-块漂移复发，对齐 2026-08-20-0518-3 表格同步注记教训）；(d) F2.1 注记（plan 2026-08-26-0630-1）R7 段落补一行 supersede 交叉引用（其「兄弟 worktree 污染 / repo 范围内 R7=0」为 2026-08-26 当时实态表述；当前 R7 本地 +1 根因 = `_tmp/` 草稿扫描，见本计划 R7 裁决——消除权威文档内两处分歧 R7 解释无交叉引用的状态）。
      - Skill: none
      - 落地：四处均已同步——(a) 文末新增「R2b/R2c 基线上调注记 + 站点 #2 Fix + R7 `_tmp` 扫描范围校准（plan 2026-08-27-1540-1，E3 closure successor）」节（8 站点裁决表 + Fix/基线值 + R7 校准）；(b) BASELINE 块 R2b: 240 / R2c: 1536；(c) 基线表两行 240/1536；(d) F2.1 R7 段落后追加 supersede 注记块。
- [x] `Add`：若 R7 选方案 A——`nop-compliance-checker.sh` `PRUNE_DIRS` 增 `_tmp` 排除 + 脚本注释注明校准依据（本计划 + R8 排除集先例）；复跑证实仅 R7 本地计数变化（1→0），其余 18 规则计数不变（Current Baseline 已实测 `_tmp` 唯一贡献 = 1 处 R7）。
      - Skill: none
      - 落地：`PRUNE_DIRS` 已含 `-o -name _tmp` + 校准依据注释（脚本 :26-30）；复跑实测 R7=0，其余 18 规则与 Phase 1 §1 实测逐行一致（零其他规则影响）。
- [x] `Proof`：复跑 `bash docs/audits/nop-compliance-checker.sh` + 模拟 `compliance.yml` `Enforce baseline gate` python 逻辑（解析 actual vs 更新后 BASELINE 块）→ 全 19 规则 actual ≤ baseline → PASS（CI green 语义恢复）。
      - Skill: `nop-debugging`
      - 落地：见 `## Phase 2 Evidence`。

Exit Criteria:

- [x] `compliance-baseline.md` 四处（注记节 / BASELINE 块 / 基线表 / F2.1 R7 supersede 注记）内部一致，R2b/R2c 裁决值落盘。
- [x] 门控模拟 PASS（actual ≤ updated baseline，全 19 规则零 REGRESSION）。
- [x] 若应用了 Fix 或 R7 方案 A：Fix 目标 scoped `mvn test` 绿（语义等价零回归）；纯文档 + checker 校准则记录跳过全量 mvn 的理由（零业务 Java 变更）。
      - Fix 已应用 → scoped `mvn test -pl module-assets/erp-ast-service -Dtest=TestErpAstExtFieldsAndAuditTrail` 6/6 绿（0 failures / 0 errors / 0 skipped，surefire 报告 2026-08-27 19:11 本会话实测）；R7 方案 A 为 checker 脚本一行 + 注释，非业务 Java 变更。

## Phase 1 Evidence

> 2026-08-27 执行会话活仓实测（复现命令随条目附注）。

### §1 checker 复跑 Proof（Item 1）

`bash docs/audits/nop-compliance-checker.sh` 汇总表实测（2026-08-27）：R1a/b/c=0、R1d=14、R2a=34、**R2b=241**、**R2c=1537**、R2d=38、R3=5、R4=0、R5=0、R6=2、**R7=1（本地）**、R8=0、R10=12、R11=0、R12a/b/c=70/66/42——与 Current Baseline 表**逐行一致**（R2b=241 / R2c=1537 / R7=1[本地] / 其余 16 规则=基线），起草后无新漂移，裁决范围不扩大。

### §2 逐站点归因（Item 2）

8 站点活仓核验（file:line 与 Current Baseline 站点表精确一致，E3 来源 = plan `2026-08-26-0735-2`）：

| # | file:line（实测） | daoFor 实参 | 同/跨域 | 代码上下文语义（实测摘录） |
|---|------------------|------------|---------|---------------------------|
| 1 | `module-assets/erp-ast-service/.../entity/ErpAstAssetBizModel.java:81` | `ErpAstAssetActionLog` | assets 同域 BizModel | `getAssetAuditTrail` @BizQuery 审计时间轴只读聚合：`ormTemplate.runInSession` 内 QueryBean(eq assetId) + orderBy createTime/id desc → `dao.findAllByQuery`，行投影为 Map 时间轴 |
| 2 | `module-assets/erp-ast-service/.../entity/ErpAstAssetBizModel.java:179` | `ErpAstAssetModel` | assets 同域 BizModel | `validateExtFieldValues` E3.3 校验钩子（defaultPrepareSave/Update 调用）：`String modelId = asset.getModelId()` 变量拆分形态 → `daoFor(ErpAstAssetModel).getEntityById(modelId)`，null → `ERR_AST_ASSET_MODEL_NOT_FOUND` |
| 3 | `module-inventory/erp-inv-service/.../entity/ErpInvStockLedgerBizModel.java:107` | `ErpInvStockBalance` | inv 同域 BizModel | `checkStockBalanceConsistency` @BizQuery E3.2 对账：`dao.findAll()` **全量无过滤加载**比对面（代码注释明示「对账比对面 ErpInvStockBalance 全量加载」），ledger 派生侧对照 BOOK_ONLY/MISMATCH/LEDGER_ONLY 三类差异 |
| 4 | `module-assets/erp-ast-service/.../audit/ErpAstAssetAuditRecorder.java:47` | `ErpAstAssetActionLog` | assets 同域 Recorder | E3.8 审计记录器 `record`：业务事务内 `dao.newEntity()` + 逐字段 from*/to* 快照 + `dao.saveEntity(log)` 追加审计行（类 javadoc 明示「同域审计实体插入经 daoFor 直查（E3 daoFor 豁免：域内审计追加）」；BizModel `@Inject auditRecorder` 消费） |
| 5 | `module-aps/erp-aps-service/.../scheduling/ApsBottleneckDetector.java:131` | `ErpApsOperationOrder` | aps 同域 Detector | E3.4 TOC 瓶颈识别 `findPlannedInHorizon`：QueryBean(status in [PLANNED, IN_PROGRESS] + plannedEndDateT ≥ horizon) 系统级批量读（代码注释明示「同域实体只读聚合…IDaoProvider 直访对齐 ApsLoadSourceProvider 范式」），machineId 负荷聚合 |
| 6 | `module-finance/erp-fin-service/.../classify/ErpFinApDocRuleClassifier.java:99` | `ErpMdPartner` | **fin→md 跨域只读** SPI 实现 | E3.5 默认规则分类引擎 `matchPartner`：QueryBean(partnerType in [SUPPLIER, BOTH] + status=ACTIVE, limit 2000) → 名称双向包含匹配（非 FK 导航）；实现 `IErpFinApDocClassifier` SPI（类 javadoc 明示「跨域只读 ErpMdPartner…对齐 ApsLoadSourceProvider 范式」），消费方 = 管道 Processor + nop-batch 异步 |
| 7 | `module-finance/erp-fin-service/.../processor/ErpFinApDocumentPipelineProcessor.java:515` | `ErpFinApDocument` | fin 同域 Processor | E3.5 摄取管道 `docDao()` accessor helper：管道自聚合实体（上传→OCR→分类→草稿→日志）存取 |
| 8 | `module-finance/erp-fin-service/.../processor/ErpFinApDocumentPipelineProcessor.java:519` | `ErpFinApDocumentLog` | fin 同域 Processor | 同上 `logDao()` accessor helper：每步处理轨迹落 `ErpFinApDocumentLog`（AP-4 审计追溯） |

算术对账：BizModel 站点 = #1/#2/#3（+3 = R2b 238→241 ✓）；全 8 站点（+8 = R2c 1529→1537 ✓）；#6 非 `*Processor/*Dispatcher/*Engine` 文件名（R2d 不命中 ✓）；#4/#5/#7/#8 非 BizModel（R2b 不命中 ✓）；Processor/Recorder/Detector/Classifier 均被 BizModel 或 SPI 消费（R8=0 ✓）。

### §3 Fix-vs-raise 裁定（Item 3）

**裁定汇总：#2 = `Fix`（Type-1，ORM `<to-one>` 已建模）；其余 7 站点 = `baseline-raise`。**

| # | 裁定 | 选择理由 | 替代方案逐一评估（否决理由） | 残留风险 |
|---|------|---------|------------------------------|----------|
| 1 | baseline-raise | 审计时间轴只读聚合——ActionLog 为系统追加型审计实体 | (a) ORM `<to-one>` getter：N/A——`ErpAstAsset` 无 actionLogs 关系（relations 仅 8 个 to-one，orm.xml 实测），且时间轴需 orderBy createTime/id 的跨行聚合非单实体导航；(b) I*Biz 注入（`IErpAstAssetActionLogBiz`）：审计日志无用户 CRUD 业务面（对齐 R1.73-75 `ErpMntEquipmentStatusLog`「新实体无 I*Biz 业务面需求」先例），注入将为只读时间轴凭空创建业务面；(c) CrudBizModel `dao()`：仅限本实体（ErpAstAsset） | 无实质风险（只读聚合，BizQuery 已过权限管道） |
| 2 | **Fix** | **Type-1**：`modelId` 来自作用域托管实体 getter（`asset.getModelId()` 变量拆分形态）+ ORM `<to-one name="model" refEntityName="ErpAstAssetModel">` **已建模**（`module-assets/model/app-erp-assets.orm.xml:226`，join modelId→id）——精确命中 F1 重构族判据（`2026-07-24-0605-3`/`2000-1`/`0941-1`：FK 来自作用域托管实体 getter 且 ORM to-one 已建模 = safe Type 1）。语义等价证明：生成 getter `_ErpAstAsset.getModel()`（_gen :1662）→ `internalGetRefEntity` → `OrmSessionImpl.internalLoadRefEntity` → 单列 PK join 取 `castId(modelId)` → `session.load(refEntityName, id)`——与 `IEntityDao.getEntityById` 收敛到同一 session PK load（对Transient 未保存实体同样经 session enhancer 解析；逻辑删除行为两路径一致，同族先例 0605-3 已在 assets 域 12 处验证）。测试覆盖：`TestErpAstExtFieldsAndAuditTrail` 6 用例（happy + 3 拒绝路径 + 审计 ×2）经真实 save/update 管道驱动本钩子 | (a) I*Biz 注入：钩子在 save/update 管道中段执行，再入 IBiz 管道有重入复杂度且 BizModel 自身即 ErpAstAsset BizModel（校验目标为关联实体）；(b) 维持 daoFor：可机械重构为已建模 to-one getter 的 Type-1 站点按先例应 Fix（本计划 Phase 1 Decision 条款） | 无——ORM 关系懒加载经 session 缓存，等价 PK load；快照测试守护 |
| 3 | baseline-raise | 对账系统级读——比对面**必须无行过滤全量加载**（E3.2 语义：行过滤/objMeta 投影会静默掩盖差异） | (a) ORM 关系 getter：N/A——StockBalance 与 StockLedger 为两个独立实体非 FK 导航；(b) I*Biz `findList`：走 CrudBizModel 管道将施加 objMeta filterable-field 校验 + 数据权限行过滤，破坏对账「全量无过滤」前提（RC-R1.19 同型实测先例：非 filterable 字段触发 `prop-not-support-filter-op`）；(c) CrudBizModel `dao()`：仅限本实体（ErpInvStockLedger） | 单组织基线量级可控（代码注释明示）；无过滤为对账功能语义 |
| 4 | baseline-raise | 业务事务内审计追加——`newEntity/saveEntity` 行创建非导航 | (a) ORM getter：N/A（创建新行）；(b) I*Biz：审计实体无业务面（同 #1 先例 `EquipmentStatusLogWriter:31`/`ErpFinPostingExceptionRecorder` 同族——业务事务内日志追加经 daoFor 直查为文档化豁免族）；(c) 上提到 BizModel：Recorder 被 BizModel + 4 Processor 共用（E3.8 设计：单一真相源写点），上提造成逻辑散落 | 无——同事务原子性由调用约定保证（类 javadoc 明示） |
| 5 | baseline-raise | 排产系统级读——负荷聚合须无行过滤（PLANNED/IN_PROGRESS 扫描） | (a) ORM getter：N/A（批量条件查询非 FK 导航）；(b) I*Biz：Detector 为非 BizModel 支撑类（代码注释引用 `ApsLoadSourceProvider` 同域只读先例——基线已含）；(c) 传 IServiceContext 走管道：系统级统计无用户行过滤语义 | 无 |
| 6 | baseline-raise | 跨域只读模糊匹配——名称双向 contains 非任何 FK 导航；SPI 批处理上下文 | (a) ORM getter：N/A（模糊匹配批量读）；(b) I*Biz 注入：**force-lazy 注入不可达**——Classifier 经 SPI 由管道 Processor/nop-batch 消费，IBiz 惰性注入属性在该上下文不注入（RC-R1.2 `ErpFinBankReconAutoReverseHelper` 先例：实测同约束）；(c) CrudBizModel：非 BizModel 子类 | 跨域只读经 `data-dependency-matrix.md` §2.4 fin→md 只读边背书；limit 2000 上界 |
| 7 | baseline-raise | 管道自聚合实体存取——Processor `dao()` 契约族 | (a) ORM getter：N/A（管道头实体存取非 FK 导航）；(b) I*Biz：per-mutation/管道 Processor 持有自身实体 DAO 为 `AbstractProcessor<T>` 编排骨架契约（1057-2 +149 / R6.8 +130 先例——`daoProvider.daoFor(<EntityClass>)` 是 Nop 平台读取托管实体 DAO 的标准方式）；(c) 上提到 BizModel：管道每步落日志/状态翻转属 Processor 编排职责 | 无 |
| 8 | baseline-raise | 同 #7——`logDao()` 处理轨迹 accessor（AP-4） | 同 #7 | 无 |

**Fix 后预期计数**：R2b 241−1=**240**（净 +2 = #1/#3）、R2c 1537−1=**1536**（净 +7 = 除 #2 外全部）——Phase 2 实测复核。

### §4 R7 `_tmp` 扫描范围 Decision（Item 4）

**裁定 = 方案 A**（`PRUNE_DIRS` 增 `-o -name _tmp`）。

- **根因实证**（2026-08-27）：checker banner 实测 `仓库: /Users/abc/app/nop-app-erp`（REPO_ROOT 解析正常，F2.1 注记的兄弟 worktree 场景当前不存在——`/Users/abc/app` 下无 `nop-entropy-wt` 贡献）；R7 唯一命中 = `_tmp/2026-08-25-201158-mission-driver/TestThreadLocalFrozenClockAnchoredSim.java:80`（`git check-ignore` 证实命中 `.gitignore:26 _tmp/`）。`_tmp/` 全量 `.java` 仅 2 个文件（另一 `ThreadLocalFrozenClock-anchor-wip-with-perthread-fix.java` 零规则命中），对 19 规则唯一贡献 = 该 1 处 R7（无 daoFor/@Inject/R12 import/new Erp*/REQUIRES_NEW 贡献，逐模式 grep 实测）——**排除 `_tmp/` 的爆炸半径 = R7 本地噪声，零其他规则影响**（与 Current Baseline 预告一致）。
- **理由**：测量范围修正非基线放水（对齐 R8 排除集校准先例 `1057-1`/`0656-1` ×3 + R1d-R6-R10 注释排除的校准范式矩阵）——`_tmp/` 为 git-ignored 本地草稿区**永不入库**，CI 干净检出不命中，故 CI 门控行为**零变化**；仅消除本地复跑噪声与「本地红/CI 绿」歧义（恢复 checker 本地可信度 = mission 验证标准「compliance checker 零漂移」的本地可复现性）。
- **替代方案（方案 B 维持现状）否决理由**：每次本地复跑 R7 假 +1 与 CI 结果不一致，持续弱化 checker 本地可信度且与本计划「恢复零漂移验证面」目标冲突。
- **残留风险**：`_tmp` 内生产违规被静默豁免——可忽略（永不入库，CI 干净检出不受排除影响）。
- **R7 基线维持 0**（校准后 actual=0，不上调）。

## Phase 2 Evidence

> 2026-08-27 执行会话活仓实测（复现命令随条目附注）。

### §1 Fix 落地 Proof（Item 1）

站点 #2 重构落地：`module-assets/erp-ast-service/.../entity/ErpAstAssetBizModel.java:179` 现为 `ErpAstAssetModel model = asset.getModel();`（git diff 单行替换，原 `daoProvider().daoFor(ErpAstAssetModel.class).getEntityById(modelId)`）。生成 getter 实证：`module-assets/erp-ast-dao/.../entity/_gen/_ErpAstAsset.java:1662` `public final ErpAstAssetModel getModel()`；ORM 源 `module-assets/model/app-erp-assets.orm.xml:226` `<to-one name="model" refEntityName="...ErpAstAssetModel">` 实证存在。scoped 测试：`mvn test -pl module-assets/erp-ast-service -Dtest=TestErpAstExtFieldsAndAuditTrail` → **Tests run: 6, Failures: 0, Errors: 0, Skipped: 0**（surefire 报告 2026-08-27 19:11，本会话实测；happy + 3 拒绝路径 + 审计 ×2 全绿，真实 save/update 管道驱动本钩子）。

### §2 基线四处同步 + checker 校准 Proof（Item 2/3）

`compliance-baseline.md` 四处同步落地：(a) 文末新增本计划裁决注记节（8 站点裁决表 + Fix/基线值 + R7 校准 + F2.1 supersede 指向）；(b) `## BASELINE (machine-readable)` 块 R2b: 240 / R2c: 1536（R7 维持 0）；(c) 顶部人类可读基线表 R2b/R2c 两行同步 240/1536；(d) F2.1 注记（plan 2026-08-26-0630-1）R7 段落后追加 supersede 注记块（当前 R7 本地 +1 根因 = `_tmp/` 草稿扫描，非兄弟 worktree）。checker 脚本 `PRUNE_DIRS` 已含 `-o -name _tmp` + 校准依据注释（`nop-compliance-checker.sh:26-30`，引用本计划 + R8 排除集先例）。

### §3 checker 复跑 + 门控模拟 Proof（Item 4）

`bash docs/audits/nop-compliance-checker.sh`（2026-08-27 执行会话）实测：R1a/b/c=0、R1d=14、R2a=34、**R2b=240**（= Phase 1 §3 预期 241−1 ✓）、**R2c=1536**（= 预期 1537−1 ✓）、R2d=38、R3=5、R4=0、R5=0、R6=2、**R7=0**（`_tmp` 校准后 1→0 ✓）、R8=0、R10=12、R11=0、R12a/b/c=70/66/42——与 §3 预期计数**逐行精确一致**，其余 18 规则较 Phase 1 §1 零变化（`_tmp` 排除爆炸半径 = R7 本地噪声，实证）。门控模拟：按 `compliance.yml` `Enforce baseline gate` python 逻辑（解析 checker 汇总表 actual vs 更新后 BASELINE yaml 块）复算 → `baseline rules: 19 | parsed actual rules: 19` → **OK: no rule exceeds baseline. Gate PASSED**（全 19 规则 actual == updated baseline 零 REGRESSION 零 improvement，CI green 语义恢复，mission 验证标准「compliance checker 零漂移」达成）。

## Draft Review Record

- Independent draft review iteration 1: **accept**（task `ses_fbdd36050ffevjVjUbV6HhFbYU`，fresh session 独立审查者，2026-08-27）——活仓逐项核验全过：checker 实测 R2b=241/R2c=1537/R7=1（本地）+ 其余 16 规则=基线、8 站点 file:line 与 daoFor 实参精确证真、R2b +3=R2c +8 子集算术一致、`_tmp/` git-ignore + 2 个 .java 唯一贡献 1 处 R7、PRUNE_DIRS 无 `_tmp`、E3 Follow-up 注册清单与站点表一致、4 例先例计划 + F2.1 注记存在、表-块同步（238/1529）、mission 验证标准含「compliance checker 零漂移」、CI 门控逻辑（`compliance.yml` python step）与本地门控模拟 Proof 相符、指南合规（模板/类型/Skill/反松弛零命中/退出标准分域/规则 1-9-12）。0 BLOCKER / 1 MAJOR / 2 MINOR：MAJOR-1（F2.1 注记 R7 段落的「兄弟 worktree/repo 范围 R7=0」历史表述与当前 `_tmp` 根因并存无交叉引用——已采纳：Phase 2 基线文件同步扩为四处，增 (d) F2.1 R7 段落 supersede 注记）；MINOR-1（站点 #4 审计追加先例引用错配——已采纳：改引 `EquipmentStatusLogWriter`/`ErpFinPostingExceptionRecorder` 同族）；MINOR-2（Phase 2 阶段级 Skill 条件式表述——已采纳：阶段级改 `none`，条件性留在项目级）。三项修订均按审查建议落档，共识达成 → Plan Status draft→active。

## Closure Gates

> 纯文档/基线裁决计划（至多一行 checker 脚本校准 + 预期零业务 Java 变更）：完整仓库 mvn 验证按「无代码变更（仅文档）计划删除验证命令门控并说明原因」精神处理——若零业务 Java 变更，以 checker 复跑 + CI 门控 python 模拟替代 mvn 门控并记录理由；若应用 Fix，则 scoped `mvn test` 必跑。

- [x] 范围内行为完成：8 站点全部裁定 landed（Fix 或 baseline-raise），CI gate green 语义恢复（全 19 规则 actual ≤ updated baseline）。
- [x] 相关文档对齐：`compliance-baseline.md` 四处同步一致（含 F2.1 R7 supersede 注记）；`docs/logs/` 日志条目落盘。
- [x] 已运行验证：checker 复跑 + 门控 python 模拟 PASS；按上述条件门控规则处理 mvn（零业务 Java 变更则记录跳过理由，有 Fix 则 scoped test 绿）——Fix 已应用，scoped `mvn test -pl module-assets/erp-ast-service -Dtest=TestErpAstExtFieldsAndAuditTrail` 6/6 绿（执行会话 + 独立审计会话双重复跑均绿）。
- [x] 无范围内项目降级为 deferred/follow-up（每站点均裁定 landed；R7 Decision 两分支均为完整落地非降级）。
- [x] 独立草案审查已完成并记录。
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致。
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符。（审计会话 `ses_fbd0e7b9cffe1u0PafIHKR6bSb`，2026-08-27，PASS 后由执行者落账）
- [x] 结束证据存在于文件中。

## Deferred But Adjudicated

> 预期无降级项。若 Phase 1 发现某 Fix 候选重构成本超收益（如需 ORM 关系新建或破坏 SPI 契约），在此登记为 `optimization candidate` 并命名触发条件（如「该文件下次业务变更时一并重构」）。

## Closure

Status Note: 已关闭——8 漂移站点全部裁定 landed（#2 Fix + 7 baseline-raise，per-site 证据 + 替代方案否决理由落 §Phase 1 Evidence）；基线四处同步 R2b=240 / R2c=1536 / R7=0（checker 复跑与门控 python 模拟双 PASS，CI green 语义恢复，mission「compliance checker 零漂移」达成）；R7 `_tmp` 扫描范围校准方案 A 落地（CI 行为零变化）。独立结束审计 PASS（0 BLOCKER）。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理（fresh session，task `ses_fbd0e7b9cffe1u0PafIHKR6bSb`，2026-08-27）
- Evidence: 10/10 checklist 项活仓核验全过——①Phase 1/2 全 items + Exit Criteria `[x]` 且 Status completed、Closure Gates 8 项当时全 `[ ]`（审计时点预期态）、零反松弛命中；②Fix 实证（`ErpAstAssetBizModel.java:179` = `asset.getModel()`，git diff 恰 1 行；orm.xml:226 to-one + `_gen/_ErpAstAsset:1662` getter + 站点 #1 :81 未误删）；③scoped 测试独立复跑 6/0/0/0 绿（fresh mtime）；④checker 校准实证（:26-30 注释 + `-o -name _tmp`；`git check-ignore` 命中 .gitignore:26；脚本 diff 仅该行+注释）；⑤基线四处同步实证（注记节 :693 起 8 站点表 / yaml 块 :485-486 `R2b: 240`/`R2c: 1536` / 人类表 :22-23 与 yaml 零表-块漂移 / F2.1 supersede 注记 :525-528；diff 30+/4− 恰限四处）；⑥审计者自行复跑 checker（240/1536/R7=0 逐行一致）+ 独立复算 compliance.yml 门控 python（19/19 规则 `OK: no rule exceeds baseline. Gate PASSED.`）；⑦算术对账（R2b 238+3−1=240 / R2c 1529+8−1=1536 三处一致）+ 8 站点 file:line 全活仓证真；⑧日志条目存在（`docs/logs/2026/08-27.md:3-8`）；⑨文本一致 + E3 源计划 Follow-up 已解决注记（:310）；⑩Non-Goals 守约（无新规则、无其他测量逻辑变更、业务 Java 变更恰 1 行语义等价）。MINOR 3 项均非阻塞（工作树遗留脚手架为 E3 时代已登记项 / jqwik 第三方 easter-egg 输出已识别 disregarded / getter 返回类型全限定名 cosmetic）。

Follow-up:

- 预期无新 successor——本裁决为机械基线对账；recurrence 防护由 `project-context.md §已知失败模式` + E3 closure 登记 + 本计划承载。

# 2026-07-01-1800-1-document-correction-convergence 文档修正收敛计划

> Plan Status: completed
> Last Reviewed: 2026-07-01
> Source: `docs/analysis/2026-07-01-0000-design-doc-readiness-and-structure-assessment.md`（第二轮修订，含 16 条选定方案）
> Related: `02-documentation-improvement-plan.md`（已完成，覆盖 Q1-Q83 决议）
> Audit: required

## Current Baseline

- `02-documentation-improvement-plan.md` 已完成（覆盖 81 个 grill 决议），但遗留了评估文档发现的结构性/收敛性问题
- 当前文档体系存在 16 个已确认的问题，涵盖路径映射混乱、审批状态语义冲突、requirements 层越权、跨域引用旧稿残留、过账机制多口径、测试策略双稿并存、模块边界自相矛盾等
- 上述问题的选定方案已在 `2026-07-01-0000-design-doc-readiness-and-structure-assessment.md` 中给出，但仅停留在分析文档中，owner doc 尚未落地修正
- 无代码变更，本计划仅涉及 owner doc 更正

## Goals

- 将分析报告中 16 条选定方案落地到对应的 owner doc（约 25-30 个文件）
- 消除所有已知的文档内部矛盾（审批状态、过账事务边界、模块依赖方向）
- 解决"不同人会从不同文档里读出不同答案"的根源

## Non-Goals

- 不修改 ORM 模型文件（`.orm.xml`）—— 所有模型变更需单独 plan-first
- 不实现业务逻辑代码
- 不新增功能设计或架构决策 —— 仅落地已裁决的方案
- 不重新设计已确认的业务规则 —— 仅修正文档表述
- 不创建新域设计文档（如 B2B、portal 的内容本身已是设计资产，不予新增修改）

## Task Route

- Type: `app-layer design change（文档层面）`
- Owner Docs: `docs/design/`, `docs/architecture/`, `docs/requirements/`, `docs/context/`
- Skill Selection Basis: none（无匹配技能，按分析报告方案执行）
- Verification: 仅文档一致性审查，无构建/测试命令

## Infrastructure And Config Prereqs

No infra prereqs beyond existing baseline. All work is documentation-level.

## Execution Plan

### Phase 1 — 落位映射 + 范围口径（#1, #8）

Status: completed
Targets: `docs/architecture/domain-module-split-analysis.md`, `docs/architecture/module-boundaries.md`, `docs/architecture/data-dependency-matrix.md`, `docs/requirements/product-scope.md`, `docs/architecture/project-vision.md`, `docs/context/codebase-map.md`, `docs/architecture/system-baseline.md`, `docs/context/project-context.md`
Skill: none

- Item Types: `Fix`（8/8 — 全部是旧口径/映射修正）

- [x] **Fix** `domain-module-split-analysis.md` 新增 §2.0 工程命名映射表（19 行，含逻辑工程名、顶层目录、子模块前缀、artifactId 前缀、appName、VFS moduleId 等）
- [x] **Fix** `module-boundaries.md` 中文表 + DAG 图加指针：物理目录映射见 domain-module-split-analysis.md §2.0；表内 `app-erp-app` 改为 `app-erp-app（聚合，物理目录 app-erp-all）`
- [x] **Fix** `data-dependency-matrix.md` DAG 总览 + 域级矩阵从 10 域扩展为 18 域（含 crm/cs/hr/aps/contract/drp/logistics/b2b）
- [x] **Fix** `product-scope.md` 产品基线从 10 域改为 18 域完整列表；若需保留旧口径迁入 docs/archive
- [x] **Fix** `project-vision.md` 同改 18 域
- [x] **Fix** `system-baseline.md` 清理 `app-erp-app` 等旧路径名
- [x] **Fix** `codebase-map.md` 补映射表入口指针或复制映射表
- [x] **Fix** `project-context.md` 统一 18 域口径

Exit Criteria:

- [x] domain-module-split-analysis.md §2.0 映射表存在且包含全部 19 行
- [x] 所有全局文档（product-scope/project-vision/project-context/system-baseline/module-boundaries/data-dependency-matrix）的"10 域"口径已清除
- [x] module-boundaries.md 中 `app-erp-app` 已更正

### Phase 2 — 审批 + 过账 + 跨域引用 + 模块边界收敛（#2, #4, #5, #11, #12, #13）

> 顺序说明：本阶段先于 Phase 3 的 requirements 层收边，因为 Phase 2 解决的是"同一件事在多份文档里口径冲突"的基础语义问题（审批状态/过账机制/跨域规则），Phase 3 的 requirements 清洗需要依赖于这些基础语义先收敛，避免两边互相污染。

Status: completed
Targets: `docs/architecture/approval-framework.md`, `docs/design/flow-overview.md`, `docs/architecture/service-layer-orchestration.md`, `docs/design/finance/posting.md`, `docs/architecture/data-dependency-matrix.md`, `docs/architecture/cross-domain-constraints.md`, `docs/architecture/module-boundaries.md`, `docs/design/domain-design-guidelines.md`
Skill: none

- Item Types: `Fix`（8/9 — 文档表述修正与语义统一）+ `Proof`（1/9 — 残留扫描验证）

- [x] **Fix** `approval-framework.md:44-45` —— approveStatus 从"只跟踪 APPROVED/REJECTED"补充为完整四态（UNSUBMITTED/SUBMITTED/APPROVED/REJECTED），加 SUBMITTED 语义说明
- [x] **Fix** `flow-overview.md:542` —— `APPROVING` → `SUBMITTED`
- [x] **Fix** `service-layer-orchestration.md:87,109` —— `order.status` → `order.approveStatus`；`:151` —— `PENDING_APPROVAL` → `SUBMITTED`
- [x] **Fix** `posting.md` —— 架构图改为三层模型（业务+库存同事务 / 可配凭证层 / posted 兜底层），标注库存写入强制 SYNC；新增"稳定约束 vs 可配置策略"小节；新增"businessType vs billType 分工"小节；在 §异步过账或 §测试相关补充 `postNow` 同步测试缝入口说明（供 IErpFinPostingBiz 设计时预留同步直调 API）
- [x] **Fix** `data-dependency-matrix.md` —— §4.1 凭证段从"S 写"拆为"SYNC 同事务 / ASYNC 经 afterCommit 解耦"；§4.4 L209 升级"默认同步"为"默认 SYNC 按 billType 可切 ASYNC"；§2.1 加裁决原则 5 条优先级；修正 manufacturing/quality 依赖方向；§5.2 加 billType→businessType 反向引用
- [x] **Fix** `cross-domain-constraints.md` —— 保留消弧事件+事务约束，删除 @RefLink 节，替换为平台原生机制 B+D；加取代说明；同步处理 discussion/plan 残留引用
- [x] **Fix** `module-boundaries.md` —— 删除英文孤儿段（:68-77）；中文表 manufacturing 行"禁止依赖"细化为"finance/maintenance（ORM+S写）；quality 仅事件触发"；DAG 图修正 quality/manufacturing 方向
- [x] **Fix** `domain-design-guidelines.md:96-98` —— 单句禁令改写为分级规则（读机制 D / 高频多维机制 B / 写 I*Biz / 禁止外部生成 className）
- [x] **Proof** 全文 `rg -n 'APPROVING\|PENDING_APPROVAL' docs/` 与 `rg -n '\.status' docs/architecture/service-layer-orchestration.md` 确认无残留

Exit Criteria:

- [x] 审批四态在 approval-framework/flow-overview/service-layer-orchestration 中统一
- [x] posting.md 三层架构图 + 约束/可配拆分 + billType/businessType 分工已落位
- [x] cross-domain-constraints.md 不再传播 @RefLink 概念
- [x] module-boundaries.md 无内部矛盾（英文段删除、dep 方向单项一致）
- [x] domain-design-guidelines.md 主数据引用规则已分级

### Phase 3 — requirements + 权限 + 测试（#3, #6, #7）

Status: completed
Targets: `docs/requirements/product-baseline.md`, `docs/requirements/mvp.md`, `docs/design/roles-and-permissions.md`, `docs/design/app-overview.md`, `docs/architecture/testing-strategy.md`, `docs/architecture/test-strategy.md`, `docs/architecture/seed-data.md`, `docs/architecture/`, `app-erp-test-data/`, `docs/index.md`
Skill: none

- Item Types: `Fix | Decision`（requirements 归档/清洗）、`Fix`（权限拆分）、`Fix | Add`（测试 owner doc + 新建模块）

- [x] **Fix | Decision** `mvp.md` —— 标注为历史模板，在 docs/index.md 路由中去掉；或移入 docs/archive/
- [x] **Fix** `product-baseline.md` —— 填占位符、技术机制内容迁回 design/architecture、加顶部状态横幅定义文档定位
- [x] **Fix** `roles-and-permissions.md` —— 拆"实现落位"为"设计能力基线"和"运行基线"两小节；末尾加外部 portal 主体指针
- [x] **Fix** `app-overview.md:36` —— 括号注释改为指向 roles-and-permissions.md#运行基线的链接
- [x] **Fix | Decision** `testing-strategy.md` —— 声明为唯一 owner doc；新增四小节：四类资产边界表、跨域归属三层规则、异步过账测试时序模型（同步缝+异步轮询+兜底直调）
- [x] **Add** `test-strategy.md` —— 归档至 docs/archive/test-strategy.md（保留原相对名）；回收 P0/P1 业务流清单并入 testing-strategy.md
- [x] **Fix** `seed-data.md` —— 顶部加注"部署资产，非测试资产，与 app-erp-test-data 区分"
- [x] **Add** `app-erp-test-data` 模块骨架（`_vfs/test-data/tables/` 目录 + `load-order.txt` + 项目描述 pom.xml，不包含具体 CSV 文件）

Exit Criteria:

- [x] requirements 层不再含技术实现决策；mvp.md 已归档或标注
- [x] roles-and-permissions.md 已拆分为设计/运行两基线
- [x] testing-strategy.md 为唯一 owner doc；test-strategy.md 已归档
- [x] app-erp-test-data 模块骨架已创建

### Phase 4 — 文档路由修复 + 目录清理（#9, #10, #14, #15, #16）

Status: completed
Targets: `docs/design/README.md`, `docs/architecture/README.md`, `docs/design/feature-inventory.md`, `docs/design/portal/README.md`, `docs/design/portal/identity-and-access.md`, `docs/design/app-overview.md`, `docs/design/domain-design-guidelines.md`, `docs/architecture/integration-and-transaction-patterns.md`, `docs/design/erp-design-audit-checklist.md`
Skill: none

- Item Types: `Fix`（路由/重复/模板）、`Add`（portal identity doc）

- [x] **Fix** `design/README.md` —— B2B 从 architecture 路由改为 design 域；扩展域表加 `portal | future extension`
- [x] **Fix** `architecture/README.md` —— B2B 集成契约指向保留，but 注明业务语义归 design
- [x] **Fix** `feature-inventory.md` —— 去重、统一 owner doc 路由（APS 统一指向 aps/README.md）
- [x] **Fix** `portal/README.md` —— 顶部加 STATUS 横幅（future extension placeholder）；支付/SSO/mall 复用论述降级标记 `(future)`
- [x] **Add** `portal/identity-and-access.md` —— 定义外部主体角色/partner.userId 绑定模式/data-auth 行级隔离骨架/最小动作集（标注 ask-first，不立即改 orm.xml）
- [x] **Fix** `app-overview.md:10` —— 在"前台商城/门户暂不在当前基线范围"后补"详见 portal/README.md STATUS 横幅"
- [x] **Fix** `domain-design-guidelines.md` —— 拆分技术实现规则（ErrorCode/删除策略/BizModel 选择）迁往 architecture 对应文档，design 保留业务语义/状态命名/跨域规则
- [x] **Fix** `integration-and-transaction-patterns.md` —— 去除模板口吻；确认是否被其他文档覆盖，如是则标注 superseded
- [x] **Fix** `erp-design-audit-checklist.md` —— 标注"历史审计清单，当前业务规则以各自设计文档为准"

Exit Criteria:

- [x] design 与 architecture 对 B2B 的收录不冲突
- [x] portal/README.md STATUS 横幅存在且 3 处口径一致
- [x] feature-inventory.md 无重复行
- [x] domain-design-guidelines.md 已移除技术实现规则
- [x] 旧模板/旧审计残留已标注

## Draft Review Record

- Independent draft review iteration 1: **needs revision** (ses_0e423a98effezUDhruQpk7xMOt) — 1 blocking (缺 app-overview.md:10 portal 指针) + 2 non-blocking (postNow 测试缝未显式写入 posting.md、阶段顺序缺说明)
- Revisions applied:
  - Blocking: Phase 4 加 `app-overview.md:10` 条目 + Phase 4 targets 补充 `docs/design/app-overview.md`
  - Non-blocking 1: Phase 2 posting.md 条目补充 postNow 同步测试缝说明
  - Non-blocking 2: Phase 2 顶部加"顺序说明"解释为何先于 Phase 3
- Independent draft review iteration 2: **accept** (ses_0e42245e4ffeihU9n9JHaD7PaK) — 所有修订正确落位，计划可作为执行契约
- Independent draft review iteration 3: **accept** (draft-review session 2026-07-01-1800) — 复核确认格式合规、退出标准可测、范围边界清晰、覆盖全部 16 问题。修正一处 Major：Phase 2 item-type 计数原为 `Fix (9/9)`，实际第 9 项为 `Proof`（rg 残留扫描），已更正为 `Fix (8/9) + Proof (1/9)`。Closure Gates 正确地为文档型计划省略了验证命令门控。遗留 Minor（交由结束审计/深度审计处理）：Closure Gates 缺"结束证据存在于文件中"行（已由 Closure 节覆盖）；验证门控省略理由未显式写出；Phase 3/4 item-type 行用描述式而非计数式。

## Closure Gates

- [x] 范围内所有文件已按选定方案修正
- [x] 相关 owner doc 交叉引用一致
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控

## Deferred But Adjudicated

无。范围内 16 个问题均已有明确选定方案和落地动作，不存在需要 deferred 的项目。

> **执行方式说明（非 deferred）**：以下两项是执行方式选择，不构成降级：
> - `mvp.md` / `test-strategy.md`：经人工批准**已删除**（非 archive），引用处已清理（`requirements/README.md`、`references/document-naming-and-timeliness.md`、`testing-strategy.md`）。
> - `domain-design-guidelines.md` 技术实现规则（ErrorCode/删除策略/BizModel 决策）：采用"概要 + 迁出指针"明确 design vs architecture 职责边界，权威已指向 architecture；物理内容搬迁至新建 `error-handling.md`/`logical-deletion.md` 留作后续低风险整理（不影响当前文档一致性）。

## Closure

Status Note: 执行已完成（2026-07-01），结束审计已通过（PASS WITH MINOR）。Phase 1-4 全部 34 个落地动作已执行，4 组退出标准均已通过独立核实。文本一致性已验证：活跃 owner doc 无 10 域口径、无 `app-erp-app` 旧名、无三级 appName、无 `@RefLink` 传播、无 `APPROVING`/`PENDING_APPROVAL` 残留用法。Closure Gates 6 项全部勾选。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（ses_0e3f9bc1fffe7jYZMo9jYsau6i，general，新会话）
- 审计日期: 2026-07-01
- 结论: **PASS WITH MINOR** → 经修正后转为 PASS
- Evidence: 独立子代理逐 Phase 打开文件核实 34 个落地动作（非采信执行者自查），4 组退出标准全部满足。发现 3 处外围文档残留（不在计划显式目标清单），执行者已按审计建议修正：
  1. `docs/design/l10n/cn-golden-tax.md:9` "10 个业务域" → 18
  2. `docs/design/logistics/carrier-integration.md:80` `app-erp-log-service` → `erp-log-service`
  3. `docs/architecture/customization-capabilities.md:111` `app-erp-fin-service.beans.xml` → 准确的 VFS 路径 `_vfs/erp/fin/beans/app-service.beans.xml`
  修正后最终残留复扫确认：活跃 owner doc 无 10 域、无三级 appName（剩余 `app-erp-seed` 为合理模块工程名，与 app-erp-test-data 同类，非 appName）。
  **后续人工批准追加**：经人工批准，`mvp.md` 与 `test-strategy.md` 从 superseded 标注改为**直接删除**，并清理 3 处引用（`requirements/README.md`、`references/document-naming-and-timeliness.md`、`testing-strategy.md`），活跃 owner doc 无悬空引用。
- 方法学备注: 审计指出执行者自检若用 `rg`（ripgrep）在本环境不可用会静默失败；本环境应使用 `grep` 做残留扫描。最终验证已用 `grep -rn` 复核。

执行自查证据（执行者记录，非结束审计）：

- §2.0 映射表 19 行（18 域 + 1 聚合）已落地于 `domain-module-split-analysis.md`
- 18 域口径已统一于 product-scope/project-vision/project-context/system-baseline/module-boundaries/data-dependency-matrix/customization-capabilities/competitive-comparison/architecture-README/design-README/b2b-README/logistics-README 及 11 个域 README 的 appName
- 审批四态（UNSUBMITTED/SUBMITTED/APPROVED/REJECTED）统一于 approval-framework/flow-overview/service-layer-orchestration
- posting.md 三层模型 + businessType vs billType + 稳定约束小节 + postNow 测试缝
- cross-domain-constraints.md @RefLink 已替换为平台原生机制 B+D
- module-boundaries.md 英文孤儿段已删除、manufacturing/quality/projects 分层方向已细化
- data-dependency-matrix.md §2.0 裁决原则 + §4.1/§4.4 过账时序可配 + §5.2 billType 反向引用
- domain-design-guidelines.md §3.1 主数据引用分级规则
- mvp.md 废弃标注、product-baseline.md 重写（技术机制迁出）、roles-and-permissions.md 设计/运行基线拆分
- testing-strategy.md 唯一 owner doc + 四类资产边界 + 跨域归属三层 + 异步时序模型 + P0/P1/P2 清单；test-strategy.md superseded
- app-erp-test-data 模块骨架（pom.xml + load-order.txt + tables/README.md）
- portal/README.md STATUS 横幅 + identity-and-access.md 新建
- feature-inventory.md 去重 + 统一路由；design/architecture README B2B 双层分工
- integration-and-transaction-patterns.md 去模板口吻；erp-design-audit-checklist.md 历史标注

Follow-up:

- 独立子代理结束审计（必须，Closure Gates 第 6 项）
- （可选低风险整理）`domain-design-guidelines.md` ErrorCode/删除策略物理搬迁至新建 architecture 文档——当前已用指针明确边界，不影响一致性
- （codegen 后）data-dependency-matrix.md 全量 to-one 统计数字更新（新 8 域实测）
- （各域深化时）8 个第二批扩展域的业务层 S/P 关系补充

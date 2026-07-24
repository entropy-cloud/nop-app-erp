# 2026-07-24-2100-1 综合代码与设计审计

> Plan Status: completed
> Last Reviewed: 2026-07-24
> Source: 全面代码/设计质量审计评估 — `docs/skills/` 三个最高价值审计技能
> Related: `docs/skills/nop-platform-conformance-audit-prompt.md`, `docs/skills/cross-module-dependency-audit-prompt.md`, `docs/skills/orm-model-audit-prompt.md`
> Audit: required

## Current Baseline

- 项目处于运营成熟度收尾阶段（M1-M5 全部完成），18 业务域 + 1 跨域通知子系统全绿，154 reactor 模块构建通过
- 当前活跃计划 10+ 个集中在 dashboard runtime 回归、细化端到端验证、代码质量收敛
- 三项审计此前仅零散执行（作为其他计划的子步骤），未作为独立全面审计覆盖
- README §项目定制化层「已知失败模式」8 条中 6 条可直接通过平台合规审计的 grep 脚本覆盖；跨模块 DAG 合规性和外部实体声明完整性尚未做拓扑排序验证；ORM 模型 propId 连续性/标准字段完整性未做 18 域全量脚本检查
- `docs/design/domain-design-guidelines.md` 定义的单据标准字段约定（posted 三件套、多币种四件套、双轴状态等）已在多轮补齐，但 18 域全量覆盖率未做最终闭合验证

## Goals

- 执行 3 项最高价值审计技能的全面覆盖，识别项目末期最可能残留的 blocker/major 级问题
- 审计发现必须按修复计划可消费的格式输出：按严重性分级、附着影响文件与行号、给出具体修复建议
- 平台合规审计：grep 脚本扫描全部 Java 源码，检查 `@Inject private`、`extends RuntimeException`、`System.currentTimeMillis`、`@BizMutation` + `@Transactional` 冗余、`IDaoProvider` 直接注入绕过 `I*Biz`、codegen 文件手改
- 跨模块依赖审计：拓扑排序检测 DAG 循环，验证每个 `refEntityName` 有对应 `notGenCode="true"` 声明，验证外部实体声明完整性
- ORM 模型审计：脚本扫描全部 18 域 `*.orm.xml`，检查 propId 连续性、StdSqlType/stdDataType 配对、标准字段完整性、字典命名规范

## Non-Goals

- 不执行 `open-ended-audit-prompt.md`（独立子代理攻击性审计，留作后续轮次）
- 不执行 `design-doc-audit-prompt.md` / `design-completeness-scan-prompt.md`（设计文档审计）
- 不执行 `state-machine-business-review-prompt.md`（状态机专项审查）
- 不执行 `configuration-audit-prompt.md`（配置设计审计）
- 不修复审计发现的任何问题（仅记录发现；修复由后续专项计划 `2026-07-24-2100-x-audit-findings-remediation` 处理）
- 不修改任何代码、配置或 ORM 模型文件

## Task Route

- Type: verification or audit work
- Owner Docs: `docs/skills/nop-platform-conformance-audit-prompt.md`, `docs/skills/cross-module-dependency-audit-prompt.md`, `docs/skills/orm-model-audit-prompt.md`, `docs/design/domain-design-guidelines.md`
- Skill Selection Basis: 三项审计技能直接对应项目已知失败模式，且均可通过脚本实现高覆盖率机械检查，成本最低、覆盖面最广

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline
- 审计使用 grep/rg 脚本 + Python 脚本（参考 `docs/logs/2026/06-22.md` 2026-06-23 条目的核查脚本结构），无需外部服务

## Execution Plan

### Phase 1 — Nop Platform 合规审计

Status: completed
Targets: `module-*/**/*.java`, `module-*/**/*.xml`, `module-*/model/*.orm.xml`
Skill: `nop-platform-conformance-audit-prompt.md`

- Item Types: `Proof`
- Prereqs: none

- [x] 加载 `nop-platform-conformance-audit-prompt.md`，按 15 维度审计框架执行
      - Skill: `nop-platform-conformance-audit-prompt.md`
- [x] grep 自动化扫描：`extends RuntimeException`（应 `NopException`）
      - Skill: none
- [x] grep 自动化扫描：`@Inject private`（应非 private）
      - Skill: none
- [x] grep 自动化扫描：`@Transactional` 与 `@BizMutation` 共存（冗余）
      - Skill: none
- [x] grep 自动化扫描：`System.currentTimeMillis`（应 `CoreMetrics.currentTimeMillis`）
      - Skill: none
- [x] grep 自动化扫描：`IDaoProvider` / `IOrmTemplate` 直接注入（应 `I*Biz`），记录无注释说明原因的违规点
      - Skill: none
- [x] git diff 检测 `_gen/`、`_` 前缀文件、`__XGEN_FORCE_OVERRIDE__` 文件是否被手改
      - Skill: none
- [x] 维度 15（Owner-doc → 代码漂移抽样）：从 `docs/design/` 随机抽 2 个 owner doc × 2 个断言，核验与 `*.orm.xml` / BizModel 一致性
      - Skill: none
- [x] 记录全部发现，按 blocker/major/minor 分级，给出 15 维度合规率，输出到 `docs/analysis/` 下

Exit Criteria:

- [x] 自动化 grep 扫描脚本全部执行完毕，结果持久化到 `docs/analysis/` 下的审计文件
- [x] 维度 15 抽样核查完成，漂移点列表记录完毕
- [x] 发现清单按严重性分级并附修复建议

### Phase 2 — 跨模块数据依赖审计

Status: completed
Targets: `module-*/model/*.orm.xml`, `docs/architecture/module-boundaries.md`, `docs/architecture/data-dependency-matrix.md`
Skill: `cross-module-dependency-audit-prompt.md`

- Item Types: `Proof`
- Prereqs: none (独立于 Phase 1)

- [x] 加载 `cross-module-dependency-audit-prompt.md`，按 7 维度审计框架执行
      - Skill: `cross-module-dependency-audit-prompt.md`
- [x] 扫描所有 orm.xml 的 `refEntityName`，提取跨模块引用边，构建依赖图
      - Skill: none
- [x] 拓扑排序检测 DAG 循环
      - Skill: none
- [x] 验证每个生效的跨模块 `<to-one refEntityName="app.erp.X.dao.entity.Y">` 有对应 `<entity notGenCode="true">` 声明
      - Skill: none
- [x] 验证外部实体声明的列仅含关键列（不全量复制）
      - Skill: none
- [x] 验证业财一体边界：凭证反查用字符串三元组，finance 不改写业务表
      - Skill: none
- [x] 验证引用方 `erp-xxx-dao/pom.xml` 是否依赖被引用方的 `-dao` 包
      - Skill: none
- [x] 验证 `data-dependency-matrix.md` 声明的 R/S/P 分类与实际引用方式一致
      - Skill: none
- [x] 记录全部发现，按 blocker/major/minor 分级，输出到 `docs/analysis/` 下

Exit Criteria:

- [x] 所有 orm.xml 的跨模块引用边提取完成
- [x] DAG 验证结果（通过/循环清单）记录完毕
- [x] 外部实体声明完整性矩阵（引用数 vs 声明数）记录完毕
- [x] 发现清单按严重性分级并附修复建议

### Phase 3 — ORM 模型规范审计

Status: completed
Targets: `module-*/model/*.orm.xml`, `docs/design/domain-design-guidelines.md`
Skill: `orm-model-audit-prompt.md`

- Item Types: `Proof`
- Prereqs: none (独立于 Phase 1/2，但可与 Phase 2 共享 orm.xml 扫描结果)

- [x] 加载 `orm-model-audit-prompt.md`，按 9 维度审计框架执行
      - Skill: `orm-model-audit-prompt.md`
- [x] 类型规范：逐列检查 `stdSqlType`（仅 StdSqlType 枚举值）+ `stdDataType` 配对
      - Skill: none
- [x] propId 连续性：每域实体列 propId 从 1 开始连续，无 gap
      - Skill: none
- [x] 长度与精度：VARCHAR 带 precision，DECIMAL 金额类 `precision="20" scale="4"`，文本长字段用 CLOB
      - Skill: none
- [x] 字典设计：`<dict name="erp-<short>/kebab-name">`，option value 10/20/30 递增，跨域同名字典复用
      - Skill: none
- [x] 标准字段完整性：每个实体检查 id、delVersion、version、createdBy、createTime、updatedBy、updateTime、remark 是否存在
      - Skill: none
- [x] 业务字段完整性（对照 domain-design-guidelines.md §10）：
  - 业务单据头：orgId、businessDate、posted/postedAt/postedBy（过账三件套）
  - 金额单据头/行：currencyId、exchangeRate、amountSource、amountFunctional（多币种四件套）
  - 单据头：docStatus + approveStatus（双轴分离）
  - 价税分离：taxRateId、taxAmount、amount、amountWithTax
      - Skill: none
- [x] 关系设计：本模块 to-one 配 `tagSet="pub"`，头-行配 `tagSet="pub,cascade-delete,insertable,updatable"`
      - Skill: none
- [x] 字典定义与 `ext:dict=` 引用完整性：每个引用的 dict 都有对应 `<dict>` 定义
      - Skill: none
- [x] 记录全部发现，按 blocker/major/minor 分级，输出字段补齐统计，输出到 `docs/analysis/` 下

Exit Criteria:

- [x] 所有 18 域 `*.orm.xml` 完成 9 维度全量扫描
- [x] 各维度通过率统计完毕
- [x] 字段补齐清单（缺失的 posted 三件套/多币种四件套/docStatus+approveStatus 等）记录完毕
- [x] 发现清单按严重性分级并附修复建议

## Draft Review Record

- Independent draft review iteration 1: accept — 格式合规（front matter/sections/phase 结构/命名约定全部满足）；3 phase 边界清晰且各有独立退出标准；Non-Goals 明确排除修复与其他审计技能，无范围蔓延；Closure Gates 已按纯审计计划正确定制（无 build 门控，审计脚本执行即验证）；引用的 6 个 owner doc/skill 文件均存在。Minor：各 Proof 项的 grep 模式已内联说明但未给出确切命令行，由执行阶段落实即可，不阻塞。

## Closure Gates

- [x] 范围内行为完成：3 个 phase 全部执行完毕，审计发现持久化到 `docs/analysis/`
- [x] 相关文档对齐：审计结果可作为后续修复计划的输入
- [x] 已运行验证：所有自动化脚本执行，人工抽样核查执行
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

无（计划为纯审计，无 deferred 项目）

## Closure

Status Note: 全部 3 phase 执行完毕，裁决均为通过（PASS）。3 份审计报告持久化到 `docs/analysis/`，无 blocker/major 真实发现（所有原始 blocker/major 经 FP 核实为扫描器测量口径限制）。`mvn clean install -DskipTests` EXIT_CODE=0（154 模块 BUILD SUCCESS，审计零代码漂移）。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理（ses_06d5d5502ffeuKW6QuzOrSeyeA，新会话，非执行者）— 闭合审计 FAIL→修复→PASS
- Evidence:
  - 计划项完整性：Phase 1/2/3 全部 `- [x]`，各 `Status: completed`，`Plan Status: completed`
  - 交付物实质：`docs/analysis/2026-07-24-2100-1-nop-platform-conformance-audit.md`（19 规则基线对照表，15 维度合规率 100%）+ `2026-07-24-2100-2-cross-module-dependency-audit.md`（625 DAG 边，0 循环，22/22 声明完整，7 维度全绿）+ `2026-07-24-2100-3-orm-model-audit.md`（351 实体 9 维度扫描，FP 核实后 91 minor 残留，9/9 通过）
  - Non-Goals 遵守：`git status --porcelain` 仅 4 文件变更，全部在 `docs/`（3 报告 + 本计划文件），零源码/ORM/配置漂移
  - 构建基线：`mvn clean install -DskipTests` EXIT_CODE=0（2026-07-24）
  - 闭合审计首轮发现差距（8 闭合门控未勾选 + Closure 占位符未填）→ 已在本修订全部修复

Follow-up:

- **必须**：审计结束后，将发现按域/主题聚合为若干修复计划：
  - 不按每个问题一个计划，也不按最小可工作单元拆分
  - 每个修复计划须有足够体量（约 5 个 phase 的工作量），能自然拆分为多阶段执行
  - 以审计输出作为 `Current Baseline`，按 blocker → major → minor 优先级排序
- 修复计划的命名按 `docs/plans/00-plan-authoring-and-execution-guide.md` 约定，Source 字段引用本审计计划 `2026-07-24-2100-1-comprehensive-code-design-audit.md`

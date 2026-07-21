# 2026-07-21-2225-3-business-module-metadata-bt5 业务模块元数据 BT5 风格（D2）

> Plan Status: active
> Last Reviewed: 2026-07-21
> Source: `docs/backlog/deepening-roadmap.md` §Milestone D / D2（`todo`）；`docs/analysis/erp-survey/2026-07-20-post-survey-strategic-gaps.md` §3.12；`docs/analysis/erp-survey/2026-07-20-0000-erp5-compare.md` §4 / 建议 3
> Related: `docs/architecture/external-api-integration-pattern.md`（§1 主题边界声明）、`docs/architecture/domain-module-split-analysis.md`（模块链）、`docs/plans/2026-07-21-1206-3-external-api-integration-reference-pattern.md`（D1 引用 D2 为独立主题）
> Audit: required

## Current Baseline

每个业务域的 meta 模块（`module-<domain>/erp-<short>-meta/`）经 `precompile/gen-meta.xgen` 从 ORM 根 `ext:` 属性生成 `_module-meta.json`，当前**仅 4 个最小字段**：

```json
{
  "moduleId": "erp/pur",
  "moduleName": "erp-pur",
  "appName": "erp-pur",
  "icon": "shopping-cart"
}
```

**ORM 根已用 ext: 属性**（grep `module-purchase/model/app-erp-purchase.orm.xml` 核实，`<orm>` 根上约 11 个 + 子元素上 `ext:dict`/`ext:estRows`）：`ext:registerShortName` / `ext:appName` / `ext:basePackageName` / `ext:entityPackageName` / `ext:mavenGroupId` / `ext:mavenArtifactId` / `ext:mavenVersion`（值 `1.0-SNAPSHOT`）/ `ext:platformVersion`（值 `2.0.0-SNAPSHOT`）/ `ext:dialect` / `ext:allowIdAsColName` / `ext:icon`。其中 `ext:mavenVersion` / `ext:platformVersion` 是版本相关既有属性——本计划新增的业务 `version` 与之**语义独立**（Maven 技术版本 vs 业务版本），见 Decision B。

**缺失（对照 ERP5 BT5 + survey §3.12）**：

- 无模块**业务版本**（ERP5 BT5 自带 `version`，如 `erp5_trade 5.4.3`）；既有 `ext:mavenVersion=1.0-SNAPSHOT` 不表达业务变更语义
- 无**业务依赖声明**（ERP5 BT5 `bt/dependency_list` 声明 `erp5_core >= 5.4.3`；nop 当前仅 Maven POM 编译期依赖，无业务级版本约束）
- 无**可选特性声明**（哪些子功能可独立开关，如 finance 的 `budget-commitment-enabled` / `budget-roll-forward-enabled` 等 config-gated 特性散落在各域 `ErpXxxConfigs.java`，未在模块元数据中声明）
- 无**运行时元数据读取器**（拓扑校验、升级前依赖完整性检查、特性清单查询均无基础设施）

**Why P2**（survey §3.12）：随模块数增长（已 18+1 域）+ config-gated 特性散布，显式的业务依赖/版本/特性元数据对升级安全、多租户版本管理、SaaS 部署场景变得必要。是对既有 `module-meta.json` 生成管线的**轻量增强**（survey 明确 "Lightweight addition to existing module-meta.json pipeline"）。

**邻近能力**（不重复造）：Maven POM 已提供编译期依赖；本计划补充**业务级**版本/依赖/特性（不同粒度，互补不替代）。

**roadmap 框架约束**（deepening-roadmap §6 D2 行）：D2 的 ORM 变更标注 = "**否** — module-meta.json 变更"。且 D2 **不在** §8「ORM 变更已授权」清单内（仅 A1/A2/A3/B1/C2 在列）。故本计划**严格避免修改 ORM 源模型**（`<domain>/model/*.orm.xml`），新字段来源改用 meta 模块内独立手写源文件（见 Decision C），与 roadmap 框架一致并规避 `project-context.md §AI 阻塞条件` 的 ORM 硬停止。

## Goals

- **G1 — owner doc NEW**：`docs/architecture/business-module-metadata.md` 定义 module-meta.json 扩展 schema（version + businessDependencies + optionalFeatures）、字段语义、生成管线扩展点、运行时读取器契约、与 Maven POM 的边界
- **G2 — 扩展生成管线**：在 ORM 根 `ext:` 属性 + `gen-meta.xgen` 模板层面支持新字段（向后兼容：未声明新字段的模块生成结果不变）
- **G3 — 运行时元数据读取器**：实现一个跨模块元数据读取/校验服务（拓扑校验业务依赖完整性、特性清单查询），供升级/部署/诊断场景使用
- **G4 — 应用到全 18+1 域 + 反模式自检**：在所有 meta 模块落地产出含新字段的 `_module-meta.json`，并在 owner doc 产出反模式自检表（如禁止把 Maven 依赖复制到 businessDependencies、禁止在 optionalFeatures 重复 config key 等）

## Non-Goals

- **不**实现运行时插件热管理（那是 D4，独立 P3 可行性研究；本计划仅提供元数据读取器，不提供 enable/disable 生命周期）
- **不**实现版本约束求解器（如 SemVer 范围解析）；首版 `businessDependencies` 仅做**存在性 + 精确版本匹配**校验，范围求解归 successor
- **不**改造 Maven 模块结构或多模块构建顺序（业务依赖元数据不影响编译期）
- **不**实现多租户/SaaS 版本管理编排（owner doc 仅声明字段语义，编排归独立 successor，触发条件=SaaS 部署需求）
- **不**修改既有 4 字段的生成（向后兼容；新字段全部 optional）

## Task Route

- Type: `architecture change`（跨 18+1 域 meta 模块的元数据扩展 + 运行时读取器，更改模块组装层的可观测元数据，非用户可见行为）
- Owner Docs: `docs/architecture/business-module-metadata.md`（**NEW**）；交叉回链 `docs/architecture/domain-module-split-analysis.md`、`docs/architecture/README.md`
- Skill Selection Basis: 涉及 codegen 模板（`gen-meta.xgen`，xpl/xgen）+ 跨模块 Java 服务（运行时读取器）→ `nop-backend-dev`（决策门、xbiz、IoC 注入、跨实体调用、错误码、产品化可定制性自检）。codegen 模板属于平台机制，按 `nop-backend-dev` 路由到 nop-entropy 平台文档参考。无前端工作 → `nop-frontend-dev` 不匹配。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（无新端口/密钥/外部服务）
- codegen 模板变更需 `mvn clean install -DskipTests` 触发增量再生（见 project-context 验证命令）

## Execution Plan

### Phase 1 - owner doc NEW + schema 裁决

Status: planned
Targets: `docs/architecture/business-module-metadata.md`（NEW）
Skill: `nop-backend-dev`

- Item Types: `Decision | Add`
- Prereqs: 无

- [ ] **Decision A — schema 字段集**：裁决 module-meta.json 扩展字段。候选：(a) `version`（字符串，模块业务版本）+ `businessDependencies`（数组，每项 `{moduleId, version?}`）+ `optionalFeatures`（数组，每项 `{feature, configKey, default}`）；(b) 更细粒度含 `minPlatformVersion`；(c) 更粗仅 version + businessDependencies。**预期裁决=a**（首版轻量，向后兼容），记录选择 + 替代方案 + 残留风险（范围求解 successor）
  - Skill: `none`
- [ ] **Decision B — 版本号约定**：裁决业务版本号格式与既有 `ext:mavenVersion=1.0-SNAPSHOT` 的关系。候选：(a) SemVer `MAJOR.MINOR.PATCH`，**独立于** `ext:mavenVersion`（理由：Maven SNAPSHOT 不表达业务变更，技术版本与业务版本解耦）；(b) 派生自 `ext:mavenVersion`；(c) 派生自 git tag。**预期裁决=a**，记录理由 + 与既有 13 个 ext: 属性的边界（本计划只新增业务 `version`，不触碰既有 ext:mavenVersion/platformVersion）
  - Skill: `none`
- [ ] **Decision C — 生成管线扩展点（源文件位置）**：裁决新字段从何而来。候选：(a) meta 模块内独立手写源文件 `module-<domain>/erp-<short>-meta/precompile/module-meta.yaml`（与 ORM 源模型解耦，`gen-meta.xgen` 在读 ORM 根之后叠加此文件），**预期裁决=a**；(b) ORM 根 `ext:version` 等新属性——**否决**：D2 不在 deepening-roadmap §8 ORM 授权清单，加 ext: 属性属 ORM 源模型变更触发 AGENTS.md 硬停止，与 roadmap「D2 仅 module-meta.json 变更」框架冲突；(c) POM 派生。记录选择 + 否决理由 + 残留风险（手写 yaml 与 ORM 根 moduleId 须保持一致，由 gen-meta.xgen 校验）
  - Skill: `nop-backend-dev`
- [ ] **Decision D — 读取器归属模块**：裁决运行时读取器实现位置。候选：(a) `app-erp-all`（聚合 app，已知全部模块）；(b) 新建 `module-sys` 风格基础设施模块；(c) `nop-entropy` 平台层。**预期裁决=a**（应用层元数据归应用，避免改平台），记录理由
  - Skill: `nop-backend-dev`
- [ ] **Add**：NEW `docs/architecture/business-module-metadata.md`，含 8 节：目的与范围（与 Maven POM/平台 module-meta.json 的边界）/ schema 字段表（Decision A）+ 版本号约定（Decision B）/ 生成管线扩展（Decision C，含 meta 模块 `precompile/module-meta.yaml` 手写源 + `gen-meta.xgen` 叠加读取改造点）/ 运行时读取器契约（Decision D）/ 与 ERP5 BT5 对照（dependency_list / version / BusinessTemplate 机制）/ 与 D4 插件热管理的关系（D2 提供元数据信息输入，D4 提供生命周期，**非硬依赖**——D4 仅由 D1 解锁，见 roadmap §7 mermaid）/ 反模式自检表 / 落地证据
  - Skill: `none`

Exit Criteria:

> 本阶段交付 owner doc + 4 项 Decision，解除 Phase 2/3 阻塞。无需仓库级验证。

- [ ] `docs/architecture/business-module-metadata.md` 存在且含 8 节骨架
- [ ] 4 项 Decision（A/B/C/D）均在 owner doc 内记录选择 + 替代方案 + 残留风险
- [ ] meta 模块 `precompile/module-meta.yaml` 手写源约定 + `gen-meta.xgen` 叠加读取改造点已显式列出（**不**触 ORM 源模型）

### Phase 2 - 生成管线扩展 + 运行时读取器

Status: planned
Targets: `module-<domain>/erp-<short>-meta/precompile/gen-meta.xgen`、`module-<domain>/erp-<short>-meta/precompile/module-meta.yaml`（新源文件，**不**触 ORM）、读取器（按 Decision D 归属）
Skill: `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 1 Decision A/C/D 裁决完成

- [ ] **Add**：扩展 `gen-meta.xgen` 模板在读完 ORM 根后**叠加读取** meta 模块内手写源 `precompile/module-meta.yaml`（若存在），合并到 `_module-meta.json` 输出（向后兼容：yaml 缺失时生成结果与现状一致，不输出新 JSON key）；模板内置一致性校验：yaml 中 moduleId 须与 ORM 根一致，否则 codegen 期报错
  - Skill: `nop-backend-dev`
- [ ] **Add**：运行时读取器（按 Decision D 归属，预期 `app-erp-all`）：扫描 classpath 全部 `_module-meta.json` → 反序列化 → 提供查询 API（`listModules` / `getModule(moduleId)` / `checkDependencyIntegrity` / `listOptionalFeatures`）；`checkDependencyIntegrity` 校验 `businessDependencies` 引用的 moduleId 存在 + 精确版本匹配（首版不做范围求解，归 successor）
  - Skill: `nop-backend-dev`
- [ ] **Add**：错误码（如 `ERP_MODULE_DEPENDENCY_MISSING` / `ERP_MODULE_VERSION_MISMATCH`）+ 诊断 `@BizQuery` 暴露（供运维/升级前检查）
  - Skill: `nop-backend-dev`
- [ ] **Proof**：单测覆盖读取器（多模块 classpath 扫描 + 依赖完整性正负路径 + 特性清单查询 + 缺失字段向后兼容）；codegen 模板变更在 1 个试点模块（purchase，新建 `erp-pur-meta/precompile/module-meta.yaml`）验证生成新字段 JSON + 一致性校验负路径（moduleId 不匹配时 codegen 期报错）
  - Skill: `nop-backend-dev`

Exit Criteria:

> 本阶段交付可用的生成管线扩展 + 读取器 + 单测，解除 Phase 3 全域应用阻塞。仅需本模块 + 试点模块 localized 验证。

- [ ] gen-meta.xgen 扩展（叠加读 yaml，**不**触 ORM）+ 读取器 + 错误码实现落盘，单测全绿（含向后兼容场景 + 一致性校验负路径）
- [ ] 试点模块（purchase）新建 `erp-pur-meta/precompile/module-meta.yaml` + codegen 生成含新字段的 `_module-meta.json`（localized codegen 验证）
- [ ] 读取器所在模块 `mvn test` 通过（localized）

### Phase 3 - 全 18+1 域应用 + owner doc 回链 + roadmap 同步

Status: planned
Targets: 全 `module-<domain>/erp-<short>-meta/precompile/module-meta.yaml`（新建手写源，**不**触 ORM 源模型）、全 `_module-meta.json` 再生、`docs/architecture/business-module-metadata.md` 落地证据
Skill: `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 2 生成管线 + 读取器落地

- [ ] **Add**：为全 18+1 域 meta 模块新建 `precompile/module-meta.yaml` 手写源，填入：(1) `version`（初始业务版本，如 `1.0.0`）；(2) `businessDependencies`——**有跨域调用的模块声明**（按 `docs/architecture/domain-module-split-analysis.md` 真实跨域关系，如 finance→[master-data, inventory]、manufacturing→[master-data, inventory, finance]），**无跨域调用的独立模块省略此字段**（如 master-data 不声明）；(3) `optionalFeatures`——从各域 `Erp*Configs.java` 的 config-gated 特性清单抽取（如 finance 的 budget-commitment/roll-forward/cost-adjust-approval/landed-cost-enabled 4 开关）。owner doc 枚举「声明 vs 省略」两类模块清单
  - Skill: `nop-backend-dev`
- [ ] **Add**：触发 `mvn clean install -DskipTests` 增量再生全部 `_module-meta.json`，确认 19 个文件均含新字段且 JSON well-formed；全 workspace BUILD SUCCESS（codegen 管线扩展 + 19 yaml 源不破坏既有 154 模块基线）
  - Skill: `nop-backend-dev`
- [ ] **Proof**：读取器 `checkDependencyIntegrity` 在聚合 app 运行时返回全绿（无 missing/mismatch）；抽取 2-3 域交叉核对生成的 businessDependencies 与 `domain-module-split-analysis.md` 真实跨域依赖一致
  - Skill: `nop-backend-dev`
- [ ] **Add**：owner doc `docs/architecture/business-module-metadata.md` 补「落地证据」节（生成管线变更 + 19 yaml 源清单 + 声明/省略分类 + 跨域依赖矩阵抽样 + 读取器 API + 测试基线）；交叉回链 `docs/architecture/domain-module-split-analysis.md` + `docs/architecture/README.md` Initial Owner Docs 段追加介绍行
  - Skill: `none`
- [ ] **Add**：`docs/backlog/deepening-roadmap.md` D2 行 `todo → done` + §8.x 落地证据段（plan / owner doc NEW / 生成管线 + 读取器 / 19 yaml 源应用 / 测试基线 / deferred successor 含 D4 关系说明）
  - Skill: `none`

Exit Criteria:

- [ ] 全 18+1 域 meta 模块含新 `precompile/module-meta.yaml` 手写源（**不**触 ORM 源模型）+ 19 个 `_module-meta.json` 再生含新字段且 well-formed
- [ ] 全 workspace `mvn clean install -DskipTests` BUILD SUCCESS（验证 codegen 管线扩展不破坏 154 模块基线）
- [ ] 读取器依赖完整性校验全绿 + 跨域依赖矩阵抽样与架构文档一致
- [ ] owner doc 落地证据段 + README 回链 + roadmap §8.x 落盘

## Draft Review Record

- Independent draft review iteration 1: `needs-revision`（`ses_07ae8fa8affeJEEx3kxh2pGKWj`，2026-07-21）— 2 项阻塞：(B1) Decision C 拟加 ORM 根 ext: 属性，但 D2 不在 §8 ORM 授权清单且 roadmap 标「否 — module-meta.json 变更」→ 与 AGENTS.md 硬停止冲突；(B2) Current Baseline 称 ORM 根仅 4 ext: 属性，实际 11+ 个（含 ext:mavenVersion/platformVersion）。另 4 项非阻塞（D2→D4 依赖过强 / Phase 4 冗余 /「按需」反松弛 / Decision C 缺残留风险）。本次修订已应用：(1) Decision C 改裁=meta 模块内手写 `module-meta.yaml` 源（不触 ORM），附否决 ext: 方案的理由；(2) Baseline ext: 列表更正含版本相关属性，Decision B 显式声明与 ext:mavenVersion 语义独立；(3) D2→D4 改「信息输入非硬依赖」；(4) 删 Phase 4，全仓库 BUILD SUCCESS 合并到 Phase 3 Exit Criteria + Closure Gates；(5)「按需」改为「有跨域调用声明 / 独立模块省略」具体规则 + owner doc 枚举清单；(6) Decision C 附残留风险（yaml moduleId 一致性）。待 iteration 2 复审。
- Independent draft review iteration 2: `acceptable-as-is`（`ses_07ae0d596ffeiqZtYkU8CRkyOY`，2026-07-21）— 两项阻塞均已解（Decision C 改 meta yaml 源 + 明确否决 ext: 方案含授权理由 + Phase 2/3 Targets 不触 ORM + Baseline 含 11+ ext: 含 mavenVersion/platformVersion + Decision B 声明语义独立；均经 gen-meta.xgen 与 ORM 根 spot-check 核实）。2 项非阻塞文本微调（Closure Status Note 残留「Phase 1-4」+ ext: 根计数 11 vs 13）已在本次微调应用。结论：可接受为执行契约，置 `active`。

## Closure Gates

> 在所有 Phase Exit Criteria 与下方门控全勾选后关闭。结束时运行一次全仓库 `mvn clean install -DskipTests` + 读取器模块 `mvn test`。

- [ ] 范围内行为完成（owner doc NEW + 生成管线扩展 + 读取器 + 19 个 meta 模块 yaml 源应用；**不**触 ORM 源模型）
- [ ] 相关文档对齐（business-module-metadata.md NEW + domain-module-split-analysis.md 交叉回链 + architecture/README.md 介绍行 + roadmap §8.x）
- [ ] 已运行验证：全 workspace `mvn clean install -DskipTests` BUILD SUCCESS + 读取器模块 `mvn test` 全绿 + 19 个 `_module-meta.json` well-formed（`python -m json.tool` 或 jq 校验）
- [ ] 无范围内项目降级为 deferred/follow-up（版本范围求解、SaaS 编排、插件热管理 D4 显式归 successor 并命名触发条件）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### 版本范围求解器（SemVer range resolution）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 首版 `businessDependencies` 仅做存在性 + 精确版本匹配，满足升级前完整性检查；范围求解为增量能力
- Successor Required: `yes`（触发条件：模块业务版本数 > 3 且出现不兼容升级场景）

### SaaS 多租户版本管理编排

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: owner doc 仅声明字段语义；SaaS 编排涉及租户级模块版本组合，属不同结果表面
- Successor Required: `yes`（触发条件：SaaS 多租户部署需求 + 平台 tenant-model 集成授权）

### D4 插件热管理研究（runtime enable/disable lifecycle）

- Classification: `watch-only residual`
- Why Not Blocking Closure: D2 仅提供元数据读取器；D4（roadmap §Milestone D，独立 P3 可行性研究）评估 OSGi-style vs Maven module isolation vs NocoBase-style 插件管理器在 Nop Platform 上的可行性。D2 的模块边界元数据**可作为 D4 评估的信息输入（非硬依赖）**；D4 仅由 D1 解锁（roadmap §7 mermaid `D1 --> D4` 已确认）
- Successor Required: `yes`（触发条件：业务客户插件热管理需求 + D4 启动指令）

## Closure

Status Note: 待 Phase 1-3 全部完成 + 独立结束审计通过后填写。

Closure Audit Evidence:

- Auditor / Agent: `<independent auditor — TBD>`
- Evidence: `<task id / log link / walkthrough record — TBD>`

Follow-up:

- 版本范围求解器（见 Deferred，触发条件已命名）
- SaaS 多租户版本管理编排（见 Deferred，触发条件已命名）
- D4 插件热管理研究（见 Deferred，D2 提供信息输入；D4 仅由 D1 解锁）

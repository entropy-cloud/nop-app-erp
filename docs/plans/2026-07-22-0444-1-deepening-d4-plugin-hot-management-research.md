# 2026-07-22-0444-1 deepening D4 — Plugin Hot Management Feasibility Research

> Plan Status: completed
> Last Reviewed: 2026-07-22
> Source: `docs/backlog/deepening-roadmap.md` §Milestone D — D4 (P3 feasibility study)
> Related: `docs/plans/2026-07-21-1206-3-external-api-integration-reference-pattern.md` (D1 done), `docs/plans/2026-07-21-2225-3-business-module-metadata-bt5.md` (D2 done)
> Audit: required

## Current Baseline

- **deepening-roadmap 10/11 done**：Milestone A（A1/A2/A3）、B（B1）、C（C1/C2/C3）、D（D1/D2/D3）全部 done；仅 D4（Plugin Hot Management Research）为 `todo`，是整个路线图最后一项。
- **D4 前置已满足**：§7 依赖图 `D1 --> D4`，D1 done 提供 3 个集成案例（logistics Carrier Gateway / b2b EDI / master-data 汇率 API client）；D2 done 提供 `module-meta.json`（version + businessDependencies + optionalFeatures）+ `ModuleMetaReader` 运行时读取器，作为插件元数据信息输入。
- **当前插件/模块管理现状**：
  - 19 个业务域均采用 Maven 多模块结构（`module-<domain>/erp-<short>-{dao,meta,service,web,app,api}`），编译期依赖。
  - 无运行时插件热加载/热卸载机制；新增域需 `nop-cli gen` + `mvn install` 重新构建全 workspace。
  - `app-erp-all` 聚合启动包包含全部 19 域；无按需加载或动态启用/禁用域的能力。
  - D2 `ModuleMetaReader` 已具备运行时扫描 `_module-meta.json` + 依赖完整性校验（存在性 + 精确版本匹配）能力，但仅只读诊断，无写入/启停能力。
- **Nop Platform 层现状**（`nop-entropy`）：平台核心不提供 OSGi 容器或插件热管理框架；IoC 容器为 Quarkus CDI（编译期/启动期 bean 发现），非运行时动态注册。
- **对标参考**（来自 post-survey-strategic-gaps.md）：OFBiz 采用组件热加载（component-load.xml）；ERP5 采用 BT5 Business Template（导出/导入/安装/升级可追溯）；NocoBase 采用插件管理器（npm 插件 + 运行时启用/禁用）。
- **roadmap 对 D4 的定位**：`ORM 变更 = 否`，纯可行性研究（P3），交付物 = `docs/analysis/plugin-hot-management-research.md`（**NEW**）。

## Goals

- 产出一份可执行的可行性研究报告 `docs/analysis/plugin-hot-management-research.md`，对比 3 种插件热管理路径在 Nop Platform 上的可行性与代价：
  1. **OSGi-style**：动态 bundle 加载/卸载（Felix/Felix-equinox）
  2. **Maven module isolation**：编译期模块隔离 + 启动期可选装载（不改运行时，改构建/部署拓扑）
  3. **NocoBase-style plugin manager**：应用层插件描述符 + 运行时启用/禁用（不卸载类，仅路由/菜单/权限级开关）
- 给出**推荐路径**（含推荐理由、残留风险、触发采用的事件/条件），供业务客户/架构决策参考。
- 明确每条路径对 Nop Platform 核心、Quarkus 启动模型、GraphQL schema 生成、代码生成管线的**侵入面**。
- 关闭 deepening-roadmap D4，使路线图 11/11 done。

## Non-Goals

- 实现任何插件系统代码（本计划为研究/分析，不落地生产代码）。
- 修改 Nop Platform 核心（nop-entropy）。
- 修改 Maven 构建结构或 `app-erp-all` 打包方式。
- 修改任何 ORM 模型、api.xml 或 owner doc 的业务语义。
- 推广到全 19 域的插件化改造（超出可行性研究范围）。
- 版本范围求解器（SemVer range resolution，D2 Deferred successor，独立触发条件）。
- SaaS 多租户版本管理编排（D2 Deferred successor，独立触发条件）。

## Task Route

- Type: `analysis / feasibility study`（纯文档，无生产代码变更）
- Owner Docs: `docs/architecture/business-module-metadata.md`（D2 owner doc，元数据信息输入）、`docs/architecture/external-api-integration-pattern.md`（D1 owner doc，集成案例输入）、`docs/architecture/domain-module-split-analysis.md`（模块拆分基线）
- Skill Selection Basis: 无匹配技能（纯研究/分析工作，不涉及 BizModel/view.xml/ORM/测试）。`Skill: none`。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（纯文档工作，无端口/密钥/外部服务依赖）。

## Execution Plan

### Phase 1 - 3 路径调研 + 对比矩阵 + 推荐裁决

Status: completed
Targets: `docs/analysis/plugin-hot-management-research.md` (**NEW**)
Skill: none

- Item Types: `Decision | Add`
- Prereqs: D1 done、D2 done（均已落地，提供输入）

- [x] `Decision`: 研究范围边界裁决 — 明确"插件热管理"在本项目语境下的定义（运行时类加载/卸载 vs 启动期可选装载 vs 应用层路由级开关），避免三路径不可比
  - Skill: none
- [x] `Add`: 路径 1 OSGi-style 调研段 — 在 Quarkus + CDI 启动模型下引入 OSGi 容器的可行性、类加载器隔离代价、GraphQL schema 动态注册障碍、与 Nop 代码生成管线（`_gen/` 增量链）的冲突面
  - Skill: none
- [x] `Add`: 路径 2 Maven module isolation 调研段 — 编译期模块隔离 + 构建拓扑可选化（profile/assembly 按域裁剪启动包）的可行性、与现有 `app-erp-all` 聚合的关系、零运行时侵入的优势、无法热加载的限制
  - Skill: none
- [x] `Add`: 路径 3 NocoBase-style plugin manager 调研段 — 应用层插件描述符 + 运行时菜单/权限/路由级启用禁用的可行性、复用 D2 `ModuleMetaReader` + `optionalFeatures` 的路径、GraphQL action 级开关（xbiz delta 增删）、不卸载类的妥协代价
  - Skill: none
- [x] `Add`: 对比矩阵 — 6 维度（热加载能力 / 类隔离 / 对平台核心侵入 / 对 Quarkus 启动模型影响 / 对代码生成管线影响 / 实现复杂度）× 3 路径，每格标注可行/不可行/部分可行 + 理由
  - Skill: none
- [x] `Decision`: 推荐路径裁决 — 记录选择、考虑的替代方案（3 路径互为候选）、残留风险、触发采用本推荐的事件/条件（如"当业务客户明确要求运行时启用/禁用域时"）
  - Skill: none

Exit Criteria:

> 本阶段交付一份完整的可行性研究文档草稿。无代码变更，无本地化类型检查需求。

- [x] `docs/analysis/plugin-hot-management-research.md` 存在且包含 3 路径调研段 + 对比矩阵 + 推荐裁决段
- [x] 推荐裁决记录了选择、替代方案、残留风险和触发条件

### Phase 2 - 平台约束复核 + owner doc 回链 + roadmap 同步

Status: completed
Targets: `docs/analysis/plugin-hot-management-research.md`、`docs/architecture/business-module-metadata.md`、`docs/backlog/deepening-roadmap.md`、`docs/backlog/implementation-roadmap.md`
Skill: none

- Item Types: `Add | Proof`
- Prereqs: Phase 1 完成

- [x] `Proof`: 平台约束复核 — 对照 `nop-entropy/docs-for-ai/` 中的 IoC/启动/代码生成文档，核实路径调研中关于"Quarkus CDI 编译期发现"、"GraphQL schema 编译期生成"、"代码生成 `_gen/` 增量链"的断言是否有平台文档佐证；如发现断言与平台实际不符，修正调研段
  - Skill: none
- [x] `Add`: owner doc 回链 — `docs/architecture/business-module-metadata.md` 增「D4 插件热管理可行性（交叉引用）」段，引用本研究的推荐路径与 D2 `ModuleMetaReader` 的关系
  - Skill: none
- [x] `Add`: roadmap 同步 — `docs/backlog/deepening-roadmap.md` §5 Milestone D D4 状态 `todo → done` + 新增 §8.11 D4 落地证据段（对齐既有 §8.1-§8.10 格式）+ §2 Work Item Status 表更新为 `todo: 0, done: 11` + `docs/backlog/implementation-roadmap.md` deepening-roadmap 状态更新
  - Skill: none

Exit Criteria:

- [x] 平台约束断言经 `nop-entropy/docs-for-ai/` 佐证或修正
- [x] owner doc 回链段存在且引用本研究
- [x] deepening-roadmap D4 标记 done 且落地证据段格式对齐既有条目

## Draft Review Record

- Independent draft review iteration 1: acceptable-as-is（独立子代理 ses_07990caeaffe）— 基线声明经仓库证据佐证（§5/§7/§8.1-§8.10 + D2 owner doc + project-context）；无阻塞项。非阻塞建议已采纳：Phase 2 Targets 增补 `implementation-roadmap.md`（修正其 stale 状态 `todo（11 项全部未开始）`→实际 10/11 done）。已知预存 stale：deepening-roadmap §2 表（`todo:7,done:4`）与 implementation-roadmap 行，Phase 2 同步时一并修正。

## Closure Gates

> 本计划为纯文档/研究，无生产代码变更。删除完整仓库验证命令门控（无代码可编译/测试），保留文档 well-formed + 仓库构建基线不回归检查。

- [x] 范围内行为完成（可行性研究文档 + 推荐裁决 + owner doc 回链 + roadmap 同步）
- [x] 相关文档对齐（business-module-metadata.md 回链段、deepening-roadmap 落地证据）
- [x] 已运行验证：`xmllint --noout`（如涉及 XML）/ markdown 结构完整；`mvn clean install -DskipTests` 不回归（确认未误改生产代码）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 插件系统实际实现

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: D4 定位为 P3 可行性研究；实际实现需业务客户明确需求 + ORM/架构授权，超出本计划范围
- Successor Required: yes（触发：业务客户明确要求运行时插件热管理 + 架构 owner doc 授权）

### SaaS 多租户版本管理编排

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: D2 Deferred successor，独立触发条件（SaaS 多租户部署 + tenant-model 集成授权）
- Successor Required: yes（与本研究的路径 3 存在信息关联，但非硬依赖）

## Closure

Status Note: 全部 Phase 已执行完成（Phase 1 + Phase 2 全 done）。Phase 1 交付 `docs/analysis/plugin-hot-management-research.md`（10 节可行性研究：3 路径调研 + 6 维度对比矩阵 + 推荐裁决 R1）。Phase 2 完成 6 项平台约束断言经 `nop-entropy/docs-for-ai/` 佐证（2 处初稿表述修正已并入正文）+ owner doc 回链（business-module-metadata.md §6.1）+ roadmap 同步（deepening-roadmap §5/§2/§4/§8.11 + implementation-roadmap）。验证：全 workspace `mvn clean install -DskipTests` BUILD SUCCESS（154 模块全绿，纯文档变更无生产代码回归）。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理 ses_0797c9948ffeZ159oZD2FnNYs6（新会话，非执行者）
- Evidence: 7 维度全 PASS — (1) 计划内部一致性（Plan Status=completed + Phase 1/2 全 [x] 无遗留 [ ]）；(2) Phase 1 交付物完整（3 路径 + 6×3 对比矩阵 + 推荐裁决 R1 含选择/替代/残留风险/触发条件）；(3) 平台约束佐证真实（ioc-and-config/api-and-graphql/model-first-development 三文档对照断言准确 + 2 处初稿表述修正成立）；(4) owner doc 回链存在（business-module-metadata.md §6.1 含 D4 推荐路径对 D2 元数据复用点表）；(5) roadmap 同步一致（deepening §5/§2/§4/§8.11 + implementation-roadmap，11/11 计数正确 A1-A3+B1+C1-C3+D1-D4=11）；(6) 构建基线无回归（git status 仅 .md 变更，无生产代码，Non-Goals 受尊重）；(7) Deferred 项分类正确（插件实现 + SaaS 多租户均 out-of-scope + 触发条件）。VERDICT: acceptable-as-is

Follow-up:

- 插件系统实际实现（触发条件见上）
- 版本范围求解器（D2 Deferred successor，触发：模块业务版本数 > 3 + 不兼容升级场景）

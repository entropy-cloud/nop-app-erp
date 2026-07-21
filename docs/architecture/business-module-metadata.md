# 业务模块元数据（Business Module Metadata，BT5 风格）

> Owner Doc：定义 `_module-meta.json` 的扩展 schema（业务版本 + 业务依赖 + 可选特性）、字段语义、生成管线扩展点、运行时读取器契约，以及与 Maven POM / 平台 module-meta.json 的边界。
>
> Source：`docs/backlog/deepening-roadmap.md` §Milestone D / D2；`docs/analysis/erp-survey/2026-07-20-post-survey-strategic-gaps.md` §3.12；`docs/analysis/erp-survey/2026-07-20-0000-erp5-compare.md` §4 / 建议 3。
>
> Related：`docs/architecture/domain-module-split-analysis.md`（模块链 + DAG）、`docs/architecture/external-api-integration-pattern.md`（§1 主题边界声明，D2 与 D1 互为独立主题）。
>
> Plan：`docs/plans/2026-07-21-2225-3-business-module-metadata-bt5.md`。

## 1. 目的与范围

本文档定义 `nop-app-erp` 全 18 业务域 + 1 跨域通知派发子系统（共 19 个 `module-*/`）的**业务级模块元数据**约定。元数据服务于：升级前依赖完整性检查、模块拓扑可观测、特性清单查询、未来 SaaS/多租户版本编排的信息输入。

### 与 Maven POM 的边界（互补不替代）

| 维度 | Maven POM（既有） | 本文档 module-meta.json（新增） |
|------|------------------|------------------------------|
| 粒度 | 编译期技术依赖（jar 级） | 业务级版本 + 业务依赖 + 可选特性 |
| 生效时刻 | 构建/编译期 | 运行时（读取器扫描 classpath） |
| 版本语义 | `ext:mavenVersion=1.0-SNAPSHOT`（技术版本，SNAPSHOT 不表达业务变更） | `version`（业务版本，SemVer，独立于 Maven 版本，见 §Decision B） |
| 用途 | classpath 组装 | 升级前完整性检查 / 拓扑诊断 / 特性清单 |

**核心原则**：Maven POM 解决"能否编译通过"；module-meta.json 解决"业务模块组合是否完整、版本是否匹配、哪些特性可开关"。两者**不同粒度，互补不替代**。

### 与平台 module-meta.json 的边界

Nop Platform 的 `/nop/templates/meta` 代码生成模板已经从 ORM 根 `ext:` 属性生成 4 个最小字段（`moduleId` / `moduleName` / `appName` / `icon`）的 `_module-meta.json`。本计划是对该管线的**轻量增强**：新增 3 个 optional 字段，向后兼容（未声明新字段的模块生成结果与现状完全一致），**不修改 ORM 源模型**（见 §Decision C）。

## 2. Schema 字段表 + 版本号约定

### 2.1 字段表（Decision A 裁决）

| 字段 | 类型 | 必填 | 语义 | 示例 |
|------|------|------|------|------|
| `moduleId` | string | 是（既有，来自 ORM 根） | 模块在虚拟文件系统中的路径标识 | `"erp/pur"` |
| `moduleName` | string | 是（既有） | Maven 模块工程名 | `"erp-pur"` |
| `appName` | string | 是（既有） | 两级应用名（`erp-<简称>`） | `"erp-pur"` |
| `icon` | string | 是（既有，来自 `ext:icon`） | AMIS 菜单图标 | `"shopping-cart"` |
| `version` | string | 否（**新增**） | 模块业务版本，SemVer `MAJOR.MINOR.PATCH` | `"1.0.0"` |
| `businessDependencies` | array | 否（**新增**） | 业务级依赖声明，每项 `{moduleId, version?}` | `[{"moduleId":"erp/md","version":"1.0.0"}]` |
| `optionalFeatures` | array | 否（**新增**） | 可选特性声明，每项 `{feature, configKey, defaultValue}` | `[{"feature":"budget-commitment","configKey":"erp-fin.budget-commitment-enabled","defaultValue":false}]` |

**裁决（Decision A）= 候选 (a)**：首版轻量三字段（`version` + `businessDependencies` + `optionalFeatures`），全部 optional。

- **替代方案 (b)** 含 `minPlatformVersion`：**否决**。平台版本约束已由 Maven POM `ext:platformVersion=2.0.0-SNAPSHOT` + 父 pom `nop-entropy` 版本表达，重复声明徒增漂移风险。
- **替代方案 (c)** 更粗仅 version + businessDependencies：**否决**。config-gated 特性（如 finance 的 `budget-commitment-enabled` 等）散落在各域 `Erp*Configs.java` / `Erp*Constants.java`，无统一清单查询入口；`optionalFeatures` 用极低成本补齐该缺口。
- **残留风险**：`businessDependencies` 首版仅做**存在性 + 精确版本匹配**校验，**不做 SemVer 范围求解**（如 `>=1.2.0`）。范围求解归 successor（触发条件：模块业务版本数 > 3 且出现不兼容升级场景）。

### 2.2 版本号约定（Decision B 裁决）

**裁决（Decision B）= 候选 (a)**：业务版本采用 SemVer `MAJOR.MINOR.PATCH`，**独立于**既有 `ext:mavenVersion=1.0-SNAPSHOT`。

**理由**：
- Maven `SNAPSHOT` 表达"开发中技术构件"，不表达"业务能力变更"。同一个 Maven `1.0-SNAPSHOT` 可能对应多次业务版本演进。
- 技术版本（编译产物标识）与业务版本（业务能力契约）解耦，是 BT5 风格的核心语义（ERP5 BT5 自带独立 `version`，如 `erp5_trade 5.4.3`）。
- 派生自 `ext:mavenVersion`（候选 b）或 git tag（候选 c）都会让业务版本失去独立语义，且 git tag 在多模块单仓库场景下与"模块级版本"不匹配。

**与既有 13 个 ext: 属性的边界**：本计划只**新增**业务 `version`（来自手写 `module-meta.yaml`），**不触碰**既有 `ext:mavenVersion` / `ext:platformVersion`（版本相关既有属性保持现状，由 codegen 模板既有逻辑处理）。

**初始版本约定**：本次首次落地，全 19 模块 `version` 初始值统一为 `"1.0.0"`（首版业务版本，与 Maven `1.0-SNAPSHOT` 对齐的业务语义起点）。

## 3. 生成管线扩展（Decision C 裁决）

### 3.1 源文件位置裁决

**裁决（Decision C）= 候选 (a)**：新字段来自 meta 模块内**独立手写源文件** `module-<domain>/erp-<short>-meta/precompile/module-meta.yaml`，与 ORM 源模型解耦。

- **替代方案 (b)** ORM 根新增 `ext:version` 等 ext: 属性：**否决**。理由：(i) D2 **不在** `deepening-roadmap.md` §8「ORM 变更已授权」清单内（仅 A1/A2/A3/B1/C2 在列）；(ii) roadmap §6 D2 行明确标注 ORM 变更 = "否 — module-meta.json 变更"；(iii) 加 ext: 属性属 ORM 源模型变更，触发 `AGENTS.md` / `project-context.md §AI 阻塞条件` 的 ORM 硬停止。候选 (a) 完全规避该冲突。
- **替代方案 (c)** POM 派生：**否决**。POM 表达技术依赖，无业务版本/特性语义（见 §1 边界表）。

**残留风险**：手写 yaml 中的 `moduleId` 必须与 ORM 根生成的 `moduleId` 一致，否则会产生元数据漂移。该一致性由 `gen-meta.xgen` 叠加读取时**强制校验**（不一致则 codegen 期报错，见 §3.2）。

### 3.2 gen-meta.xgen 叠加读取改造点

每个 meta 模块的 `precompile/gen-meta.xgen` 在既有 `codeGenerator.renderModel(...)` 之后**叠加**一段 overlay 逻辑：

1. 读取 ORM 根经平台模板生成的 `_module-meta.json`（4 既有字段）。
2. 若 meta 模块内存在手写源 `precompile/module-meta.yaml`，则用 `JsonTool.parseBeanFromYaml` 解析（平台 `nop-core` 已提供 YAML 解析能力，经由 `snakeyaml` 依赖，无需引入第三方工具）。
3. **一致性校验**：yaml 中若声明 `moduleId`，必须与 ORM 根生成的 `_module-meta.json.moduleId` 一致，否则 codegen 期抛错终止（防漂移）。
4. **合并**：将 yaml 中声明的 `version` / `businessDependencies` / `optionalFeatures` 叠加写入 `_module-meta.json`（仅当 yaml 声明了对应字段才写入；缺失字段不输出 JSON key，保持向后兼容）。
5. 回写 `_module-meta.json`。

**向后兼容保证**：yaml 缺失时，生成结果与现状完全一致（仅 4 既有字段），不输出任何新 JSON key。

### 3.3 module-meta.yaml 手写源约定

位于 `module-<domain>/erp-<short>-meta/precompile/module-meta.yaml`，YAML 格式，字段全部 optional：

```yaml
moduleId: erp/pur          # optional，仅用于一致性校验，缺失时不校验
version: "1.0.0"           # optional，业务版本 SemVer
businessDependencies:      # optional，有跨域调用的模块才声明
  - moduleId: erp/md
    version: "1.0.0"
  - moduleId: erp/inv
    version: "1.0.0"
optionalFeatures:          # optional，有 config-gated 特性才声明
  - feature: three-way-match
    configKey: erp-pur.three-way-match-enabled
    defaultValue: false
```

**声明 vs 省略规则**：
- `moduleId` 建议始终声明（供一致性校验）。
- `version` 建议始终声明（业务版本是 BT5 元数据的核心）。
- `businessDependencies`：**有跨域调用的模块声明**（按 `domain-module-split-analysis.md` §4.1 DAG），**无跨域调用的独立模块省略**（如 master-data 是 DAG 根，不声明）。
- `optionalFeatures`：**有 config-gated 特性开关的模块声明**（从各域 `Erp*Configs.java` / `Erp*Constants.java` 的 `*_ENABLED` 布尔配置抽取），无特性开关的模块省略。

## 4. 运行时读取器契约（Decision D 裁决）

### 4.1 归属模块裁决

**裁决（Decision D）= 候选 (a)**：运行时读取器实现在 `app-erp-all`（聚合 app）。

- **替代方案 (b)** 新建 `module-sys` 风格基础设施模块：**否决**。引入新模块增加构建复杂度，且元数据读取是应用层关注点。
- **替代方案 (c)** `nop-entropy` 平台层：**否决**。应用层业务元数据归应用，避免改平台（platform-first 规则的反面：应用数据不下沉平台）。

**理由**：`app-erp-all` 是唯一已知全部 19 模块的聚合 app（依赖全部 `*-web` → 传递 `*-service`/`*-dao`/`*-meta`），其 classpath 含全部 `_module-meta.json`，是运行时扫描的自然归属。

### 4.2 读取器 API 契约

读取器扫描 classpath 全部 `_module-meta.json`（经 `VirtualFileSystem.instance().getChildren("/erp")` 枚举各域，再取 `{域}/model/_module-meta.json`），反序列化为 `ModuleMetaBean`，提供查询 API：

| 方法 | 语义 |
|------|------|
| `listModules()` | 返回全部已加载模块元数据列表 |
| `getModule(moduleId)` | 按 moduleId 查询单个模块元数据 |
| `checkDependencyIntegrity()` | 校验所有模块的 `businessDependencies`：引用的 moduleId 存在 + 精确版本匹配（首版不做范围求解）。返回缺失/不匹配清单 |
| `listOptionalFeatures()` | 汇总全部模块的可选特性清单 |

`checkDependencyIntegrity` 首版仅做**存在性 + 精确版本匹配**。范围求解（SemVer range resolution）归 successor。

### 4.3 诊断暴露

读取器经 `@BizQuery` 暴露为 GraphQL 诊断端点（`ErpModuleMetaBizModel`），供运维/升级前检查调用。错误码：
- `ERP_MODULE_DEPENDENCY_MISSING` — `businessDependencies` 引用的 moduleId 在 classpath 中不存在。
- `ERP_MODULE_VERSION_MISMATCH` — 引用的 moduleId 存在但版本不匹配。

错误码定义遵循项目约定（`erp.err.module-meta.*` 前缀，描述用中文，i18n 处理翻译）。

## 5. 与 ERP5 BT5 对照

| BT5 概念 | ERP5 实现 | nop-app-erp 对应（本计划） |
|---------|----------|--------------------------|
| `version` | BusinessTemplate 自带（如 `erp5_trade 5.4.3`） | `version` 字段（SemVer，独立于 Maven 版本） |
| `bt/dependency_list` | 声明 `erp5_core >= 5.4.3` | `businessDependencies`（首版仅精确匹配，范围求解 successor） |
| BusinessTemplate 机制 | 可安装/卸载的业务模板包 | **不在本计划范围**（D2 仅元数据；生命周期归 D4，见 §6） |
| 可选特性 | ERP5 无统一标准 | `optionalFeatures`（nop 增强，对齐 config-gated 特性散布现状） |

**借鉴边界**：本计划借鉴 BT5 的"模块自描述业务版本 + 业务依赖"思想，**不照搬** BusinessTemplate 的安装/卸载/打包机制（那是 D4 插件热管理的可行性研究范围）。

## 6. 与 D4 插件热管理的关系

D2 与 D4 的关系是**信息输入，非硬依赖**：

- D2（本计划）提供模块边界元数据（版本/依赖/特性清单）。
- D4（roadmap §Milestone D，独立 P3 可行性研究）评估 OSGi-style vs Maven module isolation vs NocoBase-style 插件管理器在 Nop Platform 上的可行性，提供 enable/disable 生命周期。
- D4 **仅由 D1 解锁**（roadmap §7 mermaid `D1 --> D4`），**不**由 D2 解锁。D2 的元数据**可作为** D4 评估的信息输入（模块边界、依赖拓扑），但 D4 启动不依赖 D2 完成。

## 7. 反模式自检表

| # | 反模式 | 正确做法 |
|---|--------|---------|
| 1 | 把 Maven 编译依赖复制到 `businessDependencies` | `businessDependencies` 只声明**业务级**依赖（跨域 I*Biz 调用 / 业务单据过账引用），不重复 Maven 已表达的 jar 级依赖 |
| 2 | 在 `optionalFeatures` 重复 config key | 每个 `configKey` 在全 19 模块范围内唯一；从 `Erp*Configs.java` / `Erp*Constants.java` 的 `*_ENABLED` 布尔配置抽取，不杜撰 |
| 3 | 在 `optionalFeatures` 列入非布尔配置（数值/字符串） | `optionalFeatures` 仅声明**特性开关**（boolean config-gated），数值/字符串配置（如 cost-scale）不入此清单 |
| 4 | 修改 ORM 源模型（`*.orm.xml`）加 ext: 属性来表达业务版本 | 业务版本来自手写 `module-meta.yaml`，**绝不**触 ORM 源模型（D2 不在 §8 ORM 授权清单） |
| 5 | `module-meta.yaml` 的 moduleId 与 ORM 根不一致 | gen-meta.xgen 强制校验，不一致则 codegen 期报错 |
| 6 | 在 `businessDependencies` 声明循环依赖 | 依赖必须遵循 `domain-module-split-analysis.md` §4.1 DAG 无环 |
| 7 | 把业务 `version` 派生自 `ext:mavenVersion` | 业务版本独立于 Maven 技术版本（Decision B） |
| 8 | 修改平台 `/nop/templates/meta` 模板加新字段 | 平台模板保持现状；新字段经 app 层 `gen-meta.xgen` overlay 叠加 |

## 8. 落地证据

> Phase 3 完成后填写（2026-07-22）。

### 8.1 生成管线变更

每个 meta 模块的 `precompile/gen-meta.xgen` 在既有 `codeGenerator.renderModel(...)` 之后叠加 overlay 段：经 `JsonTool.parseYaml`（平台 `nop-core` 内置 snakeyaml）解析 `precompile/module-meta.yaml` → moduleId 一致性校验（不一致 codegen 期 `IllegalArgumentException`）→ 合并 version/businessDependencies/optionalFeatures → 回写 `_module-meta.json`。**不触 ORM 源模型**。

### 8.2 19 个 yaml 源清单 + 声明/省略分类

| 模块 | moduleId | version | businessDependencies | optionalFeatures |
|------|----------|---------|---------------------|-----------------|
| master-data | erp/md | 1.0.0 | **省略**（DAG 根） | exchange-rate-api |
| inventory | erp/inv | 1.0.0 | md/fin/pur/mfg/ast/prj | costing/trace-chain/ownership-tracking/standard-cost-ppv |
| purchase | erp/pur | 1.0.0 | md/inv/fin/qa/ast/prj | supplier-scorecard-red-gate |
| sales | erp/sal | 1.0.0 | md/inv/fin/qa/ast/prj | credit-notify |
| finance | erp/fin | 1.0.0 | md/inv/pur/sal/ast/prj | 13 开关（budget-check/commitment/carry-forward/roll-forward/expense-budget-check/exchange-revaluation/bank-fx-revaluation/notes-fx-gain-loss/multi-schema/annual-close/bad-debt-allowance-gate/auxiliary-recon-gate/posting-exception-notify） |
| assets | erp/ast | 1.0.0 | md/fin | cip-interest-capitalization |
| projects | erp/prj | 1.0.0 | md/fin/ast | expense-aggregation/pnl-auto-calc |
| manufacturing | erp/mfg | 1.0.0 | md/inv/fin/pur/sal/qa | 9 开关（forecast-consume/genealogy-write/inspection-gate/overhead-allocation/subcontract-*/variance-*） |
| quality | erp/qa | 1.0.0 | md/inv/fin/pur/sal | spc/spc-auto-ncr |
| maintenance | erp/mnt | 1.0.0 | md/inv/fin | equipment-status-link/labor-posting/spare-part-posting |
| notify | erp/notify | 1.0.0 | **省略**（跨域基础设施，被各业务域引用，自身不依赖业务域） | email-channel/sms-channel/merge |
| crm | erp/crm | 1.0.0 | md/inv/sal/qa/fin/ast/prj | event-reminder |
| cs | erp/cs | 1.0.0 | md/fin | sla/sla-notify/service-catalog/entitlement-check/canned-response/survey*/… |
| hr | erp/hr | 1.0.0 | md/fin | shift-cross-day |
| aps | erp/aps | 1.0.0 | inv/mfg | **省略**（无 config-gated 特性） |
| logistics | erp/log | 1.0.0 | inv/fin | **省略**（无 config-gated 特性） |
| b2b | erp/b2b | 1.0.0 | pur | **省略**（无 config-gated 特性） |
| contract | erp/ct | 1.0.0 | md/pur/sal | e-signature/rebate/volume-discount |
| drp | erp/drp | 1.0.0 | md/inv/pur/sal/mfg/qa | forecast-consume |

**省略 businessDependencies 的模块**：master-data（DAG 根）、notify（跨域基础设施，被引用而非引用）。
**省略 optionalFeatures 的模块**：aps / logistics / b2b（无 config-gated 布尔特性开关）。

> businessDependencies 来源：各域 `*-service/pom.xml` 真实跨域 Maven 依赖（service+dao 级，排除 notify 基础设施、test-scope），反映运行时真实跨域调用，较 `domain-module-split-analysis.md` §4.1 设计期 DAG 更细。optionalFeatures 来源：各域 `Erp*Constants.java`/`Erp*Configs.java` 的 `*_ENABLED` 布尔配置键镜像（权威默认值仍以代码为准）。

### 8.3 跨域依赖矩阵抽样

`ModuleMetaReader.checkDependencyIntegrity()` 在聚合 app 运行时返回全绿（无 missing/mismatch，全 19 模块 version=1.0.0 精确匹配）。抽样核对与 §4.1 DAG 一致：finance→{md,inv,pur,sal}、manufacturing→{md,inv}、maintenance→{md,inv}。

### 8.4 读取器 API + 错误码

- 读取器：`app.erp.all.meta.ModuleMetaReader`（POJO，经 `ModuleManager` + `VirtualFileSystem` 扫描），提供 `listModules`/`getModule`/`checkDependencyIntegrity`/`listOptionalFeatures`。
- BizModel：`app.erp.all.meta.ErpModuleMetaBizModel`（`@BizQuery` 暴露诊断端点），注册于 `app-erp-all` 新增模块 `app/all`。
- 错误码：`erp.err.module-meta.dependency-missing` / `erp.err.module-meta.version-mismatch`（`ErpModuleMetaErrors`，供硬失败场景 `NopException` 抛出）。

### 8.5 测试基线

- `TestModuleMetaReader`（**NEW**，7 场景全绿）：多模块扫描（19 erp 域 + version 全声明断言）/ 依赖完整性正路径 / 缺失依赖负路径 / 版本不匹配负路径 / 特性清单查询 / 向后兼容（DAG 根省略 businessDependencies）/ 跨域依赖矩阵抽样。
- 全 workspace `mvn clean install -DskipTests` BUILD SUCCESS（codegen 管线扩展 + 19 yaml 源不破坏既有基线）。

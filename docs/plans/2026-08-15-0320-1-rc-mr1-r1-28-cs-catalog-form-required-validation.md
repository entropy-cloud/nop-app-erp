# 2026-08-15-0320-1-rc-mr1-r1-28-cs-catalog-form-required-validation RC-R1.28 — cs 目录建单必填校验（MR1 第一批纯预授权）

> Plan Status: completed
> Last Reviewed: 2026-08-15
> Mission: requirement-compliance
> Work Item: RC-R1.28（P1-RC-060 cs UC-CS-10 服务目录请求必填表单校验——requestFormConfig schema 未驱动校验，盲拷 formData keys）
> Source: `docs/backlog/requirement-compliance-roadmap.md` §MR1 RC-R1.28 行 + `docs/audits/arm-index.md` P1-RC-060 行 + 展开器映射 `docs/audits/2026-08-07-1910-rc-mr1-r1-0-expander.md`（纯 BizModel/Processor 代码逻辑预授权）
> Related: `docs/design/customer-service/use-cases.md`（L1 UC-CS-10 异常①）；`docs/design/customer-service/service-catalog.md`（§1.4 requestFormConfig 配置格式）；`docs/audits/2026-08-08-0015-rc-ma4-a4-2-124-142-cs-f1-f2-f3-f4-runtime.md`（A4.2.142 运行时证据）；`docs/plans/2026-08-08-2219-3-rc-mr1-r1-18-19-sal-return-cost-guards-family.md`（同型范式参照——pre-approve 守卫族 + 错误码 + 负向测试矩阵）
> Audit: required

## Current Baseline

- **finding P1-RC-060（arm-index 行，UC-CS-10 异常①）**：L1（`use-cases.md:199`）逐字「表单必填项缺失 → 禁止提交」。L3 实仓（HEAD 核查）：`requestFormConfig` 字段（`module-cs/erp-cs-meta/.../_ErpCsServiceCatalogItem.xmeta:68-71`，propId=12，`domain="json"` type=String precision=4000）**存在但未被作为动态表单 schema 强制**——`ErpCsServiceCatalogItemCreateFromCatalogProcessor.buildTicketData:146-156` 经 `copyIfPresent` 盲拷 formData keys（subject/description/customerId/contactId/productId/orderNumber/urgency→priority/source），**无按 requestFormConfig.fields[].required 的必填校验**；grep `requiredField|validateForm|formSchema` 跨 cs main **零命中** → L1 异常"表单必填项缺失禁止提交"❌。§4 三判据复核（arm-index 已裁决）：(i) 无独立 plan-audit 裁决"必填校验裁剪"；(ii) `service-catalog.md §1.4` requestFormConfig 含 `required:true` 字段定义**暗示应强制**（未标 Non-Goal；§9.1 仅裁剪履行编排不涉表单校验）；(iii) product-scope 未裁剪 → **三判据均不成立 → Q4=(a) 强制实现 P1**。
- **requestFormConfig JSON 格式契约（`service-catalog.md §1.4:79-91`，权威）**：`{"fields":[{"key":"subject","label":"请求主题","type":"text","required":true}, ...]}`——每字段含 key/label/type/required；服务端须按 `fields[].required==true` 校验 formData 对应 key 非空。**契约格式为既有设计，本行不改格式只消费之**。
- **实仓（HEAD 核查）**：
  - `ErpCsServiceCatalogItemCreateFromCatalogProcessor.java`（185 行，per-mutation Processor）：`createFromCatalog` 主链 = `requireCatalogItem` → `validateCatalogItemUsable` → `buildTicketData` → `applyEntitlementToTicketData` → `ticketBiz.save` → `fulfillmentBiz.executeFulfillmentSteps`（try/catch WARN 降级）。`buildTicketData:129-166` 是唯一的 formData 消费点——**校验应接线在 `buildTicketData` 之前（或作为其前置步骤）**，在创建工单/扣减权益前拒绝，保证 L1「禁止提交」语义（工单不落库、权益不扣减）。
  - `subject` 缺省回退（`:158-160` fallback 到 `item.getName()`）与 `priority` 缺省回退（`:162-164` fallback NORMAL）在 formData 映射后执行——**Decision 项**：必填校验基准 = formData 原始键值（fallback 前）还是合并后值（fallback 后）。L1 语义「表单必填项缺失」= 客户提交的表单字段缺失 → **基准应为 formData 原始值**（fallback 是服务端兜底非客户提供），但须在计划中显式裁决。
  - `ErpCsErrors.java`：既有 `ERR_CATALOG_ITEM_INACTIVE`/`ERR_CATALOG_ITEM_NOT_FOUND`（:140-148）——新增 `ERR_CATALOG_FORM_REQUIRED_MISSING`（对齐 arm-index 行指定错误码 + A4.2.142 修复方向），i18n 描述中文（含 `{catalogItemId}` + 字段 key 参数占位符，对齐既有错误码内嵌参数风格如 :142）。
  - **⚠ 数据治理前提（A4.2.142 运行时报告 §(f) 条件）**：生产/种子 `requestFormConfig` 数据当前未维护 `required` schema（`_ErpCsServiceCatalogItem.json` 模板 requestFormConfig 为空串；seedCatalogItem 未设）——**校验仅在 schema 声明 required 时生效，否则跳过（死代码条件）**。本行实现服务端校验能力，数据治理（生产 catalogItem 维护含 required 的 schema）登记 Deferred But Adjudicated（见下）。
  - 测试：`TestErpCsServiceCatalog.java`（`testCreateFromCatalogFillsTicketFields:117-144` / `testCreateFromCatalogInactiveRejected:146-156` / `testCreateFromCatalogSubjectFallbackToItemName:158+`）——**当前 seedCatalogItem 均未设 requestFormConfig**（null → 跳过校验，既有测试零回归）；须新增 requestFormConfig seed + 必填缺失拒绝 + 必填满足放行 + 非法 JSON 容错测试。
  - `JsonTool` 已在 cs main 使用（`CannedResponseRenderer.java:67` `JsonTool.parseBeanFromText` 同型范式）——解析 requestFormConfig 复用平台工具（AGENTS.md 平台规则：JsonTool 而非第三方 JSON 库）。
- **预授权判据**（第一批纯预授权）：纯 BizModel/Processor 代码逻辑（解析 JSON schema + 必填校验 + 错误码 + 测试），**不触 ORM 结构（requestFormConfig 字段已存在不加列）/会计过账/删除**；roadmap RC-R1.28 行 `todo`，Deps（R1.0 done）已满足。
- **涉及文件**：`module-cs/erp-cs-service/src/main/java/app/erp/cs/service/processor/ErpCsServiceCatalogItemCreateFromCatalogProcessor.java`（校验步骤）；`module-cs/erp-cs-service/src/main/java/app/erp/cs/service/ErpCsErrors.java`（新增错误码）；`module-cs/erp-cs-service/src/test/java/app/erp/cs/service/TestErpCsServiceCatalog.java`（新增测试）；`docs/design/customer-service/service-catalog.md`（§1.4 服务端校验实现注记）；`docs/audits/arm-index.md` + `docs/backlog/requirement-compliance-roadmap.md` + `docs/logs/2026/08-15.md`（回填）。

## Goals

- **必填校验运行时成立（P1-RC-060 核心）**：`createFromCatalog` 在 `buildTicketData` 前接线必填校验——解析 `item.requestFormConfig`（`JsonTool.parseBeanFromText` → Map）→ 遍历 `fields[]` 中 `required==true` 的字段 → 校验 formData 对应 `key` 非空（null/空串/空白→缺失）→ 任一缺失抛新增 `ERR_CATALOG_FORM_REQUIRED_MISSING`（参数含 catalogItemId + 缺失字段 key），工单不创建、权益不扣减（L1「禁止提交」语义）。
- **容错边界**：`requestFormConfig` 为 null/空/非法 JSON/fields 为空数组 → 跳过校验（既有行为保持，零回归）；`required` 值非布尔 true → 不视为必填（防御解析）；formData 为 null → 全部必填字段缺失（按 schema 拒绝）。
- **Decision 记录**：必填校验基准（formData 原始值 vs fallback 后合并值）+ subject fallback 与必填 subject 的交互语义（若 schema 标 subject 必填而 formData 缺 subject——L1「表单必填项缺失禁止提交」字面 → 拒绝，即使 fallback 可兜底；此裁决须显式记录供人工复核）。
- **测试**：新增测试组覆盖——① schema 含必填字段 + formData 缺失 → 拒绝 + 错误码 + 工单零落库 + 权益零扣减；② schema 必填字段全满足 → 放行（含既有映射键 urgency→priority 场景）；③ requestFormConfig null → 跳过校验（回归既有测试）；④ 非法 JSON → 跳过校验（防御容错）；⑤ 空串/空白值视为缺失。
- **owner doc 收敛**：`service-catalog.md §1.4` 补服务端必填校验实现注记（校验时机 + 错误码 + 容错语义）；不修改需求契约段（use-cases L1 不动）。
- **零回归**：既有 cs 测试全绿（`TestErpCsServiceCatalog` 等）+ 全仓构建 + compliance checker 零漂移（纯校验逻辑无 REQUIRES_NEW/新 daoFor）。
- **回填**：arm-index P1-RC-060 → `done (RC-R1.28)` + roadmap 行 → `done` + `docs/logs/` 日志条目。

## Non-Goals

- **不实现 P1-RC-061 履行引擎**（actionConfig 解析/占位步骤/重试——独立 finding 触 ORM + 部分 ask-first，非本行范围）。
- **不实现 P2-RC-055 履行失败告警**（独立 P2 watch-only，非本行范围）。
- **不实现 P1-RC-054 建单自动富化族**（独立 finding，部分 ask-first，非本行范围）。
- **不触 ORM 结构**（requestFormConfig 列已存在，零列/零索引变更）。
- **不改真相源契约段落**（use-cases L1 不动；requestFormConfig JSON 格式契约不动）。
- **不实现前端动态表单渲染**（requestFormConfig 驱动的前端表单是门户自助前端 successor——本行仅服务端校验，前端渲染缺失不阻塞 L1「禁止提交」的服务端强制）。

## Task Route

- Type: `implementation-only change`（P1 需求分歧的预授权代码逻辑修复，Q4=(a) 强制实现禁止方案 B）
- Owner Docs: `docs/design/customer-service/use-cases.md`（L1 UC-CS-10 异常①）+ `docs/design/customer-service/service-catalog.md`（§1.4 requestFormConfig 格式）+ `docs/audits/2026-08-08-0015-rc-ma4-a4-2-124-142-cs-f1-f2-f3-f4-runtime.md`（A4.2.142 运行时证据）
- Skill Selection Basis: 实现面 = per-mutation Processor 校验步骤接线 + JsonTool 解析 + 错误码（`nop-backend-dev`：Processor 模式、错误码范式、JsonTool 平台工具）；测试（`nop-testing`：JunitAutoTestCase + GraphQL RPC 冒烟范式）。无 view.xml/xbiz/ORM 变更（校验在 Processor 内，无 xbiz action 新增）。

## Infrastructure And Config Prereqs

- 无新 config key/环境变量/外部服务（校验直接读 `item.requestFormConfig` 字段）。
- 分域验证前置：`mvn install -DskipTests` 后 `mvn test -pl module-cs/erp-cs-service`。

## Execution Plan

### Phase 1 - Explore requestFormConfig 消费点与校验接线点（Decision）

Status: completed
Targets: `ErpCsServiceCatalogItemCreateFromCatalogProcessor.java`；`_ErpCsServiceCatalogItem.xmeta`；`service-catalog.md`；`TestErpCsServiceCatalog.java`
Skill: `nop-backend-dev`

- Item Types: `Decision | Proof`
- Prereqs: 无（既有基线）

- [x] `Decision` **校验接线点裁决**：**选项 A（倾向）** = `buildTicketData` 前置独立 protected step `validateRequiredFormFields(item, formData)`（在 `requireCatalogItem`/`validateCatalogItemUsable` 后、`buildTicketData` 前调用——工单不落库 + 权益不扣减）；选项 B（否决） = 在 `buildTicketData` 内联校验（映射与校验耦合，subject fallback 顺序纠缠）。**理由（选项 A）**：per-mutation Processor 的 protected step 模式对齐既有 `validateSchedulePrereqs`/`checkScheduleConflict` 范式（R6.6/R6.7），校验失败在副作用（权益扣减 `applyEntitlementToTicketData`）之前拒绝，语义与 L1「禁止提交」一致；选项 B 使 fallback 与校验基准纠缠，无法清晰区分「formData 原始值 vs fallback 后值」。
      - Skill: `nop-backend-dev`
- [x] `Decision` **必填校验基准裁决（B1）**：**选项 A（倾向）** = formData 原始键值（fallback 前）——`fields[].key` 直接查 formData map，null/空串/空白=缺失 → 拒绝；**选项 B（否决）** = 合并后值（fallback 后）——subject 有 fallback 则永不缺失，使「schema 标 subject 必填」形同虚设。**理由（选项 A）**：L1「表单必填项缺失」指客户提交的表单字段缺失；fallback 是服务端兜底非客户提供，不应满足必填语义（`testCreateFromCatalogSubjectFallbackToItemName` 是**无 requestFormConfig** 场景的回归测试，有 schema 时必填拒绝与之不冲突）。**注意**：urgency→priority 映射键（schema key="urgency"）按 formData 原始键校验。
      - Skill: `nop-backend-dev`
- [x] `Proof` **运行时验证前置**：既有测试基线确认——`TestErpCsServiceCatalog` 当前测试集全绿（含 `testCreateFromCatalogFillsTicketFields`/`testCreateFromCatalogInactiveRejected`/`testCreateFromCatalogSubjectFallbackToItemName`）；`ErpCsServiceCatalogItem__createFromCatalog` mutation 入口经 GraphQL RPC 可达（`rpc(mutation, ...)` 模式）；`JsonTool.parseBeanFromText` 解析 JSON String → Map 在 cs main 已有先例（`CannedResponseRenderer.java:67`）。运行时行为最终由 Phase 3 测试给出。
      - Skill: `nop-testing`

Exit Criteria:

> 仅写此阶段实际交付的可观察结果，以及解除后续阶段阻塞所需的任何本地化检查。

- [x] 接线点裁决（A）+ 校验基准裁决（B1 A）记录落盘计划，Explore 证据（既有测试基线 + JsonTool 先例）确认
- [x] 无语法/引用错误风险点识别完成（错误码定义位置 + Processor 注入点）

### Phase 2 - 必填校验落地（P1-RC-060 核心）

Status: completed
Targets: `ErpCsServiceCatalogItemCreateFromCatalogProcessor.java`；`ErpCsErrors.java`
Skill: `nop-backend-dev`

- Item Types: `Fix | Add`
- Prereqs: Phase 1 完成

- [x] `Fix` 新增错误码 `ERR_CATALOG_FORM_REQUIRED_MISSING`（**字符串建议 `erp.err.cs.catalog-item.form-required-missing`**，对齐既有 `catalog-item.*` 族；Java 常量名按 arm-index 指定 `ERR_CATALOG_FORM_REQUIRED_MISSING`），描述中文「服务目录请求表单必填项缺失：字段 {fieldKey}」+ 参数 ARG_CATALOG_ITEM_ID + ARG_CATALOG_FIELD_KEY（对齐 `ErpCsErrors` 既有 ErrorCode 定义风格 `ErrorCode.define` + param 常量 + 描述模板内嵌 `{param}` 占位符）。
      - Skill: `nop-backend-dev`
- [x] `Fix` `createFromCatalog` 接线新 protected step `validateRequiredFormFields(ErpCsServiceCatalogItem item, Map<String, Object> formData)`（按 Phase 1 裁决位置）——实现：`item.getRequestFormConfig()` null/空白 → 跳过；`JsonTool.parseBeanFromText` 解析（解析失败 → LOG.warn + 跳过，防御容错）；取 `fields` 数组遍历，`required` 布尔 true 的字段校验 `formData.get(key)` 非空（`StringHelper` 判空白或 `value != null && !String.valueOf(value).isBlank()`），缺失 → 抛 `ERR_CATALOG_FORM_REQUIRED_MISSING`（param：catalogItemId + 字段 key + 字段 label 若可得）。
      - Skill: `nop-backend-dev`
- [x] `Decision` **容错边界裁决（T1）**：非法 JSON → 整体跳过 + LOG.warn（不抛新错误码以外的异常）；fields 非数组 → 整体跳过 + LOG.warn；单个字段 `required` 非布尔 true → 该字段不视为必填（逐字段跳过）。**理由**：配置数据质量问题不应阻断建单主流程（对齐 `fulfillmentBiz` try/catch WARN 降级范式），但合法 schema 的必填缺失必须拒绝（L1 核心语义）。
      - Skill: `nop-backend-dev`
- [x] `Add` **schema key 映射覆盖约束注记**：`validateRequiredFormFields` 校验的 schema keys 须落在 `buildTicketData` 映射集（subject/description/customerId/contactId/productId/orderNumber/urgency/source）内——映射集之外的 key 即使 formData 有值也会在落库时被静默丢弃（`copyIfPresent` 盲拷），本行在 owner doc 注记声明此约束（数据治理责任在 schema 维护方），不扩大校验范围到映射集之外。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [x] 校验步骤接线且错误码定义落地（grep 显示 `validateRequiredFormFields` 在 buildTicketData 前调用 + `ERR_CATALOG_FORM_REQUIRED_MISSING` 定义）
- [x] 容错边界行为确定（非法 JSON 跳过 + 非布尔 required 跳过，LOG.warn 记录）

### Phase 3 - 测试矩阵

Status: completed
Targets: `module-cs/erp-cs-service/src/test/java/app/erp/cs/service/TestErpCsServiceCatalog.java`（新增测试方法）
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 2 完成

- [x] `Add` 新增测试组（`TestErpCsServiceCatalog` 追加，镜像既有 `testCreateFromCatalog*` 模式——seedCatalogItem 增 requestFormConfig 参数重载或新 seed helper）：① 必填缺失拒绝——schema 标 subject+description 必填，formData 缺 description → `ERR_CATALOG_FORM_REQUIRED_MISSING` + 工单零落库（`ErpCsTicket` 无新行）+ 权益零扣减（**须 seed 活跃权益 + `isEntitlementCheckEnabled` 相关配置使断言非平凡**——既有 `TestErpCsServiceCatalog` 无权益 seed，权益 seed 范式见 `TestErpCsEntitlement`；若权益链路默认关闭则仅断言工单零落库 + 校验拒绝）；② 必填满足放行——formData 全必填键非空 → 成功 + ticket 字段映射正确；③ requestFormConfig null → 放行（回归既有 `testCreateFromCatalogFillsTicketFields` 语义）；④ 非法 JSON → 放行 + LOG.warn（断言成功创建）；⑤ 空串/空白值 → 视为缺失拒绝；⑥ 非必填字段缺失 → 放行。
      - Skill: `nop-testing`
- [x] `Proof` 既有 cs 测试零回归：`mvn test -pl module-cs/erp-cs-service`（既有测试集 + 新增全绿；记录测试计数）。
      - Skill: `nop-testing`

Exit Criteria:

- [x] 新增必填校验测试组全绿 + 既有 cs 测试零回归（`mvn test -pl module-cs/erp-cs-service` BUILD SUCCESS）
- [x] 必填拒绝有运行时断言证据（非仅静态接线——GraphQL RPC 实际调用 + 工单/权益副作用断言）

### Phase 4 - 文档回填 + arm-index/roadmap 状态

Status: completed
Targets: `docs/design/customer-service/service-catalog.md`；`docs/audits/arm-index.md`；`docs/backlog/requirement-compliance-roadmap.md`；`docs/logs/2026/08-15.md`
Skill: none

- Item Types: `Add | Fix`
- Prereqs: Phase 1-3 完成

- [x] `Add` owner doc 注记：`service-catalog.md §9.4` 补服务端必填校验实现注记（校验时机 = createFromCatalog buildTicketData 前 + 校验基准 = formData 原始值 + 错误码 + 容错语义 + **数据治理声明：校验仅在 requestFormConfig 声明 required 时生效，生产/种子 catalogItem 数据须维护含 required 的 schema** + schema keys 须落在映射集内约束 + 测试证据）；不修改需求契约段（use-cases L1 不动）。
      - Skill: none
- [x] `Add` arm-index P1-RC-060 → `done (RC-R1.28)` + 修复落地摘要（校验接线 + 错误码 + 容错边界 + 测试证据）；roadmap RC-R1.28 → done ✅（含落地摘要）；`docs/logs/2026/08-15.md` 日志条目写入。
      - Skill: none

Exit Criteria:

- [x] arm-index/roadmap 状态回填 + owner doc 注记落盘 + 日志条目写入

## Draft Review Record

- Independent draft review iteration 1: needs revision（独立子代理 ses_ffe446ce0ffeJdMwmGlmolVaH5）— 0 BLOCKER / 1 MAJOR / 6 MINOR。MAJOR = A4.2.142 数据治理条件未裁决（生产 requestFormConfig 缺 required schema 致校验死代码）——已补 baseline 注记 + Phase 4 owner doc 数据治理声明 + Deferred But Adjudicated 项；6 MINOR 全部修正（ErpCsErrors :140-148 / 错误码字符串 catalog-item 族 / 容错 T1 独立 Decision 项 / 测试①权益 seed 注记 / schema key 映射覆盖约束 / Deferred successor 一致性 + 错误模板 {param} 占位符）。
- Independent draft review iteration 2: accept（独立子代理 ses_ffe3b0258ffeXL4vkHdXKeTeWL）— 0 BLOCKER / 0 MAJOR。全部 iteration-1 发现确认修复 + 基线行号实仓复核通过；仅剩非阻塞 MINOR（Draft Review Record 填记）已随本条解决。**计划可标记 active。**

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。**完整仓库验证在此处**：结束时运行一次全量验证。

- [x] 范围内行为完成
- [x] 相关文档对齐
- [x] 已运行验证（`mvn test -pl module-cs/erp-cs-service` 全绿 + `mvn clean install -DskipTests` 全量 BUILD SUCCESS + `bash docs/audits/nop-compliance-checker.sh` actual ≤ baseline——纯校验逻辑预期零漂移，若 checker 漂移按 project-context 已知失败模式 #1 登记）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 生产/种子 requestFormConfig 数据治理（required schema 维护）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 本行实现服务端必填校验能力（L1「禁止提交」的服务端强制面）；校验仅在 requestFormConfig 声明 required 时生效，生产/种子 catalogItem 当前未维护含 required 的 schema（A4.2.142 §(f) 条件）——能力落地不依赖数据治理，但生产环境实际拦截须配套数据治理（维护含 required 的 schema）。重开事件 = 门户自助前端上线或生产目录数据治理批次启动时随行执行。
- Successor Required: `no`

### 前端动态表单渲染缺失（门户自助前端 successor）

- Classification: `watch-only residual`
- Why Not Blocking Closure: requestFormConfig 驱动的前端表单渲染（`service-catalog.md §4.2` 表单设计/门户）是门户自助前端 successor——本行实现服务端必填校验（L1「禁止提交」的服务端强制面），前端渲染缺失不改变服务端拒绝语义；重开事件 = 门户自助前端上线时由前端接入。
- Successor Required: `yes`（触发条件 = 门户自助前端上线，随数据治理批次执行）

## Closure

Status Note: 执行完成（2026-08-15）。四阶段全部 `[x]`：Phase 1 接线点裁决（A：buildTicketData 前置独立 protected step）+ 校验基准裁决（B1 A：formData 原始值 fallback 前）+ Explore 证据（既有测试基线 + JsonTool 先例）；Phase 2 必填校验落地（`ERR_CATALOG_FORM_REQUIRED_MISSING` 错误码 + `validateRequiredFormFields` protected step 接线 + 容错边界 T1 裁决 + schema key 映射集约束注记）；Phase 3 测试矩阵（`TestErpCsServiceCatalog` 新增 6 组，必填缺失拒绝含工单零落库 + 权益零扣减断言）；Phase 4 文档回填（service-catalog.md §9.4 / arm-index / roadmap / 日志）。验证：`mvn test -pl module-cs/erp-cs-service` 122/122 全绿（TestErpCsServiceCatalog 15/15）+ `mvn clean install -DskipTests` 156 模块 BUILD SUCCESS + `bash docs/audits/nop-compliance-checker.sh` actual == baseline 零漂移（R1d=14/R2a=34/R2b=230/R2c=1393/R2d=34/R12a=69/R12b=66/R12c=40）。结束审计已由独立子代理（新会话）执行并通过（见 Closure Audit Evidence），门控全 `[x]`。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话 ses_ffe2ac3b0ffekSk2VF6suoIZ8L，无执行者上下文）
- Evidence: 五点一致性核验通过——(1) Plan Status completed 与四 Phase Status completed + 全 Exit Criteria `[x]` + Closure Gates 全 `[x]` 一致；(2) Exit Criteria vs live repo 实仓复核：`ErpCsErrors.java:152-155` 含 `ERR_CATALOG_FORM_REQUIRED_MISSING`（erp.err.cs.catalog-item.form-required-missing + ARG_CATALOG_ITEM_ID/ARG_CATALOG_FIELD_KEY 参数）；`ErpCsServiceCatalogItemCreateFromCatalogProcessor.java:53,138-181` `validateRequiredFormFields` protected step 在 validateCatalogItemUsable 后、buildTicketData 前接线，JsonTool.parseBeanFromText 真实解析 + Boolean.TRUE.equals(required) 逐字段校验 + StringHelper.isBlank 判空 + 抛 NopException，容错路径（null/空白/非法 JSON/fields 非数组 LOG.warn 跳过 + 非布尔 required 跳过）齐全；(3) Anti-Hollow：6 新增测试（:193-301）全为 GraphQL RPC 实际调用 + 副作用断言（缺失拒绝含工单零落库 + 活跃 PAY_PER_TICKET 权益 usedTickets==0 零扣减），审计者实跑 15/15 + 全模块 122/122 绿；(4) Deferred honesty：数据治理 required schema 维护 + 前端动态表单渲染两项均为真实范围外事项（服务端校验能力已落地，schema 声明 required 即拦截，测试实证），显式披露带重开事件，非范围内 live defect 隐藏；(5) Docs sync：`service-catalog.md:322-332` §9.4 + `arm-index.md:238`（done RC-R1.28）+ `requirement-compliance-roadmap.md:420`（done ✅）+ `docs/logs/2026/08-15.md` 全部回填，§1.4 契约段未动。1 项 MINOR（cosmetic，Phase 4 项文字 §1.4→§9.4）已随审计后修订解决。审计通过，计划可关闭。

Follow-up:

- 无（范围内全部落地；Deferred But Adjudicated 两项已登记重开事件，非阻塞）

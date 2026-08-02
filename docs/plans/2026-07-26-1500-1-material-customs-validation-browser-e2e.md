# 2026-07-26-1500-1 物料报关记录 BizModel 校验钩子浏览器层 E2E

> Plan Status: completed
> Last Reviewed: 2026-07-26
> Source: `docs/backlog/deepening-roadmap.md` §8.3 C2 落地证据（C2 跨境贸易扩展已落地，`ErpMdMaterialCustomsBizModel` 3 个 `defaultPrepareSave`/`defaultPrepareUpdate` 校验钩子经 JUnit `TestErpMdMaterialCustoms` 4 场景单层验证，但**零浏览器层 E2E**——仅有 `material-customs.visual.spec.ts` visual smoke 覆盖 xmeta 字段可达 + findPage action 注册，不覆盖写路径校验钩子行为）；AGENTS.md §当前项目阶段明示「各域细化端到端验证」为当前重点
> Related: `docs/plans/2026-07-21-1206-1-master-data-cross-border-trade-extensions.md`（C2 后端落地 plan）、`docs/plans/2026-07-26-0500-3-c3-date-range-validation-browser-e2e.md`（C3 日期范围校验 sales 浏览器层 E2E——同型「BizModel 保存钩子写路径浏览器层验证」先例，本计划镜像其范式）、`docs/plans/2026-07-26-1407-3-exchange-rate-api-client-browser-e2e.md`（D1 master-data 浏览器层 E2E 先例）
> Audit: required

## Current Baseline

C2（Cross-Border Trade Extensions）已于 plan `2026-07-21-1206-1` 落地。实时仓库核实（2026-07-26）：

**`ErpMdMaterialCustomsBizModel` 3 校验钩子**（`module-master-data/erp-md-service/src/main/java/app/erp/md/service/entity/ErpMdMaterialCustomsBizModel.java:51-130`）：

| 钩子 | 守卫语义 | ErrorCode | 行号 |
|------|---------|-----------|------|
| `enforceDeclarationNoUnique` | declarationNo 全局唯一（DB UK 前置友好校验，经 `dao().findAllByQuery(query)` 绕过 objMeta filter `ne` 限制，镜像 `ErpMdMaterialBizModel.isCodeUnique` 范式） | `ERR_CUSTOMS_DECLARATION_NO_DUPLICATE`（ARG_DECLARATION_NO） | `:82-95` |
| `enforcePartnerIsCustomsBroker` | partnerId 非空时，引用 Partner 类型必须为 `CUSTOMS_BROKER`（可空允许无报关行自报） | `ERR_PARTNER_NOT_CUSTOMS_BROKER`（ARG_PARTNER_ID/ARG_PARTNER_TYPE）；partnerId 不存在抛 `ERR_PARTNER_NOT_FOUND` | `:101-116` |
| `enforceSourceBillPresent` | sourceBillType / sourceBillCode 至少一个非空（业务回链必填） | `ERR_CUSTOMS_SOURCE_BILL_REQUIRED`（ARG_DECLARATION_NO） | `:121-130` |

三钩子由 `defaultPrepareSave`/`defaultPrepareUpdate` 统一调 `validateOnPersist`（`:66-73`）触发，覆盖 `__save`/`__update` Map 入口（CrudBizModel 管道）。`updateEntity` 内部调用不触发（对齐 C3 关键发现 `CrudBizModel.updateEntity()` 不触发 `defaultPrepareUpdate`）。

**JUnit 单层验证**：`TestErpMdMaterialCustoms`（`module-master-data/erp-md-service/src/test/java/app/erp/md/service/TestErpMdMaterialCustoms.java`）4 场景全绿：CRUD 生命周期 / partnerType=CUSTOMER 拒绝 / sourceBill 均空拒绝 / declarationNo 重复拒绝。经 GraphQL RPC 集成测试，但**零 Playwright 浏览器层 E2E**。

**visual smoke 现状**：`tests/e2e/visual/material-customs.visual.spec.ts` 2 测试仅覆盖（1）ErpMdMaterial xmeta 9 跨境字段可达；（2）ErpMdMaterialCustoms findPage action 注册。**不覆盖**写路径（`__save`/`__update`）触发的 3 校验钩子行为。

**种子基线**：`app-erp-all/src/main/resources/_vfs/_init-data/erp_md_partner.csv` 经 `rg -c CUSTOMS_BROKER` 核实 = **0**（种子仅含 CUSTOMER + SUPPLIER）。种子 `erp_md_material.csv` + `erp_md_material_customs.csv` 无报关记录种子（C2 后端落地未补种子）。浏览器层 spec 须自包含建 CUSTOMS_BROKER partner 前置。

**种子 material**：`erp_md_material.csv` 含物料种子（MAT_1 等），spec 可复用种子物料 materialId 或自包含建测试专用物料隔离。

**字典**：`erp-md/partner-type.dict.yaml:25` 含 `CUSTOMS_BROKER` 值（C2 落地时新增）；`erp-md/customs-preference-code.dict.yaml` 含 12 FTA 协定代码。

剩余差距：C2 BizModel 3 校验钩子经 JUnit 单层验证但零浏览器层 E2E；写路径校验行为（友好错误响应 + 守卫语义）未经全栈 GraphQL `__save` 路径验证。

## Goals

- `ErpMdMaterialCustomsBizModel` 3 校验钩子（declarationNo 唯一 / partnerType CUSTOMS_BROKER / sourceBill 必填）经 Playwright 浏览器层 E2E 全栈验证——驱动真实 GraphQL `__save`/`__update` mutation 经 CrudBizModel 管道触发 `defaultPrepareSave`/`defaultPrepareUpdate` 钩子，断言正路径持久化成功 + 3 负路径守卫拒绝（errors message 含语义中文 token）
- 补全 C2 落地证据的「浏览器层验证」缺口（镜像 §8.6 C3 / §8.9 A3 / §8.4 A2 范式增「浏览器层验证」bullet）
- e2e-runbook 业务动作表新增 master-data 物料报关记录行

## Non-Goals

- **不修改生产代码**（BizModel/Provider/ErrorCode/ORM/契约/字典/种子/config 零变更）——纯测试 + 文档计划
- **不覆盖 CRUD 读路径**（findPage 列表 / view 详情）——已由 `material-customs.visual.spec.ts` visual smoke 覆盖
- **不覆盖 9 个 ErpMdMaterial 跨境字段编辑**（vatRate/drawbackRate/customsHS 等）——属 ErpMdMaterial 实体编辑面，非 ErpMdMaterialCustoms 校验钩子结果面
- **不覆盖报关完整业务流程编排**（C2 Deferred successor：海关申报完整业务流程编排，触发：业务流程需求 + 跨域 owner doc 授权）
- **不覆盖关税计算引擎**（C2 Deferred successor，触发：含反倾销税/报复性关税的复杂税率计算需求）
- **不覆盖 partnerId 不存在守卫**（`ERR_PARTNER_NOT_FOUND`）——FK 约束在 GraphQL schema 层已强制，非 BizModel 钩子结果面
- **不做 `updateEntity` 内部调用不触发钩子的对照断言**——C3 已建立此关键发现范式，本计划仅验证用户面 `__save`/`__update` 写路径

## Task Route

- Type: `verification or audit work`（JUnit 单层验证 → 浏览器层全栈验证补全，纯测试 + 文档，零生产代码变更）
- Owner Docs: `docs/design/master-data/cross-border-trade.md`（C2 owner doc，§报关场景工作流 / §ErpMdMaterialCustoms 实体设计——本计划增「浏览器层验证」实现注记）、`docs/testing/e2e-runbook.md`（业务动作表 + 套件计数）
- Skill Selection Basis: `nop-testing`（Playwright 浏览器层 E2E + 既有 business-actions/_helper 复用 + GraphQL `__save`/`__update` 写路径触发 CrudBizModel 钩子 + 自包含 setup 种子 FK 满足 + 拒绝路径裸 mutation 断言范式，对齐 0500-3 同型先例）

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline
- 复用现有 Playwright 配置 + webServer JVM 参数（ErpMdMaterialCustoms 校验钩子无 config 门控，DIRECT 可达；webServer 无需新增 JVM arg）

## Execution Plan

### Phase 1 - Explore（实体最小字段集 + 错误响应结构 + 种子 FK 可达性核实）

Status: completed
Targets: `module-master-data/model/app-erp-master-data.orm.xml`（ErpMdMaterialCustoms 实体字段 + UK）、`ErpMdMaterialCustomsBizModel.java`（钩子行号锚点）、`ErpMdErrors.java`（ErrorCode description 中文 token）、`app-erp-all/.../erp_md_partner.csv`（种子 partner-type 分布）
Skill: `none`

- Item Types: `Proof`
- Prereqs: 无

- [x] `Proof`：核实 `ErpMdMaterialCustoms__save` 最小必填字段集——经 ORM 模型 `<entity name="app.erp.md.dao.entity.ErpMdMaterialCustoms">` 逐列核实 `mandatory="true"` 列（预期 code/name/orgId 等）+ 钩子涉及列（declarationNo/partnerId/sourceBillType/sourceBillCode）precision + 可空性。笔记写入 plan Execution Decision 段
      - Skill: none
- [x] `Proof`：核实 3 ErrorCode 的 `description` 中文 token——经 `ErpMdErrors.java`（或 i18n message bundle）确认 errors message 含可断言语义 token（如「报关单号」「报关行」「业务单据」/「来源单据」等），对齐 0500-3「errors 含语义中文 token」范式（Nop GraphQL error envelope 经 `/graphql` 仅 surface `message`，token 字面按 ErrorCode description 适配）
      - Skill: none
- [x] `Proof`：核实种子 FK 可达性——`erp_md_partner.csv` CUSTOMS_BROKER 行数（预期 0 → 须自包含建）；`erp_md_material.csv` 可复用种子物料 materialId 或裁决自包含建测试专用物料隔离（镜像 0500-3 `partnerId` FK 修复范式）；orgId 种子值（预期种子 org 2 可复用）
      - Skill: none

Exit Criteria:

> Phase 1 产出可执行的 spec 设计输入：最小字段集 + 错误响应 token + setup 策略裁决。

- [x] 最小字段集 + 钩子列可空性已记录（支撑 Phase 2 `__save` payload 构造）
- [x] 3 ErrorCode description 中文 token 已记录（支撑 Phase 2 拒绝路径断言）
- [x] setup 策略裁决已记录（自包含建 CUSTOMS_BROKER partner + material 复用/自建裁决）

#### Execution Decisions（Phase 1 Explore 笔记）

经实时仓库核实（`module-master-data/model/app-erp-master-data.orm.xml:268-329` + `ErpMdMaterialCustomsBizModel.java:51-130` + `ErpMdErrors.java:155-176` + `TestErpMdMaterialCustoms.java:144-165` + `app-erp-all/.../_init-data/erp_md_partner.csv` + `erp_md_material.csv`）：

1. **最小必填字段集（修正 plan 基线 §73 预测「code/name/orgId」，对齐 Draft Review Minor (1)）**：经 ORM `app-erp-master-data.orm.xml:274-299` 逐列核实 `mandatory="true"` 列：
   - `code`（mandatory, VARCHAR 50, tagSet=var）
   - `materialId`（mandatory, BIGINT, FK→ErpMdMaterial）
   - `declarationNo`（mandatory, VARCHAR 50, UK `UK_MD_MATERIAL_CUSTOMS_DECL_NO:312` 强制全局唯一）
   - `declarationDate`（mandatory, DATE）
   - `qtyDeclared`（mandatory, DECIMAL 20,4）
   - `uomDeclared`（mandatory, VARCHAR 20 —— 海关法定单位，非 FK→ErpMdUoM）
   - `amountDeclared`（mandatory, DECIMAL 18,2）
   - **无 `name` 列 / 无 `orgId` 列**（plan 基线 §73 预测「code/name/orgId 等」不精确，Draft Review Minor (1) 已识别）。JUnit helper `TestErpMdMaterialCustoms.newCustomsPayload:144-165` 示真值：code/materialId/partnerId(可选)/declarationNo/declarationDate/qtyDeclared/uomDeclared/amountDeclared。
   - 钩子涉及列可空性：`partnerId`（**非 mandatory**，BizModel `enforcePartnerIsCustomsBroker` 守卫——非空时校验 CUSTOMS_BROKER）；`sourceBillType`/`sourceBillCode`（**均非 mandatory**，BizModel `enforceSourceBillPresent` 守卫——至少一个非空）。

2. **3 ErrorCode description 中文 token（修正 plan 基线 §101 token 备选「来源单据」，对齐 Draft Review Minor (3)）**：经 `ErpMdErrors.java:161-176` 核实 description 字面：
   - `ERR_CUSTOMS_DECLARATION_NO_DUPLICATE`（`erp.err.md.customs.declaration-no-duplicate`，:167-170）→ "报关单号[{declarationNo}]已存在，不允许重复" → 稳定可达 token：**「报关单号」**
   - `ERR_PARTNER_NOT_CUSTOMS_BROKER`（`erp.err.md.partner.not-customs-broker`，:161-164）→ "报关行[{partnerId}]的类型[{partnerType}]不是 CUSTOMS_BROKER，不可作为报关记录的报关行" → 稳定可达 token：**「报关行」**
   - `ERR_CUSTOMS_SOURCE_BILL_REQUIRED`（`erp.err.md.customs.source-bill-required`，:173-176）→ "报关记录必须填写业务单据类型(sourceBillType)或业务单据编码(sourceBillCode)至少一项" → 稳定可达 token：**「业务单据」**（**NOT「来源单据」**——plan 基线 §101 备选 token「来源单据」实际 description 不含此字面，Draft Review Minor (3) 已识别；执行期断言采用实测唯一稳定 token「业务单据」）。
   - GraphQL error envelope 仅 surface `message`（对齐 0500-3 §104 实测发现 + hr-leave 先例），断言匹配范式：`expect(JSON.stringify(errors)).toContain('<token>')`。

3. **种子 FK 可达性**：
   - `erp_md_partner.csv` `rg -c CUSTOMS_BROKER` = **0**（种子仅 CUSTOMER id=1/id=2 + SUPPLIER id=3/id=4）→ **须自包含建 CUSTOMS_BROKER partner**（唯一 code 隔离）。
   - `erp_md_material.csv` 含物料种子（MAT-001=id 1，FINISHED_PRODUCT/ACTIVE）→ **复用种子 `materialId=1`**（material FK 已满足，不新建测试物料——对齐 0500-3 §133 `MAT_1=1` 复用范式，避免污染 master-data 物料基线）。
   - **无 `orgId` 列**（见 §1），故无 orgId FK 顾虑（plan 基线 §77 「orgId 种子值」预期对本实体不适用）。
   - 无既有 `erp_md_material_customs.csv` 种子（C2 后端落地未补种子），spec 全量自包含建记录。

4. **setup 策略裁决**：
   - **自包含建 CUSTOMS_BROKER partner**：经 `ErpMdPartner__save`（mandatory: code/name/partnerType=CUSTOMS_BROKER/status=ACTIVE），唯一 code 前缀 `E2E-MC-` 隔离。
   - **复用种子 materialId=1**（MAT-001）——material FK 满足，零物料基线污染。
   - **自包含建报关记录**：唯一 code 前缀 `E2E-MC-` + 唯一 declarationNo 前缀 `DECL-E2E-MC-` 隔离。
   - cleanup 经 `deleteById` 删本 spec 产物（customs 记录 + 自建 CUSTOMS_BROKER partner），不污染共享 DB 基线。

### Phase 2 - spec 实现（正路径持久化 + 3 负路径守卫拒绝 + `__update` 自身排除）

Status: completed
Targets: `tests/e2e/business-actions/md-material-customs-validation.action.spec.ts`（NEW）、`tests/e2e/business-actions/_helper.ts`（复用既有 `createViaSave`/`saveRaw` 原语，预期零新增）
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 1 Explore 笔记就位

- [x] `Add`：新建 `md-material-customs-validation.action.spec.ts`（预期 5 用例），自包含 setup 经 `ErpMdPartner__save` 建 CUSTOMS_BROKER partner（唯一 code 隔离）+ 复用/自建物料 materialId + orgId=种子 2。5 用例：
      - (1) **正路径**：合法 `ErpMdMaterialCustoms__save`（declarationNo 唯一 + partnerId=CUSTOMS_BROKER partner + sourceBillType 非空）→ `data` 非空 + `errors` null + `__get` 反查持久化 declarationNo/partnerId/sourceBillType
      - (2) **declarationNo 重复守卫**：第二次 `__save` 同 declarationNo → `errors` 含「报关单号」语义 token + `data` null
      - (3) **partnerType 非 CUSTOMS_BROKER 守卫**：`__save` partnerId=种子 CUSTOMER/SUPPLIER partner → `errors` 含「报关行」语义 token
      - (4) **sourceBill 均空守卫**：`__save` sourceBillType=null + sourceBillCode=null → `errors` 含「来源单据」/「业务单据」语义 token
      - (5) **`__update` 自身排除**：正路径建记录后 `__update` 缩窄/变更可改字段（如保持 declarationNo 不变）→ 成功（`enforceDeclarationNoUnique` 经 `entity.getId()` 排除自身，对齐 0500-3 `enforceMutex selfId` 范式）
      - Skill: `nop-testing`
- [x] `Proof`：拒绝路径经 `saveRaw`（裸 `__save` mutation 直取 `{data,errors,json}`）断言，对齐 0500-3 拒绝路径原语范式（`createViaSave` 内置 `expect(errors).toBeNull()` 会在拒绝时失败）
      - Skill: `nop-testing`
- [x] `Proof`：cleanup 删本 spec 产物（CUSTOMS_BROKER partner + material customs 记录 + 自建物料若裁决自建），不污染 master-data dashboard / 其他 spec 基线
      - Skill: `nop-testing`

Exit Criteria:

> Phase 2 交付可运行的 spec，5 用例覆盖正路径 + 3 守卫 + `__update` 自身排除。

- [x] spec 文件存在且 5 用例全绿（`npx playwright test tests/e2e/business-actions/md-material-customs-validation.action.spec.ts` 0 failures）
- [x] 拒绝路径经 `saveRaw` 裸 mutation 断言 errors message 语义 token（非依赖 ErrorCode extension 路径）
- [x] cleanup 不污染共享 DB 基线（master-data dashboard 回归 0 新增失败）

#### Execution Decisions（Phase 2 落地注记）

- **实测 5 用例全绿**（`BASE_URL=http://127.0.0.1:8011 SKIP_WEBSERVER=1 npx playwright test tests/e2e/business-actions/md-material-customs-validation.action.spec.ts`，5 passed 36.6s）。
- **setup 策略落地**：自包含经 `ErpMdPartner__save` 建 CUSTOMS_BROKER partner（mandatory code/name/partnerType=CUSTOMS_BROKER/status=ACTIVE），唯一 code 前缀 `E2E-MC-BROKER-`；materialId 复用种子 `MAT_1=1`（MAT-001，零物料基线污染）。
- **拒绝 token 实测匹配**（对齐 Phase 1 §2 实测 ErrorCode description）：(2) declarationNo 重复 → 「报关单号」；(3) partnerType CUSTOMER → 「报关行」；(4) sourceBill 均空 → 「业务单据」（**非** plan 基线 §101 备选「来源单据」——Phase 1 §2 已纠正）。GraphQL error envelope 仅 surface `message`，断言范式 `expect(JSON.stringify(errors)).toContain('<token>')`。
- **case (3) partner 引用策略**：直接引用种子 CUSTOMER partner id=1（CUST-001），不新建（仅读引用，零 partner 基线污染），partnerType=CUSTOMER 触发 `enforcePartnerIsCustomsBroker` 拒绝。
- **case (5) `__update` 自身排除**：经 `GraphQLClient.raw` 直驱 `ErpMdMaterialCustoms__update` mutation 修改 `remark`（保持 declarationNo 不变）→ `defaultPrepareUpdate` → `validateOnPersist` → `enforceDeclarationNoUnique`：`dao().findAllByQuery(eq("declarationNo",...))` 命中自身记录，但 `Objects.equals(entity.getId(), existing.getId())` 排除自身 → 通过。
- **回归基线**：master-data dashboard smoke（`master-data.smoke.spec.ts`）1/1 全绿，0 新增失败。`material-customs.visual.spec.ts` 2 失败为**前置既有失败**（GraphQL schema API drift：visual spec 用过时 `findPage(limit:1)` 裸参数，schema 现要求 `findPage(query:{limit})`；与本计划零生产代码变更无关，`git status` 核实仅新增 spec + docs）。
- **`saveRaw` 为 spec 内局部函数**（对齐 Draft Review Minor (2) + 0500-3 范式，非共享 _helper 原语），复用 `GraphQLClient.raw` 直取 envelope。

### Phase 3 - owner doc 回链 + e2e-runbook + 日志

Status: completed
Targets: `docs/design/master-data/cross-border-trade.md`（增「浏览器层验证」实现注记）、`docs/testing/e2e-runbook.md`（业务动作表 + spec 计数）、`docs/backlog/deepening-roadmap.md`（§8.3 C2 增「浏览器层验证」bullet）、`docs/logs/2026/07-26.md`
Skill: none

- Item Types: `Add`
- Prereqs: Phase 2 spec 全绿

- [x] `Add`：`docs/design/master-data/cross-border-trade.md` §ErpMdMaterialCustoms 实体设计 增「浏览器层验证（plan 2026-07-26-1500-1）」实现注记（写路径触发 + 3 钩子断言范式 + 自包含 CUSTOMS_BROKER partner setup + 拒绝路径 `saveRaw` + errors message 语义 token）
      - Skill: none
- [x] `Add`：`docs/testing/e2e-runbook.md` 业务动作表新增 master-data 物料报关记录校验行 + spec 计数同步（镜像 §8.3 C2 范式）
      - Skill: none
- [x] `Add`：`docs/backlog/deepening-roadmap.md` §8.3 C2 落地证据增「浏览器层验证」bullet（镜像 §8.6 C3 / §8.4 A2 范式）
      - Skill: none
- [x] `Add`：`docs/logs/2026/07-26.md` 增本计划日志条目（按 `docs/logs/00-log-writing-guide.md` 格式）
      - Skill: none

Exit Criteria:

- [x] owner doc 实现注记落地（cross-border-trade.md）
- [x] e2e-runbook 业务动作表 + spec 计数同步
- [x] deepening-roadmap §8.3 C2 增「浏览器层验证」bullet
- [x] 日志条目落地

#### Execution Decisions（Phase 3 落地注记）

- `cross-border-trade.md` §3 新增子节「浏览器层验证（plan 2026-07-26-1500-1）」（位于 §3.4「UK + 前置友好校验协同」之后、§4「报关场景工作流」之前），覆盖写路径触发 + 3 守卫 + 1 正路径 + `__update` 自身排除断言范式 + 自包含 setup + 拒绝路径 `saveRaw` 原语 + errors message 语义 token。
- `e2e-runbook.md` 业务动作表追加 master-data 物料报关记录行（位于 master-data 汇率查询 API 行之后、调用范式段之前）+ spec 计数 97→98。
- `deepening-roadmap.md` §8.3 C2 增「浏览器层验证」bullet（位于测试基线 visual smoke 行之后、Deferred successor 之前，镜像 §8.6 C3 / §8.4 A2 范式）。
- `docs/logs/2026/07-26.md` 顶部增本计划日志条目（按 `docs/logs/00-log-writing-guide.md` 倒序格式）。

## Draft Review Record

- Independent draft review iteration 1: `acceptable-as-is`（`ses_060f2efb6ffee59ZVzoNXfuTvR`，独立 general 子代理，新会话冷重播无起草者上下文，2026-07-26）— 0 Blocker / 0 Major / 3 Minor。逐项 load-bearing 事实主张经实时仓库核实**全部精确匹配**：3 钩子行号锚点 ✓（enforceDeclarationNoUnique :82-95 / enforcePartnerIsCustomsBroker :101-116 / enforceSourceBillPresent :121-130）/ 4 ErrorCode ✓（ErpMdErrors.java:110,161,167,173）/ TestErpMdMaterialCustoms 4 JUnit 场景 ✓ / visual smoke 仅 2 测试不覆盖写路径 ✓ / 零 business-actions material-customs spec ✓ / 种子 0 CUSTOMS_BROKER ✓。**0500-3 镜像检查通过**：同 task type + 同 3-phase + 同 skill + 同 rejection-path saveRaw + 同 self-exclusion case + 同 Closure Gates。**3 Minor 保留给执行/结束审计**：(1) Phase 1 Proof item 1 预期 mandatory 字段 code/name/orgId 与实际不符（实际为 code/materialId/declarationNo/declarationDate/qtyDeclared/uomDeclared/amountDeclared，无 name/orgId 列）——Phase 1 Explore 自纠正（JUnit helper TestErpMdMaterialCustoms.newCustomsPayload:144-165 示真值）；(2) Phase 2 Targets 称 saveRaw 为共享 _helper 原语不准确（0500-3 中 saveRaw 为 spec 内局部函数）；(3) Phase 2 item (4) token 备选「来源单据」实际 ErrorCode description 仅含「业务单据」（ErpMdErrors.java:175）。3 Minor 均为预期/预测文本不精确，Phase 1 Explore 设计即为纠正此类，不影响执行契约成立。

## Closure Gates

> 本计划为纯测试 + 文档计划（零生产代码变更）。完整仓库验证：`mvn clean install -DskipTests`（154 模块）+ 新 spec Playwright 运行 + master-data 回归抽样。

- [x] 范围内行为完成（3 校验钩子浏览器层 E2E 5 用例全绿）
- [x] 相关文档对齐（cross-border-trade.md + e2e-runbook + deepening-roadmap §8.3）
- [x] 已运行验证：`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS + 新 spec 全绿 + master-data 回归抽样 0 新增失败
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### partnerId 不存在守卫（ERR_PARTNER_NOT_FOUND）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: FK 约束在 GraphQL schema 层已强制（partnerId 非空 + FK 存在性），非 BizModel 钩子结果面；schema 层拒绝先于 BizModel 钩子触发。
- Successor Required: `no`（触发条件：FK 约束被放宽 + BizModel 层须补存在性校验时）

### 报关完整业务流程编排 / 关税计算引擎 / b2b 海关 EDI 报文 / HS 编码字典全集

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: C2 Deferred successor——触发条件均未满足（业务客户具体业务流程需求 + 跨域 owner doc 授权 / 业务客户 EDI 报关需求 / 业务方明确需求 + 第三方服务集成 / 含反倾销税复杂税率计算需求）。
- Successor Required: `yes`（触发条件见 C2 §8.3 落地证据 Deferred successor 段）

## Closure

Status Note: 三阶段全绿交付——Phase 1 Explore 笔记内嵌 plan（最小字段集纠正 plan 基线无 name/orgId + 3 ErrorCode token 实测「报关单号」/「报关行」/「业务单据」+ 种子 0 CUSTOMS_BROKER 须自建 + materialId 复用 MAT_1=1）；Phase 2 新建 `md-material-customs-validation.action.spec.ts`（5 用例全绿 36.6s：正路径持久化 + `__get` 反查 / declarationNo 重复守卫 / partnerType 非 CUSTOMS_BROKER 守卫 / sourceBill 均空守卫 / `__update` 自身排除）；Phase 3 owner doc `cross-border-trade.md` §3 增「浏览器层验证」实现注记 + e2e-runbook 业务动作表 +master-data 物料报关记录行 + spec 计数 97→98 + deepening-roadmap §8.3 C2 增「浏览器层验证」✅ bullet + 日志。验证：`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS（1:28 min）+ 新 spec 5 passed + master-data dashboard smoke 1 passed（0 新增失败；material-customs.visual.spec.ts 2 失败为前置既有 GraphQL schema drift，与本计划零生产代码变更无关）。Closure Gates 1-6 由执行者核实勾选；gates 7-8（独立结束审计 + 结束证据）待独立子代理（新会话）执行。

Closure Audit Evidence:

- Auditor / Agent: independent general closure-audit subagent (fresh session, no executor context)
- Evidence: 冷重播逐项核实（2026-07-27），全部 load-bearing 主张精确匹配实时仓库：
  - **Spec 文件形态**（`tests/e2e/business-actions/md-material-customs-validation.action.spec.ts`，264 行）：精确 5 用例对齐 Phase 2 设计——(1) 正路径 :103 `createViaSave` + `verifyState`(`__get`) 反查 declarationNo/partnerId/sourceBillType；(2) declarationNo 重复 :138 经 `saveRaw` 裸 mutation 取 `{data,errors}` 断言「报关单号」token + data null；(3) partnerType 非 CUSTOMS_BROKER :172 `saveRaw` 断言「报关行」token；(4) sourceBill 均空 :194 `saveRaw` 断言「业务单据」token；(5) `__update` 自身排除 :217 经 `gql.raw` 直驱 `ErpMdMaterialCustoms__update` 修改 remark + `verifyState` 反查。cleanup 全部 try/finally + `deleteById`（customs 记录 + 自建 CUSTOMS_BROKER partner）。
  - **Spec ↔ BizModel 对齐**（`ErpMdMaterialCustomsBizModel.java`）：3 钩子行号锚点精确匹配——`enforceDeclarationNoUnique` :82-95（`dao().findAllByQuery` 绕 objMeta filter + `Objects.equals(entity.getId(), existing.getId())` 自身排除）/ `enforcePartnerIsCustomsBroker` :101-116（partnerId 非空时校验 CUSTOMS_BROKER）/ `enforceSourceBillPresent` :121-130（sourceBillType/Code 均空拒绝）；由 `defaultPrepareSave`/`defaultPrepareUpdate` → `validateOnPersist` :66-73 统一触发。3 ErrorCode description 中文 token 经 `ErpMdErrors.java:161-176` 逐字核实：ERR_PARTNER_NOT_CUSTOMS_BROKER(:161-164)「报关行」/ ERR_CUSTOMS_DECLARATION_NO_DUPLICATE(:167-170)「报关单号」/ ERR_CUSTOMS_SOURCE_BILL_REQUIRED(:173-176)「业务单据」——spec 断言 token 全部匹配。
  - **Playwright 实测全绿**：`BASE_URL=http://127.0.0.1:8011 SKIP_WEBSERVER=1 npx playwright test tests/e2e/business-actions/md-material-customs-validation.action.spec.ts` → **5 passed (36.3s)**，0 failures。
  - **范围纪律（零生产代码变更）**：`git status --short` + `git diff --stat` 核实——变更仅为 docs（cross-border-trade.md/deepening-roadmap.md/e2e-runbook.md/logs/2026/07-26.md）+ 新增 spec（untracked）+ 本 plan（untracked）。**零 Java/ORM/api.xml/dict/config/seed 变更**。（注：working tree 另含无关 plan 文件 `2026-07-26-0300-1` 的 1 行修改 + `2026-07-26-1500-2` untracked，属其他 plan 会话产物，非本计划引入，不构成范围违规。）
  - **文档更新落地**：`cross-border-trade.md:112` §3 ErpMdMaterialCustoms 实体设计内「### 浏览器层验证（plan 2026-07-26-1500-1）」子节（位于 §3.4 UK 协同 :105 之后、§4 报关场景工作流 :126 之前）；`e2e-runbook.md:231` 业务动作表 spec 计数 97→98 + :337 master-data 物料报关记录行；`deepening-roadmap.md:189` §8.3 C2「浏览器层验证」bullet ✅ done；`logs/2026/07-26.md:3` 本计划日志条目（倒序顶部）。
  - **构建基线**：`mvn install -DskipTests -o` → **BUILD SUCCESS**（154 模块，offline，1:32 min）。
  - **计划内部一致性**：3 Phase 全 Status: completed + 全 items [x]；Closure Gates 1-6 执行者已 [x]；gates 7-8 经本审计勾选。
- Verdict: PASS —— 纯测试 + 文档计划零生产代码变更，spec 5 用例全绿且与 BizModel 3 钩子 + 3 ErrorCode description token 精确对齐，4 处文档更新 + 154 模块 BUILD SUCCESS 全部经独立冷重播核实。

Follow-up:

- 报关完整业务流程编排 / 关税计算引擎 / b2b 海关 EDI（见 Deferred But Adjudicated 段，非阻塞）

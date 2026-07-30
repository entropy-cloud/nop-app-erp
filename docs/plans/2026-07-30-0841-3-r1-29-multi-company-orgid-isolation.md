# 2026-07-30-0841-3-r1-29-multi-company-orgid-isolation 多公司 / orgId 隔离架构补能力

> Plan Status: active
> Last Reviewed: 2026-07-30
> Source: audit-remediation-roadmap R1.29（P1-MA2-093/094/095/096/097/098/099 = 7 findings），源自 A2.18 多账套/多公司隔离审计
> Related: `docs/audits/2026-07-28-1510-arm-ma2-multi-company-isolation.md`；`docs/architecture/multi-company.md`；`docs/design/finance/multiple-accounting-schemas.md`；deferred P0-MA2-018（billR 无 acctSchemaId 列）
> Audit: required

## Current Baseline

七项 finding 经实仓逐项确认：多公司/多账套隔离写路径 + 自然键层基本成立，**读路径隔离机制全仓未落地**。**全部 config-gated / dormant 于单组织单账套默认**（`erp-fin.multi-schema-enabled=false` / `erp-fin.gl-mapping.org-dimension-enabled=false` / 种子 176 行业务单据全 orgId=2），无活跃跨组织数据腐败（P1 非 P0——「能力缺失」非「活跃缺陷」）。

**P1-MA2-093（orgId 查询隔离全仓未落地）— 确认：** 平台仅 tenant 自动过滤（本项目 0 实体启用 `useTenant`）；19 个 `erp-{module}.data-auth.xml` **全部 `<objs/>` 空规则** + 0 自定义 `IDataAuthChecker`/`IQueryTransformer` + `IServiceContext`/`IContext` 均无 `getOrgId()`；11 个 dashboard BizModel 经 `IDaoProvider` 直访绕过（空）认证管道（`ErpFinDashboardBizModel.sumBankBalance` 汇总所有组织银行账户）；种子 176 行全 orgId=2 完全掩盖跨组织泄漏。

**P1-MA2-094（orgId 写入客户端可任意指定）— 确认：** `CrudBizModel.doSaveEntity` 无 orgId 引用（平台仅 `setTenantId` stamp）；全域 grep `setOrgId` = 0；InputBean `_orgId` 是普通 Long 字段 + AMIS 前端渲染为可编辑列；无 `@BizEventListener`/`IPrepareSaveHook`/`IOrmInterceptor` auto-stamp 拦截器（grep=0）。

**P1-MA2-095（acctSchemaId 读路径泄漏）— 确认：** 写路径 PASS（`persistVoucher:785,820` stamp + `SchemaPropagator` 多账套传播 + 红冲镜像）；**读路径 FAIL**——`ErpFinReportBizModel`/`ErpFinDashboardBizModel`/`ErpFinPostingProcessor.resolveOpenPeriod`/`ErpFinAccountingPeriodProcessor` 等 **11 处查询方法省略 `acctSchemaId`（多数也省略 `orgId`）**（Report 5 + Dashboard 3 + PostingProcessor 1 + AccountingPeriod 2；finding 文字称「12 处」含 `ErpFinGlBalanceBizModel`——15 行空 CrudBizModel 无作用域 override，归 093 transformer 透明覆盖），按 periodId 或更弱条件聚合 → 多账套部署时报表/看板双计或三计。

**P1-MA2-096（ErpFinGlBalance 无 DB 强制自然键）— 确认：** `app-erp-finance.orm.xml:904-980` 含 mandatory orgId+acctSchemaId+periodId+subjectId+currencyId，**但无 `<unique-key>`**，仅非唯一索引。与 P0-MA2-020（inventory 余额）同型；降级 P1——GL 余额由过账引擎单线程维护（非并发 check-then-insert），并发腐败风险显著低于 inventory。

**P1-MA2-097（跨公司配对算法漂移 + 审计列全空）— 确认：** `ErpFinIntercompanyMatchBizModel.runMatching:86` `record.setPairKey(billCode)`（owner doc `multi-company.md:197` 声明 min/max org-pair hash + materialId）；`:85` `record.setOrgId(1L)` hardcoded；`arOrgId`/`apOrgId`/`arSideVoucherId`/`apSideVoucherId`/`materialId` 五列**从不填充**；`generateEliminationCandidates` orgId hardcoded 1L + `ErpFinConsolidationElimination.fromOrgId`/`toOrgId` 从不设置；`postElimination:181` voucher.orgId=1L。

**P1-MA2-098（runMatching 非幂等）— 确认：** `ErpFinIntercompanyMatch` 无 `(pairKey, periodId)` UK（`IDX_FIN_INTERCOMPANY_MATCH_PAIR_PERIOD` unique=false）→ 同期重复 `runMatching` 产生重复 Match 行（`code` UUID 后缀，code UK 不阻止）。

**P1-MA2-099（GL 映射 cache 默认 org-dimension-enabled=false 跨组织泄漏）— 确认：** `ErpFinGlMappingResolver.cacheKey:289-291` `(orgId==null?"_":orgId)+...`；config `erp-fin.gl-mapping.org-dimension-enabled` **默认 false**（:51, :246-248）→ 关闭态所有规则归 `"_"` 桶 + `matches()` 跳过 orgId 校验（:159 守卫 `isOrgDimensionEnabled()`）→ orgA 规则可匹配 orgB 过账请求。owner doc `multi-company.md:244-249` 已标注 dormant。

**保护区域：** orgId 隔离触及权限/数据隔离保护区域；096/098 UK 触及 ORM `[ORM ask-first]`；095/097 触及 finance 报表/配对读路径。须独立 plan-audit + closure-audit + ORM 人工确认。

## Goals

- **093** orgId 读路径隔离：app-layer 全局 `IQueryTransformer` 按 bizObj orgId 列自动追加 `eq("orgId", currentOrgId)`（config-gated）+ 第二个种子组织 + 负向隔离测试。
- **094** orgId 写路径 auto-stamp：`PrepareSave` 拦截器从上下文 stamp orgId（覆盖 client-supplied）（同 config-gate）。
- **095** finance 11 处读路径查询方法补 `acctSchemaId` + `orgId` filter。
- **096** `ErpFinGlBalance` 加 `(orgId, acctSchemaId, periodId, subjectId, currencyId)` UK [ORM ask-first]。
- **097** `runMatching` 填充 5 审计列 + `generateEliminationCandidates`/`postElimination` 正确设置 fromOrgId/toOrgId/voucher.orgId + owner doc 算法更正（pairKey=billCode）。
- **098** `ErpFinIntercompanyMatch` 加 `(pairKey, periodId)` UK [ORM ask-first] + `runMatching` 幂等（upsert/前置去重）。
- **099** owner doc `multi-company.md` 强化 org-dimension-enabled warning + 多公司部署 checklist。

## Non-Goals

- 不启用平台 tenant 机制重命名 orgId→nopTenant（P1-MA2-093 方案C——破坏性语义重命名 + 全域 useTenant 翻转，归 successor）。
- 不修改 nop-entropy 平台 `IServiceContext`/`IContext` 加 `getOrgId()`（平台内核变更——093/094 经 app-layer 上下文解析实现，Explore 确认可行性）。
- 不翻转 `erp-fin.gl-mapping.org-dimension-enabled` 默认值为 true（破坏单组织向后兼容——099 仅 document + checklist，默认翻转归 Decision 残留风险）。
- 不实现多公司报表（多账套合并报表 successor）——095 仅补单账套查询隔离，使多账套部署不双计。
- 不改 dashboard BizModel 的 IDaoProvider 直访模式（归 P1-MA1-022 读侧豁免裁决 + 093 transformer 透明覆盖）。

## Task Route

- Type: `architecture change`（多公司隔离架构级补能力）+ `implementation-only change`（finance 读路径 + UK + 配对审计列）
- Owner Docs: `docs/architecture/multi-company.md`、`docs/design/finance/multiple-accounting-schemas.md`、`docs/design/finance/posting.md`、`module-finance/model/app-erp-finance.orm.xml`
- Skill Selection Basis: 全局 IQueryTransformer + PrepareSave 拦截器 + ORM UK + 跨实体配对 → `Skill: nop-backend-dev`。UK 加完后 `mvn clean install -DskipTests` 增量再生。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. 隔离能力 config-gated（`erp.multi-company.org-isolation-enabled` 默认 false）保护单组织种子 + 既有测试。

## Execution Plan

### Phase 1 - 七项 finding 裁决 + orgId 解析机制 Explore（Decision | Explore）

Status: planned
Targets: 本计划（裁决记录）+ orgId 解析机制探查
Skill: `nop-backend-dev`

- Item Types: `Decision | Explore`
- Prereqs: none

- [ ] **Explore（093/094 orgId 解析）**：探查 app-layer 获取 `currentOrgId` 的可行路径——(a) `IContext.setAttribute/getAttribute`（app 层 attribute，不改平台接口）；(b) `IUserContext.getDeptId()` + 用户→组织映射表（nop-auth 关联）；(c) 上下文 thread-local。产出：选定路径 + 注册点（`@Named` 全局 `IQueryTransformer` + `PrepareSave` 拦截器 in `app-service.beans.xml`）。若三条均不可行且不改平台内核，回退 093/094 为 documented successor + 检测脚手架（第二种子组织 + 负向测试标记）——**无论主路径还是回退分支，都必须显式更正 owner doc `multi-company.md:29`「所有业务单据按 orgId 隔离查询」的过度声明**：主路径=实现后该声明成真；回退=更正为「当前单组织基线无自动 orgId 隔离，多公司部署须启用 `erp.multi-company.org-isolation-enabled` + successor 命名」，使 owner-doc drift 无论哪条分支都被解决（rule 13：已确认 owner-doc drift 不得降级为非阻塞 follow-up 而不解决）。
      - Skill: `nop-backend-dev`
- [ ] **Decision（093/094）**：**方案A（app-layer orgId 隔离）**，依赖 Phase 1 Explore 结论。全局 `IQueryTransformer` 按 bizObj orgId 列自动追加 `eq("orgId", currentOrgId)` + `PrepareSave` auto-stamp orgId（覆盖 client-supplied），config-gated `erp.multi-company.org-isolation-enabled` 默认 false。方案B（填充 19 data-auth.xml `<objs>` 角色级规则）被拒——角色级规则非组织级隔离 + 维护成本高；方案C（平台 tenant）归 successor。残留风险：orgId=null 历史数据 + org 列缺失实体须白名单。
      - Skill: `nop-backend-dev`
- [ ] **Decision（095）**：**方案A（11 查询方法补 filter）**。对齐 `populateTrialBalanceForAllSchemas` 范式，11 处查询方法全补 `eq("acctSchemaId", schemaId)` + `eq("orgId", orgId)`（第 12 处 `ErpFinGlBalanceBizModel` 空 CrudBizModel 由 093 transformer 透明覆盖）。方案B（owner doc 标注单账套 successor）被拒——查询补 filter 是机械修复 + 使多账套部署不双计。
      - Skill: `nop-backend-dev`
- [ ] **Decision（096）**：**方案A（UK）[ORM ask-first]**。`ErpFinGlBalance` 加 `(orgId, acctSchemaId, periodId, subjectId, currencyId)` UK + 数据 cleanup 评估（历史重复行）。须人工确认。
      - Skill: `nop-backend-dev`
- [ ] **Decision（097）**：**方案A（填审计列 + 算法更正）**。`runMatching` 从 SALE/PURCHASE 凭证 + billR 反查填充 arOrgId/apOrgId/arSideVoucherId/apSideVoucherId/materialId 五列 + owner doc `multi-company.md:197` 更正为 pairKey=billCode + `generateEliminationCandidates`/`postElimination` 正确设 fromOrgId/toOrgId/voucher.orgId。方案B（列预留 Deferred）被拒——审计列已存在 ORM，填充是机械修复 + 恢复抵消追溯链。
      - Skill: `nop-backend-dev`
- [ ] **Decision（098）**：**方案A（UK + 幂等）[ORM ask-first]**。`ErpFinIntercompanyMatch` 加 `(pairKey, periodId)` UK + `runMatching` 改 upsert/前置去重（同期同 pairKey 已 MATCHED 则 skip）。须人工确认。
      - Skill: `nop-backend-dev`
- [ ] **Decision（099）**：**documented（默认不翻转）**。owner doc `multi-company.md:244-249` 强化警告 + 多公司部署 checklist 显式要求 `erp-fin.gl-mapping.org-dimension-enabled=true`。默认翻转 true 归 successor（破坏单组织向后兼容，须独立评估）。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] Phase 1 Explore 产出 orgId 解析路径；七项 Decision 逐项记录；ORM ask-first 项（096/098）标 `[ORM ask-first]` 待人工确认。

### Phase 2 - orgId 读/写隔离架构补能力（093/094）

Status: planned
Targets: 全局 `IQueryTransformer` 实现、`PrepareSave` auto-stamp 拦截器、`app-service.beans.xml`、种子组织 + 负向测试
Skill: `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 1 Explore 确认可行

- [ ] **Add（093，读隔离）**：实现全局 `IQueryTransformer`（config-gated `erp.multi-company.org-isolation-enabled` 默认 false）——对带 orgId 列的 bizObj 自动追加 `eq("orgId", currentOrgId)`；org 列缺失/null 实体白名单（如系统配置实体）；注册于 `app-service.beans.xml`。
      - Skill: `nop-backend-dev`
- [ ] **Add（094，写 stamp）**：实现 `PrepareSave`/`@BizEventListener` auto-stamp——create 时从上下文 stamp orgId（覆盖 client-supplied `_orgId`）；同 config-gate。
      - Skill: `nop-backend-dev`
- [ ] **Add（种子 + 测试）**：新增第二个种子组织 orgId=3 + 独立业务单据；负向隔离测试——`org-isolation-enabled=true` 时 orgId=2 上下文查询 orgId=3 数据断言空 + 写 orgId=3 断言被 stamp 覆盖为 2；`=false`（默认）时回归无变化。
      - Skill: `nop-backend-dev`
- [ ] **Proof**：负向隔离测试通过（enabled=true 隔离生效 / enabled=false 回归零变化）。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] 093/094 orgId 隔离 config-gated 可测（enabled=true 读/写隔离生效，enabled=false 回归）；第二种子组织 + 负向测试落地。

### Phase 3 - finance 读路径 acctSchemaId/orgId filter（095）+ 配对审计列（097）

Status: planned
Targets: `ErpFinReportBizModel.java`、`ErpFinDashboardBizModel.java`、`ErpFinPostingProcessor.java`、`ErpFinAccountingPeriodProcessor.java`、`ErpFinIntercompanyMatchBizModel.java`、`ErpFinConsolidationEliminationBizModel.java`
Skill: `nop-backend-dev`

- Item Types: `Fix | Add`
- Prereqs: Phase 1

- [ ] **Fix（095）**：11 处查询方法补 `eq("acctSchemaId", schemaId)` + `eq("orgId", orgId)` filter——`loadGlBalances`/`loadPostedVoucherLines`/`loadPeriodStatus`/`findLatestPeriodId`/`countBillR`（Report）+ `loadGlBalances`/`loadGlBalancesInRange`/`sumArApOpen`（Dashboard）+ `resolveOpenPeriod`（PostingProcessor）+ `findOrCreatePeriodStatus`/`findUnsettledArApCodes`（AccountingPeriodProcessor）。schemaId/orgId 解析从上下文/参数（多账套经 `SchemaPropagator`）。
      - Skill: `nop-backend-dev`
- [ ] **Fix（097，配对审计列）**：`runMatching:84-91` 填充 arOrgId/apOrgId/arSideVoucherId/apSideVoucherId/materialId（从 INTERCOMPANY_SALE/PURCHASE 凭证 + billR 反查）+ 移除 `setOrgId(1L)` hardcoded；`generateEliminationCandidates` 设 `fromOrgId`/`toOrgId` + `postElimination` 设 `voucher.orgId` per-pair。
      - Skill: `nop-backend-dev`
- [ ] **Add（097，owner doc）**：`multi-company.md:197` 更正 pairKey=billCode（配对凭证共享同一业务单据 code）+ 按 (pairKey, periodId) 分组。
      - Skill: `nop-backend-dev`
- [ ] **Proof**：095 多账套测试——两账套各一张凭证，报表聚合断言不双计（每账套独立）；097 配对测试断言 5 审计列填充 + voucher.orgId per-pair。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] 095 12 查询补 filter 后多账套不双计；097 配对审计列填充 + owner doc 算法更正。

### Phase 4 - ORM UK（096/098）[ORM ask-first] + 099 owner doc + 幂等

Status: planned
Targets: `module-finance/model/app-erp-finance.orm.xml`、`ErpFinIntercompanyMatchBizModel.java`、`docs/architecture/multi-company.md`
Skill: `nop-backend-dev`

- Item Types: `Add | Fix`
- Prereqs: Phase 1 + 人工确认 ORM 变更

- [ ] **Add（096）**：`ErpFinGlBalance` 加 `<unique-key name="UK_FIN_GL_BALANCE_NATURAL" columns="orgId,acctSchemaId,periodId,subjectId,currencyId"/>` + 历史 cleanup 评估。
      - Skill: `nop-backend-dev`
- [ ] **Add（098）**：`ErpFinIntercompanyMatch` 加 `<unique-key name="UK_FIN_INTERCOMPANY_MATCH_PAIR_PERIOD" columns="pairKey,periodId"/>`（替换既有非唯一 IDX）+ `runMatching` 改前置去重（同期同 pairKey 已 MATCHED skip）/ upsert。
      - Skill: `nop-backend-dev`
- [ ] **Fix（098，幂等）**：`runMatching` 捕获 UK ConstraintViolation → skip 既有 pairKey（幂等）+ 负向测试（同期重复调用断言无重复 Match 行）。
      - Skill: `nop-backend-dev`
- [ ] **Add（099，owner doc）**：`multi-company.md:244-249` 强化 org-dimension-enabled 警告 + 多公司部署 checklist 显式要求 `=true` + 默认翻转 successor（破坏向后兼容须独立评估）。
      - Skill: `nop-backend-dev`
- [ ] **Proof**：`mvn clean install -DskipTests` 增量再生 + finance codegen 成功；098 重复 runMatching 断言幂等。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] 096/098 UK 落地 + codegen 成功；098 runMatching 幂等可测；099 owner doc checklist 落地。

## Draft Review Record

- Independent draft review iteration 1: needs revision (ses_04f839fb3ffep5ZKAKZ2hSPTQy) because (a) owner-doc 路径错误——引用不存在的 `docs/architecture/multiple-accounting-schemas.md`（实际为 `docs/design/finance/multiple-accounting-schemas.md`），3 处；(b) 093/094 Explore 回退分支未显式更正 `multi-company.md:29`「所有业务单据按 orgId 隔离查询」owner-doc drift（rule 13：已确认 drift 不得降级为非阻塞 follow-up 而不解决）。非阻塞：12 vs 11 查询站点计数（枚举 11 + ErpFinGlBalanceBizModel 空壳）/ 规模（rule 14 下无需拆分 093/094 vs 095-099）/ app-layer orgId 解析可行性高（IContext attribute + IUserContext.getDeptId 存在）。基线事实全绿（19 data-auth.xml 空 / 平台无 getOrgId / GlMappingResolver cacheKey 塌缩 / runMatching pairKey=billCode + 审计列全空 + orgId=1L hardcoded / GlBalance 无 UK / IntercompanyMatch 非唯一 IDX / 查询站点省略 acctSchemaId 均确认）。
- Independent draft review iteration 2: accept (ses_04f839fb3ffep5ZKAKZ2hSPTQy) after owner-doc 路径全改 `docs/design/finance/multiple-accounting-schemas.md`；093/094 Explore 回退分支显式更正 multi-company.md:29 drift（主路径实现成真 / 回退更正为单组织基线 + successor，两分支均解决 drift）；095 查询计数对齐为「11 处查询方法 + ErpFinGlBalanceBizModel 空壳归 093 transformer」。

## Closure Gates

- [ ] 范围内行为完成（7 项 finding：093/094 orgId 隔离 + 095 12 filter + 096/098 UK + 097 审计列 + 099 owner doc）
- [ ] 相关文档对齐（multi-company.md + multiple-accounting-schemas.md + posting.md）
- [ ] 已运行验证（`mvn clean install -DskipTests` 全绿 + `mvn test` 全绿 + compliance checker 基线不高于 M0）
- [ ] 无范围内项目降级为 deferred/follow-up（093/094 若 Explore 回退则显式 successor + 检测脚手架；099 默认翻转 successor）
- [ ] ORM ask-first 变更（096/098）经人工确认
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### 平台 tenant 机制启用（P1-MA2-093 方案C）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: app-layer IQueryTransformer（方案A）已提供组织级读隔离；平台 tenant 重命名 orgId→nopTenant 是破坏性语义变更 + 全域 useTenant 翻转。
- Successor Required: `yes`（当多租户 SaaS 部署需求明确时）

### GL 映射 org-dimension-enabled 默认翻转（P1-MA2-099）

- Classification: `watch-only residual`
- Why Not Blocking Closure: owner doc 已强化警告 + 多公司部署 checklist 显式要求；默认翻转 true 破坏单组织向后兼容。
- Successor Required: `yes`（当单组织部署基线迁移完成后独立评估默认翻转）

### nop-entropy IServiceContext 加 getOrgId()（093/094 平台内核）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 093/094 经 app-layer 上下文解析实现（Phase 1 Explore 选定路径）；平台内核变更属 nop-entropy 范畴。
- Successor Required: `yes`（当 app-layer 解析不足且需平台统一 orgId 上下文时，提 nop-entropy 变更）

## Closure

Status Note: <待执行 + 独立结束审计>

Closure Audit Evidence:

- <待独立结束审计>

Follow-up:

- 非阻塞；successor 已在 Deferred But Adjudicated 命名触发条件。

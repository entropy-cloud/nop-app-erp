# MA3 设计文档行为基线审计（A3.1）

> Report ID: `2026-07-28-1510-arm-ma3-design-doc-baseline`
> 里程碑：MA3（文档-实现一致性层）/ 工作项 A3.1
> 审计维度：设计文档行为基线（`design-doc-audit-prompt.md` 12 维度 + 维度 1 功能覆盖度外部基准）
> 审计日期：2026-07-28
> Skill：`docs/skills/design-doc-audit-prompt.md`（设计文档行为基线审查专用方法）
> 来源计划：`docs/plans/2026-07-28-1510-2-audit-remediation-ma3-design-doc-baseline.md`
> 外部基准：`docs/analysis/erp-survey/`（16 开源 ERP + 7 补充项目实测）+ `docs/analysis/2026-06-30-1200-feature-coverage-matrix.md`
> 需求基准：`docs/requirements/product-scope.md`
> 审查对象：`docs/design/` 全部文件（7 全局文档 + 18 业务域目录 + portal(future)/notify/l10n + 18 跨域模式文档）
> 审计性质：纯文档审查（不改应用代码；产出为本报告 + arm-index P1 登记 + scope matrix 终态标记）。文档修复在 MR2 批量进行。

## 0. 裁决

**Verdict: FAIL**

理由：本审计**未发现 BLOCKER**（无核心标配功能在设计中完全缺失且无「产品基线外」声明；无生产不安全的 mock/模拟 支付/退款/权限行为；功能覆盖度外部基准对照通过）。**但发现 13 项 MAJOR**（维度 2 产品基线 / 维度 3 稳定与时间敏感责任 / 维度 5 owner-doc 边界 / 维度 6 跨设计一致性 / 维度 9 角色权限 / 维度 12 维护成本重复），分布为系统性模式（非偶发）。按 `design-doc-audit-prompt.md`「有 blocker 或 major 即 fail」裁决 FAIL。

**关键区分**：本审计的 FAIL 是**文档质量层 FAIL**（设计文档作为行为基线的自洽性/边界/覆盖声明质量），非代码/契约/数据破坏。本审计为文档层，**原则上不产生 P0**。所有 blocker/major 登记为 P1（文档类，目标 MR2，依赖 MA3+MA4 done 后由 R2.0 展开机制转化为具体修复工作项行）。minor/note 作为 P2 watch-only 或观察项。

**功能覆盖度（维度 1）单独裁决：PASS**（详见 §1）。功能覆盖度通过不抵消文档质量 FAIL——两者性质不同、不可互抵。

---

## 1. 功能覆盖度结论（维度 1，外部基准）

> 与文档质量结论（§2-§12）分别给出。维度 1 查「该写的功能是否都写了」；维度 2-12 查「已写文档写得好不好」。

### 1.1 外部基准对照

以 `feature-coverage-matrix.md`（113/118 = **95.8%** 总覆盖率）为外部基准，逐项核查：

| 基准系统 | 覆盖率 | nop-app-erp 设计支撑 |
|---------|--------|---------------------|
| Odoo / ERPNext / Metasfresh / iDempiere / Dolibarr/Tryton/l10n / 管伊佳 / 赤龙 / 星云/若依 / IDURAR | 100% | ✅ 完整设计（核心域 + 扩展域 + 跨域模式） |
| WMES/Carbon/OFBiz 补充 | 80% | Scrum 显式排除（非 ERP 核心）；条码/门户已设计 |
| Axelor | 82.4% | 现场服务暂缓（与 maintenance/cs 重叠）；移动端平台能力；GDPR 排除 |
| AureusERP | 80% | 条码已设计（`inventory/barcode-integration.md`）；网站排除（归 nop-app-mall） |

### 1.2 功能深度核查（不仅查域存在）

按 plan 重点核验点抽查开源标配的**功能深度**（非仅域存在）：

| 标配能力 | 来源 | 设计支撑 | 结论 |
|---------|------|---------|------|
| CRM lead scoring | ERPNext/Axelor | `crm/lead-scoring.md` | ✅ 有 dedicated owner doc |
| 销售预测 | ERPNext | `crm/sales-forecast.md` | ✅ 有 dedicated owner doc |
| 替代工艺路线 | Axelor | `aps/alternative-routing.md` | ✅ 有 dedicated owner doc |
| 供应商评分卡 | ERPNext（8-doctype） | `purchase/supplier-evaluation.md` | ✅ 有 dedicated owner doc |
| VMI owner 维度 | Odoo | `inventory/consignment.md` | ✅ 有 dedicated owner doc |
| 批次/序列号 | Odoo/ERPNext | `inventory/README.md` + state-machine | ✅ 有设计 |
| 三单匹配 | ERPNext | `purchase/three-way-match.md` | ✅ 有 dedicated owner doc |
| CPQ | Axelor | `crm/cpq.md` | ✅ 有 dedicated owner doc |
| SPC 失控预警 | Carbon | `quality/spc.md` | ✅ 有 dedicated owner doc |
| MRP 仿真 | Axelor | `manufacturing/simulation-engine.md` | ✅ 有 dedicated owner doc |
| DRP 净需求 | Axelor/ERPNext | `drp/README.md` + 子文档 | ✅ 有 dedicated owner doc |

### 1.3 未覆盖项分级

`feature-coverage-matrix.md §未覆盖项清单`（5 项 + 3 已覆盖/排除）逐项分级：

| 功能 | 状态 | 分级 | 说明 |
|------|------|------|------|
| 现场服务（Intervention） | 🕒 暂缓 | **note**（建议显式声明产品基线外） | 与 maintenance/cs 重叠，有承接域 owner doc |
| POS 零售 | 🕒 待调研 | **note** | 随零售客户触发；非 ERP 核心标配 |
| 移动端 | 🔵 平台能力 | **note** | Nop Platform 原生支持 |
| GDPR 合规 | 🔵 排除 | **note** | 非中国市场需求，已显式排除 |
| 敏捷 Scrum | 🔵 排除 | **note** | 非 ERP 核心，已显式排除 |
| 条码/PDA | ✅ 已设计 | — | `inventory/barcode-integration.md` |
| 电商/网站 | ✅ nop-app-mall | — | 配套产品 |
| 客户/供应商门户 | ✅ 已设计 | — | `portal/README.md`（future extension placeholder） |

**结论**：无核心标配功能（blocker）/ 重要能力（major）在设计中完全缺失且无显式「产品基线外」声明。所有未覆盖项均有显式分级标注（暂缓/待调研/排除/平台能力）。**功能覆盖度维度 PASS**。

### 1.4 feature-inventory 自我声明交叉验证

`feature-inventory.md` 声称的覆盖度经抽查有实质设计支撑（每行指向单一 owner doc）。`feature-inventory.md:7` 自述「本文件不是 backlog。只记录已设计/已支持的功能」——边界清晰。未发现声称能力无实质设计支撑的情况。

---

## 2. 12 维度逐项裁决

> 每维度至少一句裁决（含「本维度无发现」）。

### 维度 2 — 产品基线：**findings（2 MAJOR）**
- 设计文档主体描述正式产品行为，未发现生产不安全的 mock/模拟 支付/退款/权限行为。但 **2 项 MAJOR**：contract/e-signature `MOCK` provider 进入正式 `sign-provider` dict（标注为测试 stub 但将随产品配置发布）+ cs/service-catalog 核心履约流水线标注「DONE 占位」（设计 §三 承诺完整多步履约，实现标记为占位）。另有 dashboards 替代指标模式（见维度 3 MAJOR）+ master-data MockExchangeRateApiClient 默认 provider 措辞（MINOR）。

### 维度 3 — 稳定与时间敏感责任：**findings（1 MAJOR，系统性）**
- **系统性实现状态泄漏**：全域设计文档普遍承载 `已落地`/`已实现`/`待实现`/`plan 2026-07-XX-XXXX-X` 引用/`裁决 N`/`实现偏离补注（plan ...）` 等**执行状态**而非稳定产品行为。最密集：`dashboards.md §实现状态`（整节 roadmap/plan-status）、`sales/README.md`（✅ 已实现 标记）、`finance/posting.md`/`period-close.md`/`posting-log.md`（A1/A2/A3 段 + 裁决 1/2/3 + plan refs）、`master-data/unified-party-identity.md`（plan 状态横幅 + Phase 落地标记）、`logistics/README.md`（实现状态补注）。owner-doc 「Deferred」**范围边界**注记可接受（描述产品基线排除什么），但 `已落地（plan XXX）`/`待实现`/`裁决 N` 是执行状态非稳定行为，属维度 3 违规。

### 维度 4 — 需求对齐：**findings（1 MAJOR）**
- `docs/requirements/product-scope.md` 里程碑框架陈旧：将状态描述为「codegen skeleton done / 下一步编写核心 BizModel / 成功指标=154 模块可编译 + 首域 CRUD」，而 AGENTS.md + 设计文档反映项目已处「业务逻辑深化与运营成熟度收尾」（M1-M5 全 done + 报表 + 看板）。**矛盾分类：需求差距（需求文档未维护），需人工决策**是否推进里程碑框架。设计文档声明「已支持」与需求文档说「下一步」构成冲突。

### 维度 5 — Owner-doc 边界：**findings（4 MAJOR，系统性）**
- **系统性 schema/Java/实现细节泄漏**到设计文档（应路由 orm.xml/architecture）：
  - **finance**（MAJOR）：`posting.md`（三层架构含表名 `stock_move/ledger/balance` + `@BizMutation` + Java 接口签名 `IErpFinAcctDocProvider`/`IErpFinFactsValidator` + 类名 `ErpFinPostingProcessor.markOriginalVoucherReversed` + plan refs + error codes）/ `posting-log.md`（裁决 1/2/3 plan 决策档案 + 类名）/ `gl-mapping-rules.md`（全字段表含类型/精度/FK + resolver 算法伪代码 + cache 设计 + file:line refs + 28 Provider 接入清单 + browser E2E 证据）/ `period-close.md`（实现范围注记含类名 + config keys + 向导组件选择裁决 + E2E 路径）。
  - **master-data**（MAJOR）：`cross-border-trade.md`（9 字段表含 code/type/precision/dict + `ErpMdMaterialCustoms` 全字段表 + UK/Index specs + 落地证据段）/ `unified-party-identity.md`（plan 执行记录：Plan Status 横幅 + Java 接口签名 + 实现文件路径表 NEW/既有 状态 + 性能数据 + 测试基线）/ `sku-multi-unit.md`（字段表 + Java 代码块 + XML 片段）。
  - **8 第二批扩展域 README**（MAJOR）：`crm/cs/hr/aps/contract/drp/logistics/b2b` README 系统性重复逐字段 schema（含 🟢 evidence 标记）+ Java SPI 契约 + DTO 包列表。第一批扩展域（assets/projects/manufacturing/quality/maintenance）遵守 owner-doc 边界规则（散文描述 + 引用 orm.xml）；第二批扩展域 README 风格漂移为「设计 + schema 引用 + 调研对照」混合。
  - **logistics 缺 architecture 拆分**（MAJOR）：b2b 有明确双层分工（`design/b2b/README.md` 业务语义 + `architecture/b2b-integration.md` 集成契约），logistics 无对应 `architecture/logistics-integration.md`——三层 Java SPI 定义 + Registry 类 + DTO 包列表内联在设计文档，与 b2b 模式不对称。

### 维度 6 — 跨设计一致性：**findings（2 MAJOR）**
- **admin/super-admin 角色身份冲突**（MAJOR）：`app-overview.md` 列两级管理员（超级管理员=全系统 + 管理员=限定职责范围），`roles-and-permissions.md` 仅定义一级「管理员」（= superuser via `skip-check-for-admin=true`）。敏感操作（反审核/作废/反结账）所有权在两文档冲突时不清。
- **8 第二批扩展域从导航/矩阵/看板覆盖中沉默遗漏**（MAJOR）：`app-overview.md` 主要导航仅含 11 核心域组（crm/cs/hr/aps/contract/drp/logistics/b2b 缺席）；`domain-design-guidelines.md §1.1` 单一职责矩阵仅含 10 域；`dashboards.md` 设计 10 看板无第二批扩展域看板且无「产品基线外」声明。读者推断 8 域不属于管理壳。

### 维度 7 — 域语言与有界上下文：**pass（无发现）**
- 全域使用自然业务语言（资产卡片/折旧/工单/质检/合同/线索/商机/...），类名仅在有意引用持久化/代码真相源时出现。跨域流程描述为业务工作流（采购到付款/销售到收款/期末结账/成本核算）。每主要业务概念有自然所属域文档。**本维度无发现**。

### 维度 8 — 工作流与状态清晰度：**findings（2 MINOR + 已登记 MA2 P2 drift）**
- 状态机文档遵循 `state-machine-business-review-prompt.md` 10 维度模板（状态定义/迁移表含触发者/前提/结果/终端异常恢复/角色/TODO 升级/场景走查）。**仅核验文档清晰度，不重做 MA2 状态机正确性裁决**。
- 2 MINOR：`purchase/returns.md`/`sales/returns.md` 将 `returnStatus`/`refundStatus` 作为状态轴列入三轴表，但相邻注记说它们是派生视图（非存储字段）——仅读表的实现者会误建模。
- MA2 已登记的 owner-doc drift（多状态承载实体缺独立章节，P2-MA2-065/067/068/069/070/071）在本审计维度下确认归类为「文档可读性缺陷」（dim 8 清晰度），无运行时影响，维持 P2 watch-only。详见 §5 复核表。

### 维度 9 — 角色权限与受保护操作：**findings（1 MAJOR + 维度 6 共享）**
- **8 第二批扩展域无角色/权限基线**（MAJOR）：`roles-and-permissions.md:121`「SUB 域（CRM/CS/HR/APS/Logistics/B2B/Contract/DRP）：…尚未定义独立 ERP 角色映射」。这些域在 supported baseline 内（feature-inventory 标「完整设计」），至少 3 个承载敏感操作（HR 工资 xwf 审批 / 合同审批电子签 / B2B EDI 对账）。「尚未定义」是执行状态非范围边界。敏感行为缺 owner-doc 基线。
- admin 身份冲突见维度 6。

### 维度 10 — 页面与交互行为：**pass（1 NOTE）**
- 重要页面/交互结果在业务级涵盖（验证/资格/空错误状态/用户可见反馈）。ui-patterns.md form-XML 模板属维度 5 边界问题（非 dim 10）。**本维度无 finding，1 NOTE**（form-XML 模板含 plan-status provenance，属 dim 5 范畴）。

### 维度 11 — 配置与操作语义：**pass（1 MINOR）**
- `domain-design-guidelines.md §17` 清晰分离业务配置（三级覆盖链）与技术机制。每域配置点表多数为业务配置。1 MINOR：logistics/b2b 等配置点表混入技术运行配置 key（`erp-log.gateway-timeout-secs`/`erp-log.log-retention-days`/`erp-b2b.async-send-cron`）——建议分「业务配置」/「运行配置」子标题。

### 维度 12 — 维护成本与重复：**findings（2 MAJOR + 1 MINOR）**
- **危险操作审计重复 4 处**（MAJOR）：`roles-and-permissions.md` §高危操作权限 + §审批与审计要求 + `domain-design-guidelines.md` §6.3 危险操作审计 + §8.4 危险操作审计——同一事实集（反审核/反结账/红字冲销）4 个维护点，将漂移。
- **状态码目录重复**（MAJOR）：`flow-overview.md §3.1/§3.2/§3.3` 发布逐单据 status 值集，`domain-design-guidelines.md §16.2` 是声明单一权威——两设计文件拥有同一 status-code 目录。`domain-glossary.md:89` 正确延迟到 §16 + orm.xml。
- 1 MINOR：`processor-delegation-auto-gen.md` deprecated 但保留（自标记为设计演进历史参考）——可接受，或归 archive/。

---

## 3. 需求/设计冲突分类摘要

| 冲突 | 分类 | 说明 |
|------|------|------|
| product-scope.md 里程碑陈旧 vs 设计文档反映先进状态 | **需求差距**（需人工决策） | 需求文档未维护；设计超前。建议更新 product-scope 里程碑框架或显式标注滞后 |
| app-overview 两级管理员 vs roles 一级管理员 | **设计漂移** | 两设计文档对同一概念（管理员）冲突命名/范围；MR2 选定单一模型并对齐 |
| dashboards 指标表数据源 vs 实现状态注记 | **设计漂移**（内部矛盾） | 指标表声明的数据源被实现状态注记推翻；MR2 使指标表权威化 |
| 设计文档「Deferred」注记 vs 代码实现 | **有意基线变更/Deferred** | owner-doc Deferred 范围边界注记可接受；但 `已落地（plan XXX）`/`待实现` 执行状态属 dim 3 违规（非设计-需求冲突，属文档卫生） |

## 4. Owner 边界摘要

维度 5 是本次审计 findings 最密集的维度（4 MAJOR）。系统性模式：
- **第一批扩展域 + 核心域多数文档**遵守边界（散文描述业务语义 + 引用 orm.xml）。
- **finance 4 份核心文档**（posting/posting-log/gl-mapping-rules/period-close）混合设计意图与架构实现细节（表名/Java 接口/类名/算法/cache/plan 决策）。
- **master-data 3 份文档**（cross-border-trade/unified-party-identity/sku-multi-unit）是 plan 执行记录/字段表转录。
- **8 第二批扩展域 README** 风格漂移（schema/DTO/SPI 重复）。
- **logistics 缺 architecture 拆分**（与 b2b 模式不对称）。
- **正面范例**：`domain-glossary.md`（最干净的边界纪律）、`feature-inventory.md`（清晰产品 vs backlog 框架）、`multiple-accounting-schemas.md`/`use-cases.md`（finance 内合规范例）、第一批扩展域 README。

## 5. 域边界摘要

维度 7 PASS。每主要业务概念有自然所属域文档；跨域流程描述为业务工作流。b2b 双层分工（设计业务语义 + architecture 集成契约）是正面范例。第一批与第二批扩展域 README 风格差异（见 §4）是 owner-doc 边界问题而非域边界问题——概念归属本身清晰。

## 6. 维护成本摘要

维度 12 findings（2 MAJOR + 1 MINOR）：危险操作审计 4× 重复 + 状态码目录 2× 重复。两者均为「同一事实集多维护点」，MR2 指定单一 owner（roles-and-permissions.md / guidelines §16.2）后其他位置改引用。

---

## 7. Finding 清单（按严重性）

> 所有 blocker/major 登记 P1（文档类，目标 MR2）。minor/note 作 P2 watch-only 或观察项。

### BLOCKER（0 项）

无。无核心标配功能在设计中完全缺失且无显式「产品基线外」声明；无生产不安全的 mock/模拟 支付/退款/权限行为。

### MAJOR（13 项 → P1-MA3-001 ~ P1-MA3-013）

| ID | 维度 | 受影响文件 | 问题 | 重要性 | 建议处理方式 |
|----|------|-----------|------|--------|-------------|
| **P1-MA3-001** | Dim 3 | 全域（sales/README.md / finance/posting.md,period-close.md,posting-log.md / master-data/unified-party-identity.md / logistics/README.md / 多域 state-machine.md 实现偏离补注 / dashboards.md §实现状态 等） | **系统性实现状态泄漏**：`已落地`/`已实现`/`待实现`/`plan XXX`/`裁决 N`/`实现偏离补注` 承载执行状态而非稳定产品行为 | 设计文档 doubling 为执行状态/plan 档案；未来实现者无法区分稳定基线 vs 进行中工作；状态标记腐烂快于行为 | 移除 plan refs / 已落地/待实现 标记到 backlog/plans；保留仅产品基线范围边界（Non-Goal/out-of-baseline）；实现偏离补注若为新的稳定真相则折回主文删除补注块 |
| **P1-MA3-002** | Dim 5 | finance/posting.md, posting-log.md, gl-mapping-rules.md, period-close.md | **finance 核心文档混合设计与架构实现**：表名（stock_move/ledger/balance）、Java 接口签名（IErpFinAcctDocProvider/IErpFinFactsValidator）、类名（ErpFinPostingProcessor.markOriginalVoucherReversed）、算法/cache 设计、plan 决策档案（裁决 1/2/3）、28 Provider 接入清单、browser E2E 证据 | 双真相源（此 doc vs architecture doc）；冻结实现选择到设计层；设计读者不需 Java 签名理解业务行为 | 剥离 Java 签名/类名/表名/plan refs/error codes/E2E 路径到 architecture/；保留业务语义（SYNC/ASYNC 契约/幂等/红冲双方向/businessType→模板映射） |
| **P1-MA3-003** | Dim 5 | master-data/cross-border-trade.md, unified-party-identity.md, sku-multi-unit.md | **master-data 文档为 plan 执行记录/字段表转录**：字段表含 code/type/precision/dict + UK/Index specs + plan Status 横幅 + Java 接口签名 + 实现文件路径表 NEW/既有 + 性能数据 + 测试基线 | schema 漂移不可检测；新读者无法区分稳定行为 vs Phase 实现证据 | 字段表替换为语义摘要 + orm.xml 引用；plan 状态/性能数据/落地证据移到 plan/log |
| **P1-MA3-004** | Dim 5 | crm/cs/hr/aps/contract/drp/logistics/b2b README.md | **8 第二批扩展域 README 重复逐字段 schema/DTO/SPI 契约**（第一批扩展域遵守边界，第二批风格漂移为「设计+schema 引用+调研对照」） | 两真相源（README 表 vs orm.xml）；schema 漂移；README 读为调研论文而非稳定基线 | 每第二批 README 重构为第一批风格（核心业务对象散文表 + 跨域协作 + 业务规则）；schema 移到 orm.xml（已在）；Java SPI/DTO 移到 architecture；🟢 evidence 表移到 erp-survey（已在） |
| **P1-MA3-005** | Dim 5 | logistics/README.md | **logistics 缺 architecture 拆分**：三层 Java SPI 定义 + Registry 类 + DTO 包列表内联设计文档，与 b2b 双层分工模式不对称 | SPI 签名变更时设计文档维护负债；业务语义 owner doc 部分技术化 | 新建 `docs/architecture/logistics-integration.md`（镜像 b2b-integration.md），SPI/Registry/DTO 契约移入，设计 README 留一段摘要 + 链接 |
| **P1-MA3-006** | Dim 2 | contract/e-signature.md:205-208, customer-service/service-catalog.md:299-312 | **占位/scaffold 行为泄漏正式设计**：(a) e-signature `MOCK` provider 进入正式 `sign-provider` dict（标注测试 stub 但随产品配置发布）；(b) cs/service-catalog 核心履约流水线多数标「DONE 占位（登记审计），实际逻辑归 successor」而 §三 设计承诺完整多步履约 | 未来实现者无法区分承诺产品行为 vs 脚手架；e-signature MOCK dict 值风险生产用户选择；service-catalog 设计承诺的履约流被标记占位 | e-signature：MOCK 移到测试 dict 或 test profile gate；设计澄清仅 3 真实 provider 是产品基线。service-catalog：更新 §三 匹配收窄范围（CREATE_TICKET + 审计登记是基线；多步履约显式 out-of-baseline）或顶部加「当前基线范围」callout 移除 per-row 占位补丁表 |
| **P1-MA3-007** | Dim 2+3 | dashboards.md:154,171,191,222-245 | **dashboards 三联缺陷**：(a) §实现状态 整节 roadmap/plan-status（plan refs + 触发条件 + 「本期仅交付」）；(b) 指标表数据源 vs 实现状态注记内部矛盾（齐套不足/OEE/不合格原因 标称数据源被注记推翻为替代）；(c) §实现约定 前端 AMIS 实现机制 + bug-fix 约定（amis-core dataMapping + ${'$'} 转义 + 禁止镜像 bug 范式） | 稳定设计文档作为进度跟踪器腐烂；指标表权威性被注记削弱；前端实现机制属 architecture | 删除 §实现状态；指标表权威化（用实际基线化数据源/公式）；前端约定移到 architecture；richer 指标若真 out-of-baseline 则行内标「(产品基线外)」+ 一行理由 |
| **P1-MA3-008** | Dim 6+9 | app-overview.md:40-46 vs roles-and-permissions.md:39,117 | **admin/super-admin 角色身份冲突**：app-overview 列两级（超级管理员=全系统 + 管理员=限定职责范围），roles 仅定义一级（管理员=superuser via skip-check-for-admin=true） | 敏感操作（反审核/作废/反结账）所有权在两文档冲突时不清 | 选定单一模型；使 app-overview 角色摘要为 roles-and-permissions.md 严格子集 |
| **P1-MA3-009** | Dim 6 | app-overview.md:16-26, domain-design-guidelines.md:20-31, dashboards.md(whole) | **8 第二批扩展域从导航/矩阵/看板覆盖沉默遗漏**：app-overview 主要导航仅 11 核心域组；guidelines §1.1 单一职责矩阵仅 10 域；dashboards 设计 10 看板无第二批扩展域且无「产品基线外」声明 | 「应用总览」under-report 支持面；读者推断 8 域不属于管理壳 | 加 8 导航组到 app-overview 或加显式指针；扩展 §1.1 矩阵或加指针；dashboards 加范围边界声明 |
| **P1-MA3-010** | Dim 9 | roles-and-permissions.md:121 | **8 第二批扩展域无角色/权限基线**：CRM/CS/HR/APS/Logistics/B2B/Contract/DRP「尚未定义独立 ERP 角色映射」。至少 3 域承载敏感操作（HR 工资/合同审批电子签/B2B EDI 对账） | 敏感行为缺 owner-doc 基线 | 为敏感子集定义角色映射，或显式声明「角色映射归 successor（触发条件：…）」为 deferred 基线范围边界 + 敏感操作单独升级；非沉默「尚未定义」 |
| **P1-MA3-011** | Dim 4 | docs/requirements/product-scope.md:50-73 | **product-scope 里程碑框架陈旧**：描述状态为「codegen skeleton done / 下一步编写核心 BizModel / 成功指标=154 模块可编译+首域 CRUD」，而实际已 M1-M5 全 done + 报表 + 看板 | 设计声明「已支持」vs 需求说「下一步」构成冲突；需求文档未维护 | 更新 product-scope 里程碑/成功指标到当前阶段，或显式标注滞后设计并路由推进到 backlog（需人工决策） |
| **P1-MA3-012** | Dim 12 | roles-and-permissions.md:53-64,77-82 + domain-design-guidelines.md:225-229,307-311 | **危险操作审计重复 4 处**：同一事实集（反审核/反结账/红字冲销）4 个维护点，列略不同 | 4 维护点将漂移（一处加操作其余不同步） | 指定单一 owner（roles-and-permissions.md 作权限/审计基线）；其余 3 处改引用 |
| **P1-MA3-013** | Dim 12 | flow-overview.md:284-313 vs domain-design-guidelines.md:543-565 | **状态码目录重复**：flow-overview §3.1/§3.2/§3.3 发布逐单据 status 值集，guidelines §16.2 是声明单一权威 | 两设计文件拥有同一 status-code 目录，漂移风险 | flow-overview §3 仅保留跨域映射语义（哪个上游 status 触发哪个下游），字面值枚举替换为指向 §16.2 的指针 |

### MINOR（8 项 → P2 watch-only）

| ID | 维度 | 受影响文件 | 问题 | 建议处理方式 |
|----|------|-----------|------|-------------|
| P2-MA3-014 | Dim 5 | app-overview.md:30-36 | 平台 codegen 机制（三层文件链/x:extends/x:override）在设计文档 | 裁剪为权威声明 + 单指针 |
| P2-MA3-015 | Dim 5 | flow-overview.md:495-499,552 | 事务传播具体值（REQUIRED/REQUIRES_NEW）+ 接口名在设计 | 保留业务结果；传播值移到 architecture |
| P2-MA3-016 | Dim 5+3 | roles-and-permissions.md:123-138 | 运行时配置 + 灰度启用 runbook 在设计 | 保留一行产品基线语义；runbook 移到 architecture/ops |
| P2-MA3-017 | Dim 3 | domain-design-guidelines.md:556,561,565 | §16.2 内联「M-4 修正：早期版本误用…」执行历史注记 | 删除「M-4 修正：早期版本…」子句；保留权威状态集 |
| P2-MA3-018 | Dim 8 | purchase/returns.md:80-83, sales/returns.md:81-86 | returnStatus/refundStatus 作状态轴列入表但实为派生视图 | 表内行内标「(派生视图，非存储字段)」或移除轴引用推导 |
| P2-MA3-019 | Dim 5 | 各域 ui-patterns.md form-XML 模板（含 plan-status provenance） | form-XML 模板含 plan refs，属 view.xml 实现内容 | 保留设计原则 + 页面要点；form-XML 模板移到 architecture/ 或各域 web 模块 |
| P2-MA3-020 | Dim 11 | logistics/b2b 等配置点表 | 技术运行配置 key（gateway-timeout/log-retention/cron）混入业务配置 | 配置点表分「业务配置」/「运行配置」子标题 |
| P2-MA3-021 | Dim 2 | master-data/exchange-rate-management.md:65 | MockExchangeRateApiClient 标为「默认 provider」措辞模糊产品 vs 测试 | 改写为「Test/stub provider for development（config-gated 默认关）；生产 provider 经 SPI 可插拔」 |

### NOTE（观察项，不强制修复）

- `domain-design-guidelines.md` 显式 architecture-deferral 头（§七/§九/§十八）是良好边界意识范例。
- `portal/`（future extension placeholder）+ `l10n/cn-golden-tax.md`（设计阶段标注）+ `notify/README.md`（跨域子系统定位）是 scope-boundary 标注正面范例。
- 跨域模式文档集（18 份）各有单一自然 owner，相互引用非重复——连贯且边界清晰。
- `processor-delegation-auto-gen.md` deprecated 保留可接受（自标记设计演进历史）；或归 archive/。
- `feature-inventory.md` 自述「不是 backlog」边界清晰，是产品-vs-backlog 框架正面范例。
- `domain-glossary.md` 是最干净的 owner-doc-boundary 范例（延迟字段/字典/状态码到 orm.xml + §16.2）。

---

## 8. MA2 已登记 owner-doc drift 复核表

> 复核 MA2 审计中标注的 owner-doc 不一致项，确认其在本审计维度下的分类与归属。

| Finding ID | MA2 描述 | 本审计复核 | 分类/归属 | 处理 |
|-----------|---------|-----------|----------|------|
| **P1-MA2-067** | closeProject (OPEN→COMPLETED) 未强制任务已结束，owner doc §迁移完整性 L35 用词「任务已结束（**或确认剩余不再执行**）」软门控 | 确认：owner doc 文字软门控 vs 代码完全缺校验。本审计维度 8（工作流/状态清晰度）：owner doc **声明**前置但用词软（「或确认」），代码漂移。属 **owner doc vs 代码 drift**（设计声明 vs 实现不一致） | 交接 **A3.3-A3.5**（owner doc vs 代码 drift 逐行核对）；本审计（设计文档内部质量）维度 8 清晰度层面：owner doc 声明存在但措辞软，维持 P1-MA2-067 归属不变 | 维持 P1-MA2-067（目标 MR1 代码/契约层）；本审计不重复登记 |
| **P1-MA2-073** | b2b EDI 出站自动化（自动发送+ACK-timeout+自动重试+ERROR 升级）全部缺失，owner doc state-machine.md 自动化承诺 vs README/MFT transport Deferred 文档不一致（P2-MA2-068） | 确认：owner doc **内部不一致**（state-machine.md §L-8/§6/§8/§9 自动化控制点 vs README.md + managed-file-transfer.md transport Deferred）。本审计维度 6（跨设计一致性）：b2b 三份文档（state-machine.md / README.md / managed-file-transfer.md）对自动化承诺内部矛盾 | **本审计维度 6 确认 P2-MA2-068**（owner doc 内部不一致，文档层）+ P1-MA2-073（自动化缺失，代码层） | P2-MA2-068 维持 P2 watch-only（文档层，目标 MR1 顺手）；P1-MA2-073 维持（代码层，目标 MR1） |
| **P2-MA2-065** | projects state-machine.md 缺 5 状态承载实体独立章节 | 确认：本审计维度 8（清晰度）确认归类为「文档可读性缺陷」（每实体状态机经代码 + plan 证据可追溯） | 维持 P2 watch-only（文档清晰度，目标 MR1 顺手） | 维持 P2-MA2-065 |
| **P2-MA2-067** | cs NEW>1h/ASSIGNED>2h 滞留升级未实现 + findSlaWarnings 无 scheduler | 确认：owner doc §避免工单滞留 声明 vs 实现仅 deadline-based。属 owner doc vs 代码 drift（设计声明 vs 实现缺口） | 交接 A3.3-A3.5；本审计维度 8 清晰度：owner doc 声明存在 | 维持 P2-MA2-067 |
| **P2-MA2-068** | b2b state-machine.md 自动化承诺 vs README/MFT transport Deferred 文档内部不一致 | **本审计维度 6 直接确认**（见 P1-MA2-073 行）——b2b 三文档内部矛盾是跨设计一致性问题 | 维持 P2 watch-only（文档层，目标 MR1 顺手） | 维持 P2-MA2-068 |
| **P2-MA2-069** | b2b TO_CANCEL dict 死状态（owner doc §2 ASCII 图两步迁移 vs 代码单步） | 确认：owner doc 声明两步取消中间态 vs 代码单步。属 owner doc vs 代码 drift | 交接 A3.3-A3.5；本审计维度 8：owner doc 声明存在但与代码漂移 | 维持 P2-MA2-069 |
| **P2-MA2-070** | 5 域（crm/cs/contract/b2b/maintenance）state-machine.md 缺多状态承载实体独立章节 | 确认：本审计维度 8 确认归类为「文档可读性缺陷」（与 P2-MA2-065 同型，全域模式） | 维持 P2 watch-only（文档清晰度，目标 MR1 顺手） | 维持 P2-MA2-070 |
| **P2-MA2-071** | aps+logistics state-machine.md 缺 Schedule/排产引擎/ShipmentLog/网关 SPI/轮询/过账 Facade 独立章节 | 确认：本审计维度 8 确认归类为「文档可读性缺陷」（同型） | 维持 P2 watch-only（文档清晰度，目标 MR1 顺手） | 维持 P2-MA2-071 |

**复核结论**：MA2 已登记 owner-doc drift findings 在本审计维度下分类与归属**全部确认一致**，无升级/降级/重新分类。其中：
- **文档内部不一致类**（P2-MA2-068）本审计维度 6 直接复核确认。
- **owner doc 声明 vs 代码 drift 类**（P1-MA2-067 / P2-MA2-067/069）属设计 vs 实现不一致，归 A3.3-A3.5 owner doc vs 代码 drift 审计逐行核对；本审计（设计文档内部质量）层面确认 owner doc 声明存在（清晰度层面通过）。
- **文档可读性缺陷类**（P2-MA2-065/070/071）本审计维度 8 确认归类，维持 P2 watch-only。
- **无 MA2 owner-doc drift finding 需在本审计重新登记为 MA3 P1**——它们已在 MA2 适当登记，本审计仅复核确认分类。

---

## 9. 高风险设计-代码背离交接标注

> 本审计为设计文档**内部**质量（自洽/边界/覆盖声明）。若发现设计声明与已落地代码严重背离致错误实现风险，标注并交接 A3.3-A3.5（owner doc vs 代码 drift）/ A4.6-A4.8（view.xml drift）。

| 交接项 | 描述 | 交接目标 |
|--------|------|---------|
| closeProject 软门控 | P1-MA2-067 owner doc §迁移完整性 声明前置但代码完全缺校验 | A3.3-A3.5（projects owner doc vs 代码 drift） |
| b2b EDI 自动化承诺 | P1-MA2-073 owner doc state-machine.md 自动化控制点 vs 代码全部缺失；P2-MA2-068 三文档内部矛盾 | A3.3-A3.5（b2b owner doc vs 代码 drift） |
| cs 滞留升级 / b2b TO_CANCEL | P2-MA2-067/069 owner doc 声明 vs 代码缺口/单步简化 | A3.3-A3.5（cs/b2b owner doc vs 代码 drift） |
| 多域 state-machine.md 缺独立章节 | P2-MA2-065/070/071 文档可读性（无运行时影响） | MR1 顺手（文档清晰度，非 drift） |
| dashboards 指标表 vs 实现替代 | P1-MA3-007 指标表数据源被实现状态注记推翻 | A4.6-A4.8（view.xml drift）/ MR2（文档层使指标表权威化） |

**说明**：本审计未发现新的致错误实现的高风险设计-代码背离（超越 MA2 已登记项）。上述交接项均为 MA2 已登记 drift 的归属确认，非新发现。

---

## 10. 剩余风险与跳过区域

### 剩余风险
1. **维度 3 实现状态泄漏是系统性模式**——MR2 批量 scrub 工作量大（全域设计文档），若不彻底则标记持续腐烂。建议 MR2 优先处理高密度文档（dashboards.md / sales/README.md / finance posting 系列 / master-data unified-party-identity.md）。
2. **维度 5 第二批扩展域 README 风格漂移**——8 份 README 重构为第一批风格工作量大；但 b2b 拆分模式已建立可复用。重构前 schema 漂移风险持续。
3. **8 第二批扩展域无角色基线**（P1-MA3-010）——多公司部署（owner doc project-vision 定位「产品化通用 ERP」）一旦深化，敏感操作（HR 工资/合同/B2B 对账）权限边界不清是安全风险。MR2 应至少为敏感子集定义角色映射。
4. **product-scope.md 陈旧**（P1-MA3-011）——需求文档与设计脱节，影响后续需求综合的可信度。

### 跳过区域
- **状态机正确性重审**——MA2（A2.5-A2.15）已 done；本审计仅核验状态文档业务级清晰度。
- **前瞻性缺失扫描**——A3.2（design-completeness-scan-prompt.md）找「从未设计的整个功能」；本审计（design-doc-audit）审「存在的内容」质量。维度 1 仅对已有文档声称覆盖做外部基准交叉验证 + 标注明显缺口。
- **owner doc vs 代码逐行 drift**——A3.3-A3.5 专项；本审计标注交接。
- **API 契约 vs 实现一致性**——A3.6。
- **索引路由有效性**——A3.7。
- **可定制性验证**——A3.8。
- 部分扩展域子文档（hr/payroll-simulation.md / crm/cpq.md / manufacturing/subcontracting.md 等）未逐行审计——采样确认 README 级 findings 在子文档级重复（尤其「待 ORM 计划落地」/「实现偏离补注」模式）。完整 sweep 预计发现 2-3× 同型 dim 3 实例。

---

## 11. 审计方法与证据

- **方法**：`docs/skills/design-doc-audit-prompt.md`（12 维度 + 功能覆盖度外部基准 + 严重性指南 blocker/major/minor/note）。
- **审查范围**：`docs/design/` 全部文件——7 全局文档（app-overview / domain-design-guidelines / domain-glossary / flow-overview / roles-and-permissions / dashboards / feature-inventory）+ 18 业务域目录（master-data / inventory / purchase / sales / finance / assets / projects / manufacturing / quality / maintenance / crm / customer-service / human-resource / aps / contract / drp / logistics / b2b）+ portal(future) / notify / l10n + 18 跨域模式文档。
- **外部基准**：`docs/analysis/erp-survey/`（40+ 文件，16 开源 ERP + 7 补充项目实测）+ `docs/analysis/2026-06-30-1200-feature-coverage-matrix.md`（113/118 = 95.8% 覆盖率）。
- **需求基准**：`docs/requirements/product-scope.md`。
- **MA2 drift 复核基准**：`docs/audits/arm-index.md`（P1-MA2-067/073 + P2-MA2-065/067/068/069/070/071）。
- **执行方式**：3 个独立 fresh-context 子代理并行审查（全局文档簇 / 核心域簇 / 扩展域+模式簇），各自按 12 维度产出 findings；主审计者综合 + 维度 1 外部基准对照 + MA2 drift 复核 + 交接标注。
- **本审计不改应用代码/文档**（产出为报告 + arm-index P1 登记 + scope matrix 标记）。文档修复在 MR2 批量进行。

---

## 12. 终态标记建议

- **scope matrix §2.3「设计文档基线」行**：`❓` → `⚠️(P1)`（13 项 MAJOR → P1 待 MR2；功能覆盖度维度 PASS 但文档质量维度 FAIL）。
- **roadmap A3.1**：`todo` → `done`（经独立 closure audit）。
- **arm-index §P1 汇总**：新增 P1-MA3-001 ~ P1-MA3-013（13 项，目标 MR2）。
- **arm-index 报告清单**：新增本报告行。

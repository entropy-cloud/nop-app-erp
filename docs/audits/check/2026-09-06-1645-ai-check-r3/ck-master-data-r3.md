# ck-master-data-r3 — master-data U01 五维符合性审计报告（ai-check-r3 M1.15）

> 工作项：M1.15（六单元全格；本报告 = U01 master-data 格——全域 FK 上游被引用面；姊妹格见 `ck-maintenance-r3.md` / `ck-aps-r3.md` / `ck-logistics-r3.md` / `ck-notify-r3.md` / `ck-common-r3.md`）。
> 执行日期：2026-09-09。审计时点 T0：HEAD `e331c55b2`（脏面 = 1 条 untracked 计划文件，tracked 零修改）——口径同 `ck-maintenance-r3.md` 头注。
> 判定依据（冻结）：`m0-5-audit-checklists.md` §1/§2 + §3.3 U01 行（含域内增量程式 `grep -c 'notGenCode'`）+ §6 勘误 E1。
> 范围：master-data 全域——伙伴身份（partner + address/contact + 统一伙伴身份）/物料（material + sku + category + customs + price tier）/UoM 换算（物料级/通用级双层 + convertQty）/汇率（exchange rate + MUTEX 钩子 + API 刷新 processor + DateRange 试点族）/科目与账套（subject/coa/acct_schema/subject_mapping/cost_center）/组织与仓库（organization/location/warehouse/employee）/税则与结算（tax_rate/settlement_method/bank_account）/供应商准入（supplier_approval 状态机 + suspendByPartner）/看板与报表（`module-master-data/erp-md-{dao,service}` src/main）；owner docs `docs/design/master-data/`（exchange-rate-management/sku-multi-unit/unified-party-identity/cross-border-trade/data-migration/README/use-cases/seed-data）。Skills：`nop-platform-conformance-audit-prompt` + `code-quality-audit-prompt`（DIM-B）；其余维 Skill: none。
> 切片边界（§3.2）：posting 引擎归 fin-1（md 无过账面）；common 抽象族行为归 U20（SupplierApproval 有 approveStatus 列 → F1.3 守卫**激活**样本；其余实体无状态锁列惰性；save 通道盲区 common-011-r3）；聚合横切面归 U21；notify 本体归 U11（md 零消费点）。
> 零生产代码改动：本报告 + 双索引 + 计划勾选注记为唯一产物。

## 1. 五维覆盖矩阵（5/5 完整）

| 维度 | 判定面（U01 全格） | 机械核查结果 | verdict |
| --- | --- | --- | --- |
| **DIM-B 后端平台合规** | §1.1 全套 + 域内增量 `grep -c 'notGenCode' app-erp-master-data.orm.xml` + 15 维度走查（域焦点：跨域被引用面——机制 B notGenCode/汇率 MUTEX 钩子/价格链 helper/SKU 多单位；共性②⑧⑨⑮）+ ⑮抽样 7 断言 | 反模式族全零（RuntimeException=0/@Inject private=0/System.currentTimeMillis=0/@Transactional 0/LocalDate.now=0）；checker 零漂移；XGEN 21 处全为 erp-md-meta dict 校验点；**notGenCode 计数 = 0**（md 为被引用侧，外部实体声明在 drp 等引用方——机制 B 方向正确，drp 侧 lesson-01 零表名双重拼接已由 M1.14 复核）；②IDaoProvider 10 文件：5 处有豁免/理由注释（Party/Dashboard/RefreshProcessor/SuspendByPartner 循环注入理由/OrganizationReferenceChecker），3 处无专项注记（SubjectMappingResolver:25-26/ErpMdReportBizModel:75-78/AcctSchemaResolver:31）→ 归 common-014-r3 全仓族（md 5 文件在缺注释清单）；⑧partner-type EMPLOYEE/CUSTOMS_BROKER **零业务 writer 死值**（仅生成层常量声明——r1 无 dict 死值条目，但全 18 域引用的伙伴类型字典死值面 = 审计登记观察项，准入实体 supplier-approval-status 5 值全活 + price-validation 3 值全活 + active-status 全活）；⑨保留层 `_erp-md.action-auth.xml` 50 FNPT（25 实体 × query/mutation）+ 8 个自定义 mutation 宿主实体均有 :mutation 兜底（零未登记）——3 个服务型 BizObject 10 个 query 无 FNPT 资源（平台默认放行形态，全域 dashboard/report 同型，注记不立项）；**汇率 MUTEX 钩子在位**：defaultPrepareSave/Update → enforceNoOverlap（维度键 fromCurrency+toCurrency+rateType）→ ErpDateRangeOverlapValidator.enforceMutex → ERR_MD_DATE_RANGE_OVERLAP，第二试点 SupplierApproval 同构；**缺口** = API 刷新 Processor dao 直写绕钩子（md-004 同点）+ ORM SupplierApproval.status defaultValue="10" 孤儿非字典默认值 = **新立 P2-CK-md-015-r3**；⑮7 断言 3 一致 4 漂移（1 归并 md-005 doc 根源 + 3 新立 017/018-r3 + 软漂移注记） | **finding**（3 新立：1 P2 + 2 P3；归并 14，见 §2） |
| **DIM-F 前端页面与 E2E** | §1.2 全套 + 主数据选择器页（picker 范式）/SKU 多单位 UI | validate:flux step[1/3] 0 error/999/855；325 ERR 全 variant 族（md 命中 22 条同族不立项）；AMIS 0 / ORM flux 缺失 0。页面面 25 实体页集全量清点：**picker 全覆盖**（25/25 picker.page.yaml，符合 picker-patterns）；25/25 view.xml 保留层 `x:extends` 继承；手写页 10 处 `@query:` REST 合规（dashboard 双载体/report 两报表/party-search/cost-center）、写路径仅经生成层 `@mutation:__save/update/delete` 与 Material/Partner 状态开关 mutation；view.xml 25/25 `xmlns:i18n-en` 逐列标注（7 个手写非 picker 页面 i18n-en 计数 0 = auth 菜单层已兜底的观察项）。**SKU 多单位无专属换算 UI**（仅 SKU 视图 uoMId/conversionRate 两列）——与 owner doc 无 UI 承诺一致，非违规。**缺陷 = 新立 P3-CK-md-016-r3**：`ErpMdSupplierApproval.view.xml:23-25` gen-control `status == 'ACTIVE' ? 'success' : 'default'` 死样式分支（该列 dict 为 supplier-approval-status 5 值无 ACTIVE → 恒 'default'，APPROVED/SUSPENDED 语义色丢失；同型 pur-017-r3 死状态分支族）。E2E：3 个 md spec PageObject 合规 + E2E_ENGINE 缺省 flux | **finding**（1 新立 P3：016） |
| **DIM-S seed 数据** | §1.3 全套 + 主数据 21 表基座 CSV（全域 FK 上游） | `TestErpSeedDataIntegrity` **4/0/0/0 全绿**（md 基座表 to-one 零悬空 = 全域 FK 上游健康度门禁）；porcelain 空（零 seed 变更）；**372 CSV + 1 SQL** 冻结值；`erp_md_*` **24 CSV** 在册（21 表基座批 + supplier_approval/material_customs/cost_center 等后续批），org/currency/partner/material/subject/uom/exchange_rate 上游族全数在册；md deploy `_seed_*.sql` = 0（登记处一致）；**变更影响面说明**：md 基座表为全域 18 域 FK 上游，任何 seed 变更触发全域双面快照重录义务——本切片零变更无触发；测试资产边界：`app-erp-test-data` 模块为骨架占位（README 明示「当前不含具体 CSV」），实际 md 夹具走 `app-erp-all/_cases/*/input/tables/erp_md_*.csv` 用例自包含——测试资产 vs 部署资产分离成立 | **pass** |
| **DIM-T 单元测试** | §1.4 全套 + 主数据夹具边界 | `mvn test -pl module-master-data/erp-md-service` **160/0/0/0 全绿**（= 锚点 160 零增量，两轮同值）；31 测试类（含 4 状态机变体 + 3 Stub*Checker）+ `_cases` 14 用例根 626 文件；RECORDING = 0；核心链覆盖在位：SupplierApproval 状态机 4 变体（BaselineIoC/DeltaOverride/Matrix/域内）+ DateRangePilots 10 + DateRanges 42 + SkuPriceValidation 10 + Masking 族（Response/ReportReadPath）+ ExchangeRateApiClient 5；**覆盖缺口 = 新立 2 条**：`resolvePriceWithSource`/`validateSkuReference`/`materialPriceListData`/`partnerListData` 4 公开动作零测试引用（**P3-CK-md-019-r3**，同型 mnt-020-r3 族——resolvePriceWithSource 为 md-002 现症主入口）+ FNPT 家族 **P3-CK-md-020-r3**（⑨面 8 mutation 宿主兜底形态注记升级，与 mnt/aps/log 同批） | **finding**（2 新立 P3：019/020） |
| **DIM-I i18n（MI 先行时序口径）** | §1.5 全套 | `--strict` **PASS exit 0** + `--self-test` **PASS**；md 探针族 CAT-3 3 维持清零零回归；WHITELIST md 条目 **1**（`ErpMdDashboardBizModel.java` E3 @Description 豁免）四要素齐备实核（文件在位 + @Description 1 行实证 + owner doc 指针 + plan 2026-09-07-1715-1 Phase 4 裁决）；`@Locale` 缺失 = 空；meta 零手改 | **pass** |

## 2. Finding 三态裁决（§2 跨轮查重全维强制）

> 查重源：r1 `ck-master-data.md`（C1.1）全 14 条逐一比对 + r2 只读目录 + §Mission 基线快照。**本轮新立 6 条（1 P2 + 5 P3）**；历史 14 ID 零覆写（r1 族最大号 CK-md-014，新立自 015 起）。

### 2.1 复用 — 0 条（14/14 open 无 fixed 同型）

### 2.2 归并（同型 open 追加证据至原 ID）— 14 条

| 原 ID | r3 复核证据（T0） |
| --- | --- |
| P2-CK-md-001 | `ErpMdSupplierApprovalBizModel.java:205-212` 仍仅 eq(partnerId) 循环取首条非 REJECTED，无 validFrom/validTo 过滤无排序原样 |
| P2-CK-md-002 | `ErpMdMaterialSkuBizModel.java:190-199` resolvePriceWithSource 仅 customer SPI→默认档，supplier 层缺失（对照 resolvePrice :171-176 三层）原样 |
| P2-CK-md-003 | `:388-390` sku==null 返 ZERO + :477-479 nullSafe→ZERO 原样 |
| P2-CK-md-004 | `ErpMdCurrencyRefreshRatesFromApiProcessor.java:65-66` [today,today+1] 闭区间 + :88-92 rateDao 直写绕 MUTEX 钩子（钩子本体 :36-46/:55-68 在位——绕行面原样） |
| P2-CK-md-005 | `ErpMdUoMConversionBizModel.java:83-91` 仅正向 findRate 无倒数换算原样（doc 根源 = 017-r3 漂移同点） |
| P3-CK-md-006 | `ErpMdExchangeRateBizModel.java:26` javadoc config-gate 声明 vs enforceNoOverlap :55-68 零 AppConfig 读取原样 |
| P3-CK-md-007 | `ErpDateRangeOverlapValidator.java:87-93` catch(Exception) 静默 null 无日志原样 |
| P3-CK-md-008 | `ErpMdSupplierApprovalBizModel.java:271-278` currentUserId 宽 catch 无日志，approvedBy 可 null 原样 |
| P3-CK-md-009 | `ErpPartyBizModel.java:221-222` default 分支裸 IllegalArgumentException 原样 |
| P3-CK-md-010 | `:206` LIKE 模式拼接未转义 `%`/`_` 原样 |
| P3-CK-md-011 | Warehouse/Location/UoM/SettlementMethod 裸 16 行壳；defaultPrepareDelete 仅 Material/MaterialSku 有原样 |
| P3-CK-md-012 | `ErpMdDashboardBizModel.java:40-41` ALERT_MAX_ROWS=5000 + L40「类 D 裁决保留」注释在位——**显式裁决保留态确认不变** |
| P3-CK-md-013 | `ErpMdReportBizModel.java:227-230` 无默认 SKU→null / 有 SKU 无价→nz→0 两义原样 |
| P3-CK-md-014 | `ErpMdMaterialSkuBizModel.java:333-343` skuId 空与未找到同抛 ERR_SKU_DEFAULT_REQUIRED 原样 |

### 2.3 新立 `-r3` — 6 条（1 P2 + 5 P3）

**P2-CK-md-015-r3**（DIM-B ⑧ ORM 孤儿默认值）
- **控制点**：`app-erp-master-data.orm.xml:1191` `ErpMdSupplierApproval.status ext:dict="erp-md/supplier-approval-status" defaultValue="10"`——合法值仅 APPLIED/APPROVED/PROBATION/SUSPENDED/REJECTED（`ErpMdConstants.java:48-52`），"10" 为字典外孤儿默认值。
- **问题**：裸 save 建行（无显式 status）落 status="10" 后：状态机 `assertCanApply` 仅容 null/REJECTED（`ErpMdSupplierApprovalStateMachine.java:56`）→ 该行**永远无法进入 apply()**；且 `findEffectiveByPartner`（md-001 现症无过滤）会把 "10" 当有效资格返回 → 采购询价/订单资格判定污染。与已修复的 RC-R1.40（priceValidationLevel 孤儿 "20"）同族。测试经 `seedApproval` 显式置 status（TestErpMdSupplierApprovalStateMachine:180-188）掩蔽。
- **三态裁决**：新立（r1 无 ORM default 面；r1 仅验证 5 态有 writer）。P2（全域 FK 上游域的数据级缺陷 + 资格判定污染路径）。
- **修复方向**：M2.x——defaultValue 改 "10"→APPLIED（或移除默认由代码赋初值）；保护区 ask-first（ORM 变更双批准）；补「裸 save 后可 apply」失败测试。

**P3-CK-md-016-r3**（DIM-F 死状态样式分支）
- **控制点**：`ErpMdSupplierApproval.view.xml:23-25` gen-control `status == 'ACTIVE' ? 'success' : 'default'`，宿主 dict supplier-approval-status 无 ACTIVE 值 → 条件恒 false。
- **问题**：全部行渲染 default label，APPROVED/SUSPENDED 语义色丢失；同型模板从 active-status 实体复制未改字面量（pur-017-r3 死状态样式分支族 md 站点）。
- **三态裁决**：新立（同族归并 pur-017-r3 家族，md 站点首登记）。P3。
- **修复方向**：M2.x——tpl 改按 APPROVED/SUSPENDED 分支；与 pur-017-r3 家族同批清理。

**P3-CK-md-017-r3**（DIM-B ⑮ owner-doc 结构性漂移）
- **控制点**：`exchange-rate-management.md` §汇率表结构 L9-18（rateDate/isActive、rateType FIXED/MIDDLE/SELLING）+ §FALLBACK 兜底 L20-30（五级查找链）+ §已引用锁定 L38-44（isLocked）vs 实仓 `orm.xml:725-733`（validFrom/validTo + rateType 默认 SPOT，Processor 写 MIDDLE）；isLocked/FALLBACK 解析器/FIXED·SELLING 值全模块 grep 零实现零 writer。
- **问题**：owner doc 结构性章节未随 C3 日期区间改造更新——文档描述的列、兜底链、锁定机制在代码中不存在；方向同 md-006（javadoc 谎称 gate）但控制点是 owner doc 本体。
- **三态裁决**：新立（md-006 是 BizModel javadoc 控制点，本条是 doc 结构章节控制点，分立）。P3。
- **修复方向**：doc 维护批——§汇率表结构/§FALLBACK/§已引用锁定三节按 validFrom/validTo + SPOT/MIDDLE 现状重写（不改需求契约段）；FIXED/SELLING 死值与 md-018 配置键同批裁决（收窄 dict or 补实现）。

**P3-CK-md-018-r3**（DIM-B ⑮/D4 doc 承诺配置键非功能化）
- **控制点**：`sku-multi-unit.md:178` `erp-md.price-tiers`（按类别配置可用档位）全仓零命中；`sku-multi-unit.md:331` `erp-md.sku-auto-create-default`（自动建默认 SKU）仅 `ErpMdConstants.java:22` 声明零读取点；`unified-party-identity.md:81` `erp-md.party-search.max-results` 仅注释提及（ErpPartyBizModel:49），实际硬编码 DEFAULT_LIMIT=50（:50,:152-157）。
- **三态裁决**：新立（同型 aps-009/log-010 死配置家族，md 站点首登记）。P3。
- **修复方向**：M2.x——三键落地或 owner doc 降级/移除声明；与 017-r3 doc 批同收口。

**P3-CK-md-019-r3**（DIM-T 覆盖缺口）
- **控制点**：`resolvePriceWithSource`（md-002 现症主入口）/`validateSkuReference`/`materialPriceListData`/`partnerListData` 4 公开动作 src/test + `_cases` 零引用（grep 实证）。
- **三态裁决**：新立（同型 mnt-020-r3/hr2-030-r3/mfg2-027-r3 族）。P3。
- **修复方向**：M2.x 测试批补四动作用例（resolvePriceWithSource 优先——供应商价层修复的回归载体）。

**P3-CK-md-020-r3**（DIM-B ⑨ FNPT 家族）
- **控制点**：8 个自定义 mutation（SupplierApproval 7 + Currency.refreshRatesFromApi 1）零专属 FNPT，仅宿主实体 :mutation 生成层兜底；准入状态机 7 mutation 无任何页面按钮接线（view.xml 零 @mutation 调用）仅 GraphQL 可达。
- **三态裁决**：新立（同族 mnt-023/aps-015/log-015-r3 本轮新立）。P3。
- **修复方向**：M2.x 按采购主管/管理员角色补 FNPT 注册（对齐 use-cases 准入角色），与家族同批。

### 2.4 归属标注（§3.2 + 跨域横切）

- posting 引擎：md 无过账面（AcctSchemaResolver 为被 fin 引用的共享工具，其内部归 fin-1 消费语义、缺注释面归 common-014-r3 全仓族）。
- common 抽象族：SupplierApproval approveStatus 列激活 F1.3 守卫 = 全仓**激活样本**（对照 aps/notify/log 惰性样本）；其余实体惰性归 pur-003 族共性；save 通道盲区佐证归 common-011-r3。
- 聚合横切面归 U21：聚合器注册在位；3 服务型 BizObject 10 query 无 FNPT 为全域 dashboard/report 同型注记（U21 横切裁决面）。
- notify：md 零消费点（grep 实证），无记录项。
- 下游引用面边界：findEffectiveByPartner/resolvePrice/convertQty/ErpDateRanges 的 18 域消费点语义归各下游格（本格为被引用侧权威登记）。

### 2.5 维度⑮断言抽样记录（3 doc × 7 断言：一致 3 / 漂移 4）

exchange-rate-management.md 3 断言（§配置项 5 键及默认值 ✓ 逐一吻合 ErpMdConfigs:15-29 / §行为约定 ERR_EXCHANGE_RATE_API_UNAVAILABLE + RATE_LIMITED 双码 ✓ / §汇率表结构+§FALLBACK+§已引用锁定 → **漂移 = 017-r3 新立**）；sku-multi-unit.md 3 断言（§数量换算「源数量 × 源系数 ÷ 目标系数」→ **漂移**（实码仅乘单系数 = md-005 doc 根源归并）/ §配置项三键默认 true ✓ / §配置项 price-tiers + sku-auto-create-default → **漂移 = 018-r3 新立**）；unified-party-identity.md 1 断言（三实体 status 共用 erp-md/active-status ✓；party-search.max-results 软漂移并入 018-r3 计数）。漂移 4 = 1 归并 + 2 新立 + 1 软漂移，扩样条款以新立 2 处理（抽样已覆盖 3 owner doc + 交叉点）。

## 3. 统计

| 级别 | 本轮新立 | 复用 | 归并 |
| --- | --- | --- | --- |
| P0 | 0 | 0 | 0 |
| P1 | 0 | 0 | 0 |
| P2 | 1（md-015-r3 孤儿默认值 "10"） | 0 | 5（md-001..005） |
| P3 | 5（md-016/017/018/019/020-r3） | 0 | 9（md-006..014） |
| **合计** | **6** | **0** | **14** |

五格 verdict：DIM-B **finding**（3 新立）/ DIM-F **finding**（1 新立）/ DIM-S **pass** / DIM-T **finding**（2 新立）/ DIM-I **pass**。历史 14 条 r1 ID 零覆写。

## 4. 剩余风险声明（横切关注点 13 完整四件套）

- **已查**：erp-md-{dao,service} 全部 Processor + 25 BizModel（含 3 服务型 BizObject）+ SupplierApproval 状态机 4 变体 + 汇率 MUTEX 钩子链（BizModel→Validator→DateRanges）+ 价格链三 helper 结构走查 + UoM 双层换算 + 机械程式全套实跑（含 notGenCode 增量 = 0）；owner docs 3 doc × 7 断言；r1 14 条全量逐条复核；r2 目录核对；md seed 24 CSV 对账 + app-erp-test-data 边界实核；picker 页 25/25 全量清点。
- **未深查（边界归属）**：18 下游域对 md 符号的消费点语义（归各下游格）；cross-border-trade/data-migration 两 doc 承诺面（海关/迁移批次的 Non-Goal 登记核对仅浅层）；浏览器端渲染回归（归看板专项）；ErpDateRanges 数学面（42 测试覆盖，仅结构审计）。
- **残留风险（登记不裁决）**：① 14 条归并 open 修复归 M2.x，P2 md-004（汇率刷新绕 MUTEX + 双日区间）建议最优先——全域汇率口径被引用面；② md-015-r3 修复涉 ORM 保护区（ask-first 双批准），建议与 RC-R1.40 同族批次；③ partner-type EMPLOYEE/CUSTOMS_BROKER 死值 + FIXED/SELLING 死值 + 018-r3 三配置键 = 「声明未落地」簇，建议统一 dict 收窄 or 实现落地的批量裁决；④ md 作为全域 FK 上游，任何 md seed/ORM 变更的全域重录义务在 M2.x 修复时强制触发。
- **successor 触发条件**：M1.17 收官完整性校验本报告 5/5 格；采购资格链投产前 md-001/015-r3 必须修复；汇率 API 刷新投产前 md-004 必须修复；supplier-approval 页面投产前 016-r3/020-r3 收口。

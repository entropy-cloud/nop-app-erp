# 配置项对照分析:产品级 / 用户级 / 系统级三层配置


> **勘误声明(2026-06-23,以 0005 配置审计报告为准)**:本报告的"三层配置"对照数据(各系统配置机制横向对比)有效。但本报告对 nop-app-erp 的诊断与改进建议存在两处认知错误,已被 `docs/analysis/2026-06-23-0005-configuration-audit.md` 纠正:
> 1. **载体错误**:本报告建议"产品级配置落地 NopSysConfig",但 nop-sys **无 NopSysConfig 实体**,正确载体是 **NopSysVariable**(系统变量)+ **NopSysUserVariable**(用户变量)。
> 2. **归类错误**:本报告把业务配置(成本方法/容差/退货政策等)都当作"应落地全局配置表",但按 configuration-audit-prompt 的四分类,这些应归 **业务表字段**(带组织/产品粒度),与 iDempiere C_AcctSchema 范式一致。项目实际已正确实践(AcctSchema.costingMethod/Bom.consumption 等已是实体字段)。
>
> 阅读本报告时,请以 0005 的四分类原则和最终裁决(PASS)为准;本报告的改进建议 1/2(落地 NopSysConfig/粒度)已被 0005 的"业务配置归业务表字段"取代。

**日期**:2026-06-23
**范围**:对照 erp-survey 中 9 个开源 ERP(Odoo/ERPNext/iDempiere/Metasfresh/管伊佳/赤龙/星云/若依/Dolibarr)的配置机制,诊断 nop-app-erp 配置设计的差距与改进方向
**结论**:nop-app-erp 当前配置**只覆盖系统级 + 产品级两层(且产品级代码零落地),用户级完全空白,粒度仅全局**。对照 iDempiere/Metasfresh 的 Client+Org 双层粒度、ERPNext 的 doctype 化产品配置、星云的按模块审批配置,nop-app-erp 在配置的可实施性、粒度、落地上有明确差距,需做 6 项改进。

## 1. 三层配置定义(本报告采用)

| 层级 | 定义 | 典型载体 | 改动影响 | nop-app-erp 对应 |
|---|---|---|---|---|
| **系统级 SYSTEM** | 技术运维配置,部署/启动时确定 | 配置文件(yml/conf/json) | 改了要重启,影响全局 | `application.yaml`(nop.*/quarkus.*) |
| **产品级 PRODUCT** | ERP 产品行为配置,实施/管理员配置,影响租户/账套业务规则 | 数据库表(NopSysConfig / Settings) | 运行时可改,不重启 | `erp-{domain}.{key}`(文档定义,代码未落地) |
| **用户级 USER** | 单用户偏好/个性化 | 用户偏好表 | 仅影响当前用户 | **完全缺失** |

## 2. erp-survey 各系统配置机制对照

### 2.1 三层配置覆盖度横向对比

| 系统 | 系统级 | 产品级 | 用户级 | 产品级粒度 | 配置存储 |
|---|---|---|---|---|---|
| **iDempiere** | ✅ OSGi/DB provider | ✅✅✅ 最丰富 | ❓ 未调研 | **Client+Org 双层(最细)** | 文件+数据库(C_AcctSchema/AD_SysConfig/AD_Workflow) |
| **Metasfresh** | ✅ yml/pom | ✅✅✅ 共享内核+扩展 | ❓ 未调研 | **Client+Org 双层** | 文件+数据库+Spring Bean(可插拔) |
| **Odoo** | ✅ odoo.conf | ✅✅ res.config.settings | ❓ 未调研 | **Company** | 文件+数据库(ORM 模型) |
| **ERPNext** | ✅ site_config.json | ✅✅ Settings doctype | ❓ 未调研 | **Company** | 文件+数据库(doctype 元数据驱动) |
| **星云** | ✅ yml+starter | ✅✅ *Config 实体+warm-flow | ❓ 未调研 | 全局(按业务模块) | 文件+数据库(*Config 表+flow_*) |
| **管伊佳** | ✅ yml | ✅ 单薄(仅审批开关) | ❓ 未调研 | 全局(无租户) | 文件+数据库(SystemConfig) |
| **赤龙** | ✅ yml+CAS | ✅ 偏财务(凭证模板/科目) | ❓ 未调研 | 全局(多组织弱) | 文件+数据库(fin_voucher_model_*) |
| **若依** | ✅ yml | ⚠️ 薄弱(靠脚手架 sys_config) | ❓ 未调研 | 全局 | 文件+数据库(sys_config/sys_dict) |
| **Dolibarr** | ✅ | ✅ 工作流开关(6 个) | ❓ 未调研 | 全局 | 数据库(admin 常量) |

### 2.2 产品级业务配置的关键差异(ERP 可实施性核心)

| 业务行为 | 配置最规范的系统 | 证据 | nop-app-erp 现状 |
|---|---|---|---|
| **审批流** | 星云(`PurchaseConfig.purchaseRequireBpm`+`purchaseBpmProcessId`,按模块配 BPM 绑定)/ iDempiere(AD_Workflow 完整引擎) | xingyun:56, workflow 报告:220 | 无审批配置(走 nop-wf 但未配置化) |
| **成本计算方法** | iDempiere/Metasfresh(`C_AcctSchema.CostingMethod` A/F/I) | idempiere:60 | `erp-fin.costing-method`(文档有,未落地) |
| **多科目表并行** | iDempiere/Metasfresh(C_AcctSchema 按 Client+Org 多套) | idempiere:89 | `erp-fin.multi-schema-enabled`(文档有,未落地) |
| **存货估值过账时机** | Odoo(`valuation=='real_time'` 才过账) | odoo:95 | 无(异步过账固定) |
| **BOM 齐套严格度** | Odoo(`consumption`:flexible/warning/strict) | odoo:56 | `erp-mfg.kitting-required`(文档有,未落地) |
| **物料批次/序列号强制** | 管伊佳(`Material.enableBatchNumber`) | jsh:71 | 走主数据字段(quality),无配置键 |
| **MTS/MTO 采购策略** | Odoo(`procure_method`) | odoo:108 | 无 |
| **会计期间控制** | ERPNext(Fiscal Year/Accounting Period)/ iDempiere(PostingType 四种) | erpnext:32 | `erp-fin.auto-post-on-close`(文档有,未落地) |
| **预算控制级别** | iDempiere(PostingType 含 Budget) | idempiere:78 | 实体字段 controlLevel(NONE/WARN/HARD,非配置键) |
| **跨实体自动化** | Dolibarr(6 个工作流开关:报价→订单→发票) | dolibarr:60 | 无 |
| **凭证规则模板** | 赤龙(`FinVoucherModelHead`+AMOUNT 占位符)/ iDempiere(FactsValidator) | redragon:62 | VoucherTemplate(设计有,机制化) |
| **负库存开关** | ❓ 各系统文档证据不足 | — | `erp-inv.allow-negative-stock`(文档有,未落地) |
| **退货政策** | ERPNext(Pricing Rule) | erpnext:32 | `erp-sal.return-qty-limit` 等 8 键(文档有,未落地) |

### 2.3 配置存储机制分类

| 存储机制 | 代表系统 | 适用层级 | nop-app-erp 对应 |
|---|---|---|---|
| **配置文件**(.conf/.yml/.json) | Odoo/ERPNext/若依/星云 | 系统级 | ✅ application.yaml |
| **数据库专用配置表** | iDempiere(AD_SysConfig)/管伊佳(SystemConfig)/星云(*Config)/若依(sys_config) | 产品级 | ⚠️ NopSysConfig 存在但零种子数据 |
| **数据库 doctype/ORM 模型** | ERPNext(Settings doctype)/Odoo(res.config.settings) | 产品级 | ❌ 无对应(产品级配置未做实体化) |
| **代码常量/枚举** | 所有系统(状态枚举) | 产品级(固化) | ⚠️ ErpXxxConfigs.java 空壳 |
| **Spring Bean 注册(扩展点)** | Metasfresh(@Component AcctDocProvider) | 产品级(可插拔) | ✅ IErpFinAcctDocProvider(已有) |
| **用户偏好表** | Odoo(res.users)/ERPNext(User) | 用户级 | ❌ 完全缺失 |

## 3. nop-app-erp 配置现状诊断

### 3.1 三层覆盖现状

| 层级 | 现状 | 成熟度 |
|---|---|---|
| **系统级** | application.yaml 约 19 项(nop.*/quarkus.*),全部已落码 | ✅ 成熟,但仅开发态(无生产 profile) |
| **产品级** | 文档定义约 42 个 `erp-{domain}.{key}` 键,但**代码零落地**(ErpXxxConfigs.java 10 个空壳,NopSysConfig 无种子) | ⚠️ 设计有,实现断层 |
| **用户级** | **完全缺失**(无用户偏好表/接口引用) | ❌ 空白 |

### 3.2 产品级配置的具体问题(6 个)

1. **代码零落地断层**:42 个文档配置键 ↔ 10 个空 ErpXxxConfigs.java ↔ 0 个 NopSysConfig 种子。设计→实现未打通,实施时配置无法生效。
2. **粒度单一(仅全局)**:所有 `erp-*` 配置都是全局,多租户/多组织场景下 `allow-negative-stock`/`costing-method`/容差无法按租户/组织差异化。对照 iDempiere/Metasfresh 的 Client+Org 双层,nop 无配置粒度。
3. **键冲突/重复**:`erp-fin.costing-method` 在 costing-methods.md 与 period-close.md 重复定义;`erp-fin.allow-negative-stock` 与 `erp-inv.allow-negative-stock` 语义重叠跨域冲突。
4. **命名规范不统一**:主流是 `erp-{domain}.{kebab-case}`,但存在 `use_multi_level_bom`(蛇形无前缀)、`auto-match-exact-amount` 等 4 个无前缀键。
5. **类型不自洽**:`erp-md.sku-price-validation` 默认值 true(布尔),但 use-cases 用 HARD/WARN/OFF 枚举,文档内部矛盾。
6. **质量/资产/项目/维护 4 域配置空白**:质量域强制质检走主数据字段、抽检比例无键名;其余 3 域 ErpXxxConfigs 全空。

### 3.3 用户级配置的完全缺失

nop-auth 平台有用户偏好机制(NopAuthUser),但**业务侧零引用**:
- 无"默认页签/默认筛选/默认排序/我的看板布局"
- 无"每页条数/列表列显隐"个性化
- 无业务单据的用户级默认值

对照 Odoo res.users / ERPNext User doctype 的偏好配置(语言/时区/默认公司/菜单自定义),nop-app-erp 用户级是空白层。

## 4. 改进建议(6 项,按优先级)

### 改进 1:产品级配置落地到 NopSysConfig(P0,打通设计→实现)

**问题**:42 个文档配置键代码零落地。
**方案**:在各域 ErpXxxConfigs.java 定义配置常量(用 nop 的 `IConfigReference`),并准备 NopSysConfig 种子数据(CSV/YAML 初始化)。这样配置可通过系统管理的"系统参数"菜单(NopSysConfig)运行时修改,不重启。
**参考**:管伊佳 SystemConfig / 若依 sys_config / iDempiere AD_SysConfig 都是数据库键值表,nop 的 NopSysConfig 是对等载体,已就绪待用。

### 改进 2:产品级配置粒度支持租户/组织级(P1,多组织场景必需)

**问题**:配置仅全局,多组织无法差异化。
**方案**:对影响业务规则的配置(负库存/成本方法/容差/退货政策),支持按 orgId 覆盖全局默认。机制:NopSysConfig 增加 orgId 维度(或用 nop 的 Profile 机制),查询时按"组织级 > 全局"优先级解析。
**参考**:iDempiere/Metasfresh 的 Client+Org 双层是 25 年沉淀的核心价值;Odoo/ERPNext 的 Company 级。

### 改进 3:审批流按业务模块配置化(P1,借鉴星云)

**问题**:走 nop-wf 但未配置化,无法实施期绑定。
**方案**:借鉴星云 `PurchaseConfig.purchaseRequireBpm`+`purchaseBpmProcessId`,每业务域建 Config 实体(`ErpPurConfig`/`ErpSalConfig`...),字段含"是否启用审批流"+"绑定的流程定义ID"。实施期配置,不改代码。
**参考**:星云 PurchaseConfig(xingyun:56)是国产 ERP 中审批配置最规范的。

### 改进 4:补齐用户级配置层(P2,体验提升)

**问题**:用户级完全缺失。
**方案**:利用 nop-auth 的 NopAuthUser 偏好机制,为业务侧增加用户级配置:默认页签、列表列显隐、每页条数、个人看板布局、默认筛选。可作为 NopAuthUser 的扩展字段或独立偏好表。
**参考**:Odoo res.users / ERPNext User doctype。

### 改进 5:配置规范化治理(P2,消除冲突)

**问题**:键冲突/命名不统一/类型不自洽。
**方案**:
- 消除重复:`erp-fin.costing-method` 统一归属 finance(去掉 period-close 重复);`allow-negative-stock` 统一归属 inventory(finance 引用 inv 的)。
- 统一命名:全部 `erp-{domain}.{kebab-case}`,修正 `use_multi_level_bom`→`erp-mfg.multi-level-bom`、无前缀的自动核销键加 `erp-fin.` 前缀。
- 修正类型:`erp-md.sku-price-validation` 统一为枚举(HARD/WARN/OFF),默认值改 OFF 或 WARN。

### 改进 6:生产级系统配置 profile(P2,运维)

**问题**:application.yaml 仅开发态(H2/明文密钥/debug=true)。
**方案**:增加 `application-prod.yaml` profile,外部化数据库连接/密钥/端口;生产关闭 debug、启用外部 MySQL、密钥走环境变量或密钥管理。

## 5. 改进优先级与实施顺序

| 优先级 | 改进 | 价值 | 工作量 | 建议时机 |
|---|---|---|---|---|
| **P0** | 1. 产品级配置落地 NopSysConfig | 打通设计→实现,配置可生效 | 中(42 键落码+种子) | 首批业务实现时 |
| **P1** | 2. 配置粒度租户/组织级 | 多组织可实施性 | 中(NopSysConfig 加维度) | 多组织需求出现时 |
| **P1** | 3. 审批流按模块配置化 | 实施期可配审批 | 中(10 个 Config 实体) | 审批需求出现时 |
| **P2** | 4. 用户级配置层 | 用户体验 | 中(偏好表+前端) | UX 打磨阶段 |
| **P2** | 5. 配置规范化治理 | 消除冲突,可维护 | 小(文档+键名修正) | 可立即做 |
| **P2** | 6. 生产 profile | 运维就绪 | 小(yml) | 部署前 |

## 6. 结论

nop-app-erp 的配置设计**蓝图清晰(42 个产品级配置键有明确语义),但实现断层(代码零落地)+ 层级缺失(用户级空白)+ 粒度单一(仅全局)**。对照 erp-survey:

- **优于**:若依/管伊佳/赤龙(它们的产品级业务配置更薄弱,nop 的 42 键覆盖更全)
- **持平**:文档层面的配置丰富度接近 iDempiere/ERPNext
- **弱于**:① 落地(它们配置都在数据库可运行时改,nop 是文档);② 粒度(iDempiere/Metasfresh 有 Client+Org 双层,nop 仅全局);③ 用户级(它们都有用户偏好,nop 空白)

改进的核心是**改进 1(落地 NopSysConfig)+ 改进 2(粒度)+ 改进 3(审批配置化)**——这三项让 nop-app-erp 从"配置设计完善但未实现"变为"配置可实施",达到 iDempiere/星云级的可实施性。

## 证据局限

1. erp-survey 文档聚焦模块/实体/业财机制,**用户级配置证据普遍缺失**(res.users/User doctype 偏好未深入调研),本报告用户级部分基于通识推断。
2. 系统级配置文件(odoo.conf/site_config.json/application.yml)的具体配置项未逐项实测,仅有"存在该文件"的推断。
3. 部分业务开关(负库存/信用控制/退货有效期)在各系统文档证据不足,对照表标注"❓"。

## 参考证据

- `docs/analysis/erp-survey/2026-06-22-0000-workflow-vs-state-machine.md`(审批/状态机配置横评)
- `docs/analysis/erp-survey/2026-06-22-0000-xingyun-erp.md`(PurchaseConfig 按模块配审批,:56)
- `docs/analysis/erp-survey/2026-06-22-0000-idempiere.md`(C_AcctSchema/AD_Workflow/FactsValidator)
- `docs/analysis/erp-survey/2026-06-22-0000-jsh-erp.md`(multiLevelApprovalFlag,:122)
- `docs/analysis/erp-survey/2026-06-22-0000-redragon-erp.md`(凭证模板 fin_voucher_model_*)
- `docs/analysis/erp-survey/2026-06-22-0000-dolibarr.md`(工作流开关)
- `docs/design/{sales,purchase,inventory,master-data,manufacturing,finance,quality}/`(nop-app-erp 配置项定义)
- `app-erp-all/src/main/resources/application.yaml`(系统级配置)

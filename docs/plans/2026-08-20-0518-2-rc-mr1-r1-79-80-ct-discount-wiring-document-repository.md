# 2026-08-20-0518-2-rc-mr1-r1-79-80-ct-discount-wiring-document-repository contract 量折扣消费接线 + 文档仓库引擎

> Plan Status: completed
> Mission: requirement-compliance
> Work Item: RC-R1.79（P1-RC-078，UC-CT-08 A）+ RC-R1.80（P1-RC-079，UC-CT-10 A/B/C/D）
> Last Reviewed: 2026-08-20
> Source: `docs/backlog/requirement-compliance-roadmap.md` MR1 RC-R1.79 / RC-R1.80（todo）+ `docs/audits/arm-index.md` P1-RC-078 / P1-RC-079
> Related: MA1 报告 `docs/audits/2026-08-05-1400-1-rc-ma1-a1-45-46-contract-lifecycle-billing-rebate.md`；A4.2 运行时确认 `docs/audits/2026-08-08-0135-rc-ma4-a4-2-155-162-contract-runtime.md`（A4.2.161/162）；RC-R1.32-35（contract 域前序修复，已 done）
> Audit: required

## Current Baseline

（2026-08-20 实仓核验）

**RC-R1.79（量折扣订单侧消费）**：

- contract 侧 API 就绪且正确：`ErpCtVolumeDiscountBizModel.resolveDiscount:44`（@BizQuery，band 匹配 fromQty<=qty<toQty + discountPercent/覆盖价优先 + 无命中回退原价）+ `validateNoOverlap:101`（区间带重叠校验）。
- **消费方零接线**：grep `resolveDiscount|IErpCtVolumeDiscountBiz` 跨 module-purchase + module-sales 零命中（A4.2.161 运行时复核一致）。
- **订单行无合同行引用载体**：`_ErpPurOrderLine` 属性集（orderId/lineNo/materialId/skuId/uoMId/quantity/unitPrice/taxRate/…/projectId/remark）**无任何 contract 字段**；purchase ORM 全文零 contract 引用；`_ErpSalOrder.contractId`（propId 5）指向 **ErpSalContract（sales 域自有轻量合同实体，属性集 customerId/validFrom/…，无 ct 关联字段）** 非 ErpCt 合同——「订单引用合同行」当前无结构载体。
- 依赖矩阵：`docs/architecture/data-dependency-matrix.md` contract 业务层关联 purchase/sales「待深化」；pur/sal-service pom 零 erp-ct 依赖。
- 授权：B 类预授权（2026-08-12「RC-R1.79（订单行调 IErpCtVolumeDiscountBiz.resolveDiscount）」）+ 跨域契约协调义务（决策项 + matrix Java 层边登记）；若采用加列载体则属 Q3 纯加性批量授权（可空无默认无索引无 UK）。

**RC-R1.80（文档仓库引擎）**：

- `ErpCtDocument` 实体字段完整（ocrText/ocrStatus/fullTextSearch/metadataTags/retentionDate/archiveDate/purgeDate/isArchived 均存在，propId 核验）**唯缺 legalHold**（grep `legalHold|legal_hold` 跨 erp-ct-dao 零命中——结构+逻辑双缺）。
- `ErpCtDocumentBizModel` 17 行 CRUD 桩；grep `ocr|fullText|retention|purge` 跨 erp-ct-service/src/main 零业务命中；全仓零 OCR 服务（ocrService/tesseract/paddleocr 零命中，A4.2.162）。
- **owner doc 声明的 dict 未物化**：owner doc `contract-repository.md:71/:81` 声明 dict `erp-ct/doc-type` 与 `erp-ct/ocr-status`，但 erp-ct-meta dict 目录现存 12 个 dict（contract-status/type、invoice-term、rebate-*、sign-*、settlement-status、accrual-method、approval-status、contract-direction）**均无此二者**；`ocrStatus` 列（orm.xml:693）无 dict tag——OCR 状态机与文档类型字典须随本计划物化（R1.81 物化 `erp-inv/drp-lt-flag` dict 先例）。
- owner doc `docs/design/contract/contract-repository.md` 活跃契约：§OCR 流程（PENDING→PROCESSING→COMPLETED/FAILED + 重试 + 人工补录）、§全文检索（fullTextSearch = docName+ocrText+code+metadataTags 拼接）、§高级搜索（keyword/编码/类型/合同/日期/文件大小/元数据标签键值对/OCR 状态/归档过滤）、§保留策略（retentionDate→归档只读、purgeDate→系统删除、config `erp-ct.doc-retention-years` 默认 10 / `doc-archive-years` 默认 20 / `doc-auto-archive` 默认 true / `doc-auto-purge` 默认 false 需人工确认）、§合规规则（ACTIVE 合同文档不归档、合同终止后起算保留、**审计锁定期禁止销毁**、Legal Hold admin 设置阻止归档/销毁）。
- 授权：A 类 ORM 批量授权（2026-08-12「contract: RC-R1.80（ErpCtDocument 加 legalHold 列）」纯加性）；**purgeDate→delete 触及数据删除保护区域——须双独立子 agent 批准**（2026-08-15 裁决升级后规程）+ 独立 plan-audit；OCR/全文/归档/守卫属代码逻辑预授权。

## Goals

- RC-R1.79：purchase/sales 订单行获得合同行引用载体并接线 `resolveDiscount`——「订单引用合同时按实际数量匹配折扣率，计算折后价」（L1 `use-cases.md:153` 逐字）落地。
- RC-R1.80：`legalHold` ORM 列 + OCR 状态机与引擎接线（SPI 载体）+ fullTextSearch 构建与全文/高级搜索 + 保留策略归档/销毁 + Legal Hold 守卫全链落地。

## Non-Goals

- 真实 OCR 识别引擎选型与部署（Tesseract/云 OCR 原生集成）——SPI + 默认实现 + 状态机为产品基线，真实引擎为部署 successor（对齐 UC-CT-09 MockSignatureProvider 先例）。
- Elasticsearch/数据库方言级 FULLTEXT 索引——跨方言差异大，搜索经 LIKE/过滤实现满足 L1「全文搜索或高级过滤器」；索引化 successor。
- OFD 解析（owner doc 建议方案列提及，L1 UC-CT-10 断言不含）。
- sales 域自有 ErpSalContract 与 ErpCt 合同的双轨归一（另一架构议题，不属本修复面）。
- 保留起算「合同 endDate 起算」的自动重算（本计划落 manual retentionDate 录入 + 到达归档；endDate 联动起算 successor，owner doc 注记）。

## Task Route

- Type: `implementation-only change`（含 Q3/A 类授权内 ORM 纯加性变更 + 数据删除保护区域双批准门控）
- Owner Docs: `docs/design/contract/volume-discount.md`（§折扣应用）+ `docs/design/contract/contract-repository.md` + `docs/design/contract/use-cases.md`（UC-CT-08/10 L1）+ `docs/design/purchase/README.md` / `docs/design/sales/README.md`（订单行引用合同行衔接注记）
- Skill Selection Basis: 主体为 BizModel/per-mutation Processor/xbiz 查询与跨域消费接线 → `nop-backend-dev`；测试 → `nop-testing`；ErpCtDocument.view.xml 搜索表单接线 → `nop-frontend-dev`（Phase 3 页面项加载）。

## Infrastructure And Config Prereqs

- 无新外部服务。OCR 引擎 SPI 默认实现为零依赖（手动补录/无操作识别器），真实引擎部署 successor。
- config 键（对齐 owner doc §保留策略配置）：`erp-ct.doc-retention-years`（默认 10）/ `erp-ct.doc-archive-years`（默认 20）/ `erp-ct.doc-auto-archive`（默认 true）/ `erp-ct.doc-auto-purge`（默认 false，**须人工确认语义保持**）；R1.79 侧 config 门控键经 D3 裁决。
- 归档/销毁扫描载体：nop-job `.job.yaml` + batch/simple job bean（R1.35/R1.37 范式，enabled 默认 false + cron config 键）。
- 数据删除回滚策略：purge 采用逻辑删除（delVersion 软删 + 审计行/remark 记录），物理删除为显式 successor——见 Phase 4 D4 与双批准门控。

## Execution Plan

### Phase 1 - RC-R1.79 折扣消费接线

Status: completed
Targets: `module-purchase/`、`module-sales/`（order line 载体 + 消费接线）+ `module-contract/`（IBiz 契约/i18n 如有增量）+ matrix 登记
Skill: nop-backend-dev

- Item Types: `Decision | Add | Proof`
- Prereqs: 无

- [x] Decision: D1 合同行引用载体——**裁决选项 a**：`ErpPurOrderLine.ctContractLineId`（propId 25）+ `ErpSalOrderLine.ctContractLineId`（propId 103），均为可空 BIGINT 无默认无索引无 UK（DDL 三方言核验：9 SQL 文件 61 行纯插入零删除，`CT_CONTRACT_LINE_ID BIGINT NULL` 三方言一致）；跨模块 to-one 载体 = 机制 B notGenCode 外部实体 `ErpCtContractLine`（biz:moduleId="erp/ct"，pur/sal orm.xml 各 1 声明，实体类由 ct-dao 生成，pur/sal-dao 增 ct-dao compile 依赖支撑生成 getter 编译）。替代方案否决：b) 头级引用弱于 L1「按行引用」粒度；c) remark 弱指针不可查询。残留风险：合同行删除/换版后悬空引用无 FK 约束（对齐既有 projects 引用同型风险，运营侧约定）。授权：Q3 纯加性常设批量授权（可空无默认无索引无 UK 逐项满足）+ B 类预授权（resolveDiscount 消费调用），双独立子 agent 批准条款未触发（未越 Q3 边界）
      - Skill: nop-backend-dev
- [x] Decision: D2 应用点与重算时机——**裁决选项 a**：defaultPrepareSave/Update fill-when-absent（行级 BizModel + 订单头 BizModel 嵌套子表路径双钩子）+ approve 时点重算守卫（`recalcCtDiscountForApprove` protected step，数量变更后以 approve 时点为准；sales 侧重算先于信用额度/可用量校验保证口径一致，purchase 侧接 `ErpPurOrderApproveProcessor.validateBusinessRules`）。**折扣基数 = 合同行单价 `ctContractLine.unitPrice`（ORM to-one getter，缺失回退订单行现价）**——稳定基数使 save/approve 重复解析无二次折扣；无命中回退原价（行零改写）。sales 优先级语义：显式合同行引用优先于促销/目录价——`persistPricingResult` 跳过 ctContractLineId 非空行的促销改写（促销不叠加）。折后价表达：purchase 行 unitPrice ← 折后价 + remark `[CT_VOLUME_DISCOUNT]节省X` 幂等标记（行无 pricingSource 列，P2-RC-023 范式 remark 载体）；sales 行 unitPrice ← 折后价 + discountRate（隐含率）/discountAmount（基数口径节省额，可见性载体不参与净额减扣）/pricingSource=`CT_VOLUME_DISCOUNT`
      - Skill: nop-backend-dev
- [x] Decision: D3 config 门控与默认值——**裁决默认 true**：`erp-pur.ct-discount-enabled` / `erp-sal.ct-discount-enabled`（`ErpPurConstants.CONFIG_CT_DISCOUNT_ENABLED` / `ErpSalConstants.CONFIG_CT_DISCOUNT_ENABLED`）。理由：对齐 ct 侧 owner doc §配置点 `erp-ct.volume-discount-enabled` 默认 true（同一能力两半的开关语义应一致）+ R1.57 auto-assign 先例；关闭时 ctContractLineId 仅存储不应用（测试断言零改写）。ct 侧聚合键 `erp-ct.volume-discount-enabled` 不叠加消费（避免双门控混淆，登记于 volume-discount.md 注记）
      - Skill: none
- [x] Add: pur/sal-service pom 增 erp-ct-dao compile（+ erp-ct-service test 挂载，镜像 R1.61 projects 先例）+ matrix §2.4 Java 层边登记（pur→ct / sal→ct 单向）——**已落地**：pur/sal-service pom ct-dao compile + ct-service test；pur/sal-dao pom ct-dao compile（notGenCode getter 编译需要）；matrix §2.4 增 pur-service→ct-dao / sal-service→ct-dao 两行（Maven 菱形非环披露，镜像 assets↔mnt 先例）+ §5.6.2 purchase/sales 行 pur 59/13、sal 56/12（`cross-module-dep-extract.py` 机器核验 640 to-one/113 external 全量同步）+ §6 contract 行 P 反查入口登记
      - Skill: none
- [x] Add: 订单行消费接线——`ErpPurCtDiscountApplier` / `ErpSalCtDiscountApplier`（@Nullable IErpCtVolumeDiscountBiz 注入容错，ct 模块缺失跳过）：qty=行数量、unitPrice=D2 裁决合同行价基数；命中 → 折后价/行金额/税额随动（purchase 税额外价 amount×rate/100 scale2 对齐 RequisitionToOrderConverter；sales 价税分离 net×rate/(1+rate) 对齐 recomputeLineAmount）+ approve 重算含头合计 Σ 重算（`recomputeOrderTotals`）；无命中回退原价；折扣来源标记（remark/pricingSource per D2）；beans.xml 注册两 applier
      - Skill: nop-backend-dev
- [x] Proof: `TestErpPurOrderCtDiscount`（6 组：命中区间 95/28500/头合计/remark 标记、覆盖价 88/52800、无命中回退 90/4500、数量跨档 90→300 approve 重算、config 关闭零应用、行级 GraphQL 保存即应用）+ `TestErpSalOrderCtDiscount`（6 组镜像：命中 95/28500/discountRate 5%/discountAmount 1500/pricingSource/头合计、覆盖价 88/52800/7200、无命中回退、跨档重算、config 关闭、行级 GraphQL）——**全绿**；erp-pur-service 334 tests / erp-sal-service 309 tests 零回归
      - Skill: nop-testing

Exit Criteria:

- [x] 引用合同行的订单行按数量匹配折扣率计算折后价（成功/回退两模式断言）；config 关闭零行为变化（既有 pur/sal 测试零回归——pur 334 / sal 309 全绿）
- [x] pur/sal→ct 依赖边落 matrix 登记；D1-D3 裁决落盘

### Phase 2 - RC-R1.80 legalHold ORM + Legal Hold 守卫

Status: completed
Targets: `module-contract/erp-ct-dao/`（orm.xml + 增量重生成）+ 守卫接线
Skill: nop-backend-dev

- Item Types: `Add | Decision`
- Prereqs: 无（与 Phase 1 可并行）

- [x] Add: `ErpCtDocument.legalHold` 纯加性列（可空无默认无索引无 UK；A 类批量授权 2026-08-12「contract: RC-R1.80」；`mvn clean install -DskipTests` 增量重生成 + DDL 三方言核验）——已落地核验：orm.xml:725 propId 27 + 三方言 DDL（mysql `LEGAL_HOLD BOOLEAN NULL` / oracle `LEGAL_HOLD CHAR(1)` + COMMENT / postgresql `legal_hold BOOLEAN`）+ `_ErpCtDocument.java`/xmeta/api beans/i18n 生成物全同步
      - Skill: none
- [x] Add: Legal Hold 守卫——`legalHold=true` 阻止所有归档/销毁操作（域错误码如 `ERR_CT_DOCUMENT_LEGAL_HOLD`）；归档文档只读守卫（归档后禁改，owner doc「归档后不可修改」）；ACTIVE 合同文档不归档守卫——`ErpCtDocumentBizModel`：archive 三守卫（legalHold/ACTIVE/已归档幂等）+ defaultPrepareUpdate 归档只读（ORM 脏值取旧值，对齐 InvoicePlan 守卫范式；legalHold 合规字段 admin 例外放行）+ defaultPrepareDelete（legalHold/已归档双阻断）+ generic 管道携带 legalHold 防绕过角色守卫；错误码 ERR_CT_DOCUMENT_LEGAL_HOLD / ERR_CT_DOCUMENT_ARCHIVED_IMMUTABLE / ERR_CT_DOCUMENT_CONTRACT_ACTIVE / ERR_CT_DOCUMENT_ROLE_REQUIRED / ERR_CT_DOCUMENT_NOT_FOUND 落 ErpCtErrors
  - Skill: nop-backend-dev
- [x] Decision: legalHold 设置权限面——owner doc「admin 手动设置」：裁决载体 = **@BizMutation + Java 角色守卫**（`IUserContext.isUserInRole(roleId)` fail-closed，镜像 hr ERR_MAKEUP_ROLE_REQUIRED 范式；roleId=`admin` 对齐 nop_auth_role.csv 种子）。否决仅 XMeta auth：action-auth 在测试 enableActionAuth=FALSE 下不可断言，不能作唯一守卫。generic save/update 携带 legalHold 字段同守卫面（防绕过专用入口）
  - Skill: none

Exit Criteria:

- [x] legalHold=true 时归档/销毁被拒（错误码断言 + 零状态变更）；守卫测试通过——`TestErpCtDocumentGuards` 4 组全绿（legalHold 阻归档+删除 / 角色守卫双侧+防绕过 / 归档只读+admin 合规例外 / ACTIVE 阻断+EXPIRED 放行+幂等）；erp-ct-service 150 tests 零回归

### Phase 3 - RC-R1.80 OCR 状态机 + SPI + fullTextSearch + 全文搜索

Status: completed
Targets: `module-contract/erp-ct-service/`（OCR SPI + Processor + 搜索 @BizQuery）+ `module-contract/erp-ct-meta/`（dict 物化）+ `module-contract/erp-ct-web/`（ErpCtDocument.view.xml 搜索表单）
Skill: nop-backend-dev

- Item Types: `Decision | Add | Proof`
- Prereqs: Phase 2（legalHold 列与守卫先落位——搜索过滤含归档态、状态机测试依赖 legalHold 守卫面）

- [x] Decision: D1 OCR 引擎载体——**裁决**：`IErpCtOcrEngine` SPI（`spi/model/OcrRecognizeRequest|Response` DTO）+ `ErpCtOcrEngineRegistry`（List 注入 + 内部建图，镜像 MockSignatureProvider/SignatureProviderRegistry 先例；config `erp-ct.ocr-engine` 选型默认 manual）+ `ManualOcrEngine`（engineCode="manual"，零依赖无操作识别器——识别恒 FAILED 引导人工补录 ocrText）；状态机 PENDING→PROCESSING→COMPLETED/FAILED（PROCESSING 拒绝并发提交，FAILED/COMPLETED 可重跑）+ 失败原因记 remark + `startOcr` 人工重新提交 mutation + `submitOcrText` 补录 mutation（**补录等同 COMPLETED 语义裁决**——ocrStatus 置 COMPLETED + fullTextSearch 重建）；接口方法参数 @Optional（GraphQL 可空过滤参数，@BizQuery 空参可路由）
      - Skill: nop-backend-dev
- [x] Add: dict 物化——`erp-ct/ocr-status`（PENDING/PROCESSING/COMPLETED/FAILED，对齐 owner doc :81）与 `erp-ct/doc-type`（规范值集 = owner doc :73-79 完整 5 值表：CONTRACT_SCAN 10 / AMENDMENT 20 / ATTACHMENT 30 / CERTIFICATE 40 / OTHER 90）两 dict yaml 落 erp-ct-meta（R1.81 物化 `erp-inv/drp-lt-flag` 先例）+ ocrStatus/docType 列 dict tag 接线——已落地核验：orm.xml:100/:108 dict 定义 + 列 ext:dict 接线（orm.xml:702/:708）+ 生成 dict yaml 两文件 + `_ErpCtDaoConstants` OCR_STATUS_*/DOC_TYPE_* 常量全同步
  - Skill: none
- [x] Add: fullTextSearch 构建接线——上传/OCR 完成/补录/metadataTags 变更时重建 `fullTextSearch = docName + ocrText + code + metadataTags 关键值` 拼接（owner doc §索引策略公式）——`rebuildFullTextSearch`（空白分段跳过 + metadataTags 解析 JSON 取键值对 + 非 JSON 原文 + 上限 4000 对齐列宽）；defaultPrepareSave 初始构建 + defaultPrepareUpdate 源字段触及重建 + OCR 完成/补录路径重建；上传缺省填充同落（ocrStatus=PENDING + retentionDate/purgeDate 按 config 年限 fill-when-absent）
  - Skill: nop-backend-dev
- [x] Add: 全文 + 高级搜索 @BizQuery——实现过滤集：keyword→fullTextSearch LIKE + code 精确 + docType + contractId + 上传日期范围 + OCR 状态 + 归档（共 7 类，L1「全文搜索**或**高级过滤器」析取满足；owner doc 9 行过滤器表余下文件大小范围/元数据标签键值对按 Deferred 登记）——`searchDocuments` @BizQuery 返回 `DocumentSearchResult` wrapper；contains/ge/le 过滤运算符经保留层 xmeta `allowFilterOp` 覆盖开放（fullTextSearch + createTime，生成层默认仅 eq/in/dateBetween 不满足 keyword/日期范围语义）
  - Skill: nop-backend-dev
- [x] Add: `ErpCtDocument.view.xml` 搜索表单最小接线（keyword 输入 + docType/OCR 状态/归档过滤 + 结果列表列扩展 fullTextSearch 摘要/ocrStatus/归档态）——query form 增 fullTextSearch[keyword] filterOp=contains + ocrStatus 单元格；grid bounded-merge 收敛为核心列 + ocrStatus/fullTextSearch/isArchived/retentionDate/archiveDate/purgeDate
      - Skill: nop-frontend-dev
- [x] Proof: `TestErpCtDocumentRepository`——OCR 状态机全迁移/失败重试/手动补录/fullTextSearch 拼接断言/keyword+过滤组合搜索/归档文档可搜索不可修改——9 组全绿：上传缺省填充（PENDING+retentionDate 2036-07-17+purgeDate 2056-07-17+fullText 含 metadataTags 键值）/手工保留期覆盖/manual FAILED+remark/FAILED→test-fixed 引擎重试 COMPLETED+ocrText+fullText 重建/PROCESSING 拒绝并发/补录 COMPLETED 语义/7 类过滤组合+日期范围/归档可搜索不可修改/RPC 冒烟（FixedTextOcrEngine 经 test-mock.beans.xml 注册 nop test-mock 范式）
      - Skill: nop-testing

Exit Criteria:

- [x] 上传→OCR（默认引擎）→fullTextSearch 构建→keyword 搜索全链断言；FAILED→人工补录→COMPLETED 语义断言
- [x] 两 dict 物化且 ocrStatus/docType 列 dict tag 生效；erp-ct-service 既有测试零回归（159 tests 全绿 = 150 既有 + 4 Phase 2 守卫 + 9 本阶段——其中既有 150 含新增 4 守卫后基线）

### Phase 4 - RC-R1.80 保留策略归档/销毁 + 数据删除双批准

Status: completed
Targets: `module-contract/erp-ct-service/`（归档/销毁编排 + job）+ config + owner doc
Skill: nop-backend-dev

- Item Types: `Decision | Add | Proof`
- Prereqs: Phase 2（legalHold 守卫先行）

- [x] Decision: D4 销毁语义（**数据删除保护区域——须双独立子 agent 批准**，批准记录落盘本计划）——**裁决选项 a 逻辑删除**：`purge` = delVersion 软删（useLogicalDelete 既有机制，行从全部常规查询消失）+ 销毁前审计（行内 remark 销毁事件[操作人/日期] **耐久载体**——不依赖可静默跳过的通知 + `ct.document-purged` 通知 best-effort R1.4 范式）；物理 DELETE 为显式 successor（触发 = 合规要求真实擦除的部署）。**双独立子 agent 批准（fresh session，互不共享执行者上下文）：APPROVED ses_fe312b7c5ffe8EW1Va8o42aJZ2（2026-08-20）+ APPROVED ses_fe312882dffeVh8nosoYox3lGN（2026-08-20）**，两批准附加条件全数落位：① purge 五守卫（admin 角色 fail-closed[人工销毁入口] / legalHold / 已归档[生命周期顺序] / **ACTIVE 合同[覆盖 SUSPENDED→ACTIVE 与 rejectAmend 重激活路径]** / **purgeDate 到达[保留义务禁提前销毁——提前销毁无通道，successor]**）；② 耐久审计不依赖通知（remark 行内 + 软删行自身）；③ 销毁专用通道独立于 generic delete（defaultPrepareDelete 双阻断保持，purge 为唯一合法销毁入口）；④ 不暴露通用 delVersion 恢复入口；⑤ 测试含 recoverability[disableLogicalDelete 复核 delVersion>0]/无模板静默跳过/mutation+job 双路径守卫断言；⑥ owner doc 回填 D4 裁决 + successor 触发条件。`doc-auto-purge` 默认 false 人工确认语义保持（manual purge mutation = 人工确认通道，同样要求 purgeDate 到达）
      - Skill: none
- [x] Add: 保留策略编排——retentionDate 到达→自动归档（isArchived=true + archiveDate，config `doc-auto-archive` 门控）+ 归档只读 + ACTIVE 合同不归档守卫接线；retentionDate/purgeDate 缺省填充（上传时按 config 年限推算，可手工覆盖）；「合同终止后起算」自动重算 successor 登记——`archiveOverdueDocuments`/`purgeOverdueDocuments` @BizMutation 批量入口（dateBetween 表达 ≤today 对齐 expireOverdueContracts 注记 + 单条失败隔离 + 复用 archive/purge mutation 守卫）；缺省填充在 Phase 3 defaultPrepareSave 已落（fill-when-absent）；endDate 联动起算 successor 已登记 contract-repository.md 实现注记
  - Skill: nop-backend-dev
- [x] Add: nop-job 归档/销毁扫描（`erp-ct-doc-retention.job.yaml` + simple job bean，R1.37 范式：enabled 默认 false + cron 键 + 逐条失败隔离 + legalHold/ACTIVE 守卫短路 + 销毁前审计记录）——`ErpCtDocRetentionJob`（cron 单键 `erp-ct.doc-retention-cron` 空值不调度 + doc-auto-archive 默认 true / doc-auto-purge 默认 false 双行为门控 + runInSession 包裹 IBiz 调用对齐 expiry job 范式）+ beans.xml 注册 + TestErpAllJobYamlLoading 32→33 计数更新通过
  - Skill: none
- [x] Add: owner doc 回填——contract-repository.md 实现注记（SPI 载体/搜索实现/软删裁决 D4/自动起算 successor）；volume-discount.md §折扣应用实现注记（D1-D3 + 消费面）；purchase/sales README 衔接注记；arm-index P1-RC-078 / P1-RC-079 → done
  - Skill: none
- [x] Proof: `TestErpCtDocRetention`——到期归档/legalHold 阻断归档+销毁/ACTIVE 阻断/purge 逻辑删除或物理删除断言（按 D4）/审计记录/config 关闭零动作/job 幂等——9 组全绿：到期归档+未到期零动作 / auto-archive off 零动作 / legalHold 双阻断[归档+销毁行仍可见] / ACTIVE 阻断+EXPIRED 对照 / D4 逻辑删除[常规查询消失 + disableLogicalDelete 复核 delVersion>0 软删行仍在 + remark 销毁事件含操作人 + 审计通知 1 条] / 无模板静默跳过不阻断 / purge 守卫三拒[未归档/purgeDate 未到/非 admin 角色] + 拒绝路径零状态变更 / auto-purge off 不销毁 / job 幂等 + cron 空值跳过
      - Skill: nop-testing

Exit Criteria:

- [x] 归档/销毁全链断言（含守卫三阻断路径）；D4 双独立子 agent 批准记录落盘（双 APPROVE session id 见 Decision 项）
- [x] config 四键 + job 键登记 `ErpCtConfigs`（CFG_DOC_RETENTION_YEARS/CFG_DOC_ARCHIVE_YEARS/CFG_DOC_AUTO_ARCHIVE/CFG_DOC_AUTO_PURGE/CFG_DOC_RETENTION_CRON + CFG_OCR_ENGINE）；TestErpAllJobYamlLoading 计数更新通过（32→33）

## Draft Review Record

- Independent draft review iteration 1: needs revision (ses_fe418b050ffeDpK1gmtVNRnQXs) because 两项 BLOCKER——基线伪事实「dict erp-ct/ocr-status 已有」（实仓 12 dict 无 ocr-status/doc-type，owner doc :71/:81 声明未物化）+ 反松弛违例「（若需）view.xml 接线」条件式范围项；另 4 项 MINOR（B 类「无 ORM」理由超越未登记、审计锁定期规则漏归属、过滤集子集选择未显式裁决、Phase 3 Prereqs 并行/串行矛盾）
- Independent draft review iteration 2: acceptable as-is (ses_fe40fb2d5ffePJg0wNbA3MSl0L) after iteration-1 六项问题（2 BLOCKER + 4 MINOR）全部修复实仓验证；2 项 MINOR（code 精确过滤器归属账目、doc-type 值集应引 :73-79 规范 5 值表 + 大小写笔误）已随手修正。共识达成，Plan Status → active。

## Closure Gates

> 完整仓库验证一次：`mvn clean install -DskipTests`（含 ORM 增量重生成）+ `mvn test`（分域聚焦 pur/sal/ct + 全仓）+ compliance checker（actual ≤ baseline；Q3/A 类加列与新增站点若触发 baseline-raise 须 per-site 证据落 `docs/audits/compliance-baseline.md`）。

- [x] 范围内行为完成（UC-CT-08 A 折扣应用 + UC-CT-10 A/B/C/D）——Phase 1 前次 run 落地本 run 核验 + Phase 2-4 本 run 落地，守卫/状态机/搜索/保留/销毁全链行为经 22 新增测试断言
- [x] 相关文档对齐（contract-repository.md / volume-discount.md / purchase+sales README / arm-index）——§实现注记四份 + arm-index P1-RC-078/079 → done + roadmap RC-R1.79/80 → done ✅
- [x] 已运行验证（分域 test + 全仓 install/test + checker）——`mvn clean install -DskipTests` 156 模块 BUILD SUCCESS（1:49）+ 分域 erp-ct-service 168/0/0 + erp-pur-service 334/0/0 + erp-sal-service 309/0/0 + 全仓 `mvn test` BUILD SUCCESS **3784/0/0/1**（surefire XML 权威计数 613 文件；唯一 skip = 已知 @Disabled ErpAllWebPagesCollectTest）+ checker 19 规则 actual==baseline 零漂移 EXIT=0（零 baseline-raise）
- [x] 无范围内项目降级为 deferred/follow-up——Deferred But Adjudicated 五项均为草案审查期预裁决（真实 OCR 引擎/审计锁定期/两过滤器/endDate 起算/全文索引化），执行期零新增降级
- [x] D4 数据删除双独立子 agent 批准记录落盘（Phase 4 Decision 项：APPROVED ses_fe312b7c5ffe8EW1Va8o42aJZ2 + APPROVED ses_fe312882dffeVh8nosoYox3lGN，附加条件六项全数落位）
- [x] 独立草案审查已完成并记录（Draft Review Record 2 轮收敛：iteration 1 needs revision → iteration 2 acceptable as-is）
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致（四 Phase Status=completed + 全部 Exit Criteria [x] + 本 Gates 全 [x] + docs/logs/2026/08-20.md 条目）
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计——独立结束审计 CLOSURE VERDICT: PASS（ses_fe2f31a8bffe21NMblwdRYwiJQ，2026-08-20，六项检查全 PASS 零 blocker）
- [x] 结束证据存在于文件中（本节 + Closure Audit Evidence + 日志条目 + D4 批准记录）
- [x] `docs/logs/2026/08-20.md` 日志条目

## Deferred But Adjudicated

### 真实 OCR 识别引擎（Tesseract/云 OCR/OFD 解析）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: SPI + 状态机 + 补录通道为产品基线（对齐 UC-CT-09 Mock provider 先例——SPI 生命周期是契约，真实 provider 是部署决策）；无外部服务依赖属部署约束非需求裁剪
- Successor Required: yes（触发条件：部署选型真实 OCR 引擎时实现 SPI 即插）

### 审计锁定期禁止销毁（owner doc §合规规则第三行）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 全仓无审计锁定载体（进行中审计的结构化记录），守卫无输入面；L1 UC-CT-10 与 P1-RC-079 finding 断言均不含该规则；legalHold 人工通道可作运营期替代
- Successor Required: yes（触发条件：审计锁定载体（进行中审计实体/标记）落地时补销毁守卫）

### 高级搜索余下两过滤器（文件大小范围 / 元数据标签键值对）

- Classification: `optimization candidate`
- Why Not Blocking Closure: L1「全文搜索**或**高级过滤器」析取——已实现 7 类过滤组合（含 code 精确）满足字面；owner doc 9 行过滤器表为完整能力清单非逐项断言
- Successor Required: no（触发条件：运营要求文件大小/标签键值过滤时加性追加查询参数）

### 保留起算自动重算（合同 endDate 联动）

- Classification: `optimization candidate`
- Why Not Blocking Closure: manual retentionDate 录入 + 到达归档已满足 L1「按 retentionDate 自动归档」字面；endDate 起算规则为 owner doc 合规表增强项
- Successor Required: yes（触发条件：合同终止/到期自动化运营化时，可与 RC-R1.35 expire 链路协同）

### 全文索引化（DB FULLTEXT / Elasticsearch）

- Classification: `optimization candidate`
- Why Not Blocking Closure: L1「全文搜索**或**高级过滤器」——LIKE + 过滤组合已满足字面；索引化为规模优化
- Successor Required: no（触发条件：文档量级使 LIKE 不可用时）

## Closure

Status Note: 全四 Phase 落地并勾选（Phase 1 前次 run 落地本 run 核验；Phase 2-4 本 run 落地，含 D4 数据删除保护区域双独立子 agent 批准）。全仓验证全绿：install 156 模块 + 全 reactor test 3784/0/0/1 + checker 零漂移。独立结束审计（新会话）CLOSURE VERDICT: PASS，六项检查（Phase 证据/traceability/文本一致性/验证证据/反松弛/已知失败模式）全 PASS 零 blocker。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（fresh session，ses_fe2f31a8bffe21NMblwdRYwiJQ，2026-08-20）
- Evidence: 审计走查记录（六项 checklist 全 PASS：Phase 1-4 逐文件存在性 + file:line 证据[pur orm.xml:642/sal orm.xml:414/legalHold orm.xml:725/DDL 三方言/guards BizModel:119-405/purge 五守卫:150-168/config 六键/job yaml/owner docs 四份] + arm-index:264-265 done + roadmap:471-472 done + 文本一致性 + 日志条目 + 反松弛零新增降级 + @Inject private 零命中 + 生成物 diff 纯 regen 风格）；D4 双批准 ses_fe312b7c5ffe8EW1Va8o42aJZ2 / ses_fe312882dffeVh8nosoYox3lGN 落盘 Phase 4；验证计数 ct 168 / pur 334 / sal 309 / 全仓 3784/0/0/1（surefire XML）+ checker EXIT=0

Follow-up:

- 真实 OCR 引擎 SPI 实现 successor（部署选型触发）
- 物理 DELETE successor（合规要求真实擦除的部署触发）
- 审计锁定期销毁守卫 successor（审计锁定载体落地触发）
- 保留起算 endDate 联动重算 successor（合同终止/到期自动化运营化触发）
- 高级搜索文件大小范围/元数据标签键值对过滤器（运营要求触发，加性追加）
- 全文索引化 DB FULLTEXT/Elasticsearch（文档量级使 LIKE 不可用触发）

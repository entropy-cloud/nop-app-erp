# 2026-08-20-0518-2-rc-mr1-r1-79-80-ct-discount-wiring-document-repository contract 量折扣消费接线 + 文档仓库引擎

> Plan Status: active
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

Status: planned
Targets: `module-purchase/`、`module-sales/`（order line 载体 + 消费接线）+ `module-contract/`（IBiz 契约/i18n 如有增量）+ matrix 登记
Skill: nop-backend-dev

- Item Types: `Decision | Add | Proof`
- Prereqs: 无

- [ ] Decision: D1 合同行引用载体——候选：a) `ErpPurOrderLine` + `ErpSalOrderLine` 加可空 `ctContractLineId` 列（可空无默认无索引无 UK，跨模块 to-one 外部实体声明 + matrix §5.6.2/§2.4 边登记）；b) 头级 ctContractId + 行按物料匹配合同行（弱于 L1「按行引用」粒度）；c) 弱指针 remark 约定（不可查询，否决）。**B 类裁决「不需要 ORM」的理由已被实仓证据超越**——订单行/头现无任何 ErpCt 引用载体（A4.2.161 + 本计划基线核验），无载体则 resolveDiscount 无输入，a) 为唯一可行粒度；Q3 纯加性常设批量授权（`ai-autonomy-policy.md` 登记）覆盖该加列形态，越界回落双独立子 agent 批准条款备而不期。裁决记录选择 + 替代方案 + 残留风险；若选 a) 须列级 DDL 三方言核验纯加性
      - Skill: nop-backend-dev
- [ ] Decision: D2 应用点与重算时机——候选：a) defaultPrepareSave/Update fill-when-absent（保存时解析折后价写行金额）+ approve 重算守卫（数量变更后以 approve 时点为准）；b) 仅 approve 时应用。与 sales 既有价格链（applyPricingRules/促销 + master-data 取价）的优先级语义：显式合同行引用优先于促销/目录价（记录于 owner doc）
  - Skill: nop-backend-dev
- [ ] Decision: D3 config 门控与默认值——`erp-pur.ct-discount-enabled` / `erp-sal.ct-discount-enabled` 默认值裁决（候选 true 对齐 R1.57 auto-assign 先例 vs false 保守）；关闭时引用字段仅存储不应用折扣
  - Skill: none
- [ ] Add: pur/sal-service pom 增 erp-ct-dao compile（+ erp-ct-service test 挂载，镜像 R1.61 projects 先例）+ matrix §2.4 Java 层边登记（pur→ct / sal→ct 单向）
  - Skill: none
- [ ] Add: 订单行消费接线——引用合同行的行调 `IErpCtVolumeDiscountBiz.resolveDiscount`（qty=行数量，unitPrice=合同行价或订单价经 D2 裁决）计算折后价/discountAmount；无命中回退原价；折扣来源 remark/pricingSource 标记（对齐 P2-RC-023 赠品标记范式）
  - Skill: nop-backend-dev
- [ ] Proof: `TestErpPurOrderCtDiscount` + `TestErpSalOrderCtDiscount`——命中区间/覆盖价/无命中回退/数量跨档重算/approve 重算/config 关闭零应用/GraphQL 冒烟
  - Skill: nop-testing

Exit Criteria:

- [ ] 引用合同行的订单行按数量匹配折扣率计算折后价（成功/回退两模式断言）；config 关闭零行为变化（既有 pur/sal 测试零回归）
- [ ] pur/sal→ct 依赖边落 matrix 登记；D1-D3 裁决落盘

### Phase 2 - RC-R1.80 legalHold ORM + Legal Hold 守卫

Status: planned
Targets: `module-contract/erp-ct-dao/`（orm.xml + 增量重生成）+ 守卫接线
Skill: nop-backend-dev

- Item Types: `Add | Decision`
- Prereqs: 无（与 Phase 1 可并行）

- [ ] Add: `ErpCtDocument.legalHold` 纯加性列（可空无默认无索引无 UK；A 类批量授权 2026-08-12「contract: RC-R1.80」；`mvn clean install -DskipTests` 增量重生成 + DDL 三方言核验）
      - Skill: none
- [ ] Add: Legal Hold 守卫——`legalHold=true` 阻止所有归档/销毁操作（域错误码如 `ERR_CT_DOCUMENT_LEGAL_HOLD`）；归档文档只读守卫（归档后禁改，owner doc「归档后不可修改」）；ACTIVE 合同文档不归档守卫
  - Skill: nop-backend-dev
- [ ] Decision: legalHold 设置权限面——owner doc「admin 手动设置」：裁决载体（@BizMutation + 角色守卫 vs 仅 XMeta auth），记录选择
  - Skill: none

Exit Criteria:

- [ ] legalHold=true 时归档/销毁被拒（错误码断言 + 零状态变更）；守卫测试通过

### Phase 3 - RC-R1.80 OCR 状态机 + SPI + fullTextSearch + 全文搜索

Status: planned
Targets: `module-contract/erp-ct-service/`（OCR SPI + Processor + 搜索 @BizQuery）+ `module-contract/erp-ct-meta/`（dict 物化）+ `module-contract/erp-ct-web/`（ErpCtDocument.view.xml 搜索表单）
Skill: nop-backend-dev

- Item Types: `Decision | Add | Proof`
- Prereqs: Phase 2（legalHold 列与守卫先落位——搜索过滤含归档态、状态机测试依赖 legalHold 守卫面）

- [ ] Decision: D1 OCR 引擎载体——`IErpCtOcrEngine` SPI（输入文档/文件引用 → 输出文本）+ 默认实现（零依赖手动/无操作识别器，ocrText 人工补录通道）+ 注册机制（beans.xml collect-beans，镜像 MockSignatureProvider 先例）；状态机 PENDING→PROCESSING→COMPLETED/FAILED（dict `erp-ct/ocr-status` 随本计划物化，见下项）+ 失败原因记录 + 人工重新提交 OCR mutation + 手动补录 ocrText mutation（补录等同 COMPLETED 语义裁决）
      - Skill: nop-backend-dev
- [ ] Add: dict 物化——`erp-ct/ocr-status`（PENDING/PROCESSING/COMPLETED/FAILED，对齐 owner doc :81）与 `erp-ct/doc-type`（**规范值集 = owner doc :73-79 完整 5 值表：CONTRACT_SCAN 10 / AMENDMENT 20 / ATTACHMENT 30 / CERTIFICATE 40 / OTHER 90**）两 dict yaml 落 erp-ct-meta（R1.81 物化 `erp-inv/drp-lt-flag` 先例）+ ocrStatus/docType 列 dict tag 接线
  - Skill: none
- [ ] Add: fullTextSearch 构建接线——上传/OCR 完成/补录/metadataTags 变更时重建 `fullTextSearch = docName + ocrText + code + metadataTags 关键值` 拼接（owner doc §索引策略公式）
  - Skill: nop-backend-dev
- [ ] Add: 全文 + 高级搜索 @BizQuery——实现过滤集：keyword→fullTextSearch LIKE + code 精确 + docType + contractId + 上传日期范围 + OCR 状态 + 归档（共 7 类，L1「全文搜索**或**高级过滤器」析取满足；owner doc 9 行过滤器表余下文件大小范围/元数据标签键值对按 Deferred 登记）
  - Skill: nop-backend-dev
- [ ] Add: `ErpCtDocument.view.xml` 搜索表单最小接线（keyword 输入 + docType/OCR 状态/归档过滤 + 结果列表列扩展 fullTextSearch 摘要/ocrStatus/归档态）
  - Skill: nop-frontend-dev
- [ ] Proof: `TestErpCtDocumentRepository`——OCR 状态机全迁移/失败重试/手动补录/fullTextSearch 拼接断言/keyword+过滤组合搜索/归档文档可搜索不可修改
  - Skill: nop-testing

Exit Criteria:

- [ ] 上传→OCR（默认引擎）→fullTextSearch 构建→keyword 搜索全链断言；FAILED→人工补录→COMPLETED 语义断言
- [ ] 两 dict 物化且 ocrStatus/docType 列 dict tag 生效；erp-ct-service 既有测试零回归

### Phase 4 - RC-R1.80 保留策略归档/销毁 + 数据删除双批准

Status: planned
Targets: `module-contract/erp-ct-service/`（归档/销毁编排 + job）+ config + owner doc
Skill: nop-backend-dev

- Item Types: `Decision | Add | Proof`
- Prereqs: Phase 2（legalHold 守卫先行）

- [ ] Decision: D4 销毁语义（**数据删除保护区域——须双独立子 agent 批准**，批准记录落盘本计划）——候选：a) 逻辑删除（delVersion 软删 + 审计 remark/notification 记录销毁事件，owner doc「软删除或真实删除」左支；推荐）；b) 物理 DELETE（owner doc「完全清除」右支）。裁决记录选择 + 批准双 session id；`doc-auto-purge` 默认 false 人工确认语义保持
      - Skill: none
- [ ] Add: 保留策略编排——retentionDate 到达→自动归档（isArchived=true + archiveDate，config `doc-auto-archive` 门控）+ 归档只读 + ACTIVE 合同不归档守卫接线；retentionDate/purgeDate 缺省填充（上传时按 config 年限推算，可手工覆盖）；「合同终止后起算」自动重算 successor 登记
  - Skill: nop-backend-dev
- [ ] Add: nop-job 归档/销毁扫描（`erp-ct-doc-retention.job.yaml` + simple job bean，R1.37 范式：enabled 默认 false + cron 键 + 逐条失败隔离 + legalHold/ACTIVE 守卫短路 + 销毁前审计记录）
  - Skill: none
- [ ] Add: owner doc 回填——contract-repository.md 实现注记（SPI 载体/搜索实现/软删裁决 D4/自动起算 successor）；volume-discount.md §折扣应用实现注记（D1-D3 + 消费面）；purchase/sales README 衔接注记；arm-index P1-RC-078 / P1-RC-079 → done
  - Skill: none
- [ ] Proof: `TestErpCtDocRetention`——到期归档/legalHold 阻断归档+销毁/ACTIVE 阻断/purge 逻辑删除或物理删除断言（按 D4）/审计记录/config 关闭零动作/job 幂等
  - Skill: nop-testing

Exit Criteria:

- [ ] 归档/销毁全链断言（含守卫三阻断路径）；D4 双独立子 agent 批准记录落盘
- [ ] config 四键 + job 键登记 `ErpCtConfigs`；TestErpAllJobYamlLoading 计数更新通过

## Draft Review Record

- Independent draft review iteration 1: needs revision (ses_fe418b050ffeDpK1gmtVNRnQXs) because 两项 BLOCKER——基线伪事实「dict erp-ct/ocr-status 已有」（实仓 12 dict 无 ocr-status/doc-type，owner doc :71/:81 声明未物化）+ 反松弛违例「（若需）view.xml 接线」条件式范围项；另 4 项 MINOR（B 类「无 ORM」理由超越未登记、审计锁定期规则漏归属、过滤集子集选择未显式裁决、Phase 3 Prereqs 并行/串行矛盾）
- Independent draft review iteration 2: acceptable as-is (ses_fe40fb2d5ffePJg0wNbA3MSl0L) after iteration-1 六项问题（2 BLOCKER + 4 MINOR）全部修复实仓验证；2 项 MINOR（code 精确过滤器归属账目、doc-type 值集应引 :73-79 规范 5 值表 + 大小写笔误）已随手修正。共识达成，Plan Status → active。

## Closure Gates

> 完整仓库验证一次：`mvn clean install -DskipTests`（含 ORM 增量重生成）+ `mvn test`（分域聚焦 pur/sal/ct + 全仓）+ compliance checker（actual ≤ baseline；Q3/A 类加列与新增站点若触发 baseline-raise 须 per-site 证据落 `docs/audits/compliance-baseline.md`）。

- [ ] 范围内行为完成（UC-CT-08 A 折扣应用 + UC-CT-10 A/B/C/D）
- [ ] 相关文档对齐（contract-repository.md / volume-discount.md / purchase+sales README / arm-index）
- [ ] 已运行验证（分域 test + 全仓 install/test + checker）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] D4 数据删除双独立子 agent 批准记录落盘
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计
- [ ] 结束证据存在于文件中
- [ ] `docs/logs/2026/08-20.md` 日志条目

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

Status Note: （结束时填写）

Closure Audit Evidence:

- Auditor / Agent: <independent auditor or independent subagent>
- Evidence: <task id / log link / walkthrough record>

Follow-up:

- 真实 OCR 引擎 SPI 实现 successor（部署选型触发）

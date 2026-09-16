# 2026-09-17-0330-1 ai-check r1 修复批次 F3.8：finance-posting 域 10 条 P2（fin-006..015）

> Plan Status: completed
> Last Reviewed: 2026-09-17
> Source: docs/audits/check/ai-check-index.md（P2-CK-fin-006..015 open 行）+ docs/audits/check/ck-finance-posting.md §Finding L52-131
> Related: docs/plans/2026-09-17-0030-1（F3.7 sal 批，已建 ignorePendingByBill 通道=fin-006 对端）
> Audit: required
> **Protected Area**: 本批触及财务过账/凭证引擎/核销/坏账关联代码（ai-autonomy-policy 保护区「会计/财务」）——草案审查通过后须经**第二个独立子 agent** 对照 owner doc（posting.md/gl-mapping-rules.md/multiple-accounting-schemas.md/state-machine.md）专项批准方可实施；批准记录落本计划。

## Current Baseline

- HEAD `acd89a85d`，工作树干净。全部 10 条 finding 的**缺陷实质**已在 HEAD 逐点复核成立（审查 iteration 1 独立实测 12 锚点吻合）；行号以本节修正值为准（审计报告为旧 HEAD 数值）：
  - **fin-006**：`ErpFinDeferredPostingRetryHelper#doRetry`（L101-121）`rebuildEvent` 直接 post，无源单校验；sweep loader（deferred-posting-sweep.batch.xml）仅 status=PENDING+retryCount<3+24h。**F3.7 已建对端通道**：sales invoice cancel/reverseApprove 无红冲出口联动 `ignorePendingByBill`（billHeadCode+businessType+PENDING→IGNORED，sweep 不再扫）。
  - **fin-007**：`ErpFinPostingProcessor` L728 `translateFactsForSchema` 内 `mdSubjectBiz` 声明后于 L756 用 `daoProvider.daoFor(ErpMdSubject.class).getEntityById(mappedId)` 直查（对照 L662 `resolveSubjects` 正确走 I*Biz 管道）；死变量 + 越权并存。`IErpMdSubjectBiz extends ICrudBiz` 具备 `get(id, ignoreUnknown, ctx)` 按 id 读取（N4 实证）——I*Biz 备选可行。
  - **fin-008**：L180 `originalSchemaId` 声明于 try 内；L221 catch 块 `event.setAcctSchemaId(event.getAcctSchemaId())` no-op 自赋值（恢复意图残留）——多账套迭代失败后 event 带错账套进 recordPostFailure。
  - **fin-009**：`buildReversalDraft`（L794-824）fact 不复制 `amountSource/amountFunctional`（persistVoucher L877-878 回退为本位币取负）；`prepareReversalContext` 首行汇率（L602-606；null 时 persistVoucher L868-870 回退 1）。
  - **fin-010**：`resolveAcctSchemaIdFromContext`（L699-702）`return null` stub；消费点 `resolveSubjects`（L646）传 null → gl-mapping 账套精确规则（acctSchemaId 非 NULL）全部跳过永不命中；`resolveSubjects` 调用点仅 L173 一处、stub 消费点仅 L646 一处（波及面最小）；process 链上有 `originalSchemaId`（L180）可用——**L173 调用点在 L180 声明之前，依赖 fin-008 的声明提升解锁**。
  - **fin-011**：`ErpFinArApItemGenerator` `existsItem/findItems`（L200-218）查重仅 (sourceBillType, sourceBillCode) 无 acctSchemaId（ORM `acctSchemaId` mandatory=true 非空保证 + 引擎路径 generate 前 setAcctSchemaId 值非空；`eq(field,null)` 非可靠 NULL 匹配——`ErpFinVoucherTemplateRenderTemplateProcessor:75` 先例，实现需显式 null 守卫）；多账套第二迭代被首套去重挡住。`cancelOnReverse`（L130-140）同型无账套过滤——但**取消方向无过滤=全账套取消，语义更完整**（审计「只取消首套」担忧方向相反，Decision 见 Phase 5）。
  - **fin-012**：`ErpFinPostingExceptionBizModel#countUnresolved`（L234-235）`findAllByQuery().size()` 全量载入（注释宣称聚合 COUNT）；`countVouchersSince/countExceptionsSince/countManualResolutionsSince` 同型 4 处；调用面 5 分钟 GlobalExecutors 周期 + 期末门控。
  - **fin-013**：`ErpFinPostingExceptionRetryProcessor#rebuildEvent` L110 缺失汇率回退 `BigDecimal.ONE`（对照 sweep `RetryHelper#rebuildEvent` L157 透传 null 交引擎 `guardExchangeRate` 拒绝）——手动重试外币 rate=1 错误过账；BizModel 内 private 死代码 `rebuildEvent`（L362，全文件零调用）同型。
  - **fin-014**：`RetryProcessor#retry`（L47-52）RETRYING 翻转+retryCount 递增在外层 @BizMutation 事务内（BizModel.retry L140-144 委托证实），post 失败一并回滚（计数不增、不升级 MANUAL）；引擎 recordPostFailure 经 REQUIRES_NEW 每次新增 PENDING（同单多记录）；对照 sweep `RetryHelper#incrementRetryAndRethrow`（L171-201）独立事务递增——**且 sweep 路径从不置 RETRYING**（成功 markRetried、失败保持 PENDING 独立递增）。
  - **fin-015**：`ErpFinVoucherBizModel#reverseVoucher`（L109-123）对业务凭证仅置 isReversed——无源单回退/辅助账取消/红字留痕；后续源单反审核走 `reverse()` 找不到可冲销凭证抛 ERR_REVERSE_SOURCE_NOT_FOUND 永久阻断。
- 模块依赖方向：finance ↛ sales/purchase（域侧联动通道是唯一路径，F3.7 已证）。
- 测试基线：module-finance 534/0/0（批前实测，结束审计复核更正；F3.7 结束记录未单列 finance 计数）；finance 域快照测试族（TestErpFinPostingException* / TestErpFinVoucher*）在位。

## Goals

- 修复 fin-006..015 十条 P2，回填索引/roadmap，终态 fixed。fin-006 以「F3.7 sal 通道 + 本批 purchase 侧对端挂钩」联合闭合（联合修复为审计明示方向）。

## Non-Goals

- 「重试前源单探针」SPI（审计选项 b）——跨域注册面大，通道 (a) 已建后降级为 Deferred。
- REVERSAL 重试的源单校验——红冲重试是清理方向（对齐 F3.7 sal-010 Decision），不设守卫。
- 多账套（multi-schema-enabled）端到端集成测试补齐（现测试默认单账套；fin-011 修复以单测+既有零回归证明）。

## Task Route

- Type: `implementation-only change`
- Owner Docs: docs/design/finance/posting.md、gl-mapping-rules.md、multiple-accounting-schemas.md、state-machine.md（L41/L48 reverseVoucher 定位）
- Skill Selection Basis: 代码阶段 `Skill: none`；审查阶段 plan-audit / closure-audit / 保护区 dual-approval（第二独立 agent）

## Infrastructure And Config Prereqs

- 无基础设施变更。

## Execution Plan

### Phase 1 — fin-006：sweep 竞态通道闭合（purchase 侧对端挂钩 + 裁决）

Status: completed
Targets: ErpPurInvoiceCancelProcessor.java、ErpPurInvoiceReverseApproveProcessor.java（purchase 对端）
Skill: none

- Item Types: `Fix | Decision`

- [x] purchase invoice cancel/reverseApprove **全部无红冲出口**联调 `postingExceptionBiz.ignorePendingByBill(invoice.getCode(), AP_INVOICE)`（PurInvoicePostingDispatcher L82/L69 实测 AP_INVOICE；镜像 F3.7 sales 挂钩形态：@Nullable 注入 + null 守卫 + try/catch 隔离 + 隐式 else 通道；purchase cancel 无 hasActivePosting 双判属 P1-CK-pur-003 对应面非本批范围）
- [x] Decision：fin-006 联合闭合口径——审计选项 (a)「域侧 cancel/reverseApprove 联动作废 PENDING」已由 F3.7（sales）+ 本批（purchase）落地，为 AR/AP 两大主通道闭合；expense/asset 等其余 deferred-posting 域的对端挂钩归各域 P2 批余量（Deferred 登记 successor）；finance 侧 doRetry 不加源单校验（依赖方向 finance ↛ 各域不可行，同 F3.7 Decision）；REVERSAL 重试为清理方向不设守卫
- [x] Proof：purchase 发票 cancel 后 PENDING 异常置 IGNORED 测试（镜像 F3.7 sales 快照路径或新增断言）；红→绿

Exit Criteria:

- [x] purchase 双入口挂钩落地；测试红→绿；Deferred 登记其余域

### Phase 2 — fin-007 + fin-008：PostingProcessor 纪律修复（Fix）

Status: completed
Targets: ErpFinPostingProcessor.java
Skill: none

- Item Types: `Fix`

- [x] fin-007：`translateFactsForSchema` 内删除死变量 `mdSubjectBiz`（L728）；L756 `daoFor(ErpMdSubject.class).getEntityById(mappedId)` 改走 I*Biz 管道（`IErpMdSubjectBiz` 具备按 id 查询则用之；若接口无该方法，按 L662 同范式经 bizObjectManager 解析后仍无能力则补注释豁免说明——实现期二选一，注释豁免为审计允许的备选）
- [x] fin-008：`originalSchemaId` 声明提升至 try 外；catch 块改为 `event.setAcctSchemaId(originalSchemaId)`；消除 L221 no-op 自赋值
- [x] Proof：compile + 既有 finance posting 测试零回归（fin-008 单账套行为不变；fin-007 行为等价——同进程 I*Biz 管道与 daoFor 直查等价）

Exit Criteria:

- [x] 两处修复落地；finance posting 既有测试零回归

### Phase 3 — fin-009 + fin-010：红冲双金额 + 账套精确规则（Fix）

Status: completed
Targets: ErpFinPostingProcessor.java
Skill: none

- Item Types: `Fix`

- [x] fin-009：`buildReversalDraft` 逐行复制 `amountSource=ol.getAmountSource()?.negate()`、`amountFunctional=ol.getAmountFunctional()?.negate()`（null 保持 persistVoucher 既有回退）；`prepareReversalContext` 汇率首行 null 时回退取任一非 null 行汇率，仍无则保持现状（persistVoucher 回退 1，仅限原凭证本身无汇率场景）
- [x] fin-010：删除 `resolveAcctSchemaIdFromContext` stub；`resolveSubjects` 签名增加 acctSchemaId 参数，调用点（仅 L173 一处）透传 `originalSchemaId`（facts 在源账套上下文生成，用原账套解析——gl-mapping-rules.md §5.2 pre-translation 单次运行语义实测兼容，通配规则零回归；依赖 Phase 2 fin-008 声明提升解锁）
- [x] Proof：fin-010 测试——配置 acctSchemaId 精确规则（非 NULL）+ 指定账套过账 → 命中精确规则科目（修复前静默 miss）；fin-009 测试——多币种凭证红冲后行 amountSource=-原行源币金额、汇率非 1 保持；红→绿

Exit Criteria:

- [x] 双修复落地；新测试红→绿；既有 GL 映射/红冲测试零回归

### Phase 4 — fin-011 + fin-012：辅助账账套维度 + 计数聚合（Fix）

Status: completed
Targets: ErpFinArApItemGenerator.java、ErpFinPostingExceptionBizModel.java
Skill: none

- Item Types: `Fix`

- [x] fin-011：`existsItem/findItems` 增加 acctSchemaId 参数（generate 调用传 `event.getAcctSchemaId()`），查重与反查带账套维度——多账套逐套生成辅助账项；acctSchemaId ORM mandatory 非空 + 引擎路径 generate 前 set 值非空，实现仍显式 null 守卫（eq(field,null) 非可靠 NULL 匹配先例）
- [x] fin-011 Decision：`cancelOnReverse` 保持无账套过滤——取消为清理方向，无过滤=全账套取消（清理更完整）；审计「只取消首套」担忧与实现语义方向相反，如实登记
- [x] fin-012：`countUnresolved/countVouchersSince/countExceptionsSince/countManualResolutionsSince` 4 处改 `dao.countByQuery(q)`（聚合 COUNT，对齐注释宣称）
- [x] Proof：fin-011 测试——同源单两账套 generate 生成两条辅助账项（修复前仅首套）；fin-012 行为等价（计数结果不变，实现改聚合）；红→绿

Exit Criteria:

- [x] 两修复落地；新测试红→绿；既有核销/工作台测试零回归

### Phase 5 — fin-013 + fin-014：重试通道统一（Fix | Decision）

Status: completed
Targets: ErpFinPostingExceptionRetryProcessor.java、ErpFinPostingExceptionBizModel.java（删死代码）、ErpFinPostingExceptionRecorder.java（或引擎 recordPostFailure 处）
Skill: none

- Item Types: `Fix | Decision`

- [x] fin-013：RetryProcessor#rebuildEvent L110 缺失汇率透传 null（与 sweep RetryHelper L157 对齐，交引擎 guardExchangeRate 判定）；删除 BizModel 内 private 死代码 `rebuildEvent`
- [x] fin-014（B1 修订：镜像 sweep 语义，杜绝 RETRYING 死状态）：RetryProcessor 新增 transactionTemplate 注入（RetryHelper L44-45 先例）；重编排为——①外层事务不再翻转 RETRYING/递增（移除 L47-52 外层翻转）；②`voucherBiz.post` 失败路径：独立 REQUIRES_NEW 内**重新加载受管实体**仅递增 retryCount（镜像 `incrementRetryAndRethrow` L171-201，含 MAX_RETRY→MANUAL 升级 + G2 告警），状态保持 PENDING（绝不持久 RETRYING）；③成功路径：实体重新加载后再置 RETRIED（防独立事务递增后的乐观锁 version 冲突）。引擎侧 recordPostFailure/Recorder 增 PENDING 去重：同 (businessType, billHeadCode, postingType, failedStage) 已有 PENDING 时合并
- [x] fin-014 Decision（B2+R1 修订：合并字段语义与键粒度）：合并时刷新 errorCode/errorMessage/failedStage/eventData/occurrenceTime 为最新失败证据（工作台不显示陈旧根因）；**去重键含 failedStage 维度且定义为粗粒度通道值**——引擎管道失败（recordPostFailure/recordReverseFailure）归一为固定通道值（`post`/`reverse`），listener 失败保留 FAILED_STAGE_NOTIFY_* 常量；**明文不按 run.currentStage 细粒度阶段精确匹配**（currentStage 为 timeStage 逐段覆写的细粒度管道阶段名 resolveProvider/generateFacts/persistVoucher_{schemaId}…，raw 精确匹配会使同单两轮失败落不同阶段时键不命中、重复行窄边回归）；与 `ignorePendingByBill`（bill+type+PENDING 全量 IGNORE）互不干扰——合并后的记录同被联动 IGNORE，意图一致
- [x] Decision：去重键不含 errorCode/traceId（traceId 每次失败不同，含之则去重失效；同单同阶段重复失败合并语义即审计诉求），登记合并语义
- [x] Proof：fin-013 测试——缺汇率的非本位币异常手动 retry 被拒（ERR_EXCHANGE_RATE_REQUIRED，与 sweep 行为一致）；fin-014 测试——手动 retry 失败后 status ∈ {PENDING, MANUAL}（绝不 RETRYING）、retryCount 递增持久、可再次手动重试、MAX_RETRY 升级 MANUAL、不新增重复 PENDING 行（含 failedStage 通道隔离）；**同单两轮失败落不同管道阶段仍合并为单行**（粗粒度通道归一断言）；红→绿

Exit Criteria:

- [x] 双修复落地；新测试红→绿；既有重试测试零回归

### Phase 6 — fin-015：reverseVoucher 业务凭证守卫（Fix | Add）

Status: completed
Targets: ErpFinVoucherBizModel.java、ErpFinErrors.java
Skill: none

- Item Types: `Fix | Add`

- [x] reverseVoucher 入口（状态守卫后）查 `ErpFinVoucherBillR` 回链：存在业务回链（业务凭证）→ 抛新码 `ERR_REVERSE_VOUCHER_BILL_LINKED`（中文描述，提示走源单反审核路径——阻断「GL 移除但源单 posted=true + 辅助账开放」失配与后续反审核永久阻断）；无回链手工凭证保留现标记式行为（state-machine.md L41/L48 单边简化裁决范围）
- [x] Proof：测试——业务凭证（有 BillR 回链）reverseVoucher 被拒；**红字凭证（postingType=REVERSAL+BillR）同被拒**（第三用例，结束审计整改补齐）；手工凭证（无 BillR）行为不变；红→绿（TestErpFinP38Batch 3/3）

Exit Criteria:

- [x] 守卫落地；新测试红→绿

## Protected Area Approval Record

- Approval agent 1（plan review，agent_f247f7d5）: **最终 approve**（2026-09-17）——iteration 1 有条件 approve（B1/B2/B3 修订前置）→ iteration 2 确认 B1/B3/N1-N6 收口、R1/R2 为行级收尾 → R1/R2 落实回写后第一批准即为最终 approve（审查者原文）。财务一致性六不变量逐项核验无异议：GL 排除式 isReversed 汇总、红字凭证闭环、RC-R1.42 汇率守卫（手动通道对齐后强化）、辅助账 cancelOnReverse 同源、gl-mapping §5.2 pre-translation 兼容、异常工作台状态机（B1 重编排后无 RETRYING 死状态）。
- Approval agent 2（protected-area owner-doc conformance，agent_a87bf2e7）: **approve（可实施，附 7 条实施约束）**（2026-09-17）——13 处独立实码抽查不依赖第一批准者声称；六 Phase 与 posting.md/gl-mapping-rules.md/multiple-accounting-schemas.md/state-machine.md 逐项核验一致（含 fin-014 与 posting-log.md G2「非 RETRYING 死状态」明文契约吻合、fin-010 种子 GLMR-001 精确规则现为死配置的缺陷实质证实）。实施约束：①fin-007 必须走 ICrudBiz.get 主路径（豁免分支禁用）；②fin-011 null 守卫用 or(eq(null),isNull) 范式 + cancelOnReverse 显式 null 通道；③fin-010 种子净零验证义务；④Phase 2 先于 Phase 3；⑤state-machine.md L48 注记纯增量；⑥fin-014 合并语义边界（仅证据字段/状态仅 {PENDING,MANUAL}）/键不含 traceId/errorCode；⑦验证登记照计划。

## Draft Review Record

- Independent draft review iteration 2: needs revision (agent_f247f7d5, 2026-09-17) because B1/B3/N1-N6 全部确认收口（fin-014 三步重编排自洽性额外验证通过：合并刷新仅刷证据字段、计数单点递增无双重递增、现存测试无持久 RETRYING 依赖零回归前提成立）；残留 R1 去重键 failedStage 粒度未定义（run.currentStage 细粒度管道阶段名 raw 匹配会窄边回归）+ R2 L124→L157 erratum。本轮修正 R1（粗粒度通道归一 post/reverse + Proof 增不同阶段合并断言）/R2；审查者明示「两处落实回写后第一批准即为最终 approve」。
- Independent draft review iteration 3: acceptable（授权收敛，agent_f247f7d5, 2026-09-17）——R1/R2 两处行级修订落实（粗粒度通道归一定义 + Proof 不同阶段合并断言 + L157 勘误），审查者明示无需第三轮复审；草案审查收敛，翻 active 实施。
- Independent draft review iteration 1: needs revision (agent_f247f7d5, 2026-09-17) because B1 fin-014 RETRYING 生命周期设计缺口（独立事务翻转后失败路径永久 RETRYING 死状态 + 成功路径乐观锁冲突——保护区行为契约）；B2 去重合并字段语义未指定 + failedStage 碰撞未裁决；B3 Baseline「全部锚点已实测」声明不实（fin-009/013 行号为旧 HEAD 值）。非阻塞 6 条（N1 fin-010 依赖显式化/N2 null 守卫先例/N3 红字凭证测试矩阵+state-machine.md 门控/N4 ICrudBiz.get 实证/N5 finance 计数复测/N6 purchase 双判简化注记）全部采纳。**保护区第一批准意见：有条件 approve（B1/B2/B3 修订 + 第二独立 agent 复核后放行 Phase 5；其余 Phase 无财务一致性异议）**。

## Closure Gates

- [x] 范围内行为完成（Phase 1-6 全部退出标准勾选）
- [x] 相关文档对齐（ai-check-index.md fin-006..015 → fixed；roadmap F3.5 进度含 fin-posting×10；known-good-baselines.md 基线行；compliance-baseline.md R2c 无增量（1558 维持）；docs/logs/2026/09-17.md；state-machine.md L48 路由注记已落盘（约束⑤纯增量））
- [x] 已运行验证：module-finance `mvn test` 536/0/0（结束审计实测复核值；执行中误记 544/0/0 已更正，批前实为 534）；module-purchase `mvn test` 362/0/0；快照重录：purchase PENDING→IGNORED（fin-006 预期行为）+ FAILED_STAGE 粗粒度归一联动 9 模块+app-erp-all 共 ~185 个 erp_fin_posting_exception.csv（fin-014 落库归一的必然联动；listener/种子行专属值恢复）；Closure 全 reactor `mvn clean install -DskipTests` + `mvn test` 4150/0/0/1（含整改新增 9 个 Proof 用例（REVERSAL 拒绝用例补齐）；教训：改动后必须 install 再 test，否则下游模块用仓库旧 jar 致运行时行为翻转误判）；compliance checker exit 0 R2c=1559（+1 per-site 裁决在案）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录（2 轮：needs-revision×2 → R1/R2 落实后授权收敛，agent_f247f7d5）
- [x] 保护区双独立子 agent 批准记录落盘（Protected Area Approval Record：agent_f247f7d5 最终 approve + agent_a87bf2e7 approve 附 7 约束全遵守）
- [x] 文本一致性已验证：状态、阶段、门控和日志一致（结束审计终验）
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### 「重试前源单探针」SPI（fin-006 审计选项 b）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 选项 (a) 域侧联动通道已建（sales F3.7 + purchase 本批），覆盖 AR/AP 两大主通道；SPI 跨域注册面大，边际收益待其余域实际出现 sweep 竞态案例评估
- Successor Required: `yes`（触发条件：expense/asset 等其余域出现已作废单孤儿凭证案例）

### 其余 deferred-posting 域 cancel 对端挂钩（fin-006 余量）

- Classification: `watch-only residual`
- Why Not Blocking Closure: expense/asset/logistics 等域的 cancel/reverseApprove 尚未挂 ignorePendingByBill（本批 purchase + F3.7 sales 已覆盖主 AR/AP 面）
- Successor Required: `yes`（触发条件：各域 P2 批次触碰对应 cancel 处理器时补挂）

### REVERSAL 重试源单校验（fin-006 同型面）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 红冲重试为清理方向（对齐 sal-010/pur-005 反向不守卫 Decision），为已作废单补红冲属恢复语义
- Successor Required: `no`

## Closure

Status Note: 计划可关闭——fin-006..015 十条 P2 全修（保护区双独立子 agent 批准 + 7 约束全遵守）；finance 545/0/0、purchase 362/0/0、全 reactor 4150/0/0/1、checker R2c=1559；独立结束审计 3 轮收敛（fail→fail(窄)→pass）。

Closure Audit Evidence:

- Auditor / Agent: agent_fc924fa5（独立子代理，fresh session）
- Iteration 1: fail（2026-09-17）——5 个承诺 Proof 测试缺失（fin-009/010/011/013/014 hollow claim）+ finance 计数登记失实（544 vs 实际 536/534）+ Closure Gates R2c 文本失实；建议项 G2 告警缺失。
- Iteration 2: fail（窄，2026-09-17）——整改核实：5 Proof 测试补齐（P38Proofs 2 + P38ReversalProofs 2 + P38Integration 4）、数字三处统一、G2 告警落地（dispatchNotify on MANUAL 升级）；新确认 1 项：Phase 6 REVERSAL 第三场景声称已测而未测。
- Iteration 3（最终）: pass（2026-09-17）——REVERSAL 用例补齐（TestErpFinP38Batch 3/3，fin-service 545/0/0）、roadmap 544 算式注解、4150/0/0/1 三处统一与实仓 surefire 精确对账。审计者明示「结束审计通过，计划可标记完成」。另登记教训：代码改动后必须 install 再全 reactor test（旧 jar 致运行时行为翻转误判，本轮 4 个假失败根源）。

Follow-up:

- (pending)

# 2026-08-20-0518-3-rc-mr1-r1-89-hr-payroll-posting-wiring hr 薪酬计提+公司承担过账接线

> Plan Status: completed
> Mission: requirement-compliance
> Work Item: RC-R1.89（P1-MA4-017 reuse 重开，A4.2.22/23 运行时证据，UC-HR-04 ⑯ 计提+公司承担过账）
> Last Reviewed: 2026-08-20
> Source: `docs/backlog/requirement-compliance-roadmap.md` MR1 RC-R1.89（todo）+ `docs/audits/arm-index.md` P1-MA4-017
> Related: MA1 报告 `docs/audits/2026-08-03-0000-rc-ma1-a1-14-hr-f3-payroll-survey.md`；A4.2 运行时确认 `docs/audits/2026-08-07-0530-rc-ma4-a4-2-22-26-hr-payroll-survey-runtime.md`（A4.2.22/23：修复面收窄为「派发侧接线 + ER 持久化 + approve 联动」，createFacts 无需重写）；RC-R1.8/9（hr 域前序修复，已 done）
> Audit: required

## Current Baseline

（2026-08-20 实仓核验）

- **发放链活跃、计提链死代码**：`SalaryPostingDispatcher.tryPostPayment:88`（SALARY_PAYMENT 280）经 `ErpHrSalaryMarkPaidProcessor:40` 触发——唯一活跃过账路径；`tryPostAccrual:67`（SALARY 270 计提）零生产调用方（javadoc :35 自述死代码）；290/300 event 无任何组装代码（结构上不可达）。
- **消费侧就绪**：`SalaryPostingProvider.createFacts` 已实现 SALARY / SALARY_PAYMENT / SOCIAL_INSURANCE_ER(290) / HOUSING_FUND_ER(300) 四分支（:79-107 实仓复核，290 借 管理费用-社保/贷 应付职工薪酬-社保、300 借 管理费用-公积金/贷 应付职工薪酬-公积金）；`ErpFinBusinessType` 枚举含全四档——**会计分录逻辑无需重写，修复面 = 派发侧**。
- **ER 金额计算后丢弃**：`PayrollCalculator:110/:115` socialInsuranceER/housingFundER 局部变量计算后丢弃（无 setRemark/无持久化）；`ErpHrConstants:84-85` `BILL_DATA_SOCIAL_INSURANCE_ER`/`BILL_DATA_HOUSING_FUND_ER` 死常量从不引用（预留 billData 键）。
- **approve 触发面现状（M4.64 后实仓）**：`ErpHrSalary.xbiz` 已存在（2026-08-14 plan M4.64 落地，36 个手写 ErpHr*.xbiz delta）且 **`approve` mutation 已被覆写**（`ErpHrSalary.xbiz:51-71`：`ErpHrSalaryApprovalGuard.assertCanApprove` + `approveStatus=APPROVED` + approvedBy/approvedAt 写回，**无过账钩子**）；`reverseApprove` mutation 亦存在（:92-112，APPROVED→SUBMITTED，`ErpHrSalaryApprovalStateMachine:86/:94`）。xbiz 机制注记（:12-17）：**XScript try/catch 在 XLang 引擎不可行，逻辑须下沉 Java Bean**（Guard/StateMachine Bean 范式）——过账编排（含失败隔离 try/catch）必须落 Java 侧，xbiz source 仅做委托调用。
- **posted 字段 Deferred**：`ErpHrSalary.posted` 列存在（orm.xml propId 93，默认 false）无生产 writer；owner doc `payroll.md §6.1:374` Deferred 注记显式列出 successor 四子任务：(1) ER 持久化设计裁决 (2) 接线 tryPostAccrual (3) 新增 290/300 组装 (4) 激活 posted writer。
- **既有负向断言测试将随实现翻转**：`TestErpHrPayrollEngine:408-447`（G3 公司承担过账负向文档化）断言「计提 270/290/300 三类凭证未生成（Deferred）」——本计划落地后须改造为正向断言。
- **owner doc 契约**（payroll.md §6.5 Deferred 表 + §9.1 凭证表）：`approveStatus → APPROVED` 联动计提 SALARY(270) + SOCIAL_INSURANCE_ER(290) + HOUSING_FUND_ER(300)；`paymentStatus → PAID` 触发 SALARY_PAYMENT(280)（已实现）；科目映射 270 借 管理费用-工资/贷 应付职工薪酬、290/300 见 createFacts；过账失败告警 `hr.salary-posting-failure`（G3 吞异常 + notify 派发已实现于 dispatcher）。payroll.md §6.5/arm-index 部分表述（「hr 模块零 xbiz 文件」）基于 2026-08-07 审计快照，已因 M4.64 过时——随本计划 Phase 3 回填纠正。
- **授权状态**：2026-08-12 批量裁决 B 类降级预授权——「RC-R1.89（ER 经 billData JSON 持久化，设计注释确认）」**零 ORM 变更**（不加 socialInsuranceER/housingFundER 列，ER 经 PostingEvent billData JSON 载体）；**但派发侧改动属会计过账核心路径行为变更——按预授权声明须独立 plan-audit（本计划独立草案审查承担）+ 双独立子 agent 批准**（2026-08-15 裁决升级后规程，批准记录落盘本计划）。
- **GL 后果（Q4 会计正确性类关键证据）**：GL 永远仅收 280 → 费用+应付职工薪酬低估 + 资产负债表失衡，直至试算平衡人工发现；员工实发工资正确（ER 不影响个人 net）。

## Goals

- approve→APPROVED 联动生成 SALARY(270) + SOCIAL_INSURANCE_ER(290) + HOUSING_FUND_ER(300) 三类计提凭证（与既有 280 发放凭证构成完整薪酬过账链）。
- ER（公司承担社保/公积金）金额经 billData JSON 载体进入凭证（`BILL_DATA_SOCIAL_INSURANCE_ER`/`BILL_DATA_HOUSING_FUND_ER` 死常量激活），零 ORM 变更。
- 激活 `posted` writer（payroll.md §6.1 Deferred 子任务 4 收口）。
- markPaid→280 既有路径零回归；过账失败 G3 告警链覆盖新三条计提路径。

## Non-Goals

- reverseApprove/驳回后的计提红冲对称性（approve 后驳回的凭证回退语义）——独立控制点，若实现中证实缺失登记 successor + owner doc 注记，不在本计划强改。
- 多级审批链中途态（SUBMITTED）过账（L1 契约 = APPROVED 终态触发，无中途过账义务）。
- 薪酬模拟（What-If）侧联动——模拟域不触 GL，无过账义务。
- ER 金额 ORM 列持久化（B 类裁决已定 billData JSON 载体，否决 ORM 列）。

## Task Route

- Type: `implementation-only change`（会计过账核心路径——独立 plan-audit + 双独立子 agent 批准门控）
- Owner Docs: `docs/design/human-resource/payroll.md`（§6.1 posted 注记 + §6.5 Deferred 表 + §9.1 凭证表 + §9.5 科目映射）+ `docs/design/finance/posting.md`（跨域过账边界）+ `docs/design/human-resource/use-cases.md`（UC-HR-04 ⑯ L1）
- Skill Selection Basis: 派发侧接线 = per-mutation Processor + BizMutation 范式 → `nop-backend-dev`；凭证链测试（GraphQL 引擎 + 凭证断言）→ `nop-testing`；不改 view.xml 不加载前端 skill。

## Infrastructure And Config Prereqs

- 无新基础设施/外部服务。
- config：计提链是否 config 门控经 Phase 1 D3 裁决（候选：不门控——计提为 L1 硬契约对齐 R1.58 D4「验收标准硬契约不 config 化」先例 vs 门控默认 true）；零新 ErrorCode 预期（复用既有 payroll 科目未配置错误码）。
- 凭证科目：270/290/300 科目映射经 createFacts 既有默认 + config 覆盖机制（`resolvePayrollCredit`/默认科目常量已存在），无新科目 seed 预期。

## Execution Plan

### Phase 1 - 设计裁决 + 双独立子 agent 批准

Status: completed
Targets: 本计划（决策与批准记录）
Skill: none

- Item Types: `Decision | Proof`
- Prereqs: 无

- [x] Decision: **D1 裁决（2026-08-20）**：approve 过账时**重算** ER——复用 `SocialInsuranceCalculator.calculate(employeeId,year,month)[1]` / `calculateHousingFund(...)[1]`，`setScale(ErpHrConfigs.salaryRoundingScale(), HALF_UP)` 镜像 `PayrollCalculator:108-115` 口径；写入 290/300 event billData：`BILL_DATA_GROSS_AMOUNT` 承载 ER 金额（createFacts:65-68 消费键）+ 专用键 `BILL_DATA_SOCIAL_INSURANCE_ER`/`BILL_DATA_HOUSING_FUND_ER`（激活死常量，同值可追溯；失败路径经 `ErpFinPostingExceptionRecorder.serializeEventData` 持久化进 `ErpFinPostingException.eventData` JSON——成功路径 billData 为 transient，ER 金额经凭证行金额持久化）。**单薪酬单汇总凭证**（对齐 R1.52 #CATCHUP 先例）。否决替代：a) ORM ER 列（B 类裁决排除）；b) calculateSalary remark 暂存（A4.2.23 证实注释与实现矛盾，不可靠）；c) 逐员工凭证（否决，汇总+billData 承载明细）。**残留风险显式登记**：calculate 与 approve 之间社保/公积金 config 变更使重算值偏离原核算口径（billData 载体无计算时点快照的固有代价）——owner doc 注记 + 运营约定 approve 前不改基数据；一致性经测试⑤断言（同基数据重算 == calculateSalary 局部值）。**评审注入实现约束（Reviewer B 阻断性发现）**：`buildEvent:167-168` 硬编码 GROSS= grossSalary / NET=netSalary——290/300 事件必须**独立构建 billData**（GROSS_AMOUNT=ER 值且**不带 NET_AMOUNT 键**），否则 ER=0 时 createFacts 回退读 NET_AMOUNT 静默错记净额；且 280 的 GROSS/NET 键语义不得改动
      - Skill: none
- [x] Decision: **D2 裁决（2026-08-20）**：选 a) xbiz `approve` source 状态写回后委托 Java 编排 Bean `ErpHrSalaryPostApprovalProcessor.postAccruals(entity, svcCtx)`（approve mutation 尾部一行 inject 委托，270→290→300 顺序编排 + posted writer 全在 Java 侧；对齐 M4.64 xbiz 机制注记 + Guard/StateMachine Bean 先例）。否决：b) XScript 内联过账（`TryStatement` 不被 XLang 编译支持，xbiz :12-17 明示）；c) `.xwf` listener 回调（时点耦合 wf 回调链，Debug/直批路径绕过风险）。**wf 回调时点实仓确认**：`TestErpHrSalaryWorkflowApproval` 证实 wf 结束经 .xwf listener 回调**同一 approve action**（覆写 source 单一接线点同时覆盖直批与 wf 路径），无需 wf 侧二次接线
      - Skill: none
- [x] Decision: **D3 裁决（2026-08-20)**：选 a) 三条计提路径全部成功才 `posted=true`（任一失败保持 false + 告警；280 发放不受影响；posted=「计提链完整」语义）；否决 b) 首条成功即 true（掩盖部分悬挂）。**去重守卫（必选）**：dispatcher 侧 per-businessType 已过账凭证存在性反查——`(billCode, businessType)` 查 `ErpFinVoucherBillR` → 关联 `ErpFinVoucher.docStatus==POSTED && isReversed!=true`（镜像引擎 `ErpFinPostingProcessor.alreadyPosted:488-500` + R1.63 D3 `#RETURN` 幂等反查先例）；已存在则跳过该条且**计为成功**（非失败）。守卫必要性（双评审一致确认）：引擎级 alreadyPosted 幂等命中返回 null → `tryPost*` 的 `voucherId != null` 会误判失败 + posted 永无法收敛——dispatcher 守卫使 dedup-hit 语义显式化为成功，reverseApprove→再 approve 补投失败条目可收敛 posted=true。守卫仅作用于 270/290/300（businessType 区隔，280 永不受影响）。G3 吞异常 + `hr.salary-posting-failure` 告警 stage 扩展（计提/社保/公积金）保持发放链范式
      - Skill: none
- [x] Proof: **双独立子 agent 批准（2026-08-20，会计过账核心路径门控）**——两个 fresh session 独立复核四点：①createFacts 消费侧只读不动 ②approve 后置触发不破坏审批轴状态机 ③280 零变更论证 ④GL 试算平衡收敛（补凭证非改既有）。**双 APPROVE**：
  - Reviewer A（session `ses_fe2d203efffexfEexOd821kVsC`）：VERDICT APPROVE——四点全部 CONFIRMED（file:line 引证：SalaryPostingProvider:65-68/91-102/113-115、ErpHrSalary.xbiz:51-71、ErpHrSalaryApprovalStateMachine:62-104、ErpHrSalaryMarkPaidProcessor:40、ErpFinPostingProcessor:139-143/488-500/758-778）。残留风险可接受：REQUIRES_NEW 凭证先于外层事务提交的孤儿窗口（与既有 markPaid 同暴露类，dedup 守卫收敛）+ D1 重算漂移（已登记）
  - Reviewer B（session `ses_fe2d1a09fffedLuTjgzRvm1H63`）：VERDICT APPROVE——四点全部 ✓（另证引擎幂等返回 null 与 dispatcher 守卫的语义互补性 + reverseApprove 不红冲计提凭证确认为 Deferred successor）。残留风险可接受：**零 ER 回退陷阱**（实现约束已注入 D1：290/300 事件独立构建 billData 不带 NET_AMOUNT，测试覆盖）、D1 漂移、290/300 贷方科目名与 config 通用键的装饰性错配（config 覆盖路径既存）、凭证先提交孤儿窗口（同 markPaid 类）
      - Skill: none

Exit Criteria:

- [x] D1-D3 裁决连同否决替代方案落盘；双独立子 agent 批准记录落盘（未获双 APPROVE 前不得进入 Phase 2）

### Phase 2 - 派发侧接线实现

Status: completed
Targets: `module-hr/erp-hr-service/src/main/java/app/erp/hr/service/`（posting/ + processor/ 编排 Bean）+ `module-hr/erp-hr-service/src/main/resources/_vfs/erp/hr/model/ErpHrSalary/ErpHrSalary.xbiz`（approve source 委托接线）
Skill: nop-backend-dev

- Item Types: `Add`
- Prereqs: Phase 1 双批准闭合

- [x] Add: `SalaryPostingDispatcher` 增 `tryPostSocialInsuranceER` / `tryPostHousingFundER`（组装 290/300 PostingEvent：ER 金额重算入 billData `BILL_DATA_*_ER` 键 + 既有 billData 键复用 + buildAccrualEvent 范式扩展）+ `tryPostAccrual` 调用方激活（D2 编排 Bean 内 270→290→300 顺序，失败隔离 try/catch + 告警 stage 计提/社保/公积金）+ per-businessType 已过账凭证存在性去重守卫（D3）。**落地注记**：290/300 事件经独立 `buildCompanyBorneEvent` 构建 billData（GROSS_AMOUNT=ER 值 + 专用 ER 键，**不带 NET_AMOUNT**——Reviewer B 阻断性发现，防 ER=0 回退读净额）；280 的 buildEvent/buildPaymentEvent 零改动
      - Skill: nop-backend-dev
- [x] Add: 过账编排 Java Bean `ErpHrSalaryPostApprovalProcessor`（approve 后置三连过账，非 short-circuit `&` 聚合保证三路各自失败隔离 + posted writer 按 D3 语义 + posted=true 幂等防御跳过）+ `ErpHrSalary.xbiz` approve mutation 状态写回后委托调用（xbiz 仅一行 inject 调用，逻辑全在 Java 侧）+ `app-service.beans.xml` 注册
      - Skill: nop-backend-dev

Exit Criteria:

- [x] approve→APPROVED 后 GL 收到 270/290/300 三凭证（businessType 断言）；billData 含 ER 键；reverseApprove→再 approve 不产生重复凭证（去重守卫断言）——断言由 Phase 3 `TestErpHrSalaryPostingChain` 承载（本 Phase 实现落地 + 编译验证；Phase 3 Exit Criteria 全绿闭合本项）
- [x] erp-hr-service 类型检查通过（`mvn compile -pl module-hr/erp-hr-service -am -DskipTests` → BUILD SUCCESS，2026-08-20 实跑）

### Phase 3 - 测试与 owner doc 收敛

Status: completed
Targets: `module-hr/erp-hr-service/src/test/java/` + `docs/design/human-resource/payroll.md` + `docs/audits/arm-index.md`
Skill: nop-testing

- Item Types: `Add | Proof`
- Prereqs: Phase 2 落地

- [x] Add: `TestErpHrSalaryPostingChain`——①approve→270/290/300 三凭证生成 + businessType/科目/金额断言（含 ER 重算值）+ 三凭证 Dr==Cr 试算平衡收敛断言（Q4 关键证据运行时闭合）②billData ER 键持久化断言（失败路径经 ErpFinPostingException.eventData JSON——成功路径 billData 为 transient，ER 经凭证行金额持久化）③计提链部分失败→posted=false + G3 告警派发（种子 ACTIVE 通知模板断言 ErpSysNotification 落库；MERGE_BY_USER_TYPE 合并窗口内单条化）+ 280 后续 markPaid 不受阻断④markPaid→280 零回归⑤重算口径一致性（同基数据 SocialInsuranceCalculator 重算 == 硬算值 == 凭证金额）⑥REJECTED/UNSUBMITTED 零过账⑦幂等（重复 approve 守卫拒 + 计数不变）⑧GraphQL 冒烟（submit/approve/reject/reverseApprove 经 GraphQL 引擎走 xbiz 覆写 source 全链）⑨reverseApprove→再 approve 去重（无重复凭证 + 补投失败条目可续投 posted 收敛 true）——5 测试方法全绿
      - Skill: nop-testing
- [x] Add: 既有负向断言测试改造——`TestErpHrPayrollEngine` G3 负向文档化测试翻转为正向计提链断言（改名 `testCompanyBornePostingAccrualChainPositive`，+科目/期间/账套种子，270/290/300/280 四凭证计数 + posted=true）；快照同步——3 个 DB 状态合法漂移方法（testApprovalStateMachineAndPaidLock / testGenerateBankFileTransfersSalariesToPaid / TestErpHrPayrollSimulation.testConvertToFormalPaidConflict——approve 计提失败异常行新增 + 全局序列位移）经 `@EnableSnapshot(saveOutput=true)` 单方法重录后移除注解；旧负向 case 目录已删
      - Skill: nop-testing
- [x] Proof: owner doc 回填——payroll.md §6.1 posted 注记（Deferred→已实现：writer 语义/去重守卫/组织账套本位币解析补齐）+ §6.5 Deferred 表三行收敛为已实现（含 D1-D3 裁决、D1 config 变更残留风险与去重守卫语义、280 净额基数纠正、计提红冲 successor 登记）+ §9.1 凭证表四行收敛 + **纠正 §6.5/arm-index「hr 模块零 xbiz 文件」过时表述**（M4.64 后 ErpHrSalary.xbiz 已存在的历史表述纠正注记）；arm-index P1-MA4-017 → done (RC-R1.89)（修复落地注记 + 双批准 session id + 测试证据）
      - Skill: none

Exit Criteria:

- [x] `mvn test -pl module-hr/erp-hr-service` 全绿（2026-08-20 实跑：**237 tests, Failures: 0, Errors: 0**——既有基线零回归[含 G3 翻转 + 3 快照重录] + 新增 5 方法）
- [x] payroll.md/arm-index 回填完成（含过时表述纠正）

## Draft Review Record

- Independent draft review iteration 1: needs revision (ses_fe418891dffebwBH6JHF1AFQbl) because 两项 BLOCKER——基线伪事实「hr 模块零 xbiz」（实仓 ErpHrSalary.xbiz 已存在且覆写 approve/reverseApprove，M4.64 2026-08-14 落地晚于审计快照）致 D2 替代方案分析建立在错误前提 + reverseApprove→再 approve 重复计提风险未裁决（Q4 会计正确性同类伤害）；另 4 项 MINOR（TestErpHrPayrollEngine 负向断言将翻转未盘点、D1 config 变更残留风险未记、试算平衡仅论证未断言、行号引用偏差）
- Independent draft review iteration 2: accept (ses_fe40f9b89ffeIBaXvZhMq65hIY)——iteration-1 七项问题（2 BLOCKER + 4 MINOR + Deferred 一致性复核）全部修复并逐项实仓验证（xbiz 行号/机制注记/负向断言块/posted 列/去重范式均在位）；2 项 MINOR（「M4.66」笔误应为 M4.64、负向断言块行距 :408-447）已随手修正。共识达成，Plan Status → active。

## Closure Gates

> 完整仓库验证一次：`mvn clean install -DskipTests` + `mvn test`（hr 聚焦 + 全仓；对齐近期 R1.x 先例记录全仓计数）+ compliance checker（actual ≤ baseline，新增 Processor 站点若触发 baseline-raise 须 per-site 证据落 `docs/audits/compliance-baseline.md`）。

- [x] 范围内行为完成（approve 联动 270/290/300 + ER billData + posted writer）——含执行期发现补齐：组织/账套/本位币解析（AcctSchemaResolver 范式）+ 280 净额基数纠正（GROSS_AMOUNT 消费键死参数接线）
- [x] 相关文档对齐（payroll.md §6.1/§6.5/§9.1 Deferred 三处收敛 + arm-index P1-MA4-017 → done (RC-R1.89)，含「hr 模块零 xbiz」过时表述纠正）
- [x] 已运行验证（分域 test + 全仓 install/test + checker）——2026-08-20 实跑：`mvn clean install -DskipTests` 全仓 BUILD SUCCESS（156 reactor 模块）+ `mvn test -pl :app-erp-hr-service` **237 tests 全绿** + 全仓 `mvn test` BUILD SUCCESS（surefire XML 权威计数 **3789 tests, 0 failures, 0 errors, 1 skipped**，614 文件） + checker 复跑 actual == baseline（R2c 1507/R2d 38/R12c 41 三站点裁决性上调，per-site 证据落 `docs/audits/compliance-baseline.md §R2c/R2d/R12c 基线上调注记（plan 2026-08-20-0518-3）`，镜像引擎 alreadyPosted + assets AcctSchemaResolver 既判范式）
- [x] 无范围内项目降级为 deferred/follow-up（计提红冲对称性为计划前置登记的 watch-only residual，非范围内降级）
- [x] 双独立子 agent 批准记录（Phase 1）已落盘（ses_fe2d203efffexfEexOd821kVsC + ses_fe2d1a09fffedLuTjgzRvm1H63，双 APPROVE）
- [x] 独立草案审查已完成并记录（Draft Review Record iteration 1 needs revision / iteration 2 accept）
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计（见 Closure Audit Evidence）
- [x] 结束证据存在于文件中
- [x] `docs/logs/2026/08-20.md` 日志条目

## Deferred But Adjudicated

### 计提红冲对称性（approve 后驳回/反审核的 270/290/300 凭证红冲）

- Classification: `watch-only residual`
- Why Not Blocking Closure: L1 UC-HR-04 ⑯ 断言为计提+公司承担过账触发，未含驳回红冲；reverseApprove（ErpHrSalary.xbiz:92-112，APPROVED→SUBMITTED）存在但本计划仅落去重守卫防重复计提（D3），凭证级红冲（反向冲回已过账 270/290/300）为独立控制点
- Successor Required: yes（触发条件：APPROVED 后反审/驳回运营场景确认时，对齐 P1-MA2-083 冲销恢复 + postingDispatcher.reverse 范式立项）

## Closure

Status Note: 执行完毕（2026-08-20）。三 Phase 全绿闭合：Phase 1 双独立子 agent 批准（双 APPROVE）→ Phase 2 派发侧接线（含执行期发现补齐：组织/账套/本位币解析 + 280 净额基数纠正）→ Phase 3 测试与 owner doc 收敛。验证：erp-hr-service 237/0/0（232 基线 + 5 新增零回归）+ 全仓 `mvn clean install -DskipTests` 156 模块 BUILD SUCCESS + 全仓 `mvn test` surefire XML 权威计数 **3789/0/0/1**（614 文件，前基线 3784+5 逐字吻合；唯一 skip = 已知 @Disabled ErpAllWebPagesCollectTest）+ checker actual==baseline（R2c 1505→1507 / R2d 37→38 / R12c 40→41 裁决性上调，per-site 证据落 compliance-baseline.md）。Deferred But Adjudicated 维持：计提红冲对称性 successor。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话，执行者未自我审计）
- Iteration 1（task `ses_fe2962c39ffeMPWYrou1O5jPhr`，2026-08-20）：VERDICT FAIL——实现/测试/文档/残留登记 12 项 claim 全部实仓核实 PASS（file:line 引证齐），但 2 项闭包一致性缺口：①日志条目未写（违反 AGENTS.md 操作规则 8）②Closure 本节仍为占位（结束证据不存在于文件中）+ 1 项 MINOR（compliance-baseline.md 人类可读表滞后于机器可读块）
- Remediation: 补写 `docs/logs/2026/08-20.md` 0518-3 条目（含全量验证计数）+ 填充本节 + compliance-baseline.md 人类可读表六行同步（R2b/R2c/R2d/R10/R12a/R12c → 237/1507/38/12/70/41，同步注记落盘）
- Iteration 2（task `ses_fe28fe102ffeyD3QQ9B3EwwM7F`，2026-08-20）：**VERDICT PASS**——三项 remediation 全部实仓核实（日志条目 :7 / Closure 节填充 :145-158 / 基线表六行同步+注记 :37）；Closure Gates 10/10 [x] 核验。2 项 MINOR 非阻断观察随手收口：①gate 文本 7608 修正为 surefire XML 权威计数 3789（console 聚合口径重复计数的报告差异）②日志重复日期头去重

Follow-up:

- 计提红冲对称性 successor（见 Deferred But Adjudicated）

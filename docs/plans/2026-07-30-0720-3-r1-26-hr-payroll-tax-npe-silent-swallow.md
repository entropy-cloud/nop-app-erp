# 2026-07-30-0720-3-r1-26-hr-payroll-tax-npe-silent-swallow hr 个税高档税率 NPE 修复 + 累计数据静默吞修复（计提+公司承担过账链路 Deferred）

> Plan Status: completed
> Last Reviewed: 2026-07-30
> Source: audit-remediation-roadmap R1.26（P1-MA4-016 + P1-MA4-018 + P1-MA4-017，源自 A4.4 hr 代码质量审计）
> Related: `docs/audits/2026-07-29-0430-arm-ma4-hr-code-quality.md`、`docs/audits/arm-index.md §P1-MA4-016/017/018`；plan `2026-07-30-0631-1-r1-21-projects-close-start-precondition-dict-deferred.md`（选择性裁决先例：便宜真实缺陷实现 + 重型功能 Deferred）、plan `2026-07-30-0341-2-r1-16-posting-error-propagation-grading-strategy.md`（业财过账链路裁决先例）；`docs/design/human-resource/payroll.md §6/§9.1`（计提+公司承担过账契约）
> Audit: required

## Current Baseline

三项 finding 经实仓逐项确认：均为「个税/累计计算缺陷 + 业财过账链路不完整」类型。**016/018 是直接影响实发工资正确性的薪酬算术缺陷**（响亮崩溃 / 静默少预扣）；**017 是业财过账链路功能缺口**（GL 永远仅收发放凭证）。三者均**不破坏已实现主路径**（薪酬计算 SocialInsuranceCalculator clamp + 累计预扣法编排 + BigDecimal 货币类型安全 + 模拟 What-If 隔离 + 跨域 Facade REQUIRES_NEW + SALARY_PAYMENT 发放凭证正常生成 + 审批五动作齐全）。

**P1-MA4-016（个税高档税率 NPE）— 确认：**
- `IncomeTaxCalculator.resolveBracket:204-214` 遍历七级累进税率表「跳过 income > rangeUpperLimit 的档位，选第一档 income ≤ rangeUpperLimit」。末档（>960000，45%）`rangeUpperLimit=null`（seed `erp_hr_tax_config` + TaxBracketParser 注释声明末档 null 表「无上限」）。
- 当累计应纳税所得额 > 960000（月薪 > ~80000 或高额奖金月的高收入员工），前 6 档全跳过，触达末档 `cumulativeTaxableIncome.compareTo(b.getRangeUpperLimit())` = `compareTo(null)` → **NullPointerException**。
- calculate() 抛 NPE → calculateSalary/runPayroll 抛 NPE → @BizMutation 事务回滚。runPayroll 整批循环无 per-employee 隔离，**任一高收入员工致整批回滚（全员工当月无薪酬）**；calculateSalary 单员工直接失败。
- 非静默错算：NPE 立即抛出无数据持久化，不会静默多缴/少缴（calculate 失败则不发非发错金额）。

**P1-MA4-018（parseCumulativeData 静默吞异常）— 确认：**
- `IncomeTaxCalculator.parseCumulativeData:158-176` 解析 cumulativeData JSON 时 `catch(Exception ignored){ return new HashMap(); }`（:173-174）**静默吞**所有解析异常返回空 map。
- 当 cumulativeData 损坏（JSON 格式错误/字段类型不符/手工编辑出错），findPreviousCumulative:140-153 返回空 → 当月按「无累计历史」计算（cumGross 仅当月、cumThreshold 仅当月 5000）→ 累计应纳税所得额远低于实际 → **累计税额低估 → 当月应纳个税低估 → 少预扣个税**（员工实发偏高 + 企业代扣代缴不足税务合规风险）。
- 同型静默吞扩散到测试 `TestErpHrPayrollEngine.extractCumulativeData:347-355`（测试也静默吞致缺陷对测试不可见）。触发需 cumulativeData 损坏前置；happy path 由同 calculator 写入合法 JSON 不触发。

**P1-MA4-017（业财过账链路不完整）— 确认：**
- `SalaryPostingDispatcher.tryPostAccrual:67-82`（计提 SALARY 270）**死代码零调用方**——grep 全 hr 模块 + xbiz + beans.xml 零引用，仅 tryPostPayment(SALARY_PAYMENT 280) 在 `ErpHrSalaryBizModel.markPaid` 触发。
- `SalaryPostingProvider.getSupportedBusinessTypes` 声明支持 SALARY/SALARY_PAYMENT/SOCIAL_INSURANCE_ER(290)/HOUSING_FUND_ER(300) 四类，但 290/300 event **永不生成**（无代码组装）。
- `PayrollCalculator.calculate:107-115` 计算 socialInsuranceER/housingFundER（公司承担）为局部变量，注释 :156-157 声称「暂存 remark」但**无 setRemark 调用**——公司承担金额计算后丢弃。
- **关键设计阻塞**：`ErpHrSalary` ORM **无 socialInsuranceER/housingFundER 列**（仅有 socialInsurance 个人部分/housingFund 个人部分 + posted 列）——方案A 实现 017 须先裁决公司承担金额持久化设计（ORM ask-first 加列 vs remark 暂存 vs 过账时重算），属设计决策非单点修复。
- owner doc `payroll.md §6 表 L435/L437` + `§9.1 L541-542` 声明 approveStatus→APPROVED 联动计提 SALARY(270)+SOCIAL_INSURANCE_ER(290)+HOUSING_FUND_ER(300)「由 approve action 的 xbiz source append 触发」——该 xbiz append 未落地（hr 模块零 xbiz 文件）。**注**：`payroll.md §posted 字段 L370` 已含 Deferred 注（P1-MA2-047，landing 于 R1.15）；`SalaryPostingDispatcher` javadoc :32-38/:56-66 亦已含「tryPostAccrual 死代码 / Successor=R1.26 / posted Deferred」标注（landing 于 R1.15/R1.16）——本计划 017 的**未落地工作仅剩 payroll.md §6 表 + §9.1 的计提/公司承担过账 Deferred 标注**（javadoc 侧无须重复更新，仅核对）。
- **parseCumulativeData 双调用注记**：`findPreviousCumulative:132-139` 含一个 dead-code 首循环（调 parseCumulativeData 但结果未用，P2-MA4-008），:140-153 才是真实累计基础读取——Phase 3 改 parseCumulativeData 为「损坏抛错」后，该 dead 循环会先于 :151 真实调用触发抛错（行为正确，因历史损坏即应响亮失败），但须在实现时留意双调用点。
- 后果：GL **永远仅收发放凭证 SALARY_PAYMENT(280)**，缺工资计提+公司承担社保+公司承担公积金 → 业财不一致（费用+应付职工薪酬低估+资产负债表失衡），直至期末试算平衡人工发现。员工实发工资正确（公司承担不影响个人 net）。

**保护区域：** 016/018 触及**薪酬保护区域**（直接影响实发工资）——但均为计算层缺陷修复（null 防御 + 移除静默吞），不触及会计凭证写路径，containment 友好，按 roadmap 规则走标准 plan-audit + closure-audit + 人工确认。017 触及**会计保护区域**（凭证写路径）+ 需 ORM 设计决策——裁决 Deferred（owner doc 正式化），successor 为独立计提+公司承担过账链路 plan。

## Goals

- 消除 hr 薪酬域两项直接影响实发工资正确性的算术缺陷：(1) **修复** resolveBracket 末档 null NPE（null 防御）；(2) **修复** parseCumulativeData 静默吞（移除 catch-all + LOG.warn + ErrorCode 可观测）。
- 计提+公司承担过账链路（017）对齐（owner doc Deferred 标注 + successor 命名触发条件）。
- owner doc 与代码一致；高收入员工薪酬不再整批崩溃；累计数据损坏不再静默少预扣个税。

## Non-Goals

- 不实现计提 SALARY(270) + 公司承担社保(290)/公积金(300) 过账链路（P1-MA4-017）——裁决 Deferred（owner doc 正式化，方案B）。**与 arm-index 推荐偏差声明**：arm-index §P1-MA4-017 方案A（推荐）实现 approve→APPROVED 联动（接线 tryPostAccrual + 持久化 socialInsuranceER/housingFundER + 新增 tryPostSocialInsuranceER/tryPostHousingFundER）；本计划裁决 Deferred，理由：(1) **会计保护区域** + ErpHrSalary 无 socialInsuranceER/housingFundER 列——方案A 须先裁决公司承担金额持久化设计（ORM ask-first 加列 vs remark 暂存 vs 过账时重算），属设计决策非单点修复；(2) 多 event 过账链路（270+290+300）+ approve action 接线属 substantial slice，须独立 plan-audit + 人工确认；(3) 审计确认漏记凭证可经试算平衡发现 + 员工实发工资正确（公司承担不影响个人 net）；(4) tryPostAccrual/tryPostPayment 的过账悬挂告警闭环已由 R1.16（P1-MA2-048）落地，悬挂可观测。successor：独立计提+公司承担过账链路 plan（裁决 ER 列持久化设计 + 接线 tryPostAccrual/290/300 + approve action）。
- 不改 ErpHrSalary ORM（不加 socialInsuranceER/housingFundER 列——归 017 successor 的 ORM ask-first 设计决策）。
- 不补 017 的测试有效性（P1-MA4-019 归 MR2 R2.13——个税高档边界/过账悬挂/累计损坏/公司承担过账缺失测试，与 016/018 修复协同但归属 MR2）。
- 不重构累计预扣法算法（仅 null 防御 + 移除静默吞）。

## Task Route

- Type: `app-layer design change`（owner doc 行为契约对齐）+ `implementation-only change`（016/018 计算层缺陷修复 + ErrorCode）
- Owner Docs: `docs/design/human-resource/payroll.md`
- Skill Selection Basis: P1-MA4-016/018 涉及计算器方法缺陷修复 + ErrorCode + 薪酬保护区域 → `Skill: nop-backend-dev`（含 nop-debugging 缺陷定位）；P1-MA4-017 owner doc Deferred 标注为纯文档 → 该部分 `Skill: none`。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline.

## Execution Plan

### Phase 1 - 三项 finding 裁决（Decision）

Status: completed
Targets: 本计划（裁决记录）
Skill: `none`

- Item Types: `Decision`
- Prereqs: none

- [x] **Decision**：三项 finding 处置方案逐项裁决（**选择性裁决**——016/018 实现薪酬算术缺陷修复[直接影响实发工资，containment 友好] + 017 Deferred[会计保护区域 + ORM 设计决策 + substantial slice]）。
      - P1-MA4-016 个税高档 NPE：**实现（arm-index 推荐方向）**。理由：(1) NPE 致 runPayroll 整批回滚（全员工当月无薪酬）——直接影响实发工资；(2) containment 极友好（resolveBracket 末档 null 防御单点：compareTo 前判 `if(b.getRangeUpperLimit()==null){ selected=b; break; }`）；(3) 响亮崩溃修复，无数据迁移。
      - P1-MA4-018 parseCumulativeData 静默吞：**实现（arm-index 推荐方向）**。理由：(1) 静默吞致累计损坏时少预扣个税（员工实发偏高 + 税务合规风险）；(2) containment 友好（移除 catch-all + LOG.warn + 抛 ErrorCode ERR_HR_CUMULATIVE_DATA_CORRUPT，使损坏可观测）；(3) 同步修复测试 extractCumulativeData 静默吞。
      - P1-MA4-017 计提+公司承担过账链路：**Deferred（owner doc 正式化，方案B）**。**与 arm-index 推荐偏差声明**：arm-index §P1-MA4-017 方案A（推荐）实现过账链路；本计划裁决 Deferred，理由：(1) 会计保护区域 + ErpHrSalary 无 ER 列——方案A 须先裁决持久化设计（ORM ask-first 加列 vs remark 暂存 vs 过账时重算），属设计决策；(2) 多 event 过账链路 + approve 接线属 substantial slice 须独立 plan-audit + 人工确认；(3) 审计确认漏记可经试算平衡发现 + 员工实发工资正确；(4) 过账悬挂告警闭环已由 R1.16 落地。successor：独立计提+公司承担过账链路 plan。
      - Skill: `none`

Exit Criteria:

- [x] Phase 1 Decision 逐项记录选择 + 理由 + 与 arm-index 推荐偏差声明 + successor 触发条件；016/018 进 Phase 2/3（实现），017 进 Phase 4（Deferred 标注）。

### Phase 2 - 个税高档 NPE 修复（P1-MA4-016）

Status: completed
Targets: `module-hr/erp-hr-service/.../payroll/IncomeTaxCalculator.java`、`docs/design/human-resource/payroll.md`
Skill: `nop-backend-dev`

- Item Types: `Fix`
- Prereqs: Phase 1

- [x] **Fix（resolveBracket null 防御）**：`IncomeTaxCalculator.resolveBracket:204-214` 在 `compareTo` 前判末档无上限：循环中 `if (b.getRangeUpperLimit() == null) { selected = b; break; }`（末档表「无上限」，income 超过所有有限上限时直接选末档），消除 `compareTo(null)` NPE。补充 javadoc 注明末档 rangeUpperLimit=null 表「无上限」语义。
      - Skill: `nop-backend-dev`
- [x] **Proof**：测试——(1) 累计应纳税所得额 > 960000（如月薪 100000 员工多月累计）→ 命中末档 45% 正确计算（rate 0.45 − quickDeduction 181920），**非 NPE**；(2) 累计 ≤ 960000 命中前 6 档（行为不变）；(3) 边界值 = 960000（命中第 6 档上界）。seed tax_config 含末档 null rangeUpperLimit（对齐 TestErpHrPayrollEngine.seedTaxConfig:284）。
      - Skill: `nop-backend-dev`
- [x] **Add（owner doc）**：payroll.md §4.5 累计预扣法补注「resolveBracket 末档 rangeUpperLimit=null 表『无上限』，income 超过所有有限上限时选末档（null 防御，消除 compareTo(null) NPE）」。
      - Skill: `none`

Exit Criteria:

- [x] resolveBracket 末档 null 防御落地（grep 确认 `getRangeUpperLimit()==null` 分支）；>960000 累计不再 NPE；新增测试全绿（Closure Gates 跑全量 mvn）；owner doc §4.5 与代码一致。

### Phase 3 - parseCumulativeData 静默吞修复（P1-MA4-018）

Status: completed
Targets: `module-hr/erp-hr-service/.../payroll/IncomeTaxCalculator.java`、`ErpHrErrors.java`、`ErpHrConstants.java`、测试 `TestErpHrPayrollEngine.extractCumulativeData`、`docs/design/human-resource/payroll.md`
Skill: `nop-backend-dev`

- Item Types: `Fix | Add`
- Prereqs: Phase 1

- [x] **Add（ErrorCode）**：`ErpHrErrors` 增 `ERR_HR_CUMULATIVE_DATA_CORRUPT`（`erp.err.hr.cumulative-data-corrupt`，描述「员工 {employeeId} 年度 {year} 累计薪酬数据 JSON 解析失败，请核对 cumulativeData 完整性」，复用 ARG_EMPLOYEE_ID + ARG_YEAR）。
      - Skill: `nop-backend-dev`
- [x] **Fix（移除静默吞）**：`IncomeTaxCalculator.parseCumulativeData:158-176` 移除 `catch(Exception ignored){}`——解析失败时 LOG.warn（记录 employeeId/year + 原始 json 片段）并抛 `NopException(ERR_HR_CUMULATIVE_DATA_CORRUPT)`（使累计损坏可观测、可拦截，而非静默重置致少预扣个税）。注意：parseCumulativeData 在 findPreviousCumulative 内调用历史月薪酬——历史月损坏抛错将阻断当月计算（符合「宁可响亮失败不静默错算」原则）；null/空 json 仍返回空 map（1 月无历史合法路径不变）。
      - Skill: `nop-backend-dev`
- [x] **Fix（测试同步）**：`TestErpHrPayrollEngine.extractCumulativeData:347-355` 移除同型静默吞——解析失败显式抛或断言，使缺陷对测试可见（闭合 A4.4 §测试也静默吞致缺陷不可见）。
      - Skill: `nop-backend-dev`
- [x] **Proof**：测试——(1) seed 损坏 cumulativeData（非法 JSON）→ findPreviousCumulative/calculate 抛 `ERR_HR_CUMULATIVE_DATA_CORRUPT`（非静默重置；留意 :132-139 dead 循环先触发，行为正确）；(2) null/空 cumulativeData → 返回空 map（1 月无历史合法路径不变）；(3) 合法 JSON → 正常解析（行为不变）。
      - Skill: `nop-backend-dev`
- [x] **Add（owner doc）**：payroll.md §4.5 补注「parseCumulativeData 解析失败时 LOG.warn + 抛 ERR_HR_CUMULATIVE_DATA_CORRUPT（累计损坏响亮失败，不静默重置致少预扣个税）；null/空 json 返回空 map（1 月无历史合法）」。
      - Skill: `none`

Exit Criteria:

- [x] parseCumulativeData 静默吞移除（grep 确认无 `catch.*ignored` + ERR_HR_CUMULATIVE_DATA_CORRUPT 抛出）；损坏 JSON 抛错；null/空合法路径不变；测试同步修复；新增测试全绿；owner doc §4.5 与代码一致。

### Phase 4 - 计提+公司承担过账链路 owner doc Deferred 标注（P1-MA4-017）

Status: completed
Targets: `docs/design/human-resource/payroll.md`
Skill: `none`

- Item Types: `Add`
- Prereqs: Phase 1

- [x] payroll.md §6 表 L435/L437 + §9.1 L541-542 正式化为「**Deferred**——当前 GL 仅收发放凭证 SALARY_PAYMENT(280)（markPaid 触发 tryPostPayment）；计提 SALARY(270) + 公司承担社保 SOCIAL_INSURANCE_ER(290) + 公积金 HOUSING_FUND_ER(300) 过账链路（approve→APPROVED 联动 tryPostAccrual + 持久化 socialInsuranceER/housingFundER + 新增 tryPostSocialInsuranceER/tryPostHousingFundER）留 successor；tryPostAccrual 当前为零调用方死代码，posted 字段 Deferred（无 setPosted writer）」；命名 successor 触发条件 + 持久化设计决策点（ORM ask-first 加列 vs remark 暂存 vs 过账时重算）。
      - Skill: `none`
- [x] payroll.md §残留风险补注「GL 永远仅收发放凭证 → 费用+应付职工薪酬低估+资产负债表失衡，直至期末试算平衡人工发现；员工实发工资正确（公司承担不影响个人 net）」。
      - Skill: `none`
- [x] **核对（非更新）**：SalaryPostingDispatcher javadoc :32-38/:56-66 + payroll.md §posted 字段 L370 已含 R1.26/P1-MA4-017 successor 标注（landing 于 R1.15/R1.16）——本项仅核对一致性，无须重复编辑（javadoc 侧已就绪）。
      - Skill: `none`

Exit Criteria:

- [x] payroll.md §6 表 + §9.1 明确 017 Deferred（计提+公司承担过账链路），owner doc 与代码（tryPostAccrual 死代码 + 290/300 永不生成 + posted 无 writer）一致；successor 触发事件 + 持久化设计决策点已命名；javadoc 侧标注核对一致（无须重复编辑）。

## Draft Review Record

- Independent draft review iteration 1: acceptable as-is (ses_04fcfb11effeMObEh2v6lPdRGH, fresh session) because 全部基线声明经实仓 file:line 验证 TRUE（resolveBracket:207 compareTo(b.getRangeUpperLimit()) 无 null 守卫→末档 null NPE / parseCumulativeData:173-174 catch(Exception ignored){} 静默吞（null/空 json :160-162 早返回空 map 合法）/ tryPostAccrual grep 全 hr 仅 4 匹配全在 SalaryPostingDispatcher 自身 javadoc+声明，零外部调用方 / PayrollCalculator:110/:115 socialInsuranceER/housingFundER 局部变量 :156-157 注释称暂存 remark 但无 setRemark 调用→丢弃 / ErpHrSalary ORM 仅 socialInsurance(EE):736 + housingFund(EE):737 + posted:764，无 ER 列→017 方案A 须 ORM ask-first 设计决策 / payroll.md §6 L435/L437 + §9.1 L541-542 声明 270/290/300 由 approve append 触发）；016/018 containment 友好计算层修复（不触及凭证写路径）适合 plan-audit + closure-audit 不无限阻塞；017 Deferred 合规则 13——「moved to explicit successor ownership」+ arm-index 列举的方案B（非发明）+ 显式偏离声明 + 命名 successor+触发条件+ORM 设计决策点 + P1-非-P0 可经试算平衡发现；拆分（实现 016/018 缓 017）可辩护——017 确需 ORM ask-first 设计决策 + 触及会计保护区域，须独立 plan-audit。采纳非阻塞修订：(1) Current Baseline 补注 SalaryPostingDispatcher javadoc :32-38/:56-66 + payroll.md §posted L370 已含 R1.26 successor 标注（landing 于 R1.15/R1.16）→ Phase 4 未落地工作仅剩 payroll.md §6 表+§9.1 Deferred 标注，javadoc 项改为「核对一致」；(2) 补 parseCumulativeData 双调用注记（findPreviousCumulative:132-139 dead 循环先于 :151 真实调用触发抛错，行为正确）。

## Closure Gates

> 本计划含代码变更（P1-MA4-016/018，触及薪酬保护区域），故 Closure Gates 含全量 `mvn` 验证（见执行时规则 7）。

- [x] 范围内行为/文档完成（016 NPE 修复 + 018 静默吞修复 + 017 Deferred 标注）
- [x] 相关文档对齐（payroll.md）
- [x] 已运行验证（`mvn clean install -DskipTests` 全绿 + hr 域 `mvn test` 全绿 + compliance checker 本计划零新增命中；grep 验证 016/018 落地）
- [x] 无范围内项目降级为 deferred/follow-up（016/018 为范围内存活实现项；017 Deferred 是处置裁决 + 已命名 successor + 持久化设计决策点，非范围内缺陷隐瞒）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 计提 SALARY(270) + 公司承担社保(290)/公积金(300) 过账链路（P1-MA4-017 successor）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 会计保护区域 + ErpHrSalary 无 socialInsuranceER/housingFundER 列——方案A 须先裁决持久化设计（ORM ask-first 加列 vs remark 暂存 vs 过账时重算）；多 event 过账链路 + approve 接线属 substantial slice 须独立 plan-audit + 人工确认；审计确认漏记可经试算平衡发现 + 员工实发工资正确（公司承担不影响个人 net）；过账悬挂告警闭环已由 R1.16 落地（posted=false 悬挂可观测）。
- Successor Required: `yes`（独立计提+公司承担过账链路 plan：(1) 裁决公司承担金额持久化设计——ORM ask-first 加 socialInsuranceER/housingFundER 列 or remark 暂存 or 过账时重算；(2) 接线 tryPostAccrual（approve→APPROVED 联动）+ 持久化公司承担金额；(3) 新增 tryPostSocialInsuranceER/tryPostHousingFundER 组装 290/300 event；(4) 激活 posted writer；协同 P1-MA4-019[MR2 R2.13] 测试有效性）

## Closure

Status Note: 全 4 Phase 落地完成，独立结束审计 PASS（2026-07-30）。016/018 计算层缺陷修复（null 防御 + 移除静默吞）+ 017 业财过账链路 Deferred（owner doc 正式化，successor + 持久化设计决策点已命名）。121 hr 测试全绿，全 workspace `mvn clean install -DskipTests` BUILD SUCCESS，compliance checker 零新增命中。

Closure Audit Evidence:

- 独立子代理（新会话，task ses_04f913108ffelnXYLDQT82ijtK）closure audit = **Verdict: PASS**（A-F 六段逐项经实仓 file:line 验证）：
  - **A. 016 NPE 修复**：`IncomeTaxCalculator.java:231-234` null-guard `if (b.getRangeUpperLimit() == null) { selected = b; break; }` 在 `compareTo`（:235）之前，无遗留 null 接收路径。
  - **B. 018 静默吞修复**：`ErpHrErrors.java:84-87` ERR_HR_CUMULATIVE_DATA_CORRUPT（`erp.err.hr.cumulative-data-corrupt`，复用 ARG_EMPLOYEE_ID + ARG_YEAR）；`IncomeTaxCalculator.java:184-190` 移除 catch-all→LOG.warn + 抛 NopException；null/空白 json :171-173 返回空 map；`TestErpHrPayrollEngine.java:384-393` 移除静默吞。
  - **C. 测试**：`TestIncomeTaxCalculator.java`（>960000 命中 45% / 边界 960000 / 损坏抛错 / null 空 / 合法解析）+ `TestErpHrPayrollEngine.testCorruptCumulativeDataThrowsNotSilentReset:258-293`；审计者本人重跑 `mvn test -pl module-hr/erp-hr-service` = Tests run: 121, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS。
  - **D. 017 Deferred**：payroll.md §6.5:437 + §9.1:545-548 三过账类型 Deferred + successor + 持久化设计决策点；§4.5:258-260 实现注记；SalaryPostingDispatcher javadoc :32-38/:56-66 + §6.1 posted :374 核对一致。
  - **E. 保护区域**：`git status --short` 零 `*.orm.xml`（无 ErpHrSalary 加列）；SalaryPostingDispatcher（凭证写路径）未在 diff（016/018 纯计算层）；017 Deferred But Adjudicated 命名 successor + ORM 设计决策点。
  - **F. 一致性**：4 Phase 全 `Status: completed` + 全 checklist `- [x]`。

Follow-up:

- 非阻塞；successor 已在 Deferred But Adjudicated + payroll.md §6.5/§9.1 双重命名触发条件。

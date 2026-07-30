# 2026-07-31-0420-3-r2-13-hr-payroll-test-effectiveness R2.13 hr 薪酬/过账链路测试有效性（残差补强）

> Plan Status: active
> Last Reviewed: 2026-07-31
> Source: `docs/backlog/audit-remediation-roadmap.md` §MR2 R2.13（P1-MA4-019 残差）
> Related: `docs/audits/arm-index.md`（P1-MA4-019/016/017/018）、R1.26（个税 NPE + 静默吞修复 + 验证测试 + 公司承担过账 Deferred successor）
> Audit: required（独立草案审查 + 独立 closure audit）

## Current Baseline

P1-MA4-019（finding 写于 R1.26 之前）的子项 (a)(c) **已由 R1.26 落地的测试大量闭合**。独立草案审查（iteration 1）实测确认下列既有测试已覆盖原 finding 列为「零覆盖」的高档税率边界与累计损坏，本计划仅补**残差缺口**。逐项实测基线：

**已闭合子项（不在本计划范围，避免重复）**：
- 个税高档税率边界（单元级）：`TestIncomeTaxCalculator`（`payroll/`，115 行）4 方法——`resolveBracket_highIncomeAboveMaxLimit_hitsLastBracketNotNpe`（:43，income=1500000>960000→45%+算术 493080 验证）、`_withinMaxLimit`、`_boundaryAtMaxLimit`、`_justAboveMaxLimit`（:75）——单元级 resolveBracket 末档 null 已覆盖（闭合 P1-MA4-016 单元测试可见性）
- 累计 JSON 损坏（单元级 + 集成级）：`TestIncomeTaxCalculator` 3 方法（`parseCumulativeData_corruptJson_throwsErrorCodeNotSilentReset` :87 抛 ERR_HR_CUMULATIVE_DATA_CORRUPT + `_nullOrEmpty_returnsEmptyMap` + `_validJson_parsesNormally`）+ `TestErpHrPayrollEngine.testCorruptCumulativeDataThrowsNotSilentReset()`（:259，集成级 seed 损坏 cumulativeData→calculateSalary→断言 ERR_HR_CUMULATIVE_DATA_CORRUPT）——单元 + 集成双层已覆盖（闭合 P1-MA4-018 测试可见性）

**残差缺口（本计划范围）**：
- **G1（P1-MA4-019 (a) 残差）高档税率集成级 E2E**：`TestErpHrPayrollEngine`（394 行，`JunitAutoTestCase` + `HrFrozenClockExtension` + IGraphQLEngine）全部 7 测试方法员工月薪 ≤30000，永不触达末档。单元级 `TestIncomeTaxCalculator` 仅隔离调用 `resolveBracket`，不演练完整薪酬引擎流程（社保钳制 + 累计预扣跨月写回 + runPayroll 批处理）。残差 = 集成级高档员工全链路（seed 月薪 100000 + frozen clock 跨月累计 → calculateSalary/runPayroll → 断言末档 45% 正确 + 无 NPE + monthTax 为正）。
- **G2（P1-MA4-019 (b)）过账悬挂**：grep 全 hr 测试零 `posted=false`/mock-post 测试。`markPaid` 忽略 `tryPostPayment` 返回值致 posted=false 窗口（P1-MA2-048，R1.16 已修复告警闭环）无 mock post 抛异常→断言 posted=false 可观测 + 无发放凭证测试。真实缺口。
- **G3（P1-MA4-019 (d)+(e) 合并）公司承担过账负向文档化**：P1-MA4-017（公司承担社保 290/公积金 300 + 计提 SALARY 270 过账链路）Deferred successor 未实现（R1.26 Deferred，须 ORM ask-first + HR/会计保护区域人工确认）。approve 薪酬后断言计提 270 + 290 + 300 三类凭证**当前未生成**（文档化 Deferred 缺口）+ 发放 SALARY_PAYMENT 凭证正常生成（正向基线）。

剩余差距：G1（集成级高档）+ G2（过账悬挂）+ G3（公司承担负向文档化）。本计划为**纯测试新增**（无生产 Java/ORM 变更），不触及薪酬保护区域运行时行为——仅补测试使行为可观测，并对未实现的 017 缺口做负向文档化。

## Goals

- G1：高档税率集成级 E2E——月薪 100000 员工 + HrFrozenClockExtension 跨月累计使累计应纳税所得额 >960000，runPayroll/calculateSalary 后断言末档 45% 正确计算 + 无 NPE + monthTax 为正（演练完整引擎流程，补单元测试不覆盖的集成路径）
- G2：过账悬挂测试——mock post 抛异常触发 tryPostPayment posted=false 窗口，断言 posted=false 可观测 + 无发放凭证（闭合 P1-MA2-048 测试可见性，依赖 R1.16 告警闭环）
- G3：公司承担过账负向文档化测试——approve 薪酬后断言计提 270 + 290 + 300 凭证**当前未生成**（文档化 P1-MA4-017 Deferred 缺口，负向断言 + 注释引用 017 successor）+ SALARY_PAYMENT 发放凭证正常生成

## Non-Goals

- 不重复实现已闭合子项（高档税率单元级 / 累计损坏单元+集成级——见 Current Baseline 既有测试清单）
- 不修改任何生产 Java 代码（PayrollCalculator/IncomeTaxCalculator/SalaryPostingDispatcher/BizModel）
- 不实现 P1-MA4-017 公司承担过账链路——属 Deferred successor，须 ORM ask-first + 保护区域人工确认 + 独立实现 plan（触及薪酬保护区域，AI 不得在无人工批准下改 ORM/会计行为）
- 不补 finance/mfg/assets/pur+sal+inv 测试有效性（分别归 R2.10/R2.11/R2.12/R2.14）
- 不补 R2.15 view.xml drift

## Task Route

- Type: `implementation-only change`（纯测试新增）
- Owner Docs: `docs/design/human-resource/payroll.md`（§4.5 个税累计预扣 / §6 计提 / §9.1 审批过账联动）。测试断言的预期行为须与 payroll.md 一致；017 Deferred 缺口的负向断言须与 payroll.md 的 Deferred 标注一致
- Skill Selection Basis: 工作方法为 Nop 服务层集成测试（`JunitAutoTestCase` + IGraphQLEngine + HrFrozenClockExtension + seed/assert）→ `nop-testing`（基类选择、@NopTestConfig、seed 只追加、拒绝路径快照处理、三层验证模型）

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（H2 localDb 集成测试；mock post 抛异常复用 finance 域既有模式——seed PENDING/retryCount 记录触发悬挂，或 @Inject 替换 Facade；HrFrozenClockExtension 已存在控制个税累计跨月时序）

## Execution Plan

### Phase 1 - hr 薪酬/过账链路残差补强（G1+G2+G3）

Status: planned
Targets: `module-hr/erp-hr-service/src/test/java/app/erp/hr/service/TestErpHrPayrollEngine.java`（新增测试方法 + 对应 `_cases/` 快照 + 高档 seed 数据）
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: R2.0 done（已 done）；R1.26 done（016/018 已修复 + 单元/集成测试已落地）；R1.16 done（048 过账悬挂分级）

- [ ] Add: G1 高档税率集成级 E2E — seed 月薪 100000 员工 + HrFrozenClockExtension 跨月推进使累计应纳税所得额 >960000，调 calculateSalary/runPayroll，断言末档 45% 税率正确计算（cumulativeTax 公式正确）、无 NPE、monthTax 为正（演练完整引擎流程：社保钳制 + 累计写回 + 批处理，补单元测试不覆盖的集成路径）
  - Skill: `nop-testing`
- [ ] Add: G2 过账悬挂测试 — mock post 抛异常触发 tryPostPayment posted=false 窗口（复用 finance 既有 seed PENDING/retry 触发模式或 @Inject 替换），断言 Salary 状态/PAID + posted=false 可观测 + 无发放凭证（闭合 P1-MA2-048 测试可见性，依赖 R1.16 告警闭环）
  - Skill: `nop-testing`
- [ ] Add: G3 公司承担过账负向文档化测试 — approve 薪酬后断言计提 SALARY(270) + 社保公司 290 + 公积金公司 300 三类凭证**当前未生成**（文档化 P1-MA4-017 Deferred 缺口，负向断言 + 注释引用 017 successor）；断言发放 SALARY_PAYMENT 凭证正常生成（正向基线）
  - Skill: `nop-testing`
- [ ] Proof: Phase 1 新增测试方法首次 RECORDING 后切 CHECKING 全绿
  - `mvn test -pl module-hr/erp-hr-service -Dtest=TestErpHrPayrollEngine`
  - Skill: none

Exit Criteria:

> hr 薪酬链路集成级高档 + 过账悬挂 + 公司承担负向文档化补齐。017 公司承担过账为 Deferred，G3 以负向断言文档化缺口，正向测试待 successor。

- [ ] G1 集成级高档（末档 45% 正确计算非 NPE）+ G2 过账悬挂（posted=false 可观测）+ G3 公司承担负向（三类凭证未生成 + 发放凭证正常）测试在 CHECKING 模式绿
- [ ] G3 负向断言与 payroll.md 的 017 Deferred 标注一致（不误报为回归）

## Draft Review Record

- Independent draft review iteration 1: needs revision (ses_04b01b805ffe2pTFbaajDSdZlx) — 原 baseline 遗漏 R1.26 已落地的 `TestIncomeTaxCalculator`（7 方法覆盖高档税率单元级 + 累计损坏单元级）+ `testCorruptCumulativeDataThrowsNotSilentReset`（集成级），致 (a)(c) 提议为重复（c 为第三份副本）。已修订：Current Baseline 完整盘点既有覆盖 + 标注已闭合子项，(a) 收敛为集成级残差 G1、(c) 移除（单元+集成双层已闭合）、(d)+(e) 合并为 G3，保留真实缺口 G2（过账悬挂）。闭合声明不再对已闭合行为重复声明。
- Independent draft review iteration 2: accept (ses_04afcdf8fffeLkvy4stPDSszhj) — 实测复核：TestIncomeTaxCalculator（4 高档 + 3 损坏方法）+ testCorruptCumulativeDataThrowsNotSilentReset:259 确认 (a) 单元级/(c) 单元+集成级已闭合；(c) 移除为诚实范围修正（R1.26 closure audit 已记录两测试），非 P1 降级；G1（全引擎高档员工月薪≤30000 永不触达末档）、G2（hr 测试零 posted=false/mock-post）、G3（零 270/290/300 负向断言）残差均真实；P1-MA4-017 Deferred 确认（无 ER 列 + tryPostAccrual 死代码 + payroll.md Deferred 标注）致 G3 负向测试选择正确；无阻塞项。草案审查收敛，可开始实施。

## Closure Gates

> 纯测试新增，无生产代码/ORM 变更。完整仓库验证在此处一次。

- [ ] 范围内行为完成（G1 + G2 + G3 残差测试方法落地并 CHECKING 绿）
- [ ] 相关文档对齐（公司承担过账负向测试与 payroll.md 017 Deferred 标注一致；若 G1/G2 测试发现 016/048 修复存在回归，升级为 Fix 不降级）
- [ ] 已运行验证：`mvn clean install -DskipTests` 全绿 + `mvn test -pl module-hr/erp-hr-service` 全绿（含新测试）+ `bash docs/audits/nop-compliance-checker.sh` 零新增命中
- [ ] 无范围内项目降级为 deferred/follow-up（017 正向实现为既有 Deferred successor，非本计划范围；G3 负向文档化测试属本计划范围内交付；高档单元级/累计损坏为既有覆盖非本计划 deferred）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### P1-MA4-017 公司承担过账链路正向实现（既有 Deferred successor）

- Classification: `out-of-scope improvement`（沿用 R1.26 Deferred 裁决）
- Why Not Blocking Closure: 本计划为测试有效性（R2.13）；017 正向实现属 substantial 独立 plan（ORM ask-first 加 socialInsuranceER/housingFundER 列 + HR/会计保护区域人工确认 + tryPostAccrual/290/300 接线），不在测试计划范围；本计划以负向文档化测试（G3）闭合 P1-MA4-019 (d)(e) 测试可见性
- Successor Required: `yes`（沿用 R1.26 命名 successor：独立计提+公司承担过账链路 plan；触发 = 人工批准 ORM ask-first + 保护区域 owner doc 后）

## Closure

Status Note: <待 closure audit 后填写>

Closure Audit Evidence:

- Auditor / Agent: <独立子代理>
- Evidence: <待 closure audit>

Follow-up:

- 无（017 正向实现 successor 沿用 R1.26 Deferred 登记，非本计划产出）

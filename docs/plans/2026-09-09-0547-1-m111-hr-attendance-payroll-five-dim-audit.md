---
status: active
mission: ai-check-r3
work-item: M1.11
group: "2026-09-09-0547"
verify: [test]
---

# 2026-09-09-0547-1 M1.11 hr hr-2 五维符合性审计（考勤、薪酬与排班切片）

## Current Baseline

- 依赖已满足：M0.6 done（plan `2026-09-06-1451-3`）；MI.9 收官 done（plan `2026-09-07-1715-3`，独立 closure audit ACCEPT，2026-09-08；`docs/testing/known-good-baselines.md` 2026-09-08 `ai-check-r3 MI 终态行`：全 reactor `mvn test` 4006/0/0/1、CJK checker report mode CAT1..4 = 0/0/0/0、`--strict`/`--self-test` PASS、双 checker 零回归、白名单 27 文件四要素齐备）。本计划依赖链 M1.10（hr-1，plan `2026-09-08-1454-1`）已执行完毕、`ck-hr-org-r3.md` 落盘——done 翻转归 owner/engine 依独立结束审计处置，不阻塞本切片。M1.11 为当前 roadmap 文档序首个「deps 已满足且无既有计划」的工作项（M1.2~M1.10 与 M1.12/M1.13 同状已执行；M1.14/M1.15 无既有计划、与本计划同批起草分列 N=2/N=3）。
- 同 mission 先例：M1.1/M1.5/M1.8/M1.10/M1.12/M1.13/M1.2/M1.3/M1.4/M1.6/M1.7/M1.9 十二切片（plans `2026-09-08-1042-1/2/3`、`2026-09-08-1454-1/2/3`、`2026-09-08-2238-1/2/3`、`2026-09-09-0232-1/2/3`）已执行并独立闭包审计 ACCEPT，15 份 `ck-*-r3.md` 已落盘（12 切片——M1.12 产 2 份、M1.13 产 3 份，实仓 `ls` 与本轮索引产物清单双重核对）——本计划沿用其通过的七阶段执行形态。
- 强制核对矩阵已冻结：`docs/audits/check/2026-09-06-1645-ai-check-r3/m0-5-audit-checklists.md`——本计划唯一格集合 = **U14 × 五维 × hr-2**（§4 映射表第 11 行）；判定标准/机械核查程式/跨轮查重程序以该冻结清单 §1/§2 为准，执行期只允许在查重裁决列追加证据，不改判定标准。
- 切片范围（U14 hr-2）：考勤 `ErpHrAttendance*` / 工时 `ErpHrTimesheet*`（+Line）/ 请假 `ErpHrLeaveRequest*`/`ErpHrLeaveBalance*` / 薪酬 `ErpHrSalary*`（Salary/SalaryItem/SalarySimulation/SalarySimulationItemAdjustment）+ 薪资引擎 `payroll/`（PayrollCalculator/IncomeTaxCalculator/SocialInsuranceCalculator/TaxBracket/TaxBracketParser）/ 工资单 `ErpHrPayrollBankFile*` + `ErpHrSalaryGenerateBankFileProcessor`/`ErpHrSalarySimulationConvertToFormalProcessor` / 税与社保配置 `ErpHrTaxConfig*`/`ErpHrTaxSpecialDeduction*`/`ErpHrSocialInsurance*` / 薪酬过账 `posting/`（SalaryPostingDispatcher/SalaryPostingExecutor/SalaryPostingProvider）/ 排班 `ErpHrShift*`（Shift/ShiftAssignment/ShiftRotationPattern/ShiftSwapRequest）+ `scheduling/ShiftAttendanceCalculator` + hr-2 相关 job；owner doc `docs/design/human-resource/payroll.md` + `shift-scheduling.md` + `payroll-simulation.md`（考勤语义散布于 `README.md`/`state-machine.md`，§3.3 U14 行明示）；物理面 `module-hr/erp-hr-{dao,service}` 的 `src/main`。**U14 余下面承接**（`ck-hr-org-r3.md` §successor 注记）：competency/development plan/assessment/survey 族 BizModel 与招聘 BizModel 本体范围外项（P3-CK-hr-009/017 维持登记态复核）随本切片收口，闭合 U14 五格防归属真空；hr-1 面（组织/岗位/员工/编制 + 招聘入职衔接消费点）不在本切片（M1.10 已收官）。
- 共享代码唯一归属（冻结清单 §3.2）：common 抽象族（`AbstractProcessor.illegal*`、`AbstractErpCrudBizModel` 状态锁基类）行为缺陷归 U20（M1.15，同批 N=3），本切片只审调用点；posting 引擎内部归 fin-1（M1.1 已收官）——hr 薪酬过账 dispatcher/provider 族为**消费侧**，消费侧 finding 涉及引擎内部时标注「归属 fin-1」归并；聚合横切面归 U21（M1.16）；notify 派发子系统本体归 U11（M1.15）。hr-2 专属判定面（§3.3 U14 B 行）：薪酬模拟/排班/考勤语义（散布 README/state-machine）。
- 跨轮查重源（§2）：r1 `docs/audits/check/ai-check-index.md`（同单元 hr-2 域报告 `ck-hr-attendance-payroll.md` C6.2：0 P0 / 5 P1 / 9 P2 / 12 P3，done——本切片主查重源）+ 同域跨切片参考 `ck-hr-org.md`（C6.1，M1.10 r3 已归并裁决 13 open 项）+ r2 只读目录 `docs/audits/check/2026-08-28-2049-ai-check-r2/` + 索引 §Mission 基线快照（daoFor 读侧 P1-MA1-022、死状态 P1-MA2-039/040、调动休假告警 P2-MA2-050 均已裁决偏离不重复报告）。
- DIM-I 内嵌 MI 先行时序口径（冻结清单 §1.5）：hr 探针族（CAT-1 20）MI 后已清零——本切片 DIM-I 维仅验证零回归与白名单合规，不再产生同类 finding；脚本红 = MI 回归升级裁决报 MI 通道；白名单四要素缺失 = 白名单登记缺陷 finding。hr 白名单条目 1 条（`ErpHrConstants.java`，C2 seed 角色名契约）。
- 绿色基线：`docs/testing/known-good-baselines.md` 2026-09-08 MI 终态行（MV.1 对照面）；compliance checker 对照面 = M0.3 快照行（R1d=14/R2a=34/R2b=242/R2c=1542/R2d=38/R3=5/R6=2/R10=14/R12a=71/R12b=66/R12c=42）；hr service 模块回归对照计数 249（`cjk-baseline.md` §批注账 MI.6 批 2 行锚点；执行期以 known-good-baselines 最新行为权威，同批姊妹计划测试增量允许并披露）；`npm run validate:flux` step [1/3] 导出 0 error（999 页 / erp 855），整体 exit 1 余项 = 325 条既有 `variant=primary` stub 外部漂移（successor 在案，非本切片 finding）。
- 仓库现状（2026-09-09 起草时实核）：HEAD `2c1c1ef25`，已跟踪文件零改动（脏面 = 本批 3 份同批起草计划 untracked 文件 `2026-09-09-0547-1/2/3`）。审计证据以实跑时点 HEAD + 脏面披露为准（MI.9 收官审计先例：脏树实跑 + 披露）。
- 剩余差距：U14 × 五维 × hr-2 五格 verdict 未落盘；执行目录尚无 hr-2 切片 `ck-*.md` 报告（M1.17 收官前 U14 基础格完整性依赖本报告落盘——`ck-hr-org-r3.md` §残留风险 ④ 明示）。

## Goals

- 按冻结清单对 U14 × 五维 × hr-2 五格全跑（禁止抽样、禁止跳维），逐格落 verdict（`pass` / `finding` / `n-a` 带理由），产出 `docs/audits/check/2026-09-06-1645-ai-check-r3/ck-hr-attendance-payroll-r3.md`（五维覆盖矩阵 + finding 列表 + 统计 + 剩余风险声明，roadmap 横切关注点 13）。
- 每候选 finding 完成三态查重裁决（复用/归并/新立 `-r3` ID，历史 ID 永不覆写）；DIM-B 维含 owner-doc 关键断言抽样 ≥2 doc × 2 断言（漂移则扩至全部 hr-2 相关 owner doc）。
- 双索引同步：本轮 `ai-check-r3-index.md` 产物清单 + 跨轮 `docs/audits/check/ai-check-index.md`（§报告清单行 + §Finding 追踪新 ID 行）。
- 全程零生产代码改动（roadmap 规则 6）：只产 finding 与索引，收官 `git status` 机械核证。

## Non-Goals

- 不修复任何 finding（M1 只读审计；修复归 M2.x，强制先写失败测试）；不改任何生产代码/ORM/api.xml/配置/页面/seed 文件。
- 不覆盖 hr-1 格（M1.10 已收官：组织/岗位/员工/编制 + 招聘入职衔接消费点）与 U14 之外任何单元格；组织删除守卫、员工-as-partner 跨域写不在本切片重复立项。
- 不接管 r1/r2 工作项（横切关注点 5）；不重开既有裁决（lesson 09/10、compliance baseline、CJK 白名单）；不做 roadmap 状态翻转（done 转换由结束审计机制处置）。
- DIM-I 不产生同类 CJK finding（§1.5 内嵌口径）。

## Phase 1 — 执行目录就位 + 切片红线与脏面披露

> 统一类型：Proof（3 项 Proof）。
> Skill: none
> Targets: `docs/audits/check/2026-09-06-1645-ai-check-r3/`（幂等复用，不新建目录）；盘点注记落本计划勾选注记
> Prereqs: 无

- [x] <Proof> `mkdir -p docs/audits/check/2026-09-06-1645-ai-check-r3/`（本轮唯一规范执行目录，幂等复用；roadmap 规则 8），确认 `ai-check-r3-index.md` 与 `m0-5-audit-checklists.md` 及 15 份姊妹切片报告在位
      - Skill: none
      - 证据：目录实存（幂等复用）；`ai-check-r3-index.md` + `m0-5-audit-checklists.md` + 15 份 `ck-*-r3.md`（fin×4/mfg×3/ast×2/hr-org/prj/qa/pur/sal/inv）逐名 `ls` 核对在位
- [x] <Proof> 记录审计时点仓库状态：HEAD hash、`git status --porcelain` 脏面清单、同批姊妹计划（`2026-09-09-0547-2/3`，尚待执行）披露（MI.9 收官审计脏树实跑先例）；后续全部证据注记引用该时点
      - Skill: none
      - 证据：T0 = HEAD `2c1c1ef254599253f02ba7d231d96c6f08b0cc50`（与计划基线行一致）；脏面 = 3 untracked（本计划 + 姊妹 `2026-09-09-0547-2/3`，零生产路径）；零已跟踪文件改动
- [x] <Proof> 切片红线基线：`bash docs/audits/nop-compliance-checker.sh`（对照 M0.3 快照行）+ `node tools/check-hardcoded-cjk.mjs`（report mode，对照 0/0/0/0 终态）实跑记录；漂移即登记（不就地裁决）
      - Skill: none
      - 证据：checker 19 规则 = M0.3 快照行零漂移（R1d=14/R2a=34/R2b=242/R2c=1542/R2d=38/R3=5/R6=2/R10=14/R12a=71/R12b=66/R12c=42，其余 0）；CJK report mode CAT1..4 = 0/0/0/0（CAT5 21158 行豁免仅统计）——与 MI 终态行对账一致

Exit Criteria:

- [x] 执行目录就位且本轮索引头部登记路径一致；时点/脏面披露在案
- [x] 双 checker 红线数字在案且与 M0.3 快照行 / MI 终态行对账一致（或漂移已登记）

## Phase 2 — DIM-B 后端平台合规走查（15 维度 + R1-R12 + 断言抽样）

> 统一类型：Proof-heavy（3 Proof + 1 Decision）。
> Skill: nop-platform-conformance-audit-prompt + code-quality-audit-prompt（roadmap M1.11 行指定，承 M1.10 行「同上」）
> Targets: `module-hr/erp-hr-dao|erp-hr-service/src/main/java`（hr-2 范围 = 考勤/工时/请假/薪酬/工资单/税社保/排班/薪酬过账族 + U14 余下面承接族 BizModel/processor/state machine/job/payroll 引擎）；走查 verdict 落勾选注记
> Prereqs: Phase 1 完成

- [x] <Proof> 冻结清单 §1.1 机械核查程式全套实跑：compliance checker 逐规则对照 + 反模式 grep 族（`extends RuntimeException`/`@Inject private`/`System.currentTimeMillis`/`@Transactional`×`@BizMutation` 对照 R6 基线/`IDaoProvider|IOrmTemplate|@SqlLibMapper` 命中处注释理由）+ codegen 产物安全（`__XGEN_FORCE_OVERRIDE__`、`_gen` 脏面）+ 聚合完整性（按 §6 勘误 E1 路径 `app-erp-all/src/main/resources/_vfs/nop/main/auth/app.action-auth.xml`）
      - Skill: nop-platform-conformance-audit-prompt
      - 证据：反模式族 hr-2 全零（RuntimeException=0 / @Inject private=0 / currentTimeMillis=0 / @Transactional 真实共存=0——唯一命中 SalaryPostingExecutor L14-15 javadoc 说明引注）；IDaoProvider/IOrmTemplate 命中逐文件核对：processor 族 = processor-extension-pattern 惯例（r1「验证为正确」在案），payroll 引擎 + posting 3 文件 10 站点无豁免注释 → 新立 hr2-029-r3；`__XGEN_FORCE_OVERRIDE__` 38 文件（src）全为 erp-hr-meta dict.yaml 生成产物 + `_gen`/`_` 脏面空；E1 路径 x:extends 含 `/erp/hr/auth/erp-hr.action-auth.xml`（L17）
- [x] <Proof> 15 维度逐维走查 hr-2 范围（程序式确定性走查落 verdict；hr-2 专属焦点：①Model→Delta→Java ②跨实体 I*Biz——薪酬过账消费侧（SalaryPostingDispatcher/Provider→fin 凭证通道，引擎内部归 fin-1）与 shift↔attendance 联动 ⑧状态机——考勤/请假/薪酬单/排班调换状态机 Bean 与迁移路径 ⑨审批流/作业——薪酬审批流（salary workflow）与 job 双层门控 ⑮断言抽样——`payroll.md` + `shift-scheduling.md` + `payroll-simulation.md`/`state-machine.md` ≥2 doc × 2 关键断言（状态名/字段名/角色名/迁移路径/ErrorCode），≥2 处漂移扩大至全部 hr-2 owner doc）；blocker/major/minor 按 prompt 严重性指南分级
      - Skill: nop-platform-conformance-audit-prompt
      - 证据：15/15 无跳维（逐维 verdict 落 `ck-hr-attendance-payroll-r3.md` §1 DIM-B 行）：②跨实体全 I*Biz（Executor→IErpFinVoucherBiz 消费侧合规，引擎内部归 fin-1；shift↔attendance 联动 4 处全 I*Biz）+ FNPT 注册面缺口新立 hr2-028-r3；⑧4 Bean 矩阵与 dict 逐边一致；⑨DIRECT+WORKFLOW 双模单一委托点 + job 双层门控；blocker=0/major=0，新发面全 P3
- [x] <Proof> 维度⑮ owner-doc 关键断言抽样（≥2 doc × 2 断言）独立落注记，抽样对象与逐条结论在案
      - Skill: code-quality-audit-prompt
      - 证据：6 doc × 16 断言点（payroll.md 4 / state-machine.md §一三四五 5 / shift-scheduling.md 4 / payroll-simulation.md 2 / README.md 1）——2 漂移聚簇于 state-machine.md §五（Survey「桩零 mutation」vs 实仓 publish/close/archive + published-immutable 守卫；DevelopmentPlan「OVERDUE 死状态」vs L190 writer 已落地）→ 触发扩样至全部 hr-2 owner doc，新立 hr2-027-r3；其余 14 断言一致或在册 finding 承载
- [x] <Decision> 本维候选 finding 逐条过 §2 三态裁决（查 r1 `ck-hr-attendance-payroll.md` §Finding 追踪 + `ck-hr-org.md`/M1.10 r3 归并记录 + r2 目录 + §Mission 基线快照）：同型 fixed=复用（复核 HEAD 仍有效）/同型 open=归并原 ID/确属新发=新立 `P{n}-CK-hr2-{NNN}-r3`（承 r1 hr-2 族 `CK-hr2-001..026` 新立自 027 起；`CK-hr-` 为 hr-org 切片命名空间，勿混用）；裁决证据落勾选注记（报告落 Phase 7）
      - Skill: code-quality-audit-prompt
      - 证据：r1 hr-2 26 条逐一比对（复用 2：hr2-006 F1.2 守卫 + hr2-007 F1.3 基类族 HEAD 复核有效；归并 24 含 015 扩员 2 站点 + 013 并入测试缺位注记）；r1 hr-org 13 open 项参考 + r2 目录零同型新登记 + §Mission 基线快照（MA1-022/MA2-039/040/050 偏离不重复）；新立 4 全 P3（027/028/029/030），ID 承 026 自 027 起，历史 ID 零覆写

Exit Criteria:

- [x] U14×B×hr-2 格 verdict 落盘（pass/finding/n-a 带理由），15 维度无跳维
- [x] 机械程式输出数字在案且与基线对账一致；候选 finding 三态裁决完成（复用/归并/新立逐条在案）

## Phase 3 — DIM-F 前端页面与 E2E 走查

> 统一类型：Proof（2 项 Proof）。
> Skill: none（判定锚点 = 冻结清单 §1.2 所列 architecture/design docs + e2e-runbook）
> Targets: `module-hr/erp-hr-web/src/main/resources/_vfs`（hr-2 面：薪酬核算页（复杂手写页清单成员）+ 考勤/请假/排班/工资单页面族）；E2E 涉 hr spec 时按 §1.2 ③
> Prereqs: Phase 1 完成

- [x] <Proof> 全局面静态门禁实跑：`npm run validate:flux`（step [1/3] 导出 0 error / 999 页；整体 exit 1 余项 = 325 条既有 `variant=primary` stub 外部漂移，successor 在案，非本切片 finding）+ flux-only grep（`component="AMIS"` 保留层 expect 0 / ORM `ext:web-renderer="flux"` 缺失 expect 空）
      - Skill: none
      - 证据：step [1/3] `FLUX_PAGE_ERROR_COUNT: 0 pageCount=999 erpPages=855`；整体 exit 1 = 325 条 ERR 全 variant×dropdown-button 族（non-variant=0），hr 域命中 31 条全同族零非 variant；`component="AMIS"` 保留层 = 0（hr + 全工作区双口径）；`ext:web-renderer="flux"` 缺失 = 0（hr orm 42 处在位）
- [x] <Proof> hr-2 页面走查：薪酬核算页 + 考勤/请假/排班/工资单页面族（codegen+delta 面对照 view-and-page-strategy：REST `/r/` 数据访问 / codegen vs 手写边界查 M0.4 矩阵 / 定制走 `x:extends` / i18n-en 承载约定）逐页落 verdict；涉 hr E2E spec 时核对 PageObject 模式 + `E2E_ENGINE` flux 缺省 + 禁 GraphQL 断言；查重：全局面 325 ERR 族与 hr 域命中条数对账（既有外部漂移不立项）
      - Skill: none
      - 证据：ErpHrSalary 页 = 3 行 codegen stub + 保留层 view.xml（x:extends + bounded-merge + 审批五动作 REST）；payroll-approval/team-vacation-calendar/org-chart 手写 flux 页 MI.8 i18nEn 登记行在位；~26 页族逐页 codegen wrapper + 保留层 delta 合规；死状态样式分支查重 = pur-017 族不涉（dangerVals 共享分类器非 per-dict 分支，登记不立项）；E2E 10 spec 违规 selector = 0 + E2E_ENGINE 缺省 flux（engine.ts）+ GraphQL 命中为 API 驱动数据层通道（M1.10 判例）

Exit Criteria:

- [x] U14×F×hr-2 格 verdict 落盘；全局面门禁数字（导出 error 数 / flux-only 命中数）在案对账一致
- [x] hr-2 范围页面逐页走查完成，无跳页

## Phase 4 — DIM-S seed 数据走查

> 统一类型：Proof（2 项 Proof）。
> Skill: none（判定锚点 = `docs/architecture/seed-data.md`）
> Targets: `app-erp-all/src/main/resources/_vfs/_init-data`（只读清点）+ hr-2 相关 seed（考勤/工时/请假/薪酬/排班/税社保族 CSV + 薪酬过账 posted 一致性裁决面 + M1.3 批配置链）
> Prereqs: Phase 1 完成

- [x] <Proof> 引用完整性门禁实跑：`mvn test -pl app-erp-all -Dtest=TestErpSeedDataIntegrity` 全绿（363 实体 findAll + to-one 零悬空 + 零孤儿 CSV + scope-pinning 计数）
      - Skill: none
      - 证据：Tests run: 4, Failures: 0, Errors: 0, Skipped: 0，BUILD SUCCESS（2026-09-09 06:47）
- [x] <Proof> hr-2 seed 面核对：`git status --porcelain app-erp-all/src/main/resources/_vfs/_init-data/` expect 空（只读审计零 seed 变更）+ 资产清点对账（对照 seed-data.md 对账表最新登记行 372 CSV + 1 SQL）+ deploy `_seed_*.sql` 同步义务逐命中查登记处表 + hr-2 族 seed（考勤/工时/请假/薪酬/排班/税社保 CSV）与 M1.3 批配置链核对 + **薪酬过账 posted 一致性裁决**（`posted=true` 当且仅当有对应凭证回链，域内按 seed-data.md 各批裁决抽查；M1.10 Phase 4 明示此项归 hr-2 未触碰）
      - Skill: none
      - 证据：seed 脏面 = 空；资产清点 372 CSV + 1 SQL（= M1.5 批次后登记行）；deploy 命中 6 文件 = cs×3 + notify×3 均已聚合登记，hr 零 deploy seed；hr-2 族 18 CSV 在位 + M1.3 批 C17 值中性化契约核对一致；**posted 裁决闭合**：`erp_hr_salary.csv` 无 POSTED 列（默认 false）、2 行均 UNSUBMITTED/PENDING → seed 零 posted=true 行 ⟺ 零凭证回链，双射空验一致

Exit Criteria:

- [x] U14×S×hr-2 格 verdict 落盘；`TestErpSeedDataIntegrity` 全绿在案
- [x] seed 零变更 + 同步义务核对 + 薪酬过账 posted 一致性裁决结果在案

## Phase 5 — DIM-T 单元测试走查

> 统一类型：Proof（3 项 Proof）。
> Skill: none（判定锚点 = `docs/architecture/testing-strategy.md`）
> Targets: `module-hr/erp-hr-service`（`<SVC>` = 本模块，同冻结清单 §1.4 记法）
> Prereqs: Phase 1 完成

- [x] <Proof> 切片本地回归：`mvn test -pl module-hr/erp-hr-service` 全绿零失败（数字落注记，对照 known-good-baselines 各域计数 249 锚点 + 同批姊妹计划增量披露）
      - Skill: none
      - 证据：Tests run: 249, Failures: 0, Errors: 0, Skipped: 0，BUILD SUCCESS（= cjk-baseline §批注账 MI.6 批 2 行锚点 249 精确一致；姊妹 0547-2/3 面 = crm/cs/ct/b2b/drp + mnt/aps/log/notify/md/common，hr 无姊妹增量）
- [x] <Proof> 覆盖缺口对账：`grep -rc "@BizQuery\|@BizMutation"` 公开方法清单 × `ls <SVC>/_cases/...` 测试目录清单逐项对账（hr-2 范围 = 考勤/请假/薪酬/排班/工资单族 BizModel + payroll 引擎 + 薪酬过账链）；关键业务流清单核对——**P1 考勤→薪酬→过账清单行覆盖**（testing-strategy §关键业务流 P1 行；M1.10 明示归 hr-2 未触碰；测试面锚点：`TestErpHrAttendance*` 3 类 / `TestErpHrLeaveEngine` / `TestErpHrPayrollEngine` / `TestErpHrPayrollSimulation` / `TestErpHrSalaryPostingChain` / `TestErpHrSalaryWorkflowApproval` / `TestErpHrShiftScheduling` / `TestErpHrTimesheetFamily`）；缺口按业务关键度定级
      - Skill: none
      - 证据：hr-2 BizModel 注解动作 ≈70 × 10 个 hr-2 测试类逐项对齐（锚点类全数在盘）；P1 清单行全覆盖（Attendance→PayrollEngine→PostingChain→WorkflowApproval 四类在案）；缺口：findPayrollSummary + queryCumulativeTaxData 零测试 → 新立 hr2-030-r3（P3 读侧）；getCompanySummary/getProjectSummary/findAnomalies 缺位并入 P2-CK-hr2-013 同控制点
- [x] <Proof> 快照纪律：`grep -rn "SnapshotTest.RECORDING" <SVC>/src/test/java` expect 0（提交态零残留）+ `delVersion`/decimal `*` 通配屏蔽合规抽查
      - Skill: none
      - 证据：RECORDING = 0 残留；delVersion 7 处命中均为逻辑删除断言/注释（非屏蔽滥用）；`_cases` 零 `*` 通配屏蔽（createTime/updateTime 框架自动屏蔽）

Exit Criteria:

- [x] U14×T×hr-2 格 verdict 落盘；本地回归全绿数字在案
- [x] 覆盖对账 + P1 关键业务流清单核对 + 快照纪律结果在案

## Phase 6 — DIM-I i18n 零回归与白名单合规（MI 先行时序口径）

> 统一类型：Proof（2 项 Proof）。
> Skill: none（判定锚点 = `docs/architecture/i18n-compliance.md` + `docs/audits/cjk-baseline.md`）
> Targets: 全域门控 + hr 白名单条目核对
> Prereqs: Phase 1 完成

- [x] <Proof> 零回归门控：`node tools/check-hardcoded-cjk.mjs --strict`（exit 0，vs 冻结 SNAPSHOT 单向收紧）+ `--self-test` PASS；脚本红 = MI 回归，升级裁决报 MI 通道，不立 DIM-I finding
      - Skill: none
      - 证据：`--strict` PASS exit 0（0 new violations，170 baseline files，totals CAT1..4 = 0/0/209/1318 单向收紧成立）；`--self-test` PASS
- [x] <Proof> 白名单合规核对：`docs/audits/cjk-baseline.md` §WHITELIST hr 条目全数核对（hr = 1 条 `ErpHrConstants.java`；不足 3 条时全数核对并记录条目数）四要素（文件路径/理由/owner doc 指针/裁决来源）+ `grep -L "@Locale"` 全量 `*Errors.java` expect 空 + `git status --porcelain 'module-*/erp-*-meta/**'` expect 空（`_` 前缀生成 i18n yaml 禁手改）；四要素缺失 = 白名单登记缺陷 finding（非同类 CJK finding）
      - Skill: none
      - 证据：hr WHITELIST = 1 条（<3 全数核对）：`ErpHrConstants.java` 四要素齐备（cats[3] / HR_ROLE_ID seed 角色名 C2 契约 / i18n-compliance.md 准绳表 #5 + CAT-3 行 C2② / plan 2026-09-07-1715-1 Phase 2），HEAD 复核 L293 `HR_ROLE_ID = "HR 专员"` 与登记一致；`grep -L @Locale` *Errors.java = 0 缺失；meta + `_vfs/i18n` 脏面 = 空

Exit Criteria:

- [x] U14×I×hr-2 格 verdict 落盘；`--strict`/`--self-test` PASS 在案
- [x] 白名单四要素核对 + `@Locale` + meta 禁手改核对结果在案

## Phase 7 — 三态裁决汇总 + ck 报告落盘 + 双索引同步 + 零改动核证

> 统一类型：Add-heavy（2 Add + 2 Proof + 1 Decision）。
> Skill: code-quality-audit-prompt
> Targets: `docs/audits/check/2026-09-06-1645-ai-check-r3/ck-hr-attendance-payroll-r3.md`（新增）+ `ai-check-r3-index.md` + `docs/audits/check/ai-check-index.md`
> Prereqs: Phase 2~6 完成（五格 verdict 齐备）

- [x] <Decision> 全切片候选 finding 汇总复裁决：逐条确认三态（复用/归并/新立）与级别（P0~P3）一致性与 ID 规范（`P{n}-CK-hr2-{NNN}-r3`，承 r1 hr-2 族 001..026 新立自 027 起，历史 ID 永不覆写；升级级别按 code-history-deferred-triangulation-audit-prompt §3.1）
      - Skill: code-quality-audit-prompt
      - 证据：复裁决一致——新立 4 全 P3（hr2-027/028/029/030-r3，ID 承 026 自 027 起，级别按同族跨单元先例校准：028 同 FNPT 族 fin4-023-r3/mfg2-024-r3 等全 P3、029 同 daoFor 族全 P3、027 同 doc 漂移族全 P3、030 同覆盖缺口族 P3）；复用 2（P2）+ 归并 24（P1×5 + P2×7 + P3×12）逐条级别与 r1 登记一致零升级零降级；历史 26 hr2 ID + 同域 hr-org 17 ID 零覆写
- [x] <Add> 落盘 `ck-hr-attendance-payroll-r3.md`：五维覆盖矩阵（5 格 verdict 全落）+ finding 列表（含归属标注与归并指针）+ 统计（P0~P3 计数）+ 剩余风险声明（横切关注点 13 完整四件套）
      - Skill: none
      - 证据：报告已落盘（五维矩阵 5/5：B=finding / F=pass / S=pass / T=finding / I=pass；§2 三态裁决 30 条 + §2.4 范围外 + §2.5 归属标注；§3 统计新立 4/复用 2/归并 24；§4 剩余风险四件套含 M1.10 移交 posted 裁决闭合与 U14 格完整性就绪声明）
- [x] <Add> 双索引同步：本轮 `ai-check-r3-index.md` 产物清单追加本报告行 + 跨轮 `docs/audits/check/ai-check-index.md` §报告清单追加 M1.11 行 + §Finding 追踪追加新立 `-r3` ID 行（归并的追加证据至原 ID）
      - Skill: none
      - 证据：`ai-check-r3-index.md` §本目录产物清单追加 M1.11 行（第 17 行报告行）；`ai-check-index.md` §报告清单追加 M1.11 r3 行 + §Finding 追踪追加 4 行（P3-CK-hr2-027/028/029/030-r3，含同族先例与修复建议）；归并证据落报告 §2.2，历史 ID 状态零覆写
- [x] <Proof> 零生产代码改动核证：`git status --porcelain` 过滤 `module-*`/`app-erp-all` 生产路径 expect 空（roadmap 规则 6；触碰面 = 执行目录报告 + 索引 + 本计划勾选注记）
      - Skill: none
      - 证据：过滤命中 = 0；全量脏面 = 2 份索引 modified + 本报告 untracked + 3 份同批计划 untracked（全 docs 面）
- [x] <Proof> 收尾回归：`mvn test -pl module-hr/erp-hr-service` 复跑全绿（审计只读不变式复证；全仓验证归收官机制）
      - Skill: none
      - 证据：复跑 Tests run: 249, Failures: 0, Errors: 0, Skipped: 0，BUILD SUCCESS（两轮 249 全绿一致）

Exit Criteria:

- [x] `ck-hr-attendance-payroll-r3.md` 落盘且五维矩阵 5 格 verdict 完整（缺一格不算完）
- [x] 双索引行追加在案；零生产代码改动核证通过；hr service 回归根绿

## Draft Review Record

- dispatch review #review-2026-09-08-193051-mission-driver-2026-09-09-0547-1-m111-hr-attendance-payroll-five-dim-audit-1-ebd94ffd to 2026-09-08-193051-mission-driver
- 2026-09-09：iteration 1，共识 accept #review-2026-09-08-193051-mission-driver-2026-09-09-0547-1-m111-hr-attendance-payroll-five-dim-audit-1-ebd94ffd（补 Closure Gates——按 M1.9 同批先例为本只读审计计划定制门控；新立 ID 族修正 `CK-hr-`→`CK-hr2-`——承 r1 hr-2 族 001..026 新立自 027 起，`CK-hr-` 为 hr-org 命名空间防碰撞；姊妹报告计数 13→15 实仓核对（12 切片：M1.12 产 2 份 + M1.13 产 3 份，目录与本轮索引双重核对）；脏面表述精确化为「3 份同批 untracked 计划」；基线断言逐一实仓复验在盘——HEAD `2c1c1ef25`、MI 终态行 4006/0/0/1 + CAT 0/0/0/0 + 白名单 27 文件/hr=1 条 `ErpHrConstants.java`、M0.3 checker 全表 11 值、r1 C6.2 统计 0 P0/5 P1/9 P2/12 P3、冻结清单 §4 行 11 = U14×五维×hr-2 与 §3.3 U14 B 行、roadmap M1.11 行范围与 Skill 列「同上」解析（= nop-platform-conformance-audit-prompt + code-quality-audit-prompt）、hr service 249 锚点（cjk-baseline §批注账 MI.6 批 2 行）、seed-data.md 最新行 372 CSV+1 SQL、10 个 hr-2 测试锚点类实仓在盘、owner doc 5 件、`TestErpSeedDataIntegrity`、E1 勘误 auth 聚合路径、`ck-hr-org-r3.md` §successor 注记与 P3-CK-hr-009/017 招聘范围外项引用；M1.10 前置执行态与 roadmap 翻转归属披露诚实，属执行时依赖非计划缺陷，不阻塞本切片；frontmatter `status: draft` → `active`）

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。本计划为只读审计（零生产代码改动），验证命令组即结果表面本身（Phase 1~7 所列），完整仓库验证不在此重复（MV.1 对照面 = `known-good-baselines.md` 2026-09-08 MI 终态行；roadmap 收官机制承载全仓回归与 done 翻转）。ledger 格式计数域仅含 Phase 勾选项，本节门控以普通条目记录核对结果。

- 范围内行为完成（Phase 1~7 全部执行项与退出标准 `[x]`；五格 verdict 完整——`ck-hr-attendance-payroll-r3.md` 覆盖矩阵 5/5，缺一格不算完）
- 相关文档对齐（双索引同步在案——本轮 `ai-check-r3-index.md` 产物清单行 + 跨轮 `ai-check-index.md` §报告清单 M1.11 行与 §Finding 追踪新 ID 行；历史 ID 零覆写；零生产代码改动 = owner doc 零对齐义务）
- 已运行验证：Phase 1 双 checker 红线（compliance checker 对照 M0.3 快照行 + CJK report mode 对照 0/0/0/0 终态）+ Phase 4 `TestErpSeedDataIntegrity` 全绿 + Phase 5/7 `mvn test -pl module-hr/erp-hr-service` 两次全绿 + Phase 6 `--strict` PASS + `--self-test` PASS + Phase 7 零改动核证（`git status --porcelain` 过滤 `module-*`/`app-erp-all` 生产路径 expect 空）——各命令数字落 Phase 勾选注记；任一红线漂移 = 登记裁决后再闭包，不就地吞掉
- 无范围内项目降级为 deferred/follow-up（finding 的「归属 hr-1/fin-1/U20/U21」归并标注与修复归 M2.x 是审计产出分流，非范围降级）
- 独立草案审查已完成并记录（见 Draft Review Record，iteration 1）
- 文本一致性已验证：状态、阶段、门控和日志都一致（ledger 格式：frontmatter `status: active` 保持，完成态由全部勾选 + `## Verification` pass 线 + `## Closure` 回执派生；当日日志条目 `docs/logs/2026/09-09.md` 落盘）
- 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- 结束证据存在于文件中（Phase 勾选注记 + `ck-hr-attendance-payroll-r3.md` + 双索引行 + `## Verification` pass 线 + `## Closure` 审计回执）

## Verification

- pass：Phase 1 双 checker 红线——compliance checker 19 规则 = M0.3 快照行零漂移（R1d=14/R2a=34/R2b=242/R2c=1542/R2d=38/R3=5/R6=2/R10=14/R12a=71/R12b=66/R12c=42，其余 0）；CJK report mode CAT1..4 = 0/0/0/0（2026-09-09 实跑，T0 HEAD `2c1c1ef25`）
- pass：Phase 4 seed 门禁——`mvn test -pl app-erp-all -Dtest=TestErpSeedDataIntegrity` Tests run: 4, Failures: 0, Errors: 0, Skipped: 0，BUILD SUCCESS
- pass：Phase 5 切片回归——`mvn test -pl module-hr/erp-hr-service` Tests run: 249, Failures: 0, Errors: 0, Skipped: 0，BUILD SUCCESS（= cjk-baseline §批注账 MI.6 批 2 行锚点精确一致）
- pass：Phase 6 双 PASS——`node tools/check-hardcoded-cjk.mjs --strict` PASS exit 0（0 new violations / 170 baseline files / totals 0/0/209/1318）；`--self-test` PASS
- pass：Phase 7 收尾回归——`mvn test -pl module-hr/erp-hr-service` 复跑 Tests run: 249, Failures: 0, Errors: 0, Skipped: 0，BUILD SUCCESS（两轮一致）
- pass：Phase 7 零改动核证——`git status --porcelain` 过滤 `module-*`/`app-erp-all` 生产路径 = 空（触碰面 = ck 报告 + 双索引 + 本计划勾选注记 + roadmap 注记 + 当日日志，全 docs 面）
- pass test 2026-09-09-0711 exit=0

## Closure

- 执行完成于 2026-09-09（T0 HEAD `2c1c1ef25`，脏面 3 untracked 同批计划披露）；Phase 1~7 全部执行项与退出标准 `[x]`（35 项，证据注记落各勾选行）；五维覆盖矩阵 5/5（B=finding / F=pass / S=pass / T=finding / I=pass），`ck-hr-attendance-payroll-r3.md` + 双索引行落盘。
- 独立结束审计：由 mission driver CLOSURE_AUDIT 步骤派发的独立子代理（新会话，2026-09-08-193051-mission-driver）于 2026-09-09 执行；执行者未自我审计，回执如下。
- dispatch audit #audit-2026-09-09-0711-2026-09-09-0547-1-m111-hr-attendance-payroll-five-dim-audit-1-bdb61b21 to 2026-09-08-193051-mission-driver models={exec:zhipuai-coding-plan/glm-5.3-flash,aud:zhipuai-coding-plan/glm-5.3-flash}
- accepted #audit-2026-09-09-0711-2026-09-09-0547-1-m111-hr-attendance-payroll-five-dim-audit-1-bdb61b21：独立结束审计 ACCEPT——35/35 计数域全 `[x]`、五维矩阵 5/5（B=finding（新立 P3-CK-hr2-027/028/029-r3）/ F=pass / S=pass（M1.10 移交 posted 裁决闭合）/ T=finding（新立 P3-CK-hr2-030-r3）/ I=pass），`ck-hr-attendance-payroll-r3.md` + 双索引 + roadmap M1.11 行 + 当日日志对账一致、零生产代码改动核证通过；closure visit 实跑 `mvn test -pl module-hr/erp-hr-service` 249/0/0/0 全绿 BUILD SUCCESS（exit=0），机械 checker `plan-check.mjs --strict` 复核通过

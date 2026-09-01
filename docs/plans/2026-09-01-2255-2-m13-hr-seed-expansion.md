# 2026-09-01-2255-2 M1.3 HR 域 seed 扩面（32 CSV）

> Plan Status: completed
> Last Reviewed: 2026-09-02
> Source: `docs/backlog/comprehensive-test-data-and-visual-coverage-roadmap.md` 工作项 M1.3（ready，Deps M0.1 + M0.2 均 done + M0.3 done 弱依赖已满足）
> Related: `docs/plans/2026-09-01-0301-1-m01-seed-scope-adjudication.md`（M0.1，规格表权威源）、`docs/plans/2026-09-01-0527-1-m02-seed-gate-hardening.md`（M0.2，门禁常量随批更新协议）、`docs/plans/2026-08-11-1030-1`（E4.2 MaskHelper 敏感字段脱敏范式，本批 PII 纪律来源）、同批 `2026-09-01-2255-1-m12c-ast-notify-seed-expansion.md`（N=1）/ `2026-09-01-2255-3-m14a-crm-cs-seed-expansion.md`（N=3）
> Audit: required

## Current Baseline

- 实仓 363 个 `app.erp.*` 实体（className 口径；运行时注册 368 = 363 + finance 5 缺 className 补充档，本域不涉及），218 个有 seed（`_init-data/` 实测 222 CSV = 218 app.erp + 4 平台，+ 1 SQL）；`TestErpSeedDataIntegrity` 基线常量 `EXPECTED_APP_ERP_CSV_COUNT = 218` / `EXPECTED_PLATFORM_CSV_COUNT = 4` / `EXPECTED_APP_ERP_ENTITY_COUNT = 363`（+ finance 5 缺 className 补充集，非本域不触碰）。
- hr 域实测已 seed 4（department / employee / SalarySimulation / SalarySimulationItemAdjustment，1045-1），缺 **32**（assessment_detail / attendance / competency / competency_level / development_plan / development_plan_item / employee_assessment / employment_contract / gap_analysis / leave_balance / leave_request / payroll_bank_file / position / recruitment / role_competency / salary / salary_item / shift / shift_assignment / shift_rotation_pattern / shift_swap_request / social_insurance_base / social_insurance_config / survey / survey_answer / survey_question / survey_response / survey_result / tax_config / tax_special_deduction / timesheet / timesheet_line）。逐实体规格见 `docs/architecture/seed-data.md` 规格表 hr 节，本计划逐行引用不复制。
- **既有文件命名事实**：`erp_hr_salary_simulation.csv` / `erp_hr_salary_simulation_item_adj.csv` 为历史软缩写命名（1045-1 先例；结束审计 m-1 勘误——原措辞 `SalarySimulation` camelCase 不准确）——本批新增 32 文件一律按规格表 `<tableName>.csv` snake_case 命名（`DataInitInitializer.loadCsvData` 按表名查找契约），不改既有 4 文件名。
- 既有 FK 锚点已就绪：`ErpHrEmployee` / `ErpHrDepartment`〔已seed〕（规格表 hr 节全部必填 FK 目标均为 hr 域内〔本批〕或 Employee〔已seed〕，无跨域必填 FK）。
- 当前验证基线：`mvn test -pl app-erp-all` **71/0/0/1** 全绿（M0.2 实测，经六批 M1.x 维持）；compliance 门控锚点 = `docs/audits/compliance-baseline.md` §BASELINE 机器块（R2c: 1537），**R2c=1542** 为历史实测值（+5 pre-existing 已登记，沿 plan `2026-09-01-0527-1` 口径协议）；全 reactor 已知 2 处预存回归含 hr 域 `TestErpHrDepartmentPositionDeleteGuard#testDeleteEmptyPositionAllowed`（roadmap Non-Goal，successor 收尾，与本计划无涉但执行期全 reactor 扫掠如遇须按预存登记）。
- 既有 CSV 约定：列头为 DB 列名大写下划线、省略审计列、ISO 日期、小写布尔、ID < 100000；装载拓扑序由 `DataInitInitializer` 自动排序。
- 冻结时钟纪律（`docs/bugs/2026-09-01-0017` / `2026-09-01-0058` 家族教训）：本批全部 seed 行日期列使用静态固定值，禁止 `now()`/滚动期间语义（attendance / leave_request / shift_assignment / contract / timesheet 类实体尤其注意）。
- **敏感字段纪律**（本计划核心约束）：F7 PII 集权威源 = `docs/design/field-formatting-patterns.md` §9（hr employee 4 字段：idCardNo / mobilePhone / bankAccountId / socialSecurityNo）+ E4.2 plan（`2026-08-11-1030-1`）字段表（另含 taxFileNo 隐藏项）；roadmap M1.3 行「salaryAmount 等」系超出 F7 集的扩展口径（`salaryAmount` 非 F7 成员）——本计划对 F7 集 4 字段严格执行伪值纪律，并对批内薪酬金额列（ORM 实证 ANNUAL_SALARY / MONTHLY_SALARY / BASIC_SALARY / GROSS_SALARY / NET_SALARY / OFFER_SALARY 等）保守纳入同纪律；seed 值一律确定性伪值（格式合法、明显非真实个人数据），不引入 production-grade 真实个人数据（roadmap Non-Goal）；`sensitive-masking.visual.spec.ts` 消费面在预分析中核实。
- **干扰面**（本计划核心风险）：HR 2 报表读取面已实证零交集——`ErpHrReportBizModel` 读 `ErpHrEmployee` + `ErpHrSalarySimulationItemAdjustment` + `ErpFinArApItem`（均不在本批 32 表）；`hr.list-value` + 10 个 `hr-*.action.spec`（assessment-dev-plan / leave-attendance / leave-shift-linkage / payroll / recruitment / salary-simulation / shift-assignment / shift-rotation / shift-swap / transfer）消费面执行期 grep 实证；`sensitive-masking.visual.spec` 断言面执行期核实；`app-erp-all/_cases` 集成用例 grep `ErpHr` 零直接引用（2026-09-01 实证），集成快照 DB 状态面按 `_chgType` 增量机制评估（M1.2b 实证纯加性插入不入既有快照）；面 1（`module-hr/erp-hr-service` 域测试：attendance/shift/timesheet/survey/gap/assessment 测试族 + 报表脱敏测试）按全仓口径豁免——域模块测试不声明 `init-database-data`，种子不入装载路径（M1.2b §⑤ 先例）；hr 预存回归 `TestErpHrDepartmentPositionDeleteGuard` 与批内表 `erp_hr_position` 同面关系显式登记（预存失败，本批不改其行为，见 Non-Goals）。
- **保护区域**：HR 薪酬/合同敏感字段触 auth/permissions 区域 = **plan-first**（roadmap 横切关注点 1 + `ai-autonomy-policy.md` 保护区域表 `auth/permissions` 行）——本批为 CSV 数据行落地 + E4.2 既有脱敏范式消费，不改权限模型；owner doc = `docs/design/human-resource/`（域 owner docs）+ `docs/design/field-formatting-patterns.md` §9（F7 集权威源）+ E4.2 plan。plan 内显式「独立 plan-audit」checkbox 见 Phase 1。
- **Deferred 消费**：seed-data.md L352「CRM/CS/HR 域配置/执行链 seed」Deferred 之 HR 子集（salary / salary_item / leave / attendance / shift / competency / social_insurance）——本计划按 roadmap M1 全量覆盖口径消费该 Deferred（触发条件由全覆盖目标取代，沿 M1.2b 消费 L297 先例）。

## Goals

- 32 个 seed CSV 落地（hr 32，逐行消费规格表 hr 节），每实体最小可用数据集（行数 ≤ 20，按规格表建议行数与用例指示编码 P / N-TERM），FK 引用零悬空，HR 域主子表链路完整（规格表口径：子表 7——competency_level / development_plan_item / shift_assignment / survey_question / survey_response / survey_result / timesheet_line；survey_answer 按规格表为独立表，以 M0.1 规格表为准）。
- F7 PII 集 4 字段 + 批内薪酬金额列全部按脱敏伪值语义落地，零真实个人数据。
- `TestErpSeedDataIntegrity` CSV 基线常量按随批更新协议推进（基数以「218 + 已落地批次 CSV 数」为准，本批 +32），seed-data.md 对账表同步。
- `docs/design/human-resource/seed-data.md`「种子数据」owner doc 段落地（新文件，含 F7 PII 集 + 薪酬金额列脱敏注记）。

## Non-Goals

- 不修改任何 ORM 模型（保护区 auto + dual-agent-approval）——本计划纯 CSV-only 路径。
- 不修改任何生产 Java 代码（唯一 Java 触点 = `TestErpSeedDataIntegrity.java` 的 CSV 基线常量按内置协议更新）；不触碰 MaskHelper / 权限模型 / 薪酬模拟引擎逻辑（seed 为被动数据）。
- 不覆盖 crm / cs 及其余 M1.x 工作项的缺 seed 实体（归同批 N=1/N=3 与后续批次）；不触碰 finance 5 个缺 className 运行时实体。
- 不修改既有 4 个 hr CSV 文件（含 camelCase 文件名）的任何行。
- 不新增 HR 域 GL 凭证/业财一体 seed（seed-data.md L351 Deferred 维持——三域报表读域表/ar_ap_item 状态列非 GL）。
- 不新增 M2.x 像素断言、不做负向视觉断言、不做跨浏览器矩阵（roadmap Non-Goal）。
- 不处置 `TestErpHrDepartmentPositionDeleteGuard#testDeleteEmptyPositionAllowed` 预存回归（roadmap Non-Goal，successor 收尾）。
- 不做「seed 字段值与 owner doc 业务规则一致性」的逐字段回放测试（roadmap Non-Goal）。

## Task Route

- Type: `implementation-only change`
- Owner Docs: `docs/architecture/seed-data.md`（规格表 hr 节 + 对账表 + L352 Deferred）、`docs/backlog/comprehensive-test-data-and-visual-coverage-roadmap.md`（M1.3 行 + 横切关注点）、`docs/testing/e2e-runbook.md`（视觉方法论 + 快照重录协议）、`docs/design/human-resource/`（域 owner docs）+ `docs/design/field-formatting-patterns.md` §9（F7 PII 集权威源）
- Skill Selection Basis: roadmap M1.3 行指定 `nop-backend-dev`（数据资产须对齐实体/字典/列命名约定）；Proof 阶段运行测试套件与视觉 spec 属测试域，加载 `nop-testing`。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（fresh-DB 由 `./scripts/start-app.sh` 承载；`init-database-data: true` 已默认开启）。
- 回滚策略：seed CSV 为纯新增文件，回滚 = 删除本批新增文件 + 常量回拨 + git revert 文档变更；不涉及数据迁移。

## Execution Plan

> 执行顺序约束：本计划为同批三计划（N=1/2/3）之二；`EXPECTED_APP_ERP_CSV_COUNT` 与 seed-data.md 对账表为共享触点，若 N=1/N=3 先落地，常量基数以「218 + 已落地批次 CSV 数」为准，避免覆盖写。

### Phase 1 - Seed CSV authoring（32 CSV）

Status: completed
Targets: `app-erp-all/src/main/resources/_vfs/_init-data/`
Skill: `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: 无（M0.1 规格表 + M0.2 门禁已就绪；规格表 hr 节全部必填 FK 锚点均已 seed 或属本批）

- [x] 逐实体核对 ORM 表名与列（`module-hr` 域 `model/*.orm.xml` tableName / `code=`）后，按规格表 hr 节（32 行，逐行引用不复制）创建 32 个 CSV；子表 7（competency_level / development_plan_item / shift_assignment / survey_question / survey_response / survey_result / timesheet_line），头/独立表 25（survey_answer 按规格表为独立表——必填 FK 指向本批 response+question），以 M0.1 规格表为准（沿 M1.1b/M1.1c 措辞漂移登记先例）；行数与用例指示编码（P / N-TERM）按规格表逐行执行
      - Skill: `nop-backend-dev`
- [x] FK 闭环按规格表逐行落实：必填 FK 仅指向〔已seed〕（ErpHrEmployee）或〔本批〕新增行；可选 FK 留空或指向已 seed 行；禁止悬空引用；字典码 ∈ ORM dict
      - Skill: `nop-backend-dev`
- [x] **PII 敏感字段脱敏伪值落地**：对照 F7 PII 集权威源（`field-formatting-patterns.md` §9 + E4.2 字段表）逐表排查本批 32 表中的 PII 列，批内薪酬金额列保守纳入同纪律，seed 值使用确定性伪值（格式合法、明显非真实个人数据），owner doc 增脱敏注记；`sensitive-masking.visual.spec` 断言面零冲突核实
      - Skill: `nop-backend-dev`
- [x] 列头与格式对齐既有 CSV 约定：DB 列名大写下划线、省略审计列、ISO 日期、小写布尔、ID < 100000、静态日期；列集以 ORM/XMeta 生成的实体列为准（先抽样同域既有 CSV，如 `erp_hr_employee.csv`）；Proof: 列错误由 Phase 3 门禁首跑拦截（M1.1c/M1.2b 先例）
      - Skill: `nop-backend-dev`
- [x] **保护区域暂停协议（HR 敏感字段段 = plan-first，roadmap 执行机制 4）**：独立 plan-audit——独立子代理（fresh session）审查本计划 PII 脱敏伪值设计与 E4.2 范式 + `field-formatting-patterns.md` §9 F7 集的一致性，批准记录落盘本计划后方可落地含 PII 列的 CSV；非 PII 表不阻塞
      - Skill: none
- [x] Proof: 干扰面预分析（执行首项，产出落本计划执行证据）——`ErpHrReportBizModel` 读取面复证（Employee + SalarySimulationItemAdjustment + ar_ap_item，均不在本批）、10 个 `hr-*.action.spec` + `hr.list-value` 消费表 grep、`sensitive-masking.visual.spec` 断言面、`app-erp-all/_cases` 表引用面复证、面 1 豁免复证（`module-hr` 域测试不声明 `init-database-data`，种子不入装载路径；hr 预存回归 `TestErpHrDepartmentPositionDeleteGuard` 与批内 `erp_hr_position` 同面关系显式登记），判定零漂移预期或预判漂移用例清单
      - Skill: `nop-testing`

Exit Criteria:

- [x] 32 个 CSV 存在于 `_init-data/`，逐实体行数与用例指示编码符合规格表
- [x] 无悬空 FK：全部必填 FK 落在〔已seed〕∪〔本批〕集合内
- [x] PII 列零真实个人数据，脱敏注记在 owner doc 落地；HR 敏感字段段独立 plan-audit 批准记录落盘本计划
- [x] 干扰面预分析结论在案（零漂移预期清单或预判漂移用例清单）

### Phase 2 - 门禁常量 + owner doc 同步

Status: completed
Targets: `app-erp-all/src/test/java/io/nop/app/all/seed/TestErpSeedDataIntegrity.java`、`docs/architecture/seed-data.md`、`docs/design/human-resource/seed-data.md`
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 1

- [x] 按常量 javadoc 随批更新协议更新 `EXPECTED_APP_ERP_CSV_COUNT`：基数以「218 + 已落地批次 CSV 数」为准本批 +32，`EXPECTED_PLATFORM_CSV_COUNT = 4` 不变；不触碰 `EXPECTED_APP_ERP_ENTITY_COUNT` / `EXPECTED_MODEL_CLASSNAME_OMITTED_ENTITIES`
      - Skill: `nop-backend-dev`
- [x] 同步 `docs/architecture/seed-data.md`：对账表计数（有 seed +32，精确缺 seed −32）+ 新增 M1.3 批次增量行 + L352 HR 子集 Deferred 消费注记 + 快照重录义务节资产计数注记（以对账表批次链为准）
      - Skill: `nop-backend-dev`
- [x] 新建 `docs/design/human-resource/seed-data.md`「种子数据」段：逐实体 CSV 文件、行数、FK 闭环图（〔已seed〕/〔本批〕标注）、用例指示编码、negative 行语义（N-TERM 终态行）、F7 PII 集脱敏伪值注记
      - Skill: `nop-backend-dev`

Exit Criteria:

- [x] 常量与对账表数值一致，且与 `_init-data/` 实际 CSV 构成一致
- [x] `docs/design/human-resource/seed-data.md` owner doc 段落地且与实际 CSV 内容一致

### Phase 3 - Proof：门禁全绿 + 回归扫掠 + 运行时装载证明

Status: completed
Targets: 本仓库验证命令
Skill: `nop-testing`

- Item Types: `Proof`（6 项验证）+ `Add`（2 项行政收尾：日志条目 + roadmap 回写）
- Prereqs: Phase 1 + Phase 2

- [x] `mvn test -pl app-erp-all -Dtest=TestErpSeedDataIntegrity` 全绿（scope-pinning 不变 + 零孤儿 CSV + 新 CSV 行数 > 0 + 引用完整性零悬空白名单零增量）
      - Skill: `nop-testing`
- [x] `mvn test -pl app-erp-all` 对照基线 71/0/0/1 评估：若本批 seed 行引起集成快照漂移，按重录协议处置——良性内容增量 → 双面重录并在本计划追加「快照重录合规声明」段；行为破坏 → 先 Fix 再闭包；零漂移 → 登记与预分析结论的对账
      - Skill: `nop-testing`
- [x] 视觉快照双面义务核查：运行 HR 相关视觉 spec（`sensitive-masking.visual` / `ext-domains-child-table` / `ext-domains-list-filter` / `dashboards.visual` + `dashboards.snapshot` / `reports.visual` + `reports.snapshot`），实仓清单复核后如有额外 hr 相关 spec 一并纳入；若 seed 变更触发 DOM/像素漂移，按 `docs/testing/e2e-runbook.md` 视觉方法论双面（DOM + 像素）同步重录，并在本计划追加「快照重录合规声明」段（载体按 e2e-runbook §快照重录合规声明协议裁决落位）；禁止单面重录
      - Skill: `nop-testing`
- [x] E2E 数值断言联动评估：10 个 `hr-*.action.spec` + `hr.list-value` + HR 2 报表 value/smoke spec（payroll-simulation-comparison / employee-net-balance）及 Phase 1 预分析标记的消费面——有消费 → 同步期望值基线并登记
      - Skill: `nop-testing`
- [x] 运行时装载行为证明：`./scripts/start-app.sh restart`（fresh-DB）后 GraphQL `/r/{Entity}__findPage` 抽样 ≥ 6 张本批表（含主子表头 competency / development_plan / shift / survey / timesheet 中至少 3 组）findPage 行数 == CSV 行数
      - Skill: `nop-testing`
- [x] `bash docs/audits/nop-compliance-checker.sh` exit 0，对照 `docs/audits/compliance-baseline.md` §BASELINE 机器块核对（机器块 R2c: 1537 + 已登记 pre-existing 增量 R2c +5 / R2b +2 / R12a +1，沿 plan `2026-09-01-0527-1` 口径协议）零新增漂移（纯资源 + 测试常量变更，预期零漂移；若有漂移按失败模式速查开独立基线裁决）
      - Skill: `nop-testing`
- [x] `docs/logs/2026/09-{day}.md`（实际执行日）追加执行条目
      - Skill: none
- [x] 独立结束审计通过后回写 roadmap 工作项 M1.3 `ready` → `done`（含批次证据摘要，格式沿 M1.1/M1.2 批先例）
      - Skill: none

Exit Criteria:

- [x] TestErpSeedDataIntegrity 全绿且常量断言通过
- [x] `mvn test -pl app-erp-all` 与基线一致或更优；快照漂移处置已登记（含快照重录合规声明，若触发）
- [x] fresh-DB 重启后抽样表行数与 CSV 一致（真实装载 0 冲突 / 0 列映射错误）
- [x] compliance checker 零漂移；日志条目在位；roadmap 状态回写完成

## Execution Evidence

### Phase 1 干扰面预分析（2026-09-02 执行，Proof 首项产出）

1. **`ErpHrReportBizModel` 读取面复证**（实仓 `module-hr/erp-hr-service/.../report/ErpHrReportBizModel.java`）：读 `ErpHrEmployee` + `ErpHrSalarySimulationItemAdjustment` + `ErpMdPartner`/`ErpFinArApItem`——均不在本批 32 表，零交集。
2. **10 个 `hr-*.action.spec` + `hr.list-value` 消费面**：grep 实证零 `ErpHr*__findPage`/`erp_hr_*` 直接表引用；payroll / salary-simulation spec 自包含（自建 employee + contract + social insurance + tax config 后清理，`tests/e2e/business-actions/hr-payroll.action.spec.ts:27` / `hr-salary-simulation.action.spec.ts:33`），不消费本批 seed 值。
3. **`sensitive-masking.visual.spec` 断言面**：仅断言 `ErpHrEmployee` F7 4 字段 + 自建记录（`HR_MASK_CODE`，`bankAccountId→1` 指向既有 ErpMdBankAccount），与本批零冲突。
4. **【基线漂移修正】`app-erp-all/_cases` 消费面**：计划基线「`_cases` 零直接引用（2026-09-01 实证）」**已陈旧**——M5 批次新增 `TestErpC17HrSalaryPayment`（`nop.orm.init-database-data=true`，加载全局 seed）并**行为耦合本批 8 表**。逐链路实证 + 值中性化设计：
   - `PayrollCalculator.findActiveContract`（limit 1 无 orderBy 无 status 过滤）→ 契约：**本批 employment_contract 全部 emp1 行 monthlySalary=15000.00 / emp2 行=8000.00**（任一行被选中均同值——无 status/orderBy 过滤，DB 返回序不确定，值中性是主契约）；`runPayroll` 遍历 active employee（集合不变）且跳过已存在非作废薪酬 → 契约重复行无害。
   - `SocialInsuranceCalculator.findBase`（limit 1 无 orderBy）→ 契约：**本批 social_insurance_base emp1/emp2 行逐值镜像 C17 seedSocialBase（SHENZHEN / 15000.00 / 15000.00）**。
   - `findConfigs(cityCode)` 对该城市全部非 HOUSING_FUND 配置**求和** → 契约：**本批 social_insurance_config 仅用 SHANGHAI，禁用 SHENZHEN**。
   - `IncomeTaxCalculator.findTaxConfig`（eq year limit 1）→ 契约：**本批 tax_config 仅 year=2025**。
   - `sumSpecialDeduction`（eq employee+year+month (2026,7) 求和 verified 行）→ 契约：**本批 tax_special_deduction 仅 year=2025**。
   - `findPreviousCumulative`（eq employee+year=2026+month<7）→ 契约：**本批 salary 仅 year=2025**（且无 (emp,2026,7) 重复风险；posted=false / approveStatus=UNSUBMITTED / paymentStatus=PENDING 不触 xwf/过账面）。
   - `summarizeAttendance`（emp+2026-07 窗口）+ `sumUnpaidLeaveDays`（APPROVED SICK/PERSONAL 重叠窗口，写快照列 `UNPAID_LEAVE_DAYS`）→ 契约：**本批 attendance/leave_request/shift_assignment 日期冻结 2026-05；无 APPROVED SICK/PERSONAL 行落入 2026-07 窗口**（SICK=REJECTED / PERSONAL=CANCELLED）。
   - 快照面：`output/tables/*.csv` 为 `_chgType` 变更行机制（`A` = 测试会话内创建行），seed 于测试会话前装载不入快照（M1.2b 先例维持）；C17 `input/tables/*.csv` 为 header-only 空表，无 ID 冲突面。
5. **面 1 豁免复证**：`module-hr` 各模块 `src/test` grep `init-database-data|initDatabaseData` 零命中——域测试不装载种子。hr 预存回归 `TestErpHrDepartmentPositionDeleteGuard#testDeleteEmptyPositionAllowed` 与批内 `erp_hr_position` 同面关系维持登记（预存失败，本批不改其行为，Non-Goal）。
6. **结论：零漂移预期**（附上述 8 条值中性化契约；若 Phase 3 `mvn test -pl app-erp-all` 出现 hr 面漂移，按预判清单回溯本节契约逐条核对）。

### Phase 3 执行证据（2026-09-02）

1. **门禁**：`mvn test -pl app-erp-all -Dtest=TestErpSeedDataIntegrity` **4/4 全绿**（Tests run: 4, Failures: 0, Errors: 0, Skipped: 0，BUILD SUCCESS）——scope-pinning 363 + `EXPECTED_APP_ERP_CSV_COUNT = 269`（218 基点 + M1.2c 19 + 本批 32，同批 N=1 先落地按执行顺序约束以「218 + 已落地批次 CSV 数」为基数）+ 平台 4 + 零孤儿 CSV + 引用完整性零悬空白名单零增量。
2. **回归扫掠**：`mvn test -pl app-erp-all` **71/0/0/1 = M0.2 六批维持基线精确一致**（BUILD SUCCESS）——含 `TestErpC17HrSalaryPayment`（`nop.orm.init-database-data=true` 装载全局 seed 的行为耦合用例）全绿，Phase 1 干扰面预分析 8 条值中性化契约（employment_contract 逐值镜像 / social_insurance_base 镜像 / config 仅 SHANGHAI / tax 仅 2025 / salary 仅 2025 / attendance-leave-shift 冻结 2026-05）实测证成；**零集成快照漂移，快照重录未触发，与预分析结论对账一致**。
3. **运行时装载证明**：jar 重打包实证 36 hr CSV（32 本批 snake_case + 4 既有）嵌入 runner；`./scripts/start-app.sh restart` fresh-DB 12s ready，app.log 复核 273 CSV 全量装载、hr 36/36、零冲突 / 零列映射错误；GraphQL `/r/{Entity}__findPage` 抽样 **18/18 表 total == CSV 行数**：competency(3) / competency_level(4) / development_plan(3) / development_plan_item(4) / shift(3) / shift_assignment(4) / survey(2) / survey_question(3) / survey_response(3) / survey_answer(5) / timesheet(3) / timesheet_line(4) / employment_contract(3) / salary(2) / social_insurance_base(2) / tax_config(1) / recruitment(3) / leave_request(3)——主子表头 5 组全部在样（超 ≥3 组要求）。
4. **视觉快照双面义务核查——零漂移裁决，重录未触发**：
   - 运行环境纠偏：首测以 `start-app.sh` 最小 flag 服务器跑 5 spec 出现 22 失败跨域面（M1.1c 已登记先例：`SiteMapApi authCascadeUp` 环境失配非 seed 因素），改用 playwright config 内嵌全 `-D` flag webServer（`-Dquarkus.profile=test` + enforcement 栈 + fresh-DB 自启）后复跑。
   - 全 flag 服务器 11 spec 实跑（plan 列 8 + 实仓清单复核增补 3 个 hr 相关：`f12-page-structure`（ErpHrEmployee 页结构/档案 drawer）/ `sensitive-operation-confirmations`（ErpHrEmployee 删除引用阻断）/ `status-tag`（ErpHrLeaveRequest soft probe）+ `tree-entity-views`（ErpHrDepartment tree））：**73 passed / 1 skipped / 34 failed**。
   - 全绿面：dashboards.visual 10/10 + dashboards.snapshot 10/10（10 域看板像素基线零漂移）+ reports.visual 24/24 + reports.snapshot 6/6（24 域报表 DOM + 6 像素基线零漂移，含 HR 2 报表 value render：employee-net-balance / payroll-simulation-comparison）+ ext-domains-list-filter 全绿（含 hr Employee asideFilter/query）+ sensitive-masking logistics 半区。
   - **34 失败预存裁决（M1.2a2 移除种子对照实验先例同型）**：移除本批 32 CSV → 重建 jar（实测 4 hr CSV）→ 同 6 失败 spec 复跑 = **34 failed / 1 skipped / 9 passed，逐测试 diff 与带种子运行完全一致（IDENTICAL FAILURE SETS）**——失败面全部为交互式 drawer/dialog/list-probe 结构断言（ext-domains-child-table 5 / f12-page-structure 8 / sensitive-masking hr 1 / sensitive-operation-confirmations 4 / status-tag hard-probe 8 / tree-entity-views 8，跨 pur/sal/fin/md/mfg/qa/cs/logistics/b2b/contract/drp 全域），与 M1.2a1 执行期登记的「flux 迁移期交互面结构断言漂移（同族 readonly/tree-entity 8 失败佐证，Follow-up 归 successor）」同族；本批 seed 变更零 DOM/像素漂移，**双面重录义务未触发，无快照重录合规声明段（未触发即合规）**。
5. **E2E 数值断言联动**：`hr.list-value` + `hr.smoke` + 10 个 `hr-*.action.spec`（assessment-dev-plan / leave-attendance / leave-shift-linkage / payroll / recruitment / salary-simulation / shift-assignment / shift-rotation / shift-swap / transfer）**38/38 全绿**（4.8m）——零期望值基线调整，预分析「自包含消费面不触本批 seed 值」实测证成；HR 2 报表 value render 在 reports.visual 内 2/2 绿。
6. **compliance checker**：**exit 0 零新增漂移**——实测 R1a=0/R1b=0/R1c=0/R1d=14/R2a=34/R2b=242/R2c=1542/R2d=38/R3=5/R4=0/R5=0/R6=2/R7=0/R8=0/R10=14/R11=0/R12a=71/R12b=66/R12c=42，与 §BASELINE 机器块逐值一致（R2c 1537+5 / R2b 240+2 / R12a 70+1 = 已登记 pre-existing 增量，沿 plan `2026-09-01-0527-1` 口径协议）；纯资源 + 测试常量 + docs 变更，符合零漂移预期。
7. **行政收尾**：日志条目 `docs/logs/2026/09-02.md` 追加在案；执行期 1 处 owner doc 勘误（survey_answer 行数 4→5，实仓 5 行与 §5 聚合口径本自洽，Phase 2 表格笔误即改）；roadmap M1.3 回写 `done` 于独立结束审计 APPROVE 后执行（下方 Closure 段）。

### Phase 1 保护区暂停协议——独立 plan-audit 批准记录（2026-09-02）

- Auditor / Agent: 独立子代理 fresh session `ses_fa1ca7ed6ffeJjneNygHBvCCiX`（read-only 审计，未复用执行者上下文）
- Evidence: 20 项实仓核验全 pass（F7 列仅在 ErpHrEmployee / 批内 32 表零 F7 列 / recruitment PII 邻接列伪值设计 / 薪酬金额列保守范围逐列吻合 / E4.2 零 seed 值纪律冲突 / owner doc 脱敏注记义务在位 / C17 干扰面 8 条契约逐条代码级复核）
- **VERDICT: APPROVED**——含 PII 列 CSV 准予落地。
- 非阻塞残留风险登记：R1 批内 employment_contract 与 social_insurance_base 的 emp1/emp2 行**全部行逐值镜像**（CSV 编写不变量，执行遵守）；R2 recruitment 候选人三列不在 E3.1/E4.2 masking 面，伪值纪律为唯一保护（owner doc 注记 load-bearing）；R3 伪值格式约定（张应聘 / 1380000102x / xxx-empNNN@example.com / example.com 保留域）写入 Phase 2 owner doc 使可重放；R4 已随批准修正执行证据括注措辞。

## Draft Review Record

- Independent draft review iteration 1: needs revision（独立子代理 fresh session `ses_fa281516affeRW4f02AGuAPbNF`）——0 Blocker + 1 Major + 4 Minor。Major M-1：F7 PII 集权威源指认失实——单一真相源为 `docs/design/field-formatting-patterns.md` §9（hr 4 字段）+ E4.2 字段表（+taxFileNo），`salaryAmount` 非 F7 成员（roadmap 措辞继承漂移），且 `docs/design/human-resource/` 下零 F7/PII 命中；已改为权威源指认 + 薪酬金额列保守纳入口径。Minor m-1：survey_answer 按规格表为独立表（子表 7/独立 25 而非「survey+4 子/独立 19」），已按 M1.1b/M1.1c 先例登记「以规格表为准」；m-2：面 1（module-hr 域测试）豁免依据与 hr 预存回归同面关系登记已补入干扰面段与预分析项；m-3：快照重录合规声明载体按 e2e-runbook 协议裁决落位注记已补；m-4（Classification 枚举外值 `consumed by this plan`）沿 M1.2b 双审计通过先例维持不改；m-5 无需修改。审查者实仓核验 15 项（hr 4 文件含 camelCase/常量/规格表 32 行 1:1/FK 锚点/报表读取面/e2e 三面/E4.2 plan 在位/_cases 零命中/known-good-baselines hr 登记/F7 溯源等）全部记录在案。
- Independent draft review iteration 2: accept（独立子代理 fresh session `ses_fa272eff0ffeMTeW5j3DGHS0Qn`）——6/6 修订核验项全部通过（F7 权威源六处改齐零死指针残留、§9 4 字段/taxFileNo 实仓吻合、主子表口径两处一致且与规格表 1:1、面 1 豁免 + 同面关系登记在案、快照载体注记对应 runbook L931 实证、Review Record 完整、反松弛零命中、基线锚点未被误改），无新引入缺陷。共识达成，转 `active`。

## Closure Gates

> 全量仓库验证（`mvn clean install -DskipTests` / 全 reactor `mvn test`）不在本计划闭包门控内：本计划变更为资源 + 单测试常量 + docs，`mvn test -pl app-erp-all` + compliance checker 已覆盖变更面；全量基线登记归 M3.1 兜底工作项。

- [x] 范围内行为完成（32 CSV + 常量 + 对账表 + owner doc 段）
- [x] 相关文档对齐（seed-data.md 对账表/快照重录注记/L352 消费注记 + human-resource seed-data.md；若快照重录触发，e2e-runbook 协议遵循已登记）
- [x] 已运行验证（Phase 3 全部 Proof 项，含 compliance checker 复跑）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

- **seed-data.md L352「CRM/CS/HR 域配置/执行链 seed」Deferred 之 HR 子集**：本计划 Phase 1 全量覆盖口径消费（触发条件由 roadmap M1 全量覆盖目标取代，沿 M1.2b 消费 L297 先例）；消费注记由 Phase 2 回写 seed-data.md。GL 凭证/业财一体子集（L351）不在本计划范围，维持 Deferred。
  - Classification: `consumed by this plan`（仅 HR 配置/执行链子集）
  - Why Not Blocking Closure: 非阻塞项——为消费登记而非遗留债务；L351 子集继续由其自身触发条件管辖。
  - Successor Required: `no`（L351 子集维持原 Deferred，不归本计划）

（其余待执行期裁定：若规格表某行证伪 CSV-only 可满足性，按反松弛规则移入本节分类登记）

## Closure

Status Note: 计划可闭包——32 seed CSV（54 行，hr 域 4/36→36/36 全覆盖）+ 门禁常量 237→269 + seed-data.md 对账表（有 seed 269 / 精确缺 99 / 273 CSV + 1 SQL）+ `docs/design/human-resource/seed-data.md` owner doc 落地（含 F7 脱敏注记与 C17 值中性化契约节，结束审计 m-1 文件名措辞勘误已落实）。验证终态：`TestErpSeedDataIntegrity` 4/4 全绿、`mvn test -pl app-erp-all` 71/0/0/1 = M0.2 六批维持基线精确一致（C17 值中性化契约实测证成，零快照漂移，快照重录双面义务未触发）、fresh-DB 装载 18/18 抽样表 findPage==CSV 行数（5 组主子表头全在样）、视觉双面 dashboards 10+10 / reports 24+6 零漂移 + 34 失败经移除种子对照实验（IDENTICAL FAILURE SETS）裁决预存 flux 迁移期交互面漂移归 successor、hr list-value/smoke/action 38/38 零联动、compliance checker exit 0 R2c=1542 零新增漂移。HR 敏感字段段保护区义务（plan-first + 独立 plan-audit）、独立草案审查（2 轮收敛）、独立结束审计三重证据齐备。roadmap M1.3 已回写 `done`。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话，未复用执行者上下文）
- Evidence: task `ses_fa0cf1740ffeWCQG2irNGbptF2`——VERDICT **APPROVE**（0 Blocker / 0 Major / 1 Minor）。审计 A..I 九项全 pass 实仓核验：①计划一致性（3 Phase completed、Evidence 三段在案、报表读取面与域测试豁免 grep 复证）；②32/32 CSV 行数与 owner doc §1 逐一吻合 + 12 文件全文核验（列头/静态日期/ID<100000）+ FK 边抽查 9 条闭环 + PII 伪值与 §3 约定一致；③常量 269 与实仓 273 CSV 独立计数双证 + 对账表全链自洽（93+12+16+12+31+26+28+19+32=269 求和复核）；④owner doc C17 契约 7/7 与 CSV 实值核对 + §5 数值自洽复核；⑤验证两项独立复跑精确复现（gate 4/4 + checker exit 0 R2c=1542/R2b=242/R12a=71）；⑥Phase 3 证据 7 项内部自洽（18/18 行数与实测逐一吻合、对照实验逻辑成立、日志条目对应）；⑦roadmap 审计时点仍为 `ready`（回写协议时序正确）；⑧Closure Gates 执行者零预勾 + Draft Review Record 两轮在案；⑨反松弛（Deferred 仅 L352 HR 子集消费登记、git 变更面与计划范围精确一致、既有 4 hr CSV/ORM/生产 Java 零触碰）。Minor m-1（既有 4 文件名实为 `erp_hr_salary_simulation*.csv` 全小写非 camelCase，纯描述性漂移）已由执行者随闭包落实勘误（plan Current Baseline + owner doc §1 两处）。

Follow-up:

- （无——预存 flux 迁移期交互面视觉漂移 34 处已在 M1.2a1 批次登记 Follow-up 归 successor，非本计划新增义务）

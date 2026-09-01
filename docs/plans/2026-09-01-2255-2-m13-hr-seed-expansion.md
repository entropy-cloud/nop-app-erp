# 2026-09-01-2255-2 M1.3 HR 域 seed 扩面（32 CSV）

> Plan Status: active
> Last Reviewed: 2026-09-01
> Source: `docs/backlog/comprehensive-test-data-and-visual-coverage-roadmap.md` 工作项 M1.3（ready，Deps M0.1 + M0.2 均 done + M0.3 done 弱依赖已满足）
> Related: `docs/plans/2026-09-01-0301-1-m01-seed-scope-adjudication.md`（M0.1，规格表权威源）、`docs/plans/2026-09-01-0527-1-m02-seed-gate-hardening.md`（M0.2，门禁常量随批更新协议）、`docs/plans/2026-08-11-1030-1`（E4.2 MaskHelper 敏感字段脱敏范式，本批 PII 纪律来源）、同批 `2026-09-01-2255-1-m12c-ast-notify-seed-expansion.md`（N=1）/ `2026-09-01-2255-3-m14a-crm-cs-seed-expansion.md`（N=3）
> Audit: required

## Current Baseline

- 实仓 363 个 `app.erp.*` 实体（className 口径；运行时注册 368 = 363 + finance 5 缺 className 补充档，本域不涉及），218 个有 seed（`_init-data/` 实测 222 CSV = 218 app.erp + 4 平台，+ 1 SQL）；`TestErpSeedDataIntegrity` 基线常量 `EXPECTED_APP_ERP_CSV_COUNT = 218` / `EXPECTED_PLATFORM_CSV_COUNT = 4` / `EXPECTED_APP_ERP_ENTITY_COUNT = 363`（+ finance 5 缺 className 补充集，非本域不触碰）。
- hr 域实测已 seed 4（department / employee / SalarySimulation / SalarySimulationItemAdjustment，1045-1），缺 **32**（assessment_detail / attendance / competency / competency_level / development_plan / development_plan_item / employee_assessment / employment_contract / gap_analysis / leave_balance / leave_request / payroll_bank_file / position / recruitment / role_competency / salary / salary_item / shift / shift_assignment / shift_rotation_pattern / shift_swap_request / social_insurance_base / social_insurance_config / survey / survey_answer / survey_question / survey_response / survey_result / tax_config / tax_special_deduction / timesheet / timesheet_line）。逐实体规格见 `docs/architecture/seed-data.md` 规格表 hr 节，本计划逐行引用不复制。
- **既有文件命名事实**：`erp_hr_SalarySimulation.csv` / `erp_hr_SalarySimulation_item_adj.csv` 为历史 camelCase/软缩写命名（1045-1 先例）——本批新增 32 文件一律按规格表 `<tableName>.csv` snake_case 命名（`DataInitInitializer.loadCsvData` 按表名查找契约），不改既有 4 文件名。
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

Status: planned
Targets: `app-erp-all/src/main/resources/_vfs/_init-data/`
Skill: `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: 无（M0.1 规格表 + M0.2 门禁已就绪；规格表 hr 节全部必填 FK 锚点均已 seed 或属本批）

- [ ] 逐实体核对 ORM 表名与列（`module-hr` 域 `model/*.orm.xml` tableName / `code=`）后，按规格表 hr 节（32 行，逐行引用不复制）创建 32 个 CSV；子表 7（competency_level / development_plan_item / shift_assignment / survey_question / survey_response / survey_result / timesheet_line），头/独立表 25（survey_answer 按规格表为独立表——必填 FK 指向本批 response+question），以 M0.1 规格表为准（沿 M1.1b/M1.1c 措辞漂移登记先例）；行数与用例指示编码（P / N-TERM）按规格表逐行执行
      - Skill: `nop-backend-dev`
- [ ] FK 闭环按规格表逐行落实：必填 FK 仅指向〔已seed〕（ErpHrEmployee）或〔本批〕新增行；可选 FK 留空或指向已 seed 行；禁止悬空引用；字典码 ∈ ORM dict
      - Skill: `nop-backend-dev`
- [ ] **PII 敏感字段脱敏伪值落地**：对照 F7 PII 集权威源（`field-formatting-patterns.md` §9 + E4.2 字段表）逐表排查本批 32 表中的 PII 列，批内薪酬金额列保守纳入同纪律，seed 值使用确定性伪值（格式合法、明显非真实个人数据），owner doc 增脱敏注记；`sensitive-masking.visual.spec` 断言面零冲突核实
      - Skill: `nop-backend-dev`
- [ ] 列头与格式对齐既有 CSV 约定：DB 列名大写下划线、省略审计列、ISO 日期、小写布尔、ID < 100000、静态日期；列集以 ORM/XMeta 生成的实体列为准（先抽样同域既有 CSV，如 `erp_hr_employee.csv`）；Proof: 列错误由 Phase 3 门禁首跑拦截（M1.1c/M1.2b 先例）
      - Skill: `nop-backend-dev`
- [ ] **保护区域暂停协议（HR 敏感字段段 = plan-first，roadmap 执行机制 4）**：独立 plan-audit——独立子代理（fresh session）审查本计划 PII 脱敏伪值设计与 E4.2 范式 + `field-formatting-patterns.md` §9 F7 集的一致性，批准记录落盘本计划后方可落地含 PII 列的 CSV；非 PII 表不阻塞
      - Skill: none
- [ ] Proof: 干扰面预分析（执行首项，产出落本计划执行证据）——`ErpHrReportBizModel` 读取面复证（Employee + SalarySimulationItemAdjustment + ar_ap_item，均不在本批）、10 个 `hr-*.action.spec` + `hr.list-value` 消费表 grep、`sensitive-masking.visual.spec` 断言面、`app-erp-all/_cases` 表引用面复证、面 1 豁免复证（`module-hr` 域测试不声明 `init-database-data`，种子不入装载路径；hr 预存回归 `TestErpHrDepartmentPositionDeleteGuard` 与批内 `erp_hr_position` 同面关系显式登记），判定零漂移预期或预判漂移用例清单
      - Skill: `nop-testing`

Exit Criteria:

- [ ] 32 个 CSV 存在于 `_init-data/`，逐实体行数与用例指示编码符合规格表
- [ ] 无悬空 FK：全部必填 FK 落在〔已seed〕∪〔本批〕集合内
- [ ] PII 列零真实个人数据，脱敏注记在 owner doc 落地；HR 敏感字段段独立 plan-audit 批准记录落盘本计划
- [ ] 干扰面预分析结论在案（零漂移预期清单或预判漂移用例清单）

### Phase 2 - 门禁常量 + owner doc 同步

Status: planned
Targets: `app-erp-all/src/test/java/io/nop/app/all/seed/TestErpSeedDataIntegrity.java`、`docs/architecture/seed-data.md`、`docs/design/human-resource/seed-data.md`
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 1

- [ ] 按常量 javadoc 随批更新协议更新 `EXPECTED_APP_ERP_CSV_COUNT`：基数以「218 + 已落地批次 CSV 数」为准本批 +32，`EXPECTED_PLATFORM_CSV_COUNT = 4` 不变；不触碰 `EXPECTED_APP_ERP_ENTITY_COUNT` / `EXPECTED_MODEL_CLASSNAME_OMITTED_ENTITIES`
      - Skill: `nop-backend-dev`
- [ ] 同步 `docs/architecture/seed-data.md`：对账表计数（有 seed +32，精确缺 seed −32）+ 新增 M1.3 批次增量行 + L352 HR 子集 Deferred 消费注记 + 快照重录义务节资产计数注记（以对账表批次链为准）
      - Skill: `nop-backend-dev`
- [ ] 新建 `docs/design/human-resource/seed-data.md`「种子数据」段：逐实体 CSV 文件、行数、FK 闭环图（〔已seed〕/〔本批〕标注）、用例指示编码、negative 行语义（N-TERM 终态行）、F7 PII 集脱敏伪值注记
      - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] 常量与对账表数值一致，且与 `_init-data/` 实际 CSV 构成一致
- [ ] `docs/design/human-resource/seed-data.md` owner doc 段落地且与实际 CSV 内容一致

### Phase 3 - Proof：门禁全绿 + 回归扫掠 + 运行时装载证明

Status: planned
Targets: 本仓库验证命令
Skill: `nop-testing`

- Item Types: `Proof`（6 项验证）+ `Add`（2 项行政收尾：日志条目 + roadmap 回写）
- Prereqs: Phase 1 + Phase 2

- [ ] `mvn test -pl app-erp-all -Dtest=TestErpSeedDataIntegrity` 全绿（scope-pinning 不变 + 零孤儿 CSV + 新 CSV 行数 > 0 + 引用完整性零悬空白名单零增量）
      - Skill: `nop-testing`
- [ ] `mvn test -pl app-erp-all` 对照基线 71/0/0/1 评估：若本批 seed 行引起集成快照漂移，按重录协议处置——良性内容增量 → 双面重录并在本计划追加「快照重录合规声明」段；行为破坏 → 先 Fix 再闭包；零漂移 → 登记与预分析结论的对账
      - Skill: `nop-testing`
- [ ] 视觉快照双面义务核查：运行 HR 相关视觉 spec（`sensitive-masking.visual` / `ext-domains-child-table` / `ext-domains-list-filter` / `dashboards.visual` + `dashboards.snapshot` / `reports.visual` + `reports.snapshot`），实仓清单复核后如有额外 hr 相关 spec 一并纳入；若 seed 变更触发 DOM/像素漂移，按 `docs/testing/e2e-runbook.md` 视觉方法论双面（DOM + 像素）同步重录，并在本计划追加「快照重录合规声明」段（载体按 e2e-runbook §快照重录合规声明协议裁决落位）；禁止单面重录
      - Skill: `nop-testing`
- [ ] E2E 数值断言联动评估：10 个 `hr-*.action.spec` + `hr.list-value` + HR 2 报表 value/smoke spec（payroll-simulation-comparison / employee-net-balance）及 Phase 1 预分析标记的消费面——有消费 → 同步期望值基线并登记
      - Skill: `nop-testing`
- [ ] 运行时装载行为证明：`./scripts/start-app.sh restart`（fresh-DB）后 GraphQL `/r/{Entity}__findPage` 抽样 ≥ 6 张本批表（含主子表头 competency / development_plan / shift / survey / timesheet 中至少 3 组）findPage 行数 == CSV 行数
      - Skill: `nop-testing`
- [ ] `bash docs/audits/nop-compliance-checker.sh` exit 0，对照 `docs/audits/compliance-baseline.md` §BASELINE 机器块核对（机器块 R2c: 1537 + 已登记 pre-existing 增量 R2c +5 / R2b +2 / R12a +1，沿 plan `2026-09-01-0527-1` 口径协议）零新增漂移（纯资源 + 测试常量变更，预期零漂移；若有漂移按失败模式速查开独立基线裁决）
      - Skill: `nop-testing`
- [ ] `docs/logs/2026/09-{day}.md`（实际执行日）追加执行条目
      - Skill: none
- [ ] 独立结束审计通过后回写 roadmap 工作项 M1.3 `ready` → `done`（含批次证据摘要，格式沿 M1.1/M1.2 批先例）
      - Skill: none

Exit Criteria:

- [ ] TestErpSeedDataIntegrity 全绿且常量断言通过
- [ ] `mvn test -pl app-erp-all` 与基线一致或更优；快照漂移处置已登记（含快照重录合规声明，若触发）
- [ ] fresh-DB 重启后抽样表行数与 CSV 一致（真实装载 0 冲突 / 0 列映射错误）
- [ ] compliance checker 零漂移；日志条目在位；roadmap 状态回写完成

## Draft Review Record

- Independent draft review iteration 1: needs revision（独立子代理 fresh session `ses_fa281516affeRW4f02AGuAPbNF`）——0 Blocker + 1 Major + 4 Minor。Major M-1：F7 PII 集权威源指认失实——单一真相源为 `docs/design/field-formatting-patterns.md` §9（hr 4 字段）+ E4.2 字段表（+taxFileNo），`salaryAmount` 非 F7 成员（roadmap 措辞继承漂移），且 `docs/design/human-resource/` 下零 F7/PII 命中；已改为权威源指认 + 薪酬金额列保守纳入口径。Minor m-1：survey_answer 按规格表为独立表（子表 7/独立 25 而非「survey+4 子/独立 19」），已按 M1.1b/M1.1c 先例登记「以规格表为准」；m-2：面 1（module-hr 域测试）豁免依据与 hr 预存回归同面关系登记已补入干扰面段与预分析项；m-3：快照重录合规声明载体按 e2e-runbook 协议裁决落位注记已补；m-4（Classification 枚举外值 `consumed by this plan`）沿 M1.2b 双审计通过先例维持不改；m-5 无需修改。审查者实仓核验 15 项（hr 4 文件含 camelCase/常量/规格表 32 行 1:1/FK 锚点/报表读取面/e2e 三面/E4.2 plan 在位/_cases 零命中/known-good-baselines hr 登记/F7 溯源等）全部记录在案。
- Independent draft review iteration 2: accept（独立子代理 fresh session `ses_fa272eff0ffeMTeW5j3DGHS0Qn`）——6/6 修订核验项全部通过（F7 权威源六处改齐零死指针残留、§9 4 字段/taxFileNo 实仓吻合、主子表口径两处一致且与规格表 1:1、面 1 豁免 + 同面关系登记在案、快照载体注记对应 runbook L931 实证、Review Record 完整、反松弛零命中、基线锚点未被误改），无新引入缺陷。共识达成，转 `active`。

## Closure Gates

> 全量仓库验证（`mvn clean install -DskipTests` / 全 reactor `mvn test`）不在本计划闭包门控内：本计划变更为资源 + 单测试常量 + docs，`mvn test -pl app-erp-all` + compliance checker 已覆盖变更面；全量基线登记归 M3.1 兜底工作项。

- [ ] 范围内行为完成（32 CSV + 常量 + 对账表 + owner doc 段）
- [ ] 相关文档对齐（seed-data.md 对账表/快照重录注记/L352 消费注记 + human-resource seed-data.md；若快照重录触发，e2e-runbook 协议遵循已登记）
- [ ] 已运行验证（Phase 3 全部 Proof 项，含 compliance checker 复跑）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

- **seed-data.md L352「CRM/CS/HR 域配置/执行链 seed」Deferred 之 HR 子集**：本计划 Phase 1 全量覆盖口径消费（触发条件由 roadmap M1 全量覆盖目标取代，沿 M1.2b 消费 L297 先例）；消费注记由 Phase 2 回写 seed-data.md。GL 凭证/业财一体子集（L351）不在本计划范围，维持 Deferred。
  - Classification: `consumed by this plan`（仅 HR 配置/执行链子集）
  - Why Not Blocking Closure: 非阻塞项——为消费登记而非遗留债务；L351 子集继续由其自身触发条件管辖。
  - Successor Required: `no`（L351 子集维持原 Deferred，不归本计划）

（其余待执行期裁定：若规格表某行证伪 CSV-only 可满足性，按反松弛规则移入本节分类登记）

## Closure

Status Note: （闭包时填写）

Closure Audit Evidence:

- Auditor / Agent: （独立结束审计时填写）
- Evidence: （task id / 实仓核验记录）

Follow-up:

- （仅非阻塞跟进项；已确认的缺陷不得出现在此处）

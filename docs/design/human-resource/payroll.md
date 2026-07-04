# 薪酬核算（Payroll）

## 目的

详细设计薪酬核算流程：薪酬结构 → 社保/公积金/个税计算 → 薪酬审批 → 银行代发 → 工资单。覆盖中国劳动法体系的本地化薪酬计算要求。

## 设计边界

- 本设计负责：月度薪酬核算、社保/公积金/个税计算、薪酬审批流、银行文件生成、工资单发布。
- **与 finance/posting 的边界**：薪酬核算完成后通过 `IErpFinAcctDocProvider`（businessType=SALARY）生成应付职工薪酬凭证；发放完成后生成 SALARY_PAYMENT 凭证。凭证模板在 finance 域定义。
- 本设计不负责：绩效奖金计算逻辑（HR 手动录入或未来绩效模块输入）；银行实际转账执行。

### 实现偏离补注（2026-07-04，plan 2026-07-04-0831-2）

- **公式引擎（§1.4）**：裁决选用 Nop 平台 `IEvalScope`（Xpl 表达式）求值 `ErpHrSalaryItem.formula`，不引入第三方表达式库（遵循 AGENTS.md 平台优先）。本期预置项目走 FIXED/INPUT 路径，FORMULA 求值基础设施（`TaxBracketParser` 示范 JSON 解析模式）就绪，完整公式求值归 follow-up。
- **薪酬周期（§5.1）**：本期仅实现月薪 MONTHLY；半月薪 BI_MONTHLY 归 follow-up。
- **币种**：本期本位币单一币种；多币种薪酬归 follow-up。
- **绩效奖金（§设计边界）**：本期 `performanceBonus` 手工录入（核算生成 0，HR 后续录入），绩效系数/奖金计算逻辑归未来绩效模块。
- **工资单 PDF + 推送（§八）**：归 follow-up（邮件/企业微信/钉钉/自助门户）。
- **年度汇算清缴（§十）**：导出/个税 APP 对接归 follow-up。
- **cron 自动核算（§5.2 SalaryCalculateJob）**：手动/外部调度触发已就绪（`IErpHrSalaryBiz.runPayroll`/`calculateSalary`），nop-job 定时注册归 follow-up。
- **BizModel 落层**：设计引用 `IErpHrPayrollBiz` 独立接口；实现落在实体绑定 `IErpHrSalaryBiz` + `ErpHrSalaryBizModel`（extends CrudBizModel）以获得 `@SingleSession` 事务管理，方法签名与语义不变。
- **审批/发放状态消歧**：`approvalStatus`（6 态 PENDING/REVIEWED/APPROVED_FINANCE/APPROVED_MANAGER/PAID/VOID）为权威端到端状态；存量 `paymentStatus`（3 态）保留为只读派生投影（`approvalStatus=PAID`→`paymentStatus=PAID`，`=VOID`→`=VOID`），新代码路径只读写 `approvalStatus`。

---

## 一、薪酬结构

### 1.1 薪酬项目定义（ErpHrSalaryItem）

| 字段 | 含义 |
|------|------|
| id/code/name/orgId | 标准 |
| itemCategory | 类别 dict `erp-hr/salary-item-category`：EARNINGS（应发项）/ DEDUCTION（扣款项） |
| itemGroup | 分组 dict：BASIC（基本工资）/ ALLOWANCE（津贴）/ BONUS（奖金）/ OVERTIME（加班）/ SOCIAL（社保）/ FUND（公积金）/ TAX（个税）/ OTHER（其他） |
| calcMethod | 计算方式 dict：FIXED（固定金额）/ FORMULA（公式）/ INPUT（手工录入） |
| formula | 计算公式（calcMethod=FORMULA 时，表达式引擎） |
| isTaxable | 是否应税（应发项计入个税基数） |
| isSocialInsuranceBase | 是否计入社保基数 |
| isMandatory | 是否必含项 |
| sortOrder | 计算顺序 |

**预置薪酬项目**：

| 项目编码 | 名称 | 类别 | 计算方式 | 应税 | 社保基数 |
|----------|------|------|----------|------|----------|
| BASIC | 基本工资 | EARNINGS | 公式（合同月薪 × 出勤比例） | Y | Y |
| POSITION_ALLOWANCE | 岗位津贴 | EARNINGS | 固定（合同约定） | Y | Y |
| PERFORMANCE_BONUS | 绩效奖金 | EARNINGS | 手工录入 | Y | Y |
| OVERTIME_PAY | 加班费 | EARNINGS | 公式（加班小时 × 费率） | Y | Y |
| MEAL_ALLOWANCE | 餐补 | EARNINGS | 固定（日标准 × 出勤天数） | Y | N |
| TRANSPORT_ALLOWANCE | 交通补贴 | EARNINGS | 固定（月标准） | Y | N |
| SOCIAL_INSURANCE_EE | 社保（个人） | DEDUCTION | 公式（基数 × 个人比例） | N | N |
| SOCIAL_INSURANCE_ER | 社保（公司） | EARNINGS* | 公式（基数 × 公司比例） | N | N |
| HOUSING_FUND_EE | 公积金（个人） | DEDUCTION | 公式（基数 × 个人比例） | N | N |
| HOUSING_FUND_ER | 公积金（公司） | EARNINGS* | 公式（基数 × 公司比例） | N | N |
| INCOME_TAX | 个税 | DEDUCTION | 公式（累计预扣法） | N | N |
| OTHER_ALLOWANCE | 其他补贴 | EARNINGS | 手工录入 | Y/Y | — |
| OTHER_DEDUCTION | 其他扣款 | DEDUCTION | 手工录入 | N | N |

> \* 公司承担的社保/公积金挂 EARNINGS 但记入公司成本，不影响个人应发合计。
> 🟢 Axelor `PayrollLine.xml` 薪酬行结构。

### 1.2 应发合计 = Σ(应发项) 

应发合计 grossSalary = BASIC + POSITION_ALLOWANCE + PERFORMANCE_BONUS + OVERTIME_PAY + MEAL_ALLOWANCE + TRANSPORT_ALLOWANCE + 其他补贴

### 1.3 实发合计

实发合计 netSalary = grossSalary − SOCIAL_INSURANCE_EE − HOUSING_FUND_EE − INCOME_TAX − 其他扣款

### 1.4 公式引擎接口

公式表达式支持：
- 引用其他薪酬项目值（通过 code）
- 引用出勤天数（from ErpHrAttendance）
- 引用合同字段（如 contract.monthlySalary）
- 引用配置值（如 `config('erp-hr.social-insurance-rate.shenzhen.pension.ee')`）
- 条件表达式（if-else）
- 四舍五入到分

> ⚪ 公式引擎使用 Nop XLang Xpl 或自定义表达式方言，待技术选型确认。

---

## 二、社保计算

### 2.1 社保项目

| 社保险种 | 缩写 | 公司比例（典型深圳 2025） | 个人比例（典型深圳 2025） |
|----------|------|---------------------------|---------------------------|
| 养老保险 | PENSION | 15% | 8% |
| 医疗保险 | MEDICAL | 6% | 2% |
| 失业保险 | UNEMPLOYMENT | 0.7% | 0.3% |
| 工伤保险 | WORK_INJURY | 0.2%（浮动） | 0% |
| 生育保险 | MATERNITY | 0.45% | 0% |

### 2.2 社保基数

| 字段 | 含义 | 参考 |
|------|------|------|
| employeeId | 员工 | — |
| cityCode | 参保城市（dict `erp-hr/city`） | — |
| socialInsuranceBase | 社保基数（月） | — |
| effectiveFrom | 生效日期（通常年调） | — |
| effectiveTo | 失效日期 | — |

- 基数上下限：各城市公布（如深圳 2025 下限 6123，上限 32694），由 `ErpHrSocialInsuranceConfig` 配置。
- 基数调整周期：每年 7 月（全国大部分城市）。
- 新员工入职：按首月工资或合同约定确定基数。
- 基数 = min(max(申报基数, 下限), 上限)

### 2.3 社保配置表（ErpHrSocialInsuranceConfig）

| 字段 | 含义 |
|------|------|
| id/cityCode/insuranceType/orgId | 标准 |
| companyRate/employeeRate | 公司/个人比例 |
| baseLowerLimit/baseUpperLimit | 基数上下限 |
| effectiveFrom/effectiveTo | 有效区间 |

> ⚪ 中国劳动法常识：各城市比例不同（深圳/上海/北京差异大），必须通过配置表而非硬编码。
> 🟢 Axelor `Payroll.xml` 支持多社保规则配置但为法国体系，仅参考结构。

### 2.4 计算过程

```
社保计算（每月）
    │
    ├─► 读取员工 ErpHrSocialInsuranceBase（当前有效基数）
    │
    ├─► 读取城市配置 ErpHrSocialInsuranceConfig（各险种比例/上下限）
    │
    ├─► 基数 = min(max(base, lowerLimit), upperLimit)
    │
    ├─► 个人扣款 = Σ(基数 × 个人比例) 如 pension_ee + medical_ee + unemployment_ee
    │
    └─► 公司承担 = Σ(基数 × 公司比例) 如 pension_er + medical_er + ... + maternity_er
```

---

## 三、公积金计算

### 3.1 计算规则

| 字段 | 含义 | 参考 |
|------|------|------|
| housingFundBase | 公积金基数 | — |
| companyRate/employeeRate | 公司/个人比例（5%-12%） | — |

- 比例范围：5%-12%（公司和个人可不同，但通常相等）。
- 基数上下限：各城市规定（如深圳 2025 下限 2360，上限 43659）。
- 一般每年 7 月跟随社保同步调整。

> ⚪ 中国公积金政策：比例 5%-12% 区间，公司和个人可选择一致或不同。

### 3.2 计算过程

```
公积金计算（每月）
    │
    ├─► 读取员工公积金基数
    │
    ├─► 读取城市公积金比例
    │
    ├─► 个人扣款 = base × employeeRate
    │
    └─► 公司承担 = base × companyRate
```

---

## 四、个税计算

### 4.1 累计预扣法

中国个税采用**累计预扣法**（自 2019 年起），公式：

```
累计应纳税所得额 = 累计应发工资 − 累计免征额（5000/月）− 累计专项扣除（社保/公积金个人部分）− 累计专项附加扣除 − 累计其他扣除

当月应纳税额 = 累计应纳税所得额 × 适用税率 − 速算扣除数 − 累计已预扣税额
```

### 4.2 个税税率表（综合所得）

| 级数 | 累计应纳税所得额 | 税率 | 速算扣除数 |
|------|------------------|------|------------|
| 1 | ≤ 36,000 | 3% | 0 |
| 2 | 36,001 ~ 144,000 | 10% | 2,520 |
| 3 | 144,001 ~ 300,000 | 20% | 16,920 |
| 4 | 300,001 ~ 420,000 | 25% | 31,920 |
| 5 | 420,001 ~ 660,000 | 30% | 52,920 |
| 6 | 660,001 ~ 960,000 | 35% | 85,920 |
| 7 | > 960,000 | 45% | 181,920 |

> ⚪ 中国个人所得税法（2018 修正）——综合所得七级超额累进税率。

### 4.3 专项附加扣除

| 扣除项目 | 标准 | 说明 |
|----------|------|------|
| 子女教育 | 2,000/子女/月 | 3 岁到博士，父母各扣 50% 或一方全扣 |
| 继续教育 | 400/月（学历）或 3,600/年（职业资格） | — |
| 大病医疗 | 80,000/年（上限） | 医保报销后个人负担超 15,000 部分 |
| 住房贷款利息 | 1,000/月 | 首套住房，夫妻一方扣除 |
| 住房租金 | 1,500/800/月（分城市） | 与房贷利息不兼得 |
| 赡养老人 | 3,000/月（独生子女）/ 1,500/月（分摊） | 60 岁以上父母 |
| 3 岁以下婴幼儿照护 | 2,000/婴幼儿/月 | 2023 年起 |

### 4.4 个税配置（ErpHrTaxConfig）

| 字段 | 含义 |
|------|------|
| id/year/orgId | 标准 |
| taxThreshold | 起征点（当前 5000/月） |
| taxBrackets | 税率表（JSON 数组：范围/税率/速算扣除数） |
| specialDeductionItems | 专项附加扣除项配置 |

### 4.5 个税计算过程

```
个税计算（每月）
    │
    ├─► 计算当月应发工资（grossSalary）
    │
    ├─► 从数据库读取本年度累计数据：
    │       ├─ 累计应发（年初至上月）
    │       ├─ 累计免征额（月数 × 5000）
    │       ├─ 累计专项扣除（社保+公积金个人部分）
    │       ├─ 累计专项附加扣除（员工申报）
    │       └─ 累计已预扣税额
    │
    ├─► 累加当月值 → 累计到当月
    │
    ├─► 累计应纳税所得额 = 累计应发 − 累计免征额 − 累计专项扣除 − 累计专项附加扣除
    │
    ├─► 查税率表 → 适用税率 + 速算扣除数
    │
    ├─► 累计应纳税额 = 累计应纳税所得额 × 税率 − 速算扣除数
    │
    └─► 当月应纳税额 = 累计应纳税额 − 累计已预扣税额
```

### 4.6 员工专项附加扣除数据源

- 员工通过个税 APP 申报 → 公司 HR 系统从自然人电子税务局（扣缴端）获取。
- 或员工填写纸质/电子申报表 → HR 在 `ErpHrTaxSpecialDeduction` 中录入。

| 字段 | 含义 |
|------|------|
| id/employeeId/year/month | 标准 |
| deductionType | 扣除项目类型 |
| monthlyAmount | 每月扣除金额 |
| effectiveFrom/effectiveTo | 有效期 |
| verified | 是否已核验 |

---

## 五、薪酬核算流程

### 5.1 薪酬周期

| 周期 | 适用 | 说明 |
|------|------|------|
| 月薪（MONTHLY） | 全员 | 每月一次，次月 5-15 日发薪 |
| 半月薪（BI_MONTHLY） | 部分岗位 | 每月 5 日和 20 日两次 |

### 5.2 核算流程

```
薪酬核算流程（每月）
    │
    0. 数据准备（上月数据截止）
    │       ├─ 考勤数据（ErpHrAttendance）：缺勤天数、加班小时、迟到/早退
    │       ├─ 休假数据（ErpHrLeaveRequest.APPROVED）：年假/病假/事假天数
    │       ├─ 绩效数据（手动录入或绩效系统）：绩效系数/奖金金额
    │       └─ 合同数据（ErpHrEmploymentContract）：月薪/津贴标准
    │
    1. 自动计算（SalaryCalculateJob）
    │       ├─ 遍历 ACTIVE/PROBATION 员工
    │       ├─ 计算基本工资（出勤比例 = 实际出勤日 / 应出勤日）
    │       ├─ 计算加班费（加班小时 × 加班费率）
    │       ├─ 计算津贴/补贴（固定项）
    │       ├─ 计算绩效奖金（手动已录入则取，否则 0）
    │       ├─ 计算社保/公积金（个人 + 公司）
    │       ├─ 计算个税（累计预扣法）
    │       ├─ 计算实发 = 应发 − 扣款
    │       └─ 生成 ErpHrSalary 记录（paymentStatus = PENDING）
    │
    2. HR 审核薪酬表
    │       ├─ 查看薪酬汇总表（部门/员工/项目汇总）
    │       ├─ 对比上月差异 / 异常值告警
    │       ├─ 手动修改（需记录修改日志）
    │       └─ 提交审核 → status = REVIEWED
    │
    3. 财务复核
    │       ├─ 校验总额与预算匹配
    │       ├─ 校验社保/公积金合计与申报一致
    │       └─ 复核通过 → status = APPROVED_FINANCE
    │
    4. 经理审批
    │       └─ 审批通过 → status = APPROVED_MANAGER
    │
    5. 发放执行
    │       ├─ 生成银行代发文件（ErpPayrollBankFile）
    │       ├─ 过账：SALARY_PAYMENT 凭证
    │       └─ 更新 paymentStatus = PAID
    │
    6. 工资单发布
    │       ├─ 生成 PDF 工资单（员工可见）
    │       └─ 推送电子工资单（邮件/APP/企业微信）
```

> 🟢 Axelor `Payroll.xml` 薪资核算流程含 HR prepare → approval → payment。
> 🟢 AureusERP `salary.php` 薪酬计算逻辑。
> ⚪ 中国薪酬流程惯例：HR prepare → finance review → manager approve → payment。

### 5.3 ErpHrSalary 扩展字段

补充 `README.md` §ErpHrSalary 未列字段：

| 字段 | 含义 |
|------|------|
| performanceFactor | 绩效系数（0-2） |
| actualWorkDays | 实际出勤日 |
| requiredWorkDays | 应出勤日 |
| totalOvertimeHours | 月总加班小时 |
| unpaidLeaveDays | 无薪假天数 |
| cumulativeData | 累计个税数据（JSON，年初至本月累计值） |
| approvalStatus | 审批状态 dict `erp-hr/salary-approval-status`：PENDING / REVIEWED / APPROVED_FINANCE / APPROVED_MANAGER / PAID / VOID |
| reviewNote | 审核备注 |
| paymentBatchNo | 发放批次号 |
| bankFileId | 银行文件（→ErpHrPayrollBankFile） |

---

## 六、薪酬审批工作流

### 6.1 审批状态机

```
待审核 (PENDING)
    │
    ├─ HR 审核 → 已复核 (REVIEWED)
    │
已复核 (REVIEWED)
    │
    ├─ 财务复核 → 财务已审批 (APPROVED_FINANCE)
    │
    └─ 退回 → 待审核 (PENDING)  [HR 修改后重新提交]
    │
财务已审批 (APPROVED_FINANCE)
    │
    ├─ 经理审批 → 经理已审批 (APPROVED_MANAGER)
    │
    └─ 退回 → 待审核 (PENDING)
    │
经理已审批 (APPROVED_MANAGER)
    │
    ├─ 发放 → 已发放 (PAID)  [终态]
    │
    └─ 作废 → 已作废 (VOID)  [终态]
```

### 6.2 审批角色

| 步骤 | 角色 | 操作 |
|------|------|------|
| 薪酬审核 | HR 薪酬专员 | 检查数据准确性，处理异常 |
| 财务复核 | 财务主管 | 预算校验、总额审核 |
| 经理审批 | 部门负责人/总经理 | 最终批准 |
| 发放 | HR 薪酬专员/出纳 | 执行支付 |

### 6.3 事件与 TODO

| 迁移 | TODO |
|------|------|
| PENDING→REVIEWED | 分配财务复核待办 |
| REVIEWED→APPROVED_FINANCE | 分配经理审批待办 |
| APPROVED_FINANCE→APPROVED_MANAGER | 分配 HR 出纳发放待办 |
| 退回（REVIEWED/APPROVED_FINANCE→PENDING） | 分配 HR 修正待办 |

---

## 七、银行文件生成（ErpHrPayrollBankFile）

### 7.1 实体

| 字段 | 含义 |
|------|------|
| id/batchNo/orgId | 标准 |
| paymentDate | 发放日期 |
| totalAmount | 总金额 |
| recordCount | 记录数 |
| fileFormat | 格式 dict：CSV / TXT（标准代发格式） |
| fileContent | 文件内容（CLOB，生成的银行文件全文） |
| status | dict：GENERATED / UPLOADED / CONFIRMED |
| bankId | 开户银行 |
| 标准审计字段 | |

### 7.2 文件格式（招商银行企业代发示例）

```
字段顺序：序号 ｜ 账号 ｜ 户名 ｜ 金额 ｜ 用途
------------------------------------------------
001 ｜ 6222************ ｜ 张三 ｜ 12000.00 ｜ 工资
002 ｜ 6222************ ｜ 李四 ｜ 15000.00 ｜ 工资
```

> ⚪ 各银行格式不同（招行/工行/建行），通过 fileFormat 区分模板。

### 7.3 生成流程

```
银行文件生成
    │
    ├─► 检索 paymentStatus = APPROVED_MANAGER 的 ErpHrSalary
    │
    ├─► 按银行分组（员工工资卡所在银行）
    │
    ├─► 调用模板引擎（按 fileFormat）生成文件内容
    │
    ├─► 创建 ErpHrPayrollBankFile
    │
    ├─► 可下载 / 自动上传银行网银（集成层）
    │
    └─► 标记 ErpHrSalary.paymentStatus = PAID
```

---

## 八、工资单

### 8.1 工资单内容

| 项目 | 说明 |
|------|------|
| 员工信息 | 姓名、工号、部门、岗位 |
| 薪酬期间 | 年份/月份 |
| 应发明细 | 基本工资/津贴/奖金/加班费等 |
| 扣款明细 | 社保/公积金/个税/其他扣款 |
| 实发金额 | 大写+小写 |
| 发放日期 | — |
| 公司信息 | 公司名称、银行账户信息 |

### 8.2 工资单发布渠道

| 渠道 | 形式 | 说明 |
|------|------|------|
| 系统内 | 电子版 | ERP 员工自助门户查看 |
| 邮件 | PDF 附件 | 自动发送至员工邮箱 |
| 企业微信/钉钉 | 消息卡片 | 集成层 |
| 纸质 | — | HR 打印下发 |

### 8.3 工资单发布流程

```
工资单发布
    │
    ├─► 薪酬发放完成后（PAID）
    │
    ├─► 调用 PDF 模板引擎
    │       ├─ 每员工一页
    │       └─ 含电子签章（可选）
    │
    ├─► 存储到附件系统（nop-file）
    │
    ├─► 发布事件 → 通知渠道（邮件/微信）
    │
    └─► 员工可在自助门户查看历史工资单
```

---

## 九、过账协议

### 9.1 过账科目映射

| businessType | 借方 | 贷方 | 触发时机 |
|-------------|------|------|---------|
| SALARY（计提） | 管理费用-工资/制造费用-工资 | 应付职工薪酬 | APPROVED_MANAGER |
| SALARY_PAYMENT（发放） | 应付职工薪酬 | 银行存款 | PAID |
| SOCIAL_INSURANCE_ER（社保公司） | 管理费用-社保 | 应付职工薪酬-社保 | 计提时 |
| HOUSING_FUND_ER（公积金公司） | 管理费用-公积金 | 应付职工薪酬-公积金 | 计提时 |

> 科目映射在 finance/posting.md 中定义，HR 域通过 `IErpFinAcctDocProvider` 提供核算数据。
> 🟢 Axelor `PayrollLine.xml` + `AccountingSituation.xml` 过账联动。

### 9.2 明细要求

过账明细应区分：
- 部门维度（管理费用 vs 制造费用 vs 销售费用）
- 成本中心维度（成本归集）
- 项目维度（项目人工成本）

---

## 十、年度汇算清缴支持

| 功能 | 说明 |
|------|------|
| 年度累计数据导出 | 导出每位员工全年应发/已扣社保/已扣公积金/已预扣个税 |
| 个税年度汇算数据 | 提供给员工个税 APP 或生成申报表 |
| 社保/公积金年审 | 导出年度基数调整报表 |

---

## 十一、关键业务规则总结

1. **薪酬计算顺序**：基本工资（考勤比例）→ 津贴/补贴 → 加班费 → 绩效奖金 → 社保公积金 → 个税 → 实发
2. **社保基数上下限**：各城市不同，通过 ErpHrSocialInsuranceConfig 配置
3. **个税累计预扣**：每年 1 月起重置累计，逐月累加，年终汇算清缴
4. **审批链不可跳过**：HR → 财务 → 经理，每步需前一步完成
5. **发放后不可修改**：PAID 后 ErpHrSalary 锁定，调整走补发/追扣流程
6. **银行文件生成**：按银行分组，区分各银行模板格式

## 参考

- `docs/design/human-resource/README.md`（HR 域基础实体）
- `docs/design/finance/posting.md`（薪资过账）
- `docs/design/finance/expense-claim.md`（费用报销）
- `docs/analysis/erp-survey/2026-06-30-0000-axelor-open-suite.md` §HRMS
- ⚪ 中国个人所得税法（2018 修正）——综合所得预扣预缴规则
- ⚪ 各城市社保/公积金政策（深圳/上海/北京）

# 项目域用例规格(Projects Use Cases)

> 从使用场景出发组织项目域可验证用例。机制细节引用不重复(指向 state-machine / cost-collection / profitability)。
> 项目核心是项目生命周期 + 工时成本 + 多来源成本归集 + 预算控制 + 盈利结算(含转固)。

## 状态轴速查(详见 state-machine.md)

```
项目状态:  DRAFT / OPEN / ON_HOLD / COMPLETED / CANCELLED
任务状态:  TODO / IN_PROGRESS / BLOCKED / DONE
```

---

## UC-PRJ-01 项目立项

**场景**:项目从草稿立项,开放成本归集。

**可验证断言**(见 state-machine.md):
```
项目: DRAFT → OPEN(立项)
OPEN 后: 允许新单据(工时/采购/领料/报销)标注该项目归集成本
DRAFT 状态: 不允许成本归集
```

**涉及机制**:state-machine.md

---

## UC-PRJ-02 工时提交触发人工成本凭证

**场景**:员工提交工时,按成本率计算人工成本并生成凭证。

**可验证断言**(见 cost-collection.md §二):
```
工时提交(项目=OPEN, 任务, 时长) →
  校验 项目.状态 == OPEN(暂停/关闭拒绝)
  成本率解析(优先级: 用户费率 > 角色费率 > 活动类型费率)
  人工成本 = 时长 × 成本率
  发布过账事件(businessType=PROJECT_LABOR_COST)
生成凭证: 借 项目成本(人工), 贷 应付职工薪酬/劳务成本
工时.已过账 == true
```

**涉及机制**:cost-collection.md §二、../finance/posting.md

---

## UC-PRJ-03 多来源成本归集

**场景**:采购/领料/报销/销售发票分别标注项目,归集到项目成本。

**可验证断言**(见 cost-collection.md §四):
```
采购订单行.项目 == P → 入库时成本归集到 P(物料类)
领料单.项目 == P → 归集(物料)
费用报销.项目 == P → 归集(费用)
各来源按成本分类(人工/物料/费用/分包)汇总到 ProjectPnl
```

**涉及机制**:cost-collection.md §四

---

## UC-PRJ-04 项目预算 STRICT 超支拦截

**场景**:工时/采购/报销触发预算检查,STRICT 模式下超支拦截。

**可验证断言**(见 cost-collection.md §三):
```
检查时机: 工时提交 / 采购审核 / 报销审核(标注项目时)
预算余量 = 项目预算 - 已归集成本 - 已承诺成本
STRICT 模式: 余量 < 0 → 拒绝该笔归集
WARNING 模式: 警告但放行
```

**涉及机制**:cost-collection.md §三

---

## UC-PRJ-05 任务依赖 DAG 成环校验

**场景**:任务有前置依赖,前置未完成不可开始;依赖关系不能成环。

**可验证断言**(见 state-machine.md §任务):
```
任务.dependsOn 前置任务
前置任务.状态 != DONE → 本任务不可 IN_PROGRESS
依赖关系 DAG 成环 → 校验失败(拒绝建立环依赖)
```

**涉及机制**:state-machine.md §任务

---

## UC-PRJ-06 项目损益汇总

**场景**:定时任务聚合项目收入(开票)与成本(归集),生成损益汇总。

**可验证断言**(见 profitability.md):
```
定时任务(nop-job) →
  聚合: 收入 = Σ Billing.amountFunctional
  成本 = Σ CostCollection(按人工/物料/费用/分包分类)
  毛利 = 收入 - 成本
  毛利率 = 毛利 / 收入
生成 ProjectPnl 记录
多币种折算到统一币种
```

**涉及机制**:profitability.md

---

## UC-PRJ-07 竣工结算与质保金

**场景**:项目竣工,生成结算单,留存质保金。

**可验证断言**(见 profitability.md §结算):
```
项目: → COMPLETED
基于最新 ProjectPnl 生成 ProjectSettlement(FINAL)
最终结算收入/成本/损益
质保金(retentionAmount)留存,到期返还
```

**涉及机制**:profitability.md §结算

---

## UC-PRJ-08 项目结算转固

**场景**:自建项目竣工,结转为固定资产(CLOSE)。

**可验证断言**(见 profitability.md §结算):
```
ProjectSettlement(settlementType=CLOSE, transferToAsset=true) →
  调用 IErpAstAssetBiz 生成资产卡片(资产域)
  生成转固凭证(经 finance IErpFinAcctDocProvider 注册 PROJECT_SETTLEMENT)
资产卡片.来源项目 == 本项目
```

**涉及机制**:profitability.md、../assets、../finance/posting.md(可插拔 Provider)

---

## UC-PRJ-09 项目暂停/关闭约束

**场景**:暂停/关闭项目时的约束。

**可验证断言**(见 state-machine.md):
```
OPEN → ON_HOLD(暂停): 拒绝新费用归集(工时/采购/报销)
ON_HOLD → OPEN(恢复): 恢复归集
→ COMPLETED/CANCELLED(关闭): 冻结,不可再归集,保留审计
关闭后历史成本/收入数据保留
```

**涉及机制**:state-machine.md

---



---

## UC-PRJ-10 项目看板

**场景**:项目看板的指标展示与异常预警。见 ../dashboards.md §项目看板。

**可验证断言**:
```
// KPI 指标数据源正确(实时聚合, 非硬编码)
KPI 卡片值 == 对应实体的实时聚合(按期间/orgId/权限过滤)
  在手项目/预算/已发生成本, 预算执行率, 毛利率, 超支/延期预警

// 预警触发
预警项 == 满足阈值条件的记录(阈值来自系统配置, 非硬编码)

// 权限
看板数据受行级权限约束(只看自己组织/部门/成本中心)
```

**涉及机制**:../dashboards.md、各域 state-machine.md、roles-and-permissions.md(行级权限)

## 用例与测试的衔接

- 工时成本(U02)→ 成本率优先级解析 + 凭证生成
- 预算控制(U04)→ STRICT/WARNING 拦截
- 盈利(U06/U07)→ 定时聚合 + 结算
- 转固(U08)→ 跨域(projects→assets→finance)

## 参考机制文档

- `state-machine.md` — 项目/任务状态
- `cost-collection.md` — 辅助核算/工时成本/预算控制/多来源归集
- `profitability.md` — 损益汇总/结算/转固
- `../finance/posting.md` — 业财过账(PROJECT_LABOR_COST)
- `../assets/depreciation-and-posting.md` — 转固入账

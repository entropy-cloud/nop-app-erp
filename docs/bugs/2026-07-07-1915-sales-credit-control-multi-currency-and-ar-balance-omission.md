# 01 销售信用控制多币种遗漏 + AR 余额遗漏（设计层风险，未落地）

> 来源审计：`docs/audits/2026-07-07-1900-comprehensive-design-and-implementation-audit.md`（C-2 财务风险）
> 关联计划：`docs/plans/2026-07-07-1915-1-audit-remediation-plan.md` H-2

## 问题

- 销售订单/出库单按 `currencyId` 多币种记账时，`ErpMdPartner.creditLimit`（单一金额）的信用控制规则未在 `docs/design/sales/` 中定义币种归一化策略
- 销售信用检查未明确定义应收账款（AR）open-item 余额作为扣减维度（仅"已开发票未收款" vs "全部订单"的口径不清）
- 影响：多币种客户出现"本币信用额度被外币订单吃掉"或"AR 已收金额未释放回信用额度"的口径错误；严重性：高（财务风险，但尚处设计阶段未落地代码）

## 复现

- 环境：销售域进入"信用额度校验"设计深化阶段（`docs/design/sales/` 已有 README 但未覆盖多币种信用策略）
- 触发：客户持有外币销售订单 + 本币信用额度时，`ErpSalOrder.confirmOrder()` 应执行信用校验，但当前设计与 `ErpMdPartner.creditLimit` 单字段不兼容
- 最小复现脚本：暂无（设计层 bug）

## 诊断方法

- 诊断难度：中等（需要交叉验证多份设计文档与 ORM 字段）
- 调查路径：审计报告 §3.2 财务风险扫描发现 → 核对 `module-master-data/model/app-erp-master-data.orm.xml` 中 `ErpMdPartner` 字段集 → 发现 `creditLimit` 无对应 `creditCurrencyId` → 核对 `docs/design/sales/state-machine.md` 与 `docs/design/finance/ar-ap-reconciliation.md` → 确认无币种归一化策略
- 被拒绝的假设："creditLimit 默认按业务单据 currencyId 比较" — 拒绝，因为不同币种金额不可直接比较
- 决定性证据：`ErpMdPartner` 表无 `creditCurrencyId` 列（参见 `module-master-data/model/app-erp-master-data.orm.xml:333-378`），且销售设计文档未声明如何归一化

## 根本原因

- 主数据模型：`ErpMdPartner.creditLimit` 是裸金额字段，缺少配套的 `creditCurrencyId` 与 `exchangeRateType`，多币种客户无法表达"信用额度币种"
- 业务规则：销售信用检查与 finance AR open-item 余额之间的扣减/释放规则未在销售域设计中明确，仅在各域 README 提及"信用控制"概念而未细化口径

## 修复

- 此为设计层 bug，修复需在后续销售域深化计划中完成（不在本整改计划范围内）
- 待落地动作：
  - `module-master-data/model/app-erp-master-data.orm.xml` 中 `ErpMdPartner` 增加 `creditCurrencyId` + `creditExchangeRateType`
  - `docs/design/sales/credit-control.md` 新建，定义信用额度币种归一化（默认按 partner.creditCurrencyId 转本币比较）+ AR open-item 余额口径
  - `docs/design/finance/ar-ap-reconciliation.md` 增补"信用控制扣减/释放规则"段落

## 测试

- 暂无自动化测试覆盖（设计层未落地）
- 待落地后补充：信用校验单元测试覆盖多币种客户场景 + AR 余额扣减/释放路径

## 受影响的工件

- `module-master-data/model/app-erp-master-data.orm.xml:333-378` — `ErpMdPartner` 字段集（需增补 `creditCurrencyId`）
- `docs/design/sales/state-machine.md` — 信用检查触发点（需明确定义与 AR 余额的对接规则）
- `docs/design/finance/ar-ap-reconciliation.md` — AR open-item 与信用控制的关系（需增补）

## 未来重构注意事项

- 多币种字段集：未来给主数据加金额字段时，必须配套币种字段（`xxxAmount` + `xxxCurrencyId`），否则单字段金额会引入隐式假设
- 跨域规则集中化：信用控制这类"业务触发 → 财务取数"的跨域规则应集中在 `docs/design/sales/credit-control.md` 或 finance 共享文档，避免散落在各域 README

## 预防差距

- 主数据字段集审计未覆盖"金额字段必配币种"规则；建议在 `docs/design/domain-design-guidelines.md` 增加字段集约束条目
- 销售域 README 与 finance 域 README 之间的信用控制口径未做交叉一致性审查

# 02 凭证明文金额通过 GraphQL 可查询（设计层安全风险，未落地）

> 来源审计：`docs/audits/2026-07-07-1900-comprehensive-design-and-implementation-audit.md`（C-4 安全风险）
> 关联计划：`docs/plans/2026-07-07-1915-1-audit-remediation-plan.md` H-2

## 问题

- `ErpFinVoucher` / `ErpFinVoucherLine` 的金额字段（`drAmount` / `crAmount` / `amount`）通过 GraphQL 默认查询接口暴露，无字段级权限控制
- 财务凭证金额对未授权用户（如普通采购员、销售员）可见，违反"凭证生成后仅财务岗位可见"的业务约束
- 影响：违反职责分离（SoD），且可能泄露公司财务敏感数据；严重性：高（安全风险，但 codegen 后的 BizModel 仍为骨架，未实装权限）

## 复现

- 环境：codegen 后的 finance-service，默认生成的 `IErpFinVoucherBiz.findPage()` 查询接口
- 触发：任意已登录用户 POST GraphQL `query { ErpFinVoucherList_findPage { items { voucherLines { drAmount crAmount } } } }`
- 最小复现脚本：通过 GraphQL Playground 执行上述查询（当前未部署运行时，属设计层风险）

## 诊断方法

- 诊断难度：直接（平台默认行为可推断）
- 调查路径：审计 §3.4 安全扫描 → 核对 `module-finance/model/app-erp-finance.orm.xml` 中 `ErpFinVoucherLine` 字段 → 确认金额字段未带 `sensitive="true"` 或类似标记 → 核对 nop-entropy 平台默认权限模型（`ai-defaults.md` 确认 codegen 默认全字段可查）
- 决定性证据：`module-finance/model/app-erp-finance.orm.xml` 的 `ErpFinVoucherLine.drAmount/crAmount` 无字段级权限注解

## 根本原因

- 平台默认：nop-entropy 的 `CrudBizModel` 默认暴露所有字段到 GraphQL，字段级权限需在 XMeta（`*.xmeta`）层显式声明
- 设计缺口：`docs/design/finance/` 未定义"金额字段仅财务角色可见"的字段级权限矩阵

## 修复

- 此为设计层 bug，修复需在 finance 域权限矩阵落地计划中完成（不在本整改计划范围内）
- 待落地动作：
  - `docs/design/finance/security-matrix.md` 新建，定义财务凭证/分录金额字段的角色可见性矩阵（财务岗位可见，其他岗位脱敏或拒查）
  - codegen 后在 `module-finance/erp-finance-meta/.../*.xmeta` 中给 `drAmount/crAmount/amount` 增加 `auth` 规则（如 `<auth permissions="fin:voucher:readAmount"/>`）
  - 在 `module-finance/erp-finance-service/.../ErpFinVoucherBizModel.java` 中给查询方法增加角色校验（或用 XMeta auth 替代）

## 测试

- 暂无自动化测试覆盖（设计层未落地）
- 待落地后补充：集成测试覆盖不同角色（财务 vs 销售 vs 采购）查询金额字段的可见性

## 受影响的工件

- `module-finance/model/app-erp-finance.orm.xml` — `ErpFinVoucherLine.drAmount/crAmount` 字段（codegen 后会进入 XMeta）
- `docs/design/finance/` — 缺少 security-matrix 文档
- 未来 `module-finance/erp-finance-meta/` 中的 XMeta 文件

## 未来重构注意事项

- 字段级权限：涉及金额、客户隐私、供应商价格等敏感字段，必须在 XMeta 显式声明 `auth`，不能依赖默认全开
- 角色矩阵集中化：财务、销售、采购、库存等业务域的可见性矩阵应在 `docs/design/<domain>/security-matrix.md` 集中维护，避免散落在各 BizModel 注解

## 预防差距

- 缺少"敏感字段"设计审查清单；建议在 `docs/design/domain-design-guidelines.md` 增加敏感字段识别规则
- codegen 默认行为的安全审计未覆盖；建议在 codegen 后跑一遍 GraphQL Schema 审查脚本

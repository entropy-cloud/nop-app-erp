# AI 自动化开发 Roadmap

> **创建日期**: 2026-06-25
> **依据**: `docs/analysis/2026-06-25-1648-ai-automation-readiness.md`
> **目标**: 从 ORM 模型到可运行 ERP 应用的实施路径
> **原则**: 测试随功能自然生长，AI 根据 owner doc 和用例自主拟制测试，不需要事前给定测试计划

## 当前状态

- 10 域 ORM 模型（145 实体）已设计完成并经审计
- 所有域 codegen 骨架已生成（1096 个 Java 文件），含实体类、DAO、I*Biz 接口、BizModel 空壳、XMeta、view.xml 骨架
- `app-erp-all` 聚合 app 已构建通过（82 模块）
- 设计文档（10 域 + 全局）+ 架构文档（9 份）完备
- BizModel 均为生成的 `CrudBizModel<T>` 空壳，无自定义业务逻辑
- I*Biz 接口均为空，无自定义方法声明
- 后续模型变更用 `mvn clean install` 增量重新生成，**不要**重跑 `nop-cli gen`

## Phase 1 — Master-data 域 BizModel 深化 + 页面定制

> **目标**：在已生成的 codegen 骨架上，为核心实体添加业务逻辑、ErrorCode、页面定制
> **自主权**：`implement`（单域，设计已就绪）

### 1.1 Master-data BizModel 深化

- [ ] ErrorCode 定义（`docs/design/master-data/README.md` §错误处理）
- [ ] I*Biz 接口声明自定义方法（@BizQuery/@BizMutation）
- [ ] ErpMdMaterial：启用/停用 + 分类树操作
- [ ] ErpMdPartner：多地址/联系人联动
- [ ] ErpMdWarehouse：CRUD
- [ ] AI 根据 `docs/design/master-data/use-cases.md` 自行拟制测试并验证

### 1.2 Master-data 页面定制

- [ ] 基于 `docs/design/master-data/ui-patterns.md` 深化物料/往来单位/仓库 view.xml
- [ ] 启动应用验证页面可访问、CRUD 可操作

**Phase 1 Exit Criteria**：
- [ ] ≥3 个核心实体有 BizModel 业务逻辑 + 测试 + 页面
- [ ] `mvn test` 通过

---

## Phase 2 — 采购→入库→凭证 端到端循环

> **目标**：验证跨域业务循环的完整链路
> **前置条件**：Phase 1 完成
> **自主权**：`plan-first`（跨域协作 + 业财打通）

### 2.1 库存域 BizModel 深化

- [ ] ErpInvStockMoveBiz：generateMove / confirm / cancel
- [ ] 库存流水写入 + 余额更新
- [ ] AI 根据 `docs/design/inventory/state-machine.md` + `cross-domain.md` 拟制测试

### 2.2 采购域 BizModel 深化

- [ ] ErpPurOrderBiz：创建/提交/审核
- [ ] ErpPurReceiveBiz：确认入库（调用 IErpInvStockMoveBiz）
- [ ] AI 根据 `docs/design/purchase/state-machine.md` + `three-way-match.md` 拟制测试

### 2.3 财务域 BizModel 深化

- [ ] ErpFinAcctDocRegistry + PurReceiveAcctDocProvider 实现
- [ ] 凭证模板配置（PURCHASE_INPUT）
- [ ] 业财回链写入
- [ ] AI 根据 `docs/design/finance/posting.md` + `state-machine.md` 拟制测试

### 2.4 端到端串联

- [ ] 采购订单 → 采购入库 → 库存移动单 → 流水 → 余额
- [ ] 采购入库 → 凭证自动生成 → 业财回链
- [ ] 全链路 `mvn test` 通过

**Phase 2 Exit Criteria**：
- [ ] 采购→入库→凭证 端到端循环可执行
- [ ] 凭证自动生成且借贷平衡
- [ ] 每个域的 BizModel 有对应测试

---

## Phase 3 — 扩展域 BizModel 深化 + 业务闭环

> **目标**：完成剩余 8 域的核心 BizModel 业务逻辑和页面定制
> **前置条件**：Phase 2 完成
> **自主权**：`implement`（单域实现，设计已就绪）

按优先级依次深化，每个域遵循相同模式：BizModel 业务逻辑 → ErrorCode → 测试 → 页面定制

1. **sales 域**：SalesOrderBiz / SalesDeliveryBiz / SalesInvoiceBiz
   - 参考：`docs/design/sales/state-machine.md` + `returns.md`
2. **manufacturing 域**：WorkOrderBiz / BomBiz / JobCardBiz
   - 参考：`docs/design/manufacturing/state-machine.md` + `bom-and-routing.md`
3. **assets 域**：AssetCardBiz / DepreciationScheduleBiz
   - 参考：`docs/design/assets/state-machine.md` + `depreciation-and-posting.md`
4. **projects 域**：ProjectBiz / TaskBiz / CostCollectionBiz
   - 参考：`docs/design/projects/state-machine.md` + `cost-collection.md`
5. **quality 域**：InspectionBiz / NcrBiz / CapaBiz
   - 参考：`docs/design/quality/state-machine.md`
6. **maintenance 域**：EquipmentBiz / MaintenancePlanBiz / MaintenanceAccessBiz
   - 参考：`docs/design/maintenance/state-machine.md`

**Phase 3 Exit Criteria**：
- [ ] 10 域全部 BizModel 业务逻辑完成
- [ ] 每域核心 BizModel 有测试
- [ ] `mvn clean install -DskipTests` 全绿

---

## 风险与缓解

| 风险 | 缓解 |
|------|------|
| ORM 模型变更请求 | 走 ask-first 保护区域流程 |
| 跨域事务一致性问题 | 按 data-dependency-matrix 的 S 写规则实现 |
| 增量重新生成产物不一致 | `mvn clean install -DskipTests` 全量重建 |

## 与现有计划的关系

- `01-product-grade-erp-model-overhaul.md`（已完成）：交付了 145 实体 ORM 模型
- codegen 骨架已由 `nop-cli gen` 生成（1096 个 Java 文件），本 roadmap 聚焦于**在已生成骨架上深化业务逻辑**

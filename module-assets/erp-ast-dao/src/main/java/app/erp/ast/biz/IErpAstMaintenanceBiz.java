
package app.erp.ast.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.ast.dao.entity.ErpAstMaintenance;

/**
 * 资产维修业务接口（UC-AST-10）。维修工单状态机：DRAFT→SUBMITTED→IN_PROGRESS→COMPLETED→POSTED（+ CANCELLED）。
 *
 * <p>费用归集（addCost）在 IN_PROGRESS 态进行；裁决处置（decideTreatment）在 COMPLETED 态进行（CAPITALIZE/EXPENSE）。
 * 详见 owner doc {@code docs/design/assets/maintenance.md}。
 */
public interface IErpAstMaintenanceBiz extends ICrudBiz<ErpAstMaintenance> {

    /**
     * 创建维修工单：关联资产卡片，可选弱关联 maintenance 域 ErpMntVisit。
     * 校验资产非终态（SCRAPPED/SOLD/DISPOSED 拒绝）。
     */
    @BizMutation
    ErpAstMaintenance createMaintenance(@Name("assetId") Long assetId,
                                        @Name("code") String code,
                                        @Name("name") @Optional String name,
                                        @Name("businessDate") @Optional String businessDate,
                                        @Name("maintenanceVisitId") @Optional Long maintenanceVisitId,
                                        @Name("reason") @Optional String reason,
                                        IServiceContext context);

    /**
     * 提交维修工单：DRAFT → SUBMITTED。
     */
    @BizMutation
    ErpAstMaintenance submit(@Name("id") Long id, IServiceContext context);

    /**
     * 开工：SUBMITTED → IN_PROGRESS。开工后可归集费用。
     */
    @BizMutation
    ErpAstMaintenance startWork(@Name("id") Long id, IServiceContext context);

    /**
     * 完工：IN_PROGRESS → COMPLETED。完工后待裁决处置。
     */
    @BizMutation
    ErpAstMaintenance completeWork(@Name("id") Long id, IServiceContext context);

    /**
     * 裁决处置方式（COMPLETED 态）：CAPITALIZE（延长寿命/提升效能）或 EXPENSE（日常维修）。
     * 阈值门控：资本化金额 < 阈值时强制费用化。
     */
    @BizMutation
    ErpAstMaintenance decideTreatment(@Name("id") Long id,
                                      @Name("treatment") String treatment,
                                      @Name("capitalizedAmount") @Optional java.math.BigDecimal capitalizedAmount,
                                      IServiceContext context);

    /**
     * 审核维修工单（config-gated，默认强制审批）。审核后可执行 post。
     */
    @BizMutation
    ErpAstMaintenance approve(@Name("id") Long id, IServiceContext context);

    /**
     * 过账：COMPLETED → POSTED。按 treatment 分派 CAPITALIZE（原值增量 + 折旧重算 + MAINTENANCE_CAPITALIZATION 凭证）
     * 或 EXPENSE（MAINTENANCE_EXPENSE 凭证）。posted=true。
     */
    @BizMutation
    ErpAstMaintenance post(@Name("id") Long id, IServiceContext context);

    /**
     * 作废：DRAFT/SUBMITTED → CANCELLED（终态）。
     */
    @BizMutation
    ErpAstMaintenance cancel(@Name("id") Long id, IServiceContext context);

    /**
     * 红冲纠错：POSTED 维修单纠错必经 reverse。红字凭证 + 资本化路径回退原值/折旧计划或费用化路径仅凭证回退。
     */
    @BizMutation
    ErpAstMaintenance reverse(@Name("id") Long id, IServiceContext context);
}

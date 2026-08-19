package app.erp.drp.biz;

import app.erp.drp.dao.entity.ErpInvDrpLeadTimeRecord;
import app.erp.drp.dao.entity.ErpInvDrpSupplierScore;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import java.time.LocalDate;
import java.util.List;

/**
 * 提前期记录 BizModel 契约（RC-R1.82 / P1-RC-082，UC-DRP-08）。
 *
 * <p>{@link #recordFromPurchaseReceive} 为 purchase 域收货审批后置 Facade（D4 裁决选项 A，
 * actualLeadTime = DATEDIFF(receiptDate, orderDate)，镜像 RC-R1.61/D1 接线方向；pur-service → drp-dao
 * Java 层边，见 data-dependency-matrix §2.4）。幂等守卫：同 purchaseOrderCode + materialId 不重复落记录。
 *
 * <p>{@link #findLeadTimeStats} 统计粒度由参数组合决定（供应商级 / 供应商+物料级 / 物料级）；
 * {@link #recalculateLeadTimeStats} 计算四维评分并回写 {@link ErpInvDrpSupplierScore} 汇总行
 * （UK supplierId+materialId upsert）。
 */
public interface IErpInvDrpLeadTimeRecordBiz extends ICrudBiz<ErpInvDrpLeadTimeRecord> {

    /**
     * purchase 收货审批后置：按采购单号 + 收货行物料落提前期记录。
     *
     * <p>varianceDays/isOnTime/earlyLateFlag 按容差系数 config {@code erp-inv.drp-lt-tolerance}
     * （默认 0.1）计算；expectedLeadTime 为 null 时三字段留空（不可判定，不入准时率分母）。
     * 订单/收货日期缺失或倒置时抛 {@code erp.err.drp.lt.dates-invalid}（调用方隔离告警，
     * L1 UC-DRP-08 异常「订单日期或收货日期缺失时不记录（跳过告警）」）。
     *
     * @return 实际新建记录数（幂等跳过既有 purchaseOrderCode+materialId 行）
     */
    @BizMutation
    int recordFromPurchaseReceive(@Name("purchaseOrderCode") String purchaseOrderCode,
                                  @Name("supplierId") Long supplierId,
                                  @Name("orderDate") LocalDate orderDate,
                                  @Name("receiptDate") LocalDate receiptDate,
                                  @Optional @Name("expectedLeadTime") Integer expectedLeadTime,
                                  @Name("materialIds") List<Long> materialIds,
                                  IServiceContext context);

    /**
     * 提前期统计分析（统计窗口 {@code erp-inv.drp-lt-stats-window-days}，默认 365 天，&le;0 全历史）。
     *
     * <p>至少提供一个过滤参数：supplierId + materialId = 供应商+物料级；仅 supplierId = 供应商级；
     * 仅 materialId = 物料级（跨供应商）。
     */
    @BizQuery
    LeadTimeStatsBean findLeadTimeStats(@Optional @Name("supplierId") Long supplierId,
                                        @Optional @Name("materialId") Long materialId,
                                        IServiceContext context);

    /**
     * 重算提前期统计与供应商可靠性评分（四维 40/30/20/10，等级 A/B/C/D 阈值 90/75/60），
     * 回写评分汇总实体（无样本维度得分记 0 且 missingDimensions 标注样本缺失，不静默忽略）。
     * 窗口内无提前期样本时抛 {@code erp.err.drp.lt.no-samples}。
     */
    @BizMutation
    ErpInvDrpSupplierScore recalculateLeadTimeStats(@Name("supplierId") Long supplierId,
                                                    @Name("materialId") Long materialId,
                                                    IServiceContext context);
}

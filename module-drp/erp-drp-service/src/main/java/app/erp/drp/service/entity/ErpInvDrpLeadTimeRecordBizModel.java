package app.erp.drp.service.entity;

import app.erp.drp.biz.IErpInvDrpLeadTimeRecordBiz;
import app.erp.drp.biz.LeadTimeStatsBean;
import app.erp.drp.dao.entity.ErpInvDrpLeadTimeRecord;
import app.erp.drp.dao.entity.ErpInvDrpSupplierScore;
import app.erp.drp.service.processor.ErpInvDrpLeadTimeProcessor;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.time.LocalDate;
import java.util.List;

/**
 * 提前期记录 BizModel（RC-R1.82 / P1-RC-082，UC-DRP-08）。薄委派层：
 * 收货后置 Facade {@code recordFromPurchaseReceive}（D4 裁决选项 A）+ 统计查询 {@code findLeadTimeStats}
 * + 评分重算 {@code recalculateLeadTimeStats} 委派 {@link ErpInvDrpLeadTimeProcessor}
 * （protected step 可被 Delta 覆盖）。
 */
@BizModel("ErpInvDrpLeadTimeRecord")
public class ErpInvDrpLeadTimeRecordBizModel extends CrudBizModel<ErpInvDrpLeadTimeRecord>
        implements IErpInvDrpLeadTimeRecordBiz {

    @Inject
    ErpInvDrpLeadTimeProcessor leadTimeProcessor;

    public ErpInvDrpLeadTimeRecordBizModel() {
        setEntityName(ErpInvDrpLeadTimeRecord.class.getName());
    }

    public void setLeadTimeProcessor(ErpInvDrpLeadTimeProcessor leadTimeProcessor) {
        this.leadTimeProcessor = leadTimeProcessor;
    }

    @Override
    @BizMutation
    public int recordFromPurchaseReceive(@Name("purchaseOrderCode") String purchaseOrderCode,
                                         @Name("supplierId") Long supplierId,
                                         @Name("orderDate") LocalDate orderDate,
                                         @Name("receiptDate") LocalDate receiptDate,
                                         @Optional @Name("expectedLeadTime") Integer expectedLeadTime,
                                         @Name("materialIds") List<Long> materialIds,
                                         IServiceContext context) {
        return leadTimeProcessor.recordFromPurchaseReceive(purchaseOrderCode, supplierId, orderDate,
                receiptDate, expectedLeadTime, materialIds, context);
    }

    @Override
    @BizQuery
    public LeadTimeStatsBean findLeadTimeStats(@Optional @Name("supplierId") Long supplierId,
                                               @Optional @Name("materialId") Long materialId,
                                               IServiceContext context) {
        return leadTimeProcessor.findLeadTimeStats(supplierId, materialId, context);
    }

    @Override
    @BizMutation
    public ErpInvDrpSupplierScore recalculateLeadTimeStats(@Name("supplierId") Long supplierId,
                                                           @Name("materialId") Long materialId,
                                                           IServiceContext context) {
        return leadTimeProcessor.recalculateLeadTimeStats(supplierId, materialId, context);
    }
}

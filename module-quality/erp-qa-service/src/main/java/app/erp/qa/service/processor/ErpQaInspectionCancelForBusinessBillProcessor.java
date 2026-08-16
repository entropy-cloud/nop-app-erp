package app.erp.qa.service.processor;

import app.erp.qa.dao.entity.ErpQaInspection;
import app.erp.qa.service.ErpQaConfigs;
import app.erp.qa.service.ErpQaConstants;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.core.context.IServiceContext;

import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * ErpQaInspection cancelForBusinessBill per-mutation Processor（RC-R1.59 UC-QA-08，
 * {@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 业务单据作废联动取消质检：按 relatedBillType+relatedBillCode 精确查询关联质检单，
 * 仅 {@code result=PENDING} 软删取消（useLogicalDelete 置 delVersion，平台逻辑删除；
 * 查询经 EQL 编译期 delVersion=0 自动过滤），终态（ACCEPTED/CONDITIONAL/REJECTED）不动（历史完整），
 * 无匹配零副作用。下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 *
 * <p>config-gated（{@code erp-qua.business-cancel-linkage-enabled}，默认 true）：关闭时零副作用返回 0。
 * 幂等：二次调用查无 PENDING 零副作用。返回实际取消数。业务域 cancel Processor 后置调用，
 * 失败以 LOG.warn 降级不阻断作废主流程（联动为辅助语义）。
 */
public class ErpQaInspectionCancelForBusinessBillProcessor extends AbstractErpQaInspectionProcessor {

    public int cancelForBusinessBill(String billType, String billCode, IServiceContext context) {
        if (!isLinkageEnabled()) {
            return 0;
        }
        List<ErpQaInspection> inspections = findPendingByRelatedBill(billType, billCode);
        int cancelled = 0;
        for (ErpQaInspection ins : inspections) {
            softDeletePending(ins);
            cancelled++;
        }
        return cancelled;
    }

    /** config 门控：默认 true（仅取消 PENDING 且关联已作废单据的质检单，零活跃数据危害）。 */
    protected boolean isLinkageEnabled() {
        return ErpQaConfigs.isBusinessCancelLinkageEnabled();
    }

    /** 按关联键查 PENDING 质检单（平台逻辑删除过滤 delVersion=0 自动生效；终态不入选）。 */
    protected List<ErpQaInspection> findPendingByRelatedBill(String billType, String billCode) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("relatedBillType", billType));
        q.addFilter(eq("relatedBillCode", billCode));
        q.addFilter(eq("result", ErpQaConstants.INSPECTION_RESULT_PENDING));
        return inspectionDao().findAllByQuery(q);
    }

    /** 软删取消：useLogicalDelete 实体 deleteEntity → 自动转 UPDATE delVersion=currentTimeMillis（非物理删除）。 */
    protected void softDeletePending(ErpQaInspection ins) {
        inspectionDao().deleteEntity(ins);
    }
}

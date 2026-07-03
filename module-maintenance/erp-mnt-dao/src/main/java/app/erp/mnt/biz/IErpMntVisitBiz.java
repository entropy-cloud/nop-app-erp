package app.erp.mnt.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.mnt.dao.entity.ErpMntVisit;

/**
 * 维护访问业务接口。除标准 CRUD 外，定义访问 5 态状态机：
 * DRAFT→SCHEDULED→IN_PROGRESS→COMPLETED / CANCELLED。
 *
 * <p>start/complete 联动设备状态（UNDER_MAINTENANCE/恢复），经 {@code EquipmentStatusLinker} 门控。
 */
public interface IErpMntVisitBiz extends ICrudBiz<ErpMntVisit> {

    @BizMutation
    ErpMntVisit schedule(@Name("visitId") Long visitId, IServiceContext context);

    @BizMutation
    ErpMntVisit start(@Name("visitId") Long visitId, IServiceContext context);

    @BizMutation
    ErpMntVisit complete(@Name("visitId") Long visitId, IServiceContext context);

    @BizMutation
    ErpMntVisit cancel(@Name("visitId") Long visitId, IServiceContext context);
}

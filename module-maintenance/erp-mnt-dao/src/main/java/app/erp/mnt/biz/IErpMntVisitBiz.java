package app.erp.mnt.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.mnt.dao.entity.ErpMntRequest;
import app.erp.mnt.dao.entity.ErpMntVisit;

/**
 * 维护访问业务接口。除标准 CRUD 外，定义访问 5 态状态机：
 * DRAFT→SCHEDULED→IN_PROGRESS→COMPLETED / CANCELLED。
 *
 * <p>start/complete 联动设备状态（UNDER_MAINTENANCE/恢复），经 {@code EquipmentStatusLinker} 门控。
 *
 * <p>{@code reportAdditionalFault}（RC-R1.31 / P1-RC-069）：维护访问 IN_PROGRESS 期间发现额外故障，
 * 记录本次访问 remark（追加语义）+ 另开新维护请求（OPEN）处理额外故障，不中断本次维护
 * （不翻转 visit 状态、不写 totalMinutes/result）。
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

    @BizMutation
    ErpMntRequest reportAdditionalFault(@Name("visitId") Long visitId,
                                        @Name("description") String description,
                                        @Name("priority") @Optional String priority,
                                        @Name("remark") @Optional String remark,
                                        IServiceContext context);
}

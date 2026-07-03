package app.erp.mnt.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.mnt.dao.entity.ErpMntRequest;

/**
 * 维护请求业务接口。除标准 CRUD 外，定义请求 6 态状态机：
 * OPEN→ACCEPTED→IN_PROGRESS→COMPLETED / REJECTED / CANCELLED。
 *
 * <p>accept 受理后生成维护访问（DRAFT, visitType=RESPONSIVE）。
 */
public interface IErpMntRequestBiz extends ICrudBiz<ErpMntRequest> {

    @BizMutation
    ErpMntRequest accept(@Name("requestId") Long requestId, IServiceContext context);

    @BizMutation
    ErpMntRequest startRepair(@Name("requestId") Long requestId, IServiceContext context);

    @BizMutation
    ErpMntRequest complete(@Name("requestId") Long requestId, IServiceContext context);

    @BizMutation
    ErpMntRequest reject(@Name("requestId") Long requestId, IServiceContext context);

    @BizMutation
    ErpMntRequest cancel(@Name("requestId") Long requestId, IServiceContext context);
}

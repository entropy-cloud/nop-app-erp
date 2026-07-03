package app.erp.mnt.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.mnt.dao.entity.ErpMntSchedule;

/**
 * 维护计划业务接口。除标准 CRUD 外，定义到期生成访问入口：
 * generateDueVisits 扫描 active 计划 nextDueDate ≤ asOfDate 生成 DRAFT 访问（visitType=PLANNED）
 * + 按 recurrenceType/frequency 推进 nextDueDate；经 {@code erp-mnt.auto-generate-due-visits} 门控。
 */
public interface IErpMntScheduleBiz extends ICrudBiz<ErpMntSchedule> {

    @BizMutation
    Integer generateDueVisits(@Name("asOfDate") java.time.LocalDate asOfDate, IServiceContext context);
}

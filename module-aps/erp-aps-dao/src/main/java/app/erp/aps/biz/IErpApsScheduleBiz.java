
package app.erp.aps.biz;

import app.erp.aps.dao.entity.ErpApsSchedule;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

public interface IErpApsScheduleBiz extends ICrudBiz<ErpApsSchedule>{

    /**
     * 发布排产方案（DRAFT→PUBLISHED）：锁定为执行参照，供 manufacturing JobCard 创建参照（弱引用）。
     */
    @BizMutation
    ErpApsSchedule publish(@Name("id") Long id, IServiceContext context);

    /**
     * 归档排产方案（DRAFT|PUBLISHED→ARCHIVED）：转为历史版本，不再作为执行参照。
     */
    @BizMutation
    ErpApsSchedule archive(@Name("id") Long id, IServiceContext context);
}

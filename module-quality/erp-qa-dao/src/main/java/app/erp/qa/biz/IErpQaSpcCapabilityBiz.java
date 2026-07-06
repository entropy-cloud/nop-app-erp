package app.erp.qa.biz;

import app.erp.qa.dao.entity.ErpQaSpcCapability;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import java.time.LocalDate;

/**
 * SPC 过程能力分析业务接口。除标准 CRUD 外，扩展手动触发计算。
 *
 * <p>权威：{@code docs/design/quality/spc.md §关键流程 4}，plan 2026-07-07-0305-2。
 */
public interface IErpQaSpcCapabilityBiz extends ICrudBiz<ErpQaSpcCapability> {

    /** 手动触发指定 chart 在指定周期内的过程能力计算。 */
    @BizMutation
    ErpQaSpcCapability calculateCapability(@Name("chartId") Long chartId,
                                            @Optional @Name("periodFrom") LocalDate periodFrom,
                                            @Optional @Name("periodTo") LocalDate periodTo,
                                            IServiceContext context);
}

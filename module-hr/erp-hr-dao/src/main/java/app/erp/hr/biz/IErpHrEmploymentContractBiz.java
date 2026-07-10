
package app.erp.hr.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.api.core.annotations.orm.SingleSession;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.hr.dao.entity.ErpHrEmploymentContract;

import java.time.LocalDate;
import java.util.List;

public interface IErpHrEmploymentContractBiz extends ICrudBiz<ErpHrEmploymentContract>{

    /**
     * 扫描到期预警合同（UC-HR-07）：status=ACTIVE 且 endDate 在 [today, today+warningDays] 区间。
     *
     * @param warningDays 预警提前天数；null 取 config erp-hr.contract-expiry-warning-days（默认 30）
     */
    @BizQuery
    List<ErpHrEmploymentContract> scanExpiringContracts(@Optional @Name("warningDays") Integer warningDays,
                                                         IServiceContext context);

    /**
     * 推进已过期合同（UC-HR-07）：status=ACTIVE 且 endDate &lt; today → status=EXPIRED。
     */
    @BizMutation
    @SingleSession
    List<ErpHrEmploymentContract> expireOverdueContracts(IServiceContext context);

    /**
     * 续签合同（UC-HR-07）：EXPIRED/ACTIVE → ACTIVE + 更新 endDate。
     */
    @BizMutation
    @SingleSession
    ErpHrEmploymentContract renew(@Name("id") String id,
                                  @Name("newEndDate") LocalDate newEndDate,
                                  IServiceContext context);
}

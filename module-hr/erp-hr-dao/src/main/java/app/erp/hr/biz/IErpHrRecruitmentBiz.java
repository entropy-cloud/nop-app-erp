
package app.erp.hr.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.orm.SingleSession;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.hr.dao.entity.ErpHrRecruitment;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface IErpHrRecruitmentBiz extends ICrudBiz<ErpHrRecruitment>{

    /**
     * 简历筛选推进（OPEN → SCREENING）。
     */
    @BizMutation
    @SingleSession
    ErpHrRecruitment moveToScreening(@Name("id") String id, IServiceContext context);

    /**
     * 安排面试（SCREENING → INTERVIEW）。
     */
    @BizMutation
    @SingleSession
    ErpHrRecruitment scheduleInterview(@Name("id") String id,
                                       @Name("interviewerId") Long interviewerId,
                                       @Name("interviewDate") LocalDate interviewDate,
                                       IServiceContext context);

    /**
     * 发放 Offer（INTERVIEW → OFFERED）。
     */
    @BizMutation
    @SingleSession
    ErpHrRecruitment makeOffer(@Name("id") String id,
                               @Name("offerSalary") BigDecimal offerSalary,
                               IServiceContext context);

    /**
     * 入职联动（OFFERED → HIRED）+ 自动创建 ErpHrEmployee + 回写 employeeId + 创建 ACTIVE 合同。
     */
    @BizMutation
    @SingleSession
    ErpHrRecruitment hire(@Name("id") String id,
                          @Name("hiredDate") LocalDate hiredDate,
                          IServiceContext context);

    /**
     * 拒绝候选人（任意阶段 → REJECTED）。
     */
    @BizMutation
    @SingleSession
    ErpHrRecruitment reject(@Name("id") String id, IServiceContext context);

    /**
     * 关闭招聘记录（→ CLOSED）。
     */
    @BizMutation
    @SingleSession
    ErpHrRecruitment close(@Name("id") String id, IServiceContext context);
}

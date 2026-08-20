package app.erp.hr.service.processor;

import app.erp.hr.dao.entity.ErpHrSalary;
import app.erp.hr.service.posting.SalaryPostingDispatcher;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * ErpHrSalary approve 后置计提过账编排 Processor（RC-R1.89 / P1-MA4-017，payroll.md §6.5/§9.1）。
 *
 * <p>{@code ErpHrSalary.xbiz} approve mutation 状态写回（approveStatus=APPROVED + approvedBy/approvedAt）
 * 后的单一委托点：按 270(SALARY)→290(SOCIAL_INSURANCE_ER)→300(HOUSING_FUND_ER) 顺序调用
 * {@link SalaryPostingDispatcher} 三条计提路径（各自失败隔离 G3 吞异常 + 告警），并按 D3 语义写
 * {@code posted} writer——三条全部成功（去重守卫命中计为成功）才 {@code posted=true}（「计提链完整」
 * 语义），任一失败保持 false；280 发放路径（markPaid）不受影响。
 *
 * <p>机制注记（对齐 M4.64 xbiz 机制注记 + Guard/StateMachine Bean 范式）：编排/失败隔离下沉 Java Bean
 * （XScript try/catch 在 XLang 引擎不可行），xbiz source 仅一行 inject 委托调用。wf 结束经 .xwf
 * listener 回调同一 approve action，本 Processor 同时覆盖直批与 wf 审批路径。posted 经 ORM dirty
 * flush 随 approve mutation 事务持久化（与 approveStatus/approvedBy/approvedAt 同机制）。
 *
 * <p>计提红冲对称性（approve 后反审的 270/290/300 凭证红冲）为 Deferred successor，本类仅靠去重
 * 守卫防重复计提（reverseApprove→再 approve 不产生重复凭证 + 补投失败条目可收敛 posted=true）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpHrSalaryPostApprovalProcessor {

    @Inject
    SalaryPostingDispatcher postingDispatcher;

    /**
     * approve 后置计提过账编排入口（xbiz approve mutation 尾部委托调用）。
     *
     * @param salary  审批中的托管实体（approveStatus 已写回 APPROVED，随外层事务 dirty flush 持久化）
     * @param context 服务上下文
     */
    public void postAccruals(ErpHrSalary salary, IServiceContext context) {
        if (shouldSkip(salary)) {
            return;
        }
        // 非 short-circuit 的 & 是刻意的：三条路径各自失败隔离（前面失败不阻断后面补投），
        // 全部执行后才聚合 posted 语义（D3：任一失败保持 false）
        boolean posted = postSalaryAccrual(salary, context)
                & postSocialInsuranceER(salary, context)
                & postHousingFundER(salary, context);
        salary.setPosted(posted);
    }

    /** 幂等防御：posted 已 true（计提链已完整）则整体跳过（重复 approve/wf 回调竞争防重）。 */
    protected boolean shouldSkip(ErpHrSalary salary) {
        return Boolean.TRUE.equals(salary.getPosted());
    }

    protected boolean postSalaryAccrual(ErpHrSalary salary, IServiceContext context) {
        return postingDispatcher.tryPostAccrual(salary);
    }

    protected boolean postSocialInsuranceER(ErpHrSalary salary, IServiceContext context) {
        return postingDispatcher.tryPostSocialInsuranceER(salary);
    }

    protected boolean postHousingFundER(ErpHrSalary salary, IServiceContext context) {
        return postingDispatcher.tryPostHousingFundER(salary);
    }
}

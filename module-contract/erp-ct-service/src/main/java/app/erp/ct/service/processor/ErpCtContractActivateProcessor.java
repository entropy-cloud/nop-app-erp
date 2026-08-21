package app.erp.ct.service.processor;

import app.erp.contract.dao.entity.ErpCtContract;
import app.erp.contract.dao.entity.ErpCtContractVersion;
import app.erp.ct.biz.IErpCtContractVersionBiz;
import app.erp.ct.service.ErpCtConfigs;
import app.erp.ct.service.ErpCtConstants;
import app.erp.ct.service.ErpCtErrors;
import app.erp.ct.service.approval.ErpCtApprovalWorkflowEngine;
import app.erp.ct.service.statemachine.ErpCtContractStateMachine;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.util.Objects;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * ErpCtContract activate per-mutation Processor（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含合同激活编排（NEGOTIATION→ACTIVE + 当前版本定稿则同步签署）；共享 protected helper 已随编排迁入。
 *
 * <p>固定来源态/目标态判断委托 {@link ErpCtContractStateMachine}（合同头 status 轴 Bean，契约 §4/§7）；
 * 动态业务守卫（contractType↔direction 组合校验）+ 签署/版本生效副作用保留原位。非法边 Bean 抛 common 层码
 * （含 {@code action}/fromStatus 元数据），本 Processor 捕获后映射领域码 {@link ErpCtErrors#ERR_CT_ILLEGAL_STATUS_TRANSITION}
 * （+ contractCode/currentStatus/expectedStatus 实体编号/上下文，common 码作 cause 保留）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpCtContractActivateProcessor {

    @Inject
    IDaoProvider daoProvider;

    @Inject
    IErpCtContractVersionBiz contractVersionBiz;

    @Inject
    ErpCtApprovalWorkflowEngine approvalEngine;

    @Inject
    ErpCtContractStateMachine stateMachine;

    public ErpCtContract activate(String contractId, IServiceContext context) {
        ErpCtContract contract = requireContract(contractId);
        try {
            stateMachine.assertCanActivate(contract.getStatus());
        } catch (NopException e) {
            throw illegalTransition(contract, ErpCtConstants.CONTRACT_STATUS_NEGOTIATION, e);
        }
        validateTypeDirectionCombo(contract);
        // 审批链完整性联动（RC-R1.34，UC-CT-07 step 5「所有节点通过后合同可进入 ACTIVE 状态」）：
        // config-gated——approval-enabled=false 或零链记录（矩阵无匹配）时跳过（既有行为零变化）；
        // 链记录存在且非全 APPROVED → 拒绝激活（L1「可进入 ACTIVE」= 前置满足，签署确认仍须显式 activate）。
        if (AppConfig.var(ErpCtConfigs.CFG_APPROVAL_ENABLED, false)
                && !approvalEngine.isChainComplete(contract.getId(), context)) {
            throw new NopException(ErpCtErrors.ERR_CT_APPROVAL_NOT_COMPLETE)
                    .param(ErpCtErrors.ARG_CONTRACT_CODE, contract.getCode());
        }

        // 当前版本须已定稿（FINALIZED），则同步签署为 SIGNED；已签署则放行。
        ErpCtContractVersion current = findCurrentVersion(contract.getId(), context);
        if (current != null && Objects.equals(current.getStatus(), ErpCtConstants.VERSION_STATUS_FINALIZED)) {
            contractVersionBiz.signVersion(current.getId(), context);
        }

        contract.setStatus(stateMachine.activateTargetStatus());
        contract.setSignDate(CoreMetrics.today());
        dao().updateEntity(contract);
        return contract;
    }

    // ---------- helpers ----------

    protected ErpCtContract requireContract(String contractId) {
        ErpCtContract contract = dao().getEntityById(contractId);
        if (contract == null) {
            throw new NopException(ErpCtErrors.ERR_CT_ILLEGAL_STATUS_TRANSITION)
                    .param(ErpCtErrors.ARG_CONTRACT_ID, contractId);
        }
        return contract;
    }

    protected void validateTypeDirectionCombo(ErpCtContract contract) {
        String type = contract.getContractType();
        String direction = contract.getContractDirection();
        if (Objects.equals(type, ErpCtConstants.CONTRACT_TYPE_PURCHASE)
                && !Objects.equals(direction, ErpCtConstants.CONTRACT_DIRECTION_INBOUND)) {
            throw new NopException(ErpCtErrors.ERR_CT_ILLEGAL_STATUS_TRANSITION)
                    .param(ErpCtErrors.ARG_CONTRACT_CODE, contract.getCode())
                    .param(ErpCtErrors.ARG_EXPECTED_STATUS, ErpCtConstants.CONTRACT_DIRECTION_INBOUND);
        }
        if (Objects.equals(type, ErpCtConstants.CONTRACT_TYPE_SALES)
                && !Objects.equals(direction, ErpCtConstants.CONTRACT_DIRECTION_OUTBOUND)) {
            throw new NopException(ErpCtErrors.ERR_CT_ILLEGAL_STATUS_TRANSITION)
                    .param(ErpCtErrors.ARG_CONTRACT_CODE, contract.getCode())
                    .param(ErpCtErrors.ARG_EXPECTED_STATUS, ErpCtConstants.CONTRACT_DIRECTION_OUTBOUND);
        }
    }

    protected ErpCtContractVersion findCurrentVersion(String contractId, IServiceContext context) {
        QueryBean query = new QueryBean();
        query.addFilter(eq("contractId", contractId));
        query.addFilter(eq("isCurrent", true));
        return contractVersionBiz.findFirst(query, null, context);
    }

    protected NopException illegalTransition(ErpCtContract contract, String expected) {
        return illegalTransition(contract, expected, null);
    }

    /** 领域非法迁移异常构造；可选 {@code cause} 保留 Bean 抛出的 common 层非法边报告（契约 §7）。 */
    protected NopException illegalTransition(ErpCtContract contract, String expected, Throwable cause) {
        return new NopException(ErpCtErrors.ERR_CT_ILLEGAL_STATUS_TRANSITION, cause)
                .param(ErpCtErrors.ARG_CONTRACT_CODE, contract.getCode())
                .param(ErpCtErrors.ARG_CURRENT_STATUS, contract.getStatus())
                .param(ErpCtErrors.ARG_EXPECTED_STATUS, expected);
    }

    protected IEntityDao<ErpCtContract> dao() {
        return daoProvider.daoFor(ErpCtContract.class);
    }
}

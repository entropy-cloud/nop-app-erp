package app.erp.mfg.service.processor;

import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.mfg.dao.entity.ErpMfgMaterialIssue;
import app.erp.mfg.service.ErpMfgConstants;
import app.erp.mfg.service.ErpMfgErrors;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * ErpMfgMaterialIssue reverseConfirm per-mutation Processor（R6.2，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含领料红冲编排（红冲 MANUFACTURING_ISSUE 凭证 → 反向 OUTGOING 库存移动单 → posted=false + docStatus=CANCELLED）；
 * 从 ErpMfgMaterialIssueBizModel 内联 @BizMutation 提取。共享 protected helper 单一真相源在 {@link AbstractErpMfgMaterialIssueProcessor}。
 */
public class ErpMfgMaterialIssueReverseConfirmProcessor extends AbstractErpMfgMaterialIssueProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(ErpMfgMaterialIssueReverseConfirmProcessor.class);

    public ErpMfgMaterialIssue reverseConfirm(Long issueId, IServiceContext context) {
        ErpMfgMaterialIssue issue = requireIssue(issueId, context);
        validateCanReverse(issue, context);

        // 1. 红冲 MANUFACTURING_ISSUE 凭证（try/catch 吞异常保持幂等，对齐 dispatchIfApplicable 正向过账范式）
        try {
            issuePostingDispatcher.reverse(issue);
        } catch (Exception e) {
            if (e instanceof NopException) {
                LOG.warn("领料红冲 GL 凭证失败（吞异常保持幂等），领料单 {} billHeadCode={}: {}",
                        issue.getCode(), issue.getCode() + "-MI", e.getMessage());
            } else {
                LOG.error("领料红冲 GL 凭证异常（吞异常保持幂等），领料单 {} billHeadCode={}",
                        issue.getCode(), issue.getCode() + "-MI", e);
            }
        }

        // 2. 反向 OUTGOING 库存移动单（经 relatedBillType+relatedBillCode 反查原移动单 → IErpInvStockMoveBiz.reverse
        //    生成 REVERSAL 反向移动单，余额自动回滚）
        ErpInvStockMove originalMove = findIssueMove(issue.getCode());
        if (originalMove != null) {
            try {
                stockMoveBiz.reverse(originalMove.getId(), context);
            } catch (Exception e) {
                if (e instanceof NopException) {
                    LOG.warn("领料红冲反向库存移动失败（吞异常保持幂等），领料单 {} moveCode={}: {}",
                            issue.getCode(), originalMove.getCode(), e.getMessage());
                } else {
                    LOG.error("领料红冲反向库存移动异常（吞异常保持幂等），领料单 {} moveCode={}",
                            issue.getCode(), originalMove.getCode(), e);
                }
            }
        }

        // 3. 翻 posted=false + docStatus=CANCELLED（状态机终态）。
        //    跨域 reverse 调用可能扰动会话脏跟踪，故重新加载并以 updateEntity 显式持久化（对齐 confirm 范式）。
        issue = requireIssue(issueId, context);
        doReverseConfirm(issue, context);
        return issue;
    }

    // ---------- step：红冲守卫与终态（protected，供派生复用与覆盖） ----------

    /**
     * 红冲前置守卫：仅 posted=true 且 docStatus=DONE（已 confirm 出库）的领料单可红冲。
     * 未过账或已 CANCELLED 抛 ERR_MATERIAL_ISSUE_NOT_POSTED。
     */
    protected void validateCanReverse(ErpMfgMaterialIssue issue, IServiceContext context) {
        String status = issue.getDocStatus();
        if (!Boolean.TRUE.equals(issue.getPosted())
                || !Objects.equals(status, ErpMfgConstants.ISSUE_STATUS_DONE)) {
            throw new NopException(ErpMfgErrors.ERR_MATERIAL_ISSUE_NOT_POSTED)
                    .param(ErpMfgErrors.ARG_WORK_ORDER_CODE, issue.getCode());
        }
    }

    /**
     * 翻 posted=false + docStatus=CANCELLED（红冲终态）。对齐 confirm 反向操作。
     */
    protected void doReverseConfirm(ErpMfgMaterialIssue issue, IServiceContext context) {
        issue.setDocStatus(ErpMfgConstants.ISSUE_STATUS_CANCELLED);
        issue.setPosted(false);
        issueDao().updateEntity(issue);
    }
}

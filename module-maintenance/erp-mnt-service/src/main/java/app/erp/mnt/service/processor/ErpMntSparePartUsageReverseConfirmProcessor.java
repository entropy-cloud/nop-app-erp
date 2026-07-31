package app.erp.mnt.service.processor;

import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.mnt.dao.entity.ErpMntSparePartUsage;
import io.nop.core.context.IServiceContext;
import io.nop.api.core.exceptions.NopException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ErpMntSparePartUsage reverseConfirm per-mutation Processor（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含备件消耗红冲编排：已过账守卫 + [会计保护区域] GL 凭证红冲（try-catch 吞异常保持幂等）+ 反向 OUTGOING 移动单
 * （try-catch 吞异常保持幂等）+ session-reload + posted=false/docStatus=CANCELLED 终态。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。共享 helper 单一真相源在 {@link AbstractErpMntSparePartUsageProcessor}。
 *
 * <p>[会计保护区域] 红冲惯法逐字搬运自原 ErpMntSparePartUsageBizModel.reverseConfirm：issuePostingDispatcher.reverseIssue
 * try-catch 吞异常 + stockMoveBiz.reverse try-catch 吞异常 + 跨域调用后 session-reload 显式持久化，对照 TestErpMntSparePartUsageReversal 校验语义不变。
 */
public class ErpMntSparePartUsageReverseConfirmProcessor extends AbstractErpMntSparePartUsageProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(ErpMntSparePartUsageReverseConfirmProcessor.class);

    public ErpMntSparePartUsage reverseConfirm(Long usageId, IServiceContext context) {
        ErpMntSparePartUsage usage = requireUsage(usageId, context);
        validateCanReverse(usage, context);

        // 1. 红冲 MAINTENANCE_ISSUE 凭证（try/catch 吞异常告警保持幂等，对齐 dispatchIfApplicable 正向过账范式；
        //    IErpFinVoucherBiz.reverse platform 内置幂等守护，无凭证时安全 no-op）
        try {
            issuePostingDispatcher.reverseIssue(usage);
        } catch (Exception e) {
            if (e instanceof NopException) {
                LOG.warn("备件消耗红冲 GL 凭证失败（吞异常保持幂等），消耗单 {} billHeadCode={}: {}",
                        usage.getCode(), usage.getCode() + "-MI", e.getMessage());
            } else {
                LOG.error("备件消耗红冲 GL 凭证异常（吞异常保持幂等），消耗单 {} billHeadCode={}",
                        usage.getCode(), usage.getCode() + "-MI", e);
            }
        }

        // 2. 反向 OUTGOING 库存移动单（经 relatedBillType+relatedBillCode 反查原移动单 → IErpInvStockMoveBiz.reverse
        //    生成 REVERSAL 反向移动单，余额自动回滚，对齐 1934-1 委外红冲范式）
        ErpInvStockMove originalMove = findIssueMove(usage.getCode());
        if (originalMove != null) {
            try {
                stockMoveBiz.reverse(originalMove.getId(), context);
            } catch (Exception e) {
                if (e instanceof NopException) {
                    LOG.warn("备件消耗红冲反向库存移动失败（吞异常保持幂等），消耗单 {} moveCode={}: {}",
                            usage.getCode(), originalMove.getCode(), e.getMessage());
                } else {
                    LOG.error("备件消耗红冲反向库存移动异常（吞异常保持幂等），消耗单 {} moveCode={}",
                            usage.getCode(), originalMove.getCode(), e);
                }
            }
        }

        // 3. 翻 posted=false + docStatus=CANCELLED（状态机终态）。
        //    跨域 reverse 调用可能扰动会话脏跟踪，故重新加载并以 updateEntity 显式持久化（对齐 confirm 范式）。
        usage = usageDao().getEntityById(usageId);
        doReverseConfirm(usage, context);
        return usage;
    }
}

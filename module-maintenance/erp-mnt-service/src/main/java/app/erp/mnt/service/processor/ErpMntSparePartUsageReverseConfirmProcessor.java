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
 * <p>P1-CK-mnt-002：GL 红冲 / 反向库存任一步失败即抛 NopException 终止红冲（不再吞异常推进
 * CANCELLED 终态）。修复前 try-catch 吞咽 + 守卫封口（validateCanReverse posted 硬守卫）
 * 形成悬挂不可恢复三件套：失败→吞→翻 CANCELLED+posted=false→validateCanReverse 永久封死
 * 重试入口。lesson 09 业财过账吞异常悬挂红冲方向变体（mnt 站点），与 mfg-005 同根因。
 * 反向调用均经 IErpFinVoucherBiz.reverse / IErpInvStockMoveBiz.reverse 平台内置幂等守护，
 * 无凭证/无移动单时安全 no-op——本修复仅改变「失败处置」语义，不影响正向幂等路径。
 */
public class ErpMntSparePartUsageReverseConfirmProcessor extends AbstractErpMntSparePartUsageProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(ErpMntSparePartUsageReverseConfirmProcessor.class);

    public ErpMntSparePartUsage reverseConfirm(String usageId, IServiceContext context) {
        ErpMntSparePartUsage usage = requireUsage(usageId, context);
        validateCanReverse(usage, context);

        // 1. 红冲 MAINTENANCE_ISSUE 凭证（IErpFinVoucherBiz.reverse 内置幂等守护，无凭证时安全 no-op）。
        //    P1-CK-mnt-002：失败不再吞——抛 NopException 终止红冲，@BizMutation 回滚事务，
        //    docStatus 保持 ACTIVE+posted=true 可重试。
        try {
            issuePostingDispatcher.reverseIssue(usage);
        } catch (NopException e) {
            LOG.error("Spare-part issue GL voucher reversal failed, reversal aborted (kept ACTIVE retryable), usage {} billHeadCode={}: {}",
                    usage.getCode(), usage.getCode() + "-MI", e.getMessage());
            throw e;
        }

        // 2. 反向 OUTGOING 库存移动单（IErpInvStockMoveBiz.reverse 内置幂等守护）。
        //    同上：失败不再吞——抛 NopException 终止红冲。
        ErpInvStockMove originalMove = findIssueMove(usage.getCode());
        if (originalMove != null) {
            try {
                stockMoveBiz.reverse(originalMove.getId(), context);
            } catch (NopException e) {
                LOG.error("Spare-part issue reverse stock move failed, reversal aborted (kept ACTIVE retryable), usage {} moveCode={}: {}",
                        usage.getCode(), originalMove.getCode(), e.getMessage());
                throw e;
            }
        }

        // 3. 翻 posted=false + docStatus=CANCELLED（状态机终态）。
        //    跨域 reverse 调用可能扰动会话脏跟踪，故重新加载并以 updateEntity 显式持久化（对齐 confirm 范式）。
        usage = usageDao().getEntityById(usageId);
        doReverseConfirm(usage, context);
        return usage;
    }
}

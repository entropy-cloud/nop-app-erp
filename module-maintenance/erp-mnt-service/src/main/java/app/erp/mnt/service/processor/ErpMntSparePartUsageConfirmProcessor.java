package app.erp.mnt.service.processor;

import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.mnt.dao.entity.ErpMntSparePartUsage;
import app.erp.mnt.dao.entity.ErpMntSparePartUsageLine;
import io.nop.core.context.IServiceContext;

import java.util.List;

/**
 * ErpMntSparePartUsage confirm per-mutation Processor（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含备件消耗确认出库编排：守卫 + 行加载 + 行非空校验 + 跨域出库（IErpInvStockMoveBiz.generateMove）
 * + [会计保护区域] session-reload + applyIssueResult + 落库 + 备件消耗 GL 过账（Dr: 维修费用 / Cr: 存货），config-gated 默认关。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。共享 helper 单一真相源在 {@link AbstractErpMntSparePartUsageProcessor}。
 *
 * <p>[会计保护区域] GL 过账 + session-reload 惯法逐字搬运自原 ErpMntSparePartUsageBizModel.confirm：
 * 跨域 generateMove 调用后重新加载并以 updateEntity 显式持久化 + dispatchIfApplicable 内部门控，对照 TestErpMntSparePartPosting 校验语义不变。
 */
public class ErpMntSparePartUsageConfirmProcessor extends AbstractErpMntSparePartUsageProcessor {

    public ErpMntSparePartUsage confirm(Long usageId, IServiceContext context) {
        ErpMntSparePartUsage usage = requireUsage(usageId, context);
        validateNotConfirmed(usage, context);
        List<ErpMntSparePartUsageLine> lines = loadLines(usageId);
        validateLinesNonEmpty(usage, lines, context);

        ErpInvStockMove move = sparePartIssueService.issue(usage, lines, context);

        // 跨域 generateMove 调用可能扰动会话脏跟踪，故重新加载并以 updateEntity 显式持久化。
        usage = usageDao().getEntityById(usageId);
        applyIssueResult(usage, lines, move, context);
        usageDao().updateEntity(usage);

        // 备件消耗 GL 过账（Dr: 维修费用 / Cr: 存货），config-gated 默认关（dispatchIfApplicable 内部门控，
        // erp-mnt.spare-part-posting-enabled=false 时不生成凭证，向后兼容）。maintenance 域独占，InvPostingDispatcher
        // 对 ERP_MNT_SPARE_PART 显式跳过交由本域处理。镜像 ManufacturingIssuePostingDispatcher 显式调用范式。
        if (isStockIssued(move)) {
            issuePostingDispatcher.dispatchIfApplicable(usageId);
        }
        return usage;
    }
}

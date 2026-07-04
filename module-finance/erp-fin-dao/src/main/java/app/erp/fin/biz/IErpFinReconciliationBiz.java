
package app.erp.fin.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.fin.dao.dto.AutoReconResult;
import app.erp.fin.dao.dto.DualSideDiffReport;
import app.erp.fin.dao.dto.ReconciliationLineInput;
import app.erp.fin.dao.entity.ErpFinReconciliation;

import java.time.LocalDate;
import java.util.List;

public interface IErpFinReconciliationBiz extends ICrudBiz<ErpFinReconciliation> {

    /**
     * 创建草稿核销单（头 + 行）。维度（账套/组织/币种/汇率）从首条发票项继承；
     * 行仅记录引用与核销金额，实际回写辅助账在 {@link #post} 时发生。
     */
    @BizMutation
    ErpFinReconciliation create(@Name("direction") String direction,
                                @Name("partnerId") Long partnerId,
                                @Name("businessDate") LocalDate businessDate,
                                @Name("lines") List<ReconciliationLineInput> lines,
                                IServiceContext context);

    /**
     * 过账核销单：校验核销约束，回写双方辅助账 settled/open/status（OPEN→PARTIAL→SETTLED），
     * 置核销单 docStatus=POSTED，并重算往来单位余额。
     */
    @BizMutation
    ErpFinReconciliation post(@Name("reconciliationId") Long reconciliationId, IServiceContext context);

    /**
     * 红冲核销单：恢复双方辅助账 settled/open/status，置核销单 docStatus=REVERSED，重算往来单位余额。
     */
    @BizMutation
    ErpFinReconciliation reverse(@Name("reconciliationId") Long reconciliationId, IServiceContext context);

    /**
     * 自动核销（{@code ar-ap-reconciliation.md §自动核销}）：按 {@code strategy}（FIFO/BY_AMOUNT/BY_RATIO，
     * 默认从 {@code erp-fin.auto-recon-strategy} 读取）匹配指定 partner+direction 的 OPEN/PARTIAL 发票项与收付款项，
     * 生成核销候选行，复用 {@link #create}+{@link #post} 路径落核销单。
     *
     * <p>config-gated：{@code erp-fin.auto-reconcile=false} 时抛 {@code ERR_AUTO_RECON_DISABLED}。
     * {@code partnerId=null} 时遍历所有有开口余额的 partner，分 partner 独立核销单。
     * 幂等：候选查询已排除 SETTLED/CANCELLED 项，重复执行只处理剩余开口项。
     *
     * @param direction  方向（RECEIVABLE/PAYABLE）
     * @param partnerId  往来单位 ID；null 表示全 partner
     * @param strategy   分摊策略（FIFO/BY_AMOUNT/BY_RATIO）；null 时从配置读取
     */
    @BizMutation
    AutoReconResult runAutoReconciliation(@Name("direction") String direction,
                                          @Name("partnerId") Long partnerId,
                                          @Name("strategy") String strategy,
                                          IServiceContext context);

    /**
     * 双面对账一致性兜底（plan 2026-07-05-0115-1 Phase 3）：比对 finance 侧 {@code ErpFinArApItem}
     * 已核销额聚合 vs 域级侧（purchase {@code ErpPurInvoice.paidAmount} / sales {@code ErpSalInvoice.receivedAmount}）
     * 已核销额聚合，产出 partner 级差异报告。差额超 {@code erp-fin.reconcile-precision} 标记 INCONSISTENT 并日志告警。
     *
     * <p>只读 + 报告，不自动修复（避免静默修改域级核销权威）。
     *
     * @param direction  方向（RECEIVABLE/PAYABLE）
     * @param partnerId  往来单位 ID；null 表示全 partner
     */
    @BizQuery
    DualSideDiffReport checkDualSideConsistency(@Name("direction") String direction,
                                                @Name("partnerId") Long partnerId,
                                                IServiceContext context);
}


package app.erp.fin.service.posting;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PostingEvent;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 承付（COMMITMENT）凭证生成 Provider（A2，plan 2026-07-21-1206-2，budget.md §承付会计）。
 *
 * <p><b>设计选择（与 BUDGET 同型）</b>：承付作为 {@code postingType=COMMITMENT} 的影子凭证，与实际凭证并行入账，
 * **不走 Provider 路由**（{@link ErpFinAcctDocRegistry} 不路由 COMMITMENT 业务类型）。承付凭证直接由
 * {@code CommitmentVoucherGenerator} 写入，与 {@code BudgetVoucherGenerator} 同型。
 *
 * <p>本类存在仅为：
 * <ul>
 *   <li>文档化承付科目解析约定（{@code erp-fin.budget-commitment-subject-code} 配置）</li>
 *   <li>满足 {@link IErpFinAcctDocProvider} 接口约定（注册中心可发现，但实际承付 flow 不经路由）</li>
 *   <li>为 successor（多维承付科目解析）保留接入点</li>
 * </ul>
 *
 * <p>{@link #getSupportedBusinessTypes()} 返回空集 → 注册中心不路由任何业务类型到此 Provider。
 */
public class CommitmentAcctDocProvider implements IErpFinAcctDocProvider {

    /**
     * 返回空集。承付 flow 不经 ErpFinAcctDocRegistry 路由（与 BUDGET 同型，直接由
     * {@code CommitmentVoucherGenerator} 写入凭证），故本 Provider 不声明支持任何业务类型。
     */
    @Override
    public Set<ErpFinBusinessType> getSupportedBusinessTypes() {
        return Collections.emptySet();
    }

    /**
     * 不应被调用。承付凭证直接经 {@code CommitmentVoucherGenerator.generateCommitment} 写入；
     * 本方法仅为满足接口约定，调用时返回空列表。
     */
    @Override
    public List<VoucherFact> createFacts(PostingEvent event, AcctDocContext ctx) {
        return Collections.emptyList();
    }
}

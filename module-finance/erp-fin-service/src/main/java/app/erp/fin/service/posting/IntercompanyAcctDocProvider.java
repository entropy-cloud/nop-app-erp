package app.erp.fin.service.posting;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PostingEvent;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 跨法人内部交易凭证生成 Provider（A3，plan 2026-07-22-1000-1，multi-company.md §Decision B）。
 *
 * <p><b>设计选择（与 BUDGET/COMMITMENT 同型）</b>：跨法人配对凭证是「一次调拨 → 两条凭证（双法人双账套）」，
 * 与单 Provider 单凭证模型不匹配，**不走 {@link ErpFinAcctDocRegistry} 路由**。配对凭证直接由
 * {@code IntercompanyVoucherGenerator} 写入（与 {@code CommitmentVoucherGenerator}/{@code BudgetVoucherGenerator} 同型）。
 *
 * <p>本类存在仅为文档化 INTERCOMPANY_* 科目解析约定（{@code IErpFinGlMappingResolver} 按 INTERCOMPANY_AR/AP/REVENUE/COST
 * accountKey 解析）+ 为 successor（多维 intercompany 科目解析）保留接入点。
 *
 * <p>{@link #getSupportedBusinessTypes()} 返回空集 → 注册中心不路由任何业务类型到此 Provider。
 */
public class IntercompanyAcctDocProvider implements IErpFinAcctDocProvider {

    @Override
    public Set<ErpFinBusinessType> getSupportedBusinessTypes() {
        return Collections.emptySet();
    }

    @Override
    public List<VoucherFact> createFacts(PostingEvent event, AcctDocContext ctx) {
        return Collections.emptyList();
    }
}

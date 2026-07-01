package app.erp.fin.service.posting.provider;

import app.erp.fin.dao.entity.ErpFinVoucherTemplate;
import app.erp.fin.dao.entity.ErpFinVoucherTemplateLine;
import app.erp.fin.service.posting.AcctDocContext;
import app.erp.fin.service.posting.ErpFinBusinessType;
import app.erp.fin.service.posting.ErpFinPostingErrors;
import app.erp.fin.service.posting.IErpFinAcctDocProvider;
import app.erp.fin.service.posting.PostingEvent;
import app.erp.fin.service.posting.VoucherFact;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.commons.util.StringHelper;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.isNull;
import static io.nop.api.core.beans.FilterBeans.or;

/**
 * 财务域内置的默认/兜底过账 Provider。按 {@code businessType}+{@code acctSchemaId} 读取
 * {@link ErpFinVoucherTemplate}(+{@link ErpFinVoucherTemplateLine})，按 {@code amountKey} 填充金额、
 * 按 {@code subjectCode}(支持 {@code ${key}} 占位符) 解析科目，产出 {@link VoucherFact} 列表。
 *
 * <p>标记为 fallback（{@link #isFallback()} 返回 {@code true}）：在注册中心中域专属 Provider 优先，
 * 本 Provider 仅兜底未被任何域 Provider 接管的核心业务类型。使引擎在无任何业务域 Provider 时即可端到端验证，
 * 并作为后续 Pur/Sal/Inv Provider 的参照实现。
 *
 * <p>声明支持 {@code posting.md} 核心业务类型表中的 6 类进销存+收付款：
 * {PURCHASE_INPUT, SALES_OUTPUT, AP_INVOICE, AR_INVOICE, PAYMENT, RECEIPT}。
 */
public class ErpFinTemplateAcctDocProvider implements IErpFinAcctDocProvider {

    private static final Set<ErpFinBusinessType> SUPPORTED = Collections.unmodifiableSet(EnumSet.of(
            ErpFinBusinessType.PURCHASE_INPUT, ErpFinBusinessType.SALES_OUTPUT,
            ErpFinBusinessType.AP_INVOICE, ErpFinBusinessType.AR_INVOICE,
            ErpFinBusinessType.PAYMENT, ErpFinBusinessType.RECEIPT));

    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([^}]+)}");

    @Inject
    IDaoProvider daoProvider;

    @Override
    public Set<ErpFinBusinessType> getSupportedBusinessTypes() {
        return SUPPORTED;
    }

    @Override
    public boolean isFallback() {
        return true;
    }

    @Override
    public List<VoucherFact> createFacts(PostingEvent event, AcctDocContext ctx) {
        ErpFinVoucherTemplate template = findTemplate(event);
        if (template == null) {
            throw new NopException(ErpFinPostingErrors.ERR_TEMPLATE_NOT_FOUND)
                    .param(ErpFinPostingErrors.ARG_BUSINESS_TYPE, event.getBusinessType());
        }

        Integer voucherType = template.getVoucherType();
        if (voucherType != null) {
            ctx.setVoucherType(voucherType);
        }

        List<ErpFinVoucherTemplateLine> lines = loadLines(template);
        Map<String, Object> billData = event.getBillData();
        List<VoucherFact> facts = new ArrayList<>(lines.size());
        for (ErpFinVoucherTemplateLine line : lines) {
            VoucherFact fact = new VoucherFact();
            fact.setSubjectCode(resolvePlaceholders(line.getSubjectCode(), billData));
            fact.setDcDirection(line.getDcDirection());
            fact.setAmount(resolveAmount(line, billData));
            fact.setAccountKey(line.getAccountKey());
            fact.setAmountKey(line.getAmountKey());
            fact.setMemo(line.getMemoTemplate());
            fact.setBusinessType(event.getBusinessType().getCode());
            facts.add(fact);
        }
        return facts;
    }

    private ErpFinVoucherTemplate findTemplate(PostingEvent event) {
        IEntityDao<ErpFinVoucherTemplate> dao = daoProvider.daoFor(ErpFinVoucherTemplate.class);
        int code = event.getBusinessType().getCode();
        Long acctSchemaId = event.getAcctSchemaId();

        QueryBean q = new QueryBean();
        q.addFilter(eq("businessType", code));
        q.addFilter(eq("isActive", Boolean.TRUE));
        q.addFilter(or(eq("acctSchemaId", acctSchemaId), isNull("acctSchemaId")));
        List<ErpFinVoucherTemplate> candidates = dao.findAllByQuery(q);
        if (candidates.isEmpty()) {
            return null;
        }

        LocalDate voucherDate = event.getVoucherDate();
        ErpFinVoucherTemplate generic = null;
        ErpFinVoucherTemplate specific = null;
        for (ErpFinVoucherTemplate t : candidates) {
            if (!inValidRange(t, voucherDate)) {
                continue;
            }
            if (t.getAcctSchemaId() != null) {
                if (t.getAcctSchemaId().equals(acctSchemaId) && specific == null) {
                    specific = t;
                }
            } else if (generic == null) {
                generic = t;
            }
        }
        return specific != null ? specific : generic;
    }

    private boolean inValidRange(ErpFinVoucherTemplate t, LocalDate voucherDate) {
        if (voucherDate == null) {
            return true;
        }
        LocalDate from = t.getValidFrom();
        LocalDate to = t.getValidTo();
        if (from != null && voucherDate.isBefore(from)) {
            return false;
        }
        if (to != null && voucherDate.isAfter(to)) {
            return false;
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private List<ErpFinVoucherTemplateLine> loadLines(ErpFinVoucherTemplate template) {
        List<ErpFinVoucherTemplateLine> lines = new ArrayList<>(template.getLines());
        lines.sort(Comparator.comparingInt(l -> l.getLineNo() == null ? Integer.MAX_VALUE : l.getLineNo()));
        return lines;
    }

    private BigDecimal resolveAmount(ErpFinVoucherTemplateLine line, Map<String, Object> billData) {
        String amountKey = line.getAmountKey();
        BigDecimal amount = null;
        if (!StringHelper.isBlank(amountKey)) {
            Object raw = billData.get(amountKey);
            if (raw == null) {
                throw new NopException(ErpFinPostingErrors.ERR_AMOUNT_KEY_NOT_RESOLVED)
                        .param(ErpFinPostingErrors.ARG_AMOUNT_KEY, amountKey);
            }
            amount = toBigDecimal(raw, amountKey);
        }
        if (amount == null) {
            String expr = line.getAmountExpression();
            if (!StringHelper.isBlank(expr)) {
                amount = toBigDecimal(expr.trim(), expr);
            }
        }
        return amount != null ? amount : BigDecimal.ZERO;
    }

    private BigDecimal toBigDecimal(Object raw, String label) {
        try {
            return new BigDecimal(raw.toString().trim());
        } catch (NumberFormatException e) {
            throw new NopException(ErpFinPostingErrors.ERR_AMOUNT_KEY_NOT_RESOLVED)
                    .param(ErpFinPostingErrors.ARG_AMOUNT_KEY, label).cause(e);
        }
    }

    private String resolvePlaceholders(String template, Map<String, Object> billData) {
        if (StringHelper.isBlank(template)) {
            return template;
        }
        Matcher m = PLACEHOLDER.matcher(template);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String key = m.group(1);
            Object val = billData.get(key);
            m.appendReplacement(sb, Matcher.quoteReplacement(val == null ? "" : val.toString()));
        }
        m.appendTail(sb);
        return sb.toString();
    }
}

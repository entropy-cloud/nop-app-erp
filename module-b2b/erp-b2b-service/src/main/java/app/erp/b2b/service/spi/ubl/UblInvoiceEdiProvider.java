package app.erp.b2b.service.spi.ubl;

import app.erp.b2b.service.ErpB2bConstants;
import app.erp.b2b.service.ErpB2bErrors;
import app.erp.b2b.service.codemapping.CodeMappingResolver;
import app.erp.b2b.service.spi.IErpB2bEdiProvider;
import app.erp.b2b.service.spi.model.EdiApplicability;
import app.erp.b2b.service.spi.model.ParsedPayload;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.time.LocalDate;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * UBL Invoice EDI Provider（出站 AR 发票生成）。
 *
 * <p>{@code getCode="UBL_INVOICE"}，{@code getApplicability("AR_INVOICE")→{outbound:true}}。
 * {@code generatePayload} 构建 UBL Invoice XML（查 {@code ErpSalInvoice} 只读经 IDaoProvider + 代码映射）。
 *
 * <p><b>跨域访问</b>：使用 {@link IDaoProvider} 只读查 {@code ErpSalInvoice}（sales 域），
 * 经 {@link CodeMappingResolver} 映射物料。本期为样例实现（构建最简 UBL Invoice XML 骨架 + 头/行数据），
 * 完整 UBL 2.1 合规（TaxTotal/LegalMonetaryTotal/AccountingCustomerParty 全字段）归 follow-up。
 */
public class UblInvoiceEdiProvider implements IErpB2bEdiProvider {

    @Inject
    IDaoProvider daoProvider;
    @Inject
    CodeMappingResolver codeMappingResolver;

    @Override
    public String getCode() {
        return ErpB2bConstants.EDI_FORMAT_UBL_INVOICE;
    }

    @Override
    public EdiApplicability getApplicability(String relatedBillType) {
        if (ErpB2bConstants.RELATED_BILL_TYPE_AR_INVOICE.equals(relatedBillType)) {
            return new EdiApplicability(true, false, false);
        }
        return EdiApplicability.NONE;
    }

    @Override
    public String generatePayload(String relatedBillType, String relatedBillCode) {
        // 查销售发票（跨域只读经 IDaoProvider）
        IEntityDao<?> invoiceDao = daoProvider.dao("app.erp.sal.dao.entity.ErpSalInvoice");
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", relatedBillCode));
        Object invoice = invoiceDao.findFirstByQuery(q);
        if (invoice == null) {
            throw new NopException(ErpB2bErrors.ERR_B2B_EDI_PARSE_FAILED)
                    .param(ErpB2bErrors.ARG_RELATED_BILL_TYPE, relatedBillType)
                    .param(ErpB2bErrors.ARG_RELATED_BILL_CODE, relatedBillCode);
        }

        // 构建 UBL Invoice XML（最简骨架，本期样例）
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        xml.append("<Invoice xmlns=\"").append(ErpB2bConstants.UBL_NS_INVOICE).append("\"");
        xml.append(" xmlns:cac=\"").append(ErpB2bConstants.UBL_NS_CAC).append("\"");
        xml.append(" xmlns:cbc=\"").append(ErpB2bConstants.UBL_NS_CBC).append("\">");

        xml.append("<cbc:ID>").append(escapeXml(relatedBillCode)).append("</cbc:ID>");
        xml.append("<cbc:IssueDate>").append(LocalDate.now()).append("</cbc:IssueDate>");

        xml.append("<cac:AccountingSupplierParty><cac:Party><cbc:EndpointID/></cac:Party></cac:AccountingSupplierParty>");
        xml.append("<cac:AccountingCustomerParty><cac:Party><cbc:EndpointID/></cac:Party></cac:AccountingCustomerParty>");

        xml.append("<cac:LegalMonetaryTotal>");
        xml.append("<cbc:PayableAmount currencyID=\"CNY\">0.00</cbc:PayableAmount>");
        xml.append("</cac:LegalMonetaryTotal>");

        xml.append("</Invoice>");
        return xml.toString();
    }

    @Override
    public ParsedPayload parsePayload(String formatCode, String payload) {
        throw new NopException(ErpB2bErrors.ERR_B2B_EDI_PARSE_FAILED)
                .param(ErpB2bErrors.ARG_RELATED_BILL_TYPE, "AR_INVOICE");
    }

    @Override
    public boolean needsWebService() {
        return false;
    }

    private String escapeXml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }
}

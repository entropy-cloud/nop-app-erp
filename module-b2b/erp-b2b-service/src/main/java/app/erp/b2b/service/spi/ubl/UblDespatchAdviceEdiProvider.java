package app.erp.b2b.service.spi.ubl;

import app.erp.b2b.service.ErpB2bConstants;
import app.erp.b2b.service.ErpB2bErrors;
import app.erp.b2b.service.codemapping.CodeMappingResolver;
import app.erp.b2b.service.spi.IErpB2bEdiProvider;
import app.erp.b2b.service.spi.model.EdiApplicability;
import app.erp.b2b.service.spi.model.ParsedPayload;
import app.erp.b2b.service.spi.model.ParsedPayload.ParsedLine;
import io.nop.api.core.exceptions.NopException;
import jakarta.inject.Inject;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * UBL DespatchAdvice EDI Provider（入站 ASN 解析）。
 *
 * <p>{@code getCode="UBL_DESPATCH_ADVICE"}，{@code getApplicability("ASN_INBOUND")→{inbound:true}}。
 * 解析 UBL DespatchAdvice XML（DespatchSupplierParty→partnerCode、DespatchLine→ParsedLine、OrderReference→relatedBillCode），
 * 纯文本 XML 解析无外部依赖。物料代码经 {@link CodeMappingResolver} 映射。
 *
 * <p>对应 {@code edi-formats.md §3.3}。UBL 样例覆盖双向契约；X12/EDIFACT 归 Non-Goal。
 */
public class UblDespatchAdviceEdiProvider implements IErpB2bEdiProvider {

    @Inject
    CodeMappingResolver codeMappingResolver;

    @Override
    public String getCode() {
        return ErpB2bConstants.EDI_FORMAT_UBL_DESPATCH_ADVICE;
    }

    @Override
    public EdiApplicability getApplicability(String relatedBillType) {
        if (ErpB2bConstants.RELATED_BILL_TYPE_ASN_INBOUND.equals(relatedBillType)) {
            return new EdiApplicability(false, true, false);
        }
        return EdiApplicability.NONE;
    }

    @Override
    public String generatePayload(String relatedBillType, String relatedBillCode) {
        throw new NopException(ErpB2bErrors.ERR_B2B_EDI_PARSE_FAILED)
                .param(ErpB2bErrors.ARG_RELATED_BILL_TYPE, relatedBillType)
                .param(ErpB2bErrors.ARG_RELATED_BILL_CODE, relatedBillCode);
    }

    @Override
    public ParsedPayload parsePayload(String formatCode, String payload) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new java.io.ByteArrayInputStream(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8)));

            ParsedPayload result = new ParsedPayload();
            result.setEdiFormatCode(getCode());
            result.setRawPayload(payload);
            result.setRelatedBillType(ErpB2bConstants.RELATED_BILL_TYPE_PO_ORDER);

            // DespatchSupplierParty → partnerCode
            String supplierCode = getTextByTagNS(doc, ErpB2bConstants.UBL_NS_CAC, "DespatchSupplierParty",
                    ErpB2bConstants.UBL_NS_CBC, "CustomerAssignedAccountID");
            if (supplierCode == null) {
                supplierCode = getTextByTagNS(doc, ErpB2bConstants.UBL_NS_CAC, "DespatchSupplierParty",
                        ErpB2bConstants.UBL_NS_CBC, "ID");
            }
            result.setPartnerCode(supplierCode);

            // OrderReference → relatedBillCode（采购订单号）
            String orderCode = getChildText(doc, ErpB2bConstants.UBL_NS_CAC, "OrderReference",
                    ErpB2bConstants.UBL_NS_CBC, "ID");
            result.setRelatedBillCode(orderCode);

            // EstimatedDeliveryDate → headers
            String deliveryDate = getChildText(doc, ErpB2bConstants.UBL_NS_CAC, "Delivery",
                    ErpB2bConstants.UBL_NS_CBC, "ActualDeliveryDate");
            if (deliveryDate == null) {
                deliveryDate = getChildText(doc, ErpB2bConstants.UBL_NS_CAC, "Delivery",
                        ErpB2bConstants.UBL_NS_CBC, "RequestedDeliveryDate");
            }
            if (deliveryDate != null) {
                result.getHeaders().put("estimatedArrivalDate", LocalDate.parse(deliveryDate));
            }

            // DespatchLine → ParsedLine(s)
            NodeList lineNodes = doc.getElementsByTagNameNS(ErpB2bConstants.UBL_NS_CAC, "DespatchLine");
            for (int i = 0; i < lineNodes.getLength(); i++) {
                Element lineEl = (Element) lineNodes.item(i);
                ParsedLine line = new ParsedLine();

                line.setLineNo(getDirectChildText(lineEl, ErpB2bConstants.UBL_NS_CBC, "ID"));
                line.setShippedQty(getDecimalDirectChild(lineEl, ErpB2bConstants.UBL_NS_CBC, "DeliveredQuantity"));
                if (line.getShippedQty() == null) {
                    line.setShippedQty(getDecimalDirectChild(lineEl, ErpB2bConstants.UBL_NS_CBC, "Quantity"));
                }
                line.setQuantity(line.getShippedQty());

                // Item/SellersItemIdentification/ID → supplierPartNo
                Element itemEl = getDirectChild(lineEl, ErpB2bConstants.UBL_NS_CAC, "Item");
                if (itemEl != null) {
                    Element sellersItemEl = getDirectChild(itemEl, ErpB2bConstants.UBL_NS_CAC, "SellersItemIdentification");
                    if (sellersItemEl != null) {
                        line.setSupplierPartNo(getDirectChildText(sellersItemEl, ErpB2bConstants.UBL_NS_CBC, "ID"));
                    }
                }

                // 代码映射：partnerCode (supplier) + MATERIAL + supplierPartNo → internal materialCode
                // 注意：partnerId 在 Provider 层尚不可用（需经 partnerCode 查 Partner），
                // 此处仅保留 supplierPartNo，映射在 ASN 入站处理层完成（parseToAsn）。
                line.setMaterialCode(line.getSupplierPartNo());

                result.getLines().add(line);
            }

            if (result.getLines().isEmpty()) {
                throw new NopException(ErpB2bErrors.ERR_B2B_EDI_PARSE_FAILED)
                        .param(ErpB2bErrors.ARG_RELATED_BILL_TYPE, "ASN_INBOUND");
            }

            return result;
        } catch (NopException e) {
            throw e;
        } catch (Exception e) {
            throw new NopException(ErpB2bErrors.ERR_B2B_EDI_PARSE_FAILED, e)
                    .param(ErpB2bErrors.ARG_RELATED_BILL_TYPE, "ASN_INBOUND");
        }
    }

    @Override
    public boolean needsWebService() {
        return false;
    }

    // ---------- XML helpers ----------

    private String getTextByTagNS(Document doc, String parentNs, String parentTag, String childNs, String childTag) {
        NodeList parents = doc.getElementsByTagNameNS(parentNs, parentTag);
        if (parents.getLength() == 0) {
            return null;
        }
        Element parent = (Element) parents.item(0);
        return getDirectChildText(parent, childNs, childTag);
    }

    private String getChildText(Document doc, String parentNs, String parentTag, String childNs, String childTag) {
        NodeList parents = doc.getElementsByTagNameNS(parentNs, parentTag);
        if (parents.getLength() == 0) {
            return null;
        }
        Element parent = (Element) parents.item(0);
        return getDirectChildText(parent, childNs, childTag);
    }

    private String getDirectChildText(Element parent, String ns, String tag) {
        Element child = getDirectChild(parent, ns, tag);
        return child != null ? child.getTextContent() : null;
    }

    private BigDecimal getDecimalDirectChild(Element parent, String ns, String tag) {
        String text = getDirectChildText(parent, ns, tag);
        if (text == null || text.isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Element getDirectChild(Element parent, String ns, String tag) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i) instanceof Element) {
                Element el = (Element) children.item(i);
                if (ns.equals(el.getNamespaceURI()) && tag.equals(el.getLocalName())) {
                    return el;
                }
            }
        }
        return null;
    }
}

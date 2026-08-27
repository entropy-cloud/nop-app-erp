package app.erp.fin.service.classify;

import app.erp.fin.dao.entity.ErpFinApDocument;
import io.nop.core.context.IServiceContext;

import java.util.Map;

/**
 * E3.5 AP 文档分类引擎 SPI（`document-driven-ap-automation.md` §2：Phase 1 规则引擎为默认实现；
 * Phase 2 ML 分类器经 SPI 注入预留，对齐 {@code IErpFinAcctDocProvider} 注入范式，触发条件 =
 * 规则分类准确率不满足业务需求）。
 */
public interface IErpFinApDocClassifier {

    /**
     * 对已解析文档分类（单据类型 + 对应方 + 置信度）。
     *
     * @param parseResult 解析要素（supplierName/invoiceNo/invoiceDate/amount/tax/total/excerpt 等）
     */
    ApDocClassification classify(ErpFinApDocument doc, Map<String, Object> parseResult, IServiceContext context);
}

package app.erp.ct.dao.dto;

import app.erp.contract.dao.entity.ErpCtDocument;

import java.util.ArrayList;
import java.util.List;

/**
 * 合同文档搜索结果（{@code IErpCtDocumentBiz.searchDocuments} 返回值）。
 *
 * <p>携带文档实体 + 冗余合同编码（前端列表展示免二次查询）。keyword 命中为
 * fullTextSearch LIKE 析取（docName + ocrText + code + metadataTags 拼接字段，
 * owner doc §索引策略公式）。
 */
public class DocumentSearchResult {
    private List<ErpCtDocument> documents = new ArrayList<>();
    private String contractCode;

    public DocumentSearchResult() {
    }

    public DocumentSearchResult(List<ErpCtDocument> documents) {
        this.documents = documents == null ? new ArrayList<>() : documents;
    }

    public List<ErpCtDocument> getDocuments() {
        return documents;
    }

    public void setDocuments(List<ErpCtDocument> documents) {
        this.documents = documents;
    }

    public String getContractCode() {
        return contractCode;
    }

    public void setContractCode(String contractCode) {
        this.contractCode = contractCode;
    }
}

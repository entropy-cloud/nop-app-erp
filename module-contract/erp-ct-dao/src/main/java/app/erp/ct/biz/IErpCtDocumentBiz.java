package app.erp.ct.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.orm.biz.ICrudBiz;
import io.nop.core.context.IServiceContext;

import app.erp.contract.dao.entity.ErpCtDocument;
import app.erp.ct.dao.dto.DocumentSearchResult;


/**
 * 合同文档业务接口（{@code docs/design/contract/contract-repository.md}）。除标准 CRUD 外：
 *
 * <ul>
 *   <li>{@link #setLegalHold}：admin 手动设置法律保留（true 阻止所有归档/销毁操作），
 *       Java 角色守卫 fail-closed。</li>
 *   <li>{@link #archive}：手动归档（retentionDate 到达自动归档经 job 复用本入口），
 *       Legal Hold / ACTIVE 合同 / 已归档幂等三守卫。</li>
 *   <li>{@link #purge}：文档销毁（D4 裁决 = 逻辑删除 + 销毁前审计记录；admin 角色 +
 *       legalHold + 已归档 + ACTIVE 合同 + purgeDate 到达五守卫；doc-auto-purge=false 时
 *       本入口即人工确认通道）。</li>
 *   <li>{@link #startOcr} / {@link #submitOcrText}：OCR 状态机入口
 *       （PENDING→PROCESSING→COMPLETED/FAILED，人工补录等同 COMPLETED）。</li>
 *   <li>{@link #searchDocuments}：全文 + 高级搜索（keyword contains fullTextSearch + 6 类过滤器，返回 wrapper）。</li>
 *   <li>{@link #archiveOverdueDocuments} / {@link #purgeOverdueDocuments}：保留策略批量扫描
 *       （job 复用；单条失败隔离）。</li>
 * </ul>
 */
public interface IErpCtDocumentBiz extends ICrudBiz<ErpCtDocument> {

    @BizMutation
    ErpCtDocument setLegalHold(@Name("documentId") String documentId,
                               @Name("legalHold") Boolean legalHold,
                               IServiceContext context);

    @BizMutation
    ErpCtDocument archive(@Name("documentId") String documentId, IServiceContext context);

    @BizMutation
    ErpCtDocument purge(@Name("documentId") String documentId, IServiceContext context);

    @BizMutation
    ErpCtDocument startOcr(@Name("documentId") String documentId, IServiceContext context);

    @BizMutation
    ErpCtDocument submitOcrText(@Name("documentId") String documentId,
                                @Name("ocrText") String ocrText,
                                IServiceContext context);

    @BizMutation
    int archiveOverdueDocuments(IServiceContext context);

    @BizMutation
    int purgeOverdueDocuments(IServiceContext context);

    @BizQuery
    DocumentSearchResult searchDocuments(@Optional @Name("keyword") String keyword,
                                         @Optional @Name("code") String code,
                                         @Optional @Name("docType") String docType,
                                         @Optional @Name("contractId") String contractId,
                                         @Optional @Name("uploadDateFrom") String uploadDateFrom,
                                         @Optional @Name("uploadDateTo") String uploadDateTo,
                                         @Optional @Name("ocrStatus") String ocrStatus,
                                         @Optional @Name("archived") Boolean archived,
                                         IServiceContext context);
}

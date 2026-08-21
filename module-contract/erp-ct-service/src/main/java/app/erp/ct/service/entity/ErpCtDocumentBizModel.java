
package app.erp.ct.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.api.core.auth.IUserContext;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.biz.crud.CrudBizModel;
import io.nop.biz.crud.EntityData;
import io.nop.core.context.IServiceContext;
import io.nop.core.lang.json.JsonTool;

import app.erp.contract.dao.entity.ErpCtContract;
import app.erp.contract.dao.entity.ErpCtDocument;
import app.erp.ct.biz.IErpCtDocumentBiz;
import app.erp.ct.dao.ErpCtDaoConstants;
import app.erp.ct.dao.dto.DocumentSearchResult;
import app.erp.ct.service.ErpCtConfigs;
import app.erp.ct.service.ErpCtConstants;
import app.erp.ct.service.ErpCtErrors;
import app.erp.ct.service.spi.ErpCtOcrEngineRegistry;
import app.erp.ct.service.spi.IErpCtOcrEngine;
import app.erp.ct.service.spi.model.OcrRecognizeRequest;
import app.erp.ct.service.spi.model.OcrRecognizeResponse;
import app.erp.notify.biz.IErpSysNotificationBiz;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.dateBetween;
import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.ge;
import static io.nop.api.core.beans.FilterBeans.le;
import static io.nop.api.core.beans.FilterBeans.contains;

/**
 * 合同文档仓库 BizModel（{@code docs/design/contract/contract-repository.md}）。
 *
 * <p>Legal Hold 三守卫（§合规规则）：{@code legalHold=true} 阻止所有归档/销毁操作；
 * 归档只读（归档后禁改禁删，仅 legalHold 合规字段可由 admin 调整）；ACTIVE 合同文档不归档。
 * generic save/update 管道中携带 legalHold 字段同样要求 admin 角色（fail-closed，防绕过
 * {@link #setLegalHold} 专用入口）。
 *
 * <p>OCR 状态机（§OCR 流程）：PENDING→PROCESSING→COMPLETED/FAILED，引擎经
 * {@link ErpCtOcrEngineRegistry} 按 config {@code erp-ct.ocr-engine} 派发
 * （默认 manual 无操作识别器→FAILED 引导人工补录；{@link #submitOcrText} 补录等同
 * COMPLETED 语义）。失败原因记 remark，FAILED 后可重新提交。
 *
 * <p>全文检索（§索引策略）：{@code fullTextSearch = docName + ocrText + code + metadataTags
 * 关键值} 拼接，上传/OCR 完成/补录/metadataTags 变更时重建（上限 4000 对齐列宽）。
 */
@BizModel("ErpCtDocument")
public class ErpCtDocumentBizModel extends CrudBizModel<ErpCtDocument> implements IErpCtDocumentBiz {

    static final Logger LOG = LoggerFactory.getLogger(ErpCtDocumentBizModel.class);

    static final int FULL_TEXT_MAX_LENGTH = 4000;

    @Inject
    ErpCtOcrEngineRegistry ocrEngineRegistry;

    /** 销毁审计通知（可选依赖：notify 子系统缺失时 best-effort 跳过）。 */
    @Inject
    IErpSysNotificationBiz notificationBiz;

    public ErpCtDocumentBizModel() {
        setEntityName(ErpCtDocument.class.getName());
    }

    public void setOcrEngineRegistry(ErpCtOcrEngineRegistry ocrEngineRegistry) {
        this.ocrEngineRegistry = ocrEngineRegistry;
    }

    // ---------- Legal Hold（admin 手动设置，owner doc §合规规则「法律保留」行） ----------

    /**
     * 设置/解除法律保留。角色守卫（Phase 2 Decision 裁决：载体 = @BizMutation + Java 角色守卫，
     * {@link IUserContext#isUserInRole(String)} 按 roleId 判定，镜像 hr ERR_MAKEUP_ROLE_REQUIRED
     * 范式 fail-closed；仅 XMeta auth 在测试 enableActionAuth=FALSE 下不可断言，不作唯一守卫）。
     */
    @Override
    @BizMutation
    public ErpCtDocument setLegalHold(@Name("documentId") String documentId,
                                      @Name("legalHold") Boolean legalHold,
                                      IServiceContext context) {
        checkLegalHoldRole();
        ErpCtDocument doc = requireDocument(documentId, context);
        doc.setLegalHold(legalHold);
        updateEntity(doc, null, context);
        return doc;
    }

    // ---------- 归档（retentionDate 到达自动归档经 job 复用本入口） ----------

    /**
     * 手动/自动归档入口。三守卫：已归档幂等返回；legalHold=true 拒绝
     * （{@link ErpCtErrors#ERR_CT_DOCUMENT_LEGAL_HOLD}）；关联合同 ACTIVE 拒绝
     * （{@link ErpCtErrors#ERR_CT_DOCUMENT_CONTRACT_ACTIVE}，owner doc §合规规则第一行）。
     */
    @Override
    @BizMutation
    public ErpCtDocument archive(@Name("documentId") String documentId, IServiceContext context) {
        ErpCtDocument doc = requireDocument(documentId, context);
        if (Boolean.TRUE.equals(doc.getIsArchived())) {
            return doc;
        }
        if (Boolean.TRUE.equals(doc.getLegalHold())) {
            throw new NopException(ErpCtErrors.ERR_CT_DOCUMENT_LEGAL_HOLD)
                    .param(ErpCtErrors.ARG_DOCUMENT_CODE, doc.getCode());
        }
        ErpCtContract contract = doc.getContract();
        if (contract != null && ErpCtConstants.CONTRACT_STATUS_ACTIVE.equals(contract.getStatus())) {
            throw new NopException(ErpCtErrors.ERR_CT_DOCUMENT_CONTRACT_ACTIVE)
                    .param(ErpCtErrors.ARG_CONTRACT_CODE, contract.getCode())
                    .param(ErpCtErrors.ARG_CURRENT_STATUS, contract.getStatus());
        }
        doc.setIsArchived(true);
        doc.setArchiveDate(CoreMetrics.today());
        updateEntity(doc, null, context);
        return doc;
    }

    // ---------- 销毁（D4 裁决：逻辑删除 + 销毁前审计记录；双独立子 agent 批准 ses_fe312b7c5ffe8EW1Va8o42aJZ2 / ses_fe312882dffeVh8nosoYox3lGN） ----------

    /**
     * 文档销毁（{@code purgeDate} 到达；owner doc §生命周期「销毁」终态）。D4 裁决 = 逻辑删除
     * （delVersion 软删——行从全部常规查询消失；物理 DELETE 为显式 successor，见 owner doc 注记）。
     *
     * <p>五守卫（双批准条件落位）：admin 角色（fail-closed，人工确认通道）；legalHold=true 阻断；
     * 仅已归档文档可销毁（生命周期顺序）；关联合同 ACTIVE 阻断（覆盖 SUSPENDED→ACTIVE /
     * rejectAmend 重激活路径）；purgeDate 到达（保留义务——禁止提前销毁，提前销毁无通道）。
     *
     * <p>销毁前审计（耐久载体，不依赖可静默跳过的通知）：行内 remark 追加销毁事件（操作人/日期）
     * → 通知 {@code ct.document-purged}（best-effort，无 ACTIVE 模板静默跳过 R1.4 范式）→
     * 同事务逻辑删除；软删行自身即耐久销毁证据（可经 disableLogicalDelete 复核，不暴露通用恢复入口）。
     */
    @Override
    @BizMutation
    public ErpCtDocument purge(@Name("documentId") String documentId, IServiceContext context) {
        checkPurgeRole();
        ErpCtDocument doc = requireDocument(documentId, context);
        if (Boolean.TRUE.equals(doc.getLegalHold())) {
            throw new NopException(ErpCtErrors.ERR_CT_DOCUMENT_LEGAL_HOLD)
                    .param(ErpCtErrors.ARG_DOCUMENT_CODE, doc.getCode());
        }
        if (!Boolean.TRUE.equals(doc.getIsArchived())) {
            throw new NopException(ErpCtErrors.ERR_CT_DOCUMENT_PURGE_NOT_ARCHIVED)
                    .param(ErpCtErrors.ARG_DOCUMENT_CODE, doc.getCode());
        }
        ErpCtContract contract = doc.getContract();
        if (contract != null && ErpCtConstants.CONTRACT_STATUS_ACTIVE.equals(contract.getStatus())) {
            throw new NopException(ErpCtErrors.ERR_CT_DOCUMENT_CONTRACT_ACTIVE)
                    .param(ErpCtErrors.ARG_CONTRACT_CODE, contract.getCode())
                    .param(ErpCtErrors.ARG_CURRENT_STATUS, contract.getStatus());
        }
        LocalDate today = CoreMetrics.today();
        if (doc.getPurgeDate() == null || doc.getPurgeDate().isAfter(today)) {
            throw new NopException(ErpCtErrors.ERR_CT_DOCUMENT_PURGE_NOT_DUE)
                    .param(ErpCtErrors.ARG_DOCUMENT_CODE, doc.getCode())
                    .param("purgeDate", doc.getPurgeDate());
        }

        String operator = resolveOperator();
        doc.setRemark(appendRemark(doc.getRemark(),
                "已销毁(purge): " + today + " by " + operator + "（D4 逻辑删除，物理删除 successor）"));
        updateEntity(doc, null, context);
        notifyPurged(doc, context);

        // 逻辑删除（useLogicalDelete=true → delVersion 软删；行从常规查询消失）
        dao().deleteEntity(doc);
        LOG.info("erp-ct-doc-purged: documentId={}, code={}, operator={}", doc.getId(), doc.getCode(), operator);
        return doc;
    }

    // ---------- 保留策略批量扫描（job 复用；单条失败隔离） ----------

    /**
     * retentionDate 到达（≤today）且未归档的文档批量归档（config doc-auto-archive 由 job 层门控；
     * 复用 {@link #archive} 三守卫，legalHold/ACTIVE 守卫异常按单条失败隔离跳过）。返回归档条数。
     */
    @Override
    @BizMutation
    public int archiveOverdueDocuments(IServiceContext context) {
        QueryBean query = new QueryBean();
        query.addFilter(eq("isArchived", false));
        // retentionDate <= today 语义：dateBetween(epoch, today)（XMeta 默认过滤集不含 le，
        // 对齐 expireOverdueContracts 注记）
        query.addFilter(dateBetween("retentionDate", LocalDate.of(1970, 1, 1), CoreMetrics.today()));
        @SuppressWarnings("unchecked")
        List<ErpCtDocument> due = (List<ErpCtDocument>) findList(query, null, context);
        int count = 0;
        for (ErpCtDocument doc : due) {
            try {
                archive(doc.getId(), context);
                count++;
            } catch (Exception ex) {
                LOG.warn("erp-ct-doc-retention: 单条文档归档失败（隔离继续）：documentId={}, reason={}",
                        doc.getId(), ex.getMessage());
            }
        }
        return count;
    }

    /**
     * purgeDate 到达（≤today）且已归档的文档批量销毁（config doc-auto-purge 由 job 层门控，默认
     * false 需人工确认；复用 {@link #purge} 五守卫，单条失败隔离）。返回销毁条数。
     */
    @Override
    @BizMutation
    public int purgeOverdueDocuments(IServiceContext context) {
        QueryBean query = new QueryBean();
        query.addFilter(eq("isArchived", true));
        query.addFilter(dateBetween("purgeDate", LocalDate.of(1970, 1, 1), CoreMetrics.today()));
        @SuppressWarnings("unchecked")
        List<ErpCtDocument> due = (List<ErpCtDocument>) findList(query, null, context);
        int count = 0;
        for (ErpCtDocument doc : due) {
            try {
                purge(doc.getId(), context);
                count++;
            } catch (Exception ex) {
                LOG.warn("erp-ct-doc-retention: 单条文档销毁失败（隔离继续）：documentId={}, reason={}",
                        doc.getId(), ex.getMessage());
            }
        }
        return count;
    }

    // ---------- OCR 状态机（PENDING→PROCESSING→COMPLETED/FAILED） ----------

    /**
     * 提交 OCR 识别（人工重新提交通道：FAILED/COMPLETED 均可重跑，PROCESSING 中拒绝并发提交）。
     * 引擎经 config {@code erp-ct.ocr-engine} 派发；成功写 ocrText + COMPLETED，
     * 失败落 FAILED + remark 记录失败原因。
     */
    @Override
    @BizMutation
    public ErpCtDocument startOcr(@Name("documentId") String documentId, IServiceContext context) {
        ErpCtDocument doc = requireDocument(documentId, context);
        checkOcrNotProcessing(doc);
        doc.setOcrStatus(ErpCtDaoConstants.OCR_STATUS_PROCESSING);
        updateEntity(doc, null, context);

        IErpCtOcrEngine engine = ocrEngineRegistry.getEngine(resolveOcrEngineCode());
        OcrRecognizeResponse response = engine.recognize(new OcrRecognizeRequest(
                doc.getId(), doc.getAttachmentFileId(), doc.getDocName(), doc.getMimeType()));
        if (response.isSuccess()) {
            doc.setOcrText(response.getText());
            doc.setOcrStatus(ErpCtDaoConstants.OCR_STATUS_COMPLETED);
        } else {
            doc.setOcrStatus(ErpCtDaoConstants.OCR_STATUS_FAILED);
            doc.setRemark(appendRemark(doc.getRemark(), "OCR 失败: " + response.getErrorMsg()));
        }
        rebuildFullTextSearch(doc);
        updateEntity(doc, null, context);
        return doc;
    }

    /**
     * 人工补录 OCR 文本（Phase 3 D1 裁决：补录等同 COMPLETED 语义——manual 基线引擎的
     * 主通道；补录后 ocrStatus=COMPLETED + fullTextSearch 重建）。
     */
    @Override
    @BizMutation
    public ErpCtDocument submitOcrText(@Name("documentId") String documentId,
                                       @Name("ocrText") String ocrText,
                                       IServiceContext context) {
        ErpCtDocument doc = requireDocument(documentId, context);
        checkOcrNotProcessing(doc);
        doc.setOcrText(ocrText);
        doc.setOcrStatus(ErpCtDaoConstants.OCR_STATUS_COMPLETED);
        rebuildFullTextSearch(doc);
        updateEntity(doc, null, context);
        return doc;
    }

    // ---------- 全文 + 高级搜索（owner doc §高级搜索；L1 UC-CT-10 A「全文搜索或高级过滤器」） ----------

    /**
     * 过滤集 7 类：keyword→fullTextSearch LIKE + code 精确 + docType + contractId +
     * 上传日期范围（createTime）+ OCR 状态 + 归档。文件大小范围/元数据标签键值对按
     * Deferred 登记（plan Deferred But Adjudicated §高级搜索余下两过滤器）。
     */
    @Override
    @BizQuery
    public DocumentSearchResult searchDocuments(@Optional @Name("keyword") String keyword,
                                                @Optional @Name("code") String code,
                                                @Optional @Name("docType") String docType,
                                                @Optional @Name("contractId") String contractId,
                                                @Optional @Name("uploadDateFrom") String uploadDateFrom,
                                                @Optional @Name("uploadDateTo") String uploadDateTo,
                                                @Optional @Name("ocrStatus") String ocrStatus,
                                                @Optional @Name("archived") Boolean archived,
                                                IServiceContext context) {
        QueryBean query = new QueryBean();
        if (notBlank(keyword)) {
            query.addFilter(contains("fullTextSearch", keyword));
        }
        if (notBlank(code)) {
            query.addFilter(eq("code", code));
        }
        if (notBlank(docType)) {
            query.addFilter(eq("docType", docType));
        }
        if (contractId != null) {
            query.addFilter(eq("contractId", contractId));
        }
        if (notBlank(uploadDateFrom)) {
            query.addFilter(ge("createTime", LocalDate.parse(uploadDateFrom).atStartOfDay()));
        }
        if (notBlank(uploadDateTo)) {
            query.addFilter(le("createTime", LocalDate.parse(uploadDateTo).atStartOfDay().plusDays(1)));
        }
        if (notBlank(ocrStatus)) {
            query.addFilter(eq("ocrStatus", ocrStatus));
        }
        if (archived != null) {
            query.addFilter(eq("isArchived", archived));
        }
        query.addOrderField("createTime", true);
        @SuppressWarnings("unchecked")
        List<ErpCtDocument> list = (List<ErpCtDocument>) findList(query, null, context);
        return new DocumentSearchResult(list);
    }

    // ---------- 保存缺省填充 + fullTextSearch 构建（owner doc §索引策略公式） ----------

    /**
     * 上传缺省填充：ocrStatus 缺省 PENDING；retentionDate 缺省 = 上传日 + doc-retention-years、
     * purgeDate 缺省 = retentionDate + doc-archive-years（fill-when-absent，手工录入可覆盖；
     * 「合同终止后起算」自动重算 successor 见 plan Deferred）；fullTextSearch 初始构建。
     * 携带 legalHold 字段要求 admin 角色（与 {@link #setLegalHold} 同一守卫面）。
     */
    @Override
    protected void defaultPrepareSave(EntityData<ErpCtDocument> entityData, IServiceContext context) {
        super.defaultPrepareSave(entityData, context);
        ErpCtDocument doc = entityData.getEntity();
        if (doc.getOcrStatus() == null) {
            doc.setOcrStatus(ErpCtDaoConstants.OCR_STATUS_PENDING);
        }
        if (doc.getRetentionDate() == null) {
            doc.setRetentionDate(CoreMetrics.today().plusYears(resolveIntConfig(
                    ErpCtConfigs.CFG_DOC_RETENTION_YEARS, ErpCtConfigs.DEFAULT_DOC_RETENTION_YEARS)));
        }
        if (doc.getPurgeDate() == null) {
            doc.setPurgeDate(doc.getRetentionDate().plusYears(resolveIntConfig(
                    ErpCtConfigs.CFG_DOC_ARCHIVE_YEARS, ErpCtConfigs.DEFAULT_DOC_ARCHIVE_YEARS)));
        }
        rebuildFullTextSearch(doc);
    }

    /**
     * 归档只读守卫（owner doc §生命周期「归档期只读、可搜索、不可删除」）：归档文档拒绝修改
     * （legalHold 合规字段除外——admin 对已归档文档仍可加/解法律保留以阻止销毁）。
     * 携带 legalHold 的 generic update 要求 admin 角色（防绕过 {@link #setLegalHold} 专用入口）。
     * 触及检索源字段（docName/ocrText/code/metadataTags）时重建 fullTextSearch。
     * 归档状态经 ORM 脏值追踪取"本次更新前"的持久化状态（对齐 ErpCtInvoicePlanBizModel 守卫范式）。
     */
    @Override
    protected void defaultPrepareUpdate(EntityData<ErpCtDocument> entityData, IServiceContext context) {
        super.defaultPrepareUpdate(entityData, context);
        Map<String, Object> data = entityData.getData();
        if (data == null) {
            return;
        }
        if (data.containsKey("legalHold")) {
            checkLegalHoldRole();
        }
        if (touchesNonComplianceFields(data)) {
            ErpCtDocument entity = entityData.getEntity();
            if (isPersistedArchived(entity)) {
                throw new NopException(ErpCtErrors.ERR_CT_DOCUMENT_ARCHIVED_IMMUTABLE)
                        .param(ErpCtErrors.ARG_DOCUMENT_CODE, entity.getCode())
                        .param("archiveDate", entity.getArchiveDate());
            }
            if (touchesFullTextSourceFields(data)) {
                rebuildFullTextSearch(entity);
            }
        }
    }

    /**
     * 删除守卫：已归档文档不可删除（归档期只读）；legalHold=true 文档不可删除
     * （法律保留阻止所有销毁操作）。销毁唯一合法入口 = {@code purge}（Phase 4，D4 裁决）。
     */
    @Override
    protected void defaultPrepareDelete(ErpCtDocument entity, IServiceContext context) {
        super.defaultPrepareDelete(entity, context);
        if (Boolean.TRUE.equals(entity.getLegalHold())) {
            throw new NopException(ErpCtErrors.ERR_CT_DOCUMENT_LEGAL_HOLD)
                    .param(ErpCtErrors.ARG_DOCUMENT_CODE, entity.getCode());
        }
        if (Boolean.TRUE.equals(entity.getIsArchived())) {
            throw new NopException(ErpCtErrors.ERR_CT_DOCUMENT_ARCHIVED_IMMUTABLE)
                    .param(ErpCtErrors.ARG_DOCUMENT_CODE, entity.getCode())
                    .param("archiveDate", entity.getArchiveDate());
        }
    }

    // ---------- fullTextSearch 构建（owner doc §索引策略公式） ----------

    /**
     * {@code fullTextSearch = docName + ocrText + code + metadataTags 关键值} 拼接
     * （空白分段跳过；metadataTags 解析 JSON 取 value 集合，解析失败按原文拼接；
     * 上限 4000 对齐列宽 FULL_TEXT_SEARCH VARCHAR(4000)）。
     */
    void rebuildFullTextSearch(ErpCtDocument doc) {
        StringBuilder sb = new StringBuilder();
        appendPart(sb, doc.getDocName());
        appendPart(sb, doc.getOcrText());
        appendPart(sb, doc.getCode());
        appendPart(sb, metadataTagValues(doc.getMetadataTags()));
        String fullText = sb.length() == 0 ? null : sb.toString();
        if (fullText != null && fullText.length() > FULL_TEXT_MAX_LENGTH) {
            fullText = fullText.substring(0, FULL_TEXT_MAX_LENGTH);
        }
        doc.setFullTextSearch(fullText);
    }

    private void appendPart(StringBuilder sb, String part) {
        if (part == null || part.trim().isEmpty()) {
            return;
        }
        if (sb.length() > 0) {
            sb.append(' ');
        }
        sb.append(part.trim());
    }

    @SuppressWarnings("unchecked")
    String metadataTagValues(String metadataTags) {
        if (metadataTags == null || metadataTags.trim().isEmpty()) {
            return null;
        }
        try {
            Object parsed = JsonTool.parseNonStrict(metadataTags);
            if (parsed instanceof Map) {
                // 关键值拼接（含键名——标签键与值均可命中检索）
                List<String> pairs = new ArrayList<>();
                for (Map.Entry<String, Object> e : ((Map<String, Object>) parsed).entrySet()) {
                    if (e.getValue() != null && !String.valueOf(e.getValue()).trim().isEmpty()) {
                        pairs.add(e.getKey() + "=" + e.getValue());
                    }
                }
                return String.join(" ", pairs);
            }
        } catch (Exception ignored) {
            // 非 JSON 原文拼接
        }
        return metadataTags.trim();
    }

    // ---------- helpers ----------

    private boolean touchesNonComplianceFields(Map<String, Object> data) {
        for (String key : data.keySet()) {
            if (!"legalHold".equals(key)) {
                return true;
            }
        }
        return false;
    }

    private boolean touchesFullTextSourceFields(Map<String, Object> data) {
        return data.containsKey("docName") || data.containsKey("ocrText")
                || data.containsKey("code") || data.containsKey("metadataTags");
    }

    private boolean isPersistedArchived(ErpCtDocument entity) {
        boolean archived = Boolean.TRUE.equals(entity.getIsArchived());
        if (entity.orm_propDirtyByName("isArchived")) {
            Object old = entity.orm_dirtyOldValues().get("isArchived");
            archived = Boolean.TRUE.equals(old);
        }
        return archived;
    }

    private void checkOcrNotProcessing(ErpCtDocument doc) {
        if (ErpCtDaoConstants.OCR_STATUS_PROCESSING.equals(doc.getOcrStatus())) {
            throw new NopException(ErpCtErrors.ERR_CT_DOCUMENT_OCR_ILLEGAL_TRANSITION)
                    .param(ErpCtErrors.ARG_DOCUMENT_CODE, doc.getCode())
                    .param(ErpCtErrors.ARG_CURRENT_STATUS, doc.getOcrStatus());
        }
    }

    private String appendRemark(String remark, String message) {
        return remark == null || remark.trim().isEmpty() ? message : remark + "；" + message;
    }

    private boolean notBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private String resolveOcrEngineCode() {
        return AppConfig.var(ErpCtConfigs.CFG_OCR_ENGINE, ErpCtConfigs.DEFAULT_OCR_ENGINE);
    }

    private int resolveIntConfig(String key, int defaultValue) {
        Integer v = AppConfig.var(key, defaultValue);
        return v == null || v < 0 ? defaultValue : v;
    }

    /**
     * Legal Hold 角色守卫（fail-closed：未登录/缺角色均拒绝，镜像
     * {@code ErpHrAttendanceBizModel#checkMakeUpRole} 范式）。
     */
    void checkLegalHoldRole() {
        checkRole(ErpCtConstants.LEGAL_HOLD_ROLE_ID);
    }

    /**
     * 销毁角色守卫（D4 双批准条件：人工销毁入口 fail-closed 角色守卫，admin 合规动作面）。
     */
    void checkPurgeRole() {
        checkRole(ErpCtConstants.LEGAL_HOLD_ROLE_ID);
    }

    private void checkRole(String roleId) {
        IUserContext userContext = IUserContext.get();
        if (userContext == null || !userContext.isUserInRole(roleId)) {
            throw new NopException(ErpCtErrors.ERR_CT_DOCUMENT_ROLE_REQUIRED)
                    .param(ErpCtErrors.ARG_ROLE_NAME, roleId);
        }
    }

    private String resolveOperator() {
        IUserContext userContext = IUserContext.get();
        return userContext == null || userContext.getUserId() == null ? "system" : userContext.getUserId();
    }

    /**
     * 销毁审计通知（best-effort：通知模板缺失/通知子系统不可用时静默跳过不阻断销毁主流程；
     * 耐久审计载体 = 软删行 + remark，见 {@link #purge}）。
     */
    private void notifyPurged(ErpCtDocument doc, IServiceContext context) {
        try {
            IErpSysNotificationBiz notificationBiz = this.notificationBiz;
            if (notificationBiz != null) {
                Map<String, Object> payload = new java.util.LinkedHashMap<>();
                payload.put("documentId", doc.getId());
                payload.put("code", doc.getCode());
                payload.put("docName", doc.getDocName());
                payload.put("purgeDate", doc.getPurgeDate());
                notificationBiz.notify(ErpCtConstants.NOTIFY_EVENT_DOCUMENT_PURGED, payload, context);
            }
        } catch (Exception ex) {
            LOG.warn("erp-ct-doc-purged: 审计通知派发失败（best-effort 跳过）：documentId={}, reason={}",
                    doc.getId(), ex.getMessage());
        }
    }

    ErpCtDocument requireDocument(String documentId, IServiceContext context) {
        ErpCtDocument doc = get(documentId, false, context);
        if (doc == null) {
            throw new NopException(ErpCtErrors.ERR_CT_DOCUMENT_NOT_FOUND)
                    .param(ErpCtErrors.ARG_DOCUMENT_ID, documentId);
        }
        return doc;
    }
}

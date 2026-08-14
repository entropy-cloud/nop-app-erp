package app.erp.crm.service;

import app.erp.crm.dao.entity.ErpCrmLead;
import app.erp.crm.dao.entity.ErpCrmStage;
import app.erp.md.dao.entity.ErpMdPartner;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.ast.GraphQLOperationType;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * CRM 转化前置守卫 + 直接升格矩阵测试（plan 2026-08-14-1815-1 Phase 3）。
 *
 * <p>经 {@link IGraphQLEngine} 调 {@code ErpCrmLead__convertToOpportunity/convertToCustomer/convertToQuotation}：
 * <ul>
 *   <li>P1-RC-032（直接升格）：QUALIFIED+LEAD 原地升格成功（不建 Partner/新 Lead、docStatus 保持 QUALIFIED）；
 *       NEW 拒绝 ERR_LEAD_NOT_QUALIFIED；已升格后重复调用 ERR_LEAD_TYPE_MISMATCH。</li>
 *   <li>P1-RC-033（convertToCustomer 前置）：NEW 拒绝 ERR_LEAD_NOT_QUALIFIED（无 Partner/新 Lead 创建）。</li>
 *   <li>P1-RC-034（convertToQuotation 前置）：非 QUALIFIED 拒绝 ERR_LEAD_NOT_QUALIFIED；
 *       QUALIFIED 但非 won-stage 拒绝 ERR_LEAD_STAGE_NOT_WON。</li>
 * </ul>
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpCrmConversionGuards extends JunitAutoTestCase {

    static final Long ORG_ID = 1301L;
    static final Long STAGE_NEW = 1101L;
    static final Long STAGE_DEMO = 1102L;
    static final Long STAGE_WON = 1103L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    // ---------- P1-RC-032 直接升格矩阵 ----------

    @Test
    public void testDirectPromoteSuccess() {
        ormTemplate.runInSession(() -> seedLead(2001L, "LEAD-PROMOTE-001", ErpCrmConstants.LEAD_TYPE_LEAD,
                ErpCrmConstants.DOC_STATUS_QUALIFIED, "Promote Corp", "promote@corp.com", null));
        ApiResponse<?> resp = convertToOpportunity(2001L);
        assertEquals(0, resp.getStatus(), "QUALIFIED+LEAD 直接升格应成功");
        ErpCrmLead lead = reloadLead(2001L);
        assertEquals(ErpCrmConstants.LEAD_TYPE_OPPORTUNITY, lead.getLeadType(), "leadType → OPPORTUNITY");
        assertEquals(ErpCrmConstants.DOC_STATUS_QUALIFIED, lead.getDocStatus(), "docStatus 保持 QUALIFIED");
        assertEquals(2001L, lead.getId(), "原 lead id 不变");
        assertNull(lead.getPartnerId(), "不关联 Partner");
        assertEquals(0, countAll(daoProvider.daoFor(ErpMdPartner.class)), "不新建 Partner");
        assertEquals(1, countAll(daoProvider.daoFor(ErpCrmLead.class)), "不新建 Lead（原 lead 原地升格）");
    }

    @Test
    public void testDirectPromoteNewRejected() {
        ormTemplate.runInSession(() -> seedLead(2002L, "LEAD-PROMOTE-NEW-001", ErpCrmConstants.LEAD_TYPE_LEAD,
                ErpCrmConstants.DOC_STATUS_NEW, "New Corp", null, null));
        ApiResponse<?> resp = convertToOpportunity(2002L);
        assertEquals(ErpCrmErrors.ERR_LEAD_NOT_QUALIFIED.getErrorCode(), resp.getCode(),
                "NEW 状态直接升格 → ERR_LEAD_NOT_QUALIFIED");
        ErpCrmLead lead = reloadLead(2002L);
        assertEquals(ErpCrmConstants.LEAD_TYPE_LEAD, lead.getLeadType(), "拒绝后 leadType 不变");
        assertEquals(ErpCrmConstants.DOC_STATUS_NEW, lead.getDocStatus(), "拒绝后 docStatus 不变");
        assertEquals(0, countAll(daoProvider.daoFor(ErpMdPartner.class)), "不新建 Partner");
        assertEquals(1, countAll(daoProvider.daoFor(ErpCrmLead.class)), "不新建 Lead");
    }

    @Test
    public void testDirectPromoteAfterPromotedRejected() {
        ormTemplate.runInSession(() -> seedLead(2003L, "LEAD-PROMOTE-AGAIN-001", ErpCrmConstants.LEAD_TYPE_LEAD,
                ErpCrmConstants.DOC_STATUS_QUALIFIED, "Again Corp", null, null));
        assertEquals(0, convertToOpportunity(2003L).getStatus(), "首次直接升格应成功");
        ApiResponse<?> second = convertToOpportunity(2003L);
        assertEquals(ErpCrmErrors.ERR_LEAD_TYPE_MISMATCH.getErrorCode(), second.getCode(),
                "已升格（leadType=OPPORTUNITY）再调 → ERR_LEAD_TYPE_MISMATCH（leadType 校验先于 docStatus）");
    }

    // ---------- P1-RC-033 convertToCustomer 前置矩阵 ----------

    @Test
    public void testConvertToCustomerNewRejected() {
        ormTemplate.runInSession(() -> seedLead(2004L, "LEAD-CTC-NEW-001", ErpCrmConstants.LEAD_TYPE_LEAD,
                ErpCrmConstants.DOC_STATUS_NEW, "Ctc Corp", "ctc@corp.com", null));
        ApiResponse<?> resp = convertToCustomer(2004L);
        assertEquals(ErpCrmErrors.ERR_LEAD_NOT_QUALIFIED.getErrorCode(), resp.getCode(),
                "NEW 状态 convertToCustomer → ERR_LEAD_NOT_QUALIFIED");
        assertEquals(0, countAll(daoProvider.daoFor(ErpMdPartner.class)), "无 Partner 创建");
        assertEquals(1, countAll(daoProvider.daoFor(ErpCrmLead.class)), "无新 Lead 创建");
    }

    // ---------- P1-RC-034 convertToQuotation 前置矩阵 ----------

    @Test
    public void testConvertToQuotationNonQualifiedRejected() {
        ormTemplate.runInSession(() -> {
            seedStage(STAGE_NEW, "STG-NEW", "新线索", 10, 20);
            seedLead(2005L, "OPP-CTQ-NEW-001", ErpCrmConstants.LEAD_TYPE_OPPORTUNITY,
                    ErpCrmConstants.DOC_STATUS_NEW, "Ctq Corp", null, null);
            seedLead(2006L, "OPP-CTQ-LOST-001", ErpCrmConstants.LEAD_TYPE_OPPORTUNITY,
                    ErpCrmConstants.DOC_STATUS_LOST, "Lost Corp", null, null);
        });
        ApiResponse<?> badNew = convertToQuotation(2005L);
        assertEquals(ErpCrmErrors.ERR_LEAD_NOT_QUALIFIED.getErrorCode(), badNew.getCode(),
                "NEW 状态 OPPORTUNITY → ERR_LEAD_NOT_QUALIFIED");
        ApiResponse<?> badLost = convertToQuotation(2006L);
        assertEquals(ErpCrmErrors.ERR_LEAD_NOT_QUALIFIED.getErrorCode(), badLost.getCode(),
                "LOST 状态 OPPORTUNITY → ERR_LEAD_NOT_QUALIFIED");
    }

    @Test
    public void testConvertToQuotationNotWonStageRejected() {
        ormTemplate.runInSession(() -> {
            seedStage(STAGE_NEW, "STG-NEW", "新线索", 10, 20);
            seedStage(STAGE_DEMO, "STG-DEMO", "方案演示", 20, 40);
            // QUALIFIED OPPORTUNITY 但 stageId=非 won-stage
            ErpCrmLead opp = newLead(2007L, "OPP-CTQ-NOWON-001", ErpCrmConstants.LEAD_TYPE_OPPORTUNITY,
                    ErpCrmConstants.DOC_STATUS_QUALIFIED);
            opp.setStageId(STAGE_DEMO);
            daoProvider.daoFor(ErpCrmLead.class).saveEntity(opp);
        });
        ApiResponse<?> resp = convertToQuotation(2007L);
        assertEquals(ErpCrmErrors.ERR_LEAD_STAGE_NOT_WON.getErrorCode(), resp.getCode(),
                "QUALIFIED 但非 won-stage → ERR_LEAD_STAGE_NOT_WON");
    }

    // ---------- rpc helpers ----------

    private ApiResponse<?> convertToOpportunity(Long leadId) {
        return rpc(mutation, "ErpCrmLead__convertToOpportunity", Map.of("leadId", leadId));
    }

    private ApiResponse<?> convertToCustomer(Long leadId) {
        return rpc(mutation, "ErpCrmLead__convertToCustomer", Map.of("leadId", leadId));
    }

    private ApiResponse<?> convertToQuotation(Long leadId) {
        return rpc(mutation, "ErpCrmLead__convertToQuotation", Map.of("leadId", leadId));
    }

    private ApiResponse<?> rpc(GraphQLOperationType opType, String action, Map<String, Object> data) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, ApiRequest.build(data));
        return graphQLEngine.executeRpc(ctx);
    }

    // ---------- seed / reload helpers ----------

    private void seedLead(Long id, String code, String leadType, String docStatus,
                          String companyName, String email, String phone) {
        ErpCrmLead lead = newLead(id, code, leadType, docStatus);
        lead.setCompanyName(companyName);
        lead.setContactEmail(email);
        lead.setContactPhone(phone);
        lead.setContactName("联系人" + id);
        daoProvider.daoFor(ErpCrmLead.class).saveEntity(lead);
    }

    private ErpCrmLead newLead(Long id, String code, String leadType, String docStatus) {
        ErpCrmLead lead = new ErpCrmLead();
        lead.setId(id);
        lead.setCode(code);
        lead.setOrgId(ORG_ID);
        lead.setLeadType(leadType);
        lead.setDocStatus(docStatus);
        return lead;
    }

    private void seedStage(Long id, String code, String name, int sequence, int defaultProbability) {
        seedStage(id, code, name, sequence, defaultProbability, false);
    }

    private void seedStage(Long id, String code, String name, int sequence, int defaultProbability,
                           boolean isWonStage) {
        IEntityDao<ErpCrmStage> dao = daoProvider.daoFor(ErpCrmStage.class);
        ErpCrmStage stage = new ErpCrmStage();
        stage.setId(id);
        stage.setCode(code);
        stage.setStageName(name);
        stage.setSequence(sequence);
        stage.setDefaultProbability(defaultProbability);
        stage.setIsWonStage(isWonStage);
        dao.saveEntity(stage);
    }

    private ErpCrmLead reloadLead(Long id) {
        return daoProvider.daoFor(ErpCrmLead.class).getEntityById(id);
    }

    private int countAll(IEntityDao<?> dao) {
        QueryBean q = new QueryBean();
        q.setLimit(1000);
        return dao.findAllByQuery(q).size();
    }
}

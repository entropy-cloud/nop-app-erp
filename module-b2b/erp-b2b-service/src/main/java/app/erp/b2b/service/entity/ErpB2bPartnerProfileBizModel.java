
package app.erp.b2b.service.entity;

import app.erp.b2b.biz.IErpB2bCertificationChecklistBiz;
import app.erp.b2b.biz.IErpB2bPartnerProfileBiz;
import app.erp.b2b.biz.IErpB2bTestExchangeBiz;
import app.erp.b2b.dao.entity.ErpB2bCertificationChecklist;
import app.erp.b2b.dao.entity.ErpB2bPartnerProfile;
import app.erp.b2b.dao.entity.ErpB2bTestExchange;
import app.erp.b2b.service.ErpB2bConfigs;
import app.erp.b2b.service.ErpB2bConstants;
import app.erp.b2b.service.ErpB2bErrors;
import app.erp.b2b.service.statemachine.ErpB2bPartnerProfileStateMachine;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.biz.crud.CrudBizModel;
import io.nop.commons.util.StringHelper;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 伙伴档案 BizModel（RC-R1.36，P1-RC-080：UC-B2B-007 伙伴上线状态机推进）。
 *
 * <p>状态迁移守卫经 {@link ErpB2bPartnerProfileStateMachine} Bean（契约
 * {@code entity-state-machine-bean.md}）——Bean 抛 common 层非法迁移码，本类映射为领域
 * {@link ErpB2bErrors#ERR_B2B_PARTNER_ILLEGAL_TRANSITION}（common 码作 cause，镜像
 * {@code ErpB2bEdiDocBizModel:188-226} 映射范式）。
 *
 * <p>推进门槛（promoteToCertified 前置，D1/D2 裁决）：测试通过率 ≥ config
 * {@code erp-b2b.onboarding-test-pass-rate}（默认 0.9，TestExchange 按 partnerProfileId 聚合，
 * 零行=通过率 0 拒绝）+ 关键用例 TC-001/TC-004 必过（testCaseCode 前缀匹配）+ 认证清单必检项全过
 * （isMandatory=true 行全 isPassed=true，零行=拒绝）。ERROR 阻断查询（EdiDoc blockingLevel）本行
 * 不实现——无 partnerProfileId 锚点，登记 Deferred（successor = 伙伴级 EDI 统计需求立项）。
 */
@BizModel("ErpB2bPartnerProfile")
public class ErpB2bPartnerProfileBizModel extends CrudBizModel<ErpB2bPartnerProfile> implements IErpB2bPartnerProfileBiz {

    @Inject
    ErpB2bPartnerProfileStateMachine stateMachine;
    @Inject
    IErpB2bTestExchangeBiz testExchangeBiz;
    @Inject
    IErpB2bCertificationChecklistBiz checklistBiz;

    public ErpB2bPartnerProfileBizModel() {
        setEntityName(ErpB2bPartnerProfile.class.getName());
    }

    @Override
    @BizMutation
    public ErpB2bPartnerProfile promoteToTesting(@Name("profileId") String profileId, IServiceContext context) {
        ErpB2bPartnerProfile profile = requireEntity(profileId, null, context);
        String from = profile.getStatus();
        assertCan("promoteToTesting", profile, from, ErpB2bConstants.PARTNER_STATUS_REGISTERED);
        assertProfileComplete(profile);
        profile.setStatus(stateMachine.promoteToTestingTargetStatus());
        updateEntity(profile, null, context);
        return profile;
    }

    @Override
    @BizMutation
    public ErpB2bPartnerProfile promoteToCertified(@Name("profileId") String profileId, IServiceContext context) {
        ErpB2bPartnerProfile profile = requireEntity(profileId, null, context);
        String from = profile.getStatus();
        assertCan("promoteToCertified", profile, from, ErpB2bConstants.PARTNER_STATUS_TESTING);
        assertPassRateMet(profile, context);
        assertCertificationMet(profile, context);
        profile.setStatus(stateMachine.promoteToCertifiedTargetStatus());
        updateEntity(profile, null, context);
        return profile;
    }

    @Override
    @BizMutation
    public ErpB2bPartnerProfile activate(@Name("profileId") String profileId, IServiceContext context) {
        ErpB2bPartnerProfile profile = requireEntity(profileId, null, context);
        String from = profile.getStatus();
        assertCan("activate", profile, from, ErpB2bConstants.PARTNER_STATUS_CERTIFIED);
        profile.setStatus(stateMachine.activateTargetStatus());
        profile.setGoLiveDate(CoreMetrics.currentDate());
        updateEntity(profile, null, context);
        return profile;
    }

    @Override
    @BizMutation
    public ErpB2bPartnerProfile suspend(@Name("profileId") String profileId, IServiceContext context) {
        ErpB2bPartnerProfile profile = requireEntity(profileId, null, context);
        String from = profile.getStatus();
        assertCan("suspend", profile, from, "REGISTERED/TESTING/CERTIFIED/PRODUCTION");
        profile.setStatus(stateMachine.suspendTargetStatus());
        updateEntity(profile, null, context);
        return profile;
    }

    @Override
    @BizMutation
    public ErpB2bPartnerProfile deactivate(@Name("profileId") String profileId, IServiceContext context) {
        ErpB2bPartnerProfile profile = requireEntity(profileId, null, context);
        String from = profile.getStatus();
        assertCan("deactivate", profile, from, "非终态（REGISTERED/TESTING/CERTIFIED/PRODUCTION/SUSPENDED）");
        profile.setStatus(stateMachine.deactivateTargetStatus());
        profile.setArchivedAt(CoreMetrics.currentTimestamp());
        updateEntity(profile, null, context);
        return profile;
    }

    // ---------- 门槛校验（D1/D2 裁决） ----------

    /**
     * promoteToTesting 前置（D1 选项 B 全配置校验）：partnerId/protocol/authMethod/transportEndpoint/
     * allowedFormats 非空——partner-onboarding.md Stage 1 完成事项四件 + 迁移表前置「基本配置完整」。
     */
    private void assertProfileComplete(ErpB2bPartnerProfile profile) {
        List<String> missing = new ArrayList<>();
        if (profile.getPartnerId() == null) {
            missing.add("partnerId");
        }
        if (StringHelper.isBlank(profile.getProtocol())) {
            missing.add("protocol");
        }
        if (StringHelper.isBlank(profile.getAuthMethod())) {
            missing.add("authMethod");
        }
        if (StringHelper.isBlank(profile.getTransportEndpoint())) {
            missing.add("transportEndpoint");
        }
        if (StringHelper.isBlank(profile.getAllowedFormats())) {
            missing.add("allowedFormats");
        }
        if (!missing.isEmpty()) {
            throw new NopException(ErpB2bErrors.ERR_B2B_PARTNER_PROFILE_INCOMPLETE)
                    .param(ErpB2bErrors.ARG_PARTNER_CODE, profile.getCode())
                    .param(ErpB2bErrors.ARG_MISSING_FIELDS, String.join(",", missing));
        }
    }

    /**
     * promoteToCertified 前置一（D1）：测试通过率 ≥ config（默认 0.9，TestExchange 按 partnerProfileId 聚合，
     * 零行=通过率 0 拒绝）+ 关键用例 TC-001/TC-004 必过（testCaseCode 前缀匹配）。
     */
    private void assertPassRateMet(ErpB2bPartnerProfile profile, IServiceContext context) {
        double threshold = AppConfig.var(ErpB2bConfigs.CONFIG_ONBOARDING_TEST_PASS_RATE,
                ErpB2bConfigs.DEFAULT_ONBOARDING_TEST_PASS_RATE);
        long total = countTestExchanges(profile.getId(), null, context);
        long passed = countTestExchanges(profile.getId(), true, context);
        double rate = total == 0 ? 0d : (double) passed / total;
        List<String> missingKeyCases = new ArrayList<>();
        for (String keyCase : new String[]{"TC-001", "TC-004"}) {
            if (countKeyCasePassed(profile.getId(), keyCase, context) == 0) {
                missingKeyCases.add(keyCase);
            }
        }
        if (rate + 1e-9 < threshold || !missingKeyCases.isEmpty()) {
            throw new NopException(ErpB2bErrors.ERR_B2B_PARTNER_PASS_RATE_NOT_MET)
                    .param(ErpB2bErrors.ARG_PARTNER_CODE, profile.getCode())
                    .param(ErpB2bErrors.ARG_PASS_RATE, Math.round(rate * 10000d) / 10000d)
                    .param(ErpB2bErrors.ARG_THRESHOLD, threshold)
                    .param(ErpB2bErrors.ARG_MISSING_KEY_CASES, String.join(",", missingKeyCases));
        }
    }

    /**
     * promoteToCertified 前置二（D2）：认证清单 isMandatory=true 行全部 isPassed=true；
     * 零 Checklist 行 = 空清单 = 拒绝（无认证流程证据不可证明「所有必检项通过」）。
     */
    private void assertCertificationMet(ErpB2bPartnerProfile profile, IServiceContext context) {
        QueryBean query = new QueryBean();
        query.addFilter(eq("partnerProfileId", profile.getId()));
        query.addFilter(eq("isMandatory", true));
        List<ErpB2bCertificationChecklist> mandatoryItems = checklistBiz.findList(query, null, context);
        List<String> unpassed = new ArrayList<>();
        for (ErpB2bCertificationChecklist item : mandatoryItems) {
            if (item.getIsPassed() == null || !item.getIsPassed()) {
                unpassed.add(item.getChecklistItem());
            }
        }
        if (mandatoryItems.isEmpty()) {
            unpassed.add("空清单（无认证检查记录）");
        }
        if (!unpassed.isEmpty()) {
            throw new NopException(ErpB2bErrors.ERR_B2B_PARTNER_CERTIFICATION_NOT_MET)
                    .param(ErpB2bErrors.ARG_PARTNER_CODE, profile.getCode())
                    .param(ErpB2bErrors.ARG_UNPASSED_ITEMS, String.join(";", unpassed));
        }
    }

    private long countTestExchanges(String profileId, Boolean passed, IServiceContext context) {
        QueryBean query = new QueryBean();
        query.addFilter(eq("partnerProfileId", profileId));
        if (passed != null) {
            query.addFilter(eq("passed", passed));
        }
        return testExchangeBiz.findCount(query, context);
    }

    private long countKeyCasePassed(String profileId, String keyCase, IServiceContext context) {
        QueryBean query = new QueryBean();
        query.addFilter(eq("partnerProfileId", profileId));
        query.addFilter(eq("passed", true));
        List<ErpB2bTestExchange> passedExchanges = testExchangeBiz.findList(query, null, context);
        long count = 0;
        for (ErpB2bTestExchange exchange : passedExchanges) {
            if (exchange.getTestCaseCode() != null && exchange.getTestCaseCode().startsWith(keyCase)) {
                count++;
            }
        }
        return count;
    }

    // ---------- 守卫映射（契约 §7：common 码 → 领域码） ----------

    /**
     * 经 StateMachine Bean 断言来源态合法；非法边（Bean 报告 common 层码）映射为领域
     * {@code ERR_B2B_PARTNER_ILLEGAL_TRANSITION} + 伙伴编码/上下文，common 码作 cause 保留（契约 §7）。
     */
    private void assertCan(String action, ErpB2bPartnerProfile profile, String from, String expected) {
        try {
            switch (action) {
                case "promoteToTesting":
                    stateMachine.assertCanPromoteToTesting(from);
                    break;
                case "promoteToCertified":
                    stateMachine.assertCanPromoteToCertified(from);
                    break;
                case "activate":
                    stateMachine.assertCanActivate(from);
                    break;
                case "suspend":
                    stateMachine.assertCanSuspend(from);
                    break;
                case "deactivate":
                    stateMachine.assertCanDeactivate(from);
                    break;
                default:
                    throw new IllegalArgumentException("unexpected action: " + action);
            }
        } catch (NopException e) {
            throw illegalTransition(profile, from, expected, e);
        }
    }

    private NopException illegalTransition(ErpB2bPartnerProfile profile, String current, String expected, Throwable cause) {
        return new NopException(ErpB2bErrors.ERR_B2B_PARTNER_ILLEGAL_TRANSITION, cause)
                .param(ErpB2bErrors.ARG_PARTNER_CODE, profile.getCode())
                .param(ErpB2bErrors.ARG_CURRENT_STATE, current)
                .param(ErpB2bErrors.ARG_EXPECTED_STATE, expected);
    }

}

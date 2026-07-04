package app.erp.cs.service.entity;

import app.erp.cs.service.ErpCsConstants;

/**
 * NPS 分类器。权威：{@code docs/design/customer-service/csat.md §1.2}。
 *
 * <p>9-10 推荐者（PROMOTER）/ 7-8 被动者（PASSIVE）/ 0-6 贬损者（DETRACTOR）。
 * 超出区间返回 null（调用方应在 submitSurvey 前校验区间）。
 */
public final class NpsClassifier {

    private NpsClassifier() {
    }

    public static String classify(int npsScore) {
        if (npsScore >= 9 && npsScore <= 10) {
            return ErpCsConstants.NPS_CATEGORY_PROMOTER;
        }
        if (npsScore >= 7 && npsScore <= 8) {
            return ErpCsConstants.NPS_CATEGORY_PASSIVE;
        }
        if (npsScore >= 0 && npsScore <= 6) {
            return ErpCsConstants.NPS_CATEGORY_DETRACTOR;
        }
        return null;
    }
}

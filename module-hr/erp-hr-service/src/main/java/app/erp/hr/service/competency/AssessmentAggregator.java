package app.erp.hr.service.competency;

import app.erp.hr.dao.entity.ErpHrAssessmentDetail;
import app.erp.hr.service.ErpHrConfigs;
import app.erp.hr.service.ErpHrConstants;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.exceptions.ErrorCode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.function.Function;

/**
 * 360 评估聚合引擎（competency-management.md §评估流程 §360 评估流程）。纯函数式 + 注入加载函数，
 * 便于单测。对一员工一胜任力，按 assessmentType 加权平均 actualLevel：
 * <pre>
 *   aggregated = selfW × selfLevel + mgrW × mgrLevel + peerW × avgPeer + subW × avgSub
 * </pre>
 *
 * <p><b>缺类型重归一化 Decision（实现注记）</b>：当某 assessmentType 在该胜任力下无任何记录时，
 * 该类型权重置零、其余类型权重重归一化（而非抛错）。仅当全部类型都无记录时抛 ERR_ASSESSMENT_NO_DETAILS。
 * 这与现实对齐：360 评估中并非所有员工都有下级/同级评估人。原 design 文档未明确该边界，本类在此固化。
 *
 * <p>权重经 {@link ErpHrConfigs} 配置化（默认 15%/50%/25%/10%）；非 360 类型评估（仅 SELF/MANAGER 单源）
 * 直接取该源 actualLevel 作为聚合值，跳过加权。
 */
public class AssessmentAggregator {

    /**
     * 按胜任力聚合员工的多源评估打分，返回四舍五入到整数的 actualLevel。
     *
     * @param competencyId             被聚合的胜任力
     * @param assessmentType           评估类型（SELF/MANAGER/PEER/SUBORDINATE/360）
     * @param detailsByCompetency      该评估所有明细（已按 competencyId 过滤）—— 注入式加载函数的等价产物
     * @return 聚合后 actualLevel（1-5 量表，四舍五入到整数）
     */
    public int aggregate(Long competencyId,
                         String assessmentType,
                         List<ErpHrAssessmentDetail> detailsByCompetency) {
        if (detailsByCompetency == null || detailsByCompetency.isEmpty()) {
            throw new NopException(ERR_AGGREGATE_NO_DETAILS)
                    .param("competencyId", competencyId);
        }

        // 非 360 类型：单源直接取均值（无权重混合）
        if (!ErpHrConstants.ASSESSMENT_TYPE_360.equals(assessmentType)) {
            return averageLevel(detailsByCompetency);
        }

        // 360：按 sourceType 分组加权
        BigDecimal selfSum = BigDecimal.ZERO;
        BigDecimal mgrSum = BigDecimal.ZERO;
        BigDecimal peerSum = BigDecimal.ZERO;
        BigDecimal subSum = BigDecimal.ZERO;
        int selfCnt = 0, mgrCnt = 0, peerCnt = 0, subCnt = 0;
        for (ErpHrAssessmentDetail d : detailsByCompetency) {
            int lvl = nz(d.getActualLevel());
            String src = d.getSourceType() != null ? d.getSourceType() : assessmentType;
            switch (src) {
                case ErpHrConstants.ASSESSMENT_TYPE_SELF:
                    selfSum = selfSum.add(BigDecimal.valueOf(lvl)); selfCnt++; break;
                case ErpHrConstants.ASSESSMENT_TYPE_MANAGER:
                    mgrSum = mgrSum.add(BigDecimal.valueOf(lvl)); mgrCnt++; break;
                case ErpHrConstants.ASSESSMENT_TYPE_PEER:
                    peerSum = peerSum.add(BigDecimal.valueOf(lvl)); peerCnt++; break;
                case ErpHrConstants.ASSESSMENT_TYPE_SUBORDINATE:
                    subSum = subSum.add(BigDecimal.valueOf(lvl)); subCnt++; break;
                default:
                    // 未知 sourceType 归到 assessmentType（如 360 自身作为 fallback）
                    break;
            }
        }

        BigDecimal selfW = ErpHrConfigs.assessmentSelfWeight();
        BigDecimal mgrW = ErpHrConfigs.assessmentManagerWeight();
        BigDecimal peerW = ErpHrConfigs.assessmentPeerWeight();
        BigDecimal subW = ErpHrConfigs.assessmentSubordinateWeight();

        // 缺类型重归一化：置零并按其余权重归一
        if (selfCnt == 0) selfW = BigDecimal.ZERO;
        if (mgrCnt == 0) mgrW = BigDecimal.ZERO;
        if (peerCnt == 0) peerW = BigDecimal.ZERO;
        if (subCnt == 0) subW = BigDecimal.ZERO;

        BigDecimal weightSum = selfW.add(mgrW).add(peerW).add(subW);
        if (weightSum.signum() <= 0) {
            throw new NopException(ERR_AGGREGATE_NO_DETAILS)
                    .param("competencyId", competencyId);
        }

        BigDecimal selfAvg = selfCnt == 0 ? BigDecimal.ZERO : selfSum.divide(BigDecimal.valueOf(selfCnt), 6, RoundingMode.HALF_UP);
        BigDecimal mgrAvg = mgrCnt == 0 ? BigDecimal.ZERO : mgrSum.divide(BigDecimal.valueOf(mgrCnt), 6, RoundingMode.HALF_UP);
        BigDecimal peerAvg = peerCnt == 0 ? BigDecimal.ZERO : peerSum.divide(BigDecimal.valueOf(peerCnt), 6, RoundingMode.HALF_UP);
        BigDecimal subAvg = subCnt == 0 ? BigDecimal.ZERO : subSum.divide(BigDecimal.valueOf(subCnt), 6, RoundingMode.HALF_UP);

        BigDecimal weighted = selfW.multiply(selfAvg)
                .add(mgrW.multiply(mgrAvg))
                .add(peerW.multiply(peerAvg))
                .add(subW.multiply(subAvg));
        BigDecimal normalized = weighted.divide(weightSum, 6, RoundingMode.HALF_UP);
        int rounded = normalized.setScale(0, RoundingMode.HALF_UP).intValue();
        return clampLevel(rounded);
    }

    /**
     * 提供给 BizModel 的便捷入口：给定加载函数，聚合指定 competencyId 的所有 detail。
     * 加载函数注入便于单测替换为内存 list。
     */
    public int aggregateWithLoader(Long competencyId,
                                   String assessmentType,
                                   Function<Long, List<ErpHrAssessmentDetail>> detailLoader) {
        return aggregate(competencyId, assessmentType, detailLoader.apply(competencyId));
    }

    static int averageLevel(List<ErpHrAssessmentDetail> details) {
        BigDecimal sum = BigDecimal.ZERO;
        int cnt = 0;
        for (ErpHrAssessmentDetail d : details) {
            sum = sum.add(BigDecimal.valueOf(nz(d.getActualLevel())));
            cnt++;
        }
        if (cnt == 0) {
            throw new NopException(ERR_AGGREGATE_NO_DETAILS);
        }
        BigDecimal avg = sum.divide(BigDecimal.valueOf(cnt), 6, RoundingMode.HALF_UP);
        return clampLevel(avg.setScale(0, RoundingMode.HALF_UP).intValue());
    }

    static int nz(Integer v) {
        return v != null ? v : 0;
    }

    static int clampLevel(int v) {
        if (v < ErpHrConstants.COMPETENCY_LEVEL_MIN) return ErpHrConstants.COMPETENCY_LEVEL_MIN;
        if (v > ErpHrConstants.COMPETENCY_LEVEL_MAX) return ErpHrConstants.COMPETENCY_LEVEL_MAX;
        return v;
    }

    /**
     * 本类私有错误码——聚合层无可评估数据时抛出。BizModel 调用方应在外层将"提交时无明细"映射为
     * {@link app.erp.hr.service.ErpHrErrors#ERR_ASSESSMENT_NO_DETAILS}（评估提交语义）。
     * 这里独立定义避免循环依赖 service 接口。
     */
    static final ErrorCode ERR_AGGREGATE_NO_DETAILS = ErrorCode.define(
            "erp.err.hr.assessment-aggregate-no-details",
            "胜任力 {competencyId} 无可聚合的评估明细",
            "competencyId");
}

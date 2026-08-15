package app.erp.ast.service.statemachine;

import app.erp.ast.service.ErpAstConstants;
import app.erp.common.service.ErpCommonErrors;
import io.nop.api.core.exceptions.NopException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 资产卡片（{@code ErpAstAsset}）实体级状态机 Bean —— 一 Bean 对应一实体一轴（{@code status} 生命周期轴）。
 *
 * <p>权威契约：{@code docs/architecture/entity-state-machine-bean.md}；
 * 业务语义：{@code docs/design/assets/state-machine.md §适用对象一：资产卡片}。
 *
 * <p>严格无状态（契约 §2）：不注入 DAO/IBiz/IServiceContext/事务，只接收状态值。承载 Asset 卡片生命周期
 * 迁移矩阵 + 终态/初始态分类 + 只读 {@link #transitions()} 元数据。可经 Delta 同名 Bean 覆盖（契约 §6）
 * 替换基线矩阵。
 *
 * <p><strong>writer 轴</strong>：Asset.status 写路径 = 跨实体文档 Processor 的 side-effect（capitalization 资本化 /
 * disposal 处置 / inventory 盘亏触发处置）+ RC-R1.54 起 Asset 自有 suspend/resume mutation（IN_SERVICE↔IDLE）。
 * 本 Bean 集中治理这些写入的固定来源/目标态判断；各文档自身的 approveStatus/docStatus 轴不在本 Bean（归计划 2）。
 *
 * <p>迁移矩阵（8 命名动作）：capitalize(DRAFT→IN_SERVICE)、reverseCapitalize(IN_SERVICE→DRAFT，资本化 posted
 * 窗口)、suspend(IN_SERVICE→IDLE，RC-R1.54)、resume(IDLE→IN_SERVICE，RC-R1.54)、disposeScrap(IN_SERVICE/IDLE→SCRAPPED，
 * RC-R1.54 扩展 IDLE 来源对齐 owner doc §2「IN_SERVICE/IDLE → SCRAPPED/SOLD」契约)、disposeSell(IN_SERVICE/IDLE→SOLD)、
 * reverseDisposal(SCRAPPED/SOLD→IN_SERVICE，处置 posted 窗口)、inventoryShortageDisposal(IN_SERVICE/IDLE→SCRAPPED，
 * 盘点盘亏触发处置)。
 *
 * <p><strong>IDLE 已实装（RC-R1.54）</strong>：dict {@code erp-ast/asset-status} 值 IDLE 经 suspend 可达、
 * 经 resume/dispose 出边——不再为死状态。折旧引擎 IN_SERVICE-only 查询 + {@code validateAssetInService} 守卫
 * 自然满足「IDLE 期间不计提」语义（owner doc §1/UC-AST-03 ③）。
 *
 * <p>分类：initial={DRAFT}，terminal={SCRAPPED, SOLD}。非法边抛 common 层
 * {@link ErpCommonErrors#ERR_ILLEGAL_STATUS_TRANSITION}（参数 {@code currentStatus}/{@code expectedStatus} +
 * {@code action} 补充诊断参数）；领域 ErrorCode 映射归调用方 Processor（契约 §7）。
 */
public class ErpAstAssetStateMachine {

    /** 补充诊断参数键：被拒绝的动作名（供 M5.2 守卫/诊断消费）。 */
    public static final String ARG_ACTION = "action";

    // ---------- 显式动作方法（主路径） ----------

    /**
     * capitalize 守卫：来源态为 {@code DRAFT}/{@code null} 合法（资本化入账——新卡建卡后置 IN_SERVICE）。
     */
    public void assertCanCapitalize(String status) {
        String s = normalize(status);
        if (!ErpAstConstants.ASSET_STATUS_DRAFT.equals(s)) {
            throw illegal("capitalize", s, ErpAstConstants.ASSET_STATUS_DRAFT);
        }
    }

    /**
     * reverseCapitalize 守卫：来源态为 {@code IN_SERVICE} 合法（reverseApprove posted 窗口逆资本化）。
     */
    public void assertCanReverseCapitalize(String status) {
        String s = normalize(status);
        if (!ErpAstConstants.ASSET_STATUS_IN_SERVICE.equals(s)) {
            throw illegal("reverseCapitalize", s, ErpAstConstants.ASSET_STATUS_IN_SERVICE);
        }
    }

    /**
     * suspend 守卫（RC-R1.54）：来源态为 {@code IN_SERVICE} 合法（暂停使用→闲置，闲置期停提折旧）。
     */
    public void assertCanSuspend(String status) {
        String s = normalize(status);
        if (!ErpAstConstants.ASSET_STATUS_IN_SERVICE.equals(s)) {
            throw illegal("suspend", s, ErpAstConstants.ASSET_STATUS_IN_SERVICE);
        }
    }

    /**
     * resume 守卫（RC-R1.54）：来源态为 {@code IDLE} 合法（闲置恢复→使用中，恢复计提折旧）。
     */
    public void assertCanResume(String status) {
        String s = normalize(status);
        if (!ErpAstConstants.ASSET_STATUS_IDLE.equals(s)) {
            throw illegal("resume", s, ErpAstConstants.ASSET_STATUS_IDLE);
        }
    }

    /**
     * dispose 守卫（disposeScrap/disposeSell 共用来源态判定）：来源态为 {@code IN_SERVICE}/{@code IDLE} 合法
     * （RC-R1.54 扩展 IDLE 来源，对齐 owner doc §2「IN_SERVICE/IDLE → SCRAPPED/SOLD」契约）。
     */
    public void assertCanDispose(String status) {
        String s = normalize(status);
        if (!ErpAstConstants.ASSET_STATUS_IN_SERVICE.equals(s)
                && !ErpAstConstants.ASSET_STATUS_IDLE.equals(s)) {
            throw illegal("dispose", s,
                    ErpAstConstants.ASSET_STATUS_IN_SERVICE + " 或 " + ErpAstConstants.ASSET_STATUS_IDLE);
        }
    }

    /**
     * reverseDisposal 守卫：来源态为 {@code SCRAPPED}/{@code SOLD} 合法（reverseApprove posted 窗口恢复在用）。
     */
    public void assertCanReverseDispose(String status) {
        String s = normalize(status);
        if (!ErpAstConstants.ASSET_STATUS_SCRAPPED.equals(s)
                && !ErpAstConstants.ASSET_STATUS_SOLD.equals(s)) {
            throw illegal("reverseDisposal", s,
                    ErpAstConstants.ASSET_STATUS_SCRAPPED + " 或 " + ErpAstConstants.ASSET_STATUS_SOLD);
        }
    }

    /**
     * inventoryShortageDisposal 守卫：来源态为 {@code IN_SERVICE}/{@code IDLE} 合法（盘点盘亏触发处置，
     * 对齐盘点范围过滤 liveStatuses={IN_SERVICE,IDLE} 既有行为；RC-R1.54 后 IDLE 经 suspend 可达）。
     */
    public void assertCanShortageDispose(String status) {
        String s = normalize(status);
        if (!ErpAstConstants.ASSET_STATUS_IN_SERVICE.equals(s)
                && !ErpAstConstants.ASSET_STATUS_IDLE.equals(s)) {
            throw illegal("inventoryShortageDisposal", s,
                    ErpAstConstants.ASSET_STATUS_IN_SERVICE + " 或 " + ErpAstConstants.ASSET_STATUS_IDLE);
        }
    }

    // ---------- 动作目标态（供 Processor 写回） ----------

    public String capitalizeTargetStatus() {
        return ErpAstConstants.ASSET_STATUS_IN_SERVICE;
    }

    /** reverseCapitalize 目标态=DRAFT（资本化 reverseApprove posted 窗口回滚至草稿）。 */
    public String reverseCapitalizeTargetStatus() {
        return ErpAstConstants.ASSET_STATUS_DRAFT;
    }

    /** suspend 目标态=IDLE（RC-R1.54：暂停使用，闲置期停提折旧）。 */
    public String suspendTargetStatus() {
        return ErpAstConstants.ASSET_STATUS_IDLE;
    }

    /** resume 目标态=IN_SERVICE（RC-R1.54：闲置恢复使用，恢复计提折旧）。 */
    public String resumeTargetStatus() {
        return ErpAstConstants.ASSET_STATUS_IN_SERVICE;
    }

    public String disposeScrapTargetStatus() {
        return ErpAstConstants.ASSET_STATUS_SCRAPPED;
    }

    public String disposeSellTargetStatus() {
        return ErpAstConstants.ASSET_STATUS_SOLD;
    }

    /** reverseDisposal 目标态=IN_SERVICE（处置 reverseApprove posted 窗口恢复在用）。 */
    public String reverseDisposalTargetStatus() {
        return ErpAstConstants.ASSET_STATUS_IN_SERVICE;
    }

    /** inventoryShortageDisposal 目标态=SCRAPPED（盘亏触发处置置报废）。 */
    public String shortageDisposeTargetStatus() {
        return ErpAstConstants.ASSET_STATUS_SCRAPPED;
    }

    // ---------- 终态/初始态分类 ----------

    /**
     * 业务终态判定。卡片终态为 {@code SCRAPPED}/{@code SOLD}（对外处置完成，无出边）。
     */
    public boolean isTerminal(String status) {
        return ErpAstConstants.ASSET_STATUS_SCRAPPED.equals(status)
                || ErpAstConstants.ASSET_STATUS_SOLD.equals(status);
    }

    // ---------- 只读元数据接口（完备性/可达性分析用，非 Processor 主调用路径） ----------

    /**
     * 迁移元数据：11 条边（8 命名动作——disposeScrap/disposeSell/reverseDisposal 双源，RC-R1.54 增
     * suspend/resume + dispose 的 IDLE 来源，IDLE 不再为死状态）。
     */
    public List<TransitionDefinition> transitions() {
        return Collections.unmodifiableList(Arrays.asList(
                new TransitionDefinition("capitalize", ErpAstConstants.ASSET_STATUS_DRAFT, ErpAstConstants.ASSET_STATUS_IN_SERVICE),
                new TransitionDefinition("reverseCapitalize", ErpAstConstants.ASSET_STATUS_IN_SERVICE, ErpAstConstants.ASSET_STATUS_DRAFT),
                new TransitionDefinition("suspend", ErpAstConstants.ASSET_STATUS_IN_SERVICE, ErpAstConstants.ASSET_STATUS_IDLE),
                new TransitionDefinition("resume", ErpAstConstants.ASSET_STATUS_IDLE, ErpAstConstants.ASSET_STATUS_IN_SERVICE),
                new TransitionDefinition("disposeScrap", ErpAstConstants.ASSET_STATUS_IN_SERVICE, ErpAstConstants.ASSET_STATUS_SCRAPPED),
                new TransitionDefinition("disposeScrap", ErpAstConstants.ASSET_STATUS_IDLE, ErpAstConstants.ASSET_STATUS_SCRAPPED),
                new TransitionDefinition("disposeSell", ErpAstConstants.ASSET_STATUS_IN_SERVICE, ErpAstConstants.ASSET_STATUS_SOLD),
                new TransitionDefinition("disposeSell", ErpAstConstants.ASSET_STATUS_IDLE, ErpAstConstants.ASSET_STATUS_SOLD),
                new TransitionDefinition("reverseDisposal", ErpAstConstants.ASSET_STATUS_SCRAPPED, ErpAstConstants.ASSET_STATUS_IN_SERVICE),
                new TransitionDefinition("reverseDisposal", ErpAstConstants.ASSET_STATUS_SOLD, ErpAstConstants.ASSET_STATUS_IN_SERVICE),
                new TransitionDefinition("inventoryShortageDisposal", ErpAstConstants.ASSET_STATUS_IN_SERVICE, ErpAstConstants.ASSET_STATUS_SCRAPPED)));
    }

    public List<String> terminalStatuses() {
        return Collections.unmodifiableList(Arrays.asList(
                ErpAstConstants.ASSET_STATUS_SCRAPPED,
                ErpAstConstants.ASSET_STATUS_SOLD));
    }

    public List<String> initialStatuses() {
        return Collections.unmodifiableList(Arrays.asList(ErpAstConstants.ASSET_STATUS_DRAFT));
    }

    // ---------- 内部 ----------

    /** null 归一化为 DRAFT（初始态语义：未设置=草稿），与 CRUD 创建写 DRAFT 语义一致。 */
    private static String normalize(String status) {
        return status == null ? ErpAstConstants.ASSET_STATUS_DRAFT : status;
    }

    private static NopException illegal(String action, String currentStatus, String expectedStatus) {
        return new NopException(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION)
                .param(ErpCommonErrors.ARG_CURRENT_STATUS, currentStatus)
                .param(ErpCommonErrors.ARG_EXPECTED_STATUS, expectedStatus)
                .param(ARG_ACTION, action);
    }

    /** 只读迁移定义记录（供 M5.1/M5.2 可达性/完备性分析与文档一致性校验消费）。 */
    public static final class TransitionDefinition {
        private final String action;
        private final String fromStatus;
        private final String toStatus;

        TransitionDefinition(String action, String fromStatus, String toStatus) {
            this.action = action;
            this.fromStatus = fromStatus;
            this.toStatus = toStatus;
        }

        public String getAction() {
            return action;
        }

        public String getFromStatus() {
            return fromStatus;
        }

        public String getToStatus() {
            return toStatus;
        }
    }
}

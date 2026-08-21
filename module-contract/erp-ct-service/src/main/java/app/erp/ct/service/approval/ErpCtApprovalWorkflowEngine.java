package app.erp.ct.service.approval;

import app.erp.contract.dao.entity.ErpCtApprovalMatrix;
import app.erp.contract.dao.entity.ErpCtApprovalRecord;
import app.erp.contract.dao.entity.ErpCtContract;
import app.erp.ct.biz.IErpCtApprovalMatrixBiz;
import app.erp.ct.biz.IErpCtApprovalRecordBiz;
import app.erp.ct.service.ErpCtConfigs;
import app.erp.ct.service.ErpCtConstants;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.convert.ConvertHelper;
import io.nop.auth.biz.INopAuthRoleBiz;
import io.nop.auth.biz.INopAuthUserRoleBiz;
import io.nop.auth.dao.entity.NopAuthRole;
import io.nop.auth.dao.entity.NopAuthUserRole;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.in;

/**
 * 合同审批工作流引擎（RC-R1.34，P1-RC-077，UC-CT-07）。
 *
 * <p>纯编排助手 Bean（无状态）：金额匹配矩阵节点 + 生成审批记录 + 链状态推导。
 * mutation 入口归 {@code ErpCtApprovalRecordBizModel}（approve/reject/resubmit）与
 * {@code ErpCtContractBizModel}（terminate 法务门控，D1 选项 B——独立 approveTermination/
 * rejectTermination，记录复用本引擎的生成/解析语义）。
 *
 * <p>D1/D2/D3/D7 裁决（plan 2026-08-15-1023-1 Phase 1）：
 * <ul>
 *   <li>D1：terminate 法务门控 = 独立 mutation + ApprovalRecord 复用（approvalMatrixId=null 判别），
 *       引擎链仅面向 submit 提交链（approvalMatrixId != null）；</li>
 *   <li>D2：approverRole 视为 nop-auth roleName，经 INopAuthRoleBiz/INopAuthUserRoleBiz 双过滤解析
 *       （对齐 A1.51 resolveRole 范式），无命中留空由操作员手工指定；</li>
 *   <li>D3：驳回超限 = 派生计数（contractId+approvalOrder 的 REJECTED 记录数 vs
 *       {@code erp-ct.approval-max-retries}，零 ORM）；</li>
 *   <li>D7：resubmit = 每轮追加新 ApprovalRecord 行（禁止原地翻转），派生计数递增使锁定可达。</li>
 * </ul>
 */
public class ErpCtApprovalWorkflowEngine {

    @Inject
    IErpCtApprovalMatrixBiz matrixBiz;

    @Inject
    IErpCtApprovalRecordBiz recordBiz;

    @Inject
    INopAuthRoleBiz authRoleBiz;

    @Inject
    INopAuthUserRoleBiz authUserRoleBiz;

    // ---------- D2 角色→用户解析 ----------

    /**
     * 角色名 → userId 解析（D2 选项 A）：roleName 匹配 NopAuthRole → roleId 集合 →
     * NopAuthUserRole → userId 集合，取确定序首个（userId 字符串排序最小）；无命中返回 null
     * （approverId 留空 = 操作员手工指定语义）。
     */
    public String resolveApproverId(String roleName, IServiceContext ctx) {
        if (roleName == null || roleName.isBlank()) {
            return null;
        }
        QueryBean roleQ = new QueryBean();
        roleQ.addFilter(eq("roleName", roleName));
        roleQ.setLimit(100);
        List<NopAuthRole> roles = authRoleBiz.findList(roleQ, null, ctx);
        if (roles == null || roles.isEmpty()) {
            return null;
        }
        Set<String> roleIds = new LinkedHashSet<>();
        for (NopAuthRole role : roles) {
            if (role.getRoleId() != null) {
                roleIds.add(role.getRoleId());
            }
        }
        if (roleIds.isEmpty()) {
            return null;
        }
        QueryBean urQ = new QueryBean();
        urQ.addFilter(in("roleId", roleIds));
        urQ.setLimit(500);
        List<NopAuthUserRole> urs = authUserRoleBiz.findList(urQ, null, ctx);
        if (urs == null || urs.isEmpty()) {
            return null;
        }
        return urs.stream()
                .map(NopAuthUserRole::getUserId)
                .filter(u -> u != null && !u.isBlank())
                .min(Comparator.naturalOrder())
                .orElse(null);
    }

    // ---------- 金额匹配 + 记录生成（submit 后置接线） ----------

    /**
     * 按 totalAmount 匹配适用审批节点（UC-CT-07 step 2）：isActive=true + contractType
     * 匹配（null 通配）+ orgId 匹配（null 通配）+ minAmount≤totalAmount≤maxAmount
     * （null 无界），approvalOrder 升序。
     */
    public List<ErpCtApprovalMatrix> matchByAmount(ErpCtContract contract, IServiceContext ctx) {
        QueryBean query = new QueryBean();
        query.addFilter(eq("isActive", true));
        List<ErpCtApprovalMatrix> nodes = matrixBiz.findList(query, null, ctx);
        if (nodes == null || nodes.isEmpty()) {
            return new ArrayList<>();
        }
        List<ErpCtApprovalMatrix> matched = new ArrayList<>();
        BigDecimal total = contract.getTotalAmount();
        for (ErpCtApprovalMatrix node : nodes) {
            if (!matchContractType(node.getContractType(), contract.getContractType())) {
                continue;
            }
            if (!matchOrg(node.getOrgId(), contract.getOrgId())) {
                continue;
            }
            if (!matchAmount(node, total)) {
                continue;
            }
            matched.add(node);
        }
        matched.sort(Comparator.comparing(n -> n.getApprovalOrder() == null ? 0 : n.getApprovalOrder()));
        return matched;
    }

    /**
     * 生成审批记录（UC-CT-07 step 3-5）：每节点一条——首节点 PENDING 其余 WAITING，
     * approverId 按 D2 解析；合同 orgId 落记录。
     */
    public List<ErpCtApprovalRecord> generateRecords(ErpCtContract contract,
                                                     List<ErpCtApprovalMatrix> nodes,
                                                     IServiceContext ctx) {
        List<ErpCtApprovalRecord> records = new ArrayList<>();
        for (int i = 0; i < nodes.size(); i++) {
            ErpCtApprovalMatrix node = nodes.get(i);
            ErpCtApprovalRecord record = recordBiz.newEntity();
            record.setContractId(contract.getId());
            record.setOrgId(contract.getOrgId());
            record.setApprovalMatrixId(node.getId());
            record.setApprovalOrder(node.getApprovalOrder());
            record.setApproverId(resolveApproverId(node.getApproverRole(), ctx));
            record.setApprovalStatus(i == 0
                    ? ErpCtConstants.APPROVAL_STATUS_PENDING
                    : ErpCtConstants.APPROVAL_STATUS_WAITING);
            recordBiz.saveEntity(record, null, ctx);
            records.add(record);
        }
        return records;
    }

    // ---------- 链状态推导（approve/reject/resubmit/activate 联动消费） ----------

    /** 合同全部审批记录（含历史轮次；terminate 记录 approvalMatrixId=null 亦含）。 */
    public List<ErpCtApprovalRecord> findRecords(String contractId, IServiceContext ctx) {
        QueryBean query = new QueryBean();
        query.addFilter(eq("contractId", contractId));
        List<ErpCtApprovalRecord> list = recordBiz.findList(query, null, ctx);
        return list == null ? new ArrayList<>() : list;
    }

    /** 某 (contractId, approvalOrder) 的最新记录（跨轮次取 id 最大者）；无记录返回 null。 */
    public ErpCtApprovalRecord latestRecord(String contractId, Integer approvalOrder, IServiceContext ctx) {
        ErpCtApprovalRecord latest = null;
        for (ErpCtApprovalRecord record : findRecords(contractId, ctx)) {
            if (approvalOrder.equals(record.getApprovalOrder())
                    && (latest == null || idOrder(record, latest) > 0)) {
                latest = record;
            }
        }
        return latest;
    }

    // seq-string id 数值序比较（id 为 String 后保留跨轮次取 id 最大者语义）
    protected int idOrder(ErpCtApprovalRecord record, ErpCtApprovalRecord latest) {
        return Long.compare(ConvertHelper.toLong(record.getId()), ConvertHelper.toLong(latest.getId()));
    }

    /**
     * 派生驳回计数（D3）：(contractId, approvalOrder) 组的 REJECTED 记录数。
     * 追加行生命周期（D7）下随轮次递增，锁定可达。
     */
    public int rejectedCount(String contractId, Integer approvalOrder, IServiceContext ctx) {
        int count = 0;
        for (ErpCtApprovalRecord record : findRecords(contractId, ctx)) {
            if (approvalOrder.equals(record.getApprovalOrder())
                    && ErpCtConstants.APPROVAL_STATUS_REJECTED.equals(record.getApprovalStatus())) {
                count++;
            }
        }
        return count;
    }

    /**
     * 最新被驳回节点（D7 resubmit 基准）：最新轮次中 id 最大的 REJECTED 链记录；
     * 无驳回返回 null。非链记录（terminate，approvalMatrixId=null）排除。
     */
    public ErpCtApprovalRecord latestRejected(String contractId, IServiceContext ctx) {
        ErpCtApprovalRecord latest = null;
        for (ErpCtApprovalRecord record : findRecords(contractId, ctx)) {
            if (record.getApprovalMatrixId() == null) {
                continue;
            }
            if (ErpCtConstants.APPROVAL_STATUS_REJECTED.equals(record.getApprovalStatus())
                    && (latest == null || idOrder(record, latest) > 0)) {
                latest = record;
            }
        }
        return latest;
    }

    /**
     * 链完整性（activate 前置校验）：每个出现过的链节点（approvalMatrixId != null 的
     * 不同 approvalOrder）的最新记录均 APPROVED 时链完整；零链记录亦视为完整（引擎未启用
     * 或矩阵无匹配节点 = 无需审批）。终止记录（approvalMatrixId=null）不参与。
     */
    public boolean isChainComplete(String contractId, IServiceContext ctx) {
        Set<Integer> orders = new LinkedHashSet<>();
        for (ErpCtApprovalRecord record : findRecords(contractId, ctx)) {
            if (record.getApprovalMatrixId() != null && record.getApprovalOrder() != null) {
                orders.add(record.getApprovalOrder());
            }
        }
        for (Integer order : orders) {
            ErpCtApprovalRecord latest = latestRecord(contractId, order, ctx);
            if (latest == null
                    || !ErpCtConstants.APPROVAL_STATUS_APPROVED.equals(latest.getApprovalStatus())) {
                return false;
            }
        }
        return true;
    }

    /** 合同是否存在待处理（PENDING）终止申请记录（D1 terminate 幂等守卫）。 */
    public boolean hasPendingTermination(String contractId, IServiceContext ctx) {
        for (ErpCtApprovalRecord record : findRecords(contractId, ctx)) {
            if (record.getApprovalMatrixId() == null
                    && ErpCtConstants.APPROVAL_STATUS_PENDING.equals(record.getApprovalStatus())) {
                return true;
            }
        }
        return false;
    }

    // ---------- 匹配语义 helpers ----------

    protected boolean matchContractType(String nodeType, String contractType) {
        return nodeType == null || nodeType.equals(contractType);
    }

    protected boolean matchOrg(String nodeOrgId, String contractOrgId) {
        return nodeOrgId == null || nodeOrgId.equals(contractOrgId);
    }

    protected boolean matchAmount(ErpCtApprovalMatrix node, BigDecimal total) {
        if (total == null) {
            return false;
        }
        if (node.getMinAmount() != null && total.compareTo(node.getMinAmount()) < 0) {
            return false;
        }
        if (node.getMaxAmount() != null && total.compareTo(node.getMaxAmount()) > 0) {
            return false;
        }
        return true;
    }
}

package app.erp.cs.service.entity;

import app.erp.cs.service.ErpCsConstants;

/**
 * 工单优先级排序助手。将字典码值映射为可比较的序数（数值越大优先级越高）。
 *
 * <p>权威：{@code module-cs/model/app-erp-cs.orm.xml} dict {@code erp-cs/ticket-priority}。
 */
public final class TicketPriorityRank {

    private TicketPriorityRank() {
    }

    /** 返回优先级序数；null 或未知返回 0（最低）。URGENT=40 / HIGH=30 / NORMAL=20 / LOW=10。 */
    public static int rank(String priority) {
        if (priority == null) {
            return 0;
        }
        switch (priority) {
            case ErpCsConstants.TICKET_PRIORITY_URGENT:
                return 40;
            case ErpCsConstants.TICKET_PRIORITY_HIGH:
                return 30;
            case ErpCsConstants.TICKET_PRIORITY_NORMAL:
                return 20;
            case ErpCsConstants.TICKET_PRIORITY_LOW:
                return 10;
            default:
                return 0;
        }
    }
}

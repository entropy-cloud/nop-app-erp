package app.erp.hr.dao;

@SuppressWarnings({"PMD","java:S116"})
public interface _ErpHrDaoConstants {
    
    /**
     * 雇佣状态: 在职 
     */
    String EMPLOYMENT_STATUS_ACTIVE = "ACTIVE";
                    
    /**
     * 雇佣状态: 试用 
     */
    String EMPLOYMENT_STATUS_PROBATION = "PROBATION";
                    
    /**
     * 雇佣状态: 已离职 
     */
    String EMPLOYMENT_STATUS_RESIGNED = "RESIGNED";
                    
    /**
     * 雇佣状态: 已解雇 
     */
    String EMPLOYMENT_STATUS_TERMINATED = "TERMINATED";
                    
    /**
     * 雇佣状态: 已退休 
     */
    String EMPLOYMENT_STATUS_RETIRED = "RETIRED";
                    
    /**
     * 员工类型: 全职 
     */
    String EMPLOYEE_TYPE_FULL_TIME = "FULL_TIME";
                    
    /**
     * 员工类型: 兼职 
     */
    String EMPLOYEE_TYPE_PART_TIME = "PART_TIME";
                    
    /**
     * 员工类型: 劳务 
     */
    String EMPLOYEE_TYPE_CONTRACTOR = "CONTRACTOR";
                    
    /**
     * 员工类型: 实习 
     */
    String EMPLOYEE_TYPE_INTERN = "INTERN";
                    
    /**
     * 性别: 男 
     */
    String GENDER_MALE = "MALE";
                    
    /**
     * 性别: 女 
     */
    String GENDER_FEMALE = "FEMALE";
                    
    /**
     * 婚姻状况: 未婚 
     */
    String MARITAL_STATUS_SINGLE = "SINGLE";
                    
    /**
     * 婚姻状况: 已婚 
     */
    String MARITAL_STATUS_MARRIED = "MARRIED";
                    
    /**
     * 婚姻状况: 离婚 
     */
    String MARITAL_STATUS_DIVORCED = "DIVORCED";
                    
    /**
     * 合同类型: 固定期限 
     */
    String CONTRACT_TYPE_FIXED_TERM = "FIXED_TERM";
                    
    /**
     * 合同类型: 无固定期限 
     */
    String CONTRACT_TYPE_OPEN_ENDED = "OPEN_ENDED";
                    
    /**
     * 合同类型: 项目制 
     */
    String CONTRACT_TYPE_PROJECT = "PROJECT";
                    
    /**
     * 合同状态: 生效中 
     */
    String CONTRACT_STATUS_ACTIVE = "ACTIVE";
                    
    /**
     * 合同状态: 已到期 
     */
    String CONTRACT_STATUS_EXPIRED = "EXPIRED";
                    
    /**
     * 合同状态: 已解除 
     */
    String CONTRACT_STATUS_TERMINATED = "TERMINATED";
                    
    /**
     * 合同状态: 已中止 
     */
    String CONTRACT_STATUS_SUSPENDED = "SUSPENDED";
                    
    /**
     * 休假类型: 年假 
     */
    String LEAVE_TYPE_ANNUAL = "ANNUAL";
                    
    /**
     * 休假类型: 病假 
     */
    String LEAVE_TYPE_SICK = "SICK";
                    
    /**
     * 休假类型: 事假 
     */
    String LEAVE_TYPE_PERSONAL = "PERSONAL";
                    
    /**
     * 休假类型: 婚假 
     */
    String LEAVE_TYPE_MARRIAGE = "MARRIAGE";
                    
    /**
     * 休假类型: 产假 
     */
    String LEAVE_TYPE_MATERNITY = "MATERNITY";
                    
    /**
     * 休假类型: 丧假 
     */
    String LEAVE_TYPE_FUNERAL = "FUNERAL";
                    
    /**
     * 休假类型: 调休 
     */
    String LEAVE_TYPE_COMPENSATORY = "COMPENSATORY";
                    
    /**
     * 休假状态: 草稿 
     */
    String LEAVE_STATUS_DRAFT = "DRAFT";
                    
    /**
     * 休假状态: 已提交 
     */
    String LEAVE_STATUS_SUBMITTED = "SUBMITTED";
                    
    /**
     * 休假状态: 已批准 
     */
    String LEAVE_STATUS_APPROVED = "APPROVED";
                    
    /**
     * 休假状态: 已驳回 
     */
    String LEAVE_STATUS_REJECTED = "REJECTED";
                    
    /**
     * 休假状态: 已取消 
     */
    String LEAVE_STATUS_CANCELLED = "CANCELLED";
                    
    /**
     * 工时表状态: 草稿 
     */
    String TIMESHEET_STATUS_DRAFT = "DRAFT";
                    
    /**
     * 工时表状态: 已提交 
     */
    String TIMESHEET_STATUS_SUBMITTED = "SUBMITTED";
                    
    /**
     * 工时表状态: 已批准 
     */
    String TIMESHEET_STATUS_APPROVED = "APPROVED";
                    
    /**
     * 工时表状态: 已驳回 
     */
    String TIMESHEET_STATUS_REJECTED = "REJECTED";
                    
    /**
     * 招聘状态: 待处理 
     */
    String RECRUITMENT_STATUS_OPEN = "OPEN";
                    
    /**
     * 招聘状态: 简历筛选 
     */
    String RECRUITMENT_STATUS_SCREENING = "SCREENING";
                    
    /**
     * 招聘状态: 面试中 
     */
    String RECRUITMENT_STATUS_INTERVIEW = "INTERVIEW";
                    
    /**
     * 招聘状态: 已发Offer 
     */
    String RECRUITMENT_STATUS_OFFERED = "OFFERED";
                    
    /**
     * 招聘状态: 已入职 
     */
    String RECRUITMENT_STATUS_HIRED = "HIRED";
                    
    /**
     * 招聘状态: 已拒绝 
     */
    String RECRUITMENT_STATUS_REJECTED = "REJECTED";
                    
    /**
     * 招聘状态: 已关闭 
     */
    String RECRUITMENT_STATUS_CLOSED = "CLOSED";
                    
    /**
     * 薪资支付状态: 待支付 
     */
    String SALARY_PAYMENT_STATUS_PENDING = "PENDING";
                    
    /**
     * 薪资支付状态: 已支付 
     */
    String SALARY_PAYMENT_STATUS_PAID = "PAID";
                    
    /**
     * 薪资支付状态: 已作废 
     */
    String SALARY_PAYMENT_STATUS_VOID = "VOID";
                    
    /**
     * 考勤来源: 打卡 
     */
    String ATTENDANCE_SOURCE_CARD = "CARD";
                    
    /**
     * 考勤来源: 指纹 
     */
    String ATTENDANCE_SOURCE_BIOMETRIC = "BIOMETRIC";
                    
    /**
     * 考勤来源: 移动 
     */
    String ATTENDANCE_SOURCE_MOBILE = "MOBILE";
                    
    /**
     * 招聘来源: 招聘网站 
     */
    String RECRUITMENT_SOURCE_RECRUITMENT_WEBSITE = "RECRUITMENT_WEBSITE";
                    
    /**
     * 招聘来源: 猎头/中介 
     */
    String RECRUITMENT_SOURCE_AGENCY = "AGENCY";
                    
    /**
     * 招聘来源: 员工推荐 
     */
    String RECRUITMENT_SOURCE_REFERRAL = "REFERRAL";
                    
    /**
     * 招聘来源: 校园招聘 
     */
    String RECRUITMENT_SOURCE_CAMPUS = "CAMPUS";
                    
    /**
     * 模拟状态: 草稿 
     */
    String SIMULATION_STATUS_DRAFT = "DRAFT";
                    
    /**
     * 模拟状态: 审核中 
     */
    String SIMULATION_STATUS_IN_REVIEW = "IN_REVIEW";
                    
    /**
     * 模拟状态: 已审批 
     */
    String SIMULATION_STATUS_APPROVED = "APPROVED";
                    
    /**
     * 模拟状态: 已驳回 
     */
    String SIMULATION_STATUS_REJECTED = "REJECTED";
                    
    /**
     * 模拟状态: 已转正式 
     */
    String SIMULATION_STATUS_CONVERTED = "CONVERTED";
                    
    /**
     * 班次类型: 固定班 
     */
    String SHIFT_TYPE_FIXED = "FIXED";
                    
    /**
     * 班次类型: 倒班 
     */
    String SHIFT_TYPE_ROTATING = "ROTATING";
                    
    /**
     * 班次类型: 弹性班 
     */
    String SHIFT_TYPE_FLEXIBLE = "FLEXIBLE";
                    
    /**
     * 调换状态: 待审批 
     */
    String SWAP_STATUS_PENDING = "PENDING";
                    
    /**
     * 调换状态: 已批准 
     */
    String SWAP_STATUS_APPROVED = "APPROVED";
                    
    /**
     * 调换状态: 已驳回 
     */
    String SWAP_STATUS_REJECTED = "REJECTED";
                    
    /**
     * 调换状态: 已取消 
     */
    String SWAP_STATUS_CANCELLED = "CANCELLED";
                    
    /**
     * 调研类型: 年度敬业度调研 
     */
    String SURVEY_TYPE_ANNUAL_ENGAGEMENT = "ANNUAL_ENGAGEMENT";
                    
    /**
     * 调研类型: 脉搏调研 
     */
    String SURVEY_TYPE_PULSE = "PULSE";
                    
    /**
     * 调研类型: eNPS调研 
     */
    String SURVEY_TYPE_ENPS = "ENPS";
                    
    /**
     * 调研类型: 临时调研 
     */
    String SURVEY_TYPE_ADHOC = "ADHOC";
                    
    /**
     * 调研状态: 草稿 
     */
    String SURVEY_STATUS_DRAFT = "DRAFT";
                    
    /**
     * 调研状态: 进行中 
     */
    String SURVEY_STATUS_OPEN = "OPEN";
                    
    /**
     * 调研状态: 已截止 
     */
    String SURVEY_STATUS_CLOSED = "CLOSED";
                    
    /**
     * 调研状态: 已归档 
     */
    String SURVEY_STATUS_ARCHIVED = "ARCHIVED";
                    
    /**
     * 题目类型: 评分题 
     */
    String QUESTION_TYPE_RATING = "RATING";
                    
    /**
     * 题目类型: 单选题 
     */
    String QUESTION_TYPE_SINGLE_CHOICE = "SINGLE_CHOICE";
                    
    /**
     * 题目类型: 多选题 
     */
    String QUESTION_TYPE_MULTI_CHOICE = "MULTI_CHOICE";
                    
    /**
     * 题目类型: 开放文本 
     */
    String QUESTION_TYPE_OPEN_TEXT = "OPEN_TEXT";
                    
    /**
     * 题目类型: eNPS题 
     */
    String QUESTION_TYPE_ENPS = "ENPS";
                    
    /**
     * 驱动因子分类: 成长发展 
     */
    String DRIVER_CATEGORY_GROWTH = "GROWTH";
                    
    /**
     * 驱动因子分类: 认可激励 
     */
    String DRIVER_CATEGORY_RECOGNITION = "RECOGNITION";
                    
    /**
     * 驱动因子分类: 管理支持 
     */
    String DRIVER_CATEGORY_MANAGEMENT = "MANAGEMENT";
                    
    /**
     * 驱动因子分类: 健康福祉 
     */
    String DRIVER_CATEGORY_WELLBEING = "WELLBEING";
                    
    /**
     * 驱动因子分类: 文化认同 
     */
    String DRIVER_CATEGORY_ALIGNMENT = "ALIGNMENT";
                    
    /**
     * 胜任力分类: 技能 
     */
    String COMPETENCY_CATEGORY_SKILL = "SKILL";
                    
    /**
     * 胜任力分类: 行为 
     */
    String COMPETENCY_CATEGORY_BEHAVIOR = "BEHAVIOR";
                    
    /**
     * 胜任力分类: 知识 
     */
    String COMPETENCY_CATEGORY_KNOWLEDGE = "KNOWLEDGE";
                    
    /**
     * 评估类型: 自评 
     */
    String ASSESSMENT_TYPE_SELF = "SELF";
                    
    /**
     * 评估类型: 上级评估 
     */
    String ASSESSMENT_TYPE_MANAGER = "MANAGER";
                    
    /**
     * 评估类型: 同级评估 
     */
    String ASSESSMENT_TYPE_PEER = "PEER";
                    
    /**
     * 评估类型: 下级评估 
     */
    String ASSESSMENT_TYPE_SUBORDINATE = "SUBORDINATE";
                    
    /**
     * 评估类型: 360评估 
     */
    String ASSESSMENT_TYPE_360 = "360";
                    
    /**
     * 评估状态: 草稿 
     */
    String ASSESSMENT_STATUS_DRAFT = "DRAFT";
                    
    /**
     * 评估状态: 已提交 
     */
    String ASSESSMENT_STATUS_SUBMITTED = "SUBMITTED";
                    
    /**
     * 评估状态: 已完成 
     */
    String ASSESSMENT_STATUS_COMPLETED = "COMPLETED";
                    
    /**
     * 差距严重程度: 无差距 
     */
    String GAP_SEVERITY_NONE = "NONE";
                    
    /**
     * 差距严重程度: 轻微 
     */
    String GAP_SEVERITY_MINOR = "MINOR";
                    
    /**
     * 差距严重程度: 明显 
     */
    String GAP_SEVERITY_MODERATE = "MODERATE";
                    
    /**
     * 差距严重程度: 严重 
     */
    String GAP_SEVERITY_CRITICAL = "CRITICAL";
                    
    /**
     * 发展计划状态: 草稿 
     */
    String DEVPLAN_STATUS_DRAFT = "DRAFT";
                    
    /**
     * 发展计划状态: 进行中 
     */
    String DEVPLAN_STATUS_IN_PROGRESS = "IN_PROGRESS";
                    
    /**
     * 发展计划状态: 已完成 
     */
    String DEVPLAN_STATUS_COMPLETED = "COMPLETED";
                    
    /**
     * 发展计划状态: 已取消 
     */
    String DEVPLAN_STATUS_CANCELLED = "CANCELLED";
                    
    /**
     * 计划项状态: 未开始 
     */
    String PLAN_ITEM_STATUS_NOT_STARTED = "NOT_STARTED";
                    
    /**
     * 计划项状态: 进行中 
     */
    String PLAN_ITEM_STATUS_IN_PROGRESS = "IN_PROGRESS";
                    
    /**
     * 计划项状态: 已完成 
     */
    String PLAN_ITEM_STATUS_ACHIEVED = "ACHIEVED";
                    
    /**
     * 计划项状态: 已逾期 
     */
    String PLAN_ITEM_STATUS_OVERDUE = "OVERDUE";
                    
}

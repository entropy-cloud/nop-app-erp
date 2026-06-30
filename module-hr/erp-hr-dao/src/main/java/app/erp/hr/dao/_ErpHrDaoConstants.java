package app.erp.hr.dao;

@SuppressWarnings({"PMD","java:S116"})
public interface _ErpHrDaoConstants {
    
    /**
     * 雇佣状态: 在职 
     */
    int EMPLOYMENT_STATUS_ACTIVE = 10;
                    
    /**
     * 雇佣状态: 试用 
     */
    int EMPLOYMENT_STATUS_PROBATION = 20;
                    
    /**
     * 雇佣状态: 已离职 
     */
    int EMPLOYMENT_STATUS_RESIGNED = 30;
                    
    /**
     * 雇佣状态: 已解雇 
     */
    int EMPLOYMENT_STATUS_TERMINATED = 40;
                    
    /**
     * 雇佣状态: 已退休 
     */
    int EMPLOYMENT_STATUS_RETIRED = 50;
                    
    /**
     * 员工类型: 全职 
     */
    int EMPLOYEE_TYPE_FULL_TIME = 10;
                    
    /**
     * 员工类型: 兼职 
     */
    int EMPLOYEE_TYPE_PART_TIME = 20;
                    
    /**
     * 员工类型: 劳务 
     */
    int EMPLOYEE_TYPE_CONTRACTOR = 30;
                    
    /**
     * 员工类型: 实习 
     */
    int EMPLOYEE_TYPE_INTERN = 40;
                    
    /**
     * 性别: 男 
     */
    int GENDER_MALE = 10;
                    
    /**
     * 性别: 女 
     */
    int GENDER_FEMALE = 20;
                    
    /**
     * 婚姻状况: 未婚 
     */
    int MARITAL_STATUS_SINGLE = 10;
                    
    /**
     * 婚姻状况: 已婚 
     */
    int MARITAL_STATUS_MARRIED = 20;
                    
    /**
     * 婚姻状况: 离婚 
     */
    int MARITAL_STATUS_DIVORCED = 30;
                    
    /**
     * 合同类型: 固定期限 
     */
    int CONTRACT_TYPE_FIXED_TERM = 10;
                    
    /**
     * 合同类型: 无固定期限 
     */
    int CONTRACT_TYPE_OPEN_ENDED = 20;
                    
    /**
     * 合同类型: 项目制 
     */
    int CONTRACT_TYPE_PROJECT = 30;
                    
    /**
     * 合同状态: 生效中 
     */
    int CONTRACT_STATUS_ACTIVE = 10;
                    
    /**
     * 合同状态: 已到期 
     */
    int CONTRACT_STATUS_EXPIRED = 20;
                    
    /**
     * 合同状态: 已解除 
     */
    int CONTRACT_STATUS_TERMINATED = 30;
                    
    /**
     * 合同状态: 已中止 
     */
    int CONTRACT_STATUS_SUSPENDED = 40;
                    
    /**
     * 休假类型: 年假 
     */
    int LEAVE_TYPE_ANNUAL = 10;
                    
    /**
     * 休假类型: 病假 
     */
    int LEAVE_TYPE_SICK = 20;
                    
    /**
     * 休假类型: 事假 
     */
    int LEAVE_TYPE_PERSONAL = 30;
                    
    /**
     * 休假类型: 婚假 
     */
    int LEAVE_TYPE_MARRIAGE = 40;
                    
    /**
     * 休假类型: 产假 
     */
    int LEAVE_TYPE_MATERNITY = 50;
                    
    /**
     * 休假类型: 丧假 
     */
    int LEAVE_TYPE_FUNERAL = 60;
                    
    /**
     * 休假类型: 调休 
     */
    int LEAVE_TYPE_COMPENSATORY = 70;
                    
    /**
     * 休假状态: 草稿 
     */
    int LEAVE_STATUS_DRAFT = 10;
                    
    /**
     * 休假状态: 已提交 
     */
    int LEAVE_STATUS_SUBMITTED = 20;
                    
    /**
     * 休假状态: 已批准 
     */
    int LEAVE_STATUS_APPROVED = 30;
                    
    /**
     * 休假状态: 已驳回 
     */
    int LEAVE_STATUS_REJECTED = 40;
                    
    /**
     * 休假状态: 已取消 
     */
    int LEAVE_STATUS_CANCELLED = 50;
                    
    /**
     * 工时表状态: 草稿 
     */
    int TIMESHEET_STATUS_DRAFT = 10;
                    
    /**
     * 工时表状态: 已提交 
     */
    int TIMESHEET_STATUS_SUBMITTED = 20;
                    
    /**
     * 工时表状态: 已批准 
     */
    int TIMESHEET_STATUS_APPROVED = 30;
                    
    /**
     * 工时表状态: 已驳回 
     */
    int TIMESHEET_STATUS_REJECTED = 40;
                    
    /**
     * 招聘状态: 待处理 
     */
    int RECRUITMENT_STATUS_OPEN = 10;
                    
    /**
     * 招聘状态: 简历筛选 
     */
    int RECRUITMENT_STATUS_SCREENING = 20;
                    
    /**
     * 招聘状态: 面试中 
     */
    int RECRUITMENT_STATUS_INTERVIEW = 30;
                    
    /**
     * 招聘状态: 已发Offer 
     */
    int RECRUITMENT_STATUS_OFFERED = 40;
                    
    /**
     * 招聘状态: 已入职 
     */
    int RECRUITMENT_STATUS_HIRED = 50;
                    
    /**
     * 招聘状态: 已拒绝 
     */
    int RECRUITMENT_STATUS_REJECTED = 60;
                    
    /**
     * 招聘状态: 已关闭 
     */
    int RECRUITMENT_STATUS_CLOSED = 70;
                    
    /**
     * 薪资支付状态: 待支付 
     */
    int SALARY_PAYMENT_STATUS_PENDING = 10;
                    
    /**
     * 薪资支付状态: 已支付 
     */
    int SALARY_PAYMENT_STATUS_PAID = 20;
                    
    /**
     * 薪资支付状态: 已作废 
     */
    int SALARY_PAYMENT_STATUS_VOID = 30;
                    
    /**
     * 考勤来源: 打卡 
     */
    int ATTENDANCE_SOURCE_CARD = 10;
                    
    /**
     * 考勤来源: 指纹 
     */
    int ATTENDANCE_SOURCE_BIOMETRIC = 20;
                    
    /**
     * 考勤来源: 移动 
     */
    int ATTENDANCE_SOURCE_MOBILE = 30;
                    
    /**
     * 招聘来源: 招聘网站 
     */
    int RECRUITMENT_SOURCE_RECRUITMENT_WEBSITE = 10;
                    
    /**
     * 招聘来源: 猎头/中介 
     */
    int RECRUITMENT_SOURCE_AGENCY = 20;
                    
    /**
     * 招聘来源: 员工推荐 
     */
    int RECRUITMENT_SOURCE_REFERRAL = 30;
                    
    /**
     * 招聘来源: 校园招聘 
     */
    int RECRUITMENT_SOURCE_CAMPUS = 40;
                    
    /**
     * 模拟状态: 草稿 
     */
    int SIMULATION_STATUS_DRAFT = 10;
                    
    /**
     * 模拟状态: 审核中 
     */
    int SIMULATION_STATUS_IN_REVIEW = 20;
                    
    /**
     * 模拟状态: 已审批 
     */
    int SIMULATION_STATUS_APPROVED = 30;
                    
    /**
     * 模拟状态: 已驳回 
     */
    int SIMULATION_STATUS_REJECTED = 40;
                    
    /**
     * 模拟状态: 已转正式 
     */
    int SIMULATION_STATUS_CONVERTED = 50;
                    
    /**
     * 班次类型: 固定班 
     */
    int SHIFT_TYPE_FIXED = 10;
                    
    /**
     * 班次类型: 倒班 
     */
    int SHIFT_TYPE_ROTATING = 20;
                    
    /**
     * 班次类型: 弹性班 
     */
    int SHIFT_TYPE_FLEXIBLE = 30;
                    
    /**
     * 调换状态: 待审批 
     */
    int SWAP_STATUS_PENDING = 10;
                    
    /**
     * 调换状态: 已批准 
     */
    int SWAP_STATUS_APPROVED = 20;
                    
    /**
     * 调换状态: 已驳回 
     */
    int SWAP_STATUS_REJECTED = 30;
                    
    /**
     * 调换状态: 已取消 
     */
    int SWAP_STATUS_CANCELLED = 40;
                    
    /**
     * 调研类型: 年度敬业度调研 
     */
    int SURVEY_TYPE_ANNUAL_ENGAGEMENT = 10;
                    
    /**
     * 调研类型: 脉搏调研 
     */
    int SURVEY_TYPE_PULSE = 20;
                    
    /**
     * 调研类型: eNPS调研 
     */
    int SURVEY_TYPE_ENPS = 30;
                    
    /**
     * 调研类型: 临时调研 
     */
    int SURVEY_TYPE_ADHOC = 40;
                    
    /**
     * 调研状态: 草稿 
     */
    int SURVEY_STATUS_DRAFT = 10;
                    
    /**
     * 调研状态: 进行中 
     */
    int SURVEY_STATUS_OPEN = 20;
                    
    /**
     * 调研状态: 已截止 
     */
    int SURVEY_STATUS_CLOSED = 30;
                    
    /**
     * 调研状态: 已归档 
     */
    int SURVEY_STATUS_ARCHIVED = 40;
                    
    /**
     * 题目类型: 评分题 
     */
    int QUESTION_TYPE_RATING = 10;
                    
    /**
     * 题目类型: 单选题 
     */
    int QUESTION_TYPE_SINGLE_CHOICE = 20;
                    
    /**
     * 题目类型: 多选题 
     */
    int QUESTION_TYPE_MULTI_CHOICE = 30;
                    
    /**
     * 题目类型: 开放文本 
     */
    int QUESTION_TYPE_OPEN_TEXT = 40;
                    
    /**
     * 题目类型: eNPS题 
     */
    int QUESTION_TYPE_ENPS = 50;
                    
    /**
     * 驱动因子分类: 成长发展 
     */
    int DRIVER_CATEGORY_GROWTH = 10;
                    
    /**
     * 驱动因子分类: 认可激励 
     */
    int DRIVER_CATEGORY_RECOGNITION = 20;
                    
    /**
     * 驱动因子分类: 管理支持 
     */
    int DRIVER_CATEGORY_MANAGEMENT = 30;
                    
    /**
     * 驱动因子分类: 健康福祉 
     */
    int DRIVER_CATEGORY_WELLBEING = 40;
                    
    /**
     * 驱动因子分类: 文化认同 
     */
    int DRIVER_CATEGORY_ALIGNMENT = 50;
                    
    /**
     * 胜任力分类: 技能 
     */
    int COMPETENCY_CATEGORY_SKILL = 10;
                    
    /**
     * 胜任力分类: 行为 
     */
    int COMPETENCY_CATEGORY_BEHAVIOR = 20;
                    
    /**
     * 胜任力分类: 知识 
     */
    int COMPETENCY_CATEGORY_KNOWLEDGE = 30;
                    
    /**
     * 评估类型: 自评 
     */
    int ASSESSMENT_TYPE_SELF = 10;
                    
    /**
     * 评估类型: 上级评估 
     */
    int ASSESSMENT_TYPE_MANAGER = 20;
                    
    /**
     * 评估类型: 同级评估 
     */
    int ASSESSMENT_TYPE_PEER = 30;
                    
    /**
     * 评估类型: 下级评估 
     */
    int ASSESSMENT_TYPE_SUBORDINATE = 40;
                    
    /**
     * 评估类型: 360评估 
     */
    int ASSESSMENT_TYPE_360 = 50;
                    
    /**
     * 评估状态: 草稿 
     */
    int ASSESSMENT_STATUS_DRAFT = 10;
                    
    /**
     * 评估状态: 已提交 
     */
    int ASSESSMENT_STATUS_SUBMITTED = 20;
                    
    /**
     * 评估状态: 已完成 
     */
    int ASSESSMENT_STATUS_COMPLETED = 30;
                    
    /**
     * 差距严重程度: 无差距 
     */
    int GAP_SEVERITY_NONE = 10;
                    
    /**
     * 差距严重程度: 轻微 
     */
    int GAP_SEVERITY_MINOR = 20;
                    
    /**
     * 差距严重程度: 明显 
     */
    int GAP_SEVERITY_MODERATE = 30;
                    
    /**
     * 差距严重程度: 严重 
     */
    int GAP_SEVERITY_CRITICAL = 40;
                    
    /**
     * 发展计划状态: 草稿 
     */
    int DEVPLAN_STATUS_DRAFT = 10;
                    
    /**
     * 发展计划状态: 进行中 
     */
    int DEVPLAN_STATUS_IN_PROGRESS = 20;
                    
    /**
     * 发展计划状态: 已完成 
     */
    int DEVPLAN_STATUS_COMPLETED = 30;
                    
    /**
     * 发展计划状态: 已取消 
     */
    int DEVPLAN_STATUS_CANCELLED = 40;
                    
    /**
     * 计划项状态: 未开始 
     */
    int PLAN_ITEM_STATUS_NOT_STARTED = 10;
                    
    /**
     * 计划项状态: 进行中 
     */
    int PLAN_ITEM_STATUS_IN_PROGRESS = 20;
                    
    /**
     * 计划项状态: 已完成 
     */
    int PLAN_ITEM_STATUS_ACHIEVED = 30;
                    
    /**
     * 计划项状态: 已逾期 
     */
    int PLAN_ITEM_STATUS_OVERDUE = 40;
                    
}

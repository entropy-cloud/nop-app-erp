package app.erp.crm.dao.entity._gen;

import io.nop.orm.model.IEntityModel;
import io.nop.orm.support.DynamicOrmEntity;
import io.nop.orm.support.OrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code
import io.nop.orm.IOrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code

import io.nop.api.core.convert.ConvertHelper;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;

import app.erp.crm.dao.entity.ErpCrmLead;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  线索/商机: erp_crm_lead
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpCrmLead extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 编码: CODE VARCHAR */
    public static final String PROP_NAME_code = "code";
    public static final int PROP_ID_code = 2;
    
    /* 业务组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 3;
    
    /* 线索/商机类型: LEAD_TYPE VARCHAR */
    public static final String PROP_NAME_leadType = "leadType";
    public static final int PROP_ID_leadType = 4;
    
    /* 客户: PARTNER_ID BIGINT */
    public static final String PROP_NAME_partnerId = "partnerId";
    public static final int PROP_ID_partnerId = 5;
    
    /* 联系人姓名: CONTACT_NAME VARCHAR */
    public static final String PROP_NAME_contactName = "contactName";
    public static final int PROP_ID_contactName = 6;
    
    /* 联系电话: CONTACT_PHONE VARCHAR */
    public static final String PROP_NAME_contactPhone = "contactPhone";
    public static final int PROP_ID_contactPhone = 7;
    
    /* 联系邮箱: CONTACT_EMAIL VARCHAR */
    public static final String PROP_NAME_contactEmail = "contactEmail";
    public static final int PROP_ID_contactEmail = 8;
    
    /* 公司名称: COMPANY_NAME VARCHAR */
    public static final String PROP_NAME_companyName = "companyName";
    public static final int PROP_ID_companyName = 9;
    
    /* 职位: JOB_TITLE VARCHAR */
    public static final String PROP_NAME_jobTitle = "jobTitle";
    public static final int PROP_ID_jobTitle = 10;
    
    /* 部门: DEPARTMENT VARCHAR */
    public static final String PROP_NAME_department = "department";
    public static final int PROP_ID_department = 11;
    
    /* 线索来源: SOURCE_ID BIGINT */
    public static final String PROP_NAME_sourceId = "sourceId";
    public static final int PROP_ID_sourceId = 12;
    
    /* 线索状态: LEAD_STATUS_ID BIGINT */
    public static final String PROP_NAME_leadStatusId = "leadStatusId";
    public static final int PROP_ID_leadStatusId = 13;
    
    /* 漏斗阶段: STAGE_ID BIGINT */
    public static final String PROP_NAME_stageId = "stageId";
    public static final int PROP_ID_stageId = 14;
    
    /* 预期收入: EXPECTED_REVENUE DECIMAL */
    public static final String PROP_NAME_expectedRevenue = "expectedRevenue";
    public static final int PROP_ID_expectedRevenue = 15;
    
    /* 乐观预期: BEST_CASE_AMOUNT DECIMAL */
    public static final String PROP_NAME_bestCaseAmount = "bestCaseAmount";
    public static final int PROP_ID_bestCaseAmount = 16;
    
    /* 悲观预期: WORST_CASE_AMOUNT DECIMAL */
    public static final String PROP_NAME_worstCaseAmount = "worstCaseAmount";
    public static final int PROP_ID_worstCaseAmount = 17;
    
    /* 周期性收入: RECURRING_REVENUE DECIMAL */
    public static final String PROP_NAME_recurringRevenue = "recurringRevenue";
    public static final int PROP_ID_recurringRevenue = 18;
    
    /* 周期性计划: RECURRING_PLAN VARCHAR */
    public static final String PROP_NAME_recurringPlan = "recurringPlan";
    public static final int PROP_ID_recurringPlan = 19;
    
    /* 预期签单日: EXPECTED_CLOSE_DATE DATE */
    public static final String PROP_NAME_expectedCloseDate = "expectedCloseDate";
    public static final int PROP_ID_expectedCloseDate = 20;
    
    /* 成交概率: PROBABILITY INTEGER */
    public static final String PROP_NAME_probability = "probability";
    public static final int PROP_ID_probability = 21;
    
    /* 营销活动: CAMPAIGN_ID BIGINT */
    public static final String PROP_NAME_campaignId = "campaignId";
    public static final int PROP_ID_campaignId = 22;
    
    /* UTM Medium: UTM_MEDIUM VARCHAR */
    public static final String PROP_NAME_utmMedium = "utmMedium";
    public static final int PROP_ID_utmMedium = 23;
    
    /* UTM Source: UTM_SOURCE VARCHAR */
    public static final String PROP_NAME_utmSource = "utmSource";
    public static final int PROP_ID_utmSource = 24;
    
    /* 负责人: OWNER_ID VARCHAR */
    public static final String PROP_NAME_ownerId = "ownerId";
    public static final int PROP_ID_ownerId = 25;
    
    /* 销售团队: TEAM_ID BIGINT */
    public static final String PROP_NAME_teamId = "teamId";
    public static final int PROP_ID_teamId = 26;
    
    /* 丢单原因: LOST_REASON_ID BIGINT */
    public static final String PROP_NAME_lostReasonId = "lostReasonId";
    public static final int PROP_ID_lostReasonId = 27;
    
    /* 丢单描述: LOST_REASON_DESC VARCHAR */
    public static final String PROP_NAME_lostReasonDesc = "lostReasonDesc";
    public static final int PROP_ID_lostReasonDesc = 28;
    
    /* 最后联系日期: LAST_CONTACT_DATE DATETIME */
    public static final String PROP_NAME_lastContactDate = "lastContactDate";
    public static final int PROP_ID_lastContactDate = 29;
    
    /* 下次活动日期: NEXT_ACTIVITY_DATE DATETIME */
    public static final String PROP_NAME_nextActivityDate = "nextActivityDate";
    public static final int PROP_ID_nextActivityDate = 30;
    
    /* 转化结果单据类型: RELATED_BILL_TYPE VARCHAR */
    public static final String PROP_NAME_relatedBillType = "relatedBillType";
    public static final int PROP_ID_relatedBillType = 31;
    
    /* 转化结果单据号: RELATED_BILL_CODE VARCHAR */
    public static final String PROP_NAME_relatedBillCode = "relatedBillCode";
    public static final int PROP_ID_relatedBillCode = 32;
    
    /* 单据状态: DOC_STATUS VARCHAR */
    public static final String PROP_NAME_docStatus = "docStatus";
    public static final int PROP_ID_docStatus = 33;
    
    /* 备注: REMARK VARCHAR */
    public static final String PROP_NAME_remark = "remark";
    public static final int PROP_ID_remark = 34;
    
    /* 逻辑删除版本: DEL_VERSION BIGINT */
    public static final String PROP_NAME_delVersion = "delVersion";
    public static final int PROP_ID_delVersion = 35;
    
    /* 数据版本: VERSION INTEGER */
    public static final String PROP_NAME_version = "version";
    public static final int PROP_ID_version = 36;
    
    /* 创建人: CREATED_BY VARCHAR */
    public static final String PROP_NAME_createdBy = "createdBy";
    public static final int PROP_ID_createdBy = 37;
    
    /* 创建时间: CREATE_TIME TIMESTAMP */
    public static final String PROP_NAME_createTime = "createTime";
    public static final int PROP_ID_createTime = 38;
    
    /* 修改人: UPDATED_BY VARCHAR */
    public static final String PROP_NAME_updatedBy = "updatedBy";
    public static final int PROP_ID_updatedBy = 39;
    
    /* 修改时间: UPDATE_TIME TIMESTAMP */
    public static final String PROP_NAME_updateTime = "updateTime";
    public static final int PROP_ID_updateTime = 40;
    
    /* 销售区域: TERRITORY_ID BIGINT */
    public static final String PROP_NAME_territoryId = "territoryId";
    public static final int PROP_ID_territoryId = 41;
    

    private static int _PROP_ID_BOUND = 42;

    
    /* relation:  */
    public static final String PROP_NAME_partner = "partner";
    
    /* relation:  */
    public static final String PROP_NAME_source = "source";
    
    /* relation:  */
    public static final String PROP_NAME_leadStatus = "leadStatus";
    
    /* relation:  */
    public static final String PROP_NAME_stage = "stage";
    
    /* relation:  */
    public static final String PROP_NAME_campaign = "campaign";
    
    /* relation:  */
    public static final String PROP_NAME_team = "team";
    
    /* relation:  */
    public static final String PROP_NAME_territory = "territory";
    
    /* relation:  */
    public static final String PROP_NAME_lostReason = "lostReason";
    
    /* relation:  */
    public static final String PROP_NAME_org = "org";
    
    /* relation:  */
    public static final String PROP_NAME_events = "events";
    
    /* relation:  */
    public static final String PROP_NAME_activities = "activities";
    
    /* relation:  */
    public static final String PROP_NAME_convLogs = "convLogs";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[42];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_code] = PROP_NAME_code;
          PROP_NAME_TO_ID.put(PROP_NAME_code, PROP_ID_code);
      
          PROP_ID_TO_NAME[PROP_ID_orgId] = PROP_NAME_orgId;
          PROP_NAME_TO_ID.put(PROP_NAME_orgId, PROP_ID_orgId);
      
          PROP_ID_TO_NAME[PROP_ID_leadType] = PROP_NAME_leadType;
          PROP_NAME_TO_ID.put(PROP_NAME_leadType, PROP_ID_leadType);
      
          PROP_ID_TO_NAME[PROP_ID_partnerId] = PROP_NAME_partnerId;
          PROP_NAME_TO_ID.put(PROP_NAME_partnerId, PROP_ID_partnerId);
      
          PROP_ID_TO_NAME[PROP_ID_contactName] = PROP_NAME_contactName;
          PROP_NAME_TO_ID.put(PROP_NAME_contactName, PROP_ID_contactName);
      
          PROP_ID_TO_NAME[PROP_ID_contactPhone] = PROP_NAME_contactPhone;
          PROP_NAME_TO_ID.put(PROP_NAME_contactPhone, PROP_ID_contactPhone);
      
          PROP_ID_TO_NAME[PROP_ID_contactEmail] = PROP_NAME_contactEmail;
          PROP_NAME_TO_ID.put(PROP_NAME_contactEmail, PROP_ID_contactEmail);
      
          PROP_ID_TO_NAME[PROP_ID_companyName] = PROP_NAME_companyName;
          PROP_NAME_TO_ID.put(PROP_NAME_companyName, PROP_ID_companyName);
      
          PROP_ID_TO_NAME[PROP_ID_jobTitle] = PROP_NAME_jobTitle;
          PROP_NAME_TO_ID.put(PROP_NAME_jobTitle, PROP_ID_jobTitle);
      
          PROP_ID_TO_NAME[PROP_ID_department] = PROP_NAME_department;
          PROP_NAME_TO_ID.put(PROP_NAME_department, PROP_ID_department);
      
          PROP_ID_TO_NAME[PROP_ID_sourceId] = PROP_NAME_sourceId;
          PROP_NAME_TO_ID.put(PROP_NAME_sourceId, PROP_ID_sourceId);
      
          PROP_ID_TO_NAME[PROP_ID_leadStatusId] = PROP_NAME_leadStatusId;
          PROP_NAME_TO_ID.put(PROP_NAME_leadStatusId, PROP_ID_leadStatusId);
      
          PROP_ID_TO_NAME[PROP_ID_stageId] = PROP_NAME_stageId;
          PROP_NAME_TO_ID.put(PROP_NAME_stageId, PROP_ID_stageId);
      
          PROP_ID_TO_NAME[PROP_ID_expectedRevenue] = PROP_NAME_expectedRevenue;
          PROP_NAME_TO_ID.put(PROP_NAME_expectedRevenue, PROP_ID_expectedRevenue);
      
          PROP_ID_TO_NAME[PROP_ID_bestCaseAmount] = PROP_NAME_bestCaseAmount;
          PROP_NAME_TO_ID.put(PROP_NAME_bestCaseAmount, PROP_ID_bestCaseAmount);
      
          PROP_ID_TO_NAME[PROP_ID_worstCaseAmount] = PROP_NAME_worstCaseAmount;
          PROP_NAME_TO_ID.put(PROP_NAME_worstCaseAmount, PROP_ID_worstCaseAmount);
      
          PROP_ID_TO_NAME[PROP_ID_recurringRevenue] = PROP_NAME_recurringRevenue;
          PROP_NAME_TO_ID.put(PROP_NAME_recurringRevenue, PROP_ID_recurringRevenue);
      
          PROP_ID_TO_NAME[PROP_ID_recurringPlan] = PROP_NAME_recurringPlan;
          PROP_NAME_TO_ID.put(PROP_NAME_recurringPlan, PROP_ID_recurringPlan);
      
          PROP_ID_TO_NAME[PROP_ID_expectedCloseDate] = PROP_NAME_expectedCloseDate;
          PROP_NAME_TO_ID.put(PROP_NAME_expectedCloseDate, PROP_ID_expectedCloseDate);
      
          PROP_ID_TO_NAME[PROP_ID_probability] = PROP_NAME_probability;
          PROP_NAME_TO_ID.put(PROP_NAME_probability, PROP_ID_probability);
      
          PROP_ID_TO_NAME[PROP_ID_campaignId] = PROP_NAME_campaignId;
          PROP_NAME_TO_ID.put(PROP_NAME_campaignId, PROP_ID_campaignId);
      
          PROP_ID_TO_NAME[PROP_ID_utmMedium] = PROP_NAME_utmMedium;
          PROP_NAME_TO_ID.put(PROP_NAME_utmMedium, PROP_ID_utmMedium);
      
          PROP_ID_TO_NAME[PROP_ID_utmSource] = PROP_NAME_utmSource;
          PROP_NAME_TO_ID.put(PROP_NAME_utmSource, PROP_ID_utmSource);
      
          PROP_ID_TO_NAME[PROP_ID_ownerId] = PROP_NAME_ownerId;
          PROP_NAME_TO_ID.put(PROP_NAME_ownerId, PROP_ID_ownerId);
      
          PROP_ID_TO_NAME[PROP_ID_teamId] = PROP_NAME_teamId;
          PROP_NAME_TO_ID.put(PROP_NAME_teamId, PROP_ID_teamId);
      
          PROP_ID_TO_NAME[PROP_ID_lostReasonId] = PROP_NAME_lostReasonId;
          PROP_NAME_TO_ID.put(PROP_NAME_lostReasonId, PROP_ID_lostReasonId);
      
          PROP_ID_TO_NAME[PROP_ID_lostReasonDesc] = PROP_NAME_lostReasonDesc;
          PROP_NAME_TO_ID.put(PROP_NAME_lostReasonDesc, PROP_ID_lostReasonDesc);
      
          PROP_ID_TO_NAME[PROP_ID_lastContactDate] = PROP_NAME_lastContactDate;
          PROP_NAME_TO_ID.put(PROP_NAME_lastContactDate, PROP_ID_lastContactDate);
      
          PROP_ID_TO_NAME[PROP_ID_nextActivityDate] = PROP_NAME_nextActivityDate;
          PROP_NAME_TO_ID.put(PROP_NAME_nextActivityDate, PROP_ID_nextActivityDate);
      
          PROP_ID_TO_NAME[PROP_ID_relatedBillType] = PROP_NAME_relatedBillType;
          PROP_NAME_TO_ID.put(PROP_NAME_relatedBillType, PROP_ID_relatedBillType);
      
          PROP_ID_TO_NAME[PROP_ID_relatedBillCode] = PROP_NAME_relatedBillCode;
          PROP_NAME_TO_ID.put(PROP_NAME_relatedBillCode, PROP_ID_relatedBillCode);
      
          PROP_ID_TO_NAME[PROP_ID_docStatus] = PROP_NAME_docStatus;
          PROP_NAME_TO_ID.put(PROP_NAME_docStatus, PROP_ID_docStatus);
      
          PROP_ID_TO_NAME[PROP_ID_remark] = PROP_NAME_remark;
          PROP_NAME_TO_ID.put(PROP_NAME_remark, PROP_ID_remark);
      
          PROP_ID_TO_NAME[PROP_ID_delVersion] = PROP_NAME_delVersion;
          PROP_NAME_TO_ID.put(PROP_NAME_delVersion, PROP_ID_delVersion);
      
          PROP_ID_TO_NAME[PROP_ID_version] = PROP_NAME_version;
          PROP_NAME_TO_ID.put(PROP_NAME_version, PROP_ID_version);
      
          PROP_ID_TO_NAME[PROP_ID_createdBy] = PROP_NAME_createdBy;
          PROP_NAME_TO_ID.put(PROP_NAME_createdBy, PROP_ID_createdBy);
      
          PROP_ID_TO_NAME[PROP_ID_createTime] = PROP_NAME_createTime;
          PROP_NAME_TO_ID.put(PROP_NAME_createTime, PROP_ID_createTime);
      
          PROP_ID_TO_NAME[PROP_ID_updatedBy] = PROP_NAME_updatedBy;
          PROP_NAME_TO_ID.put(PROP_NAME_updatedBy, PROP_ID_updatedBy);
      
          PROP_ID_TO_NAME[PROP_ID_updateTime] = PROP_NAME_updateTime;
          PROP_NAME_TO_ID.put(PROP_NAME_updateTime, PROP_ID_updateTime);
      
          PROP_ID_TO_NAME[PROP_ID_territoryId] = PROP_NAME_territoryId;
          PROP_NAME_TO_ID.put(PROP_NAME_territoryId, PROP_ID_territoryId);
      
    }

    
    /* ID: ID */
    private java.lang.Long _id;
    
    /* 编码: CODE */
    private java.lang.String _code;
    
    /* 业务组织: ORG_ID */
    private java.lang.Long _orgId;
    
    /* 线索/商机类型: LEAD_TYPE */
    private java.lang.String _leadType;
    
    /* 客户: PARTNER_ID */
    private java.lang.Long _partnerId;
    
    /* 联系人姓名: CONTACT_NAME */
    private java.lang.String _contactName;
    
    /* 联系电话: CONTACT_PHONE */
    private java.lang.String _contactPhone;
    
    /* 联系邮箱: CONTACT_EMAIL */
    private java.lang.String _contactEmail;
    
    /* 公司名称: COMPANY_NAME */
    private java.lang.String _companyName;
    
    /* 职位: JOB_TITLE */
    private java.lang.String _jobTitle;
    
    /* 部门: DEPARTMENT */
    private java.lang.String _department;
    
    /* 线索来源: SOURCE_ID */
    private java.lang.Long _sourceId;
    
    /* 线索状态: LEAD_STATUS_ID */
    private java.lang.Long _leadStatusId;
    
    /* 漏斗阶段: STAGE_ID */
    private java.lang.Long _stageId;
    
    /* 预期收入: EXPECTED_REVENUE */
    private java.math.BigDecimal _expectedRevenue;
    
    /* 乐观预期: BEST_CASE_AMOUNT */
    private java.math.BigDecimal _bestCaseAmount;
    
    /* 悲观预期: WORST_CASE_AMOUNT */
    private java.math.BigDecimal _worstCaseAmount;
    
    /* 周期性收入: RECURRING_REVENUE */
    private java.math.BigDecimal _recurringRevenue;
    
    /* 周期性计划: RECURRING_PLAN */
    private java.lang.String _recurringPlan;
    
    /* 预期签单日: EXPECTED_CLOSE_DATE */
    private java.time.LocalDate _expectedCloseDate;
    
    /* 成交概率: PROBABILITY */
    private java.lang.Integer _probability;
    
    /* 营销活动: CAMPAIGN_ID */
    private java.lang.Long _campaignId;
    
    /* UTM Medium: UTM_MEDIUM */
    private java.lang.String _utmMedium;
    
    /* UTM Source: UTM_SOURCE */
    private java.lang.String _utmSource;
    
    /* 负责人: OWNER_ID */
    private java.lang.String _ownerId;
    
    /* 销售团队: TEAM_ID */
    private java.lang.Long _teamId;
    
    /* 丢单原因: LOST_REASON_ID */
    private java.lang.Long _lostReasonId;
    
    /* 丢单描述: LOST_REASON_DESC */
    private java.lang.String _lostReasonDesc;
    
    /* 最后联系日期: LAST_CONTACT_DATE */
    private java.time.LocalDateTime _lastContactDate;
    
    /* 下次活动日期: NEXT_ACTIVITY_DATE */
    private java.time.LocalDateTime _nextActivityDate;
    
    /* 转化结果单据类型: RELATED_BILL_TYPE */
    private java.lang.String _relatedBillType;
    
    /* 转化结果单据号: RELATED_BILL_CODE */
    private java.lang.String _relatedBillCode;
    
    /* 单据状态: DOC_STATUS */
    private java.lang.String _docStatus;
    
    /* 备注: REMARK */
    private java.lang.String _remark;
    
    /* 逻辑删除版本: DEL_VERSION */
    private java.lang.Long _delVersion;
    
    /* 数据版本: VERSION */
    private java.lang.Integer _version;
    
    /* 创建人: CREATED_BY */
    private java.lang.String _createdBy;
    
    /* 创建时间: CREATE_TIME */
    private java.sql.Timestamp _createTime;
    
    /* 修改人: UPDATED_BY */
    private java.lang.String _updatedBy;
    
    /* 修改时间: UPDATE_TIME */
    private java.sql.Timestamp _updateTime;
    
    /* 销售区域: TERRITORY_ID */
    private java.lang.Long _territoryId;
    

    public _ErpCrmLead(){
        // for debug
    }

    protected ErpCrmLead newInstance(){
        ErpCrmLead entity = new ErpCrmLead();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpCrmLead cloneInstance() {
        ErpCrmLead entity = newInstance();
        orm_forEachInitedProp((value, propId) -> {
            entity.orm_propValue(propId,value);
        });
        return entity;
    }

    @Override
    public String orm_entityName() {
      // 如果存在实体模型对象，则以模型对象上的设置为准
      IEntityModel entityModel = orm_entityModel();
      if(entityModel != null)
          return entityModel.getName();
      return "app.erp.crm.dao.entity.ErpCrmLead";
    }

    @Override
    public int orm_propIdBound(){
      IEntityModel entityModel = orm_entityModel();
      if(entityModel != null)
          return entityModel.getPropIdBound();
      return _PROP_ID_BOUND;
    }

    @Override
    public Object orm_id() {
    
        return buildSimpleId(PROP_ID_id);
     
    }

    @Override
    public boolean orm_isPrimary(int propId) {
        
            return propId == PROP_ID_id;
          
    }

    @Override
    public String orm_propName(int propId) {
        if(propId >= PROP_ID_TO_NAME.length)
            return super.orm_propName(propId);
        String propName = PROP_ID_TO_NAME[propId];
        if(propName == null)
           return super.orm_propName(propId);
        return propName;
    }

    @Override
    public int orm_propId(String propName) {
        Integer propId = PROP_NAME_TO_ID.get(propName);
        if(propId == null)
            return super.orm_propId(propName);
        return propId;
    }

    @Override
    public Object orm_propValue(int propId) {
        switch(propId){
        
            case PROP_ID_id:
               return getId();
        
            case PROP_ID_code:
               return getCode();
        
            case PROP_ID_orgId:
               return getOrgId();
        
            case PROP_ID_leadType:
               return getLeadType();
        
            case PROP_ID_partnerId:
               return getPartnerId();
        
            case PROP_ID_contactName:
               return getContactName();
        
            case PROP_ID_contactPhone:
               return getContactPhone();
        
            case PROP_ID_contactEmail:
               return getContactEmail();
        
            case PROP_ID_companyName:
               return getCompanyName();
        
            case PROP_ID_jobTitle:
               return getJobTitle();
        
            case PROP_ID_department:
               return getDepartment();
        
            case PROP_ID_sourceId:
               return getSourceId();
        
            case PROP_ID_leadStatusId:
               return getLeadStatusId();
        
            case PROP_ID_stageId:
               return getStageId();
        
            case PROP_ID_expectedRevenue:
               return getExpectedRevenue();
        
            case PROP_ID_bestCaseAmount:
               return getBestCaseAmount();
        
            case PROP_ID_worstCaseAmount:
               return getWorstCaseAmount();
        
            case PROP_ID_recurringRevenue:
               return getRecurringRevenue();
        
            case PROP_ID_recurringPlan:
               return getRecurringPlan();
        
            case PROP_ID_expectedCloseDate:
               return getExpectedCloseDate();
        
            case PROP_ID_probability:
               return getProbability();
        
            case PROP_ID_campaignId:
               return getCampaignId();
        
            case PROP_ID_utmMedium:
               return getUtmMedium();
        
            case PROP_ID_utmSource:
               return getUtmSource();
        
            case PROP_ID_ownerId:
               return getOwnerId();
        
            case PROP_ID_teamId:
               return getTeamId();
        
            case PROP_ID_lostReasonId:
               return getLostReasonId();
        
            case PROP_ID_lostReasonDesc:
               return getLostReasonDesc();
        
            case PROP_ID_lastContactDate:
               return getLastContactDate();
        
            case PROP_ID_nextActivityDate:
               return getNextActivityDate();
        
            case PROP_ID_relatedBillType:
               return getRelatedBillType();
        
            case PROP_ID_relatedBillCode:
               return getRelatedBillCode();
        
            case PROP_ID_docStatus:
               return getDocStatus();
        
            case PROP_ID_remark:
               return getRemark();
        
            case PROP_ID_delVersion:
               return getDelVersion();
        
            case PROP_ID_version:
               return getVersion();
        
            case PROP_ID_createdBy:
               return getCreatedBy();
        
            case PROP_ID_createTime:
               return getCreateTime();
        
            case PROP_ID_updatedBy:
               return getUpdatedBy();
        
            case PROP_ID_updateTime:
               return getUpdateTime();
        
            case PROP_ID_territoryId:
               return getTerritoryId();
        
           default:
              return super.orm_propValue(propId);
        }
    }

    

    @Override
    public void orm_propValue(int propId, Object value){
        switch(propId){
        
            case PROP_ID_id:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_id));
               }
               setId(typedValue);
               break;
            }
        
            case PROP_ID_code:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_code));
               }
               setCode(typedValue);
               break;
            }
        
            case PROP_ID_orgId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_orgId));
               }
               setOrgId(typedValue);
               break;
            }
        
            case PROP_ID_leadType:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_leadType));
               }
               setLeadType(typedValue);
               break;
            }
        
            case PROP_ID_partnerId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_partnerId));
               }
               setPartnerId(typedValue);
               break;
            }
        
            case PROP_ID_contactName:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_contactName));
               }
               setContactName(typedValue);
               break;
            }
        
            case PROP_ID_contactPhone:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_contactPhone));
               }
               setContactPhone(typedValue);
               break;
            }
        
            case PROP_ID_contactEmail:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_contactEmail));
               }
               setContactEmail(typedValue);
               break;
            }
        
            case PROP_ID_companyName:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_companyName));
               }
               setCompanyName(typedValue);
               break;
            }
        
            case PROP_ID_jobTitle:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_jobTitle));
               }
               setJobTitle(typedValue);
               break;
            }
        
            case PROP_ID_department:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_department));
               }
               setDepartment(typedValue);
               break;
            }
        
            case PROP_ID_sourceId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_sourceId));
               }
               setSourceId(typedValue);
               break;
            }
        
            case PROP_ID_leadStatusId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_leadStatusId));
               }
               setLeadStatusId(typedValue);
               break;
            }
        
            case PROP_ID_stageId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_stageId));
               }
               setStageId(typedValue);
               break;
            }
        
            case PROP_ID_expectedRevenue:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_expectedRevenue));
               }
               setExpectedRevenue(typedValue);
               break;
            }
        
            case PROP_ID_bestCaseAmount:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_bestCaseAmount));
               }
               setBestCaseAmount(typedValue);
               break;
            }
        
            case PROP_ID_worstCaseAmount:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_worstCaseAmount));
               }
               setWorstCaseAmount(typedValue);
               break;
            }
        
            case PROP_ID_recurringRevenue:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_recurringRevenue));
               }
               setRecurringRevenue(typedValue);
               break;
            }
        
            case PROP_ID_recurringPlan:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_recurringPlan));
               }
               setRecurringPlan(typedValue);
               break;
            }
        
            case PROP_ID_expectedCloseDate:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_expectedCloseDate));
               }
               setExpectedCloseDate(typedValue);
               break;
            }
        
            case PROP_ID_probability:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_probability));
               }
               setProbability(typedValue);
               break;
            }
        
            case PROP_ID_campaignId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_campaignId));
               }
               setCampaignId(typedValue);
               break;
            }
        
            case PROP_ID_utmMedium:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_utmMedium));
               }
               setUtmMedium(typedValue);
               break;
            }
        
            case PROP_ID_utmSource:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_utmSource));
               }
               setUtmSource(typedValue);
               break;
            }
        
            case PROP_ID_ownerId:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_ownerId));
               }
               setOwnerId(typedValue);
               break;
            }
        
            case PROP_ID_teamId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_teamId));
               }
               setTeamId(typedValue);
               break;
            }
        
            case PROP_ID_lostReasonId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_lostReasonId));
               }
               setLostReasonId(typedValue);
               break;
            }
        
            case PROP_ID_lostReasonDesc:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_lostReasonDesc));
               }
               setLostReasonDesc(typedValue);
               break;
            }
        
            case PROP_ID_lastContactDate:{
               java.time.LocalDateTime typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDateTime(value,
                       err-> newTypeConversionError(PROP_NAME_lastContactDate));
               }
               setLastContactDate(typedValue);
               break;
            }
        
            case PROP_ID_nextActivityDate:{
               java.time.LocalDateTime typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDateTime(value,
                       err-> newTypeConversionError(PROP_NAME_nextActivityDate));
               }
               setNextActivityDate(typedValue);
               break;
            }
        
            case PROP_ID_relatedBillType:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_relatedBillType));
               }
               setRelatedBillType(typedValue);
               break;
            }
        
            case PROP_ID_relatedBillCode:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_relatedBillCode));
               }
               setRelatedBillCode(typedValue);
               break;
            }
        
            case PROP_ID_docStatus:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_docStatus));
               }
               setDocStatus(typedValue);
               break;
            }
        
            case PROP_ID_remark:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_remark));
               }
               setRemark(typedValue);
               break;
            }
        
            case PROP_ID_delVersion:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_delVersion));
               }
               setDelVersion(typedValue);
               break;
            }
        
            case PROP_ID_version:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_version));
               }
               setVersion(typedValue);
               break;
            }
        
            case PROP_ID_createdBy:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_createdBy));
               }
               setCreatedBy(typedValue);
               break;
            }
        
            case PROP_ID_createTime:{
               java.sql.Timestamp typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toTimestamp(value,
                       err-> newTypeConversionError(PROP_NAME_createTime));
               }
               setCreateTime(typedValue);
               break;
            }
        
            case PROP_ID_updatedBy:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_updatedBy));
               }
               setUpdatedBy(typedValue);
               break;
            }
        
            case PROP_ID_updateTime:{
               java.sql.Timestamp typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toTimestamp(value,
                       err-> newTypeConversionError(PROP_NAME_updateTime));
               }
               setUpdateTime(typedValue);
               break;
            }
        
            case PROP_ID_territoryId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_territoryId));
               }
               setTerritoryId(typedValue);
               break;
            }
        
           default:
              super.orm_propValue(propId,value);
        }
    }

    @Override
    public void orm_internalSet(int propId, Object value) {
        switch(propId){
        
            case PROP_ID_id:{
               onInitProp(propId);
               this._id = (java.lang.Long)value;
               orm_id(); // 如果是设置主键字段，则触发watcher
               break;
            }
        
            case PROP_ID_code:{
               onInitProp(propId);
               this._code = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_orgId:{
               onInitProp(propId);
               this._orgId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_leadType:{
               onInitProp(propId);
               this._leadType = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_partnerId:{
               onInitProp(propId);
               this._partnerId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_contactName:{
               onInitProp(propId);
               this._contactName = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_contactPhone:{
               onInitProp(propId);
               this._contactPhone = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_contactEmail:{
               onInitProp(propId);
               this._contactEmail = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_companyName:{
               onInitProp(propId);
               this._companyName = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_jobTitle:{
               onInitProp(propId);
               this._jobTitle = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_department:{
               onInitProp(propId);
               this._department = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_sourceId:{
               onInitProp(propId);
               this._sourceId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_leadStatusId:{
               onInitProp(propId);
               this._leadStatusId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_stageId:{
               onInitProp(propId);
               this._stageId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_expectedRevenue:{
               onInitProp(propId);
               this._expectedRevenue = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_bestCaseAmount:{
               onInitProp(propId);
               this._bestCaseAmount = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_worstCaseAmount:{
               onInitProp(propId);
               this._worstCaseAmount = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_recurringRevenue:{
               onInitProp(propId);
               this._recurringRevenue = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_recurringPlan:{
               onInitProp(propId);
               this._recurringPlan = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_expectedCloseDate:{
               onInitProp(propId);
               this._expectedCloseDate = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_probability:{
               onInitProp(propId);
               this._probability = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_campaignId:{
               onInitProp(propId);
               this._campaignId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_utmMedium:{
               onInitProp(propId);
               this._utmMedium = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_utmSource:{
               onInitProp(propId);
               this._utmSource = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_ownerId:{
               onInitProp(propId);
               this._ownerId = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_teamId:{
               onInitProp(propId);
               this._teamId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_lostReasonId:{
               onInitProp(propId);
               this._lostReasonId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_lostReasonDesc:{
               onInitProp(propId);
               this._lostReasonDesc = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_lastContactDate:{
               onInitProp(propId);
               this._lastContactDate = (java.time.LocalDateTime)value;
               
               break;
            }
        
            case PROP_ID_nextActivityDate:{
               onInitProp(propId);
               this._nextActivityDate = (java.time.LocalDateTime)value;
               
               break;
            }
        
            case PROP_ID_relatedBillType:{
               onInitProp(propId);
               this._relatedBillType = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_relatedBillCode:{
               onInitProp(propId);
               this._relatedBillCode = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_docStatus:{
               onInitProp(propId);
               this._docStatus = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_remark:{
               onInitProp(propId);
               this._remark = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_delVersion:{
               onInitProp(propId);
               this._delVersion = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_version:{
               onInitProp(propId);
               this._version = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_createdBy:{
               onInitProp(propId);
               this._createdBy = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_createTime:{
               onInitProp(propId);
               this._createTime = (java.sql.Timestamp)value;
               
               break;
            }
        
            case PROP_ID_updatedBy:{
               onInitProp(propId);
               this._updatedBy = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_updateTime:{
               onInitProp(propId);
               this._updateTime = (java.sql.Timestamp)value;
               
               break;
            }
        
            case PROP_ID_territoryId:{
               onInitProp(propId);
               this._territoryId = (java.lang.Long)value;
               
               break;
            }
        
           default:
              super.orm_internalSet(propId,value);
        }
    }

    
    /**
     * ID: ID
     */
    public final java.lang.Long getId(){
         onPropGet(PROP_ID_id);
         return _id;
    }

    /**
     * ID: ID
     */
    public final void setId(java.lang.Long value){
        if(onPropSet(PROP_ID_id,value)){
            this._id = value;
            internalClearRefs(PROP_ID_id);
            orm_id();
        }
    }
    
    /**
     * 编码: CODE
     */
    public final java.lang.String getCode(){
         onPropGet(PROP_ID_code);
         return _code;
    }

    /**
     * 编码: CODE
     */
    public final void setCode(java.lang.String value){
        if(onPropSet(PROP_ID_code,value)){
            this._code = value;
            internalClearRefs(PROP_ID_code);
            
        }
    }
    
    /**
     * 业务组织: ORG_ID
     */
    public final java.lang.Long getOrgId(){
         onPropGet(PROP_ID_orgId);
         return _orgId;
    }

    /**
     * 业务组织: ORG_ID
     */
    public final void setOrgId(java.lang.Long value){
        if(onPropSet(PROP_ID_orgId,value)){
            this._orgId = value;
            internalClearRefs(PROP_ID_orgId);
            
        }
    }
    
    /**
     * 线索/商机类型: LEAD_TYPE
     */
    public final java.lang.String getLeadType(){
         onPropGet(PROP_ID_leadType);
         return _leadType;
    }

    /**
     * 线索/商机类型: LEAD_TYPE
     */
    public final void setLeadType(java.lang.String value){
        if(onPropSet(PROP_ID_leadType,value)){
            this._leadType = value;
            internalClearRefs(PROP_ID_leadType);
            
        }
    }
    
    /**
     * 客户: PARTNER_ID
     */
    public final java.lang.Long getPartnerId(){
         onPropGet(PROP_ID_partnerId);
         return _partnerId;
    }

    /**
     * 客户: PARTNER_ID
     */
    public final void setPartnerId(java.lang.Long value){
        if(onPropSet(PROP_ID_partnerId,value)){
            this._partnerId = value;
            internalClearRefs(PROP_ID_partnerId);
            
        }
    }
    
    /**
     * 联系人姓名: CONTACT_NAME
     */
    public final java.lang.String getContactName(){
         onPropGet(PROP_ID_contactName);
         return _contactName;
    }

    /**
     * 联系人姓名: CONTACT_NAME
     */
    public final void setContactName(java.lang.String value){
        if(onPropSet(PROP_ID_contactName,value)){
            this._contactName = value;
            internalClearRefs(PROP_ID_contactName);
            
        }
    }
    
    /**
     * 联系电话: CONTACT_PHONE
     */
    public final java.lang.String getContactPhone(){
         onPropGet(PROP_ID_contactPhone);
         return _contactPhone;
    }

    /**
     * 联系电话: CONTACT_PHONE
     */
    public final void setContactPhone(java.lang.String value){
        if(onPropSet(PROP_ID_contactPhone,value)){
            this._contactPhone = value;
            internalClearRefs(PROP_ID_contactPhone);
            
        }
    }
    
    /**
     * 联系邮箱: CONTACT_EMAIL
     */
    public final java.lang.String getContactEmail(){
         onPropGet(PROP_ID_contactEmail);
         return _contactEmail;
    }

    /**
     * 联系邮箱: CONTACT_EMAIL
     */
    public final void setContactEmail(java.lang.String value){
        if(onPropSet(PROP_ID_contactEmail,value)){
            this._contactEmail = value;
            internalClearRefs(PROP_ID_contactEmail);
            
        }
    }
    
    /**
     * 公司名称: COMPANY_NAME
     */
    public final java.lang.String getCompanyName(){
         onPropGet(PROP_ID_companyName);
         return _companyName;
    }

    /**
     * 公司名称: COMPANY_NAME
     */
    public final void setCompanyName(java.lang.String value){
        if(onPropSet(PROP_ID_companyName,value)){
            this._companyName = value;
            internalClearRefs(PROP_ID_companyName);
            
        }
    }
    
    /**
     * 职位: JOB_TITLE
     */
    public final java.lang.String getJobTitle(){
         onPropGet(PROP_ID_jobTitle);
         return _jobTitle;
    }

    /**
     * 职位: JOB_TITLE
     */
    public final void setJobTitle(java.lang.String value){
        if(onPropSet(PROP_ID_jobTitle,value)){
            this._jobTitle = value;
            internalClearRefs(PROP_ID_jobTitle);
            
        }
    }
    
    /**
     * 部门: DEPARTMENT
     */
    public final java.lang.String getDepartment(){
         onPropGet(PROP_ID_department);
         return _department;
    }

    /**
     * 部门: DEPARTMENT
     */
    public final void setDepartment(java.lang.String value){
        if(onPropSet(PROP_ID_department,value)){
            this._department = value;
            internalClearRefs(PROP_ID_department);
            
        }
    }
    
    /**
     * 线索来源: SOURCE_ID
     */
    public final java.lang.Long getSourceId(){
         onPropGet(PROP_ID_sourceId);
         return _sourceId;
    }

    /**
     * 线索来源: SOURCE_ID
     */
    public final void setSourceId(java.lang.Long value){
        if(onPropSet(PROP_ID_sourceId,value)){
            this._sourceId = value;
            internalClearRefs(PROP_ID_sourceId);
            
        }
    }
    
    /**
     * 线索状态: LEAD_STATUS_ID
     */
    public final java.lang.Long getLeadStatusId(){
         onPropGet(PROP_ID_leadStatusId);
         return _leadStatusId;
    }

    /**
     * 线索状态: LEAD_STATUS_ID
     */
    public final void setLeadStatusId(java.lang.Long value){
        if(onPropSet(PROP_ID_leadStatusId,value)){
            this._leadStatusId = value;
            internalClearRefs(PROP_ID_leadStatusId);
            
        }
    }
    
    /**
     * 漏斗阶段: STAGE_ID
     */
    public final java.lang.Long getStageId(){
         onPropGet(PROP_ID_stageId);
         return _stageId;
    }

    /**
     * 漏斗阶段: STAGE_ID
     */
    public final void setStageId(java.lang.Long value){
        if(onPropSet(PROP_ID_stageId,value)){
            this._stageId = value;
            internalClearRefs(PROP_ID_stageId);
            
        }
    }
    
    /**
     * 预期收入: EXPECTED_REVENUE
     */
    public final java.math.BigDecimal getExpectedRevenue(){
         onPropGet(PROP_ID_expectedRevenue);
         return _expectedRevenue;
    }

    /**
     * 预期收入: EXPECTED_REVENUE
     */
    public final void setExpectedRevenue(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_expectedRevenue,value)){
            this._expectedRevenue = value;
            internalClearRefs(PROP_ID_expectedRevenue);
            
        }
    }
    
    /**
     * 乐观预期: BEST_CASE_AMOUNT
     */
    public final java.math.BigDecimal getBestCaseAmount(){
         onPropGet(PROP_ID_bestCaseAmount);
         return _bestCaseAmount;
    }

    /**
     * 乐观预期: BEST_CASE_AMOUNT
     */
    public final void setBestCaseAmount(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_bestCaseAmount,value)){
            this._bestCaseAmount = value;
            internalClearRefs(PROP_ID_bestCaseAmount);
            
        }
    }
    
    /**
     * 悲观预期: WORST_CASE_AMOUNT
     */
    public final java.math.BigDecimal getWorstCaseAmount(){
         onPropGet(PROP_ID_worstCaseAmount);
         return _worstCaseAmount;
    }

    /**
     * 悲观预期: WORST_CASE_AMOUNT
     */
    public final void setWorstCaseAmount(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_worstCaseAmount,value)){
            this._worstCaseAmount = value;
            internalClearRefs(PROP_ID_worstCaseAmount);
            
        }
    }
    
    /**
     * 周期性收入: RECURRING_REVENUE
     */
    public final java.math.BigDecimal getRecurringRevenue(){
         onPropGet(PROP_ID_recurringRevenue);
         return _recurringRevenue;
    }

    /**
     * 周期性收入: RECURRING_REVENUE
     */
    public final void setRecurringRevenue(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_recurringRevenue,value)){
            this._recurringRevenue = value;
            internalClearRefs(PROP_ID_recurringRevenue);
            
        }
    }
    
    /**
     * 周期性计划: RECURRING_PLAN
     */
    public final java.lang.String getRecurringPlan(){
         onPropGet(PROP_ID_recurringPlan);
         return _recurringPlan;
    }

    /**
     * 周期性计划: RECURRING_PLAN
     */
    public final void setRecurringPlan(java.lang.String value){
        if(onPropSet(PROP_ID_recurringPlan,value)){
            this._recurringPlan = value;
            internalClearRefs(PROP_ID_recurringPlan);
            
        }
    }
    
    /**
     * 预期签单日: EXPECTED_CLOSE_DATE
     */
    public final java.time.LocalDate getExpectedCloseDate(){
         onPropGet(PROP_ID_expectedCloseDate);
         return _expectedCloseDate;
    }

    /**
     * 预期签单日: EXPECTED_CLOSE_DATE
     */
    public final void setExpectedCloseDate(java.time.LocalDate value){
        if(onPropSet(PROP_ID_expectedCloseDate,value)){
            this._expectedCloseDate = value;
            internalClearRefs(PROP_ID_expectedCloseDate);
            
        }
    }
    
    /**
     * 成交概率: PROBABILITY
     */
    public final java.lang.Integer getProbability(){
         onPropGet(PROP_ID_probability);
         return _probability;
    }

    /**
     * 成交概率: PROBABILITY
     */
    public final void setProbability(java.lang.Integer value){
        if(onPropSet(PROP_ID_probability,value)){
            this._probability = value;
            internalClearRefs(PROP_ID_probability);
            
        }
    }
    
    /**
     * 营销活动: CAMPAIGN_ID
     */
    public final java.lang.Long getCampaignId(){
         onPropGet(PROP_ID_campaignId);
         return _campaignId;
    }

    /**
     * 营销活动: CAMPAIGN_ID
     */
    public final void setCampaignId(java.lang.Long value){
        if(onPropSet(PROP_ID_campaignId,value)){
            this._campaignId = value;
            internalClearRefs(PROP_ID_campaignId);
            
        }
    }
    
    /**
     * UTM Medium: UTM_MEDIUM
     */
    public final java.lang.String getUtmMedium(){
         onPropGet(PROP_ID_utmMedium);
         return _utmMedium;
    }

    /**
     * UTM Medium: UTM_MEDIUM
     */
    public final void setUtmMedium(java.lang.String value){
        if(onPropSet(PROP_ID_utmMedium,value)){
            this._utmMedium = value;
            internalClearRefs(PROP_ID_utmMedium);
            
        }
    }
    
    /**
     * UTM Source: UTM_SOURCE
     */
    public final java.lang.String getUtmSource(){
         onPropGet(PROP_ID_utmSource);
         return _utmSource;
    }

    /**
     * UTM Source: UTM_SOURCE
     */
    public final void setUtmSource(java.lang.String value){
        if(onPropSet(PROP_ID_utmSource,value)){
            this._utmSource = value;
            internalClearRefs(PROP_ID_utmSource);
            
        }
    }
    
    /**
     * 负责人: OWNER_ID
     */
    public final java.lang.String getOwnerId(){
         onPropGet(PROP_ID_ownerId);
         return _ownerId;
    }

    /**
     * 负责人: OWNER_ID
     */
    public final void setOwnerId(java.lang.String value){
        if(onPropSet(PROP_ID_ownerId,value)){
            this._ownerId = value;
            internalClearRefs(PROP_ID_ownerId);
            
        }
    }
    
    /**
     * 销售团队: TEAM_ID
     */
    public final java.lang.Long getTeamId(){
         onPropGet(PROP_ID_teamId);
         return _teamId;
    }

    /**
     * 销售团队: TEAM_ID
     */
    public final void setTeamId(java.lang.Long value){
        if(onPropSet(PROP_ID_teamId,value)){
            this._teamId = value;
            internalClearRefs(PROP_ID_teamId);
            
        }
    }
    
    /**
     * 丢单原因: LOST_REASON_ID
     */
    public final java.lang.Long getLostReasonId(){
         onPropGet(PROP_ID_lostReasonId);
         return _lostReasonId;
    }

    /**
     * 丢单原因: LOST_REASON_ID
     */
    public final void setLostReasonId(java.lang.Long value){
        if(onPropSet(PROP_ID_lostReasonId,value)){
            this._lostReasonId = value;
            internalClearRefs(PROP_ID_lostReasonId);
            
        }
    }
    
    /**
     * 丢单描述: LOST_REASON_DESC
     */
    public final java.lang.String getLostReasonDesc(){
         onPropGet(PROP_ID_lostReasonDesc);
         return _lostReasonDesc;
    }

    /**
     * 丢单描述: LOST_REASON_DESC
     */
    public final void setLostReasonDesc(java.lang.String value){
        if(onPropSet(PROP_ID_lostReasonDesc,value)){
            this._lostReasonDesc = value;
            internalClearRefs(PROP_ID_lostReasonDesc);
            
        }
    }
    
    /**
     * 最后联系日期: LAST_CONTACT_DATE
     */
    public final java.time.LocalDateTime getLastContactDate(){
         onPropGet(PROP_ID_lastContactDate);
         return _lastContactDate;
    }

    /**
     * 最后联系日期: LAST_CONTACT_DATE
     */
    public final void setLastContactDate(java.time.LocalDateTime value){
        if(onPropSet(PROP_ID_lastContactDate,value)){
            this._lastContactDate = value;
            internalClearRefs(PROP_ID_lastContactDate);
            
        }
    }
    
    /**
     * 下次活动日期: NEXT_ACTIVITY_DATE
     */
    public final java.time.LocalDateTime getNextActivityDate(){
         onPropGet(PROP_ID_nextActivityDate);
         return _nextActivityDate;
    }

    /**
     * 下次活动日期: NEXT_ACTIVITY_DATE
     */
    public final void setNextActivityDate(java.time.LocalDateTime value){
        if(onPropSet(PROP_ID_nextActivityDate,value)){
            this._nextActivityDate = value;
            internalClearRefs(PROP_ID_nextActivityDate);
            
        }
    }
    
    /**
     * 转化结果单据类型: RELATED_BILL_TYPE
     */
    public final java.lang.String getRelatedBillType(){
         onPropGet(PROP_ID_relatedBillType);
         return _relatedBillType;
    }

    /**
     * 转化结果单据类型: RELATED_BILL_TYPE
     */
    public final void setRelatedBillType(java.lang.String value){
        if(onPropSet(PROP_ID_relatedBillType,value)){
            this._relatedBillType = value;
            internalClearRefs(PROP_ID_relatedBillType);
            
        }
    }
    
    /**
     * 转化结果单据号: RELATED_BILL_CODE
     */
    public final java.lang.String getRelatedBillCode(){
         onPropGet(PROP_ID_relatedBillCode);
         return _relatedBillCode;
    }

    /**
     * 转化结果单据号: RELATED_BILL_CODE
     */
    public final void setRelatedBillCode(java.lang.String value){
        if(onPropSet(PROP_ID_relatedBillCode,value)){
            this._relatedBillCode = value;
            internalClearRefs(PROP_ID_relatedBillCode);
            
        }
    }
    
    /**
     * 单据状态: DOC_STATUS
     */
    public final java.lang.String getDocStatus(){
         onPropGet(PROP_ID_docStatus);
         return _docStatus;
    }

    /**
     * 单据状态: DOC_STATUS
     */
    public final void setDocStatus(java.lang.String value){
        if(onPropSet(PROP_ID_docStatus,value)){
            this._docStatus = value;
            internalClearRefs(PROP_ID_docStatus);
            
        }
    }
    
    /**
     * 备注: REMARK
     */
    public final java.lang.String getRemark(){
         onPropGet(PROP_ID_remark);
         return _remark;
    }

    /**
     * 备注: REMARK
     */
    public final void setRemark(java.lang.String value){
        if(onPropSet(PROP_ID_remark,value)){
            this._remark = value;
            internalClearRefs(PROP_ID_remark);
            
        }
    }
    
    /**
     * 逻辑删除版本: DEL_VERSION
     */
    public final java.lang.Long getDelVersion(){
         onPropGet(PROP_ID_delVersion);
         return _delVersion;
    }

    /**
     * 逻辑删除版本: DEL_VERSION
     */
    public final void setDelVersion(java.lang.Long value){
        if(onPropSet(PROP_ID_delVersion,value)){
            this._delVersion = value;
            internalClearRefs(PROP_ID_delVersion);
            
        }
    }
    
    /**
     * 数据版本: VERSION
     */
    public final java.lang.Integer getVersion(){
         onPropGet(PROP_ID_version);
         return _version;
    }

    /**
     * 数据版本: VERSION
     */
    public final void setVersion(java.lang.Integer value){
        if(onPropSet(PROP_ID_version,value)){
            this._version = value;
            internalClearRefs(PROP_ID_version);
            
        }
    }
    
    /**
     * 创建人: CREATED_BY
     */
    public final java.lang.String getCreatedBy(){
         onPropGet(PROP_ID_createdBy);
         return _createdBy;
    }

    /**
     * 创建人: CREATED_BY
     */
    public final void setCreatedBy(java.lang.String value){
        if(onPropSet(PROP_ID_createdBy,value)){
            this._createdBy = value;
            internalClearRefs(PROP_ID_createdBy);
            
        }
    }
    
    /**
     * 创建时间: CREATE_TIME
     */
    public final java.sql.Timestamp getCreateTime(){
         onPropGet(PROP_ID_createTime);
         return _createTime;
    }

    /**
     * 创建时间: CREATE_TIME
     */
    public final void setCreateTime(java.sql.Timestamp value){
        if(onPropSet(PROP_ID_createTime,value)){
            this._createTime = value;
            internalClearRefs(PROP_ID_createTime);
            
        }
    }
    
    /**
     * 修改人: UPDATED_BY
     */
    public final java.lang.String getUpdatedBy(){
         onPropGet(PROP_ID_updatedBy);
         return _updatedBy;
    }

    /**
     * 修改人: UPDATED_BY
     */
    public final void setUpdatedBy(java.lang.String value){
        if(onPropSet(PROP_ID_updatedBy,value)){
            this._updatedBy = value;
            internalClearRefs(PROP_ID_updatedBy);
            
        }
    }
    
    /**
     * 修改时间: UPDATE_TIME
     */
    public final java.sql.Timestamp getUpdateTime(){
         onPropGet(PROP_ID_updateTime);
         return _updateTime;
    }

    /**
     * 修改时间: UPDATE_TIME
     */
    public final void setUpdateTime(java.sql.Timestamp value){
        if(onPropSet(PROP_ID_updateTime,value)){
            this._updateTime = value;
            internalClearRefs(PROP_ID_updateTime);
            
        }
    }
    
    /**
     * 销售区域: TERRITORY_ID
     */
    public final java.lang.Long getTerritoryId(){
         onPropGet(PROP_ID_territoryId);
         return _territoryId;
    }

    /**
     * 销售区域: TERRITORY_ID
     */
    public final void setTerritoryId(java.lang.Long value){
        if(onPropSet(PROP_ID_territoryId,value)){
            this._territoryId = value;
            internalClearRefs(PROP_ID_territoryId);
            
        }
    }
    
    /**
     * 
     */
    public final app.erp.md.dao.entity.ErpMdPartner getPartner(){
       return (app.erp.md.dao.entity.ErpMdPartner)internalGetRefEntity(PROP_NAME_partner);
    }

    public final void setPartner(app.erp.md.dao.entity.ErpMdPartner refEntity){
   
           if(refEntity == null){
           
                   this.setPartnerId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_partner, refEntity,()->{
           
                           this.setPartnerId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.crm.dao.entity.ErpCrmSource getSource(){
       return (app.erp.crm.dao.entity.ErpCrmSource)internalGetRefEntity(PROP_NAME_source);
    }

    public final void setSource(app.erp.crm.dao.entity.ErpCrmSource refEntity){
   
           if(refEntity == null){
           
                   this.setSourceId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_source, refEntity,()->{
           
                           this.setSourceId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.crm.dao.entity.ErpCrmLeadStatus getLeadStatus(){
       return (app.erp.crm.dao.entity.ErpCrmLeadStatus)internalGetRefEntity(PROP_NAME_leadStatus);
    }

    public final void setLeadStatus(app.erp.crm.dao.entity.ErpCrmLeadStatus refEntity){
   
           if(refEntity == null){
           
                   this.setLeadStatusId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_leadStatus, refEntity,()->{
           
                           this.setLeadStatusId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.crm.dao.entity.ErpCrmStage getStage(){
       return (app.erp.crm.dao.entity.ErpCrmStage)internalGetRefEntity(PROP_NAME_stage);
    }

    public final void setStage(app.erp.crm.dao.entity.ErpCrmStage refEntity){
   
           if(refEntity == null){
           
                   this.setStageId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_stage, refEntity,()->{
           
                           this.setStageId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.crm.dao.entity.ErpCrmCampaign getCampaign(){
       return (app.erp.crm.dao.entity.ErpCrmCampaign)internalGetRefEntity(PROP_NAME_campaign);
    }

    public final void setCampaign(app.erp.crm.dao.entity.ErpCrmCampaign refEntity){
   
           if(refEntity == null){
           
                   this.setCampaignId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_campaign, refEntity,()->{
           
                           this.setCampaignId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.crm.dao.entity.ErpCrmTeam getTeam(){
       return (app.erp.crm.dao.entity.ErpCrmTeam)internalGetRefEntity(PROP_NAME_team);
    }

    public final void setTeam(app.erp.crm.dao.entity.ErpCrmTeam refEntity){
   
           if(refEntity == null){
           
                   this.setTeamId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_team, refEntity,()->{
           
                           this.setTeamId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.crm.dao.entity.ErpCrmTerritory getTerritory(){
       return (app.erp.crm.dao.entity.ErpCrmTerritory)internalGetRefEntity(PROP_NAME_territory);
    }

    public final void setTerritory(app.erp.crm.dao.entity.ErpCrmTerritory refEntity){
   
           if(refEntity == null){
           
                   this.setTerritoryId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_territory, refEntity,()->{
           
                           this.setTerritoryId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.crm.dao.entity.ErpCrmLostReason getLostReason(){
       return (app.erp.crm.dao.entity.ErpCrmLostReason)internalGetRefEntity(PROP_NAME_lostReason);
    }

    public final void setLostReason(app.erp.crm.dao.entity.ErpCrmLostReason refEntity){
   
           if(refEntity == null){
           
                   this.setLostReasonId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_lostReason, refEntity,()->{
           
                           this.setLostReasonId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.md.dao.entity.ErpMdOrganization getOrg(){
       return (app.erp.md.dao.entity.ErpMdOrganization)internalGetRefEntity(PROP_NAME_org);
    }

    public final void setOrg(app.erp.md.dao.entity.ErpMdOrganization refEntity){
   
           if(refEntity == null){
           
                   this.setOrgId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_org, refEntity,()->{
           
                           this.setOrgId(refEntity.getId());
                       
           });
           }
       
    }
       
    private final OrmEntitySet<app.erp.crm.dao.entity.ErpCrmEvent> _events = new OrmEntitySet<>(this, PROP_NAME_events,
        null, null,app.erp.crm.dao.entity.ErpCrmEvent.class);

    /**
     * 。 refPropName: , keyProp: {rel.keyProp}
     */
    public final IOrmEntitySet<app.erp.crm.dao.entity.ErpCrmEvent> getEvents(){
       return _events;
    }
       
    private final OrmEntitySet<app.erp.crm.dao.entity.ErpCrmActivity> _activities = new OrmEntitySet<>(this, PROP_NAME_activities,
        null, null,app.erp.crm.dao.entity.ErpCrmActivity.class);

    /**
     * 。 refPropName: , keyProp: {rel.keyProp}
     */
    public final IOrmEntitySet<app.erp.crm.dao.entity.ErpCrmActivity> getActivities(){
       return _activities;
    }
       
    private final OrmEntitySet<app.erp.crm.dao.entity.ErpCrmLeadConvLog> _convLogs = new OrmEntitySet<>(this, PROP_NAME_convLogs,
        null, null,app.erp.crm.dao.entity.ErpCrmLeadConvLog.class);

    /**
     * 。 refPropName: , keyProp: {rel.keyProp}
     */
    public final IOrmEntitySet<app.erp.crm.dao.entity.ErpCrmLeadConvLog> getConvLogs(){
       return _convLogs;
    }
       
}
// resume CPD analysis - CPD-ON

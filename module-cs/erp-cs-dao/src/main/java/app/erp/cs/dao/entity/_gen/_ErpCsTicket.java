package app.erp.cs.dao.entity._gen;

import io.nop.orm.model.IEntityModel;
import io.nop.orm.support.DynamicOrmEntity;
import io.nop.orm.support.OrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code
import io.nop.orm.IOrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code

import io.nop.api.core.convert.ConvertHelper;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;

import app.erp.cs.dao.entity.ErpCsTicket;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  客服工单: erp_cs_ticket
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpCsTicket extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 单号: CODE VARCHAR */
    public static final String PROP_NAME_code = "code";
    public static final int PROP_ID_code = 2;
    
    /* 业务组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 3;
    
    /* 工单主题: SUBJECT VARCHAR */
    public static final String PROP_NAME_subject = "subject";
    public static final int PROP_ID_subject = 4;
    
    /* 问题描述: DESCRIPTION VARCHAR */
    public static final String PROP_NAME_description = "description";
    public static final int PROP_ID_description = 5;
    
    /* 客户: CUSTOMER_ID BIGINT */
    public static final String PROP_NAME_customerId = "customerId";
    public static final int PROP_ID_customerId = 6;
    
    /* 联系人: CONTACT_ID BIGINT */
    public static final String PROP_NAME_contactId = "contactId";
    public static final int PROP_ID_contactId = 7;
    
    /* 工单类型: TICKET_TYPE_ID BIGINT */
    public static final String PROP_NAME_ticketTypeId = "ticketTypeId";
    public static final int PROP_ID_ticketTypeId = 8;
    
    /* 优先级: PRIORITY VARCHAR */
    public static final String PROP_NAME_priority = "priority";
    public static final int PROP_ID_priority = 9;
    
    /* 来源: SOURCE VARCHAR */
    public static final String PROP_NAME_source = "source";
    public static final int PROP_ID_source = 10;
    
    /* 分配处理人: ASSIGNED_TO_ID VARCHAR */
    public static final String PROP_NAME_assignedToId = "assignedToId";
    public static final int PROP_ID_assignedToId = 11;
    
    /* SLA 策略: SLA_POLICY_ID BIGINT */
    public static final String PROP_NAME_slaPolicyId = "slaPolicyId";
    public static final int PROP_ID_slaPolicyId = 12;
    
    /* SLA 截止时间: DEADLINE_DATE_TIME DATETIME */
    public static final String PROP_NAME_deadlineDateTime = "deadlineDateTime";
    public static final int PROP_ID_deadlineDateTime = 13;
    
    /* SLA 是否完成: IS_SLA_COMPLETED BOOLEAN */
    public static final String PROP_NAME_isSlaCompleted = "isSlaCompleted";
    public static final int PROP_ID_isSlaCompleted = 14;
    
    /* 开始处理时间: START_DATE_TIME DATETIME */
    public static final String PROP_NAME_startDateTime = "startDateTime";
    public static final int PROP_ID_startDateTime = 15;
    
    /* 关闭时间: END_DATE_TIME DATETIME */
    public static final String PROP_NAME_endDateTime = "endDateTime";
    public static final int PROP_ID_endDateTime = 16;
    
    /* 处理时长(分钟): DURATION INTEGER */
    public static final String PROP_NAME_duration = "duration";
    public static final int PROP_ID_duration = 17;
    
    /* 进度(%): PROGRESS INTEGER */
    public static final String PROP_NAME_progress = "progress";
    public static final int PROP_ID_progress = 18;
    
    /* 工单状态: STATUS VARCHAR */
    public static final String PROP_NAME_status = "status";
    public static final int PROP_ID_status = 19;
    
    /* 单据状态: DOC_STATUS VARCHAR */
    public static final String PROP_NAME_docStatus = "docStatus";
    public static final int PROP_ID_docStatus = 20;
    
    /* 审核状态: APPROVE_STATUS VARCHAR */
    public static final String PROP_NAME_approveStatus = "approveStatus";
    public static final int PROP_ID_approveStatus = 21;
    
    /* 备注: REMARK VARCHAR */
    public static final String PROP_NAME_remark = "remark";
    public static final int PROP_ID_remark = 22;
    
    /* 服务目录项: CATALOG_ITEM_ID BIGINT */
    public static final String PROP_NAME_catalogItemId = "catalogItemId";
    public static final int PROP_ID_catalogItemId = 23;
    
    /* 逻辑删除版本: DEL_VERSION BIGINT */
    public static final String PROP_NAME_delVersion = "delVersion";
    public static final int PROP_ID_delVersion = 24;
    
    /* 数据版本: VERSION INTEGER */
    public static final String PROP_NAME_version = "version";
    public static final int PROP_ID_version = 25;
    
    /* 创建人: CREATED_BY VARCHAR */
    public static final String PROP_NAME_createdBy = "createdBy";
    public static final int PROP_ID_createdBy = 26;
    
    /* 创建时间: CREATE_TIME TIMESTAMP */
    public static final String PROP_NAME_createTime = "createTime";
    public static final int PROP_ID_createTime = 27;
    
    /* 修改人: UPDATED_BY VARCHAR */
    public static final String PROP_NAME_updatedBy = "updatedBy";
    public static final int PROP_ID_updatedBy = 28;
    
    /* 修改时间: UPDATE_TIME TIMESTAMP */
    public static final String PROP_NAME_updateTime = "updateTime";
    public static final int PROP_ID_updateTime = 29;
    
    /* 审核人: APPROVED_BY VARCHAR */
    public static final String PROP_NAME_approvedBy = "approvedBy";
    public static final int PROP_ID_approvedBy = 200;
    
    /* 审核时间: APPROVED_AT DATETIME */
    public static final String PROP_NAME_approvedAt = "approvedAt";
    public static final int PROP_ID_approvedAt = 201;
    
    /* 业务日期: BUSINESS_DATE DATE */
    public static final String PROP_NAME_businessDate = "businessDate";
    public static final int PROP_ID_businessDate = 202;
    

    private static int _PROP_ID_BOUND = 203;

    
    /* relation:  */
    public static final String PROP_NAME_customer = "customer";
    
    /* relation:  */
    public static final String PROP_NAME_contact = "contact";
    
    /* relation:  */
    public static final String PROP_NAME_ticketType = "ticketType";
    
    /* relation:  */
    public static final String PROP_NAME_slaPolicy = "slaPolicy";
    
    /* relation:  */
    public static final String PROP_NAME_org = "org";
    
    /* relation:  */
    public static final String PROP_NAME_catalogItem = "catalogItem";
    
    /* relation:  */
    public static final String PROP_NAME_actions = "actions";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[203];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_code] = PROP_NAME_code;
          PROP_NAME_TO_ID.put(PROP_NAME_code, PROP_ID_code);
      
          PROP_ID_TO_NAME[PROP_ID_orgId] = PROP_NAME_orgId;
          PROP_NAME_TO_ID.put(PROP_NAME_orgId, PROP_ID_orgId);
      
          PROP_ID_TO_NAME[PROP_ID_subject] = PROP_NAME_subject;
          PROP_NAME_TO_ID.put(PROP_NAME_subject, PROP_ID_subject);
      
          PROP_ID_TO_NAME[PROP_ID_description] = PROP_NAME_description;
          PROP_NAME_TO_ID.put(PROP_NAME_description, PROP_ID_description);
      
          PROP_ID_TO_NAME[PROP_ID_customerId] = PROP_NAME_customerId;
          PROP_NAME_TO_ID.put(PROP_NAME_customerId, PROP_ID_customerId);
      
          PROP_ID_TO_NAME[PROP_ID_contactId] = PROP_NAME_contactId;
          PROP_NAME_TO_ID.put(PROP_NAME_contactId, PROP_ID_contactId);
      
          PROP_ID_TO_NAME[PROP_ID_ticketTypeId] = PROP_NAME_ticketTypeId;
          PROP_NAME_TO_ID.put(PROP_NAME_ticketTypeId, PROP_ID_ticketTypeId);
      
          PROP_ID_TO_NAME[PROP_ID_priority] = PROP_NAME_priority;
          PROP_NAME_TO_ID.put(PROP_NAME_priority, PROP_ID_priority);
      
          PROP_ID_TO_NAME[PROP_ID_source] = PROP_NAME_source;
          PROP_NAME_TO_ID.put(PROP_NAME_source, PROP_ID_source);
      
          PROP_ID_TO_NAME[PROP_ID_assignedToId] = PROP_NAME_assignedToId;
          PROP_NAME_TO_ID.put(PROP_NAME_assignedToId, PROP_ID_assignedToId);
      
          PROP_ID_TO_NAME[PROP_ID_slaPolicyId] = PROP_NAME_slaPolicyId;
          PROP_NAME_TO_ID.put(PROP_NAME_slaPolicyId, PROP_ID_slaPolicyId);
      
          PROP_ID_TO_NAME[PROP_ID_deadlineDateTime] = PROP_NAME_deadlineDateTime;
          PROP_NAME_TO_ID.put(PROP_NAME_deadlineDateTime, PROP_ID_deadlineDateTime);
      
          PROP_ID_TO_NAME[PROP_ID_isSlaCompleted] = PROP_NAME_isSlaCompleted;
          PROP_NAME_TO_ID.put(PROP_NAME_isSlaCompleted, PROP_ID_isSlaCompleted);
      
          PROP_ID_TO_NAME[PROP_ID_startDateTime] = PROP_NAME_startDateTime;
          PROP_NAME_TO_ID.put(PROP_NAME_startDateTime, PROP_ID_startDateTime);
      
          PROP_ID_TO_NAME[PROP_ID_endDateTime] = PROP_NAME_endDateTime;
          PROP_NAME_TO_ID.put(PROP_NAME_endDateTime, PROP_ID_endDateTime);
      
          PROP_ID_TO_NAME[PROP_ID_duration] = PROP_NAME_duration;
          PROP_NAME_TO_ID.put(PROP_NAME_duration, PROP_ID_duration);
      
          PROP_ID_TO_NAME[PROP_ID_progress] = PROP_NAME_progress;
          PROP_NAME_TO_ID.put(PROP_NAME_progress, PROP_ID_progress);
      
          PROP_ID_TO_NAME[PROP_ID_status] = PROP_NAME_status;
          PROP_NAME_TO_ID.put(PROP_NAME_status, PROP_ID_status);
      
          PROP_ID_TO_NAME[PROP_ID_docStatus] = PROP_NAME_docStatus;
          PROP_NAME_TO_ID.put(PROP_NAME_docStatus, PROP_ID_docStatus);
      
          PROP_ID_TO_NAME[PROP_ID_approveStatus] = PROP_NAME_approveStatus;
          PROP_NAME_TO_ID.put(PROP_NAME_approveStatus, PROP_ID_approveStatus);
      
          PROP_ID_TO_NAME[PROP_ID_remark] = PROP_NAME_remark;
          PROP_NAME_TO_ID.put(PROP_NAME_remark, PROP_ID_remark);
      
          PROP_ID_TO_NAME[PROP_ID_catalogItemId] = PROP_NAME_catalogItemId;
          PROP_NAME_TO_ID.put(PROP_NAME_catalogItemId, PROP_ID_catalogItemId);
      
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
      
          PROP_ID_TO_NAME[PROP_ID_approvedBy] = PROP_NAME_approvedBy;
          PROP_NAME_TO_ID.put(PROP_NAME_approvedBy, PROP_ID_approvedBy);
      
          PROP_ID_TO_NAME[PROP_ID_approvedAt] = PROP_NAME_approvedAt;
          PROP_NAME_TO_ID.put(PROP_NAME_approvedAt, PROP_ID_approvedAt);
      
          PROP_ID_TO_NAME[PROP_ID_businessDate] = PROP_NAME_businessDate;
          PROP_NAME_TO_ID.put(PROP_NAME_businessDate, PROP_ID_businessDate);
      
    }

    
    /* ID: ID */
    private java.lang.Long _id;
    
    /* 单号: CODE */
    private java.lang.String _code;
    
    /* 业务组织: ORG_ID */
    private java.lang.Long _orgId;
    
    /* 工单主题: SUBJECT */
    private java.lang.String _subject;
    
    /* 问题描述: DESCRIPTION */
    private java.lang.String _description;
    
    /* 客户: CUSTOMER_ID */
    private java.lang.Long _customerId;
    
    /* 联系人: CONTACT_ID */
    private java.lang.Long _contactId;
    
    /* 工单类型: TICKET_TYPE_ID */
    private java.lang.Long _ticketTypeId;
    
    /* 优先级: PRIORITY */
    private java.lang.String _priority;
    
    /* 来源: SOURCE */
    private java.lang.String _source;
    
    /* 分配处理人: ASSIGNED_TO_ID */
    private java.lang.String _assignedToId;
    
    /* SLA 策略: SLA_POLICY_ID */
    private java.lang.Long _slaPolicyId;
    
    /* SLA 截止时间: DEADLINE_DATE_TIME */
    private java.time.LocalDateTime _deadlineDateTime;
    
    /* SLA 是否完成: IS_SLA_COMPLETED */
    private java.lang.Boolean _isSlaCompleted;
    
    /* 开始处理时间: START_DATE_TIME */
    private java.time.LocalDateTime _startDateTime;
    
    /* 关闭时间: END_DATE_TIME */
    private java.time.LocalDateTime _endDateTime;
    
    /* 处理时长(分钟): DURATION */
    private java.lang.Integer _duration;
    
    /* 进度(%): PROGRESS */
    private java.lang.Integer _progress;
    
    /* 工单状态: STATUS */
    private java.lang.String _status;
    
    /* 单据状态: DOC_STATUS */
    private java.lang.String _docStatus;
    
    /* 审核状态: APPROVE_STATUS */
    private java.lang.String _approveStatus;
    
    /* 备注: REMARK */
    private java.lang.String _remark;
    
    /* 服务目录项: CATALOG_ITEM_ID */
    private java.lang.Long _catalogItemId;
    
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
    
    /* 审核人: APPROVED_BY */
    private java.lang.String _approvedBy;
    
    /* 审核时间: APPROVED_AT */
    private java.time.LocalDateTime _approvedAt;
    
    /* 业务日期: BUSINESS_DATE */
    private java.time.LocalDate _businessDate;
    

    public _ErpCsTicket(){
        // for debug
    }

    protected ErpCsTicket newInstance(){
        ErpCsTicket entity = new ErpCsTicket();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpCsTicket cloneInstance() {
        ErpCsTicket entity = newInstance();
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
      return "app.erp.cs.dao.entity.ErpCsTicket";
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
        
            case PROP_ID_subject:
               return getSubject();
        
            case PROP_ID_description:
               return getDescription();
        
            case PROP_ID_customerId:
               return getCustomerId();
        
            case PROP_ID_contactId:
               return getContactId();
        
            case PROP_ID_ticketTypeId:
               return getTicketTypeId();
        
            case PROP_ID_priority:
               return getPriority();
        
            case PROP_ID_source:
               return getSource();
        
            case PROP_ID_assignedToId:
               return getAssignedToId();
        
            case PROP_ID_slaPolicyId:
               return getSlaPolicyId();
        
            case PROP_ID_deadlineDateTime:
               return getDeadlineDateTime();
        
            case PROP_ID_isSlaCompleted:
               return getIsSlaCompleted();
        
            case PROP_ID_startDateTime:
               return getStartDateTime();
        
            case PROP_ID_endDateTime:
               return getEndDateTime();
        
            case PROP_ID_duration:
               return getDuration();
        
            case PROP_ID_progress:
               return getProgress();
        
            case PROP_ID_status:
               return getStatus();
        
            case PROP_ID_docStatus:
               return getDocStatus();
        
            case PROP_ID_approveStatus:
               return getApproveStatus();
        
            case PROP_ID_remark:
               return getRemark();
        
            case PROP_ID_catalogItemId:
               return getCatalogItemId();
        
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
        
            case PROP_ID_approvedBy:
               return getApprovedBy();
        
            case PROP_ID_approvedAt:
               return getApprovedAt();
        
            case PROP_ID_businessDate:
               return getBusinessDate();
        
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
        
            case PROP_ID_subject:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_subject));
               }
               setSubject(typedValue);
               break;
            }
        
            case PROP_ID_description:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_description));
               }
               setDescription(typedValue);
               break;
            }
        
            case PROP_ID_customerId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_customerId));
               }
               setCustomerId(typedValue);
               break;
            }
        
            case PROP_ID_contactId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_contactId));
               }
               setContactId(typedValue);
               break;
            }
        
            case PROP_ID_ticketTypeId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_ticketTypeId));
               }
               setTicketTypeId(typedValue);
               break;
            }
        
            case PROP_ID_priority:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_priority));
               }
               setPriority(typedValue);
               break;
            }
        
            case PROP_ID_source:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_source));
               }
               setSource(typedValue);
               break;
            }
        
            case PROP_ID_assignedToId:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_assignedToId));
               }
               setAssignedToId(typedValue);
               break;
            }
        
            case PROP_ID_slaPolicyId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_slaPolicyId));
               }
               setSlaPolicyId(typedValue);
               break;
            }
        
            case PROP_ID_deadlineDateTime:{
               java.time.LocalDateTime typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDateTime(value,
                       err-> newTypeConversionError(PROP_NAME_deadlineDateTime));
               }
               setDeadlineDateTime(typedValue);
               break;
            }
        
            case PROP_ID_isSlaCompleted:{
               java.lang.Boolean typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBoolean(value,
                       err-> newTypeConversionError(PROP_NAME_isSlaCompleted));
               }
               setIsSlaCompleted(typedValue);
               break;
            }
        
            case PROP_ID_startDateTime:{
               java.time.LocalDateTime typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDateTime(value,
                       err-> newTypeConversionError(PROP_NAME_startDateTime));
               }
               setStartDateTime(typedValue);
               break;
            }
        
            case PROP_ID_endDateTime:{
               java.time.LocalDateTime typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDateTime(value,
                       err-> newTypeConversionError(PROP_NAME_endDateTime));
               }
               setEndDateTime(typedValue);
               break;
            }
        
            case PROP_ID_duration:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_duration));
               }
               setDuration(typedValue);
               break;
            }
        
            case PROP_ID_progress:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_progress));
               }
               setProgress(typedValue);
               break;
            }
        
            case PROP_ID_status:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_status));
               }
               setStatus(typedValue);
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
        
            case PROP_ID_approveStatus:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_approveStatus));
               }
               setApproveStatus(typedValue);
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
        
            case PROP_ID_catalogItemId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_catalogItemId));
               }
               setCatalogItemId(typedValue);
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
        
            case PROP_ID_approvedBy:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_approvedBy));
               }
               setApprovedBy(typedValue);
               break;
            }
        
            case PROP_ID_approvedAt:{
               java.time.LocalDateTime typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDateTime(value,
                       err-> newTypeConversionError(PROP_NAME_approvedAt));
               }
               setApprovedAt(typedValue);
               break;
            }
        
            case PROP_ID_businessDate:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_businessDate));
               }
               setBusinessDate(typedValue);
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
        
            case PROP_ID_subject:{
               onInitProp(propId);
               this._subject = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_description:{
               onInitProp(propId);
               this._description = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_customerId:{
               onInitProp(propId);
               this._customerId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_contactId:{
               onInitProp(propId);
               this._contactId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_ticketTypeId:{
               onInitProp(propId);
               this._ticketTypeId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_priority:{
               onInitProp(propId);
               this._priority = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_source:{
               onInitProp(propId);
               this._source = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_assignedToId:{
               onInitProp(propId);
               this._assignedToId = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_slaPolicyId:{
               onInitProp(propId);
               this._slaPolicyId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_deadlineDateTime:{
               onInitProp(propId);
               this._deadlineDateTime = (java.time.LocalDateTime)value;
               
               break;
            }
        
            case PROP_ID_isSlaCompleted:{
               onInitProp(propId);
               this._isSlaCompleted = (java.lang.Boolean)value;
               
               break;
            }
        
            case PROP_ID_startDateTime:{
               onInitProp(propId);
               this._startDateTime = (java.time.LocalDateTime)value;
               
               break;
            }
        
            case PROP_ID_endDateTime:{
               onInitProp(propId);
               this._endDateTime = (java.time.LocalDateTime)value;
               
               break;
            }
        
            case PROP_ID_duration:{
               onInitProp(propId);
               this._duration = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_progress:{
               onInitProp(propId);
               this._progress = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_status:{
               onInitProp(propId);
               this._status = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_docStatus:{
               onInitProp(propId);
               this._docStatus = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_approveStatus:{
               onInitProp(propId);
               this._approveStatus = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_remark:{
               onInitProp(propId);
               this._remark = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_catalogItemId:{
               onInitProp(propId);
               this._catalogItemId = (java.lang.Long)value;
               
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
        
            case PROP_ID_approvedBy:{
               onInitProp(propId);
               this._approvedBy = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_approvedAt:{
               onInitProp(propId);
               this._approvedAt = (java.time.LocalDateTime)value;
               
               break;
            }
        
            case PROP_ID_businessDate:{
               onInitProp(propId);
               this._businessDate = (java.time.LocalDate)value;
               
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
     * 单号: CODE
     */
    public final java.lang.String getCode(){
         onPropGet(PROP_ID_code);
         return _code;
    }

    /**
     * 单号: CODE
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
     * 工单主题: SUBJECT
     */
    public final java.lang.String getSubject(){
         onPropGet(PROP_ID_subject);
         return _subject;
    }

    /**
     * 工单主题: SUBJECT
     */
    public final void setSubject(java.lang.String value){
        if(onPropSet(PROP_ID_subject,value)){
            this._subject = value;
            internalClearRefs(PROP_ID_subject);
            
        }
    }
    
    /**
     * 问题描述: DESCRIPTION
     */
    public final java.lang.String getDescription(){
         onPropGet(PROP_ID_description);
         return _description;
    }

    /**
     * 问题描述: DESCRIPTION
     */
    public final void setDescription(java.lang.String value){
        if(onPropSet(PROP_ID_description,value)){
            this._description = value;
            internalClearRefs(PROP_ID_description);
            
        }
    }
    
    /**
     * 客户: CUSTOMER_ID
     */
    public final java.lang.Long getCustomerId(){
         onPropGet(PROP_ID_customerId);
         return _customerId;
    }

    /**
     * 客户: CUSTOMER_ID
     */
    public final void setCustomerId(java.lang.Long value){
        if(onPropSet(PROP_ID_customerId,value)){
            this._customerId = value;
            internalClearRefs(PROP_ID_customerId);
            
        }
    }
    
    /**
     * 联系人: CONTACT_ID
     */
    public final java.lang.Long getContactId(){
         onPropGet(PROP_ID_contactId);
         return _contactId;
    }

    /**
     * 联系人: CONTACT_ID
     */
    public final void setContactId(java.lang.Long value){
        if(onPropSet(PROP_ID_contactId,value)){
            this._contactId = value;
            internalClearRefs(PROP_ID_contactId);
            
        }
    }
    
    /**
     * 工单类型: TICKET_TYPE_ID
     */
    public final java.lang.Long getTicketTypeId(){
         onPropGet(PROP_ID_ticketTypeId);
         return _ticketTypeId;
    }

    /**
     * 工单类型: TICKET_TYPE_ID
     */
    public final void setTicketTypeId(java.lang.Long value){
        if(onPropSet(PROP_ID_ticketTypeId,value)){
            this._ticketTypeId = value;
            internalClearRefs(PROP_ID_ticketTypeId);
            
        }
    }
    
    /**
     * 优先级: PRIORITY
     */
    public final java.lang.String getPriority(){
         onPropGet(PROP_ID_priority);
         return _priority;
    }

    /**
     * 优先级: PRIORITY
     */
    public final void setPriority(java.lang.String value){
        if(onPropSet(PROP_ID_priority,value)){
            this._priority = value;
            internalClearRefs(PROP_ID_priority);
            
        }
    }
    
    /**
     * 来源: SOURCE
     */
    public final java.lang.String getSource(){
         onPropGet(PROP_ID_source);
         return _source;
    }

    /**
     * 来源: SOURCE
     */
    public final void setSource(java.lang.String value){
        if(onPropSet(PROP_ID_source,value)){
            this._source = value;
            internalClearRefs(PROP_ID_source);
            
        }
    }
    
    /**
     * 分配处理人: ASSIGNED_TO_ID
     */
    public final java.lang.String getAssignedToId(){
         onPropGet(PROP_ID_assignedToId);
         return _assignedToId;
    }

    /**
     * 分配处理人: ASSIGNED_TO_ID
     */
    public final void setAssignedToId(java.lang.String value){
        if(onPropSet(PROP_ID_assignedToId,value)){
            this._assignedToId = value;
            internalClearRefs(PROP_ID_assignedToId);
            
        }
    }
    
    /**
     * SLA 策略: SLA_POLICY_ID
     */
    public final java.lang.Long getSlaPolicyId(){
         onPropGet(PROP_ID_slaPolicyId);
         return _slaPolicyId;
    }

    /**
     * SLA 策略: SLA_POLICY_ID
     */
    public final void setSlaPolicyId(java.lang.Long value){
        if(onPropSet(PROP_ID_slaPolicyId,value)){
            this._slaPolicyId = value;
            internalClearRefs(PROP_ID_slaPolicyId);
            
        }
    }
    
    /**
     * SLA 截止时间: DEADLINE_DATE_TIME
     */
    public final java.time.LocalDateTime getDeadlineDateTime(){
         onPropGet(PROP_ID_deadlineDateTime);
         return _deadlineDateTime;
    }

    /**
     * SLA 截止时间: DEADLINE_DATE_TIME
     */
    public final void setDeadlineDateTime(java.time.LocalDateTime value){
        if(onPropSet(PROP_ID_deadlineDateTime,value)){
            this._deadlineDateTime = value;
            internalClearRefs(PROP_ID_deadlineDateTime);
            
        }
    }
    
    /**
     * SLA 是否完成: IS_SLA_COMPLETED
     */
    public final java.lang.Boolean getIsSlaCompleted(){
         onPropGet(PROP_ID_isSlaCompleted);
         return _isSlaCompleted;
    }

    /**
     * SLA 是否完成: IS_SLA_COMPLETED
     */
    public final void setIsSlaCompleted(java.lang.Boolean value){
        if(onPropSet(PROP_ID_isSlaCompleted,value)){
            this._isSlaCompleted = value;
            internalClearRefs(PROP_ID_isSlaCompleted);
            
        }
    }
    
    /**
     * 开始处理时间: START_DATE_TIME
     */
    public final java.time.LocalDateTime getStartDateTime(){
         onPropGet(PROP_ID_startDateTime);
         return _startDateTime;
    }

    /**
     * 开始处理时间: START_DATE_TIME
     */
    public final void setStartDateTime(java.time.LocalDateTime value){
        if(onPropSet(PROP_ID_startDateTime,value)){
            this._startDateTime = value;
            internalClearRefs(PROP_ID_startDateTime);
            
        }
    }
    
    /**
     * 关闭时间: END_DATE_TIME
     */
    public final java.time.LocalDateTime getEndDateTime(){
         onPropGet(PROP_ID_endDateTime);
         return _endDateTime;
    }

    /**
     * 关闭时间: END_DATE_TIME
     */
    public final void setEndDateTime(java.time.LocalDateTime value){
        if(onPropSet(PROP_ID_endDateTime,value)){
            this._endDateTime = value;
            internalClearRefs(PROP_ID_endDateTime);
            
        }
    }
    
    /**
     * 处理时长(分钟): DURATION
     */
    public final java.lang.Integer getDuration(){
         onPropGet(PROP_ID_duration);
         return _duration;
    }

    /**
     * 处理时长(分钟): DURATION
     */
    public final void setDuration(java.lang.Integer value){
        if(onPropSet(PROP_ID_duration,value)){
            this._duration = value;
            internalClearRefs(PROP_ID_duration);
            
        }
    }
    
    /**
     * 进度(%): PROGRESS
     */
    public final java.lang.Integer getProgress(){
         onPropGet(PROP_ID_progress);
         return _progress;
    }

    /**
     * 进度(%): PROGRESS
     */
    public final void setProgress(java.lang.Integer value){
        if(onPropSet(PROP_ID_progress,value)){
            this._progress = value;
            internalClearRefs(PROP_ID_progress);
            
        }
    }
    
    /**
     * 工单状态: STATUS
     */
    public final java.lang.String getStatus(){
         onPropGet(PROP_ID_status);
         return _status;
    }

    /**
     * 工单状态: STATUS
     */
    public final void setStatus(java.lang.String value){
        if(onPropSet(PROP_ID_status,value)){
            this._status = value;
            internalClearRefs(PROP_ID_status);
            
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
     * 审核状态: APPROVE_STATUS
     */
    public final java.lang.String getApproveStatus(){
         onPropGet(PROP_ID_approveStatus);
         return _approveStatus;
    }

    /**
     * 审核状态: APPROVE_STATUS
     */
    public final void setApproveStatus(java.lang.String value){
        if(onPropSet(PROP_ID_approveStatus,value)){
            this._approveStatus = value;
            internalClearRefs(PROP_ID_approveStatus);
            
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
     * 服务目录项: CATALOG_ITEM_ID
     */
    public final java.lang.Long getCatalogItemId(){
         onPropGet(PROP_ID_catalogItemId);
         return _catalogItemId;
    }

    /**
     * 服务目录项: CATALOG_ITEM_ID
     */
    public final void setCatalogItemId(java.lang.Long value){
        if(onPropSet(PROP_ID_catalogItemId,value)){
            this._catalogItemId = value;
            internalClearRefs(PROP_ID_catalogItemId);
            
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
     * 审核人: APPROVED_BY
     */
    public final java.lang.String getApprovedBy(){
         onPropGet(PROP_ID_approvedBy);
         return _approvedBy;
    }

    /**
     * 审核人: APPROVED_BY
     */
    public final void setApprovedBy(java.lang.String value){
        if(onPropSet(PROP_ID_approvedBy,value)){
            this._approvedBy = value;
            internalClearRefs(PROP_ID_approvedBy);
            
        }
    }
    
    /**
     * 审核时间: APPROVED_AT
     */
    public final java.time.LocalDateTime getApprovedAt(){
         onPropGet(PROP_ID_approvedAt);
         return _approvedAt;
    }

    /**
     * 审核时间: APPROVED_AT
     */
    public final void setApprovedAt(java.time.LocalDateTime value){
        if(onPropSet(PROP_ID_approvedAt,value)){
            this._approvedAt = value;
            internalClearRefs(PROP_ID_approvedAt);
            
        }
    }
    
    /**
     * 业务日期: BUSINESS_DATE
     */
    public final java.time.LocalDate getBusinessDate(){
         onPropGet(PROP_ID_businessDate);
         return _businessDate;
    }

    /**
     * 业务日期: BUSINESS_DATE
     */
    public final void setBusinessDate(java.time.LocalDate value){
        if(onPropSet(PROP_ID_businessDate,value)){
            this._businessDate = value;
            internalClearRefs(PROP_ID_businessDate);
            
        }
    }
    
    /**
     * 
     */
    public final app.erp.md.dao.entity.ErpMdPartner getCustomer(){
       return (app.erp.md.dao.entity.ErpMdPartner)internalGetRefEntity(PROP_NAME_customer);
    }

    public final void setCustomer(app.erp.md.dao.entity.ErpMdPartner refEntity){
   
           if(refEntity == null){
           
                   this.setCustomerId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_customer, refEntity,()->{
           
                           this.setCustomerId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.md.dao.entity.ErpMdPartner getContact(){
       return (app.erp.md.dao.entity.ErpMdPartner)internalGetRefEntity(PROP_NAME_contact);
    }

    public final void setContact(app.erp.md.dao.entity.ErpMdPartner refEntity){
   
           if(refEntity == null){
           
                   this.setContactId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_contact, refEntity,()->{
           
                           this.setContactId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.cs.dao.entity.ErpCsTicketType getTicketType(){
       return (app.erp.cs.dao.entity.ErpCsTicketType)internalGetRefEntity(PROP_NAME_ticketType);
    }

    public final void setTicketType(app.erp.cs.dao.entity.ErpCsTicketType refEntity){
   
           if(refEntity == null){
           
                   this.setTicketTypeId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_ticketType, refEntity,()->{
           
                           this.setTicketTypeId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.cs.dao.entity.ErpCsSlaPolicy getSlaPolicy(){
       return (app.erp.cs.dao.entity.ErpCsSlaPolicy)internalGetRefEntity(PROP_NAME_slaPolicy);
    }

    public final void setSlaPolicy(app.erp.cs.dao.entity.ErpCsSlaPolicy refEntity){
   
           if(refEntity == null){
           
                   this.setSlaPolicyId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_slaPolicy, refEntity,()->{
           
                           this.setSlaPolicyId(refEntity.getId());
                       
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
       
    /**
     * 
     */
    public final app.erp.cs.dao.entity.ErpCsServiceCatalogItem getCatalogItem(){
       return (app.erp.cs.dao.entity.ErpCsServiceCatalogItem)internalGetRefEntity(PROP_NAME_catalogItem);
    }

    public final void setCatalogItem(app.erp.cs.dao.entity.ErpCsServiceCatalogItem refEntity){
   
           if(refEntity == null){
           
                   this.setCatalogItemId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_catalogItem, refEntity,()->{
           
                           this.setCatalogItemId(refEntity.getId());
                       
           });
           }
       
    }
       
    private final OrmEntitySet<app.erp.cs.dao.entity.ErpCsTicketAction> _actions = new OrmEntitySet<>(this, PROP_NAME_actions,
        null, null,app.erp.cs.dao.entity.ErpCsTicketAction.class);

    /**
     * 。 refPropName: , keyProp: {rel.keyProp}
     */
    public final IOrmEntitySet<app.erp.cs.dao.entity.ErpCsTicketAction> getActions(){
       return _actions;
    }
       
}
// resume CPD analysis - CPD-ON

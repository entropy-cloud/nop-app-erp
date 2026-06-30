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

import app.erp.crm.dao.entity.ErpCrmEvent;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  活动/事件: erp_crm_event
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpCrmEvent extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 编码: CODE VARCHAR */
    public static final String PROP_NAME_code = "code";
    public static final int PROP_ID_code = 2;
    
    /* 业务组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 3;
    
    /* 事件类型: EVENT_TYPE VARCHAR */
    public static final String PROP_NAME_eventType = "eventType";
    public static final int PROP_ID_eventType = 4;
    
    /* 活动类别: EVENT_CATEGORY_ID BIGINT */
    public static final String PROP_NAME_eventCategoryId = "eventCategoryId";
    public static final int PROP_ID_eventCategoryId = 5;
    
    /* 主题: SUBJECT VARCHAR */
    public static final String PROP_NAME_subject = "subject";
    public static final int PROP_ID_subject = 6;
    
    /* 描述: DESCRIPTION VARCHAR */
    public static final String PROP_NAME_description = "description";
    public static final int PROP_ID_description = 7;
    
    /* 开始时间: START_DATE_TIME DATETIME */
    public static final String PROP_NAME_startDateTime = "startDateTime";
    public static final int PROP_ID_startDateTime = 8;
    
    /* 结束时间: END_DATE_TIME DATETIME */
    public static final String PROP_NAME_endDateTime = "endDateTime";
    public static final int PROP_ID_endDateTime = 9;
    
    /* 时长(分钟): DURATION INTEGER */
    public static final String PROP_NAME_duration = "duration";
    public static final int PROP_ID_duration = 10;
    
    /* 关联线索/商机: RELATED_LEAD_ID BIGINT */
    public static final String PROP_NAME_relatedLeadId = "relatedLeadId";
    public static final int PROP_ID_relatedLeadId = 11;
    
    /* 关联单据类型: RELATED_BILL_TYPE VARCHAR */
    public static final String PROP_NAME_relatedBillType = "relatedBillType";
    public static final int PROP_ID_relatedBillType = 12;
    
    /* 关联单据号: RELATED_BILL_CODE VARCHAR */
    public static final String PROP_NAME_relatedBillCode = "relatedBillCode";
    public static final int PROP_ID_relatedBillCode = 13;
    
    /* 关联客户: PARTNER_ID BIGINT */
    public static final String PROP_NAME_partnerId = "partnerId";
    public static final int PROP_ID_partnerId = 14;
    
    /* 联系人: CONTACT_ID BIGINT */
    public static final String PROP_NAME_contactId = "contactId";
    public static final int PROP_ID_contactId = 15;
    
    /* 负责人: OWNER_ID BIGINT */
    public static final String PROP_NAME_ownerId = "ownerId";
    public static final int PROP_ID_ownerId = 16;
    
    /* 活动状态: STATUS VARCHAR */
    public static final String PROP_NAME_status = "status";
    public static final int PROP_ID_status = 17;
    
    /* 优先级: PRIORITY VARCHAR */
    public static final String PROP_NAME_priority = "priority";
    public static final int PROP_ID_priority = 18;
    
    /* 是否重复事件: IS_RECURRENT BOOLEAN */
    public static final String PROP_NAME_isRecurrent = "isRecurrent";
    public static final int PROP_ID_isRecurrent = 19;
    
    /* 父事件: PARENT_EVENT_ID BIGINT */
    public static final String PROP_NAME_parentEventId = "parentEventId";
    public static final int PROP_ID_parentEventId = 20;
    
    /* 提醒提前分钟数: REMINDER_MINUTES_BEFORE INTEGER */
    public static final String PROP_NAME_reminderMinutesBefore = "reminderMinutesBefore";
    public static final int PROP_ID_reminderMinutesBefore = 21;
    
    /* 备注: REMARK VARCHAR */
    public static final String PROP_NAME_remark = "remark";
    public static final int PROP_ID_remark = 22;
    
    /* 逻辑删除版本: DEL_VERSION BIGINT */
    public static final String PROP_NAME_delVersion = "delVersion";
    public static final int PROP_ID_delVersion = 23;
    
    /* 数据版本: VERSION INTEGER */
    public static final String PROP_NAME_version = "version";
    public static final int PROP_ID_version = 24;
    
    /* 创建人: CREATED_BY VARCHAR */
    public static final String PROP_NAME_createdBy = "createdBy";
    public static final int PROP_ID_createdBy = 25;
    
    /* 创建时间: CREATE_TIME TIMESTAMP */
    public static final String PROP_NAME_createTime = "createTime";
    public static final int PROP_ID_createTime = 26;
    
    /* 修改人: UPDATED_BY VARCHAR */
    public static final String PROP_NAME_updatedBy = "updatedBy";
    public static final int PROP_ID_updatedBy = 27;
    
    /* 修改时间: UPDATE_TIME TIMESTAMP */
    public static final String PROP_NAME_updateTime = "updateTime";
    public static final int PROP_ID_updateTime = 28;
    

    private static int _PROP_ID_BOUND = 29;

    
    /* relation:  */
    public static final String PROP_NAME_relatedLead = "relatedLead";
    
    /* relation:  */
    public static final String PROP_NAME_partner = "partner";
    
    /* relation:  */
    public static final String PROP_NAME_parentEvent = "parentEvent";
    
    /* relation:  */
    public static final String PROP_NAME_org = "org";
    
    /* relation:  */
    public static final String PROP_NAME_eventCategory = "eventCategory";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[29];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_code] = PROP_NAME_code;
          PROP_NAME_TO_ID.put(PROP_NAME_code, PROP_ID_code);
      
          PROP_ID_TO_NAME[PROP_ID_orgId] = PROP_NAME_orgId;
          PROP_NAME_TO_ID.put(PROP_NAME_orgId, PROP_ID_orgId);
      
          PROP_ID_TO_NAME[PROP_ID_eventType] = PROP_NAME_eventType;
          PROP_NAME_TO_ID.put(PROP_NAME_eventType, PROP_ID_eventType);
      
          PROP_ID_TO_NAME[PROP_ID_eventCategoryId] = PROP_NAME_eventCategoryId;
          PROP_NAME_TO_ID.put(PROP_NAME_eventCategoryId, PROP_ID_eventCategoryId);
      
          PROP_ID_TO_NAME[PROP_ID_subject] = PROP_NAME_subject;
          PROP_NAME_TO_ID.put(PROP_NAME_subject, PROP_ID_subject);
      
          PROP_ID_TO_NAME[PROP_ID_description] = PROP_NAME_description;
          PROP_NAME_TO_ID.put(PROP_NAME_description, PROP_ID_description);
      
          PROP_ID_TO_NAME[PROP_ID_startDateTime] = PROP_NAME_startDateTime;
          PROP_NAME_TO_ID.put(PROP_NAME_startDateTime, PROP_ID_startDateTime);
      
          PROP_ID_TO_NAME[PROP_ID_endDateTime] = PROP_NAME_endDateTime;
          PROP_NAME_TO_ID.put(PROP_NAME_endDateTime, PROP_ID_endDateTime);
      
          PROP_ID_TO_NAME[PROP_ID_duration] = PROP_NAME_duration;
          PROP_NAME_TO_ID.put(PROP_NAME_duration, PROP_ID_duration);
      
          PROP_ID_TO_NAME[PROP_ID_relatedLeadId] = PROP_NAME_relatedLeadId;
          PROP_NAME_TO_ID.put(PROP_NAME_relatedLeadId, PROP_ID_relatedLeadId);
      
          PROP_ID_TO_NAME[PROP_ID_relatedBillType] = PROP_NAME_relatedBillType;
          PROP_NAME_TO_ID.put(PROP_NAME_relatedBillType, PROP_ID_relatedBillType);
      
          PROP_ID_TO_NAME[PROP_ID_relatedBillCode] = PROP_NAME_relatedBillCode;
          PROP_NAME_TO_ID.put(PROP_NAME_relatedBillCode, PROP_ID_relatedBillCode);
      
          PROP_ID_TO_NAME[PROP_ID_partnerId] = PROP_NAME_partnerId;
          PROP_NAME_TO_ID.put(PROP_NAME_partnerId, PROP_ID_partnerId);
      
          PROP_ID_TO_NAME[PROP_ID_contactId] = PROP_NAME_contactId;
          PROP_NAME_TO_ID.put(PROP_NAME_contactId, PROP_ID_contactId);
      
          PROP_ID_TO_NAME[PROP_ID_ownerId] = PROP_NAME_ownerId;
          PROP_NAME_TO_ID.put(PROP_NAME_ownerId, PROP_ID_ownerId);
      
          PROP_ID_TO_NAME[PROP_ID_status] = PROP_NAME_status;
          PROP_NAME_TO_ID.put(PROP_NAME_status, PROP_ID_status);
      
          PROP_ID_TO_NAME[PROP_ID_priority] = PROP_NAME_priority;
          PROP_NAME_TO_ID.put(PROP_NAME_priority, PROP_ID_priority);
      
          PROP_ID_TO_NAME[PROP_ID_isRecurrent] = PROP_NAME_isRecurrent;
          PROP_NAME_TO_ID.put(PROP_NAME_isRecurrent, PROP_ID_isRecurrent);
      
          PROP_ID_TO_NAME[PROP_ID_parentEventId] = PROP_NAME_parentEventId;
          PROP_NAME_TO_ID.put(PROP_NAME_parentEventId, PROP_ID_parentEventId);
      
          PROP_ID_TO_NAME[PROP_ID_reminderMinutesBefore] = PROP_NAME_reminderMinutesBefore;
          PROP_NAME_TO_ID.put(PROP_NAME_reminderMinutesBefore, PROP_ID_reminderMinutesBefore);
      
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
      
    }

    
    /* ID: ID */
    private java.lang.Long _id;
    
    /* 编码: CODE */
    private java.lang.String _code;
    
    /* 业务组织: ORG_ID */
    private java.lang.Long _orgId;
    
    /* 事件类型: EVENT_TYPE */
    private java.lang.String _eventType;
    
    /* 活动类别: EVENT_CATEGORY_ID */
    private java.lang.Long _eventCategoryId;
    
    /* 主题: SUBJECT */
    private java.lang.String _subject;
    
    /* 描述: DESCRIPTION */
    private java.lang.String _description;
    
    /* 开始时间: START_DATE_TIME */
    private java.time.LocalDateTime _startDateTime;
    
    /* 结束时间: END_DATE_TIME */
    private java.time.LocalDateTime _endDateTime;
    
    /* 时长(分钟): DURATION */
    private java.lang.Integer _duration;
    
    /* 关联线索/商机: RELATED_LEAD_ID */
    private java.lang.Long _relatedLeadId;
    
    /* 关联单据类型: RELATED_BILL_TYPE */
    private java.lang.String _relatedBillType;
    
    /* 关联单据号: RELATED_BILL_CODE */
    private java.lang.String _relatedBillCode;
    
    /* 关联客户: PARTNER_ID */
    private java.lang.Long _partnerId;
    
    /* 联系人: CONTACT_ID */
    private java.lang.Long _contactId;
    
    /* 负责人: OWNER_ID */
    private java.lang.Long _ownerId;
    
    /* 活动状态: STATUS */
    private java.lang.String _status;
    
    /* 优先级: PRIORITY */
    private java.lang.String _priority;
    
    /* 是否重复事件: IS_RECURRENT */
    private java.lang.Boolean _isRecurrent;
    
    /* 父事件: PARENT_EVENT_ID */
    private java.lang.Long _parentEventId;
    
    /* 提醒提前分钟数: REMINDER_MINUTES_BEFORE */
    private java.lang.Integer _reminderMinutesBefore;
    
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
    

    public _ErpCrmEvent(){
        // for debug
    }

    protected ErpCrmEvent newInstance(){
        ErpCrmEvent entity = new ErpCrmEvent();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpCrmEvent cloneInstance() {
        ErpCrmEvent entity = newInstance();
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
      return "app.erp.crm.dao.entity.ErpCrmEvent";
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
        
            case PROP_ID_eventType:
               return getEventType();
        
            case PROP_ID_eventCategoryId:
               return getEventCategoryId();
        
            case PROP_ID_subject:
               return getSubject();
        
            case PROP_ID_description:
               return getDescription();
        
            case PROP_ID_startDateTime:
               return getStartDateTime();
        
            case PROP_ID_endDateTime:
               return getEndDateTime();
        
            case PROP_ID_duration:
               return getDuration();
        
            case PROP_ID_relatedLeadId:
               return getRelatedLeadId();
        
            case PROP_ID_relatedBillType:
               return getRelatedBillType();
        
            case PROP_ID_relatedBillCode:
               return getRelatedBillCode();
        
            case PROP_ID_partnerId:
               return getPartnerId();
        
            case PROP_ID_contactId:
               return getContactId();
        
            case PROP_ID_ownerId:
               return getOwnerId();
        
            case PROP_ID_status:
               return getStatus();
        
            case PROP_ID_priority:
               return getPriority();
        
            case PROP_ID_isRecurrent:
               return getIsRecurrent();
        
            case PROP_ID_parentEventId:
               return getParentEventId();
        
            case PROP_ID_reminderMinutesBefore:
               return getReminderMinutesBefore();
        
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
        
            case PROP_ID_eventType:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_eventType));
               }
               setEventType(typedValue);
               break;
            }
        
            case PROP_ID_eventCategoryId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_eventCategoryId));
               }
               setEventCategoryId(typedValue);
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
        
            case PROP_ID_relatedLeadId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_relatedLeadId));
               }
               setRelatedLeadId(typedValue);
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
        
            case PROP_ID_partnerId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_partnerId));
               }
               setPartnerId(typedValue);
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
        
            case PROP_ID_ownerId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_ownerId));
               }
               setOwnerId(typedValue);
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
        
            case PROP_ID_priority:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_priority));
               }
               setPriority(typedValue);
               break;
            }
        
            case PROP_ID_isRecurrent:{
               java.lang.Boolean typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBoolean(value,
                       err-> newTypeConversionError(PROP_NAME_isRecurrent));
               }
               setIsRecurrent(typedValue);
               break;
            }
        
            case PROP_ID_parentEventId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_parentEventId));
               }
               setParentEventId(typedValue);
               break;
            }
        
            case PROP_ID_reminderMinutesBefore:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_reminderMinutesBefore));
               }
               setReminderMinutesBefore(typedValue);
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
        
            case PROP_ID_eventType:{
               onInitProp(propId);
               this._eventType = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_eventCategoryId:{
               onInitProp(propId);
               this._eventCategoryId = (java.lang.Long)value;
               
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
        
            case PROP_ID_relatedLeadId:{
               onInitProp(propId);
               this._relatedLeadId = (java.lang.Long)value;
               
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
        
            case PROP_ID_partnerId:{
               onInitProp(propId);
               this._partnerId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_contactId:{
               onInitProp(propId);
               this._contactId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_ownerId:{
               onInitProp(propId);
               this._ownerId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_status:{
               onInitProp(propId);
               this._status = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_priority:{
               onInitProp(propId);
               this._priority = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_isRecurrent:{
               onInitProp(propId);
               this._isRecurrent = (java.lang.Boolean)value;
               
               break;
            }
        
            case PROP_ID_parentEventId:{
               onInitProp(propId);
               this._parentEventId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_reminderMinutesBefore:{
               onInitProp(propId);
               this._reminderMinutesBefore = (java.lang.Integer)value;
               
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
     * 事件类型: EVENT_TYPE
     */
    public final java.lang.String getEventType(){
         onPropGet(PROP_ID_eventType);
         return _eventType;
    }

    /**
     * 事件类型: EVENT_TYPE
     */
    public final void setEventType(java.lang.String value){
        if(onPropSet(PROP_ID_eventType,value)){
            this._eventType = value;
            internalClearRefs(PROP_ID_eventType);
            
        }
    }
    
    /**
     * 活动类别: EVENT_CATEGORY_ID
     */
    public final java.lang.Long getEventCategoryId(){
         onPropGet(PROP_ID_eventCategoryId);
         return _eventCategoryId;
    }

    /**
     * 活动类别: EVENT_CATEGORY_ID
     */
    public final void setEventCategoryId(java.lang.Long value){
        if(onPropSet(PROP_ID_eventCategoryId,value)){
            this._eventCategoryId = value;
            internalClearRefs(PROP_ID_eventCategoryId);
            
        }
    }
    
    /**
     * 主题: SUBJECT
     */
    public final java.lang.String getSubject(){
         onPropGet(PROP_ID_subject);
         return _subject;
    }

    /**
     * 主题: SUBJECT
     */
    public final void setSubject(java.lang.String value){
        if(onPropSet(PROP_ID_subject,value)){
            this._subject = value;
            internalClearRefs(PROP_ID_subject);
            
        }
    }
    
    /**
     * 描述: DESCRIPTION
     */
    public final java.lang.String getDescription(){
         onPropGet(PROP_ID_description);
         return _description;
    }

    /**
     * 描述: DESCRIPTION
     */
    public final void setDescription(java.lang.String value){
        if(onPropSet(PROP_ID_description,value)){
            this._description = value;
            internalClearRefs(PROP_ID_description);
            
        }
    }
    
    /**
     * 开始时间: START_DATE_TIME
     */
    public final java.time.LocalDateTime getStartDateTime(){
         onPropGet(PROP_ID_startDateTime);
         return _startDateTime;
    }

    /**
     * 开始时间: START_DATE_TIME
     */
    public final void setStartDateTime(java.time.LocalDateTime value){
        if(onPropSet(PROP_ID_startDateTime,value)){
            this._startDateTime = value;
            internalClearRefs(PROP_ID_startDateTime);
            
        }
    }
    
    /**
     * 结束时间: END_DATE_TIME
     */
    public final java.time.LocalDateTime getEndDateTime(){
         onPropGet(PROP_ID_endDateTime);
         return _endDateTime;
    }

    /**
     * 结束时间: END_DATE_TIME
     */
    public final void setEndDateTime(java.time.LocalDateTime value){
        if(onPropSet(PROP_ID_endDateTime,value)){
            this._endDateTime = value;
            internalClearRefs(PROP_ID_endDateTime);
            
        }
    }
    
    /**
     * 时长(分钟): DURATION
     */
    public final java.lang.Integer getDuration(){
         onPropGet(PROP_ID_duration);
         return _duration;
    }

    /**
     * 时长(分钟): DURATION
     */
    public final void setDuration(java.lang.Integer value){
        if(onPropSet(PROP_ID_duration,value)){
            this._duration = value;
            internalClearRefs(PROP_ID_duration);
            
        }
    }
    
    /**
     * 关联线索/商机: RELATED_LEAD_ID
     */
    public final java.lang.Long getRelatedLeadId(){
         onPropGet(PROP_ID_relatedLeadId);
         return _relatedLeadId;
    }

    /**
     * 关联线索/商机: RELATED_LEAD_ID
     */
    public final void setRelatedLeadId(java.lang.Long value){
        if(onPropSet(PROP_ID_relatedLeadId,value)){
            this._relatedLeadId = value;
            internalClearRefs(PROP_ID_relatedLeadId);
            
        }
    }
    
    /**
     * 关联单据类型: RELATED_BILL_TYPE
     */
    public final java.lang.String getRelatedBillType(){
         onPropGet(PROP_ID_relatedBillType);
         return _relatedBillType;
    }

    /**
     * 关联单据类型: RELATED_BILL_TYPE
     */
    public final void setRelatedBillType(java.lang.String value){
        if(onPropSet(PROP_ID_relatedBillType,value)){
            this._relatedBillType = value;
            internalClearRefs(PROP_ID_relatedBillType);
            
        }
    }
    
    /**
     * 关联单据号: RELATED_BILL_CODE
     */
    public final java.lang.String getRelatedBillCode(){
         onPropGet(PROP_ID_relatedBillCode);
         return _relatedBillCode;
    }

    /**
     * 关联单据号: RELATED_BILL_CODE
     */
    public final void setRelatedBillCode(java.lang.String value){
        if(onPropSet(PROP_ID_relatedBillCode,value)){
            this._relatedBillCode = value;
            internalClearRefs(PROP_ID_relatedBillCode);
            
        }
    }
    
    /**
     * 关联客户: PARTNER_ID
     */
    public final java.lang.Long getPartnerId(){
         onPropGet(PROP_ID_partnerId);
         return _partnerId;
    }

    /**
     * 关联客户: PARTNER_ID
     */
    public final void setPartnerId(java.lang.Long value){
        if(onPropSet(PROP_ID_partnerId,value)){
            this._partnerId = value;
            internalClearRefs(PROP_ID_partnerId);
            
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
     * 负责人: OWNER_ID
     */
    public final java.lang.Long getOwnerId(){
         onPropGet(PROP_ID_ownerId);
         return _ownerId;
    }

    /**
     * 负责人: OWNER_ID
     */
    public final void setOwnerId(java.lang.Long value){
        if(onPropSet(PROP_ID_ownerId,value)){
            this._ownerId = value;
            internalClearRefs(PROP_ID_ownerId);
            
        }
    }
    
    /**
     * 活动状态: STATUS
     */
    public final java.lang.String getStatus(){
         onPropGet(PROP_ID_status);
         return _status;
    }

    /**
     * 活动状态: STATUS
     */
    public final void setStatus(java.lang.String value){
        if(onPropSet(PROP_ID_status,value)){
            this._status = value;
            internalClearRefs(PROP_ID_status);
            
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
     * 是否重复事件: IS_RECURRENT
     */
    public final java.lang.Boolean getIsRecurrent(){
         onPropGet(PROP_ID_isRecurrent);
         return _isRecurrent;
    }

    /**
     * 是否重复事件: IS_RECURRENT
     */
    public final void setIsRecurrent(java.lang.Boolean value){
        if(onPropSet(PROP_ID_isRecurrent,value)){
            this._isRecurrent = value;
            internalClearRefs(PROP_ID_isRecurrent);
            
        }
    }
    
    /**
     * 父事件: PARENT_EVENT_ID
     */
    public final java.lang.Long getParentEventId(){
         onPropGet(PROP_ID_parentEventId);
         return _parentEventId;
    }

    /**
     * 父事件: PARENT_EVENT_ID
     */
    public final void setParentEventId(java.lang.Long value){
        if(onPropSet(PROP_ID_parentEventId,value)){
            this._parentEventId = value;
            internalClearRefs(PROP_ID_parentEventId);
            
        }
    }
    
    /**
     * 提醒提前分钟数: REMINDER_MINUTES_BEFORE
     */
    public final java.lang.Integer getReminderMinutesBefore(){
         onPropGet(PROP_ID_reminderMinutesBefore);
         return _reminderMinutesBefore;
    }

    /**
     * 提醒提前分钟数: REMINDER_MINUTES_BEFORE
     */
    public final void setReminderMinutesBefore(java.lang.Integer value){
        if(onPropSet(PROP_ID_reminderMinutesBefore,value)){
            this._reminderMinutesBefore = value;
            internalClearRefs(PROP_ID_reminderMinutesBefore);
            
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
     * 
     */
    public final app.erp.crm.dao.entity.ErpCrmLead getRelatedLead(){
       return (app.erp.crm.dao.entity.ErpCrmLead)internalGetRefEntity(PROP_NAME_relatedLead);
    }

    public final void setRelatedLead(app.erp.crm.dao.entity.ErpCrmLead refEntity){
   
           if(refEntity == null){
           
                   this.setRelatedLeadId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_relatedLead, refEntity,()->{
           
                           this.setRelatedLeadId(refEntity.getId());
                       
           });
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
    public final app.erp.crm.dao.entity.ErpCrmEvent getParentEvent(){
       return (app.erp.crm.dao.entity.ErpCrmEvent)internalGetRefEntity(PROP_NAME_parentEvent);
    }

    public final void setParentEvent(app.erp.crm.dao.entity.ErpCrmEvent refEntity){
   
           if(refEntity == null){
           
                   this.setParentEventId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_parentEvent, refEntity,()->{
           
                           this.setParentEventId(refEntity.getId());
                       
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
    public final app.erp.crm.dao.entity.ErpCrmEventCategory getEventCategory(){
       return (app.erp.crm.dao.entity.ErpCrmEventCategory)internalGetRefEntity(PROP_NAME_eventCategory);
    }

    public final void setEventCategory(app.erp.crm.dao.entity.ErpCrmEventCategory refEntity){
   
           if(refEntity == null){
           
                   this.setEventCategoryId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_eventCategory, refEntity,()->{
           
                           this.setEventCategoryId(refEntity.getId());
                       
           });
           }
       
    }
       
}
// resume CPD analysis - CPD-ON

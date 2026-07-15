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

import app.erp.cs.dao.entity.ErpCsTimeEntry;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  工单计时条目: erp_cs_time_entry
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpCsTimeEntry extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 业务组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 2;
    
    /* 关联工单: TICKET_ID BIGINT */
    public static final String PROP_NAME_ticketId = "ticketId";
    public static final int PROP_ID_ticketId = 3;
    
    /* 处理人: AGENT_ID BIGINT */
    public static final String PROP_NAME_agentId = "agentId";
    public static final int PROP_ID_agentId = 4;
    
    /* 开始时间: START_TIME TIMESTAMP */
    public static final String PROP_NAME_startTime = "startTime";
    public static final int PROP_ID_startTime = 5;
    
    /* 结束时间: END_TIME TIMESTAMP */
    public static final String PROP_NAME_endTime = "endTime";
    public static final int PROP_ID_endTime = 6;
    
    /* 时长(分钟): DURATION INTEGER */
    public static final String PROP_NAME_duration = "duration";
    public static final int PROP_ID_duration = 7;
    
    /* 是否可计费: IS_BILLABLE BOOLEAN */
    public static final String PROP_NAME_isBillable = "isBillable";
    public static final int PROP_ID_isBillable = 8;
    
    /* 计费费率: BILLING_RATE DECIMAL */
    public static final String PROP_NAME_billingRate = "billingRate";
    public static final int PROP_ID_billingRate = 9;
    
    /* 计费金额: BILLABLE_AMOUNT DECIMAL */
    public static final String PROP_NAME_billableAmount = "billableAmount";
    public static final int PROP_ID_billableAmount = 10;
    
    /* 工作内容描述: DESCRIPTION VARCHAR */
    public static final String PROP_NAME_description = "description";
    public static final int PROP_ID_description = 11;
    
    /* 审批状态: APPROVAL_STATUS VARCHAR */
    public static final String PROP_NAME_approvalStatus = "approvalStatus";
    public static final int PROP_ID_approvalStatus = 12;
    
    /* 审批人: APPROVED_BY_ID VARCHAR */
    public static final String PROP_NAME_approvedById = "approvedById";
    public static final int PROP_ID_approvedById = 13;
    
    /* 审批时间: APPROVED_AT TIMESTAMP */
    public static final String PROP_NAME_approvedAt = "approvedAt";
    public static final int PROP_ID_approvedAt = 14;
    
    /* 关联项目: PROJECT_ID BIGINT */
    public static final String PROP_NAME_projectId = "projectId";
    public static final int PROP_ID_projectId = 15;
    
    /* 关联任务: TASK_ID BIGINT */
    public static final String PROP_NAME_taskId = "taskId";
    public static final int PROP_ID_taskId = 16;
    
    /* 来源: SOURCE VARCHAR */
    public static final String PROP_NAME_source = "source";
    public static final int PROP_ID_source = 17;
    
    /* 逻辑删除版本: DEL_VERSION BIGINT */
    public static final String PROP_NAME_delVersion = "delVersion";
    public static final int PROP_ID_delVersion = 18;
    
    /* 数据版本: VERSION INTEGER */
    public static final String PROP_NAME_version = "version";
    public static final int PROP_ID_version = 19;
    
    /* 创建人: CREATED_BY VARCHAR */
    public static final String PROP_NAME_createdBy = "createdBy";
    public static final int PROP_ID_createdBy = 20;
    
    /* 创建时间: CREATE_TIME TIMESTAMP */
    public static final String PROP_NAME_createTime = "createTime";
    public static final int PROP_ID_createTime = 21;
    
    /* 修改人: UPDATED_BY VARCHAR */
    public static final String PROP_NAME_updatedBy = "updatedBy";
    public static final int PROP_ID_updatedBy = 22;
    
    /* 修改时间: UPDATE_TIME TIMESTAMP */
    public static final String PROP_NAME_updateTime = "updateTime";
    public static final int PROP_ID_updateTime = 23;
    

    private static int _PROP_ID_BOUND = 24;

    
    /* relation:  */
    public static final String PROP_NAME_ticket = "ticket";
    
    /* relation:  */
    public static final String PROP_NAME_org = "org";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[24];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_orgId] = PROP_NAME_orgId;
          PROP_NAME_TO_ID.put(PROP_NAME_orgId, PROP_ID_orgId);
      
          PROP_ID_TO_NAME[PROP_ID_ticketId] = PROP_NAME_ticketId;
          PROP_NAME_TO_ID.put(PROP_NAME_ticketId, PROP_ID_ticketId);
      
          PROP_ID_TO_NAME[PROP_ID_agentId] = PROP_NAME_agentId;
          PROP_NAME_TO_ID.put(PROP_NAME_agentId, PROP_ID_agentId);
      
          PROP_ID_TO_NAME[PROP_ID_startTime] = PROP_NAME_startTime;
          PROP_NAME_TO_ID.put(PROP_NAME_startTime, PROP_ID_startTime);
      
          PROP_ID_TO_NAME[PROP_ID_endTime] = PROP_NAME_endTime;
          PROP_NAME_TO_ID.put(PROP_NAME_endTime, PROP_ID_endTime);
      
          PROP_ID_TO_NAME[PROP_ID_duration] = PROP_NAME_duration;
          PROP_NAME_TO_ID.put(PROP_NAME_duration, PROP_ID_duration);
      
          PROP_ID_TO_NAME[PROP_ID_isBillable] = PROP_NAME_isBillable;
          PROP_NAME_TO_ID.put(PROP_NAME_isBillable, PROP_ID_isBillable);
      
          PROP_ID_TO_NAME[PROP_ID_billingRate] = PROP_NAME_billingRate;
          PROP_NAME_TO_ID.put(PROP_NAME_billingRate, PROP_ID_billingRate);
      
          PROP_ID_TO_NAME[PROP_ID_billableAmount] = PROP_NAME_billableAmount;
          PROP_NAME_TO_ID.put(PROP_NAME_billableAmount, PROP_ID_billableAmount);
      
          PROP_ID_TO_NAME[PROP_ID_description] = PROP_NAME_description;
          PROP_NAME_TO_ID.put(PROP_NAME_description, PROP_ID_description);
      
          PROP_ID_TO_NAME[PROP_ID_approvalStatus] = PROP_NAME_approvalStatus;
          PROP_NAME_TO_ID.put(PROP_NAME_approvalStatus, PROP_ID_approvalStatus);
      
          PROP_ID_TO_NAME[PROP_ID_approvedById] = PROP_NAME_approvedById;
          PROP_NAME_TO_ID.put(PROP_NAME_approvedById, PROP_ID_approvedById);
      
          PROP_ID_TO_NAME[PROP_ID_approvedAt] = PROP_NAME_approvedAt;
          PROP_NAME_TO_ID.put(PROP_NAME_approvedAt, PROP_ID_approvedAt);
      
          PROP_ID_TO_NAME[PROP_ID_projectId] = PROP_NAME_projectId;
          PROP_NAME_TO_ID.put(PROP_NAME_projectId, PROP_ID_projectId);
      
          PROP_ID_TO_NAME[PROP_ID_taskId] = PROP_NAME_taskId;
          PROP_NAME_TO_ID.put(PROP_NAME_taskId, PROP_ID_taskId);
      
          PROP_ID_TO_NAME[PROP_ID_source] = PROP_NAME_source;
          PROP_NAME_TO_ID.put(PROP_NAME_source, PROP_ID_source);
      
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
    
    /* 业务组织: ORG_ID */
    private java.lang.Long _orgId;
    
    /* 关联工单: TICKET_ID */
    private java.lang.Long _ticketId;
    
    /* 处理人: AGENT_ID */
    private java.lang.Long _agentId;
    
    /* 开始时间: START_TIME */
    private java.sql.Timestamp _startTime;
    
    /* 结束时间: END_TIME */
    private java.sql.Timestamp _endTime;
    
    /* 时长(分钟): DURATION */
    private java.lang.Integer _duration;
    
    /* 是否可计费: IS_BILLABLE */
    private java.lang.Boolean _isBillable;
    
    /* 计费费率: BILLING_RATE */
    private java.math.BigDecimal _billingRate;
    
    /* 计费金额: BILLABLE_AMOUNT */
    private java.math.BigDecimal _billableAmount;
    
    /* 工作内容描述: DESCRIPTION */
    private java.lang.String _description;
    
    /* 审批状态: APPROVAL_STATUS */
    private java.lang.String _approvalStatus;
    
    /* 审批人: APPROVED_BY_ID */
    private java.lang.String _approvedById;
    
    /* 审批时间: APPROVED_AT */
    private java.sql.Timestamp _approvedAt;
    
    /* 关联项目: PROJECT_ID */
    private java.lang.Long _projectId;
    
    /* 关联任务: TASK_ID */
    private java.lang.Long _taskId;
    
    /* 来源: SOURCE */
    private java.lang.String _source;
    
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
    

    public _ErpCsTimeEntry(){
        // for debug
    }

    protected ErpCsTimeEntry newInstance(){
        ErpCsTimeEntry entity = new ErpCsTimeEntry();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpCsTimeEntry cloneInstance() {
        ErpCsTimeEntry entity = newInstance();
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
      return "app.erp.cs.dao.entity.ErpCsTimeEntry";
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
        
            case PROP_ID_orgId:
               return getOrgId();
        
            case PROP_ID_ticketId:
               return getTicketId();
        
            case PROP_ID_agentId:
               return getAgentId();
        
            case PROP_ID_startTime:
               return getStartTime();
        
            case PROP_ID_endTime:
               return getEndTime();
        
            case PROP_ID_duration:
               return getDuration();
        
            case PROP_ID_isBillable:
               return getIsBillable();
        
            case PROP_ID_billingRate:
               return getBillingRate();
        
            case PROP_ID_billableAmount:
               return getBillableAmount();
        
            case PROP_ID_description:
               return getDescription();
        
            case PROP_ID_approvalStatus:
               return getApprovalStatus();
        
            case PROP_ID_approvedById:
               return getApprovedById();
        
            case PROP_ID_approvedAt:
               return getApprovedAt();
        
            case PROP_ID_projectId:
               return getProjectId();
        
            case PROP_ID_taskId:
               return getTaskId();
        
            case PROP_ID_source:
               return getSource();
        
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
        
            case PROP_ID_orgId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_orgId));
               }
               setOrgId(typedValue);
               break;
            }
        
            case PROP_ID_ticketId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_ticketId));
               }
               setTicketId(typedValue);
               break;
            }
        
            case PROP_ID_agentId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_agentId));
               }
               setAgentId(typedValue);
               break;
            }
        
            case PROP_ID_startTime:{
               java.sql.Timestamp typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toTimestamp(value,
                       err-> newTypeConversionError(PROP_NAME_startTime));
               }
               setStartTime(typedValue);
               break;
            }
        
            case PROP_ID_endTime:{
               java.sql.Timestamp typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toTimestamp(value,
                       err-> newTypeConversionError(PROP_NAME_endTime));
               }
               setEndTime(typedValue);
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
        
            case PROP_ID_isBillable:{
               java.lang.Boolean typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBoolean(value,
                       err-> newTypeConversionError(PROP_NAME_isBillable));
               }
               setIsBillable(typedValue);
               break;
            }
        
            case PROP_ID_billingRate:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_billingRate));
               }
               setBillingRate(typedValue);
               break;
            }
        
            case PROP_ID_billableAmount:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_billableAmount));
               }
               setBillableAmount(typedValue);
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
        
            case PROP_ID_approvalStatus:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_approvalStatus));
               }
               setApprovalStatus(typedValue);
               break;
            }
        
            case PROP_ID_approvedById:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_approvedById));
               }
               setApprovedById(typedValue);
               break;
            }
        
            case PROP_ID_approvedAt:{
               java.sql.Timestamp typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toTimestamp(value,
                       err-> newTypeConversionError(PROP_NAME_approvedAt));
               }
               setApprovedAt(typedValue);
               break;
            }
        
            case PROP_ID_projectId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_projectId));
               }
               setProjectId(typedValue);
               break;
            }
        
            case PROP_ID_taskId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_taskId));
               }
               setTaskId(typedValue);
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
        
            case PROP_ID_orgId:{
               onInitProp(propId);
               this._orgId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_ticketId:{
               onInitProp(propId);
               this._ticketId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_agentId:{
               onInitProp(propId);
               this._agentId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_startTime:{
               onInitProp(propId);
               this._startTime = (java.sql.Timestamp)value;
               
               break;
            }
        
            case PROP_ID_endTime:{
               onInitProp(propId);
               this._endTime = (java.sql.Timestamp)value;
               
               break;
            }
        
            case PROP_ID_duration:{
               onInitProp(propId);
               this._duration = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_isBillable:{
               onInitProp(propId);
               this._isBillable = (java.lang.Boolean)value;
               
               break;
            }
        
            case PROP_ID_billingRate:{
               onInitProp(propId);
               this._billingRate = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_billableAmount:{
               onInitProp(propId);
               this._billableAmount = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_description:{
               onInitProp(propId);
               this._description = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_approvalStatus:{
               onInitProp(propId);
               this._approvalStatus = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_approvedById:{
               onInitProp(propId);
               this._approvedById = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_approvedAt:{
               onInitProp(propId);
               this._approvedAt = (java.sql.Timestamp)value;
               
               break;
            }
        
            case PROP_ID_projectId:{
               onInitProp(propId);
               this._projectId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_taskId:{
               onInitProp(propId);
               this._taskId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_source:{
               onInitProp(propId);
               this._source = (java.lang.String)value;
               
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
     * 关联工单: TICKET_ID
     */
    public final java.lang.Long getTicketId(){
         onPropGet(PROP_ID_ticketId);
         return _ticketId;
    }

    /**
     * 关联工单: TICKET_ID
     */
    public final void setTicketId(java.lang.Long value){
        if(onPropSet(PROP_ID_ticketId,value)){
            this._ticketId = value;
            internalClearRefs(PROP_ID_ticketId);
            
        }
    }
    
    /**
     * 处理人: AGENT_ID
     */
    public final java.lang.Long getAgentId(){
         onPropGet(PROP_ID_agentId);
         return _agentId;
    }

    /**
     * 处理人: AGENT_ID
     */
    public final void setAgentId(java.lang.Long value){
        if(onPropSet(PROP_ID_agentId,value)){
            this._agentId = value;
            internalClearRefs(PROP_ID_agentId);
            
        }
    }
    
    /**
     * 开始时间: START_TIME
     */
    public final java.sql.Timestamp getStartTime(){
         onPropGet(PROP_ID_startTime);
         return _startTime;
    }

    /**
     * 开始时间: START_TIME
     */
    public final void setStartTime(java.sql.Timestamp value){
        if(onPropSet(PROP_ID_startTime,value)){
            this._startTime = value;
            internalClearRefs(PROP_ID_startTime);
            
        }
    }
    
    /**
     * 结束时间: END_TIME
     */
    public final java.sql.Timestamp getEndTime(){
         onPropGet(PROP_ID_endTime);
         return _endTime;
    }

    /**
     * 结束时间: END_TIME
     */
    public final void setEndTime(java.sql.Timestamp value){
        if(onPropSet(PROP_ID_endTime,value)){
            this._endTime = value;
            internalClearRefs(PROP_ID_endTime);
            
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
     * 是否可计费: IS_BILLABLE
     */
    public final java.lang.Boolean getIsBillable(){
         onPropGet(PROP_ID_isBillable);
         return _isBillable;
    }

    /**
     * 是否可计费: IS_BILLABLE
     */
    public final void setIsBillable(java.lang.Boolean value){
        if(onPropSet(PROP_ID_isBillable,value)){
            this._isBillable = value;
            internalClearRefs(PROP_ID_isBillable);
            
        }
    }
    
    /**
     * 计费费率: BILLING_RATE
     */
    public final java.math.BigDecimal getBillingRate(){
         onPropGet(PROP_ID_billingRate);
         return _billingRate;
    }

    /**
     * 计费费率: BILLING_RATE
     */
    public final void setBillingRate(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_billingRate,value)){
            this._billingRate = value;
            internalClearRefs(PROP_ID_billingRate);
            
        }
    }
    
    /**
     * 计费金额: BILLABLE_AMOUNT
     */
    public final java.math.BigDecimal getBillableAmount(){
         onPropGet(PROP_ID_billableAmount);
         return _billableAmount;
    }

    /**
     * 计费金额: BILLABLE_AMOUNT
     */
    public final void setBillableAmount(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_billableAmount,value)){
            this._billableAmount = value;
            internalClearRefs(PROP_ID_billableAmount);
            
        }
    }
    
    /**
     * 工作内容描述: DESCRIPTION
     */
    public final java.lang.String getDescription(){
         onPropGet(PROP_ID_description);
         return _description;
    }

    /**
     * 工作内容描述: DESCRIPTION
     */
    public final void setDescription(java.lang.String value){
        if(onPropSet(PROP_ID_description,value)){
            this._description = value;
            internalClearRefs(PROP_ID_description);
            
        }
    }
    
    /**
     * 审批状态: APPROVAL_STATUS
     */
    public final java.lang.String getApprovalStatus(){
         onPropGet(PROP_ID_approvalStatus);
         return _approvalStatus;
    }

    /**
     * 审批状态: APPROVAL_STATUS
     */
    public final void setApprovalStatus(java.lang.String value){
        if(onPropSet(PROP_ID_approvalStatus,value)){
            this._approvalStatus = value;
            internalClearRefs(PROP_ID_approvalStatus);
            
        }
    }
    
    /**
     * 审批人: APPROVED_BY_ID
     */
    public final java.lang.String getApprovedById(){
         onPropGet(PROP_ID_approvedById);
         return _approvedById;
    }

    /**
     * 审批人: APPROVED_BY_ID
     */
    public final void setApprovedById(java.lang.String value){
        if(onPropSet(PROP_ID_approvedById,value)){
            this._approvedById = value;
            internalClearRefs(PROP_ID_approvedById);
            
        }
    }
    
    /**
     * 审批时间: APPROVED_AT
     */
    public final java.sql.Timestamp getApprovedAt(){
         onPropGet(PROP_ID_approvedAt);
         return _approvedAt;
    }

    /**
     * 审批时间: APPROVED_AT
     */
    public final void setApprovedAt(java.sql.Timestamp value){
        if(onPropSet(PROP_ID_approvedAt,value)){
            this._approvedAt = value;
            internalClearRefs(PROP_ID_approvedAt);
            
        }
    }
    
    /**
     * 关联项目: PROJECT_ID
     */
    public final java.lang.Long getProjectId(){
         onPropGet(PROP_ID_projectId);
         return _projectId;
    }

    /**
     * 关联项目: PROJECT_ID
     */
    public final void setProjectId(java.lang.Long value){
        if(onPropSet(PROP_ID_projectId,value)){
            this._projectId = value;
            internalClearRefs(PROP_ID_projectId);
            
        }
    }
    
    /**
     * 关联任务: TASK_ID
     */
    public final java.lang.Long getTaskId(){
         onPropGet(PROP_ID_taskId);
         return _taskId;
    }

    /**
     * 关联任务: TASK_ID
     */
    public final void setTaskId(java.lang.Long value){
        if(onPropSet(PROP_ID_taskId,value)){
            this._taskId = value;
            internalClearRefs(PROP_ID_taskId);
            
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
    public final app.erp.cs.dao.entity.ErpCsTicket getTicket(){
       return (app.erp.cs.dao.entity.ErpCsTicket)internalGetRefEntity(PROP_NAME_ticket);
    }

    public final void setTicket(app.erp.cs.dao.entity.ErpCsTicket refEntity){
   
           if(refEntity == null){
           
                   this.setTicketId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_ticket, refEntity,()->{
           
                           this.setTicketId(refEntity.getId());
                       
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
       
}
// resume CPD analysis - CPD-ON

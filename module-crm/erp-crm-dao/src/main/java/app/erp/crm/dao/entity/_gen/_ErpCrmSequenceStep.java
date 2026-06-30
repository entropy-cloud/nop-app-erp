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

import app.erp.crm.dao.entity.ErpCrmSequenceStep;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  序列步骤: erp_crm_sequence_step
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpCrmSequenceStep extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 所属序列: SEQUENCE_ID BIGINT */
    public static final String PROP_NAME_sequenceId = "sequenceId";
    public static final int PROP_ID_sequenceId = 2;
    
    /* 业务组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 3;
    
    /* 步骤名称: STEP_NAME VARCHAR */
    public static final String PROP_NAME_stepName = "stepName";
    public static final int PROP_ID_stepName = 4;
    
    /* 步骤序号: STEP_ORDER INTEGER */
    public static final String PROP_NAME_stepOrder = "stepOrder";
    public static final int PROP_ID_stepOrder = 5;
    
    /* 距上一步间隔天数: DUE_DAYS INTEGER */
    public static final String PROP_NAME_dueDays = "dueDays";
    public static final int PROP_ID_dueDays = 6;
    
    /* 步骤活动类型: ACTIVITY_TYPE VARCHAR */
    public static final String PROP_NAME_activityType = "activityType";
    public static final int PROP_ID_activityType = 7;
    
    /* 步骤说明: STEP_DESCRIPTION VARCHAR */
    public static final String PROP_NAME_stepDescription = "stepDescription";
    public static final int PROP_ID_stepDescription = 8;
    
    /* 完成条件: COMPLETION_CONDITION VARCHAR */
    public static final String PROP_NAME_completionCondition = "completionCondition";
    public static final int PROP_ID_completionCondition = 9;
    
    /* 是否必须完成: IS_MANDATORY BOOLEAN */
    public static final String PROP_NAME_isMandatory = "isMandatory";
    public static final int PROP_ID_isMandatory = 10;
    
    /* 是否自动创建事件: AUTO_CREATE_EVENT BOOLEAN */
    public static final String PROP_NAME_autoCreateEvent = "autoCreateEvent";
    public static final int PROP_ID_autoCreateEvent = 11;
    
    /* 备注: REMARK VARCHAR */
    public static final String PROP_NAME_remark = "remark";
    public static final int PROP_ID_remark = 12;
    
    /* 逻辑删除版本: DEL_VERSION BIGINT */
    public static final String PROP_NAME_delVersion = "delVersion";
    public static final int PROP_ID_delVersion = 13;
    
    /* 数据版本: VERSION INTEGER */
    public static final String PROP_NAME_version = "version";
    public static final int PROP_ID_version = 14;
    
    /* 创建人: CREATED_BY VARCHAR */
    public static final String PROP_NAME_createdBy = "createdBy";
    public static final int PROP_ID_createdBy = 15;
    
    /* 创建时间: CREATE_TIME TIMESTAMP */
    public static final String PROP_NAME_createTime = "createTime";
    public static final int PROP_ID_createTime = 16;
    
    /* 修改人: UPDATED_BY VARCHAR */
    public static final String PROP_NAME_updatedBy = "updatedBy";
    public static final int PROP_ID_updatedBy = 17;
    
    /* 修改时间: UPDATE_TIME TIMESTAMP */
    public static final String PROP_NAME_updateTime = "updateTime";
    public static final int PROP_ID_updateTime = 18;
    

    private static int _PROP_ID_BOUND = 19;

    
    /* relation:  */
    public static final String PROP_NAME_sequence = "sequence";
    
    /* relation:  */
    public static final String PROP_NAME_org = "org";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[19];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_sequenceId] = PROP_NAME_sequenceId;
          PROP_NAME_TO_ID.put(PROP_NAME_sequenceId, PROP_ID_sequenceId);
      
          PROP_ID_TO_NAME[PROP_ID_orgId] = PROP_NAME_orgId;
          PROP_NAME_TO_ID.put(PROP_NAME_orgId, PROP_ID_orgId);
      
          PROP_ID_TO_NAME[PROP_ID_stepName] = PROP_NAME_stepName;
          PROP_NAME_TO_ID.put(PROP_NAME_stepName, PROP_ID_stepName);
      
          PROP_ID_TO_NAME[PROP_ID_stepOrder] = PROP_NAME_stepOrder;
          PROP_NAME_TO_ID.put(PROP_NAME_stepOrder, PROP_ID_stepOrder);
      
          PROP_ID_TO_NAME[PROP_ID_dueDays] = PROP_NAME_dueDays;
          PROP_NAME_TO_ID.put(PROP_NAME_dueDays, PROP_ID_dueDays);
      
          PROP_ID_TO_NAME[PROP_ID_activityType] = PROP_NAME_activityType;
          PROP_NAME_TO_ID.put(PROP_NAME_activityType, PROP_ID_activityType);
      
          PROP_ID_TO_NAME[PROP_ID_stepDescription] = PROP_NAME_stepDescription;
          PROP_NAME_TO_ID.put(PROP_NAME_stepDescription, PROP_ID_stepDescription);
      
          PROP_ID_TO_NAME[PROP_ID_completionCondition] = PROP_NAME_completionCondition;
          PROP_NAME_TO_ID.put(PROP_NAME_completionCondition, PROP_ID_completionCondition);
      
          PROP_ID_TO_NAME[PROP_ID_isMandatory] = PROP_NAME_isMandatory;
          PROP_NAME_TO_ID.put(PROP_NAME_isMandatory, PROP_ID_isMandatory);
      
          PROP_ID_TO_NAME[PROP_ID_autoCreateEvent] = PROP_NAME_autoCreateEvent;
          PROP_NAME_TO_ID.put(PROP_NAME_autoCreateEvent, PROP_ID_autoCreateEvent);
      
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
    
    /* 所属序列: SEQUENCE_ID */
    private java.lang.Long _sequenceId;
    
    /* 业务组织: ORG_ID */
    private java.lang.Long _orgId;
    
    /* 步骤名称: STEP_NAME */
    private java.lang.String _stepName;
    
    /* 步骤序号: STEP_ORDER */
    private java.lang.Integer _stepOrder;
    
    /* 距上一步间隔天数: DUE_DAYS */
    private java.lang.Integer _dueDays;
    
    /* 步骤活动类型: ACTIVITY_TYPE */
    private java.lang.String _activityType;
    
    /* 步骤说明: STEP_DESCRIPTION */
    private java.lang.String _stepDescription;
    
    /* 完成条件: COMPLETION_CONDITION */
    private java.lang.String _completionCondition;
    
    /* 是否必须完成: IS_MANDATORY */
    private java.lang.Boolean _isMandatory;
    
    /* 是否自动创建事件: AUTO_CREATE_EVENT */
    private java.lang.Boolean _autoCreateEvent;
    
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
    

    public _ErpCrmSequenceStep(){
        // for debug
    }

    protected ErpCrmSequenceStep newInstance(){
        ErpCrmSequenceStep entity = new ErpCrmSequenceStep();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpCrmSequenceStep cloneInstance() {
        ErpCrmSequenceStep entity = newInstance();
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
      return "app.erp.crm.dao.entity.ErpCrmSequenceStep";
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
        
            case PROP_ID_sequenceId:
               return getSequenceId();
        
            case PROP_ID_orgId:
               return getOrgId();
        
            case PROP_ID_stepName:
               return getStepName();
        
            case PROP_ID_stepOrder:
               return getStepOrder();
        
            case PROP_ID_dueDays:
               return getDueDays();
        
            case PROP_ID_activityType:
               return getActivityType();
        
            case PROP_ID_stepDescription:
               return getStepDescription();
        
            case PROP_ID_completionCondition:
               return getCompletionCondition();
        
            case PROP_ID_isMandatory:
               return getIsMandatory();
        
            case PROP_ID_autoCreateEvent:
               return getAutoCreateEvent();
        
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
        
            case PROP_ID_sequenceId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_sequenceId));
               }
               setSequenceId(typedValue);
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
        
            case PROP_ID_stepName:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_stepName));
               }
               setStepName(typedValue);
               break;
            }
        
            case PROP_ID_stepOrder:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_stepOrder));
               }
               setStepOrder(typedValue);
               break;
            }
        
            case PROP_ID_dueDays:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_dueDays));
               }
               setDueDays(typedValue);
               break;
            }
        
            case PROP_ID_activityType:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_activityType));
               }
               setActivityType(typedValue);
               break;
            }
        
            case PROP_ID_stepDescription:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_stepDescription));
               }
               setStepDescription(typedValue);
               break;
            }
        
            case PROP_ID_completionCondition:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_completionCondition));
               }
               setCompletionCondition(typedValue);
               break;
            }
        
            case PROP_ID_isMandatory:{
               java.lang.Boolean typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBoolean(value,
                       err-> newTypeConversionError(PROP_NAME_isMandatory));
               }
               setIsMandatory(typedValue);
               break;
            }
        
            case PROP_ID_autoCreateEvent:{
               java.lang.Boolean typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBoolean(value,
                       err-> newTypeConversionError(PROP_NAME_autoCreateEvent));
               }
               setAutoCreateEvent(typedValue);
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
        
            case PROP_ID_sequenceId:{
               onInitProp(propId);
               this._sequenceId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_orgId:{
               onInitProp(propId);
               this._orgId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_stepName:{
               onInitProp(propId);
               this._stepName = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_stepOrder:{
               onInitProp(propId);
               this._stepOrder = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_dueDays:{
               onInitProp(propId);
               this._dueDays = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_activityType:{
               onInitProp(propId);
               this._activityType = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_stepDescription:{
               onInitProp(propId);
               this._stepDescription = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_completionCondition:{
               onInitProp(propId);
               this._completionCondition = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_isMandatory:{
               onInitProp(propId);
               this._isMandatory = (java.lang.Boolean)value;
               
               break;
            }
        
            case PROP_ID_autoCreateEvent:{
               onInitProp(propId);
               this._autoCreateEvent = (java.lang.Boolean)value;
               
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
     * 所属序列: SEQUENCE_ID
     */
    public final java.lang.Long getSequenceId(){
         onPropGet(PROP_ID_sequenceId);
         return _sequenceId;
    }

    /**
     * 所属序列: SEQUENCE_ID
     */
    public final void setSequenceId(java.lang.Long value){
        if(onPropSet(PROP_ID_sequenceId,value)){
            this._sequenceId = value;
            internalClearRefs(PROP_ID_sequenceId);
            
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
     * 步骤名称: STEP_NAME
     */
    public final java.lang.String getStepName(){
         onPropGet(PROP_ID_stepName);
         return _stepName;
    }

    /**
     * 步骤名称: STEP_NAME
     */
    public final void setStepName(java.lang.String value){
        if(onPropSet(PROP_ID_stepName,value)){
            this._stepName = value;
            internalClearRefs(PROP_ID_stepName);
            
        }
    }
    
    /**
     * 步骤序号: STEP_ORDER
     */
    public final java.lang.Integer getStepOrder(){
         onPropGet(PROP_ID_stepOrder);
         return _stepOrder;
    }

    /**
     * 步骤序号: STEP_ORDER
     */
    public final void setStepOrder(java.lang.Integer value){
        if(onPropSet(PROP_ID_stepOrder,value)){
            this._stepOrder = value;
            internalClearRefs(PROP_ID_stepOrder);
            
        }
    }
    
    /**
     * 距上一步间隔天数: DUE_DAYS
     */
    public final java.lang.Integer getDueDays(){
         onPropGet(PROP_ID_dueDays);
         return _dueDays;
    }

    /**
     * 距上一步间隔天数: DUE_DAYS
     */
    public final void setDueDays(java.lang.Integer value){
        if(onPropSet(PROP_ID_dueDays,value)){
            this._dueDays = value;
            internalClearRefs(PROP_ID_dueDays);
            
        }
    }
    
    /**
     * 步骤活动类型: ACTIVITY_TYPE
     */
    public final java.lang.String getActivityType(){
         onPropGet(PROP_ID_activityType);
         return _activityType;
    }

    /**
     * 步骤活动类型: ACTIVITY_TYPE
     */
    public final void setActivityType(java.lang.String value){
        if(onPropSet(PROP_ID_activityType,value)){
            this._activityType = value;
            internalClearRefs(PROP_ID_activityType);
            
        }
    }
    
    /**
     * 步骤说明: STEP_DESCRIPTION
     */
    public final java.lang.String getStepDescription(){
         onPropGet(PROP_ID_stepDescription);
         return _stepDescription;
    }

    /**
     * 步骤说明: STEP_DESCRIPTION
     */
    public final void setStepDescription(java.lang.String value){
        if(onPropSet(PROP_ID_stepDescription,value)){
            this._stepDescription = value;
            internalClearRefs(PROP_ID_stepDescription);
            
        }
    }
    
    /**
     * 完成条件: COMPLETION_CONDITION
     */
    public final java.lang.String getCompletionCondition(){
         onPropGet(PROP_ID_completionCondition);
         return _completionCondition;
    }

    /**
     * 完成条件: COMPLETION_CONDITION
     */
    public final void setCompletionCondition(java.lang.String value){
        if(onPropSet(PROP_ID_completionCondition,value)){
            this._completionCondition = value;
            internalClearRefs(PROP_ID_completionCondition);
            
        }
    }
    
    /**
     * 是否必须完成: IS_MANDATORY
     */
    public final java.lang.Boolean getIsMandatory(){
         onPropGet(PROP_ID_isMandatory);
         return _isMandatory;
    }

    /**
     * 是否必须完成: IS_MANDATORY
     */
    public final void setIsMandatory(java.lang.Boolean value){
        if(onPropSet(PROP_ID_isMandatory,value)){
            this._isMandatory = value;
            internalClearRefs(PROP_ID_isMandatory);
            
        }
    }
    
    /**
     * 是否自动创建事件: AUTO_CREATE_EVENT
     */
    public final java.lang.Boolean getAutoCreateEvent(){
         onPropGet(PROP_ID_autoCreateEvent);
         return _autoCreateEvent;
    }

    /**
     * 是否自动创建事件: AUTO_CREATE_EVENT
     */
    public final void setAutoCreateEvent(java.lang.Boolean value){
        if(onPropSet(PROP_ID_autoCreateEvent,value)){
            this._autoCreateEvent = value;
            internalClearRefs(PROP_ID_autoCreateEvent);
            
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
    public final app.erp.crm.dao.entity.ErpCrmSequence getSequence(){
       return (app.erp.crm.dao.entity.ErpCrmSequence)internalGetRefEntity(PROP_NAME_sequence);
    }

    public final void setSequence(app.erp.crm.dao.entity.ErpCrmSequence refEntity){
   
           if(refEntity == null){
           
                   this.setSequenceId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_sequence, refEntity,()->{
           
                           this.setSequenceId(refEntity.getId());
                       
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

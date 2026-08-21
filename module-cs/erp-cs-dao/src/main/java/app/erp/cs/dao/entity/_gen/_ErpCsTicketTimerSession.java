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

import app.erp.cs.dao.entity.ErpCsTicketTimerSession;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  工单计时器会话: erp_cs_ticket_timer_session
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpCsTicketTimerSession extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 业务组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 2;
    
    /* 客服: AGENT_ID VARCHAR */
    public static final String PROP_NAME_agentId = "agentId";
    public static final int PROP_ID_agentId = 3;
    
    /* 关联工单: TICKET_ID BIGINT */
    public static final String PROP_NAME_ticketId = "ticketId";
    public static final int PROP_ID_ticketId = 4;
    
    /* 开始时间: START_TIME TIMESTAMP */
    public static final String PROP_NAME_startTime = "startTime";
    public static final int PROP_ID_startTime = 5;
    
    /* 停止时间: STOP_TIME TIMESTAMP */
    public static final String PROP_NAME_stopTime = "stopTime";
    public static final int PROP_ID_stopTime = 6;
    
    /* 暂停开始时间: PAUSE_START_DATE_TIME TIMESTAMP */
    public static final String PROP_NAME_pauseStartDateTime = "pauseStartDateTime";
    public static final int PROP_ID_pauseStartDateTime = 7;
    
    /* 累计暂停时长(分钟): CUMULATIVE_PAUSE_MINUTES INTEGER */
    public static final String PROP_NAME_cumulativePauseMinutes = "cumulativePauseMinutes";
    public static final int PROP_ID_cumulativePauseMinutes = 8;
    
    /* 暂停原因: PAUSE_REASON VARCHAR */
    public static final String PROP_NAME_pauseReason = "pauseReason";
    public static final int PROP_ID_pauseReason = 9;
    
    /* 会话状态: STATUS VARCHAR */
    public static final String PROP_NAME_status = "status";
    public static final int PROP_ID_status = 10;
    
    /* 进行中标记: ACTIVE_FLAG VARCHAR */
    public static final String PROP_NAME_activeFlag = "activeFlag";
    public static final int PROP_ID_activeFlag = 11;
    
    /* 逻辑删除版本: DEL_VERSION BIGINT */
    public static final String PROP_NAME_delVersion = "delVersion";
    public static final int PROP_ID_delVersion = 12;
    
    /* 数据版本: VERSION INTEGER */
    public static final String PROP_NAME_version = "version";
    public static final int PROP_ID_version = 13;
    
    /* 创建人: CREATED_BY VARCHAR */
    public static final String PROP_NAME_createdBy = "createdBy";
    public static final int PROP_ID_createdBy = 14;
    
    /* 创建时间: CREATE_TIME TIMESTAMP */
    public static final String PROP_NAME_createTime = "createTime";
    public static final int PROP_ID_createTime = 15;
    
    /* 修改人: UPDATED_BY VARCHAR */
    public static final String PROP_NAME_updatedBy = "updatedBy";
    public static final int PROP_ID_updatedBy = 16;
    
    /* 修改时间: UPDATE_TIME TIMESTAMP */
    public static final String PROP_NAME_updateTime = "updateTime";
    public static final int PROP_ID_updateTime = 17;
    

    private static int _PROP_ID_BOUND = 18;

    
    /* relation:  */
    public static final String PROP_NAME_ticket = "ticket";
    
    /* relation:  */
    public static final String PROP_NAME_org = "org";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[18];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_orgId] = PROP_NAME_orgId;
          PROP_NAME_TO_ID.put(PROP_NAME_orgId, PROP_ID_orgId);
      
          PROP_ID_TO_NAME[PROP_ID_agentId] = PROP_NAME_agentId;
          PROP_NAME_TO_ID.put(PROP_NAME_agentId, PROP_ID_agentId);
      
          PROP_ID_TO_NAME[PROP_ID_ticketId] = PROP_NAME_ticketId;
          PROP_NAME_TO_ID.put(PROP_NAME_ticketId, PROP_ID_ticketId);
      
          PROP_ID_TO_NAME[PROP_ID_startTime] = PROP_NAME_startTime;
          PROP_NAME_TO_ID.put(PROP_NAME_startTime, PROP_ID_startTime);
      
          PROP_ID_TO_NAME[PROP_ID_stopTime] = PROP_NAME_stopTime;
          PROP_NAME_TO_ID.put(PROP_NAME_stopTime, PROP_ID_stopTime);
      
          PROP_ID_TO_NAME[PROP_ID_pauseStartDateTime] = PROP_NAME_pauseStartDateTime;
          PROP_NAME_TO_ID.put(PROP_NAME_pauseStartDateTime, PROP_ID_pauseStartDateTime);
      
          PROP_ID_TO_NAME[PROP_ID_cumulativePauseMinutes] = PROP_NAME_cumulativePauseMinutes;
          PROP_NAME_TO_ID.put(PROP_NAME_cumulativePauseMinutes, PROP_ID_cumulativePauseMinutes);
      
          PROP_ID_TO_NAME[PROP_ID_pauseReason] = PROP_NAME_pauseReason;
          PROP_NAME_TO_ID.put(PROP_NAME_pauseReason, PROP_ID_pauseReason);
      
          PROP_ID_TO_NAME[PROP_ID_status] = PROP_NAME_status;
          PROP_NAME_TO_ID.put(PROP_NAME_status, PROP_ID_status);
      
          PROP_ID_TO_NAME[PROP_ID_activeFlag] = PROP_NAME_activeFlag;
          PROP_NAME_TO_ID.put(PROP_NAME_activeFlag, PROP_ID_activeFlag);
      
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
    private java.lang.String _id;
    
    /* 业务组织: ORG_ID */
    private java.lang.String _orgId;
    
    /* 客服: AGENT_ID */
    private java.lang.String _agentId;
    
    /* 关联工单: TICKET_ID */
    private java.lang.String _ticketId;
    
    /* 开始时间: START_TIME */
    private java.sql.Timestamp _startTime;
    
    /* 停止时间: STOP_TIME */
    private java.sql.Timestamp _stopTime;
    
    /* 暂停开始时间: PAUSE_START_DATE_TIME */
    private java.sql.Timestamp _pauseStartDateTime;
    
    /* 累计暂停时长(分钟): CUMULATIVE_PAUSE_MINUTES */
    private java.lang.Integer _cumulativePauseMinutes;
    
    /* 暂停原因: PAUSE_REASON */
    private java.lang.String _pauseReason;
    
    /* 会话状态: STATUS */
    private java.lang.String _status;
    
    /* 进行中标记: ACTIVE_FLAG */
    private java.lang.String _activeFlag;
    
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
    

    public _ErpCsTicketTimerSession(){
        // for debug
    }

    protected ErpCsTicketTimerSession newInstance(){
        ErpCsTicketTimerSession entity = new ErpCsTicketTimerSession();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpCsTicketTimerSession cloneInstance() {
        ErpCsTicketTimerSession entity = newInstance();
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
      return "app.erp.cs.dao.entity.ErpCsTicketTimerSession";
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
        
            case PROP_ID_agentId:
               return getAgentId();
        
            case PROP_ID_ticketId:
               return getTicketId();
        
            case PROP_ID_startTime:
               return getStartTime();
        
            case PROP_ID_stopTime:
               return getStopTime();
        
            case PROP_ID_pauseStartDateTime:
               return getPauseStartDateTime();
        
            case PROP_ID_cumulativePauseMinutes:
               return getCumulativePauseMinutes();
        
            case PROP_ID_pauseReason:
               return getPauseReason();
        
            case PROP_ID_status:
               return getStatus();
        
            case PROP_ID_activeFlag:
               return getActiveFlag();
        
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
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_id));
               }
               setId(typedValue);
               break;
            }
        
            case PROP_ID_orgId:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_orgId));
               }
               setOrgId(typedValue);
               break;
            }
        
            case PROP_ID_agentId:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_agentId));
               }
               setAgentId(typedValue);
               break;
            }
        
            case PROP_ID_ticketId:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_ticketId));
               }
               setTicketId(typedValue);
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
        
            case PROP_ID_stopTime:{
               java.sql.Timestamp typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toTimestamp(value,
                       err-> newTypeConversionError(PROP_NAME_stopTime));
               }
               setStopTime(typedValue);
               break;
            }
        
            case PROP_ID_pauseStartDateTime:{
               java.sql.Timestamp typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toTimestamp(value,
                       err-> newTypeConversionError(PROP_NAME_pauseStartDateTime));
               }
               setPauseStartDateTime(typedValue);
               break;
            }
        
            case PROP_ID_cumulativePauseMinutes:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_cumulativePauseMinutes));
               }
               setCumulativePauseMinutes(typedValue);
               break;
            }
        
            case PROP_ID_pauseReason:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_pauseReason));
               }
               setPauseReason(typedValue);
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
        
            case PROP_ID_activeFlag:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_activeFlag));
               }
               setActiveFlag(typedValue);
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
               this._id = (java.lang.String)value;
               orm_id(); // 如果是设置主键字段，则触发watcher
               break;
            }
        
            case PROP_ID_orgId:{
               onInitProp(propId);
               this._orgId = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_agentId:{
               onInitProp(propId);
               this._agentId = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_ticketId:{
               onInitProp(propId);
               this._ticketId = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_startTime:{
               onInitProp(propId);
               this._startTime = (java.sql.Timestamp)value;
               
               break;
            }
        
            case PROP_ID_stopTime:{
               onInitProp(propId);
               this._stopTime = (java.sql.Timestamp)value;
               
               break;
            }
        
            case PROP_ID_pauseStartDateTime:{
               onInitProp(propId);
               this._pauseStartDateTime = (java.sql.Timestamp)value;
               
               break;
            }
        
            case PROP_ID_cumulativePauseMinutes:{
               onInitProp(propId);
               this._cumulativePauseMinutes = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_pauseReason:{
               onInitProp(propId);
               this._pauseReason = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_status:{
               onInitProp(propId);
               this._status = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_activeFlag:{
               onInitProp(propId);
               this._activeFlag = (java.lang.String)value;
               
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
    public final java.lang.String getId(){
         onPropGet(PROP_ID_id);
         return _id;
    }

    /**
     * ID: ID
     */
    public final void setId(java.lang.String value){
        if(onPropSet(PROP_ID_id,value)){
            this._id = value;
            internalClearRefs(PROP_ID_id);
            orm_id();
        }
    }
    
    /**
     * 业务组织: ORG_ID
     */
    public final java.lang.String getOrgId(){
         onPropGet(PROP_ID_orgId);
         return _orgId;
    }

    /**
     * 业务组织: ORG_ID
     */
    public final void setOrgId(java.lang.String value){
        if(onPropSet(PROP_ID_orgId,value)){
            this._orgId = value;
            internalClearRefs(PROP_ID_orgId);
            
        }
    }
    
    /**
     * 客服: AGENT_ID
     */
    public final java.lang.String getAgentId(){
         onPropGet(PROP_ID_agentId);
         return _agentId;
    }

    /**
     * 客服: AGENT_ID
     */
    public final void setAgentId(java.lang.String value){
        if(onPropSet(PROP_ID_agentId,value)){
            this._agentId = value;
            internalClearRefs(PROP_ID_agentId);
            
        }
    }
    
    /**
     * 关联工单: TICKET_ID
     */
    public final java.lang.String getTicketId(){
         onPropGet(PROP_ID_ticketId);
         return _ticketId;
    }

    /**
     * 关联工单: TICKET_ID
     */
    public final void setTicketId(java.lang.String value){
        if(onPropSet(PROP_ID_ticketId,value)){
            this._ticketId = value;
            internalClearRefs(PROP_ID_ticketId);
            
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
     * 停止时间: STOP_TIME
     */
    public final java.sql.Timestamp getStopTime(){
         onPropGet(PROP_ID_stopTime);
         return _stopTime;
    }

    /**
     * 停止时间: STOP_TIME
     */
    public final void setStopTime(java.sql.Timestamp value){
        if(onPropSet(PROP_ID_stopTime,value)){
            this._stopTime = value;
            internalClearRefs(PROP_ID_stopTime);
            
        }
    }
    
    /**
     * 暂停开始时间: PAUSE_START_DATE_TIME
     */
    public final java.sql.Timestamp getPauseStartDateTime(){
         onPropGet(PROP_ID_pauseStartDateTime);
         return _pauseStartDateTime;
    }

    /**
     * 暂停开始时间: PAUSE_START_DATE_TIME
     */
    public final void setPauseStartDateTime(java.sql.Timestamp value){
        if(onPropSet(PROP_ID_pauseStartDateTime,value)){
            this._pauseStartDateTime = value;
            internalClearRefs(PROP_ID_pauseStartDateTime);
            
        }
    }
    
    /**
     * 累计暂停时长(分钟): CUMULATIVE_PAUSE_MINUTES
     */
    public final java.lang.Integer getCumulativePauseMinutes(){
         onPropGet(PROP_ID_cumulativePauseMinutes);
         return _cumulativePauseMinutes;
    }

    /**
     * 累计暂停时长(分钟): CUMULATIVE_PAUSE_MINUTES
     */
    public final void setCumulativePauseMinutes(java.lang.Integer value){
        if(onPropSet(PROP_ID_cumulativePauseMinutes,value)){
            this._cumulativePauseMinutes = value;
            internalClearRefs(PROP_ID_cumulativePauseMinutes);
            
        }
    }
    
    /**
     * 暂停原因: PAUSE_REASON
     */
    public final java.lang.String getPauseReason(){
         onPropGet(PROP_ID_pauseReason);
         return _pauseReason;
    }

    /**
     * 暂停原因: PAUSE_REASON
     */
    public final void setPauseReason(java.lang.String value){
        if(onPropSet(PROP_ID_pauseReason,value)){
            this._pauseReason = value;
            internalClearRefs(PROP_ID_pauseReason);
            
        }
    }
    
    /**
     * 会话状态: STATUS
     */
    public final java.lang.String getStatus(){
         onPropGet(PROP_ID_status);
         return _status;
    }

    /**
     * 会话状态: STATUS
     */
    public final void setStatus(java.lang.String value){
        if(onPropSet(PROP_ID_status,value)){
            this._status = value;
            internalClearRefs(PROP_ID_status);
            
        }
    }
    
    /**
     * 进行中标记: ACTIVE_FLAG
     */
    public final java.lang.String getActiveFlag(){
         onPropGet(PROP_ID_activeFlag);
         return _activeFlag;
    }

    /**
     * 进行中标记: ACTIVE_FLAG
     */
    public final void setActiveFlag(java.lang.String value){
        if(onPropSet(PROP_ID_activeFlag,value)){
            this._activeFlag = value;
            internalClearRefs(PROP_ID_activeFlag);
            
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

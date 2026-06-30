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

import app.erp.cs.dao.entity.ErpCsSlaPolicy;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  SLA 策略: erp_cs_sla_policy
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpCsSlaPolicy extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 编码: CODE VARCHAR */
    public static final String PROP_NAME_code = "code";
    public static final int PROP_ID_code = 2;
    
    /* 名称: NAME VARCHAR */
    public static final String PROP_NAME_name = "name";
    public static final int PROP_ID_name = 3;
    
    /* 适用工单类型: TICKET_TYPE_ID BIGINT */
    public static final String PROP_NAME_ticketTypeId = "ticketTypeId";
    public static final int PROP_ID_ticketTypeId = 4;
    
    /* 最低触发优先级: MIN_PRIORITY INTEGER */
    public static final String PROP_NAME_minPriority = "minPriority";
    public static final int PROP_ID_minPriority = 5;
    
    /* 适用团队: TEAM_ID BIGINT */
    public static final String PROP_NAME_teamId = "teamId";
    public static final int PROP_ID_teamId = 6;
    
    /* 解决时限(小时): RESOLVE_HOURS INTEGER */
    public static final String PROP_NAME_resolveHours = "resolveHours";
    public static final int PROP_ID_resolveHours = 7;
    
    /* 解决时限(天): RESOLVE_DAYS INTEGER */
    public static final String PROP_NAME_resolveDays = "resolveDays";
    public static final int PROP_ID_resolveDays = 8;
    
    /* 仅计算工作日: IS_WORKING_DAYS BOOLEAN */
    public static final String PROP_NAME_isWorkingDays = "isWorkingDays";
    public static final int PROP_ID_isWorkingDays = 9;
    
    /* 升级通知人: ESCALATION_USER_ID BIGINT */
    public static final String PROP_NAME_escalationUserId = "escalationUserId";
    public static final int PROP_ID_escalationUserId = 10;
    
    /* 说明: DESCRIPTION VARCHAR */
    public static final String PROP_NAME_description = "description";
    public static final int PROP_ID_description = 11;
    
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
    public static final String PROP_NAME_ticketType = "ticketType";
    
    /* relation:  */
    public static final String PROP_NAME_team = "team";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[18];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_code] = PROP_NAME_code;
          PROP_NAME_TO_ID.put(PROP_NAME_code, PROP_ID_code);
      
          PROP_ID_TO_NAME[PROP_ID_name] = PROP_NAME_name;
          PROP_NAME_TO_ID.put(PROP_NAME_name, PROP_ID_name);
      
          PROP_ID_TO_NAME[PROP_ID_ticketTypeId] = PROP_NAME_ticketTypeId;
          PROP_NAME_TO_ID.put(PROP_NAME_ticketTypeId, PROP_ID_ticketTypeId);
      
          PROP_ID_TO_NAME[PROP_ID_minPriority] = PROP_NAME_minPriority;
          PROP_NAME_TO_ID.put(PROP_NAME_minPriority, PROP_ID_minPriority);
      
          PROP_ID_TO_NAME[PROP_ID_teamId] = PROP_NAME_teamId;
          PROP_NAME_TO_ID.put(PROP_NAME_teamId, PROP_ID_teamId);
      
          PROP_ID_TO_NAME[PROP_ID_resolveHours] = PROP_NAME_resolveHours;
          PROP_NAME_TO_ID.put(PROP_NAME_resolveHours, PROP_ID_resolveHours);
      
          PROP_ID_TO_NAME[PROP_ID_resolveDays] = PROP_NAME_resolveDays;
          PROP_NAME_TO_ID.put(PROP_NAME_resolveDays, PROP_ID_resolveDays);
      
          PROP_ID_TO_NAME[PROP_ID_isWorkingDays] = PROP_NAME_isWorkingDays;
          PROP_NAME_TO_ID.put(PROP_NAME_isWorkingDays, PROP_ID_isWorkingDays);
      
          PROP_ID_TO_NAME[PROP_ID_escalationUserId] = PROP_NAME_escalationUserId;
          PROP_NAME_TO_ID.put(PROP_NAME_escalationUserId, PROP_ID_escalationUserId);
      
          PROP_ID_TO_NAME[PROP_ID_description] = PROP_NAME_description;
          PROP_NAME_TO_ID.put(PROP_NAME_description, PROP_ID_description);
      
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
    
    /* 名称: NAME */
    private java.lang.String _name;
    
    /* 适用工单类型: TICKET_TYPE_ID */
    private java.lang.Long _ticketTypeId;
    
    /* 最低触发优先级: MIN_PRIORITY */
    private java.lang.Integer _minPriority;
    
    /* 适用团队: TEAM_ID */
    private java.lang.Long _teamId;
    
    /* 解决时限(小时): RESOLVE_HOURS */
    private java.lang.Integer _resolveHours;
    
    /* 解决时限(天): RESOLVE_DAYS */
    private java.lang.Integer _resolveDays;
    
    /* 仅计算工作日: IS_WORKING_DAYS */
    private java.lang.Boolean _isWorkingDays;
    
    /* 升级通知人: ESCALATION_USER_ID */
    private java.lang.Long _escalationUserId;
    
    /* 说明: DESCRIPTION */
    private java.lang.String _description;
    
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
    

    public _ErpCsSlaPolicy(){
        // for debug
    }

    protected ErpCsSlaPolicy newInstance(){
        ErpCsSlaPolicy entity = new ErpCsSlaPolicy();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpCsSlaPolicy cloneInstance() {
        ErpCsSlaPolicy entity = newInstance();
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
      return "app.erp.cs.dao.entity.ErpCsSlaPolicy";
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
        
            case PROP_ID_name:
               return getName();
        
            case PROP_ID_ticketTypeId:
               return getTicketTypeId();
        
            case PROP_ID_minPriority:
               return getMinPriority();
        
            case PROP_ID_teamId:
               return getTeamId();
        
            case PROP_ID_resolveHours:
               return getResolveHours();
        
            case PROP_ID_resolveDays:
               return getResolveDays();
        
            case PROP_ID_isWorkingDays:
               return getIsWorkingDays();
        
            case PROP_ID_escalationUserId:
               return getEscalationUserId();
        
            case PROP_ID_description:
               return getDescription();
        
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
        
            case PROP_ID_name:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_name));
               }
               setName(typedValue);
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
        
            case PROP_ID_minPriority:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_minPriority));
               }
               setMinPriority(typedValue);
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
        
            case PROP_ID_resolveHours:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_resolveHours));
               }
               setResolveHours(typedValue);
               break;
            }
        
            case PROP_ID_resolveDays:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_resolveDays));
               }
               setResolveDays(typedValue);
               break;
            }
        
            case PROP_ID_isWorkingDays:{
               java.lang.Boolean typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBoolean(value,
                       err-> newTypeConversionError(PROP_NAME_isWorkingDays));
               }
               setIsWorkingDays(typedValue);
               break;
            }
        
            case PROP_ID_escalationUserId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_escalationUserId));
               }
               setEscalationUserId(typedValue);
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
        
            case PROP_ID_name:{
               onInitProp(propId);
               this._name = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_ticketTypeId:{
               onInitProp(propId);
               this._ticketTypeId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_minPriority:{
               onInitProp(propId);
               this._minPriority = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_teamId:{
               onInitProp(propId);
               this._teamId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_resolveHours:{
               onInitProp(propId);
               this._resolveHours = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_resolveDays:{
               onInitProp(propId);
               this._resolveDays = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_isWorkingDays:{
               onInitProp(propId);
               this._isWorkingDays = (java.lang.Boolean)value;
               
               break;
            }
        
            case PROP_ID_escalationUserId:{
               onInitProp(propId);
               this._escalationUserId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_description:{
               onInitProp(propId);
               this._description = (java.lang.String)value;
               
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
     * 名称: NAME
     */
    public final java.lang.String getName(){
         onPropGet(PROP_ID_name);
         return _name;
    }

    /**
     * 名称: NAME
     */
    public final void setName(java.lang.String value){
        if(onPropSet(PROP_ID_name,value)){
            this._name = value;
            internalClearRefs(PROP_ID_name);
            
        }
    }
    
    /**
     * 适用工单类型: TICKET_TYPE_ID
     */
    public final java.lang.Long getTicketTypeId(){
         onPropGet(PROP_ID_ticketTypeId);
         return _ticketTypeId;
    }

    /**
     * 适用工单类型: TICKET_TYPE_ID
     */
    public final void setTicketTypeId(java.lang.Long value){
        if(onPropSet(PROP_ID_ticketTypeId,value)){
            this._ticketTypeId = value;
            internalClearRefs(PROP_ID_ticketTypeId);
            
        }
    }
    
    /**
     * 最低触发优先级: MIN_PRIORITY
     */
    public final java.lang.Integer getMinPriority(){
         onPropGet(PROP_ID_minPriority);
         return _minPriority;
    }

    /**
     * 最低触发优先级: MIN_PRIORITY
     */
    public final void setMinPriority(java.lang.Integer value){
        if(onPropSet(PROP_ID_minPriority,value)){
            this._minPriority = value;
            internalClearRefs(PROP_ID_minPriority);
            
        }
    }
    
    /**
     * 适用团队: TEAM_ID
     */
    public final java.lang.Long getTeamId(){
         onPropGet(PROP_ID_teamId);
         return _teamId;
    }

    /**
     * 适用团队: TEAM_ID
     */
    public final void setTeamId(java.lang.Long value){
        if(onPropSet(PROP_ID_teamId,value)){
            this._teamId = value;
            internalClearRefs(PROP_ID_teamId);
            
        }
    }
    
    /**
     * 解决时限(小时): RESOLVE_HOURS
     */
    public final java.lang.Integer getResolveHours(){
         onPropGet(PROP_ID_resolveHours);
         return _resolveHours;
    }

    /**
     * 解决时限(小时): RESOLVE_HOURS
     */
    public final void setResolveHours(java.lang.Integer value){
        if(onPropSet(PROP_ID_resolveHours,value)){
            this._resolveHours = value;
            internalClearRefs(PROP_ID_resolveHours);
            
        }
    }
    
    /**
     * 解决时限(天): RESOLVE_DAYS
     */
    public final java.lang.Integer getResolveDays(){
         onPropGet(PROP_ID_resolveDays);
         return _resolveDays;
    }

    /**
     * 解决时限(天): RESOLVE_DAYS
     */
    public final void setResolveDays(java.lang.Integer value){
        if(onPropSet(PROP_ID_resolveDays,value)){
            this._resolveDays = value;
            internalClearRefs(PROP_ID_resolveDays);
            
        }
    }
    
    /**
     * 仅计算工作日: IS_WORKING_DAYS
     */
    public final java.lang.Boolean getIsWorkingDays(){
         onPropGet(PROP_ID_isWorkingDays);
         return _isWorkingDays;
    }

    /**
     * 仅计算工作日: IS_WORKING_DAYS
     */
    public final void setIsWorkingDays(java.lang.Boolean value){
        if(onPropSet(PROP_ID_isWorkingDays,value)){
            this._isWorkingDays = value;
            internalClearRefs(PROP_ID_isWorkingDays);
            
        }
    }
    
    /**
     * 升级通知人: ESCALATION_USER_ID
     */
    public final java.lang.Long getEscalationUserId(){
         onPropGet(PROP_ID_escalationUserId);
         return _escalationUserId;
    }

    /**
     * 升级通知人: ESCALATION_USER_ID
     */
    public final void setEscalationUserId(java.lang.Long value){
        if(onPropSet(PROP_ID_escalationUserId,value)){
            this._escalationUserId = value;
            internalClearRefs(PROP_ID_escalationUserId);
            
        }
    }
    
    /**
     * 说明: DESCRIPTION
     */
    public final java.lang.String getDescription(){
         onPropGet(PROP_ID_description);
         return _description;
    }

    /**
     * 说明: DESCRIPTION
     */
    public final void setDescription(java.lang.String value){
        if(onPropSet(PROP_ID_description,value)){
            this._description = value;
            internalClearRefs(PROP_ID_description);
            
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
    public final app.erp.cs.dao.entity.ErpCsTeam getTeam(){
       return (app.erp.cs.dao.entity.ErpCsTeam)internalGetRefEntity(PROP_NAME_team);
    }

    public final void setTeam(app.erp.cs.dao.entity.ErpCsTeam refEntity){
   
           if(refEntity == null){
           
                   this.setTeamId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_team, refEntity,()->{
           
                           this.setTeamId(refEntity.getId());
                       
           });
           }
       
    }
       
}
// resume CPD analysis - CPD-ON

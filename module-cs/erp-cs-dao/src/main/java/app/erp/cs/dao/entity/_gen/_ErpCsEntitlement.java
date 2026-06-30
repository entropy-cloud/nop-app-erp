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

import app.erp.cs.dao.entity.ErpCsEntitlement;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  服务权益: erp_cs_entitlement
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpCsEntitlement extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 编码: CODE VARCHAR */
    public static final String PROP_NAME_code = "code";
    public static final int PROP_ID_code = 2;
    
    /* 业务组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 3;
    
    /* 客户: PARTNER_ID BIGINT */
    public static final String PROP_NAME_partnerId = "partnerId";
    public static final int PROP_ID_partnerId = 4;
    
    /* 支持合同: CONTRACT_ID BIGINT */
    public static final String PROP_NAME_contractId = "contractId";
    public static final int PROP_ID_contractId = 5;
    
    /* SLA策略: SLA_POLICY_ID BIGINT */
    public static final String PROP_NAME_slaPolicyId = "slaPolicyId";
    public static final int PROP_ID_slaPolicyId = 6;
    
    /* 服务类型: SERVICE_TYPE INTEGER */
    public static final String PROP_NAME_serviceType = "serviceType";
    public static final int PROP_ID_serviceType = 7;
    
    /* 生效日期: START_DATE DATE */
    public static final String PROP_NAME_startDate = "startDate";
    public static final int PROP_ID_startDate = 8;
    
    /* 失效日期: END_DATE DATE */
    public static final String PROP_NAME_endDate = "endDate";
    public static final int PROP_ID_endDate = 9;
    
    /* 最大工单数: MAX_TICKETS INTEGER */
    public static final String PROP_NAME_maxTickets = "maxTickets";
    public static final int PROP_ID_maxTickets = 10;
    
    /* 已用工单数: USED_TICKETS INTEGER */
    public static final String PROP_NAME_usedTickets = "usedTickets";
    public static final int PROP_ID_usedTickets = 11;
    
    /* 承诺响应时限(分钟): MAX_RESPONSE_TIME INTEGER */
    public static final String PROP_NAME_maxResponseTime = "maxResponseTime";
    public static final int PROP_ID_maxResponseTime = 12;
    
    /* 承诺解决时限(分钟): MAX_RESOLUTION_TIME INTEGER */
    public static final String PROP_NAME_maxResolutionTime = "maxResolutionTime";
    public static final int PROP_ID_maxResolutionTime = 13;
    
    /* 是否启用: IS_ACTIVE BOOLEAN */
    public static final String PROP_NAME_isActive = "isActive";
    public static final int PROP_ID_isActive = 14;
    
    /* 备注: NOTES VARCHAR */
    public static final String PROP_NAME_notes = "notes";
    public static final int PROP_ID_notes = 15;
    
    /* 逻辑删除版本: DEL_VERSION BIGINT */
    public static final String PROP_NAME_delVersion = "delVersion";
    public static final int PROP_ID_delVersion = 16;
    
    /* 数据版本: VERSION INTEGER */
    public static final String PROP_NAME_version = "version";
    public static final int PROP_ID_version = 17;
    
    /* 创建人: CREATED_BY VARCHAR */
    public static final String PROP_NAME_createdBy = "createdBy";
    public static final int PROP_ID_createdBy = 18;
    
    /* 创建时间: CREATE_TIME TIMESTAMP */
    public static final String PROP_NAME_createTime = "createTime";
    public static final int PROP_ID_createTime = 19;
    
    /* 修改人: UPDATED_BY VARCHAR */
    public static final String PROP_NAME_updatedBy = "updatedBy";
    public static final int PROP_ID_updatedBy = 20;
    
    /* 修改时间: UPDATE_TIME TIMESTAMP */
    public static final String PROP_NAME_updateTime = "updateTime";
    public static final int PROP_ID_updateTime = 21;
    

    private static int _PROP_ID_BOUND = 22;

    
    /* relation:  */
    public static final String PROP_NAME_contract = "contract";
    
    /* relation:  */
    public static final String PROP_NAME_slaPolicy = "slaPolicy";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[22];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_code] = PROP_NAME_code;
          PROP_NAME_TO_ID.put(PROP_NAME_code, PROP_ID_code);
      
          PROP_ID_TO_NAME[PROP_ID_orgId] = PROP_NAME_orgId;
          PROP_NAME_TO_ID.put(PROP_NAME_orgId, PROP_ID_orgId);
      
          PROP_ID_TO_NAME[PROP_ID_partnerId] = PROP_NAME_partnerId;
          PROP_NAME_TO_ID.put(PROP_NAME_partnerId, PROP_ID_partnerId);
      
          PROP_ID_TO_NAME[PROP_ID_contractId] = PROP_NAME_contractId;
          PROP_NAME_TO_ID.put(PROP_NAME_contractId, PROP_ID_contractId);
      
          PROP_ID_TO_NAME[PROP_ID_slaPolicyId] = PROP_NAME_slaPolicyId;
          PROP_NAME_TO_ID.put(PROP_NAME_slaPolicyId, PROP_ID_slaPolicyId);
      
          PROP_ID_TO_NAME[PROP_ID_serviceType] = PROP_NAME_serviceType;
          PROP_NAME_TO_ID.put(PROP_NAME_serviceType, PROP_ID_serviceType);
      
          PROP_ID_TO_NAME[PROP_ID_startDate] = PROP_NAME_startDate;
          PROP_NAME_TO_ID.put(PROP_NAME_startDate, PROP_ID_startDate);
      
          PROP_ID_TO_NAME[PROP_ID_endDate] = PROP_NAME_endDate;
          PROP_NAME_TO_ID.put(PROP_NAME_endDate, PROP_ID_endDate);
      
          PROP_ID_TO_NAME[PROP_ID_maxTickets] = PROP_NAME_maxTickets;
          PROP_NAME_TO_ID.put(PROP_NAME_maxTickets, PROP_ID_maxTickets);
      
          PROP_ID_TO_NAME[PROP_ID_usedTickets] = PROP_NAME_usedTickets;
          PROP_NAME_TO_ID.put(PROP_NAME_usedTickets, PROP_ID_usedTickets);
      
          PROP_ID_TO_NAME[PROP_ID_maxResponseTime] = PROP_NAME_maxResponseTime;
          PROP_NAME_TO_ID.put(PROP_NAME_maxResponseTime, PROP_ID_maxResponseTime);
      
          PROP_ID_TO_NAME[PROP_ID_maxResolutionTime] = PROP_NAME_maxResolutionTime;
          PROP_NAME_TO_ID.put(PROP_NAME_maxResolutionTime, PROP_ID_maxResolutionTime);
      
          PROP_ID_TO_NAME[PROP_ID_isActive] = PROP_NAME_isActive;
          PROP_NAME_TO_ID.put(PROP_NAME_isActive, PROP_ID_isActive);
      
          PROP_ID_TO_NAME[PROP_ID_notes] = PROP_NAME_notes;
          PROP_NAME_TO_ID.put(PROP_NAME_notes, PROP_ID_notes);
      
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
    
    /* 客户: PARTNER_ID */
    private java.lang.Long _partnerId;
    
    /* 支持合同: CONTRACT_ID */
    private java.lang.Long _contractId;
    
    /* SLA策略: SLA_POLICY_ID */
    private java.lang.Long _slaPolicyId;
    
    /* 服务类型: SERVICE_TYPE */
    private java.lang.Integer _serviceType;
    
    /* 生效日期: START_DATE */
    private java.time.LocalDate _startDate;
    
    /* 失效日期: END_DATE */
    private java.time.LocalDate _endDate;
    
    /* 最大工单数: MAX_TICKETS */
    private java.lang.Integer _maxTickets;
    
    /* 已用工单数: USED_TICKETS */
    private java.lang.Integer _usedTickets;
    
    /* 承诺响应时限(分钟): MAX_RESPONSE_TIME */
    private java.lang.Integer _maxResponseTime;
    
    /* 承诺解决时限(分钟): MAX_RESOLUTION_TIME */
    private java.lang.Integer _maxResolutionTime;
    
    /* 是否启用: IS_ACTIVE */
    private java.lang.Boolean _isActive;
    
    /* 备注: NOTES */
    private java.lang.String _notes;
    
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
    

    public _ErpCsEntitlement(){
        // for debug
    }

    protected ErpCsEntitlement newInstance(){
        ErpCsEntitlement entity = new ErpCsEntitlement();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpCsEntitlement cloneInstance() {
        ErpCsEntitlement entity = newInstance();
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
      return "app.erp.cs.dao.entity.ErpCsEntitlement";
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
        
            case PROP_ID_partnerId:
               return getPartnerId();
        
            case PROP_ID_contractId:
               return getContractId();
        
            case PROP_ID_slaPolicyId:
               return getSlaPolicyId();
        
            case PROP_ID_serviceType:
               return getServiceType();
        
            case PROP_ID_startDate:
               return getStartDate();
        
            case PROP_ID_endDate:
               return getEndDate();
        
            case PROP_ID_maxTickets:
               return getMaxTickets();
        
            case PROP_ID_usedTickets:
               return getUsedTickets();
        
            case PROP_ID_maxResponseTime:
               return getMaxResponseTime();
        
            case PROP_ID_maxResolutionTime:
               return getMaxResolutionTime();
        
            case PROP_ID_isActive:
               return getIsActive();
        
            case PROP_ID_notes:
               return getNotes();
        
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
        
            case PROP_ID_partnerId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_partnerId));
               }
               setPartnerId(typedValue);
               break;
            }
        
            case PROP_ID_contractId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_contractId));
               }
               setContractId(typedValue);
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
        
            case PROP_ID_serviceType:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_serviceType));
               }
               setServiceType(typedValue);
               break;
            }
        
            case PROP_ID_startDate:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_startDate));
               }
               setStartDate(typedValue);
               break;
            }
        
            case PROP_ID_endDate:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_endDate));
               }
               setEndDate(typedValue);
               break;
            }
        
            case PROP_ID_maxTickets:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_maxTickets));
               }
               setMaxTickets(typedValue);
               break;
            }
        
            case PROP_ID_usedTickets:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_usedTickets));
               }
               setUsedTickets(typedValue);
               break;
            }
        
            case PROP_ID_maxResponseTime:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_maxResponseTime));
               }
               setMaxResponseTime(typedValue);
               break;
            }
        
            case PROP_ID_maxResolutionTime:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_maxResolutionTime));
               }
               setMaxResolutionTime(typedValue);
               break;
            }
        
            case PROP_ID_isActive:{
               java.lang.Boolean typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBoolean(value,
                       err-> newTypeConversionError(PROP_NAME_isActive));
               }
               setIsActive(typedValue);
               break;
            }
        
            case PROP_ID_notes:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_notes));
               }
               setNotes(typedValue);
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
        
            case PROP_ID_partnerId:{
               onInitProp(propId);
               this._partnerId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_contractId:{
               onInitProp(propId);
               this._contractId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_slaPolicyId:{
               onInitProp(propId);
               this._slaPolicyId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_serviceType:{
               onInitProp(propId);
               this._serviceType = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_startDate:{
               onInitProp(propId);
               this._startDate = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_endDate:{
               onInitProp(propId);
               this._endDate = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_maxTickets:{
               onInitProp(propId);
               this._maxTickets = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_usedTickets:{
               onInitProp(propId);
               this._usedTickets = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_maxResponseTime:{
               onInitProp(propId);
               this._maxResponseTime = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_maxResolutionTime:{
               onInitProp(propId);
               this._maxResolutionTime = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_isActive:{
               onInitProp(propId);
               this._isActive = (java.lang.Boolean)value;
               
               break;
            }
        
            case PROP_ID_notes:{
               onInitProp(propId);
               this._notes = (java.lang.String)value;
               
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
     * 支持合同: CONTRACT_ID
     */
    public final java.lang.Long getContractId(){
         onPropGet(PROP_ID_contractId);
         return _contractId;
    }

    /**
     * 支持合同: CONTRACT_ID
     */
    public final void setContractId(java.lang.Long value){
        if(onPropSet(PROP_ID_contractId,value)){
            this._contractId = value;
            internalClearRefs(PROP_ID_contractId);
            
        }
    }
    
    /**
     * SLA策略: SLA_POLICY_ID
     */
    public final java.lang.Long getSlaPolicyId(){
         onPropGet(PROP_ID_slaPolicyId);
         return _slaPolicyId;
    }

    /**
     * SLA策略: SLA_POLICY_ID
     */
    public final void setSlaPolicyId(java.lang.Long value){
        if(onPropSet(PROP_ID_slaPolicyId,value)){
            this._slaPolicyId = value;
            internalClearRefs(PROP_ID_slaPolicyId);
            
        }
    }
    
    /**
     * 服务类型: SERVICE_TYPE
     */
    public final java.lang.Integer getServiceType(){
         onPropGet(PROP_ID_serviceType);
         return _serviceType;
    }

    /**
     * 服务类型: SERVICE_TYPE
     */
    public final void setServiceType(java.lang.Integer value){
        if(onPropSet(PROP_ID_serviceType,value)){
            this._serviceType = value;
            internalClearRefs(PROP_ID_serviceType);
            
        }
    }
    
    /**
     * 生效日期: START_DATE
     */
    public final java.time.LocalDate getStartDate(){
         onPropGet(PROP_ID_startDate);
         return _startDate;
    }

    /**
     * 生效日期: START_DATE
     */
    public final void setStartDate(java.time.LocalDate value){
        if(onPropSet(PROP_ID_startDate,value)){
            this._startDate = value;
            internalClearRefs(PROP_ID_startDate);
            
        }
    }
    
    /**
     * 失效日期: END_DATE
     */
    public final java.time.LocalDate getEndDate(){
         onPropGet(PROP_ID_endDate);
         return _endDate;
    }

    /**
     * 失效日期: END_DATE
     */
    public final void setEndDate(java.time.LocalDate value){
        if(onPropSet(PROP_ID_endDate,value)){
            this._endDate = value;
            internalClearRefs(PROP_ID_endDate);
            
        }
    }
    
    /**
     * 最大工单数: MAX_TICKETS
     */
    public final java.lang.Integer getMaxTickets(){
         onPropGet(PROP_ID_maxTickets);
         return _maxTickets;
    }

    /**
     * 最大工单数: MAX_TICKETS
     */
    public final void setMaxTickets(java.lang.Integer value){
        if(onPropSet(PROP_ID_maxTickets,value)){
            this._maxTickets = value;
            internalClearRefs(PROP_ID_maxTickets);
            
        }
    }
    
    /**
     * 已用工单数: USED_TICKETS
     */
    public final java.lang.Integer getUsedTickets(){
         onPropGet(PROP_ID_usedTickets);
         return _usedTickets;
    }

    /**
     * 已用工单数: USED_TICKETS
     */
    public final void setUsedTickets(java.lang.Integer value){
        if(onPropSet(PROP_ID_usedTickets,value)){
            this._usedTickets = value;
            internalClearRefs(PROP_ID_usedTickets);
            
        }
    }
    
    /**
     * 承诺响应时限(分钟): MAX_RESPONSE_TIME
     */
    public final java.lang.Integer getMaxResponseTime(){
         onPropGet(PROP_ID_maxResponseTime);
         return _maxResponseTime;
    }

    /**
     * 承诺响应时限(分钟): MAX_RESPONSE_TIME
     */
    public final void setMaxResponseTime(java.lang.Integer value){
        if(onPropSet(PROP_ID_maxResponseTime,value)){
            this._maxResponseTime = value;
            internalClearRefs(PROP_ID_maxResponseTime);
            
        }
    }
    
    /**
     * 承诺解决时限(分钟): MAX_RESOLUTION_TIME
     */
    public final java.lang.Integer getMaxResolutionTime(){
         onPropGet(PROP_ID_maxResolutionTime);
         return _maxResolutionTime;
    }

    /**
     * 承诺解决时限(分钟): MAX_RESOLUTION_TIME
     */
    public final void setMaxResolutionTime(java.lang.Integer value){
        if(onPropSet(PROP_ID_maxResolutionTime,value)){
            this._maxResolutionTime = value;
            internalClearRefs(PROP_ID_maxResolutionTime);
            
        }
    }
    
    /**
     * 是否启用: IS_ACTIVE
     */
    public final java.lang.Boolean getIsActive(){
         onPropGet(PROP_ID_isActive);
         return _isActive;
    }

    /**
     * 是否启用: IS_ACTIVE
     */
    public final void setIsActive(java.lang.Boolean value){
        if(onPropSet(PROP_ID_isActive,value)){
            this._isActive = value;
            internalClearRefs(PROP_ID_isActive);
            
        }
    }
    
    /**
     * 备注: NOTES
     */
    public final java.lang.String getNotes(){
         onPropGet(PROP_ID_notes);
         return _notes;
    }

    /**
     * 备注: NOTES
     */
    public final void setNotes(java.lang.String value){
        if(onPropSet(PROP_ID_notes,value)){
            this._notes = value;
            internalClearRefs(PROP_ID_notes);
            
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
    public final app.erp.cs.dao.entity.ErpCsContract getContract(){
       return (app.erp.cs.dao.entity.ErpCsContract)internalGetRefEntity(PROP_NAME_contract);
    }

    public final void setContract(app.erp.cs.dao.entity.ErpCsContract refEntity){
   
           if(refEntity == null){
           
                   this.setContractId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_contract, refEntity,()->{
           
                           this.setContractId(refEntity.getId());
                       
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
       
}
// resume CPD analysis - CPD-ON

package app.erp.log.dao.entity._gen;

import io.nop.orm.model.IEntityModel;
import io.nop.orm.support.DynamicOrmEntity;
import io.nop.orm.support.OrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code
import io.nop.orm.IOrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code

import io.nop.api.core.convert.ConvertHelper;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;

import app.erp.log.dao.entity.ErpLogCarrier;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  承运商: erp_log_carrier
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpLogCarrier extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 编码: CODE VARCHAR */
    public static final String PROP_NAME_code = "code";
    public static final int PROP_ID_code = 2;
    
    /* 业务组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 3;
    
    /* 承运商名称: CARRIER_NAME VARCHAR */
    public static final String PROP_NAME_carrierName = "carrierName";
    public static final int PROP_ID_carrierName = 4;
    
    /* 承运商类型: CARRIER_TYPE INTEGER */
    public static final String PROP_NAME_carrierType = "carrierType";
    public static final int PROP_ID_carrierType = 5;
    
    /* 网关标识: GATEWAY_ID VARCHAR */
    public static final String PROP_NAME_gatewayId = "gatewayId";
    public static final int PROP_ID_gatewayId = 6;
    
    /* 承运商往来单位ID: PARTNER_ID BIGINT */
    public static final String PROP_NAME_partnerId = "partnerId";
    public static final int PROP_ID_partnerId = 7;
    
    /* 是否启用: IS_ACTIVE INTEGER */
    public static final String PROP_NAME_isActive = "isActive";
    public static final int PROP_ID_isActive = 8;
    
    /* 追踪URL模板: TRACKING_URL_TEMPLATE VARCHAR */
    public static final String PROP_NAME_trackingUrlTemplate = "trackingUrlTemplate";
    public static final int PROP_ID_trackingUrlTemplate = 9;
    
    /* 最大包裹重量(kg): MAX_PARCEL_WEIGHT DECIMAL */
    public static final String PROP_NAME_maxParcelWeight = "maxParcelWeight";
    public static final int PROP_ID_maxParcelWeight = 10;
    
    /* 支持服务类型: SUPPORTED_SERVICE_TYPES VARCHAR */
    public static final String PROP_NAME_supportedServiceTypes = "supportedServiceTypes";
    public static final int PROP_ID_supportedServiceTypes = 11;
    
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
    public static final String PROP_NAME_configs = "configs";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[19];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_code] = PROP_NAME_code;
          PROP_NAME_TO_ID.put(PROP_NAME_code, PROP_ID_code);
      
          PROP_ID_TO_NAME[PROP_ID_orgId] = PROP_NAME_orgId;
          PROP_NAME_TO_ID.put(PROP_NAME_orgId, PROP_ID_orgId);
      
          PROP_ID_TO_NAME[PROP_ID_carrierName] = PROP_NAME_carrierName;
          PROP_NAME_TO_ID.put(PROP_NAME_carrierName, PROP_ID_carrierName);
      
          PROP_ID_TO_NAME[PROP_ID_carrierType] = PROP_NAME_carrierType;
          PROP_NAME_TO_ID.put(PROP_NAME_carrierType, PROP_ID_carrierType);
      
          PROP_ID_TO_NAME[PROP_ID_gatewayId] = PROP_NAME_gatewayId;
          PROP_NAME_TO_ID.put(PROP_NAME_gatewayId, PROP_ID_gatewayId);
      
          PROP_ID_TO_NAME[PROP_ID_partnerId] = PROP_NAME_partnerId;
          PROP_NAME_TO_ID.put(PROP_NAME_partnerId, PROP_ID_partnerId);
      
          PROP_ID_TO_NAME[PROP_ID_isActive] = PROP_NAME_isActive;
          PROP_NAME_TO_ID.put(PROP_NAME_isActive, PROP_ID_isActive);
      
          PROP_ID_TO_NAME[PROP_ID_trackingUrlTemplate] = PROP_NAME_trackingUrlTemplate;
          PROP_NAME_TO_ID.put(PROP_NAME_trackingUrlTemplate, PROP_ID_trackingUrlTemplate);
      
          PROP_ID_TO_NAME[PROP_ID_maxParcelWeight] = PROP_NAME_maxParcelWeight;
          PROP_NAME_TO_ID.put(PROP_NAME_maxParcelWeight, PROP_ID_maxParcelWeight);
      
          PROP_ID_TO_NAME[PROP_ID_supportedServiceTypes] = PROP_NAME_supportedServiceTypes;
          PROP_NAME_TO_ID.put(PROP_NAME_supportedServiceTypes, PROP_ID_supportedServiceTypes);
      
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
    
    /* 承运商名称: CARRIER_NAME */
    private java.lang.String _carrierName;
    
    /* 承运商类型: CARRIER_TYPE */
    private java.lang.Integer _carrierType;
    
    /* 网关标识: GATEWAY_ID */
    private java.lang.String _gatewayId;
    
    /* 承运商往来单位ID: PARTNER_ID */
    private java.lang.Long _partnerId;
    
    /* 是否启用: IS_ACTIVE */
    private java.lang.Integer _isActive;
    
    /* 追踪URL模板: TRACKING_URL_TEMPLATE */
    private java.lang.String _trackingUrlTemplate;
    
    /* 最大包裹重量(kg): MAX_PARCEL_WEIGHT */
    private java.lang.String _maxParcelWeight;
    
    /* 支持服务类型: SUPPORTED_SERVICE_TYPES */
    private java.lang.String _supportedServiceTypes;
    
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
    

    public _ErpLogCarrier(){
        // for debug
    }

    protected ErpLogCarrier newInstance(){
        ErpLogCarrier entity = new ErpLogCarrier();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpLogCarrier cloneInstance() {
        ErpLogCarrier entity = newInstance();
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
      return "app.erp.log.dao.entity.ErpLogCarrier";
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
        
            case PROP_ID_carrierName:
               return getCarrierName();
        
            case PROP_ID_carrierType:
               return getCarrierType();
        
            case PROP_ID_gatewayId:
               return getGatewayId();
        
            case PROP_ID_partnerId:
               return getPartnerId();
        
            case PROP_ID_isActive:
               return getIsActive();
        
            case PROP_ID_trackingUrlTemplate:
               return getTrackingUrlTemplate();
        
            case PROP_ID_maxParcelWeight:
               return getMaxParcelWeight();
        
            case PROP_ID_supportedServiceTypes:
               return getSupportedServiceTypes();
        
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
        
            case PROP_ID_carrierName:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_carrierName));
               }
               setCarrierName(typedValue);
               break;
            }
        
            case PROP_ID_carrierType:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_carrierType));
               }
               setCarrierType(typedValue);
               break;
            }
        
            case PROP_ID_gatewayId:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_gatewayId));
               }
               setGatewayId(typedValue);
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
        
            case PROP_ID_isActive:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_isActive));
               }
               setIsActive(typedValue);
               break;
            }
        
            case PROP_ID_trackingUrlTemplate:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_trackingUrlTemplate));
               }
               setTrackingUrlTemplate(typedValue);
               break;
            }
        
            case PROP_ID_maxParcelWeight:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_maxParcelWeight));
               }
               setMaxParcelWeight(typedValue);
               break;
            }
        
            case PROP_ID_supportedServiceTypes:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_supportedServiceTypes));
               }
               setSupportedServiceTypes(typedValue);
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
        
            case PROP_ID_carrierName:{
               onInitProp(propId);
               this._carrierName = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_carrierType:{
               onInitProp(propId);
               this._carrierType = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_gatewayId:{
               onInitProp(propId);
               this._gatewayId = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_partnerId:{
               onInitProp(propId);
               this._partnerId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_isActive:{
               onInitProp(propId);
               this._isActive = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_trackingUrlTemplate:{
               onInitProp(propId);
               this._trackingUrlTemplate = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_maxParcelWeight:{
               onInitProp(propId);
               this._maxParcelWeight = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_supportedServiceTypes:{
               onInitProp(propId);
               this._supportedServiceTypes = (java.lang.String)value;
               
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
     * 承运商名称: CARRIER_NAME
     */
    public final java.lang.String getCarrierName(){
         onPropGet(PROP_ID_carrierName);
         return _carrierName;
    }

    /**
     * 承运商名称: CARRIER_NAME
     */
    public final void setCarrierName(java.lang.String value){
        if(onPropSet(PROP_ID_carrierName,value)){
            this._carrierName = value;
            internalClearRefs(PROP_ID_carrierName);
            
        }
    }
    
    /**
     * 承运商类型: CARRIER_TYPE
     */
    public final java.lang.Integer getCarrierType(){
         onPropGet(PROP_ID_carrierType);
         return _carrierType;
    }

    /**
     * 承运商类型: CARRIER_TYPE
     */
    public final void setCarrierType(java.lang.Integer value){
        if(onPropSet(PROP_ID_carrierType,value)){
            this._carrierType = value;
            internalClearRefs(PROP_ID_carrierType);
            
        }
    }
    
    /**
     * 网关标识: GATEWAY_ID
     */
    public final java.lang.String getGatewayId(){
         onPropGet(PROP_ID_gatewayId);
         return _gatewayId;
    }

    /**
     * 网关标识: GATEWAY_ID
     */
    public final void setGatewayId(java.lang.String value){
        if(onPropSet(PROP_ID_gatewayId,value)){
            this._gatewayId = value;
            internalClearRefs(PROP_ID_gatewayId);
            
        }
    }
    
    /**
     * 承运商往来单位ID: PARTNER_ID
     */
    public final java.lang.Long getPartnerId(){
         onPropGet(PROP_ID_partnerId);
         return _partnerId;
    }

    /**
     * 承运商往来单位ID: PARTNER_ID
     */
    public final void setPartnerId(java.lang.Long value){
        if(onPropSet(PROP_ID_partnerId,value)){
            this._partnerId = value;
            internalClearRefs(PROP_ID_partnerId);
            
        }
    }
    
    /**
     * 是否启用: IS_ACTIVE
     */
    public final java.lang.Integer getIsActive(){
         onPropGet(PROP_ID_isActive);
         return _isActive;
    }

    /**
     * 是否启用: IS_ACTIVE
     */
    public final void setIsActive(java.lang.Integer value){
        if(onPropSet(PROP_ID_isActive,value)){
            this._isActive = value;
            internalClearRefs(PROP_ID_isActive);
            
        }
    }
    
    /**
     * 追踪URL模板: TRACKING_URL_TEMPLATE
     */
    public final java.lang.String getTrackingUrlTemplate(){
         onPropGet(PROP_ID_trackingUrlTemplate);
         return _trackingUrlTemplate;
    }

    /**
     * 追踪URL模板: TRACKING_URL_TEMPLATE
     */
    public final void setTrackingUrlTemplate(java.lang.String value){
        if(onPropSet(PROP_ID_trackingUrlTemplate,value)){
            this._trackingUrlTemplate = value;
            internalClearRefs(PROP_ID_trackingUrlTemplate);
            
        }
    }
    
    /**
     * 最大包裹重量(kg): MAX_PARCEL_WEIGHT
     */
    public final java.lang.String getMaxParcelWeight(){
         onPropGet(PROP_ID_maxParcelWeight);
         return _maxParcelWeight;
    }

    /**
     * 最大包裹重量(kg): MAX_PARCEL_WEIGHT
     */
    public final void setMaxParcelWeight(java.lang.String value){
        if(onPropSet(PROP_ID_maxParcelWeight,value)){
            this._maxParcelWeight = value;
            internalClearRefs(PROP_ID_maxParcelWeight);
            
        }
    }
    
    /**
     * 支持服务类型: SUPPORTED_SERVICE_TYPES
     */
    public final java.lang.String getSupportedServiceTypes(){
         onPropGet(PROP_ID_supportedServiceTypes);
         return _supportedServiceTypes;
    }

    /**
     * 支持服务类型: SUPPORTED_SERVICE_TYPES
     */
    public final void setSupportedServiceTypes(java.lang.String value){
        if(onPropSet(PROP_ID_supportedServiceTypes,value)){
            this._supportedServiceTypes = value;
            internalClearRefs(PROP_ID_supportedServiceTypes);
            
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
    
    private final OrmEntitySet<app.erp.log.dao.entity.ErpLogCarrierConfig> _configs = new OrmEntitySet<>(this, PROP_NAME_configs,
        null, null,app.erp.log.dao.entity.ErpLogCarrierConfig.class);

    /**
     * 。 refPropName: , keyProp: {rel.keyProp}
     */
    public final IOrmEntitySet<app.erp.log.dao.entity.ErpLogCarrierConfig> getConfigs(){
       return _configs;
    }
       
}
// resume CPD analysis - CPD-ON

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

import app.erp.log.dao.entity.ErpLogShipmentParcel;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  包裹: erp_log_shipment_parcel
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpLogShipmentParcel extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 发运单ID: SHIPMENT_ID BIGINT */
    public static final String PROP_NAME_shipmentId = "shipmentId";
    public static final int PROP_ID_shipmentId = 2;
    
    /* 包裹编号: PARCEL_NO VARCHAR */
    public static final String PROP_NAME_parcelNo = "parcelNo";
    public static final int PROP_ID_parcelNo = 3;
    
    /* 运单号: TRACKING_NO VARCHAR */
    public static final String PROP_NAME_trackingNo = "trackingNo";
    public static final int PROP_ID_trackingNo = 4;
    
    /* 面单URL: LABEL_URL VARCHAR */
    public static final String PROP_NAME_labelUrl = "labelUrl";
    public static final int PROP_ID_labelUrl = 5;
    
    /* 重量(kg): WEIGHT DECIMAL */
    public static final String PROP_NAME_weight = "weight";
    public static final int PROP_ID_weight = 6;
    
    /* 长(cm): LENGTH DECIMAL */
    public static final String PROP_NAME_length = "length";
    public static final int PROP_ID_length = 7;
    
    /* 宽(cm): WIDTH DECIMAL */
    public static final String PROP_NAME_width = "width";
    public static final int PROP_ID_width = 8;
    
    /* 高(cm): HEIGHT DECIMAL */
    public static final String PROP_NAME_height = "height";
    public static final int PROP_ID_height = 9;
    
    /* 申报价值: DECLARED_VALUE DECIMAL */
    public static final String PROP_NAME_declaredValue = "declaredValue";
    public static final int PROP_ID_declaredValue = 10;
    
    /* 是否有效: IS_ACTIVE INTEGER */
    public static final String PROP_NAME_isActive = "isActive";
    public static final int PROP_ID_isActive = 11;
    
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
    public static final String PROP_NAME_shipment = "shipment";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[19];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_shipmentId] = PROP_NAME_shipmentId;
          PROP_NAME_TO_ID.put(PROP_NAME_shipmentId, PROP_ID_shipmentId);
      
          PROP_ID_TO_NAME[PROP_ID_parcelNo] = PROP_NAME_parcelNo;
          PROP_NAME_TO_ID.put(PROP_NAME_parcelNo, PROP_ID_parcelNo);
      
          PROP_ID_TO_NAME[PROP_ID_trackingNo] = PROP_NAME_trackingNo;
          PROP_NAME_TO_ID.put(PROP_NAME_trackingNo, PROP_ID_trackingNo);
      
          PROP_ID_TO_NAME[PROP_ID_labelUrl] = PROP_NAME_labelUrl;
          PROP_NAME_TO_ID.put(PROP_NAME_labelUrl, PROP_ID_labelUrl);
      
          PROP_ID_TO_NAME[PROP_ID_weight] = PROP_NAME_weight;
          PROP_NAME_TO_ID.put(PROP_NAME_weight, PROP_ID_weight);
      
          PROP_ID_TO_NAME[PROP_ID_length] = PROP_NAME_length;
          PROP_NAME_TO_ID.put(PROP_NAME_length, PROP_ID_length);
      
          PROP_ID_TO_NAME[PROP_ID_width] = PROP_NAME_width;
          PROP_NAME_TO_ID.put(PROP_NAME_width, PROP_ID_width);
      
          PROP_ID_TO_NAME[PROP_ID_height] = PROP_NAME_height;
          PROP_NAME_TO_ID.put(PROP_NAME_height, PROP_ID_height);
      
          PROP_ID_TO_NAME[PROP_ID_declaredValue] = PROP_NAME_declaredValue;
          PROP_NAME_TO_ID.put(PROP_NAME_declaredValue, PROP_ID_declaredValue);
      
          PROP_ID_TO_NAME[PROP_ID_isActive] = PROP_NAME_isActive;
          PROP_NAME_TO_ID.put(PROP_NAME_isActive, PROP_ID_isActive);
      
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
    
    /* 发运单ID: SHIPMENT_ID */
    private java.lang.Long _shipmentId;
    
    /* 包裹编号: PARCEL_NO */
    private java.lang.String _parcelNo;
    
    /* 运单号: TRACKING_NO */
    private java.lang.String _trackingNo;
    
    /* 面单URL: LABEL_URL */
    private java.lang.String _labelUrl;
    
    /* 重量(kg): WEIGHT */
    private java.lang.String _weight;
    
    /* 长(cm): LENGTH */
    private java.lang.String _length;
    
    /* 宽(cm): WIDTH */
    private java.lang.String _width;
    
    /* 高(cm): HEIGHT */
    private java.lang.String _height;
    
    /* 申报价值: DECLARED_VALUE */
    private java.math.BigDecimal _declaredValue;
    
    /* 是否有效: IS_ACTIVE */
    private java.lang.Integer _isActive;
    
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
    

    public _ErpLogShipmentParcel(){
        // for debug
    }

    protected ErpLogShipmentParcel newInstance(){
        ErpLogShipmentParcel entity = new ErpLogShipmentParcel();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpLogShipmentParcel cloneInstance() {
        ErpLogShipmentParcel entity = newInstance();
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
      return "app.erp.log.dao.entity.ErpLogShipmentParcel";
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
        
            case PROP_ID_shipmentId:
               return getShipmentId();
        
            case PROP_ID_parcelNo:
               return getParcelNo();
        
            case PROP_ID_trackingNo:
               return getTrackingNo();
        
            case PROP_ID_labelUrl:
               return getLabelUrl();
        
            case PROP_ID_weight:
               return getWeight();
        
            case PROP_ID_length:
               return getLength();
        
            case PROP_ID_width:
               return getWidth();
        
            case PROP_ID_height:
               return getHeight();
        
            case PROP_ID_declaredValue:
               return getDeclaredValue();
        
            case PROP_ID_isActive:
               return getIsActive();
        
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
        
            case PROP_ID_shipmentId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_shipmentId));
               }
               setShipmentId(typedValue);
               break;
            }
        
            case PROP_ID_parcelNo:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_parcelNo));
               }
               setParcelNo(typedValue);
               break;
            }
        
            case PROP_ID_trackingNo:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_trackingNo));
               }
               setTrackingNo(typedValue);
               break;
            }
        
            case PROP_ID_labelUrl:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_labelUrl));
               }
               setLabelUrl(typedValue);
               break;
            }
        
            case PROP_ID_weight:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_weight));
               }
               setWeight(typedValue);
               break;
            }
        
            case PROP_ID_length:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_length));
               }
               setLength(typedValue);
               break;
            }
        
            case PROP_ID_width:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_width));
               }
               setWidth(typedValue);
               break;
            }
        
            case PROP_ID_height:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_height));
               }
               setHeight(typedValue);
               break;
            }
        
            case PROP_ID_declaredValue:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_declaredValue));
               }
               setDeclaredValue(typedValue);
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
        
            case PROP_ID_shipmentId:{
               onInitProp(propId);
               this._shipmentId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_parcelNo:{
               onInitProp(propId);
               this._parcelNo = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_trackingNo:{
               onInitProp(propId);
               this._trackingNo = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_labelUrl:{
               onInitProp(propId);
               this._labelUrl = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_weight:{
               onInitProp(propId);
               this._weight = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_length:{
               onInitProp(propId);
               this._length = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_width:{
               onInitProp(propId);
               this._width = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_height:{
               onInitProp(propId);
               this._height = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_declaredValue:{
               onInitProp(propId);
               this._declaredValue = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_isActive:{
               onInitProp(propId);
               this._isActive = (java.lang.Integer)value;
               
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
     * 发运单ID: SHIPMENT_ID
     */
    public final java.lang.Long getShipmentId(){
         onPropGet(PROP_ID_shipmentId);
         return _shipmentId;
    }

    /**
     * 发运单ID: SHIPMENT_ID
     */
    public final void setShipmentId(java.lang.Long value){
        if(onPropSet(PROP_ID_shipmentId,value)){
            this._shipmentId = value;
            internalClearRefs(PROP_ID_shipmentId);
            
        }
    }
    
    /**
     * 包裹编号: PARCEL_NO
     */
    public final java.lang.String getParcelNo(){
         onPropGet(PROP_ID_parcelNo);
         return _parcelNo;
    }

    /**
     * 包裹编号: PARCEL_NO
     */
    public final void setParcelNo(java.lang.String value){
        if(onPropSet(PROP_ID_parcelNo,value)){
            this._parcelNo = value;
            internalClearRefs(PROP_ID_parcelNo);
            
        }
    }
    
    /**
     * 运单号: TRACKING_NO
     */
    public final java.lang.String getTrackingNo(){
         onPropGet(PROP_ID_trackingNo);
         return _trackingNo;
    }

    /**
     * 运单号: TRACKING_NO
     */
    public final void setTrackingNo(java.lang.String value){
        if(onPropSet(PROP_ID_trackingNo,value)){
            this._trackingNo = value;
            internalClearRefs(PROP_ID_trackingNo);
            
        }
    }
    
    /**
     * 面单URL: LABEL_URL
     */
    public final java.lang.String getLabelUrl(){
         onPropGet(PROP_ID_labelUrl);
         return _labelUrl;
    }

    /**
     * 面单URL: LABEL_URL
     */
    public final void setLabelUrl(java.lang.String value){
        if(onPropSet(PROP_ID_labelUrl,value)){
            this._labelUrl = value;
            internalClearRefs(PROP_ID_labelUrl);
            
        }
    }
    
    /**
     * 重量(kg): WEIGHT
     */
    public final java.lang.String getWeight(){
         onPropGet(PROP_ID_weight);
         return _weight;
    }

    /**
     * 重量(kg): WEIGHT
     */
    public final void setWeight(java.lang.String value){
        if(onPropSet(PROP_ID_weight,value)){
            this._weight = value;
            internalClearRefs(PROP_ID_weight);
            
        }
    }
    
    /**
     * 长(cm): LENGTH
     */
    public final java.lang.String getLength(){
         onPropGet(PROP_ID_length);
         return _length;
    }

    /**
     * 长(cm): LENGTH
     */
    public final void setLength(java.lang.String value){
        if(onPropSet(PROP_ID_length,value)){
            this._length = value;
            internalClearRefs(PROP_ID_length);
            
        }
    }
    
    /**
     * 宽(cm): WIDTH
     */
    public final java.lang.String getWidth(){
         onPropGet(PROP_ID_width);
         return _width;
    }

    /**
     * 宽(cm): WIDTH
     */
    public final void setWidth(java.lang.String value){
        if(onPropSet(PROP_ID_width,value)){
            this._width = value;
            internalClearRefs(PROP_ID_width);
            
        }
    }
    
    /**
     * 高(cm): HEIGHT
     */
    public final java.lang.String getHeight(){
         onPropGet(PROP_ID_height);
         return _height;
    }

    /**
     * 高(cm): HEIGHT
     */
    public final void setHeight(java.lang.String value){
        if(onPropSet(PROP_ID_height,value)){
            this._height = value;
            internalClearRefs(PROP_ID_height);
            
        }
    }
    
    /**
     * 申报价值: DECLARED_VALUE
     */
    public final java.math.BigDecimal getDeclaredValue(){
         onPropGet(PROP_ID_declaredValue);
         return _declaredValue;
    }

    /**
     * 申报价值: DECLARED_VALUE
     */
    public final void setDeclaredValue(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_declaredValue,value)){
            this._declaredValue = value;
            internalClearRefs(PROP_ID_declaredValue);
            
        }
    }
    
    /**
     * 是否有效: IS_ACTIVE
     */
    public final java.lang.Integer getIsActive(){
         onPropGet(PROP_ID_isActive);
         return _isActive;
    }

    /**
     * 是否有效: IS_ACTIVE
     */
    public final void setIsActive(java.lang.Integer value){
        if(onPropSet(PROP_ID_isActive,value)){
            this._isActive = value;
            internalClearRefs(PROP_ID_isActive);
            
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
    public final app.erp.log.dao.entity.ErpLogShipment getShipment(){
       return (app.erp.log.dao.entity.ErpLogShipment)internalGetRefEntity(PROP_NAME_shipment);
    }

    public final void setShipment(app.erp.log.dao.entity.ErpLogShipment refEntity){
   
           if(refEntity == null){
           
                   this.setShipmentId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_shipment, refEntity,()->{
           
                           this.setShipmentId(refEntity.getId());
                       
           });
           }
       
    }
       
}
// resume CPD analysis - CPD-ON

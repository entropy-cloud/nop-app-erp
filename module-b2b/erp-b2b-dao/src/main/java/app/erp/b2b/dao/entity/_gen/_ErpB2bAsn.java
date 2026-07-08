package app.erp.b2b.dao.entity._gen;

import io.nop.orm.model.IEntityModel;
import io.nop.orm.support.DynamicOrmEntity;
import io.nop.orm.support.OrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code
import io.nop.orm.IOrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code

import io.nop.api.core.convert.ConvertHelper;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;

import app.erp.b2b.dao.entity.ErpB2bAsn;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  提前发货通知: erp_b2b_asn
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpB2bAsn extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* ASN 编码: CODE VARCHAR */
    public static final String PROP_NAME_code = "code";
    public static final int PROP_ID_code = 2;
    
    /* 业务组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 3;
    
    /* 来源 EDI 文档: SOURCE_EDI_DOC_ID BIGINT */
    public static final String PROP_NAME_sourceEdiDocId = "sourceEdiDocId";
    public static final int PROP_ID_sourceEdiDocId = 4;
    
    /* 发货方（供应商）: PARTNER_ID BIGINT */
    public static final String PROP_NAME_partnerId = "partnerId";
    public static final int PROP_ID_partnerId = 5;
    
    /* 发货日期: SHIPMENT_DATE DATE */
    public static final String PROP_NAME_shipmentDate = "shipmentDate";
    public static final int PROP_ID_shipmentDate = 6;
    
    /* 预计到货日期: ESTIMATED_ARRIVAL_DATE DATE */
    public static final String PROP_NAME_estimatedArrivalDate = "estimatedArrivalDate";
    public static final int PROP_ID_estimatedArrivalDate = 7;
    
    /* 物流单号: TRACKING_NO VARCHAR */
    public static final String PROP_NAME_trackingNo = "trackingNo";
    public static final int PROP_ID_trackingNo = 8;
    
    /* 关联采购订单类型: RELATED_BILL_TYPE VARCHAR */
    public static final String PROP_NAME_relatedBillType = "relatedBillType";
    public static final int PROP_ID_relatedBillType = 9;
    
    /* 关联采购订单号: RELATED_BILL_CODE VARCHAR */
    public static final String PROP_NAME_relatedBillCode = "relatedBillCode";
    public static final int PROP_ID_relatedBillCode = 10;
    
    /* 状态: STATUS VARCHAR */
    public static final String PROP_NAME_status = "status";
    public static final int PROP_ID_status = 11;
    
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
    
    /* 业务日期: BUSINESS_DATE DATE */
    public static final String PROP_NAME_businessDate = "businessDate";
    public static final int PROP_ID_businessDate = 19;
    

    private static int _PROP_ID_BOUND = 20;

    
    /* relation:  */
    public static final String PROP_NAME_sourceEdiDoc = "sourceEdiDoc";
    
    /* relation:  */
    public static final String PROP_NAME_partner = "partner";
    
    /* relation:  */
    public static final String PROP_NAME_org = "org";
    
    /* relation:  */
    public static final String PROP_NAME_lines = "lines";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[20];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_code] = PROP_NAME_code;
          PROP_NAME_TO_ID.put(PROP_NAME_code, PROP_ID_code);
      
          PROP_ID_TO_NAME[PROP_ID_orgId] = PROP_NAME_orgId;
          PROP_NAME_TO_ID.put(PROP_NAME_orgId, PROP_ID_orgId);
      
          PROP_ID_TO_NAME[PROP_ID_sourceEdiDocId] = PROP_NAME_sourceEdiDocId;
          PROP_NAME_TO_ID.put(PROP_NAME_sourceEdiDocId, PROP_ID_sourceEdiDocId);
      
          PROP_ID_TO_NAME[PROP_ID_partnerId] = PROP_NAME_partnerId;
          PROP_NAME_TO_ID.put(PROP_NAME_partnerId, PROP_ID_partnerId);
      
          PROP_ID_TO_NAME[PROP_ID_shipmentDate] = PROP_NAME_shipmentDate;
          PROP_NAME_TO_ID.put(PROP_NAME_shipmentDate, PROP_ID_shipmentDate);
      
          PROP_ID_TO_NAME[PROP_ID_estimatedArrivalDate] = PROP_NAME_estimatedArrivalDate;
          PROP_NAME_TO_ID.put(PROP_NAME_estimatedArrivalDate, PROP_ID_estimatedArrivalDate);
      
          PROP_ID_TO_NAME[PROP_ID_trackingNo] = PROP_NAME_trackingNo;
          PROP_NAME_TO_ID.put(PROP_NAME_trackingNo, PROP_ID_trackingNo);
      
          PROP_ID_TO_NAME[PROP_ID_relatedBillType] = PROP_NAME_relatedBillType;
          PROP_NAME_TO_ID.put(PROP_NAME_relatedBillType, PROP_ID_relatedBillType);
      
          PROP_ID_TO_NAME[PROP_ID_relatedBillCode] = PROP_NAME_relatedBillCode;
          PROP_NAME_TO_ID.put(PROP_NAME_relatedBillCode, PROP_ID_relatedBillCode);
      
          PROP_ID_TO_NAME[PROP_ID_status] = PROP_NAME_status;
          PROP_NAME_TO_ID.put(PROP_NAME_status, PROP_ID_status);
      
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
      
          PROP_ID_TO_NAME[PROP_ID_businessDate] = PROP_NAME_businessDate;
          PROP_NAME_TO_ID.put(PROP_NAME_businessDate, PROP_ID_businessDate);
      
    }

    
    /* ID: ID */
    private java.lang.Long _id;
    
    /* ASN 编码: CODE */
    private java.lang.String _code;
    
    /* 业务组织: ORG_ID */
    private java.lang.Long _orgId;
    
    /* 来源 EDI 文档: SOURCE_EDI_DOC_ID */
    private java.lang.Long _sourceEdiDocId;
    
    /* 发货方（供应商）: PARTNER_ID */
    private java.lang.Long _partnerId;
    
    /* 发货日期: SHIPMENT_DATE */
    private java.time.LocalDate _shipmentDate;
    
    /* 预计到货日期: ESTIMATED_ARRIVAL_DATE */
    private java.time.LocalDate _estimatedArrivalDate;
    
    /* 物流单号: TRACKING_NO */
    private java.lang.String _trackingNo;
    
    /* 关联采购订单类型: RELATED_BILL_TYPE */
    private java.lang.String _relatedBillType;
    
    /* 关联采购订单号: RELATED_BILL_CODE */
    private java.lang.String _relatedBillCode;
    
    /* 状态: STATUS */
    private java.lang.String _status;
    
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
    
    /* 业务日期: BUSINESS_DATE */
    private java.time.LocalDate _businessDate;
    

    public _ErpB2bAsn(){
        // for debug
    }

    protected ErpB2bAsn newInstance(){
        ErpB2bAsn entity = new ErpB2bAsn();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpB2bAsn cloneInstance() {
        ErpB2bAsn entity = newInstance();
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
      return "app.erp.b2b.dao.entity.ErpB2bAsn";
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
        
            case PROP_ID_sourceEdiDocId:
               return getSourceEdiDocId();
        
            case PROP_ID_partnerId:
               return getPartnerId();
        
            case PROP_ID_shipmentDate:
               return getShipmentDate();
        
            case PROP_ID_estimatedArrivalDate:
               return getEstimatedArrivalDate();
        
            case PROP_ID_trackingNo:
               return getTrackingNo();
        
            case PROP_ID_relatedBillType:
               return getRelatedBillType();
        
            case PROP_ID_relatedBillCode:
               return getRelatedBillCode();
        
            case PROP_ID_status:
               return getStatus();
        
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
        
            case PROP_ID_sourceEdiDocId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_sourceEdiDocId));
               }
               setSourceEdiDocId(typedValue);
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
        
            case PROP_ID_shipmentDate:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_shipmentDate));
               }
               setShipmentDate(typedValue);
               break;
            }
        
            case PROP_ID_estimatedArrivalDate:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_estimatedArrivalDate));
               }
               setEstimatedArrivalDate(typedValue);
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
        
            case PROP_ID_status:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_status));
               }
               setStatus(typedValue);
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
        
            case PROP_ID_sourceEdiDocId:{
               onInitProp(propId);
               this._sourceEdiDocId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_partnerId:{
               onInitProp(propId);
               this._partnerId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_shipmentDate:{
               onInitProp(propId);
               this._shipmentDate = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_estimatedArrivalDate:{
               onInitProp(propId);
               this._estimatedArrivalDate = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_trackingNo:{
               onInitProp(propId);
               this._trackingNo = (java.lang.String)value;
               
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
        
            case PROP_ID_status:{
               onInitProp(propId);
               this._status = (java.lang.String)value;
               
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
     * ASN 编码: CODE
     */
    public final java.lang.String getCode(){
         onPropGet(PROP_ID_code);
         return _code;
    }

    /**
     * ASN 编码: CODE
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
     * 来源 EDI 文档: SOURCE_EDI_DOC_ID
     */
    public final java.lang.Long getSourceEdiDocId(){
         onPropGet(PROP_ID_sourceEdiDocId);
         return _sourceEdiDocId;
    }

    /**
     * 来源 EDI 文档: SOURCE_EDI_DOC_ID
     */
    public final void setSourceEdiDocId(java.lang.Long value){
        if(onPropSet(PROP_ID_sourceEdiDocId,value)){
            this._sourceEdiDocId = value;
            internalClearRefs(PROP_ID_sourceEdiDocId);
            
        }
    }
    
    /**
     * 发货方（供应商）: PARTNER_ID
     */
    public final java.lang.Long getPartnerId(){
         onPropGet(PROP_ID_partnerId);
         return _partnerId;
    }

    /**
     * 发货方（供应商）: PARTNER_ID
     */
    public final void setPartnerId(java.lang.Long value){
        if(onPropSet(PROP_ID_partnerId,value)){
            this._partnerId = value;
            internalClearRefs(PROP_ID_partnerId);
            
        }
    }
    
    /**
     * 发货日期: SHIPMENT_DATE
     */
    public final java.time.LocalDate getShipmentDate(){
         onPropGet(PROP_ID_shipmentDate);
         return _shipmentDate;
    }

    /**
     * 发货日期: SHIPMENT_DATE
     */
    public final void setShipmentDate(java.time.LocalDate value){
        if(onPropSet(PROP_ID_shipmentDate,value)){
            this._shipmentDate = value;
            internalClearRefs(PROP_ID_shipmentDate);
            
        }
    }
    
    /**
     * 预计到货日期: ESTIMATED_ARRIVAL_DATE
     */
    public final java.time.LocalDate getEstimatedArrivalDate(){
         onPropGet(PROP_ID_estimatedArrivalDate);
         return _estimatedArrivalDate;
    }

    /**
     * 预计到货日期: ESTIMATED_ARRIVAL_DATE
     */
    public final void setEstimatedArrivalDate(java.time.LocalDate value){
        if(onPropSet(PROP_ID_estimatedArrivalDate,value)){
            this._estimatedArrivalDate = value;
            internalClearRefs(PROP_ID_estimatedArrivalDate);
            
        }
    }
    
    /**
     * 物流单号: TRACKING_NO
     */
    public final java.lang.String getTrackingNo(){
         onPropGet(PROP_ID_trackingNo);
         return _trackingNo;
    }

    /**
     * 物流单号: TRACKING_NO
     */
    public final void setTrackingNo(java.lang.String value){
        if(onPropSet(PROP_ID_trackingNo,value)){
            this._trackingNo = value;
            internalClearRefs(PROP_ID_trackingNo);
            
        }
    }
    
    /**
     * 关联采购订单类型: RELATED_BILL_TYPE
     */
    public final java.lang.String getRelatedBillType(){
         onPropGet(PROP_ID_relatedBillType);
         return _relatedBillType;
    }

    /**
     * 关联采购订单类型: RELATED_BILL_TYPE
     */
    public final void setRelatedBillType(java.lang.String value){
        if(onPropSet(PROP_ID_relatedBillType,value)){
            this._relatedBillType = value;
            internalClearRefs(PROP_ID_relatedBillType);
            
        }
    }
    
    /**
     * 关联采购订单号: RELATED_BILL_CODE
     */
    public final java.lang.String getRelatedBillCode(){
         onPropGet(PROP_ID_relatedBillCode);
         return _relatedBillCode;
    }

    /**
     * 关联采购订单号: RELATED_BILL_CODE
     */
    public final void setRelatedBillCode(java.lang.String value){
        if(onPropSet(PROP_ID_relatedBillCode,value)){
            this._relatedBillCode = value;
            internalClearRefs(PROP_ID_relatedBillCode);
            
        }
    }
    
    /**
     * 状态: STATUS
     */
    public final java.lang.String getStatus(){
         onPropGet(PROP_ID_status);
         return _status;
    }

    /**
     * 状态: STATUS
     */
    public final void setStatus(java.lang.String value){
        if(onPropSet(PROP_ID_status,value)){
            this._status = value;
            internalClearRefs(PROP_ID_status);
            
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
    public final app.erp.b2b.dao.entity.ErpB2bEdiDoc getSourceEdiDoc(){
       return (app.erp.b2b.dao.entity.ErpB2bEdiDoc)internalGetRefEntity(PROP_NAME_sourceEdiDoc);
    }

    public final void setSourceEdiDoc(app.erp.b2b.dao.entity.ErpB2bEdiDoc refEntity){
   
           if(refEntity == null){
           
                   this.setSourceEdiDocId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_sourceEdiDoc, refEntity,()->{
           
                           this.setSourceEdiDocId(refEntity.getId());
                       
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
       
    private final OrmEntitySet<app.erp.b2b.dao.entity.ErpB2bAsnLine> _lines = new OrmEntitySet<>(this, PROP_NAME_lines,
        null, null,app.erp.b2b.dao.entity.ErpB2bAsnLine.class);

    /**
     * 。 refPropName: , keyProp: {rel.keyProp}
     */
    public final IOrmEntitySet<app.erp.b2b.dao.entity.ErpB2bAsnLine> getLines(){
       return _lines;
    }
       
}
// resume CPD analysis - CPD-ON

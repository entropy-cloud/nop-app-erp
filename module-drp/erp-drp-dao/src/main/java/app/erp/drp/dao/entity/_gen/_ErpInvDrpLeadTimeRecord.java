package app.erp.drp.dao.entity._gen;

import io.nop.orm.model.IEntityModel;
import io.nop.orm.support.DynamicOrmEntity;
import io.nop.orm.support.OrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code
import io.nop.orm.IOrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code

import io.nop.api.core.convert.ConvertHelper;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;

import app.erp.drp.dao.entity.ErpInvDrpLeadTimeRecord;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  提前期记录: erp_inv_drp_lead_time_record
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpInvDrpLeadTimeRecord extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 业务组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 2;
    
    /* 供应商: SUPPLIER_ID BIGINT */
    public static final String PROP_NAME_supplierId = "supplierId";
    public static final int PROP_ID_supplierId = 3;
    
    /* 物料: MATERIAL_ID BIGINT */
    public static final String PROP_NAME_materialId = "materialId";
    public static final int PROP_ID_materialId = 4;
    
    /* 订单日期: ORDER_DATE DATE */
    public static final String PROP_NAME_orderDate = "orderDate";
    public static final int PROP_ID_orderDate = 5;
    
    /* 入库日期: RECEIPT_DATE DATE */
    public static final String PROP_NAME_receiptDate = "receiptDate";
    public static final int PROP_ID_receiptDate = 6;
    
    /* 实际提前期(天): ACTUAL_LEAD_TIME INTEGER */
    public static final String PROP_NAME_actualLeadTime = "actualLeadTime";
    public static final int PROP_ID_actualLeadTime = 7;
    
    /* 预期提前期(天): EXPECTED_LEAD_TIME INTEGER */
    public static final String PROP_NAME_expectedLeadTime = "expectedLeadTime";
    public static final int PROP_ID_expectedLeadTime = 8;
    
    /* 偏差天数: VARIANCE_DAYS INTEGER */
    public static final String PROP_NAME_varianceDays = "varianceDays";
    public static final int PROP_ID_varianceDays = 9;
    
    /* 采购单号: PURCHASE_ORDER_CODE VARCHAR */
    public static final String PROP_NAME_purchaseOrderCode = "purchaseOrderCode";
    public static final int PROP_ID_purchaseOrderCode = 10;
    
    /* 是否准时: IS_ON_TIME BOOLEAN */
    public static final String PROP_NAME_isOnTime = "isOnTime";
    public static final int PROP_ID_isOnTime = 11;
    
    /* 提前/延迟标记: EARLY_LATE_FLAG VARCHAR */
    public static final String PROP_NAME_earlyLateFlag = "earlyLateFlag";
    public static final int PROP_ID_earlyLateFlag = 12;
    
    /* 备注: REMARK VARCHAR */
    public static final String PROP_NAME_remark = "remark";
    public static final int PROP_ID_remark = 13;
    
    /* 逻辑删除版本: DEL_VERSION BIGINT */
    public static final String PROP_NAME_delVersion = "delVersion";
    public static final int PROP_ID_delVersion = 14;
    
    /* 数据版本: VERSION INTEGER */
    public static final String PROP_NAME_version = "version";
    public static final int PROP_ID_version = 15;
    
    /* 创建人: CREATED_BY VARCHAR */
    public static final String PROP_NAME_createdBy = "createdBy";
    public static final int PROP_ID_createdBy = 16;
    
    /* 创建时间: CREATE_TIME TIMESTAMP */
    public static final String PROP_NAME_createTime = "createTime";
    public static final int PROP_ID_createTime = 17;
    
    /* 修改人: UPDATED_BY VARCHAR */
    public static final String PROP_NAME_updatedBy = "updatedBy";
    public static final int PROP_ID_updatedBy = 18;
    
    /* 修改时间: UPDATE_TIME TIMESTAMP */
    public static final String PROP_NAME_updateTime = "updateTime";
    public static final int PROP_ID_updateTime = 19;
    

    private static int _PROP_ID_BOUND = 20;

    
    /* relation:  */
    public static final String PROP_NAME_supplier = "supplier";
    
    /* relation:  */
    public static final String PROP_NAME_material = "material";
    
    /* relation:  */
    public static final String PROP_NAME_org = "org";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[20];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_orgId] = PROP_NAME_orgId;
          PROP_NAME_TO_ID.put(PROP_NAME_orgId, PROP_ID_orgId);
      
          PROP_ID_TO_NAME[PROP_ID_supplierId] = PROP_NAME_supplierId;
          PROP_NAME_TO_ID.put(PROP_NAME_supplierId, PROP_ID_supplierId);
      
          PROP_ID_TO_NAME[PROP_ID_materialId] = PROP_NAME_materialId;
          PROP_NAME_TO_ID.put(PROP_NAME_materialId, PROP_ID_materialId);
      
          PROP_ID_TO_NAME[PROP_ID_orderDate] = PROP_NAME_orderDate;
          PROP_NAME_TO_ID.put(PROP_NAME_orderDate, PROP_ID_orderDate);
      
          PROP_ID_TO_NAME[PROP_ID_receiptDate] = PROP_NAME_receiptDate;
          PROP_NAME_TO_ID.put(PROP_NAME_receiptDate, PROP_ID_receiptDate);
      
          PROP_ID_TO_NAME[PROP_ID_actualLeadTime] = PROP_NAME_actualLeadTime;
          PROP_NAME_TO_ID.put(PROP_NAME_actualLeadTime, PROP_ID_actualLeadTime);
      
          PROP_ID_TO_NAME[PROP_ID_expectedLeadTime] = PROP_NAME_expectedLeadTime;
          PROP_NAME_TO_ID.put(PROP_NAME_expectedLeadTime, PROP_ID_expectedLeadTime);
      
          PROP_ID_TO_NAME[PROP_ID_varianceDays] = PROP_NAME_varianceDays;
          PROP_NAME_TO_ID.put(PROP_NAME_varianceDays, PROP_ID_varianceDays);
      
          PROP_ID_TO_NAME[PROP_ID_purchaseOrderCode] = PROP_NAME_purchaseOrderCode;
          PROP_NAME_TO_ID.put(PROP_NAME_purchaseOrderCode, PROP_ID_purchaseOrderCode);
      
          PROP_ID_TO_NAME[PROP_ID_isOnTime] = PROP_NAME_isOnTime;
          PROP_NAME_TO_ID.put(PROP_NAME_isOnTime, PROP_ID_isOnTime);
      
          PROP_ID_TO_NAME[PROP_ID_earlyLateFlag] = PROP_NAME_earlyLateFlag;
          PROP_NAME_TO_ID.put(PROP_NAME_earlyLateFlag, PROP_ID_earlyLateFlag);
      
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
    
    /* 业务组织: ORG_ID */
    private java.lang.Long _orgId;
    
    /* 供应商: SUPPLIER_ID */
    private java.lang.Long _supplierId;
    
    /* 物料: MATERIAL_ID */
    private java.lang.Long _materialId;
    
    /* 订单日期: ORDER_DATE */
    private java.time.LocalDate _orderDate;
    
    /* 入库日期: RECEIPT_DATE */
    private java.time.LocalDate _receiptDate;
    
    /* 实际提前期(天): ACTUAL_LEAD_TIME */
    private java.lang.Integer _actualLeadTime;
    
    /* 预期提前期(天): EXPECTED_LEAD_TIME */
    private java.lang.Integer _expectedLeadTime;
    
    /* 偏差天数: VARIANCE_DAYS */
    private java.lang.Integer _varianceDays;
    
    /* 采购单号: PURCHASE_ORDER_CODE */
    private java.lang.String _purchaseOrderCode;
    
    /* 是否准时: IS_ON_TIME */
    private java.lang.Boolean _isOnTime;
    
    /* 提前/延迟标记: EARLY_LATE_FLAG */
    private java.lang.String _earlyLateFlag;
    
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
    

    public _ErpInvDrpLeadTimeRecord(){
        // for debug
    }

    protected ErpInvDrpLeadTimeRecord newInstance(){
        ErpInvDrpLeadTimeRecord entity = new ErpInvDrpLeadTimeRecord();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpInvDrpLeadTimeRecord cloneInstance() {
        ErpInvDrpLeadTimeRecord entity = newInstance();
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
      return "app.erp.drp.dao.entity.ErpInvDrpLeadTimeRecord";
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
        
            case PROP_ID_supplierId:
               return getSupplierId();
        
            case PROP_ID_materialId:
               return getMaterialId();
        
            case PROP_ID_orderDate:
               return getOrderDate();
        
            case PROP_ID_receiptDate:
               return getReceiptDate();
        
            case PROP_ID_actualLeadTime:
               return getActualLeadTime();
        
            case PROP_ID_expectedLeadTime:
               return getExpectedLeadTime();
        
            case PROP_ID_varianceDays:
               return getVarianceDays();
        
            case PROP_ID_purchaseOrderCode:
               return getPurchaseOrderCode();
        
            case PROP_ID_isOnTime:
               return getIsOnTime();
        
            case PROP_ID_earlyLateFlag:
               return getEarlyLateFlag();
        
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
        
            case PROP_ID_orgId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_orgId));
               }
               setOrgId(typedValue);
               break;
            }
        
            case PROP_ID_supplierId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_supplierId));
               }
               setSupplierId(typedValue);
               break;
            }
        
            case PROP_ID_materialId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_materialId));
               }
               setMaterialId(typedValue);
               break;
            }
        
            case PROP_ID_orderDate:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_orderDate));
               }
               setOrderDate(typedValue);
               break;
            }
        
            case PROP_ID_receiptDate:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_receiptDate));
               }
               setReceiptDate(typedValue);
               break;
            }
        
            case PROP_ID_actualLeadTime:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_actualLeadTime));
               }
               setActualLeadTime(typedValue);
               break;
            }
        
            case PROP_ID_expectedLeadTime:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_expectedLeadTime));
               }
               setExpectedLeadTime(typedValue);
               break;
            }
        
            case PROP_ID_varianceDays:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_varianceDays));
               }
               setVarianceDays(typedValue);
               break;
            }
        
            case PROP_ID_purchaseOrderCode:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_purchaseOrderCode));
               }
               setPurchaseOrderCode(typedValue);
               break;
            }
        
            case PROP_ID_isOnTime:{
               java.lang.Boolean typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBoolean(value,
                       err-> newTypeConversionError(PROP_NAME_isOnTime));
               }
               setIsOnTime(typedValue);
               break;
            }
        
            case PROP_ID_earlyLateFlag:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_earlyLateFlag));
               }
               setEarlyLateFlag(typedValue);
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
        
            case PROP_ID_orgId:{
               onInitProp(propId);
               this._orgId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_supplierId:{
               onInitProp(propId);
               this._supplierId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_materialId:{
               onInitProp(propId);
               this._materialId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_orderDate:{
               onInitProp(propId);
               this._orderDate = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_receiptDate:{
               onInitProp(propId);
               this._receiptDate = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_actualLeadTime:{
               onInitProp(propId);
               this._actualLeadTime = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_expectedLeadTime:{
               onInitProp(propId);
               this._expectedLeadTime = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_varianceDays:{
               onInitProp(propId);
               this._varianceDays = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_purchaseOrderCode:{
               onInitProp(propId);
               this._purchaseOrderCode = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_isOnTime:{
               onInitProp(propId);
               this._isOnTime = (java.lang.Boolean)value;
               
               break;
            }
        
            case PROP_ID_earlyLateFlag:{
               onInitProp(propId);
               this._earlyLateFlag = (java.lang.String)value;
               
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
     * 供应商: SUPPLIER_ID
     */
    public final java.lang.Long getSupplierId(){
         onPropGet(PROP_ID_supplierId);
         return _supplierId;
    }

    /**
     * 供应商: SUPPLIER_ID
     */
    public final void setSupplierId(java.lang.Long value){
        if(onPropSet(PROP_ID_supplierId,value)){
            this._supplierId = value;
            internalClearRefs(PROP_ID_supplierId);
            
        }
    }
    
    /**
     * 物料: MATERIAL_ID
     */
    public final java.lang.Long getMaterialId(){
         onPropGet(PROP_ID_materialId);
         return _materialId;
    }

    /**
     * 物料: MATERIAL_ID
     */
    public final void setMaterialId(java.lang.Long value){
        if(onPropSet(PROP_ID_materialId,value)){
            this._materialId = value;
            internalClearRefs(PROP_ID_materialId);
            
        }
    }
    
    /**
     * 订单日期: ORDER_DATE
     */
    public final java.time.LocalDate getOrderDate(){
         onPropGet(PROP_ID_orderDate);
         return _orderDate;
    }

    /**
     * 订单日期: ORDER_DATE
     */
    public final void setOrderDate(java.time.LocalDate value){
        if(onPropSet(PROP_ID_orderDate,value)){
            this._orderDate = value;
            internalClearRefs(PROP_ID_orderDate);
            
        }
    }
    
    /**
     * 入库日期: RECEIPT_DATE
     */
    public final java.time.LocalDate getReceiptDate(){
         onPropGet(PROP_ID_receiptDate);
         return _receiptDate;
    }

    /**
     * 入库日期: RECEIPT_DATE
     */
    public final void setReceiptDate(java.time.LocalDate value){
        if(onPropSet(PROP_ID_receiptDate,value)){
            this._receiptDate = value;
            internalClearRefs(PROP_ID_receiptDate);
            
        }
    }
    
    /**
     * 实际提前期(天): ACTUAL_LEAD_TIME
     */
    public final java.lang.Integer getActualLeadTime(){
         onPropGet(PROP_ID_actualLeadTime);
         return _actualLeadTime;
    }

    /**
     * 实际提前期(天): ACTUAL_LEAD_TIME
     */
    public final void setActualLeadTime(java.lang.Integer value){
        if(onPropSet(PROP_ID_actualLeadTime,value)){
            this._actualLeadTime = value;
            internalClearRefs(PROP_ID_actualLeadTime);
            
        }
    }
    
    /**
     * 预期提前期(天): EXPECTED_LEAD_TIME
     */
    public final java.lang.Integer getExpectedLeadTime(){
         onPropGet(PROP_ID_expectedLeadTime);
         return _expectedLeadTime;
    }

    /**
     * 预期提前期(天): EXPECTED_LEAD_TIME
     */
    public final void setExpectedLeadTime(java.lang.Integer value){
        if(onPropSet(PROP_ID_expectedLeadTime,value)){
            this._expectedLeadTime = value;
            internalClearRefs(PROP_ID_expectedLeadTime);
            
        }
    }
    
    /**
     * 偏差天数: VARIANCE_DAYS
     */
    public final java.lang.Integer getVarianceDays(){
         onPropGet(PROP_ID_varianceDays);
         return _varianceDays;
    }

    /**
     * 偏差天数: VARIANCE_DAYS
     */
    public final void setVarianceDays(java.lang.Integer value){
        if(onPropSet(PROP_ID_varianceDays,value)){
            this._varianceDays = value;
            internalClearRefs(PROP_ID_varianceDays);
            
        }
    }
    
    /**
     * 采购单号: PURCHASE_ORDER_CODE
     */
    public final java.lang.String getPurchaseOrderCode(){
         onPropGet(PROP_ID_purchaseOrderCode);
         return _purchaseOrderCode;
    }

    /**
     * 采购单号: PURCHASE_ORDER_CODE
     */
    public final void setPurchaseOrderCode(java.lang.String value){
        if(onPropSet(PROP_ID_purchaseOrderCode,value)){
            this._purchaseOrderCode = value;
            internalClearRefs(PROP_ID_purchaseOrderCode);
            
        }
    }
    
    /**
     * 是否准时: IS_ON_TIME
     */
    public final java.lang.Boolean getIsOnTime(){
         onPropGet(PROP_ID_isOnTime);
         return _isOnTime;
    }

    /**
     * 是否准时: IS_ON_TIME
     */
    public final void setIsOnTime(java.lang.Boolean value){
        if(onPropSet(PROP_ID_isOnTime,value)){
            this._isOnTime = value;
            internalClearRefs(PROP_ID_isOnTime);
            
        }
    }
    
    /**
     * 提前/延迟标记: EARLY_LATE_FLAG
     */
    public final java.lang.String getEarlyLateFlag(){
         onPropGet(PROP_ID_earlyLateFlag);
         return _earlyLateFlag;
    }

    /**
     * 提前/延迟标记: EARLY_LATE_FLAG
     */
    public final void setEarlyLateFlag(java.lang.String value){
        if(onPropSet(PROP_ID_earlyLateFlag,value)){
            this._earlyLateFlag = value;
            internalClearRefs(PROP_ID_earlyLateFlag);
            
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
    public final app.erp.md.dao.entity.ErpMdPartner getSupplier(){
       return (app.erp.md.dao.entity.ErpMdPartner)internalGetRefEntity(PROP_NAME_supplier);
    }

    public final void setSupplier(app.erp.md.dao.entity.ErpMdPartner refEntity){
   
           if(refEntity == null){
           
                   this.setSupplierId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_supplier, refEntity,()->{
           
                           this.setSupplierId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.md.dao.entity.ErpMdMaterial getMaterial(){
       return (app.erp.md.dao.entity.ErpMdMaterial)internalGetRefEntity(PROP_NAME_material);
    }

    public final void setMaterial(app.erp.md.dao.entity.ErpMdMaterial refEntity){
   
           if(refEntity == null){
           
                   this.setMaterialId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_material, refEntity,()->{
           
                           this.setMaterialId(refEntity.getId());
                       
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

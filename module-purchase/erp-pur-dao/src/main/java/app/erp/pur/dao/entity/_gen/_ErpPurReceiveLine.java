package app.erp.pur.dao.entity._gen;

import io.nop.orm.model.IEntityModel;
import io.nop.orm.support.DynamicOrmEntity;
import io.nop.orm.support.OrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code
import io.nop.orm.IOrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code

import io.nop.api.core.convert.ConvertHelper;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;

import app.erp.pur.dao.entity.ErpPurReceiveLine;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  采购入库单行: erp_pur_receive_line
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpPurReceiveLine extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 入库单ID: RECEIVE_ID BIGINT */
    public static final String PROP_NAME_receiveId = "receiveId";
    public static final int PROP_ID_receiveId = 2;
    
    /* 订单行ID: ORDER_LINE_ID BIGINT */
    public static final String PROP_NAME_orderLineId = "orderLineId";
    public static final int PROP_ID_orderLineId = 3;
    
    /* 行号: LINE_NO INTEGER */
    public static final String PROP_NAME_lineNo = "lineNo";
    public static final int PROP_ID_lineNo = 4;
    
    /* 物料: MATERIAL_ID BIGINT */
    public static final String PROP_NAME_materialId = "materialId";
    public static final int PROP_ID_materialId = 5;
    
    /* SKU: SKU_ID BIGINT */
    public static final String PROP_NAME_skuId = "skuId";
    public static final int PROP_ID_skuId = 6;
    
    /* 计量单位: UO_M_ID BIGINT */
    public static final String PROP_NAME_uoMId = "uoMId";
    public static final int PROP_ID_uoMId = 7;
    
    /* 实收数量: QUANTITY DECIMAL */
    public static final String PROP_NAME_quantity = "quantity";
    public static final int PROP_ID_quantity = 8;
    
    /* 拒收数量: REJECTED_QUANTITY DECIMAL */
    public static final String PROP_NAME_rejectedQuantity = "rejectedQuantity";
    public static final int PROP_ID_rejectedQuantity = 9;
    
    /* 单价(不含税): UNIT_PRICE DECIMAL */
    public static final String PROP_NAME_unitPrice = "unitPrice";
    public static final int PROP_ID_unitPrice = 10;
    
    /* 税率(%): TAX_RATE DECIMAL */
    public static final String PROP_NAME_taxRate = "taxRate";
    public static final int PROP_ID_taxRate = 11;
    
    /* 税额: TAX_AMOUNT DECIMAL */
    public static final String PROP_NAME_taxAmount = "taxAmount";
    public static final int PROP_ID_taxAmount = 12;
    
    /* 金额(不含税): AMOUNT DECIMAL */
    public static final String PROP_NAME_amount = "amount";
    public static final int PROP_ID_amount = 13;
    
    /* 入库库位: WAREHOUSE_ID BIGINT */
    public static final String PROP_NAME_warehouseId = "warehouseId";
    public static final int PROP_ID_warehouseId = 14;
    
    /* 批号: BATCH_NO VARCHAR */
    public static final String PROP_NAME_batchNo = "batchNo";
    public static final int PROP_ID_batchNo = 15;
    
    /* 备注: REMARK VARCHAR */
    public static final String PROP_NAME_remark = "remark";
    public static final int PROP_ID_remark = 16;
    
    /* 逻辑删除版本: DEL_VERSION BIGINT */
    public static final String PROP_NAME_delVersion = "delVersion";
    public static final int PROP_ID_delVersion = 17;
    
    /* 数据版本: VERSION INTEGER */
    public static final String PROP_NAME_version = "version";
    public static final int PROP_ID_version = 18;
    
    /* 创建人: CREATED_BY VARCHAR */
    public static final String PROP_NAME_createdBy = "createdBy";
    public static final int PROP_ID_createdBy = 19;
    
    /* 创建时间: CREATE_TIME TIMESTAMP */
    public static final String PROP_NAME_createTime = "createTime";
    public static final int PROP_ID_createTime = 20;
    
    /* 修改人: UPDATED_BY VARCHAR */
    public static final String PROP_NAME_updatedBy = "updatedBy";
    public static final int PROP_ID_updatedBy = 21;
    
    /* 修改时间: UPDATE_TIME TIMESTAMP */
    public static final String PROP_NAME_updateTime = "updateTime";
    public static final int PROP_ID_updateTime = 22;
    

    private static int _PROP_ID_BOUND = 23;

    
    /* relation:  */
    public static final String PROP_NAME_receive = "receive";
    
    /* relation:  */
    public static final String PROP_NAME_material = "material";
    
    /* relation:  */
    public static final String PROP_NAME_sku = "sku";
    
    /* relation:  */
    public static final String PROP_NAME_uoM = "uoM";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[23];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_receiveId] = PROP_NAME_receiveId;
          PROP_NAME_TO_ID.put(PROP_NAME_receiveId, PROP_ID_receiveId);
      
          PROP_ID_TO_NAME[PROP_ID_orderLineId] = PROP_NAME_orderLineId;
          PROP_NAME_TO_ID.put(PROP_NAME_orderLineId, PROP_ID_orderLineId);
      
          PROP_ID_TO_NAME[PROP_ID_lineNo] = PROP_NAME_lineNo;
          PROP_NAME_TO_ID.put(PROP_NAME_lineNo, PROP_ID_lineNo);
      
          PROP_ID_TO_NAME[PROP_ID_materialId] = PROP_NAME_materialId;
          PROP_NAME_TO_ID.put(PROP_NAME_materialId, PROP_ID_materialId);
      
          PROP_ID_TO_NAME[PROP_ID_skuId] = PROP_NAME_skuId;
          PROP_NAME_TO_ID.put(PROP_NAME_skuId, PROP_ID_skuId);
      
          PROP_ID_TO_NAME[PROP_ID_uoMId] = PROP_NAME_uoMId;
          PROP_NAME_TO_ID.put(PROP_NAME_uoMId, PROP_ID_uoMId);
      
          PROP_ID_TO_NAME[PROP_ID_quantity] = PROP_NAME_quantity;
          PROP_NAME_TO_ID.put(PROP_NAME_quantity, PROP_ID_quantity);
      
          PROP_ID_TO_NAME[PROP_ID_rejectedQuantity] = PROP_NAME_rejectedQuantity;
          PROP_NAME_TO_ID.put(PROP_NAME_rejectedQuantity, PROP_ID_rejectedQuantity);
      
          PROP_ID_TO_NAME[PROP_ID_unitPrice] = PROP_NAME_unitPrice;
          PROP_NAME_TO_ID.put(PROP_NAME_unitPrice, PROP_ID_unitPrice);
      
          PROP_ID_TO_NAME[PROP_ID_taxRate] = PROP_NAME_taxRate;
          PROP_NAME_TO_ID.put(PROP_NAME_taxRate, PROP_ID_taxRate);
      
          PROP_ID_TO_NAME[PROP_ID_taxAmount] = PROP_NAME_taxAmount;
          PROP_NAME_TO_ID.put(PROP_NAME_taxAmount, PROP_ID_taxAmount);
      
          PROP_ID_TO_NAME[PROP_ID_amount] = PROP_NAME_amount;
          PROP_NAME_TO_ID.put(PROP_NAME_amount, PROP_ID_amount);
      
          PROP_ID_TO_NAME[PROP_ID_warehouseId] = PROP_NAME_warehouseId;
          PROP_NAME_TO_ID.put(PROP_NAME_warehouseId, PROP_ID_warehouseId);
      
          PROP_ID_TO_NAME[PROP_ID_batchNo] = PROP_NAME_batchNo;
          PROP_NAME_TO_ID.put(PROP_NAME_batchNo, PROP_ID_batchNo);
      
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
    
    /* 入库单ID: RECEIVE_ID */
    private java.lang.Long _receiveId;
    
    /* 订单行ID: ORDER_LINE_ID */
    private java.lang.Long _orderLineId;
    
    /* 行号: LINE_NO */
    private java.lang.Integer _lineNo;
    
    /* 物料: MATERIAL_ID */
    private java.lang.Long _materialId;
    
    /* SKU: SKU_ID */
    private java.lang.Long _skuId;
    
    /* 计量单位: UO_M_ID */
    private java.lang.Long _uoMId;
    
    /* 实收数量: QUANTITY */
    private java.lang.String _quantity;
    
    /* 拒收数量: REJECTED_QUANTITY */
    private java.lang.String _rejectedQuantity;
    
    /* 单价(不含税): UNIT_PRICE */
    private java.lang.String _unitPrice;
    
    /* 税率(%): TAX_RATE */
    private java.lang.String _taxRate;
    
    /* 税额: TAX_AMOUNT */
    private java.lang.String _taxAmount;
    
    /* 金额(不含税): AMOUNT */
    private java.lang.String _amount;
    
    /* 入库库位: WAREHOUSE_ID */
    private java.lang.Long _warehouseId;
    
    /* 批号: BATCH_NO */
    private java.lang.String _batchNo;
    
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
    

    public _ErpPurReceiveLine(){
        // for debug
    }

    protected ErpPurReceiveLine newInstance(){
        ErpPurReceiveLine entity = new ErpPurReceiveLine();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpPurReceiveLine cloneInstance() {
        ErpPurReceiveLine entity = newInstance();
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
      return "app.erp.pur.dao.entity.ErpPurReceiveLine";
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
        
            case PROP_ID_receiveId:
               return getReceiveId();
        
            case PROP_ID_orderLineId:
               return getOrderLineId();
        
            case PROP_ID_lineNo:
               return getLineNo();
        
            case PROP_ID_materialId:
               return getMaterialId();
        
            case PROP_ID_skuId:
               return getSkuId();
        
            case PROP_ID_uoMId:
               return getUoMId();
        
            case PROP_ID_quantity:
               return getQuantity();
        
            case PROP_ID_rejectedQuantity:
               return getRejectedQuantity();
        
            case PROP_ID_unitPrice:
               return getUnitPrice();
        
            case PROP_ID_taxRate:
               return getTaxRate();
        
            case PROP_ID_taxAmount:
               return getTaxAmount();
        
            case PROP_ID_amount:
               return getAmount();
        
            case PROP_ID_warehouseId:
               return getWarehouseId();
        
            case PROP_ID_batchNo:
               return getBatchNo();
        
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
        
            case PROP_ID_receiveId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_receiveId));
               }
               setReceiveId(typedValue);
               break;
            }
        
            case PROP_ID_orderLineId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_orderLineId));
               }
               setOrderLineId(typedValue);
               break;
            }
        
            case PROP_ID_lineNo:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_lineNo));
               }
               setLineNo(typedValue);
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
        
            case PROP_ID_skuId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_skuId));
               }
               setSkuId(typedValue);
               break;
            }
        
            case PROP_ID_uoMId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_uoMId));
               }
               setUoMId(typedValue);
               break;
            }
        
            case PROP_ID_quantity:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_quantity));
               }
               setQuantity(typedValue);
               break;
            }
        
            case PROP_ID_rejectedQuantity:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_rejectedQuantity));
               }
               setRejectedQuantity(typedValue);
               break;
            }
        
            case PROP_ID_unitPrice:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_unitPrice));
               }
               setUnitPrice(typedValue);
               break;
            }
        
            case PROP_ID_taxRate:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_taxRate));
               }
               setTaxRate(typedValue);
               break;
            }
        
            case PROP_ID_taxAmount:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_taxAmount));
               }
               setTaxAmount(typedValue);
               break;
            }
        
            case PROP_ID_amount:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_amount));
               }
               setAmount(typedValue);
               break;
            }
        
            case PROP_ID_warehouseId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_warehouseId));
               }
               setWarehouseId(typedValue);
               break;
            }
        
            case PROP_ID_batchNo:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_batchNo));
               }
               setBatchNo(typedValue);
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
        
            case PROP_ID_receiveId:{
               onInitProp(propId);
               this._receiveId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_orderLineId:{
               onInitProp(propId);
               this._orderLineId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_lineNo:{
               onInitProp(propId);
               this._lineNo = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_materialId:{
               onInitProp(propId);
               this._materialId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_skuId:{
               onInitProp(propId);
               this._skuId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_uoMId:{
               onInitProp(propId);
               this._uoMId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_quantity:{
               onInitProp(propId);
               this._quantity = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_rejectedQuantity:{
               onInitProp(propId);
               this._rejectedQuantity = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_unitPrice:{
               onInitProp(propId);
               this._unitPrice = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_taxRate:{
               onInitProp(propId);
               this._taxRate = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_taxAmount:{
               onInitProp(propId);
               this._taxAmount = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_amount:{
               onInitProp(propId);
               this._amount = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_warehouseId:{
               onInitProp(propId);
               this._warehouseId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_batchNo:{
               onInitProp(propId);
               this._batchNo = (java.lang.String)value;
               
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
     * 入库单ID: RECEIVE_ID
     */
    public final java.lang.Long getReceiveId(){
         onPropGet(PROP_ID_receiveId);
         return _receiveId;
    }

    /**
     * 入库单ID: RECEIVE_ID
     */
    public final void setReceiveId(java.lang.Long value){
        if(onPropSet(PROP_ID_receiveId,value)){
            this._receiveId = value;
            internalClearRefs(PROP_ID_receiveId);
            
        }
    }
    
    /**
     * 订单行ID: ORDER_LINE_ID
     */
    public final java.lang.Long getOrderLineId(){
         onPropGet(PROP_ID_orderLineId);
         return _orderLineId;
    }

    /**
     * 订单行ID: ORDER_LINE_ID
     */
    public final void setOrderLineId(java.lang.Long value){
        if(onPropSet(PROP_ID_orderLineId,value)){
            this._orderLineId = value;
            internalClearRefs(PROP_ID_orderLineId);
            
        }
    }
    
    /**
     * 行号: LINE_NO
     */
    public final java.lang.Integer getLineNo(){
         onPropGet(PROP_ID_lineNo);
         return _lineNo;
    }

    /**
     * 行号: LINE_NO
     */
    public final void setLineNo(java.lang.Integer value){
        if(onPropSet(PROP_ID_lineNo,value)){
            this._lineNo = value;
            internalClearRefs(PROP_ID_lineNo);
            
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
     * SKU: SKU_ID
     */
    public final java.lang.Long getSkuId(){
         onPropGet(PROP_ID_skuId);
         return _skuId;
    }

    /**
     * SKU: SKU_ID
     */
    public final void setSkuId(java.lang.Long value){
        if(onPropSet(PROP_ID_skuId,value)){
            this._skuId = value;
            internalClearRefs(PROP_ID_skuId);
            
        }
    }
    
    /**
     * 计量单位: UO_M_ID
     */
    public final java.lang.Long getUoMId(){
         onPropGet(PROP_ID_uoMId);
         return _uoMId;
    }

    /**
     * 计量单位: UO_M_ID
     */
    public final void setUoMId(java.lang.Long value){
        if(onPropSet(PROP_ID_uoMId,value)){
            this._uoMId = value;
            internalClearRefs(PROP_ID_uoMId);
            
        }
    }
    
    /**
     * 实收数量: QUANTITY
     */
    public final java.lang.String getQuantity(){
         onPropGet(PROP_ID_quantity);
         return _quantity;
    }

    /**
     * 实收数量: QUANTITY
     */
    public final void setQuantity(java.lang.String value){
        if(onPropSet(PROP_ID_quantity,value)){
            this._quantity = value;
            internalClearRefs(PROP_ID_quantity);
            
        }
    }
    
    /**
     * 拒收数量: REJECTED_QUANTITY
     */
    public final java.lang.String getRejectedQuantity(){
         onPropGet(PROP_ID_rejectedQuantity);
         return _rejectedQuantity;
    }

    /**
     * 拒收数量: REJECTED_QUANTITY
     */
    public final void setRejectedQuantity(java.lang.String value){
        if(onPropSet(PROP_ID_rejectedQuantity,value)){
            this._rejectedQuantity = value;
            internalClearRefs(PROP_ID_rejectedQuantity);
            
        }
    }
    
    /**
     * 单价(不含税): UNIT_PRICE
     */
    public final java.lang.String getUnitPrice(){
         onPropGet(PROP_ID_unitPrice);
         return _unitPrice;
    }

    /**
     * 单价(不含税): UNIT_PRICE
     */
    public final void setUnitPrice(java.lang.String value){
        if(onPropSet(PROP_ID_unitPrice,value)){
            this._unitPrice = value;
            internalClearRefs(PROP_ID_unitPrice);
            
        }
    }
    
    /**
     * 税率(%): TAX_RATE
     */
    public final java.lang.String getTaxRate(){
         onPropGet(PROP_ID_taxRate);
         return _taxRate;
    }

    /**
     * 税率(%): TAX_RATE
     */
    public final void setTaxRate(java.lang.String value){
        if(onPropSet(PROP_ID_taxRate,value)){
            this._taxRate = value;
            internalClearRefs(PROP_ID_taxRate);
            
        }
    }
    
    /**
     * 税额: TAX_AMOUNT
     */
    public final java.lang.String getTaxAmount(){
         onPropGet(PROP_ID_taxAmount);
         return _taxAmount;
    }

    /**
     * 税额: TAX_AMOUNT
     */
    public final void setTaxAmount(java.lang.String value){
        if(onPropSet(PROP_ID_taxAmount,value)){
            this._taxAmount = value;
            internalClearRefs(PROP_ID_taxAmount);
            
        }
    }
    
    /**
     * 金额(不含税): AMOUNT
     */
    public final java.lang.String getAmount(){
         onPropGet(PROP_ID_amount);
         return _amount;
    }

    /**
     * 金额(不含税): AMOUNT
     */
    public final void setAmount(java.lang.String value){
        if(onPropSet(PROP_ID_amount,value)){
            this._amount = value;
            internalClearRefs(PROP_ID_amount);
            
        }
    }
    
    /**
     * 入库库位: WAREHOUSE_ID
     */
    public final java.lang.Long getWarehouseId(){
         onPropGet(PROP_ID_warehouseId);
         return _warehouseId;
    }

    /**
     * 入库库位: WAREHOUSE_ID
     */
    public final void setWarehouseId(java.lang.Long value){
        if(onPropSet(PROP_ID_warehouseId,value)){
            this._warehouseId = value;
            internalClearRefs(PROP_ID_warehouseId);
            
        }
    }
    
    /**
     * 批号: BATCH_NO
     */
    public final java.lang.String getBatchNo(){
         onPropGet(PROP_ID_batchNo);
         return _batchNo;
    }

    /**
     * 批号: BATCH_NO
     */
    public final void setBatchNo(java.lang.String value){
        if(onPropSet(PROP_ID_batchNo,value)){
            this._batchNo = value;
            internalClearRefs(PROP_ID_batchNo);
            
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
    public final app.erp.pur.dao.entity.ErpPurReceive getReceive(){
       return (app.erp.pur.dao.entity.ErpPurReceive)internalGetRefEntity(PROP_NAME_receive);
    }

    public final void setReceive(app.erp.pur.dao.entity.ErpPurReceive refEntity){
   
           if(refEntity == null){
           
                   this.setReceiveId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_receive, refEntity,()->{
           
                           this.setReceiveId(refEntity.getId());
                       
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
    public final app.erp.md.dao.entity.ErpMdMaterialSku getSku(){
       return (app.erp.md.dao.entity.ErpMdMaterialSku)internalGetRefEntity(PROP_NAME_sku);
    }

    public final void setSku(app.erp.md.dao.entity.ErpMdMaterialSku refEntity){
   
           if(refEntity == null){
           
                   this.setSkuId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_sku, refEntity,()->{
           
                           this.setSkuId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.md.dao.entity.ErpMdUoM getUoM(){
       return (app.erp.md.dao.entity.ErpMdUoM)internalGetRefEntity(PROP_NAME_uoM);
    }

    public final void setUoM(app.erp.md.dao.entity.ErpMdUoM refEntity){
   
           if(refEntity == null){
           
                   this.setUoMId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_uoM, refEntity,()->{
           
                           this.setUoMId(refEntity.getId());
                       
           });
           }
       
    }
       
}
// resume CPD analysis - CPD-ON

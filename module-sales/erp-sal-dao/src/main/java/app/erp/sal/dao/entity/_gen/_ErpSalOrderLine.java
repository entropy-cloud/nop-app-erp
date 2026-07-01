package app.erp.sal.dao.entity._gen;

import io.nop.orm.model.IEntityModel;
import io.nop.orm.support.DynamicOrmEntity;
import io.nop.orm.support.OrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code
import io.nop.orm.IOrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code

import io.nop.api.core.convert.ConvertHelper;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;

import app.erp.sal.dao.entity.ErpSalOrderLine;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  销售订单行: erp_sal_order_line
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpSalOrderLine extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 订单ID: ORDER_ID BIGINT */
    public static final String PROP_NAME_orderId = "orderId";
    public static final int PROP_ID_orderId = 2;
    
    /* 行号: LINE_NO INTEGER */
    public static final String PROP_NAME_lineNo = "lineNo";
    public static final int PROP_ID_lineNo = 3;
    
    /* 物料: MATERIAL_ID BIGINT */
    public static final String PROP_NAME_materialId = "materialId";
    public static final int PROP_ID_materialId = 4;
    
    /* SKU: SKU_ID BIGINT */
    public static final String PROP_NAME_skuId = "skuId";
    public static final int PROP_ID_skuId = 5;
    
    /* 计量单位: UOM_ID BIGINT */
    public static final String PROP_NAME_uoMId = "uoMId";
    public static final int PROP_ID_uoMId = 6;
    
    /* 数量: QUANTITY DECIMAL */
    public static final String PROP_NAME_quantity = "quantity";
    public static final int PROP_ID_quantity = 7;
    
    /* 单价(不含税): UNIT_PRICE DECIMAL */
    public static final String PROP_NAME_unitPrice = "unitPrice";
    public static final int PROP_ID_unitPrice = 8;
    
    /* 税率(%): TAX_RATE DECIMAL */
    public static final String PROP_NAME_taxRate = "taxRate";
    public static final int PROP_ID_taxRate = 9;
    
    /* 税率主数据: TAX_RATE_ID BIGINT */
    public static final String PROP_NAME_taxRateId = "taxRateId";
    public static final int PROP_ID_taxRateId = 10;
    
    /* 税额: TAX_AMOUNT DECIMAL */
    public static final String PROP_NAME_taxAmount = "taxAmount";
    public static final int PROP_ID_taxAmount = 11;
    
    /* 金额(不含税): AMOUNT DECIMAL */
    public static final String PROP_NAME_amount = "amount";
    public static final int PROP_ID_amount = 12;
    
    /* 金额(含税): AMOUNT_WITH_TAX DECIMAL */
    public static final String PROP_NAME_amountWithTax = "amountWithTax";
    public static final int PROP_ID_amountWithTax = 13;
    
    /* 已出库数量: DELIVERED_QUANTITY DECIMAL */
    public static final String PROP_NAME_deliveredQuantity = "deliveredQuantity";
    public static final int PROP_ID_deliveredQuantity = 14;
    
    /* 已开票数量: INVOICED_QUANTITY DECIMAL */
    public static final String PROP_NAME_invoicedQuantity = "invoicedQuantity";
    public static final int PROP_ID_invoicedQuantity = 15;
    
    /* 发货仓库: WAREHOUSE_ID BIGINT */
    public static final String PROP_NAME_warehouseId = "warehouseId";
    public static final int PROP_ID_warehouseId = 16;
    
    /* 项目: PROJECT_ID BIGINT */
    public static final String PROP_NAME_projectId = "projectId";
    public static final int PROP_ID_projectId = 17;
    
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
    
    /* 备注: REMARK VARCHAR */
    public static final String PROP_NAME_remark = "remark";
    public static final int PROP_ID_remark = 24;
    

    private static int _PROP_ID_BOUND = 25;

    
    /* relation:  */
    public static final String PROP_NAME_order = "order";
    
    /* relation:  */
    public static final String PROP_NAME_material = "material";
    
    /* relation:  */
    public static final String PROP_NAME_sku = "sku";
    
    /* relation:  */
    public static final String PROP_NAME_uoM = "uoM";
    
    /* relation:  */
    public static final String PROP_NAME_taxRateMd = "taxRateMd";
    
    /* relation:  */
    public static final String PROP_NAME_warehouse = "warehouse";
    
    /* relation:  */
    public static final String PROP_NAME_project = "project";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[25];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_orderId] = PROP_NAME_orderId;
          PROP_NAME_TO_ID.put(PROP_NAME_orderId, PROP_ID_orderId);
      
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
      
          PROP_ID_TO_NAME[PROP_ID_unitPrice] = PROP_NAME_unitPrice;
          PROP_NAME_TO_ID.put(PROP_NAME_unitPrice, PROP_ID_unitPrice);
      
          PROP_ID_TO_NAME[PROP_ID_taxRate] = PROP_NAME_taxRate;
          PROP_NAME_TO_ID.put(PROP_NAME_taxRate, PROP_ID_taxRate);
      
          PROP_ID_TO_NAME[PROP_ID_taxRateId] = PROP_NAME_taxRateId;
          PROP_NAME_TO_ID.put(PROP_NAME_taxRateId, PROP_ID_taxRateId);
      
          PROP_ID_TO_NAME[PROP_ID_taxAmount] = PROP_NAME_taxAmount;
          PROP_NAME_TO_ID.put(PROP_NAME_taxAmount, PROP_ID_taxAmount);
      
          PROP_ID_TO_NAME[PROP_ID_amount] = PROP_NAME_amount;
          PROP_NAME_TO_ID.put(PROP_NAME_amount, PROP_ID_amount);
      
          PROP_ID_TO_NAME[PROP_ID_amountWithTax] = PROP_NAME_amountWithTax;
          PROP_NAME_TO_ID.put(PROP_NAME_amountWithTax, PROP_ID_amountWithTax);
      
          PROP_ID_TO_NAME[PROP_ID_deliveredQuantity] = PROP_NAME_deliveredQuantity;
          PROP_NAME_TO_ID.put(PROP_NAME_deliveredQuantity, PROP_ID_deliveredQuantity);
      
          PROP_ID_TO_NAME[PROP_ID_invoicedQuantity] = PROP_NAME_invoicedQuantity;
          PROP_NAME_TO_ID.put(PROP_NAME_invoicedQuantity, PROP_ID_invoicedQuantity);
      
          PROP_ID_TO_NAME[PROP_ID_warehouseId] = PROP_NAME_warehouseId;
          PROP_NAME_TO_ID.put(PROP_NAME_warehouseId, PROP_ID_warehouseId);
      
          PROP_ID_TO_NAME[PROP_ID_projectId] = PROP_NAME_projectId;
          PROP_NAME_TO_ID.put(PROP_NAME_projectId, PROP_ID_projectId);
      
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
      
          PROP_ID_TO_NAME[PROP_ID_remark] = PROP_NAME_remark;
          PROP_NAME_TO_ID.put(PROP_NAME_remark, PROP_ID_remark);
      
    }

    
    /* ID: ID */
    private java.lang.Long _id;
    
    /* 订单ID: ORDER_ID */
    private java.lang.Long _orderId;
    
    /* 行号: LINE_NO */
    private java.lang.Integer _lineNo;
    
    /* 物料: MATERIAL_ID */
    private java.lang.Long _materialId;
    
    /* SKU: SKU_ID */
    private java.lang.Long _skuId;
    
    /* 计量单位: UOM_ID */
    private java.lang.Long _uoMId;
    
    /* 数量: QUANTITY */
    private java.math.BigDecimal _quantity;
    
    /* 单价(不含税): UNIT_PRICE */
    private java.math.BigDecimal _unitPrice;
    
    /* 税率(%): TAX_RATE */
    private java.math.BigDecimal _taxRate;
    
    /* 税率主数据: TAX_RATE_ID */
    private java.lang.Long _taxRateId;
    
    /* 税额: TAX_AMOUNT */
    private java.math.BigDecimal _taxAmount;
    
    /* 金额(不含税): AMOUNT */
    private java.math.BigDecimal _amount;
    
    /* 金额(含税): AMOUNT_WITH_TAX */
    private java.math.BigDecimal _amountWithTax;
    
    /* 已出库数量: DELIVERED_QUANTITY */
    private java.math.BigDecimal _deliveredQuantity;
    
    /* 已开票数量: INVOICED_QUANTITY */
    private java.math.BigDecimal _invoicedQuantity;
    
    /* 发货仓库: WAREHOUSE_ID */
    private java.lang.Long _warehouseId;
    
    /* 项目: PROJECT_ID */
    private java.lang.Long _projectId;
    
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
    
    /* 备注: REMARK */
    private java.lang.String _remark;
    

    public _ErpSalOrderLine(){
        // for debug
    }

    protected ErpSalOrderLine newInstance(){
        ErpSalOrderLine entity = new ErpSalOrderLine();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpSalOrderLine cloneInstance() {
        ErpSalOrderLine entity = newInstance();
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
      return "app.erp.sal.dao.entity.ErpSalOrderLine";
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
        
            case PROP_ID_orderId:
               return getOrderId();
        
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
        
            case PROP_ID_unitPrice:
               return getUnitPrice();
        
            case PROP_ID_taxRate:
               return getTaxRate();
        
            case PROP_ID_taxRateId:
               return getTaxRateId();
        
            case PROP_ID_taxAmount:
               return getTaxAmount();
        
            case PROP_ID_amount:
               return getAmount();
        
            case PROP_ID_amountWithTax:
               return getAmountWithTax();
        
            case PROP_ID_deliveredQuantity:
               return getDeliveredQuantity();
        
            case PROP_ID_invoicedQuantity:
               return getInvoicedQuantity();
        
            case PROP_ID_warehouseId:
               return getWarehouseId();
        
            case PROP_ID_projectId:
               return getProjectId();
        
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
        
            case PROP_ID_remark:
               return getRemark();
        
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
        
            case PROP_ID_orderId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_orderId));
               }
               setOrderId(typedValue);
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
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_quantity));
               }
               setQuantity(typedValue);
               break;
            }
        
            case PROP_ID_unitPrice:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_unitPrice));
               }
               setUnitPrice(typedValue);
               break;
            }
        
            case PROP_ID_taxRate:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_taxRate));
               }
               setTaxRate(typedValue);
               break;
            }
        
            case PROP_ID_taxRateId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_taxRateId));
               }
               setTaxRateId(typedValue);
               break;
            }
        
            case PROP_ID_taxAmount:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_taxAmount));
               }
               setTaxAmount(typedValue);
               break;
            }
        
            case PROP_ID_amount:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_amount));
               }
               setAmount(typedValue);
               break;
            }
        
            case PROP_ID_amountWithTax:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_amountWithTax));
               }
               setAmountWithTax(typedValue);
               break;
            }
        
            case PROP_ID_deliveredQuantity:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_deliveredQuantity));
               }
               setDeliveredQuantity(typedValue);
               break;
            }
        
            case PROP_ID_invoicedQuantity:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_invoicedQuantity));
               }
               setInvoicedQuantity(typedValue);
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
        
            case PROP_ID_projectId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_projectId));
               }
               setProjectId(typedValue);
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
        
            case PROP_ID_remark:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_remark));
               }
               setRemark(typedValue);
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
        
            case PROP_ID_orderId:{
               onInitProp(propId);
               this._orderId = (java.lang.Long)value;
               
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
               this._quantity = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_unitPrice:{
               onInitProp(propId);
               this._unitPrice = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_taxRate:{
               onInitProp(propId);
               this._taxRate = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_taxRateId:{
               onInitProp(propId);
               this._taxRateId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_taxAmount:{
               onInitProp(propId);
               this._taxAmount = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_amount:{
               onInitProp(propId);
               this._amount = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_amountWithTax:{
               onInitProp(propId);
               this._amountWithTax = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_deliveredQuantity:{
               onInitProp(propId);
               this._deliveredQuantity = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_invoicedQuantity:{
               onInitProp(propId);
               this._invoicedQuantity = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_warehouseId:{
               onInitProp(propId);
               this._warehouseId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_projectId:{
               onInitProp(propId);
               this._projectId = (java.lang.Long)value;
               
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
        
            case PROP_ID_remark:{
               onInitProp(propId);
               this._remark = (java.lang.String)value;
               
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
     * 订单ID: ORDER_ID
     */
    public final java.lang.Long getOrderId(){
         onPropGet(PROP_ID_orderId);
         return _orderId;
    }

    /**
     * 订单ID: ORDER_ID
     */
    public final void setOrderId(java.lang.Long value){
        if(onPropSet(PROP_ID_orderId,value)){
            this._orderId = value;
            internalClearRefs(PROP_ID_orderId);
            
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
     * 计量单位: UOM_ID
     */
    public final java.lang.Long getUoMId(){
         onPropGet(PROP_ID_uoMId);
         return _uoMId;
    }

    /**
     * 计量单位: UOM_ID
     */
    public final void setUoMId(java.lang.Long value){
        if(onPropSet(PROP_ID_uoMId,value)){
            this._uoMId = value;
            internalClearRefs(PROP_ID_uoMId);
            
        }
    }
    
    /**
     * 数量: QUANTITY
     */
    public final java.math.BigDecimal getQuantity(){
         onPropGet(PROP_ID_quantity);
         return _quantity;
    }

    /**
     * 数量: QUANTITY
     */
    public final void setQuantity(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_quantity,value)){
            this._quantity = value;
            internalClearRefs(PROP_ID_quantity);
            
        }
    }
    
    /**
     * 单价(不含税): UNIT_PRICE
     */
    public final java.math.BigDecimal getUnitPrice(){
         onPropGet(PROP_ID_unitPrice);
         return _unitPrice;
    }

    /**
     * 单价(不含税): UNIT_PRICE
     */
    public final void setUnitPrice(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_unitPrice,value)){
            this._unitPrice = value;
            internalClearRefs(PROP_ID_unitPrice);
            
        }
    }
    
    /**
     * 税率(%): TAX_RATE
     */
    public final java.math.BigDecimal getTaxRate(){
         onPropGet(PROP_ID_taxRate);
         return _taxRate;
    }

    /**
     * 税率(%): TAX_RATE
     */
    public final void setTaxRate(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_taxRate,value)){
            this._taxRate = value;
            internalClearRefs(PROP_ID_taxRate);
            
        }
    }
    
    /**
     * 税率主数据: TAX_RATE_ID
     */
    public final java.lang.Long getTaxRateId(){
         onPropGet(PROP_ID_taxRateId);
         return _taxRateId;
    }

    /**
     * 税率主数据: TAX_RATE_ID
     */
    public final void setTaxRateId(java.lang.Long value){
        if(onPropSet(PROP_ID_taxRateId,value)){
            this._taxRateId = value;
            internalClearRefs(PROP_ID_taxRateId);
            
        }
    }
    
    /**
     * 税额: TAX_AMOUNT
     */
    public final java.math.BigDecimal getTaxAmount(){
         onPropGet(PROP_ID_taxAmount);
         return _taxAmount;
    }

    /**
     * 税额: TAX_AMOUNT
     */
    public final void setTaxAmount(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_taxAmount,value)){
            this._taxAmount = value;
            internalClearRefs(PROP_ID_taxAmount);
            
        }
    }
    
    /**
     * 金额(不含税): AMOUNT
     */
    public final java.math.BigDecimal getAmount(){
         onPropGet(PROP_ID_amount);
         return _amount;
    }

    /**
     * 金额(不含税): AMOUNT
     */
    public final void setAmount(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_amount,value)){
            this._amount = value;
            internalClearRefs(PROP_ID_amount);
            
        }
    }
    
    /**
     * 金额(含税): AMOUNT_WITH_TAX
     */
    public final java.math.BigDecimal getAmountWithTax(){
         onPropGet(PROP_ID_amountWithTax);
         return _amountWithTax;
    }

    /**
     * 金额(含税): AMOUNT_WITH_TAX
     */
    public final void setAmountWithTax(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_amountWithTax,value)){
            this._amountWithTax = value;
            internalClearRefs(PROP_ID_amountWithTax);
            
        }
    }
    
    /**
     * 已出库数量: DELIVERED_QUANTITY
     */
    public final java.math.BigDecimal getDeliveredQuantity(){
         onPropGet(PROP_ID_deliveredQuantity);
         return _deliveredQuantity;
    }

    /**
     * 已出库数量: DELIVERED_QUANTITY
     */
    public final void setDeliveredQuantity(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_deliveredQuantity,value)){
            this._deliveredQuantity = value;
            internalClearRefs(PROP_ID_deliveredQuantity);
            
        }
    }
    
    /**
     * 已开票数量: INVOICED_QUANTITY
     */
    public final java.math.BigDecimal getInvoicedQuantity(){
         onPropGet(PROP_ID_invoicedQuantity);
         return _invoicedQuantity;
    }

    /**
     * 已开票数量: INVOICED_QUANTITY
     */
    public final void setInvoicedQuantity(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_invoicedQuantity,value)){
            this._invoicedQuantity = value;
            internalClearRefs(PROP_ID_invoicedQuantity);
            
        }
    }
    
    /**
     * 发货仓库: WAREHOUSE_ID
     */
    public final java.lang.Long getWarehouseId(){
         onPropGet(PROP_ID_warehouseId);
         return _warehouseId;
    }

    /**
     * 发货仓库: WAREHOUSE_ID
     */
    public final void setWarehouseId(java.lang.Long value){
        if(onPropSet(PROP_ID_warehouseId,value)){
            this._warehouseId = value;
            internalClearRefs(PROP_ID_warehouseId);
            
        }
    }
    
    /**
     * 项目: PROJECT_ID
     */
    public final java.lang.Long getProjectId(){
         onPropGet(PROP_ID_projectId);
         return _projectId;
    }

    /**
     * 项目: PROJECT_ID
     */
    public final void setProjectId(java.lang.Long value){
        if(onPropSet(PROP_ID_projectId,value)){
            this._projectId = value;
            internalClearRefs(PROP_ID_projectId);
            
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
     * 
     */
    public final app.erp.sal.dao.entity.ErpSalOrder getOrder(){
       return (app.erp.sal.dao.entity.ErpSalOrder)internalGetRefEntity(PROP_NAME_order);
    }

    public final void setOrder(app.erp.sal.dao.entity.ErpSalOrder refEntity){
   
           if(refEntity == null){
           
                   this.setOrderId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_order, refEntity,()->{
           
                           this.setOrderId(refEntity.getId());
                       
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
       
    /**
     * 
     */
    public final app.erp.md.dao.entity.ErpMdTaxRate getTaxRateMd(){
       return (app.erp.md.dao.entity.ErpMdTaxRate)internalGetRefEntity(PROP_NAME_taxRateMd);
    }

    public final void setTaxRateMd(app.erp.md.dao.entity.ErpMdTaxRate refEntity){
   
           if(refEntity == null){
           
                   this.setTaxRateId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_taxRateMd, refEntity,()->{
           
                           this.setTaxRateId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.md.dao.entity.ErpMdWarehouse getWarehouse(){
       return (app.erp.md.dao.entity.ErpMdWarehouse)internalGetRefEntity(PROP_NAME_warehouse);
    }

    public final void setWarehouse(app.erp.md.dao.entity.ErpMdWarehouse refEntity){
   
           if(refEntity == null){
           
                   this.setWarehouseId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_warehouse, refEntity,()->{
           
                           this.setWarehouseId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.prj.dao.entity.ErpPrjProject getProject(){
       return (app.erp.prj.dao.entity.ErpPrjProject)internalGetRefEntity(PROP_NAME_project);
    }

    public final void setProject(app.erp.prj.dao.entity.ErpPrjProject refEntity){
   
           if(refEntity == null){
           
                   this.setProjectId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_project, refEntity,()->{
           
                           this.setProjectId(refEntity.getId());
                       
           });
           }
       
    }
       
}
// resume CPD analysis - CPD-ON

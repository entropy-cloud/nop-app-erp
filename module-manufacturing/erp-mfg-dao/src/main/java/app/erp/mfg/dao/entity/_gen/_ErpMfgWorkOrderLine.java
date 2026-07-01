package app.erp.mfg.dao.entity._gen;

import io.nop.orm.model.IEntityModel;
import io.nop.orm.support.DynamicOrmEntity;
import io.nop.orm.support.OrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code
import io.nop.orm.IOrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code

import io.nop.api.core.convert.ConvertHelper;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;

import app.erp.mfg.dao.entity.ErpMfgWorkOrderLine;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  工单行: erp_mfg_work_order_line
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpMfgWorkOrderLine extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 工单ID: WORK_ORDER_ID BIGINT */
    public static final String PROP_NAME_workOrderId = "workOrderId";
    public static final int PROP_ID_workOrderId = 2;
    
    /* 行号: LINE_NO INTEGER */
    public static final String PROP_NAME_lineNo = "lineNo";
    public static final int PROP_ID_lineNo = 3;
    
    /* 行类型(OUTPUT 产出/INPUT 投入/BYPRODUCT 联副): LINE_TYPE VARCHAR */
    public static final String PROP_NAME_lineType = "lineType";
    public static final int PROP_ID_lineType = 4;
    
    /* 物料: MATERIAL_ID BIGINT */
    public static final String PROP_NAME_materialId = "materialId";
    public static final int PROP_ID_materialId = 5;
    
    /* SKU: SKU_ID BIGINT */
    public static final String PROP_NAME_skuId = "skuId";
    public static final int PROP_ID_skuId = 6;
    
    /* 计量单位: UO_M_ID BIGINT */
    public static final String PROP_NAME_uoMId = "uoMId";
    public static final int PROP_ID_uoMId = 7;
    
    /* 计划数量: PLANNED_QUANTITY DECIMAL */
    public static final String PROP_NAME_plannedQuantity = "plannedQuantity";
    public static final int PROP_ID_plannedQuantity = 8;
    
    /* 实际数量(完工/领用): ACTUAL_QUANTITY DECIMAL */
    public static final String PROP_NAME_actualQuantity = "actualQuantity";
    public static final int PROP_ID_actualQuantity = 9;
    
    /* 报废数量: SCRAPPED_QUANTITY DECIMAL */
    public static final String PROP_NAME_scrappedQuantity = "scrappedQuantity";
    public static final int PROP_ID_scrappedQuantity = 10;
    
    /* 领料仓库(投入用): SOURCE_WAREHOUSE_ID BIGINT */
    public static final String PROP_NAME_sourceWarehouseId = "sourceWarehouseId";
    public static final int PROP_ID_sourceWarehouseId = 11;
    
    /* 入库仓库(产出用): DEST_WAREHOUSE_ID BIGINT */
    public static final String PROP_NAME_destWarehouseId = "destWarehouseId";
    public static final int PROP_ID_destWarehouseId = 12;
    
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
    public static final String PROP_NAME_workOrder = "workOrder";
    
    /* relation:  */
    public static final String PROP_NAME_material = "material";
    
    /* relation:  */
    public static final String PROP_NAME_sku = "sku";
    
    /* relation:  */
    public static final String PROP_NAME_uoM = "uoM";
    
    /* relation:  */
    public static final String PROP_NAME_sourceWarehouse = "sourceWarehouse";
    
    /* relation:  */
    public static final String PROP_NAME_destWarehouse = "destWarehouse";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[20];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_workOrderId] = PROP_NAME_workOrderId;
          PROP_NAME_TO_ID.put(PROP_NAME_workOrderId, PROP_ID_workOrderId);
      
          PROP_ID_TO_NAME[PROP_ID_lineNo] = PROP_NAME_lineNo;
          PROP_NAME_TO_ID.put(PROP_NAME_lineNo, PROP_ID_lineNo);
      
          PROP_ID_TO_NAME[PROP_ID_lineType] = PROP_NAME_lineType;
          PROP_NAME_TO_ID.put(PROP_NAME_lineType, PROP_ID_lineType);
      
          PROP_ID_TO_NAME[PROP_ID_materialId] = PROP_NAME_materialId;
          PROP_NAME_TO_ID.put(PROP_NAME_materialId, PROP_ID_materialId);
      
          PROP_ID_TO_NAME[PROP_ID_skuId] = PROP_NAME_skuId;
          PROP_NAME_TO_ID.put(PROP_NAME_skuId, PROP_ID_skuId);
      
          PROP_ID_TO_NAME[PROP_ID_uoMId] = PROP_NAME_uoMId;
          PROP_NAME_TO_ID.put(PROP_NAME_uoMId, PROP_ID_uoMId);
      
          PROP_ID_TO_NAME[PROP_ID_plannedQuantity] = PROP_NAME_plannedQuantity;
          PROP_NAME_TO_ID.put(PROP_NAME_plannedQuantity, PROP_ID_plannedQuantity);
      
          PROP_ID_TO_NAME[PROP_ID_actualQuantity] = PROP_NAME_actualQuantity;
          PROP_NAME_TO_ID.put(PROP_NAME_actualQuantity, PROP_ID_actualQuantity);
      
          PROP_ID_TO_NAME[PROP_ID_scrappedQuantity] = PROP_NAME_scrappedQuantity;
          PROP_NAME_TO_ID.put(PROP_NAME_scrappedQuantity, PROP_ID_scrappedQuantity);
      
          PROP_ID_TO_NAME[PROP_ID_sourceWarehouseId] = PROP_NAME_sourceWarehouseId;
          PROP_NAME_TO_ID.put(PROP_NAME_sourceWarehouseId, PROP_ID_sourceWarehouseId);
      
          PROP_ID_TO_NAME[PROP_ID_destWarehouseId] = PROP_NAME_destWarehouseId;
          PROP_NAME_TO_ID.put(PROP_NAME_destWarehouseId, PROP_ID_destWarehouseId);
      
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
    
    /* 工单ID: WORK_ORDER_ID */
    private java.lang.Long _workOrderId;
    
    /* 行号: LINE_NO */
    private java.lang.Integer _lineNo;
    
    /* 行类型(OUTPUT 产出/INPUT 投入/BYPRODUCT 联副): LINE_TYPE */
    private java.lang.String _lineType;
    
    /* 物料: MATERIAL_ID */
    private java.lang.Long _materialId;
    
    /* SKU: SKU_ID */
    private java.lang.Long _skuId;
    
    /* 计量单位: UO_M_ID */
    private java.lang.Long _uoMId;
    
    /* 计划数量: PLANNED_QUANTITY */
    private java.math.BigDecimal _plannedQuantity;
    
    /* 实际数量(完工/领用): ACTUAL_QUANTITY */
    private java.math.BigDecimal _actualQuantity;
    
    /* 报废数量: SCRAPPED_QUANTITY */
    private java.math.BigDecimal _scrappedQuantity;
    
    /* 领料仓库(投入用): SOURCE_WAREHOUSE_ID */
    private java.lang.Long _sourceWarehouseId;
    
    /* 入库仓库(产出用): DEST_WAREHOUSE_ID */
    private java.lang.Long _destWarehouseId;
    
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
    

    public _ErpMfgWorkOrderLine(){
        // for debug
    }

    protected ErpMfgWorkOrderLine newInstance(){
        ErpMfgWorkOrderLine entity = new ErpMfgWorkOrderLine();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpMfgWorkOrderLine cloneInstance() {
        ErpMfgWorkOrderLine entity = newInstance();
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
      return "app.erp.mfg.dao.entity.ErpMfgWorkOrderLine";
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
        
            case PROP_ID_workOrderId:
               return getWorkOrderId();
        
            case PROP_ID_lineNo:
               return getLineNo();
        
            case PROP_ID_lineType:
               return getLineType();
        
            case PROP_ID_materialId:
               return getMaterialId();
        
            case PROP_ID_skuId:
               return getSkuId();
        
            case PROP_ID_uoMId:
               return getUoMId();
        
            case PROP_ID_plannedQuantity:
               return getPlannedQuantity();
        
            case PROP_ID_actualQuantity:
               return getActualQuantity();
        
            case PROP_ID_scrappedQuantity:
               return getScrappedQuantity();
        
            case PROP_ID_sourceWarehouseId:
               return getSourceWarehouseId();
        
            case PROP_ID_destWarehouseId:
               return getDestWarehouseId();
        
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
        
            case PROP_ID_workOrderId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_workOrderId));
               }
               setWorkOrderId(typedValue);
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
        
            case PROP_ID_lineType:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_lineType));
               }
               setLineType(typedValue);
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
        
            case PROP_ID_plannedQuantity:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_plannedQuantity));
               }
               setPlannedQuantity(typedValue);
               break;
            }
        
            case PROP_ID_actualQuantity:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_actualQuantity));
               }
               setActualQuantity(typedValue);
               break;
            }
        
            case PROP_ID_scrappedQuantity:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_scrappedQuantity));
               }
               setScrappedQuantity(typedValue);
               break;
            }
        
            case PROP_ID_sourceWarehouseId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_sourceWarehouseId));
               }
               setSourceWarehouseId(typedValue);
               break;
            }
        
            case PROP_ID_destWarehouseId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_destWarehouseId));
               }
               setDestWarehouseId(typedValue);
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
        
            case PROP_ID_workOrderId:{
               onInitProp(propId);
               this._workOrderId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_lineNo:{
               onInitProp(propId);
               this._lineNo = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_lineType:{
               onInitProp(propId);
               this._lineType = (java.lang.String)value;
               
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
        
            case PROP_ID_plannedQuantity:{
               onInitProp(propId);
               this._plannedQuantity = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_actualQuantity:{
               onInitProp(propId);
               this._actualQuantity = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_scrappedQuantity:{
               onInitProp(propId);
               this._scrappedQuantity = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_sourceWarehouseId:{
               onInitProp(propId);
               this._sourceWarehouseId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_destWarehouseId:{
               onInitProp(propId);
               this._destWarehouseId = (java.lang.Long)value;
               
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
     * 工单ID: WORK_ORDER_ID
     */
    public final java.lang.Long getWorkOrderId(){
         onPropGet(PROP_ID_workOrderId);
         return _workOrderId;
    }

    /**
     * 工单ID: WORK_ORDER_ID
     */
    public final void setWorkOrderId(java.lang.Long value){
        if(onPropSet(PROP_ID_workOrderId,value)){
            this._workOrderId = value;
            internalClearRefs(PROP_ID_workOrderId);
            
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
     * 行类型(OUTPUT 产出/INPUT 投入/BYPRODUCT 联副): LINE_TYPE
     */
    public final java.lang.String getLineType(){
         onPropGet(PROP_ID_lineType);
         return _lineType;
    }

    /**
     * 行类型(OUTPUT 产出/INPUT 投入/BYPRODUCT 联副): LINE_TYPE
     */
    public final void setLineType(java.lang.String value){
        if(onPropSet(PROP_ID_lineType,value)){
            this._lineType = value;
            internalClearRefs(PROP_ID_lineType);
            
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
     * 计划数量: PLANNED_QUANTITY
     */
    public final java.math.BigDecimal getPlannedQuantity(){
         onPropGet(PROP_ID_plannedQuantity);
         return _plannedQuantity;
    }

    /**
     * 计划数量: PLANNED_QUANTITY
     */
    public final void setPlannedQuantity(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_plannedQuantity,value)){
            this._plannedQuantity = value;
            internalClearRefs(PROP_ID_plannedQuantity);
            
        }
    }
    
    /**
     * 实际数量(完工/领用): ACTUAL_QUANTITY
     */
    public final java.math.BigDecimal getActualQuantity(){
         onPropGet(PROP_ID_actualQuantity);
         return _actualQuantity;
    }

    /**
     * 实际数量(完工/领用): ACTUAL_QUANTITY
     */
    public final void setActualQuantity(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_actualQuantity,value)){
            this._actualQuantity = value;
            internalClearRefs(PROP_ID_actualQuantity);
            
        }
    }
    
    /**
     * 报废数量: SCRAPPED_QUANTITY
     */
    public final java.math.BigDecimal getScrappedQuantity(){
         onPropGet(PROP_ID_scrappedQuantity);
         return _scrappedQuantity;
    }

    /**
     * 报废数量: SCRAPPED_QUANTITY
     */
    public final void setScrappedQuantity(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_scrappedQuantity,value)){
            this._scrappedQuantity = value;
            internalClearRefs(PROP_ID_scrappedQuantity);
            
        }
    }
    
    /**
     * 领料仓库(投入用): SOURCE_WAREHOUSE_ID
     */
    public final java.lang.Long getSourceWarehouseId(){
         onPropGet(PROP_ID_sourceWarehouseId);
         return _sourceWarehouseId;
    }

    /**
     * 领料仓库(投入用): SOURCE_WAREHOUSE_ID
     */
    public final void setSourceWarehouseId(java.lang.Long value){
        if(onPropSet(PROP_ID_sourceWarehouseId,value)){
            this._sourceWarehouseId = value;
            internalClearRefs(PROP_ID_sourceWarehouseId);
            
        }
    }
    
    /**
     * 入库仓库(产出用): DEST_WAREHOUSE_ID
     */
    public final java.lang.Long getDestWarehouseId(){
         onPropGet(PROP_ID_destWarehouseId);
         return _destWarehouseId;
    }

    /**
     * 入库仓库(产出用): DEST_WAREHOUSE_ID
     */
    public final void setDestWarehouseId(java.lang.Long value){
        if(onPropSet(PROP_ID_destWarehouseId,value)){
            this._destWarehouseId = value;
            internalClearRefs(PROP_ID_destWarehouseId);
            
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
    public final app.erp.mfg.dao.entity.ErpMfgWorkOrder getWorkOrder(){
       return (app.erp.mfg.dao.entity.ErpMfgWorkOrder)internalGetRefEntity(PROP_NAME_workOrder);
    }

    public final void setWorkOrder(app.erp.mfg.dao.entity.ErpMfgWorkOrder refEntity){
   
           if(refEntity == null){
           
                   this.setWorkOrderId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_workOrder, refEntity,()->{
           
                           this.setWorkOrderId(refEntity.getId());
                       
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
    public final app.erp.md.dao.entity.ErpMdWarehouse getSourceWarehouse(){
       return (app.erp.md.dao.entity.ErpMdWarehouse)internalGetRefEntity(PROP_NAME_sourceWarehouse);
    }

    public final void setSourceWarehouse(app.erp.md.dao.entity.ErpMdWarehouse refEntity){
   
           if(refEntity == null){
           
                   this.setSourceWarehouseId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_sourceWarehouse, refEntity,()->{
           
                           this.setSourceWarehouseId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.md.dao.entity.ErpMdWarehouse getDestWarehouse(){
       return (app.erp.md.dao.entity.ErpMdWarehouse)internalGetRefEntity(PROP_NAME_destWarehouse);
    }

    public final void setDestWarehouse(app.erp.md.dao.entity.ErpMdWarehouse refEntity){
   
           if(refEntity == null){
           
                   this.setDestWarehouseId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_destWarehouse, refEntity,()->{
           
                           this.setDestWarehouseId(refEntity.getId());
                       
           });
           }
       
    }
       
}
// resume CPD analysis - CPD-ON

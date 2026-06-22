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

import app.erp.mfg.dao.entity.ErpMfgCostRollupLine;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  标准成本滚算行: erp_mfg_cost_rollup_line
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpMfgCostRollupLine extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 滚算单ID: COST_ROLLUP_ID BIGINT */
    public static final String PROP_NAME_costRollupId = "costRollupId";
    public static final int PROP_ID_costRollupId = 2;
    
    /* 行号: LINE_NO INTEGER */
    public static final String PROP_NAME_lineNo = "lineNo";
    public static final int PROP_ID_lineNo = 3;
    
    /* 产品: MATERIAL_ID BIGINT */
    public static final String PROP_NAME_materialId = "materialId";
    public static final int PROP_ID_materialId = 4;
    
    /* 计量单位: UO_M_ID BIGINT */
    public static final String PROP_NAME_uoMId = "uoMId";
    public static final int PROP_ID_uoMId = 5;
    
    /* 材料成本: MATERIAL_COST DECIMAL */
    public static final String PROP_NAME_materialCost = "materialCost";
    public static final int PROP_ID_materialCost = 6;
    
    /* 人工成本: LABOR_COST DECIMAL */
    public static final String PROP_NAME_laborCost = "laborCost";
    public static final int PROP_ID_laborCost = 7;
    
    /* 制造费用: OVERHEAD_COST DECIMAL */
    public static final String PROP_NAME_overheadCost = "overheadCost";
    public static final int PROP_ID_overheadCost = 8;
    
    /* 委外成本: SUBCONTRACT_COST DECIMAL */
    public static final String PROP_NAME_subcontractCost = "subcontractCost";
    public static final int PROP_ID_subcontractCost = 9;
    
    /* 总成本: TOTAL_COST DECIMAL */
    public static final String PROP_NAME_totalCost = "totalCost";
    public static final int PROP_ID_totalCost = 10;
    
    /* 单位标准成本: UNIT_COST DECIMAL */
    public static final String PROP_NAME_unitCost = "unitCost";
    public static final int PROP_ID_unitCost = 11;
    
    /* 币种: CURRENCY_ID BIGINT */
    public static final String PROP_NAME_currencyId = "currencyId";
    public static final int PROP_ID_currencyId = 12;
    
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
    public static final String PROP_NAME_costRollup = "costRollup";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[19];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_costRollupId] = PROP_NAME_costRollupId;
          PROP_NAME_TO_ID.put(PROP_NAME_costRollupId, PROP_ID_costRollupId);
      
          PROP_ID_TO_NAME[PROP_ID_lineNo] = PROP_NAME_lineNo;
          PROP_NAME_TO_ID.put(PROP_NAME_lineNo, PROP_ID_lineNo);
      
          PROP_ID_TO_NAME[PROP_ID_materialId] = PROP_NAME_materialId;
          PROP_NAME_TO_ID.put(PROP_NAME_materialId, PROP_ID_materialId);
      
          PROP_ID_TO_NAME[PROP_ID_uoMId] = PROP_NAME_uoMId;
          PROP_NAME_TO_ID.put(PROP_NAME_uoMId, PROP_ID_uoMId);
      
          PROP_ID_TO_NAME[PROP_ID_materialCost] = PROP_NAME_materialCost;
          PROP_NAME_TO_ID.put(PROP_NAME_materialCost, PROP_ID_materialCost);
      
          PROP_ID_TO_NAME[PROP_ID_laborCost] = PROP_NAME_laborCost;
          PROP_NAME_TO_ID.put(PROP_NAME_laborCost, PROP_ID_laborCost);
      
          PROP_ID_TO_NAME[PROP_ID_overheadCost] = PROP_NAME_overheadCost;
          PROP_NAME_TO_ID.put(PROP_NAME_overheadCost, PROP_ID_overheadCost);
      
          PROP_ID_TO_NAME[PROP_ID_subcontractCost] = PROP_NAME_subcontractCost;
          PROP_NAME_TO_ID.put(PROP_NAME_subcontractCost, PROP_ID_subcontractCost);
      
          PROP_ID_TO_NAME[PROP_ID_totalCost] = PROP_NAME_totalCost;
          PROP_NAME_TO_ID.put(PROP_NAME_totalCost, PROP_ID_totalCost);
      
          PROP_ID_TO_NAME[PROP_ID_unitCost] = PROP_NAME_unitCost;
          PROP_NAME_TO_ID.put(PROP_NAME_unitCost, PROP_ID_unitCost);
      
          PROP_ID_TO_NAME[PROP_ID_currencyId] = PROP_NAME_currencyId;
          PROP_NAME_TO_ID.put(PROP_NAME_currencyId, PROP_ID_currencyId);
      
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
    
    /* 滚算单ID: COST_ROLLUP_ID */
    private java.lang.Long _costRollupId;
    
    /* 行号: LINE_NO */
    private java.lang.Integer _lineNo;
    
    /* 产品: MATERIAL_ID */
    private java.lang.Long _materialId;
    
    /* 计量单位: UO_M_ID */
    private java.lang.Long _uoMId;
    
    /* 材料成本: MATERIAL_COST */
    private java.lang.String _materialCost;
    
    /* 人工成本: LABOR_COST */
    private java.lang.String _laborCost;
    
    /* 制造费用: OVERHEAD_COST */
    private java.lang.String _overheadCost;
    
    /* 委外成本: SUBCONTRACT_COST */
    private java.lang.String _subcontractCost;
    
    /* 总成本: TOTAL_COST */
    private java.lang.String _totalCost;
    
    /* 单位标准成本: UNIT_COST */
    private java.lang.String _unitCost;
    
    /* 币种: CURRENCY_ID */
    private java.lang.Long _currencyId;
    
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
    

    public _ErpMfgCostRollupLine(){
        // for debug
    }

    protected ErpMfgCostRollupLine newInstance(){
        ErpMfgCostRollupLine entity = new ErpMfgCostRollupLine();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpMfgCostRollupLine cloneInstance() {
        ErpMfgCostRollupLine entity = newInstance();
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
      return "app.erp.mfg.dao.entity.ErpMfgCostRollupLine";
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
        
            case PROP_ID_costRollupId:
               return getCostRollupId();
        
            case PROP_ID_lineNo:
               return getLineNo();
        
            case PROP_ID_materialId:
               return getMaterialId();
        
            case PROP_ID_uoMId:
               return getUoMId();
        
            case PROP_ID_materialCost:
               return getMaterialCost();
        
            case PROP_ID_laborCost:
               return getLaborCost();
        
            case PROP_ID_overheadCost:
               return getOverheadCost();
        
            case PROP_ID_subcontractCost:
               return getSubcontractCost();
        
            case PROP_ID_totalCost:
               return getTotalCost();
        
            case PROP_ID_unitCost:
               return getUnitCost();
        
            case PROP_ID_currencyId:
               return getCurrencyId();
        
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
        
            case PROP_ID_costRollupId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_costRollupId));
               }
               setCostRollupId(typedValue);
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
        
            case PROP_ID_uoMId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_uoMId));
               }
               setUoMId(typedValue);
               break;
            }
        
            case PROP_ID_materialCost:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_materialCost));
               }
               setMaterialCost(typedValue);
               break;
            }
        
            case PROP_ID_laborCost:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_laborCost));
               }
               setLaborCost(typedValue);
               break;
            }
        
            case PROP_ID_overheadCost:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_overheadCost));
               }
               setOverheadCost(typedValue);
               break;
            }
        
            case PROP_ID_subcontractCost:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_subcontractCost));
               }
               setSubcontractCost(typedValue);
               break;
            }
        
            case PROP_ID_totalCost:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_totalCost));
               }
               setTotalCost(typedValue);
               break;
            }
        
            case PROP_ID_unitCost:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_unitCost));
               }
               setUnitCost(typedValue);
               break;
            }
        
            case PROP_ID_currencyId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_currencyId));
               }
               setCurrencyId(typedValue);
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
        
            case PROP_ID_costRollupId:{
               onInitProp(propId);
               this._costRollupId = (java.lang.Long)value;
               
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
        
            case PROP_ID_uoMId:{
               onInitProp(propId);
               this._uoMId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_materialCost:{
               onInitProp(propId);
               this._materialCost = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_laborCost:{
               onInitProp(propId);
               this._laborCost = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_overheadCost:{
               onInitProp(propId);
               this._overheadCost = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_subcontractCost:{
               onInitProp(propId);
               this._subcontractCost = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_totalCost:{
               onInitProp(propId);
               this._totalCost = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_unitCost:{
               onInitProp(propId);
               this._unitCost = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_currencyId:{
               onInitProp(propId);
               this._currencyId = (java.lang.Long)value;
               
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
     * 滚算单ID: COST_ROLLUP_ID
     */
    public final java.lang.Long getCostRollupId(){
         onPropGet(PROP_ID_costRollupId);
         return _costRollupId;
    }

    /**
     * 滚算单ID: COST_ROLLUP_ID
     */
    public final void setCostRollupId(java.lang.Long value){
        if(onPropSet(PROP_ID_costRollupId,value)){
            this._costRollupId = value;
            internalClearRefs(PROP_ID_costRollupId);
            
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
     * 产品: MATERIAL_ID
     */
    public final java.lang.Long getMaterialId(){
         onPropGet(PROP_ID_materialId);
         return _materialId;
    }

    /**
     * 产品: MATERIAL_ID
     */
    public final void setMaterialId(java.lang.Long value){
        if(onPropSet(PROP_ID_materialId,value)){
            this._materialId = value;
            internalClearRefs(PROP_ID_materialId);
            
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
     * 材料成本: MATERIAL_COST
     */
    public final java.lang.String getMaterialCost(){
         onPropGet(PROP_ID_materialCost);
         return _materialCost;
    }

    /**
     * 材料成本: MATERIAL_COST
     */
    public final void setMaterialCost(java.lang.String value){
        if(onPropSet(PROP_ID_materialCost,value)){
            this._materialCost = value;
            internalClearRefs(PROP_ID_materialCost);
            
        }
    }
    
    /**
     * 人工成本: LABOR_COST
     */
    public final java.lang.String getLaborCost(){
         onPropGet(PROP_ID_laborCost);
         return _laborCost;
    }

    /**
     * 人工成本: LABOR_COST
     */
    public final void setLaborCost(java.lang.String value){
        if(onPropSet(PROP_ID_laborCost,value)){
            this._laborCost = value;
            internalClearRefs(PROP_ID_laborCost);
            
        }
    }
    
    /**
     * 制造费用: OVERHEAD_COST
     */
    public final java.lang.String getOverheadCost(){
         onPropGet(PROP_ID_overheadCost);
         return _overheadCost;
    }

    /**
     * 制造费用: OVERHEAD_COST
     */
    public final void setOverheadCost(java.lang.String value){
        if(onPropSet(PROP_ID_overheadCost,value)){
            this._overheadCost = value;
            internalClearRefs(PROP_ID_overheadCost);
            
        }
    }
    
    /**
     * 委外成本: SUBCONTRACT_COST
     */
    public final java.lang.String getSubcontractCost(){
         onPropGet(PROP_ID_subcontractCost);
         return _subcontractCost;
    }

    /**
     * 委外成本: SUBCONTRACT_COST
     */
    public final void setSubcontractCost(java.lang.String value){
        if(onPropSet(PROP_ID_subcontractCost,value)){
            this._subcontractCost = value;
            internalClearRefs(PROP_ID_subcontractCost);
            
        }
    }
    
    /**
     * 总成本: TOTAL_COST
     */
    public final java.lang.String getTotalCost(){
         onPropGet(PROP_ID_totalCost);
         return _totalCost;
    }

    /**
     * 总成本: TOTAL_COST
     */
    public final void setTotalCost(java.lang.String value){
        if(onPropSet(PROP_ID_totalCost,value)){
            this._totalCost = value;
            internalClearRefs(PROP_ID_totalCost);
            
        }
    }
    
    /**
     * 单位标准成本: UNIT_COST
     */
    public final java.lang.String getUnitCost(){
         onPropGet(PROP_ID_unitCost);
         return _unitCost;
    }

    /**
     * 单位标准成本: UNIT_COST
     */
    public final void setUnitCost(java.lang.String value){
        if(onPropSet(PROP_ID_unitCost,value)){
            this._unitCost = value;
            internalClearRefs(PROP_ID_unitCost);
            
        }
    }
    
    /**
     * 币种: CURRENCY_ID
     */
    public final java.lang.Long getCurrencyId(){
         onPropGet(PROP_ID_currencyId);
         return _currencyId;
    }

    /**
     * 币种: CURRENCY_ID
     */
    public final void setCurrencyId(java.lang.Long value){
        if(onPropSet(PROP_ID_currencyId,value)){
            this._currencyId = value;
            internalClearRefs(PROP_ID_currencyId);
            
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
    public final app.erp.mfg.dao.entity.ErpMfgCostRollup getCostRollup(){
       return (app.erp.mfg.dao.entity.ErpMfgCostRollup)internalGetRefEntity(PROP_NAME_costRollup);
    }

    public final void setCostRollup(app.erp.mfg.dao.entity.ErpMfgCostRollup refEntity){
   
           if(refEntity == null){
           
                   this.setCostRollupId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_costRollup, refEntity,()->{
           
                           this.setCostRollupId(refEntity.getId());
                       
           });
           }
       
    }
       
}
// resume CPD analysis - CPD-ON

package app.erp.ast.dao.entity._gen;

import io.nop.orm.model.IEntityModel;
import io.nop.orm.support.DynamicOrmEntity;
import io.nop.orm.support.OrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code
import io.nop.orm.IOrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code

import io.nop.api.core.convert.ConvertHelper;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;

import app.erp.ast.dao.entity.ErpAstSplitLine;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  资产拆分行: erp_ast_split_line
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpAstSplitLine extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 拆分单: SPLIT_ID BIGINT */
    public static final String PROP_NAME_splitId = "splitId";
    public static final int PROP_ID_splitId = 2;
    
    /* 所属组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 3;
    
    /* 行号: LINE_NO INTEGER */
    public static final String PROP_NAME_lineNo = "lineNo";
    public static final int PROP_ID_lineNo = 4;
    
    /* 目标资产编码: TARGET_ASSET_CODE VARCHAR */
    public static final String PROP_NAME_targetAssetCode = "targetAssetCode";
    public static final int PROP_ID_targetAssetCode = 5;
    
    /* 目标资产名称: TARGET_ASSET_NAME VARCHAR */
    public static final String PROP_NAME_targetAssetName = "targetAssetName";
    public static final int PROP_ID_targetAssetName = 6;
    
    /* 目标资产类别: CATEGORY_ID BIGINT */
    public static final String PROP_NAME_categoryId = "categoryId";
    public static final int PROP_ID_categoryId = 7;
    
    /* 分摊方式: ALLOCATION_METHOD VARCHAR */
    public static final String PROP_NAME_allocationMethod = "allocationMethod";
    public static final int PROP_ID_allocationMethod = 8;
    
    /* 比例: PROPORTION DECIMAL */
    public static final String PROP_NAME_proportion = "proportion";
    public static final int PROP_ID_proportion = 9;
    
    /* 原值金额: ORIGINAL_COST_AMOUNT DECIMAL */
    public static final String PROP_NAME_originalCostAmount = "originalCostAmount";
    public static final int PROP_ID_originalCostAmount = 10;
    
    /* 累计折旧金额: ACCUMULATED_DEPRECIATION_AMOUNT DECIMAL */
    public static final String PROP_NAME_accumulatedDepreciationAmount = "accumulatedDepreciationAmount";
    public static final int PROP_ID_accumulatedDepreciationAmount = 11;
    
    /* 账面净值: NET_BOOK_VALUE DECIMAL */
    public static final String PROP_NAME_netBookValue = "netBookValue";
    public static final int PROP_ID_netBookValue = 12;
    
    /* 目标资产: TARGET_ASSET_ID BIGINT */
    public static final String PROP_NAME_targetAssetId = "targetAssetId";
    public static final int PROP_ID_targetAssetId = 13;
    
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
    
    /* 备注: REMARK VARCHAR */
    public static final String PROP_NAME_remark = "remark";
    public static final int PROP_ID_remark = 20;
    

    private static int _PROP_ID_BOUND = 21;

    
    /* relation:  */
    public static final String PROP_NAME_split = "split";
    
    /* relation:  */
    public static final String PROP_NAME_org = "org";
    
    /* relation:  */
    public static final String PROP_NAME_category = "category";
    
    /* relation:  */
    public static final String PROP_NAME_targetAsset = "targetAsset";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[21];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_splitId] = PROP_NAME_splitId;
          PROP_NAME_TO_ID.put(PROP_NAME_splitId, PROP_ID_splitId);
      
          PROP_ID_TO_NAME[PROP_ID_orgId] = PROP_NAME_orgId;
          PROP_NAME_TO_ID.put(PROP_NAME_orgId, PROP_ID_orgId);
      
          PROP_ID_TO_NAME[PROP_ID_lineNo] = PROP_NAME_lineNo;
          PROP_NAME_TO_ID.put(PROP_NAME_lineNo, PROP_ID_lineNo);
      
          PROP_ID_TO_NAME[PROP_ID_targetAssetCode] = PROP_NAME_targetAssetCode;
          PROP_NAME_TO_ID.put(PROP_NAME_targetAssetCode, PROP_ID_targetAssetCode);
      
          PROP_ID_TO_NAME[PROP_ID_targetAssetName] = PROP_NAME_targetAssetName;
          PROP_NAME_TO_ID.put(PROP_NAME_targetAssetName, PROP_ID_targetAssetName);
      
          PROP_ID_TO_NAME[PROP_ID_categoryId] = PROP_NAME_categoryId;
          PROP_NAME_TO_ID.put(PROP_NAME_categoryId, PROP_ID_categoryId);
      
          PROP_ID_TO_NAME[PROP_ID_allocationMethod] = PROP_NAME_allocationMethod;
          PROP_NAME_TO_ID.put(PROP_NAME_allocationMethod, PROP_ID_allocationMethod);
      
          PROP_ID_TO_NAME[PROP_ID_proportion] = PROP_NAME_proportion;
          PROP_NAME_TO_ID.put(PROP_NAME_proportion, PROP_ID_proportion);
      
          PROP_ID_TO_NAME[PROP_ID_originalCostAmount] = PROP_NAME_originalCostAmount;
          PROP_NAME_TO_ID.put(PROP_NAME_originalCostAmount, PROP_ID_originalCostAmount);
      
          PROP_ID_TO_NAME[PROP_ID_accumulatedDepreciationAmount] = PROP_NAME_accumulatedDepreciationAmount;
          PROP_NAME_TO_ID.put(PROP_NAME_accumulatedDepreciationAmount, PROP_ID_accumulatedDepreciationAmount);
      
          PROP_ID_TO_NAME[PROP_ID_netBookValue] = PROP_NAME_netBookValue;
          PROP_NAME_TO_ID.put(PROP_NAME_netBookValue, PROP_ID_netBookValue);
      
          PROP_ID_TO_NAME[PROP_ID_targetAssetId] = PROP_NAME_targetAssetId;
          PROP_NAME_TO_ID.put(PROP_NAME_targetAssetId, PROP_ID_targetAssetId);
      
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
    
    /* 拆分单: SPLIT_ID */
    private java.lang.Long _splitId;
    
    /* 所属组织: ORG_ID */
    private java.lang.Long _orgId;
    
    /* 行号: LINE_NO */
    private java.lang.Integer _lineNo;
    
    /* 目标资产编码: TARGET_ASSET_CODE */
    private java.lang.String _targetAssetCode;
    
    /* 目标资产名称: TARGET_ASSET_NAME */
    private java.lang.String _targetAssetName;
    
    /* 目标资产类别: CATEGORY_ID */
    private java.lang.Long _categoryId;
    
    /* 分摊方式: ALLOCATION_METHOD */
    private java.lang.String _allocationMethod;
    
    /* 比例: PROPORTION */
    private java.math.BigDecimal _proportion;
    
    /* 原值金额: ORIGINAL_COST_AMOUNT */
    private java.math.BigDecimal _originalCostAmount;
    
    /* 累计折旧金额: ACCUMULATED_DEPRECIATION_AMOUNT */
    private java.math.BigDecimal _accumulatedDepreciationAmount;
    
    /* 账面净值: NET_BOOK_VALUE */
    private java.math.BigDecimal _netBookValue;
    
    /* 目标资产: TARGET_ASSET_ID */
    private java.lang.Long _targetAssetId;
    
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
    

    public _ErpAstSplitLine(){
        // for debug
    }

    protected ErpAstSplitLine newInstance(){
        ErpAstSplitLine entity = new ErpAstSplitLine();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpAstSplitLine cloneInstance() {
        ErpAstSplitLine entity = newInstance();
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
      return "app.erp.ast.dao.entity.ErpAstSplitLine";
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
        
            case PROP_ID_splitId:
               return getSplitId();
        
            case PROP_ID_orgId:
               return getOrgId();
        
            case PROP_ID_lineNo:
               return getLineNo();
        
            case PROP_ID_targetAssetCode:
               return getTargetAssetCode();
        
            case PROP_ID_targetAssetName:
               return getTargetAssetName();
        
            case PROP_ID_categoryId:
               return getCategoryId();
        
            case PROP_ID_allocationMethod:
               return getAllocationMethod();
        
            case PROP_ID_proportion:
               return getProportion();
        
            case PROP_ID_originalCostAmount:
               return getOriginalCostAmount();
        
            case PROP_ID_accumulatedDepreciationAmount:
               return getAccumulatedDepreciationAmount();
        
            case PROP_ID_netBookValue:
               return getNetBookValue();
        
            case PROP_ID_targetAssetId:
               return getTargetAssetId();
        
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
        
            case PROP_ID_splitId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_splitId));
               }
               setSplitId(typedValue);
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
        
            case PROP_ID_lineNo:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_lineNo));
               }
               setLineNo(typedValue);
               break;
            }
        
            case PROP_ID_targetAssetCode:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_targetAssetCode));
               }
               setTargetAssetCode(typedValue);
               break;
            }
        
            case PROP_ID_targetAssetName:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_targetAssetName));
               }
               setTargetAssetName(typedValue);
               break;
            }
        
            case PROP_ID_categoryId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_categoryId));
               }
               setCategoryId(typedValue);
               break;
            }
        
            case PROP_ID_allocationMethod:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_allocationMethod));
               }
               setAllocationMethod(typedValue);
               break;
            }
        
            case PROP_ID_proportion:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_proportion));
               }
               setProportion(typedValue);
               break;
            }
        
            case PROP_ID_originalCostAmount:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_originalCostAmount));
               }
               setOriginalCostAmount(typedValue);
               break;
            }
        
            case PROP_ID_accumulatedDepreciationAmount:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_accumulatedDepreciationAmount));
               }
               setAccumulatedDepreciationAmount(typedValue);
               break;
            }
        
            case PROP_ID_netBookValue:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_netBookValue));
               }
               setNetBookValue(typedValue);
               break;
            }
        
            case PROP_ID_targetAssetId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_targetAssetId));
               }
               setTargetAssetId(typedValue);
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
        
            case PROP_ID_splitId:{
               onInitProp(propId);
               this._splitId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_orgId:{
               onInitProp(propId);
               this._orgId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_lineNo:{
               onInitProp(propId);
               this._lineNo = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_targetAssetCode:{
               onInitProp(propId);
               this._targetAssetCode = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_targetAssetName:{
               onInitProp(propId);
               this._targetAssetName = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_categoryId:{
               onInitProp(propId);
               this._categoryId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_allocationMethod:{
               onInitProp(propId);
               this._allocationMethod = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_proportion:{
               onInitProp(propId);
               this._proportion = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_originalCostAmount:{
               onInitProp(propId);
               this._originalCostAmount = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_accumulatedDepreciationAmount:{
               onInitProp(propId);
               this._accumulatedDepreciationAmount = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_netBookValue:{
               onInitProp(propId);
               this._netBookValue = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_targetAssetId:{
               onInitProp(propId);
               this._targetAssetId = (java.lang.Long)value;
               
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
     * 拆分单: SPLIT_ID
     */
    public final java.lang.Long getSplitId(){
         onPropGet(PROP_ID_splitId);
         return _splitId;
    }

    /**
     * 拆分单: SPLIT_ID
     */
    public final void setSplitId(java.lang.Long value){
        if(onPropSet(PROP_ID_splitId,value)){
            this._splitId = value;
            internalClearRefs(PROP_ID_splitId);
            
        }
    }
    
    /**
     * 所属组织: ORG_ID
     */
    public final java.lang.Long getOrgId(){
         onPropGet(PROP_ID_orgId);
         return _orgId;
    }

    /**
     * 所属组织: ORG_ID
     */
    public final void setOrgId(java.lang.Long value){
        if(onPropSet(PROP_ID_orgId,value)){
            this._orgId = value;
            internalClearRefs(PROP_ID_orgId);
            
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
     * 目标资产编码: TARGET_ASSET_CODE
     */
    public final java.lang.String getTargetAssetCode(){
         onPropGet(PROP_ID_targetAssetCode);
         return _targetAssetCode;
    }

    /**
     * 目标资产编码: TARGET_ASSET_CODE
     */
    public final void setTargetAssetCode(java.lang.String value){
        if(onPropSet(PROP_ID_targetAssetCode,value)){
            this._targetAssetCode = value;
            internalClearRefs(PROP_ID_targetAssetCode);
            
        }
    }
    
    /**
     * 目标资产名称: TARGET_ASSET_NAME
     */
    public final java.lang.String getTargetAssetName(){
         onPropGet(PROP_ID_targetAssetName);
         return _targetAssetName;
    }

    /**
     * 目标资产名称: TARGET_ASSET_NAME
     */
    public final void setTargetAssetName(java.lang.String value){
        if(onPropSet(PROP_ID_targetAssetName,value)){
            this._targetAssetName = value;
            internalClearRefs(PROP_ID_targetAssetName);
            
        }
    }
    
    /**
     * 目标资产类别: CATEGORY_ID
     */
    public final java.lang.Long getCategoryId(){
         onPropGet(PROP_ID_categoryId);
         return _categoryId;
    }

    /**
     * 目标资产类别: CATEGORY_ID
     */
    public final void setCategoryId(java.lang.Long value){
        if(onPropSet(PROP_ID_categoryId,value)){
            this._categoryId = value;
            internalClearRefs(PROP_ID_categoryId);
            
        }
    }
    
    /**
     * 分摊方式: ALLOCATION_METHOD
     */
    public final java.lang.String getAllocationMethod(){
         onPropGet(PROP_ID_allocationMethod);
         return _allocationMethod;
    }

    /**
     * 分摊方式: ALLOCATION_METHOD
     */
    public final void setAllocationMethod(java.lang.String value){
        if(onPropSet(PROP_ID_allocationMethod,value)){
            this._allocationMethod = value;
            internalClearRefs(PROP_ID_allocationMethod);
            
        }
    }
    
    /**
     * 比例: PROPORTION
     */
    public final java.math.BigDecimal getProportion(){
         onPropGet(PROP_ID_proportion);
         return _proportion;
    }

    /**
     * 比例: PROPORTION
     */
    public final void setProportion(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_proportion,value)){
            this._proportion = value;
            internalClearRefs(PROP_ID_proportion);
            
        }
    }
    
    /**
     * 原值金额: ORIGINAL_COST_AMOUNT
     */
    public final java.math.BigDecimal getOriginalCostAmount(){
         onPropGet(PROP_ID_originalCostAmount);
         return _originalCostAmount;
    }

    /**
     * 原值金额: ORIGINAL_COST_AMOUNT
     */
    public final void setOriginalCostAmount(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_originalCostAmount,value)){
            this._originalCostAmount = value;
            internalClearRefs(PROP_ID_originalCostAmount);
            
        }
    }
    
    /**
     * 累计折旧金额: ACCUMULATED_DEPRECIATION_AMOUNT
     */
    public final java.math.BigDecimal getAccumulatedDepreciationAmount(){
         onPropGet(PROP_ID_accumulatedDepreciationAmount);
         return _accumulatedDepreciationAmount;
    }

    /**
     * 累计折旧金额: ACCUMULATED_DEPRECIATION_AMOUNT
     */
    public final void setAccumulatedDepreciationAmount(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_accumulatedDepreciationAmount,value)){
            this._accumulatedDepreciationAmount = value;
            internalClearRefs(PROP_ID_accumulatedDepreciationAmount);
            
        }
    }
    
    /**
     * 账面净值: NET_BOOK_VALUE
     */
    public final java.math.BigDecimal getNetBookValue(){
         onPropGet(PROP_ID_netBookValue);
         return _netBookValue;
    }

    /**
     * 账面净值: NET_BOOK_VALUE
     */
    public final void setNetBookValue(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_netBookValue,value)){
            this._netBookValue = value;
            internalClearRefs(PROP_ID_netBookValue);
            
        }
    }
    
    /**
     * 目标资产: TARGET_ASSET_ID
     */
    public final java.lang.Long getTargetAssetId(){
         onPropGet(PROP_ID_targetAssetId);
         return _targetAssetId;
    }

    /**
     * 目标资产: TARGET_ASSET_ID
     */
    public final void setTargetAssetId(java.lang.Long value){
        if(onPropSet(PROP_ID_targetAssetId,value)){
            this._targetAssetId = value;
            internalClearRefs(PROP_ID_targetAssetId);
            
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
    public final app.erp.ast.dao.entity.ErpAstSplit getSplit(){
       return (app.erp.ast.dao.entity.ErpAstSplit)internalGetRefEntity(PROP_NAME_split);
    }

    public final void setSplit(app.erp.ast.dao.entity.ErpAstSplit refEntity){
   
           if(refEntity == null){
           
                   this.setSplitId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_split, refEntity,()->{
           
                           this.setSplitId(refEntity.getId());
                       
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
       
    /**
     * 
     */
    public final app.erp.ast.dao.entity.ErpAstAssetCategory getCategory(){
       return (app.erp.ast.dao.entity.ErpAstAssetCategory)internalGetRefEntity(PROP_NAME_category);
    }

    public final void setCategory(app.erp.ast.dao.entity.ErpAstAssetCategory refEntity){
   
           if(refEntity == null){
           
                   this.setCategoryId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_category, refEntity,()->{
           
                           this.setCategoryId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.ast.dao.entity.ErpAstAsset getTargetAsset(){
       return (app.erp.ast.dao.entity.ErpAstAsset)internalGetRefEntity(PROP_NAME_targetAsset);
    }

    public final void setTargetAsset(app.erp.ast.dao.entity.ErpAstAsset refEntity){
   
           if(refEntity == null){
           
                   this.setTargetAssetId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_targetAsset, refEntity,()->{
           
                           this.setTargetAssetId(refEntity.getId());
                       
           });
           }
       
    }
       
}
// resume CPD analysis - CPD-ON

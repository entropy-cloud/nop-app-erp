package app.erp.prj.dao.entity._gen;

import io.nop.orm.model.IEntityModel;
import io.nop.orm.support.DynamicOrmEntity;
import io.nop.orm.support.OrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code
import io.nop.orm.IOrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code
import io.nop.api.core.convert.ConvertHelper;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;

import app.erp.prj.dao.entity.ErpPrjCostCollectionLine;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  项目成本归集行: erp_prj_cost_collection_line
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpPrjCostCollectionLine extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 归集单ID: COST_COLLECTION_ID BIGINT */
    public static final String PROP_NAME_costCollectionId = "costCollectionId";
    public static final int PROP_ID_costCollectionId = 2;
    
    /* 行号: LINE_NO INTEGER */
    public static final String PROP_NAME_lineNo = "lineNo";
    public static final int PROP_ID_lineNo = 3;
    
    /* 成本类别(人工/材料/费用/分包): COST_CATEGORY VARCHAR */
    public static final String PROP_NAME_costCategory = "costCategory";
    public static final int PROP_ID_costCategory = 4;
    
    /* 来源单据类型(TIMESHEET/PURCHASE/EXPENSE): SOURCE_BILL_TYPE VARCHAR */
    public static final String PROP_NAME_sourceBillType = "sourceBillType";
    public static final int PROP_ID_sourceBillType = 5;
    
    /* 来源单据号: SOURCE_BILL_CODE VARCHAR */
    public static final String PROP_NAME_sourceBillCode = "sourceBillCode";
    public static final int PROP_ID_sourceBillCode = 6;
    
    /* 科目: SUBJECT_ID BIGINT */
    public static final String PROP_NAME_subjectId = "subjectId";
    public static final int PROP_ID_subjectId = 7;
    
    /* 任务: TASK_ID BIGINT */
    public static final String PROP_NAME_taskId = "taskId";
    public static final int PROP_ID_taskId = 8;
    
    /* 金额: AMOUNT DECIMAL */
    public static final String PROP_NAME_amount = "amount";
    public static final int PROP_ID_amount = 9;
    
    /* 备注: REMARK VARCHAR */
    public static final String PROP_NAME_remark = "remark";
    public static final int PROP_ID_remark = 10;
    
    /* 逻辑删除版本: DEL_VERSION BIGINT */
    public static final String PROP_NAME_delVersion = "delVersion";
    public static final int PROP_ID_delVersion = 11;
    
    /* 数据版本: VERSION INTEGER */
    public static final String PROP_NAME_version = "version";
    public static final int PROP_ID_version = 12;
    
    /* 创建人: CREATED_BY VARCHAR */
    public static final String PROP_NAME_createdBy = "createdBy";
    public static final int PROP_ID_createdBy = 13;
    
    /* 创建时间: CREATE_TIME TIMESTAMP */
    public static final String PROP_NAME_createTime = "createTime";
    public static final int PROP_ID_createTime = 14;
    
    /* 修改人: UPDATED_BY VARCHAR */
    public static final String PROP_NAME_updatedBy = "updatedBy";
    public static final int PROP_ID_updatedBy = 15;
    
    /* 修改时间: UPDATE_TIME TIMESTAMP */
    public static final String PROP_NAME_updateTime = "updateTime";
    public static final int PROP_ID_updateTime = 16;
    

    private static int _PROP_ID_BOUND = 17;

    
    /* relation:  */
    public static final String PROP_NAME_costCollection = "costCollection";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[17];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_costCollectionId] = PROP_NAME_costCollectionId;
          PROP_NAME_TO_ID.put(PROP_NAME_costCollectionId, PROP_ID_costCollectionId);
      
          PROP_ID_TO_NAME[PROP_ID_lineNo] = PROP_NAME_lineNo;
          PROP_NAME_TO_ID.put(PROP_NAME_lineNo, PROP_ID_lineNo);
      
          PROP_ID_TO_NAME[PROP_ID_costCategory] = PROP_NAME_costCategory;
          PROP_NAME_TO_ID.put(PROP_NAME_costCategory, PROP_ID_costCategory);
      
          PROP_ID_TO_NAME[PROP_ID_sourceBillType] = PROP_NAME_sourceBillType;
          PROP_NAME_TO_ID.put(PROP_NAME_sourceBillType, PROP_ID_sourceBillType);
      
          PROP_ID_TO_NAME[PROP_ID_sourceBillCode] = PROP_NAME_sourceBillCode;
          PROP_NAME_TO_ID.put(PROP_NAME_sourceBillCode, PROP_ID_sourceBillCode);
      
          PROP_ID_TO_NAME[PROP_ID_subjectId] = PROP_NAME_subjectId;
          PROP_NAME_TO_ID.put(PROP_NAME_subjectId, PROP_ID_subjectId);
      
          PROP_ID_TO_NAME[PROP_ID_taskId] = PROP_NAME_taskId;
          PROP_NAME_TO_ID.put(PROP_NAME_taskId, PROP_ID_taskId);
      
          PROP_ID_TO_NAME[PROP_ID_amount] = PROP_NAME_amount;
          PROP_NAME_TO_ID.put(PROP_NAME_amount, PROP_ID_amount);
      
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
    
    /* 归集单ID: COST_COLLECTION_ID */
    private java.lang.Long _costCollectionId;
    
    /* 行号: LINE_NO */
    private java.lang.Integer _lineNo;
    
    /* 成本类别(人工/材料/费用/分包): COST_CATEGORY */
    private java.lang.String _costCategory;
    
    /* 来源单据类型(TIMESHEET/PURCHASE/EXPENSE): SOURCE_BILL_TYPE */
    private java.lang.String _sourceBillType;
    
    /* 来源单据号: SOURCE_BILL_CODE */
    private java.lang.String _sourceBillCode;
    
    /* 科目: SUBJECT_ID */
    private java.lang.Long _subjectId;
    
    /* 任务: TASK_ID */
    private java.lang.Long _taskId;
    
    /* 金额: AMOUNT */
    private java.lang.String _amount;
    
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
    

    public _ErpPrjCostCollectionLine(){
        // for debug
    }

    protected ErpPrjCostCollectionLine newInstance(){
        ErpPrjCostCollectionLine entity = new ErpPrjCostCollectionLine();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpPrjCostCollectionLine cloneInstance() {
        ErpPrjCostCollectionLine entity = newInstance();
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
      return "app.erp.prj.dao.entity.ErpPrjCostCollectionLine";
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
        
            case PROP_ID_costCollectionId:
               return getCostCollectionId();
        
            case PROP_ID_lineNo:
               return getLineNo();
        
            case PROP_ID_costCategory:
               return getCostCategory();
        
            case PROP_ID_sourceBillType:
               return getSourceBillType();
        
            case PROP_ID_sourceBillCode:
               return getSourceBillCode();
        
            case PROP_ID_subjectId:
               return getSubjectId();
        
            case PROP_ID_taskId:
               return getTaskId();
        
            case PROP_ID_amount:
               return getAmount();
        
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
        
            case PROP_ID_costCollectionId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_costCollectionId));
               }
               setCostCollectionId(typedValue);
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
        
            case PROP_ID_costCategory:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_costCategory));
               }
               setCostCategory(typedValue);
               break;
            }
        
            case PROP_ID_sourceBillType:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_sourceBillType));
               }
               setSourceBillType(typedValue);
               break;
            }
        
            case PROP_ID_sourceBillCode:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_sourceBillCode));
               }
               setSourceBillCode(typedValue);
               break;
            }
        
            case PROP_ID_subjectId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_subjectId));
               }
               setSubjectId(typedValue);
               break;
            }
        
            case PROP_ID_taskId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_taskId));
               }
               setTaskId(typedValue);
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
        
            case PROP_ID_costCollectionId:{
               onInitProp(propId);
               this._costCollectionId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_lineNo:{
               onInitProp(propId);
               this._lineNo = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_costCategory:{
               onInitProp(propId);
               this._costCategory = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_sourceBillType:{
               onInitProp(propId);
               this._sourceBillType = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_sourceBillCode:{
               onInitProp(propId);
               this._sourceBillCode = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_subjectId:{
               onInitProp(propId);
               this._subjectId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_taskId:{
               onInitProp(propId);
               this._taskId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_amount:{
               onInitProp(propId);
               this._amount = (java.lang.String)value;
               
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
     * 归集单ID: COST_COLLECTION_ID
     */
    public final java.lang.Long getCostCollectionId(){
         onPropGet(PROP_ID_costCollectionId);
         return _costCollectionId;
    }

    /**
     * 归集单ID: COST_COLLECTION_ID
     */
    public final void setCostCollectionId(java.lang.Long value){
        if(onPropSet(PROP_ID_costCollectionId,value)){
            this._costCollectionId = value;
            internalClearRefs(PROP_ID_costCollectionId);
            
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
     * 成本类别(人工/材料/费用/分包): COST_CATEGORY
     */
    public final java.lang.String getCostCategory(){
         onPropGet(PROP_ID_costCategory);
         return _costCategory;
    }

    /**
     * 成本类别(人工/材料/费用/分包): COST_CATEGORY
     */
    public final void setCostCategory(java.lang.String value){
        if(onPropSet(PROP_ID_costCategory,value)){
            this._costCategory = value;
            internalClearRefs(PROP_ID_costCategory);
            
        }
    }
    
    /**
     * 来源单据类型(TIMESHEET/PURCHASE/EXPENSE): SOURCE_BILL_TYPE
     */
    public final java.lang.String getSourceBillType(){
         onPropGet(PROP_ID_sourceBillType);
         return _sourceBillType;
    }

    /**
     * 来源单据类型(TIMESHEET/PURCHASE/EXPENSE): SOURCE_BILL_TYPE
     */
    public final void setSourceBillType(java.lang.String value){
        if(onPropSet(PROP_ID_sourceBillType,value)){
            this._sourceBillType = value;
            internalClearRefs(PROP_ID_sourceBillType);
            
        }
    }
    
    /**
     * 来源单据号: SOURCE_BILL_CODE
     */
    public final java.lang.String getSourceBillCode(){
         onPropGet(PROP_ID_sourceBillCode);
         return _sourceBillCode;
    }

    /**
     * 来源单据号: SOURCE_BILL_CODE
     */
    public final void setSourceBillCode(java.lang.String value){
        if(onPropSet(PROP_ID_sourceBillCode,value)){
            this._sourceBillCode = value;
            internalClearRefs(PROP_ID_sourceBillCode);
            
        }
    }
    
    /**
     * 科目: SUBJECT_ID
     */
    public final java.lang.Long getSubjectId(){
         onPropGet(PROP_ID_subjectId);
         return _subjectId;
    }

    /**
     * 科目: SUBJECT_ID
     */
    public final void setSubjectId(java.lang.Long value){
        if(onPropSet(PROP_ID_subjectId,value)){
            this._subjectId = value;
            internalClearRefs(PROP_ID_subjectId);
            
        }
    }
    
    /**
     * 任务: TASK_ID
     */
    public final java.lang.Long getTaskId(){
         onPropGet(PROP_ID_taskId);
         return _taskId;
    }

    /**
     * 任务: TASK_ID
     */
    public final void setTaskId(java.lang.Long value){
        if(onPropSet(PROP_ID_taskId,value)){
            this._taskId = value;
            internalClearRefs(PROP_ID_taskId);
            
        }
    }
    
    /**
     * 金额: AMOUNT
     */
    public final java.lang.String getAmount(){
         onPropGet(PROP_ID_amount);
         return _amount;
    }

    /**
     * 金额: AMOUNT
     */
    public final void setAmount(java.lang.String value){
        if(onPropSet(PROP_ID_amount,value)){
            this._amount = value;
            internalClearRefs(PROP_ID_amount);
            
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
    public final app.erp.prj.dao.entity.ErpPrjCostCollection getCostCollection(){
       return (app.erp.prj.dao.entity.ErpPrjCostCollection)internalGetRefEntity(PROP_NAME_costCollection);
    }

    public final void setCostCollection(app.erp.prj.dao.entity.ErpPrjCostCollection refEntity){
   
           if(refEntity == null){
           
                   this.setCostCollectionId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_costCollection, refEntity,()->{
           
                           this.setCostCollectionId(refEntity.getId());
                       
           });
           }
       
    }
       
}
// resume CPD analysis - CPD-ON

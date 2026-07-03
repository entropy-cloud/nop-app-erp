package app.erp.qa.dao.entity._gen;

import io.nop.orm.model.IEntityModel;
import io.nop.orm.support.DynamicOrmEntity;
import io.nop.orm.support.OrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code
import io.nop.orm.IOrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code

import io.nop.api.core.convert.ConvertHelper;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;

import app.erp.qa.dao.entity.ErpQaInspectionTemplateLine;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  质检模板行: erp_qa_inspection_template_line
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpQaInspectionTemplateLine extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 模板ID: TEMPLATE_ID BIGINT */
    public static final String PROP_NAME_templateId = "templateId";
    public static final int PROP_ID_templateId = 2;
    
    /* 行号: LINE_NO INTEGER */
    public static final String PROP_NAME_lineNo = "lineNo";
    public static final int PROP_ID_lineNo = 3;
    
    /* 检验参数名称: PARAMETER_NAME VARCHAR */
    public static final String PROP_NAME_parameterName = "parameterName";
    public static final int PROP_ID_parameterName = 4;
    
    /* 规格下限: SPEC_MIN DECIMAL */
    public static final String PROP_NAME_specMin = "specMin";
    public static final int PROP_ID_specMin = 5;
    
    /* 规格上限: SPEC_MAX DECIMAL */
    public static final String PROP_NAME_specMax = "specMax";
    public static final int PROP_ID_specMax = 6;
    
    /* 计量单位: UNIT VARCHAR */
    public static final String PROP_NAME_unit = "unit";
    public static final int PROP_ID_unit = 7;
    
    /* 是否必检: IS_REQUIRED INTEGER */
    public static final String PROP_NAME_isRequired = "isRequired";
    public static final int PROP_ID_isRequired = 8;
    
    /* 检验方法: INSPECTION_METHOD VARCHAR */
    public static final String PROP_NAME_inspectionMethod = "inspectionMethod";
    public static final int PROP_ID_inspectionMethod = 10;
    
    /* 排序: SORT_NUM INTEGER */
    public static final String PROP_NAME_sortNum = "sortNum";
    public static final int PROP_ID_sortNum = 11;
    
    /* 逻辑删除版本: DEL_VERSION BIGINT */
    public static final String PROP_NAME_delVersion = "delVersion";
    public static final int PROP_ID_delVersion = 12;
    
    /* 数据版本: VERSION INTEGER */
    public static final String PROP_NAME_version = "version";
    public static final int PROP_ID_version = 13;
    
    /* 创建人: CREATED_BY VARCHAR */
    public static final String PROP_NAME_createdBy = "createdBy";
    public static final int PROP_ID_createdBy = 14;
    
    /* 创建时间: CREATE_TIME TIMESTAMP */
    public static final String PROP_NAME_createTime = "createTime";
    public static final int PROP_ID_createTime = 15;
    
    /* 修改人: UPDATED_BY VARCHAR */
    public static final String PROP_NAME_updatedBy = "updatedBy";
    public static final int PROP_ID_updatedBy = 16;
    
    /* 修改时间: UPDATE_TIME TIMESTAMP */
    public static final String PROP_NAME_updateTime = "updateTime";
    public static final int PROP_ID_updateTime = 17;
    

    private static int _PROP_ID_BOUND = 18;

    
    /* relation:  */
    public static final String PROP_NAME_template = "template";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[18];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_templateId] = PROP_NAME_templateId;
          PROP_NAME_TO_ID.put(PROP_NAME_templateId, PROP_ID_templateId);
      
          PROP_ID_TO_NAME[PROP_ID_lineNo] = PROP_NAME_lineNo;
          PROP_NAME_TO_ID.put(PROP_NAME_lineNo, PROP_ID_lineNo);
      
          PROP_ID_TO_NAME[PROP_ID_parameterName] = PROP_NAME_parameterName;
          PROP_NAME_TO_ID.put(PROP_NAME_parameterName, PROP_ID_parameterName);
      
          PROP_ID_TO_NAME[PROP_ID_specMin] = PROP_NAME_specMin;
          PROP_NAME_TO_ID.put(PROP_NAME_specMin, PROP_ID_specMin);
      
          PROP_ID_TO_NAME[PROP_ID_specMax] = PROP_NAME_specMax;
          PROP_NAME_TO_ID.put(PROP_NAME_specMax, PROP_ID_specMax);
      
          PROP_ID_TO_NAME[PROP_ID_unit] = PROP_NAME_unit;
          PROP_NAME_TO_ID.put(PROP_NAME_unit, PROP_ID_unit);
      
          PROP_ID_TO_NAME[PROP_ID_isRequired] = PROP_NAME_isRequired;
          PROP_NAME_TO_ID.put(PROP_NAME_isRequired, PROP_ID_isRequired);
      
          PROP_ID_TO_NAME[PROP_ID_inspectionMethod] = PROP_NAME_inspectionMethod;
          PROP_NAME_TO_ID.put(PROP_NAME_inspectionMethod, PROP_ID_inspectionMethod);
      
          PROP_ID_TO_NAME[PROP_ID_sortNum] = PROP_NAME_sortNum;
          PROP_NAME_TO_ID.put(PROP_NAME_sortNum, PROP_ID_sortNum);
      
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
    
    /* 模板ID: TEMPLATE_ID */
    private java.lang.Long _templateId;
    
    /* 行号: LINE_NO */
    private java.lang.Integer _lineNo;
    
    /* 检验参数名称: PARAMETER_NAME */
    private java.lang.String _parameterName;
    
    /* 规格下限: SPEC_MIN */
    private java.math.BigDecimal _specMin;
    
    /* 规格上限: SPEC_MAX */
    private java.math.BigDecimal _specMax;
    
    /* 计量单位: UNIT */
    private java.lang.String _unit;
    
    /* 是否必检: IS_REQUIRED */
    private java.lang.Integer _isRequired;
    
    /* 检验方法: INSPECTION_METHOD */
    private java.lang.String _inspectionMethod;
    
    /* 排序: SORT_NUM */
    private java.lang.Integer _sortNum;
    
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
    

    public _ErpQaInspectionTemplateLine(){
        // for debug
    }

    protected ErpQaInspectionTemplateLine newInstance(){
        ErpQaInspectionTemplateLine entity = new ErpQaInspectionTemplateLine();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpQaInspectionTemplateLine cloneInstance() {
        ErpQaInspectionTemplateLine entity = newInstance();
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
      return "app.erp.qa.dao.entity.ErpQaInspectionTemplateLine";
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
        
            case PROP_ID_templateId:
               return getTemplateId();
        
            case PROP_ID_lineNo:
               return getLineNo();
        
            case PROP_ID_parameterName:
               return getParameterName();
        
            case PROP_ID_specMin:
               return getSpecMin();
        
            case PROP_ID_specMax:
               return getSpecMax();
        
            case PROP_ID_unit:
               return getUnit();
        
            case PROP_ID_isRequired:
               return getIsRequired();
        
            case PROP_ID_inspectionMethod:
               return getInspectionMethod();
        
            case PROP_ID_sortNum:
               return getSortNum();
        
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
        
            case PROP_ID_templateId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_templateId));
               }
               setTemplateId(typedValue);
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
        
            case PROP_ID_parameterName:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_parameterName));
               }
               setParameterName(typedValue);
               break;
            }
        
            case PROP_ID_specMin:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_specMin));
               }
               setSpecMin(typedValue);
               break;
            }
        
            case PROP_ID_specMax:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_specMax));
               }
               setSpecMax(typedValue);
               break;
            }
        
            case PROP_ID_unit:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_unit));
               }
               setUnit(typedValue);
               break;
            }
        
            case PROP_ID_isRequired:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_isRequired));
               }
               setIsRequired(typedValue);
               break;
            }
        
            case PROP_ID_inspectionMethod:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_inspectionMethod));
               }
               setInspectionMethod(typedValue);
               break;
            }
        
            case PROP_ID_sortNum:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_sortNum));
               }
               setSortNum(typedValue);
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
        
            case PROP_ID_templateId:{
               onInitProp(propId);
               this._templateId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_lineNo:{
               onInitProp(propId);
               this._lineNo = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_parameterName:{
               onInitProp(propId);
               this._parameterName = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_specMin:{
               onInitProp(propId);
               this._specMin = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_specMax:{
               onInitProp(propId);
               this._specMax = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_unit:{
               onInitProp(propId);
               this._unit = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_isRequired:{
               onInitProp(propId);
               this._isRequired = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_inspectionMethod:{
               onInitProp(propId);
               this._inspectionMethod = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_sortNum:{
               onInitProp(propId);
               this._sortNum = (java.lang.Integer)value;
               
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
     * 模板ID: TEMPLATE_ID
     */
    public final java.lang.Long getTemplateId(){
         onPropGet(PROP_ID_templateId);
         return _templateId;
    }

    /**
     * 模板ID: TEMPLATE_ID
     */
    public final void setTemplateId(java.lang.Long value){
        if(onPropSet(PROP_ID_templateId,value)){
            this._templateId = value;
            internalClearRefs(PROP_ID_templateId);
            
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
     * 检验参数名称: PARAMETER_NAME
     */
    public final java.lang.String getParameterName(){
         onPropGet(PROP_ID_parameterName);
         return _parameterName;
    }

    /**
     * 检验参数名称: PARAMETER_NAME
     */
    public final void setParameterName(java.lang.String value){
        if(onPropSet(PROP_ID_parameterName,value)){
            this._parameterName = value;
            internalClearRefs(PROP_ID_parameterName);
            
        }
    }
    
    /**
     * 规格下限: SPEC_MIN
     */
    public final java.math.BigDecimal getSpecMin(){
         onPropGet(PROP_ID_specMin);
         return _specMin;
    }

    /**
     * 规格下限: SPEC_MIN
     */
    public final void setSpecMin(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_specMin,value)){
            this._specMin = value;
            internalClearRefs(PROP_ID_specMin);
            
        }
    }
    
    /**
     * 规格上限: SPEC_MAX
     */
    public final java.math.BigDecimal getSpecMax(){
         onPropGet(PROP_ID_specMax);
         return _specMax;
    }

    /**
     * 规格上限: SPEC_MAX
     */
    public final void setSpecMax(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_specMax,value)){
            this._specMax = value;
            internalClearRefs(PROP_ID_specMax);
            
        }
    }
    
    /**
     * 计量单位: UNIT
     */
    public final java.lang.String getUnit(){
         onPropGet(PROP_ID_unit);
         return _unit;
    }

    /**
     * 计量单位: UNIT
     */
    public final void setUnit(java.lang.String value){
        if(onPropSet(PROP_ID_unit,value)){
            this._unit = value;
            internalClearRefs(PROP_ID_unit);
            
        }
    }
    
    /**
     * 是否必检: IS_REQUIRED
     */
    public final java.lang.Integer getIsRequired(){
         onPropGet(PROP_ID_isRequired);
         return _isRequired;
    }

    /**
     * 是否必检: IS_REQUIRED
     */
    public final void setIsRequired(java.lang.Integer value){
        if(onPropSet(PROP_ID_isRequired,value)){
            this._isRequired = value;
            internalClearRefs(PROP_ID_isRequired);
            
        }
    }
    
    /**
     * 检验方法: INSPECTION_METHOD
     */
    public final java.lang.String getInspectionMethod(){
         onPropGet(PROP_ID_inspectionMethod);
         return _inspectionMethod;
    }

    /**
     * 检验方法: INSPECTION_METHOD
     */
    public final void setInspectionMethod(java.lang.String value){
        if(onPropSet(PROP_ID_inspectionMethod,value)){
            this._inspectionMethod = value;
            internalClearRefs(PROP_ID_inspectionMethod);
            
        }
    }
    
    /**
     * 排序: SORT_NUM
     */
    public final java.lang.Integer getSortNum(){
         onPropGet(PROP_ID_sortNum);
         return _sortNum;
    }

    /**
     * 排序: SORT_NUM
     */
    public final void setSortNum(java.lang.Integer value){
        if(onPropSet(PROP_ID_sortNum,value)){
            this._sortNum = value;
            internalClearRefs(PROP_ID_sortNum);
            
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
    public final app.erp.qa.dao.entity.ErpQaInspectionTemplate getTemplate(){
       return (app.erp.qa.dao.entity.ErpQaInspectionTemplate)internalGetRefEntity(PROP_NAME_template);
    }

    public final void setTemplate(app.erp.qa.dao.entity.ErpQaInspectionTemplate refEntity){
   
           if(refEntity == null){
           
                   this.setTemplateId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_template, refEntity,()->{
           
                           this.setTemplateId(refEntity.getId());
                       
           });
           }
       
    }
       
}
// resume CPD analysis - CPD-ON

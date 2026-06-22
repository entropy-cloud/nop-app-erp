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

import app.erp.qa.dao.entity.ErpQaNonConformance;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  不合格品报告: erp_qa_non_conformance
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpQaNonConformance extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 单号: CODE VARCHAR */
    public static final String PROP_NAME_code = "code";
    public static final int PROP_ID_code = 2;
    
    /* 报告日期: NCR_DATE DATE */
    public static final String PROP_NAME_ncrDate = "ncrDate";
    public static final int PROP_ID_ncrDate = 3;
    
    /* 来源类型: SOURCE_TYPE VARCHAR */
    public static final String PROP_NAME_sourceType = "sourceType";
    public static final int PROP_ID_sourceType = 4;
    
    /* 来源单号: SOURCE_CODE VARCHAR */
    public static final String PROP_NAME_sourceCode = "sourceCode";
    public static final int PROP_ID_sourceCode = 5;
    
    /* 物料: MATERIAL_ID BIGINT */
    public static final String PROP_NAME_materialId = "materialId";
    public static final int PROP_ID_materialId = 6;
    
    /* 不合格数量: QUANTITY DECIMAL */
    public static final String PROP_NAME_quantity = "quantity";
    public static final int PROP_ID_quantity = 7;
    
    /* 问题描述: DESCRIPTION VARCHAR */
    public static final String PROP_NAME_description = "description";
    public static final int PROP_ID_description = 8;
    
    /* 严重程度: SEVERITY INTEGER */
    public static final String PROP_NAME_severity = "severity";
    public static final int PROP_ID_severity = 9;
    
    /* 状态: STATUS INTEGER */
    public static final String PROP_NAME_status = "status";
    public static final int PROP_ID_status = 10;
    
    /* 责任人: ASSIGNED_TO BIGINT */
    public static final String PROP_NAME_assignedTo = "assignedTo";
    public static final int PROP_ID_assignedTo = 11;
    
    /* 解决人: RESOLVED_BY BIGINT */
    public static final String PROP_NAME_resolvedBy = "resolvedBy";
    public static final int PROP_ID_resolvedBy = 12;
    
    /* 解决时间: RESOLVED_AT DATETIME */
    public static final String PROP_NAME_resolvedAt = "resolvedAt";
    public static final int PROP_ID_resolvedAt = 13;
    
    /* 解决措施: RESOLUTION VARCHAR */
    public static final String PROP_NAME_resolution = "resolution";
    public static final int PROP_ID_resolution = 14;
    
    /* 备注: REMARK VARCHAR */
    public static final String PROP_NAME_remark = "remark";
    public static final int PROP_ID_remark = 15;
    
    /* 逻辑删除版本: DEL_VERSION BIGINT */
    public static final String PROP_NAME_delVersion = "delVersion";
    public static final int PROP_ID_delVersion = 16;
    
    /* 数据版本: VERSION INTEGER */
    public static final String PROP_NAME_version = "version";
    public static final int PROP_ID_version = 17;
    
    /* 创建人: CREATED_BY VARCHAR */
    public static final String PROP_NAME_createdBy = "createdBy";
    public static final int PROP_ID_createdBy = 18;
    
    /* 创建时间: CREATE_TIME TIMESTAMP */
    public static final String PROP_NAME_createTime = "createTime";
    public static final int PROP_ID_createTime = 19;
    
    /* 修改人: UPDATED_BY VARCHAR */
    public static final String PROP_NAME_updatedBy = "updatedBy";
    public static final int PROP_ID_updatedBy = 20;
    
    /* 修改时间: UPDATE_TIME TIMESTAMP */
    public static final String PROP_NAME_updateTime = "updateTime";
    public static final int PROP_ID_updateTime = 21;
    

    private static int _PROP_ID_BOUND = 22;

    
    /* relation:  */
    public static final String PROP_NAME_actions = "actions";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[22];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_code] = PROP_NAME_code;
          PROP_NAME_TO_ID.put(PROP_NAME_code, PROP_ID_code);
      
          PROP_ID_TO_NAME[PROP_ID_ncrDate] = PROP_NAME_ncrDate;
          PROP_NAME_TO_ID.put(PROP_NAME_ncrDate, PROP_ID_ncrDate);
      
          PROP_ID_TO_NAME[PROP_ID_sourceType] = PROP_NAME_sourceType;
          PROP_NAME_TO_ID.put(PROP_NAME_sourceType, PROP_ID_sourceType);
      
          PROP_ID_TO_NAME[PROP_ID_sourceCode] = PROP_NAME_sourceCode;
          PROP_NAME_TO_ID.put(PROP_NAME_sourceCode, PROP_ID_sourceCode);
      
          PROP_ID_TO_NAME[PROP_ID_materialId] = PROP_NAME_materialId;
          PROP_NAME_TO_ID.put(PROP_NAME_materialId, PROP_ID_materialId);
      
          PROP_ID_TO_NAME[PROP_ID_quantity] = PROP_NAME_quantity;
          PROP_NAME_TO_ID.put(PROP_NAME_quantity, PROP_ID_quantity);
      
          PROP_ID_TO_NAME[PROP_ID_description] = PROP_NAME_description;
          PROP_NAME_TO_ID.put(PROP_NAME_description, PROP_ID_description);
      
          PROP_ID_TO_NAME[PROP_ID_severity] = PROP_NAME_severity;
          PROP_NAME_TO_ID.put(PROP_NAME_severity, PROP_ID_severity);
      
          PROP_ID_TO_NAME[PROP_ID_status] = PROP_NAME_status;
          PROP_NAME_TO_ID.put(PROP_NAME_status, PROP_ID_status);
      
          PROP_ID_TO_NAME[PROP_ID_assignedTo] = PROP_NAME_assignedTo;
          PROP_NAME_TO_ID.put(PROP_NAME_assignedTo, PROP_ID_assignedTo);
      
          PROP_ID_TO_NAME[PROP_ID_resolvedBy] = PROP_NAME_resolvedBy;
          PROP_NAME_TO_ID.put(PROP_NAME_resolvedBy, PROP_ID_resolvedBy);
      
          PROP_ID_TO_NAME[PROP_ID_resolvedAt] = PROP_NAME_resolvedAt;
          PROP_NAME_TO_ID.put(PROP_NAME_resolvedAt, PROP_ID_resolvedAt);
      
          PROP_ID_TO_NAME[PROP_ID_resolution] = PROP_NAME_resolution;
          PROP_NAME_TO_ID.put(PROP_NAME_resolution, PROP_ID_resolution);
      
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
    
    /* 单号: CODE */
    private java.lang.String _code;
    
    /* 报告日期: NCR_DATE */
    private java.time.LocalDate _ncrDate;
    
    /* 来源类型: SOURCE_TYPE */
    private java.lang.String _sourceType;
    
    /* 来源单号: SOURCE_CODE */
    private java.lang.String _sourceCode;
    
    /* 物料: MATERIAL_ID */
    private java.lang.Long _materialId;
    
    /* 不合格数量: QUANTITY */
    private java.lang.String _quantity;
    
    /* 问题描述: DESCRIPTION */
    private java.lang.String _description;
    
    /* 严重程度: SEVERITY */
    private java.lang.Integer _severity;
    
    /* 状态: STATUS */
    private java.lang.Integer _status;
    
    /* 责任人: ASSIGNED_TO */
    private java.lang.Long _assignedTo;
    
    /* 解决人: RESOLVED_BY */
    private java.lang.Long _resolvedBy;
    
    /* 解决时间: RESOLVED_AT */
    private java.time.LocalDateTime _resolvedAt;
    
    /* 解决措施: RESOLUTION */
    private java.lang.String _resolution;
    
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
    

    public _ErpQaNonConformance(){
        // for debug
    }

    protected ErpQaNonConformance newInstance(){
        ErpQaNonConformance entity = new ErpQaNonConformance();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpQaNonConformance cloneInstance() {
        ErpQaNonConformance entity = newInstance();
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
      return "app.erp.qa.dao.entity.ErpQaNonConformance";
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
        
            case PROP_ID_ncrDate:
               return getNcrDate();
        
            case PROP_ID_sourceType:
               return getSourceType();
        
            case PROP_ID_sourceCode:
               return getSourceCode();
        
            case PROP_ID_materialId:
               return getMaterialId();
        
            case PROP_ID_quantity:
               return getQuantity();
        
            case PROP_ID_description:
               return getDescription();
        
            case PROP_ID_severity:
               return getSeverity();
        
            case PROP_ID_status:
               return getStatus();
        
            case PROP_ID_assignedTo:
               return getAssignedTo();
        
            case PROP_ID_resolvedBy:
               return getResolvedBy();
        
            case PROP_ID_resolvedAt:
               return getResolvedAt();
        
            case PROP_ID_resolution:
               return getResolution();
        
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
        
            case PROP_ID_code:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_code));
               }
               setCode(typedValue);
               break;
            }
        
            case PROP_ID_ncrDate:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_ncrDate));
               }
               setNcrDate(typedValue);
               break;
            }
        
            case PROP_ID_sourceType:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_sourceType));
               }
               setSourceType(typedValue);
               break;
            }
        
            case PROP_ID_sourceCode:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_sourceCode));
               }
               setSourceCode(typedValue);
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
        
            case PROP_ID_quantity:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_quantity));
               }
               setQuantity(typedValue);
               break;
            }
        
            case PROP_ID_description:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_description));
               }
               setDescription(typedValue);
               break;
            }
        
            case PROP_ID_severity:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_severity));
               }
               setSeverity(typedValue);
               break;
            }
        
            case PROP_ID_status:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_status));
               }
               setStatus(typedValue);
               break;
            }
        
            case PROP_ID_assignedTo:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_assignedTo));
               }
               setAssignedTo(typedValue);
               break;
            }
        
            case PROP_ID_resolvedBy:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_resolvedBy));
               }
               setResolvedBy(typedValue);
               break;
            }
        
            case PROP_ID_resolvedAt:{
               java.time.LocalDateTime typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDateTime(value,
                       err-> newTypeConversionError(PROP_NAME_resolvedAt));
               }
               setResolvedAt(typedValue);
               break;
            }
        
            case PROP_ID_resolution:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_resolution));
               }
               setResolution(typedValue);
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
        
            case PROP_ID_code:{
               onInitProp(propId);
               this._code = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_ncrDate:{
               onInitProp(propId);
               this._ncrDate = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_sourceType:{
               onInitProp(propId);
               this._sourceType = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_sourceCode:{
               onInitProp(propId);
               this._sourceCode = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_materialId:{
               onInitProp(propId);
               this._materialId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_quantity:{
               onInitProp(propId);
               this._quantity = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_description:{
               onInitProp(propId);
               this._description = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_severity:{
               onInitProp(propId);
               this._severity = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_status:{
               onInitProp(propId);
               this._status = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_assignedTo:{
               onInitProp(propId);
               this._assignedTo = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_resolvedBy:{
               onInitProp(propId);
               this._resolvedBy = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_resolvedAt:{
               onInitProp(propId);
               this._resolvedAt = (java.time.LocalDateTime)value;
               
               break;
            }
        
            case PROP_ID_resolution:{
               onInitProp(propId);
               this._resolution = (java.lang.String)value;
               
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
     * 单号: CODE
     */
    public final java.lang.String getCode(){
         onPropGet(PROP_ID_code);
         return _code;
    }

    /**
     * 单号: CODE
     */
    public final void setCode(java.lang.String value){
        if(onPropSet(PROP_ID_code,value)){
            this._code = value;
            internalClearRefs(PROP_ID_code);
            
        }
    }
    
    /**
     * 报告日期: NCR_DATE
     */
    public final java.time.LocalDate getNcrDate(){
         onPropGet(PROP_ID_ncrDate);
         return _ncrDate;
    }

    /**
     * 报告日期: NCR_DATE
     */
    public final void setNcrDate(java.time.LocalDate value){
        if(onPropSet(PROP_ID_ncrDate,value)){
            this._ncrDate = value;
            internalClearRefs(PROP_ID_ncrDate);
            
        }
    }
    
    /**
     * 来源类型: SOURCE_TYPE
     */
    public final java.lang.String getSourceType(){
         onPropGet(PROP_ID_sourceType);
         return _sourceType;
    }

    /**
     * 来源类型: SOURCE_TYPE
     */
    public final void setSourceType(java.lang.String value){
        if(onPropSet(PROP_ID_sourceType,value)){
            this._sourceType = value;
            internalClearRefs(PROP_ID_sourceType);
            
        }
    }
    
    /**
     * 来源单号: SOURCE_CODE
     */
    public final java.lang.String getSourceCode(){
         onPropGet(PROP_ID_sourceCode);
         return _sourceCode;
    }

    /**
     * 来源单号: SOURCE_CODE
     */
    public final void setSourceCode(java.lang.String value){
        if(onPropSet(PROP_ID_sourceCode,value)){
            this._sourceCode = value;
            internalClearRefs(PROP_ID_sourceCode);
            
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
     * 不合格数量: QUANTITY
     */
    public final java.lang.String getQuantity(){
         onPropGet(PROP_ID_quantity);
         return _quantity;
    }

    /**
     * 不合格数量: QUANTITY
     */
    public final void setQuantity(java.lang.String value){
        if(onPropSet(PROP_ID_quantity,value)){
            this._quantity = value;
            internalClearRefs(PROP_ID_quantity);
            
        }
    }
    
    /**
     * 问题描述: DESCRIPTION
     */
    public final java.lang.String getDescription(){
         onPropGet(PROP_ID_description);
         return _description;
    }

    /**
     * 问题描述: DESCRIPTION
     */
    public final void setDescription(java.lang.String value){
        if(onPropSet(PROP_ID_description,value)){
            this._description = value;
            internalClearRefs(PROP_ID_description);
            
        }
    }
    
    /**
     * 严重程度: SEVERITY
     */
    public final java.lang.Integer getSeverity(){
         onPropGet(PROP_ID_severity);
         return _severity;
    }

    /**
     * 严重程度: SEVERITY
     */
    public final void setSeverity(java.lang.Integer value){
        if(onPropSet(PROP_ID_severity,value)){
            this._severity = value;
            internalClearRefs(PROP_ID_severity);
            
        }
    }
    
    /**
     * 状态: STATUS
     */
    public final java.lang.Integer getStatus(){
         onPropGet(PROP_ID_status);
         return _status;
    }

    /**
     * 状态: STATUS
     */
    public final void setStatus(java.lang.Integer value){
        if(onPropSet(PROP_ID_status,value)){
            this._status = value;
            internalClearRefs(PROP_ID_status);
            
        }
    }
    
    /**
     * 责任人: ASSIGNED_TO
     */
    public final java.lang.Long getAssignedTo(){
         onPropGet(PROP_ID_assignedTo);
         return _assignedTo;
    }

    /**
     * 责任人: ASSIGNED_TO
     */
    public final void setAssignedTo(java.lang.Long value){
        if(onPropSet(PROP_ID_assignedTo,value)){
            this._assignedTo = value;
            internalClearRefs(PROP_ID_assignedTo);
            
        }
    }
    
    /**
     * 解决人: RESOLVED_BY
     */
    public final java.lang.Long getResolvedBy(){
         onPropGet(PROP_ID_resolvedBy);
         return _resolvedBy;
    }

    /**
     * 解决人: RESOLVED_BY
     */
    public final void setResolvedBy(java.lang.Long value){
        if(onPropSet(PROP_ID_resolvedBy,value)){
            this._resolvedBy = value;
            internalClearRefs(PROP_ID_resolvedBy);
            
        }
    }
    
    /**
     * 解决时间: RESOLVED_AT
     */
    public final java.time.LocalDateTime getResolvedAt(){
         onPropGet(PROP_ID_resolvedAt);
         return _resolvedAt;
    }

    /**
     * 解决时间: RESOLVED_AT
     */
    public final void setResolvedAt(java.time.LocalDateTime value){
        if(onPropSet(PROP_ID_resolvedAt,value)){
            this._resolvedAt = value;
            internalClearRefs(PROP_ID_resolvedAt);
            
        }
    }
    
    /**
     * 解决措施: RESOLUTION
     */
    public final java.lang.String getResolution(){
         onPropGet(PROP_ID_resolution);
         return _resolution;
    }

    /**
     * 解决措施: RESOLUTION
     */
    public final void setResolution(java.lang.String value){
        if(onPropSet(PROP_ID_resolution,value)){
            this._resolution = value;
            internalClearRefs(PROP_ID_resolution);
            
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
    
    private final OrmEntitySet<app.erp.qa.dao.entity.ErpQaAction> _actions = new OrmEntitySet<>(this, PROP_NAME_actions,
        null, null,app.erp.qa.dao.entity.ErpQaAction.class);

    /**
     * 。 refPropName: , keyProp: {rel.keyProp}
     */
    public final IOrmEntitySet<app.erp.qa.dao.entity.ErpQaAction> getActions(){
       return _actions;
    }
       
}
// resume CPD analysis - CPD-ON

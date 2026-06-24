package app.erp.fin.dao.entity._gen;

import io.nop.orm.model.IEntityModel;
import io.nop.orm.support.DynamicOrmEntity;
import io.nop.orm.support.OrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code
import io.nop.orm.IOrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code

import io.nop.api.core.convert.ConvertHelper;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;

import app.erp.fin.dao.entity.ErpFinVoucherTemplateLine;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  凭证模板行: erp_fin_voucher_template_line
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpFinVoucherTemplateLine extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 模板ID: TEMPLATE_ID BIGINT */
    public static final String PROP_NAME_templateId = "templateId";
    public static final int PROP_ID_templateId = 2;
    
    /* 行号: LINE_NO INTEGER */
    public static final String PROP_NAME_lineNo = "lineNo";
    public static final int PROP_ID_lineNo = 3;
    
    /* 科目编码(可含占位符): SUBJECT_CODE VARCHAR */
    public static final String PROP_NAME_subjectCode = "subjectCode";
    public static final int PROP_ID_subjectCode = 4;
    
    /* 借贷方向: DC_DIRECTION INTEGER */
    public static final String PROP_NAME_dcDirection = "dcDirection";
    public static final int PROP_ID_dcDirection = 5;
    
    /* 金额表达式: AMOUNT_EXPRESSION VARCHAR */
    public static final String PROP_NAME_amountExpression = "amountExpression";
    public static final int PROP_ID_amountExpression = 6;
    
    /* 科目映射键: ACCOUNT_KEY VARCHAR */
    public static final String PROP_NAME_accountKey = "accountKey";
    public static final int PROP_ID_accountKey = 7;
    
    /* 金额占位键: AMOUNT_KEY VARCHAR */
    public static final String PROP_NAME_amountKey = "amountKey";
    public static final int PROP_ID_amountKey = 8;
    
    /* 摘要模板: MEMO_TEMPLATE VARCHAR */
    public static final String PROP_NAME_memoTemplate = "memoTemplate";
    public static final int PROP_ID_memoTemplate = 9;
    
    /* 逻辑删除版本: DEL_VERSION BIGINT */
    public static final String PROP_NAME_delVersion = "delVersion";
    public static final int PROP_ID_delVersion = 10;
    
    /* 数据版本: VERSION INTEGER */
    public static final String PROP_NAME_version = "version";
    public static final int PROP_ID_version = 11;
    
    /* 创建人: CREATED_BY VARCHAR */
    public static final String PROP_NAME_createdBy = "createdBy";
    public static final int PROP_ID_createdBy = 12;
    
    /* 创建时间: CREATE_TIME TIMESTAMP */
    public static final String PROP_NAME_createTime = "createTime";
    public static final int PROP_ID_createTime = 13;
    
    /* 修改人: UPDATED_BY VARCHAR */
    public static final String PROP_NAME_updatedBy = "updatedBy";
    public static final int PROP_ID_updatedBy = 14;
    
    /* 修改时间: UPDATE_TIME TIMESTAMP */
    public static final String PROP_NAME_updateTime = "updateTime";
    public static final int PROP_ID_updateTime = 15;
    

    private static int _PROP_ID_BOUND = 16;

    
    /* relation:  */
    public static final String PROP_NAME_template = "template";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[16];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_templateId] = PROP_NAME_templateId;
          PROP_NAME_TO_ID.put(PROP_NAME_templateId, PROP_ID_templateId);
      
          PROP_ID_TO_NAME[PROP_ID_lineNo] = PROP_NAME_lineNo;
          PROP_NAME_TO_ID.put(PROP_NAME_lineNo, PROP_ID_lineNo);
      
          PROP_ID_TO_NAME[PROP_ID_subjectCode] = PROP_NAME_subjectCode;
          PROP_NAME_TO_ID.put(PROP_NAME_subjectCode, PROP_ID_subjectCode);
      
          PROP_ID_TO_NAME[PROP_ID_dcDirection] = PROP_NAME_dcDirection;
          PROP_NAME_TO_ID.put(PROP_NAME_dcDirection, PROP_ID_dcDirection);
      
          PROP_ID_TO_NAME[PROP_ID_amountExpression] = PROP_NAME_amountExpression;
          PROP_NAME_TO_ID.put(PROP_NAME_amountExpression, PROP_ID_amountExpression);
      
          PROP_ID_TO_NAME[PROP_ID_accountKey] = PROP_NAME_accountKey;
          PROP_NAME_TO_ID.put(PROP_NAME_accountKey, PROP_ID_accountKey);
      
          PROP_ID_TO_NAME[PROP_ID_amountKey] = PROP_NAME_amountKey;
          PROP_NAME_TO_ID.put(PROP_NAME_amountKey, PROP_ID_amountKey);
      
          PROP_ID_TO_NAME[PROP_ID_memoTemplate] = PROP_NAME_memoTemplate;
          PROP_NAME_TO_ID.put(PROP_NAME_memoTemplate, PROP_ID_memoTemplate);
      
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
    
    /* 科目编码(可含占位符): SUBJECT_CODE */
    private java.lang.String _subjectCode;
    
    /* 借贷方向: DC_DIRECTION */
    private java.lang.Integer _dcDirection;
    
    /* 金额表达式: AMOUNT_EXPRESSION */
    private java.lang.String _amountExpression;
    
    /* 科目映射键: ACCOUNT_KEY */
    private java.lang.String _accountKey;
    
    /* 金额占位键: AMOUNT_KEY */
    private java.lang.String _amountKey;
    
    /* 摘要模板: MEMO_TEMPLATE */
    private java.lang.String _memoTemplate;
    
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
    

    public _ErpFinVoucherTemplateLine(){
        // for debug
    }

    protected ErpFinVoucherTemplateLine newInstance(){
        ErpFinVoucherTemplateLine entity = new ErpFinVoucherTemplateLine();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpFinVoucherTemplateLine cloneInstance() {
        ErpFinVoucherTemplateLine entity = newInstance();
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
      return "app.erp.fin.dao.entity.ErpFinVoucherTemplateLine";
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
        
            case PROP_ID_subjectCode:
               return getSubjectCode();
        
            case PROP_ID_dcDirection:
               return getDcDirection();
        
            case PROP_ID_amountExpression:
               return getAmountExpression();
        
            case PROP_ID_accountKey:
               return getAccountKey();
        
            case PROP_ID_amountKey:
               return getAmountKey();
        
            case PROP_ID_memoTemplate:
               return getMemoTemplate();
        
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
        
            case PROP_ID_subjectCode:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_subjectCode));
               }
               setSubjectCode(typedValue);
               break;
            }
        
            case PROP_ID_dcDirection:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_dcDirection));
               }
               setDcDirection(typedValue);
               break;
            }
        
            case PROP_ID_amountExpression:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_amountExpression));
               }
               setAmountExpression(typedValue);
               break;
            }
        
            case PROP_ID_accountKey:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_accountKey));
               }
               setAccountKey(typedValue);
               break;
            }
        
            case PROP_ID_amountKey:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_amountKey));
               }
               setAmountKey(typedValue);
               break;
            }
        
            case PROP_ID_memoTemplate:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_memoTemplate));
               }
               setMemoTemplate(typedValue);
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
        
            case PROP_ID_subjectCode:{
               onInitProp(propId);
               this._subjectCode = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_dcDirection:{
               onInitProp(propId);
               this._dcDirection = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_amountExpression:{
               onInitProp(propId);
               this._amountExpression = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_accountKey:{
               onInitProp(propId);
               this._accountKey = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_amountKey:{
               onInitProp(propId);
               this._amountKey = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_memoTemplate:{
               onInitProp(propId);
               this._memoTemplate = (java.lang.String)value;
               
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
     * 科目编码(可含占位符): SUBJECT_CODE
     */
    public final java.lang.String getSubjectCode(){
         onPropGet(PROP_ID_subjectCode);
         return _subjectCode;
    }

    /**
     * 科目编码(可含占位符): SUBJECT_CODE
     */
    public final void setSubjectCode(java.lang.String value){
        if(onPropSet(PROP_ID_subjectCode,value)){
            this._subjectCode = value;
            internalClearRefs(PROP_ID_subjectCode);
            
        }
    }
    
    /**
     * 借贷方向: DC_DIRECTION
     */
    public final java.lang.Integer getDcDirection(){
         onPropGet(PROP_ID_dcDirection);
         return _dcDirection;
    }

    /**
     * 借贷方向: DC_DIRECTION
     */
    public final void setDcDirection(java.lang.Integer value){
        if(onPropSet(PROP_ID_dcDirection,value)){
            this._dcDirection = value;
            internalClearRefs(PROP_ID_dcDirection);
            
        }
    }
    
    /**
     * 金额表达式: AMOUNT_EXPRESSION
     */
    public final java.lang.String getAmountExpression(){
         onPropGet(PROP_ID_amountExpression);
         return _amountExpression;
    }

    /**
     * 金额表达式: AMOUNT_EXPRESSION
     */
    public final void setAmountExpression(java.lang.String value){
        if(onPropSet(PROP_ID_amountExpression,value)){
            this._amountExpression = value;
            internalClearRefs(PROP_ID_amountExpression);
            
        }
    }
    
    /**
     * 科目映射键: ACCOUNT_KEY
     */
    public final java.lang.String getAccountKey(){
         onPropGet(PROP_ID_accountKey);
         return _accountKey;
    }

    /**
     * 科目映射键: ACCOUNT_KEY
     */
    public final void setAccountKey(java.lang.String value){
        if(onPropSet(PROP_ID_accountKey,value)){
            this._accountKey = value;
            internalClearRefs(PROP_ID_accountKey);
            
        }
    }
    
    /**
     * 金额占位键: AMOUNT_KEY
     */
    public final java.lang.String getAmountKey(){
         onPropGet(PROP_ID_amountKey);
         return _amountKey;
    }

    /**
     * 金额占位键: AMOUNT_KEY
     */
    public final void setAmountKey(java.lang.String value){
        if(onPropSet(PROP_ID_amountKey,value)){
            this._amountKey = value;
            internalClearRefs(PROP_ID_amountKey);
            
        }
    }
    
    /**
     * 摘要模板: MEMO_TEMPLATE
     */
    public final java.lang.String getMemoTemplate(){
         onPropGet(PROP_ID_memoTemplate);
         return _memoTemplate;
    }

    /**
     * 摘要模板: MEMO_TEMPLATE
     */
    public final void setMemoTemplate(java.lang.String value){
        if(onPropSet(PROP_ID_memoTemplate,value)){
            this._memoTemplate = value;
            internalClearRefs(PROP_ID_memoTemplate);
            
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
    public final app.erp.fin.dao.entity.ErpFinVoucherTemplate getTemplate(){
       return (app.erp.fin.dao.entity.ErpFinVoucherTemplate)internalGetRefEntity(PROP_NAME_template);
    }

    public final void setTemplate(app.erp.fin.dao.entity.ErpFinVoucherTemplate refEntity){
   
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

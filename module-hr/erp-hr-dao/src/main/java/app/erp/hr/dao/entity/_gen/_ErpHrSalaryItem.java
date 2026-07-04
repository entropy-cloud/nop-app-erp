package app.erp.hr.dao.entity._gen;

import io.nop.orm.model.IEntityModel;
import io.nop.orm.support.DynamicOrmEntity;
import io.nop.orm.support.OrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code
import io.nop.orm.IOrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code

import io.nop.api.core.convert.ConvertHelper;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;

import app.erp.hr.dao.entity.ErpHrSalaryItem;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  薪酬项目: erp_hr_salary_item
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpHrSalaryItem extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 编码: CODE VARCHAR */
    public static final String PROP_NAME_code = "code";
    public static final int PROP_ID_code = 2;
    
    /* 名称: NAME VARCHAR */
    public static final String PROP_NAME_name = "name";
    public static final int PROP_ID_name = 3;
    
    /* 类别: ITEM_CATEGORY VARCHAR */
    public static final String PROP_NAME_itemCategory = "itemCategory";
    public static final int PROP_ID_itemCategory = 4;
    
    /* 分组: ITEM_GROUP VARCHAR */
    public static final String PROP_NAME_itemGroup = "itemGroup";
    public static final int PROP_ID_itemGroup = 5;
    
    /* 计算方式: CALC_METHOD VARCHAR */
    public static final String PROP_NAME_calcMethod = "calcMethod";
    public static final int PROP_ID_calcMethod = 6;
    
    /* 计算公式: FORMULA VARCHAR */
    public static final String PROP_NAME_formula = "formula";
    public static final int PROP_ID_formula = 7;
    
    /* 是否应税: IS_TAXABLE BOOLEAN */
    public static final String PROP_NAME_isTaxable = "isTaxable";
    public static final int PROP_ID_isTaxable = 8;
    
    /* 计入社保基数: IS_SOCIAL_INSURANCE_BASE BOOLEAN */
    public static final String PROP_NAME_isSocialInsuranceBase = "isSocialInsuranceBase";
    public static final int PROP_ID_isSocialInsuranceBase = 9;
    
    /* 是否必含: IS_MANDATORY BOOLEAN */
    public static final String PROP_NAME_isMandatory = "isMandatory";
    public static final int PROP_ID_isMandatory = 10;
    
    /* 排序号: SORT_ORDER INTEGER */
    public static final String PROP_NAME_sortOrder = "sortOrder";
    public static final int PROP_ID_sortOrder = 11;
    
    /* 业务组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 12;
    
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
    public static final String PROP_NAME_org = "org";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[20];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_code] = PROP_NAME_code;
          PROP_NAME_TO_ID.put(PROP_NAME_code, PROP_ID_code);
      
          PROP_ID_TO_NAME[PROP_ID_name] = PROP_NAME_name;
          PROP_NAME_TO_ID.put(PROP_NAME_name, PROP_ID_name);
      
          PROP_ID_TO_NAME[PROP_ID_itemCategory] = PROP_NAME_itemCategory;
          PROP_NAME_TO_ID.put(PROP_NAME_itemCategory, PROP_ID_itemCategory);
      
          PROP_ID_TO_NAME[PROP_ID_itemGroup] = PROP_NAME_itemGroup;
          PROP_NAME_TO_ID.put(PROP_NAME_itemGroup, PROP_ID_itemGroup);
      
          PROP_ID_TO_NAME[PROP_ID_calcMethod] = PROP_NAME_calcMethod;
          PROP_NAME_TO_ID.put(PROP_NAME_calcMethod, PROP_ID_calcMethod);
      
          PROP_ID_TO_NAME[PROP_ID_formula] = PROP_NAME_formula;
          PROP_NAME_TO_ID.put(PROP_NAME_formula, PROP_ID_formula);
      
          PROP_ID_TO_NAME[PROP_ID_isTaxable] = PROP_NAME_isTaxable;
          PROP_NAME_TO_ID.put(PROP_NAME_isTaxable, PROP_ID_isTaxable);
      
          PROP_ID_TO_NAME[PROP_ID_isSocialInsuranceBase] = PROP_NAME_isSocialInsuranceBase;
          PROP_NAME_TO_ID.put(PROP_NAME_isSocialInsuranceBase, PROP_ID_isSocialInsuranceBase);
      
          PROP_ID_TO_NAME[PROP_ID_isMandatory] = PROP_NAME_isMandatory;
          PROP_NAME_TO_ID.put(PROP_NAME_isMandatory, PROP_ID_isMandatory);
      
          PROP_ID_TO_NAME[PROP_ID_sortOrder] = PROP_NAME_sortOrder;
          PROP_NAME_TO_ID.put(PROP_NAME_sortOrder, PROP_ID_sortOrder);
      
          PROP_ID_TO_NAME[PROP_ID_orgId] = PROP_NAME_orgId;
          PROP_NAME_TO_ID.put(PROP_NAME_orgId, PROP_ID_orgId);
      
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
    
    /* 编码: CODE */
    private java.lang.String _code;
    
    /* 名称: NAME */
    private java.lang.String _name;
    
    /* 类别: ITEM_CATEGORY */
    private java.lang.String _itemCategory;
    
    /* 分组: ITEM_GROUP */
    private java.lang.String _itemGroup;
    
    /* 计算方式: CALC_METHOD */
    private java.lang.String _calcMethod;
    
    /* 计算公式: FORMULA */
    private java.lang.String _formula;
    
    /* 是否应税: IS_TAXABLE */
    private java.lang.Boolean _isTaxable;
    
    /* 计入社保基数: IS_SOCIAL_INSURANCE_BASE */
    private java.lang.Boolean _isSocialInsuranceBase;
    
    /* 是否必含: IS_MANDATORY */
    private java.lang.Boolean _isMandatory;
    
    /* 排序号: SORT_ORDER */
    private java.lang.Integer _sortOrder;
    
    /* 业务组织: ORG_ID */
    private java.lang.Long _orgId;
    
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
    

    public _ErpHrSalaryItem(){
        // for debug
    }

    protected ErpHrSalaryItem newInstance(){
        ErpHrSalaryItem entity = new ErpHrSalaryItem();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpHrSalaryItem cloneInstance() {
        ErpHrSalaryItem entity = newInstance();
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
      return "app.erp.hr.dao.entity.ErpHrSalaryItem";
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
        
            case PROP_ID_name:
               return getName();
        
            case PROP_ID_itemCategory:
               return getItemCategory();
        
            case PROP_ID_itemGroup:
               return getItemGroup();
        
            case PROP_ID_calcMethod:
               return getCalcMethod();
        
            case PROP_ID_formula:
               return getFormula();
        
            case PROP_ID_isTaxable:
               return getIsTaxable();
        
            case PROP_ID_isSocialInsuranceBase:
               return getIsSocialInsuranceBase();
        
            case PROP_ID_isMandatory:
               return getIsMandatory();
        
            case PROP_ID_sortOrder:
               return getSortOrder();
        
            case PROP_ID_orgId:
               return getOrgId();
        
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
        
            case PROP_ID_name:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_name));
               }
               setName(typedValue);
               break;
            }
        
            case PROP_ID_itemCategory:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_itemCategory));
               }
               setItemCategory(typedValue);
               break;
            }
        
            case PROP_ID_itemGroup:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_itemGroup));
               }
               setItemGroup(typedValue);
               break;
            }
        
            case PROP_ID_calcMethod:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_calcMethod));
               }
               setCalcMethod(typedValue);
               break;
            }
        
            case PROP_ID_formula:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_formula));
               }
               setFormula(typedValue);
               break;
            }
        
            case PROP_ID_isTaxable:{
               java.lang.Boolean typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBoolean(value,
                       err-> newTypeConversionError(PROP_NAME_isTaxable));
               }
               setIsTaxable(typedValue);
               break;
            }
        
            case PROP_ID_isSocialInsuranceBase:{
               java.lang.Boolean typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBoolean(value,
                       err-> newTypeConversionError(PROP_NAME_isSocialInsuranceBase));
               }
               setIsSocialInsuranceBase(typedValue);
               break;
            }
        
            case PROP_ID_isMandatory:{
               java.lang.Boolean typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBoolean(value,
                       err-> newTypeConversionError(PROP_NAME_isMandatory));
               }
               setIsMandatory(typedValue);
               break;
            }
        
            case PROP_ID_sortOrder:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_sortOrder));
               }
               setSortOrder(typedValue);
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
        
            case PROP_ID_name:{
               onInitProp(propId);
               this._name = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_itemCategory:{
               onInitProp(propId);
               this._itemCategory = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_itemGroup:{
               onInitProp(propId);
               this._itemGroup = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_calcMethod:{
               onInitProp(propId);
               this._calcMethod = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_formula:{
               onInitProp(propId);
               this._formula = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_isTaxable:{
               onInitProp(propId);
               this._isTaxable = (java.lang.Boolean)value;
               
               break;
            }
        
            case PROP_ID_isSocialInsuranceBase:{
               onInitProp(propId);
               this._isSocialInsuranceBase = (java.lang.Boolean)value;
               
               break;
            }
        
            case PROP_ID_isMandatory:{
               onInitProp(propId);
               this._isMandatory = (java.lang.Boolean)value;
               
               break;
            }
        
            case PROP_ID_sortOrder:{
               onInitProp(propId);
               this._sortOrder = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_orgId:{
               onInitProp(propId);
               this._orgId = (java.lang.Long)value;
               
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
     * 编码: CODE
     */
    public final java.lang.String getCode(){
         onPropGet(PROP_ID_code);
         return _code;
    }

    /**
     * 编码: CODE
     */
    public final void setCode(java.lang.String value){
        if(onPropSet(PROP_ID_code,value)){
            this._code = value;
            internalClearRefs(PROP_ID_code);
            
        }
    }
    
    /**
     * 名称: NAME
     */
    public final java.lang.String getName(){
         onPropGet(PROP_ID_name);
         return _name;
    }

    /**
     * 名称: NAME
     */
    public final void setName(java.lang.String value){
        if(onPropSet(PROP_ID_name,value)){
            this._name = value;
            internalClearRefs(PROP_ID_name);
            
        }
    }
    
    /**
     * 类别: ITEM_CATEGORY
     */
    public final java.lang.String getItemCategory(){
         onPropGet(PROP_ID_itemCategory);
         return _itemCategory;
    }

    /**
     * 类别: ITEM_CATEGORY
     */
    public final void setItemCategory(java.lang.String value){
        if(onPropSet(PROP_ID_itemCategory,value)){
            this._itemCategory = value;
            internalClearRefs(PROP_ID_itemCategory);
            
        }
    }
    
    /**
     * 分组: ITEM_GROUP
     */
    public final java.lang.String getItemGroup(){
         onPropGet(PROP_ID_itemGroup);
         return _itemGroup;
    }

    /**
     * 分组: ITEM_GROUP
     */
    public final void setItemGroup(java.lang.String value){
        if(onPropSet(PROP_ID_itemGroup,value)){
            this._itemGroup = value;
            internalClearRefs(PROP_ID_itemGroup);
            
        }
    }
    
    /**
     * 计算方式: CALC_METHOD
     */
    public final java.lang.String getCalcMethod(){
         onPropGet(PROP_ID_calcMethod);
         return _calcMethod;
    }

    /**
     * 计算方式: CALC_METHOD
     */
    public final void setCalcMethod(java.lang.String value){
        if(onPropSet(PROP_ID_calcMethod,value)){
            this._calcMethod = value;
            internalClearRefs(PROP_ID_calcMethod);
            
        }
    }
    
    /**
     * 计算公式: FORMULA
     */
    public final java.lang.String getFormula(){
         onPropGet(PROP_ID_formula);
         return _formula;
    }

    /**
     * 计算公式: FORMULA
     */
    public final void setFormula(java.lang.String value){
        if(onPropSet(PROP_ID_formula,value)){
            this._formula = value;
            internalClearRefs(PROP_ID_formula);
            
        }
    }
    
    /**
     * 是否应税: IS_TAXABLE
     */
    public final java.lang.Boolean getIsTaxable(){
         onPropGet(PROP_ID_isTaxable);
         return _isTaxable;
    }

    /**
     * 是否应税: IS_TAXABLE
     */
    public final void setIsTaxable(java.lang.Boolean value){
        if(onPropSet(PROP_ID_isTaxable,value)){
            this._isTaxable = value;
            internalClearRefs(PROP_ID_isTaxable);
            
        }
    }
    
    /**
     * 计入社保基数: IS_SOCIAL_INSURANCE_BASE
     */
    public final java.lang.Boolean getIsSocialInsuranceBase(){
         onPropGet(PROP_ID_isSocialInsuranceBase);
         return _isSocialInsuranceBase;
    }

    /**
     * 计入社保基数: IS_SOCIAL_INSURANCE_BASE
     */
    public final void setIsSocialInsuranceBase(java.lang.Boolean value){
        if(onPropSet(PROP_ID_isSocialInsuranceBase,value)){
            this._isSocialInsuranceBase = value;
            internalClearRefs(PROP_ID_isSocialInsuranceBase);
            
        }
    }
    
    /**
     * 是否必含: IS_MANDATORY
     */
    public final java.lang.Boolean getIsMandatory(){
         onPropGet(PROP_ID_isMandatory);
         return _isMandatory;
    }

    /**
     * 是否必含: IS_MANDATORY
     */
    public final void setIsMandatory(java.lang.Boolean value){
        if(onPropSet(PROP_ID_isMandatory,value)){
            this._isMandatory = value;
            internalClearRefs(PROP_ID_isMandatory);
            
        }
    }
    
    /**
     * 排序号: SORT_ORDER
     */
    public final java.lang.Integer getSortOrder(){
         onPropGet(PROP_ID_sortOrder);
         return _sortOrder;
    }

    /**
     * 排序号: SORT_ORDER
     */
    public final void setSortOrder(java.lang.Integer value){
        if(onPropSet(PROP_ID_sortOrder,value)){
            this._sortOrder = value;
            internalClearRefs(PROP_ID_sortOrder);
            
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

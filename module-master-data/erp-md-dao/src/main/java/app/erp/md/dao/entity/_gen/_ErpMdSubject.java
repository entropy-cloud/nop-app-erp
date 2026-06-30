package app.erp.md.dao.entity._gen;

import io.nop.orm.model.IEntityModel;
import io.nop.orm.support.DynamicOrmEntity;
import io.nop.orm.support.OrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code
import io.nop.orm.IOrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code

import io.nop.api.core.convert.ConvertHelper;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;

import app.erp.md.dao.entity.ErpMdSubject;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  会计科目: erp_md_subject
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpMdSubject extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 科目编码: CODE VARCHAR */
    public static final String PROP_NAME_code = "code";
    public static final int PROP_ID_code = 2;
    
    /* 科目名称: NAME VARCHAR */
    public static final String PROP_NAME_name = "name";
    public static final int PROP_ID_name = 3;
    
    /* 父级科目: PARENT_ID BIGINT */
    public static final String PROP_NAME_parentId = "parentId";
    public static final int PROP_ID_parentId = 4;
    
    /* 科目类别: SUBJECT_CLASS INTEGER */
    public static final String PROP_NAME_subjectClass = "subjectClass";
    public static final int PROP_ID_subjectClass = 5;
    
    /* 余额方向: DIRECTION INTEGER */
    public static final String PROP_NAME_direction = "direction";
    public static final int PROP_ID_direction = 6;
    
    /* 余额类型: BALANCE_TYPE INTEGER */
    public static final String PROP_NAME_balanceType = "balanceType";
    public static final int PROP_ID_balanceType = 7;
    
    /* 核算币种: CURRENCY_ID BIGINT */
    public static final String PROP_NAME_currencyId = "currencyId";
    public static final int PROP_ID_currencyId = 8;
    
    /* 辅助-往来单位: IS_AUXILIARY_PARTNER BOOLEAN */
    public static final String PROP_NAME_isAuxiliaryPartner = "isAuxiliaryPartner";
    public static final int PROP_ID_isAuxiliaryPartner = 9;
    
    /* 辅助-部门: IS_AUXILIARY_DEPARTMENT BOOLEAN */
    public static final String PROP_NAME_isAuxiliaryDepartment = "isAuxiliaryDepartment";
    public static final int PROP_ID_isAuxiliaryDepartment = 10;
    
    /* 辅助-项目: IS_AUXILIARY_PROJECT BOOLEAN */
    public static final String PROP_NAME_isAuxiliaryProject = "isAuxiliaryProject";
    public static final int PROP_ID_isAuxiliaryProject = 11;
    
    /* 辅助-仓库: IS_AUXILIARY_WAREHOUSE BOOLEAN */
    public static final String PROP_NAME_isAuxiliaryWarehouse = "isAuxiliaryWarehouse";
    public static final int PROP_ID_isAuxiliaryWarehouse = 12;
    
    /* 辅助-物料: IS_AUXILIARY_PRODUCT BOOLEAN */
    public static final String PROP_NAME_isAuxiliaryProduct = "isAuxiliaryProduct";
    public static final int PROP_ID_isAuxiliaryProduct = 13;
    
    /* 辅助-成本中心: IS_AUXILIARY_COST_CENTER BOOLEAN */
    public static final String PROP_NAME_isAuxiliaryCostCenter = "isAuxiliaryCostCenter";
    public static final int PROP_ID_isAuxiliaryCostCenter = 14;
    
    /* 是否预算控制: IS_BUDGETABLE BOOLEAN */
    public static final String PROP_NAME_isBudgetable = "isBudgetable";
    public static final int PROP_ID_isBudgetable = 15;
    
    /* 是否明细科目: IS_LEAF BOOLEAN */
    public static final String PROP_NAME_isLeaf = "isLeaf";
    public static final int PROP_ID_isLeaf = 16;
    
    /* 状态: STATUS INTEGER */
    public static final String PROP_NAME_status = "status";
    public static final int PROP_ID_status = 17;
    
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
    public static final String PROP_NAME_parent = "parent";
    
    /* relation:  */
    public static final String PROP_NAME_currency = "currency";
    
    /* relation:  */
    public static final String PROP_NAME_children = "children";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[25];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_code] = PROP_NAME_code;
          PROP_NAME_TO_ID.put(PROP_NAME_code, PROP_ID_code);
      
          PROP_ID_TO_NAME[PROP_ID_name] = PROP_NAME_name;
          PROP_NAME_TO_ID.put(PROP_NAME_name, PROP_ID_name);
      
          PROP_ID_TO_NAME[PROP_ID_parentId] = PROP_NAME_parentId;
          PROP_NAME_TO_ID.put(PROP_NAME_parentId, PROP_ID_parentId);
      
          PROP_ID_TO_NAME[PROP_ID_subjectClass] = PROP_NAME_subjectClass;
          PROP_NAME_TO_ID.put(PROP_NAME_subjectClass, PROP_ID_subjectClass);
      
          PROP_ID_TO_NAME[PROP_ID_direction] = PROP_NAME_direction;
          PROP_NAME_TO_ID.put(PROP_NAME_direction, PROP_ID_direction);
      
          PROP_ID_TO_NAME[PROP_ID_balanceType] = PROP_NAME_balanceType;
          PROP_NAME_TO_ID.put(PROP_NAME_balanceType, PROP_ID_balanceType);
      
          PROP_ID_TO_NAME[PROP_ID_currencyId] = PROP_NAME_currencyId;
          PROP_NAME_TO_ID.put(PROP_NAME_currencyId, PROP_ID_currencyId);
      
          PROP_ID_TO_NAME[PROP_ID_isAuxiliaryPartner] = PROP_NAME_isAuxiliaryPartner;
          PROP_NAME_TO_ID.put(PROP_NAME_isAuxiliaryPartner, PROP_ID_isAuxiliaryPartner);
      
          PROP_ID_TO_NAME[PROP_ID_isAuxiliaryDepartment] = PROP_NAME_isAuxiliaryDepartment;
          PROP_NAME_TO_ID.put(PROP_NAME_isAuxiliaryDepartment, PROP_ID_isAuxiliaryDepartment);
      
          PROP_ID_TO_NAME[PROP_ID_isAuxiliaryProject] = PROP_NAME_isAuxiliaryProject;
          PROP_NAME_TO_ID.put(PROP_NAME_isAuxiliaryProject, PROP_ID_isAuxiliaryProject);
      
          PROP_ID_TO_NAME[PROP_ID_isAuxiliaryWarehouse] = PROP_NAME_isAuxiliaryWarehouse;
          PROP_NAME_TO_ID.put(PROP_NAME_isAuxiliaryWarehouse, PROP_ID_isAuxiliaryWarehouse);
      
          PROP_ID_TO_NAME[PROP_ID_isAuxiliaryProduct] = PROP_NAME_isAuxiliaryProduct;
          PROP_NAME_TO_ID.put(PROP_NAME_isAuxiliaryProduct, PROP_ID_isAuxiliaryProduct);
      
          PROP_ID_TO_NAME[PROP_ID_isAuxiliaryCostCenter] = PROP_NAME_isAuxiliaryCostCenter;
          PROP_NAME_TO_ID.put(PROP_NAME_isAuxiliaryCostCenter, PROP_ID_isAuxiliaryCostCenter);
      
          PROP_ID_TO_NAME[PROP_ID_isBudgetable] = PROP_NAME_isBudgetable;
          PROP_NAME_TO_ID.put(PROP_NAME_isBudgetable, PROP_ID_isBudgetable);
      
          PROP_ID_TO_NAME[PROP_ID_isLeaf] = PROP_NAME_isLeaf;
          PROP_NAME_TO_ID.put(PROP_NAME_isLeaf, PROP_ID_isLeaf);
      
          PROP_ID_TO_NAME[PROP_ID_status] = PROP_NAME_status;
          PROP_NAME_TO_ID.put(PROP_NAME_status, PROP_ID_status);
      
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
    
    /* 科目编码: CODE */
    private java.lang.String _code;
    
    /* 科目名称: NAME */
    private java.lang.String _name;
    
    /* 父级科目: PARENT_ID */
    private java.lang.Long _parentId;
    
    /* 科目类别: SUBJECT_CLASS */
    private java.lang.Integer _subjectClass;
    
    /* 余额方向: DIRECTION */
    private java.lang.Integer _direction;
    
    /* 余额类型: BALANCE_TYPE */
    private java.lang.Integer _balanceType;
    
    /* 核算币种: CURRENCY_ID */
    private java.lang.Long _currencyId;
    
    /* 辅助-往来单位: IS_AUXILIARY_PARTNER */
    private java.lang.Boolean _isAuxiliaryPartner;
    
    /* 辅助-部门: IS_AUXILIARY_DEPARTMENT */
    private java.lang.Boolean _isAuxiliaryDepartment;
    
    /* 辅助-项目: IS_AUXILIARY_PROJECT */
    private java.lang.Boolean _isAuxiliaryProject;
    
    /* 辅助-仓库: IS_AUXILIARY_WAREHOUSE */
    private java.lang.Boolean _isAuxiliaryWarehouse;
    
    /* 辅助-物料: IS_AUXILIARY_PRODUCT */
    private java.lang.Boolean _isAuxiliaryProduct;
    
    /* 辅助-成本中心: IS_AUXILIARY_COST_CENTER */
    private java.lang.Boolean _isAuxiliaryCostCenter;
    
    /* 是否预算控制: IS_BUDGETABLE */
    private java.lang.Boolean _isBudgetable;
    
    /* 是否明细科目: IS_LEAF */
    private java.lang.Boolean _isLeaf;
    
    /* 状态: STATUS */
    private java.lang.Integer _status;
    
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
    

    public _ErpMdSubject(){
        // for debug
    }

    protected ErpMdSubject newInstance(){
        ErpMdSubject entity = new ErpMdSubject();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpMdSubject cloneInstance() {
        ErpMdSubject entity = newInstance();
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
      return "app.erp.md.dao.entity.ErpMdSubject";
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
        
            case PROP_ID_parentId:
               return getParentId();
        
            case PROP_ID_subjectClass:
               return getSubjectClass();
        
            case PROP_ID_direction:
               return getDirection();
        
            case PROP_ID_balanceType:
               return getBalanceType();
        
            case PROP_ID_currencyId:
               return getCurrencyId();
        
            case PROP_ID_isAuxiliaryPartner:
               return getIsAuxiliaryPartner();
        
            case PROP_ID_isAuxiliaryDepartment:
               return getIsAuxiliaryDepartment();
        
            case PROP_ID_isAuxiliaryProject:
               return getIsAuxiliaryProject();
        
            case PROP_ID_isAuxiliaryWarehouse:
               return getIsAuxiliaryWarehouse();
        
            case PROP_ID_isAuxiliaryProduct:
               return getIsAuxiliaryProduct();
        
            case PROP_ID_isAuxiliaryCostCenter:
               return getIsAuxiliaryCostCenter();
        
            case PROP_ID_isBudgetable:
               return getIsBudgetable();
        
            case PROP_ID_isLeaf:
               return getIsLeaf();
        
            case PROP_ID_status:
               return getStatus();
        
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
        
            case PROP_ID_parentId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_parentId));
               }
               setParentId(typedValue);
               break;
            }
        
            case PROP_ID_subjectClass:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_subjectClass));
               }
               setSubjectClass(typedValue);
               break;
            }
        
            case PROP_ID_direction:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_direction));
               }
               setDirection(typedValue);
               break;
            }
        
            case PROP_ID_balanceType:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_balanceType));
               }
               setBalanceType(typedValue);
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
        
            case PROP_ID_isAuxiliaryPartner:{
               java.lang.Boolean typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBoolean(value,
                       err-> newTypeConversionError(PROP_NAME_isAuxiliaryPartner));
               }
               setIsAuxiliaryPartner(typedValue);
               break;
            }
        
            case PROP_ID_isAuxiliaryDepartment:{
               java.lang.Boolean typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBoolean(value,
                       err-> newTypeConversionError(PROP_NAME_isAuxiliaryDepartment));
               }
               setIsAuxiliaryDepartment(typedValue);
               break;
            }
        
            case PROP_ID_isAuxiliaryProject:{
               java.lang.Boolean typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBoolean(value,
                       err-> newTypeConversionError(PROP_NAME_isAuxiliaryProject));
               }
               setIsAuxiliaryProject(typedValue);
               break;
            }
        
            case PROP_ID_isAuxiliaryWarehouse:{
               java.lang.Boolean typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBoolean(value,
                       err-> newTypeConversionError(PROP_NAME_isAuxiliaryWarehouse));
               }
               setIsAuxiliaryWarehouse(typedValue);
               break;
            }
        
            case PROP_ID_isAuxiliaryProduct:{
               java.lang.Boolean typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBoolean(value,
                       err-> newTypeConversionError(PROP_NAME_isAuxiliaryProduct));
               }
               setIsAuxiliaryProduct(typedValue);
               break;
            }
        
            case PROP_ID_isAuxiliaryCostCenter:{
               java.lang.Boolean typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBoolean(value,
                       err-> newTypeConversionError(PROP_NAME_isAuxiliaryCostCenter));
               }
               setIsAuxiliaryCostCenter(typedValue);
               break;
            }
        
            case PROP_ID_isBudgetable:{
               java.lang.Boolean typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBoolean(value,
                       err-> newTypeConversionError(PROP_NAME_isBudgetable));
               }
               setIsBudgetable(typedValue);
               break;
            }
        
            case PROP_ID_isLeaf:{
               java.lang.Boolean typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBoolean(value,
                       err-> newTypeConversionError(PROP_NAME_isLeaf));
               }
               setIsLeaf(typedValue);
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
        
            case PROP_ID_parentId:{
               onInitProp(propId);
               this._parentId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_subjectClass:{
               onInitProp(propId);
               this._subjectClass = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_direction:{
               onInitProp(propId);
               this._direction = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_balanceType:{
               onInitProp(propId);
               this._balanceType = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_currencyId:{
               onInitProp(propId);
               this._currencyId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_isAuxiliaryPartner:{
               onInitProp(propId);
               this._isAuxiliaryPartner = (java.lang.Boolean)value;
               
               break;
            }
        
            case PROP_ID_isAuxiliaryDepartment:{
               onInitProp(propId);
               this._isAuxiliaryDepartment = (java.lang.Boolean)value;
               
               break;
            }
        
            case PROP_ID_isAuxiliaryProject:{
               onInitProp(propId);
               this._isAuxiliaryProject = (java.lang.Boolean)value;
               
               break;
            }
        
            case PROP_ID_isAuxiliaryWarehouse:{
               onInitProp(propId);
               this._isAuxiliaryWarehouse = (java.lang.Boolean)value;
               
               break;
            }
        
            case PROP_ID_isAuxiliaryProduct:{
               onInitProp(propId);
               this._isAuxiliaryProduct = (java.lang.Boolean)value;
               
               break;
            }
        
            case PROP_ID_isAuxiliaryCostCenter:{
               onInitProp(propId);
               this._isAuxiliaryCostCenter = (java.lang.Boolean)value;
               
               break;
            }
        
            case PROP_ID_isBudgetable:{
               onInitProp(propId);
               this._isBudgetable = (java.lang.Boolean)value;
               
               break;
            }
        
            case PROP_ID_isLeaf:{
               onInitProp(propId);
               this._isLeaf = (java.lang.Boolean)value;
               
               break;
            }
        
            case PROP_ID_status:{
               onInitProp(propId);
               this._status = (java.lang.Integer)value;
               
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
     * 科目编码: CODE
     */
    public final java.lang.String getCode(){
         onPropGet(PROP_ID_code);
         return _code;
    }

    /**
     * 科目编码: CODE
     */
    public final void setCode(java.lang.String value){
        if(onPropSet(PROP_ID_code,value)){
            this._code = value;
            internalClearRefs(PROP_ID_code);
            
        }
    }
    
    /**
     * 科目名称: NAME
     */
    public final java.lang.String getName(){
         onPropGet(PROP_ID_name);
         return _name;
    }

    /**
     * 科目名称: NAME
     */
    public final void setName(java.lang.String value){
        if(onPropSet(PROP_ID_name,value)){
            this._name = value;
            internalClearRefs(PROP_ID_name);
            
        }
    }
    
    /**
     * 父级科目: PARENT_ID
     */
    public final java.lang.Long getParentId(){
         onPropGet(PROP_ID_parentId);
         return _parentId;
    }

    /**
     * 父级科目: PARENT_ID
     */
    public final void setParentId(java.lang.Long value){
        if(onPropSet(PROP_ID_parentId,value)){
            this._parentId = value;
            internalClearRefs(PROP_ID_parentId);
            
        }
    }
    
    /**
     * 科目类别: SUBJECT_CLASS
     */
    public final java.lang.Integer getSubjectClass(){
         onPropGet(PROP_ID_subjectClass);
         return _subjectClass;
    }

    /**
     * 科目类别: SUBJECT_CLASS
     */
    public final void setSubjectClass(java.lang.Integer value){
        if(onPropSet(PROP_ID_subjectClass,value)){
            this._subjectClass = value;
            internalClearRefs(PROP_ID_subjectClass);
            
        }
    }
    
    /**
     * 余额方向: DIRECTION
     */
    public final java.lang.Integer getDirection(){
         onPropGet(PROP_ID_direction);
         return _direction;
    }

    /**
     * 余额方向: DIRECTION
     */
    public final void setDirection(java.lang.Integer value){
        if(onPropSet(PROP_ID_direction,value)){
            this._direction = value;
            internalClearRefs(PROP_ID_direction);
            
        }
    }
    
    /**
     * 余额类型: BALANCE_TYPE
     */
    public final java.lang.Integer getBalanceType(){
         onPropGet(PROP_ID_balanceType);
         return _balanceType;
    }

    /**
     * 余额类型: BALANCE_TYPE
     */
    public final void setBalanceType(java.lang.Integer value){
        if(onPropSet(PROP_ID_balanceType,value)){
            this._balanceType = value;
            internalClearRefs(PROP_ID_balanceType);
            
        }
    }
    
    /**
     * 核算币种: CURRENCY_ID
     */
    public final java.lang.Long getCurrencyId(){
         onPropGet(PROP_ID_currencyId);
         return _currencyId;
    }

    /**
     * 核算币种: CURRENCY_ID
     */
    public final void setCurrencyId(java.lang.Long value){
        if(onPropSet(PROP_ID_currencyId,value)){
            this._currencyId = value;
            internalClearRefs(PROP_ID_currencyId);
            
        }
    }
    
    /**
     * 辅助-往来单位: IS_AUXILIARY_PARTNER
     */
    public final java.lang.Boolean getIsAuxiliaryPartner(){
         onPropGet(PROP_ID_isAuxiliaryPartner);
         return _isAuxiliaryPartner;
    }

    /**
     * 辅助-往来单位: IS_AUXILIARY_PARTNER
     */
    public final void setIsAuxiliaryPartner(java.lang.Boolean value){
        if(onPropSet(PROP_ID_isAuxiliaryPartner,value)){
            this._isAuxiliaryPartner = value;
            internalClearRefs(PROP_ID_isAuxiliaryPartner);
            
        }
    }
    
    /**
     * 辅助-部门: IS_AUXILIARY_DEPARTMENT
     */
    public final java.lang.Boolean getIsAuxiliaryDepartment(){
         onPropGet(PROP_ID_isAuxiliaryDepartment);
         return _isAuxiliaryDepartment;
    }

    /**
     * 辅助-部门: IS_AUXILIARY_DEPARTMENT
     */
    public final void setIsAuxiliaryDepartment(java.lang.Boolean value){
        if(onPropSet(PROP_ID_isAuxiliaryDepartment,value)){
            this._isAuxiliaryDepartment = value;
            internalClearRefs(PROP_ID_isAuxiliaryDepartment);
            
        }
    }
    
    /**
     * 辅助-项目: IS_AUXILIARY_PROJECT
     */
    public final java.lang.Boolean getIsAuxiliaryProject(){
         onPropGet(PROP_ID_isAuxiliaryProject);
         return _isAuxiliaryProject;
    }

    /**
     * 辅助-项目: IS_AUXILIARY_PROJECT
     */
    public final void setIsAuxiliaryProject(java.lang.Boolean value){
        if(onPropSet(PROP_ID_isAuxiliaryProject,value)){
            this._isAuxiliaryProject = value;
            internalClearRefs(PROP_ID_isAuxiliaryProject);
            
        }
    }
    
    /**
     * 辅助-仓库: IS_AUXILIARY_WAREHOUSE
     */
    public final java.lang.Boolean getIsAuxiliaryWarehouse(){
         onPropGet(PROP_ID_isAuxiliaryWarehouse);
         return _isAuxiliaryWarehouse;
    }

    /**
     * 辅助-仓库: IS_AUXILIARY_WAREHOUSE
     */
    public final void setIsAuxiliaryWarehouse(java.lang.Boolean value){
        if(onPropSet(PROP_ID_isAuxiliaryWarehouse,value)){
            this._isAuxiliaryWarehouse = value;
            internalClearRefs(PROP_ID_isAuxiliaryWarehouse);
            
        }
    }
    
    /**
     * 辅助-物料: IS_AUXILIARY_PRODUCT
     */
    public final java.lang.Boolean getIsAuxiliaryProduct(){
         onPropGet(PROP_ID_isAuxiliaryProduct);
         return _isAuxiliaryProduct;
    }

    /**
     * 辅助-物料: IS_AUXILIARY_PRODUCT
     */
    public final void setIsAuxiliaryProduct(java.lang.Boolean value){
        if(onPropSet(PROP_ID_isAuxiliaryProduct,value)){
            this._isAuxiliaryProduct = value;
            internalClearRefs(PROP_ID_isAuxiliaryProduct);
            
        }
    }
    
    /**
     * 辅助-成本中心: IS_AUXILIARY_COST_CENTER
     */
    public final java.lang.Boolean getIsAuxiliaryCostCenter(){
         onPropGet(PROP_ID_isAuxiliaryCostCenter);
         return _isAuxiliaryCostCenter;
    }

    /**
     * 辅助-成本中心: IS_AUXILIARY_COST_CENTER
     */
    public final void setIsAuxiliaryCostCenter(java.lang.Boolean value){
        if(onPropSet(PROP_ID_isAuxiliaryCostCenter,value)){
            this._isAuxiliaryCostCenter = value;
            internalClearRefs(PROP_ID_isAuxiliaryCostCenter);
            
        }
    }
    
    /**
     * 是否预算控制: IS_BUDGETABLE
     */
    public final java.lang.Boolean getIsBudgetable(){
         onPropGet(PROP_ID_isBudgetable);
         return _isBudgetable;
    }

    /**
     * 是否预算控制: IS_BUDGETABLE
     */
    public final void setIsBudgetable(java.lang.Boolean value){
        if(onPropSet(PROP_ID_isBudgetable,value)){
            this._isBudgetable = value;
            internalClearRefs(PROP_ID_isBudgetable);
            
        }
    }
    
    /**
     * 是否明细科目: IS_LEAF
     */
    public final java.lang.Boolean getIsLeaf(){
         onPropGet(PROP_ID_isLeaf);
         return _isLeaf;
    }

    /**
     * 是否明细科目: IS_LEAF
     */
    public final void setIsLeaf(java.lang.Boolean value){
        if(onPropSet(PROP_ID_isLeaf,value)){
            this._isLeaf = value;
            internalClearRefs(PROP_ID_isLeaf);
            
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
    public final app.erp.md.dao.entity.ErpMdSubject getParent(){
       return (app.erp.md.dao.entity.ErpMdSubject)internalGetRefEntity(PROP_NAME_parent);
    }

    public final void setParent(app.erp.md.dao.entity.ErpMdSubject refEntity){
   
           if(refEntity == null){
           
                   this.setParentId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_parent, refEntity,()->{
           
                           this.setParentId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.md.dao.entity.ErpMdCurrency getCurrency(){
       return (app.erp.md.dao.entity.ErpMdCurrency)internalGetRefEntity(PROP_NAME_currency);
    }

    public final void setCurrency(app.erp.md.dao.entity.ErpMdCurrency refEntity){
   
           if(refEntity == null){
           
                   this.setCurrencyId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_currency, refEntity,()->{
           
                           this.setCurrencyId(refEntity.getId());
                       
           });
           }
       
    }
       
    private final OrmEntitySet<app.erp.md.dao.entity.ErpMdSubject> _children = new OrmEntitySet<>(this, PROP_NAME_children,
        null, null,app.erp.md.dao.entity.ErpMdSubject.class);

    /**
     * 。 refPropName: , keyProp: {rel.keyProp}
     */
    public final IOrmEntitySet<app.erp.md.dao.entity.ErpMdSubject> getChildren(){
       return _children;
    }
       
}
// resume CPD analysis - CPD-ON

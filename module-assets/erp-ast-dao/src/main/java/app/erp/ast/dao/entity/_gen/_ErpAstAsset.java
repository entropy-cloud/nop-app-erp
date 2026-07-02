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

import app.erp.ast.dao.entity.ErpAstAsset;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  固定资产: erp_ast_asset
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpAstAsset extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 资产编码: CODE VARCHAR */
    public static final String PROP_NAME_code = "code";
    public static final int PROP_ID_code = 2;
    
    /* 资产名称: NAME VARCHAR */
    public static final String PROP_NAME_name = "name";
    public static final int PROP_ID_name = 3;
    
    /* 所属组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 4;
    
    /* 资产类别: CATEGORY_ID BIGINT */
    public static final String PROP_NAME_categoryId = "categoryId";
    public static final int PROP_ID_categoryId = 5;
    
    /* 取得日期: ACQUISITION_DATE DATE */
    public static final String PROP_NAME_acquisitionDate = "acquisitionDate";
    public static final int PROP_ID_acquisitionDate = 6;
    
    /* 币种: CURRENCY_ID BIGINT */
    public static final String PROP_NAME_currencyId = "currencyId";
    public static final int PROP_ID_currencyId = 7;
    
    /* 原值: ORIGINAL_VALUE DECIMAL */
    public static final String PROP_NAME_originalValue = "originalValue";
    public static final int PROP_ID_originalValue = 8;
    
    /* 当前价值: CURRENT_VALUE DECIMAL */
    public static final String PROP_NAME_currentValue = "currentValue";
    public static final int PROP_ID_currentValue = 9;
    
    /* 残值: RESIDUAL_VALUE DECIMAL */
    public static final String PROP_NAME_residualValue = "residualValue";
    public static final int PROP_ID_residualValue = 10;
    
    /* 折旧方法: DEPRECIATION_METHOD INTEGER */
    public static final String PROP_NAME_depreciationMethod = "depreciationMethod";
    public static final int PROP_ID_depreciationMethod = 11;
    
    /* 折旧率: DEPRECIATION_RATE DECIMAL */
    public static final String PROP_NAME_depreciationRate = "depreciationRate";
    public static final int PROP_ID_depreciationRate = 12;
    
    /* 使用年限(月): USEFUL_LIFE_MONTHS INTEGER */
    public static final String PROP_NAME_usefulLifeMonths = "usefulLifeMonths";
    public static final int PROP_ID_usefulLifeMonths = 13;
    
    /* 使用部门: DEPARTMENT_ID BIGINT */
    public static final String PROP_NAME_departmentId = "departmentId";
    public static final int PROP_ID_departmentId = 14;
    
    /* 使用地点: LOCATION_ID BIGINT */
    public static final String PROP_NAME_locationId = "locationId";
    public static final int PROP_ID_locationId = 15;
    
    /* 使用人(职员): EMPLOYEE_ID BIGINT */
    public static final String PROP_NAME_employeeId = "employeeId";
    public static final int PROP_ID_employeeId = 16;
    
    /* 使用人(往来单位,旧字段保留): STAFF_ID BIGINT */
    public static final String PROP_NAME_staffId = "staffId";
    public static final int PROP_ID_staffId = 17;
    
    /* 品牌型号: BRAND_MODEL VARCHAR */
    public static final String PROP_NAME_brandModel = "brandModel";
    public static final int PROP_ID_brandModel = 18;
    
    /* 资产状态: STATUS INTEGER */
    public static final String PROP_NAME_status = "status";
    public static final int PROP_ID_status = 19;
    
    /* 逻辑删除版本: DEL_VERSION BIGINT */
    public static final String PROP_NAME_delVersion = "delVersion";
    public static final int PROP_ID_delVersion = 20;
    
    /* 数据版本: VERSION INTEGER */
    public static final String PROP_NAME_version = "version";
    public static final int PROP_ID_version = 21;
    
    /* 创建人: CREATED_BY VARCHAR */
    public static final String PROP_NAME_createdBy = "createdBy";
    public static final int PROP_ID_createdBy = 22;
    
    /* 创建时间: CREATE_TIME TIMESTAMP */
    public static final String PROP_NAME_createTime = "createTime";
    public static final int PROP_ID_createTime = 23;
    
    /* 修改人: UPDATED_BY VARCHAR */
    public static final String PROP_NAME_updatedBy = "updatedBy";
    public static final int PROP_ID_updatedBy = 24;
    
    /* 修改时间: UPDATE_TIME TIMESTAMP */
    public static final String PROP_NAME_updateTime = "updateTime";
    public static final int PROP_ID_updateTime = 25;
    
    /* 备注: REMARK VARCHAR */
    public static final String PROP_NAME_remark = "remark";
    public static final int PROP_ID_remark = 26;
    
    /* 累计折旧: ACCUMULATED_DEPRECIATION DECIMAL */
    public static final String PROP_NAME_accumulatedDepreciation = "accumulatedDepreciation";
    public static final int PROP_ID_accumulatedDepreciation = 27;
    
    /* 净值: NET_BOOK_VALUE DECIMAL */
    public static final String PROP_NAME_netBookValue = "netBookValue";
    public static final int PROP_ID_netBookValue = 28;
    

    private static int _PROP_ID_BOUND = 29;

    
    /* relation:  */
    public static final String PROP_NAME_category = "category";
    
    /* relation:  */
    public static final String PROP_NAME_department = "department";
    
    /* relation:  */
    public static final String PROP_NAME_location = "location";
    
    /* relation:  */
    public static final String PROP_NAME_employee = "employee";
    
    /* relation:  */
    public static final String PROP_NAME_currency = "currency";
    
    /* relation:  */
    public static final String PROP_NAME_org = "org";
    
    /* relation:  */
    public static final String PROP_NAME_staff = "staff";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[29];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_code] = PROP_NAME_code;
          PROP_NAME_TO_ID.put(PROP_NAME_code, PROP_ID_code);
      
          PROP_ID_TO_NAME[PROP_ID_name] = PROP_NAME_name;
          PROP_NAME_TO_ID.put(PROP_NAME_name, PROP_ID_name);
      
          PROP_ID_TO_NAME[PROP_ID_orgId] = PROP_NAME_orgId;
          PROP_NAME_TO_ID.put(PROP_NAME_orgId, PROP_ID_orgId);
      
          PROP_ID_TO_NAME[PROP_ID_categoryId] = PROP_NAME_categoryId;
          PROP_NAME_TO_ID.put(PROP_NAME_categoryId, PROP_ID_categoryId);
      
          PROP_ID_TO_NAME[PROP_ID_acquisitionDate] = PROP_NAME_acquisitionDate;
          PROP_NAME_TO_ID.put(PROP_NAME_acquisitionDate, PROP_ID_acquisitionDate);
      
          PROP_ID_TO_NAME[PROP_ID_currencyId] = PROP_NAME_currencyId;
          PROP_NAME_TO_ID.put(PROP_NAME_currencyId, PROP_ID_currencyId);
      
          PROP_ID_TO_NAME[PROP_ID_originalValue] = PROP_NAME_originalValue;
          PROP_NAME_TO_ID.put(PROP_NAME_originalValue, PROP_ID_originalValue);
      
          PROP_ID_TO_NAME[PROP_ID_currentValue] = PROP_NAME_currentValue;
          PROP_NAME_TO_ID.put(PROP_NAME_currentValue, PROP_ID_currentValue);
      
          PROP_ID_TO_NAME[PROP_ID_residualValue] = PROP_NAME_residualValue;
          PROP_NAME_TO_ID.put(PROP_NAME_residualValue, PROP_ID_residualValue);
      
          PROP_ID_TO_NAME[PROP_ID_depreciationMethod] = PROP_NAME_depreciationMethod;
          PROP_NAME_TO_ID.put(PROP_NAME_depreciationMethod, PROP_ID_depreciationMethod);
      
          PROP_ID_TO_NAME[PROP_ID_depreciationRate] = PROP_NAME_depreciationRate;
          PROP_NAME_TO_ID.put(PROP_NAME_depreciationRate, PROP_ID_depreciationRate);
      
          PROP_ID_TO_NAME[PROP_ID_usefulLifeMonths] = PROP_NAME_usefulLifeMonths;
          PROP_NAME_TO_ID.put(PROP_NAME_usefulLifeMonths, PROP_ID_usefulLifeMonths);
      
          PROP_ID_TO_NAME[PROP_ID_departmentId] = PROP_NAME_departmentId;
          PROP_NAME_TO_ID.put(PROP_NAME_departmentId, PROP_ID_departmentId);
      
          PROP_ID_TO_NAME[PROP_ID_locationId] = PROP_NAME_locationId;
          PROP_NAME_TO_ID.put(PROP_NAME_locationId, PROP_ID_locationId);
      
          PROP_ID_TO_NAME[PROP_ID_employeeId] = PROP_NAME_employeeId;
          PROP_NAME_TO_ID.put(PROP_NAME_employeeId, PROP_ID_employeeId);
      
          PROP_ID_TO_NAME[PROP_ID_staffId] = PROP_NAME_staffId;
          PROP_NAME_TO_ID.put(PROP_NAME_staffId, PROP_ID_staffId);
      
          PROP_ID_TO_NAME[PROP_ID_brandModel] = PROP_NAME_brandModel;
          PROP_NAME_TO_ID.put(PROP_NAME_brandModel, PROP_ID_brandModel);
      
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
      
          PROP_ID_TO_NAME[PROP_ID_accumulatedDepreciation] = PROP_NAME_accumulatedDepreciation;
          PROP_NAME_TO_ID.put(PROP_NAME_accumulatedDepreciation, PROP_ID_accumulatedDepreciation);
      
          PROP_ID_TO_NAME[PROP_ID_netBookValue] = PROP_NAME_netBookValue;
          PROP_NAME_TO_ID.put(PROP_NAME_netBookValue, PROP_ID_netBookValue);
      
    }

    
    /* ID: ID */
    private java.lang.Long _id;
    
    /* 资产编码: CODE */
    private java.lang.String _code;
    
    /* 资产名称: NAME */
    private java.lang.String _name;
    
    /* 所属组织: ORG_ID */
    private java.lang.Long _orgId;
    
    /* 资产类别: CATEGORY_ID */
    private java.lang.Long _categoryId;
    
    /* 取得日期: ACQUISITION_DATE */
    private java.time.LocalDate _acquisitionDate;
    
    /* 币种: CURRENCY_ID */
    private java.lang.Long _currencyId;
    
    /* 原值: ORIGINAL_VALUE */
    private java.math.BigDecimal _originalValue;
    
    /* 当前价值: CURRENT_VALUE */
    private java.math.BigDecimal _currentValue;
    
    /* 残值: RESIDUAL_VALUE */
    private java.math.BigDecimal _residualValue;
    
    /* 折旧方法: DEPRECIATION_METHOD */
    private java.lang.Integer _depreciationMethod;
    
    /* 折旧率: DEPRECIATION_RATE */
    private java.math.BigDecimal _depreciationRate;
    
    /* 使用年限(月): USEFUL_LIFE_MONTHS */
    private java.lang.Integer _usefulLifeMonths;
    
    /* 使用部门: DEPARTMENT_ID */
    private java.lang.Long _departmentId;
    
    /* 使用地点: LOCATION_ID */
    private java.lang.Long _locationId;
    
    /* 使用人(职员): EMPLOYEE_ID */
    private java.lang.Long _employeeId;
    
    /* 使用人(往来单位,旧字段保留): STAFF_ID */
    private java.lang.Long _staffId;
    
    /* 品牌型号: BRAND_MODEL */
    private java.lang.String _brandModel;
    
    /* 资产状态: STATUS */
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
    
    /* 累计折旧: ACCUMULATED_DEPRECIATION */
    private java.math.BigDecimal _accumulatedDepreciation;
    
    /* 净值: NET_BOOK_VALUE */
    private java.math.BigDecimal _netBookValue;
    

    public _ErpAstAsset(){
        // for debug
    }

    protected ErpAstAsset newInstance(){
        ErpAstAsset entity = new ErpAstAsset();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpAstAsset cloneInstance() {
        ErpAstAsset entity = newInstance();
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
      return "app.erp.ast.dao.entity.ErpAstAsset";
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
        
            case PROP_ID_orgId:
               return getOrgId();
        
            case PROP_ID_categoryId:
               return getCategoryId();
        
            case PROP_ID_acquisitionDate:
               return getAcquisitionDate();
        
            case PROP_ID_currencyId:
               return getCurrencyId();
        
            case PROP_ID_originalValue:
               return getOriginalValue();
        
            case PROP_ID_currentValue:
               return getCurrentValue();
        
            case PROP_ID_residualValue:
               return getResidualValue();
        
            case PROP_ID_depreciationMethod:
               return getDepreciationMethod();
        
            case PROP_ID_depreciationRate:
               return getDepreciationRate();
        
            case PROP_ID_usefulLifeMonths:
               return getUsefulLifeMonths();
        
            case PROP_ID_departmentId:
               return getDepartmentId();
        
            case PROP_ID_locationId:
               return getLocationId();
        
            case PROP_ID_employeeId:
               return getEmployeeId();
        
            case PROP_ID_staffId:
               return getStaffId();
        
            case PROP_ID_brandModel:
               return getBrandModel();
        
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
        
            case PROP_ID_accumulatedDepreciation:
               return getAccumulatedDepreciation();
        
            case PROP_ID_netBookValue:
               return getNetBookValue();
        
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
        
            case PROP_ID_orgId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_orgId));
               }
               setOrgId(typedValue);
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
        
            case PROP_ID_acquisitionDate:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_acquisitionDate));
               }
               setAcquisitionDate(typedValue);
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
        
            case PROP_ID_originalValue:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_originalValue));
               }
               setOriginalValue(typedValue);
               break;
            }
        
            case PROP_ID_currentValue:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_currentValue));
               }
               setCurrentValue(typedValue);
               break;
            }
        
            case PROP_ID_residualValue:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_residualValue));
               }
               setResidualValue(typedValue);
               break;
            }
        
            case PROP_ID_depreciationMethod:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_depreciationMethod));
               }
               setDepreciationMethod(typedValue);
               break;
            }
        
            case PROP_ID_depreciationRate:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_depreciationRate));
               }
               setDepreciationRate(typedValue);
               break;
            }
        
            case PROP_ID_usefulLifeMonths:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_usefulLifeMonths));
               }
               setUsefulLifeMonths(typedValue);
               break;
            }
        
            case PROP_ID_departmentId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_departmentId));
               }
               setDepartmentId(typedValue);
               break;
            }
        
            case PROP_ID_locationId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_locationId));
               }
               setLocationId(typedValue);
               break;
            }
        
            case PROP_ID_employeeId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_employeeId));
               }
               setEmployeeId(typedValue);
               break;
            }
        
            case PROP_ID_staffId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_staffId));
               }
               setStaffId(typedValue);
               break;
            }
        
            case PROP_ID_brandModel:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_brandModel));
               }
               setBrandModel(typedValue);
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
        
            case PROP_ID_accumulatedDepreciation:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_accumulatedDepreciation));
               }
               setAccumulatedDepreciation(typedValue);
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
        
            case PROP_ID_orgId:{
               onInitProp(propId);
               this._orgId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_categoryId:{
               onInitProp(propId);
               this._categoryId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_acquisitionDate:{
               onInitProp(propId);
               this._acquisitionDate = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_currencyId:{
               onInitProp(propId);
               this._currencyId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_originalValue:{
               onInitProp(propId);
               this._originalValue = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_currentValue:{
               onInitProp(propId);
               this._currentValue = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_residualValue:{
               onInitProp(propId);
               this._residualValue = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_depreciationMethod:{
               onInitProp(propId);
               this._depreciationMethod = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_depreciationRate:{
               onInitProp(propId);
               this._depreciationRate = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_usefulLifeMonths:{
               onInitProp(propId);
               this._usefulLifeMonths = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_departmentId:{
               onInitProp(propId);
               this._departmentId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_locationId:{
               onInitProp(propId);
               this._locationId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_employeeId:{
               onInitProp(propId);
               this._employeeId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_staffId:{
               onInitProp(propId);
               this._staffId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_brandModel:{
               onInitProp(propId);
               this._brandModel = (java.lang.String)value;
               
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
        
            case PROP_ID_accumulatedDepreciation:{
               onInitProp(propId);
               this._accumulatedDepreciation = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_netBookValue:{
               onInitProp(propId);
               this._netBookValue = (java.math.BigDecimal)value;
               
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
     * 资产编码: CODE
     */
    public final java.lang.String getCode(){
         onPropGet(PROP_ID_code);
         return _code;
    }

    /**
     * 资产编码: CODE
     */
    public final void setCode(java.lang.String value){
        if(onPropSet(PROP_ID_code,value)){
            this._code = value;
            internalClearRefs(PROP_ID_code);
            
        }
    }
    
    /**
     * 资产名称: NAME
     */
    public final java.lang.String getName(){
         onPropGet(PROP_ID_name);
         return _name;
    }

    /**
     * 资产名称: NAME
     */
    public final void setName(java.lang.String value){
        if(onPropSet(PROP_ID_name,value)){
            this._name = value;
            internalClearRefs(PROP_ID_name);
            
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
     * 资产类别: CATEGORY_ID
     */
    public final java.lang.Long getCategoryId(){
         onPropGet(PROP_ID_categoryId);
         return _categoryId;
    }

    /**
     * 资产类别: CATEGORY_ID
     */
    public final void setCategoryId(java.lang.Long value){
        if(onPropSet(PROP_ID_categoryId,value)){
            this._categoryId = value;
            internalClearRefs(PROP_ID_categoryId);
            
        }
    }
    
    /**
     * 取得日期: ACQUISITION_DATE
     */
    public final java.time.LocalDate getAcquisitionDate(){
         onPropGet(PROP_ID_acquisitionDate);
         return _acquisitionDate;
    }

    /**
     * 取得日期: ACQUISITION_DATE
     */
    public final void setAcquisitionDate(java.time.LocalDate value){
        if(onPropSet(PROP_ID_acquisitionDate,value)){
            this._acquisitionDate = value;
            internalClearRefs(PROP_ID_acquisitionDate);
            
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
     * 原值: ORIGINAL_VALUE
     */
    public final java.math.BigDecimal getOriginalValue(){
         onPropGet(PROP_ID_originalValue);
         return _originalValue;
    }

    /**
     * 原值: ORIGINAL_VALUE
     */
    public final void setOriginalValue(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_originalValue,value)){
            this._originalValue = value;
            internalClearRefs(PROP_ID_originalValue);
            
        }
    }
    
    /**
     * 当前价值: CURRENT_VALUE
     */
    public final java.math.BigDecimal getCurrentValue(){
         onPropGet(PROP_ID_currentValue);
         return _currentValue;
    }

    /**
     * 当前价值: CURRENT_VALUE
     */
    public final void setCurrentValue(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_currentValue,value)){
            this._currentValue = value;
            internalClearRefs(PROP_ID_currentValue);
            
        }
    }
    
    /**
     * 残值: RESIDUAL_VALUE
     */
    public final java.math.BigDecimal getResidualValue(){
         onPropGet(PROP_ID_residualValue);
         return _residualValue;
    }

    /**
     * 残值: RESIDUAL_VALUE
     */
    public final void setResidualValue(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_residualValue,value)){
            this._residualValue = value;
            internalClearRefs(PROP_ID_residualValue);
            
        }
    }
    
    /**
     * 折旧方法: DEPRECIATION_METHOD
     */
    public final java.lang.Integer getDepreciationMethod(){
         onPropGet(PROP_ID_depreciationMethod);
         return _depreciationMethod;
    }

    /**
     * 折旧方法: DEPRECIATION_METHOD
     */
    public final void setDepreciationMethod(java.lang.Integer value){
        if(onPropSet(PROP_ID_depreciationMethod,value)){
            this._depreciationMethod = value;
            internalClearRefs(PROP_ID_depreciationMethod);
            
        }
    }
    
    /**
     * 折旧率: DEPRECIATION_RATE
     */
    public final java.math.BigDecimal getDepreciationRate(){
         onPropGet(PROP_ID_depreciationRate);
         return _depreciationRate;
    }

    /**
     * 折旧率: DEPRECIATION_RATE
     */
    public final void setDepreciationRate(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_depreciationRate,value)){
            this._depreciationRate = value;
            internalClearRefs(PROP_ID_depreciationRate);
            
        }
    }
    
    /**
     * 使用年限(月): USEFUL_LIFE_MONTHS
     */
    public final java.lang.Integer getUsefulLifeMonths(){
         onPropGet(PROP_ID_usefulLifeMonths);
         return _usefulLifeMonths;
    }

    /**
     * 使用年限(月): USEFUL_LIFE_MONTHS
     */
    public final void setUsefulLifeMonths(java.lang.Integer value){
        if(onPropSet(PROP_ID_usefulLifeMonths,value)){
            this._usefulLifeMonths = value;
            internalClearRefs(PROP_ID_usefulLifeMonths);
            
        }
    }
    
    /**
     * 使用部门: DEPARTMENT_ID
     */
    public final java.lang.Long getDepartmentId(){
         onPropGet(PROP_ID_departmentId);
         return _departmentId;
    }

    /**
     * 使用部门: DEPARTMENT_ID
     */
    public final void setDepartmentId(java.lang.Long value){
        if(onPropSet(PROP_ID_departmentId,value)){
            this._departmentId = value;
            internalClearRefs(PROP_ID_departmentId);
            
        }
    }
    
    /**
     * 使用地点: LOCATION_ID
     */
    public final java.lang.Long getLocationId(){
         onPropGet(PROP_ID_locationId);
         return _locationId;
    }

    /**
     * 使用地点: LOCATION_ID
     */
    public final void setLocationId(java.lang.Long value){
        if(onPropSet(PROP_ID_locationId,value)){
            this._locationId = value;
            internalClearRefs(PROP_ID_locationId);
            
        }
    }
    
    /**
     * 使用人(职员): EMPLOYEE_ID
     */
    public final java.lang.Long getEmployeeId(){
         onPropGet(PROP_ID_employeeId);
         return _employeeId;
    }

    /**
     * 使用人(职员): EMPLOYEE_ID
     */
    public final void setEmployeeId(java.lang.Long value){
        if(onPropSet(PROP_ID_employeeId,value)){
            this._employeeId = value;
            internalClearRefs(PROP_ID_employeeId);
            
        }
    }
    
    /**
     * 使用人(往来单位,旧字段保留): STAFF_ID
     */
    public final java.lang.Long getStaffId(){
         onPropGet(PROP_ID_staffId);
         return _staffId;
    }

    /**
     * 使用人(往来单位,旧字段保留): STAFF_ID
     */
    public final void setStaffId(java.lang.Long value){
        if(onPropSet(PROP_ID_staffId,value)){
            this._staffId = value;
            internalClearRefs(PROP_ID_staffId);
            
        }
    }
    
    /**
     * 品牌型号: BRAND_MODEL
     */
    public final java.lang.String getBrandModel(){
         onPropGet(PROP_ID_brandModel);
         return _brandModel;
    }

    /**
     * 品牌型号: BRAND_MODEL
     */
    public final void setBrandModel(java.lang.String value){
        if(onPropSet(PROP_ID_brandModel,value)){
            this._brandModel = value;
            internalClearRefs(PROP_ID_brandModel);
            
        }
    }
    
    /**
     * 资产状态: STATUS
     */
    public final java.lang.Integer getStatus(){
         onPropGet(PROP_ID_status);
         return _status;
    }

    /**
     * 资产状态: STATUS
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
     * 累计折旧: ACCUMULATED_DEPRECIATION
     */
    public final java.math.BigDecimal getAccumulatedDepreciation(){
         onPropGet(PROP_ID_accumulatedDepreciation);
         return _accumulatedDepreciation;
    }

    /**
     * 累计折旧: ACCUMULATED_DEPRECIATION
     */
    public final void setAccumulatedDepreciation(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_accumulatedDepreciation,value)){
            this._accumulatedDepreciation = value;
            internalClearRefs(PROP_ID_accumulatedDepreciation);
            
        }
    }
    
    /**
     * 净值: NET_BOOK_VALUE
     */
    public final java.math.BigDecimal getNetBookValue(){
         onPropGet(PROP_ID_netBookValue);
         return _netBookValue;
    }

    /**
     * 净值: NET_BOOK_VALUE
     */
    public final void setNetBookValue(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_netBookValue,value)){
            this._netBookValue = value;
            internalClearRefs(PROP_ID_netBookValue);
            
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
    public final app.erp.md.dao.entity.ErpMdOrganization getDepartment(){
       return (app.erp.md.dao.entity.ErpMdOrganization)internalGetRefEntity(PROP_NAME_department);
    }

    public final void setDepartment(app.erp.md.dao.entity.ErpMdOrganization refEntity){
   
           if(refEntity == null){
           
                   this.setDepartmentId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_department, refEntity,()->{
           
                           this.setDepartmentId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.md.dao.entity.ErpMdLocation getLocation(){
       return (app.erp.md.dao.entity.ErpMdLocation)internalGetRefEntity(PROP_NAME_location);
    }

    public final void setLocation(app.erp.md.dao.entity.ErpMdLocation refEntity){
   
           if(refEntity == null){
           
                   this.setLocationId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_location, refEntity,()->{
           
                           this.setLocationId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.md.dao.entity.ErpMdEmployee getEmployee(){
       return (app.erp.md.dao.entity.ErpMdEmployee)internalGetRefEntity(PROP_NAME_employee);
    }

    public final void setEmployee(app.erp.md.dao.entity.ErpMdEmployee refEntity){
   
           if(refEntity == null){
           
                   this.setEmployeeId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_employee, refEntity,()->{
           
                           this.setEmployeeId(refEntity.getId());
                       
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
    public final app.erp.md.dao.entity.ErpMdEmployee getStaff(){
       return (app.erp.md.dao.entity.ErpMdEmployee)internalGetRefEntity(PROP_NAME_staff);
    }

    public final void setStaff(app.erp.md.dao.entity.ErpMdEmployee refEntity){
   
           if(refEntity == null){
           
                   this.setStaffId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_staff, refEntity,()->{
           
                           this.setStaffId(refEntity.getId());
                       
           });
           }
       
    }
       
}
// resume CPD analysis - CPD-ON

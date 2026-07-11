//__XGEN_FORCE_OVERRIDE__
    package app.erp.ast.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpAstCipInputBean extends CrudInputBase {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private String _code;

    
        @PropMeta(propId=2)
    
        public String getCode(){
            return _code;
        }

        public void setCode(String value){
            this._code = value;
        }


        private String _name;

    
        @PropMeta(propId=3)
    
        public String getName(){
            return _name;
        }

        public void setName(String value){
            this._name = value;
        }


        private Long _orgId;

    
        @PropMeta(propId=4)
    
        public Long getOrgId(){
            return _orgId;
        }

        public void setOrgId(Long value){
            this._orgId = value;
        }


        private Long _categoryId;

    
        @PropMeta(propId=5)
    
        public Long getCategoryId(){
            return _categoryId;
        }

        public void setCategoryId(Long value){
            this._categoryId = value;
        }


        private Long _currencyId;

    
        @PropMeta(propId=6)
    
        public Long getCurrencyId(){
            return _currencyId;
        }

        public void setCurrencyId(Long value){
            this._currencyId = value;
        }


        private java.time.LocalDate _businessDate;

    
        @PropMeta(propId=7)
    
        public java.time.LocalDate getBusinessDate(){
            return _businessDate;
        }

        public void setBusinessDate(java.time.LocalDate value){
            this._businessDate = value;
        }


        private java.time.LocalDate _estimatedCompletionDate;

    
        @PropMeta(propId=8)
    
        public java.time.LocalDate getEstimatedCompletionDate(){
            return _estimatedCompletionDate;
        }

        public void setEstimatedCompletionDate(java.time.LocalDate value){
            this._estimatedCompletionDate = value;
        }


        private java.math.BigDecimal _accumulatedCost;

    
        @PropMeta(propId=9)
    
        public java.math.BigDecimal getAccumulatedCost(){
            return _accumulatedCost;
        }

        public void setAccumulatedCost(java.math.BigDecimal value){
            this._accumulatedCost = value;
        }


        private Boolean _isCompleted;

    
        @PropMeta(propId=10)
    
        public Boolean getIsCompleted(){
            return _isCompleted;
        }

        public void setIsCompleted(Boolean value){
            this._isCompleted = value;
        }


        private Long _completedAssetId;

    
        @PropMeta(propId=11)
    
        public Long getCompletedAssetId(){
            return _completedAssetId;
        }

        public void setCompletedAssetId(Long value){
            this._completedAssetId = value;
        }


        private String _status;

    
        @PropMeta(propId=12)
    
        public String getStatus(){
            return _status;
        }

        public void setStatus(String value){
            this._status = value;
        }


        private String _remark;

    
        @PropMeta(propId=19)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private Long _projectId;

    
        @PropMeta(propId=20)
    
        public Long getProjectId(){
            return _projectId;
        }

        public void setProjectId(Long value){
            this._projectId = value;
        }


        private String _cipAssetCategorySnapshot;

    
        @PropMeta(propId=21)
    
        public String getCipAssetCategorySnapshot(){
            return _cipAssetCategorySnapshot;
        }

        public void setCipAssetCategorySnapshot(String value){
            this._cipAssetCategorySnapshot = value;
        }


        private java.math.BigDecimal _exchangeRate;

    
        @PropMeta(propId=25)
    
        public java.math.BigDecimal getExchangeRate(){
            return _exchangeRate;
        }

        public void setExchangeRate(java.math.BigDecimal value){
            this._exchangeRate = value;
        }


        private java.math.BigDecimal _amountSource;

    
        @PropMeta(propId=26)
    
        public java.math.BigDecimal getAmountSource(){
            return _amountSource;
        }

        public void setAmountSource(java.math.BigDecimal value){
            this._amountSource = value;
        }


        private java.math.BigDecimal _amountFunctional;

    
        @PropMeta(propId=27)
    
        public java.math.BigDecimal getAmountFunctional(){
            return _amountFunctional;
        }

        public void setAmountFunctional(java.math.BigDecimal value){
            this._amountFunctional = value;
        }


    }

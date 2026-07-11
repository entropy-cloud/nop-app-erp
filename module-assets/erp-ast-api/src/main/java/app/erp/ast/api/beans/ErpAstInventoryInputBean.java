//__XGEN_FORCE_OVERRIDE__
    package app.erp.ast.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    import java.util.List;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpAstInventoryInputBean extends CrudInputBase {

    
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


        private String _status;

    
        @PropMeta(propId=5)
    
        public String getStatus(){
            return _status;
        }

        public void setStatus(String value){
            this._status = value;
        }


        private Long _rangeDepartmentId;

    
        @PropMeta(propId=6)
    
        public Long getRangeDepartmentId(){
            return _rangeDepartmentId;
        }

        public void setRangeDepartmentId(Long value){
            this._rangeDepartmentId = value;
        }


        private Long _rangeCategoryId;

    
        @PropMeta(propId=7)
    
        public Long getRangeCategoryId(){
            return _rangeCategoryId;
        }

        public void setRangeCategoryId(Long value){
            this._rangeCategoryId = value;
        }


        private Long _rangeLocationId;

    
        @PropMeta(propId=8)
    
        public Long getRangeLocationId(){
            return _rangeLocationId;
        }

        public void setRangeLocationId(Long value){
            this._rangeLocationId = value;
        }


        private Long _responsibleById;

    
        @PropMeta(propId=9)
    
        public Long getResponsibleById(){
            return _responsibleById;
        }

        public void setResponsibleById(Long value){
            this._responsibleById = value;
        }


        private java.time.LocalDate _businessDate;

    
        @PropMeta(propId=10)
    
        public java.time.LocalDate getBusinessDate(){
            return _businessDate;
        }

        public void setBusinessDate(java.time.LocalDate value){
            this._businessDate = value;
        }


        private Long _currencyId;

    
        @PropMeta(propId=11)
    
        public Long getCurrencyId(){
            return _currencyId;
        }

        public void setCurrencyId(Long value){
            this._currencyId = value;
        }


        private java.math.BigDecimal _exchangeRate;

    
        @PropMeta(propId=12)
    
        public java.math.BigDecimal getExchangeRate(){
            return _exchangeRate;
        }

        public void setExchangeRate(java.math.BigDecimal value){
            this._exchangeRate = value;
        }


        private Integer _surplusCount;

    
        @PropMeta(propId=13)
    
        public Integer getSurplusCount(){
            return _surplusCount;
        }

        public void setSurplusCount(Integer value){
            this._surplusCount = value;
        }


        private Integer _shortageCount;

    
        @PropMeta(propId=14)
    
        public Integer getShortageCount(){
            return _shortageCount;
        }

        public void setShortageCount(Integer value){
            this._shortageCount = value;
        }


        private Integer _matchedCount;

    
        @PropMeta(propId=15)
    
        public Integer getMatchedCount(){
            return _matchedCount;
        }

        public void setMatchedCount(Integer value){
            this._matchedCount = value;
        }


        private java.math.BigDecimal _surplusAmount;

    
        @PropMeta(propId=16)
    
        public java.math.BigDecimal getSurplusAmount(){
            return _surplusAmount;
        }

        public void setSurplusAmount(java.math.BigDecimal value){
            this._surplusAmount = value;
        }


        private java.math.BigDecimal _shortageAmount;

    
        @PropMeta(propId=17)
    
        public java.math.BigDecimal getShortageAmount(){
            return _shortageAmount;
        }

        public void setShortageAmount(java.math.BigDecimal value){
            this._shortageAmount = value;
        }


        private java.math.BigDecimal _amountSource;

    
        @PropMeta(propId=23)
    
        public java.math.BigDecimal getAmountSource(){
            return _amountSource;
        }

        public void setAmountSource(java.math.BigDecimal value){
            this._amountSource = value;
        }


        private java.math.BigDecimal _amountFunctional;

    
        @PropMeta(propId=24)
    
        public java.math.BigDecimal getAmountFunctional(){
            return _amountFunctional;
        }

        public void setAmountFunctional(java.math.BigDecimal value){
            this._amountFunctional = value;
        }


        private String _remark;

    
        @PropMeta(propId=31)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private List<ErpAstInventoryLineInputBean> _lines;

        public List<ErpAstInventoryLineInputBean> getLines(){
            return _lines;
        }

        public void setLines(List<ErpAstInventoryLineInputBean> value){
            this._lines = value;
        }


    }

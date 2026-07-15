//__XGEN_FORCE_OVERRIDE__
    package app.erp.fin.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpFinNotesDiscountOutputBean {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _notesReceivableId;

    
        @PropMeta(propId=2)
    
        public Long getNotesReceivableId(){
            return _notesReceivableId;
        }

        public void setNotesReceivableId(Long value){
            this._notesReceivableId = value;
        }


        private Long _orgId;

    
        @PropMeta(propId=3)
    
        public Long getOrgId(){
            return _orgId;
        }

        public void setOrgId(Long value){
            this._orgId = value;
        }


        private java.time.LocalDate _discountDate;

    
        @PropMeta(propId=4)
    
        public java.time.LocalDate getDiscountDate(){
            return _discountDate;
        }

        public void setDiscountDate(java.time.LocalDate value){
            this._discountDate = value;
        }


        private Long _bankId;

    
        @PropMeta(propId=5)
    
        public Long getBankId(){
            return _bankId;
        }

        public void setBankId(Long value){
            this._bankId = value;
        }


        private java.math.BigDecimal _faceAmount;

    
        @PropMeta(propId=6)
    
        public java.math.BigDecimal getFaceAmount(){
            return _faceAmount;
        }

        public void setFaceAmount(java.math.BigDecimal value){
            this._faceAmount = value;
        }


        private java.math.BigDecimal _discountInterest;

    
        @PropMeta(propId=7)
    
        public java.math.BigDecimal getDiscountInterest(){
            return _discountInterest;
        }

        public void setDiscountInterest(java.math.BigDecimal value){
            this._discountInterest = value;
        }


        private java.math.BigDecimal _netAmount;

    
        @PropMeta(propId=8)
    
        public java.math.BigDecimal getNetAmount(){
            return _netAmount;
        }

        public void setNetAmount(java.math.BigDecimal value){
            this._netAmount = value;
        }


        private Long _currencyId;

    
        @PropMeta(propId=9)
    
        public Long getCurrencyId(){
            return _currencyId;
        }

        public void setCurrencyId(Long value){
            this._currencyId = value;
        }


        private java.math.BigDecimal _exchangeRate;

    
        @PropMeta(propId=10)
    
        public java.math.BigDecimal getExchangeRate(){
            return _exchangeRate;
        }

        public void setExchangeRate(java.math.BigDecimal value){
            this._exchangeRate = value;
        }


        private java.math.BigDecimal _exchangeGainLoss;

    
        @PropMeta(propId=11)
    
        public java.math.BigDecimal getExchangeGainLoss(){
            return _exchangeGainLoss;
        }

        public void setExchangeGainLoss(java.math.BigDecimal value){
            this._exchangeGainLoss = value;
        }


        private Boolean _posted;

    
        @PropMeta(propId=12)
    
        public Boolean getPosted(){
            return _posted;
        }

        public void setPosted(Boolean value){
            this._posted = value;
        }


        private String _postedBy;

    
        @PropMeta(propId=13)
    
        public String getPostedBy(){
            return _postedBy;
        }

        public void setPostedBy(String value){
            this._postedBy = value;
        }


        private java.sql.Timestamp _postedAt;

    
        @PropMeta(propId=14)
    
        public java.sql.Timestamp getPostedAt(){
            return _postedAt;
        }

        public void setPostedAt(java.sql.Timestamp value){
            this._postedAt = value;
        }


        private String _remark;

    
        @PropMeta(propId=15)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=16)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=17)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=18)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=19)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=20)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=21)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private Map<String,Object> _notesReceivable;

        public Map<String,Object> getNotesReceivable(){
            return _notesReceivable;
        }

        public void setNotesReceivable(Map<String,Object> value){
            this._notesReceivable = value;
        }


        private Map<String,Object> _bank;

        public Map<String,Object> getBank(){
            return _bank;
        }

        public void setBank(Map<String,Object> value){
            this._bank = value;
        }


        private Map<String,Object> _org;

        public Map<String,Object> getOrg(){
            return _org;
        }

        public void setOrg(Map<String,Object> value){
            this._org = value;
        }


    }

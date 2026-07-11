//__XGEN_FORCE_OVERRIDE__
    package app.erp.prj.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    import java.util.List;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpPrjProjectSettlementInputBean extends CrudInputBase {

    
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


        private Long _projectId;

    
        @PropMeta(propId=3)
    
        public Long getProjectId(){
            return _projectId;
        }

        public void setProjectId(Long value){
            this._projectId = value;
        }


        private Long _orgId;

    
        @PropMeta(propId=4)
    
        public Long getOrgId(){
            return _orgId;
        }

        public void setOrgId(Long value){
            this._orgId = value;
        }


        private Long _customerId;

    
        @PropMeta(propId=5)
    
        public Long getCustomerId(){
            return _customerId;
        }

        public void setCustomerId(Long value){
            this._customerId = value;
        }


        private java.time.LocalDate _businessDate;

    
        @PropMeta(propId=6)
    
        public java.time.LocalDate getBusinessDate(){
            return _businessDate;
        }

        public void setBusinessDate(java.time.LocalDate value){
            this._businessDate = value;
        }


        private String _settlementType;

    
        @PropMeta(propId=7)
    
        public String getSettlementType(){
            return _settlementType;
        }

        public void setSettlementType(String value){
            this._settlementType = value;
        }


        private Long _pnlSnapshotId;

    
        @PropMeta(propId=8)
    
        public Long getPnlSnapshotId(){
            return _pnlSnapshotId;
        }

        public void setPnlSnapshotId(Long value){
            this._pnlSnapshotId = value;
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


        private java.math.BigDecimal _amountSource;

    
        @PropMeta(propId=11)
    
        public java.math.BigDecimal getAmountSource(){
            return _amountSource;
        }

        public void setAmountSource(java.math.BigDecimal value){
            this._amountSource = value;
        }


        private java.math.BigDecimal _amountFunctional;

    
        @PropMeta(propId=12)
    
        public java.math.BigDecimal getAmountFunctional(){
            return _amountFunctional;
        }

        public void setAmountFunctional(java.math.BigDecimal value){
            this._amountFunctional = value;
        }


        private java.math.BigDecimal _finalRevenue;

    
        @PropMeta(propId=13)
    
        public java.math.BigDecimal getFinalRevenue(){
            return _finalRevenue;
        }

        public void setFinalRevenue(java.math.BigDecimal value){
            this._finalRevenue = value;
        }


        private java.math.BigDecimal _finalCost;

    
        @PropMeta(propId=14)
    
        public java.math.BigDecimal getFinalCost(){
            return _finalCost;
        }

        public void setFinalCost(java.math.BigDecimal value){
            this._finalCost = value;
        }


        private java.math.BigDecimal _finalProfit;

    
        @PropMeta(propId=15)
    
        public java.math.BigDecimal getFinalProfit(){
            return _finalProfit;
        }

        public void setFinalProfit(java.math.BigDecimal value){
            this._finalProfit = value;
        }


        private java.math.BigDecimal _retentionAmount;

    
        @PropMeta(propId=16)
    
        public java.math.BigDecimal getRetentionAmount(){
            return _retentionAmount;
        }

        public void setRetentionAmount(java.math.BigDecimal value){
            this._retentionAmount = value;
        }


        private java.time.LocalDate _retentionDueDate;

    
        @PropMeta(propId=17)
    
        public java.time.LocalDate getRetentionDueDate(){
            return _retentionDueDate;
        }

        public void setRetentionDueDate(java.time.LocalDate value){
            this._retentionDueDate = value;
        }


        private Boolean _transferToAsset;

    
        @PropMeta(propId=18)
    
        public Boolean getTransferToAsset(){
            return _transferToAsset;
        }

        public void setTransferToAsset(Boolean value){
            this._transferToAsset = value;
        }


        private Long _assetCardId;

    
        @PropMeta(propId=19)
    
        public Long getAssetCardId(){
            return _assetCardId;
        }

        public void setAssetCardId(Long value){
            this._assetCardId = value;
        }


        private String _settlementVoucherCode;

    
        @PropMeta(propId=20)
    
        public String getSettlementVoucherCode(){
            return _settlementVoucherCode;
        }

        public void setSettlementVoucherCode(String value){
            this._settlementVoucherCode = value;
        }


        private String _docStatus;

    
        @PropMeta(propId=21)
    
        public String getDocStatus(){
            return _docStatus;
        }

        public void setDocStatus(String value){
            this._docStatus = value;
        }


        private String _approveStatus;

    
        @PropMeta(propId=22)
    
        public String getApproveStatus(){
            return _approveStatus;
        }

        public void setApproveStatus(String value){
            this._approveStatus = value;
        }


        private String _remark;

    
        @PropMeta(propId=26)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private List<ErpPrjProjectSettlementLineInputBean> _lines;

        public List<ErpPrjProjectSettlementLineInputBean> getLines(){
            return _lines;
        }

        public void setLines(List<ErpPrjProjectSettlementLineInputBean> value){
            this._lines = value;
        }


    }

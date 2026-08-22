//__XGEN_FORCE_OVERRIDE__
    package app.erp.mfg.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpMfgBatchGenealogyOutputBean {

    
        private String _id;

    
        @PropMeta(propId=1)
    
        public String getId(){
            return _id;
        }

        public void setId(String value){
            this._id = value;
        }


        private String _workOrderId;

    
        @PropMeta(propId=2)
    
        public String getWorkOrderId(){
            return _workOrderId;
        }

        public void setWorkOrderId(String value){
            this._workOrderId = value;
        }


        private String _jobCardId;

    
        @PropMeta(propId=3)
    
        public String getJobCardId(){
            return _jobCardId;
        }

        public void setJobCardId(String value){
            this._jobCardId = value;
        }


        private String _operationId;

    
        @PropMeta(propId=4)
    
        public String getOperationId(){
            return _operationId;
        }

        public void setOperationId(String value){
            this._operationId = value;
        }


        private String _inputLotId;

    
        @PropMeta(propId=5)
    
        public String getInputLotId(){
            return _inputLotId;
        }

        public void setInputLotId(String value){
            this._inputLotId = value;
        }


        private String _inputMaterialId;

    
        @PropMeta(propId=6)
    
        public String getInputMaterialId(){
            return _inputMaterialId;
        }

        public void setInputMaterialId(String value){
            this._inputMaterialId = value;
        }


        private java.math.BigDecimal _inputQty;

    
        @PropMeta(propId=7)
    
        public java.math.BigDecimal getInputQty(){
            return _inputQty;
        }

        public void setInputQty(java.math.BigDecimal value){
            this._inputQty = value;
        }


        private String _inputUoMId;

    
        @PropMeta(propId=8)
    
        public String getInputUoMId(){
            return _inputUoMId;
        }

        public void setInputUoMId(String value){
            this._inputUoMId = value;
        }


        private String _outputLotId;

    
        @PropMeta(propId=9)
    
        public String getOutputLotId(){
            return _outputLotId;
        }

        public void setOutputLotId(String value){
            this._outputLotId = value;
        }


        private String _outputMaterialId;

    
        @PropMeta(propId=10)
    
        public String getOutputMaterialId(){
            return _outputMaterialId;
        }

        public void setOutputMaterialId(String value){
            this._outputMaterialId = value;
        }


        private java.math.BigDecimal _outputQty;

    
        @PropMeta(propId=11)
    
        public java.math.BigDecimal getOutputQty(){
            return _outputQty;
        }

        public void setOutputQty(java.math.BigDecimal value){
            this._outputQty = value;
        }


        private String _outputUoMId;

    
        @PropMeta(propId=12)
    
        public String getOutputUoMId(){
            return _outputUoMId;
        }

        public void setOutputUoMId(String value){
            this._outputUoMId = value;
        }


        private java.time.LocalDate _productionDate;

    
        @PropMeta(propId=13)
    
        public java.time.LocalDate getProductionDate(){
            return _productionDate;
        }

        public void setProductionDate(java.time.LocalDate value){
            this._productionDate = value;
        }


        private java.sql.Timestamp _productionTime;

    
        @PropMeta(propId=14)
    
        public java.sql.Timestamp getProductionTime(){
            return _productionTime;
        }

        public void setProductionTime(java.sql.Timestamp value){
            this._productionTime = value;
        }


        private Integer _lineNo;

    
        @PropMeta(propId=15)
    
        public Integer getLineNo(){
            return _lineNo;
        }

        public void setLineNo(Integer value){
            this._lineNo = value;
        }


        private String _lotStatus;

    
        @PropMeta(propId=16)
    
        public String getLotStatus(){
            return _lotStatus;
        }

        public void setLotStatus(String value){
            this._lotStatus = value;
        }


        private Boolean _isInputConsumed;

    
        @PropMeta(propId=17)
    
        public Boolean getIsInputConsumed(){
            return _isInputConsumed;
        }

        public void setIsInputConsumed(Boolean value){
            this._isInputConsumed = value;
        }


        private String _remark;

    
        @PropMeta(propId=18)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=19)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=20)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=21)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=22)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=23)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=24)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private Map<String,Object> _workOrder;

        public Map<String,Object> getWorkOrder(){
            return _workOrder;
        }

        public void setWorkOrder(Map<String,Object> value){
            this._workOrder = value;
        }


        private Map<String,Object> _jobCard;

        public Map<String,Object> getJobCard(){
            return _jobCard;
        }

        public void setJobCard(Map<String,Object> value){
            this._jobCard = value;
        }


        private Map<String,Object> _inputMaterial;

        public Map<String,Object> getInputMaterial(){
            return _inputMaterial;
        }

        public void setInputMaterial(Map<String,Object> value){
            this._inputMaterial = value;
        }


        private Map<String,Object> _outputMaterial;

        public Map<String,Object> getOutputMaterial(){
            return _outputMaterial;
        }

        public void setOutputMaterial(Map<String,Object> value){
            this._outputMaterial = value;
        }


        private Map<String,Object> _operation;

        public Map<String,Object> getOperation(){
            return _operation;
        }

        public void setOperation(Map<String,Object> value){
            this._operation = value;
        }


        private Map<String,Object> _inputLot;

        public Map<String,Object> getInputLot(){
            return _inputLot;
        }

        public void setInputLot(Map<String,Object> value){
            this._inputLot = value;
        }


        private Map<String,Object> _inputUoM;

        public Map<String,Object> getInputUoM(){
            return _inputUoM;
        }

        public void setInputUoM(Map<String,Object> value){
            this._inputUoM = value;
        }


        private Map<String,Object> _outputLot;

        public Map<String,Object> getOutputLot(){
            return _outputLot;
        }

        public void setOutputLot(Map<String,Object> value){
            this._outputLot = value;
        }


        private Map<String,Object> _outputUoM;

        public Map<String,Object> getOutputUoM(){
            return _outputUoM;
        }

        public void setOutputUoM(Map<String,Object> value){
            this._outputUoM = value;
        }


    }

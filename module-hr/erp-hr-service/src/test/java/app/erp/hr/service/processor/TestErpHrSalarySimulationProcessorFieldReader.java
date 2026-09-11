package app.erp.hr.service.processor;

import app.erp.hr.dao.entity.ErpHrSalary;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P1-CK-hr2-005 回归（Processor 侧副本）：AbstractErpHrSalarySimulationProcessor#readSalaryField
 * 同患派生字段 default-ZERO 缺陷（adjustItem/convertToFormal/applyBatchAdjustment 消费面），
 * 与 BizModel 副本同步修（plan 2026-09-11-1530-1 Phase 2 单源收敛）。
 */
public class TestErpHrSalarySimulationProcessorFieldReader {

    /** 与 ErpHrSalarySimulationBizModel.SALARY_ITEM_CODES 13 项保持一致（跨包不可见故内联）。 */
    private static final String[] ITEM_CODES = {
            "basicSalary", "positionAllowance", "performanceBonus", "overtimePay",
            "mealAllowance", "transportAllowance", "otherAllowance",
            "grossSalary", "socialInsurance", "housingFund", "taxAmount",
            "otherDeductions", "netSalary"};

    private ErpHrSalary newFullyPopulatedSalary() {
        ErpHrSalary salary = new ErpHrSalary();
        salary.setBasicSalary(new BigDecimal("100.01"));
        salary.setPositionAllowance(new BigDecimal("200.02"));
        salary.setPerformanceBonus(new BigDecimal("300.03"));
        salary.setOvertimePay(new BigDecimal("400.04"));
        salary.setMealAllowance(new BigDecimal("500.05"));
        salary.setTransportAllowance(new BigDecimal("600.06"));
        salary.setOtherAllowance(new BigDecimal("700.07"));
        salary.setGrossSalary(new BigDecimal("2800.28"));
        salary.setSocialInsurance(new BigDecimal("900.09"));
        salary.setHousingFund(new BigDecimal("800.08"));
        salary.setTaxAmount(new BigDecimal("70.07"));
        salary.setOtherDeductions(new BigDecimal("60.06"));
        salary.setNetSalary(new BigDecimal("970.97"));
        return salary;
    }

    @Test
    public void testProcessorReadSalaryFieldCoversDerivedItems() {
        ErpHrSalarySimulationAdjustItemProcessor processor = new ErpHrSalarySimulationAdjustItemProcessor();
        ErpHrSalary salary = newFullyPopulatedSalary();
        for (String itemCode : ITEM_CODES) {
            BigDecimal value = processor.readSalaryField(salary, itemCode);
            assertTrue(value.signum() != 0,
                    "itemCode " + itemCode + " 读取值不应恒为 0（Processor 副本派生字段缺失）");
        }
        assertEquals(new BigDecimal("970.97"), processor.readSalaryField(salary, "netSalary"));
    }
}

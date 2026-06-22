#!/bin/sh
# 通过 nop-cli 从 model/app-erp.orm.xlsx 生成多模块工程。
# 需要先用 nop-cli convert 将 model/app-erp.orm.xml 转换为 xlsx，或直接维护 xlsx。
# 依赖：nop-cli.jar 需置于项目根目录或 PATH 中。
java -jar nop-cli.jar gen -t=/nop/templates/orm model/app-erp.orm.xlsx

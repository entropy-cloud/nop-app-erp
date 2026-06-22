#!/bin/sh
# 从已有数据库反向生成 ORM 模型（可选，用于已有数据库的逆向工程）
# 用法：修改下面的数据库连接参数后执行
java -Dfile.encoding=UTF8 -jar nop-cli.jar reverse-db app-erp \
  -c=com.mysql.cj.jdbc.Driver \
  --username=erp \
  --password=erp123456 \
  --jdbcUrl="jdbc:mysql://127.0.0.1:3306/app-erp?useUnicode=true&characterEncoding=utf-8&useSSL=true&serverTimezone=UTC"

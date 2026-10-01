#!/usr/bin/env bash
# USC-02a %prod action-auth 灰度探针（plan 2026-10-01-0313-1，双独立子代理批准）
#
# 以 %prod profile 启动 app-erp-all runner（独立端口 + 独立 DB），验证：
#   [guard] 启动日志 nop.config.vars 含 enable-action-auth=true（生效来源 = %prod 块，
#           根配置无该键，%dev=%test 块未被激活）
#   [①] role-restricted GraphQL FNPT 动作 → nop.err.auth.no-permission（enforcement 拒绝形状）
#   [②] role-restricted SiteMap：B 类 5 域 TOPM 隐藏 + notify 可见
#        （crm/cs/drp 零种子真 fail-closed；aps/log 对 restricted 亦无种子角色 → 隐藏）
#   [③] role-pur（采购员）SiteMap：erp-pur + erp-md TOPM + md-material SUBM 可见、erp-fin 隐藏
#        （USC-02a per-entity 种子落地效果）
#   [④] nop（平台 admin）SiteMap：erp-sys 可见（可见 ≠ 可用）+ erp-fin 隐藏（RBAC 姿态）
#   [⑤] role-pur ErpPurOrder__findPage → no-permission（「菜单壳」已验收部署语义：
#        %prod 全操作 enforcement + query/mutation FNPT 零种子 → 页面数据加载 403）
#
# 退出码：0 = 全部断言过；非零 = 对应断言失败（fail-loud，无静默跳过）。
# 前置：mvn clean install -DskipTests 产出新鲜 runner jar（lessons/26 stale jar 防线——
#        本脚本不构建，只校验 jar 存在；调用方负责先构建）。
set -euo pipefail

PORT=8081
BASE="http://localhost:${PORT}"
JAR="app-erp-all/target/app-erp-all-1.0-SNAPSHOT-runner.jar"
LOG="_tmp/prod-auth-probe-server.log"
DB="./db/erp-probe"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

cd "${REPO_ROOT}"
mkdir -p _tmp

if [ ! -f "${JAR}" ]; then
  echo "FAIL[precondition]: runner jar 不存在：${JAR}（先运行 mvn clean install -DskipTests）" >&2
  exit 2
fi

# ---- 启动 %prod server（独立端口 + 独立 DB）----
rm -f "${DB}.mv.db" "${DB}.trace.db"
java -Dquarkus.profile=prod \
     -Dquarkus.http.port="${PORT}" \
     -Dnop.datasource.jdbc-url="jdbc:h2:${DB}" \
     -Dnop.orm.init-database-data=true \
     -Dfile.encoding=UTF8 \
     -jar "${JAR}" > "${LOG}" 2>&1 &
SERVER_PID=$!
cleanup() {
  kill "${SERVER_PID}" >/dev/null 2>&1 || true
  wait "${SERVER_PID}" 2>/dev/null || true
  rm -f "${DB}.mv.db" "${DB}.trace.db"
}
trap cleanup EXIT

echo "-- waiting for server on :${PORT} ..."
READY=0
for i in $(seq 1 120); do
  if curl -sf -o /dev/null "${BASE}/"; then READY=1; break; fi
  if ! kill -0 "${SERVER_PID}" 2>/dev/null; then
    echo "FAIL[startup]: server 进程退出，日志尾部：" >&2
    tail -40 "${LOG}" >&2
    exit 2
  fi
  sleep 2
done
if [ "${READY}" != "1" ]; then
  echo "FAIL[startup]: 240s 内未就绪，日志尾部：" >&2
  tail -40 "${LOG}" >&2
  exit 2
fi
echo "-- server ready (pid ${SERVER_PID})"

# ---- [guard] 启动日志 config 生效值 ----
if grep -q 'enable-action-auth=true' "${LOG}"; then
  echo "PASS[guard]: nop.config.vars 实测 enable-action-auth=true（%prod 块生效——根配置无该键，%dev/%test 未激活）"
else
  echo "FAIL[guard]: 启动日志未见 enable-action-auth=true（%prod 块未生效？），日志片段：" >&2
  grep -n "enable-action-auth" "${LOG}" | head -5 >&2 || echo "  (日志无 enable-action-auth 字样)" >&2
  exit 3
fi

# ---- helpers ----
login_token() { # $1 = username
  curl -sf -X POST "${BASE}/r/LoginApi__login" \
    -H 'Content-Type: application/json' \
    -d "{\"loginType\":1,\"principalId\":\"$1\",\"principalSecret\":\"123\"}" \
  | python3 -c 'import json,sys; d=json.load(sys.stdin); t=(d.get("data") or {}).get("accessToken"); print(t or "")'
}

graphql() { # $1 = token, $2 = query
  curl -sf -X POST "${BASE}/graphql" \
    -H 'Content-Type: application/json' \
    -H "Authorization: Bearer $1" \
    -d "{\"query\":$(python3 -c 'import json,sys; print(json.dumps(sys.argv[1]))' "$2")}"
}

sitemap_ids() { # $1 = token -> flattened resource ids, one per line
  curl -sf -X POST "${BASE}/r/SiteMapApi__getSiteMap" \
    -H 'Content-Type: application/json' \
    -H "Authorization: Bearer $1" \
    -d '{}' \
  | python3 -c '
import json,sys
d=json.load(sys.stdin)
data=d.get("data", d)
ids=set()
def walk(n):
    if not n: return
    if isinstance(n, dict):
        if n.get("id"): ids.add(n["id"])
        for k in ("children","items"):
            for c in (n.get(k) or []): walk(c)
    elif isinstance(n, list):
        for c in n: walk(c)
walk(data)
print("\n".join(sorted(ids)))'
}

extract_error_code() { # $1 = json body -> stdout = nop-error-code（顶层 extensions 优先，逐错误 extensions 兜底）
  python3 - "$1" <<'PY'
import json, sys
d = json.loads(sys.argv[1])
top = (d.get("extensions") or {}).get("nop-error-code")
if top:
    print(top)
else:
    for e in (d.get("errors") or []):
        c = (e.get("extensions") or {}).get("nop-error-code")
        if c:
            print(c)
            break
PY
}

expect_error_code() { # $1 = token, $2 = query, $3 = expected code, $4 = label
  local json code
  json=$(graphql "$1" "$2")
  code=$(extract_error_code "${json}")
  if [ "${code}" = "$3" ]; then
    echo "PASS[$4]: nop-error-code = ${code}"
  else
    echo "FAIL[$4]: 期望 ${3}，实得「${code:-无错误}」，响应：${json:0:400}" >&2
    exit 10
  fi
}

expect_sitemap() { # $1 = token, $2 = label, $3.. = "id:present|absent" 断言对
  local token="$1" label="$2"; shift 2
  local ids
  ids=$(sitemap_ids "${token}")
  local spec id want
  for spec in "$@"; do
    id="${spec%%:*}"
    want="${spec##*:}"
    if [ "${want}" = "present" ]; then
      if printf '%s\n' "${ids}" | grep -Fxq "${id}"; then
        echo "PASS[${label}]: ${id} 可见"
      else
        echo "FAIL[${label}]: ${id} 应可见而缺席" >&2
        exit 11
      fi
    else
      if printf '%s\n' "${ids}" | grep -Fxq "${id}"; then
        echo "FAIL[${label}]: ${id} 应隐藏而可见" >&2
        exit 11
      else
        echo "PASS[${label}]: ${id} 隐藏"
      fi
    fi
  done
}

expect_sitemap_contains() { # $1 = token, $2 = label, $3 = substring, $4 = present|absent
  local token="$1" label="$2" sub="$3" want="$4"
  local hits
  hits=$(sitemap_ids "${token}" | grep -c "${sub}" || true)
  if [ "${want}" = "present" ]; then
    if [ "${hits}" -gt 0 ]; then echo "PASS[${label}]: 含 ${sub} 的资源可见（${hits} 个）"
    else echo "FAIL[${label}]: 应存在含 ${sub} 的资源" >&2; exit 12; fi
  else
    if [ "${hits}" -eq 0 ]; then echo "PASS[${label}]: 无含 ${sub} 的资源"
    else echo "FAIL[${label}]: 不应存在含 ${sub} 的资源（${hits} 个）" >&2; exit 12; fi
  fi
}

# ---- [①] restricted FNPT 动作拒绝 ----
TOK_RESTRICTED=$(login_token "role-restricted")
[ -n "${TOK_RESTRICTED}" ] || { echo "FAIL[①]: role-restricted 登录失败" >&2; exit 20; }
expect_error_code "${TOK_RESTRICTED}" \
  'mutation{ ErpFinBadDebt__writeOff(arApItemId:999999, reason:"prod-auth-probe"){ id } }' \
  'nop.err.auth.no-permission' "①"

# ---- [②] restricted SiteMap：B 类隐藏 + notify 可见 ----
expect_sitemap "${TOK_RESTRICTED}" "②" \
  "erp-crm:absent" "erp-cs:absent" "erp-aps:absent" "erp-log:absent" "erp-drp:absent"
expect_sitemap_contains "${TOK_RESTRICTED}" "②" "notify" "present"

# ---- [③] 采购员 SiteMap：本域 + md 种子可见、fin 隐藏 ----
TOK_PUR=$(login_token "role-pur")
[ -n "${TOK_PUR}" ] || { echo "FAIL[③]: role-pur 登录失败" >&2; exit 20; }
expect_sitemap "${TOK_PUR}" "③" \
  "erp-pur:present" "erp-md:present" "md-material:present" "erp-fin:absent"

# ---- [④] 平台 admin SiteMap：erp-sys 可见（可见≠可用）、fin 隐藏 ----
TOK_NOP=$(login_token "nop")
[ -n "${TOK_NOP}" ] || { echo "FAIL[④]: nop 登录失败" >&2; exit 20; }
expect_sitemap "${TOK_NOP}" "④" "erp-sys:present" "erp-fin:absent"

# ---- [⑤] 「菜单壳」已验收语义：采购员 findPage 被拒 ----
expect_error_code "${TOK_PUR}" \
  '{ ErpPurOrder__findPage(query:{offset:0,limit:1}){ total } }' \
  'nop.err.auth.no-permission' "⑤"

# ---- [⑥] data-auth 断言组（USC-02b：窄种子豁免面 + 行过滤活体 + 菜单壳非豁免面维持）----
TOK_INSPECTOR=$(login_token "role-inspector")
[ -n "${TOK_INSPECTOR}" ] || { echo "FAIL[⑥]: role-inspector 登录失败" >&2; exit 20; }
# ⑥ qa 正向活体：质检员保存检验单（inspectorId=21 载荷自见 → checkDataAuth 过检）
QA_SAVE=$(graphql "${TOK_INSPECTOR}" 'mutation{ ErpQaInspection__save(data:{code:"PROBE-QA-001",inspectionType:"INCOMING",materialId:"1",businessDate:"2026-08-11",inspectionDate:"2026-08-11",inspectorId:"21",result:"PENDING",docStatus:"DRAFT",approveStatus:"UNSUBMITTED"}){ id code } }')
QA_OK=$(printf '%s' "${QA_SAVE}" | python3 -c 'import json,sys; d=json.load(sys.stdin); print("yes" if (d.get("data") or {}).get("ErpQaInspection__save") else "no")')
if [ "${QA_OK}" = "yes" ]; then
  echo "PASS[⑥]: 质检员 ErpQaInspection__save 成功（:save 窄种子 + inspectorId 载荷自见过检）"
else
  echo "FAIL[⑥]: 质检员保存检验单失败：${QA_SAVE:0:300}" >&2
  exit 13
fi
QA_ID=$(printf '%s' "${QA_SAVE}" | python3 -c 'import json,sys; d=json.load(sys.stdin); print((d.get("data") or {}).get("ErpQaInspection__save",{}).get("id",""))')
[ -n "${QA_ID}" ] || { echo "FAIL[⑥]: save 未返回 id" >&2; exit 13; }
QA_GET=$(graphql "${TOK_INSPECTOR}" "{ ErpQaInspection__get(id: \"${QA_ID}\"){ id code inspectorId } }")
QA_VISIBLE=$(printf '%s' "${QA_GET}" | python3 -c 'import json,sys; d=json.load(sys.stdin); g=(d.get("data") or {}).get("ErpQaInspection__get"); print("yes" if g and g.get("code")=="PROBE-QA-001" else "no")')
if [ "${QA_VISIBLE}" = "yes" ]; then
  echo "PASS[⑥]: 质检员 __get 自建行可见（行过滤 + query 种子协同正例，inspectorId 自见）"
else
  echo "FAIL[⑥]: 自建行不可见或载荷不符：${QA_GET:0:300}" >&2
  exit 13
fi
# ⑥' sal 负向：销售员 findPage 成功但全部种子行（createdBy≠12）不可见
TOK_SAL=$(login_token "role-sal")
[ -n "${TOK_SAL}" ] || { echo "FAIL[⑥']: role-sal 登录失败" >&2; exit 20; }
SAL_FIND=$(graphql "${TOK_SAL}" '{ ErpSalOrder__findPage(query:{offset:0,limit:5}){ total } }')
SAL_TOTAL=$(printf '%s' "${SAL_FIND}" | python3 -c 'import json,sys; d=json.load(sys.stdin); print((d.get("data") or {}).get("ErpSalOrder__findPage",{}).get("total","ERR"))')
if [ "${SAL_TOTAL}" = "0" ]; then
  echo "PASS[⑥']: 销售员 findPage 成功且全部种子行不可见（createdBy 行过滤，total=0）"
else
  echo "FAIL[⑥']: 期望 total=0，实得 ${SAL_TOTAL}，响应：${SAL_FIND:0:300}" >&2
  exit 13
fi
# ⑥'' 菜单壳非豁免面维持 + md 种子可读
expect_error_code "${TOK_PUR}"   '{ ErpPurOrder__findPage(query:{offset:0,limit:1}){ total } }'   'nop.err.auth.no-permission' "⑥''pur"
TOK_FIN=$(login_token "role-finance")
[ -n "${TOK_FIN}" ] || { echo "FAIL[⑥'']: role-finance 登录失败" >&2; exit 20; }
CUR_FIND=$(graphql "${TOK_FIN}" '{ ErpMdCurrency__findPage(query:{offset:0,limit:1}){ total } }')
CUR_OK=$(printf '%s' "${CUR_FIND}" | python3 -c 'import json,sys; d=json.load(sys.stdin); print("yes" if d.get("data") and d.get("data").get("ErpMdCurrency__findPage") is not None else "no")')
if [ "${CUR_OK}" = "yes" ]; then
  echo "PASS[⑥'']: 财务员 ErpMdCurrency__findPage 可读（md :query 种子）"
else
  echo "FAIL[⑥'']: 财务员查询币种失败：${CUR_FIND:0:300}" >&2
  exit 13
fi

# ---- [⑦] 操作留痕读路径收口（USC-03：admin 可查询 oplog 与字段级变更史）----
TOK_NOP2=$(login_token "nop")
[ -n "${TOK_NOP2}" ] || { echo "FAIL[⑦]: nop 登录失败" >&2; exit 20; }
OPLOG_FIND=$(graphql "${TOK_NOP2}" '{ NopAuthOpLog__findPage(query:{offset:0,limit:1}){ total } }')
OPLOG_OK=$(printf '%s' "${OPLOG_FIND}" | python3 -c 'import json,sys; d=json.load(sys.stdin); print("yes" if d.get("data") and d.get("data").get("NopAuthOpLog__findPage") is not None else "no")')
if [ "${OPLOG_OK}" = "yes" ]; then
  echo "PASS[⑦]: admin NopAuthOpLog__findPage 可查（读路径收口活体）"
else
  echo "FAIL[⑦]: admin 查询操作日志失败：${OPLOG_FIND:0:300}" >&2
  exit 14
fi
CL_FIND=$(graphql "${TOK_NOP2}" '{ NopSysChangeLog__findPage(query:{offset:0,limit:1}){ total } }')
CL_OK=$(printf '%s' "${CL_FIND}" | python3 -c 'import json,sys; d=json.load(sys.stdin); print("yes" if d.get("data") and d.get("data").get("NopSysChangeLog__findPage") is not None else "no")')
if [ "${CL_OK}" = "yes" ]; then
  echo "PASS[⑦]: admin NopSysChangeLog__findPage 可查（字段级变更史关闭口径验收）"
else
  echo "FAIL[⑦]: admin 查询变更史失败：${CL_FIND:0:300}" >&2
  exit 14
fi

echo ""
echo "== 全部断言通过（guard + ①②③④⑤ + ⑥⑥'⑥'' + ⑦）：%prod action-auth + data-auth + 审计读路径灰度语义验证绿 =="

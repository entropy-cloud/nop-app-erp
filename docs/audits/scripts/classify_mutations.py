#!/usr/bin/env python3
"""
MQ Q1 Phase 4 — 存活变异体三分类 + Q4 盲区清单生成器。

解析 pitest mutations.xml，按设计文档 §4.3 三分类：
  (1) 生成代码噪声（_gen + api.beans + api.crud）—— 应为 0（excludedClasses 双控）
  (2) 等价变异候选（getter/setter/trivial 方法名启发式）
  (3) 真实测试盲区 —— 供 sibling plan Q4 消费（§8.2 格式）

用法: classify_mutations.py <domain> <mutations.xml> [--posting-keywords]

口径：
  - 「检测到」(killed) = KILLED + TIMED_OUT
  - 「存活」(survived) = SURVIVED + NO_COVERAGE + MEMORY_ERROR + RUN_ERROR
  - mutation score = killed / generated
"""
import re
import sys
import xml.etree.ElementTree as ET
from collections import Counter, defaultdict

# 过账 dispatcher/Processor 关键字（Q4 协同 join key，设计文档 §8.2）
POSTING_KEYWORDS = re.compile(
    r'(Posting|Dispatcher|Processor|Executor|Reverse|Approve|Close|Settle|Reconcile)',
    re.IGNORECASE,
)

# 生成代码噪声包模式（设计文档 §1.3 两类生成包）
GEN_NOISE = re.compile(r'(\.dao\.entity\._gen\.|\.api\.beans\.|\.api\.crud\.)')

# 等价变异启发式：方法名是 trivial accessor（设计文档 §4.3 类二）
TRIVIAL_METHOD = re.compile(
    r'^(get[A-Z]|set[A-Z]|is[A-Z]|has[A-Z]|toString|hashCode|equals|<init>|<clinit>|canEqual)',
    re.IGNORECASE,
)


def classify(xml_path, domain):
    root = ET.parse(xml_path).getroot()
    by_status = Counter()
    gen_noise = []        # (1) 生成噪声（应为 0）
    equivalent = []       # (2) 等价变异候选
    blindspots = []       # (3) 真实盲区
    all_surviving = []

    for mut in root.iter('mutation'):
        status = mut.get('status', 'UNKNOWN')
        by_status[status] += 1
        cls = mut.findtext('mutatedClass', '')
        method = mut.findtext('mutatedMethod', '')
        line = mut.findtext('lineNumber', '')
        mutator = mut.findtext('mutator', '')
        is_surviving = status in ('SURVIVED', 'NO_COVERAGE', 'MEMORY_ERROR', 'RUN_ERROR')
        if not is_surviving:
            continue
        all_surviving.append((cls, method, line, status, mutator))

        if GEN_NOISE.search(cls):
            gen_noise.append((cls, method, line, status))
        elif TRIVIAL_METHOD.match(method):
            equivalent.append((cls, method, line, status, mutator))
        else:
            blindspots.append((cls, method, line, status, mutator))

    generated = sum(by_status.values())
    killed = by_status.get('KILLED', 0) + by_status.get('TIMED_OUT', 0)
    score = round(killed * 100 / generated) if generated else 0

    # 真实盲区按类聚合（设计文档 §8.2）
    bs_by_class = defaultdict(list)
    for cls, method, line, status, mutator in blindspots:
        bs_by_class[cls].append((method, line, status, mutator))

    return {
        'domain': domain,
        'by_status': dict(by_status),
        'generated': generated,
        'killed': killed,
        'score': score,
        'gen_noise': gen_noise,
        'equivalent': equivalent,
        'blindspots': blindspots,
        'bs_by_class': bs_by_class,
    }


def render(result):
    d = result['domain']
    print(f"# {d} 存活变异体三分类（设计文档 §4.3）\n")
    print(f"## 计数概览\n")
    print(f"- generated（全部变异体）: {result['generated']}")
    st = result['by_status']
    print(f"- KILLED: {st.get('KILLED',0)} | TIMED_OUT: {st.get('TIMED_OUT',0)} | SURVIVED: {st.get('SURVIVED',0)} | NO_COVERAGE: {st.get('NO_COVERAGE',0)} | MEMORY_ERROR: {st.get('MEMORY_ERROR',0)} | RUN_ERROR: {st.get('RUN_ERROR',0)}")
    print(f"- mutation score = (KILLED+TIMED_OUT)/generated = {result['killed']}/{result['generated']} = **{result['score']}%**\n")
    print(f"## 三分类\n")
    print(f"### (1) 生成代码噪声（_gen + api.beans + api.crud）—— 应为 0")
    print(f"计数: **{len(result['gen_noise'])}**" + ("  ✅ 配置双控生效" if not result['gen_noise'] else "  ❌ 配置失效须修"))
    for cls, method, line, status in result['gen_noise'][:20]:
        print(f"  - {cls}.{method}:{line} [{status}]")
    print(f"\n### (2) 等价变异候选（getter/setter/trivial）")
    print(f"计数: {len(result['equivalent'])}")
    print(f"\n### (3) 真实测试盲区（供 Q4 消费）")
    print(f"计数: **{len(result['blindspots'])}**\n")
    print(f"## 真实盲区按类聚合（设计文档 §8.2 格式）\n")
    print(f"| 类 (FQCN) | 存活变异体数 | 是否过账 dispatcher/Processor | Q4 可消费性 |")
    print(f"|-----------|--------------|-------------------------------|-------------|")
    for cls in sorted(result['bs_by_class'], key=lambda c: -len(result['bs_by_class'][c])):
        n = len(result['bs_by_class'][cls])
        is_posting = bool(POSTING_KEYWORDS.search(cls))
        consume = "高（Q4 优先覆盖）" if is_posting else "中/低（后续 MR3-style）"
        print(f"| {cls} | {n} | {'是' if is_posting else '否'} | {consume} |")
    return result


def main():
    if len(sys.argv) < 3:
        print(__doc__)
        sys.exit(2)
    domain = sys.argv[1]
    xml_path = sys.argv[2]
    result = classify(xml_path, domain)
    render(result)


if __name__ == '__main__':
    main()

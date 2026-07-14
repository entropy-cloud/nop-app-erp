<!--
  nop-app-erp Development Journey v3
  Theme: Three Levels of Nested Control Loops
  Total Slides: 33
-->
<section>
  <h1>Loop 的嵌套<br/><span>nop-app-erp × Attractor-Guided Engineering</span></h1>
  <p class="subtitle">22 天 · 18+1 业务域 · 154 模块 · 3 层嵌套控制 Loop</p>
  <p class="author">基于 Nop Platform · AGE 工作流实践</p>
</section>

<section>
  <h2>一个让你不安的问题</h2>
  <div class="two-column">
    <div class="column">
      <h4>信念：AI 辅助开发一切顺利</h4>
      <ul>
        <li>07-12 竞争杠杆审计</li>
        <li>"承诺的功能/优势是否已完整兑现？"</li>
      </ul>
      <h4 style="margin-top:1rem">子代理审计结论：</h4>
      <ul class="icon-list">
        <li class="icon-bullet"><i class="fa-solid fa-circle-exclamation" style="color:var(--color-danger)"></i> "零 Delta 使用"</li>
        <li class="icon-bullet"><i class="fa-solid fa-circle-exclamation" style="color:var(--color-danger)"></i> "finance↔inventory 循环依赖"</li>
      </ul>
    </div>
    <div class="column">
      <h4>事实：Grep 验证</h4>
      <ul>
        <li><strong>338</strong> 个 view.xml 使用了 <code>x:extends</code>（Delta 机制）</li>
        <li>Reactor 构建<strong>无环</strong></li>
      </ul>
      <div class="highlight-box">
        子代理不是在撒谎。同一个上下文里生成代码、测试、检查报告——天然一致的确认偏差。
      </div>
    </div>
  </div>
  <p style="margin-top:1rem"><strong>核心问题：做完 100 个变更后，系统还在正确的结构上吗？</strong></p>
</section>

<section>
  <h2>22 天的输出——系统出产物</h2>
  <div class="stat-grid" style="grid-template-columns: repeat(4, 1fr);">
    <div class="stat-card"><div class="stat-number">18+1</div><div class="stat-label">业务域</div></div>
    <div class="stat-card"><div class="stat-number">154</div><div class="stat-label">Reactor 模块</div></div>
    <div class="stat-card"><div class="stat-number">352</div><div class="stat-label">自有实体</div></div>
    <div class="stat-card"><div class="stat-number">~2900</div><div class="stat-label">Java 测试 / 0 failure</div></div>
  </div>
  <div class="stat-grid" style="grid-template-columns: repeat(4, 1fr); margin-top:0.8rem;">
    <div class="stat-card"><div class="stat-number">260+</div><div class="stat-label">E2E Spec / 0 regression</div></div>
    <div class="stat-card"><div class="stat-number">189</div><div class="stat-label">Plan 文件</div></div>
    <div class="stat-card"><div class="stat-number">9</div><div class="stat-label">审计轮次</div></div>
    <div class="stat-card"><div class="stat-number">24+10</div><div class="stat-label">报表 + 看板</div></div>
  </div>
  <p style="margin-top:1rem; text-align:center">这些不是"22 天的奇迹"——是三层控制 Loop 的系统输出产物。</p>
</section>

<section>
  <h2>路线图</h2>
  <div class="cycle-diagram" style="grid-template-columns: repeat(6, 1fr);">
    <div class="cycle-step" style="background:var(--color-primary);color:white">Act 1<br/>Plan Loop</div>
    <div class="cycle-arrow"><i class="fa-solid fa-arrow-right"></i></div>
    <div class="cycle-step" style="background:var(--color-secondary);color:white">Act 2<br/>Mission Driver</div>
    <div class="cycle-arrow"><i class="fa-solid fa-arrow-right"></i></div>
    <div class="cycle-step" style="background:var(--color-warning);color:white">Act 3<br/>Attractor Cycle</div>
    <div class="cycle-arrow"><i class="fa-solid fa-arrow-right"></i></div>
    <div class="cycle-step" style="background:var(--color-danger);color:white">Act 4<br/>Evidence</div>
    <div class="cycle-arrow"><i class="fa-solid fa-arrow-right"></i></div>
    <div class="cycle-step" style="background:#7c3aed;color:white">Act 5<br/>Take Away</div>
  </div>
  <p style="margin-top:1.5rem; text-align:center">从最内层控制 Loop 开始，逐层向外展开。</p>
</section>

<section>
  <h2>没有 Plan Loop 会怎样？</h2>
  <div class="two-column">
    <div class="column">
      <h4>传统 AI 开发模式</h4>
      <div class="flow-layout" style="grid-template-columns:1fr auto 1fr auto 1fr;">
        <div class="flow-step">用户说</div>
        <div class="flow-arrow"><i class="fa-solid fa-arrow-right"></i></div>
        <div class="flow-step">AI 写</div>
        <div class="flow-arrow"><i class="fa-solid fa-arrow-right"></i></div>
        <div class="flow-step">用户改</div>
      </div>
      <div class="flow-layout" style="grid-template-columns:1fr auto 1fr; margin-top:0.5rem;">
        <div class="flow-step">AI 再写</div>
        <div class="flow-arrow"><i class="fa-solid fa-arrow-right"></i></div>
        <div class="flow-step" style="border-color:var(--color-danger)">∞ 循环</div>
      </div>
      <p style="margin-top:0.5rem"><span class="tag-danger">无退出条件</span></p>
    </div>
    <div class="column">
      <h4>四大缺失</h4>
      <ul class="icon-list">
        <li class="icon-bullet"><i class="fa-solid fa-circle-xmark" style="color:var(--color-danger)"></i> <strong>无基线</strong> — AI 从对话记忆推断代码状态</li>
        <li class="icon-bullet"><i class="fa-solid fa-circle-xmark" style="color:var(--color-danger)"></i> <strong>无范围</strong> — 经常做多或做少</li>
        <li class="icon-bullet"><i class="fa-solid fa-circle-xmark" style="color:var(--color-danger)"></i> <strong>无退出标准</strong> — "差不多了"才算完</li>
        <li class="icon-bullet"><i class="fa-solid fa-circle-xmark" style="color:var(--color-danger)"></i> <strong>无审计</strong> — AI 自己说自己做完了</li>
      </ul>
    </div>
  </div>
</section>

<section>
  <h2>Plan 作为关闭契约</h2>
  <p>Plan 不是任务清单，是<strong>关闭契约</strong>：</p>
  <div class="flow-layout" style="grid-template-columns:1fr auto 1fr auto 1fr auto 1fr auto 1fr;">
    <div class="flow-step">起草<br/><small>draft</small></div>
    <div class="flow-arrow"><i class="fa-solid fa-arrow-right"></i></div>
    <div class="flow-step" style="border-color:var(--color-danger)">独立审查<br/><small>fresh session</small></div>
    <div class="flow-arrow"><i class="fa-solid fa-arrow-right"></i></div>
    <div class="flow-step" style="border-color:var(--color-secondary)">Active<br/><small>可执行</small></div>
    <div class="flow-arrow"><i class="fa-solid fa-arrow-right"></i></div>
    <div class="flow-step">执行 Phase<br/><small>含验证</small></div>
    <div class="flow-arrow"><i class="fa-solid fa-arrow-right"></i></div>
    <div class="flow-step" style="border-color:var(--color-danger)">结束审计<br/><small>fresh session</small></div>
  </div>
  <div class="comparison-table" style="margin-top:1rem">
    <table>
      <tr><th>契约要素</th><th>解决的问题</th></tr>
      <tr><td><code>Current Baseline</code></td><td>从仓库读取，不依赖记忆</td></tr>
      <tr><td><code>Goals + Non-Goals</code></td><td>明确做什么和不做什么</td></tr>
      <tr><td><code>Exit Criteria</code></td><td>可观察的关闭条件</td></tr>
      <tr><td><code>Closure Gates</code></td><td>最终验证检查清单</td></tr>
      <tr><td><code>Draft Review Record</code></td><td>独立审查迭代记录</td></tr>
    </table>
  </div>
</section>

<section>
  <h2>实例：Plan 1100-1 销售定价引擎</h2>
  <div class="flow-layout" style="grid-template-columns:1fr auto 1fr auto 1fr auto 1fr auto 1fr auto 1fr;">
    <div class="flow-step">draft</div>
    <div class="flow-arrow"><i class="fa-solid fa-arrow-right"></i></div>
    <div class="flow-step" style="border-color:var(--color-warning)">审查 iter1<br/>needs_revision</div>
    <div class="flow-arrow"><i class="fa-solid fa-arrow-right"></i></div>
    <div class="flow-step">修订</div>
    <div class="flow-arrow"><i class="fa-solid fa-arrow-right"></i></div>
    <div class="flow-step" style="border-color:var(--color-secondary)">审查 iter2<br/>accept</div>
    <div class="flow-arrow"><i class="fa-solid fa-arrow-right"></i></div>
    <div class="flow-step">active<br/>Phase 1-4</div>
    <div class="flow-arrow"><i class="fa-solid fa-arrow-right"></i></div>
    <div class="flow-step" style="border-color:var(--color-primary)">全绿 ✅<br/>completed</div>
  </div>
  <div class="highlight-box" style="margin-top:1rem">
    <strong>该批次 4 个 Plan 独立审查共拦截 4 个 P0 缺陷</strong>——码值冲突、BUDGET 污染实际财务、GlBalance 前提错误、维度歧义。<strong>全部在编码前拦截。</strong>
  </div>
  <p style="margin-top:0.5rem">从起草到完成约 2 天。没有 Plan Loop：4 个 P0 在运行期才暴露，修复成本高 5-10 倍。</p>
</section>

<section>
  <h2>为什么独立审查必须 fresh session</h2>
  <div class="two-column">
    <div class="column">
      <div class="highlight-box" style="border-left-color:var(--color-danger)">
        <strong>❌ 同一上下文</strong>
        <p style="margin-top:0.5rem">AI 写代码 → 同一 AI 说"做完了"<br/>天然确认偏差</p>
      </div>
    </div>
    <div class="column">
      <div class="highlight-box" style="border-left-color:var(--color-secondary)">
        <strong>✅ Fresh session 独立审查</strong>
        <p style="margin-top:0.5rem">不继承执行上下文<br/>冷启动重查仓库</p>
      </div>
    </div>
  </div>
  <h4 style="margin-top:1rem">nop-app-erp 的真实教训（07-12）</h4>
  <ul>
    <li>子代理声称"零 Delta 使用" → <strong>grep 发现 338 个 <code>x:extends</code></strong></li>
    <li>子代理声称"循环依赖" → <strong><code>mvn clean install</code> 无环</strong></li>
  </ul>
</section>

<section>
  <h2>189 份 Plan 的可追溯性</h2>
  <p>每一份 Plan 都可以追溯：</p>
  <div class="card-grid card-grid-2" style="margin-top:1rem">
    <div class="stat-card"><i class="fa-solid fa-route" style="font-size:2rem;color:var(--color-primary)"></i><br/><strong>为什么做</strong><br/><small><code>Source</code> → 路线图工作项</small></div>
    <div class="stat-card"><i class="fa-solid fa-database" style="font-size:2rem;color:var(--color-primary)"></i><br/><strong>做前状态</strong><br/><small><code>Current Baseline</code> → 仓库快照</small></div>
    <div class="stat-card"><i class="fa-solid fa-scale-balanced" style="font-size:2rem;color:var(--color-primary)"></i><br/><strong>做了什么决策</strong><br/><small><code>Decision</code> → 备选方案 + 被否原因</small></div>
    <div class="stat-card"><i class="fa-solid fa-check-double" style="font-size:2rem;color:var(--color-primary)"></i><br/><strong>做完了吗</strong><br/><small><code>Closure Gates</code> → 独立审计记录</small></div>
  </div>
  <p style="margin-top:1rem"><strong>轨迹信息的外部化</strong>——关键决策在文件里，不在临时对话里。下次 AI 会话可以重新加载。</p>
</section>

<section>
  <h2>为什么需要更大一层的 Loop？</h2>
  <div class="two-column">
    <div class="column">
      <h4>Plan Loop 只管理"一次变更"</h4>
      <ul>
        <li>谁来决定下一个做什么？</li>
        <li>谁确保所有路线图工作项都被执行？</li>
        <li>谁在空闲时触发审计？</li>
        <li>谁把 Plan 完成状态同步回路线图？</li>
      </ul>
      <p style="margin-top:0.5rem">全凭人做 → 手动、不可重复、依赖注意力</p>
    </div>
    <div class="column">
      <h4>Mission Driver 是更大一层的 Loop</h4>
      <div style="text-align:center; padding:1rem">
        <i class="fa-solid fa-rotate" style="font-size:4rem;color:var(--color-secondary)"></i>
        <p style="margin-top:0.5rem">在 Plan Loop 之外加一个编排层<br/><strong>自动遍历路线图</strong></p>
      </div>
    </div>
  </div>
</section>

<section>
  <h2>Mission Driver：完整运行实例</h2>
  <p style="margin-bottom:0.8rem">07-10 的真实运行记录：</p>
  <div class="comparison-table">
    <table>
      <tr><th>时间</th><th>步骤</th><th>结果</th></tr>
      <tr><td>08:00</td><td><span class="tag-success">CHECK</span></td><td>mvn clean install 154 模块 BUILD SUCCESS</td></tr>
      <tr><td>08:05</td><td><span class="tag">REVIEW_PLANS</span></td><td>4 个 draft Plan 逐个审查 → all active</td></tr>
      <tr><td>09:30</td><td><span class="tag">EXEC_PLANS</span></td><td>4 个 active Plan 逐个执行完毕，全绿 ✅</td></tr>
      <tr><td>16:00</td><td><span class="tag">DRAFT_PLANS</span></td><td>路线图全 done → nothing</td></tr>
      <tr><td>16:05</td><td><span class="tag-warn">DEEP_AUDIT</span></td><td>自动启动深度审计…</td></tr>
    </table>
  </div>
</section>

<section>
  <h2>Mission Driver 的真实 Loop</h2>
  <div class="cycle-diagram" style="grid-template-columns:1fr auto 1fr auto 1fr auto 1fr auto 1fr;">
    <div class="cycle-step" style="background:var(--color-primary);color:white">CHECK<br/><small>健康检查</small></div>
    <div class="cycle-arrow"><i class="fa-solid fa-arrow-right"></i></div>
    <div class="cycle-step" style="background:var(--color-secondary);color:white">REVIEW_PLANS<br/><small>draft→active</small></div>
    <div class="cycle-arrow"><i class="fa-solid fa-arrow-right"></i></div>
    <div class="cycle-step" style="background:var(--color-warning);color:white">EXEC_PLANS<br/><small>forEach active</small></div>
    <div class="cycle-arrow"><i class="fa-solid fa-arrow-right"></i></div>
    <div class="cycle-step" style="background:#7c3aed;color:white">DRAFT_PLANS<br/><small>路线图→新Plan</small></div>
    <div class="cycle-arrow"><i class="fa-solid fa-arrow-right"></i></div>
    <div class="cycle-step" style="background:var(--color-danger);color:white">DEEP_AUDIT<br/><small>系统自审计</small></div>
  </div>
  <p style="text-align:center; margin-top:1rem">来自 <code>flows/mission-driver.json</code> 的实际流程</p>
  <div class="highlight-box" style="margin-top:0.5rem">
    <strong>关键设计</strong>：REVIEW 在 EXEC 之前（避免重复起草）· EXEC 用 forEach（一次返回后重新扫描）· DRAFT 包含自审计（引擎层不设独立审计步骤）
  </div>
</section>

<section>
  <h2>DEEP_AUDIT——系统不自满</h2>
  <div class="two-column">
    <div class="column">
      <div class="flow-layout" style="grid-template-columns:1fr auto 1fr;">
        <div class="flow-step">MULTI_AUDIT<br/>多维审计</div>
        <div class="flow-arrow"><i class="fa-solid fa-arrow-right"></i></div>
        <div class="flow-step">OPEN_AUDIT<br/>开放式审计</div>
      </div>
      <div class="flow-arrow" style="text-align:center; margin:0.5rem 0"><i class="fa-solid fa-arrow-down"></i></div>
      <div class="flow-step" style="text-align:center">DRAFT_FROM_AUDITS<br/>从审计起草新 Plan</div>
    </div>
    <div class="column">
      <h4>07-07 综合审计实例</h4>
      <ul>
        <li>4 路并行子代理独立审计</li>
        <li><span class="tag-danger">4 严重</span> + <span class="tag-warn">6 高</span> + 9 中 + 6 低</li>
        <li>表名双前缀违规（7 域）</li>
        <li>docStatus 不一致 / 冗余字典</li>
        <li>ErrorCode 不统一（5 域 87 码）</li>
        <li><strong>1-3 天内全部闭合</strong></li>
      </ul>
    </div>
  </div>
  <p style="margin-top:0.5rem">传统模式"全 done"是交付信号。AGE：全 done 是<strong>自动升级审计的信号</strong>。</p>
</section>

<section>
  <h2>两层 Loop 协作：竞争杠杆审计</h2>
  <p>07-12 用户要求核实"8 个超越点"</p>
  <div class="comparison-table" style="margin-top:0.8rem">
    <table>
      <tr><th>裁决</th><th>数量</th><th>内容</th></tr>
      <tr><td><span class="tag-success">完整兑现</span></td><td>4</td><td>多套账 / 业财一体三件套 / 7 种成本方法 / AR-AP</td></tr>
      <tr><td><span class="tag-warn">有缺口</span></td><td>2</td><td>制造委外空壳 / Delta 文档数据错误</td></tr>
      <tr><td><span class="tag-danger">夸大</span></td><td>2</td><td>多公司仅地基 / 域独立部署言过其实</td></tr>
    </table>
  </div>
  <p style="margin-top:0.5rem">审计发现 → DRAFT_PLANS 产生 4 个新 Plan → EXEC_PLANS 执行 → 修正完成。<strong>两层 Loop 协同完成闭环。</strong></p>
</section>

<section>
  <h2>但还不够——谁定义方向？</h2>
  <div style="text-align:center; padding:2rem">
    <i class="fa-solid fa-circle-question" style="font-size:5rem;color:var(--color-warning)"></i>
    <p style="font-size:1.5rem; margin-top:1rem">Plan Loop 和 Mission Driver Loop<br/>解决了<strong>"按路线图执行"</strong></p>
    <p style="font-size:1.5rem; margin-top:0.5rem">但路线图本身正确吗？<strong>方向合理吗？</strong></p>
    <div class="highlight-box" style="margin-top:1rem; font-size:1.2rem">
      "如果路线图方向错了，执行得越好，偏离得越快。"
    </div>
  </div>
</section>

<section>
  <h2>什么是 Attractor——dynamical systems 视角</h2>
  <div class="card-grid card-grid-2">
    <div class="stat-card" style="grid-column:1">
      <strong>🧲 Attractor（吸引子）</strong>
      <p>系统应长期回归的<strong>稳定结构</strong></p>
    </div>
    <div class="stat-card" style="grid-column:2">
      <strong>🛤️ Trajectory（轨迹）</strong>
      <p>每轮生成-验证-修正的<strong>实际路径</strong></p>
    </div>
    <div class="stat-card" style="grid-column:1">
      <strong>🌌 State Space</strong>
      <p>系统可能进入的<strong>所有状态</strong></p>
    </div>
    <div class="stat-card" style="grid-column:2">
      <strong>🎮 Control（控制）</strong>
      <p>将轨迹拉回 attractor 的<strong>机制</strong><br/><small>Plan Loop + Mission Driver</small></p>
    </div>
  </div>
  <div class="highlight-box" style="margin-top:0.8rem">
    <strong>三个最易混淆的点</strong>：吸引子 ≠ 边界（禁止行为 vs 汇聚目标）·吸引子 ≠ 护栏（执行层 vs 方向层）·吸引子 ≠ 控制目标（先于控制存在）
  </div>
  <p style="margin-top:0.5rem">吸引子 <strong>不是</strong> 文档。文档是承载者。吸引子是"所有实现必须从 ORM XML 单一真相源生成"这个<strong>结构不变量</strong>。</p>
</section>

<section>
  <h2>nop-app-erp 的三个核心吸引子</h2>
  <div class="card-grid card-grid-2">
    <div class="stat-card">
      <i class="fa-solid fa-database" style="font-size:2rem;color:var(--color-primary)"></i>
      <p style="margin-top:0.5rem"><strong>模型驱动</strong></p>
      <p><small>不变量：所有实现从 ORM XML 生成，不允许手写绕过的 Entity/DAO</small></p>
      <p><small>承载者：<code>module-*/model/*.orm.xml</code></small></p>
    </div>
    <div class="stat-card">
      <i class="fa-solid fa-diagram-project" style="font-size:2rem;color:var(--color-secondary)"></i>
      <p style="margin-top:0.5rem"><strong>状态一致性</strong></p>
      <p><small>不变量：每域的审批/过账/状态迁移必须与状态机一致</small></p>
      <p><small>承载者：<code>docs/design/*/state-machine.md</code></small></p>
    </div>
    <div class="stat-card" style="grid-column:1 / span 2">
      <i class="fa-solid fa-file-invoice" style="font-size:2rem;color:var(--color-warning)"></i>
      <p style="margin-top:0.5rem"><strong>过账契约</strong></p>
      <p><small>不变量：所有过账走同一 SPI 契约，红字用同向取负约定</small></p>
      <p><small>承载者：<code>docs/design/finance/posting.md</code></small></p>
    </div>
  </div>
</section>

<section>
  <h2>用户其实在做什么——定义和演进吸引子</h2>
  <div class="comparison-table">
    <table>
      <tr><th>介入类型</th><th>频次</th><th>用户做了什么</th><th>对吸引子的影响</th></tr>
      <tr>
        <td><span class="tag-danger">A 类</span></td>
        <td>9 次<br/>06-22 ~ 07-03</td>
        <td>明确指明平台机制<br/>"租户开关是全局配置，不是每个实体手写"</td>
        <td><strong>修正吸引子的技术基底</strong></td>
      </tr>
      <tr>
        <td><span class="tag-warn">B 类</span></td>
        <td>9 次</td>
        <td>指明工程原则方向<br/>"过账方法应挂聚合根 BizModel"</td>
        <td><strong>修正吸引子的结构原则</strong></td>
      </tr>
      <tr>
        <td><span class="tag-success">C 类</span></td>
        <td>绝大多数<br/>07-04 后为主</td>
        <td>只让 AI 自查对比<br/>"对照 erp-survey 检查功能覆盖"</td>
        <td><strong>在吸引子内让 AI 自行扩张</strong></td>
      </tr>
    </table>
  </div>
  <p style="margin-top:0.8rem"><strong>关键洞察</strong>：用户后期介入归零不是因为 AI 学会了写代码，而是因为<strong>吸引子已经定义好了</strong>。方向对了，AI 能自动扩张。</p>
</section>

<section>
  <h2>三层汇聚——从割裂到嵌套</h2>
  <p style="margin-bottom:1rem">这三层不是独立的——它们是<strong>嵌套</strong>的：</p>
  <ul>
    <li><strong>Mission Driver</strong> 的 <code>EXEC_PLANS</code> 对每个 active Plan 走 <code>plan-execution</code> subflow → <strong>包含 Plan Loop</strong></li>
    <li><strong>Attractor</strong> 的修订（如 07-01 过账引擎聚合根纠正）→ 新路线图工作项 → <strong>Mission Driver 自动编排</strong> → 分解为多个 Plan → <strong>每个 Plan 走 Plan Loop</strong></li>
    <li><strong>Plan Loop</strong> 完成 → 同步回路线图 → <strong>Mission Driver 自动选下一个</strong></li>
  </ul>
  <div class="highlight-box" style="margin-top:1rem; text-align:center; font-size:1.3rem">
    <strong>Plan 关闭每一次变更。Mission 编排每一个计划。吸引子定义方向。<br/>三层都在，系统才能自运转。</strong>
  </div>
</section>

<section>
  <h2>22 天接力赛——三层的交替主导</h2>
  <div class="timeline" style="margin-top:0.5rem">
    <div class="timeline-items">
      <div class="timeline-item"><small>S1·4d</small><br/>🎯 <strong>吸引子定义</strong><br/><small>竞品调研 + ORM + 文档</small></div>
      <div class="timeline-item"><small>S2·2d</small><br/>🔍 <strong>吸引子校准</strong><br/><small>grill 83 题决议</small></div>
      <div class="timeline-item"><small>S3·2d</small><br/>📋 <strong>Plan Loop 密集</strong><br/><small>9 个 Plan</small></div>
      <div class="timeline-item"><small>S4·2d</small><br/>⚙️ <strong>Mission Driver 批处理</strong><br/><small>13 域铺开</small></div>
      <div class="timeline-item"><small>S5·3d</small><br/>🔄 <strong>三层协同峰值</strong><br/><small>P2P/O2C/报表</small></div>
      <div class="timeline-item"><small>S6·1d</small><br/>🔧 <strong>吸引子修正</strong><br/><small>4 严重+6 高</small></div>
      <div class="timeline-item"><small>S7·2d</small><br/>🧪 <strong>观测性就绪</strong><br/><small>Playwright+种子</small></div>
      <div class="timeline-item"><small>S8·4d</small><br/>🎯 <strong>三层收敛</strong><br/><small>竞争核实+产品化</small></div>
    </div>
  </div>
</section>

<section>
  <h2>验证基线 + 计划产出</h2>
  <div class="two-column">
    <div class="column">
      <h4>验证基线演进</h4>
      <ul>
        <li>06-23：81 模块首闭环</li>
        <li>07-01：146 模块全绿</li>
        <li>07-13：154 模块全绿</li>
        <li>测试 ~2900，E2E 260+</li>
      </ul>
      <p style="margin-top:0.5rem">贯穿始终的 full-green</p>
    </div>
    <div class="column">
      <h4>计划产出密度</h4>
      <ul>
        <li>07-01：9 个 Plan（核心逻辑爆发）</li>
        <li>07-02：14 个 Plan（业财铺开）</li>
        <li>07-10：13 个 Plan（深化）</li>
      </ul>
      <p style="margin-top:0.5rem">批处理模式：一批 draft → review → exec → close</p>
    </div>
  </div>
</section>

<section>
  <h2>审计方法论 8 阶段演进</h2>
  <div class="comparison-table" style="font-size:0.9rem">
    <table>
      <tr><th>#</th><th>时间</th><th>方法</th><th>演进点</th></tr>
      <tr><td>1</td><td>07-02</td><td>单代理 + rg 扫描</td><td>首次合规审计</td></tr>
      <tr><td>2</td><td>07-04</td><td>单代理 + 6 维度</td><td>结束审计范式定型</td></tr>
      <tr><td>3</td><td>07-05</td><td>3 路并行子代理</td><td>首次多代理并行</td></tr>
      <tr><td><strong>4</strong></td><td><strong>07-05</strong></td><td><strong>4 路对抗性子代理</strong></td><td><strong>⭐ 方法论飞跃：方法存在→语义保真</strong></td></tr>
      <tr><td>5</td><td>07-05</td><td>工具化合规检查器</td><td>人工经验→可重复脚本</td></tr>
      <tr><td>6</td><td>07-06</td><td>UC 断言对照</td><td>粒度：代码合规→业务实现率</td></tr>
      <tr><td>7</td><td>07-07</td><td>4 路 + 自省</td><td>审计质量回顾</td></tr>
      <tr><td>8</td><td>07-12</td><td>3 路 + 主代理复核</td><td>纠正 2 处子代理误判</td></tr>
    </table>
  </div>
  <p style="margin-top:0.5rem"><strong>Meta 洞察</strong>：控制 Loop 自身也在演进——5 个演进特征：单→多路 / 方法存在→语义保真 / 人工→工具化 / 代码合规→业务实现率 / 子代理断言须复核</p>
</section>

<section>
  <h2>路线图终态</h2>
  <div class="card-grid card-grid-2">
    <div class="stat-card" style="text-align:center">
      <i class="fa-solid fa-check-circle" style="font-size:3rem;color:var(--color-secondary)"></i>
      <p style="margin-top:0.5rem"><strong>crud-roadmap</strong></p>
      <p>18 域全部 done ✅</p>
    </div>
    <div class="stat-card" style="text-align:center">
      <i class="fa-solid fa-check-circle" style="font-size:3rem;color:var(--color-secondary)"></i>
      <p style="margin-top:0.5rem"><strong>core-business-roadmap</strong></p>
      <p>M1/M4/M5 全部 done ✅</p>
    </div>
    <div class="stat-card" style="text-align:center; grid-column:1 / span 2">
      <i class="fa-solid fa-check-circle" style="font-size:3rem;color:var(--color-secondary)"></i>
      <p style="margin-top:0.5rem"><strong>extended-roadmap</strong></p>
      <p>M2/M3 全部 done ✅</p>
    </div>
  </div>
  <p style="margin-top:0.5rem; text-align:center">Mission Driver 遍历了每个工作项——<strong>没有漏项、没有跳过</strong></p>
</section>

<section>
  <h2>可复制的是控制结构，不是代码</h2>
  <p>22 天 154 模块 → 说"AI 真强"是错误归因。</p>
  <div class="card-grid card-grid-2" style="margin-top:1rem">
    <div class="stat-card">
      <i class="fa-solid fa-file-lines" style="font-size:2rem;color:var(--color-primary)"></i>
      <p style="margin-top:0.5rem"><strong>Plan Loop</strong></p>
      <p><small>docs/plans/ + fresh session 审计契约</small></p>
      <p><small>任何项目都能用</small></p>
    </div>
    <div class="stat-card">
      <i class="fa-solid fa-robot" style="font-size:2rem;color:var(--color-secondary)"></i>
      <p style="margin-top:0.5rem"><strong>Mission Driver Loop</strong></p>
      <p><small>tools/mission-driver/ engine.js</small></p>
      <p><small>已包含在 AGE 模板</small></p>
    </div>
    <div class="stat-card" style="grid-column:1 / span 2">
      <i class="fa-solid fa-compass" style="font-size:2rem;color:var(--color-warning)"></i>
      <p style="margin-top:0.5rem"><strong>Attractor Evolution Cycle</strong></p>
      <p><small>owner docs 体系 + 设计审查流程</small></p>
      <p><small>AGE 模板的 docs 骨架</small></p>
    </div>
  </div>
  <div class="highlight-box" style="margin-top:0.8rem; text-align:center">
    <code style="font-size:1.1rem">cp -r ~/app/attractor-guided-engineering-template/ ./my-project/</code>
  </div>
</section>

<section>
  <h2>在自己项目落地——7 步</h2>
  <div class="flow-layout" style="grid-template-columns:1fr auto 1fr auto 1fr auto 1fr;">
    <div class="flow-step">① 复制模板</div>
    <div class="flow-arrow"><i class="fa-solid fa-arrow-right"></i></div>
    <div class="flow-step">② 填验证命令<br/><small>~15 min</small></div>
    <div class="flow-arrow"><i class="fa-solid fa-arrow-right"></i></div>
    <div class="flow-step">③ 填保护区域<br/><small>~10 min</small></div>
    <div class="flow-arrow"><i class="fa-solid fa-arrow-right"></i></div>
    <div class="flow-step">④ <span style="color:var(--color-danger)">⭐ 定义吸引子</span><br/><small>不能外包给 AI</small></div>
  </div>
  <div class="flow-arrow" style="text-align:center; margin:0.5rem 0"><i class="fa-solid fa-arrow-down"></i></div>
  <div class="flow-layout" style="grid-template-columns:1fr auto 1fr auto 1fr;">
    <div class="flow-step">⑤ 创建路线图</div>
    <div class="flow-arrow"><i class="fa-solid fa-arrow-right"></i></div>
    <div class="flow-step">⑥ Mission 配置</div>
    <div class="flow-arrow"><i class="fa-solid fa-arrow-right"></i></div>
    <div class="flow-step">⑦ 自动运行<br/><small>mission driver</small></div>
  </div>
  <div class="highlight-box" style="margin-top:0.8rem">
    "The responsibility for defining a new attractor cannot be outsourced to AI by default." — AGE 文章
  </div>
</section>

<section>
  <h2>三个 Takeaway</h2>
  <div class="card-grid card-grid-2">
    <div class="stat-card" style="grid-column:1 / span 2">
      <i class="fa-solid fa-file-signature" style="font-size:2rem;color:var(--color-primary)"></i>
      <p style="margin-top:0.5rem"><strong>把"变更"从对话变成契约</strong></p>
      <p><small>对话不可追溯 → Plan Loop：起草→审查→审计→关闭</small></p>
    </div>
    <div class="stat-card" style="grid-column:1 / span 2">
      <i class="fa-solid fa-route" style="font-size:2rem;color:var(--color-secondary)"></i>
      <p style="margin-top:0.5rem"><strong>把"编排"从记忆变成自动</strong></p>
      <p><small>不需要人记住下一步 → Mission Driver 自动选、起草、验证</small></p>
    </div>
    <div class="stat-card" style="grid-column:1 / span 2">
      <i class="fa-solid fa-book" style="font-size:2rem;color:var(--color-warning)"></i>
      <p style="margin-top:0.5rem"><strong>把"方向"从个人变成文件</strong></p>
      <p><small>架构判断是稀缺资源 → 吸引子承载于 owner docs，可版本、可审计、可继承</small></p>
    </div>
  </div>
</section>

<section>
  <h1>人定义方向<br/><span>AI 执行扩张 · 系统检查偏离</span></h1>
  <p class="subtitle">100 个变更后，系统还在正确的结构上——如果三层 Loop 都在。</p>
  <div style="margin-top:1.5rem">
    <p>Plan 关闭每一次变更。</p>
    <p>Mission 编排每一个计划。</p>
    <p>Attractor 定义方向。</p>
  </div>
  <p style="margin-top:2rem; font-size:0.9rem">nop-app-erp · AGE 工作流 · nop-entropy</p>
  <p style="font-size:0.8rem; color:var(--color-text-light)">谢谢 · Q&A</p>
</section>

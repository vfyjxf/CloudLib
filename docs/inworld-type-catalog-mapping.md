# Inworld UI 类型目录 → 布局 API 映射（22 类）

计划 §2.5 的 22 类 inworld UI 类型目录，逐类给出「基础 profile + 切面覆盖」的表达方案。
每行的可表达性由 `TypeCatalogMappingTest`（`api.ui.inworld.layout` 测试包）以三重方式证明：
**编译期**（spec 可构造）、**校验期**（通过 `FacetRules` 非法组合表）、**管线期**
（经 `PipelineAssembler` 组装后由真实 `InworldCoordinator` 驱动一帧并获得 strongest rung 授予）。

约定：

- 「profile 预设」之外的条目 = 在 profile 七切面预设上用 `with*` 方法声明的覆盖。
- 跨族重锚定（world → screen）时先换 `spaces` 再换 `anchor`（`withAnchor` 会按锚点族自动
  调整 orientation，中间态必须保持合法——见 `ElementSpec.withAnchor` 的 javadoc）。
- 七维能力（锚点/朝向/空间政策/降级链/分组/稳定性/屏外行为）里凡未显式覆盖的维度，取 profile 预设默认。

## 源追踪 · 实体

| 类型 | 基础 profile | 声明覆盖 | 七维要点 |
|---|---|---|---|
| T1 血条/名牌 | `nameplate` | `anchor=entity(id, lift)` | cameraBillboard；active；密度降级阶梯 (100×26→pip)；`ClusterToRepresentative` 分组；WoW 基线稳定参数；屏外可经 waypoint 族的 directionalOnly 思路降级（ANGLE 由 `waypoint` 算法绑定承接） |
| T2 伤害数字 | `transientUi` | `anchor=entity(id, lift)` | ghost（互不推挤）；快 fade 阶梯；TTL 由驱动方 retract（进入 linger） |
| T3 状态图标行 | `orbit` | `anchor=entity(id)`；`group=OrbitAroundAnchor`（分组判别式 = 实体 id） | 环槽位；passive；icon/pip 阶梯；组容量阶梯：外扩环→聚合→隐藏 |
| T4 实体跟随面板 | `follow` | `anchor=entity(id, 0.5)`；`custom(SidebarLayouter)`（逃生舱） | nameplate 动力学；layers 含 screenPanel（遮挡转侧栏是切面覆盖不了的定制，走 `InworldLayouter`） |
| T5 载具/骑乘 | `orbit` | `anchor=entity(vehicleId)`；`group=OrbitAroundAnchor` | 多乘客环聚合；锚点是载具而非乘客 |
| T6 气泡对话 | `transientUi` | `anchor=entity(id, 2.0)`；`group=StackInColumn` | 列栈 + "+N" 溢出；TTL 同 T2；说话者切换迟滞由组内 sticky 槽承接 |

## 源追踪 · 方块

| 类型 | 基础 profile | 声明覆盖 | 七维要点 |
|---|---|---|---|
| T7 贴面面板 | `facePanel` | `anchor=blockFace(x,y,z,normal,inset)` | blockFace 朝向（锚点/朝向强绑定，规则 1 强制）；fixed；紧凑阶梯 |
| T8 全息 | `hologram` | `anchor=position(x,y,z)` | yawBillboard；fixed；垂直偏移弹簧（nameplateBaseline 的 spring） |
| T9 进度条（熔炉式） | `facePanel` | `anchor=blockFace(...)`；`degrade=fixed(48×12→20×20)` | 迷人贴面 → 角标（iconOnly rung） |
| T10 区域/体积标记 | `ground` | `anchor=position(...)` | fixed（不参与矩形遮蔽）；线框/角标为渲染层语义 |
| T11 地面投影 | `ground` | `anchor=position(...)`（规范用法） | groundParallel（需要 position/blockFace 锚点，规则 1）；fixed |

## 镜头/世界混合

| 类型 | 基础 profile | 声明覆盖 | 七维要点 |
|---|---|---|---|
| T12 选中描边 | `transientUi` | `anchor=entity(id)`；`spaces={indicator}+ghost` | 纯渲染层；ghost 规则 3/4 强制「不避让/不被避让/单候选」 |
| T13 leader 连线 | `nameplate` | `anchor=position(...)` | 自适应 s→po→hyperleader 由 nameplate 算法绑定 |
| T14 waypoint（屏内） | `waypoint` | `anchor=position(...)` | indicator 层免矩形避让；深度缩放进降级链 |
| T15 waypoint（屏外） | `waypoint` | `anchor=position(...)`（投影出屏） | allowsClamp 把候选拉回工作区边缘；ANGLE 编码为算法绑定 |
| T16 ping/标记 | `transientUi` | `anchor=position(...)`；`group=OrbitAroundAnchor` | 扇形展开 = 环槽位分组；TTL 由驱动方管理 |

## 屏幕空间

| 类型 | 基础 profile | 声明覆盖 | 七维要点 |
|---|---|---|---|
| T17 受击方向指示 | `transientUi` | `spaces={indicator}+ghost` 先行，再 `anchor=cameraTracked(0.5,0.5)`（orientation 自动随锚点族变为 screen） | 屏中心环弧；豁免避让 |
| T18 方位带/罗盘 | `dock` | `anchor=cameraTracked(0.5,0.03)`；`degrade=passive(200×18→…)` | 顶边 dock 扫描线；HUD 排除区联动（avoids={hudBase,hudOverlay}） |
| T19 交互提示 | `excentric` | `anchor=position/blockFace（准星目标）` | Excentric 纵列 + 直 leader；passive |
| T20 径向菜单 | `transientUi` | `spaces={screenPanel}+fixed+priority 20`，再 `anchor=cameraTracked(0.5,0.5)` | 模态独占：fixed 豁免 + 高优先级；暂停协商由驱动方控制协调器节流 |
| T21 检视层 | `transientUi` | `spaces={screenPanel,hudOverlay}+fixed+priority 30`，再 `anchor=cameraTracked(0.5,0.5)` | 模态拍平；冻结世界追踪由驱动方停发 `LayoutEnvironment` |
| T22 小地图/附属屏 | `dock` | profile 预设即规范用法（`cameraTracked(0.97,0.05)`） | 一维 dock 游标；角部停靠 |

## 非法切面组合表（`FacetRules`）

1. **锚点↔朝向**：blockFace 朝向需要 blockFace 锚点；groundParallel 需要 position/blockFace 锚点；
   cameraBillboard/yawBillboard 需要世界锚点；screen 朝向需要 cameraTracked/none 锚点（反之亦然）。
   `withAnchor` 对不兼容的朝向自动随锚点族调整（锚点选择蕴含朝向族），构造器直配仍拒绝。
2. **层↔锚点**：声明 `worldAnchored` 层必须是世界锚点。
3. **ghost 避让**：ghost 不避让、不被避让、无视排除区。
4. **ghost 候选**：ghost 只允许 anchoredQuad/none（单候选）族，不得使用 dock/环/列等占位机器。
5. **分组锚点**：`OrbitAroundAnchor`/`ClusterToRepresentative` 需要世界锚点。
6. **分组×算法**：`ClusterToRepresentative` 需要 profile 的算法绑定聚类（nameplate/orbit 族）。
7. **worldOnly 能力**：worldOnly 元素必须自带世界表示——提议世界候选的 custom layouter，或携带世界
  包围盒的世界锚点。
8. **zone 参与**（Z2）：声明 `ZoneFacet` 必须参与屏幕仲裁——不得是 ghost（ghost 与 zone 成本的
   overlap/邻接机器矛盾），也不得是 worldOnly（zone 点阵是屏幕矩形，worldOnly 完全绕开屏幕仲裁）。
9. **zone 锚点**（Z2）：声明 zone 必须有已声明锚点（非 `none`——点阵停靠在声明的锚点上）且 profile 的
   placement 族是锚点定位的（不得是 dockCursor——dock 候选沿屏幕边扫描，锚点只选边）。
10. **zone 入场级**（Z2）：`ZoneFacet` 的初始 LodTier 只能是 `full`/`compact`（内容承载级）；
    `icon` 及以下是协调器阶梯的降级结果，`clustered` 是分组层的裁决，都不是合法声明。

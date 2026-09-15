# 单位换算 API

> [English version](unit-api.md)

CloudLib 的单位系统用于换算游戏中的各种单位——时间、能量、物品、流体——全程使用**无损精确算术**，即使面对 1:9、1:144 这类不规则比例也不会丢精度。它支持跨单位族桥接（物品 ↔ 流体）、跨族的物质数量比较（`MaterialAmount`），以及人类可读的文本渲染。

包结构：`dev.vfyjxf.cloudlib.api.unit`（预置单位包在 `.units`，文本渲染在 `.text`）。

## 核心概念

### Ratio —— 精确算术

所有数量和换算系数都是 `Ratio`：基于 `BigInteger` 的不可变有理数，始终以约分后的最简形式存储。除非你主动要求，否则不会发生任何舍入。

```java
Ratio.of(9);          // 9
Ratio.of(1, 9);       // 1/9，用于不规则比例
Ratio.ofDecimal(0.1); // 精确的 1/10，不是浮点近似
```

某些规则天然就是有损的（比如实测效率 0.9），可以打上 approximate 标记。这个标记是来源信息：算术运算会按"与"传播它，因此任何经过近似规则的换算链端到端都保持不精确，格式化输出会加上 `≈` 前缀。

```java
Ratio.approximate(0.9).isExact(); // false
// 构建端：.convert(a, b).byApproximate(0.9)
// 数值端：quantity.isExact()
```

### Unit / UnitFamily —— 幻影类型单位族

`UnitFamily` 把度量同一种事物的单位归为一族。族分两种：

- `Kind.measure`——抽象度量（时间、能量），换算规则普适。
- `Kind.matter`——实体物质（物品、流体），换算可能因材料而异，且该族可以注册一个**基准单位**，供 `MaterialAmount` 归一化使用。

泛型参数是幻影标记：直接用常量持有者类本身当标记，无需额外定义标记类型：

```java
public final class TimeUnits {
    public static final UnitFamily<TimeUnits> family = UnitFamily.measure(Namespace.ofMc("time"));
    public static final Unit<TimeUnits> tick = Unit.of(family, Namespace.ofMc("tick"));
}
```

得益于这个标记，同族操作（`Quantity.to`、`add`、`subtract`、`decompose`）在编译期就能得到检查。

### 规则与桥接

换算图由规则构成。两端在同一族内的是**规则（rule）**，跨族的是**桥接（bridge）**。注册采用流式 API：

```java
UnitConverter.builder()
        .convert(EnergyUnits.eu, EnergyUnits.fe).by(4)                           // 1 eu = 4 fe
        .convert(TimeUnits.second, TimeUnits.tick).fixed().by(20)                // 永远不可被覆盖
        .convert(ItemUnits.ingot, ItemUnits.block).forMaterial(iron).by(1, 4)    // 特定材料的覆盖规则
        .convert(ItemUnits.ingot, FluidUnits.millibucket).by(144)                // 跨族桥接
        .build();
```

冲突语义（针对同一有向端点对）：

| 情形 | 结果 |
|---|---|
| 同一对端点以相同比例重复注册 | 抛 `RuleConflictException`（重复规则） |
| 同一对端点以不同比例重复注册 | 覆盖原有的非固定规则 |
| 任一方向上已有 `fixed()` 规则 | 抛 `RuleConflictException`——固定规则不可触碰 |
| 材料 `Namespace` 不同 | 属于另一条边，与通用规则共存 |

解析时材料专属规则优先于通用规则。批量注册则通过 `UnitRule` 记录和 `UnitPack` 打包完成（见下文）。

### 路径解析

换算通过在规则图上做 BFS 最短路径搜索来解析：任意深度的链都能走通（block → ingot → nugget → millibucket），规则自动反向生效，材料专属的边优先于通用边被探索，结果按转换器缓存（memoize）。

## 快速上手

用预置单位包构建一个转换器，然后通过 `Quantity` 流式换算：

```java
UnitConverter converter = UnitConverter.builder()
        .add(TimeUnits.pack())
        .add(ItemUnits.pack())
        .add(FluidUnits.pack())
        .add(GtUnits.pack())
        .build();

converter.convert(1, TimeUnits.hour, TimeUnits.tick).value();   // 72000
converter.quantity(1, ItemUnits.block).to(ItemUnits.nugget);    // 81 个粒
converter.quantity(2, ItemUnits.ingot).toCross(FluidUnits.millibucket); // 288 mB
```

离散换算把数量拆成目标单位的整数部分加上以源单位表示的余数：

```java
DiscreteResult<ItemUnits> result = converter.quantity(10, ItemUnits.ingot)
        .toDiscrete(ItemUnits.block);
result.amount();    // 1 个块
result.remainder(); // 1 个锭
result.exact();     // false —— 有余数
```

`Quantity.toLongExact()` 返回精确的整数值，值不是整数时抛 `InexactResultException`。所有异常都继承自 `UnitConversionException`：

- `NoConversionPathException`——两个单位之间没有可达的规则链。
- `InexactResultException`——对分数值强求精确整数结果。
- `RuleConflictException`——构建期出现重复注册或违反固定规则。

## 预置单位包

所有单位包都在 `dev.vfyjxf.cloudlib.api.unit.units` 下，用 `.add(...pack())` 注册。

**TimeUnits**（`measure`）——规则全部固定：

| 单位 | 规则 |
|---|---|
| `tick`、`second`、`minute`、`hour` | 1 秒 = 20 tick，1 分钟 = 60 秒，1 小时 = 60 分钟 |

**EnergyUnits**（`measure`）：`eu`、`fe`；默认 1 eu = 4 fe，**非固定**，可以覆盖。

**ItemUnits**（`matter`，基准单位 `ingot`）：

| 单位 | 默认规则 |
|---|---|
| `ingot`、`block`、`nugget`、`dust`、`smallDust`、`tinyDust`、`gem`、`plate`、`gear`、`rod` | 1 锭 = 9 粒，1 块 = 9 锭，1 粉 = 4 小粉，1 粉 = 9 微粉 |

`gem` 刻意**不带**任何通用规则：金属块是 9 个锭，而宝石块是 9 个宝石，如果加一条通用的 `block`/`ingot` ↔ `gem` 规则，会经传递性错误地把锭和宝石等同起来。宝石的换算请按材料单独接线，或使用约定包。

**FluidUnits**（`matter`，基准单位 `millibucket`）：`millibucket`、`bucket`、`droplet`；1 桶 = 1000 mB。`droplet` 仅有定义（不同模组的大小约定不同）。

**TicUnits**——匠魂 3 熔炼炉约定（1.18+），全部为指向 `millibucket` 的**固定桥接**：

| nugget | ingot | block | gem |
|---|---|---|---|
| 10 mB | 90 mB | 810 mB | 100 mB |

**GtUnits**——格雷科技约定（GTCEu / GT Modern）。新增 `liter`（GT 的显示单位，与 `millibucket` 固定 1:1）、一组固定桥接和物品形态规则：

| nugget | ingot | block | dust | smallDust | tinyDust | plate | rod | gear |
|---|---|---|---|---|---|---|---|---|
| 16 | 144 | 1296 | 144 | 36 | 16 | 144 | 72 | 576 |

物品形态规则：1 板 = 1 锭，1 杆 = 1/2 锭，1 齿轮 = 4 锭。

> **`TicUnits` 与 `GtUnits` 互斥。** 两者对锭的换算比例不一致（90 对 144），又因为桥接都是固定的，把两个包加进同一个转换器会抛 `RuleConflictException`，而不会悄悄混用两套约定。材料专属的桥接（`.forMaterial(iron).by(...)`）位于独立的边上，依然可以同固定的通用桥接共存。

## MaterialAmount —— 物质的量

`MaterialAmount` 把任意物质数量归一化为所属族注册的**基准单位**，并打上材料标签。同种材料的两份数量由此可以跨族、跨单位比较，还能作为枢纽换算回任意单位。基准单位由单位包注册（`ItemUnits.pack()` 注册 `ingot`，`FluidUnits.pack()` 注册 `millibucket`），也可以用 `builder.baseUnit(unit)` 手动注册——每个物质族一个。

```java
Namespace iron = Namespace.ofCommon("iron");

MaterialAmount ingots = converter.quantity(2, ItemUnits.ingot).toMaterialAmount(iron);
MaterialAmount molten = converter.quantity(288, FluidUnits.millibucket).toMaterialAmount(iron);

ingots.equivalentTo(molten);        // 在 GtUnits 约定下为 true（144 mB/锭）
molten.to(ItemUnits.nugget);        // 18 个粒
molten.to(GtUnits.liter);           // 288 L
```

而在 `TicUnits` 约定下，同样 2 个锭等价于 180 mB。`equivalentTo` 要求材料相同，且换算后的值相等——桥接也计算在内。

## 文本与展示

`UnitNames` 负责提供单位的显示名。`UnitNames.defaults()` 从 id 路径派生（`minecraft:millibucket` → `"millibucket"`）；builder 可以注册自定义名和带材料修饰的名字：

```java
UnitNames names = UnitNames.builder()
        .name(FluidUnits.millibucket, "mB")
        .name(iron, ItemUnits.ingot, "铁锭")
        .build();
QuantityFormatter formatter = new QuantityFormatter(converter, names);
// 内置默认值：converter.formatter()，基于 UnitNames.defaults()
```

`QuantityFormatter` 一览：

- `format(quantity)` → `20 ticks`、`1/9 block`；不精确的值带 `≈` 前缀。
- `format(materialAmount)` → `2 铁锭`（使用材料修饰名）。
- `formatDecimal(quantity, precision)` → `≈0.111 block`（整数值仍保持精确输出）。
- `decompose(quantity, hour, minute, second)` → 沿降序链做精确的贪心拆分，例如 5000 秒 → `[1 hour, 23 minutes, 20 seconds]`；为零的项会被跳过。
- `formatDecomposed(quantity, hour, minute, second)` → `"1h 23m 20s"`。短名默认取单位名首字母，可用 `formatter.withShortName(unit, "min")` 按单位覆盖。
- `ratioText(ingot, block)` → `"1:9"`。

`UnitComponents` 是 Minecraft 适配层（只能在游戏侧加载）。它把数量渲染成 `Component`，单位名通过形如 `unit.<root>.<path>` 的可翻译语言键解析——`minecraft:tick` → `unit.minecraft.tick`：

```java
UnitComponents components = new UnitComponents(converter, names);
components.format(quantity);                          // "20 " + 可翻译的单位名
components.format(materialAmount);                    // 数值 + 可翻译的基准单位名
components.decompose(quantity, hour, minute, second); // "1h 23m 20s"，包成 Component
```

在语言文件里补上键即可，例如 `"unit.minecraft.tick": "tick"`。

## 自定义单位族与规则

一个完整的自定义物质族——气体——演示持有者类模式、基准单位注册和到流体的桥接。API 中携带规则的集合统一使用 Eclipse Collections 的 `ImmutableList`（通过 `Lists.immutable.of(...)` 构建），与 CloudLib 其余部分保持一致。对于只使用 Java 标准集合的消费方，每个规则集合都提供不可修改的 `java.util.List` 视图：预置单位类上是 `rulesAsList` 常量，`UnitPack` 上是 `rulesAsList()` 方法，任何 `ImmutableList` 也可以自行调用 `castToList()` 得到：

```java
public final class GasUnits {

    public static final UnitFamily<GasUnits> family = UnitFamily.matter(Namespace.of("mymod", "gas"));

    public static final Unit<GasUnits> millibucket = Unit.of(family, Namespace.of("mymod", "millibucket"));
    public static final Unit<GasUnits> bucket = Unit.of(family, Namespace.of("mymod", "bucket"));

    public static final Unit<GasUnits> base = millibucket;

    public static final ImmutableList<UnitRule> rules = Lists.immutable.of(
            UnitRule.matter(bucket, millibucket, Ratio.of(1000))
    );

    public static UnitPack pack() {
        return UnitPack.of(rules, base);
    }

    private GasUnits() {
    }
}
```

```java
UnitConverter converter = UnitConverter.builder()
        .add(GasUnits.pack())
        .add(FluidUnits.pack())
        .convert(GasUnits.millibucket, FluidUnits.millibucket).fixed().by(1)
        .build();

converter.quantity(1, GasUnits.bucket).toCross(FluidUnits.millibucket); // 1000 mB
```

由于两个族都注册了基准单位，同种材料的气体和流体数量现在也能通过 `MaterialAmount.equivalentTo` 互相比较。

## 延伸阅读

`src/test/java/dev/vfyjxf/cloudlib/api/unit/` 下的测试套件本身就是可执行的示例：`UnitConverterTest`（构建器语义）、`CrossFamilyTest`（桥接）、`MatterRulesTest` 与 `MaterialAmountTest`（材料）、`ModConventionPacksTest`（TiC/GT 约定包）、`FormatterTest`（文本渲染）。

# Unit Conversion API

> [中文文档](unit-api.zh-CN.md)

CloudLib's unit system converts game units — time, energy, items, fluids — with **lossless exact arithmetic**, even across irregular ratios like 1:9 or 1:144. It supports cross-family bridges (items ↔ fluids), substance-amount comparison across families (`MaterialAmount`), and human-readable text rendering.

Package: `dev.vfyjxf.cloudlib.api.unit` (predefined packs in `.units`, text rendering in `.text`).

## Core concepts

### Ratio — exact arithmetic

Every amount and conversion factor is a `Ratio`: an immutable rational number backed by `BigInteger`, always reduced to lowest terms. Nothing ever rounds unless you ask it to.

```java
Ratio.of(9);          // 9
Ratio.of(1, 9);       // 1/9, for irregular ratios
Ratio.ofDecimal(0.1); // exactly 1/10, not a floating-point approximation
```

Rules that are inherently lossy (e.g. a measured efficiency of 0.9) can be flagged as approximate. The flag is provenance metadata: arithmetic ANDs it, so any conversion chain that touches an approximate rule stays inexact end to end, and formatted output gains a `≈` prefix.

```java
Ratio.approximate(0.9).isExact(); // false
// builder side: .convert(a, b).byApproximate(0.9)
// quantity side: quantity.isExact()
```

### Unit / UnitFamily — phantom-typed families

A `UnitFamily` groups units that measure the same kind of thing. There are two kinds:

- `Kind.measure` — abstract measures (time, energy). Conversions are universal.
- `Kind.matter` — physical matter (items, fluids). Conversions may depend on the material, and the family can have a **base unit** for `MaterialAmount` normalization.

The generic parameter is a phantom marker; use the constants-holder class itself as the marker, so no extra marker type is needed:

```java
public final class TimeUnits {
    public static final UnitFamily<TimeUnits> family = UnitFamily.measure(Namespace.ofMc("time"));
    public static final Unit<TimeUnits> tick = Unit.of(family, Namespace.ofMc("tick"));
}
```

Same-family operations (`Quantity.to`, `add`, `subtract`, `decompose`) are compile-time enforced through this marker.

### Rules and bridges

A conversion graph is built from rules. Endpoints in the same family form a **rule**; endpoints in different families form a **bridge**. Registration is fluent:

```java
UnitConverter.builder()
        .convert(EnergyUnits.eu, EnergyUnits.fe).by(4)                           // 1 eu = 4 fe
        .convert(TimeUnits.second, TimeUnits.tick).fixed().by(20)                // can never be overridden
        .convert(ItemUnits.ingot, ItemUnits.block).forMaterial(iron).by(1, 4)    // material-specific override
        .convert(ItemUnits.ingot, FluidUnits.millibucket).by(144)                // cross-family bridge
        .build();
```

Conflict semantics, per directed pair:

| Situation | Result |
|---|---|
| Same pair re-registered with the same ratio | `RuleConflictException` (duplicate) |
| Same pair re-registered with a different ratio | Overrides a non-fixed rule |
| Either direction is `fixed()` | `RuleConflictException` — fixed rules are untouchable |
| Different `Namespace` material | A separate edge; coexists with the generic rule |

Material-specific rules win over generic ones during resolution. Bulk registration goes through `UnitRule` records and `UnitPack` bundles (see below).

### Resolution

Conversions resolve by BFS shortest path over the rule graph: any chain depth works (block → ingot → nugget → millibucket), rules apply in reverse automatically, material-specific edges are explored before generic ones, and results are memoized per converter.

## Getting started

Build a converter from predefined packs, then convert fluently through `Quantity`:

```java
UnitConverter converter = UnitConverter.builder()
        .add(TimeUnits.pack())
        .add(ItemUnits.pack())
        .add(FluidUnits.pack())
        .add(GtUnits.pack())
        .build();

converter.convert(1, TimeUnits.hour, TimeUnits.tick).value();   // 72000
converter.quantity(1, ItemUnits.block).to(ItemUnits.nugget);    // 81 nuggets
converter.quantity(2, ItemUnits.ingot).toCross(FluidUnits.millibucket); // 288 mB
```

Discrete conversion splits into whole target units plus a remainder in the source unit:

```java
DiscreteResult<ItemUnits> result = converter.quantity(10, ItemUnits.ingot)
        .toDiscrete(ItemUnits.block);
result.amount();    // 1 block
result.remainder(); // 1 ingot
result.exact();     // false — there is a remainder
```

`Quantity.toLongExact()` returns the exact integral value and throws `InexactResultException` when the value isn't integral. All exceptions extend `UnitConversionException`:

- `NoConversionPathException` — no rule chain connects the two units.
- `InexactResultException` — an exact integral result was demanded of a fractional value.
- `RuleConflictException` — duplicate or fixed-rule-violating registration at build time.

## Predefined packs

All packs live in `dev.vfyjxf.cloudlib.api.unit.units` and register with `.add(...pack())`.

**TimeUnits** (`measure`) — all rules fixed:

| Unit | Rule |
|---|---|
| `tick`, `second`, `minute`, `hour` | 1 second = 20 ticks, 1 minute = 60 seconds, 1 hour = 60 minutes |

**EnergyUnits** (`measure`): `eu`, `fe`; 1 eu = 4 fe as a **non-fixed default** you may override.

**ItemUnits** (`matter`, base unit `ingot`):

| Units | Default rules |
|---|---|
| `ingot`, `block`, `nugget`, `dust`, `smallDust`, `tinyDust`, `gem`, `plate`, `gear`, `rod` | 1 ingot = 9 nuggets, 1 block = 9 ingots, 1 dust = 4 smallDust, 1 dust = 9 tinyDust |

`gem` intentionally has **no** generic rules: metal blocks are 9 ingots while gem blocks are 9 gems, so a generic `block`/`ingot` ↔ `gem` rule would wrongly equate ingot and gem transitively. Wire gem conversions per material, or use a convention pack.

**FluidUnits** (`matter`, base unit `millibucket`): `millibucket`, `bucket`, `droplet`; 1 bucket = 1000 mB. `droplet` is definition-only (its size differs per mod).

**TicUnits** — Tinkers' Construct 3 smeltery conventions, all **fixed bridges** to `millibucket`:

| nugget | ingot | block | gem |
|---|---|---|---|
| 10 mB | 90 mB | 810 mB | 100 mB |

**GtUnits** — GregTech conventions (GTCEu / GT Modern). Adds `liter` (GT's display unit, fixed 1:1 with `millibucket`), fixed bridges, and item-form rules:

| nugget | ingot | block | dust | smallDust | tinyDust | plate | rod | gear |
|---|---|---|---|---|---|---|---|---|
| 16 | 144 | 1296 | 144 | 36 | 16 | 144 | 72 | 576 |

Item-form rules: plate = 1 ingot, rod = 1/2 ingot, gear = 4 ingots.

> **`TicUnits` and `GtUnits` are mutually exclusive.** They disagree on the ingot ratio (90 vs 144), and because their bridges are fixed, adding both packs to one converter throws `RuleConflictException` instead of silently mixing conventions. Material-specific bridges (`.forMaterial(iron).by(...)`) live on separate edges and still coexist with the fixed generic bridges.

## MaterialAmount — amount of substance

`MaterialAmount` normalizes any matter quantity into its family's registered **base unit**, tagged with a material. Two amounts of the same material can then be compared across families and units, and converted back to any unit as a hub. Base units come from packs (`ItemUnits.pack()` registers `ingot`, `FluidUnits.pack()` registers `millibucket`) or `builder.baseUnit(unit)` — one per matter family.

```java
Namespace iron = Namespace.ofCommon("iron");

MaterialAmount ingots = converter.quantity(2, ItemUnits.ingot).toMaterialAmount(iron);
MaterialAmount molten = converter.quantity(288, FluidUnits.millibucket).toMaterialAmount(iron);

ingots.equivalentTo(molten);        // true under GtUnits (144 mB/ingot)
molten.to(ItemUnits.nugget);        // 18 nuggets
molten.to(GtUnits.liter);           // 288 L
```

Under `TicUnits` the same 2 ingots are equivalent to 180 mB instead. `equivalentTo` requires equal materials and equal values after conversion — bridges included.

## Text and display

`UnitNames` provides display names. `UnitNames.defaults()` derives them from id paths (`minecraft:millibucket` → `"millibucket"`); a builder supplies custom and material-qualified names:

```java
UnitNames names = UnitNames.builder()
        .name(FluidUnits.millibucket, "mB")
        .name(iron, ItemUnits.ingot, "iron ingot")
        .build();
QuantityFormatter formatter = new QuantityFormatter(converter, names);
// the built-in default: converter.formatter() backed by UnitNames.defaults()
```

`QuantityFormatter`:

- `format(quantity)` → `20 ticks`, `1/9 block`; inexact values get a `≈` prefix.
- `format(materialAmount)` → `2 iron ingots` (material-qualified name).
- `formatDecimal(quantity, precision)` → `≈0.111 block` (integral values stay exact).
- `decompose(quantity, hour, minute, second)` → exact greedy breakdown, e.g. 5000 s → `[1 hour, 23 minutes, 20 seconds]`; zero elements are skipped.
- `formatDecomposed(quantity, hour, minute, second)` → `"1h 23m 20s"`. Short names default to the first letter of the unit name; override per unit with `formatter.withShortName(unit, "min")`.
- `ratioText(ingot, block)` → `"1:9"`.

`UnitComponents` is the Minecraft adapter (load it game-side only). It renders `Component`s whose unit names resolve through translatable lang keys of the form `unit.<root>.<path>` — `minecraft:tick` → `unit.minecraft.tick`:

```java
UnitComponents components = new UnitComponents(converter, names);
components.format(quantity);                        // "20 " + translatable unit name
components.format(materialAmount);                  // value + translatable base-unit name
components.decompose(quantity, hour, minute, second); // "1h 23m 20s" as a Component
```

Add the keys to your lang files, e.g. `"unit.minecraft.tick": "tick"`.

## Custom families and rules

A complete custom matter family — gases — with the holder-class pattern, a base unit, and a bridge to fluids. Rule-carrying collections across the API are Eclipse Collections `ImmutableList`s (built with `Lists.immutable.of(...)`), matching the rest of CloudLib. For consumers working with plain Java collections, every rules collection also exposes an unmodifiable `java.util.List` view: the predefined packs carry it as `rulesAsList`, `UnitPack` offers `rulesAsList()`, and any `ImmutableList` can produce one itself via `castToList()`:

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

Because both families have base units, gas and fluid amounts of the same material now compare through `MaterialAmount.equivalentTo` as well.

## Further reading

The test suite under `src/test/java/dev/vfyjxf/cloudlib/api/unit/` doubles as executable examples: `UnitConverterTest` (builder semantics), `CrossFamilyTest` (bridges), `MatterRulesTest` and `MaterialAmountTest` (materials), `ModConventionPacksTest` (TiC/GT packs), `FormatterTest` (text rendering).

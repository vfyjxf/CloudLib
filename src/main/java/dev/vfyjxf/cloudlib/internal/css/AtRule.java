/* Ported from katana-parser (MIT, (c) 2015 Hackers and Painters) — see LICENSE-katana.txt */
package dev.vfyjxf.cloudlib.internal.css;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * An at-rule {@code @name prelude ;} or {@code @name prelude { block }}, katana's
 * {@code Katana*Rule} family folded into one type.
 *
 * @param name the at-rule name, lowercased
 * @param prelude raw component values before the {@code ;} or block (whitespace preserved)
 * @param block the block contents, interpreted by at-rule name: a {@link RulesBlock} for
 *     grouping rules ({@code media}, {@code supports}, {@code layer}, {@code scope}, …), a
 *     {@link DeclarationsBlock} for declaration rules ({@code font-face}, {@code page},
 *     {@code viewport}, …), or a {@link ValuesBlock} otherwise; {@code null} when the at-rule
 *     has no block
 */
public record AtRule(String name, List<ComponentValue> prelude, @Nullable Block block) implements Rule {

    public AtRule {
        prelude = List.copyOf(prelude);
    }

    /** The block variants of an at-rule, matching how katana types each {@code Katana*Rule}. */
    public sealed interface Block {

        /** A {@code { rule-list }}: grouping at-rules such as {@code @media}/{@code @supports}. */
        record Rules(List<Rule> rules) implements Block {

            public Rules {
                rules = List.copyOf(rules);
            }
        }

        /**
         * A {@code { declaration-list }}: {@code @font-face}, {@code @page}, {@code @viewport} …
         * (keyframe blocks are deliberately kept raw per task spec).
         */
        record Declarations(List<Declaration> declarations) implements Block {

            public Declarations {
                declarations = List.copyOf(declarations);
            }
        }

        /** Uninterpreted component values for every other at-rule block. */
        record Values(List<ComponentValue> values) implements Block {

            public Values {
                values = List.copyOf(values);
            }
        }
    }
}

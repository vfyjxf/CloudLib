package dev.vfyjxf.cloudlib.api.search;

/**
 * Scenario-style field declarations for the search tests: demonstrates the
 * intended pattern — the scenario declares its fields and binds one set; the
 * library provides no registry.
 */
final class TestSearchFields {
    static final SearchField contains = SearchField.of("contains", new SearchFieldSpec(30, false, true, null, SearchFieldSpec.Unit.none, java.util.List.of("has"), true));
    static final SearchField text = SearchField.of("text", new SearchFieldSpec(60, false, false, null, "q", "search"));
    static final SearchField mod = SearchField.of("mod", new SearchFieldSpec(70, false, false, '@', "@mod"));
    static final SearchField tag = SearchField.of("tag", new SearchFieldSpec(80, false, false, '$', "$tag", "tags"));
    static final SearchField tooltip = SearchField.of("tooltip", new SearchFieldSpec(80, false, false, '#', "#tooltip", "tip"));
    static final SearchField name = SearchField.of("name", new SearchFieldSpec(120, false, false, null, "n"));
    static final SearchField id = SearchField.of("id", new SearchFieldSpec(100, false, false, '&', "&id", "registry", "rl"));
    static final SearchField out = SearchField.of("out", new SearchFieldSpec(110, false, true, null, "o", "result", "results"));
    static final SearchField out_id = SearchField.of("out_id", new SearchFieldSpec(110, false, true, null, "out.id", "o.id", "result.id", "results.id"));
    static final SearchField in = SearchField.of("in", new SearchFieldSpec(90, false, true, null, "i", "ingredient", "ingredients"));
    static final SearchField in_id = SearchField.of("in_id", new SearchFieldSpec(90, false, true, null, "in.id", "i.id", "ingredient.id", "ingredients.id"));
    static final SearchField system = SearchField.of("system", new SearchFieldSpec(50, false, false, null, "recipe"));
    static final SearchField db = SearchField.of("db", new SearchFieldSpec(40, false, false, null, "database"));
    static final SearchField machine = SearchField.of("machine", new SearchFieldSpec(50, false, false, null));
    static final SearchField time = SearchField.of("time", new SearchFieldSpec(30, true, false, null, SearchFieldSpec.Unit.time, java.util.List.of()));
    static final SearchField energy = SearchField.of("energy", new SearchFieldSpec(30, true, false, null, SearchFieldSpec.Unit.energy, java.util.List.of()));
    static final SearchField cat = SearchField.of("cat", new SearchFieldSpec(85, false, true, null));
    static final SearchField cat_id = SearchField.of("cat_id", new SearchFieldSpec(85, false, true, null, "cat.id"));
    static final SearchField byproduct = SearchField.of("byproduct", new SearchFieldSpec(30, false, true, null, "bp", "by"));
    static final SearchField byproduct_id = SearchField.of("byproduct_id", new SearchFieldSpec(30, false, true, null, "bp.id", "by.id", "byproduct.id"));
    static final SearchField chance = SearchField.of("chance", new SearchFieldSpec(30, true, false, null, SearchFieldSpec.Unit.chance, java.util.List.of()));
    static final SearchField tier = SearchField.of("tier", new SearchFieldSpec(30, true, false, null));
    static final SearchField level = SearchField.of("level", new SearchFieldSpec(30, true, false, null));
    static final SearchField context = SearchField.of("context", new SearchFieldSpec(40, false, false, '%'));
    static final SearchField plan = SearchField.of("plan", new SearchFieldSpec(40, false, false, null));
    static final SearchField profile = SearchField.of("profile", new SearchFieldSpec(40, false, false, null));

    static final SearchFieldSet SET = SearchFieldSet.of(
        contains, text, mod, tag, tooltip, name, id, out, out_id, in, in_id,
        system, db, machine, time, energy, cat, cat_id, byproduct, byproduct_id,
        chance, tier, level, context, plan, profile);

    static SearchParser parser() {
        return new SearchParser(SET, text);
    }

    static SearchCodecs codecs() {
        return SearchCodecs.of(SET);
    }

    private TestSearchFields() {
    }
}

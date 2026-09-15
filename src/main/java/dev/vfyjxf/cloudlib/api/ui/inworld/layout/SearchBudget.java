package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

/**
 * The solver's effort limits: total node expansions, leader route
 * evaluations, repair recursion depth and route variants kept per panel.
 */
public record SearchBudget(int expansions, int routeEvaluations, int repairDepth, int routeVariants) {

    public SearchBudget {
        if (expansions < 1 || routeEvaluations < 1 || repairDepth < 0 || routeVariants < 1) {
            throw new IllegalArgumentException("invalid search budget");
        }
    }

    public static SearchBudget defaults() {
        return new SearchBudget(16000, 384, 4, 8);
    }
}

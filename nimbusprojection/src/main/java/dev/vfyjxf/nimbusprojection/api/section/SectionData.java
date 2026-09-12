package dev.vfyjxf.nimbusprojection.api.section;

/**
 * One section's serializable snapshot — the data half of a composable
 * container section. Implementations are records holding plain data
 * (item stacks, tank entries, energy numbers); the matching
 * {@link SectionProvider} produces them and the registered codec ships
 * them to the client.
 */
public interface SectionData {

    /** The token identifying this data's kind (and its codec). */
    SectionType<?> type();
}

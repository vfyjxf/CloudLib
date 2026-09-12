package dev.vfyjxf.nimbusprojection.api.section;

/**
 * One resolved section at a block position — id ({@code "type/index"}),
 * kind token, and the data the provider produced.
 */
public record SectionInstance<D extends SectionData>(String id, SectionType<D> type, D data) {}

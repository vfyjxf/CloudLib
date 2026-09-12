/**
 * Declarative panel provisioning: a {@link PanelProvider} is re-evaluated on
 * an interval and offers every panel it wants alive through a
 * {@link PanelSink}; the runtime reconciles offers by spec key so surviving
 * panels keep their widget state.
 */
package dev.vfyjxf.nimbusprojection.api.provider;

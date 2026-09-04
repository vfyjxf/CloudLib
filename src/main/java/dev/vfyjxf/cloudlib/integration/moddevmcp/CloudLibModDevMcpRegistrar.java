package dev.vfyjxf.cloudlib.integration.moddevmcp;

import dev.vfyjxf.cloudlib.integration.moddevmcp.tools.CloudLibToolRegistrar;
import dev.vfyjxf.cloudlib.integration.moddevmcp.ui.CloudLibUiDriver;
import dev.vfyjxf.moddev.api.event.RegisterClientOperationsEvent;
import dev.vfyjxf.moddev.api.registrar.ClientOperationRegistrar;
import dev.vfyjxf.moddev.api.registrar.ClientRegistrar;

@ClientRegistrar
public final class CloudLibModDevMcpRegistrar implements ClientOperationRegistrar {

    @Override
    public void register(RegisterClientOperationsEvent event) {
        event.registerUiDriver(new CloudLibUiDriver());
        CloudLibToolRegistrar.register(event);
    }
}

package dev.vfyjxf.cloudlib.integration.moddevmcp.tools;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CloudLibToolRegistrarTest {

    @Test
    void toolSchemasRegister() {
        assertTrue(CloudLibToolRegistrar.schemas().containsKey("cloudlib.ui.tree"));
    }
}

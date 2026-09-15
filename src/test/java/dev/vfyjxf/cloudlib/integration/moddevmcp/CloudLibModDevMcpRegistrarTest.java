package dev.vfyjxf.cloudlib.integration.moddevmcp;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class CloudLibModDevMcpRegistrarTest {

    @Test
    void registrarClassLoads() throws Exception {
        assertNotNull(Class.forName("dev.vfyjxf.cloudlib.integration.moddevmcp.CloudLibModDevMcpRegistrar"));
    }
}

package dev.vfyjxf.cloudlib.gradle.style;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Guards the contract between {@link CloudStylePlugin} and the JDT profile it
 * packages: the keys that wrap a parameter list one element per line, with the
 * parentheses on their own lines once the list no longer fits, and the
 * line-width/indentation keys the rest of the profile is tuned around.
 * <p>
 * Spotless hands this XML to the Eclipse formatter as-is, so a profile that
 * keeps these settings renders
 *
 * <pre>
 * void call(
 *     TypeA a,
 *     TypeB b
 * ) {
 * </pre>
 *
 * The rendered result itself is asserted by the consumer projects'
 * {@code spotlessCheck}, which runs the real formatter.
 */
class CloudStyleProfileTest {

    private static final String PROFILE = "/dev/vfyjxf/cloudlib/gradle/style/cloud-jdt-formatter.xml";

    private static final String PREFIX = "org.eclipse.jdt.core.formatter.";

    /** {@code M_ONE_PER_LINE_SPLIT | M_INDENT_BY_ONE}: one element per line, one indent unit deep. */
    private static final String ONE_PER_LINE_INDENT_BY_ONE = "52";

    @Test
    void breaksParameterListsOneElementPerLine() {
        Map<String, String> profile = profile();
        for (String key : List.of(
                "parameters_in_method_declaration",
                "parameters_in_constructor_declaration",
                "arguments_in_method_invocation",
                "arguments_in_allocation_expression",
                "arguments_in_explicit_constructor_call",
                "arguments_in_qualified_allocation_expression",
                "arguments_in_enum_constant",
                "arguments_in_annotation",
                "record_components")) {
            assertEquals(
                    ONE_PER_LINE_INDENT_BY_ONE,
                    profile.get(PREFIX + "alignment_for_" + key),
                    "alignment_for_" + key);
        }
    }

    @Test
    void putsWrappedParenthesesOnTheirOwnLines() {
        Map<String, String> profile = profile();
        for (String key : List.of(
                "parentheses_positions_in_method_delcaration",
                "parentheses_positions_in_method_invocation",
                "parentheses_positions_in_record_declaration")) {
            assertEquals("separate_lines_if_wrapped", profile.get(PREFIX + key), key);
        }
    }

    @Test
    void keepsShortListsAndEmptyBodiesOnOneLine() {
        Map<String, String> profile = profile();
        assertEquals("one_line_if_empty", profile.get(PREFIX + "keep_method_body_on_one_line"));
        assertEquals("one_line_if_empty", profile.get(PREFIX + "keep_type_declaration_on_one_line"));
        assertEquals("one_line_if_empty", profile.get(PREFIX + "keep_record_declaration_on_one_line"));
        assertEquals("true", profile.get(PREFIX + "keep_imple_if_on_one_line"));
        assertEquals("true", profile.get(PREFIX + "keep_then_statement_on_same_line"));
    }

    @Test
    void matchesTheCloudLibLineAndIndentationSettings() {
        Map<String, String> profile = profile();
        assertEquals("120", profile.get(PREFIX + "lineSplit"));
        assertEquals("space", profile.get(PREFIX + "tabulation.char"));
        assertEquals("4", profile.get(PREFIX + "tabulation.size"));
        assertEquals("4", profile.get(PREFIX + "indentation.size"));
        assertEquals("2", profile.get(PREFIX + "continuation_indentation"));
        assertEquals("end_of_line", profile.get(PREFIX + "brace_position_for_method_declaration"));
        assertEquals("false", profile.get(PREFIX + "insert_new_line_before_else_in_if_statement"));
    }

    /**
     * The pre-4.13 JDT keys for breaking parameter lists no longer exist in the
     * formatter and would be silently ignored, so the profile has to carry the
     * modern parenthesis-position keys instead.
     */
    @Test
    void usesTheModernParenthesisKeys() {
        Map<String, String> profile = profile();
        assertFalse(profile.keySet().stream().anyMatch(key -> key.contains("new_line_after_opening_paren")));
        assertFalse(profile.keySet().stream().anyMatch(key -> key.contains("new_line_before_closing_paren")));
    }

    private static Map<String, String> profile() {
        try (InputStream in = CloudStylePlugin.class.getResourceAsStream(PROFILE)) {
            assertNotNull(in, "the profile must be packaged on the plugin's classpath");
            Element root = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(in)
                    .getDocumentElement();
            assertEquals("profiles", root.getNodeName());
            Map<String, String> settings = new LinkedHashMap<>();
            for (Element profile : children(root, "profile")) {
                for (Element setting : children(profile, "setting")) {
                    settings.put(setting.getAttribute("id"), setting.getAttribute("value"));
                }
            }
            assertTrue(settings.size() > 300, "the profile carries the full JDT setting set");
            return settings;
        } catch (AssertionError e) {
            throw e;
        } catch (Exception e) {
            throw new AssertionError("failed to read " + PROFILE, e);
        }
    }

    private static List<Element> children(Element parent, String name) {
        NodeList nodes = parent.getChildNodes();
        List<Element> found = new ArrayList<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE && name.equals(node.getNodeName())) {
                found.add((Element) node);
            }
        }
        return found;
    }

}

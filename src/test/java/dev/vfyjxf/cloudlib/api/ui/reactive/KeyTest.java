package dev.vfyjxf.cloudlib.api.ui.reactive;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the Key system.
 */
@DisplayName("Key System")
class KeyTest {

    @Nested
    @DisplayName("ValueKey")
    class ValueKeyTests {
        
        @Test
        @DisplayName("equal values produce equal keys")
        void equalValues() {
            var key1 = Key.of("user-123");
            var key2 = Key.of("user-123");
            
            assertEquals(key1, key2);
            assertEquals(key1.hashCode(), key2.hashCode());
        }
        
        @Test
        @DisplayName("different values produce different keys")
        void differentValues() {
            var key1 = Key.of("user-123");
            var key2 = Key.of("user-456");
            
            assertNotEquals(key1, key2);
        }
        
        @Test
        @DisplayName("works with different types")
        void differentTypes() {
            var stringKey = Key.of("test");
            var intKey = Key.of(42);
            var boolKey = Key.of(true);
            
            assertNotEquals(stringKey, intKey);
            assertNotEquals(intKey, boolKey);
            
            // Same value, same key
            assertEquals(Key.of(42), Key.of(42));
        }
    }
    
    @Nested
    @DisplayName("ObjectKey")
    class ObjectKeyTests {
        
        @Test
        @DisplayName("same object produces equal keys")
        void sameObject() {
            var obj = new Object();
            var key1 = Key.object(obj);
            var key2 = Key.object(obj);
            
            assertEquals(key1, key2);
            assertEquals(key1.hashCode(), key2.hashCode());
        }
        
        @Test
        @DisplayName("different objects produce different keys even if equal")
        void differentObjects() {
            var str1 = new String("test"); // Intentionally not interned
            var str2 = new String("test");
            
            // String values are equal
            assertEquals(str1, str2);
            
            // But ObjectKeys are different (reference equality)
            var key1 = Key.object(str1);
            var key2 = Key.object(str2);
            assertNotEquals(key1, key2);
        }
    }
    
    @Nested
    @DisplayName("UniqueKey")
    class UniqueKeyTests {
        
        @Test
        @DisplayName("each call creates a unique key")
        void alwaysUnique() {
            var key1 = Key.unique();
            var key2 = Key.unique();
            var key3 = Key.unique();
            
            assertNotEquals(key1, key2);
            assertNotEquals(key2, key3);
            assertNotEquals(key1, key3);
        }
        
        @Test
        @DisplayName("same instance is equal to itself")
        void equalToSelf() {
            var key = Key.unique();
            assertEquals(key, key);
        }
    }
    
    @Nested
    @DisplayName("CompositeKey")
    class CompositeKeyTests {
        
        @Test
        @DisplayName("same parts produce equal keys")
        void equalParts() {
            var key1 = Key.of("section", "row", 1);
            var key2 = Key.of("section", "row", 1);
            
            assertEquals(key1, key2);
            assertEquals(key1.hashCode(), key2.hashCode());
        }
        
        @Test
        @DisplayName("different parts produce different keys")
        void differentParts() {
            var key1 = Key.of("section", "row", 1);
            var key2 = Key.of("section", "row", 2);
            
            assertNotEquals(key1, key2);
        }
        
        @Test
        @DisplayName("order matters")
        void orderMatters() {
            var key1 = Key.of("a", "b");
            var key2 = Key.of("b", "a");
            
            assertNotEquals(key1, key2);
        }
        
        @Test
        @DisplayName("single value returns ValueKey")
        void singleValueIsValueKey() {
            var key = Key.of("single");
            assertTrue(key instanceof Key.ValueKey<?>);
        }
        
        @Test
        @DisplayName("multiple values return CompositeKey")
        void multipleValuesIsCompositeKey() {
            var key = Key.of("a", "b");
            assertTrue(key instanceof Key.CompositeKey);
        }
    }
    
    @Nested
    @DisplayName("KeyScope")
    class KeyScopeTests {
        
        @Test
        @DisplayName("generates sequential AutoKeys")
        void sequentialKeys() {
            var scope = Key.scope("buttons");
            
            var key0 = scope.next();
            var key1 = scope.next();
            var key2 = scope.next();
            
            // Now generates AutoKey instead of CompositeKey
            assertEquals(new Key.AutoKey("buttons", 0), key0);
            assertEquals(new Key.AutoKey("buttons", 1), key1);
            assertEquals(new Key.AutoKey("buttons", 2), key2);
        }
        
        @Test
        @DisplayName("reset restarts counter")
        void resetCounter() {
            var scope = Key.scope("items");
            
            scope.next(); // 0
            scope.next(); // 1
            scope.reset();
            
            assertEquals(new Key.AutoKey("items", 0), scope.next());
        }
        
        @Test
        @DisplayName("with creates named keys")
        void withNamedKeys() {
            var scope = Key.scope("section");
            
            var header = scope.with("header");
            var content = scope.with("content");
            var footer = scope.with("footer");
            
            assertEquals(Key.of("section", "header"), header);
            assertEquals(Key.of("section", "content"), content);
            assertEquals(Key.of("section", "footer"), footer);
        }
        
        @Test
        @DisplayName("child creates nested scopes with path")
        void childScopes() {
            var parent = Key.scope("list");
            var child = parent.child("item");
            
            var key0 = child.next();
            var key1 = child.next();
            
            // AutoKey with nested path: list/item
            assertEquals(new Key.AutoKey("list/item", 0), key0);
            assertEquals(new Key.AutoKey("list/item", 1), key1);
        }
        
        @Test
        @DisplayName("child scopes are cached")
        void childScopesCached() {
            var parent = Key.scope("list");
            var child1 = parent.child("item");
            var child2 = parent.child("item");
            
            assertSame(child1, child2);
        }
        
        @Test
        @DisplayName("resetAll resets all children")
        void resetAllChildren() {
            var parent = Key.scope("list");
            var child = parent.child("item");
            
            parent.next(); // list/0
            child.next();  // list/item/0
            child.next();  // list/item/1
            
            parent.resetAll();
            
            assertEquals(new Key.AutoKey("list", 0), parent.next());
            assertEquals(new Key.AutoKey("list/item", 0), child.next());
        }
    }
    
    @Nested
    @DisplayName("AutoKey and withScope")
    class AutoKeyTests {
        
        @BeforeEach
        void setUp() {
            Key.resetScope();
            // Clear ThreadLocal
            Key.CURRENT_SCOPE.remove();
        }
        
        @Test
        @DisplayName("auto() generates sequential keys in current scope")
        void autoGeneratesSequential() {
            Key.withScope("test", () -> {
                var key0 = Key.auto();
                var key1 = Key.auto();
                var key2 = Key.auto();
                
                assertEquals(new Key.AutoKey("test", 0), key0);
                assertEquals(new Key.AutoKey("test", 1), key1);
                assertEquals(new Key.AutoKey("test", 2), key2);
            });
        }
        
        @Test
        @DisplayName("auto(hint) includes hint in key")
        void autoWithHint() {
            Key.withScope("buttons", () -> {
                var key = Key.auto("submit");
                
                assertEquals(new Key.AutoKey("buttons", 0, "submit"), key);
                assertTrue(key.toString().contains("submit"));
            });
        }
        
        @Test
        @DisplayName("nested withScope creates hierarchical paths")
        void nestedScopes() {
            Key.withScope("outer", () -> {
                var outerKey = Key.auto();
                
                Key.withScope("inner", () -> {
                    var innerKey = Key.auto();
                    assertEquals(new Key.AutoKey("outer/inner", 0), innerKey);
                });
                
                assertEquals(new Key.AutoKey("outer", 0), outerKey);
            });
        }
        
        @Test
        @DisplayName("withScope returns value from supplier")
        void withScopeReturnsValue() {
            String result = Key.withScope("test", () -> {
                Key.auto(); // consume one
                return "result";
            });
            
            assertEquals("result", result);
        }
        
        @Test
        @DisplayName("scopes are isolated")
        void scopesIsolated() {
            Key.withScope("scope1", () -> {
                Key.auto(); // scope1/0
                Key.auto(); // scope1/1
            });
            
            Key.withScope("scope2", () -> {
                var key = Key.auto();
                assertEquals(new Key.AutoKey("scope2", 0), key);
            });
        }
    }
    
    @Nested
    @DisplayName("toString")
    class ToStringTests {
        
        @Test
        @DisplayName("ValueKey toString")
        void valueKeyToString() {
            assertEquals("Key(test)", Key.of("test").toString());
            assertEquals("Key(42)", Key.of(42).toString());
        }
        
        @Test
        @DisplayName("CompositeKey toString")
        void compositeKeyToString() {
            assertEquals("Key(a/b)", Key.of("a", "b").toString());
            assertEquals("Key(section/row/1)", Key.of("section", "row", 1).toString());
        }
        
        @Test
        @DisplayName("UniqueKey toString contains id")
        void uniqueKeyToString() {
            var key = Key.unique();
            assertTrue(key.toString().startsWith("UniqueKey#"));
        }
        
        @Test
        @DisplayName("ObjectKey toString contains hash")
        void objectKeyToString() {
            var key = Key.object(new Object());
            assertTrue(key.toString().startsWith("ObjectKey@"));
        }
        
        @Test
        @DisplayName("AutoKey toString shows path and index")
        void autoKeyToString() {
            var key = new Key.AutoKey("buttons", 5);
            assertEquals("AutoKey(buttons/5)", key.toString());
        }
        
        @Test
        @DisplayName("AutoKey toString with hint")
        void autoKeyWithHintToString() {
            var key = new Key.AutoKey("buttons", 5, "submit");
            assertEquals("AutoKey(buttons/5:submit)", key.toString());
        }
    }
}

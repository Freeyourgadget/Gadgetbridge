package com.google.gson.typeadapters;

import static org.junit.Assert.*;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.junit.Test;

/**
 * Test our changes to the RuntimeTypeAdapterFactory, which allow for serialization and deserialization
 * of a null type label.
 */
public class RuntimeTypeAdapterFactoryTest {
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapterFactory(RuntimeTypeAdapterFactory
                    .of(Parent.class, "type")
                    .registerSubtype(Child1.class, null)
                    .registerSubtype(Child2.class, "child2")
            )
            .create();

    @Test
    public void testNullLabelSerialize() {
        final Child1 child1 = new Child1("hello1");
        final Child2 child2 = new Child2("hello2");

        assertEquals("{\"field1\":\"hello1\",\"fieldParent\":\"fromChild1\"}", GSON.toJson(child1, Parent.class));
        assertEquals("{\"type\":\"child2\",\"field2\":\"hello2\",\"fieldParent\":\"fromChild2\"}", GSON.toJson(child2, Parent.class));
    }

    @Test
    public void testNullLabelDeserialize() {
        final Parent child1 = GSON.fromJson("{\"field1\":\"hello1\",\"fieldParent\":\"fromChild1\"}", Parent.class);
        assertTrue(child1 instanceof Child1);
        assertEquals("fromChild1", child1.fieldParent);
        assertEquals("hello1", ((Child1) child1).field1);

        final Parent child2 = GSON.fromJson("{\"type\":\"child2\",\"field2\":\"hello2\",\"fieldParent\":\"fromChild2\"}", Parent.class);
        assertTrue(child2 instanceof Child2);
        assertEquals("fromChild2", child2.fieldParent);
        assertEquals("hello2", ((Child2) child2).field2);
    }

    private static class Parent {
        private final String fieldParent;

        private Parent(final String fieldParent) {
            this.fieldParent = fieldParent;
        }
    }

    private static class Child1 extends Parent {
        private final String field1;

        private Child1(final String field1) {
            super("fromChild1");
            this.field1 = field1;
        }
    }

    private static class Child2 extends Parent {
        private final String field2;

        private Child2(final String field2) {
            super("fromChild2");
            this.field2 = field2;
        }
    }
}

package mbot;

import mbot.internal.Json;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonTest {

    @Test
    @SuppressWarnings("unchecked")
    void roundTrip() {
        String text = "{\"a\":[1,2.5,-3e2],\"b\":\"q\\\"\\n\",\"c\":true,\"d\":null,\"e\":{}}";
        Map<String, Object> m = (Map<String, Object>) Json.parse(text);
        assertEquals(List.of(1.0, 2.5, -300.0), m.get("a"));
        assertEquals("q\"\n", m.get("b"));
        assertEquals(Boolean.TRUE, m.get("c"));
        // nulls are dropped on write; numbers lose a trailing .0
        assertEquals("{\"a\":[1,2.5,-300],\"b\":\"q\\\"\\n\",\"c\":true,\"e\":{}}", Json.write(m));
    }
}

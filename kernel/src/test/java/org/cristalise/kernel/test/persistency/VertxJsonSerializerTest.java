/**
 * This file is part of the CRISTAL-iSE kernel.
 * Copyright (c) 2001-2015 The CRISTAL Consortium. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 3 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; with out even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 *
 * http://www.fsf.org/licensing/licenses/lgpl.html
 */
package org.cristalise.kernel.test.persistency;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.utils.VertxJsonMarshaller;
import io.vertx.core.json.JsonObject;
import org.junit.Test;

public class VertxJsonSerializerTest {

    private final VertxJsonMarshaller serializer = new VertxJsonMarshaller();

    @Test
    public void testUntypedRoundTripJsonObject() throws Exception {
        JsonObject original = new JsonObject()
            .put("name", "alice")
            .put("count", 3)
            .put("active", true);

        String json = serializer.marshall(original);
        Object roundTrip = serializer.unmarshall(json);

        assertTrue(roundTrip instanceof JsonObject);
        assertEquals(original, roundTrip);
    }

    @Test
    public void testTypedUnmarshall() throws Exception {
        SampleData original = new SampleData("alice", 3, true);

        String json = serializer.marshall(original);
        SampleData roundTrip = serializer.unmarshall(json, SampleData.class);

        assertEquals(original.getName(), roundTrip.getName());
        assertEquals(original.getCount(), roundTrip.getCount());
        assertEquals(original.isActive(), roundTrip.isActive());
    }

    @Test
    public void testNullContract() throws Exception {
        assertEquals("null", serializer.marshall(null));
        assertNull(serializer.unmarshall("null"));
        assertNull(serializer.unmarshall("  null  "));
        assertNull(serializer.unmarshall("null", SampleData.class));
    }

    @Test
    public void testUnmarshallInvalidJson() {
        assertThrows(InvalidDataException.class, () -> serializer.unmarshall("{not-json"));
    }

    @Test
    public void testUnmarshallNullInput() {
        assertThrows(InvalidDataException.class, () -> serializer.unmarshall(null));
    }

    @Test
    public void testTypedUnmarshallNullInput() {
        assertThrows(InvalidDataException.class, () -> serializer.unmarshall(null, SampleData.class));
    }

    public static class SampleData {
        private String name;
        private int count;
        private boolean active;

        public SampleData() { }

        public SampleData(String name, int count, boolean active) {
            this.name = name;
            this.count = count;
            this.active = active;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }
    }
}

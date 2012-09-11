/*
 * This file is part of NodeBox.
 *
 * Copyright (C) 2008 Frederik De Bleser (frederik@pandora.be)
 *
 * NodeBox is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * NodeBox is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with NodeBox. If not, see <http://www.gnu.org/licenses/>.
 */
package nodebox.graphics;

import org.junit.Test;

import static junit.framework.Assert.*;

public class RectTest {

    @Test
    public void testEmpty() {
        assertFalse(new Rect(100, 100, 100, 100).isEmpty());
        assertFalse(new Rect(1, 1, 1, 1).isEmpty());
        assertFalse(new Rect(0, 0, 1, 1).isEmpty());
        assertFalse(new Rect(0, 0, -100, -200).isEmpty());

        assertTrue(new Rect(0, 0, 0, 0).isEmpty());
        assertTrue(new Rect(-10, 0, 0, 10).isEmpty());
        assertTrue(new Rect(-10, 0, 200, 0).isEmpty());
        assertTrue(new Rect(20, 30, 10, 0).isEmpty());
    }

    @Test
    public void testIntersects() {
        assertFalse(new Rect(0, 0, 20, 20).intersects(new Rect(100, 100, 20, 20)));
        assertTrue(new Rect(0, 0, 20, 20).intersects(new Rect(0, 0, 20, 20)));
    }

    @Test
    public void testUnited() {
        Rect r1 = new Rect(10, 20, 30, 40);
        Rect r2 = new Rect(40, 30, 50, 30);
        assertEquals(new Rect(10, 20, 40 + 50 - 10, 30 + 30 - 20), r1.united(r2));
        Rect r3 = new Rect(10, 20, 30, 40);
        Rect r4 = new Rect(10, 120, 30, 40);
        assertEquals(new Rect(10, 20, 30, 120 + 40 - 20), r3.united(r4));
    }

    @Test
    public void testContains() {
        Rect r = new Rect(10, 20, 30, 40);
        assertTrue(r.contains(new Point(10, 20)));
        assertTrue(r.contains(new Point(11, 22)));
        assertTrue(r.contains(new Point(40, 60)));
        assertFalse(r.contains(new Point(0, 0)));
        assertFalse(r.contains(new Point(-11, -22)));
        assertFalse(r.contains(new Point(100, 200)));
        assertFalse(r.contains(new Point(15, 200)));
        assertFalse(r.contains(new Point(200, 25)));

        assertTrue(r.contains(new Rect(10, 20, 30, 40)));
        assertTrue(r.contains(new Rect(15, 25, 5, 5)));
        assertFalse(r.contains(new Rect(15, 25, 30, 40)));
        assertFalse(r.contains(new Rect(1, 2, 3, 4)));
        assertFalse(r.contains(new Rect(15, 25, 300, 400)));
        assertFalse(r.contains(new Rect(15, 25, 5, 400)));
        assertFalse(r.contains(new Rect(15, 25, 400, 5)));
    }

}

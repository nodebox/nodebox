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
package net.nodebox.graphics;

import junit.framework.TestCase;

public class BezierPathTest extends TestCase {

    public void testSize() {
        BezierPath p = new BezierPath();
        assertEquals(0, p.size());
        p.moveto(0, 0);
        assertEquals(1, p.size());
        p.lineto(100, 100);
        assertEquals(2, p.size());
        p.clear();
        assertEquals(0, p.size());
    }

    public void testClone() {
        BezierPath p1 = new BezierPath();
        p1.moveto(0, 0);
        p1.lineto(100, 100);
        assertEquals(2, p1.size());
        BezierPath p2 = (BezierPath) p1.clone();
        assertEquals(2, p2.size());
        p1.clear();
        assertEquals(0, p1.size());
        assertEquals(2, p2.size());
    }

    public void testElements() {
        BezierPath p = new BezierPath();
        p.moveto(1, 2);
        p.lineto(3, 4);
        p.close();
        assertEquals(3, p.size());
        assertEquals(new PathElement(PathElement.MOVETO, 1, 2), p.getElementAt(0));
        assertEquals(new PathElement(PathElement.LINETO, 3, 4), p.getElementAt(1));
        assertEquals(new PathElement(PathElement.CLOSE), p.getElementAt(2));
    }

    public void testEquality() {
        BezierPath p1 = new BezierPath();
        assertEquals(p1, p1);
        BezierPath p2 = new BezierPath();
        assertEquals(p1, p2);
        BezierPath p3 = new BezierPath();
        p1.moveto(1, 2);
        p2.moveto(1, 2);
        assertEquals(p1, p2);
        assertNotSame(p1, p3);
        p1.lineto(3, 4);
        assertNotSame(p1, p2);
        assertNotSame(p1, p3);
    }

    public void testBounds() {
        BezierPath p1 = new BezierPath();
        p1.addRect(20, 30, 40, 50);
        assertEquals(new Rect(20, 30, 40, 50), p1.getBounds());
        BezierPath p2 = new BezierPath();
        p2.addRect(60, 70, 80, 90);
        assertEquals(new Rect(60, 70, 80, 90), p2.getBounds());
    }
}

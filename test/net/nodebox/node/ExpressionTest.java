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
package net.nodebox.node;

import junit.framework.TestCase;

public class ExpressionTest extends TestCase {

    public void testSimple() {
        Node n = new Node(Parameter.TYPE_INT);
        Parameter p1 = n.addParameter("p1", Parameter.TYPE_INT);
        Expression e = new Expression(p1, "1 + 2");
        assertEquals(3, e.asInt());
    }
}

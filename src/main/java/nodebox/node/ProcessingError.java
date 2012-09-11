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
package nodebox.node;

/**
 * @author Frederik
 */
public class ProcessingError extends RuntimeException {

    private Node node;

    public ProcessingError(Node node) {
        this.node = node;
    }

    public ProcessingError(Node node, String message) {
        super(message);
        this.node = node;
    }

    public ProcessingError(Node node, Throwable cause) {
        super(cause);
        this.node = node;
    }

    public ProcessingError(Node node, String message, Throwable cause) {
        super(message, cause);
        this.node = node;
    }

    public Node getNode() {
        return node;
    }
}

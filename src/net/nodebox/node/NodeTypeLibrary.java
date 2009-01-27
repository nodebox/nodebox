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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

public abstract class NodeTypeLibrary {


    private static Logger logger = Logger.getLogger("net.nodebox.node.NodeTypeLibrary");

    private String name;
    private HashMap<String, NodeType> types = new HashMap<String, NodeType>();
    private Version version;

    public NodeTypeLibrary(String name, Version version) {
        this.name = name;
        this.version = version.clone();
    }

    public String getName() {
        return name;
    }

    public Version getVersion() {
        return version;
    }

    public String versionAsString() {
        return version.toString();
    }

    /**
     * Returns the canonical path for this library.
     *
     * @return the canonical path for this library.
     */
    public String getPath() {
        return null;
    }

    //// Types collection ////

    protected void addNodeType(NodeType nodeType) {
        types.put(nodeType.getName(), nodeType);
    }


    //// Library loading ////

    public boolean isLoaded() {
        // Default implementation always returns true
        return true;
    }

    public void load() {
        // Default implementation does nothing
    }

    public boolean reload() {
        // Default implementation does nothing
        return false;
    }

    //// Node info ////

    public boolean hasNodeType(String nodeName) {
        if (!isLoaded()) load();
        return types.containsKey(nodeName);
    }

    public NodeType getNodeType(String nodeName) throws NotFoundException {
        if (!isLoaded()) load();
        if (types.containsKey(nodeName)) {
            return types.get(nodeName);
        } else {
            throw new NotFoundException(this, nodeName, "Node type " + nodeName + " not found in library " + getName());
        }
    }

    public List<NodeType> getNodeTypes() {
        if (!isLoaded()) load();
        return new ArrayList<NodeType>(types.values());
    }

}

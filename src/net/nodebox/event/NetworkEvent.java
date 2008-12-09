package net.nodebox.event;

import net.nodebox.node.Network;
import net.nodebox.node.Node;
import net.nodebox.node.Parameter;

import java.util.EventObject;


/**
 * An event that is triggered when a change in the networks' state was detected.
 *
 * @see NetworkListener
 */
public class NetworkEvent extends EventObject {

    public String command;

    public Node node;
    public Node oldNode;
    public Parameter input;
    public Parameter output;
    public String oldName;
    public String newName;

    /**
     * Constructs a NetworkEvent object.
     *
     * @param source the Network that is the source of the event
     *               (typically <code>this</code>)
     */
    public NetworkEvent(Network source) {
        super(source);
    }

    /**
     * Constructs a NetworkEvent object.
     *
     * @param source the Network that is the source of the event
     *               (typically <code>this</code>)
     * @param node   the node involved in the event
     */
    public NetworkEvent(Network source, Node node) {
        super(source);
        this.node = node;
    }

    /**
     * Constructs a NetworkEvent object.
     *
     * @param source the Network that is the source of the event
     *               (typically <code>this</code>)
     * @param input  the input parameter
     * @param output the output parameter
     */
    public NetworkEvent(Network source, Parameter input, Parameter output) {
        super(source);
        this.input = input;
        this.output = output;
    }

    /**
     * Constructs a NetworkEvent object.
     *
     * @param source  the Network that is the source of the event
     *                (typically <code>this</code>)
     * @param node    the node that was renamed
     * @param oldName the old name
     * @param newName the new name
     */
    public NetworkEvent(Network source, Node node, String oldName, String newName) {
        super(source);
        this.node = node;
        this.oldName = oldName;
        this.newName = newName;
    }

    /**
     * Constructs a NetworkEvent object.
     *
     * @param source  the Network that is the source of the event
     *                (typically <code>this</code>)
     * @param oldNode the old node involved in the event
     * @param newNode the new node involved in the event
     */
    public NetworkEvent(Network source, Node oldNode, Node newNode) {
        super(source);
        this.oldNode = oldNode;
        this.node = newNode;
    }


}


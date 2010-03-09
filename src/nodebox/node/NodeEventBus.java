package nodebox.node;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class NodeEventBus {

    public List<WeakReference<NodeEventListener>> listeners = new CopyOnWriteArrayList<WeakReference<NodeEventListener>>();

    public void addListener(NodeEventListener l) {
        listeners.add(new WeakReference<NodeEventListener>(l));
    }

    public boolean removeListener(NodeEventListener l) {
        Iterator<WeakReference<NodeEventListener>> it = listeners.iterator();
        WeakReference<NodeEventListener> toRemove = null;
        while (it.hasNext()) {
            WeakReference<NodeEventListener> ref = it.next();
            if (l == ref.get()) {
                toRemove = ref;
                break;
            }
        }
        if (toRemove != null) {
            listeners.remove(toRemove);
            return true;
        }
        return false;
    }

    public List<NodeEventListener> getListeners() {
        List<NodeEventListener> ll = new ArrayList<NodeEventListener>();
        for (WeakReference<NodeEventListener> ref : listeners) {
            NodeEventListener l = ref.get();
            if (l != null)
                ll.add(l);
        }
        return ll;
    }

    public void send(NodeEvent event) {
        for (WeakReference<NodeEventListener> ref : listeners) {
            NodeEventListener l = ref.get();
            if (l != null)
                l.receive(event);
        }
    }
}

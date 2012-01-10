package nodebox.node;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class Dispatcher {

    public static class Receiver {
        private Object object;
        private Method method;
        private String signal;
        private Object sender;

        public Receiver(Object object, Method method, String signal, Object sender) {
            this.object = object;
            this.method = method;
            this.signal = signal;
            this.sender = sender;
        }

        public Object getObject() {
            return object;
        }

        public Method getMethod() {
            return method;
        }

        public String getSignal() {
            return signal;
        }

        public Object getSender() {
            return sender;
        }

        public void send() {
            try {
                method.invoke(object);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    //// Instance attributes ////

    private static List<Receiver> receivers = new ArrayList<Receiver>();

    public static void removeAllReceivers() {
        receivers.clear();
    }

    public static boolean connect(Object object, String methodName, String signal) {
        return connect(object, methodName, signal, null);
    }

    public static boolean connect(Object object, String methodName, String signal, Object sender) {
        Method m;
        try {
            m = object.getClass().getMethod(methodName);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            return false;
        }
        return connect(object, m, signal, sender);
    }

    public static boolean connect(Object object, Method method, String signal) {
        return connect(object, method, signal, null);
    }

    public static boolean connect(Object object, Method method, String signal, Object sender) {
        Receiver r = new Receiver(object, method, signal, sender);
        if (getReceiver(object, method, signal, sender) != null) return false;
        return receivers.add(r);
    }

    public static boolean disconnect(Object object, String methodName, String signal) {
        return disconnect(object, methodName, signal, null);
    }

    public static boolean disconnect(Object object, String methodName, String signal, Object sender) {
        Method m;
        try {
            m = object.getClass().getMethod(methodName);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            return false;
        }
        return disconnect(object, m, signal, sender);
    }

    public static boolean disconnect(Object object, Method method, String signal) {
        return disconnect(object, method, signal, null);
    }

    public static boolean disconnect(Object object, Method method, String signal, Object sender) {
        Receiver r = getReceiver(object, method, signal, sender);
        if (r == null) return false;
        return receivers.remove(r);
    }

    public static Receiver getReceiver(Object object, Method method, String signal, Object sender) {
        for (Receiver r : receivers) {
            if (r.getObject() == object
                    && r.method.equals(method)
                    && r.getSignal().equals(signal)
                    && (r.getSender() == null || r.getSender() == sender))
                return r;
        }
        return null;
    }


    public static List<Receiver> getReceivers(String signal) {
        return getReceivers(signal, null);
    }

    public static List<Receiver> getReceivers(String signal, Object sender) {
        List<Receiver> tmp = new ArrayList<Receiver>();
        for (Receiver r : receivers) {
            if (r.getSignal().equals(signal) && (r.getSender() == null || r.getSender() == sender)) {
                tmp.add(r);
            }
        }
        return tmp;
    }

    public static void send(String signal) {
        List<Receiver> tmp = getReceivers(signal);
        for (Receiver r : tmp) {
            r.send();
        }
    }

    public static void send(String signal, Object sender) {
        List<Receiver> tmp = getReceivers(signal, sender);
        for (Receiver r : tmp) {
            r.send();
        }
    }

}

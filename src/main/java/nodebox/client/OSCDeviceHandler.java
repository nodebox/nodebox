package nodebox.client;

import com.google.common.collect.ImmutableList;
import oscP5.OscEventListener;
import oscP5.OscMessage;
import oscP5.OscP5;
import oscP5.OscStatus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OSCDeviceHandler implements DeviceHandler {

    private String name;
    private OscP5 oscP5;
    private int oscPort;
    private Map<String, List<Object>> oscMessages = new HashMap<String, List<Object>>();

    public OSCDeviceHandler(String name) {
        this(name, -1);
    }

    public OSCDeviceHandler(String name, int oscPort) {
        this.name = name;
        this.oscPort = oscPort;
        oscP5 = null;
        oscMessages.clear();
    }

    public String getName() {
        return name;
    }

    public int getPort() {
        return oscPort;
    }

    public Map<String, List<Object>> getOscMessages() {
        return oscMessages;
    }

    private static int randomOSCPort() {
        return 1024 + (int) Math.round(Math.random() * 10000);
    }

    public void start() {
        start(oscPort);
    }

    public void start(int port) {
        if (oscP5 != null)
            stop();
        oscPort = port;
        oscMessages.clear();
        oscP5 = new OscP5(new Object(), port);
        oscP5.addListener(new OscEventListener() {
            @Override
            public void oscEvent(OscMessage m) {
                ImmutableList<Object> arguments = ImmutableList.copyOf(m.arguments());
                oscMessages.put(m.addrPattern(), arguments);
            }

            @Override
            public void oscStatus(OscStatus ignored) {
            }
        });
    }

    public void stop() {
        if (oscP5 != null)
            oscP5.stop();
        oscP5 = null;
        oscPort = -1;
        oscMessages.clear();
    }
}

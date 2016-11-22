package nodebox.network;

import java.net.URI;

import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.ContainerProvider;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;


//import java.util.UUID;
//import java.util.concurrent.ConcurrentLinkedQueue;
//import java.util.concurrent.ConcurrentHashMap;
//import static java.lang.System.out;
/* NOTES:
probably use a ConcurrentMap for the queue
https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ConcurrentMap.html

Flow:
Whenever a node wants to send websocket data it adds a value to the OUT map by calling sendMessage(), send message returns a GUID which is also the key for the map entry
The node then blocks in a loop quierying the IN map with the GUID given to it
Meanwhile another thread is constantly looking at the OUT map and sends websocket data when a new map entry is created
the same thread is also reading websocket data and adding to the IN queue.
the same thread also picks up on special incoming request for application control


 */


/**
 * Created by Gear on 11/17/2016.
 */
@ClientEndpoint
public class webSocketClientEndpoint {
    public boolean isConnected = false;
    Session userSession = null;
    private MessageHandler messageHandler;
    private URI endpointURI;

    //private ConcurrentLinkedQueue<UUID> queue;
    //private ConcurrentHashMap<UUID, String> map;

    public webSocketClientEndpoint(URI endpointURI) {
        //this.queue = queue;
        //this.map = map;
        this.endpointURI = endpointURI;
        try {
            WebSocketContainer container = ContainerProvider
                    .getWebSocketContainer();
            container.connectToServer(this, endpointURI);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Callback hook for Connection open events.
    @OnOpen
    public void onOpen(Session userSession) {
        this.userSession = userSession;
        this.isConnected = true;
    }

    // Callback hook for Connection close events.
    @OnClose
    public void onClose(Session userSession, CloseReason reason) {
        this.userSession = null;
        this.isConnected = false;
    }

    // Callback hook for Message Events. This method will be invoked when a client send a message.
    @OnMessage
    public void onMessage(String message) {
        if (this.messageHandler != null)
            this.messageHandler.handleMessage(message);
    }

    // register message handler
    public void addMessageHandler(MessageHandler msgHandler) {
        this.messageHandler = msgHandler;
    }

    // Send a message to the server.
    public void sendMessage(String message) {
        this.userSession.getAsyncRemote().sendText(message);
    }


    // Message handler.
    public static interface MessageHandler {
        public void handleMessage(String message);
    }
}
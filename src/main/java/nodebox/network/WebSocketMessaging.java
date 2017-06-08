package nodebox.network;

//import com.google.common.collect.ImmutableList;
import nodebox.NodeBox;
import nodebox.client.Application;
import nodebox.client.NodeBoxDocument;
import nodebox.ui.ImageFormat;

//import java.io.IOException;
//import java.util.List;
//import java.util.HashMap;

//import java.util.HashMap;
//import java.util.concurrent.ConcurrentLinkedQueue;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import java.util.UUID;
import java.net.URI;
import java.net.URISyntaxException;
import javax.json.*;
//import javax.json.JsonArray;
//import javax.websocket.DeploymentException;
import java.io.StringReader;
import java.util.concurrent.locks.ReentrantReadWriteLock;

//import java.util.concurrent.CompletableFuture;
//import java.io.IOException;

public class WebSocketMessaging {

    private static ConcurrentHashMap<UUID, JsonObject> INMap = new ConcurrentHashMap<UUID, JsonObject>(1000);  // Graph messages coming in from the server
    public static webSocketClientEndpoint clientEndpoint = null;
    private static NodeBoxDocument document = null;
    private static final ReentrantReadWriteLock rrwl = new ReentrantReadWriteLock();

    public static UUID sendData(JsonObject msg){
        if(clientEndpoint != null){
            // Need to add an entry in the map and queue
            UUID id = UUID.randomUUID();

            JsonObject model = Json.createObjectBuilder()
                    .add(WSDefs.TYPE, WSDefs.DATA)
                    .add(WSDefs.ID, id.toString())
                    .add(WSDefs.MESSAGE, msg)
                    .build();
            //System.out.println("sendMessage");
            if(!clientEndpoint.sendMessage(model.toString())) return(null);
            return(id);
        }
        return(null);
    }

    public static JsonObject getMessage(UUID id){
        if(clientEndpoint != null){
            if(INMap.containsKey(id)){
                JsonObject retVal = INMap.get(id);
                INMap.remove(id);
                //System.out.println("Received message");
                return(retVal);
            }
        }
        return(null);
    }

    public static boolean isEndPointUserSessionValid() {
        rrwl.readLock().lock();
        boolean retVal = false;
        try {
            if(clientEndpoint != null) {
                if(clientEndpoint.userSession != null) {
                    retVal = true;
                }
            }
        }
        finally {
            rrwl.readLock().unlock();
        }

        return retVal;
    }


    public static void setEndpoint(webSocketClientEndpoint ep) {

        rrwl.writeLock().lock();
        try {
            clientEndpoint = ep;
        }
        finally {
            rrwl.writeLock().unlock();
        }

        if(ep == null) return;

        clientEndpoint.addMessageHandler(new webSocketClientEndpoint.MessageHandler() {
            public void handleMessage(String message) {
                if(message.toLowerCase().equals("pong")) return;

                JsonObject jsonObj = Json.createReader(new StringReader(message)).readObject();
                String type = jsonObj.getString(WSDefs.TYPE).trim().toLowerCase();
                String idStr = jsonObj.getString(WSDefs.ID).trim();
                JsonObject msg = jsonObj.getJsonObject(WSDefs.MESSAGE);

                if(type.equals(WSDefs.DATA)) {
                    UUID id = UUID.fromString(idStr);
                    INMap.put(id, msg);
                }
            }
        });
    }

    public static synchronized boolean close() {
        if(clientEndpoint == null) return false;
        if(clientEndpoint.userSession == null) return false;
        try {
            clientEndpoint.userSession.close();
        } catch(IOException e) {

        }

        return true;
    }


    public static void startSystem(String webSockServer){

        // Async server connection and reconection
        class createEndpointTask implements Runnable {
            String str;
            createEndpointTask(String s) { str = s; }
            public void run() {
                // Try connecting to the server forever
                while(true) {

                    if(!WebSocketMessaging.isEndPointUserSessionValid()) {
                        System.out.println("Trying to connect...");
                        try{
                            webSocketClientEndpoint cep = new webSocketClientEndpoint(new URI(str), 2000);
                            WebSocketMessaging.setEndpoint(cep);
                        }catch (URISyntaxException  | IllegalStateException  ee) {

                        }
                    } else {
                        //clientEndpoint.ping();
                    }

                    try {
                        Thread.sleep(2000);
                    } catch(java.lang.InterruptedException e){

                    }
                }
            }
        }
        Thread t = new Thread(new createEndpointTask(webSockServer));
        t.start();
    }

}

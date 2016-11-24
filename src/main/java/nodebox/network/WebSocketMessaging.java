package nodebox.network;

//import com.google.common.collect.ImmutableList;
import nodebox.client.NodeBoxDocument;

//import java.io.IOException;
//import java.util.List;
//import java.util.HashMap;

//import java.util.HashMap;
//import java.util.concurrent.ConcurrentLinkedQueue;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;
import java.net.URI;
import java.net.URISyntaxException;
import javax.json.Json;
import javax.json.JsonObject;
//import javax.json.JsonArray;
//import javax.websocket.DeploymentException;
import java.io.StringReader;

//import java.util.concurrent.CompletableFuture;
//import java.io.IOException;

public class WebSocketMessaging {

    private static ConcurrentHashMap<UUID, JsonObject> INMap = new ConcurrentHashMap<UUID, JsonObject>(1000);  // Messages coming in from the server
    public static webSocketClientEndpoint clientEndpoint = null;
    private static Thread webSockCon;
    private static NodeBoxDocument document = null;


    public static UUID sendData(JsonObject msg){
        if(clientEndpoint != null){
            // Need to add an entry in the map and queue
            UUID id = UUID.randomUUID();

            JsonObject model = Json.createObjectBuilder()
                    .add("type", "dta")
                    .add("id", id.toString())
                    .add("msg", msg)
                    .build();
            System.out.println("sendMessage");
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
                System.out.println("Received message");
                return(retVal);
            }
        }
        return(null);
    }


    public static synchronized void processSocketAppMessage(String cmd, JsonObject jsonObj)
    {
        if(document != null && clientEndpoint != null) {

            if(cmd.toLowerCase().equals("play")) {
                document.playAnimation();
            }

            if(cmd.toLowerCase().equals("stop")) {
                document.stopAnimation();
            }
            if(cmd.toLowerCase().equals("rewind")) {
                //document.blockTillStoppedRendering();

               document.setAnimationFrame(1);
            }

            if(cmd.toLowerCase().equals("setframe")) {
                //document.blockTillStoppedRendering();
                document.setAnimationFrame((long)jsonObj.getInt("frameNumber"));
            }

            if(cmd.toLowerCase().equals("reload")) {
                document.reload();
            }

            if(cmd.toLowerCase().equals("getframe")) {


            }
        }
    }

    public static synchronized boolean isEndPointUserSessionValid() {
        if(clientEndpoint == null) return false;
        if(clientEndpoint.userSession == null) return false;
        return true;
    }


    public static synchronized void setEndpoint(webSocketClientEndpoint ep) {

        clientEndpoint = ep;
        if(ep == null) return;

        clientEndpoint.addMessageHandler(new webSocketClientEndpoint.MessageHandler() {
            public void handleMessage(String message) {
                if(message.toLowerCase().equals("pong")) return;

                JsonObject jsonObj = Json.createReader(new StringReader(message)).readObject();
                String type = jsonObj.getString("type").trim().toLowerCase();
                String idStr = jsonObj.getString("id").trim();
                JsonObject msg = jsonObj.getJsonObject("msg");

                if(type.equals("dta")) {
                    UUID id = UUID.fromString(idStr);
                    INMap.put(id, msg);
                }
                else if(type.equals("cmd") || type.equals("req")) {
                    processSocketAppMessage(idStr, msg);
                }
                else if(type.equals("rly")) {

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

    public static void startSystem(final NodeBoxDocument inDocument, String webSockServer){

        document = inDocument;

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

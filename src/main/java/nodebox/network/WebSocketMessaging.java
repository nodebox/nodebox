package nodebox.network;



import com.google.common.collect.ImmutableList;
import nodebox.client.NodeBoxDocument;

import java.util.List;
import java.util.HashMap;

//import java.util.HashMap;
//import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;
import java.net.URI;
import java.net.URISyntaxException;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonArray;
import java.io.StringReader;
/**
 * Created by Gear on 11/13/2016.
 */
public class WebSocketMessaging {

    //private static ConcurrentLinkedQueue<UUID> OUTQueue = new ConcurrentLinkedQueue<UUID>();
    //private static ConcurrentHashMap<UUID, String> OUTMap = new ConcurrentHashMap<UUID, String>(1000); // Messages going out to the server
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

            clientEndpoint.sendMessage(model.toString());
            return(id);
        }
        return(null);
    }

    public static JsonObject getMessage(UUID id){
        if(clientEndpoint != null){
            if(INMap.containsKey(id)){
                JsonObject retVal = INMap.get(id);
                INMap.remove(id);
                return(retVal);
            }
        }
        return(null);
    }



    public static void processSocketAppMessage(String cmd, JsonObject jsonObj)
    {
        if(document != null && clientEndpoint != null) {
            //JsonArray msgObj = jsonObj.getJsonArray("msg");


            // Building map
            //HashMap<String, JsonObject> msghm = buildMapFromJSONArray(msgObj);

            if(cmd.equals("play")) {
                document.playAnimation();
            }

            if(cmd.equals("stop")) {
                document.stopAnimation();
            }
            if(cmd.equals("rewind")) {
                document.rewindAnimation();
            }

            if(cmd.equals("setframe")) {
                //JsonObject val = msghm.get("ACTION_SETFRAME");
                //document.setFrame((long)java.lang.Integer.parseInt(val.toString()));
                document.setFrame((long)jsonObj.getInt("frameNumber"));
            }

            if(cmd.equals("reload")) {
                document.reload();
            }

            if(cmd.equals("getframe")) {


            }
        }
    }
/*
    public static HashMap<String, JsonObject> buildMapFromJSONArray(JsonArray jsonArrayObj)
    {
        HashMap<String, JsonObject> msghm = new HashMap<String, JsonObject>();

        for(int c = 0; c < jsonArrayObj.size(); c++) {
            JsonObject curArrayVal = jsonArrayObj.getJsonObject(c);
            msghm.put(curArrayVal.getString("key"), curArrayVal.getJsonObject("value"));
        }

        return(msghm);
    }*/


    public static void startSystem(final NodeBoxDocument inDocument, String webSockServer){
        //webSockCon = new Thread(new webSocketConnector(webSockServer, OUTQueue, OUTMap));
        //webSockCon.run();
        document = inDocument;

        try{

            clientEndpoint = new webSocketClientEndpoint(new URI(webSockServer));

            clientEndpoint.addMessageHandler(new webSocketClientEndpoint.MessageHandler() {
                public void handleMessage(String message) {
                    JsonObject jsonObj = Json.createReader(new StringReader(message)).readObject();
                    String type = jsonObj.getString("type").trim();
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

            // Block til open
            while(!clientEndpoint.isConnected){
                //Thread.sleep(100);
            }


        }catch (URISyntaxException e) {

        }


    }
}

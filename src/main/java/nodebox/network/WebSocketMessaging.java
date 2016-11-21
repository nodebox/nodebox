package nodebox.network;



//import java.util.Map;
//import java.util.HashMap;
//import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;
import java.net.URI;
import java.net.URISyntaxException;
import javax.json.Json;
import javax.json.JsonObject;
import java.io.StringReader;
/**
 * Created by Gear on 11/13/2016.
 */
public class WebSocketMessaging {

   // private static ConcurrentLinkedQueue<UUID> OUTQueue = new ConcurrentLinkedQueue<UUID>();
    //private static ConcurrentHashMap<UUID, String> OUTMap = new ConcurrentHashMap<UUID, String>(1000); // Messages going out to the server
    private static ConcurrentHashMap<UUID, String> INMap = new ConcurrentHashMap<UUID, String>(1000);  // Messages coming in from the server
    private static webSocketClientEndpoint clientEndpoint = null;
    private static Thread webSockCon;

    public static UUID sendMessage(String msg){
        if(clientEndpoint != null){
            // Need to add an entry in the map and queue
            UUID id = UUID.randomUUID();
            //OUTQueue.add(id);
            //OUTMap.put(id, msg);

            JsonObject model = Json.createObjectBuilder()
                    .add("id", id.toString())
                    .add("msg", msg)
                    .build();

            clientEndpoint.sendMessage(model.toString());

            return(id);
        }
        return(null);
    }

    public static String getMessage(UUID id){
        if(clientEndpoint != null){
            if(INMap.containsKey(id)){
                String retVal = INMap.get(id);
                INMap.remove(id);
                return(retVal);
            }
        }
        return(null);
    }

    public static void startSystem(String webSockServer){
        //webSockCon = new Thread(new webSocketConnector(webSockServer, OUTQueue, OUTMap));
        //webSockCon.run();
        try{
            clientEndpoint = new webSocketClientEndpoint(new URI(webSockServer));


            clientEndpoint.addMessageHandler(new webSocketClientEndpoint.MessageHandler() {
                public void handleMessage(String message) {
                    JsonObject jsonObj = Json.createReader(new StringReader(message)).readObject();
                    String idStr = jsonObj.getString("id");
                    String msg = jsonObj.getString("msg");
                    UUID id = UUID.fromString(idStr);
                    INMap.put(id, msg);
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

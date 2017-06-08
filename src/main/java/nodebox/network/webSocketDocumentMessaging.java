package nodebox.network;

import nodebox.client.Application;
import nodebox.client.NodeBoxDocument;
import nodebox.ui.ImageFormat;

import javax.json.*;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by Gear on 12/7/2016.
 */
public class webSocketDocumentMessaging {

    private final NodeBoxDocument document;
    private ConcurrentHashMap<UUID, JsonObject> INMap = new ConcurrentHashMap<UUID, JsonObject>(1000);  // Graph messages coming in from the server
    private ConcurrentLinkedQueue<JsonObject> INCmdMap = new ConcurrentLinkedQueue<JsonObject>();  // Command messages coming in from the server
    private final ReentrantReadWriteLock rrwl = new ReentrantReadWriteLock();
    public webSocketClientEndpoint clientEndpoint = null;

    public webSocketDocumentMessaging(NodeBoxDocument document, String webSockServer) {
        this.document = document;

        // Async server connection and reconection
        class createEndpointTask implements Runnable {
            private final String str;
            private final webSocketDocumentMessaging msgDoc;
            createEndpointTask(String s, webSocketDocumentMessaging msg) { str = s; msgDoc = msg;}
            public void run() {
                // Try connecting to the server forever
                while(true) {

                    if(!msgDoc.isEndPointUserSessionValid()) {
                        System.out.println("Trying to connect...");
                        try{
                            webSocketClientEndpoint cep = new webSocketClientEndpoint(new URI(str), 2000);
                            msgDoc.setEndpoint(cep);
                        }catch (URISyntaxException | IllegalStateException  ee) {

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
        Thread t = new Thread(new createEndpointTask(webSockServer, this));
        t.start();


        class processCommandQueue implements Runnable {
            private final webSocketDocumentMessaging msgDoc;
            final JsonObject empty = Json.createReader(new StringReader("{}")).readObject();
            processCommandQueue(webSocketDocumentMessaging msg) { msgDoc = msg; }

            public void run() {
                while(true) {

                    if(!Application.getInstance().openingDoc.get()) {

                        // Getting COMMAND
                        JsonObject cmd = INCmdMap.peek();
                        if(cmd != null) {
                            String idStr = cmd.getString("id").trim();
                            if(idStr == WSDefs.CMDS.STOP || idStr == WSDefs.CMDS.SETFRAME || idStr == WSDefs.CMDS.REWIND) {
                                msgDoc.processSocketAppCommand(idStr, cmd.getJsonObject("msg"));
                                msgDoc.INCmdMap.poll();
                            }
                            else {
                                // Send STOP command before other command
                                msgDoc.processSocketAppCommand(WSDefs.CMDS.STOP, empty);
                                while(msgDoc.document.isRendering.get() || msgDoc.document.isExporting.get()) {}

                                cmd = msgDoc.INCmdMap.poll();
                                if(cmd != null) {
                                    msgDoc.processSocketAppCommand(cmd.getString("id").trim(), cmd.getJsonObject("msg"));
                                }
                            }
                        }

                        try {
                            Thread.sleep(100);
                        } catch(java.lang.InterruptedException e){

                        }
                    }
                }
            }
        }

        Thread pt = new Thread(new processCommandQueue(this));
        pt.start();
    }

    public synchronized boolean close() {
        if(this.clientEndpoint == null) return false;
        if(this.clientEndpoint.userSession == null) return false;
        try {
            this.clientEndpoint.userSession.close();
        } catch(IOException e) {

        }

        return true;
    }

    private void sendResponse(String cmd, JsonObject msg){
        if(this.clientEndpoint != null){
            // Need to add an entry in the map and queue
            //UUID id = UUID.randomUUID();

            JsonObject model = Json.createObjectBuilder()
                    .add(WSDefs.TYPE, WSDefs.RESPONSE)
                    .add(WSDefs.ID, cmd)
                    .add(WSDefs.MESSAGE, msg)
                    .build();
            //System.out.println("sendMessage");
            this.clientEndpoint.sendMessage(model.toString());
            //if(!this.clientEndpoint.sendMessage(model.toString())) return(null);
            //return(id);
        }
        //return(null);
    }

    public JsonObject getMessage(UUID id){
        if(this.clientEndpoint != null){
            if(this.INMap.containsKey(id)){
                JsonObject retVal = this.INMap.get(id);
                this.INMap.remove(id);
                //System.out.println("Received message");
                return(retVal);
            }
        }
        return(null);
    }

    private void respondGetFrame() {
        JsonObject model = Json.createObjectBuilder().add(WSDefs.TAGS.FRAME, this.document.getFrame()).build();;
        this.sendResponse(WSDefs.TAGS.GETFRAME, model);
    }

    private void respondGetDocs() {
        JsonBuilderFactory factory = Json.createBuilderFactory(null);
        JsonObjectBuilder rootObjB = factory.createObjectBuilder();
        JsonArrayBuilder rootArrayB = factory.createArrayBuilder();

        for (NodeBoxDocument doc : Application.getInstance().getDocuments()) {
            JsonObjectBuilder curObjB = factory.createObjectBuilder();
            curObjB.add(WSDefs.ID, doc.id.toString());
            curObjB.add(WSDefs.TAGS.FILENAME, doc.getDocumentFile().getAbsolutePath());
            rootArrayB.add(curObjB);
        }

        rootObjB.add(WSDefs.TAGS.DOCUMENTS, rootArrayB);
        this.sendResponse(WSDefs.TAGS.GETDOCS, rootObjB.build());
    }

    private void respondGetDoc() {
        JsonBuilderFactory factory = Json.createBuilderFactory(null);
        JsonObjectBuilder rootObjB = factory.createObjectBuilder();
        rootObjB.add(WSDefs.ID, this.document.id.toString());
        rootObjB.add(WSDefs.TAGS.FILENAME, this.document.getDocumentFile().getAbsolutePath());

        this.sendResponse(WSDefs.TAGS.GETDOC, rootObjB.build());
    }

    private void processSocketAppRequest(String cmd, JsonObject jsonObj) {
        if(this.document != null && this.clientEndpoint != null) {
            if(cmd.toLowerCase().equals(WSDefs.TAGS.GETFRAME)) {
                this.respondGetFrame();
            }
            else if(cmd.toLowerCase().equals(WSDefs.TAGS.GETDOCS)) {
                this.respondGetDocs();
            }
            else if(cmd.toLowerCase().equals(WSDefs.TAGS.GETDOC)) {
                this.respondGetDoc();
            }
        }
    }

    private void processSocketAppCommand(String cmd, JsonObject jsonObj)
    {
        if(this.document != null && this.clientEndpoint != null) {

            // Using if instead of case because I want to use .equals
            if(cmd.toLowerCase().equals(WSDefs.CMDS.PLAY)) {
                this.document.playAnimation();
            }
            else if(cmd.toLowerCase().equals(WSDefs.CMDS.STOP)) {
                this.document.stopAnimation();
            }
            else if(cmd.toLowerCase().equals(WSDefs.CMDS.REWIND)) {
                this.document.setFrame(1);
            }
            else if(cmd.toLowerCase().equals(WSDefs.CMDS.SETFRAME)) {
                this.document.setFrame((long)jsonObj.getInt(WSDefs.TAGS.FRAMENUMBER));
            }
            else if(cmd.toLowerCase().equals(WSDefs.CMDS.RELOAD)) {
                this.document.reload();
            }
            else if(cmd.toLowerCase().equals(WSDefs.CMDS.LOAD)) {

                File f = new File(jsonObj.getString(WSDefs.TAGS.FILENAME));
                if(Application.canLoadFile(f)) {
                    Application.getInstance().openDocument(f);
                }
            }
            else if(cmd.toLowerCase().equals(WSDefs.CMDS.EXPORTRANGE)) {
                String docFileName = this.document.getDocumentFile().getName();
                String fileName = docFileName + "-" + jsonObj.getString(WSDefs.TAGS.FILENAME);
                File d = new File(jsonObj.getString(WSDefs.TAGS.EXPORTPATH));
                int start = jsonObj.getInt(WSDefs.TAGS.STARTFRAME);
                int end = jsonObj.getInt(WSDefs.TAGS.ENDFRAME);
                String formatStr = jsonObj.getString(WSDefs.TAGS.EXPORTFORMAT);
                ImageFormat format = ImageFormat.of(formatStr);

                this.document.exportRange(fileName, d, start, end, format);
            }
        }
    }

    public boolean isEndPointUserSessionValid() {
        this.rrwl.readLock().lock();
        boolean retVal = false;
        try {
            if(this.clientEndpoint != null) {
                if(this.clientEndpoint.userSession != null) {
                    retVal = true;
                }
            }
        }
        finally {
            this.rrwl.readLock().unlock();
        }

        return retVal;
    }

    public void setEndpoint(webSocketClientEndpoint ep) {

        this.rrwl.writeLock().lock();
        try {
            this.clientEndpoint = ep;
        }
        finally {
            this.rrwl.writeLock().unlock();
        }

        if(ep == null) return;


        this.clientEndpoint.addMessageHandler(new webSocketClientEndpoint.MessageHandler() {
            public void handleMessage(String message) {
                if(message.toLowerCase().equals("pong")) return;

                JsonObject jsonObj = Json.createReader(new StringReader(message)).readObject();
                String type = jsonObj.getString(WSDefs.TYPE).trim().toLowerCase();
                String idStr = jsonObj.getString(WSDefs.ID).trim();
                JsonObject msg = jsonObj.getJsonObject(WSDefs.MESSAGE);

                if(type.equals(WSDefs.COMMAND)) {
                    INCmdMap.add(jsonObj);
                }
                else if(type.equals(WSDefs.REQUEST)) {
                    processSocketAppRequest(idStr, msg);
                }
                else if(type.equals(WSDefs.RESPONSE)) {
                    // Client should never receive a RESPONSE
                }
            }
        });
    }
}

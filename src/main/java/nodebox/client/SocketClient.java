package nodebox.client;
import java.io.*;
import java.net.*;

/**
 * Created by Gear on 11/1/2016.
 */
public class SocketClient {
    private Socket socket;
    //private BufferedReader inFromServer;
    //private DataOutputStream outToServer;
    SocketClient() {
        System.out.println("Socket Class: created");
        this.socket = new Socket();

    }

    public boolean connect(String host, int port, long retryInterval) {
        //System.out.println("Socket Class: connect");
       // Thread thread1 = new Thread(new SocketClientThread("thread1", retryInterval));
       // thread1.start();
        return(false);
    }

}

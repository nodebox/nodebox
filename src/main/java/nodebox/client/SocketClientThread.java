package nodebox.client;
import java.io.*;
import java.net.*;
/**
 * Created by Gear on 11/1/2016.
 */
public class SocketClientThread implements Runnable{
    private Socket socket;
    private String server;
    private int port;
    private long retryInterval;
    private long readInterval = 100;

    private boolean isConnected = false;

    public SocketClientThread(String server, int port, long retryInterval) {
        this.server = server;
        this.port = port;
        this.retryInterval = retryInterval;

        this.socket = new Socket();
    }


    public void run() {
        //Display info about this particular thread
        System.out.println("Thread: run");

        int i = 0;

        // Main loop for connection and reading data
        while(true){

            if(!isConnected){
                try {
                    this.socket.connect(new InetSocketAddress(this.server, this.port), 1000);
                    System.out.println("Connected: " + socket);
                    this.isConnected = true;

                }
                catch(IOException uhe) {
                    System.out.println("Host unknown: " + uhe.getMessage());
                }
            }
            else {

            }

            System.out.println(i);
            i++;
            try{
                Thread.sleep(this.retryInterval);
            }
            catch(InterruptedException e){

            }
            System.out.println("In thread after sleep");
        }



    }
}

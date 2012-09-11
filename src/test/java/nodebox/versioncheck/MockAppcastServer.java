package nodebox.versioncheck;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.StringTokenizer;

public class MockAppcastServer implements Runnable {

    private static final File mediaRoot;

    private final int port;
    private boolean running;
    private ServerSocket server;

    static {
        mediaRoot = new File("src/test/java/nodebox/versioncheck");
    }

    public MockAppcastServer(int port) {
        this.port = port;
    }

    public void run() {
        running = true;
        try {
            server = new ServerSocket(port);
            while (running) {
                Socket socket = server.accept();
                handleRequest(socket);
            }
        } catch (IOException ignored) {
        }
    }

    public boolean isRunning() {
        return running;
    }

    public void stop() {
        if (!running) return;
        running = false;
        if (server != null) {
            try {
                server.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        server = null;
    }

    private void handleRequest(Socket socket) throws IOException {
        PrintStream os = new PrintStream(new BufferedOutputStream(socket.getOutputStream()));
        BufferedReader is = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        String line = is.readLine();
        if (line == null) return;
        String version = "";
        StringTokenizer st = new StringTokenizer(line);
        String method = st.nextToken();
        String fileName = st.nextToken();
        if (st.hasMoreTokens()) {
            version = st.nextToken();
        }
        //read header but ignore
        while ((line = is.readLine()) != null) {
            if (line.trim().equals("")) break;
        }
        //we only do GET
        if (method.equals("GET") || method.equals("HEAD")) {
            boolean headOnly = false;
            if (method.equals("HEAD")) {
                headOnly = true;
            }
            byte[] data;

            File f = new File(mediaRoot, fileName);
            try {
                data = readFileAsString(f).getBytes();
                String header = "HTTP/1.0 200 OK\r\n";
                header += standardHeaders();
                header += "Content-length: " + data.length + "\r\n";
                header += "Content-type: text/xml\r\n\r\n";
                os.print(header);
                if (!headOnly) {
                    os.write(data);
                }
            } catch (IOException e) {
                String header = "HTTP/1.0 404 Not Found\r\n";
                header += standardHeaders();
                os.print(header);
            }
            os.flush();
            os.close();
        } else {
            //not a get/head so output error
            if (version.startsWith("HTTP/")) { //send headers
                String header = "HTTP/1.0 501 Not Implemented\r\n";
                header += standardHeaders();
                header += "Content-type: text/html\r\n\r\n";
                os.print(header);
            }
            os.println("<HTML><HEAD><TITLE>Not Implemented</TITLE><HEAD>");
            os.println("<BODY><H1>HTTP Error 501: Not Implemented</H1></BODY></HTML>");
        }
        os.close();
        socket.close();
    }

    /**
     * Send standard HTTP headers.
     *
     * @return String with standard headers.
     */
    private String standardHeaders() {
        //Date now = LazyDate.getDate();
        Date now = new Date();
        return "Date: " + now + "\r\nServer: MockAppcastServer http 1.0\r\n";
    }

    /**
     * Read in a file at the given path and return its contents as a string.
     *
     * @param f the file to open.
     * @return the contents of the file as a string.
     * @throws java.io.IOException if the file could not be read.
     */
    private static String readFileAsString(File f)
            throws java.io.IOException {
        StringBuilder fileData = new StringBuilder(1000);
        BufferedReader reader = new BufferedReader(
                new FileReader(f));
        char[] buf = new char[1024];
        int numRead;
        while ((numRead = reader.read(buf)) != -1) {
            String readData = String.valueOf(buf, 0, numRead);
            fileData.append(readData);
            buf = new char[1024];
        }
        reader.close();
        return fileData.toString();
    }

    public static void main(String[] args) {
        MockAppcastServer server = new MockAppcastServer(8080);
        server.run();
    }


}

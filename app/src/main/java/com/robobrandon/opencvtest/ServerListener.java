package com.robobrandon.opencvtest;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Mat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PipedReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Created by brandon on 6/6/16.
 */
public class ServerListener implements Runnable {
    private MainActivity parent;
    private final int SERVER_PORT;
    private final String SERVER_IP;
    private Thread t;
    private String threadName;
    private final String eol = "\0";
    Mat capturedFrame;


    public ServerListener(MainActivity parent, int serverPort, String serverIp, PipedReader r){
        SERVER_PORT = serverPort;
        SERVER_IP = serverIp;
        threadName = "Server Listener";
        parent = parent;
    }

    public void run(){
        // Can use this as a backup method...  save image and THEN stream
        //Bitmap bm = Bitmap.createBitmap(capturedFrame.cols(), capturedFrame.rows(),Bitmap.Config.ARGB_8888);
        //Utils.matToBitmap(capturedFrame, bm);

        // Possible that I'll need to specify elemSize?
        // int size = (int) capturedFrame.total() * capturedFrame.elemSize();
//        int size = (int) (capturedFrame.total());
        // byte[] buffer = new byte[size];

        Socket socket = null;
        PrintWriter out = null;
        BufferedReader in = null;

        //Try to connect to the server
        try{
            socket = new Socket(SERVER_IP, SERVER_PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        }
        catch(UnknownHostException e) {
            System.err.println("IP address " + SERVER_IP + " could not be determined");
            System.exit(1);
        } catch(IOException e) {
            System.err.println("Error with I/O in connecting to " + SERVER_IP + ":" + SERVER_PORT);
            System.err.println(e.getLocalizedMessage());
            System.exit(1);
        }

        String fromServer, toServer;
        while(true) {
            // First check to see if we have an image to send
            if (parent.frameRequested){
                // Notify that we're sending a frame so that the main thread doesn't push another
                // frame through before we're finished sending this one.
                parent.sendingFrame = true;
                // Notify server of how much it needs to buffer for image
                long size = parent.capturedFrame.elemSize();
                toServer = "SendingFrame," + size + "\n";
                System.out.println("Sending: " + toServer + " to the server");
                out.println(toServer + eol);

                System.out.println("Sending frame");
                if (parent.capturedFrame.isContinuous()) {
                    out.println(parent.capturedFrame);
                }
            } else{
                try {
                    while ((fromServer = in.readLine()) != null) {
                        fromServer = fromServer.replace(eol, "\n");
                        System.out.println("Server says: " + fromServer);
                    }

                } catch (IOException e) {
                    //Exit gracefully
                    System.err.println("Error - Connection with server lost.  System exiting.");
                    try {
                        socket.close();
                    } catch (IOException e1) {
                        System.err.println("Error with I/O in connecting to " + SERVER_IP + ":" + SERVER_PORT);
                    }
                    System.exit(0);
                }
            }

        }
    }

    public void start(){
        System.out.println("Starting " +  threadName );
        if (t == null) {
            t = new Thread (this, threadName);
            t.start ();
        }
    }

}

package com.example.cloudlet.ingunibo.cloudlettestapp;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;


public class SendPositionsThread extends Thread {
    private static final int CLOUDLET_SERVER_POSITIONS_PORT=11111;
    private static final String TAG = "SendPositionsThread";

    //DEBUG
//    private static final String cloudletAddr="homespartaco25.duckdns.org";
    //private static final String cloudletAddr="137.204.57.29";

    private Socket sendPositionsSocket;
    private BufferedReader in;
    //private PrintWriter out;
    private ObjectOutputStream out;

    private InetAddress discoveredAddr;
    private double [][] positionsToSend = new double
            [CloudletClientConfiguration.NUM_POSITIONS][2];

    public SendPositionsThread(InetAddress inetaddr, double [][] positions){
        super();
//        this.discoveredAddr=inetaddr;

        //DEBUG TODO Delete
        try{
            this.discoveredAddr=InetAddress.getByName("137.204.57.29");
        }
        catch(UnknownHostException uhe){
            Log.e(TAG, "UnknownHostException ");
            uhe.printStackTrace();
        }

        this.positionsToSend=positions;
    }

    @Override
    public void run() {
        try {
//            sendPositionsSocket = new Socket(this.discoveredAddr,
            sendPositionsSocket = new Socket("137.204.57.29",
                    CLOUDLET_SERVER_POSITIONS_PORT);

            in = new BufferedReader(new InputStreamReader(sendPositionsSocket
                    .getInputStream()));
            //out = new PrintWriter(sendPositionsSocket.getOutputStream());
            out = new ObjectOutputStream(sendPositionsSocket.getOutputStream());
            out.writeObject(this.positionsToSend);
            out.flush();
            Log.d(TAG, "Completed trasmission to cloudlet.");
            stopConnection();
        }
        catch (IOException ioe) {
            Log.e(TAG, "IOException during send data position to the cloud, EXIT");
            ioe.printStackTrace();
        }
    }

    /*garbage collector*/
    public void stopConnection() throws IOException{
        if (sendPositionsSocket!=null) sendPositionsSocket.close();
        if(in!=null) in.close();
        if(out!=null) out.close();
    }
}


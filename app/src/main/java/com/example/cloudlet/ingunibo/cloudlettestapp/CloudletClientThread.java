package com.example.cloudlet.ingunibo.cloudlettestapp;

import android.content.Context;
import android.util.Log;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

public class CloudletClientThread extends Thread{
    private static final String TAG = "CloudletClientThread";

    private InetAddress addr;
    private int port;
    private Context ctxActivity;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    private static final String KNOCK_MSG = "knock";
    private static final String WHAT_SERVICE_MSG = "GET_SERVICE";
    private static final String SERVICE_MSG = "PUT_SERVICE";
    private static final String DEF_SERVICE_NAME = "ubuntu-server";
    private static final String IMG_CHK_OK_MSG = "IMAGE_OK";
    private static final String SERVER_UNKNOWN_SERVICE_MSG= "UNKNOWN";

    public CloudletClientThread(InetAddress cloudletServerAddr, int cloudletServerPort, Context ctx){
        super();
        this.addr=cloudletServerAddr;this.port=cloudletServerPort; this.ctxActivity = ctx;
    }

    public void updateTxtView(final String msg, final boolean append){
        ((DiscoveryActivity)ctxActivity).runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        TextView txtView = ((DiscoveryActivity)ctxActivity).getTextView();
                        if (append) txtView.append("\n"+msg);
                        else txtView.setText(msg);
                    }
                }
        );
    }

    @Override
    public void run() {
        try {
            socket = new Socket(addr, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream());

            out.println(KNOCK_MSG);
            out.flush();

            String communication;
            int commInt=0;
            while (commInt==0) {
                    loopcomm: while ((communication=in.readLine())!=null) {
                        Log.i(TAG, "Server says: " + communication);
                        switch (communication) {
                            case WHAT_SERVICE_MSG:
                                out.println(SERVICE_MSG);
                                out.flush();
                                out.println(DEF_SERVICE_NAME);
                                out.flush();
                                break;
                            case IMG_CHK_OK_MSG:
                                updateTxtView("Server can start my service, waiting...", false);
                                commInt++;
                                break loopcomm;
                            case SERVER_UNKNOWN_SERVICE_MSG:
                                Log.e(TAG, "Error from Cloudlet Server. Service unknown, closing...");
                                commInt++;
                                break loopcomm;
                        }
                    }
                    stopConnection();
            }

        } catch (IOException ioe) {
            Log.e(TAG, "IOException occurred, EXIT");
            ioe.printStackTrace();
        }
    }//run

    public void stopConnection() throws IOException{
        if (socket!=null) socket.close();
        if(in!=null) in.close();
        if(out!=null) out.close();
    }
}
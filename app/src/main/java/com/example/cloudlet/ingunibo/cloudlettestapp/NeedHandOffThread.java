package com.example.cloudlet.ingunibo.cloudlettestapp;


import android.content.Context;
import android.util.Log;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class NeedHandOffThread extends Thread{
    private static final String TAG = "NeedHandOffThread";

    private Context ctxActivity;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private static final String HANDOFF_NEED_MSG = "HANDOFF";
    private static final String HANDOFF_OK_MSG = "HANDOFF_OK";
    private static final String IP_RES_MSG = "IP_RES";
    private static final String IP_REQ_MSG = "IP_REQ";
    private String addrCloudlet;
    private String addrPrevCloudlet;
    private int portCloudlet;

    public NeedHandOffThread(Context ctx, String addr, int port, String prevAddr){
        super();
        this.ctxActivity=ctx;
        this.addrCloudlet=addr;
        this.portCloudlet=port;
        this.addrPrevCloudlet=prevAddr;
    }

    public void updateTxtView(final String msg){
        ((DiscoveryActivity)ctxActivity).runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        TextView txtView = ((DiscoveryActivity)ctxActivity).getTextView();
                        txtView.setText("\n"+msg);
                    }
                }
        );
    }

    @Override
    public void run() {
        try {
            socket = new Socket(addrCloudlet, portCloudlet);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream());

            out.println(HANDOFF_NEED_MSG);
            out.flush();

            String communication;
            int commInt=0;
            while (commInt==0) {
                loopcomm:while ((communication = in.readLine()) != null) {
                    Log.i(TAG, "Server Says= " + communication);
                    switch(communication){
                        case IP_REQ_MSG:
                            out.println(IP_RES_MSG);
                            out.println(addrPrevCloudlet);
                            out.flush();
                            Log.i(TAG, "Address of previous cloudlet "+addrPrevCloudlet+
                                    " sent to current cloudlet "+addrCloudlet);
                            break;
                        case HANDOFF_OK_MSG:
                            updateTxtView("Waiting for handoff");
                            commInt++;
                            break loopcomm;
                    }
                }
                stopConnection();
            }
        }catch (IOException ioe){
            Log.e(TAG, "Problems in socket communication for reconnect");
            ioe.printStackTrace();
        }

    }

    public void stopConnection() throws IOException{
        if (socket!=null) socket.close();
        if(in!=null) in.close();
        if(out!=null) out.close();
    }
}

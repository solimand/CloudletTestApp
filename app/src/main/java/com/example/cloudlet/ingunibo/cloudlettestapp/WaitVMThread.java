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

public class WaitVMThread extends Thread{
    private static final String TAG ="WaitVMThread";
    private static final String REQ_VM_READY_MSG ="REQ_VM_READY";
    private static final String REQ_OVERLAY_URL_MSG ="REQ_OVERLAY_URL";
//    private static final String DEF_OVERLAY_URL = "http://localhost:54321/overlays/overlay-os.zip";
    private static final String DEF_MAN_ADDR_MSG ="MAN_ADDR";
    private static final String DEF_PUB_ADDR_MSG ="PUB_ADDR";
    private static final String KNOCK_MSG = "knock";
    private static final String OK_MSG = "OK";
    private static final String WAIT_MSG = "WAIT";

    private InetAddress addr;
    private int port;
    private Context ctxActivity;
    private String overlayUrl;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    public WaitVMThread(InetAddress cloudletServerAddr, int cloudletServerPort, Context ctx, String olUrl){
        super();
        this.addr=cloudletServerAddr;this.port=cloudletServerPort; this.ctxActivity = ctx;
        this.overlayUrl=olUrl;
    }

    public void updateTxtView(final String msg, final boolean append){
        ((DiscoveryActivity)ctxActivity).runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        TextView txtView = ((DiscoveryActivity)ctxActivity).getTextView();
                        if(append)
                            txtView.append("\n"+msg);
                        else
                            txtView.setText(msg);
                    }
                }
        );
        Log.d(TAG, msg);
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
            int commInterrupt=0;
            while (commInterrupt==0){
                loopWaiting: while ((communication=in.readLine())!=null){
                    Log.i(TAG, "Server says: " + communication);
                    switch (communication){
                        case OK_MSG:
                            out.println(REQ_VM_READY_MSG);
                            out.flush();
                            break;
                        case DEF_MAN_ADDR_MSG:
                            String serviceManAddr = in.readLine();
                            updateTxtView("Service management addr = "+serviceManAddr,true);
                            ((DiscoveryActivity) ctxActivity).setServiceMngmntAddr(serviceManAddr);
                            break;
                        case DEF_PUB_ADDR_MSG:
                            String serviceLocAddr = in.readLine();
                            updateTxtView("Service public addr = "+serviceLocAddr,true);
                            ((DiscoveryActivity) ctxActivity).setServiceLocalAddr(serviceLocAddr);
                            break loopWaiting;
                        case WAIT_MSG:
                            Log.i(TAG, "VM not Ready.");
                            updateTxtView("VM not ready yet. Retry later...",true);
                            break loopWaiting;

                        case REQ_OVERLAY_URL_MSG:
                            out.println(overlayUrl);
                            out.flush();
                            break;

                        default:
                            Log.d(TAG, "default case switch");
                            break;
                    }
                }
                stopConnection();
                commInterrupt++;
            }
        }catch (IOException ioe) {
            Log.e(TAG, "Error in wait VM thread");
            ioe.printStackTrace();
        }

    }//run

    public void stopConnection() throws IOException{
        if (socket!=null) socket.close();
        if(in!=null) in.close();
        if(out!=null) out.close();
    }
}

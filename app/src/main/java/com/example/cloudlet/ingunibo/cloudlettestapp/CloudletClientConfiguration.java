package com.example.cloudlet.ingunibo.cloudlettestapp;



public class CloudletClientConfiguration {

    /*POSITIONS*/
    public static final int NUM_POSITIONS =50;
    private static final int CLOUDLET_SERVER_POSITIONS_PORT=11111;


    /*DEBUG*/
    private static final String cloudletAddr="homespartaco25.duckdns.org";

    /*TODO change ref in files that use these variables*/
    private static final String SERVICE_TYPE = "_cloudlet._udp.";
    private static final String SERVICE_PACKAGE = "com.example.cloudlet.ingunibo.mobileopencv";
    private static final String INTENT_LOCAL_IP_EXTRA_NAME = "EXTRA_LOCAL_IP";
    private static final String INTENT_MAN_IP_EXTRA_NAME = "EXTRA_MAN_IP";
    private static final String INTENT_HANDOFF_IP_EXTRA_NAME = "EXTRA_LOCAL_IP";
    private static final String DEF_OVERLAY_URL = "http://localhost:54321/overlays/overlay-os.zip";
    private static final String MOBILE_OVERLAY_URL = ":8080/overlay-os.zip";
    private static final String CLOUD_OVERLAY_URL = "http://137.204.57.244:38670/overlays/overlay-os.zip";
    private static final String PRIV_FILE_NAME = "cloudletInfo.txt";
    private static final String GPS_UPDATE_ON="Stop Location Update";
    private static final String GPS_UPDATE_OFF="Start Location Update";

}

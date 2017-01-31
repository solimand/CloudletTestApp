package com.example.cloudlet.ingunibo.cloudlettestapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Environment;
import android.os.StrictMode;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;

public class DiscoveryActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, LocationListener {
    private static final String TAG = "DiscoveryActivity";
    private static final String USER_ID = "user1";
//    private android.net.wifi.WifiManager.MulticastLock lock = null;
    private android.os.Handler genericHandler = new android.os.Handler();

    /**MY ENV*/
    private static final String SERVICE_TYPE = "_cloudlet._udp.";
    private static final String SERVICE_PACKAGE = "com.example.cloudlet.ingunibo.mobileopencv";
    private static final String INTENT_LOCAL_IP_EXTRA_NAME = "EXTRA_LOCAL_IP";
    private static final String INTENT_MAN_IP_EXTRA_NAME = "EXTRA_MAN_IP";
    private static final String INTENT_HANDOFF_IP_EXTRA_NAME = "EXTRA_LOCAL_IP";
    private static final String DEF_OVERLAY_URL = "http://localhost:54321/overlays/overlay-os.zip";
    private static final String MOBILE_OVERLAY_URL = ":8080/overlay-os.zip";
//    private static String CLOUD_OVERLAY_URL = "https://www.dropbox.com/s/qb908jtyyxqejvy/overlay-os.zip?dl=1";
    private static final String CLOUD_OVERLAY_URL = "http://137.204.57.244:38670/overlays/overlay-os.zip";
    private static final String PRIV_FILE_NAME = "cloudletInfo.txt";
    private static final String CLOUD_PREDICTION_URL="http://137.204.57.244:22225";
    private static final String GPS_UPDATE_ON="Stop Location Update";
    private static final String GPS_UPDATE_OFF="Start Location Update";

    /**GOOGLE*/
    private NsdManager mNsdManager;
    private NsdManager.ResolveListener mResolveListener;
    private NsdManager.DiscoveryListener mDiscoveryListener;
    private NsdServiceInfo mService;
    private int discoveredPort=0;
    private InetAddress discoveredAddr=null;
    private String serviceMngmntAddr="";
    private String serviceLocalAddr="";

    /**GOOGLE_LOCATION_SERVICE**/
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private LocationRequest mLocationRequest;
    private Location mCurrentLocation;
    private String mLastUpdateTime;

    /**VIEW**/
    private Button stopDiscoveryButton;
    protected TextView showNotifTextView;
    private RadioGroup radioGroupOverlayUrl;

    /**---------------------------------------Activity---------------------------------------**/

    /**Result of selection Previous Cloudlet IP Address**/
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode==1 && resultCode== Activity.RESULT_OK){
            String ipHandoff = data.getExtras().getString(INTENT_HANDOFF_IP_EXTRA_NAME);
            Log.i(TAG, "IP of previous Cloudlet Selected: "+ipHandoff);

            //request handoff to previous cloudlet
            try{
                NeedHandOffThread hoThread = new NeedHandOffThread(
                        DiscoveryActivity.this, discoveredAddr.getHostAddress(), discoveredPort, ipHandoff);
                hoThread.start();
                hoThread.join();
            }catch(InterruptedException ie) {
                Log.e(TAG, "Interrupted Exception Handoff Thread");
                ie.printStackTrace();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discovery);

        /**DEV**/
        /*This is for the NetworkOnMainThreadException*/
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        mNsdManager = (NsdManager) this.getSystemService(Context.NSD_SERVICE);

        // Create an instance of GoogleAPIClient
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        createLocationRequest();
        //TODO prompt user to change the location settings based on 'result' value.
            /*
        createLocationRequest();
        //get the current location settings of a user's device
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);
        //check whether the current location settings are satisfied:
        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient,
                        builder.build());
                        */

        /**VIEWS**/
        Button googleDiscoveryButton = (Button) this.findViewById(R.id.buttonDiscoveryGoogle);
        stopDiscoveryButton = (Button) this.findViewById(R.id.buttonStopDiscovery);
        stopDiscoveryButton.setEnabled(false);
        Button connectButton =(Button) this.findViewById(R.id.buttonConnect);
        Button waitVMbutton = (Button)this.findViewById(R.id.buttonWaitVM);
        //Button reconnectButton = (Button)this.findViewById(R.id.buttonReConnect);
        final Button locationButton = (Button)this.findViewById(R.id.buttonLocationUpdate);
        Button connectServiceButton = (Button)this.findViewById(R.id.buttonConnectService);
        radioGroupOverlayUrl = (RadioGroup)this.findViewById(R.id.radioGroupOverlayType);
        radioGroupOverlayUrl.check(R.id.radioButtonCloudlet);

        /*start/stop Location Update*/
        locationButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                genericHandler.postDelayed(new Runnable() {
                    public void run() {
                        if(locationButton.getText().toString()==GPS_UPDATE_OFF) {
                            Log.i(TAG, "Starting Location Updates...");
                            /**Location Service Set Up**/
                            // Create an instance of GoogleAPIClient.
                            /*if (mGoogleApiClient == null) {
                                mGoogleApiClient = new GoogleApiClient.Builder(DiscoveryActivity.this)
                                        .addConnectionCallbacks(DiscoveryActivity.this)
                                        .addApi(LocationServices.API)
                                        .build();
                            }*/
                            //TODO start updates
                            locationButton.setText(GPS_UPDATE_ON);
                        }
                        else if(locationButton.getText().toString()==GPS_UPDATE_ON){
                            Log.i(TAG, "Stopping Location Updates...");
                            //TODO stop updates
                            locationButton.setText(GPS_UPDATE_OFF);
                        }



                    }
                }, 100);
            }
        });

        /*start service that uses cloudlet*/
        connectServiceButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                genericHandler.postDelayed(new Runnable() {
                    public void run() {
                        Log.i(TAG, "Starting Service...");
                        Intent launchServiceIntent = getPackageManager().getLaunchIntentForPackage(SERVICE_PACKAGE);

                        if (!getServiceMngmntAddr().equals(""))
                            launchServiceIntent.putExtra(INTENT_MAN_IP_EXTRA_NAME, getServiceMngmntAddr());
                        if (!getServiceLocalAddr().equals(""))
                            launchServiceIntent.putExtra(INTENT_LOCAL_IP_EXTRA_NAME, getServiceLocalAddr());

                        startActivity(launchServiceIntent);
                    }
                }, 100);
            }
        });


        /**OLD HANDOFF: open activity to select IP of previous Cloudlet**/
        /*
        reconnectButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                genericHandler.postDelayed(new Runnable() {
                    public void run() {
                        initializeResolveListener();
                        initializeDiscoveryListener();
                        mNsdManager.discoverServices(
                                SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
                        stopDiscoveryButton.setEnabled(true);
                        stopDiscoveryButton.setClickable(true);

                        List<String> addrsInFile = readPrivateTxtFile(PRIV_FILE_NAME);
                        Intent hoListIntent = new Intent(DiscoveryActivity.this, HandoffListActivity.class);
                        hoListIntent.putStringArrayListExtra(INTENT_HANDOFF_IP_EXTRA_NAME, (ArrayList<String>) addrsInFile);
                        startActivityForResult(hoListIntent,1);
                    }
                }, 100);
            }
        });*/

        /*discovery nearby cloudlet*/
        googleDiscoveryButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                genericHandler.postDelayed(new Runnable() {
                    public void run() {
                        initializeResolveListener();
                        initializeDiscoveryListener();
                        mNsdManager.discoverServices(
                                SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
                        stopDiscoveryButton.setEnabled(true);
                        stopDiscoveryButton.setClickable(true);
                    }
                }, 100);
            }
        });

        stopDiscoveryButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                genericHandler.postDelayed(new Runnable() {
                    public void run() {
                        stopDiscovery();
                    }
                }, 100);
            }
        });

        /*contact cloudlet*/
        connectButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                genericHandler.postDelayed(new Runnable() {
                    public void run() {
                        CloudletClientThread clientThread;
                        try {
                            clientThread=new CloudletClientThread(discoveredAddr, discoveredPort, DiscoveryActivity.this);
                            clientThread.start();
                            clientThread.join();
                        } catch (InterruptedException ie) {
                            ie.printStackTrace();
                        }
                    }
                }, 100);
            }
        });

        /*get VM status*/
        waitVMbutton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                genericHandler.postDelayed(new Runnable() {
                    public void run() {
//                        showDialog();
                        int overlayType = radioGroupOverlayUrl.getCheckedRadioButtonId();
                        switch (overlayType){
                            case R.id.radioButtonDevice:
                                //Overlay from Device
                                InetAddress myLocalAddr = getIpAddress();
                                if (myLocalAddr!=null) {
                                    doWaitVM("http://" + myLocalAddr.getHostAddress() + MOBILE_OVERLAY_URL);
                                }
                                else
                                {
                                    Log.e(TAG, "getIPaddr returns null, retry...");
                                    notifyTextView("Problem getting IP addr", false);
                                }
                                break;
                            case R.id.radioButtonCloud:
                                //Overlay from Cloud
                                doWaitVM(CLOUD_OVERLAY_URL);
                                break;
                            case R.id.radioButtonCloudlet:
                                //Overlay on Cloudlet
                                doWaitVM(DEF_OVERLAY_URL);
                                break;
                            case -1:
                                notifyTextView("\nSelect type of overlay URL, Please", true);
                                break;
                        }
                    }
                }, 100);
            }
        });
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();  //stop Api Client
        super.onStop();
    }
    @Override
    protected void onStart() {
        mGoogleApiClient.connect();     //start Api Client
        Log.d(TAG, "Google Api Client started...");
        super.onStart();
    }

    /**---------------------------------------Google NSD---------------------------------------**/

    private void stopDiscovery() {
        stopDiscoveryButton.setEnabled(false);
        stopDiscoveryButton.setClickable(false);
        if (mDiscoveryListener!=null)
            mNsdManager.stopServiceDiscovery(mDiscoveryListener);

    }

    /*Local Service Resolutor*/
    public void initializeResolveListener() {
        mResolveListener = new NsdManager.ResolveListener() {

            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                // Called when the resolve fails.  Use the error code to debug.
                Log.e(TAG, "Resolve failed" + errorCode);
                notifyTextView("Sorry nothing found", true);
            }

            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfo) {
                Log.d(TAG, "Resolve Succeeded. " + serviceInfo);

                mService = serviceInfo;
                discoveredPort= mService.getPort();
                discoveredAddr=mService.getHost();

                Log.d(TAG, "Service resolved with host: " + discoveredAddr.getHostAddress() +
                        " and port: " + discoveredPort);
                notifyTextView("Cloudlet host --> " + discoveredAddr.getHostAddress() +
                        " port -->" + discoveredPort, false);
                if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                    Log.i(TAG, "External Media correctly mounted");
                    File logPrivFile = new File(getExternalFilesDir(null), "cloudletInfo.txt");
                    if (!logPrivFile.exists()){
                        Log.i(TAG,"Creating new Log File for Cloudlet");
                        try {
                            if (logPrivFile.createNewFile()) {
                                FileOutputStream outLog = new FileOutputStream(logPrivFile);
                                OutputStreamWriter outLogWriter = new OutputStreamWriter(outLog);
                                outLogWriter.write(discoveredAddr.getHostAddress());
                                outLogWriter.flush();
                                outLogWriter.close();
                                outLog.close();
                            }
                            else Log.e(TAG,"Error creating log file for Cloudlet, retry...");
                        }catch(IOException ioe){
                            Log.e(TAG, "Exception in creating log file");
                            ioe.printStackTrace();
                        }
                    }
                    else{
                        //a file with previous IP address exists
                        Log.i(TAG,"Updating Log File for Cloudlet");
                        List<String> addrsInFile = readPrivateTxtFile(PRIV_FILE_NAME);
                        if (addrsInFile!=null){//save last addr
                            try {
                                FileOutputStream outLog = new FileOutputStream(logPrivFile);
                                OutputStreamWriter outLogWriter = new OutputStreamWriter(outLog);
                                outLogWriter.write(addrsInFile.get(addrsInFile.size()-1)+"\n"+discoveredAddr.getHostAddress());
                                outLogWriter.flush();
                                outLogWriter.close();
                                outLog.close();
                            }catch(IOException ioe){
                                Log.e(TAG, "Exception in creating log file");
                                ioe.printStackTrace();
                            }
                        }
                        else{
                            Log.e(TAG, "Log file unavailable, no info to update.");
                            notifyTextView("Log file unavailable", false);
                        }
                    }
                }
                else {
                    Log.e(TAG, "External Media unavailable, no info for performing handoff.");
                    notifyTextView("External Media unavailable", false);
                }
            }
        };
    }

    public void initializeDiscoveryListener() {
        // Instantiate a new DiscoveryListener
        mDiscoveryListener = new NsdManager.DiscoveryListener() {
            //  Called as soon as service discovery begins.
            @Override
            public void onDiscoveryStarted(String regType) {
                Log.d(TAG, "Service discovery started");
            }
            @Override
            public void onServiceFound(NsdServiceInfo service) {
                // A service was found!  Do something with it.
                Log.d(TAG, "Service discovery success " + service);
                if (service.getServiceType().equals(SERVICE_TYPE)) {
                    // Service type is the string containing the protocol and
                    // transport layer for this service.
                    mNsdManager.resolveService(service, mResolveListener);
                }
                else Log.d(TAG, "Unknown Service Type: " + service.getServiceType());
            }
            @Override
            public void onServiceLost(NsdServiceInfo service) {
                // When the network service is no longer available.
                // Internal bookkeeping code goes here.
                Log.e(TAG, "service lost" + service);
                notifyTextView("Sorry Service was lost...", true);
            }
            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.i(TAG, "Discovery stopped: " + serviceType);
                notifyTextView("\nDiscovery Stopped", true);
            }
            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
                notifyTextView("Sorry, Discovery failed", true);
                mNsdManager.stopServiceDiscovery(this);
            }
            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
                notifyTextView("Sorry, Stop Discovery failed", true);
                mNsdManager.stopServiceDiscovery(this);
            }
        };
    }


    /**---------------------------------------TXT_VIEW---------------------------------------**/
    public void notifyTextView (final String msg, final boolean append) {
        genericHandler.postDelayed(new Runnable() {
            public void run() {
                showNotifTextView = (TextView) findViewById(R.id.showDiscoveryInfo);
                if (!append)
                    showNotifTextView.setText(msg);
                else
                    showNotifTextView.append(msg);
            }
        }, 100);
    }
    public TextView getTextView () {
        return (TextView)findViewById(R.id.showDiscoveryInfo);
    }

    /**---------------------------------------TXT_FILE---------------------------------------**/
    private List<String> readPrivateTxtFile(String fileName){
        List<String> addrs = null;
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                Log.i(TAG, "External Media correctly mounted");
                File logPrivFile = new File(getExternalFilesDir(null), fileName);
                if (logPrivFile.exists()) {
                    try {
                        BufferedReader br = new BufferedReader(new FileReader(logPrivFile));
                        addrs = new ArrayList</*String*/>();
                        String lastAddr;
                        int numLine=0;
                        while ((lastAddr = br.readLine()) != null) {
                            addrs.add(lastAddr);
                            Log.i(TAG,"FileLog Line " + numLine+" contains addr " +lastAddr);
                            numLine++;
                        }
                        br.close();
                    } catch (FileNotFoundException fnfe) {
                        Log.e(TAG, "Error get log file");
                        fnfe.printStackTrace();
                    } catch (IOException ioe) {
                        Log.e(TAG, "Exception while reading file");
                        ioe.printStackTrace();
                    }
                }
                else { //log file doesn't extist
                    Log.e(TAG, "Log file unavailable, no info to perform handoff.");
                    notifyTextView("Log file unavailable", false);
                }
            }
            else
                Log.e(TAG, "External Media unavailable, no info to perform handoff.");

        return addrs;
    }

    /*TEST READ FILE*/
    private void readPrivateCoordFile(String fileName) throws IOException {
        FileInputStream test = openFileInput(fileName);
        byte[] buffer = new byte[1024];
        StringBuffer fileContent = new StringBuffer("");
        int n;
        while ((n = test.read(buffer)) != -1)
        {
            fileContent.append(new String(buffer, 0, n));
        }
        Log.d(TAG, "LATITUDINI\n"+fileContent.toString());
    }

    public String getServiceLocalAddr() {
        return serviceLocalAddr;
    }
    public void setServiceLocalAddr(String serviceLocalAddr) {
        this.serviceLocalAddr = serviceLocalAddr;
    }
    public String getServiceMngmntAddr() {
        return serviceMngmntAddr;
    }
    public void setServiceMngmntAddr(String serviceMngmntAddr) {
        this.serviceMngmntAddr = serviceMngmntAddr;
    }

    /*get status of VM*/
    private void doWaitVM(String overlayType){
        WaitVMThread waitVMThread;// = null;
        try {
            waitVMThread=new WaitVMThread(discoveredAddr, discoveredPort, DiscoveryActivity.this, overlayType);
            waitVMThread.start();
            waitVMThread.join();
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }
    }

    private static InetAddress getIpAddress() {
        try {
            for (Enumeration en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = (NetworkInterface) en.nextElement();
                for (Enumeration enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = (InetAddress)enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()&&inetAddress instanceof Inet4Address) {
                        String ipAddress=inetAddress.getHostAddress();
                        Log.e("IP address",""+ipAddress);
                        return inetAddress;
                    }
                }
            }
        } catch (SocketException ex) {
            Log.e(TAG, "Socket exception in GetIP Address of Utilities" + ex.toString());
        }
        return null;

    }

    /*****************************LOCATION_METHODS_2017***************************/
    //To store parameters for requests
    protected void createLocationRequest() {
        Log.d(TAG, "Request Creation");
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000); //10 secs
        mLocationRequest.setFastestInterval(50000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        Log.d(TAG, "Request Created");
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "check Self Permission Failed");//TODO
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        //one shot Location
        /*
        Log.d(TAG, "Get Last Location...");
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {
            notifyTextView("Latitude = "+String.valueOf(mLastLocation.getLatitude())+"\n", false);
            notifyTextView("Longitude = "+String.valueOf(mLastLocation.getLongitude())+"\n", true);
        }
        //TODO check whether location updates are currently active
        */

        //request location update
        Log.d(TAG, "Get Current Location...");
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "Connection to position Suspended...");
        //TODO actions for connection suspended.
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "Location Changed...");
        mCurrentLocation = location;
        //TODO if last update too short don't write file
        mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
//        notifyTextView("Last Update Time = "+mLastUpdateTime+"\n",true);
//        notifyTextView("Current Latitude = "+String.valueOf(mCurrentLocation.getLatitude())+"\n",true);
//        notifyTextView("Current Longitude = "+String.valueOf(mCurrentLocation.getLongitude())+"\n",true);
        //TODO if file is too large, delete and create new one.
        writeLocationOnFile();
        notifyTextView("File Written at: "+mLastUpdateTime,true);
    }

    private void writeLocationOnFile(){
        OutputStreamWriter outputStreamWriterLat,outputStreamWriterLon;
        try {
            //check if file exist
            File fileLat = new File(DiscoveryActivity.this.getFilesDir().getAbsolutePath()
                    +"/"+USER_ID+"_lat.txt");
            File fileLon = new File(DiscoveryActivity.this.getFilesDir().getAbsolutePath()
                    +"/"+USER_ID+"_lon.txt");

            if(!fileLat.exists() && !fileLon.exists()) {
                Log.d(TAG,"Not existing file");
                outputStreamWriterLat = new OutputStreamWriter(
                        getApplicationContext().openFileOutput(USER_ID + "_lat.txt",
                                Context.MODE_PRIVATE));
                outputStreamWriterLon = new OutputStreamWriter(
                        getApplicationContext().openFileOutput(USER_ID + "_lon.txt",
                                Context.MODE_PRIVATE));
            }
            else {
                Log.d(TAG,"Existing File");
                outputStreamWriterLat = new OutputStreamWriter(
                        getApplicationContext().openFileOutput(USER_ID + "_lat.txt",
                                Context.MODE_APPEND));
                outputStreamWriterLon = new OutputStreamWriter(
                        getApplicationContext().openFileOutput(USER_ID + "_lon.txt",
                                Context.MODE_APPEND));
            }
            outputStreamWriterLat.append(mCurrentLocation.getLatitude()+"\n");
            outputStreamWriterLat.close();
            outputStreamWriterLon.append(mCurrentLocation.getLatitude()+"\n");
            outputStreamWriterLon.close();

            sendFileToCloud();

            //DEBUG
//            Log.d(TAG,DiscoveryActivity.this.getFilesDir().getAbsolutePath());
        }
        catch (FileNotFoundException fe){
            fe.printStackTrace();
            Log.e(TAG, "Error File not found");
        }
        catch (IOException ioe){
            ioe.printStackTrace();
            Log.e(TAG, "Error Write File");
        }
    }

    /**
     * curl -F userID_lat@="localpath" http://SERVER_IP:SERVER_PORT
     * */
    private /*void*/ String sendFileToCloud() throws IOException{
        try {

            //connection URL and RestTemplate instance
            String url = CLOUD_PREDICTION_URL;
            RestTemplate restTemplate = new RestTemplate();

            //Set Content-Type = multipart/form-data
            HttpHeaders requestHeaders = new HttpHeaders();
            requestHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
            //Populate data to POST
            MultiValueMap<String, Object> formData;
            Resource resource = new FileSystemResource(
                    DiscoveryActivity.this.getFilesDir().getAbsolutePath() + USER_ID + "_lat.txt");
            formData = new LinkedMultiValueMap<String, Object>();
            formData.add("description", "LatitudeFile");
            formData.add("file", resource);
            // Populate the MultiValueMap being serialized and headers in an HttpEntity object to use for the request
            HttpEntity<MultiValueMap<String, Object>> requestEntity =
                    new HttpEntity<MultiValueMap<String, Object>>(formData, requestHeaders);

            //POST request, response
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST,
                    requestEntity, String.class);

            // Return the response body to display to the user
            return response.getBody();

            //        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<String, Object>();
            //        parts.add("Content-Type", "image/jpeg");
            //        parts.add("file", resource);
        }
        catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return null;
    }
}
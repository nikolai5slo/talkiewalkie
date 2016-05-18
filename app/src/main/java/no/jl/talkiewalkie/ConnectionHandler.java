package no.jl.talkiewalkie;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.DnsSdServiceResponseListener;
import android.net.wifi.p2p.WifiP2pManager.DnsSdTxtRecordListener;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.util.Log;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * Created by nikolai5 on 5/10/16.
 */
public class ConnectionHandler extends BroadcastReceiver implements WifiP2pManager.ConnectionInfoListener{
    public static final String TAG = "ConnectionHandler";
    public static final String SERV_NAME="_talkie&walkie*";
    public static final String SERV_TYPE="_audio._udp";

    Activity activity;
    Channel mChannel;
    WifiP2pManager mManager;

    public ConnectionHandler(WifiP2pManager mManager, Channel mChannel, Activity activity){
        this.activity=activity;
        this.mChannel=mChannel;
        this.mManager=mManager;

    }
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            // Get the state of connection
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                Log.d(TAG, "WifiDirect connected");
            } else {
                Log.e(TAG, "WifiDirect connection failed.");
            }

        }else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            // When connection changed, request connection info

            NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

            if (networkInfo.isConnected()) {
                // We are connected with the other device, request connection
                // info to find group owner IP
                mManager.requestConnectionInfo(mChannel, this);
            }
        }
    }


    public void serviceRegistration(){
        // Record for service
        Map record = new HashMap();
        record.put("user", "Janez Kranjski" + (int)(Math.random() * 100000));

        // Create service
        WifiP2pDnsSdServiceInfo serviceInfo = WifiP2pDnsSdServiceInfo.newInstance(SERV_NAME, SERV_TYPE, record);

        mManager.addLocalService(mChannel, serviceInfo, new ActionListener() {
            @Override
            public void onSuccess() {
               Log.d(TAG, "Service registration successful.");
            }

            @Override
            public void onFailure(int reason) {
                Log.e(TAG, "Service registration failed.");
            }
        });
    }

    public void discoverServices(){

        // Prepare listener for discovering records
        DnsSdTxtRecordListener txtListener = new DnsSdTxtRecordListener() {
        @Override
        public void onDnsSdTxtRecordAvailable(String fullDomain, Map record, WifiP2pDevice device) {
                Log.d(TAG, "DnsSdTxtRecord available -" + record.toString());
            }
        };

        // Prepare listener for discovering actual service
        DnsSdServiceResponseListener servListener = new DnsSdServiceResponseListener() {
            @Override
            public void onDnsSdServiceAvailable(String instanceName, String registrationType,
                                                WifiP2pDevice device) {

                WifiP2pConfig config = new WifiP2pConfig();
                config.deviceAddress = device.deviceAddress;
                config.wps.setup = WpsInfo.PBC;

                mManager.connect(mChannel, config, new ActionListener() {
                    @Override
                    public void onSuccess() {
                       Log.d(TAG, "Request to connect to peer successful.");
                    }

                    @Override
                    public void onFailure(int reason) {
                        Log.e(TAG, "Request to connect to peer failed.");
                    }
                });

                Log.d(TAG, "New service discovered:" + instanceName);
            }
        };

        mManager.setDnsSdResponseListeners(mChannel, servListener, txtListener);



        // Create service discover request
        WifiP2pDnsSdServiceRequest serviceRequest = WifiP2pDnsSdServiceRequest.newInstance();
        mManager.addServiceRequest(mChannel, serviceRequest, new ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Service request successful.");
            }

            @Override
            public void onFailure(int reason) {
                Log.e(TAG, "Service request failed.");
            }
        });


        // Run service discover
        mManager.discoverServices(mChannel, new ActionListener() {

            @Override
            public void onSuccess() {
                Log.d(TAG, "Service discover successful.");
            }

            @Override
            public void onFailure(int code) {
                // Command failed.  Check for P2P_UNSUPPORTED, ERROR, or BUSY
                if (code == WifiP2pManager.P2P_UNSUPPORTED) {
                    Log.e(TAG, "P2P isn't supported on this device.");
                }else
                Log.e(TAG, "Service discover failed.");
            }
        });
    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info) {
        try {
            InetAddress groupOwnerAddress = InetAddress.getByName(info.groupOwnerAddress.getHostAddress());

        /*
        Log.d(TAG,"Group host address: " + info.groupOwnerAddress.getHostAddress());
        WifiManager wifii= (WifiManager) activity.getSystemService(Context.WIFI_SERVICE);
        DhcpInfo d=wifii.getDhcpInfo();
        Log.d(TAG,"Subnet mask: " +  intToIp(d.netmask));
        */



            // After the group negotiation, we can determine the group owner.
            if (info.groupFormed && info.isGroupOwner) {
                Log.d(TAG, "Connected as group over");
                clientReady(true, groupOwnerAddress);
                // Do whatever tasks are specific to the group owner.
                // One common case is creating a server thread and accepting
                // incoming connections.
            } else if (info.groupFormed) {
                Log.d(TAG, "Connected as client");
                clientReady(false, groupOwnerAddress);
                // The other device acts as the client. In this case,
                // you'll want to create a client thread that connects to the group
                // owner.
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

    }

    private LinkedList<ConnectionStatusListener> csListeners = new LinkedList<>();
    public void addConnectionStatusListener(ConnectionStatusListener csl){
        csListeners.add(csl);
    }

    public void removeConnectionStatusListener(ConnectionStatusListener csl){
        csListeners.remove(csl);
    }

    private void clientReady(boolean server, InetAddress srvAddr){
        for (ConnectionStatusListener l: csListeners)
            l.clientReady(server, srvAddr);
    }

    interface ConnectionStatusListener{
        void clientReady(boolean server, InetAddress srvAddr);
    }

    private String intToIp(int i) {
       return ((i >> 24 ) & 0xFF ) + "." +
                   ((i >> 16 ) & 0xFF) + "." +
                   ((i >> 8 ) & 0xFF) + "." +
                   ( i & 0xFF) ;
    }
}

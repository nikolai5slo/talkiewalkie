package no.jl.talkiewalkie;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pManager;

public class TalkActivity extends AppCompatActivity {
    private final IntentFilter intentFilter = new IntentFilter();
    private WifiP2pManager.Channel mChannel;
    private WifiP2pManager mManager;
    public static final String TAG = "TalkActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_talk);

        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);

        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);

        conHandler = new ConnectionHandler(mManager, mChannel, this);

        conHandler.serviceRegistration();
        conHandler.discoverServices();;
    }

    ConnectionHandler conHandler;
     /** register the BroadcastReceiver with the intent values to be matched */
    @Override
    public void onResume() {
        super.onResume();
        registerReceiver(conHandler, intentFilter);

    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(conHandler);
    }
}

package no.jl.talkiewalkie;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import java.net.InetAddress;
import java.net.SocketException;

public class TalkActivity extends AppCompatActivity {
    private final IntentFilter intentFilter = new IntentFilter();
    private WifiP2pManager.Channel mChannel;
    private WifiP2pManager mManager;
    public static final String TAG = "TalkActivity";
    private AudioInputStream receiver=null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_talk);

        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);

        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);

        conHandler = new ConnectionHandler(mManager, mChannel, this);

        conHandler.serviceRegistration();
        conHandler.discoverServices();;


        receiver = new AudioInputStream(2349, TalkActivity.this);

        receiver.start();
        conHandler.addConnectionStatusListener(new ConnectionHandler.ConnectionStatusListener() {
            @Override
            public void clientReady(boolean server, InetAddress srvAddr) {
                Log.d(TAG, "Address:" + srvAddr.getHostAddress());
            }
        });

        Button tB = (Button) findViewById(R.id.talkButton);
        try {
            final AudioStreamer streamer = new AudioStreamer(2349, 128, this);
            tB.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    Log.d(TAG, "Action "+event.getAction());
                    if(event.getAction() == MotionEvent.ACTION_DOWN){
                        streamer.startRec();
                        Log.d(TAG, "Action press");
                    }else if(event.getAction() == MotionEvent.ACTION_UP){
                        streamer.stopRec();
                        Log.d(TAG, "Action release");
                    }
                    return false;
                }
            });


        } catch (SocketException e) {
            e.printStackTrace();
        }

    }

    ConnectionHandler conHandler;
     /** register the BroadcastReceiver with the intent values to be matched */
    @Override
    public void onResume() {
        super.onResume();
        registerReceiver(conHandler, intentFilter);
        receiver = new AudioInputStream(2349, this);
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(conHandler);
        if(receiver!=null){
            receiver.halt();
        }
    }
}

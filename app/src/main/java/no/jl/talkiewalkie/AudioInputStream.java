package no.jl.talkiewalkie;

import android.app.Activity;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.net.wifi.WifiManager;
import android.provider.ContactsContract;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.Objects;

/**
 * Created by nikolai5 on 5/10/16.
 */
public class AudioInputStream extends Thread{
    public final static String TAG = "AudioInputStream";
    private LinkedList<InetAddress> clients=new LinkedList<>();
    private int port;
    private Context mContext;
    private AudioTrack track;
    public AudioInputStream(int udpPort, Context mContext){
        this.port=udpPort;
        this.mContext=mContext;
        track = new  AudioTrack(AudioManager.STREAM_VOICE_CALL, sampleRate, channelConfig, audioFormat , minBufSize, AudioTrack.MODE_STREAM);
    }

    private int sampleRate = 8000 ; // 44100 for music
    private int channelConfig = AudioFormat.CHANNEL_OUT_MONO;
    private int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    int minBufSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat);

    private boolean running=true;

    @Override
    public void run() {
        Log.d(TAG, "Server handler run...");
        WifiManager wifi = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        WifiManager.MulticastLock lock = wifi.createMulticastLock("talkiewalkie");

        try {
            DatagramSocket serverSocket = new DatagramSocket(port, InetAddress.getByName("0.0.0.0"));
            serverSocket.setBroadcast(true);
            //serverSocket.setSoTimeout(15000); //15 sec wait for the client to connect
            byte[] data = new byte[1024];
            DatagramPacket packet = new DatagramPacket(data, data.length);
            if(track.getState() == AudioTrack.STATE_UNINITIALIZED){
                throw new Exception("Can't initialize audio track");
            }
            track.play();

            while(true) {
                synchronized (this) {
                    if (!running) break;
                }
                try {
                    lock.acquire();
                    serverSocket.receive(packet);
                    track.write(data, 0, data.length);
                } catch (SocketException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                lock.release();
            }
            serverSocket.close();
            track.stop();
        }catch (SocketException e){
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void finalize(){
       track.release();
    }

    public synchronized void halt(){
        running=false;
    }

}

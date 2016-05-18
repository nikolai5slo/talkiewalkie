package no.jl.talkiewalkie;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * Created by nikolai5 on 5/11/16.
 */
class AudioStreamer implements Runnable{
    int port;
    boolean running=false;
    ArrayList<InetAddress> clients = new ArrayList<>();
    Context mContext;
    public static final String TAG ="AudioStreamer";
    private Thread thread=null;
    private AudioRecord recorder=null;

    private int sampleRate = 8000 ; // 44100 for music
    private int channelConfig = AudioFormat.CHANNEL_IN_MONO;
    private int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    int minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);

    public AudioStreamer(int port, int bufferSize, Context mContext) throws SocketException {
        this.port=port;
        this.mContext=mContext;
        recorder = new AudioRecord(MediaRecorder.AudioSource.DEFAULT,sampleRate,channelConfig,audioFormat,minBufSize);
    }

    InetAddress getBroadcastAddress() throws IOException {
        WifiManager wifi = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        DhcpInfo dhcp = wifi.getDhcpInfo();
        // handle null somehow

        int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
        byte[] quads = new byte[4];
        for (int k = 0; k < 4; k++)
          quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
        return InetAddress.getByAddress(quads);
    }


    @Override
    public void run() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
        if(ContextCompat.checkSelfPermission(mContext, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions((Activity)mContext, new String[]{Manifest.permission.RECORD_AUDIO}, 20);
        }
        //recorder = findAudioRecord();
        try {
            if (recorder.getState() == AudioRecord.STATE_UNINITIALIZED) {
                throw new Exception("Can't initialize audio recorder");
            }

            recorder.startRecording();

            //DatagramPacket packet = new DatagramPacket(data, data.length);

            try {
                DatagramSocket socket = new DatagramSocket();
                socket.setBroadcast(true);
                while (true) {
                    synchronized (this){
                        if(!running) break;
                    }
                    byte[] data = new byte[1024];
                    minBufSize = recorder.read(data, 0, data.length);
                    DatagramPacket packet = new DatagramPacket(data, data.length, getBroadcastAddress(), port);
                    socket.send(packet);
                    Log.d(TAG, "Send " + minBufSize);
                }
            } catch (SocketException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            recorder.stop();
            Log.d(TAG, "Released");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    void startRec(){
        if(!running) {
            running = true;
            thread = new Thread(this);
            thread.start();
        }
    }
    void stopRec(){
        if(running) {
            running = false;
        }
    }

    protected synchronized void finalize(){
        if(recorder!=null) {
            recorder.release();
        }
    }

}

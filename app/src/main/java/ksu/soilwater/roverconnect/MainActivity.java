package ksu.soilwater.roverconnect;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.DocumentsContract;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.room.Room;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.stream.IntStream;

import static android.app.PendingIntent.getActivity;
import static android.content.ContentValues.TAG;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.data.kml.KmlLayer;

import org.xmlpull.v1.XmlPullParserException;

import ksu.soilwater.roverconnect.storage.AppDatabase;
import ksu.soilwater.roverconnect.storage.Measurement;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private String deviceName = null;
    private String deviceAddress;
    private SupportMapFragment mapFragment;
    public static Handler handler;
    public static BluetoothSocket mmSocket;
    public static ConnectedThread connectedThread;
    public static CreateConnectThread createConnectThread;
    List<Integer> points;
    AppDatabase db;
    GoogleMap googleMap=null;
    Integer seconds=0;

    private final static int CONNECTING_STATUS = 1; // used in bluetooth handler to identify message status
    private final static int MESSAGE_READ = 2; // used in bluetooth handler to identify message update

    TextView value1;
    TextView value2;
    TextView value3;
    TextView value4;
    TextView value5;
    TextView value6;
    TextView nc1,nc2,nc3,nc4,nc5,nc6,nc7,nc8;
    private void update(Measurement m, String volts, int[] nc){
        value1.setText(m.ncTotal);
        value2.setText(m.stdDev);
        value3.setText(m.id);
        value4.setText(m.time);
        value6.setText(volts);

        nc1.setText(String.format("%d", nc[0]));
        nc2.setText(String.format("%d", nc[1]));
        nc3.setText(String.format("%d", nc[2]));
        nc4.setText(String.format("%d", nc[3]));
        nc5.setText(String.format("%d", nc[4]));
        nc6.setText(String.format("%d", nc[5]));
        nc7.setText(String.format("%d", nc[6]));
        nc8.setText(String.format("%d", nc[7]));


        if(googleMap!=null){
            LatLng loc=new LatLng(m.lat,m.lng);
            googleMap.animateCamera(CameraUpdateFactory.newLatLng(loc));
            int fill=ColorUtils.blendARGB(0xff0000ff, 0xffff0000, (Float.valueOf(m.ncTotal)-190)/250);
            CircleOptions circleOptions=new CircleOptions();
            circleOptions.center(loc);
            circleOptions.radius(10);
            circleOptions.fillColor(fill);
            circleOptions.strokeColor(fill);
            googleMap.addCircle(circleOptions);
        }
    }
    private static final int PICK_KML_FILE = 2;

    private void openFile(Uri pickerInitialUri) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("application/vnd.google-earth.kml+xml");
        // Optionally, specify a URI for the file that should appear in the
        // system file picker when it loads.
        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri);

        startActivityForResult(intent, PICK_KML_FILE);
        intent.getAction();
    }
    private void addKML(){
        openFile(Uri.parse("/sdcard"));
        //KmlLayer layer = new KmlLayer(googleMap, R.raw.geojson_file, getApplicationContext());

    }
    private double calculateSD(int numArray[])
    {
        double sum = 0.0, standardDeviation = 0.0;
        int length = numArray.length;

        for(int num : numArray) {
            sum += (Double.valueOf(num));
        }

        double mean = sum/length;

        for(double num: numArray) {
            standardDeviation += Math.pow(num - mean, 2);
        }

        return Math.sqrt(standardDeviation/length);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Uri content_describer = data.getData();
        String src = content_describer.getPath();
        Log.d("FILE",src);
        File source = new File(src);
        Log.d("FILE",((Boolean)source.exists()).toString());
        try {
            InputStream stream=getContentResolver().openInputStream(content_describer);
            //InputStream stream=new FileInputStream(source);
            KmlLayer layer = new KmlLayer(googleMap, stream, getApplicationContext());
            layer.addLayerToMap();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (XmlPullParserException x){
            x.printStackTrace();
        } catch (java.io.IOException f){
            f.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        AppDatabase db = Room.databaseBuilder(getApplicationContext(),
                AppDatabase.class, "cache-data").build();

         value1=findViewById(R.id.value1);
         value2=findViewById(R.id.value2);
         value3=findViewById(R.id.value3);
         value4=findViewById(R.id.value4);
         value5=findViewById(R.id.value5);
         value6=findViewById(R.id.value6);

         nc1=findViewById(R.id.nc1);
        nc2=findViewById(R.id.nc2);
        nc3=findViewById(R.id.nc3);
        nc4=findViewById(R.id.nc4);
        nc5=findViewById(R.id.nc5);
        nc6=findViewById(R.id.nc6);
        nc7=findViewById(R.id.nc7);
        nc8=findViewById(R.id.nc8);


         points=new ArrayList<Integer>();

         new Timer().scheduleAtFixedRate(new TimerTask() {
             @Override
             public void run() {
                 seconds++;
                 runOnUiThread(new Runnable() {
                     @Override
                     public void run() {
                         value5.setText(seconds.toString());
                     }
                 });
             }
         }, 0,1000);

        // UI Initialization
        final Button buttonConnect = findViewById(R.id.buttonConnect);
        final Button buttonKML = findViewById(R.id.buttonKML);
        final Button buttonClean = findViewById(R.id.buttonClean);

        final Toolbar toolbar = findViewById(R.id.toolbar);
        final ProgressBar progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);

        // If a bluetooth device has been selected from SelectDeviceActivity
        deviceName = getIntent().getStringExtra("deviceName");
        if (deviceName != null){
            // Get the device address to make BT Connection
            deviceAddress = getIntent().getStringExtra("deviceAddress");
            // Show progree and connection status
            toolbar.setSubtitle("Connecting to " + deviceName + "...");
            progressBar.setVisibility(View.VISIBLE);
            buttonConnect.setEnabled(false);

            /*
            This is the most important piece of code. When "deviceName" is found
            the code will call a new thread to create a bluetooth connection to the
            selected device (see the thread code below)
             */
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            createConnectThread = new CreateConnectThread(bluetoothAdapter,deviceAddress);
            createConnectThread.start();
        }

        /*
        Second most important piece of Code. GUI Handler
         */
        handler = new Handler(Looper.getMainLooper()) {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void handleMessage(Message msg){
                switch (msg.what){
                    case CONNECTING_STATUS:
                        switch(msg.arg1){
                            case 1:
                                toolbar.setSubtitle("Connected to " + deviceName);
                                progressBar.setVisibility(View.GONE);
                                buttonConnect.setEnabled(true);
                                break;
                            case -1:
                                toolbar.setSubtitle("Device fails to connect");
                                progressBar.setVisibility(View.GONE);
                                buttonConnect.setEnabled(true);
                                break;
                        }
                        break;

                    case MESSAGE_READ:
                        String roverMsg = msg.obj.toString(); // Read message from Arduino
                        String[] packets = roverMsg.split("\\,");
                        Log.d("Packet",packets.toString());
                        int nc[]=new int[8];
                        Float volts=Float.parseFloat(packets[9].trim());
                        nc[0]= Integer.parseInt(packets[10].trim());
                        nc[1]= Integer.parseInt(packets[11].trim());
                        nc[2]= Integer.parseInt(packets[12].trim());
                        nc[3]= Integer.parseInt(packets[13].trim());
                        nc[4]= Integer.parseInt(packets[14].trim());
                        nc[5]= Integer.parseInt(packets[15].trim());
                        nc[6]= Integer.parseInt(packets[16].trim());
                        nc[7]= Integer.parseInt(packets[17].trim());
                        Integer count= 0;
                        for (int neutron: nc) {
                           count+=neutron;
                        }
                        seconds=0;
                        Double lat = Double.parseDouble(packets[27].trim());
                        Double lng = -1*Double.parseDouble(packets[28].trim());
                        Double stddev = calculateSD(nc);
                        Log.e("STD: ",stddev.toString());
                        points.add(count);
                        Measurement m=new Measurement();
                        m.ncTotal=count.toString();
                        m.stdDev= String.format("%.3f", stddev);
                        m.id=packets[0].trim();
                        m.lat=lat;
                        m.lng=lng;
                        m.time=packets[1];
                        update(m, String.format("%.2f", volts), nc);
                        db.measurementData().insertAll(m);

                        break;
                }
            }
        };

        // Select Bluetooth Device
        buttonConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Move to adapter list
                Intent intent = new Intent(MainActivity.this, SelectDeviceActivity.class);
                startActivity(intent);
            }
        });
        buttonKML.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Move to adapter list
                addKML();
            }
        });
        buttonClean.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Move to adapter list
                googleMap.clear();
                db.measurementData().clearTable();
            }
        });

    }



    @Override
    public void onMapReady(GoogleMap gm) {
        googleMap=gm;
        List<Measurement> cache= db.measurementData().getAll();
        for (Measurement m:cache) {
            int[]nc_temp={0,0,0,0,0,0,0,0};
            update(m,"--", nc_temp);
        }
    }

    /* ============================ Thread to Create Bluetooth Connection =================================== */
    public static class CreateConnectThread extends Thread {

        public CreateConnectThread(BluetoothAdapter bluetoothAdapter, String address) {
            /*
            Use a temporary object that is later assigned to mmSocket
            because mmSocket is final.
             */
            BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(address);
            BluetoothSocket tmp = null;
            UUID uuid = bluetoothDevice.getUuids()[0].getUuid();

            try {
                /*
                Get a BluetoothSocket to connect with the given BluetoothDevice.
                Due to Android device varieties,the method below may not work fo different devices.
                You should try using other methods i.e. :
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
                 */
                tmp = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(uuid);

            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            bluetoothAdapter.cancelDiscovery();
            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();
                Log.e("Status", "Device connected");
                handler.obtainMessage(CONNECTING_STATUS, 1, -1).sendToTarget();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                try {
                    mmSocket.close();
                    Log.e("Status", "Cannot connect to device");
                    handler.obtainMessage(CONNECTING_STATUS, -1, -1).sendToTarget();
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                }
                return;
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            connectedThread = new ConnectedThread(mmSocket);
            connectedThread.run();
        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
        }
    }

    /* =============================== Thread for Data Transfer =========================================== */
    public static class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes = 0; // bytes returned from read()
            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    /*
                    Read from the InputStream from Arduino until termination character is reached.
                    Then send the whole String message to GUI Handler.
                     */
                    buffer[bytes] = (byte) mmInStream.read();
                    String readMessage;
                    if (buffer[bytes] == '\n'){
                        readMessage = new String(buffer,0,bytes);
                        Log.e("Arduino Message",readMessage);
                        handler.obtainMessage(MESSAGE_READ,readMessage).sendToTarget();
                        bytes = 0;
                    } else {
                        bytes++;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }


        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

    /* ============================ Terminate Connection at BackPress ====================== */
    @Override
    public void onBackPressed() {
        // Terminate Bluetooth Connection and close app
        if (createConnectThread != null){
            createConnectThread.cancel();
        }
        Intent a = new Intent(Intent.ACTION_MAIN);
        a.addCategory(Intent.CATEGORY_HOME);
        a.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(a);
    }
}

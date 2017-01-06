package com.empers.rssi_analyzer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.OnScrollListener;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.NumberPicker;

import com.empers.rssi_analyzer.adapters.NodeAdapter;
import com.empers.rssi_analyzer.ble.BluetoothLeService;
import com.empers.rssi_analyzer.ble.BluetoothSPP;
import com.empers.rssi_analyzer.ble.BluetoothState;
import com.empers.rssi_analyzer.ble.GattAttributes;
import com.empers.rssi_analyzer.database.GroupDB;
import com.empers.rssi_analyzer.database.NodeDB;
import com.empers.rssi_analyzer.objects.Group;
import com.empers.rssi_analyzer.objects.Node;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@SuppressLint("NewApi")
public class MainActivity extends AppCompatActivity {
	private static final String TAG = MainActivity.class.getSimpleName();
	private static int mScanNode = 0;
	private static boolean isPatchScan = false;
	//----------- use for BLE ----------------------
	private String mDeviceName;
	private String mDeviceAddress = "";
	private BluetoothLeService mBluetoothLeService;
	private BluetoothGattCharacteristic mNotifyCharacteristic;
	private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<>();
	private boolean mConnected = false;
    private boolean isFabShowing = true;
	//---------------------------------------------
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothSPP mBluetooth;
    private FloatingActionButton fab1;
    private RecyclerView mRecList;
    private NodeAdapter mAdapter;
    private int mGroupID = 0;
    private String mGroupName;
    private NodeDB database;
    private List<Node> mNodes;
    private BTApp btApp;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

        btApp = (BTApp)getApplication();
        mGroupID = getIntent().getIntExtra("GROUP_ID", 1);
        mGroupName = getIntent().getStringExtra("GROUP_NAME");
		Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
		toolbar.setTitle(mGroupName);
		setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		fab1 = (FloatingActionButton) findViewById(R.id.fab1);
		fab1.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
                isPatchScan = true;
                try {
                    int node_id = mNodes.get(mScanNode).getNodeId();
                    retrieveRssi(node_id);
                    mAdapter.setNodeScan(node_id, true, 0);
                } catch (ArrayIndexOutOfBoundsException ex) {
                    ex.printStackTrace();
                    isPatchScan = false;
                } catch (NullPointerException ex) {
                    ex.printStackTrace();
                    isPatchScan = false;
                }
			}			
		});

		mRecList = (RecyclerView)findViewById(R.id.recycler);
		mRecList.setHasFixedSize(true);
		LinearLayoutManager llm = new LinearLayoutManager(getApplicationContext());
		llm.setOrientation(LinearLayoutManager.VERTICAL);
		mRecList.setLayoutManager(llm);
        mRecList.addOnScrollListener(new OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 0) {
                    hideFab();
                    //fab1.setVisibility(View.INVISIBLE);
                } else if (dy < 0) {
                    showFab();
                    //fab1.setVisibility(View.VISIBLE);
                }
            }
        });

        new AsyncTask<Void, Void, List<Node>>() {
            @Override
            protected List<Node> doInBackground(Void... params) {
                database = new NodeDB(getApplicationContext());
                return database.getNodes(mGroupID);
            }

            @Override
            protected void onPostExecute(List<Node> nodes) {
                super.onPostExecute(nodes);
                if(nodes != null && nodes.size() > 0) {
                    mNodes = nodes;
                    mAdapter = new NodeAdapter(MainActivity.this, nodes);
                    mRecList.setAdapter(mAdapter);
                } else {
                    showAddNewNodeDialog();
                }
            }
        }.execute();
        mBluetoothAdapter = ((BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();

        mDeviceAddress = getSharedPreferences("DEVICES", MODE_PRIVATE).getString("MAC_" + mGroupID, "");
        if(mDeviceAddress.isEmpty()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle(R.string.empty_mac_title).setMessage(R.string.empty_mac_message);
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    scanBTMac();
                }
            });
            builder.setNegativeButton(R.string.back, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            });
            AlertDialog alert = builder.create();
            //alert.setCanceledOnTouchOutside(false);
            alert.setCancelable(false);
            alert.setOnKeyListener(new DialogInterface.OnKeyListener() {
                @Override
                public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                    if (keyCode == KeyEvent.KEYCODE_BACK) {
                        dialog.dismiss();
                        finish();
                    }
                    return true;
                }
            });
            alert.show();
        } else {
            Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
            bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        }
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction("com.bt.action_scan");
        registerReceiver(mGattUpdateReceiver, intentFilter);
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, BluetoothState.REQUEST_ENABLE_BT);
        }
    }

    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()){
            case android.R.id.home:
                onBackPressed();
                break;
            case R.id.action_new:
                showAddNewNodeDialog();
                break;
            case R.id.action_edit:
                showEditGroupDialog();
                break;
			case R.id.action_search_ble:	// connect to BT
                scanBTMac();
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onPause() {
		super.onPause();
	    unregisterReceiver(mGattUpdateReceiver);
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		if(mBluetooth != null) {
			mBluetooth.stopService();
		}
        try {
            unbindService(mServiceConnection);
        } catch(IllegalArgumentException ex) {
            ex.printStackTrace();
        }
        mBluetoothLeService = null;
	}

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 101: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    IntentIntegrator integrator = new IntentIntegrator(this);
                    integrator.initiateScan();
                }
            }
        }
    }

    @Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		if(requestCode == BluetoothState.REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_CANCELED) {
                finish();
                return;
            }
		} else {
            try {
                IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
                if (scanResult != null) {
                    final String re = scanResult.getContents();
                    if (!re.isEmpty()) {
                        mDeviceAddress = re;
                        try {
                            unbindService(mServiceConnection);
                        } catch (IllegalArgumentException ex) {
                            ex.printStackTrace();
                        }
                        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
                        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                getSharedPreferences("DEVICES", MODE_PRIVATE).edit()
                                        .putString("MAC_" + mGroupID, re).apply();
                            }
                        }).start();
                    }
                }
            } catch (NullPointerException ex) {
                ex.printStackTrace();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
	}

    private void showAddNewNodeDialog() {
        LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.dialog_new_node,
                (ViewGroup)findViewById(android.R.id.content),
                false);

        final NumberPicker nodePicker = (NumberPicker)layout.findViewById(R.id.numberPicker);
        nodePicker.setMinValue(1);
        nodePicker.setMaxValue(255);
        nodePicker.setValue(1);
        final EditText nameTV = (EditText)layout.findViewById(R.id.input_name);

        android.support.v7.app.AlertDialog.Builder builder =
                new android.support.v7.app.AlertDialog.Builder(MainActivity.this).setView(layout);
        builder.setTitle(getString(R.string.new_node));
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int node_id = nodePicker.getValue();
                String inputName = nameTV.getText().toString();
                if(inputName.isEmpty()) {
                    nameTV.setError(getString(R.string.error_name));
                    nameTV.requestFocus();
                } else {
                    new AsyncTask<Object, Void, List<Node>>() {
                        @Override
                        protected List<Node> doInBackground(Object... params) {
                            Node node1 = new Node();
                            node1.setNodeId(Integer.parseInt(params[0].toString()));
                            node1.setDisplayName(params[1].toString());
                            node1.setGroupId(mGroupID);
                            database.addNode(node1);
                            return database.getNodes(mGroupID);
                        }

                        @Override
                        protected void onPostExecute(List<Node> nodes) {
                            super.onPostExecute(nodes);
                            if(mAdapter == null) {
                                mAdapter = new NodeAdapter(MainActivity.this, nodes);
                                mRecList.setAdapter(mAdapter);
                            } else {
                                mAdapter.updateData(nodes);
                            }
                            mNodes = nodes;
                        }
                    }.execute(node_id, inputName);
                }
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        android.support.v7.app.AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void showEditGroupDialog() {
        View viewInflated = LayoutInflater.from(MainActivity.this)
                .inflate(R.layout.dialog_name, (ViewGroup)findViewById(android.R.id.content), false);
        final EditText nameText = (EditText)viewInflated.findViewById(R.id.input_name);
        nameText.setText(mGroupName);
        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(MainActivity.this);
        builder.setView(viewInflated);
        builder.setPositiveButton(getString(android.R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(!nameText.getText().toString().isEmpty()) {
                    String inputName = nameText.getText().toString();
                    if(!inputName.equals(mGroupName)) {
                        getSupportActionBar().setTitle(inputName);
                        new AsyncTask<String, Void, Void>() {
                            @Override
                            protected Void doInBackground(String... params) {
                                Group group1 = new Group();
                                group1.setGroupName(params[0]);
                                group1.setGroupId(mGroupID);
                                GroupDB db = new GroupDB(getApplicationContext());
                                db.updateGroup(group1);
                                db.close();
                                return null;
                            }
                        }.execute(inputName);
                    }
                }
            }
        });
        builder.setNegativeButton(getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }

    private void displayGattServices(final List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                mGattCharacteristics = new ArrayList<>();
                // Loops through available GATT Services.
                for (BluetoothGattService gattService : gattServices) {
                    List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
                    ArrayList<BluetoothGattCharacteristic> charas = new ArrayList<>();
                    // Loops through available Characteristics.
                    for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                        charas.add(gattCharacteristic);
                        String uuid = gattCharacteristic.getUuid().toString();
                        printLog("UUID = " + uuid);
                        if(uuid.equalsIgnoreCase(GattAttributes.BLE_TX_CHANNEL)){
                            btApp.setTX(gattCharacteristic);
                            printLog("TX_UUID = " + uuid);
                        } else if(uuid.equalsIgnoreCase(GattAttributes.BLE_RX_CHANNEL)){
                            btApp.setRX(gattCharacteristic);
                            mBluetoothLeService.setCharacteristicNotification(gattCharacteristic, true);
                            printLog("RX_UUID = " + uuid);
                        }
                    }
                    mGattCharacteristics.add(charas);
                }
                return null;
            }
        }.execute();
    }

	public void ble_send(String data){
		if(mConnected){
			btApp.getTX().setValue(data);
			mBluetoothLeService.writeCharacteristic(btApp.getTX(), false);
		}
	}

    public void retrieveRssi(int nodeID){
        String msg = String.format(Locale.getDefault(), "{\"CMD\":\"scan\",\"ID\":%d}\n", nodeID);
        ble_send(msg);
        //mBluetooth.send(msg, true);
    }

    // parse the result data from ZWAVE
    public void parseResult(String result){
        try {
            JSONObject jObject = new JSONObject(result);
            int id = jObject.getInt("NodeID");
            int rssi = jObject.getInt("rssi");
            mAdapter.setNodeScan(id, false, rssi);

            if(isPatchScan) {
                mScanNode++;
                try {
                    int node_id = mNodes.get(mScanNode).getNodeId();
                    retrieveRssi(node_id);
                    mAdapter.setNodeScan(node_id, true, 0);
                } catch (ArrayIndexOutOfBoundsException ex) {
                    ex.printStackTrace();
                    isPatchScan = false;
                    mScanNode = 0;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // show text to buffer
    private void printLog(String msg){
        Log.d(TAG, "" + msg);
    }

    // find my BT device
    public void scanBTMac(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{ Manifest.permission.CAMERA }, 101);
        } else {
            IntentIntegrator integrator = new IntentIntegrator(this);
            integrator.initiateScan();
        }
    }

    private void hideFab() {
        if (isFabShowing) {
            isFabShowing = false;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                final Point point = new Point();
                getWindow().getWindowManager().getDefaultDisplay().getSize(point);
                final float translation = fab1.getY() - point.y;
                fab1.animate().translationYBy(-translation).start();
            } else {
                Animation animation = AnimationUtils.makeOutAnimation(MainActivity.this, true);
                animation.setFillAfter(true);
                animation.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {}

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        fab1.setClickable(false);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {}
                });
                fab1.startAnimation(animation);
            }
        }
    }

    private void showFab() {
        if (!isFabShowing) {
            isFabShowing = true;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                fab1.animate().translationY(0).start();
            } else {
                Animation animation = AnimationUtils.makeInAnimation(MainActivity.this, false);
                animation.setFillAfter(true);
                animation.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {}

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        fab1.setClickable(true);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {}
                });
                fab1.startAnimation(animation);
            }
        }
    }

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection(){
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                printLog("Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            printLog("..mBluetoothLeService.connect to " + mDeviceAddress);
            mBluetoothLeService.connect(mDeviceAddress);
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            // TODO Auto-generated method stub
            printLog("..onServiceDisconnected ");
            mBluetoothLeService = null;
        }
    };

    /**
     // Handles various events fired by the Service.
     // ACTION_GATT_CONNECTED: connected to a GATT server.
     // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
     // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
     // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
     //                        or notification operations.
     */
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                printLog("ACTION_GATT_CONNECTED");
                //                updateConnectionState(R.string.connected);
                //                invalidateOptionsMenu();
            } else if(BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                printLog("ACTION_GATT_DISCONNECTED");
            } else if(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                printLog("ACTION_GATT_SERVICES_DISCOVERED");
                try {
                    displayGattServices(mBluetoothLeService.getSupportedGattServices());
                } catch (NullPointerException ex) {
                    ex.printStackTrace();
                }
                //mBluetoothLeService.setCharacteristicNotification(btApp.getRX(), true);	// enable read callback
            } else if(BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                printLog("ACTION_DATA_AVAILABLE=" + intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
                parseResult(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
                //	 displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            } else if(action.equals("com.bt.action_scan")) {
                int nodeID = intent.getIntExtra("NODE_ID", 0);
                retrieveRssi(nodeID);
            }
        }
    };
}

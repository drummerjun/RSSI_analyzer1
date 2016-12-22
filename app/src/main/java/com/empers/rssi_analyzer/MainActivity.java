package com.empers.rssi_analyzer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.NumberPicker;

import com.empers.rssi_analyzer.Adapter.NodeAdapter;
import com.empers.rssi_analyzer.database.GroupDB;
import com.empers.rssi_analyzer.database.NodeDB;
import com.empers.rssi_analyzer.objects.Group;
import com.empers.rssi_analyzer.objects.Node;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

@SuppressLint("NewApi")
public class MainActivity extends AppCompatActivity {
	private static final String TAG = MainActivity.class.getSimpleName();
	private static int mScanNode = 0;
	private static int mScanLoop = 0;
	//----------- use for BLE ----------------------
	private String mDeviceName;
	private String mDeviceAddress = "50:65:83:6A:90:FD";
	private BluetoothLeService mBluetoothLeService;
	private BluetoothGattCharacteristic mNotifyCharacteristic;
	private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<>();
	private boolean mConnected = false;
	private BluetoothGattCharacteristic tx_channel;
	private BluetoothGattCharacteristic rx_channel;
	//---------------------------------------------
    private BluetoothSPP mBluetooth;
    private FloatingActionButton fab1;
    private RecyclerView mRecList;
    private NodeAdapter mAdapter;
    private int mSID = 1;
    private int mGroupID = 0;
    private String mGroupName;
    private NodeDB database;
    private List<Node> mNodes;

	  // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection(){
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                showText("Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            showText("..mBluetoothLeService.connect to "+mDeviceAddress);
            mBluetoothLeService.connect(mDeviceAddress);			
		}
		@Override
		public void onServiceDisconnected(ComponentName name) {
			// TODO Auto-generated method stub
			 showText("..onServiceDisconnected ");
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
	             showText("ACTION_GATT_CONNECTED");
	//                updateConnectionState(R.string.connected);
	//                invalidateOptionsMenu();
			 }else if(BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)){
				 mConnected = false;
				 showText("ACTION_GATT_DISCONNECTED");
			 }else if(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)){
				// Show all the supported services and characteristics on the user interface.
				 showText("ACTION_GATT_SERVICES_DISCOVERED");
	             displayGattServices(mBluetoothLeService.getSupportedGattServices());
	             setup_ble_channel();
			 }else if(BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)){
			//	 displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
				 showText("ACTION_DATA_AVAILABLE="+intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
			 }			
		}
    };
    
    /**
    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    */
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
                = new ArrayList<>();
        mGattCharacteristics = new ArrayList<>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<>();
            uuid = gattService.getUuid().toString();
 //           showText("s_uuid="+uuid);
            currentServiceData.put(Constants.LIST_NAME, SampleGattAttributes.lookup(uuid, unknownServiceString));
            currentServiceData.put(Constants.LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData = new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas = new ArrayList<BluetoothGattCharacteristic>();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<>();
                uuid = gattCharacteristic.getUuid().toString();
     //           showText("c_uuid="+uuid);
                if(uuid.equalsIgnoreCase(SampleGattAttributes.BLE_TX_CHANNEL)){
                	tx_channel = gattCharacteristic;
                	showText("tx_uuid="+uuid);
                }
                if(uuid.equalsIgnoreCase(SampleGattAttributes.BLE_RX_CHANNEL)){
                	rx_channel = gattCharacteristic;
                	showText("rx_uuid="+uuid);
                }
                currentCharaData.put(Constants.LIST_NAME, SampleGattAttributes.lookup(uuid, unknownCharaString));
                currentCharaData.put(Constants.LIST_UUID, uuid);
                gattCharacteristicGroupData.add(currentCharaData);
            }
            mGattCharacteristics.add(charas);
            gattCharacteristicData.add(gattCharacteristicGroupData);
        }
    }
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
        mGroupID = getIntent().getIntExtra("GROUP_ID", 1);
        mGroupName = getIntent().getStringExtra("GROUP_NAME");
		Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
		toolbar.setTitle(mGroupName);
		setSupportActionBar(toolbar);
        try {
            if(isTaskRoot()) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            } else {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
        } catch(NullPointerException ex) {
            ex.printStackTrace();
        }

		fab1 = (FloatingActionButton) findViewById(R.id.fab1);
		fab1.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				mScanNode = 0;
				mScanLoop = 10;
                if(mScanNode < mNodes.size()){
                    int node_id = mNodes.get(mScanNode).getNodeId();
                    testZWAVE_RSSI(node_id);
                    mAdapter.setNodeScan(node_id, true, 0);
                }
			}			
		});

		mRecList = (RecyclerView)findViewById(R.id.recycler);
		mRecList.setHasFixedSize(true);
		LinearLayoutManager llm = new LinearLayoutManager(getApplicationContext());
		llm.setOrientation(LinearLayoutManager.VERTICAL);
		mRecList.setLayoutManager(llm);

        database = new NodeDB(getApplicationContext());
        new AsyncTask<Void, Void, List<Node>>() {
            @Override
            protected List<Node> doInBackground(Void... params) {
                return database.getNodes(mGroupID);
            }

            @Override
            protected void onPostExecute(List<Node> nodes) {
                super.onPostExecute(nodes);
                mNodes = nodes;
                mAdapter = new NodeAdapter(MainActivity.this, nodes);
                mRecList.setAdapter(mAdapter);
            }
        }.execute();

		mBluetooth = new BluetoothSPP(getApplicationContext());
		mBluetooth.setupService();
		mBluetooth.startService(BluetoothState.DEVICE_OTHER);
		
		// Listener for data receiving
		mBluetooth.setOnDataReceivedListener(new BluetoothSPP.OnDataReceivedListener(){
			@Override
			public void onDataReceived(byte[] data, String message) {
				// TODO Auto-generated method stub
				showText("RCV="+message);
				parseResult(message);
				// scan for the following
				
				mScanNode++;
				// loop scan
				if(mScanNode < mNodes.size() ){
					testZWAVE_RSSI(mNodes.get(mScanNode).getNodeId());
				}else{
					mScanLoop--;
					if(mScanLoop > 0 ){
						mScanNode = 0;
						testZWAVE_RSSI(mNodes.get(mScanNode).getNodeId());
					}
				}
				
			}			
		});

		// Listener for bluetooth connection status
		mBluetooth.setBluetoothConnectionListener(new BluetoothSPP.BluetoothConnectionListener(){

			@Override
			public void onDeviceConnected(String name, String address) {
				// TODO Auto-generated method stub
				showText("onDeviceConnected");
			}

			@Override
			public void onDeviceDisconnected() {
				// TODO Auto-generated method stub
				showText("onDeviceDisconnected");
			}

			@Override
			public void onDeviceConnectionFailed() {
				// TODO Auto-generated method stub
				showText("onDeviceConnectionFailed");
			}
			
		});
		
		// Listener when bluetooth connection has changed
		mBluetooth.setBluetoothStateListener(new BluetoothSPP.BluetoothStateListener(){

			@Override
			public void onServiceStateChanged(int state) {
				// TODO Auto-generated method stub
				if(state == BluetoothState.STATE_CONNECTED){
					// Do something when successfully connected
					showText("BluetoothState.STATE_CONNECTED");
				} else if(state == BluetoothState.STATE_CONNECTING){
					// Do something while connecting
					showText("BluetoothState.STATE_CONNECTING");
				} else if(state == BluetoothState.STATE_LISTEN){
					// Do something when device is waiting for connection
					showText("BluetoothState.STATE_LISTEN");
				} else if(state == BluetoothState.STATE_NONE){
					// Do something when device don't have any connection
					showText("BluetoothState.STATE_NONE");
				}				
			}			
		});

		// Using auto connection

		mBluetooth.autoConnect("Keyword for filter paired device");
		
		// Listener for auto connection
		mBluetooth.setAutoConnectionListener(new BluetoothSPP.AutoConnectionListener(){

			@Override
			public void onAutoConnectionStarted() {
				// Do something when searching for new connection device
				
			}

			@Override
			public void onNewConnection(String name, String address) {
				// Do something when auto connection has stared
				
			}
			
		});

	    Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
	    bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
	        
		CheckBTState();
        //initList();
        IntentFilter filter = new IntentFilter(Constants.ACTION_SCAN);
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, filter);
    }

	// test the ZWAVE RSSI
	public void testZWAVE_RSSI(int nodeID){
		String msg = String.format(Locale.getDefault(), "{\"CMD\":\"scan\",\"ID\":%d}", nodeID);
		mBluetooth.send(msg, true);
		//if(app.setSCAN(nodeID, true, groupID)) {
        //    mAdapter.notifyDataSetChanged();
        //}
	}
	
	// pair the ZWAVE device
	public void pairZWAVE(){
		mBluetooth.send("{\"CMD\":\"pair\"}", true);
	}
	
	// parse the result data from ZWAVE
	public void parseResult(String result){
		try {
			JSONObject jObject = new JSONObject(result);
			int id = jObject.getInt("NodeID");
			int rssi = jObject.getInt("rssi");
            mAdapter.setNodeScan(id, false, rssi);
			//app.setSCAN(id, false);			// Off the scan mark
			//if(app.setRSSI(id, rssi)){
			//	mAdapter.notifyDataSetChanged();	// refresh
			//}
		} catch (JSONException e) {
			e.printStackTrace();
		}		
	}
	
	private void CheckBTState(){
		if(!mBluetooth.isBluetoothAvailable()){
			showText("BluetoothNotAvailable");
		}else{
			showText("BluetoothAvailable");
		}
	}
	
	// show text to buffer
	private void showText(String msg){
        Log.d(TAG, "" + msg);
	}
	
	private void AlertBox(String title, String message){
		new AlertDialog.Builder(this)
		.setTitle(title)
		.setMessage(message+" Press OK to exit.")
		.setPositiveButton("OK",new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				finish();				
			}			
		}).show();		
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
				findBT();
				break;
			case R.id.action_pair:	// Pair Z-Wave
				pairZWAVE();
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
		if(!mBluetooth.isBluetoothEnabled()){
			showText("...BluetoothDisable()");
			mBluetooth.startService(BluetoothState.DEVICE_OTHER);
		}else{
			showText("...BluetoothEnable()");
		}
	}
	
	// find my BT device
	public void findBT(){
//		Intent intent=new Intent(getApplicationContext(),DeviceList.class);
		Intent intent = new Intent(MainActivity.this, DeviceScanActivity.class);
		startActivityForResult(intent, BluetoothState.REQUEST_CONNECT_DEVICE);
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
        registerReceiver(mGattUpdateReceiver, intentFilter);
        if (mBluetoothLeService != null) {
        	showText("mBluetoothLeService.connect to "+mDeviceAddress);
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            showText("Connect request result=" + result);
        }
	}

	@Override
	protected void onPause() {
		super.onPause();
	    unregisterReceiver(mGattUpdateReceiver);
	}

	@Override
	protected void onStop() {
		super.onStop();
        try {
            mBluetooth.stopService();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		if(mBluetooth != null) {
			mBluetooth.stopService();
		}
		unbindService(mServiceConnection);
        mBluetoothLeService = null;
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
//		super.onActivityResult(requestCode, resultCode, data);
		if(requestCode == BluetoothState.REQUEST_CONNECT_DEVICE){
			showText("..REQUEST_CONNECT_DEVICE");
			if(resultCode == Activity.RESULT_OK){
				if(mBluetooth != null) {
                    mBluetooth.connect(data);
                }
				showText("..Back from BLE scanning");
				
				mDeviceName = data.getExtras().getString(DeviceControlActivity.EXTRAS_DEVICE_NAME);
				mDeviceAddress = data.getExtras().getString(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS);
				showText(mDeviceName);
		        showText(mDeviceAddress);
		        connect_2_ble_device();
			}
		} else if(requestCode == BluetoothState.REQUEST_ENABLE_BT) {
			showText("..BluetoothState.REQUEST_ENABLE_BT");
			if(resultCode==Activity.RESULT_OK) {
    			mBluetooth.setupService();
				mBluetooth.startService(BluetoothState.DEVICE_OTHER);
				//setup();
			}
		}
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
                            mAdapter.updateData(nodes);
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

	// connect to the BLE device
	public void connect_2_ble_device(){
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
	}
	
	// set up the BLE channel for two-way communication
	public void setup_ble_channel(){
		mBluetoothLeService.setCharacteristicNotification(rx_channel, true);	// enable read callback
		ble_send("Hello world!");
	}
	
	// send data to ZWAVE RSSI analyzer
	public void ble_send(String data){
		if(mConnected){
			tx_channel.setValue(data);
			mBluetoothLeService.writeCharacteristic(tx_channel);
		}
	}

    public static class ScrollAwareFABBehavior extends FloatingActionButton.Behavior {

        public ScrollAwareFABBehavior() {
            super();
        }

        public ScrollAwareFABBehavior(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        @Override
        public boolean onStartNestedScroll(CoordinatorLayout coordinatorLayout,
                                           FloatingActionButton child,
                                           View directTargetChild,
                                           View target, int nestedScrollAxes)
        {
            return nestedScrollAxes == ViewCompat.SCROLL_AXIS_VERTICAL ||
                    super.onStartNestedScroll(coordinatorLayout, child, directTargetChild, target,
                            nestedScrollAxes);
        }

        @Override
        public void onNestedScroll(CoordinatorLayout coordinatorLayout, FloatingActionButton child,
                                   View target, int dxConsumed, int dyConsumed,
                                   int dxUnconsumed, int dyUnconsumed)
        {
            super.onNestedScroll(coordinatorLayout, child, target,
                    dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed);
            if (dyConsumed > 0 && child.getVisibility() == View.VISIBLE) {
                child.hide();
            } else if (dyConsumed < 0 && child.getVisibility() != View.VISIBLE) {
                child.show();
            }
        }
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action.equals(Constants.ACTION_SCAN)) {
                int nodeID = intent.getIntExtra("NODE_ID", 0);
                testZWAVE_RSSI(nodeID);
            }
        }
    };
}

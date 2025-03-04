package com.liquidcontrols.lciqsdkdemo;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.text.Layout;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.liquidcontrols.lcr.iq.sdk.BlueToothConnectionOptions;
import com.liquidcontrols.lcr.iq.sdk.ConnectionOptions;
import com.liquidcontrols.lcr.iq.sdk.DeviceInfo;
import com.liquidcontrols.lcr.iq.sdk.FieldItem;
import com.liquidcontrols.lcr.iq.sdk.LCRCommunicationException;
import com.liquidcontrols.lcr.iq.sdk.LcrSdk;
import com.liquidcontrols.lcr.iq.sdk.RequestField;
import com.liquidcontrols.lcr.iq.sdk.ResponseField;
import com.liquidcontrols.lcr.iq.sdk.SDKDeviceException;
import com.liquidcontrols.lcr.iq.sdk.WiFiConnectionOptions;
import com.liquidcontrols.lcr.iq.sdk.interfaces.CommandListener;
import com.liquidcontrols.lcr.iq.sdk.interfaces.DeviceCommunicationListener;
import com.liquidcontrols.lcr.iq.sdk.interfaces.DeviceConnectionListener;
import com.liquidcontrols.lcr.iq.sdk.interfaces.DeviceListener;
import com.liquidcontrols.lcr.iq.sdk.interfaces.DeviceStatusListener;
import com.liquidcontrols.lcr.iq.sdk.interfaces.FieldListener;
import com.liquidcontrols.lcr.iq.sdk.interfaces.NetworkConnectionListener;
import com.liquidcontrols.lcr.iq.sdk.interfaces.PrinterStatusListener;
import com.liquidcontrols.lcr.iq.sdk.interfaces.SwitchStateListener;
import com.liquidcontrols.lcr.iq.sdk.lc.api.constants.FIELDS.FIELD_REQUEST_STATES;
import com.liquidcontrols.lcr.iq.sdk.lc.api.constants.FIELDS.UNITS;
import com.liquidcontrols.lcr.iq.sdk.lc.api.constants.LCR.COMMAND_STATE;
import com.liquidcontrols.lcr.iq.sdk.lc.api.constants.LCR.FIELD_WRITE_STATE;
import com.liquidcontrols.lcr.iq.sdk.lc.api.constants.LCR.LCR_COMMAND;
import com.liquidcontrols.lcr.iq.sdk.lc.api.constants.LCR.LCR_COMMUNICATION_STATUS;
import com.liquidcontrols.lcr.iq.sdk.lc.api.constants.LCR.LCR_DELIVERY_CODE;
import com.liquidcontrols.lcr.iq.sdk.lc.api.constants.LCR.LCR_DELIVERY_STATUS;
import com.liquidcontrols.lcr.iq.sdk.lc.api.constants.LCR.LCR_DEVICE_CONNECTION_STATE;
import com.liquidcontrols.lcr.iq.sdk.lc.api.constants.LCR.LCR_DEVICE_STATE;
import com.liquidcontrols.lcr.iq.sdk.lc.api.constants.LCR.LCR_MESSAGE_STATE;
import com.liquidcontrols.lcr.iq.sdk.lc.api.constants.LCR.LCR_PRINTER_STATUS;
import com.liquidcontrols.lcr.iq.sdk.lc.api.constants.LCR.LCR_SECURITY_LEVEL;
import com.liquidcontrols.lcr.iq.sdk.lc.api.constants.LCR.LCR_SWITCH_STATE;
import com.liquidcontrols.lcr.iq.sdk.lc.api.constants.LCR.LCR_THREAD_CONNECTION_STATE;
import com.liquidcontrols.lcr.iq.sdk.lc.api.constants.LCR.PRINTING_STATE;
import com.liquidcontrols.lcr.iq.sdk.lc.api.device.InternalEvent;
import com.liquidcontrols.lcr.iq.sdk.lc.api.network.NETWORK_TYPE;
import com.liquidcontrols.lcr.iq.sdk.utils.AsyncCallback;
import com.liquidcontrols.lcr.iq.sdk.utils.TimeSet;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static android.provider.AlarmClock.EXTRA_MESSAGE;

public class MainActivity extends AppCompatActivity {

	/** Enum to set application title and connect/disconnect button status */
	private enum DEVICE_ACTION {
		CONNECT,
		CONNECTING,
		DISCONNECT,
		DISCONNECTING
	}

	/** Enum to set on / off (start / stop) button states */
	private enum BUTTON_ACTIONS {
		ACTIVE,
		PASSIVE
	}

	private enum CONNECTION_TYPE {
		BLUETOOTH,
		WIFI
	}

	/** Control chars for start print work */
	private final byte[] PRINT_START_CODES = new byte[] {0x1b, 0x40};
	/** Control chars for end print work */
	private final byte[] PRINT_END_CODES= new byte[] {0x0a, 0x1d, 0x56};
	/** Sample string to print */
	private final String PRINT_TEXT = "Hello World!";

	/** Set FieldItem name for write field in LCR device (show dialog to for value) */
	private final String WRITE_FIELD_NAME = "GROSSPRESET";

	/** SDK Library object - Init SDK Library object with application context */
	private LcrSdk lcrSdk = new LcrSdk(this);

	/** Dialog variables for send data to device */
	private AlertDialog.Builder sendDataToDeviceDialogBuilder;
	private AlertDialog sendDataToDeviceDialog;

	/** List for available fields */
	private final List<FieldItem> availableLCRFields = new ArrayList<>();

	/** User interface objects - Buttons*/
	private Button buttonChangeConnectionState;
	private Button buttonChangeDataRequestState;
	private Button buttonChangeCommandState;
	private Button buttonChangePreset;
	private Button buttonRequestExtraData;


	/** User interface objects - Text view objects where data is shown */
	private TextView textViewGrossPresetData;
	private TextView textViewGrossQtyData;
	private TextView textViewFlowRate;

	/** User interface objects - Text view object where status information is shown */
	private TextView textViewSwitchStateData;
	private TextView textViewFlowStateData;
	private TextView textViewDeviceStateData;
	private TextView textViewDeviceConnectionStateData;
	private TextView textViewNetworkTypeData;
	private TextView textViewNetworkConnectionStateData;
	private TextView textViewNetworkAdddressData;

	/** User interface object - Logger text view */
	private TextView textViewLogger;

	/** List of strings for textViewDataLogger (shown in app screen) */
	private List<String> textViewLoggerDataList = new ArrayList<>();

	/** Bluetooth Auto-Connect **/
	private ArrayList<BluetoothDevice> mDeviceList = new ArrayList<BluetoothDevice>();
	private BluetoothAdapter mBluetoothAdapter;
	private BroadcastReceiver mBroadcastReceiver;
	private IntentFilter mBroadcastIntentFilter;
	private final static int REQUEST_ENABLE_BT = 111;

	/** Connect Activity */
	private final static int REQUEST_CONNECT = 1;


	/**
	 * Unique name for device (any name will do, as long its unique).
	 * Sdk use this name to operating with device. Each device must have different name
	 */
	private final String deviceId = "LCR.iQ";

	/** LCR device LCP protocol address */
	private Integer lcpLCRAddress = 250;

	/** SDK LCP protocol address */
	private final Integer lcpSDKAddress = 20;

	/** Setup for WiFi connection */
	///private final String wifiIpAddress = "192.168.1.1"; // OnTheGo
	private String wifiIpAddress = "192.168.1.30"; // WiFi Direct
	private Integer wifiPort = 10001;

	/** Setup for Bluetooth connection */
	private String bluetoothPairedName = "IOGEAR GBC232A Serial Adapter";

	/** Connection type to use */
	private CONNECTION_TYPE connectionToUse = CONNECTION_TYPE.WIFI;
	///private CONNECTION_TYPE connectionToUse = CONNECTION_TYPE.BLUETOOTH;

	/** Connection initialized */
	private boolean bConnectionInitialized = false;

	/** LCR User fields to get data */
	private FieldItem grossQty = null;
	private FieldItem grossPreset = null;
	private FieldItem flowRate = null;

	/** Application title string */
	private String appTitleText;

	/** SDK logcat print setup for developer purposes */
	private final Boolean logErrors = false; // Full error report will show in listener
	private final Boolean logWarnings = true;
	private final Boolean logInfo = true;
	private final Boolean logDebug = true;

	private LCR_COMMAND deliveryStatus = LCR_COMMAND.END_DELIVERY;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Check permissions
		if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH)
				!= PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(this,
					new String[]{Manifest.permission.BLUETOOTH},
					1);
		}
		if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN)
				!= PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(this,
					new String[]{Manifest.permission.BLUETOOTH_ADMIN},
					1);
		}
		if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
				!= PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(this,
					new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
					1);
		}

		// Init Dialog builder object (for enter gross preset value)
		sendDataToDeviceDialogBuilder = new AlertDialog.Builder(this);

		// Save app name for app title change by connection state
		appTitleText = getString(R.string.app_name);

		// Load user interface objects
		loadUserInterfaceObjects();

		textViewLogger.setMovementMethod(new ScrollingMovementMethod());

		// Disable user interface object
		buttonChangeConnectionState.setEnabled(true);
		setEnableStateOfConnectionRequestedButtons(false);
		setGrossPresetActionsEnabledState(false);

		// Bluetooth
		mBroadcastReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();
                {
                    String strInfo = "Action " + action;
                    setTextViewLogger("BT : " + strInfo);
                    Log.i("Main", strInfo);
                }
                // Found
                if (BluetoothDevice.ACTION_FOUND.equals(action)){
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    mDeviceList.add(device);
                    /** FOUND **/
                    String strInfo = "Bluetooth Device Found " + device.getName() +" " + device.getAddress();
                    setTextViewLogger("BT : " + strInfo);
                    Log.i("Main", strInfo);
                }

                // Found
                if (BluetoothDevice.ACTION_FOUND.equals(action)){
                    /** TBI **/
                }
				// Pair/Unpair
				if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)){
					final int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
					final int prevState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);
                    {
                        String strInfo = "From " + Integer.toString(prevState) + " To " + Integer.toString(state) ;
                        setTextViewLogger("BT : " + strInfo);
                        Log.i("Main", strInfo);
                    }
					if ( state == BluetoothDevice.BOND_BONDED && prevState == BluetoothDevice.BOND_BONDING ) {
						/** BONDED **/
						String strInfo = "Bluetooth Device Paired ";
						setTextViewLogger("BT : " + strInfo);
						Log.i("Main", strInfo);
						doConnectDevice();
					} else if ( state == BluetoothDevice.BOND_NONE && prevState == BluetoothDevice.BOND_BONDED ) {
						/** UNBONDED **/
						String strInfo = "Bluetooth Device Unpaired ";
						setTextViewLogger("BT : " + strInfo);
						Log.i("Main", strInfo);
					} else if ( state == BluetoothDevice.BOND_NONE && prevState == BluetoothDevice.BOND_BONDING ) {
                        /** FAILED BONDING **/
                        String strInfo = "Bluetooth Device Pairing Failed ";
                        setTextViewLogger("BT : " + strInfo);
                        Log.i("Main", strInfo);
                        doUIActionsForDeviceDisconnected();
                    }
				}


			}
		};

		// Register broadcast receiver
		mBroadcastIntentFilter = new IntentFilter();
		mBroadcastIntentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        mBroadcastIntentFilter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
        mBroadcastIntentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
		registerReceiver(mBroadcastReceiver, mBroadcastIntentFilter);

		// Initialize Bluetooth
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		} else {
			doStartScanning();
		}

		// Don't do it here
		if ( bConnectionInitialized ) {
			// Request SDK make init routines. Call AsyncCallback for result
			// -> SDK can not perform any tasks before init is success
			// -> Init must done async way
			lcrSdk.init(new AsyncCallback() {
				@Override
				public void onAsyncReturn(@Nullable Throwable error) {
					// Throwable only has data if error occurred
					if (error != null) {
						// Error at init
						String strError = "ERROR INIT SDK : " + error.getLocalizedMessage();
						setTextViewLogger("SDK : " + strError);
						Log.e("Main", strError);

					} else {
						setTextViewLogger("SDK : SDK Init success");
						// Add listeners to receive data from SDK
						addSDKListeners();
						// Add device to communicate with
						doAddDevice();
					}
				}
			});
		}
	}

	public void doStartScanning() {
		if (!mBluetoothAdapter.isDiscovering()) {
			if (!mBluetoothAdapter.startDiscovery()) {
				// Error starting scanning
				String strError = "Failed to start scanning : ";
				setTextViewLogger("BT : " + strError);
				Log.e("Main", strError);
			} else {
				/** Scanning **/
				String strInfo = "Bluetooth scanning started ";
				setTextViewLogger("BT : " + strInfo);
				Log.i("Main", strInfo);
			}
		} else {
			String strInfo = "Bluetooth already scanning";
			setTextViewLogger("BT : " + strInfo);
			Log.i("Main", strInfo);
		}
	}

	public void doStopScanning() {
		if (mBluetoothAdapter.isDiscovering()) {
			if (!mBluetoothAdapter.cancelDiscovery()) {
				// Error stopping scanning
				String strError = "Failed to stop scanning : ";
				setTextViewLogger("BT : " + strError);
				Log.e("Main", strError);
			} else {
				/** Scanning **/
				String strInfo = "Bluetooth scanning stopped ";
				setTextViewLogger("BT : " + strInfo);
				Log.i("Main", strInfo);
			}
		} else {
			String strInfo = "Bluetooth not scanning";
			setTextViewLogger("BT : " + strInfo);
			Log.i("Main", strInfo);
		}
	}

	/** Actions when app is going to destroy */
	@Override
	public void onDestroy() {
		unregisterReceiver(mBroadcastReceiver);
		super.onDestroy();
		if(lcrSdk != null) {
			// Remove device
			try {
				lcrSdk.removeDevice(getDeviceId());
			} catch (SDKDeviceException e) {
				e.printStackTrace();
			}
			// Remove used listeners
			lcrSdk.removeAllListeners();
			// Request SDK perform quit actions
			lcrSdk.quit();
		}
	}


	/** Loading user interface objects */
	private void loadUserInterfaceObjects() {
		// Buttons
		buttonChangeConnectionState = findViewById(R.id.button_change_connection_state);
		buttonChangeDataRequestState = findViewById(R.id.button_change_data_request_state);
		buttonChangeCommandState = findViewById(R.id.button_change_command_state);
		buttonRequestExtraData = findViewById(R.id.button_request_extra_data);
		buttonChangePreset = findViewById(R.id.button_preset);

		// Data text views
		textViewGrossPresetData = findViewById(R.id.textView_data_preset);
		textViewGrossQtyData = findViewById(R.id.textView_data_qty);
		textViewFlowRate = findViewById(R.id.textView_data_flow);

		// Status / state -text views
		textViewSwitchStateData = findViewById(R.id.textView_data_switch_state);
		textViewFlowStateData = findViewById(R.id.textView_data_flow_state);
		textViewDeviceStateData = findViewById(R.id.textView_data_device_state);
		textViewDeviceConnectionStateData = findViewById(R.id.textView_data_device_connection_state);
		textViewNetworkTypeData = findViewById(R.id.textView_data_network_type);
		textViewNetworkConnectionStateData = findViewById(R.id.textView_data_network_connection_State);
		textViewNetworkAdddressData = findViewById(R.id.textView_data_network_address);

		// Logger text view
		textViewLogger = findViewById(R.id.textView_data_logger);
	}

	/**
	 * Set state for buttons what required device connection to active
	 * @param enabled	<code>true</code> buttons are enabled
	 */
	private void setEnableStateOfConnectionRequestedButtons(Boolean enabled) {
		buttonChangeDataRequestState.setEnabled(enabled);
		buttonChangeCommandState.setEnabled(enabled);
		buttonRequestExtraData.setEnabled(enabled);
	}

	/**
	 * Set application TITLE and connection change button status
	 * @param action enum value for set text (in current action)
	 */
	private void setAppTitle(DEVICE_ACTION action) {
		switch (action) {
			case CONNECT:
				setTitle(appTitleText + " : Connected to " + getDeviceId());
				break;
			case DISCONNECT:
				setTitle(appTitleText + " : Disconnected from " + getDeviceId());
				break;
			case CONNECTING:
				setTitle(appTitleText + " : Connecting to " + getDeviceId());
				break;
			case DISCONNECTING:
				setTitle(appTitleText + " : Disconnecting from " + getDeviceId());
				break;
		}
	}

	@Override
	protected void onActivityResult (int requestCode,
						   int resultCode,
						   Intent data)
	{
		String mode;
		String value;
		String value2;
		String lcpAddress;
		Boolean good=false;


		if ( requestCode == REQUEST_ENABLE_BT ) {
			if (resultCode == Activity.RESULT_OK) {
				// Bluetooth Enabled
				String strInfo = "Bluetooh enabled";
				setTextViewLogger("BT : " + strInfo);
				Log.e("Main", strInfo);
				doStartScanning();
			}
		}

		if ( requestCode == REQUEST_CONNECT ) {

			if (resultCode == Activity.RESULT_CANCELED) {
				// Do not connect!
				buttonChangeConnectionState.setEnabled(true);
				return;
			} else if ( resultCode == Activity.RESULT_OK ) {
				// Do connect!
				Bundle bundle = data.getExtras();
				mode = bundle.getString("connection");
				value = bundle.getString("value");
				value2 = bundle.getString("value2");
				lcpAddress = bundle.getString("lcpAddress");

				if (mode.equalsIgnoreCase("wifi")) {
					// Setup for wifi connection
					connectionToUse = CONNECTION_TYPE.WIFI;
					wifiIpAddress = value;
					wifiPort = Integer.parseInt(value2);
					lcpLCRAddress = Integer.parseInt(lcpAddress);
					good = true;
				} else if (mode.equalsIgnoreCase("bluetooth")) {
					// Setup for Bluetooth connection
					connectionToUse = CONNECTION_TYPE.BLUETOOTH;
					bluetoothPairedName = value;
					lcpLCRAddress = Integer.parseInt(lcpAddress);
					good = true;
				} else {
					// Bad mode or data
					buttonChangeConnectionState.setEnabled(true);
				}

				// Connect if good
				if (good) {
					// Moved SDK initialization here and do it only once
					if (!bConnectionInitialized) {
						// Request SDK make init routines. Call AsyncCallback for result
						// -> SDK can not perform any tasks before init is success
						// -> Init must done async way
						lcrSdk.init(new AsyncCallback() {
							@Override
							public void onAsyncReturn(@Nullable Throwable error) {
								// Throwable only has data if error occurred
								if (error != null) {
									// Error at init
									String strError = "ERROR INIT SDK : " + error.getLocalizedMessage();
									setTextViewLogger("SDK : " + strError);
									Log.e("Main", strError);

								} else {
									bConnectionInitialized = true;
									setTextViewLogger("SDK : SDK Init success");
									// Add listeners to receive data from SDK
									addSDKListeners();
									// Add device to communicate with
									doAddDevice();
								}
							}
						});
					}
					// This approach does not work
					// Remove current one
					//				doRemoveDevice();
					// Add new one
					//				doAddDevice();

					// Set button text to "Connecting"
					//buttonChangeConnectionState.setText(R.string.button_switch_connection_state_on_connecting);
					//setAppTitle(DEVICE_ACTION.CONNECTING);
					//doConnectDevice();
				}
			}
		}
	}

	/** Button - Change connection stabutton_change_connection_statete - Click actions */
	public void onClick_Button_Change_Connection_State(View view) {
		// Disable button for time of change connection state (button enabled in listeners section)
		buttonChangeConnectionState.setEnabled(false);
		if ( !bConnectionInitialized ) {
			// Select Device to connect to
			Intent intent = new Intent(this, ConnectActivity.class);
			///EditText editText = (EditText) findViewById(R.id.editText);
			//String message = editText.getText().toString();
			intent.putExtra(EXTRA_MESSAGE, "message");
			this.startActivityForResult(intent, REQUEST_CONNECT);
		} else {
			// Get button action
			if (buttonChangeConnectionState.getTag().equals(DEVICE_ACTION.CONNECT)) {
				// Set button text to "Connecting"
				buttonChangeConnectionState.setText(R.string.button_switch_connection_state_on_connecting);
				setAppTitle(DEVICE_ACTION.CONNECTING);
				doConnectDevice();
			} else if (buttonChangeConnectionState.getTag().equals(DEVICE_ACTION.DISCONNECT)) {
				// Disable buttons who needs connection
				setEnableStateOfConnectionRequestedButtons(false);
				// Set button text to "Disconnecting"
				buttonChangeConnectionState.setText(R.string.button_switch_connection_state_on_disconnecting);
				setAppTitle(DEVICE_ACTION.DISCONNECTING);
				doDisconnectDevice();
			}
		}
	}

	/** Button - Set preset value */
	public void onClick_Button_Preset(View view) {
		showSendDataToDeviceDialog();
	}

	/**
	 * Button - Change field data request state
	 * <p>
	 * NOTE!<br>
	 * - User can only request data from fields what arrived in<br>
	 * {@link FieldListener#onFieldInfoChanged(String, DeviceInfo, List) onFieldInfoChanged} listener
	 * </p>
	 * <p>NOTE!<br>
	 * - AsyncCallback for add/remove field request, return status only to communicating with SDK.<br>
	 *   SDK will process request for each device and give return values in listener :<br>
	 *   > success in {@link FieldListener#onFieldDataRequestAddSuccess(String, DeviceInfo, RequestField, Boolean) success listener}<br>
	 *   > failure in {@link FieldListener#onFieldDataRequestAddFailed(String, DeviceInfo, RequestField, Throwable) failed listener}
	 * </p>
	 */
	public void onClick_Button_Change_Data_Request_State(View view) {

		if(lcrSdk == null || buttonChangeDataRequestState == null) {
			return;
		}

		if(buttonChangeDataRequestState.getTag() == null
				|| buttonChangeDataRequestState.getTag().equals(BUTTON_ACTIONS.ACTIVE)) {

			// Get field items by name (using helper)
			grossQty = findUserFieldByName("GROSSQTY");
			grossPreset = findUserFieldByName("GROSSPRESET");
			flowRate = findUserFieldByName("FLOWRATE");

			// If gross preset field is available
			if (grossPreset != null) {
				setTextViewLogger("SDK : Send command for request field " + grossPreset.getFieldName() + " with 5 second update interval");
				// Request field from device, 5 sec interval
				lcrSdk.requestFieldData(
						getDeviceId(),
						new RequestField(
								grossPreset,
								new TimeSet(5, TimeUnit.SECONDS)),
						new AsyncCallback() {
							@Override
							public void onAsyncReturn(@Nullable Throwable throwable) {
								if(throwable != null) {
									setTextViewLogger("SDK : Request command for field " + grossPreset.getFieldName() + " failed : " + throwable.getLocalizedMessage());
								} else {
									setTextViewLogger("SDK : Request command for field " + grossPreset.getFieldName() + " success");
								}
							}
						});
			}

			// If gross quantity field is available
			if (grossQty != null) {
				setTextViewLogger("SDK : Send command for request field " + grossQty.getFieldName() + " with 1 second update interval");
				// Request field from device, 1 sec interval
				lcrSdk.requestFieldData(
						getDeviceId(),
						new RequestField(
								grossQty,
								new TimeSet(1, TimeUnit.SECONDS)),
						new AsyncCallback() {
							@Override
							public void onAsyncReturn(@Nullable Throwable throwable) {
								if(throwable != null) {
									setTextViewLogger("SDK : Request command for field " + grossQty.getFieldName() + " failed : " + throwable.getLocalizedMessage());
								} else {
									setTextViewLogger("SDK : Request command for field " + grossQty.getFieldName() + " success");
								}
							}
						});
			}

			// If flowRate field is available
			if (flowRate != null) {
				setTextViewLogger("SDK : Send command for request field " + flowRate.getFieldName() + " with 2 second update interval");
				// Request field from device, 2 sec interval
				lcrSdk.requestFieldData(
						getDeviceId(),
						new RequestField(
								flowRate,
								new TimeSet(2, TimeUnit.SECONDS)),
						new AsyncCallback() {
							@Override
							public void onAsyncReturn(@Nullable Throwable throwable) {
								if(throwable != null) {
									setTextViewLogger("SDK : Request command for field " + flowRate.getFieldName() + " failed : " + throwable.getLocalizedMessage());
								} else {
									setTextViewLogger("SDK : Request command for field " + flowRate.getFieldName() + " success");
								}
							}
						});
			}
			buttonChangeDataRequestState.setText(R.string.button_data_request_stop_data_request);
			buttonChangeDataRequestState.setTag(BUTTON_ACTIONS.PASSIVE);
		} else {
			// Removing data requests
			// If gross preset field is available
			if (grossPreset != null) {
				setTextViewLogger("SDK : Send command for removing field data request " + grossPreset.getFieldName());
				lcrSdk.removeFieldDataRequest(
						getDeviceId(),
						grossPreset,
						new AsyncCallback() {
							@Override
							public void onAsyncReturn(@Nullable Throwable throwable) {
								if(throwable != null) {
									setTextViewLogger("SDK : Remove command for for field data request " + grossPreset.getFieldName() + " failed : " + throwable.getLocalizedMessage());
								} else {
									setTextViewLogger("SDK : Remove command for field data request " + grossPreset.getFieldName() + " success");
								}
							}
						});
			}

			// If gross quantity field is available
			if (grossQty != null) {
				setTextViewLogger("SDK : Send command for removing field data request " + grossQty.getFieldName());
				lcrSdk.removeFieldDataRequest(
						getDeviceId(),
						grossQty,
						new AsyncCallback() {
							@Override
							public void onAsyncReturn(@Nullable Throwable throwable) {
								if(throwable != null) {
									setTextViewLogger("SDK : Remove command for field data request " + grossQty.getFieldName() + " failed : " + throwable.getLocalizedMessage());
								} else {
									setTextViewLogger("SDK : Remove command for field data request " + grossQty.getFieldName() + " success");
								}
							}
						});
			}

			// If flowRate field is available
			if (flowRate != null) {
				setTextViewLogger("SDK : Send command for removing field data request " + flowRate.getFieldName());
				lcrSdk.removeFieldDataRequest(
						getDeviceId(),
						flowRate,
						new AsyncCallback() {
							@Override
							public void onAsyncReturn(@Nullable Throwable throwable) {
								if(throwable != null) {
									setTextViewLogger("SDK : Remove command for field data request " + flowRate.getFieldName() + " failed : " + throwable.getLocalizedMessage());
								} else {
									setTextViewLogger("SDK : Remove command for field data request " + flowRate.getFieldName() + " success");
								}
							}
						});
			}
			buttonChangeDataRequestState.setText(R.string.button_data_request_start_data_request);
			buttonChangeDataRequestState.setTag(BUTTON_ACTIONS.ACTIVE);
		}
	}

	/**
	 * Button - Print small sample text to LCR printer
	 * (no any special format)
	 */
	public void onClick_Button_Print(View view) {

		// Command variable
		LCR_COMMAND command;

		if (lcrSdk == null) {
			return;
		}

		// Check for state of delivery
		if (deliveryStatus == LCR_COMMAND.RUN || deliveryStatus == LCR_COMMAND.PAUSE)
		{
			// When running delivery send real print command
			command = LCR_COMMAND.END_DELIVERY;
			deliveryStatus = LCR_COMMAND.END_DELIVERY;
			// Put command
			lcrSdk.addDeviceCommand(
					getDeviceId(),
					command);
		}
		else
		{
			try {
				lcrSdk.addPrintWork(
						getDeviceId(),
						String.format(
								"%s%s%s",
								Arrays.toString(PRINT_START_CODES),
								PRINT_TEXT,
								Arrays.toString(PRINT_END_CODES)).getBytes());
			} catch (Exception e) {
				setTextViewLogger("Error add print work : " + e.getMessage());
			}
		}
	}

	/** Button - Change command state (start / pause) */
	public void onClick_Button_Change_Command_State(View view) {

		if(lcrSdk == null || buttonChangeCommandState == null) {
			return;
		}

		// Disable button to prevent multiple press
		buttonChangeCommandState.setEnabled(false);

		// Command variable
		LCR_COMMAND command;

		if(buttonChangeCommandState.getTag() == null
			|| buttonChangeCommandState.getTag().equals(BUTTON_ACTIONS.PASSIVE)) {
			command = LCR_COMMAND.RUN;
			deliveryStatus = LCR_COMMAND.RUN;
		} else {
			command = LCR_COMMAND.PAUSE;
			deliveryStatus = LCR_COMMAND.PAUSE;
		}

		// Put command
		lcrSdk.addDeviceCommand(
				getDeviceId(),
				command);
	}

	/** Button - Exit (Actions for quit app) */
	public void onClick_Button_Exit(View view) {
		this.finishAffinity();
	}

	/** Reset status text data text */
	private void resetStatusTexts() {
		textViewSwitchStateData.setText(R.string.text_state_unknown);
		textViewFlowStateData.setText(R.string.text_state_unknown);
		textViewDeviceStateData.setText(R.string.text_state_unknown);
	}

	/** Helper to look available field by name */
	@Nullable
	private FieldItem findUserFieldByName(@NonNull String name) {
		for(FieldItem item : availableLCRFields) {
			if(item.getFieldName().equals(name)) {
				return item;
			}
		}
		return null;
	}

	/** Add LCR Device object to SDK */
	private void doAddDevice() {
		if(lcrSdk == null) {
			return;
		}

		ConnectionOptions connectionOptions;

		// Check what type of connection to use
		if(connectionToUse.equals(CONNECTION_TYPE.WIFI)) {
			setTextViewLogger("SDK : Using Wifi connection");
			connectionOptions = getWifiConnectionOptions();
			textViewNetworkTypeData.setText(R.string.text_network_type_wifi);
		} else {
			setTextViewLogger("SDK : Using Bluetooth connection");
			connectionOptions = getBluetoothConnectionOptions();
			textViewNetworkTypeData.setText(R.string.text_network_type_bluetooth);
		}
		// Set default text for device and network connection state
		textViewDeviceConnectionStateData.setText(R.string.button_switch_connection_state_disconnect);

		// Synchronize way to add device (no callback need), using try-catch for error detection
		try {

			lcrSdk.addDevice(
					getDeviceInfo(),
					connectionOptions);

			/* !! NOTE !! Device add will be confirmed in DeviceListener */

		} catch (Exception e) {
			// Device add request fail
			String strError = "Device add request failed : " + e.getLocalizedMessage();
			Log.e("Main",strError);
			setTextViewLogger("SDK :" + strError);
		}
		doUIActionsForDeviceDisconnected();
	}

	/** Remove LCR Device object from SDK */
	private void doRemoveDevice() {
		if(lcrSdk != null) {
			// Remove device
			try {
				lcrSdk.removeDevice(getDeviceId());
			} catch (SDKDeviceException e) {
				e.printStackTrace();
			}
			// Remove used listeners
			lcrSdk.removeAllListeners();
		}
	}


	/**
	 * Generate device info for making connection to LCR device.
	 * @return	Device Info for connection
	 */
	private DeviceInfo getDeviceInfo() {
		// Making device info with device id
		DeviceInfo returnDeviceInfo = new DeviceInfo(getDeviceId());

		// Set Device LCP address
		returnDeviceInfo.setDeviceAddress(lcpLCRAddress);

		// Set SDK LCP address
		returnDeviceInfo.setSdkAddress(lcpSDKAddress);

		return returnDeviceInfo;
	}

	/**
	 * Define connection parameters for using WIFI network.
	 * NOTE!
	 * Wifi must be connected to Liquid Control On The GO WiFi adapter and device where this app is running
	 * IP Address and Port are defined inside On The Go Wifi adapter settings
	 *
	 * @return Connection options for WiFi connection
	 */
	private WiFiConnectionOptions getWifiConnectionOptions() {

		return new WiFiConnectionOptions(
				// IP Address
				wifiIpAddress,
				// Port
				wifiPort);
	}

	/**
	 * Define connection parameters for Bluetooth connection.
	 * NOTE!
	 * Bluetooth pairing must be done with Liquid Control bluetooth adapter and device where this app is running
	 * @return Connection options for Bluetooth connection
	 */
	private BlueToothConnectionOptions getBluetoothConnectionOptions() {
		return new BlueToothConnectionOptions(
				// Name of paired bluetooth device where LCR Register is connected
				bluetoothPairedName);
	}

	/** Request SDK to disconnect from device */
	private void doDisconnectDevice() {
		// Check SDK object (if call this method after close object)
		if(lcrSdk == null) {
			return;
		}
		// Call SDK to make disconnect command for device
		lcrSdk.disconnect(getDeviceId());
	}

	/** Request SDK to connect to device */
	private void doConnectDevice() {
		// Check SDK object (if call this method after close object)
		if(lcrSdk == null) {
			return;
		}
		// Check if Bluetooth device and if it is paired
		if ( connectionToUse == CONNECTION_TYPE.BLUETOOTH ){
			if ( ! checkIfPaired(bluetoothPairedName) ){
				// First pair the device
                pairDevice(bluetoothPairedName);
				return;
			}
		}
		// Call SDK to make connect
		lcrSdk.connect(getDeviceId());
	}

	/** Blueooth pair the device **/
	private void pairDevice(String deviceId) {
		if ( mDeviceList != null && mDeviceList.size() > 0 ) {
			for ( BluetoothDevice device : mDeviceList ) {
				String name = device.getName();
				if ( deviceId.equals(name) ) {
					try {
						doStopScanning();
						String strInfo = "Bluetooth Device " + name +  " Pairing Start";
						setTextViewLogger("BT : " + strInfo);
						Log.i("Main", strInfo);
						device.createBond();
					} catch ( Exception e ) {
						String strError = "Bluetooth Device " + name +  " Pairing  Failed with" + e.toString();
						Log.e("Main",strError);
						setTextViewLogger("BT :" + strError);
					}
				}
			}
		}
	}

	/** Blueooth chedk if decice is paired **/
	boolean checkIfPaired(String deviceId) {
		Set<BluetoothDevice> bondedDevices = mBluetoothAdapter.getBondedDevices();
		if ( bondedDevices != null && bondedDevices.size() > 0 ) {
			for ( BluetoothDevice device : bondedDevices ) {
                String name = device.getName();
				if (  deviceId.equals(name) ) {
					return true;
				}
			}
		}
		return false;
	}
	/**
	 * Get device id information
	 * @return	Device id as string
	 */
	@NonNull
	private String getDeviceId() {
		return this.deviceId;
	}

	/** Add all listeners to SDK for monitoring LCR device data */
	private void addSDKListeners() {
		if(lcrSdk == null) {
			return;
		}
		// Device connection listener
		lcrSdk.addListener(deviceConnectionListener);
		// Field listener
		lcrSdk.addListener(fieldListener);
		// Command listener
		lcrSdk.addListener(commandListener);
		// Device status / state
		lcrSdk.addListener(deviceStatusListener);
		// Switch state listener
		lcrSdk.addListener(switchStateListener);
		// Printer status listener
		lcrSdk.addListener(printerStatusListener);

		// ** New listeners **
		// Add device communication listener
		lcrSdk.addListener(deviceCommunicationListener);
		// Device add/remove listener
		lcrSdk.addListener(deviceListener);
		// Network status listener (for logging purposes)
		lcrSdk.addListener(networkConnectionListener);

		setTextViewLogger("SDK : Add listeners");
	}

	/**
	 * Refresh latest listener values (by add same listener again)
	 * Note! Adding same listener again get latest data what SDK service has stored.
	 */
	private void refreshStatusListeners() {
		// Device status / state
		lcrSdk.addListener(deviceStatusListener);
		// Switch state listener
		lcrSdk.addListener(switchStateListener);
		// Printer status
		lcrSdk.addListener(printerStatusListener);
	}

	/** Show data write setup dialog -> Publish data to LCR device */
	private void showSendDataToDeviceDialog() {

		sendDataToDeviceDialogBuilder.setTitle("Write data to device");
		sendDataToDeviceDialogBuilder.setMessage("Set field : " + WRITE_FIELD_NAME + " value for device :\n" + getDeviceId());

		final EditText input = new EditText(this);
		// Limit input type for numbers only (changed to have decimals also)
		input.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
		// Set default value (if it exist)
		if(textViewGrossPresetData.getTag() != null) {
			// Set default value (remove decimals)
			String value[] = textViewGrossPresetData.getTag().toString().split("\\.");
			input.setText(value[0]);
		}
		sendDataToDeviceDialogBuilder.setView(input);
		sendDataToDeviceDialogBuilder.setPositiveButton("Send to Device", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialogInterface, int i) {
				sendFieldItemValueToDevice(input.getText().toString());
			}
		});
		sendDataToDeviceDialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialogInterface, int i) {
				//preDialog.cancel();
			}
		});
		sendDataToDeviceDialog = sendDataToDeviceDialogBuilder.create();
		sendDataToDeviceDialog.show();

	}

	/** Send field data value to LCR device */
	private void sendFieldItemValueToDevice(@Nullable String value) {
		if(value == null || lcrSdk == null) {
			return;
		}
		setTextViewLogger("Data to send LCR : " + value);
		// Final check for field before write data
		FieldItem fieldItemToSend = findUserFieldByName(WRITE_FIELD_NAME);
		if(fieldItemToSend != null && fieldItemToSend.getWritePermission()) {
			lcrSdk.publishFieldData(
					getDeviceId(),
					fieldItemToSend,
					value);
		}
	}

	/** Enabled / Disabled GROSSPRESET user interface objects */
	private void setGrossPresetActionsEnabledState(@NonNull Boolean enabled) {

		// Set button state in main view
		buttonChangePreset.setEnabled(enabled);

		// Set GROSSPRESET change popup button states
		if(sendDataToDeviceDialog != null && sendDataToDeviceDialog.isShowing()) {
			sendDataToDeviceDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(enabled);
		}
	}

	/**
	 * Activated when Field Data list is arrived from Device.
	 * This check write permission to send data in GROSSPRESET field
	 */
	private void checkAndSetPresetUpdateState() {

		// Search GROSS_PRESET field for temporary variable
		FieldItem tmpGrossPreset = findUserFieldByName(WRITE_FIELD_NAME);
		if(tmpGrossPreset == null) {
			// GROSS_PRESET field did not found, disable gross preset UI components
			setGrossPresetActionsEnabledState(false);
		}
	}

	/**
	 * Update data in textView logger (in screen)
	 * @param text	Log text to print logger screen
	 */
	private void scrollTextViewLogger()
	{
		if(textViewLogger != null){
			final Layout layout = textViewLogger.getLayout();
			if(layout != null){
				int scrollDelta = layout.getLineBottom(textViewLogger.getLineCount() - 1)
						- textViewLogger.getScrollY() - textViewLogger.getHeight();
				if(scrollDelta > 0)
					textViewLogger.scrollBy(0, scrollDelta);
			}
		}
	}

	private void setTextViewLogger(String text) {
		final Integer MAX_LINES = 100;

		if(text == null || textViewLogger == null) {
			return;
		}

		if(textViewLoggerDataList.size() > MAX_LINES) {
			textViewLoggerDataList.remove(0);
		}
		textViewLoggerDataList.add(text);
		String textBuffer = "";
		// Make data to print
		for(String str : textViewLoggerDataList) {
			textBuffer = textBuffer + str + "\n";
		}
		// Print data
		textViewLogger.setText(textBuffer);
		// Scroll all the way down
		scrollTextViewLogger();
	}

	/** Set user interface objects after device add is done successfully */
	private void doUIActionsForDeviceAddSuccess() {

		// Activate change connection state button (for enabled make connection request)
		buttonChangeConnectionState.setEnabled(true);
		// Set button next action
		buttonChangeConnectionState.setTag(DEVICE_ACTION.CONNECT);
		setTextViewLogger("SDK : Add device success");

		//
		// Set SDK log messages printing to Logcat (for develop purposes). Eating some extra CPU when on
		//

		// Log errors in logcat (Note! deviceCommunicationListener show full error report)
		lcrSdk.setErrorLevelDebugEnabled(getDeviceId(),logErrors);

		// Log warning level events in logcat
		lcrSdk.setWarningLevelDebugEnabled(getDeviceId(), logWarnings);

		// Show Info level events in logcat
		lcrSdk.setInfoLevelDebugEnabled(getDeviceId(),logInfo);

		// Log debug level events in logcat (lots of data...)
		lcrSdk.setDebugLevelDebugEnabled(getDeviceId(),logDebug);
	}

	/** Set User Interface objects in disconnected mode */
	private void doUIActionsForDeviceDisconnected() {
		// Set title
		setAppTitle(DEVICE_ACTION.DISCONNECT);
		// Set button text for connect (same button used for connect also)
		buttonChangeConnectionState.setText(R.string.button_switch_connection_state_connect);
		// Enabled connect/disconnect button
		buttonChangeConnectionState.setEnabled(true);
		// Disable buttons who needs connection
		setEnableStateOfConnectionRequestedButtons(false);
		// Save next command state to button
		buttonChangeConnectionState.setTag(DEVICE_ACTION.CONNECT);
		// Reset status text
		resetStatusTexts();
		// Set gross preset UI actions state
		setGrossPresetActionsEnabledState(false);

	}

	/** Set User Interface objects in Connected mode */
	private void doUIActionsForDeviceConnected() {
		// Set title
		setAppTitle(DEVICE_ACTION.CONNECT);
		// Set button text for disconnect (same button used for connect also)
		buttonChangeConnectionState.setText(R.string.button_switch_connection_state_disconnect);
		// Enabled connect/disconnect button
		buttonChangeConnectionState.setEnabled(true);
		// Enabled buttons who needs connection
		setEnableStateOfConnectionRequestedButtons(true);
		// Save next command state to button
		buttonChangeConnectionState.setTag(DEVICE_ACTION.DISCONNECT);
		//setGrossPresetActionsEnabledState(true);
		// Re-set listeners to refresh status data from SDK service
		refreshStatusListeners();

	}

	/** Set User Interface objects in device connection error mode */
	private void doUIActionsForDeviceError() {
		// Set title
		setAppTitle(DEVICE_ACTION.DISCONNECT);
		// Set button text for connect (same button used for connect also)
		buttonChangeConnectionState.setText(R.string.button_switch_connection_state_connect);
		// Enabled connect/disconnect button
		buttonChangeConnectionState.setEnabled(true);
		// Disable buttons who needs connection
		setEnableStateOfConnectionRequestedButtons(false);
		// Save next command state to button
		buttonChangeConnectionState.setTag(DEVICE_ACTION.CONNECT);
		// Reset status text
		resetStatusTexts();
		// Set gross preset UI actions state
		setGrossPresetActionsEnabledState(false);

	}


	/** @see #objToStrWithNullCheck(Object, String)  */
	private String objToStrWithNullCheck(@Nullable Object valueToCheck) {
		return this.objToStrWithNullCheck(valueToCheck, "(null)");
	}

	/**
	 * Small tool to check null values from objects.
	 * @param valueToCheck	Object to check to check
	 * @param valueIfNull	String to return if valueToCheck parameter is <code>null</code>
	 * @return Checked value output as .toString() method call
	 */
	@NonNull
	private String objToStrWithNullCheck(@Nullable Object valueToCheck, @NonNull String valueIfNull) {
		if(valueToCheck != null && valueToCheck.toString() != null) {
			return valueToCheck.toString();
		}
		return valueIfNull;
	}

	//
	// Listener section defines data what will be monitored from LCR device
	//

	/** Device add / remove listener */
	public DeviceListener deviceListener = new DeviceListener() {

		/**
		 * Called when device add operation success
		 * @param deviceId	Device identification
		 */
		@Override
		public void onDeviceAddSuccess(@NonNull String deviceId) {

			// Logging success
			setTextViewLogger("Add device success : " + deviceId);

			// Set user interface objects for device add success
			doUIActionsForDeviceAddSuccess();
		}

		/**
		 * Called when device add operation failed
		 * @param deviceId	Device identification
		 * @param cause		Cause of error
		 */
		@Override
		public void onDeviceAddFailed(@NonNull String deviceId, SDKDeviceException cause) {
			String strCause = "(null)";
			if(cause != null) {
				strCause = cause.getMessage();
			}
			// Logging add device error
			setTextViewLogger("Add device failed : " + strCause);

		}

		/**
		 * Called when device remove operation is success
		 * @param deviceId	Device identification
		 */
		@Override
		public void onDeviceRemoveSuccess(@NonNull String deviceId) {
			// Logging actions
			setTextViewLogger("Remove device success");

		}

		/**
		 * Called when device remove operation is failed
		 * @param deviceId	Device identification
		 * @param cause		Cause of error
		 */
		@Override
		public void onDeviceRemoveFailed(@NonNull String deviceId, SDKDeviceException cause) {
			String strCause = "(null)";
			if(cause != null) {
				strCause = cause.getMessage();
			}
			// Logging remove device error
			setTextViewLogger("Remove device failed : " + strCause);

		}
	};

	/** Device connection listener */
	public DeviceConnectionListener deviceConnectionListener = new DeviceConnectionListener() {
		/**
		 * Called when connection to LCR device is made with all relevant information.
		 * @param deviceId 		Device identification string
		 * @param deviceInfo	Device information
		 */
		@Override
		public void deviceOnConnect(
				@NonNull String deviceId,
				@NonNull DeviceInfo deviceInfo) {

			// Set user interface for connected state
			doUIActionsForDeviceConnected();

			String logText = "Device on CONNECTED : "
					+ deviceId
					+ " LCP SDK Address : "
					+ deviceInfo.getSdkAddress().toString()
					+ " LCP Device Address : "
					+ deviceInfo.getDeviceAddress().toString();

			setTextViewLogger(logText);
			Log.d("DEVICE", logText);
		}
		/**
		 * Called when device lost connection
		 * @param deviceId		Device identification string
		 * @param deviceInfo	Device information
		 * @param cause			Reason for connection lost
		 */
		@Override
		public void deviceOnDisconnect(
				@NonNull String deviceId,
				@NonNull DeviceInfo deviceInfo,
				@Nullable Throwable cause) {

			// Set user interface for disconnected state
			doUIActionsForDeviceDisconnected();

			// Log text about disconnecting
			String causeString = "unknown";
			if(cause != null) {
				causeString = objToStrWithNullCheck(cause.getLocalizedMessage());
			}
			setTextViewLogger("Device on DISCONNECTED : " + deviceId + " Cause : " + causeString);
		}

		/**
		 * Called when device connection enter in error state
		 * @param deviceId		Device identification string
		 * @param deviceInfo	Device information
		 * @param cause			Cause of error
		 */
		@Override
		public void deviceOnError(
				@NonNull String deviceId,
				@NonNull DeviceInfo deviceInfo,
				@Nullable Throwable cause) {

			// Set user interface for error connected state
			doUIActionsForDeviceError();

			String errorMsg = "";
			if(cause != null) {
				errorMsg = objToStrWithNullCheck(cause.getLocalizedMessage());
			}
			setTextViewLogger("Device on ERROR : " + deviceId + " Cause : " + errorMsg);
		}


		/**
		 * Notify any status change events
		 * @param deviceId		Device identification string
		 * @param deviceInfo	Device information
		 * @param newValue		New State
		 * @param oldValue		Old State
		 */
		@Override
		public void deviceConnectionStateChanged(
				@NonNull String deviceId,
				@NonNull DeviceInfo deviceInfo,
				@Nullable LCR_DEVICE_CONNECTION_STATE newValue,
				@Nullable LCR_DEVICE_CONNECTION_STATE oldValue) {

			textViewDeviceConnectionStateData.setText(objToStrWithNullCheck(newValue));

			setTextViewLogger("Device connection state changed : " + oldValue + " -> " + newValue);
		}

		/**
		 * Device network status changed
		 * @param deviceId			Device identification string
		 * @param deviceInfo		Device info
		 * @param connectionOptions	Connection info
		 * @param newValue			Network new State
		 * @param oldValue			Network old state
		 */
		@Override
		public void deviceNetworkStateChanged(
				@NonNull String deviceId,
				@NonNull DeviceInfo deviceInfo,
				@Nullable ConnectionOptions connectionOptions,
				@NonNull LCR_THREAD_CONNECTION_STATE newValue,
				@NonNull LCR_THREAD_CONNECTION_STATE oldValue) {
		    String networkAdressData;


			if(connectionOptions != null) {
				if(connectionOptions instanceof BlueToothConnectionOptions) {
					textViewNetworkTypeData.setText(R.string.text_network_type_bluetooth);
					textViewNetworkAdddressData.setText(bluetoothPairedName);
				} else if(connectionOptions instanceof WiFiConnectionOptions) {

                    networkAdressData = wifiIpAddress + String.format(":%d", wifiPort);
                    textViewNetworkTypeData.setText(R.string.text_network_type_wifi);
					textViewNetworkAdddressData.setText(networkAdressData);
				} else {
					textViewNetworkTypeData.setText(R.string.text_unknown);
					textViewNetworkAdddressData.setText(R.string.text_unknown);
				}
			} else {
				textViewNetworkTypeData.setText(R.string.text_unknown);
				textViewNetworkAdddressData.setText(R.string.text_unknown);
			}
			textViewNetworkConnectionStateData.setText(newValue.toString());

			setTextViewLogger("Network connection state changed : " + oldValue + " -> " + newValue);
		}
	};

	/** Listener to handle all Field operation events */
	public FieldListener fieldListener = new FieldListener() {

		/**
		 * onFieldInfoChanged event is activated when device field list is available or
		 * field list status is changed.
		 * Field Data read request can be done only for items what is in field list.
		 *
		 * @param deviceId		Device Id for using multiple devices same time
		 * @param deviceInfo	Device info
		 * @param fields		List of available fields
		 */
		@Override
		public void onFieldInfoChanged(
				@NonNull String deviceId,
				@NonNull DeviceInfo deviceInfo,
				@Nullable List<FieldItem> fields) {

			// Clear local list
			availableLCRFields.clear();
			if(fields != null) {
				String logText = "Field info arrived : " + fields.size() + " fields";
				setTextViewLogger(logText);
				// Add all list items to local list
				availableLCRFields.addAll(fields);
			}
			// Check GROSS_PRESET access and set UI Components
			checkAndSetPresetUpdateState();
		}

		/**
		 * onFieldReadDataChanged event is activated when new data arrived in requested field
		 * @param deviceId		Device Id for using multiple devices same time
		 * @param deviceInfo	Device info
		 * @param responseField	Reply/response field with data
		 * @param requestField 	Requested field info
		 */
		@Override
		public void onFieldReadDataChanged(
				@NonNull String deviceId,
				@NonNull DeviceInfo deviceInfo,
				@NonNull ResponseField responseField,
				@NonNull RequestField requestField) {

			// Temporary variables for unit information
			String strMeasureUnit = "unknown";
			String strRateBaseUnit = "unknown";

			// For not log items what is displayed in separated fields
			Boolean showInLog = true;

			// Get measure unit
			if(responseField.getUnits().get(UNITS.MEASURE_UNIT) != null) {
				strMeasureUnit = responseField.getUnits().get(UNITS.MEASURE_UNIT).toLowerCase(Locale.getDefault());
			}
			// Get rate base unit (used in flow rate)
			if(responseField.getUnits().get(UNITS.RATE_BASE) != null) {
				strRateBaseUnit = responseField.getUnits().get(UNITS.RATE_BASE).toLowerCase(Locale.getDefault());
			}

			// Get data to show in text views
			if(requestField.getItemToRequest().equals(grossPreset)) {
				// Set logger off for this field
				showInLog = false;
				// Save value as default for set GROSSPRESET value
				textViewGrossPresetData.setTag(responseField.getNewValue());

				// Format setText string
				textViewGrossPresetData.setText(
						String.format(
								Locale.getDefault(),
								"%s   %s",
								responseField.getNewValue(),
								strMeasureUnit));
			}
			if(requestField.getItemToRequest().equals(grossQty)) {
				// Set logger off for this field
				showInLog = false;
				// Format setText string
				textViewGrossQtyData.setText(
						String.format(
								Locale.getDefault(),
								"%s   %s",
								responseField.getNewValue(),
								strMeasureUnit));
			}
			if(requestField.getItemToRequest().equals(flowRate)) {
				// Set logger off for this field
				showInLog = false;
				// Format setText string
				textViewFlowRate.setText(
						String.format(
								Locale.getDefault(),
								"%s   %s/%s",
								responseField.getNewValue(),
								strMeasureUnit,
								strRateBaseUnit));
			}

			if(showInLog) {
				String logText = "Field data arrive : "
						+ responseField.getFieldItem().getFieldName()
						+ " - " + responseField.getOldValue()
						+ " -> "
						+ responseField.getNewValue();

				// Logging field data change event
				setTextViewLogger(logText);
			}
		}


		/**
		 * Called when field data request run is success (data has received from LCR device)
		 * (activated every time when data request has success, even received data values are same)
		 * @param deviceId			Device Id
		 * @param deviceInfo		Device info
		 * @param responseField		Reply field with data
		 * @param requestField		Requested field info
		 */
		@Override
		public void onFieldDataRequestSuccess(
				@NonNull String deviceId,
				@NonNull DeviceInfo deviceInfo,
				@NonNull ResponseField responseField,
				@NonNull RequestField requestField) {

			// Logging this event with timestamp
			String logString;
			SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss");
			String ts = sdf.format(new Date());


			logString = String.format(Locale.getDefault(),
					"Field data request success %s time : %s" ,requestField.getItemToRequest().getFieldName(), ts);

			// Write log text
			setTextViewLogger(logString);

			/*
			* NOTE!
			* This event return field data request values (ResponseField), even data has not change
			*/

		}

		/**
		 * Called when field item data request is failed (Request data from LCR device is failed)
		 * @param deviceId		Device Id
		 * @param deviceInfo	Device info
		 * @param requestField	Requested field info
		 * @param cause			Error cause / message
		 */
		@Override
		public void onFieldDataRequestFailed(
				@NonNull String deviceId,
				@NonNull DeviceInfo deviceInfo,
				@NonNull RequestField requestField,
				@NonNull Throwable cause) {

			String logString;

			logString = String.format(Locale.getDefault(),
					"Field data request failed %s\nCause : %s"
					,requestField.getItemToRequest().getFieldName()
					,cause.getLocalizedMessage());

			// Write log text
			setTextViewLogger(logString);
		}

		/**
		* Called when field item data request {@link FIELD_REQUEST_STATES state} has changed
		* @param deviceId		Device Id
		* @param deviceInfo		Device info
		* @param requestField	Requested field info
		* @param newValue		New value of {@link FIELD_REQUEST_STATES}
		* @param oldValue		Old value of {@link FIELD_REQUEST_STATES}
		*/
		@Override
		public void onFieldDataRequestStateChanged(
				@NonNull String deviceId,
				@NonNull DeviceInfo deviceInfo,
				@NonNull RequestField requestField,
				@NonNull FIELD_REQUEST_STATES newValue,
				@NonNull FIELD_REQUEST_STATES oldValue) {

			// Logging this event with timestamp
			String logString;
			String frs_old="";
			String frs_new="";
			SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss.SSS");
			String ts = sdf.format(new Date());

			switch (oldValue) {
				case NONE : frs_old = "NONE"; break;
				case ADD: frs_old = "ADD"; break;
				case ADD_SUCCESS: frs_old = "ADD_SUCCESS"; break;
				case ADD_FAILED: frs_old = "ADD_FAILED"; break;
				case QUEUED: frs_old = "QUEUED"; break;
				case PAUSED : frs_old = "PAUSED"; break;
				case CHECK_RELEVANT_FIELDS : frs_old = "CHECK_RELEVANT_FIELDS"; break;
				case RUNNING : frs_old = "RUNNING"; break;
				case RUN_SUCCESS : frs_old = "RUN_SUCCESS"; break;
				case RUN_FAILED : frs_old = "RUN_FAILED"; break;
				case REMOVED : frs_old = "REMOVED"; break;
				default : frs_old = "UNKNOWN";
			}

			switch (newValue) {
				case NONE : frs_new = "NONE"; break;
				case ADD: frs_new = "ADD"; break;
				case ADD_SUCCESS: frs_new = "ADD_SUCCESS"; break;
				case ADD_FAILED: frs_new = "ADD_FAILED"; break;
				case QUEUED: frs_new = "QUEUED"; break;
				case PAUSED : frs_new = "PAUSED"; break;
				case CHECK_RELEVANT_FIELDS : frs_new = "CHECK_RELEVANT_FIELDS"; break;
				case RUNNING : frs_new = "RUNNING"; break;
				case RUN_SUCCESS : frs_new = "RUN_SUCCESS"; break;
				case RUN_FAILED : frs_new = "RUN_FAILED"; break;
				case REMOVED : frs_new = "REMOVED"; break;
				default : frs_new = "UNKNOWN";
			}

			logString = String.format(Locale.getDefault(),
					"Field data request state change %s %s -> %s time : %s" ,requestField.getItemToRequest().getFieldName(), frs_old, frs_new, ts);

			// Write log text
			setTextViewLogger(logString);

		}

		/**
		* onFieldDataRequestAddSuccess event activated when new field data request add success
		* (activated only one time for each new field data request)
		* @param deviceId			Device Id
		* @param deviceInfo			Device info
		* @param requestField		Original requested field information
		* @param overWriteRequest	<code>true</code> if previous request from same field was replaced
		*/
		@Override
		public void onFieldDataRequestAddSuccess(
				@NonNull String deviceId,
				@NonNull DeviceInfo deviceInfo,
				@NonNull RequestField requestField,
				@NonNull Boolean overWriteRequest) {

			// Logging data request add success event
			setTextViewLogger("Field data request add success : " + requestField.getItemToRequest().getFieldName());
		}

		/**
		* Called when new field data request task add failed
		* (activated only one time for each new field data request)
		* @param deviceId		Device Id
		* @param deviceInfo		Device info
		* @param requestField	Original requested field information
		* @param cause			Cause of error as throwable
		*/
		@Override
		public void onFieldDataRequestAddFailed(
				@NonNull String deviceId,
				@NonNull DeviceInfo deviceInfo,
				@NonNull RequestField requestField,
				@NonNull Throwable cause) {

			// Logging data request add failed
			setTextViewLogger("Field data request add failed : "
					+ requestField.getItemToRequest().getFieldName()
					+ "\nCause :"
					+ cause.getLocalizedMessage());
		}

		/**
		 * onFieldDataRequestRemoved event activated when field data request is removed
		 * (activate only one time)
		 * @param deviceId		Device Id for using multiple devices same time
		 * @param deviceInfo	Device info
		 * @param requestField	Original requested field information
		 * @param info			Information of remove (why removed)
		 */
		@Override
		public void onFieldDataRequestRemoved(
				@NonNull String deviceId,
				@NonNull DeviceInfo deviceInfo,
				@NonNull RequestField requestField,
				@NonNull String info) {

			setTextViewLogger("Field read data request removed : " + requestField.getItemToRequest().getFieldName() + " - " + info);
		}

		/**
		 * onFieldWriteStatusChanged event is activated when write process status is changed
		 * @param deviceId		Device Id for using multiple devices same time
		 * @param deviceInfo	Device info
		 * @param fieldItem		Field information
		 * @param data			Data to write into field
		 * @param newValue		New Status
		 * @param oldValue		Old Status
		 */
		@Override
		public void onFieldWriteStatusChanged(
				@NonNull String deviceId,
				@NonNull DeviceInfo deviceInfo,
				@NonNull FieldItem fieldItem,
				@NonNull String data,
				@Nullable FIELD_WRITE_STATE newValue,
				@Nullable FIELD_WRITE_STATE oldValue) {

			setTextViewLogger("Write field state : " + oldValue + " -> " + newValue);
		}

		/**
		 * onFieldWriteSuccess activated when write process is success to LCR device.
		 * (activate only one time for each write process)
		 * @param deviceId		Device Id for using multiple devices same time
		 * @param deviceInfo	Device Info
		 * @param fieldItem		Field information
		 * @param data			Data what is written to field
		 */
		@Override
		public void onFieldWriteSuccess(
				@NonNull String deviceId,
				@NonNull DeviceInfo deviceInfo,
				@NonNull FieldItem fieldItem,
				@NonNull String data) {

			setTextViewLogger("Field data write success : " + fieldItem.getFieldName());
		}

		/**
		 * onFieldWriteFailed activated when field write process has failed
		 * (activate only one time for each write process)
		 * @param deviceId		Device Id for using multiple devices same time
		 * @param deviceInfo	Device Info
		 * @param fieldItem		Field info
		 * @param data			Data what tried to write to field
		 * @param cause			Cause of fail
		 */
		@Override
		public void onFieldWriteFailed(
				@NonNull String deviceId,
				@NonNull DeviceInfo deviceInfo,
				@NonNull FieldItem fieldItem,
				@NonNull String data,
				@Nullable Throwable cause) {

			String errorMsg = "(unknown)";
			if(cause != null) {
				errorMsg = cause.getLocalizedMessage();
			}
			if(fieldItem != null) {
				setTextViewLogger("Field data write failed : " + fieldItem.getFieldName() + " Cause : " + errorMsg);
			} else {
				setTextViewLogger("Field data write failed : " + "null fieldItem" + " Cause : " + errorMsg);
			}
		}
	};

	/** Device command listener */
	public CommandListener commandListener = new CommandListener() {
		/**
		 * onCommandStateChanged listener is activated when ever command state is changed
		 * @param deviceId		Device Id for using multiple devices same time
		 * @param deviceInfo	Information about device
		 * @param command		Command
		 * @param newValue		New Value
		 * @param oldValue		Old Value
		 */
		@Override
		public void onCommandStateChanged(
				@NonNull String deviceId,
				@NonNull DeviceInfo deviceInfo,
				@NonNull LCR_COMMAND command,
				@Nullable COMMAND_STATE newValue,
				@Nullable COMMAND_STATE oldValue) {

			setTextViewLogger("Command state : " + oldValue + " -> " + newValue);
		}

		/**
		 * onCommandSuccess listener is activated when command is run SUCCESSFULLY
		 * @param deviceId		Device Id for using multiple devices same time
		 * @param deviceInfo	Information about device
		 * @param command		Command
		 */
		@Override
		public void onCommandSuccess(
				@NonNull String deviceId,
				@NonNull DeviceInfo deviceInfo,
				@NonNull LCR_COMMAND command) {

			setTextViewLogger("Command success : " + command);
		}

		/**
		 * onCommandFailed listener is activated when command is FAILED to run
		 * @param deviceId		Device Id for using multiple devices same time
		 * @param deviceInfo	Information about device
		 * @param command		Command
		 * @param cause			Cause for command fail (can be <code>null</code>)
		 */
		@Override
		public void onCommandFailed(
				@NonNull String deviceId,
				@NonNull DeviceInfo deviceInfo,
				@NonNull LCR_COMMAND command,
				@Nullable Throwable cause) {

			String errorMsg = "(unknown)";
			if(cause != null) {
				errorMsg = cause.getLocalizedMessage();
			}
			setTextViewLogger("Command failed : " + command + " Cause : " + errorMsg);
		}
	};



	/** Device operation switch state listener */
	public SwitchStateListener switchStateListener = new SwitchStateListener() {
		 /**
		 * Event of Switch State changed
		 * @param deviceId		Device identification
		 * @param deviceInfo	Device info
		 * @param newValue		New switch state
		 * @param oldValue		Old switch state
		 */
		 @Override
		 public void onSwitchStateChanged(
				@NonNull String deviceId,
				@NonNull DeviceInfo deviceInfo,
				@Nullable LCR_SWITCH_STATE newValue,
				@Nullable LCR_SWITCH_STATE oldValue) {

			 // Make variables for show status (getString formatting don't allow null)
			 String newValueText = "(null)";
			 if(newValue != null) {
				 newValueText = newValue.toString();
			 }
			 // Set switch state text
		 	textViewSwitchStateData.setText(newValueText);

			setTextViewLogger("Switch state : " + oldValue + " -> " + newValue);
		}

	};

	/** Listener for most of Device status and state information */
	public DeviceStatusListener deviceStatusListener = new DeviceStatusListener() {

		/**
		 * Event is activated when delivery active state is changed
		 * @param deviceId				Device identification
		 * @param deviceInfo			Device information
		 * @param deliveryActiveState	Delivery active <code>true</code> delivery is active
		 */
		@Override
		public void onDeliveryActiveStateChanged(
				@NonNull String deviceId,
				@NonNull DeviceInfo deviceInfo,
				@NonNull Boolean deliveryActiveState) {

			// Logging delivery active state
			setTextViewLogger("Delivery active state changed : " + deliveryActiveState);
		}

		/**
		 * Event is activated when LCR Device {@link LCR_DEVICE_STATE state} is changed
		 * @param deviceId		Device identification
		 * @param deviceInfo	Device info
		 * @param newValue		New device {@link LCR_DEVICE_STATE state}
		 * @param oldValue		Old device {@link LCR_DEVICE_STATE state}
		 */
		@Override
		public void onDeviceStateChanged(
				@NonNull String deviceId,
				@NonNull DeviceInfo deviceInfo,
				@Nullable LCR_DEVICE_STATE newValue,
				@Nullable LCR_DEVICE_STATE oldValue) {

			// Set device state text
			textViewDeviceStateData.setText(objToStrWithNullCheck(newValue));

			// Set start/pause button by state
			if(buttonChangeCommandState != null && newValue != null) {
				// Any of button state change enable it
				buttonChangeCommandState.setEnabled(true);
				switch (newValue) {
					case STATE_RUN:
						// Set button text
						buttonChangeCommandState.setText(R.string.button_command_State_pause);
						// Set button next action
						buttonChangeCommandState.setTag(BUTTON_ACTIONS.ACTIVE);
						setGrossPresetActionsEnabledState(false);
						break;
					default:
						// Set button text
						buttonChangeCommandState.setText(R.string.button_command_state_start);
						// Set button next action
						buttonChangeCommandState.setTag(BUTTON_ACTIONS.PASSIVE);
						setGrossPresetActionsEnabledState(true);
						break;
				}
			}
			// Logging device state
			setTextViewLogger("Device state changed : " + oldValue + " -> " + newValue);
		}


		/**
		* Event is activated when one of {@link LCR_DELIVERY_CODE LCR_DELIVERY_CODE} status is changed.
		* Each delivery code has values of <code>null</code>, <code>true</code> or <code>false</code>
		* @param deviceId	Device identification
		* @param deviceInfo	Device info
		* @param code		Delivery code {@link LCR_DELIVERY_CODE}
		* @param newValue	New value
		* @param oldValue	Old value
		*/
		@Override
		public void onDeliveryCodeChanged(
				@NonNull String deviceId,
				@NonNull DeviceInfo deviceInfo,
				@NonNull LCR_DELIVERY_CODE code,
				@Nullable Boolean newValue,
				@Nullable Boolean oldValue) {

			// Check out FLOW_ACTIVE event
			if(code.equals(LCR_DELIVERY_CODE.FLOW_ACTIVE)) {
				if(newValue == null) {
					textViewFlowStateData.setText(R.string.text_state_unknown);
				} else if(newValue) {
					textViewFlowStateData.setText(R.string.text_flow_state_on);
				} else {
					textViewFlowStateData.setText(R.string.text_flow_state_off);
				}
			}
			// Logging delivery code status
			setTextViewLogger("Delivery code changed : " + code + " " + oldValue + " -> " + newValue);
		}

		/**
		 * Event is activated when one of {@link LCR_DELIVERY_STATUS LCR_DELIVERY_STATUS} code is changed.
		 * Each delivery status code has values of <code>null</code>, <code>true</code> or <code>false</code>
		 * @param deviceId		Device identification
		 * @param deviceInfo	Device info
		 * @param code			{@link LCR_DELIVERY_STATUS LCR_DELIVERY_STATUS} code
		 * @param newValue		New Value
		 * @param oldValue		Old Value
		 */
		public void onDeliveryStatusChanged(
				@NonNull String deviceId,
				@NonNull DeviceInfo deviceInfo,
				@NonNull LCR_DELIVERY_STATUS code,
				@Nullable Boolean newValue,
				@Nullable Boolean oldValue) {

			// Logging delivery status codes
			setTextViewLogger("Delivery Status changed : " + code + " " + objToStrWithNullCheck(oldValue) + " -> " + objToStrWithNullCheck(newValue));
		}

		/**
		 * Event is activated when one of security level {@link LCR_SECURITY_LEVEL code} value is changed.
		 * @param deviceId		Device identification
		 * @param deviceInfo	Device info
		 * @param newValue		New value of current security level code
		 * @param oldValue		Old value of current security level code
		 */
		public void onSecurityLevelChanged(
				@NonNull String deviceId,
				@NonNull DeviceInfo deviceInfo,
				@Nullable LCR_SECURITY_LEVEL newValue,
				@Nullable LCR_SECURITY_LEVEL oldValue) {

			// Logging security level (old security level -> new security level)
			setTextViewLogger("Security level changed : " + " " + oldValue + " -> " + newValue);
		}
	};

	/** Printer status monitoring */
	PrinterStatusListener printerStatusListener = new PrinterStatusListener() {
		/**
		 * Event is activated when printer status is changed
		 * @param DeviceID		Device Id
		 * @param deviceInfo	Device info
		 * @param statusCode	Printer status code
		 * @param newValue		New state for current status code
		 * @param oldValue		Old state for current status code
		 */
		@Override
		public void onPrinterStatusChanged(
				@NonNull String DeviceID,
				@NonNull DeviceInfo deviceInfo,
				@NonNull LCR_PRINTER_STATUS statusCode,
				@Nullable Boolean newValue,
				@Nullable Boolean oldValue) {

			// Logging printer status codes and values
			setTextViewLogger("Printer Status : " + statusCode + " " + oldValue + " -> " + newValue);
		}

		/**
		 * onPrintStatusChanged Listener is activated when print status for print item is changed.
		 * Sta
		 * @param DeviceId		Device identification
		 * @param deviceInfo	Device information
		 * @param workId		Print work identification
		 * @param newValue		Print item new status
		 * @param oldValue		Print item old status
		 */
		@Override
		public void onPrintStatusChanged(
				@NonNull String DeviceId,
				@NonNull DeviceInfo deviceInfo,
				@NonNull String workId,
				@Nullable PRINTING_STATE newValue,
				@Nullable PRINTING_STATE oldValue) {

			// Logging printing status change
			setTextViewLogger("Printing status : " + oldValue + " -> " + newValue);
		}

		/**
		 * onPrintSuccess event is activated when print data is successfully send to LCR device.
		 * @param deviceId		Device identification
		 * @param deviceInfo	Device information
		 * @param workId		Print work identification
		 */
		@Override
		public void onPrintSuccess(
				@NonNull String deviceId,
				@NonNull DeviceInfo deviceInfo,
				@NonNull String workId) {

			// Logging print success
			setTextViewLogger("Printing success");
		}

		/**
		 * onPrintFailed event is activated when sending print data to LCR device has failed
		 * @param deviceId		Device identification
		 * @param deviceInfo	Device information
		 * @param workId		Print work identification
		 * @param cause			Error cause (note! can be <code>null</code>)
		 */
		@Override
		public void onPrintFailed(
				@NonNull String deviceId,
				@NonNull DeviceInfo deviceInfo,
				@NonNull String workId,
				@Nullable Throwable cause) {

			String strCause = "(null)";
			if(cause != null && cause.getMessage() != null) {
				strCause = String.format(
						Locale.getDefault(),
						"LCP Error : %s",
						cause.getMessage());
			}
			// Logging print work failed
			setTextViewLogger("Print failed : " + strCause);
		}
	};

	/**
	 * DeviceCommunicationListener report SDK communication information to LCR device
	 */
	private DeviceCommunicationListener deviceCommunicationListener = new DeviceCommunicationListener() {

		/**
		 * Notify when message state is changed between SDK and LCR device
		 * NOTE! This listener update very high frequency
		 * @param deviceId		Device identification string
		 * @param deviceInfo	Device info
		 * @param newValue		New value
		 * @param oldValue		Old value
		 */
		@Override
		public void onMessageStateChanged(
				@NonNull String deviceId,
				@NonNull DeviceInfo deviceInfo,
				@Nullable LCR_MESSAGE_STATE newValue,
				@Nullable LCR_MESSAGE_STATE oldValue) {

		}

		/**
		 * Notify when SDK has detect communication error with LCR device.
		 *
		 * Also notify some important internal SDK errors (example. generated output message size errors).
		 *
		 * About event trace :
		 * - Event trace start when device object get run turn from SDK DeviceRunner Thread
		 * - Event list actions is add in key points of SDK and LCR device communicating
		 * - Event list is cleared when device object finnish run or onCommunicationStatus error listeners is notified
		 *
		 * Event trace is not yet fully implemented inside SDK. More events will add recording later on.
		 *
		 * @param deviceId		Device identification string
		 * @param deviceInfo	Device Info
		 * @param cause			Special type of exception
		 */
		@Override
		public void onCommunicationStatusError(
				@NonNull String deviceId,
				@NonNull DeviceInfo deviceInfo,
				@NonNull LCRCommunicationException cause) {

			Log.e("ERROR_EVENT", "---------------------------------");
			Log.e("ERROR_EVENT", "Error event from : " + deviceId);
			Log.e("ERROR_EVENT", "-------- event data start--------");
			// Print events trace to lead up in error (not complete trace yet)
			Integer lineNumber = 1;
			for(InternalEvent event : cause.getEvents()) {
				// Print events (all type of events)
				Log.e("ERROR_EVENT", String.valueOf(lineNumber++) + " - " + objToStrWithNullCheck(event.toShortFormat()));
			}
			Log.e("ERROR_EVENT", "--------- event data end ---------");
		}

		/**
		 * Notify when SDK communication status with LCR device is changed
		 * !! NOTE !!
		 * Not full implemented inside SDK
		 * @param deviceId		Device identification string
		 * @param deviceInfo	Device info
		 * @param newValue		New Value
		 * @param oldValue		Old Value
		 */
		@Override
		public void onCommunicationStatusChanged(
				@NonNull String deviceId,
				@NonNull DeviceInfo deviceInfo,
				@Nullable LCR_COMMUNICATION_STATUS newValue,
				@Nullable LCR_COMMUNICATION_STATUS oldValue) {
		}
	};

	/** Monitor network connection states (for all devices) */
	public NetworkConnectionListener networkConnectionListener = new NetworkConnectionListener() {

		/**
		 * Called when network connection state changed
		 * @param networkType		Network type
		 * @param connectionOptions	Connection options
		 * @param attachedDevices	List of devices what is attach to network
		 * @param newValue			New value
		 * @param oldValue			Old Value
		 */
		@Override
		public void onNetworkConnectionStateChange(
				@NonNull NETWORK_TYPE networkType,
				@NonNull ConnectionOptions connectionOptions,
				@NonNull List<DeviceInfo> attachedDevices,
				@Nullable LCR_THREAD_CONNECTION_STATE newValue,
				@Nullable LCR_THREAD_CONNECTION_STATE oldValue) {

			// Device level network status change is reported also in DeviceConnectionListener#deviceNetworkStateChanged
		}

		/**
		 * Called when network connection is success
		 * @param networkType		Network type
		 * @param connectionOptions	Connection options
		 * @param attachedDevices	Devices
		 */
		@Override
		public void onNetworkConnected(
				@NonNull NETWORK_TYPE networkType,
				@NonNull ConnectionOptions connectionOptions,
				@NonNull List<DeviceInfo> attachedDevices) {

			// Logging network connect
			setTextViewLogger("Network disconnected : " + networkType.name());
		}

		/**
		 * Called when network is disconnected
		 * @param networkType		Network type
		 * @param connectionOptions	Connection options
		 * @param attachedDevices	Devices
		 * @param cause				Cause of error
		 */
		@Override
		public void onNetworkDisconnected(
				@NonNull NETWORK_TYPE networkType,
				@NonNull ConnectionOptions connectionOptions,
				@NonNull List<DeviceInfo> attachedDevices,
				@Nullable Throwable cause) {

			String strCause = "(null)";
			if(cause != null) {
				strCause = cause.getMessage();
			}
			// Logging network disconnecting
			setTextViewLogger("Network disconnected : " + networkType.name() + " : " + strCause);
		}

		/**
		 * Called when network is on error (need user operations for recover)
		 * @param networkType		Network type
		 * @param connectionOptions	Connection options
		 * @param attachedDevices	Devices
		 * @param cause				Cause of error
		 */
		@Override
		public void onNetworkError(
				@NonNull NETWORK_TYPE networkType,
				@NonNull ConnectionOptions connectionOptions,
				@NonNull List<DeviceInfo> attachedDevices,
				@Nullable Throwable cause) {

			String strCause = "(null)";
			if(cause != null) {
				strCause = cause.getMessage();
			}
			// Logging network error
			setTextViewLogger("Network error : " + networkType.name() + " : " + strCause);
		}
	};
}


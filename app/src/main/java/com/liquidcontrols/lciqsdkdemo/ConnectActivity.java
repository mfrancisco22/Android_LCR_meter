package com.liquidcontrols.lciqsdkdemo;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Set;

public class ConnectActivity extends AppCompatActivity {
    private TextView ipAddressLabel;
    private TextView ipPortLabel;
    private TextView btAddressLabel;
    private TextView lcpAddressLabel;
    private EditText lcpAddress;
    private EditText ipAddress;
    private EditText ipPort;
    private EditText btAddress;
    private ListView pairedDevicesView;
    private ArrayAdapter aAdapter;
    private BluetoothAdapter bAdapter = BluetoothAdapter.getDefaultAdapter();

    // Connections
    private enum CONNECTION_TYPE {
        NONE,
        BLUETOOTH,
        WIFI
    }

    private CONNECTION_TYPE connectionToUse = CONNECTION_TYPE.NONE;

    // Select Bluetooth
    private void selectBluetooth(boolean enable) {
        // Bluetooth controls
        if ( btAddressLabel != null) {
            btAddressLabel.setEnabled(enable);
        }
        if ( btAddress != null) {
            btAddress.setEnabled(enable);
            // Focus
            if ( enable )
                btAddress.requestFocus();
        }

        if ( pairedDevicesView != null) {
            pairedDevicesView.setEnabled(enable);
        }

        if ( enable )
            connectionToUse = CONNECTION_TYPE.BLUETOOTH;
    }

    // Select Bluetooth from JSON
    private void selectBluetoothFromJSON(boolean enable) {
        // Bluetooth controls
        if ( btAddressLabel != null) {
            btAddressLabel.setEnabled(enable);
        }
        if ( btAddress != null) {
            btAddress.setEnabled(enable);
            // Focus
            if ( enable )
                btAddress.requestFocus();
        }

        if ( pairedDevicesView != null) {
            pairedDevicesView.setEnabled(enable);
        }

        if ( enable )
            connectionToUse = CONNECTION_TYPE.BLUETOOTH;
    }

    // Select Wi-Fi
    private void selectWiFi(boolean enable) {
        // Wi-Fi controls
        if (ipAddressLabel != null) {
            ipAddressLabel.setEnabled(enable);
        }

        if (ipAddress != null) {
            ipAddress.setEnabled(enable);
            // Focus
            if ( enable )
                ipAddress.requestFocus();
        }

        if (ipPortLabel != null) {
            ipPortLabel.setEnabled(enable);
        }

        if (ipPort != null) {
            ipPort.setEnabled(enable);
        }

        if ( enable )
            connectionToUse = CONNECTION_TYPE.WIFI;
    }

    // Navigation support
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_connect);
        this.setFinishOnTouchOutside(false);

        // IP Address
        ipAddressLabel = (TextView) findViewById(R.id.ip_address_label);
        ipAddress = (EditText) findViewById(R.id.ip_address);
        ipAddress.setSelection(ipAddress.getText().length());

        // IP Port
        ipPortLabel = (TextView) findViewById(R.id.ip_port_label);
        ipPort = (EditText) findViewById(R.id.ip_port);
        ipPort.setSelection(ipPort.getText().length());

        // LCP Address
        lcpAddressLabel = findViewById(R.id.lcp_node_address_label);
        lcpAddress = findViewById(R.id.lcp_node_address);
        lcpAddress.setSelection(lcpAddress.getText().length());
        lcpAddress.setFocusable(true);
        lcpAddress.setEnabled(true);


        InputFilter[] filters = new InputFilter[1];
        filters[0] = new InputFilter() {
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                if (end > start) {
                    String destTxt = dest.toString();
                    String resultingTxt = destTxt.substring(0, dstart) + source.subSequence(start, end) + destTxt.substring(dend);
                    if (!resultingTxt.matches ("^\\d{1,3}(\\.(\\d{1,3}(\\.(\\d{1,3}(\\.(\\d{1,3})?)?)?)?)?)?")) {
                        return "";
                    } else {
                        String[] splits = resultingTxt.split("\\.");
                        for (int i=0; i<splits.length; i++) {
                            if (Integer.valueOf(splits[i]) > 255) {
                                return "";
                            }
                        }
                    }
                }
                return null;
            }
        };
        ipAddress.setFilters(filters);


        // Bluetooth address
        btAddressLabel = (TextView) findViewById(R.id.bt_device_label);
        btAddress = (EditText) findViewById(R.id.bt_device_address);

        // Bluetooth Paired Devices View
        pairedDevicesView = (ListView) findViewById(R.id.deviceList);
        pairedDevicesView.setClickable(true);
        pairedDevicesView.setAdapter(aAdapter);
        // Handle on click
        pairedDevicesView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position,
                                    long id) {

                String item = ((TextView)view).getText().toString();
                // Parse string into name and baddr
                // [LCRIQ-f8-dc-7a-18-93-6e][A8:1B:6A:6E:5A:DA]
                // [][A8:1B:6A:6E:5A:DA]
                int idx1 = item.indexOf("[", 0);
                int idx2 = item.indexOf("]", idx1);
                int idx3 = item.indexOf("[", idx2);
                int idx4 = item.indexOf("]", idx3);
                String name=item.substring(idx1+1,idx2);
                String baddr=item.substring(idx3+1,idx4);
                if ( name.isEmpty()) {
                    item = baddr;
                } else {
                    item = name;
                }
                btAddress.setText(item);
                Toast.makeText(getBaseContext(), "Selected" + item, Toast.LENGTH_LONG).show();
            }
        });


        // OK button
        Button btnOK = (Button) findViewById(R.id.ok_button);
        btnOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle extras = new Bundle();
                Intent result = new Intent();
                String value;
                if ( connectionToUse == CONNECTION_TYPE.WIFI ) {
                    extras.putString("connection", "wifi");
                    value = ipAddress.getText().toString();
                    extras.putString("value", value);
                    value = ipPort.getText().toString();
                    extras.putString("value2", value);
                    value = lcpAddress.getText().toString();
                    extras.putString("lcpAddress", value);
                    result.putExtras(extras);
                    // sets the result for the calling activity
                    setResult(RESULT_OK, result);
                } else  if ( connectionToUse == CONNECTION_TYPE.BLUETOOTH ) {
                    value = btAddress.getText().toString();
                    extras.putString("connection", "bluetooth");
                    extras.putString("value", value);
                    extras.putString("value2", ""); // TBI
                    value = lcpAddress.getText().toString();
                    extras.putString("lcpAddress", value);
                    result.putExtras(extras);
                    // sets the result for the calling activity
                    setResult(RESULT_OK, result);
                } else {
                    // Not selected
                    // sets the result for the calling activity
                    setResult(RESULT_CANCELED, null);
                }
                // Finish this activity
                finish();
            }
        });

        // WiFi button
        Button btnWiFi = (Button)findViewById(R.id.button_select_wifi);
        btnWiFi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectWiFi(true);
                selectBluetooth(false);
                selectBluetoothFromJSON(false);
            }
        });

        // Bluetooth button
        Button btn = (Button)findViewById(R.id.button_select_bluetooth);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectWiFi(false);
                selectBluetooth(true);


                Set<BluetoothDevice> pairedDevices = bAdapter.getBondedDevices();
                ArrayList list = new ArrayList();
                if(pairedDevices.size()>0){
                    for(BluetoothDevice device: pairedDevices){
                        String devicename = device.getName();
                        String baddr = device.getAddress();
                        String description = "NA";
                        list.add("["+devicename+"] ["+baddr+"] "+description);
                        //list.add(devicename);
                    }

                    aAdapter = new ArrayAdapter(getApplicationContext(), android.R.layout.simple_list_item_1, list){
                        @Override
                        public View getView(int position, View convertView, ViewGroup parent){
                            // Get the current item from ListView
                            View view = super.getView(position,convertView,parent);
                            if(position %2 == 1)
                            {
                                // Set a background color for ListView regular row/item
                                view.setBackgroundColor(getResources().getColor(R.color.colorGrayLight));
                            }
                            else
                            {
                                // Set the background color for alternate row/item
                                view.setBackgroundColor(getResources().getColor(R.color.colorGrayDark));
                            }
                            return view;
                        }
                    };

                    pairedDevicesView.setAdapter(aAdapter);
                }

            }
        });

        // Bluetooth from JSON button
        Button btnBTJSON = (Button)findViewById(R.id.button_select_bluetooth_from_json);
        btnBTJSON.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectWiFi(false);
                selectBluetoothFromJSON(true);

                String json = null;
                try {
                    InputStream is = getAssets().open("BTDevice.json");
                    int size = is.available();
                    byte[] buffer = new byte[size];
                    is.read(buffer);
                    is.close();
                    json = new String(buffer, "UTF-8");
                } catch (IOException ex) {
                    ex.printStackTrace();
                    return;
                }

                ArrayList list = new ArrayList();
                try {
                    JSONObject obj = new JSONObject(json);
                    JSONArray arr = obj.getJSONArray("devices");
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject device = arr.getJSONObject(i);
                        String devicename = device.optString("name");
                        String baddr = device.optString("baddr");
                        String description = device.optString("description");
                        list.add("["+devicename+"] ["+baddr+"] "+description);
                        //list.add(devicename);
                    }
                } catch ( JSONException ex) {
                    ex.printStackTrace();
                    return;
                }

                aAdapter = new ArrayAdapter(getApplicationContext(), android.R.layout.simple_list_item_1, list){
                    @Override
                    public View getView(int position, View convertView, ViewGroup parent){
                        // Get the current item from ListView
                        View view = super.getView(position,convertView,parent);
                        if(position %2 == 1)
                        {
                            // Set a background color for ListView regular row/item
                            view.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
                        }
                        else
                        {
                            // Set the background color for alternate row/item
                            view.setBackgroundColor(getResources().getColor(R.color.colorPrimaryLight));
                        }
                        return view;
                    }
                };

                pairedDevicesView.setAdapter(aAdapter);

            }
        });

        // Initialize Views
        if(bAdapter==null){
            Toast.makeText(getApplicationContext(),"Bluetooth Not Supported", Toast.LENGTH_SHORT).show();
        }
        else{
            /*** TBI ***/
        }

        // By default enable Wi-Fi
        selectWiFi(true);
        selectBluetooth(false);
    }

}

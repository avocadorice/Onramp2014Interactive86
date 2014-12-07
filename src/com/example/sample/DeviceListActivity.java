/*********************************************************************/
/* Copyright (c) 2014 TOYOTA MOTOR CORPORATION. All rights reserved. */
/*********************************************************************/

package com.example.sample;


import java.util.Set;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class DeviceListActivity extends Activity {

    public static String EXTRA_DEVICE_ADDRESS = "device_address";
    private BluetoothAdapter mBtAdapter;
    private ArrayAdapter<String> mPairedDevicesArrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.device_list);
	
		setResult(Activity.RESULT_CANCELED);
	
		setupComponent();
	
		setupDeviceList();
    }

    private void setupComponent() {
	
		mPairedDevicesArrayAdapter = new ArrayAdapter<String>(this,
			R.layout.device_name);
	
		ListView pairedListView = (ListView) findViewById(R.id.paired_devices);
		pairedListView.setAdapter(mPairedDevicesArrayAdapter);
		pairedListView.setOnItemClickListener(mDeviceClickListener);
    }

    private void setupDeviceList() {

		mBtAdapter = BluetoothAdapter.getDefaultAdapter();
		Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();

		if (pairedDevices.size() > 0) {
		    findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);
		    for (BluetoothDevice device : pairedDevices) {
			mPairedDevicesArrayAdapter.add(device.getName() + "\n"
				+ device.getAddress());
		    }
		} else {
		    String noDevices = "Device not found";
		    mPairedDevicesArrayAdapter.add(noDevices);
		}
    }

    private OnItemClickListener mDeviceClickListener = new OnItemClickListener() {

		public void onItemClick(AdapterView<?> av, View view, int position, long id) {
		    String info = ((TextView) view).getText().toString();
		    String address = info.substring(info.length() - 17);

		    Intent intent = new Intent();
		    intent.putExtra(EXTRA_DEVICE_ADDRESS, address);

		    setResult(Activity.RESULT_OK, intent);
		    finish();
		}
    };
}

/*********************************************************************/
/* Copyright (c) 2014 TOYOTA MOTOR CORPORATION. All rights reserved. */
/*********************************************************************/

package com.example.sample;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.UUID;
import android.os.Build;
import android.util.Log;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

public class Communication  {

	/* Bluetooth Serial Port Profile */
	private static final UUID MY_UUID = UUID
			.fromString("00001101-0000-1000-8000-00805f9b34fb");

	private ConnectThread _connectThread;
	private ConnectedThread _connectedThread;
	private int _state;

	public static final int STATE_NONE = 0;
	public static final int STATE_CONNECTING = 2;
	public static final int STATE_CONNECTED = 3;
	public static final int STATE_CONNECT_FAILED = 4;
	public static final int STATE_DISCONNECTED = 5;

	private ICommNotify _iCommNotify;
	private String _tag = "Communication";

	public Communication() {
		setState(STATE_NONE);
	}

	private synchronized void setState(int state) {
		_state = state;
	}

	public synchronized int getState() {
		return _state;
	}

	public synchronized void connect(BluetoothDevice device) throws IOException{
		if (getState() == STATE_CONNECTING) {
			if (_connectThread != null) {
				_connectThread.cancel();
				_connectThread = null;
			}
		}

		if (_connectedThread != null) {
			_connectedThread.cancel();
			_connectedThread = null;
		}

		try {
			_connectThread = new ConnectThread(device);
			setState(STATE_CONNECTING);
			notifyBTState(STATE_CONNECTING);
			_connectThread.start();
		} catch (IOException e) {
			e.printStackTrace();
			throw e;
		}
	}

	public synchronized void connected(BluetoothSocket socket,
			BluetoothDevice device) {
		if (_connectThread != null) {
			_connectThread.cancel();
			_connectThread = null;
		}

		if (_connectedThread != null) {
			_connectedThread.cancel();
			_connectedThread = null;
		}

		_connectedThread = new ConnectedThread(socket);
		_connectedThread.start();

		setState(STATE_CONNECTED);
	}

	private void connectionFailed() {
		setState(STATE_NONE);
	}

	private void connectionLost() {
		setState(STATE_NONE);
	}

	private class ConnectThread extends Thread {
		private final BluetoothSocket mmSocket;
		private final BluetoothDevice mmDevice;

		public ConnectThread(BluetoothDevice device) throws IOException {
			mmDevice = device;
			BluetoothSocket tmp = null;

			try {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD_MR1){
					/* Android 2.3 and higher */
					tmp = device.createInsecureRfcommSocketToServiceRecord(MY_UUID);
				}else{
					/* under Android 2.3 */
					tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
				}

			} catch (IOException e) {
				Log.e(_tag,"#### device createRfcommSocketToServiceRecord Error #### " + e.getMessage());
				connectionFailed();
				notifyBTState(STATE_CONNECT_FAILED);
				throw e;
			}
			mmSocket = tmp;
		}

		public void run() {
			setName("ConnectThread");

			try {
				BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
				adapter.cancelDiscovery();
				
				Log.d(_tag,"#### CONNECTING ####");
				mmSocket.connect();
				Log.d(_tag,"#### CONNECT OK ####");
				notifyBTState(STATE_CONNECTED);
			} catch (IOException e) {
				Log.e(_tag,"#### socket connect Error #### " + e.getMessage());
				connectionFailed();
				notifyBTState(STATE_CONNECT_FAILED);
				try {
					mmSocket.close();
				} catch (IOException e2) {
					Log.e(_tag,"#### socket close Error #### " + e.getMessage());
				}
				return;
			}

			synchronized (Communication.this) {
				_connectThread = null;
			}
			connected(mmSocket, mmDevice);

		}

		public void cancel() {
			try {
				Log.d(_tag,"#### mmSocket.close()  ####");
				mmSocket.close();
			} catch (IOException e) {
				Log.e(_tag,"#### socket close Error #### " + e.getMessage());
			}
		}
	}

	private class ConnectedThread extends Thread {
		private boolean available;
		private final BluetoothSocket mmSocket;
		private final InputStream mmInStream;
		private final OutputStream mmOutStream;

		public ConnectedThread(BluetoothSocket socket) {
			available = true;
			mmSocket = socket;
			InputStream tmpIn = null;
			OutputStream tmpOut = null;

			/* Get the BluetoothSocket input and output streams */
			try {
				tmpIn = socket.getInputStream();
				tmpOut = socket.getOutputStream();
			} catch (IOException e) {
				Log.e(_tag,"#### socket getInputStream Error #### " + e.getMessage());
			}

			mmInStream = tmpIn;
			mmOutStream = tmpOut;
		}

		public void run() {
			byte[] buffer = new byte[1024];
			int bytes;

			/* Keep listening to the InputStream while connected */
			while (available) {
				try {
					/* Read from the InputStream */
					bytes = mmInStream.read(buffer);
					if(bytes <= 0){
						connectionLost();
						notifyBTState(STATE_DISCONNECTED);
						break;
					}
					notifyDatRcv(buffer, bytes);

				} catch (IOException e) {
					e.printStackTrace();
					Log.e(_tag,"#### StreamReadError #### " + e.getMessage());
					connectionLost();
					notifyBTState(STATE_DISCONNECTED);
					break;
				}
			}
		}

		public void write(byte[] buffer) throws IOException {
			try {
				mmOutStream.write(buffer);

			} catch (IOException e) {
				Log.e(_tag,"#### StreamWriteError #### " + e.getMessage());
				throw e;
			}
		}

		public void cancel() {
			available = false;
			try {
				Log.d(_tag,"#### mmInStream.close()  ####");
				mmInStream.close();
				Log.d(_tag,"#### mmOutStream().close()  ####");
				mmOutStream.close();
				Log.d(_tag,"#### mmSocket.close()  ####");
				mmSocket.close();
			} catch (IOException e) {
				Log.e(_tag,"#### socket close Error #### " + e.getMessage());
			}
		}
	}



	private void notifyBTState(int nState) {
		if (_iCommNotify != null){
			_iCommNotify.notifyBluetoothState(nState);
		}
	}

	private void notifyDatRcv(byte[] byBuffer, int nSize) {
		if (_iCommNotify != null){
			ByteBuffer buf = ByteBuffer.wrap(byBuffer, 0, nSize);
			_iCommNotify.notifyReceiveData(buf);
		}
	}

	public synchronized boolean closeSession() {
		boolean result = false;
		if (_connectThread != null) {
			_connectThread.cancel();
			_connectThread = null;
			result = true;
		}
		if (_connectedThread != null) {
			_connectedThread.cancel();
			_connectedThread = null;
			result = true;
		}
		setState(STATE_NONE);
		return result;
	}

	public synchronized void writeData(ByteBuffer data){
		if (_connectedThread != null) {
			try {
				_connectedThread.write(data.array());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public synchronized Boolean openSession(String strDevAddress){
		try {
			/* Get the Bluetooth device by the address */
			BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(strDevAddress);
			connect(device);
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	public void setICommNotify(ICommNotify iCommNotify) {
		_iCommNotify = iCommNotify;
	}
	
	
	public Boolean isCommunication(){
		if ((_state == STATE_CONNECTING) || (_state == STATE_CONNECTED)) {
			return true;
		}
		return false;
	}
}

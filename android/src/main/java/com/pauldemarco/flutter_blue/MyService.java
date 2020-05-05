package com.pauldemarco.flutter_blue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;


public class MyService extends Service {
	private static int FLAG = 0;  
	private static int MSG_UPDATE=2;
	//private final static String TAG = Ble_Activity.class.getSimpleName();
	public static String HEART_RATE_MEASUREMENT = "0000ffe1-0000-1000-8000-00805f9b34fb";
	protected static String EXTRAS_DEVICE_NAME ="DEVICE_NAME";;
	protected static String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
	protected static String EXTRAS_DEVICE_RSSI = "RSSI";
    public static boolean mConnected = false;
    private String status="disconnected";
	//private String mDeviceName;
    private String mDeviceAddress;
    //private String mRssi;
    //private Bundle b;
    public static boolean nConnect=false;
    
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    public static BluetoothGattCharacteristic target_chara=null;
    public static String RecvString="";
    public static String ObjectRecv="";
    private Handler myHandler = new Handler() {  
        //2.????????????
        public void handleMessage(Message msg) {   
             switch (msg.what) {   
                  //??????????
                  case 1:   
                  {
                       //????View
                	   String state = msg.getData().getString("connect_state");
                       if(state.equals("connected"))
                       {
                    	   nConnect=true;
                       }
                       else
                       {
                    	   nConnect=false;
                       }
                       break;   
                   }  
                  case 2:
                  {
                	  String  state = msg.getData().getString("RecvData");
                	  /*if((state.length()==26)&&(state.substring(0, 6).equals("0C00EE")))//15693
                	  {
                		  ObjectRecv=state;
                		  state="";
                	  }*/
                	  RecvString+=state;
                	  break;
                  }
             }
             super.handleMessage(msg);   
        }  
   };
   public static String DeviceName="";
   private static final int Type = 0;
   private Handler mHandler;
   private ArrayList<Integer> rssis;
   public static ArrayList<BluetoothDevice> mBleArray;
   boolean _discoveryFinished = false;    
   boolean bRun = true;
   boolean bThread = false;
   public static BluetoothAdapter mBluetoothAdapter;
   public static boolean mScanning;
   public static boolean scan_flag;
   int REQUEST_ENABLE_BT=1;
   private static final long SCAN_PERIOD = 10000;
   public static int Reader_type=0;
   public MyService() {
   }
   private static final String TAG = "LocalService"; 
   private IBinder binder=new LocalBinder();
   @Override
   public IBinder onBind(Intent intent) {
	   return binder;
   }
   
   //????????????Binder
   public class LocalBinder extends Binder{
       //??????????
	   MyService getService(){
           return MyService.this;
       }
   }

   @Override 
   public void onStart(Intent intent, int startId) { 
           Log.i(TAG, "onStart"); 
           super.onStart(intent, startId); 
           mHandler=new Handler();
           mScanning=false;
           mBleArray = new ArrayList<BluetoothDevice>();
           Intent gattServiceIntent = new Intent(MyService.this, BluetoothLeService.class);
    	   bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    	   registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
   } 
   
   @Override 
   public void onDestroy() { 
           Log.i(TAG, "onDestroy"); 
           super.onDestroy(); 
           unbindService(mServiceConnection);
           BTClient.mBluetoothLeService = null;
   } 
   
   /*service ???????*/
   private final ServiceConnection mServiceConnection = new ServiceConnection() {
       @Override
       public void onServiceConnected(ComponentName componentName, IBinder service) {
       	BTClient.mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
           if (!BTClient.mBluetoothLeService.initialize()) {
           }
           // Automatically connects to the device upon successful start-up initialization.
           //????bluetoothservice ??connect ??????????????
           BTClient.mBluetoothLeService.connect(mDeviceAddress);
       }

       @Override
       public void onServiceDisconnected(ComponentName componentName) {
       	BTClient.mBluetoothLeService = null;
       }
   };
   
   
   private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
       @Override
       public void onReceive(Context context, Intent intent) {
           final String action = intent.getAction();
           if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
               mConnected = true;
               status="connected";
               updateConnectionState(status);
               //nConnect=true;
               /////////////////////////////////////////////////////////////
               System.out.println("BroadcastReceiver :"+"device connected");
             
           } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
               mConnected = false;
               status="disconnected";
               updateConnectionState(status);
               //nConnect=false;
              // unregisterReceiver(mGattUpdateReceiver);//??????????
 			  //  BTClient.mBluetoothLeService = null;
               System.out.println("BroadcastReceiver :"+"device disconnected");
              
           } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
               // Show all the supported services and characteristics on the user interface.
               displayGattServices(BTClient.mBluetoothLeService.getSupportedGattServices());
           	 System.out.println("BroadcastReceiver :"+"device SERVICES_DISCOVERED");
           } 
          	 else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
          		//byte[] Msg = intent.getExtras().getByteArray(BluetoothLeService.EXTRA_DATA);
          		
          		 
          		 String temp =intent.getExtras().getString(
						BluetoothLeService.EXTRA_DATA);
          		
              displayData(temp);
          	// System.out.println("BroadcastReceiver onData:"+intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
          }
       }
   };
   
   /*??????????*/
   private void updateConnectionState( String status)
   {
       Message msg =new Message();
       msg.what=1;
       Bundle b = new Bundle();
       b.putString("connect_state", status);
       msg.setData(b);
   	   myHandler.sendMessage(msg);
   	   System.out.println("connect_state:"+status);
   }
   
   
   /*?????????*/
   private  IntentFilter makeGattUpdateIntentFilter() {
       final IntentFilter intentFilter = new IntentFilter();
       intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
       intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
       intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
       intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
       return intentFilter;
   }
   
  
   private  void  displayData(String rev_string)
   {
   	   Message msg =new Message();
       msg.what=2;
       Bundle b = new Bundle();
       b.putString("RecvData", rev_string);
       msg.setData(b);
   	   myHandler.sendMessage(msg);
   	   System.out.println("RecvData:"+rev_string);
   }
   public void ScanBtDevice(boolean enable)
   {
	   if (enable) {
           Log.i("SCAN", "begin.....................");
           mScanning = true;
           scan_flag=false;
           if(!mBleArray.isEmpty())
           mBleArray.clear();
           mBluetoothAdapter.startLeScan(mLeScanCallback);
       } else {
       	Log.i("Stop", "stoping................");
           mScanning = false;
           mBluetoothAdapter.stopLeScan(mLeScanCallback);
           scan_flag=true;
       }
   }
   public boolean GetConnectState()
   {
	   return nConnect;
   }
   public void ConnectBT(String mDeviceAddress)
   {
       if (mScanning) {
       	/*???????*/
           mBluetoothAdapter.stopLeScan(mLeScanCallback);
           mScanning = false;
       }

       if(BTClient.mBluetoothLeService != null) {
       final boolean result = BTClient.mBluetoothLeService.connect(mDeviceAddress);
      }
   }
   
   public void DisconnectBT() {
	       //unregisterReceiver(mGattUpdateReceiver);
	       BTClient.mBluetoothLeService.disconnect();
	       BTClient.mBluetoothLeService.close();
		   //BTClient.mBluetoothLeService = null;  	   
       }
   private void displayGattServices(List<BluetoothGattService> gattServices){
   	 
   	 
		 if (gattServices == null) return;
	        String uuid = null;
	        String unknownServiceString = "unknown_service";
	        String unknownCharaString = "unknown_characteristic";
		 
	        //????????,?????????????????????
	        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
	        
	        //??????????????????????????????????????
	        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
	                = new ArrayList<ArrayList<HashMap<String, String>>>();
	        
	        //????????????????????
	        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
	        
	     // Loops through available GATT Services.
	        for (BluetoothGattService gattService : gattServices) {
	        
	        	//??????????
	        	HashMap<String, String> currentServiceData = new HashMap<String, String>();
	            uuid = gattService.getUuid().toString();
	            
	            //????????uuid????????????????SampleGattAttributes?????????????
	           
	            gattServiceData.add(currentServiceData);
	            
	            System.out.println("Service uuid:"+uuid);
	        	
	            ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
	                    new ArrayList<HashMap<String, String>>();
	            
	            //????????????????????????????
	            List<BluetoothGattCharacteristic> gattCharacteristics =
	                    gattService.getCharacteristics();
	            
	            ArrayList<BluetoothGattCharacteristic> charas =
	                    new ArrayList<BluetoothGattCharacteristic>();
	            
	         // Loops through available Characteristics.
	            //?????????????????????ÿ????????
	            for (final BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
	                charas.add(gattCharacteristic);
	                HashMap<String, String> currentCharaData = new HashMap<String, String>();
	                uuid = gattCharacteristic.getUuid().toString();
              
	               
	                if(gattCharacteristic.getUuid().toString().equals(HEART_RATE_MEASUREMENT)){                    
	                    //?????????Characteristic?????????mOnDataAvailable.onCharacteristicRead()  
	                   
	                      
	                    //????Characteristic???????,???????????????????mOnDataAvailable.onCharacteristicWrite()  
	                	BTClient.mBluetoothLeService.setCharacteristicNotification(gattCharacteristic, true);  
	                    target_chara=gattCharacteristic;
	                    //????????????  
	                    //????????????????  
	                    //mBluetoothLeService.writeCharacteristic(gattCharacteristic);  
	                }  
	                List<BluetoothGattDescriptor> descriptors= gattCharacteristic.getDescriptors();
	                for(BluetoothGattDescriptor descriptor:descriptors)
	                {
	                	System.out.println("---descriptor UUID:"+descriptor.getUuid());
	                	//??????????????
	                	BTClient.mBluetoothLeService.getCharacteristicDescriptor(descriptor); 
	                	//mBluetoothLeService.setCharacteristicNotification(gattCharacteristic, true);
	                }
	                
	                gattCharacteristicGroupData.add(currentCharaData);
	            }
	            //??????????????????????????????????
	            mGattCharacteristics.add(charas);
	            //???????????????????????????????
	            gattCharacteristicData.add(gattCharacteristicGroupData);
	            
	        }
     }
   
   /*???????????????????????????BluetoothDevice????????name MAC ???*/
	public  BluetoothAdapter.LeScanCallback mLeScanCallback =
	        new BluetoothAdapter.LeScanCallback() {
	    
		@Override
		public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
			// TODO Auto-generated method stub
				
			 if(!mBleArray.contains(device)) {
				 	mBleArray.add(device);
				 	//rssis.add(rssi);
	            }
			System.out.println("Address:"+device.getAddress());
			//System.out.println("Name:"+device.getName());
			//System.out.println("rssi:"+rssi);
			
		}
	};
		
   
}

package com.pauldemarco.flutter_blue;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothSocket;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

public class BTClient {
	 public static BluetoothLeService mBluetoothLeService=null;
	 public static byte[] RecvBuff = new byte[5000];
	 public static int RecvLength=0;
	 private static long time1=0;  
	 public static byte ComAddr;
	 public static long CmdTime=500;
	 public static String DeviceName="";
	 public static String tagid="";
	 public static boolean CmdIng=false;
	 public static String ActiveModeStr="";
	 public static String GetDevName()
	 {
		 return DeviceName;
	 }


	 public static String init_com(byte Baudrate,byte Parity)
	 {
		byte[] data=new byte[7];
		data[0] = 0;
		data[1] = 7;
		data[2] = 1;
		data[3] = Baudrate;
		data[4] = Parity;
		getCRC(data,5);
		return bytesToHexString(data,0,7) ;
	 }
	 
	 public static void SetDevName(String name)
	 {
		 DeviceName=name;
	 }
	 public static void settag_id(String name){
		 tagid=name;
	 }
	 
	 public static String gettag_id()
	 {
		 return tagid;
	 }

	 public static void getCRC(byte[] data,int Len)
	 {
		int i, j;
		int current_crc_value = 0xFFFF;
		for (i = 0; i <Len ; i++)
		{
		    current_crc_value = current_crc_value ^ (data[i] & 0xFF);
		    for (j = 0; j < 8; j++)
		    {
		        if ((current_crc_value & 0x01) != 0)
		            current_crc_value = (current_crc_value >> 1) ^ 0x8408;
		        else
		            current_crc_value = (current_crc_value >> 1);
		    }
		}
		data[i++] = (byte) (current_crc_value & 0xFF);
		data[i] = (byte) ((current_crc_value >> 8) & 0xFF); 
	 }
	 public static boolean CheckCRC(byte[] data,int len)
	 {
		 byte[]daw =new byte[256];
		 memcpy(data,0,daw,0,len);
		 getCRC(daw,len);
		 if(0==daw[len+1] && 0==daw[len])
		 {
			 return true;
		 }
		 else
		 {
			 return false;
		 }
	 }

	 public static void ArrayClear(byte[] Msg,int Size)
	 {
		 for(int i=0;i<Size;i++){
			 Msg[i]=0;
		 }
	 }
	 public static void memcpy(byte[] SourceByte,int StartBit_1,byte[] Targetbyte,int StartBit_2,int Length )
	 {
		 for(int m=0;m<Length;m++){
			 Targetbyte[StartBit_2+m]=SourceByte[StartBit_1+m];
		 }
	 }
	 public static void memcpy(byte[] SourceByte,byte[] Targetbyte,int Length )
	 {
		 for(int m=0;m<Length;m++){
			 Targetbyte[m]=SourceByte[+m];
		 }
	 }
	 public static String bytesToHexString(byte[] src, int offset, int length) {     
		    String stmp="";    
		    StringBuilder sb = new StringBuilder("");    
		    for (int n=0;n<length;n++)    
		    {    
		        stmp = Integer.toHexString(src[n+offset] & 0xFF);    
		        sb.append((stmp.length()==1)? "0"+stmp : stmp);     
		    }    
		    return sb.toString().toUpperCase().trim();  
	    }


	 public static byte[] hexStringToBytes(String hexString) {  
	        if (hexString == null || hexString.equals("")) {  
	            return null;  
	        }  
	        hexString = hexString.toUpperCase();  
	        int length = hexString.length() / 2;   
	        char[] hexChars = hexString.toCharArray();  
	        byte[] d = new byte[length];  
	        for (int i = 0; i < length; i++) {  
	            int pos = i * 2;  
	            d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));  
	        }  
	        return d;  
	    }   
	 private static byte charToByte(char c) {  
	        return (byte) "0123456789ABCDEF".indexOf(c);  
	    } 

	 public static int GetData()
	 {
	   time1= System.currentTimeMillis();
	   while((System.currentTimeMillis()-time1)<2000){
		   SystemClock.sleep(20);
		   int recvLen=MyService.RecvString.length()/2;
		   if(recvLen>0)
		   {
			   byte[] buffer =new byte[recvLen];
			   buffer=hexStringToBytes(MyService.RecvString);
			   memcpy(buffer,0,RecvBuff,0,recvLen);
			   RecvLength=recvLen;
			   int activelen=buffer[0]+1;
			   if(CheckCRC(RecvBuff,activelen))
			   {
				   Log.d("read data:", MyService.RecvString);
			       CmdIng=false;
				   return 0;
			   }
		   } 
	   }
	   CmdIng=false;
	   return -1;
	 }
	 
	 public static int GetReaderInfo(byte[]Version,byte[] Power,byte[] Fre)
	 {
		 byte[] Msg = new byte[6];
		 Msg[0] = 0x04;
		 Msg[1] = (byte)0xFF;
		 Msg[2] = 0x21;
		 Msg[3] = 0x19;
		 Msg[4] = (byte)0x95;
		 CmdTime=500;
		 MyService.target_chara.setValue(Msg); 
		 ArrayClear(RecvBuff,5000);
		 RecvLength=0;
		 MyService.RecvString="";
		 mBluetoothLeService.writeCharacteristic(MyService.target_chara);
		 if(GetData()==0){
        	ComAddr = RecvBuff[1];
        	Version[0] = RecvBuff[4];
        	Version[1] = RecvBuff[5];
        	Power[0] = RecvBuff[10];
        	Fre[0] = RecvBuff[8];
        	Fre[1] = RecvBuff[9];
      		return RecvBuff[3];
         }
		 return -1;
	 }
	 
	 public static int SetPower(byte Pwr)
	 {
		 byte[] Msg = new byte[6];
		 Msg[0] = 0x05;
		 Msg[1] = (byte)(ComAddr & 255);
		 Msg[2] = 0x2F;
		 Msg[3] = Pwr;
		 getCRC(Msg,4);
		 CmdTime=500;
		 MyService.target_chara.setValue(Msg); 
		 ArrayClear(RecvBuff,5000);
		 RecvLength=0;
		 MyService.RecvString="";
		 mBluetoothLeService.writeCharacteristic(MyService.target_chara);
		 if(GetData()==0){
        	ComAddr = RecvBuff[1];
      		return RecvBuff[3];
         }
		 return -1;
	 }
	 
	 public static int SetRegion(byte MaxFre,byte MinFre)
	 {
		 byte[] Msg = new byte[7];
		 Msg[0] = 0x06;
		 Msg[1] = (byte)(ComAddr & 255);
		 Msg[2] = 0x22;
		 Msg[3] = MaxFre;
		 Msg[4] = MinFre;
		 getCRC(Msg,5);
		 CmdTime=500;
		 MyService.target_chara.setValue(Msg); 
		 ArrayClear(RecvBuff,300);
		 RecvLength=0;
		 MyService.RecvString="";
		 mBluetoothLeService.writeCharacteristic(MyService.target_chara);
		 if(GetData()==0){
        	ComAddr = RecvBuff[1];
      		return RecvBuff[3];
         }
		 return -1;
	 }
	 
	 public static int SetBaudRate(byte BaudRate)
	 {
		 byte[] Msg = new byte[10];
		 Msg[0] = 0x05;
		 Msg[1] = (byte)(ComAddr & 255);
		 Msg[2] = 0x28;
		 Msg[3] = BaudRate;
		 getCRC(Msg,4);
		 CmdTime=500;
		 MyService.target_chara.setValue(Msg); 
		 ArrayClear(RecvBuff,300);
		 RecvLength=0;
		 MyService.RecvString="";
		 mBluetoothLeService.writeCharacteristic(MyService.target_chara);
		 if(GetData()==0){
        	ComAddr = RecvBuff[1];
      		return RecvBuff[3];
         }
		 return -1;
	 }

	 public static int GetInventoryData()
	 {
		   time1 = System.currentTimeMillis();

		   while ((System.currentTimeMillis() - time1) < 3000) {

		   	SystemClock.sleep(10);

		   	int recvLen = MyService.RecvString.length() / 2;

			   if (recvLen > 0) {
				   byte[] buffer = new byte[recvLen];
				   buffer = hexStringToBytes(MyService.RecvString);

				   memcpy(buffer,0, RecvBuff,0, recvLen);
				   RecvLength = recvLen;
				   byte[] Buff1 = new byte[5000];

				   memcpy(RecvBuff,0, Buff1,0, recvLen);
				   int nTurn = recvLen;

				   while(nTurn > 0) {

				   	byte[] Buff2 = new byte[5000];

					if (nTurn < (Buff1[0] & 255) + 1) {
						break;
					}

					if (((Buff1[3] != 0x03) && (Buff1[3] != 0x04)) && ((Buff1[0] & 255) + 1 == nTurn)) {
						CmdIng = false;
						return 0;
					}

					nTurn = nTurn - (Buff1[0] & 255) - 1;
					memcpy(Buff1,(Buff1[0]&255)+1,Buff2,0,nTurn);
					ArrayClear(Buff1,5000);
					memcpy(Buff2,0,Buff1,0,nTurn);

				   }
		       }
		   }

		   CmdIng = false;

		   return -1;
	 }

	 public static int Inventory_G2(byte QValue,byte Session, byte AdrTID, byte LenTID, byte TIDFlag,int[] CardNum,byte[] EPCList,int[] EPCLength)
	 {
	 	byte[] Msg=new byte[10];
	 	Msg[1]=(byte)(ComAddr & 255);
		Msg[2]=1;
		Msg[3]=QValue;
		Msg[4]=Session;

		if (TIDFlag==0) {
			Msg[0]=6;
			getCRC(Msg,5);
		} else {
			Msg[0]=8;
			Msg[5]=AdrTID;
			Msg[6]=LenTID;
			getCRC(Msg,7);
		 }

		MyService.target_chara.setValue(Msg);
		ArrayClear(RecvBuff,300);
		RecvLength=0;
		MyService.RecvString="";
		mBluetoothLeService.writeCharacteristic(MyService.target_chara);
		Log.d("Write data:", "AAAAAAAAAAAAAAAAA");

		if (GetInventoryData() == 0) {

			byte[] szBuff = new byte[3000];
			byte[] szBuff1 = new byte[3000];

			memcpy(RecvBuff,0,szBuff,0,RecvLength);

			int Nlen = 0;

			while (RecvLength > 0) {

				int nLenszBuff = (szBuff[0] & 255) + 1;

				if ((szBuff[3] == 0x01) || (szBuff[3] == 0x02) || (szBuff[3] == 0x03) || (szBuff[3] == 0x04)) {
					//*Ant=szBuff[4];
					CardNum[0] += (szBuff[5] & 255);
					memcpy(szBuff,6, szBuff1, Nlen,(szBuff[0] & 255) - 7);
					Nlen += ((szBuff[0] & 255) - 7);

					if ((RecvLength - (szBuff[0] & 255) - 1) > 0) {
						byte[] daw = new byte[3000];
						memcpy(szBuff,(szBuff[0] & 255) + 1, daw,0,RecvLength - (szBuff[0] * 255) - 1);
						ArrayClear(szBuff,3000);
						memcpy(daw,0, szBuff,0,RecvLength - (szBuff[0] * 255) - 1);
					}
				}

				RecvLength = RecvLength - nLenszBuff;
			}

			memcpy(szBuff1,0, EPCList,0, Nlen);
			EPCLength[0] = Nlen;

			return szBuff[3];
		}

		return 0x30;
	}
	 
	 public static int ReadData_G2(byte Enum,byte[] EPC,byte Mem,byte WordAddr,byte Num,byte[]Psd,byte[] Data)
	 {
		 byte[] Msg=new byte[200];
		 Msg[0]=(byte)(12+Enum*2);
		 Msg[1]=(byte)(ComAddr & 255);
		 Msg[2]=0x02;
		 Msg[3]=Enum;
		 for(int i=0;i<Enum*2;i++)
		 {
			 Msg[4+i]=EPC[i];
		 }
		 Msg[4+Enum*2]=Mem;
		 Msg[5+Enum*2]=WordAddr;
		 Msg[6+Enum*2]=Num;
		 Msg[7+Enum*2]=Psd[0];
		 Msg[8+Enum*2]=Psd[1];
		 Msg[9+Enum*2]=Psd[2];
		 Msg[10+Enum*2]=Psd[3];
		 getCRC(Msg,11+Enum*2);
		 CmdTime=500;
		 int Len = 13+Enum*2;
		 boolean SendFlag=true;
		 while(SendFlag)
		 {
			 if((Len - 20)>0)
			 {
				 byte[]data = new byte[20];
				 memcpy(Msg,0,data,0,20);
				 byte[]daw = new byte[200];
				 memcpy(Msg,20,daw,0,Len- 20);
				 memcpy(daw,0,Msg,0,Len -20);
				 Len = Len -20;
				 MyService.target_chara.setValue(data); 
				 mBluetoothLeService.writeCharacteristic(MyService.target_chara);
				 Log.d("write:", bytesToHexString(data,0,20));
			 }
			 else
			 {
				 byte[]data = new byte[Len];
				 memcpy(Msg,0,data,0,Len);
				 MyService.target_chara.setValue(data); 
				 mBluetoothLeService.writeCharacteristic(MyService.target_chara);
				 Log.d("write:", bytesToHexString(data,0,Len));
				 SendFlag =false;
			 }
		 }
		 ArrayClear(RecvBuff,300);
		 RecvLength=0;
		 MyService.RecvString="";
		 if(GetData()==0)
         {
       	    ComAddr = RecvBuff[1];
       	    if(RecvBuff[3]==0)
       	    {
       	    	memcpy(RecvBuff,4,Data,0,RecvLength-6);
       	    	return 0;
       	    }
      	    return -1;
         }
		 return -1;
	 }
	 public static int WriteData_G2(byte Enum,byte[] EPC,byte Mem,byte WordAddr,byte WNum,byte[]Psd,byte[] Data)
	 {
		 byte[] Msg=new byte[200];
		 Msg[0]=(byte)(12+Enum*2+WNum*2);
		 Msg[1]=(byte)(ComAddr & 255);
		 Msg[2]=0x03;
		 Msg[3]=WNum;
		 Msg[4]=Enum;
		 for(int i=0;i<Enum*2;i++)
		 {
			 Msg[5+i]=EPC[i];
		 }
		 Msg[5+Enum*2]=Mem;
		 Msg[6+Enum*2]=WordAddr;
		 for(int i=0;i<WNum*2;i++)
		 {
			 Msg[7+Enum*2+i]=Data[i];
		 }
		 Msg[7+Enum*2+WNum*2]=Psd[0];
		 Msg[8+Enum*2+WNum*2]=Psd[1];
		 Msg[9+Enum*2+WNum*2]=Psd[2];
		 Msg[10+Enum*2+WNum*2]=Psd[3];
		 getCRC(Msg,11+Enum*2+WNum*2);
		 CmdTime=500;
		 int Len = 13+Enum*2+WNum*2;
		 boolean SendFlag=true;
		 while(SendFlag)
		 {
			 if((Len - 20)>0)
			 {
				 byte[]data = new byte[20];
				 memcpy(Msg,0,data,0,20);
				 byte[]daw = new byte[200];
				 memcpy(Msg,20,daw,0,Len- 20);
				 memcpy(daw,0,Msg,0,Len -18);
				 Len = Len -20;
				 MyService.target_chara.setValue(data); 
				 Log.d("write:", bytesToHexString(data,0,20));
				 mBluetoothLeService.writeCharacteristic(MyService.target_chara);
			 }
			 else
			 {
				 byte[]data = new byte[Len];
				 memcpy(Msg,0,data,0,Len);
				 MyService.target_chara.setValue(data); 
				 Log.d("write:", bytesToHexString(data,0,Len));
				 mBluetoothLeService.writeCharacteristic(MyService.target_chara);
				 SendFlag =false;
			 }
		 }
		 ArrayClear(RecvBuff,300);
		 RecvLength=0;
		 MyService.RecvString="";
		 if(GetData()==0)
         {
       	    ComAddr = RecvBuff[1];
       	    if(RecvBuff[3]==0)
       	    {
       	    	return 0;
       	    }
      	    return -1;
         }
		 return -1;
	 }
}

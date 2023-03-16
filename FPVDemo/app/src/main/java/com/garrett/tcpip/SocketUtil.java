package com.garrett.tcpip;

import android.annotation.SuppressLint;

import com.dji.FPVDemo.MainActivity;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Date;

/*
 * filename:SocketUtil.java
 * author:Garrett
 * comment:
 */

/*
 * @author martin
 * 
 */
public class SocketUtil {
	final static int START_FLAG = 0x11;
	/*
	 * write string 2 a outputstream
	 * 
	 * @param str
	 *            to write string
	 * @param in
	 *            stream
	 * @throws IOException 
	 */
	public static void writeStr2Stream(String str, OutputStream out) throws IOException {
		try {
			//System.out.println(SocketUtil.getNowTime() + ": prepared to write [" + str + "].");
			// add buffered writer
			BufferedOutputStream writer = new BufferedOutputStream(out);
			writer.write(START_FLAG);
			writer.write(str.getBytes("utf-8").length);
			writer.write(str.getBytes("utf-8").length>>8);
			// write
			writer.write(str.getBytes("utf-8"));
			
			writer.flush();
		} catch (IOException ex) {
			//System.out.println(SocketUtil.getNowTime() + ex);
			throw ex;
		}
	}

	public static void wrightBytes2Stream(byte[] data, OutputStream out) throws Exception {
		try {
			// add buffered writer
			BufferedOutputStream writer = new BufferedOutputStream(out);
			writer.write(START_FLAG);
//			给数据加个数据头，内容为该段数据的长度
			writer.write(data.length);
			writer.write(data.length>>8);
			// write
			writer.write(data);

			writer.flush();
		} catch (Exception ex) {
			//System.out.println(SocketUtil.getNowTime() + ex);
			throw ex;
		}
	}
	
	public static byte[] readXBytes2ByteArray(InputStream in, int x) throws IOException, InterruptedException{
		ByteArrayOutputStream outStream = new ByteArrayOutputStream();  
		byte[] data = new byte[2048];  
		int len = 0;
		System.out.println("开始取数据");
		int dataLength = x;
		try {
			int dataLeft = dataLength;
			Date start = new Date();
			for(; (outStream.size() < dataLength); ){
				//System.out.println(outStream.size() + "/"+dataLength);
				while ((len = in.read(data, 0, dataLeft > 2048?2048:dataLeft)) != -1) {
					dataLeft -= len;
					// if the length of array is 1M
					if (2048 == len) {
						//then append all chars of the array
						outStream.write(data,0,len);
						
					} 
					// if the length of array is less then 1M
					else {
						//then append the valid chars
						for (int i = 0; i < len; i++) {
							outStream.write(data[i]);
						}
						if(len == 0) break;
					}
				}
				
				//当读取时间超过3秒，看做读取失败
				if ((new Date().getTime() - start.getTime()) > 3000 ){
					break;
				}
				//Thread.sleep(100);
			}
			if (dataLeft == 0){
				System.out.println("读取成功");
			}
			else{
				System.out.println("读取失败");
				return null;

			}

		} catch (IOException e) {
			//System.out.println(e);
			throw e;
		}
		data = null;
		System.out.println("end reading string from stream");
		byte[] byteOut = outStream.toByteArray();
		return byteOut;
	}
	
	public static int[] readXBytes(InputStream in, int x) throws IOException, InterruptedException{
		byte[] byteOut = readXBytes2ByteArray( in,  x);
		int[] intOut = new int[byteOut.length];
		for(int i = 0; i < byteOut.length; i++)
		{
			intOut[i] = byteOut[i] & 0x0FF;
		}
		return intOut;
	}
	
	public static int[] readIntFromStream(InputStream in) throws IOException, InterruptedException {
		
		byte[] byteOut = readByteFromStream(in);
		//System.out.println("end reading string from stream");
		int[] intOut = new int[byteOut.length];
		for(int i = 0; i < byteOut.length; i++)
		{
			intOut[i] = byteOut[i] & 0x0FF;
		}
		return intOut;
	}
	
	public static byte[] readByteFromStream(InputStream in) throws InterruptedException, IOException  {
		byte[] data = null;
		//System.out.println("开始取数据");
		int dataLength= 0;
		try {
			byte[] head = null;
			head = readXBytes2ByteArray(in, 3);
			if (head[0] == START_FLAG){
				//System.out.println(start[1] + "+" + (start[2]<<8) );
				dataLength = (head[1]&(int)0xff) + ((head[2]<<8)&(int)0xffff);
			}
			else{
				//System.out.println("文件头出错: "+ head[0] + ", " + head[1] + ", " + head[2]);
			}
			
			data = readXBytes2ByteArray(in, dataLength);

		} catch (IOException e) {
			//System.out.println(e);
			throw e;
		}
		if (dataLength < 20){
			ByteArrayOutputStream allOut = null;
			//System.out.println(new String(data));
			if ((new String(data)).contains("big data")){
				//System.out.println("数据量大于0xffff");
				int[] packageNum = readIntFromStream(in);
				//System.out.println("数据包个数为：" + packageNum[0]);
				
				allOut = new ByteArrayOutputStream();
				for (int i = 0; i < packageNum[0]; i++){
					byte[] receive = readByteFromStream(in);
					allOut.write(receive);
				}
				data = allOut.toByteArray();
			}
		}
		return data;
	}
	/*
	 * read string from a inputstream
	 * 
	 * @param in
	 * @return
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	public static String readStrFromStream(InputStream in) throws IOException, InterruptedException {
		//System.out.println(getNowTime() + " : start to read string from stream");
		byte[] data = readByteFromStream(in);
		return new String(data, "utf-8");
	}
	
	public static String getNowTime()
	{
		return new Date().toString();
	}

	
	public static void main(String[] args){
		ServerSocket s;

		try {
			s = new ServerSocket(30002);
			Socket socketF = s.accept();
			OutputStream os = socketF.getOutputStream();              
			InputStream inF = socketF.getInputStream();
			while(true){
				
				int bsF[];
	            bsF = SocketUtil.readIntFromStream(inF);
//	            bsF = SocketUtil.readXBytes(inF, 614400);
	            int l = bsF.length;
	            if(l != 614400){
	                l++;      	
	            }
			}
		} catch (IOException  e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}

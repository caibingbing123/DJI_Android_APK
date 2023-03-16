package com.garrett.tcpip;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

//import com.a.a.d.e;
import com.dji.FPVDemo.MainActivity;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/*
 * filename:RequestThread.java
 * author:martin
 * comment:a thread sending request to socket server. 
 */

/*
 * @author Garrett
 * 
 */
public class RequestThread implements Runnable {
	// id
	private String protocol = "heartbreak Protocol";
	HeartBreakThread heartBreakThread;
	private int heartBreakTimeout = 0;
	private String ip = "127.0.0.1";	//默认ip
	private Handler h;					//备用，当数据传输的过程需要在activity中体现时可以使用它来传输message
	public Socket heartBreakerRequest;	//提供给长连接用的socket
	public BlockingQueue<byte[]> queue = new  LinkedBlockingQueue<byte[]>(70000);//资源的队列，用作要传输的字节流的缓冲区
	public BlockingQueue<Integer> bytenum = new  LinkedBlockingQueue<Integer>(10000);//资源的队列，用作要传输的字节流的缓冲区

	/*
	 * keep connection online or not. default value : false.
	 */
	private boolean isLongConnection = false;//当和服务器端进行确认可以建立长连接时将被置为真
	private int heartPort = 30001;
	private int requstPort = 30000;
	public MainActivity activity = null;
	public int heartThreadPriory = Thread.NORM_PRIORITY;
	public boolean isThreadRunning = false;//当本线程正在运行时被置为真，以防止socket被重复建立
	public int sleepTime = 5;
	private RequestThread(){
//		this.h = ClientActivity.getTCPHandler();
	}
	
	public RequestThread(String ip) {
		this();
		this.ip = ip;
	}
	
	public RequestThread(String ip, int heartBreakTimeout) {
		this();
		this.ip = ip;
		this.heartBreakTimeout = heartBreakTimeout;
	}
	
	public RequestThread( String ip, int heartBreakTimeout, int requstPort, int heartPort) {
		this();
		this.isLongConnection = false;
		this.ip = ip;
		this.heartBreakTimeout = heartBreakTimeout;
		this.heartPort  = heartPort;
		this.requstPort  = requstPort;
	}
	
	@Override
	public void run() {
		isThreadRunning = true;
		Socket request = null;
		try {
			
			// connect to socket server
			//socket1：用来与服务器端确认建立长连接以及接受服务器断开长连接的命令
			request = new Socket(ip, this.requstPort);
			request.setSoTimeout(this.heartBreakTimeout);
			this.activity.setResultToTv("--请求开启端口："+ this.requstPort);
			//Thread.sleep(1000);
			SocketUtil.writeStr2Stream("请求长连接", request.getOutputStream());	
			String reqStr = "";
			while(reqStr.isEmpty())
				reqStr = SocketUtil.readStrFromStream(request.getInputStream());
			if(reqStr.equals("允许开始长连接")){
				try {
					heartBreakerRequest =  new Socket(ip, this.heartPort);
					heartBreakerRequest.setSoTimeout(this.heartBreakTimeout);
					this.activity.setResultToTv("----请求开启端口："+ this.heartPort);
					//heartBreakerRequest.setKeepAlive(true);   //setKeepAlive相当于是系统提供的一个大间隔（2个小时）的心跳 
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				//建立长连接
				heartBreakThread = new HeartBreakThread(this.heartBreakerRequest, queue);
				heartBreakThread.bytenum = bytenum;
				heartBreakThread.sleepTime = this.sleepTime ;
				Thread t = new Thread(heartBreakThread );
				t.setPriority(this.heartThreadPriory);
				t.start();
				heartBreakThread.activity = this.activity;
				
				//Thread.sleep(500);
				isLongConnection = true;
				this.activity.setResultToTv("建立长连接："+ip);
			}
			else{
				isLongConnection = false;
				this.activity.setResultToTv("--------建立长连接失败!!!!");
			}
			
			//如果允许进行长连接，就在本线程内持续检测从服务器送来的长连接终止指令
			if (isLongConnection)
			{
				try{
					while (heartBreakThread.state == false){
						SocketUtil.writeStr2Stream(protocol, request
								.getOutputStream());
						Thread.sleep(3000);
					}
				}
				catch (Exception e){
					this.activity.setResultToTv(e.getMessage());
					this.activity.setResultToTv(e.toString());
				}
				finally{
					this.heartBreakThread.setKeepAlive(false);
					while(!this.heartBreakThread.state);
				}
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		} 
		catch (Throwable e) {
			e.printStackTrace();
		} finally {
			if (request != null) {
				try {
					//结束长连接，关闭所有socket
					request.close();
					if (heartBreakerRequest != null) heartBreakerRequest.close();
					isLongConnection = false;
					if (heartBreakThread != null) heartBreakThread.setKeepAlive(false);
					queue.clear();
					this.activity.setResultToTv("关闭连接，清除队列!!!!!");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		isThreadRunning = false;
	}

	public boolean isLongConnection() {
		return isLongConnection;
	}
	
	public void setHeartThreadDead(){
		if (heartBreakThread != null)
		this.heartBreakThread.setKeepAlive(false);
	}
	
	public boolean send2Master(byte[] data, int len){
//		byte[] dataSend = null;
//		try{
//			dataSend = new byte[len];		//这里产生的数组在socket发送的过程中如果没有被接收，
//			就会一直存在与内存之中，造成内存溢出，所以要检测exception
//		}
//		catch (OutOfMemoryError e){
//			this.activity.setResultToTv(e.getMessage());
//			this.activity.setResultToTv(e.toString());
//			this.heartBreakThread.setKeepAlive(false);
//			while(!this.heartBreakThread.state);
//			return false;
//		}
//    	System.arraycopy(data, 0, dataSend, 0, len);
		if (isLongConnection == true){
//			if (queue.remainingCapacity() < 5000){
//				this.activity.setResultToTv("队列将要满，清空队列");
//				queue.clear();
//			}
			//当长连接仍在维持时，把需要送过去的数据放到队列里，等待长连接发送。否则为了防止队列内存溢出，清除队列。
//			if((!this.heartBreakerRequest.isClosed()) ){
				try {
					bytenum.put(len);
					queue.put(data);		//如果队列已满就等待
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
//			}
//			else{
//				this.activity.setResultToTv("heart连接关闭，清空队列");
//				queue.clear();
//			}

			return true;
		}
		else{
			return false;
		}
	}

	public void setLongConnection(boolean isLongConnection) {
		this.isLongConnection = isLongConnection;
	}
	
	public void receiveFromMaster(){
		if (isLongConnection == true){
			
		}
	}
}

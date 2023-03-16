package com.garrett.tcpip;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.dji.FPVDemo.MainActivity;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

/*
 * @filename HeartBreakThread.java
 * @comment heartbreak testing thread, used to keep socket connect alive.
 * @author Martin
 * 
 */
public class HeartBreakThread implements Runnable {
	/*
	 * client socket
	 */
	public int sleepTime = 5;
	
	public boolean state =  false;	//state指示了长连接的状态，当为假时说明长连接仍存活
	private Socket request;					
	public MainActivity activity = null;
	/**
	 * a flag used to identify whether to keep connect alive.
	 */
	private boolean isKeepAlive = true;//从外部关闭长连接的控制为，在线程外部把它置为假时，长连接在发送完最后一帧数据后将退出。
	
	int dataInBuffer = 0;
	
	private BlockingQueue<byte[]> queue;	//数据队列
	public BlockingQueue<Integer> bytenum;//资源的队列，用作要传输的字节流的缓冲区

	
	//????????????java中的赋值是怎样传送数据的？是赋予指针还是直接复制一个新的？为什么这里的queue在赋值后，右端表达式改变后左端表达式亦随之改变？
	//答案：java中对象的赋值，如A=B,实际上就是把B对象的对象引用赋给A，而不是把B所引用的整个对象复制给A，
	//		具体可以类比C语言的两个指针的赋值，赋值后，当B引用的对象发生变化时，A也会变化！！
	//final关键字的作用是防止方法内部对变量的改变对外部的变量造成的不应当的变化
	public HeartBreakThread(Socket request,final BlockingQueue<byte[]> queue) {
		this.request = request;
		this.queue = queue;
		state = false;
		isKeepAlive = true;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		
			try {
				while (isKeepAlive) {
						byte[] data = queue.poll();
						if(data == null) {
							Thread.sleep(sleepTime);		//为低优先级的线程腾出时间
							continue;
						}
						//if(data.length<4000){		//如果数据太少，就等到存到1000再发送
							ByteArrayOutputStream data2Send = new ByteArrayOutputStream();
							data2Send.write(data, 0, bytenum.poll());
//							data2Send.write(data);
							while(data2Send.size()<4000){
								byte[] temp = queue.poll();
								if(temp!= null) {
									data2Send.write(temp, 0, bytenum.poll());
								}
								else{
									Thread.sleep(sleepTime);		
								}
							}
							data = data2Send.toByteArray();
						//}
						if (queue.size() > 1000){
							this.activity.setResultToTv("队列中剩下的数组数："+queue.size());
						}
						SocketUtil.wrightBytes2Stream(data, request.getOutputStream());
				}
			} catch (IOException e) {
				this.activity.setResultToTv(e.getMessage());
				this.activity.setResultToTv(e.toString());
				e.printStackTrace();
			} catch (Exception e) {
				this.activity.setResultToTv(e.getMessage());
				this.activity.setResultToTv(e.toString());
				e.printStackTrace();
			}finally{
				try {
					request.getOutputStream().close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				this.isKeepAlive = false;
				state = true;
				this.activity.showToast("*****结束*****");
			}
	}

	public Socket getRequest() {
		return request;
	}

	public void setRequest(Socket request) {
		this.request = request;
	}

	public boolean isKeepAlive() {
		return isKeepAlive;
	}

	public void setKeepAlive(boolean isKeepAlive) {
		this.isKeepAlive = isKeepAlive;
	}

}

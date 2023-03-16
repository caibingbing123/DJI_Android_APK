package com.garrett.util.timer;

import java.text.SimpleDateFormat;
import java.util.Date;

public class MyClock{
	public MyClock(){
		
	}
	
	static public String getSecond(){
		SimpleDateFormat df = new SimpleDateFormat("ss");//设置日期格式
		return df.format(new Date());// new Date()为获取当前系统时间
	}
	
	static public String getMinutes(){
		SimpleDateFormat df = new SimpleDateFormat("mm");//设置日期格式
		return df.format(new Date());// new Date()为获取当前系统时间
	}
	
	static public String getClock(){
		SimpleDateFormat df = new SimpleDateFormat("mm:ss:SS");//设置日期格式
		return df.format(new Date());// new Date()为获取当前系统时间
	}
	
}

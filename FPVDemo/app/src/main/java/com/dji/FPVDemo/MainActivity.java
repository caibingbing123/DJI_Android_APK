package com.dji.FPVDemo;

import static com.dji.FPVDemo.FPVDemoApplication.isGimbalModuleAvailable;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.media.MediaFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.TextureView.SurfaceTextureListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;

import com.garrett.tcpip.RequestThread;
import com.garrett.tcpip.SocketUtil;
import com.garrett.ui.RectView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

import dji.common.camera.SettingsDefinitions;
import dji.common.camera.SystemState;
import dji.common.error.DJIError;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.flightcontroller.virtualstick.FlightControlData;
import dji.common.flightcontroller.virtualstick.FlightCoordinateSystem;
import dji.common.flightcontroller.virtualstick.RollPitchControlMode;
import dji.common.flightcontroller.virtualstick.VerticalControlMode;
import dji.common.flightcontroller.virtualstick.YawControlMode;
import dji.common.gimbal.Rotation;
import dji.common.gimbal.RotationMode;
import dji.common.product.Model;
import dji.common.useraccount.UserAccountState;
import dji.common.util.CommonCallbacks;
import dji.midware.usb.P3.UsbAccessoryService;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.payload.Payload;
import dji.sdk.products.Aircraft;
import dji.sdk.useraccount.UserAccountManager;

public class MainActivity extends Activity implements  OnClickListener {

    private static final String TAG = MainActivity.class.getName();
    protected VideoFeeder.VideoDataListener mReceivedVideoDataListener = null;
    File file = new File("/sdcard/fortest" + ".h264");
    FileOutputStream fops;
    InputStream fis = null;
    ByteArrayOutputStream toDisk = new ByteArrayOutputStream();
    private final static int MSG_INIT_DECODER = 1;
    protected static final int MSG_DSP = 2;
    protected static final int SHOWTOAST = 3;
    // Codec for video live view
    protected DJICodecManager mCodecManager = null;

    protected TextureView mVideoSurface = null;
    private Button mSprinRightBtn;
    private ToggleButton mTransmitImageBtn;
    private ToggleButton mBtnVirtualStick;
    private ToggleButton mOnekeyFlyBtn;
    //画框
    private RectView rv = null;
    private int x, y, w, h;
    //IP通信
    private ImageView mAircraftImageView;
    private ImageView mPCImageView;
    private ImageView mPCControlView;
    private boolean aircraftLinkState = false;
    private boolean pcImageLinkState = false;
    private boolean pcControlLinkState = false;
    private boolean isTransmitting = false;
    boolean isFlying = false;
    private String ip = "192.168.0.108";
    //private String ip = "192.168.181.104";
    RequestThread rt;
    RequestThread rtControl;
    byte[] data;
    int di = 0;
    int bytenum = 0;
    int receiveTime = 0;
    private Timer mTimer;
    //云台
    private float mGimbalPitch;
    //飞行控制
    private FlightController mFlightController;
    private float mPitch;
    private float mRoll;
    private float mYaw;
    private float mThrottle;

    //提示
    TextView tv;

    class ControlTimerTask implements Runnable {
        //==============================================================================
        //指令格式1	    {0, 1, 3, 1/2/3/4, 0/1, -10,00~10,00, 0/1/2, -2400~2400}
//			        	 │	│  │    │       │        │        │        └─只有在控制云台时有用，范围为-2400到2400，占据两个字节。
//			        	 │	│  │    │       │        │        │
//			     		 │	│  │    │       │        │        └──────────只有在垂直移动时有效
//			  		     │	│  │ 	│   	│ 	     │				       0：垂直静止
//			     		 │	│  │	│	    │		 │				       1：垂直上升
//			     		 │	│  │	│	    │		 │				       2：垂直下降
//			        	 │  │  │    │       │        └───────────────────只有在前后移动/左右移动/水平旋转时有用，代表了移动的速度
//			        	 │  │  │    │       │							   因为是个大于255的整数，在这里要占据两个字节。
//			     		 │  │  │    │       └────────────────────────────第六七八个数的正负号
//			        	 │  │  │    └────────────────────────────────────指示了动作的类型
//			        	 │  │  │                                           1：垂直移动
//			        	 │  │  │                                           2：前后移动
//			        	 │  │  │                                           3：左右移动
//			        	 │  │  │                                           4：水平旋转
//						 │  │  │                                           5：云台上下转动
//			             └──┴──┴─────────────────────────────────────────帧头标志
         //指令格式2     {}
        //============================================================================
        @Override
        public void run() {
            isFlying = true;
            while (isFlying) {
                if (isFlying) {
                    //setResultToToast("checkpoint 1");
                    byte[] data2 = null;
                    if (rtControl.isLongConnection() == true) {
                        //setResultToToast("checkpoint 2");
                        data2 = null;
                        try {
                            //rtControl.heartBreakerRequest.getInputStream().read(data2, 0, 3);
                            data2 = SocketUtil.readByteFromStream(rtControl.heartBreakerRequest.getInputStream());
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            //setResultToTv("读取InterruptedException"+e);
                        } catch (IOException e) {
                            e.printStackTrace();
                            //setResultToTv("读取InterruptedException"+e);
                        }
                        if (data2 != null) {
                            if (data2.length < 3) {
                                continue;
                            }
                            if ((data2[0] == 0) && (data2[1] == 1) && (data2[2] == 3)) {
                                switch (data2[3]) {
                                    case 1:
                                        switch (data2[7]){
                                            //下上运动速度-4~4m/s
                                            case 0:
//                                                mPitch = (float)(0);
//                                                mRoll = (float)(0);
//                                                mYaw = (float) (0);
                                                mThrottle = (float)(0);
                                                showToast("悬停:");
                                                break;
                                            case 1:
//                                                mPitch = (float)(0);
//                                                mRoll = (float)(0);
//                                                mYaw = (float) (0);
                                                mThrottle = (float)(1);
                                                showToast("上升:"+mThrottle);
                                                break;
                                            case 2:
//                                                mPitch = (float)(0);
//                                                mRoll = (float)(0);
//                                                mYaw = (float) (0);
                                                mThrottle = (float)(-1);
                                                showToast("下降:"+mThrottle);
                                                break;
                                            default: break;
                                        }
                                        break;
                                    case 2:
                                        //后前运动速度-15~15m/s
//                                        mPitch = (float)(0);
                                        mRoll = (float) ((float)(data2[4]*((data2[5] & (int)0xff) + ((data2[6] << 8)&(int)0xffff)))*(5.0/1000.0));
//                                        mYaw = (float) (0);
//                                        mThrottle = (float)(0);
                                        if(mRoll>0){
                                            setResultToTv("向前:"+mRoll);
                                        }else if (mRoll<0){
                                            setResultToTv("向后:"+mRoll);
                                        }
                                        break;
                                    case 3:
                                        //右左运动速度-15~15m/s
                                        mPitch = (float) ((float)(data2[4]*((data2[5] & (int)0xff) + ((data2[6] << 8)&(int)0xffff)))*(5.0/1000.0));
//                                        mRoll = (float)(0);
//                                        mYaw = (float) (0);
//                                        mThrottle = (float)(0);
                                        if(mPitch>0){
                                            setResultToTv("向左:"+mPitch);
                                        }else if (mPitch<0){
                                            setResultToTv("向右:"+mPitch);
                                        }
                                        break;
                                    case 4:
                                        //水平转动角度-180~180或角速度-100~100（采用角速度）
//                                        mPitch = (float)(0);
//                                        mRoll = (float)(0);
                                        mYaw = (float) ((float)((data2[4]*((data2[5] & (int)0xff) + ((data2[6] << 8)&(int) 0xffff))))*(65.0/1000.0));
//                                        mThrottle = (float)(0);
                                        if(mYaw>0){
                                            setResultToTv("右旋:"+mYaw);
                                        }else if (mYaw<0){
                                            setResultToTv("左旋:"+mYaw);
                                        }
                                        break;
                                    case 5:
                                        float MG = (float) ((float)((-1)*(data2[4]*((data2[8] & (int)0xff) + ((data2[9] << 8) & (int) 0xffff))))*(7.0/2400.0));
                                        if ((MG<0.15)&(MG>-0.15)){
                                            mGimbalPitch=(float)(0);
                                            //stopGimble();
                                            setResultToTv("云台STOP");
                                        }else if (MG<-0.15){
                                            mGimbalPitch=(float)(-6);
                                            setResultToTv("云台DOWN，速度：" + mGimbalPitch);
                                        }else if (MG>0.15){
                                            mGimbalPitch=(float)(6);
                                            setResultToTv("云台UP，速度：" + mGimbalPitch);
                                        }
                                        break;
                                    default:
                                        break;
                                }
                            }
                        } else {
                            try {
                                Thread.sleep(40);
                            } catch (InterruptedException e) {
                                // TODO Auto-generated catch block
                                //e.printStackTrace();
                            }
                        }
                    } else {
                        try {
                            Thread.sleep(40);
                        } catch (InterruptedException e) {
                            // TODO Auto-generated catch block
                            //e.printStackTrace();
                        }
                        continue;
                    }

                } else {
                    return;
                }
            }
            //setResultToTv("	结束定时器@"+MyClock.getClock());
            return;
        }
    }

    private Handler mHandler =new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            switch (msg.what)
            {
                case MSG_INIT_DECODER:
                    Surface mSurface = (Surface)msg.obj;
                    initDecoder(mSurface);
                    break;
                case MSG_DSP:
                    mImageView.setImageBitmap((Bitmap) msg.obj);
                    mImageView.invalidate();
                    break;
                default:
                    break;
            }
            return false;
        }
    });
    public void writeTerminal(TextView _debugTextView, String data, int MAX_LINE) {
        _debugTextView.append(data);
        // Erase excessive lines
        int excessLineNumber = _debugTextView.getLineCount() - MAX_LINE;
        if (excessLineNumber > 0) {
            int eolIndex = -1;
            CharSequence charSequence = _debugTextView.getText();
            for (int i = 0; i < excessLineNumber; i++) {
                do {
                    eolIndex++;
                } while (eolIndex < charSequence.length() && charSequence.charAt(eolIndex) != '\n');
            }
            if (eolIndex < charSequence.length()) {
                _debugTextView.getEditableText().delete(0, eolIndex + 1);
            } else {
                _debugTextView.setText("");
            }
        }
    }

    public void setResultToTv(final String result) {
        MainActivity.this.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                // TODO Auto-generated method stub
                writeTerminal(tv, result + "\n", 10);
            }
        });
    }

    public void showToast(final String msg) {
        MainActivity.this.runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private ImageView mImageView;
    /*
        @Override
        public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
            Log.e(TAG, "onSurfaceTextureAvailable");
            if (mCodecManager == null) {
                mCodecManager = new DJICodecManager(this, surface, width, height);
            }
            setResultToTv("width"+width);
            setResultToTv("height"+height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {
            Log.e(TAG, "onSurfaceTextureSizeChanged");
        }

        @Override
        public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
            Log.e(TAG,"onSurfaceTextureDestroyed");
            if (mCodecManager != null) {
                mCodecManager.cleanSurface();
                mCodecManager = null;
            }
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {

        }
    */
    class countingTask extends TimerTask {
        @Override
        public void run() {
            // TODO Auto-generated method stub
            synchronized (MainActivity.this) {
                setResultToTv("字节数为：" + bytenum);
                setResultToTv("接收次数为：" + receiveTime);
                receiveTime = 0;
                bytenum = 0;
            }
        }
    }

    class Task extends TimerTask {
        //int times = 1;
        @Override
        public void run() {
            checkConnectState();
            if (rt != null) {
                if (!pcImageLinkState) {
                    if (rt.isThreadRunning == false) {
                        new Thread(rt).start();
                    }
                }
            } else {
                if (!rt.isThreadRunning) {
                    isTransmitting = false;
                    uninitPreviewer();
                }
            }
            if (rtControl != null) {
                if (!pcControlLinkState) {
                    if (rtControl.isThreadRunning == false) {
                        new Thread(rtControl).start();
                    }
                }
            }

        }

    }

    private void updateIconState() {

        //与飞行器的连接
        BaseProduct product = FPVDemoApplication.getProductInstance();
        Camera camera = FPVDemoApplication.getCameraInstance();
        if (product != null && product.isConnected()) {
            if (camera != null) {
                if (!aircraftLinkState) {
                    aircraftLinkState = true;
                    mAircraftImageView.setImageResource(R.drawable.aircraft);
                    mAircraftImageView.postInvalidate();
                }
            } else {
                if (aircraftLinkState) {
                    aircraftLinkState = false;
                    setResultToTv("飞行器连接--NO--");
                    mAircraftImageView.setImageResource(R.drawable.aircraft_unconnected);
                    mAircraftImageView.postInvalidate();
                }
            }
        } else {
            if (aircraftLinkState) {
                aircraftLinkState = false;
                setResultToTv("飞行器连接--NO--");
                mAircraftImageView.setImageResource(R.drawable.aircraft_unconnected);
                mAircraftImageView.postInvalidate();
            }
        }
        //当发现与电脑的图传连接不通时改变图标
        if (rt != null) {
            if (rt.heartBreakerRequest != null) {
                if (rt.heartBreakerRequest.isClosed()) {
                    if (pcImageLinkState == true) {
                        pcImageLinkState = false;
                        setResultToTv("PC图传连接关闭");
                        mPCImageView.setImageResource(R.drawable.pc_unconnected);
                        mPCImageView.postInvalidate();
                    }
                } else {
                    if (pcImageLinkState == false) {
                        pcImageLinkState = true;
                        mPCImageView.setImageResource(R.drawable.pc);
                        mPCImageView.postInvalidate();
                    }
                }
            } else {
                if (pcImageLinkState == true) {
                    pcImageLinkState = false;
                    setResultToTv("PC图传NO");
                    mPCImageView.setImageResource(R.drawable.pc_unconnected);
                    mPCImageView.postInvalidate();
                }
            }
        } else {
            setResultToTv("rt--isnull--");
        }
        //当发现与电脑的控制通道连接不通时改变图标
        if (rtControl != null) {
            if (rtControl.heartBreakerRequest != null) {
                if (rtControl.heartBreakerRequest.isClosed()) {
                    if (pcControlLinkState == true) {
                        pcControlLinkState = false;
                        setResultToTv("PC控制连接关闭");
                        mPCControlView.setImageResource(R.drawable.pc_unconnected);
                        mPCControlView.postInvalidate();
                    }
                } else {
                    if (pcControlLinkState == false) {
                        pcControlLinkState = true;
                        mPCControlView.setImageResource(R.drawable.pc);
                        mPCControlView.postInvalidate();
                    }
                }
            } else {
                if (pcControlLinkState == true) {
                    pcControlLinkState = false;
                    setResultToTv("PC控制无法开启");
                    mPCControlView.setImageResource(R.drawable.pc_unconnected);
                    mPCControlView.postInvalidate();
                }
            }
        } else {
            setResultToTv("rtControl为--isnull--");
        }

    }

    private void checkConnectState() {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateIconState();

            }
        });
    }

    class MySurfaceTextureListener implements SurfaceTextureListener{

        @Override
        public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
            Log.e(TAG, "onSurfaceTextureAvailable");
            if (mCodecManager == null) {
                Surface mSurface  = new Surface(surface);
                mHandler.sendMessageDelayed(Message.obtain(mHandler, MSG_INIT_DECODER, mSurface), 200);
                mCodecManager = new DJICodecManager(MainActivity.this, surface, width, height);
            }else {
                //mCodecManager.changeOutputSurface(new Surface(surface));
            }
            setResultToTv("width"+width);
            setResultToTv("height"+height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
            Log.e(TAG,"onSurfaceTextureDestroyed");
            if (mCodecManager != null) {
                //mCodecManager.changeOutputSurface(null);
                mCodecManager.cleanSurface();
                mCodecManager = null;
            }
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {

        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
//	    	try {
//				fops = new FileOutputStream(file);
//			} catch (FileNotFoundException e1) {
//		      // TODO Auto-generated catch block
//				e1.printStackTrace();
//			}
        try {
            fis = new FileInputStream(file);
        } catch (FileNotFoundException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        mVideoSurface = new TextureView(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tv = (TextView) findViewById(R.id.textView1);
        //handler = new Handler();
        initUI();
        this.rt = new RequestThread(ip, 2000, 30000, 30001);
        rt.activity = this;
        rt.heartThreadPriory = Thread.MAX_PRIORITY - 1;
        //rt.heartThreadPriory = 2;
        this.rtControl = new RequestThread(ip, 200, 30002, 30003);
        rtControl.activity = this;
        rtControl.heartThreadPriory = 1;
        rtControl.sleepTime = 1000;
// The callback for receiving the raw H264 video data for camera live view
        /*
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                    mReceivedVideoDataListener = new VideoFeeder.VideoDataListener() {
                        @Override
                        public void onReceive(byte[] videoBuffer, int size) {
                            if (mCodecManager != null) {
                                //这个函数不能被阻塞，否则图像会花掉
                                if (isTransmitting) {
//	            		sendThread s = new sendThread(videoBuffer,size);
//	            		Thread t = new Thread(s);
//	            		t.setPriority(Thread.MAX_PRIORITY);
//	            		t.start();
                                    rt.send2Master(videoBuffer, size);
                                } else {
                                    mCodecManager.sendDataToDecoder(videoBuffer, size);
                                }
                            }
                        }
                    };
                }
        });*/
    }

    protected void onProductChange() {
        initPreviewer();
        loginAccount();
    }

    private void loginAccount() {

        UserAccountManager.getInstance().logIntoDJIUserAccount(this,
                new CommonCallbacks.CompletionCallbackWith<UserAccountState>() {
                    @Override
                    public void onSuccess(final UserAccountState userAccountState) {
                        Log.e(TAG, "Login Success");
                    }

                    @Override
                    public void onFailure(DJIError error) {
                        showToast("Login Error:"
                                + error.getDescription());
                    }
                });
    }



    @Override
    public void onResume() {
        Log.e(TAG, "onResume");
        initPreviewer();
        initFlightController();
        onProductChange();
        if (mVideoSurface == null) {
            Log.e(TAG, "mVideoSurface is null");
        }
        mTimer = new Timer();
        Task task = new Task();
        mTimer.schedule(task, 0, 500);
        countingTask ct = new countingTask();
        mTimer.schedule(ct, 0, 1000);

        //读取控制数据线程
        ControlTimerTask read_t = new ControlTimerTask();
        Thread ctt = new Thread(read_t);
        ctt.setPriority(3);
        ctt.start();
        //执行控制数据线程
        SendVirtualStickDataTask control_t = new SendVirtualStickDataTask();
        Thread sdt = new Thread(control_t);
        sdt.setPriority(3);
        sdt.start();
//        SendGimbleDataTask sgt = new SendGimbleDataTask();
//        Thread g = new Thread(sgt);
//        g.setPriority(3);
//        g.start();
        super.onResume();
    }

    @Override
    public void onPause() {
        Log.e(TAG, "onPause");
        uninitPreviewer();
        this.isTransmitting = false;
        if(mTimer!=null) {
            mTimer.cancel();
            mTimer.purge();
            mTimer = null;
        }
        if((rt.isThreadRunning) && (rt.isLongConnection())){
            rt.setHeartThreadDead();
            while(rt.isThreadRunning);
        }
        if(rtControl.isThreadRunning && (rtControl.isLongConnection())){
            rtControl.setHeartThreadDead();
            while(rtControl.isThreadRunning);
        }
        super.onPause();
    }

    @Override
    public void onStop() {
        Log.e(TAG, "onStop");
        super.onStop();
    }

    public void onReturn(View view) {
        Log.e(TAG, "onReturn");
        this.finish();
    }

    @Override
    protected void onDestroy() {
        Log.e(TAG, "onDestroy");
        uninitPreviewer();
        super.onDestroy();
    }

    private void initFlightController() {
        Aircraft aircraft = FPVDemoApplication.getAircraftInstance();
        if (aircraft == null || !aircraft.isConnected()) {
            showToast("Disconnected");
            mFlightController = null;
            return;
        } else {
            mFlightController = aircraft.getFlightController();
            mFlightController.setRollPitchControlMode(RollPitchControlMode.VELOCITY);
            mFlightController.setYawControlMode(YawControlMode.ANGULAR_VELOCITY);
            mFlightController.setVerticalControlMode(VerticalControlMode.VELOCITY);
            mFlightController.setRollPitchCoordinateSystem(FlightCoordinateSystem.BODY);
        }
        if (isGimbalModuleAvailable()){
            setResultToTv("Gimbal_Ready_");
        }else {
            setResultToTv("Gimbal_UNREADY_");
        }
    }
    private void initUI() {
        // init mVideoSurface
        mVideoSurface = (TextureView) findViewById(R.id.video_previewer_surface);
        if (null != mVideoSurface) {
            mVideoSurface.setSurfaceTextureListener(new MySurfaceTextureListener());
        }
        rv = (RectView) findViewById(R.id.rect_view);
        rv.addOptionalRect(0.5, 0.5, 1, 1, "target", "1");
        rv.setOnTouchListener(new View.OnTouchListener() {
            int x1 = -1;
            int y1 = -1;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // TODO Auto-generated method stub
                int xl;
                int xr;
                int yt;
                int yb;
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        x1 = (int) event.getX();
                        y1 = (int) event.getY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        int mx = (int) event.getX();
                        int my = (int) event.getY();
                        xl = Math.min(mx, x1);
                        xr = Math.max(mx, x1);
                        yt = Math.min(my, y1);
                        yb = Math.max(my, y1);
                        rv.setOptionalRect("1", xl, yt, xr, yb, "target");
                        break;
                    case MotionEvent.ACTION_UP:
                        int x2 = (int) event.getX();
                        int y2 = (int) event.getY();
                        xl = Math.min(x2, x1);
                        xr = Math.max(x2, x1);
                        yt = Math.min(y2, y1);
                        yb = Math.max(y2, y1);
                        rv.setOptionalRect("1", xl, yt, xr, yb, "target");
                        x1 = -1;
                        y1 = -1;
                        x = xl;
                        y = yt;
                        w = xr - xl;
                        h = yb - yt;
                        setResultToTv("画框完成");
                        byte[] data = {0, 2, 1,  (byte) x, (byte) (x >> 8)
                                ,(byte) y, (byte) (y >> 8)
                                ,(byte) w, (byte) (w >> 8)
                                ,(byte) h, (byte) (h >> 8)
                                ,(byte) rv.getWidth(), (byte) (rv.getWidth() >> 8)
                                ,(byte) rv.getHeight(), (byte) (rv.getHeight() >> 8)};
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    setResultToTv("发送框数据");
                                    SocketUtil.wrightBytes2Stream(data,rtControl.heartBreakerRequest.getOutputStream());
                                    setResultToTv("发送框完成");
                                } catch (IOException e) {
                                    showToast("发送框数据失败1！！！"+e);
                                    e.printStackTrace();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    setResultToTv("发送框数据失败2！！！"+e);
                                }
                            }
                        }).start();
                        break;
                }
                return true;
            }
        });
        //init btn
        mSprinRightBtn = (Button) findViewById(R.id.btn_sprin_right);
        mTransmitImageBtn = (ToggleButton) findViewById(R.id.btn_transmit_image);
        mOnekeyFlyBtn = (ToggleButton) findViewById(R.id.btn_one_key_fly);
        mBtnVirtualStick=(ToggleButton) findViewById(R.id.btn_virtual_stick) ;
        //init image
        this.mAircraftImageView = (ImageView) findViewById(R.id.imageViewAircraft);
        this.mPCImageView = (ImageView) findViewById(R.id.imageViewPC);
        this.mPCControlView = (ImageView) findViewById(R.id.imageViewControlConnect);

        mOnekeyFlyBtn.setOnClickListener(this);
        mSprinRightBtn.setOnClickListener(this);
        mTransmitImageBtn.setOnClickListener(this);
        mBtnVirtualStick.setOnClickListener(this);

        mTransmitImageBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    beginImageTransmite();
                } else {
                    stopImageTransmite();
                }
            }
        });
        mOnekeyFlyBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    oneKeyFly();
                } else {
                    onekeydown();
                }
            }
        });
        mBtnVirtualStick.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    enablevirtualstick();
                } else {
                    disablevirtualstick();
                }
            }
        });
    }

    private void initPreviewer() {

        BaseProduct product = FPVDemoApplication.getProductInstance();

        if (product == null || !product.isConnected()) {
            showToast(getString(R.string.disconnected));
        } else {
            if (mCodecManager!=null) {
                mCodecManager.switchSource(DJICodecManager.VideoSource.CAMERA);
            }
            if (null != mVideoSurface) {
                mVideoSurface.setSurfaceTextureListener(new MySurfaceTextureListener());
            }
            if (!product.getModel().equals(Model.UNKNOWN_AIRCRAFT)) {
                VideoFeeder.getInstance().getPrimaryVideoFeed().addVideoDataListener(mReceivedVideoDataListener);
            }
        }
    }

    private void uninitPreviewer() {
        Camera camera = FPVDemoApplication.getCameraInstance();
        if (camera != null) {
            // Reset the callback
            VideoFeeder.getInstance().getPrimaryVideoFeed().addVideoDataListener(null);
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_sprin_right) {
            spin2Right();
        }
    }

    public void beginImageTransmite() {
        Camera camera = FPVDemoApplication.getCameraInstance();
        if (!isTransmitting) {
            if ((camera != null)&&(rt.isLongConnection()) && (this.aircraftLinkState) && (this.pcImageLinkState)) {
                this.isTransmitting = true;
                Toast.makeText(this, "开始图传", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "请确保与局域网和飞行控制器的连接正常", Toast.LENGTH_SHORT).show();
            }
        } else {
            isTransmitting = false;
            Toast.makeText(this, "停止图传", Toast.LENGTH_SHORT).show();
        }
    }
    public void stopImageTransmite() {
        isTransmitting = false;
        Toast.makeText(this, "图传已停止", Toast.LENGTH_SHORT).show();
    }

    public void oneKeyFly() {
        if(mFlightController != null){
            mFlightController.startTakeoff(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError != null) {
                        showToast(djiError.getDescription());
                    } else {
                        showToast("起飞成功");
                    }
                }
            });
        }
    }
    public void onekeydown(){
        mGimbalPitch=(float)(0);
        //stopGimble();
        if (mFlightController != null) {
            mFlightController.startLanding(
                    djiError -> {
                        if (djiError != null) {
                            showToast(djiError.getDescription());
                        } else {
                            showToast("开始降落");
                        }
                    }
            );
        }
    }

    public void enablevirtualstick(){

        if (mFlightController != null) {
            mFlightController.setVirtualStickModeEnabled(true, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError != null) {
                        showToast(djiError.getDescription());
                    } else {
                        showToast("控制开启");
                    }
                }
            });
        }
    }
    public void disablevirtualstick(){
        mGimbalPitch=(float)(0);
        stopGimble();
        if (mFlightController != null) {
            mFlightController.setVirtualStickModeEnabled(false, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError != null) {
                        showToast(djiError.getDescription());
                    } else {
                        showToast("控制关闭");
                    }
                }
            });
        }
    }

    public void spin2Right() {
        mPitch = (float)(0);
        mRoll = (float)(0);
        mYaw = (float) (10);
        mThrottle = (float)(0);
        showToast("右转:" + mYaw);
    }

    private void stopGimble(){
        if (isGimbalModuleAvailable()){
            FPVDemoApplication.getProductInstance().getGimbal().
                    rotate(null, new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {

                        }
                    });
        }
    }

    class SendVirtualStickDataTask implements Runnable {
        @Override
        public void run() {
            while (isFlying) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (mFlightController != null) {
                    mFlightController.sendVirtualStickFlightControlData(
                            new FlightControlData(mPitch, mRoll, mYaw, mThrottle), djiError -> {
                            }
                    );
                }
                if(isGimbalModuleAvailable()) {
                    FPVDemoApplication.getProductInstance().getGimbal().rotate(new Rotation.Builder().pitch(mGimbalPitch)
                            .mode(RotationMode.SPEED)
                            .yaw(Rotation.NO_ROTATION)
                            .roll(Rotation.NO_ROTATION)
                            .time(0)
                            .build(), new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                        }
                    });
                }
            }
        }
    }
/*
    class SendGimbleDataTask implements Runnable {

        @Override
        public void run() {
            while(isGimbalModuleAvailable()){
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                FPVDemoApplication.getProductInstance().getGimbal().rotate(new Rotation.Builder().pitch(mGimbalPitch)
                        .mode(RotationMode.SPEED)
                        .yaw(Rotation.NO_ROTATION)
                        .roll(Rotation.NO_ROTATION)
                        .time(0)
                        .build(), new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {

                    }
                });
            }
        }
    }
*/
    /*
     * 将图片写入到磁盘
     *
     * @param img      图片数据流
     * @param fileName 文件保存时的名称
     */
    public void writeImageTODisk(byte[] img, String fileName){
        try {
            File file=new File(fileName);
            FileOutputStream fops =new FileOutputStream(file);
            fops.write(img);
            fops.flush();
            fops.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public byte[] readFile2Byte(String pathStr) throws Exception {
        byte[] outByte = null;
        InputStream is = null;
        ByteArrayOutputStream out = null;
        try{

            out = new ByteArrayOutputStream();

        }
        catch (Exception e){
            Log.e("TimingMmsService.error" ,e.getMessage());
        }
        try {
            is = new FileInputStream(pathStr);// pathStr 文件路径
            byte[] b = new byte[1024];
            int n;
            while ((n = is.read(b)) != -1) {
                out.write(b, 0, n);
            }// end while
        } catch (Exception e) {
            Log.e("TimingMmsService.error" ,e.getMessage());
            throw new Exception("System error,SendTimingMms.getBytesFromFile", e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                    if (out != null) out.close();
                    outByte = out.toByteArray();
                } catch (Exception e) {
                    Log.e("error", e.toString());// TODO
                }// end try
            }// end if
        }// end try

        return outByte;
    }
    /*
     * Description : init decoder
     */
    private void initDecoder(Surface surface) {

        mReceivedVideoDataListener=new VideoFeeder.VideoDataListener() {
            @Override
            public void onReceive(byte[] videoBuffer, int size) {

                if (isTransmitting){
//                    sendThread s = new sendThread(videoBuffer,size);
//                    Thread t = new Thread(s);
//                    t.setPriority(8);
//                    t.start();
                    rt.send2Master(videoBuffer,size);
                }else{
                    mCodecManager.sendDataToDecoder(videoBuffer,size);
                }
            }
        };
       VideoFeeder.getInstance().getPrimaryVideoFeed().addVideoDataListener(mReceivedVideoDataListener);
    }
    class sendThread implements Runnable{
        byte[] videoBuffer;
        int size;
        sendThread(byte[] videoBuffer,int size){
            this.videoBuffer = videoBuffer;
            this.size = size;
        }
        @Override
        public void run() {
            // TODO Auto-generated method stub
            rt.send2Master(videoBuffer, size);
        }

    }
}



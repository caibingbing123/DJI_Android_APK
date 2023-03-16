# DJI_Android_APK
大疆移动端APP：使用DJI Mobile SDK开发Android应用程序访问DJI产品功能，实现自主飞行，控制相机云台，接收实时视频图传。

1、使用Android Studio导入其中项目，更改代码里面ip为笔记本的IP；
2、在[大疆开发者]([DJI Developer](https://developer.dji.com/cn/))官网申请与项目包同名DJI Mobile SDK密钥；
3、更改AndroidManifest.xml文件中为自己密钥，如下：

<meta-data android:name="com.dji.sdk.API_KEY" android:value="xxxxx"/>；
4、编译并且安装在版本为7以上的安卓手机上。

package com.garrett.ui;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

public class RectView extends View {
	private class DipRect{
		protected String id = "default";
		
		//这里的left top right bottom 都是0到1以内的小数
		private double left = -1;
		private double top = -1;
		private double right = -1;
		private double bottom = -1;
		private String text = "";
		private boolean isValid = false;
		public DipRect(){
		}
		public DipRect(String id){
			this.id = id;
		}
		
		protected void setCordinate(double l, double t, double r, double b){
			this.bottom = b;
			this.top = t;
			this.right = r;
			this.left = l;
			this.isValid = true;
		}
		
		protected void setCordinate(int l, int t, int r, int b){
			this.bottom = (float)b / (float)RectView.this.getHeight();
			this.top = (float)t /(float) RectView.this.getHeight();
			this.right = (float)r / (float)RectView.this.getWidth();
			this.left = (float)l /(float) RectView.this.getWidth();
			this.isValid = true;
		}
		
		protected Canvas drawRect(Canvas canvas){
			canvas.drawRect((float)(this.left * RectView.this.getWidth()) , (float)(this.top* RectView.this.getHeight()), (float)(this.right* RectView.this.getWidth()), (float)(this.bottom* RectView.this.getHeight()), mLinePaint);
			return canvas;
		}
		
		protected Canvas drawText(Canvas canvas){
			canvas.drawText(this.text, (int)(this.left* RectView.this.getWidth()), (int)(this.bottom* RectView.this.getHeight()), RectView.this.mTextPaint);
			return canvas;
		}
		
	}
	private DipRect face = new DipRect("face");
	private DipRect leftEye = new DipRect("left eye");
	private DipRect rightEye = new DipRect("right eye");
	private ArrayList<DipRect>  optionalRect = new ArrayList<DipRect>(); 
	private Paint mLinePaint;
	private Paint mTextPaint;
	
	private String text = "Searching face";
	public RectView(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
		initPaint();
	}

	@Override
	protected void onDraw(Canvas canvas) {
		// TODO Auto-generated method stub
		super.onDraw(canvas);
		if(face.isValid)
		{
			face.drawRect(canvas);
			face.drawText(canvas);
		}
		if(leftEye.isValid){
			leftEye.drawRect(canvas);
		}
		if(rightEye.isValid){
			rightEye.drawRect(canvas);
		}
		if(!this.optionalRect.isEmpty()){
			for(int i = 0; i < this.optionalRect.size(); i++){
				this.optionalRect.get(i).drawRect(canvas);
				if(this.optionalRect.get(i).text != ""){
					this.optionalRect.get(i).drawText(canvas);
				}
			}
		}
	}

	public void setFace(double left, double top, double right, double bottom){
		this.face.left = left;
		this.face.right = right;
		this.face.bottom = bottom;
		this.face.top = top;
		this.face.isValid = true;
		invalidate();
	}
	
	public void setLeftEye(double l, double t, double r, double b){
		this.leftEye.setCordinate(l, r, t, b);
		invalidate();
	}
	
	public void setRightEye(double l, double t, double r, double b){
		this.rightEye.setCordinate(l, r, t, b);
		invalidate();
	}
	
	public void setFaceText(String s){
		this.face.text = s;
		invalidate();
	}
	
	@Override
	public void invalidate(){
		this.setVisibility(View.VISIBLE);
		super.invalidate();
	}
	
	private void initPaint(){
		mLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
//		int color = Color.rgb(0, 150, 255);
		int color = Color.rgb(98, 212, 68);
//		mLinePaint.setColor(Color.RED);
		mLinePaint.setColor(color);
		mLinePaint.setStyle(Style.STROKE);
		mLinePaint.setStrokeWidth(5f);
		mLinePaint.setAlpha(180);
		mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		int colorText = Color.YELLOW;
		mTextPaint.setColor(colorText);
		mTextPaint.setTextSize(50);
	}
	
	public void addOptionalRect(double l, double t, double r, double b, String text, String id){
		DipRect dr = new DipRect(id);
		dr.setCordinate(l, t, r, b);
		dr.text = text;
		this.optionalRect.add(dr);
		invalidate();
	}
	
	public void addOptionalRect(int l, int t, int r, int b, String text, String id){
		DipRect dr = new DipRect(id);
		dr.setCordinate(l, t, r, b);
		dr.text = text;
		this.optionalRect.add(dr);
		invalidate();
	}
	
	public void setOptionalRect(String id, double l, double t, double r, double b, String text){
		for (int i = 0; i < optionalRect.size(); i++){
			if(optionalRect.get(i).id.equals(id)){
				optionalRect.get(i).setCordinate(l, t, r, b);
				optionalRect.get(i).text = text;
				
			}
		}
		invalidate();
	}
	
	public void setOptionalRect(String id, int l, int t, int r, int b, String text){
		for (int i = 0; i < optionalRect.size(); i++){
			if(optionalRect.get(i).id.equals(id)){
				optionalRect.get(i).setCordinate(l, t, r, b);
				optionalRect.get(i).text = text;
				
			}
		}
		invalidate();
	}
	
	public void deleteOptionalRect(String id){
		for (int i = 0; i < optionalRect.size(); i++){
			if(optionalRect.get(i).id.equals(id)){
				optionalRect.remove(i);
			
			}
		}
		invalidate();
	}
}

package com.garrett.ui;

import com.dji.FPVDemo.R;

import android.app.Dialog;
import android.content.Context;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class HKDialogLoading extends Dialog {
	Context context;

	public HKDialogLoading(Context context, int theme) {
		super(context, theme);
		this.context = context;
		ini();
	}

	public HKDialogLoading(Context context) {
		super(context);
		this.context = context;
		ini();
	}

	void ini() {
		/**
		 * "加载项"布局，此布局被添加到ListView的Footer中。
		 */
		LinearLayout contentView = new LinearLayout(context);
		contentView.setMinimumHeight(60);
		contentView.setGravity(Gravity.CENTER);
		contentView.setOrientation(LinearLayout.HORIZONTAL);

		/**
		 * 向"加载项"布局中添加一个圆型进度条。
		 */
		ImageView image = new ImageView(context);
		image.setImageResource(R.drawable.loading_icon);
		Animation anim = AnimationUtils.loadAnimation(context,
				R.anim.rotate_repeat);
		LinearInterpolator lir = new LinearInterpolator();
		anim.setInterpolator(lir);
		image.setAnimation(anim);

		contentView.addView(image);
		setContentView(contentView);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// 按下键盘上返回按钮
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			this.dismiss();
		}
		return true;
	}
}
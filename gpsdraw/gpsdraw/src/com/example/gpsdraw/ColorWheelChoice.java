package com.example.gpsdraw;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class ColorWheelChoice extends SurfaceView {
	float r, g, b;
	public ColorWheelChoice(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	public ColorWheelChoice(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}
	
	public ColorWheelChoice(Context context) {
		super(context);
		init();
	}
	
	void init()
	{
		this.setWillNotDraw(false);
		getHolder().addCallback(new SurfaceHolder.Callback() {
			@Override
			public void surfaceDestroyed(SurfaceHolder holder) {
			}
			
			@SuppressLint("WrongCall")
			@Override
			public void surfaceCreated(SurfaceHolder holder) {
				Canvas canvas = holder.lockCanvas(null);
				onDraw(canvas);
				holder.unlockCanvasAndPost(canvas);
			}
			
			@Override
			public void surfaceChanged(SurfaceHolder holder, int format, int width,
					int height) {
			}
		});
	}
	

	@Override
	protected void onDraw(Canvas canvas) {
		Paint paint = new Paint();
		paint.setColor(Color.argb(255, (int)(255*r), (int)(255*g), (int)(255*b)));
		canvas.drawRect(canvas.getClipBounds(), paint);
	}
	
	public void setColor(float r, float g, float b)
	{
		this.r = r;
		this.g = g;
		this.b = b;
		this.invalidate();
	}


}

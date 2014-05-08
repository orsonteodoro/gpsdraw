package com.example.gpsdraw;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class ColorWheel extends SurfaceView {
	public static Bitmap bmp;
	
	public ColorWheel(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	public ColorWheel(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}
	
	public ColorWheel(Context context) {
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
		if (bmp == null)
		{
			float l = Math.min(this.getWidth(), this.getHeight());
			bmp = Bitmap.createBitmap((int)l, (int)l, Bitmap.Config.ARGB_8888);
			bmp.eraseColor(Color.WHITE);
			for(float r = 0; r < l/2; r+=1)
			{
				for(float i = 0; i < 360; i+=0.1f)
				{
					int x = (int) (l/2 + r*Math.cos(Math.toRadians(i)));
					int y = (int) (l/2 - r*Math.sin(Math.toRadians(i)));
					bmp.setPixel(x, y, Color.HSVToColor(new float[]{i,1f,r/(l/2)}));
				}
			}
		}		
		paint.setColor(Color.WHITE);
		canvas.drawRect(canvas.getClipBounds(), paint);
		canvas.drawBitmap(bmp, 0, 0, paint);
	}
}

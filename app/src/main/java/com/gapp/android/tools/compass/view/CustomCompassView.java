package com.gapp.android.tools.compass.view;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.sftech.tools.compassmap.R;

public class CustomCompassView extends View {

	private Bitmap bmpCompass;
	
	private Bitmap bitmapBase;
	
	private Bitmap bitmapBaseT_;
	
	private Matrix mMatrix;
	
	private double mDegree;
	
	private Paint mPaint;
	
	private int width;
	
	private int height;
	
	private boolean isFront;
	
	private boolean initialEffect;
	
	private boolean isTransparent;

	public CustomCompassView(Context context) {
		super(context);

		init();
	}

	public CustomCompassView(Context context, AttributeSet attrs) {

		super( context, attrs );
		init();
	}

	public CustomCompassView(Context context, AttributeSet attrs, int defStyle) {

		super( context, attrs, defStyle );
		init();
	}	

	private void init() {
		Resources r = getResources();
		bmpCompass = BitmapFactory.decodeResource(r, R.drawable.compass_display);
		bitmapBase = BitmapFactory.decodeResource(r, R.drawable.compass_base);
		bitmapBaseT_ = BitmapFactory.decodeResource(r, R.drawable.compass_base_camera);

		mMatrix = new Matrix();

		assert(bmpCompass != null);

		mPaint = new Paint();
		mPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
		mPaint.setFilterBitmap(true);
	}

	
	public void destroy() {
		
		
		bitmapClear(bmpCompass);
		bitmapClear(bitmapBase);
		bitmapClear(bitmapBaseT_);

		bitmapBaseT_ = null;
		bmpCompass = null;
		bitmapBase = null;
		mMatrix = null;
		mPaint = null;
	}
	
	
	private void bitmapClear(Bitmap bmp) {
		if (bmp != null)
			bmp.recycle();
	}

	public void setTransparentMode(boolean fTransparent) {
		isTransparent = fTransparent;
	}

	
	@Override
	public void onDraw(Canvas c){
		
		//Bitmap bmpBase = bitmapBaseT_;
		Bitmap bmpBase = isTransparent? bitmapBaseT_:bitmapBase;
		
		if (bmpCompass != null) {
			
			c.drawBitmap(bmpBase, (width - bmpBase.getWidth()) / 2, (height - bmpBase.getHeight()) / 2, mPaint);
			
			mMatrix.reset();		
			mMatrix.postTranslate(bmpCompass.getWidth() / -2.0f, bmpCompass.getHeight() / -2.0f);
			mMatrix.postRotate((float) (mDegree * (isFront ? -1.0f : 1.0f)));
			mMatrix.postTranslate(width / 2.0f, height / 2.0f);
			c.concat(mMatrix);	
			c.drawBitmap(bmpCompass, 0, 0, mPaint);
		}
		
	}

	
	@Override
	protected void onSizeChanged (int w, int h, int oldw, int oldh) 
	{
		width = w;
		height = h;
	}

	
	public void updatePosition(double orientation, double pitch, double roll) {

		if (initialEffect == false) {
			mDegree = orientation + 120.0f;
			mDegree %= 360.0f;
			initialEffect = true;
		}

		isFront = (-90.0f <= roll && roll <= 90.0f);

		if (Math.abs(orientation) > 90.0f) {

			if (mDegree > 0 && orientation < 0) {

				orientation += 360.0f;
				mDegree = orientation * 0.1f + mDegree * 0.9f - 360.0f;

				return;

			} else if (mDegree < 0 && orientation > 0) {

				orientation -= 360.0f;
				mDegree = orientation * 0.1f + mDegree * 0.9f + 360.0f;

				return;
			}
		}

		mDegree = orientation * 0.1f + mDegree * 0.9f;

		this.invalidate();
	}

}

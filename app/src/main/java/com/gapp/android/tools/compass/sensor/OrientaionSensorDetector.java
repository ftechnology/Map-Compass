package com.gapp.android.tools.compass.sensor;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.Display;
import android.view.Surface;

public class OrientaionSensorDetector implements SensorSupportedInterface,
		SensorEventListener {

	private float[] aValues = new float[3];

	private float[] mValues = new float[3];

	private OrientationListener orientationListener;

	private SensorManager sensorManager;

	private boolean isWorking;

	private int distancePosRotation;

	static public interface OrientationListener {
		public void onOrientaion(double d, double e, double f);
	}

	public void setListener(OrientationListener l) {
		orientationListener = l;
	}

	public OrientaionSensorDetector(Activity activity) {

		sensorManager = (SensorManager) activity
				.getSystemService(Context.SENSOR_SERVICE);
		Display d = activity.getWindowManager().getDefaultDisplay();
		distancePosRotation = d.getRotation();
	}

	@Override
	public void sensorOpen() throws SensorException {

		if (checkSensorExists() == false) {
			throw new SensorException();
		}

		if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) == null) {

			return;
		}

		sensorManager.registerListener(this,
				sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
				SensorManager.SENSOR_DELAY_UI);
		sensorManager.registerListener(this,
				sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
				SensorManager.SENSOR_DELAY_UI);
	}

	public void destory() {
		sensorManager = null;
		orientationListener = null;
	}

	private boolean checkSensorExists() {

		List<Sensor> sensors = sensorManager.getSensorList(Sensor.TYPE_ALL);

		boolean haveAcc = false;
		boolean haveMag = false;

		for (Sensor s : sensors) {

			if (s.getType() == Sensor.TYPE_ACCELEROMETER)
				haveAcc = true;

			if (s.getType() == Sensor.TYPE_MAGNETIC_FIELD)
				haveMag = true;
		}

		return haveAcc && haveMag;
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	@Override
	public void onSensorChanged(SensorEvent event) {

		switch (event.sensor.getType()) {

		case Sensor.TYPE_ACCELEROMETER:
			aValues = event.values.clone();
			break;
		case Sensor.TYPE_MAGNETIC_FIELD:
			mValues = event.values.clone();
			break;
		default:

		}

		if (aValues != null && mValues != null) {

			float[] R = new float[16];
			float[] I = new float[16];

			SensorManager.getRotationMatrix(R, I, aValues, mValues);

			float[] actual_orientation = new float[3];

			calculateExactOrientation(R, actual_orientation);

			if (orientationListener != null) {

				double orientation = Math.toDegrees(actual_orientation[0]);
				double pitch = Math.toDegrees(actual_orientation[1]);
				double roll = Math.toDegrees(actual_orientation[2]);

				if (orientation != 0.0f && pitch != 0.0f && roll != 0.0f)
					isWorking = true;

				if (isWorking)
					orientationListener.onOrientaion(orientation, pitch, roll);
			}
		}
	}

	private void calculateExactOrientation(float[] R, float[] out) {

		int dr = distancePosRotation;

		if (dr == Surface.ROTATION_0) {
			SensorManager.getOrientation(R, out);

		} else {
			float[] outR = new float[16];

			if (dr == Surface.ROTATION_90) {
				SensorManager.remapCoordinateSystem(R, SensorManager.AXIS_Y,
						SensorManager.AXIS_MINUS_X, outR);

			} else if (dr == Surface.ROTATION_180) {
				float[] outR2 = new float[16];
				SensorManager.remapCoordinateSystem(R, SensorManager.AXIS_Y,
						SensorManager.AXIS_MINUS_X, outR2);
				SensorManager.remapCoordinateSystem(outR2,
						SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_X, outR);

			} else if (dr == Surface.ROTATION_270) {
				SensorManager.remapCoordinateSystem(R,
						SensorManager.AXIS_MINUS_Y, SensorManager.AXIS_X, outR);

			}
			SensorManager.getOrientation(outR, out);
		}
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub

	}

}
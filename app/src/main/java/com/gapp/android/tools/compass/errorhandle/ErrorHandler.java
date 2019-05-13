package com.gapp.android.tools.compass.errorhandle;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import com.gapp.android.tools.compass.sensor.SensorException;
import com.gapp.android.tools.compass.sensor.SensorSupportedInterface;

public class ErrorHandler {

	
	public static void open_sensor(SensorSupportedInterface sensor, Activity activity) {

		try {

			sensor.sensorOpen();

		} catch (SensorException e) {

			errorDialog(activity, "Sensor not supported", createSimpleListnerFinish(activity));
		}
	}

	public static DialogInterface.OnClickListener createSimpleListnerFinish(final Activity activity) {

		return new DialogInterface.OnClickListener () {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				activity.finish();
			}	
		};
	}

	private static void errorDialog(Context context, String msg_id, DialogInterface.OnClickListener oklistner) {

		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
		alertDialogBuilder.setTitle("Error occured");
		alertDialogBuilder.setMessage(msg_id);
		alertDialogBuilder.setPositiveButton("Ok", oklistner);

		alertDialogBuilder.setCancelable(false);
		AlertDialog alertDialog = alertDialogBuilder.create();
		
		alertDialog.show();
	}
	
}

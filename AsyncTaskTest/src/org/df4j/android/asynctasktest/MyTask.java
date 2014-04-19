package org.df4j.android.asynctasktest;

import org.df4j.android.uiconnector.AsyncTask;
import org.df4j.android.uiconnector.ProxyConnector;

import android.util.Log;

class MyTask extends AsyncTask<Integer, ProgressView> {	
	protected ProgressView connector;
	
	public MyTask(int variant) {
		if (variant==0) {
		    connector = new MyConnector(this);
		} else {
			connector = ProxyConnector.makeProxy(ProgressView.class, this);
		}
	}

	@Override
	protected void doInBackground(Integer... params) {
		long start=System.currentTimeMillis();
		int times=params[0];
		try {
			for (int i = 0; i <times; i++) {
				Thread.sleep(MainActivity.SLEEP_TIME);
				connector.publishProgress(i);
			}
		} catch (InterruptedException e) {
			Log.w("doInBackground", "InterruptedException");
			connector.publishFailure(e);
			return;
		}
		connector.publishResult(System.currentTimeMillis()-start);
	}
}
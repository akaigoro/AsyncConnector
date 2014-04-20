package org.df4j.android.asynctasktest;

import org.df4j.android.uiconnector.AsyncConnector;

import android.util.Log;

class MyTask extends AsyncConnector<Integer, ProgressView> {	
	protected ProgressView connector;
	
	public MyTask(int variant) {
		if (variant==0) {
		    connector = new MyConnector();
		} else {
			connector = super.makeProxy(ProgressView.class);
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
			try {
				connector.publishFailure(System.currentTimeMillis()-start, e);
			} catch (Throwable e1) {
				// this should not happen
				e1.printStackTrace();
			}
			return;
		}
		connector.publishResult(System.currentTimeMillis()-start);
	}

	 class MyConnector implements ProgressView {		

			// to implement ProgressView for background task ---//
			
		    @Override
			public void publishProgress(final Integer value) {
		    	new UIMessage(){
		            @Override
					protected void act(ProgressView listener) {
		   				listener.publishProgress(value);
		            }
		        }.start();
			}

		    @Override
			public void publishResult(final Long time) {
		    	new UIMessage(){
		            @Override
					protected void act(ProgressView listener) {
		    			listener.publishResult(time);
		            }
		        }.start();
			}

		    @Override
			public void publishFailure(final Long time, final Throwable e) {
		    	new UIMessage(){
		            @Override
					protected void act(ProgressView listener) throws Throwable {
		    			listener.publishFailure(time, e);
		            }
		        }.start();
			}
		}
}
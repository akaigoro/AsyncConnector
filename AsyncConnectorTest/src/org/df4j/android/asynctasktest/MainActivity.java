package org.df4j.android.asynctasktest;

import java.util.concurrent.ThreadPoolExecutor;

import org.df4j.android.log.Logm;
import org.df4j.android.uiconnector.ExceptionHandler;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

public class MainActivity extends Activity {
	private static final String MY_TASK_ID = "myTaskId";
	private static final String MY_DIALOG = "myDialog";
	static final int SLEEP_TIME=80;
	
	MyDialog myDialog;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Logm.i();
		setContentView(R.layout.main);

		((Button)findViewById(R.id.btnstart00)).setOnClickListener(new ButtonStart(0,0));
		((Button)findViewById(R.id.btnstart01)).setOnClickListener(new ButtonStart(0,1));
		((Button)findViewById(R.id.btnstart10)).setOnClickListener(new ButtonStart(1,0));
		((Button)findViewById(R.id.btnstart11)).setOnClickListener(new ButtonStart(1,1));
	}
	 
    @Override
    protected void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);
    	if (myDialog!=null) {
    		Logm.i("myDialog!=null");
    		outState.putBundle(MY_DIALOG, myDialog.onSaveInstanceState());
    	} else {
    		Logm.i("myDialog==null");		
            outState.remove(MY_DIALOG);
    	}
    }

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		Bundle dialogState=savedInstanceState.getBundle(MY_DIALOG);
		if (dialogState!=null) {
			myDialog = new MyDialog(dialogState);
		}
	}

	private final class ButtonStart implements android.view.View.OnClickListener {
		int var1, var2;
		
		public ButtonStart(int var1, int var2) {
			this.var1 = var1;
			this.var2 = var2;
		}

		@Override
		public void onClick(View v) {
			if (myDialog==null) {
				myDialog = new MyDialog(var1, var2);
			} else {
				Log.w("btnStart.onClick", "myDialog!=null");
			}			
		}
	}

	class MyDialog extends Dialog implements ProgressView, ExceptionHandler {
		ProgressBar progressBar;
		TextView tvLoading;
		TextView tvPer;
		Button btnCancel;
		Button btnError;
		boolean makeError=false;
		MyTask myTask;
		{
			this.requestWindowFeature(Window.FEATURE_NO_TITLE);
			this.setContentView(R.layout.progressdialog);
			progressBar = (ProgressBar) this.findViewById(R.id.progressBar1);
			tvLoading = (TextView) this.findViewById(R.id.tv1);
			tvPer =  (TextView) this.findViewById(R.id.tvper);
			btnCancel = (Button) this.findViewById(R.id.btncancel);
			btnCancel.setOnClickListener(new android.view.View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (myTask!=null) {
					    myTask.cancel(true);
					}
				}
			});
			btnError = (Button) this.findViewById(R.id.Button01);
			btnError.setOnClickListener(new android.view.View.OnClickListener() {
				@Override
				public void onClick(View v) {
					makeError=true;
				}
			});
			this.show();
		}
		
		public MyDialog(int var1, int var2) {
			super(MainActivity.this);
			myTask = new MyTask(var2);
			myTask.setListener(this);  // rule 1: when Dialog is shown, it should register
			if (var1==0) {
				ThreadPoolExecutor exec = (ThreadPoolExecutor)android.os.AsyncTask.THREAD_POOL_EXECUTOR;
				myTask.executeOnExecutor(exec, 100);
			} else {
				myTask.execute(100);
			}
			Logm.i("started", myTask.getListedId());
		}

		public MyDialog(Bundle dialogState) {
			super(MainActivity.this);
			int myTaskId=dialogState.getInt(MY_TASK_ID);
    		myTask=MyTask.<MyTask>getTask(myTaskId);
    		if (myTask!=null) {
    			myTask.setListener(this);  // rule 1
    			Logm.i("restored", myTaskId);
    		} else {
    			Logm.e("could not restore: task", myTaskId+" not found");
    		}
		}

		/**
		 * rule 2: when dialog is dismissed, it should unregister itself
		 * both in task and in activity
		 */
		@Override
		public void dismiss() {
			super.dismiss();
			myDialog=null;
			myTask.removeListener(); 
		}

		public Bundle onSaveInstanceState() {
			Bundle bundle=super.onSaveInstanceState();
    		int myTaskId=myTask.getListedId();
			bundle.putInt(MY_TASK_ID, myTaskId);
			this.dismiss();
			Logm.i("dismissed", myTaskId);
			return bundle;
		}

		@Override
		public void publishProgress(Integer progress) {
			if (makeError) {
				// check how ExceptionHandler works - 
				// this.handleException() should be invoked
				makeError=false;
				Logm.i("make error");
				throw new RuntimeException();
			}
			progressBar.setProgress(progress);
			tvLoading.setText("Loading...  " + progress + " %");
			tvPer.setText(progress+" %");
		}

		@Override
		public void handleException(Throwable e) {
			Logm.i(e);
			showResult("Exception!", e.toString());
		}
		
		@Override
		public void publishResult(Long time) {
			Logm.i();
			this.dismiss();
			showResult("Completed!", "Your Task completed in "+time+" ms");
	    }
		
		@Override
		public void publishFailure(Long time, Throwable e) {
			Logm.i();
			this.dismiss();
			String excName=e.toString();
			excName=excName.substring(excName.lastIndexOf('.')+1);
			showResult("Cancelled!", "Your Task is cancelled with "+excName+" after "+time+" ms");
		}

		public void showResult(String title, String message) {
			new AlertDialog.Builder(MainActivity.this)
	        .setTitle(title)
	        .setMessage(message)
	        .setCancelable(false)
	        .setNegativeButton("Dismiss", new DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface dialog, int id) {
	                dialog.cancel();
	            }
	        })
	        .create()
	        .show();
	    }
		
	}

}
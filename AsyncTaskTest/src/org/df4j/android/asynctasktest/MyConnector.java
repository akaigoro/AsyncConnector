/*
 * Copyright 2014 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.df4j.android.asynctasktest;

import org.df4j.android.uiconnector.AsyncConnector;
import org.df4j.android.uiconnector.AsyncTask;

/**
 *  implements interface similar to that of android.os.AsyncTask.
 *
 * @param <Integer>  type of Integer message
 * @param <Integer>  type of final message
 */
public class MyConnector extends AsyncConnector<ProgressView> implements ProgressView {		

	public MyConnector(AsyncTask<?, ProgressView> taskBase) {
		super(taskBase);
	}

	private PortHandler<Integer> progressor = new PortHandler<Integer>() {
		@Override
		protected void act(ProgressView listener, Integer value) {
			listener.publishProgress(value);
		}
	};
	
	private PortHandler<Long> finalizer = new PortHandler<Long>() {
		@Override
		protected void act(ProgressView listener, Long m) {
			listener.publishResult(m);
		}			
	};

	private PortHandler<Throwable> errFinalizer = new PortHandler<Throwable>() {
		@Override
		protected void act(ProgressView listener, Throwable m) {
			listener.publishFailure(m);
		}			
	};

	// to implement ProgressView for background task ---//
	
	public void publishProgress(Integer value) {
		progressor.post(value);
	}

	public void publishResult(Long time) {
		finalizer.post(time);
	}

	@Override
	public void publishFailure(Throwable e) {
		errFinalizer.post(e);	}
}
/*
 * Copyright 2014 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.df4j.android.uiconnector;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import android.os.Handler;
import android.os.Looper;
import android.util.SparseArray;

/**
 * 
 * @author rfq
 *
 * @param <Params> type of parameters for {@{doInBackground} method
 * @param <L> type of UI Listener
 */
public abstract class AsyncTask<Params, L> {	
	/** tasks are numbered */
	private static final AtomicInteger idCount=new AtomicInteger();
	private static SparseArray<AsyncTask<?,?>> tasks=new  SparseArray<AsyncTask<?,?>>();
	private final int listedId;
	private L listener;
	/** only one last message is saved when listener is disconnected */
	Runnable  lastMessage;
	/** executes messages on UI thread */
	private Handler uiHandler = new Handler(Looper.getMainLooper());		
	private Future<?> task=null;

	public AsyncTask() {
		listedId=idCount.incrementAndGet();// list numbers start with 1; 0 means nor started task
		tasks.put(listedId, this);
	}
	
	/** 
	 * listener become connected and starts to receive messages
	 * @param listener
	 */
	public void setListener(L listener) {
		this.listener=listener;
		if (lastMessage!=null) {
			lastMessage.run();
			lastMessage=null;
		}
	}
	
	/** 
	 * listener become disconnected 
	 */
	public void removeListener() {
		this.setListener(null);
	}

    public L getListener() {
		return listener;
	}

	@SuppressWarnings("unchecked")
	public static <T> T getTask(int id) {
		return (T) tasks.get(id);
	}
	
	public int getListedId() {
		return listedId;
	}
	
	public void postMessage(Runnable message){
		uiHandler.post(message);
	}
	
	/** does actual work and sends messages to the listener */
	protected abstract void doInBackground(Params... params);
	
	/** cancels the task
	 * 
	 * @param mayInterruptIfRunning
	 *    true means do interrupt task's thread
	 */
	public boolean cancel(boolean mayInterruptIfRunning) {
		if (task==null) {
			throw new IllegalStateException("task not started");
		}
		return task.cancel(mayInterruptIfRunning);
	}

	/**
	 * starts execution on the executor
	 * @param params
	 *     parameters for the {@link doInBackground} method
	 */
    public void executeOnExecutor(ExecutorService exec, final Params... params) {
		task=exec.submit(new Runnable(){
			@Override
			public void run() {
				try {
					doInBackground(params);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}			
		});
    }

	/**
	 * starts execution on a dedicated thread
	 * @param params
	 *     parameters for the {@link doInBackground} method
	 */
	public void execute(final Params... params) {
		task=new ThreadFuture(params);
    }
	
	class ThreadFuture implements Future<Void> {
		volatile boolean done=false;
		volatile boolean cancelled=false;
		CountDownLatch doneSignal = new CountDownLatch(1);
		Thread thread;
		
		ThreadFuture(final Params[] params){
			thread = new Thread(new Runnable(){
				@Override
				public void run() {
					try {
						doInBackground(params);
					} catch (Exception e) {
						e.printStackTrace(); // TODO pass to Future
					} finally {
						done=true;
						doneSignal.countDown();
						thread=null;
					}
				}				
			});
			thread.start();
		}

		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			if (done) {
				return false;
			}
			if (mayInterruptIfRunning) {
				thread.interrupt();
				done=true;
				cancelled=true;
				return true;
			}
			return false;
		}

		@Override
		public synchronized boolean isCancelled() {
			return cancelled;
		}

		@Override
		public boolean isDone() {
			return done;
		}

		@Override
		public Void get() throws InterruptedException, ExecutionException {
			doneSignal.await();
			return null;
		}

		@Override
		public Void get(long timeout, TimeUnit unit)
				throws InterruptedException, ExecutionException,
				TimeoutException {
			doneSignal.await(timeout, unit);
			return null;
		}
		
	}
}
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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.SparseArray;

/**
 * This package solves well known problem of {@link android.os.AsyncTask} class,
 * when UI form to display progress and result stops to work after 
 * orientation change or after moving to background.
 * 
 * The solution is to split AsyncTask in 3 objects:
 *  - listener - an UI form that actually shows messages from the task
 *  - connector between the UI form and async task
 *  - the task itself
 *  
 *  The UI form can disconnect and another instance with the same interface
 *  can connect later - this does not affect the task's functionality.
 *  
 *  Another difference is that {@link android.os.AsyncTask} offers strict interface with listener:
 *   <code>
        protected void {@link android.os.AsyncTask#onProgressUpdate(Progress... values)}
        protected void {@link android.os.AsyncTask#onPostExecute(Result result)}
 *   </code>
 *   while AsyncConnector allows define almost any interface. 
 *   
 *   Recommended sequence of development steps is as follows:
 *   - implement desired UI form, determine desired types of messages
 *   to be received from the computational task.
 *   
 *   - declare the resulting interface as separate java interface type
 *   - Implement connector: either using {@link AsyncConnector.UIMessage}
 *     or call to {@link AsyncConnector#makeProxy(Class)
 *  
 *  - Implement method {@link AsyncConnector#doInBackground(Object...)}.
 *  Inside this method, send messages to the UI form via connector.
 *  
 *  - assemble all parts together
 *  </code>
 *   
 * @param <Params> type of parameters for {@link #doInBackground} method
 * @param <L> interface type of UI Listener
 */
public abstract class AsyncConnector<Params, L> {	
	/** tasks are numbered */
	private static final AtomicInteger idCount=new AtomicInteger();
	private static SparseArray<AsyncConnector<?,?>> tasks=new  SparseArray<AsyncConnector<?,?>>();
	private final int listedId;
	private L listener;
	/** only one last message is saved when listener is disconnected */
	Runnable  lastMessage;
	/** executes messages on UI thread */
	private Handler uiHandler = new MessageHandler(Looper.getMainLooper());		
	private Future<?> task=null;

	public AsyncConnector() {
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
	 * Messages are lost, except for latest one
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
            throw new UnsupportedOperationException();
        }

		@Override
		public Void get(long timeout, TimeUnit unit) {
            throw new UnsupportedOperationException();
		}
		
	}
	
	@SuppressWarnings("unchecked")
	public <I> I makeProxy(Class<I> intrf) {
		return (I) Proxy.newProxyInstance(intrf.getClassLoader(),
    	        new Class[] { intrf }, new ConnectorInvocationHandler());
	}
	
    // ============================== message handling on listener's side ======//
	
	/**
	 * handles messages issued by any kind of connectors
	 * invokes listener's methods when it is connected
	 * @author rfq
	 *
	 */
	class MessageHandler extends Handler {

		public MessageHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void dispatchMessage(Message msg) {
	        Runnable callback = msg.getCallback();
			if (callback == null) {
				super.dispatchMessage(msg);
				return;
	        }
			if (listener==null) {
				lastMessage=callback;
				return;
			}
			try {
				callback.run();
			} catch (Throwable e) {
				if (listener instanceof ExceptionHandler) {
					((ExceptionHandler)listener).handleException(e);
				} else {
					e.printStackTrace();
				}
			}
		}
	}

	public abstract class UIMessage implements Runnable {
        @Override
        public void run() {
        	L listener = getListener();
			try {
				act(listener);
			} catch (Throwable e) {
				throwUncheked(e); // to be handled in MessageHandler
			}
        }
    	public void start() {
    		uiHandler.post(this);
    	}
        protected abstract void act(L listener) throws Throwable;
	}
    
	class ConnectorInvocationHandler implements InvocationHandler {

		public Object invoke(Object proxy, final Method method, final Object[] args) throws Throwable {
			uiHandler.post(new Runnable(){
			    @Override
			    public void run() {
				    L listener = getListener();
					try {
						method.invoke(listener, args);
					} catch (InvocationTargetException e) {
						throwUncheked(e.getCause());
					} catch (Error e) {
						throw e;
					} catch (RuntimeException e) {
						throw e;
					} catch (Throwable e) {
						throwUncheked(e); // to be handled in MessageHandler
					}
				}
			
			});
            return null;
	    }
	}
	
	/**
	 * throws checked exception as if it is unchecked
	 * convenient to use instead of explicit throw in proxied methods - otherwise
	 * interface methods should have been declared as throwing exceptionsm which is
	 * false and incovinient for interface side.
	 * taken from http://blog.ragozin.info/2011/10/java-how-to-throw-undeclared-checked.html
	 * @param e
	 */
    public static void throwUncheked(Throwable e) {
    	AsyncConnector.<RuntimeException>throwAny(e);
    }
   
    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void throwAny(Throwable e) throws E {
        throw (E)e;
    }

}
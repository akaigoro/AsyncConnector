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

/** connects AsyncTask and UI Listener.
 *
 *   For each interface method, define field like this:
 *   <code>
 *   	PortHandler<Integer> progressor = new PortHandler<Integer>() {
		@Override
		protected void act(Listener listener, Integer value) {
			listener.publishProgress(value);
		}
	};
</code>
  * then, in the {@link AsyncTask#doInBackground} method, pass message to the listener with
  * <code> progressor.post(99); </code>
  * 
  * 
 * @param <L> type of AsyncListener
 */
public abstract class AsyncConnector<L> {
	protected AsyncTask<?,L> taskBase;
	
	public AsyncConnector(AsyncTask<?, L> taskBase) {
		this.taskBase = taskBase;
	}

	public abstract class PortHandler<M> {

        public final void post(final M m) {
        	taskBase.postMessage(new Runnable(){
                @Override
                public void run() {
       			    L listener = taskBase.getListener();
    				if (listener==null) {
    					taskBase.lastMessage=this;
    					return;
    				}
                    try {
						PortHandler.this.act(listener, m);
					} catch (Throwable e) {
						if (listener instanceof ExceptionHandler) {
							((ExceptionHandler)listener).handleException(e);
						} else {
							e.printStackTrace();
						}
					}            
                }
            });
        }

       protected abstract void act(L listener, M m);
    }

}
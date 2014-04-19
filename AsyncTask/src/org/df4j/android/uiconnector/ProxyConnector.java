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

public class ProxyConnector<I, L> extends AsyncConnector<L>{
    
	public ProxyConnector(AsyncTask<?, L> taskBase) {
		super(taskBase);
	}

	@SuppressWarnings("unchecked")
	public static <I,L> I makeProxy(Class<I> intrf, AsyncTask<?, L> taskBase) {
		return (I) Proxy.newProxyInstance(intrf.getClassLoader(),
    	        new Class[] { intrf }, new ConnectorInvocationHandler<L>(taskBase));
	}
    
	static class ConnectorInvocationHandler<L> implements InvocationHandler {
		AsyncTask<?, L> taskBase;
		
	    public ConnectorInvocationHandler(AsyncTask<?, L> taskBase) {
			this.taskBase = taskBase;
		}

		public Object invoke(Object proxy, final Method method, final Object[] args) throws Throwable {
			taskBase.postMessage(new Runnable(){
				L listener = taskBase.getListener();
                @Override
                public void run() {
       			    L listener = taskBase.getListener();
    				if (listener==null) {
    					taskBase.lastMessage=this;
    					return;
    				}
					try {
						method.invoke(listener, args);
					} catch (IllegalAccessException e) {
						handleException(e);
					} catch (IllegalArgumentException e) {
						handleException(e);
					} catch (InvocationTargetException e) {
						handleException(e.getCause());
					}
                }

				private void handleException(Throwable e) {
					if (listener instanceof ExceptionHandler) {
						((ExceptionHandler)listener).handleException(e);
					} else {
						e.printStackTrace();
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
    	ProxyConnector.<RuntimeException>throwAny(e);
    }
   
    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void throwAny(Throwable e) throws E {
        throw (E)e;
    }
}
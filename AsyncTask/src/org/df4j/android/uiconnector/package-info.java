/**
 * This package solves well known of {@link AsyncTask} class,
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
 *   Recommended sequence of development steps is as follows:
 *   - implement desired UI form, determine desired types of messages
 *   to be received from the computational task.  {@link android.os.AsyncTask} offers strict interface:
 *   <code>
        protected void {@link android.os.AsyncTask#onProgressUpdate(Progress... values)}
        protected void {@link android.os.AsyncTask#onPostExecute(Result result)}
 *   </code>
 *   With this package, programmer can define almost any interface.
 *   
 *   - declare the resulting interface as separate java interface type
 *   - Implement connector: either extend class {@link AsyncConnector},
 *     or call to {@link org.df4j.android.uiconnector.ProxyConnector#makeProxy(Class, AsyncTask)
 *  
 *  - Implement the class for computational task: extend class {@link AsyncTask} and
 *  override method {@link AsyncTask#doInBackground(Object...)}.
 *  Inside this method, send messages to the UI form via connector.
 *  
 *  - assemble all parts together
 *  </code>
 *   
 */
package org.df4j.android.uiconnector;

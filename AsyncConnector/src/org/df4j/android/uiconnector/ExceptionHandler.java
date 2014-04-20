package org.df4j.android.uiconnector;

/** 
 * if listener supports this interface, then exceptions thrown
 * from listener's interface methods redirect to {@linr #handleException(Throwable)},
 * otherwise they only print stack trace.
 * 
 * @author rfq
 *
 */
public interface ExceptionHandler {

	public void handleException(Throwable e);

}

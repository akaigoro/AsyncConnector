/*
 * Copyright 2014 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.df4j.android.log;

import android.util.Log;

public class Logm {
	
	private static void _println(int prio, Object... args) {
		StackTraceElement callereElement = new Throwable().getStackTrace()[2]; 
		String callerClassName = callereElement.getClassName(); 
		String shortCallerClassName=callerClassName.substring(callerClassName.lastIndexOf('.')+1); 
		String msg=callereElement.getMethodName(); //caller Method Name;
		if (args.length>0) {
			StringBuilder sb=new StringBuilder(msg);
			for (Object arg: args) {
				sb.append(':');
				sb.append(arg);
			}
			msg=sb.toString();
		}
		Log.println(prio, shortCallerClassName, msg); 
	}

	public static void println(int prio, Object... args) {
		_println(prio, args); 
	}

	public static void v(Object... args) {
		_println(Log.VERBOSE, args); 
	}

	public static void d(Object... args) {
		_println(Log.DEBUG, args); 
	}

	public static void i(Object... args) {
		_println(Log.INFO, args); 
	}

	public static void w(Object... args) {
		_println(Log.WARN, args); 
	}

	public static void e(Object... args) {
		_println(Log.ERROR, args); 
	}
}

/**
 * 
 */
package org.jocean.event.extend.management.impl;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import org.jocean.event.extend.management.annotation.Indicator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author isdom
 *
 */
public class CountersGetterImpl implements InvocationHandler {
	
    private static final Logger LOG = 
        	LoggerFactory.getLogger(CountersGetterImpl.class);
    
	public CountersGetterImpl(final ConcurrentMap<Object, AtomicLong> counters) {
		this._counters = counters;
	}
	
	public Object invoke(final Object proxy, final Method method, final Object[] args)
			throws Throwable {
        final String methodName = method.getName();
        final Class<?> returnType = method.getReturnType();

        //   An invocation of the hashCode, equals, or toString methods
        // declared in java.lang.Object on a proxy instance will be 
        // encoded and dispatched to the invocation handler's invoke
        // method in the same manner as interface method invocations are
        // encoded and dispatched, as described above. The declaring 
        // class of the Method object passed to invoke will be
        // java.lang.Object. Other public methods of a proxy instance
        // inherited from java.lang.Object are not overridden by a proxy
        // class, so invocations of those methods behave like they do
        // for instances of java.lang.Object.
        if (methodName.equals("hashCode")) {
            return this._counters.hashCode();
        } else if (methodName.equals("equals")) {
            return (proxy == args[0]);
        } else if (methodName.equals("toString")) {
            return "Counters [" + this._counters.toString() + "]";
        }
        
        /* Inexplicably, InvocationHandler specifies that args is null
           when the _method takes no arguments rather than a
           zero-length array.  */
        final int nargs = (args == null) ? 0 : args.length;

        if (methodName.startsWith("get")
            && methodName.length() > 3
            && nargs == 0
            && (returnType.equals(long.class)
            	|| returnType.equals(Long.class) ) ) {
        	
        	final String key;
        	
        	final Indicator indicator = method.getAnnotation(Indicator.class);
        	if ( null != indicator ) {
        		key = indicator.key();
        	}
        	else {
        		key = methodName.substring(3);
        	}
        	
			final AtomicLong ret = this._counters.get(key);
			return	(null != ret ? ret.get() : 0);
        }

        LOG.warn("Invalid _method {} while valid is : getXXX"
        		+"\r\n Or invalid parameters count {}", 
        		methodName, nargs );
		return 0;
	}
	
	private	final ConcurrentMap<Object, AtomicLong> _counters;
	
}

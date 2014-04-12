/**
 * 
 */
package org.jocean.event.extend.management.impl;

import java.util.concurrent.atomic.AtomicLong;

import org.jocean.event.extend.management.DealCounterMXBean;


/**
 * @author isdom
 *
 */
public class DealCounterImpl implements DealCounterMXBean {

	public DealCounterImpl(final AtomicLong al) {
		_counter = al;
	}
	
	@Override
	public long getDealCount() {
		return	_counter.get();
	}

	final AtomicLong _counter;
}

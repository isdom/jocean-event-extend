/**
 * 
 */
package org.jocean.event.extend.executor;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.jocean.event.extend.management.TimerMXBean;
import org.jocean.idiom.ExceptionUtils;
import org.jocean.idiom.ObservationDestroyable;
import org.jocean.idiom.ObservationDestroyableSupport;
import org.jocean.j2se.jmx.MBeanRegisterSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author isdom
 *
 */
public class TimerServiceByScheduleExecutor 
	implements TimerService, ObservationDestroyable, TimerMXBean {

    private static final Logger LOG = 
        	LoggerFactory.getLogger(TimerServiceByScheduleExecutor.class);
    
	public TimerServiceByScheduleExecutor(final String name) {
		this.name = name;
		
		this.innerService = 
				new ScheduledThreadPoolExecutor(1, 
					new ThreadFactory(){
						public Thread newThread(Runnable r) {
							return new Thread(r, "timer-" + name);
						}});
	    	
		this._mbeanSupport =
				new MBeanRegisterSupport("org.jocean:type=timer", null);
		
		this._mbeanSupport.registerMBean("name=" + name, this);
		
		rescheduleCheckTimeoutEvent();
		
	}
	
    //	implements TimerMXBean
	@Override
	public int getScheduledTaskCount() {
		return	this.timeoutHolder.size();
	}

	@Override
	public int getServiceTaskCount() {
		return	this.innerService.getQueue().size();
	}

	@Override
	public boolean destroy() {
		if ( _destroySupport.destroy() ) {
			this.innerService.shutdownNow();
			this._mbeanSupport.destroy();
			return	true;
		}
		else {
			return	false;
		}
	}

	@Override
	public boolean isDestroyed() {
		return this._destroySupport.isDestroyed();
	}

	@Override
	public void registerOnDestroyedListener(final ObservationDestroyable.Listener listener) {
		this._destroySupport.registerOnDestroyedListener(listener);
	}

	@Override
	public void unregisterOnDestroyedListener(final ObservationDestroyable.Listener listener) {
		this._destroySupport.unregisterOnDestroyedListener(listener);
	}
	
	public long getIntervalOfCheckTimeoutEvent() {
		return intervalOfCheckTimeoutEvent;
	}
	
	/**
	 * @param intervalOfCheckTimeoutEvent the intervalOfCheckTimeoutEvent to set
	 */
	public synchronized void setIntervalOfCheckTimeoutEvent(
			long intervalOfCheckTimeoutEvent) {
		this.intervalOfCheckTimeoutEvent = intervalOfCheckTimeoutEvent;
		this.rescheduleCheckTimeoutEvent();
	}
	
	/* (non-Javadoc)
	 * @see org.jocean.event.extend.TimerService#schedule(java.lang.Runnable, long)
	 */
	@Override
	public Callable<Boolean> schedule(final Runnable task, final long delay) {
		this.innerService.submit(new Runnable(){

			public void run() {
				doScheduleTimeout(task, delay);
			}});
		
		return new Callable<Boolean>(){

			public Boolean call() {
				try {
					return innerService.submit(new Callable<Boolean>(){

						public Boolean call() {
							return doCancelTimeout(task);
						}}).get();
				} catch (Exception e) {
				    LOG.warn("exception when innerService.submit, detail:{}", 
				            ExceptionUtils.exception2detail(e));
				}
				return false;
			}};
	}

	//	in timerExecService thread
	private void doScheduleTimeout(final Runnable task, final long delay) {
		timeoutHolder.put(task, System.currentTimeMillis() + delay);
	}
	
	//	in timerExecService thread
	private boolean doCancelTimeout(final Runnable task) {
		if ( null == timeoutHolder.remove(task) ) {
			if ( LOG.isWarnEnabled() ) {
				LOG.warn("Can not found matched timeout task {}, maybe has been emited.", task);
			}
			return false;
		}
		else {
			if ( LOG.isDebugEnabled() ) {
				LOG.debug("cancel timeout task {} succeed.", task);
			}
			return true;
		}
	}
	
	private	void rescheduleCheckTimeoutEvent() {
		try {
			if ( null != futureOfCheckTimeoutEvent ) {
				futureOfCheckTimeoutEvent.cancel(true);
			}
			futureOfCheckTimeoutEvent = this.innerService.scheduleWithFixedDelay(
					new Runnable(){
						public void run() {
							doCheckTimeoutEvents();
						}}, 
				this.intervalOfCheckTimeoutEvent, 
				this.intervalOfCheckTimeoutEvent, 
				TimeUnit.MILLISECONDS);
		}
		catch (Exception e) {
			LOG.error("rescheduleCheckTimeoutEvent:", e);
		}
	}
	
	//	in timerExecService thread
	private void doCheckTimeoutEvents() {
		final long now = System.currentTimeMillis();
		
		try {
			final Set<Map.Entry<Runnable, Long>> entrySet = timeoutHolder.entrySet();
			
			final Iterator<Map.Entry<Runnable, Long>> iter = entrySet.iterator();
			while ( iter.hasNext() ) {
				final Map.Entry<Runnable, Long> entry = iter.next();
				if ( entry.getValue() <= now ) {
					//	condition matched, emit timeout event
					iter.remove();
					//	TODO ? fix timeout event
					try {
						entry.getKey().run();
					}
					catch (Exception e) {
						LOG.error("exception when run timer task: {}", 
								ExceptionUtils.exception2detail(e));
					}
				}
			}
		}
		catch (Exception e) {
			LOG.error("doCheckTimeoutEvents:", e);
		}
	}
	
	public String getName() {
		return name;
	}
	
	@Override
	public String toString() {
		return "TimerServiceByScheduleExecutor [name=" + name + "]";
	}


	private	final String name;
	
    //	JMX support
    private	final MBeanRegisterSupport 	_mbeanSupport;
    
    private final ObservationDestroyableSupport _destroySupport = 
    		new ObservationDestroyableSupport(this);
	
	private final ScheduledThreadPoolExecutor innerService;
	
    private	final Map<Runnable, Long>	timeoutHolder = 
        	new HashMap<Runnable, Long>();
    
    //	50L ms
    private	long				intervalOfCheckTimeoutEvent = 50L;
    private	ScheduledFuture<?>	futureOfCheckTimeoutEvent = null;

}

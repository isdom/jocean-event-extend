package org.jocean.seda.executor;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.jocean.idiom.ObservationDestroyable;
import org.jocean.idiom.ObservationDestroyableSupport;
import org.jocean.j2se.MBeanRegisterSupport;
import org.jocean.seda.common.FlowCountListener;
import org.jocean.seda.common.FlowCounter;
import org.jocean.seda.common.FlowCounterAware;
import org.jocean.seda.management.ExecutorMXBean;
import org.jocean.seda.management.FlowRunnerMXBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class FlowBasedExecutorSource 
	implements ExecutorSource, FlowCounterAware, 
	        ObservationDestroyable, ExecutorMXBean {

    private final class FlowThread extends Thread 
        implements ThreadOfExecutorService {

        public FlowThread(ThreadGroup group, Runnable target, String name,
                long stackSize) {
            super(group, target, name, stackSize);
        }

        @Override
        public ExecutorService selfExecutorService() {
            return FlowBasedExecutorSource.this._executor;
        }
    }

    private static final Logger LOG = 
        	LoggerFactory.getLogger(FlowBasedExecutorSource.class);
    
	private static final int DEFAULT_SIZELIMIT = 100;

    private static final AtomicInteger THREAD_POOL_ID = new AtomicInteger(1);
    
    private class DefaultThreadFactory implements ThreadFactory {
        final ThreadGroup group;
        final AtomicInteger threadNumber = new AtomicInteger(1);
        final String namePrefix;

        DefaultThreadFactory(final String name) {
            SecurityManager s = System.getSecurityManager();
            group = (s != null)? s.getThreadGroup() :
                                 Thread.currentThread().getThreadGroup();
            namePrefix = (null != name ? name : "pool") 
            			+ "-flowexecutor-"
                        + THREAD_POOL_ID.getAndIncrement() 
                        + "-thread-";
        }

        public Thread newThread(final Runnable r) {
            Thread t = new FlowThread(group, r,
                                  namePrefix + threadNumber.getAndIncrement(),
                                  0);
            if (t.isDaemon())
                t.setDaemon(false);
            if (t.getPriority() != Thread.NORM_PRIORITY)
                t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
    }
    
	private final FlowCountListener _listener = new FlowCountListener() {

		@Override
		public void onActiveFlowCountIncrement(final FlowCounter flowCounter,
				int newActiveFlowCount) {
			doWhenActiveFlowCountIncrement(flowCounter.getRunner());
		}

		@Override
		public void onActiveFlowCountDecrement(final FlowCounter flowCounter,
				int newActiveFlowCount) {
			doWhenActiveFlowCountDecrement(flowCounter.getRunner());
		}};

	private void doWhenActiveFlowCountIncrement(final FlowRunnerMXBean flowRunner) {
		updateActiveFlowCountOf(flowRunner);
		enlargePoolSize(calculateTotalActiveFlowCount());
	}

	private void doWhenActiveFlowCountDecrement(final FlowRunnerMXBean flowRunner) {
		updateActiveFlowCountOf(flowRunner);
	}
	
	private void updateActiveFlowCountOf(final FlowRunnerMXBean flowRunner) {
		final int id = flowRunner.getId();
		final Map<Integer, AtomicInteger> counters = this._activeFlowCounts.get();
		
		final AtomicInteger counter = counters.get(id);
		if ( null != counter ) {
			counter.set(flowRunner.getFlowActiveCount());
		}
	}

    private int calculateTotalActiveFlowCount() {
    	int ret = 0;
    	final Map<Integer, AtomicInteger> counters = _activeFlowCounts.get();
    	final Collection<AtomicInteger> values = counters.values();
    	
    	for (AtomicInteger counter : values ) {
    		ret += counter.get();
    	}
    	return ret;
    }
    
	public FlowBasedExecutorSource(final String name) {
		final DefaultThreadFactory dtf = new DefaultThreadFactory(name);
		
		this._name = name;
		this._executor = new ThreadPoolExecutor(0, DEFAULT_SIZELIMIT, 
                1L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(), 
                dtf);
		
	    _timerService =
	    		new ScheduledThreadPoolExecutor(1, 
	    			new ThreadFactory(){
	    				public Thread newThread(Runnable r) {
	    					return new Thread(r, 
	    							dtf.namePrefix + "timer");
	    				}});
		
		rescheduleAdjustThreadPoolCoreSize();
		
		this._mbeanSupport =
				new MBeanRegisterSupport("org.jocean:type=executor", null);
		
		this._mbeanSupport.registerMBean("name=" + name, this);
	}

	@Override
	public int getExecutorActiveThreadCount() {
		return this._executor.getActiveCount();
	}

	@Override
	public long getExecutorCompletedTaskCount() {
		return this._executor.getCompletedTaskCount();
	}

	@Override
	public int getExecutorCorePoolSize() {
		return this._executor.getCorePoolSize();
	}

	@Override
	public int getExecutorCurrentPoolSize() {
		return this._executor.getPoolSize();
	}

	@Override
	public long getExecutorHandledTaskCount() {
		return this._executor.getTaskCount();
	}

	@Override
	public int getExecutorLargestPoolSize() {
		return this._executor.getLargestPoolSize();
	}

	@Override
	public int getExecutorMaximumPoolSize() {
		return this._executor.getMaximumPoolSize();
	}

	@Override
	public int getExecutorPendingTaskCount() {
    	if ( null != this._executor ) {
    		return	this._executor.getQueue().size();
    	}
    	return	-1;
	}
	
	public String getName() {
		return	this._name;
	}
	
	@Override
	public ExecutorService getExecutor() {
		return this._executor;
	}
	
	@Override
	public boolean destroy() {
		if ( this._destroySupport.destroy() ) {
			this._timerService.shutdownNow();
			this._executor.shutdownNow();
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

	//	implements FlowCounterAware
	@Override
	public void registerFlowCounter(final FlowCounter flowCounter) {
		if ( this._flowCounters.add(flowCounter) ) {
			
			synchronized(this._activeFlowCounts) {
				// copy on write change policy
				final Map<Integer, AtomicInteger> current = this._activeFlowCounts.get();
				if ( current.containsKey(flowCounter.getRunner().getId())) {
					LOG.warn("FlowCounter's id [{}] duplicated, ignore register counter", 
							flowCounter.getRunner().getId());
					return;
				}
				
				final Map<Integer, AtomicInteger> newCounts = 
						new HashMap<Integer, AtomicInteger>(current);
				
				newCounts.put(flowCounter.getRunner().getId(), 
						new AtomicInteger(flowCounter.getRunner().getFlowActiveCount()));
				this._activeFlowCounts.set(newCounts);
			}

			//	enable spec flow counter active count monitor
			flowCounter.registerFlowCountListener(this._listener);
			
		}
	}

	@Override
	public void unregisterFlowCounter(final FlowCounter flowCounter) {
		if ( this._flowCounters.remove(flowCounter) ) {
			flowCounter.unregisterFlowCountListener(this._listener);
			
			synchronized(this._activeFlowCounts) {
				// copy on write change policy
				final Map<Integer, AtomicInteger> current = this._activeFlowCounts.get();
				if ( !current.containsKey(flowCounter.getRunner().getId())) {
					LOG.warn("FlowCounter's id [{}] lost, ignore unregister counter", 
							flowCounter.getRunner().getId());
					return;
				}
				
				final Map<Integer, AtomicInteger> newCounts = 
						new HashMap<Integer, AtomicInteger>(current);
				
				newCounts.remove(flowCounter.getRunner().getId());
				this._activeFlowCounts.set(newCounts);
			}
		}
	}
	
	/**
	 * @return the intervalOfAdjustThreadPoolCoreSize
	 */
	public long getIntervalOfAdjustExecutorCorePoolSize() {
		return _intervalOfAdjustExecutorCorePoolSize;
	}

	/**
	 * @param intervalOfAdjustThreadPoolCoreSize the intervalOfAdjustThreadPoolCoreSize to set
	 */
	public synchronized void setIntervalOfAdjustExecutorCorePoolSize(
			final long intervalOfAdjustThreadPoolCoreSize) {
		this._intervalOfAdjustExecutorCorePoolSize = intervalOfAdjustThreadPoolCoreSize;
		this.rescheduleAdjustThreadPoolCoreSize();
	}
	
	public int getThreadPoolSizeLimit() {
		return _threadPoolSizeLimit;
	}

	public void setThreadPoolSizeLimit(final int poolSize) {
		if ( poolSize > 0 ) {
			this._threadPoolSizeLimit = poolSize;
			this.reducePoolSize(poolSize);
			this._executor.setMaximumPoolSize(poolSize);
		}
	}
	
	public int getPendingTaskCount() {
    	return	this._executor.getQueue().size();
	}
	
    private void enlargePoolSize(final int currentPoolSize) {
		if ( currentPoolSize > this._executor.getCorePoolSize() ) {
			synchronized (this) {
				//	double check for more effective
				if ( currentPoolSize > this._executor.getCorePoolSize() ) {
					this._executor.setCorePoolSize(currentPoolSize);
				}
			}
		}
    }
    
    
    private void reducePoolSize(final int currentPoolSize) {
		if ( currentPoolSize < this._executor.getCorePoolSize() ) {
			synchronized (this) {
				if ( currentPoolSize < this._executor.getCorePoolSize() ) {
					this._executor.setCorePoolSize(currentPoolSize);
				}
			}
		}
    }
    
	private	void rescheduleAdjustThreadPoolCoreSize() {
		try {
			if ( null != this._futureOfAdjustExecutorCorePoolSize ) {
				this._futureOfAdjustExecutorCorePoolSize.cancel(true);
			}
			this._futureOfAdjustExecutorCorePoolSize = this._timerService.scheduleWithFixedDelay(
					new Runnable(){
						public void run() {
							resetLargestActiveJobCount();
						}}, 
					this._intervalOfAdjustExecutorCorePoolSize, 
					this._intervalOfAdjustExecutorCorePoolSize, 
					TimeUnit.MILLISECONDS);
		}
		catch (Exception e) {
			LOG.error("rescheduleAdjustThreadPoolCoreSize:", e);
		}
	}
	
    private void resetLargestActiveJobCount() {
    	try {
	    	this._periodLargestActiveFlowCount.set(calculateTotalActiveFlowCount());
	    	reducePoolSize( this._periodLargestActiveFlowCount.get());
    	}
    	catch (Exception e) {
    		LOG.error("resetLargestActiveJobCount:", e);
    	}
	}
    
	private final String 			_name;
    private final ThreadPoolExecutor _executor;
    private final ScheduledThreadPoolExecutor _timerService;
    
    //	JMX support
    private	final MBeanRegisterSupport 	_mbeanSupport;
    
    private final ObservationDestroyableSupport _destroySupport = 
    		new ObservationDestroyableSupport(this);
    
    private final Set<FlowCounter> _flowCounters = 
    		new ConcurrentSkipListSet<FlowCounter>();
    
    private final AtomicReference<Map<Integer, AtomicInteger>> _activeFlowCounts = 
    		new AtomicReference<Map<Integer, AtomicInteger>>(new HashMap<Integer, AtomicInteger>());
    
	private	final AtomicInteger	_periodLargestActiveFlowCount = new AtomicInteger(0);
	
    //	1L s
    private	long				_intervalOfAdjustExecutorCorePoolSize = 1 * 1000L;
    private	ScheduledFuture<?>	_futureOfAdjustExecutorCorePoolSize = null;
	private	volatile int _threadPoolSizeLimit = DEFAULT_SIZELIMIT;

}

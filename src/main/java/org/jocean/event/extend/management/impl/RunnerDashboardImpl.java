/**
 * 
 */
package org.jocean.event.extend.management.impl;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

import org.jocean.event.api.internal.EventHandler;
import org.jocean.event.core.FlowContext;
import org.jocean.event.core.FlowStateChangeListener;
import org.jocean.event.core.FlowTracker;
import org.jocean.event.extend.common.EventDrivenFlowRunner;
import org.jocean.event.extend.management.RunnerDashboardMXBean;
import org.jocean.event.extend.management.annotation.IndicateInterface;
import org.jocean.j2se.MBeanRegisterSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;


/**
 * @author isdom
 *
 */
public class RunnerDashboardImpl
	implements RunnerDashboardMXBean {
	
	@Override
	public long getDealAverageTimeToActive() {
		final long completed = _runner.getDealCompletedCount();
		return completed > 0 ? dealTotalTimeToActive.get() / completed : -1;
	}

	@Override
	public long getDealAverageTimeToLive() {
		final long completed = _runner.getDealCompletedCount();
		return completed > 0 ? dealTotalTimeToLive.get() / completed : -1;
	}

	@Override
	public long getDealBypassCount() {
		return this._runner.getDealBypassCount();
	}

	@Override
	public long getDealCompletedCount() {
		return this._runner.getDealCompletedCount();
	}

	@Override
	public long getDealHandledCount() {
		return this._runner.getDealHandledCount();
	}

	@Override
	public long getDealLargestTimeToActive() {
		return dealLargestTimeToActive;
	}

	@Override
	public long getDealLargestTimeToLive() {
		return dealLargestTimeToLive;
	}

	@Override
	public long getDealSmallestTimeToActive() {
		return dealSmallestTimeToActive;
	}

	@Override
	public long getDealSmallestTimeToLive() {
		return dealSmallestTimeToLive;
	}

	@Override
	public int getFlowActiveCount() {
		return	this._runner.getFlowActiveCount();
	}

	@Override
	public int getFlowInactiveCount() {
		return getFlowTotalCount() - getFlowActiveCount();
	}
	
	@Override
	public int getFlowTotalCount() {
		return	this._runner.getFlowTotalCount();
	}

	/*
	@Override
	public int getJobLargestActiveCount() {
    	return	largestActiveJobCount.get();
	}

	@Override
	public int getJobPeriodLargestActiveCount() {
    	return	periodLargestActiveJobCount.get();
	}

	@Override
	public int getJobTimeoutEventCount() {
		return	timeoutHolder.size();
	}
	
	@Override
	public String[] getJobsDetail() {
		final Set<Map.Entry<ID, CTX>> ctxs = contextHolder.entrySet();
		
		final List<String> ret = new ArrayList<String>();
		final SimpleDateFormat formater = new SimpleDateFormat("yyMMdd-HH:mm:ss.SSS");
		
		for ( Map.Entry<ID, CTX> entry : ctxs ) {
			final StringBuilder sb = new StringBuilder();
			
			sb.append(entry.getKey().toString());
			sb.append(":[");
			
			CTX job = entry.getValue();
			sb.append(job.getCurrentState());
			sb.append("]create:");
			sb.append(formater.format( new Date(job.getCreateTime())) );
			sb.append("/last:");
			sb.append(formater.format( new Date(job.getLastModify())) );
			ret.add(sb.toString());
		}
		
		return	ret.toArray(new String[0]);
	}

	@Override
	public int getInnerServiceTaskCount() {
		if ( null != innerService ) {
			return	innerService.getQueue().size();
		}
		return	-1;
	}
	*/

	@Override
	public void resetDealTimeExtremum() {
	    dealLargestTimeToLive = -1;
	    dealLargestTimeToActive = -1;
	    dealSmallestTimeToLive = Integer.MAX_VALUE;
	    dealSmallestTimeToActive = Integer.MAX_VALUE;
	}

	@Override
	public String getDealTimeCountAsString() {
		try {
			return	innerService.submit(new Callable<String>() {
				public String call() throws Exception {
					return doGetDealTimeCountAsString();
				}}).get();
		} catch (Exception e) {
			LOG.error("Dashboard.getDealTimeCountAsString:", e);
			return	null;
		}
	}

	@Override
	public void resetDealTimeCounter() {
		try {
			innerService.submit(new Runnable() {
				public void run() {
					doResetDealTimeCounter();
				}}).get();
		} catch (Exception e) {
			LOG.error("Dashboard.resetDealTimeCounter:", e);
		}
	}

	@Override
	public String getDealEndReasonCountAsString() {
		try {
			return	innerService.submit(new Callable<String>() {
				public String call() throws Exception {
					return doGetDealEndReasonCountAsString();
				}}).get();
		} catch (Exception e) {
			LOG.error("Dashboard.getDealEndReasonCountAsString:", e);
			return	null;
		}
	}

	@Override
	public void resetDealEndReasonCounter() {
		try {
			innerService.submit(new Runnable() {
				public void run() {
					doResetDealEndReasonCounter();
				}}).get();
		} catch (Exception e) {
			LOG.error("Dashboard.resetDealEndReasonCounter:", e);
		}
	}
	
	private String doGetDealTimeCountAsString() {
		StringBuilder sb = new StringBuilder();
		
		sb.append("timeToLive:\r\n");
//		for (Map.Entry<Object, AtomicLong> entry : dealTimeToLiveCounter.entrySet()) {
//			sb.append(entry.getKey());
//			sb.append(":\t");
//			sb.append(entry.getValue().get());
//			sb.append(" \r\n");
//		}
//		
//		sb.append("timeToActive:\r\n");
//		for (Map.Entry<Object, AtomicLong> entry : dealTimeToActiveCounter.entrySet()) {
//			sb.append(entry.getKey());
//			sb.append(":\t");
//			sb.append(entry.getValue().get());
//			sb.append(" \r\n");
//		}
		
		return	sb.toString();
	}
	
	private void doResetDealTimeCounter() {
//		dealTimeToLiveCounter.clear();
//		dealTimeToActiveCounter.clear();
		//	TODO
		//	clear endreason counter
	}

	private String doGetDealEndReasonCountAsString() {
		final StringBuilder sb = new StringBuilder();
		
		for (Map.Entry<Object, AtomicLong> entry : erCounters.entrySet()) {
			sb.append(entry.getKey());
			sb.append(":\t");
			sb.append(entry.getValue().get());
			sb.append(" \r\n");
		}
		
		return	sb.toString();
	}
	
	private void doResetDealEndReasonCounter() {
//		erCounters.clear();
	}
	
    private static final Logger LOG = 
        	LoggerFactory.getLogger(RunnerDashboardImpl.class);
    
	private final FlowStateChangeListener myListener = new FlowStateChangeListener() {
        @Override
        public void beforeFlowChangeTo(FlowContext ctx,
                EventHandler nextHandler, String causeEvent, Object[] causeArgs)
                throws Exception {
            updateStateAwareStatistics(ctx);
        }

        @Override
        public void afterFlowDestroy(FlowContext ctx) throws Exception {
            updateAllDealStatistics(ctx);
        }
		
	};
	
	public RunnerDashboardImpl(final EventDrivenFlowRunner runner) {
		this._runner = runner;
		
		this.innerService = 
				new ScheduledThreadPoolExecutor(1, 
					new ThreadFactory(){
						public Thread newThread(Runnable r) {
							return new Thread(r, 
								RunnerDashboardImpl.this._runner.getName() + "-dashboard-thrd");
						}});
		
    	this._mbeanSupport = 
        		new MBeanRegisterSupport(runner.getObjectNamePrefix(), null);
		
		final Function<Long,String> trans = new MillisecondsToText10ms_100ms_500ms_1s_5s();
		
		this.transformerOfTTL = trans;
		this.transformerOfTTA = trans;

		this._mbeanSupport.registerMBean("module=dashboard", this);
		
		this._runner.getExtend(FlowTracker.class)
		    .registerFlowStateChangeListener(myListener);
	}

	public void destroy() {
		this._runner.getExtend(FlowTracker.class)
		    .unregisterFlowStateChangeListener(myListener);
		
		this.innerService.shutdownNow();
		
		this._mbeanSupport.destroy();
	}
	
	/**
	 * @return the transformerOfTimeToLive
	 */
	public Function<Long,String> getTransformerOfTimeToLive() {
		return transformerOfTTL;
	}

	/**
	 * @param transformerOfTimeToLive the transformerOfTimeToLive to set
	 */
	public void setTransformerOfTimeToLive(final Function<Long,String> transformerOfTimeToLive) {
		this.transformerOfTTL = transformerOfTimeToLive;
	}
	
	/**
	 * @return the transformerOfTimeToActive
	 */
	public Function<Long,String> getTransformerOfTimeToActive() {
		return transformerOfTTA;
	}

	/**
	 * @param transformerOfTimeToActive the transformerOfTimeToActive to set
	 */
	public void setTransformerOfTimeToActive(Function<Long,String> transformerOfTimeToActive) {
		this.transformerOfTTA = transformerOfTimeToActive;
	}
	
	private void updateStateAwareStatistics(final FlowContext ctx) {
		final String currentStateName = ctx.getCurrentHandler().getName();
		final long ttl = System.currentTimeMillis() - 
				ctx.getLastModify();
		
		innerService.submit(new Runnable() {
			public void run() {
				countTimeForStateAware(transformerOfTTL, currentStateName, ttl);
			}});
	}

    private void countTimeForStateAware(
    		final Function<Long,String> defaultTransformer, 
    		final String stateName, final long ttl) {
        ERTuple tuple = erTuplesOfStateAwareTTL.get(stateName);
        if (tuple == null) {
            tuple = createAndRegisterEndReasonTuple(defaultTransformer, 
            		"stateAwareTTL,state="+stateName, "ttl");
            erTuplesOfStateAwareTTL.put(stateName, tuple);
        }

        final Function<Long,String> trans = tuple.trans;
        if (null != trans) {
            final Object key = trans.apply(ttl);
            if (null != key) {
                AtomicLong count = tuple.counter.get(key);
                if (null == count) {
                    count = new AtomicLong(0);
                    tuple.counter.put(key, count);
                }
                count.incrementAndGet();
            }
        }
    }
    
	private void updateAllDealStatistics(final FlowContext ctx) {
		final long timeToLive = ctx.getTimeToLive();
		final long timeToActive = ctx.getTimeToActive();
		
		if ( timeToLive > this.dealLargestTimeToLive ) {
			this.dealLargestTimeToLive = timeToLive;
		}
		if ( timeToLive < this.dealSmallestTimeToLive ) {
			this.dealSmallestTimeToLive = timeToLive;
		}
		
		if ( timeToActive > this.dealLargestTimeToActive ) {
			this.dealLargestTimeToActive = timeToActive;
		}
		if ( timeToActive < this.dealSmallestTimeToActive ) {
			this.dealSmallestTimeToActive = timeToActive;
		}
		
		dealTotalTimeToLive.addAndGet(timeToLive);
		dealTotalTimeToActive.addAndGet(timeToActive);
		final Object reason = ctx.getEndReason();

		innerService.submit(new Runnable() {
			public void run() {
				doCountDeal(reason, timeToLive, timeToActive);
			}});
	}
	
	private void doCountDeal(Object reason, final long timeToLive, final long timeToActive) {
		
		if ( null == reason ) {
			reason = "";
		}
		
		countDealForEndReason(reason);
		
		//	ttl 
		countTimeForEndReason(
				timeToLive,
				reason, 
				transformerOfTTL, 
				erTuplesOfTTL, 
				"ttl");

		// 	tta
		countTimeForEndReason(
				timeToActive,
				reason, 
				transformerOfTTA, 
				erTuplesOfTTA, 
				"tta");
	}
	
	private AtomicLong createAndRegisterEndReasonCounter(final Object reason) {
		final AtomicLong al = new AtomicLong(0);
		this._mbeanSupport.registerMBean(
				createObjectNameForDeal(reason, "category=all"), 
				new DealCounterImpl(al) );
		
		return	al;
	}
	
	private void countDealForEndReason(final Object reason) {
		final String key = reason.toString();
		AtomicLong counter = erCounters.get(key);
		if ( null == counter ) {
			counter = createAndRegisterEndReasonCounter(reason);
			erCounters.put(key, counter);
		}
		counter.incrementAndGet();
	}
	
    private String createObjectNameForDeal(final Object reason, final String suffix) {
    	return	"module=deal,result=" + reason.toString() + "," + suffix;
    }

	private ERTuple createAndRegisterEndReasonTuple(
			final Function<Long,String> transformer, final Object reason, final String category) {
		final ERTuple tuple = new ERTuple(transformer);
		
		final Class<?> mbeanIntf = getIndicateType(tuple.trans.getClass());
		if ( null != mbeanIntf ) {
			this._mbeanSupport.registerMBean(
					createObjectNameForDeal(reason, "category=" + category), 
					Proxy.newProxyInstance(
							mbeanIntf.getClassLoader(), 
							new Class<?>[]{mbeanIntf}, 
							new CountersGetterImpl( tuple.counter ) ) );
		}
		else {
			LOG.error("can't get IndicateType for [" + tuple.trans.getClass() + "]");
		}
		
		return	tuple;
	}
	
	private static Class<?> getIndicateType(final Class<?> cls) {
		final IndicateInterface anno = cls.getAnnotation(IndicateInterface.class);
		if ( null != anno ) {
			return	anno.type();
		}
		
		return	null;
	}
	
	private void countTimeForEndReason(
			final long 			value,
			final Object 		reason, 
			final Function<Long,String>	defaultTransformer,				
			final Map<String, ERTuple> tuples, 
			final String		category) {
		
		final String	keyOfTuple = reason.toString();
		ERTuple tuple = tuples.get(keyOfTuple);
		
		if ( null == tuple ) {
			tuple = createAndRegisterEndReasonTuple(defaultTransformer, reason, category);
			tuples.put(keyOfTuple, tuple);
		}
		
		final Function<Long,String>	trans = tuple.trans;
		if ( null != trans ) {
			final Object key = trans.apply(value);
			if ( null != key ) {
				AtomicLong count = tuple.counter.get(key);
				if ( null == count ) {
					count = new AtomicLong(0);
					tuple.counter.put(key, count);
				}
				count.incrementAndGet();
			}
		}
	}
	
	private final EventDrivenFlowRunner _runner;
	private final ScheduledThreadPoolExecutor innerService;
	
    //	JMX support
    private	final MBeanRegisterSupport 	_mbeanSupport;
    
    private	volatile long		dealLargestTimeToLive = -1;
    private	volatile long		dealLargestTimeToActive = -1;
    private	volatile long		dealSmallestTimeToLive = Integer.MAX_VALUE;
    private	volatile long		dealSmallestTimeToActive = Integer.MAX_VALUE;
    private	final AtomicLong	dealTotalTimeToLive = new AtomicLong(0);
    private	final AtomicLong	dealTotalTimeToActive = new AtomicLong(0);

    //	deal statistics value
    private	volatile Function<Long,String>	transformerOfTTL = null;
    private	volatile Function<Long,String>	transformerOfTTA = null;

    private	final Map<Object, AtomicLong>	erCounters = 
    	new HashMap<Object, AtomicLong>();
    
    private static class ERTuple {
    	final Function<Long,String> trans;
    	final ConcurrentMap<Object, AtomicLong>	counter = 
    			new ConcurrentHashMap<Object, AtomicLong>();
    	
    	ERTuple(final Function<Long,String> trans) {
    		this.trans = trans;
    	}
    }
    
    private	final Map<String, ERTuple>	erTuplesOfTTL = 
    	new HashMap<String, ERTuple>();
    
    private	final Map<String, ERTuple>	erTuplesOfTTA = 
    	new HashMap<String, ERTuple>();
    
    private final Map<String, ERTuple> erTuplesOfStateAwareTTL = 
    		new HashMap<String, ERTuple>();
}

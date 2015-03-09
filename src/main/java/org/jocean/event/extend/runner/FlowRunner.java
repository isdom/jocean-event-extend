/**
 * 
 */
package org.jocean.event.extend.runner;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.jocean.event.api.EventEngine;
import org.jocean.event.api.EventReceiver;
import org.jocean.event.api.FlowLifecycleListener;
import org.jocean.event.api.FlowStateChangedListener;
import org.jocean.event.api.internal.EventHandler;
import org.jocean.event.api.internal.Eventable;
import org.jocean.event.core.FlowContext;
import org.jocean.event.core.FlowContext.ReactorBuilder;
import org.jocean.event.core.FlowContextImpl;
import org.jocean.event.extend.common.EventDrivenFlowRunner;
import org.jocean.event.extend.common.FlowCountListener;
import org.jocean.event.extend.common.FlowCounter;
import org.jocean.event.extend.common.FlowCounterAware;
import org.jocean.event.extend.executor.ExecutorSource;
import org.jocean.event.extend.executor.ThreadOfExecutorService;
import org.jocean.event.extend.executor.TimerService;
import org.jocean.event.extend.management.FlowRunnerMXBean;
import org.jocean.idiom.COWCompositeSupport;
import org.jocean.idiom.Detachable;
import org.jocean.idiom.ExceptionUtils;
import org.jocean.idiom.ExectionLoop;
import org.jocean.idiom.InterfaceUtils;
import org.jocean.idiom.ObservationDestroyable;
import org.jocean.idiom.ObservationDestroyableSupport;
import org.jocean.idiom.Visitor;
import org.jocean.j2se.jmx.MBeanRegisterSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * @author hp
 *
 */
public class FlowRunner implements EventDrivenFlowRunner {
	
    private static final Logger LOG = 
    	LoggerFactory.getLogger(FlowRunner.class);

    private static final AtomicInteger allRunnerCounter = new AtomicInteger(0);
    
	public FlowRunner(final String name, final String onPrefix, final TimerService  timerService) {
    	this._name = ( null != name ? name : super.toString() );	// ensure this.name is not null
    	this._id = allRunnerCounter.incrementAndGet();
    	this._timerService = timerService;
    	
    	this._mbeanSupport = 
    		new MBeanRegisterSupport( (null == onPrefix ) 
					? "org.jocean:type=flows,name=" + this._name 
					: onPrefix + ",name=" + this._name ,
				null );
    	
        //	register mbean of self
		this._mbeanSupport.registerMBean("module=runner", this);
    }

	public EventEngine buildEventEngine() {
		return	new EventEngine() {

			@Override
			public EventReceiver create(
					final String name,
					final EventHandler init,
					final Object... reactors
					) {
				return createEventReceiverOf(name, init, reactors);
			}};
	}
	
    private EventReceiver createEventReceiverOf(
            final String name, 
            final EventHandler initHandler,
            final Object... reactors
            ) {
        //  create new receiver
        final FlowContextImpl ctx = initFlowCtx(name, reactors, initHandler);
        
        final EventReceiver newReceiver = genEventReceiverWithCtx(name, ctx);
        
        final FlowLifecycleListener lifecycleListener = 
        		InterfaceUtils.compositeByType(reactors, FlowLifecycleListener.class);
		if (null!=lifecycleListener) {
			try {
				lifecycleListener.afterEventReceiverCreated(newReceiver);
			}
			catch (Exception e) {
				LOG.error("exception when invoke flow {}'s afterEventReceiverCreated, detail: {}",
						name, ExceptionUtils.exception2detail(e));
			}
        }
        
        return  newReceiver;
    }
	
	/**
	 * @param ctx
	 * @return
	 */
	private EventReceiver genEventReceiverWithCtx(final String name, final FlowContextImpl ctx) {
		return	new EventReceiver() {

			@Override
			public boolean acceptEvent(final String event, final Object... args) throws Exception {
		        try {
		            return ctx.processEvent(event, args);
		        }
		        catch (final Exception e) {
		            LOG.error("exception when flow({})'s processEvent, detail:{}, try end flow", 
		                    this, ExceptionUtils.exception2detail(e));
		            ctx.destroy(event, args);
		            throw e;
		        }
			}

            @Override
            public boolean acceptEvent(final Eventable eventable, final Object... args)
                    throws Exception {
                try {
                    return ctx.processEvent(eventable, args);
                }
                catch (final Exception e) {
                    LOG.error("exception when flow({})'s processEvent, detail:{}, try end flow", 
                            this, ExceptionUtils.exception2detail(e));
		            ctx.destroy(eventable.event(), args);
                    throw e;
                }
			}
            
            @Override
            public String toString() {
                return null != name 
            		? "EventReceiver [" + name +"]"
            		: super.toString();
            }
		};
	}
	
	@Override
    public void addReactorBuilder(
            final FlowContext.ReactorBuilder builder) {
        if ( null == builder ) {
            LOG.warn("addReactorBuilder: builder is null, just ignore");
        }
        else {
            if ( !_reactorBuilderSupport.addComponent(builder) ) {
                LOG.warn("addReactorBuilder: builder {} has already added", 
                		builder);
            }
        }
    }

	@Override
    public void removeReactorBuilder(
    		final FlowContext.ReactorBuilder builder) {
        if ( null == builder ) {
            LOG.warn("removeReactorBuilder: builder is null, just ignore");
        }
        else {
            _reactorBuilderSupport.removeComponent(builder);
        }
    }
    
	//	implements Extensible
	@SuppressWarnings("unchecked")
	@Override
    public <INTF> INTF queryInterfaceInstance(final Class<INTF> type) {
		if ( type.equals(FlowCounter.class)) {
			return	(INTF)this._flowCounter;
		}
		else {
			return null;
		}
	}
	
    private ExecutorService getWorkService() {
		final ExecutorSource source = this._executorSource.get();
		return	(null != source) ? source.getExecutor() : null;
	}

	public void setExecutorSource(final ExecutorSource newSource) {
		synchronized (this._executorSource) {
			//	保证 newService 的 registerFlowCounter 以及 可能的 unregisterFlowCounter 是按顺序被调用的
			final ExecutorSource prevSource = this._executorSource.getAndSet(newSource);
			
			if ( null == prevSource ) {
				//	first
				if ( newSource instanceof FlowCounterAware ) {
					//	means _executorSource != null
					((FlowCounterAware)newSource).registerFlowCounter(this._flowCounter);
				}
			}
			else if ( !prevSource.equals(newSource) ) {
				// service changed
				if ( prevSource instanceof FlowCounterAware ) {
					((FlowCounterAware)prevSource).unregisterFlowCounter(this._flowCounter);
				}
				if ( newSource instanceof FlowCounterAware ) {
					((FlowCounterAware)newSource).registerFlowCounter(this._flowCounter);
				}
			}
		}
	}
	
	@Override
	public String getName() {
		return this._name;
	}

	@Override
	public int getId() {
		return	this._id;
	}

	@Override
	public int getFlowTotalCount() {
		return this._totalFlowCount.get();
	}

	@Override
	public int getFlowActiveCount() {
		return	this._activeFlowCount.get();
	}

	public long getDealHandledCount() {
		return this._dealHandledCount.get();
	}
	
	public long getDealCompletedCount() {
		return this._dealCompletedCount.get();
	}

	public long getDealBypassCount() {
		return this._dealBypassCount.get();
	}


    @Override
    public String[] getFlowsDetail() {
        final List<String> ret = new ArrayList<String>();
        final SimpleDateFormat formater = new SimpleDateFormat("yyyyMMdd-HH:mm:ss.SSS");
        
        final Iterator<FlowContextImpl> iter = this._flowContexts.iterator();
        
        while ( iter.hasNext() ) {
            final FlowContextImpl ctx = iter.next();
            if ( null != ctx ) {
                final StringBuilder sb = new StringBuilder();
                
                sb.append("flow:");
                sb.append( ctx.toString() );
                
                sb.append("/state:");
                final EventHandler currentHandler = ctx.getCurrentHandler();
                sb.append( null != currentHandler ? currentHandler.getName() : "(null)" );
            
                sb.append("/create:");
                sb.append(formater.format( new Date(ctx.getCreateTime())) );
                
                sb.append("/last:");
                sb.append(formater.format( new Date(ctx.getLastModify())) );
                
                sb.append("/tta:");
                sb.append(ctx.getTimeToActive());
                
                sb.append("/ttl:");
                sb.append(ctx.getTimeToLive());
                
                sb.append("/endReason:");
                final Object endReason = ctx.getEndReason();
                sb.append(null != endReason ? endReason.toString() : "(null)");
                
                ret.add(sb.toString());
            }
        }
        
        return  ret.toArray(new String[0]);
    }
    
    private FlowContextImpl initFlowCtx(
			final String 	name,
	        final Object[] 	reactors, 
            final EventHandler initHandler 
            ) {
        final FlowContextImpl newCtx = 
            new FlowContextImpl(name, genExectionLoop(), this._ctxStatusReactor);
        
		newCtx.setReactors(addReactors(reactors, newCtx));
        newCtx.setCurrentHandler(initHandler, null, null);
        
        if (this._flowContexts.add(newCtx) ) {
            // add new context
            this._totalFlowCount.incrementAndGet();
        }
        
        incDealHandledCount();
        
        return  newCtx;
    }
    
	private Object[] addReactors(final Object[] reactors,
			final FlowContextImpl newCtx) {
		if (this._reactorBuilderSupport.isEmpty()) {
			final Object[] newReactors = Arrays.copyOf(reactors, reactors.length + 1);
			newReactors[newReactors.length-1] = hookOnFlowCtxDestoryed(newCtx);
			return newReactors;
		}
		else {
			final List<Object> newReactors = new ArrayList<>();
			newReactors.addAll(Arrays.asList(reactors));
			newReactors.add(hookOnFlowCtxDestoryed(newCtx));
			this._reactorBuilderSupport.foreachComponent(new Visitor<ReactorBuilder> () {
				@Override
				public void visit(final ReactorBuilder builder) throws Exception {
					final Object[] ret = builder.buildReactors(newCtx);
					if (null!=ret && ret.length>0) {
						newReactors.addAll(Arrays.asList(ret));
					}
				}});
			return newReactors.toArray();
		}
	}
	
	private FlowStateChangedListener<EventHandler> hookOnFlowCtxDestoryed(
			final FlowContextImpl ctx) {
		return new FlowStateChangedListener<EventHandler>() {
			@Override
			public void onStateChanged(
					final EventHandler prev, 
					final EventHandler next,
					final String causeEvent, 
					final Object[] causeArgs) throws Exception {
				if (null==next) {
					onFlowCtxDestroyed(ctx);
				}
			}
		};
	}
	
	private ExectionLoop genExectionLoop() {
        final ExecutorService executorService = this.getWorkService();
        if ( null == executorService ) {
            LOG.warn("executorService not ready or runner stopped, just ignore");
            throw new RuntimeException("FlowRunner: executorService not ready or runner stopped");
        }

        return new ExectionLoop() {

            @Override
            public boolean inExectionLoop() {
                final Thread currentThread = Thread.currentThread();
                
                return ( (currentThread instanceof ThreadOfExecutorService)
                    && (((ThreadOfExecutorService)currentThread).selfExecutorService() == executorService) );
            }

            @Override
            public Detachable submit(final Runnable runnable) {
                final Future<?> future = executorService.submit(runnable);
                return new Detachable() {
                    @Override
                    public void detach() {
                        future.cancel(false);
                    }};
            }

            @Override
            public Detachable schedule(final Runnable runnable, final long delayMillis) {
                final Callable<Boolean> cancel = _timerService.schedule(runnable, delayMillis);
                return new Detachable() {
                    @Override
                    public void detach() throws Exception {
                        cancel.call();
                    }};
            }
        };
    }

    @Override
	public boolean destroy() {

		if ( _destroySupport.destroy() ) {
			//	force unregister FlowCounter for valid prev executor service
			this.setExecutorSource(null);
			
			this._mbeanSupport.destroy();
			
			this._flowCountListenerSupport.clear();
			this._reactorBuilderSupport.clear();
			
			if ( LOG.isInfoEnabled() ) {
				LOG.info("runner {}/{} destroyed.", this._name, this._id);
			}
			return	true;
		}
		else {
			LOG.warn("Runner already destroyed.");
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

	@Override
	public boolean isRunning() {
		return	!this.isDestroyed();
	}
	
	@Override
	public boolean isCheckBusy() {
		return checkBusy;
	}

	@Override
	public void setCheckBusy(final boolean checkBusy) {
		this.checkBusy = checkBusy;
	}

	@Override
	public int getBusyThreshold() {
		return busyThreshold;
	}

	@Override
	public void setBusyThreshold(final int busyThreshold) {
		this.busyThreshold = busyThreshold;
	}

    private static void updateLargestCount(final AtomicInteger largestCount, 
    		final int currentCount) {
    	int cnt = largestCount.get();
		boolean modified = false;
    	while ( !modified && cnt < currentCount ) {
    		modified = largestCount.compareAndSet(cnt, currentCount);
    		cnt = largestCount.get();
    	}
    }
    
    private	void incActiveFlowCount() {
    	final int nowCount = _activeFlowCount.incrementAndGet();
    	updateLargestCount(_largestActiveJobCount, nowCount);
    	
    	//	fire listener
		_flowCountListenerSupport.foreachComponent(new Visitor<FlowCountListener>() {

			@Override
			public void visit(final FlowCountListener listener) throws Exception {
				listener.onActiveFlowCountIncrement(_flowCounter, nowCount);
			}});
    }
    
    private	void decActiveFlowCount() {
		final int nowCount = _activeFlowCount.decrementAndGet();
		
    	//	fire listener
		_flowCountListenerSupport.foreachComponent(new Visitor<FlowCountListener>() {

			@Override
			public void visit(final FlowCountListener listener) throws Exception {
				listener.onActiveFlowCountDecrement(_flowCounter, nowCount);
			}});
    }
    
    private void onFlowCtxDestroyed(final FlowContextImpl ctx) {
        if ( this._flowContexts.remove(ctx) ) {
            //  移除操作有效
            this._totalFlowCount.decrementAndGet();
        }
        
        incDealCompletedCount();
    }
    
	private void incDealBypassCount() {
		this._dealBypassCount.incrementAndGet();
	}
	
	private void incDealHandledCount() {
	    this._dealHandledCount.incrementAndGet();
	}
	
	private void incDealCompletedCount() {
	    this._dealCompletedCount.incrementAndGet();
	}

	/**
	 * @return the objectNamePrefix
	 */
	@Override
	public String getObjectNamePrefix() {
		return this._mbeanSupport.getObjectNamePrefix();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return this._name + "-" + this._id;
	}

	private boolean checkIfExceedLimit(final FlowContextImpl ctx) {
		final int activedFlowCount = this._activeFlowCount.get();
		if ( this.checkBusy 
			&& activedFlowCount >= this.busyThreshold ) {
			
			//	超过最大 Actived Flow 限制值
			LOG.warn("try active flow({}) when current actived flow count({}) has exceed limit({})", 
					ctx, activedFlowCount, this.busyThreshold);
			return true;
		}
		else {
		    return false;
		}
	}
	
	private	final FlowContextImpl.StatusReactor _ctxStatusReactor = 
        	new FlowContextImpl.StatusReactor() {
                @Override
                public boolean checkIfExceedLimit(FlowContextImpl ctx) {
                    return FlowRunner.this.checkIfExceedLimit(ctx);
                }
                
    			public void onActive(final FlowContextImpl ctx){
    				incActiveFlowCount();
    			}
    	
    			public void onUnactive(final FlowContextImpl ctx) {
    				decActiveFlowCount();
    			}

                @Override
                public void onDestroyByExceedLimit(final FlowContextImpl ctx) {
                    incDealBypassCount();
                }
    	    };
        
    private final Set<FlowContextImpl> _flowContexts = 
            new ConcurrentSkipListSet<FlowContextImpl>();
	
	private final String		_name;
	private	final int			_id;
	private final TimerService  _timerService;
	
	private	final ObservationDestroyableSupport	_destroySupport = 
			new ObservationDestroyableSupport(this);
	
	private	final AtomicReference<ExecutorSource> _executorSource = 
			new AtomicReference<ExecutorSource>(null);
	
	private	final AtomicInteger	_totalFlowCount = new AtomicInteger(0);
	private	final AtomicInteger	_activeFlowCount = new AtomicInteger(0);
	private	final AtomicInteger	_largestActiveJobCount = new AtomicInteger(0);
	
	private	volatile boolean	checkBusy = true;
	private	volatile int		busyThreshold = 200;
	
	private	final AtomicLong _dealHandledCount = new AtomicLong(0);
	private	final AtomicLong _dealCompletedCount = new AtomicLong(0);
	private	final AtomicLong _dealBypassCount = new AtomicLong(0);
	
	//	JMX support
    private	final MBeanRegisterSupport 	_mbeanSupport;
	
    private final COWCompositeSupport<FlowContext.ReactorBuilder> _reactorBuilderSupport
        = new COWCompositeSupport<FlowContext.ReactorBuilder>();
    
	private final COWCompositeSupport<FlowCountListener> _flowCountListenerSupport
		= new COWCompositeSupport<FlowCountListener>();

	private final class InnerFlowCounter implements FlowCounter, Comparable<InnerFlowCounter> {
        @Override
        public FlowRunnerMXBean getRunner() {
            return FlowRunner.this;
        }
    
        @Override
        public void registerFlowCountListener(final FlowCountListener listener) {
            if ( null == listener ) {
                LOG.warn("registerJobCountListener: listener is null, just ignore");
            }
            else {
                if ( !_flowCountListenerSupport.addComponent(listener) ) {
                    LOG.warn("registerFlowCountListener: listener {} has already registered", 
                            listener);
                }
            }
        }
        
        @Override
        public void unregisterFlowCountListener(final FlowCountListener listener) {
            if ( null == listener ) {
                LOG.warn("unregisterFlowCountListener: listener is null, just ignore");
            }
            else {
                _flowCountListenerSupport.removeComponent(listener);
            }
        }
        
        int containerId() {
            return FlowRunner.this._id;
        }

        @Override
        public int compareTo(final InnerFlowCounter o) {
            return this.containerId() - o.containerId();
        }
	}
	
	private final FlowCounter _flowCounter = new InnerFlowCounter();
}

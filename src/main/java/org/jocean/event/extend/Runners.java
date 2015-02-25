/**
 * 
 */
package org.jocean.event.extend;

import org.jocean.event.api.EventReceiverSource;
import org.jocean.event.extend.executor.ExecutorSource;
import org.jocean.event.extend.executor.TimerService;
import org.jocean.event.extend.management.impl.RunnerDashboardImpl;
import org.jocean.event.extend.runner.FlowRunner;
import org.jocean.idiom.ObservationDestroyable;
import org.jocean.j2se.jmx.InstanceLocator;


/**
 * @author isdom
 *
 */
public class Runners {
	private static final String EVENTRECVRSRC_STR = "eventreceiver_source";

	public static class Config {
		String		_name;
		ExecutorSource	_executorSource;
		TimerService _timerService;
        
		//	optional
		String		_objectNamePrefix = null;
		boolean		_enableDashboard = true;
		
		public Config() {
		}
		
        public Config(
                final String objNamePrefix,
                final String name,
                final TimerService timer,
                final ExecutorSource executorSource) {
            this._objectNamePrefix = objNamePrefix;
            this._name = name;
            this._timerService = timer;
            this._executorSource = executorSource;
        }
        
		public Config name(final String name) {
			this._name = name;
			return	this;
		}
		
		public Config executorSource(final ExecutorSource source) {
			this._executorSource = source;
			return	this;
		}
		
        public Config timerService(final TimerService timer) {
            this._timerService = timer;
            return  this;
        }
        
		public Config objectNamePrefix(final String prefix) {
			this._objectNamePrefix = prefix;
			return	this;
		}
		
		public Config enableDashboard(final boolean enabled) {
			this._enableDashboard = enabled;
			return	this;
		}
	}
	
	public static EventReceiverSource build(final Config config) {
		final FlowRunner runner = 
				new FlowRunner(
						config._name, config._objectNamePrefix, config._timerService);
		
		runner.setExecutorSource(config._executorSource);

		if ( config._enableDashboard ) {
			final RunnerDashboardImpl dashboard = 
					new RunnerDashboardImpl(runner);
			runner.registerOnDestroyedListener(new ObservationDestroyable.Listener() {
				
				@Override
				public void onDestroyed(final ObservationDestroyable destroyable) throws Exception {
					dashboard.destroy();
				}
			});
		}
		
		InstanceLocator.registerInstance(EVENTRECVRSRC_STR, runner.getName(), runner);
		
		runner.registerOnDestroyedListener(new ObservationDestroyable.Listener() {

			@Override
			public void onDestroyed(ObservationDestroyable destroyable) throws Exception {
				InstanceLocator.unregisterInstance(EVENTRECVRSRC_STR, runner.getName());
			}});
		
		return	runner.genEventReceiverSource();
	}
	
	public static EventReceiverSource lookup(final String name) {
		final FlowRunner runner =	(FlowRunner)InstanceLocator.locateInstance(EVENTRECVRSRC_STR, name);
		return (null != runner) ? (EventReceiverSource)runner.genEventReceiverSource() : null;
	}
}

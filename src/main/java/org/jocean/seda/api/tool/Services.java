/**
 * 
 */
package org.jocean.seda.api.tool;

import org.jocean.idiom.ObservationDestroyable;
import org.jocean.j2se.InstanceLocator;
import org.jocean.seda.api.ExecutorSource;
import org.jocean.seda.api.TimerService;
import org.jocean.seda.executor.FlowBasedExecutorSource;
import org.jocean.seda.executor.TimerServiceByScheduleExecutor;


/**
 * @author isdom
 *
 */
public class Services {
	public static ExecutorSource newFlowBasedExecutorSource(final String name) {
		final  FlowBasedExecutorSource source = new FlowBasedExecutorSource(name);
		
		InstanceLocator.registerInstance("executor", name, source);
		
		source.registerOnDestroyedListener(new ObservationDestroyable.Listener() {

			@Override
			public void onDestroyed(ObservationDestroyable destroyable) throws Exception {
				InstanceLocator.unregisterInstance("executor", source.getName());
			}});

		return	source;
	}
	
	public static ExecutorSource lookupOrCreateFlowBasedExecutorSource(final String name) {
		final ExecutorSource source = 
				InstanceLocator.locateInstance("executor", name);
		if ( null != source ) {
			return source;
		}
		else {
			return newFlowBasedExecutorSource(name);
		}
	}

	public static TimerService newDefaultTimerService(final String name) {
		final  TimerServiceByScheduleExecutor timer = 
				new TimerServiceByScheduleExecutor(name);
		
		InstanceLocator.registerInstance("timer", name, timer);
		
		timer.registerOnDestroyedListener(new ObservationDestroyable.Listener() {

			@Override
			public void onDestroyed(ObservationDestroyable destroyable) throws Exception {
				InstanceLocator.unregisterInstance("timer", timer.getName());
			}});
		
		return	timer;
	}
	
	public static TimerService lookupOrCreateTimerService(final String name) {
		final TimerService service = InstanceLocator.locateInstance("timer", name);
		if ( null != service ) {
			return service;
		}
		else {
			return newDefaultTimerService(name);
		}
	}
}

/**
 * 
 */
package org.jocean.event.extend.common;

import org.jocean.event.core.FlowContext;
import org.jocean.event.extend.management.FlowRunnerMXBean;
import org.jocean.idiom.InterfaceSource;
import org.jocean.idiom.ObservationDestroyable;


/**
 * @author isdom
 *
 */
public interface EventDrivenFlowRunner
	extends FlowRunnerMXBean, InterfaceSource, ObservationDestroyable {
	
	public String getObjectNamePrefix();

    public void addReactorBuilder(
            final FlowContext.ReactorBuilder builder);

    public void removeReactorBuilder(
    		final FlowContext.ReactorBuilder builder);
}

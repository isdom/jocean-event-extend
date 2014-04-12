/**
 * 
 */
package org.jocean.event.extend.common;

import org.jocean.event.extend.management.FlowRunnerMXBean;
import org.jocean.idiom.Extensible;
import org.jocean.idiom.ObservationDestroyable;


/**
 * @author isdom
 *
 */
public interface EventDrivenFlowRunner
	extends FlowRunnerMXBean, Extensible, ObservationDestroyable {
	
	public String getObjectNamePrefix();
}

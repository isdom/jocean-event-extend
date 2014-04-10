/**
 * 
 */
package org.jocean.seda.common;

import org.jocean.idiom.Extensible;
import org.jocean.idiom.ObservationDestroyable;
import org.jocean.seda.management.FlowRunnerMXBean;


/**
 * @author isdom
 *
 */
public interface EventDrivenFlowRunner
	extends FlowRunnerMXBean, Extensible, ObservationDestroyable {
	
	public String getObjectNamePrefix();
}

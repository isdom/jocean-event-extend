/**
 * 
 */
package org.jocean.event.extend.common;

import org.jocean.event.extend.management.FlowRunnerMXBean;

/**
 * @author isdom
 *
 */
public interface FlowCounter {

	public FlowRunnerMXBean getRunner();
	
	public void registerFlowCountListener(
			final FlowCountListener listener);
	
	public void unregisterFlowCountListener(
			final FlowCountListener listener);
	
}

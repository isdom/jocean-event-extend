/**
 * 
 */
package org.jocean.seda.common;

import org.jocean.seda.management.FlowRunnerMXBean;

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

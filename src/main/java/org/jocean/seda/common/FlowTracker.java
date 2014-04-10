/**
 * 
 */
package org.jocean.seda.common;

/**
 * @author hp
 *
 */
public interface FlowTracker {
	
	public void registerFlowStateChangeListener(
			final FlowStateChangeListener listener);
	
	public void unregisterFlowStateChangeListener(
			final FlowStateChangeListener listener);

}

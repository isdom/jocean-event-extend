/**
 * 
 */
package org.jocean.event.extend.common;


/**
 * @author isdom
 *
 */
public interface FlowCountListener {
	
	public void onActiveFlowCountIncrement(
			final FlowCounter container, final int newActiveFlowCount);
	
	public void onActiveFlowCountDecrement(
			final FlowCounter container, final int newActiveFlowCount);
}

/**
 * 
 */
package org.jocean.event.extend.common;

/**
 * @author isdom
 *
 */
public interface FlowCounterAware {
	
	public void registerFlowCounter(final FlowCounter flowCounter);
	
	public void unregisterFlowCounter(final FlowCounter flowCounter);

}

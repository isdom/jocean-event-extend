/**
 * 
 */
package org.jocean.seda.common;

import org.jocean.seda.api.EventHandler;

/**
 * @author isdom
 *
 */
public interface FlowStateChangeListener {
	
	public void beforeFlowChangeTo(
			final FlowContext 	ctx, 
            final EventHandler  nextHandler, 
			final String 		causeEvent, 
			final Object[] 		causeArgs) throws Exception;
	
	public void afterFlowDestroy(final FlowContext ctx) throws Exception;
}
  
/**
 * 
 */
package org.jocean.seda.common;

import org.jocean.seda.api.EventHandler;

/**
 * @author isdom
 *
 */
public interface FlowContext {

	public EventHandler getCurrentHandler();
	
	public Object getEndReason();

	public long getCreateTime();
	
	public long getLastModify();

	public long getTimeToActive();

	public long getTimeToLive();
}

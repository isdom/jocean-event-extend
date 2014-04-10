/**
 * 
 */
package org.jocean.seda.api;

/**
 * @author isdom
 *
 */
public interface EventInvoker {
	
	public <RET> RET invoke(final Object[] args) throws Exception;
	
	public String	getBindedEvent();
}

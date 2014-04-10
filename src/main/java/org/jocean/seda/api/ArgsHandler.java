/**
 * 
 */
package org.jocean.seda.api;

/**
 * @author isdom
 *
 */
public interface ArgsHandler {
	
	public Object[] beforeAcceptEvent(final Object[] args) throws Exception;
	
	public void afterAcceptEvent(final Object[] args) throws Exception;
}

/**
 * 
 */
package org.jocean.seda.api;

/**
 * @author isdom
 *
 */
public interface EventUnhandleAware extends Eventable {
    public void onEventUnhandle(final String event, final Object ... args) throws Exception;
}

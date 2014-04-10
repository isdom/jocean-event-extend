/**
 * 
 */
package org.jocean.seda.api;

/**
 * @author isdom
 *
 */
/**
 * @author isdom
 *
 * @param <ID>
 * @param <JOB>
 */
public interface EventReceiverSource {

    public <FLOW> EventReceiver create(final FlowSource<FLOW> source);

    public <FLOW> EventReceiver create(final FLOW flow, final EventHandler initHandler);
}

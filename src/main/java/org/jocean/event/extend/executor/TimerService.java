/**
 * 
 */
package org.jocean.event.extend.executor;

import java.util.concurrent.Callable;

/**
 * @author isdom
 *
 */
public interface TimerService {
	//	return runnable to cancel pending task
	public Callable<Boolean> schedule(final Runnable task, final long delay);
}

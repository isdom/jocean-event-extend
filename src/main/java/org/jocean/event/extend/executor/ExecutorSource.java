/**
 * 
 */
package org.jocean.event.extend.executor;

import java.util.concurrent.ExecutorService;

/**
 * @author isdom
 *
 */
public interface ExecutorSource {
	public ExecutorService getExecutor();
}

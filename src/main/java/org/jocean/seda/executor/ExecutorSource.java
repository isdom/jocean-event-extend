/**
 * 
 */
package org.jocean.seda.executor;

import java.util.concurrent.ExecutorService;

/**
 * @author isdom
 *
 */
public interface ExecutorSource {
	public ExecutorService getExecutor();
}

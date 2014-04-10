/**
 * 
 */
package org.jocean.seda.api;

import java.util.concurrent.ExecutorService;

/**
 * @author isdom
 *
 */
public interface ExecutorSource {
	public ExecutorService getExecutor();
}

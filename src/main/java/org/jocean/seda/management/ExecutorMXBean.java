/**
 * 
 */
package org.jocean.seda.management;

/**
 * @author isdom
 *
 */
public interface ExecutorMXBean {
	
	public int getExecutorActiveThreadCount();
	
	public int getExecutorLargestPoolSize();

	public int getExecutorCorePoolSize();
	
	public int getExecutorMaximumPoolSize();

	public int getExecutorCurrentPoolSize();
	
 	public long getExecutorHandledTaskCount();
 	
	public long getExecutorCompletedTaskCount();

    public int getExecutorPendingTaskCount();

}

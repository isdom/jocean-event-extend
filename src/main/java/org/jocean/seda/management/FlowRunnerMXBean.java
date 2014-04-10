/**
 * 
 */
package org.jocean.seda.management;

/**
 * @author isdom
 *
 */
public interface FlowRunnerMXBean {
	
	//	operations
	public boolean destroy();
	
	public void setCheckBusy(final boolean checkBusy);

	public void setBusyThreshold(final int busyThreshold);
	
	//	attributes
	public String getName();
	
	public int getId();
	
	public int getFlowTotalCount();
	
	public int getFlowActiveCount();
	
	public long getDealHandledCount();
	
	public long getDealCompletedCount();

	public long getDealBypassCount();
	
	public boolean isRunning();
	
	public boolean isCheckBusy();
	
	public int getBusyThreshold();
}

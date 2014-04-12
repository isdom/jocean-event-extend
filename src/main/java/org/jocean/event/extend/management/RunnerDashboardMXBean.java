/**
 * 
 */
package org.jocean.event.extend.management;

/**
 * @author isdom
 *
 */
public interface RunnerDashboardMXBean {
	
	public long getDealHandledCount();
	
	public long getDealCompletedCount();
	
	public long getDealBypassCount();

	public long getDealLargestTimeToLive();

	public long getDealLargestTimeToActive();

	public long getDealSmallestTimeToLive();

	public long getDealSmallestTimeToActive();

	public long getDealAverageTimeToLive();

	public long getDealAverageTimeToActive();

	public void resetDealTimeExtremum();

	public String	getDealTimeCountAsString();

	public void resetDealTimeCounter();

	public String	getDealEndReasonCountAsString();
	
	public void resetDealEndReasonCounter();
	
	public int getFlowTotalCount();
	
	public int getFlowInactiveCount();
	
	public int getFlowActiveCount();
	
//    public int 	getJobLargestActiveCount();
//    
//	public String[] getJobsDetail();
}

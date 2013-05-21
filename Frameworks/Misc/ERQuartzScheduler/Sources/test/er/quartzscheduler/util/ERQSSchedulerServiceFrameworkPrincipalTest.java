package er.quartzscheduler.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Properties;

import org.junit.BeforeClass;
import org.junit.Test;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.JobListener;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;

import er.quartzscheduler.foundation.ERQSJobListener;
import er.quartzscheduler.foundation.ERQSJobSupervisor;

public class ERQSSchedulerServiceFrameworkPrincipalTest 
{
	public class MySupervisor implements Job
	{
		public void execute(final JobExecutionContext arg0) throws JobExecutionException 
		{
			// Nothing to do			
		}		
	}
	public class MyJobListener implements JobListener
	{

		public String getName() 
		{
			return MyJobListener.class.getName();
		}

		public void jobExecutionVetoed(final JobExecutionContext arg0) 
		{
			// Nothing to do			

		}

		public void jobToBeExecuted(final JobExecutionContext arg0) 
		{
			// Nothing to do			

		}

		public void jobWasExecuted(final JobExecutionContext arg0, final JobExecutionException arg1) 
		{
			// TODO Auto-generated method stub

		}
	}

	@BeforeClass
	public static void testSetup() 
	{
		ERQSSchedulerFP4Test.setUpFrameworkPrincipal();
		Properties p = new Properties(System.getProperties());
		p.setProperty("org.quartz.jobStore.class", "org.quartz.simpl.RAMJobStore");
		p.setProperty("org.quartz.threadPool.class", "org.quartz.simpl.SimpleThreadPool");
		System.setProperties(p);
	}

	@Test 
	public void testSharedInstanceNotNull()
	{
		ERQSSchedulerServiceFrameworkPrincipal fp = ERQSSchedulerFP4Test.getSharedInstance();
		assertNotNull(fp);
	}

	@Test
	public void testInstantiateJobSupervisor() throws SchedulerException 
	{
		ERQSSchedulerServiceFrameworkPrincipal fp = ERQSSchedulerFP4Test.getSharedInstance();
		fp.instantiateJobSupervisor();
		JobDetail supervisor = fp.getScheduler().getJobDetail(new JobKey("JobSupervisor", Scheduler.DEFAULT_GROUP));
		assertNotNull(supervisor);
		assertEquals(supervisor.getJobClass(), ERQSJobSupervisor.class);
		fp.stopScheduler();
	}

	@Test
	public void testGetDefaultJobListener() 
	{
		ERQSSchedulerServiceFrameworkPrincipal fp = ERQSSchedulerFP4Test.getSharedInstance();
		assertTrue(fp.getDefaultJobListener() instanceof ERQSJobListener);
	}

	@Test
	public void testAddJobListener() throws SchedulerException 
	{
		ERQSSchedulerServiceFrameworkPrincipal fp = ERQSSchedulerFP4Test.getSharedInstance();
		fp.addJobListener(fp.getDefaultJobListener());
		JobListener aJobListener = fp.getScheduler().getListenerManager().getJobListener(ERQSJobListener.class.getName());
		assertNotNull(aJobListener);
	}

	@Test
	public void testDefaultSupervisorSleepDuration() 
	{
		ERQSSchedulerServiceFrameworkPrincipal fp = ERQSSchedulerFP4Test.getSharedInstance();
		assertTrue(fp.supervisorSleepDuration() == ERQSJobSupervisor.DEFAULT_SLEEP_DURATION);
	}

	@Test
	public void testGetListOfJobDescription() 
	{
		ERQSSchedulerServiceFrameworkPrincipal fp = ERQSSchedulerFP4Test.getSharedInstance();
		assertTrue(fp.getListOfJobDescription(null).size() == 0);
	}

	@Test
	public void testNewEditingContext() 
	{
		ERQSSchedulerServiceFrameworkPrincipal fp = ERQSSchedulerFP4Test.getSharedInstance();
		assertNotNull(fp.newEditingContext());
	}
}

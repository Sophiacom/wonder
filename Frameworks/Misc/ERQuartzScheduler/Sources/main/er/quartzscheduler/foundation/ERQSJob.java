package er.quartzscheduler.foundation;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.InterruptableJob;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.UnableToInterruptJobException;

import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.eocontrol.EOGlobalID;
import com.webobjects.foundation.NSTimestamp;

import er.extensions.eof.ERXEOControlUtilities;
import er.quartzscheduler.util.ERQSUtilities;

/**
 * Subclass ERQSJob to create your own jobs.<p>
 * 
 * The most important method to implement is _execute(). That's the place to put your code that is will be 
 * executed periodically.<br>
 * You have to tell if your job supports interruption.<br><br>
 * The willXXX and validateForXXX methods can be empty.<br><br>
 * 
 * @author Philippe Rabier
 * @see #_execute()
 * @see #isJobInterruptible()
 */
@DisallowConcurrentExecution
public abstract class ERQSJob extends ERQSAbstractJob implements InterruptableJob
{
	public static final String ENTERPRISE_OBJECT_KEY = "eoJobKey";
	public static final String NOT_PERSISTENT_OBJECT_KEY = "jobKey";

	private ERQSJobDescription jobDescription;
	private boolean jobInterrupted = false;

	/**
	 * Implementation of Job interface.
	 * Called by the Scheduler when a Trigger associated with the job is fired.<br>
	 * getJobContext() returns the jobContext after execute() is called.<p>
	 * 
	 * @param jobexecutioncontext passed by the scheduler
	 * @see <a href="http://quartz-scheduler.org/documentation/best-practices">http://quartz-scheduler.org/documentation/best-practices</a>
	 */
	@Override
	public final void execute(final JobExecutionContext jobexecutioncontext) throws JobExecutionException
	{
		super.execute(jobexecutioncontext);
		
		try
		{
			_execute();
		} catch (Exception e)
		{
			log.error("method: execute: " + e.getMessage(), e);
		}	
	}

	/**
	 * _execute() is called by execute(). Put your code here and everything will be set up for you.<p>
	 * To be sure that any exception will be caught, _execute() call must be surround by a try/catch block otherwise
	 * the job is considered running forever.
	 * 
	 * @throws JobExecutionException 
	 * 
	 */
	protected abstract void _execute() throws JobExecutionException;
	
	/**
	 * It's a good place to put code that will be executed before job description deletion.<p>
	 * Nothing is done automatically, you have to call this method manually if you want to give a chance to the job to
	 * use its own logic.<br><br>
	 * 
	 * However you can use the static methods of ERQSUtilities as helper.
	 * 
	 * @param aJobDescription
	 * @see ERQSUtilities#willDelete(ERQSJobDescription)
	 */
	public abstract void willDelete(ERQSJobDescription aJobDescription);

	/**
	 * It's a good place to put code that will be executed before job description save.<p>
	 * Nothing is done automatically, you have to call this method manually if you want to give a chance to the job to
	 * use its own logic.<br><br>
	 * 
	 * However you can use the static methods of ERQSUtilities as helper.
	 * 
	 * @param aJobDescription
	 * @see ERQSUtilities#willSave(ERQSJobDescription)
	 */
	public abstract void willSave(ERQSJobDescription aJobDescription);

	/**
	 * It's a good place to put code that will check if the job description can be saved or not.<p>
	 * Nothing is done automatically, you have to call this method manually if you want to give a chance to the job to
	 * use its own logic.<br><br>
	 * 
	 * However you can use the static methods of ERQSUtilities as helper.
	 * 
	 * @param aJobDescription
	 * @see ERQSUtilities#validateForSave(ERQSJobDescription)
	 */
	public abstract void validateForSave(ERQSJobDescription aJobDescription);

	/**
	 * It's a good place to put code that will check if the job description can be deleted or not.<p>
	 * Nothing is done automatically, you have to call this method manually if you want to give a chance to the job to
	 * use its own logic.<br><br>
	 * 
	 * However you can use the static methods of ERQSUtilities as helper.
	 * 
	 * @param aJobDescription
	 * @see ERQSUtilities#validateForDelete(ERQSJobDescription)
	 */
	public abstract void validateForDelete(ERQSJobDescription aJobDescription);

	/**
	 * Return the ERQSJobDescription object attached to the job.
	 *
	 * @param ec editingContext (can be null only if ERQSJobDescription object is not an Enterprise Object)
	 * @return the ERQSJobDescription object.
	 * @throws IllegalArgumentException if ec is null and ERQSJobDescription object is an Enterprise Object
	 * @throws IllegalStateException if there is no EOGlobalID in the JobDataMap 
	 */
	public ERQSJobDescription getJobDescription(final EOEditingContext ec)
	{
		ERQSJobDescription aJobDescription;
		
		if (jobDescription == null)
		{
			JobExecutionContext context = getJobContext();
			
			if (context.getMergedJobDataMap() != null)
			{
				EOGlobalID id = (EOGlobalID) context.getMergedJobDataMap().get(ENTERPRISE_OBJECT_KEY);
				if (id != null)
				{
					if (ec == null)
						throw new IllegalArgumentException("method: getJobDescription: ec parameter can be null.");
					jobDescription = (ERQSJobDescription) ec.faultForGlobalID(id, ec);
				}
				else
					jobDescription = (ERQSJobDescription) context.getMergedJobDataMap().get(NOT_PERSISTENT_OBJECT_KEY);

				if (jobDescription == null)
					throw new IllegalStateException("method: getJobDescription: unknown jobDescription.");
			}
			else
			{
				throw new IllegalStateException("method: getJobDescription: no job detail or job data map. The jobDescription is still null.");
			}
			aJobDescription = jobDescription;
		}
		else
		{
			aJobDescription = jobDescription;
			if (aJobDescription.isEnterpriseObject())
			{
				if (ec == null)
					throw new IllegalArgumentException("method: getJobDescription: ec parameter can be null.");
				aJobDescription = (ERQSJobDescription) ERXEOControlUtilities.localInstanceOfObject(ec, (EOEnterpriseObject)aJobDescription);
			}
		}
		
		if (log.isDebugEnabled())
			log.debug("method: getJobDescription: jobDescription: " + jobDescription);
		return aJobDescription;
	}

	/**
	 * Helper method that calls editingContext() before calling getJobDescription(final EOEditingContext ec)<p>
	 * Useful method if you always use the editing context provided by editingContext() in your code.
	 * 
	 * @return the ERQSJobDescription object
	 */
	public ERQSJobDescription getJobDescription()
	{
		return getJobDescription(editingContext());
	}

	/**
	 * 
	 * @return the last execution date stored in the ERQSJobDescription object
	 */
	public NSTimestamp getLastExecutionDate() 
	{
		return getJobDescription().lastExecutionDate();
	}
	
    /**
     * Called by the <code>{@link Scheduler}</code> when a user interrupts the <code>Job</code>.
     *
     * @throws UnableToInterruptJobException if the job can't be interrupted.
     */
    public void interrupt() throws UnableToInterruptJobException 
    {
    	log.info("method: interrupt has been called for the job: " + getJobDescription());
    	if (this.isJobInterruptible())
    		jobInterrupted = true;
    	else
    		throw new UnableToInterruptJobException("The job " + getJobDescription().name() + " can't be interrupted.");
    }
    
    /**
     * You have to return true if your implementation handles the interruption of your job.<p>
     * To handle the interruption, you have to check periodically the method <code>isJobInterrupted</code><br>
     * If this method returns false, <code>interrupt</code> throws a <code>UnableToInterruptJobException</code> exception.
     * 
     * @return <code>true</code> if the job can be interrupted, <code>false</code> otherwise.
     * @see #isJobInterrupted()
     * @see #interrupt()
     */
    abstract public boolean isJobInterruptible();
    
    /**
     * Check this method periodically to give a chance to interrupt the job.<p>
     * The main cause is a user clicking on the stop button in the ERQSJobInformation component.
     * 
     * @return <code>true</code> if a user is asking to stop the job
     */
    protected boolean isJobInterrupted()
    {
    	return jobInterrupted;
    }
}

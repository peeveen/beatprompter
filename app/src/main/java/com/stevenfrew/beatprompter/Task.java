package com.stevenfrew.beatprompter;

import android.util.Log;

public abstract class Task implements Runnable
{
    private boolean mRunning;
    private boolean mStop=false;
    private final Object runningSync=new Object();
    private final Object stopSync=new Object();

    private final static String TASKTAG="task";

    public Task(boolean initialRunningState)
    {
        mRunning=initialRunningState;
    }
    private boolean getIsRunning()
    {
        synchronized (runningSync)
        {
            return mRunning;
        }
    }
    private void setIsRunning(boolean value)
    {
        synchronized (runningSync)
        {
            mRunning=value;
        }
    }
    protected boolean getShouldStop()
    {
        synchronized (stopSync)
        {
            return mStop;
        }
    }
    protected void setShouldStop(boolean value)
    {
        synchronized (stopSync)
        {
            mStop=value;
        }
    }
    public void run()
    {
        Log.d(TASKTAG,"Task initialising.");
        initialise();
        Log.d(TASKTAG,"Task starting.");
        while(!getShouldStop())
        {
            if(getIsRunning())
            {
                doWork();
            }
            else
            {
                try
                {
                    Thread.sleep(500);
                }
                catch(InterruptedException ie)
                {
                    Log.d(TASKTAG,"MIDI thread sleep (while paused) was interrupted.",ie);
                }
            }
        }
        Log.d(TASKTAG,"Task ending ... cleaning up.");
        cleanup();
        Log.d(TASKTAG,"Task ended.");
    }
    void stop()
    {
        setIsRunning(false);
        setShouldStop(true);
    }
    void initialise()
    {
    }
    void cleanup()
    {
    }
    private void pause()
    {
        setIsRunning(false);
    }
    private void resume()
    {
        setIsRunning(true);
    }
    public abstract void doWork();

    static void pauseTask(Task task,Thread thread) {
        if(task!=null) {
            task.pause();
            if(thread!=null)
                thread.interrupt();
        }
    }

    static void resumeTask(Task task) {
        if(task!=null) {
            task.resume();
        }
    }

    static void stopTask(Task task,Thread thread) {
        if(task!=null) {
            task.stop();
            if(thread!=null) {
                thread.interrupt();
                try {
                    thread.join();
                } catch (InterruptedException ie) {
                    Log.d(TASKTAG, "Task interrupted while waiting for join.", ie);
                }
            }
        }
    }
}

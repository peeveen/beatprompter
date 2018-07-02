package com.stevenfrew.beatprompter.midi;

import android.util.Log;

import com.stevenfrew.beatprompter.BeatPrompterApplication;
import com.stevenfrew.beatprompter.Task;
import com.stevenfrew.beatprompter.Utils;

public class ClockSignalGeneratorTask extends Task
{
    private double mLastSignalTime=0.0;
    private int mClockSignalsSent=0;
    private double mNanoSecondsPerMidiSignal=0.0;
    private double mNextNanoSecondsPerMidiSignal=0.0;
    private final Object nanoSecondsPerMidiSignalSync=new Object();
    private final Object nextNanoSecondsPerMidiSignalSync=new Object();
    private final Object lastSignalTimeSync=new Object();
    private final Object clockSignalsSentSync=new Object();
    private boolean mRegistered;

    public ClockSignalGeneratorTask(boolean registered)
    {
        super(false);
        mRegistered=registered;
    }
    private double getNanoSecondsPerMidiSignal()
    {
        synchronized(nanoSecondsPerMidiSignalSync)
        {
            return mNanoSecondsPerMidiSignal;
        }
    }
    private void setNanoSecondsPerMidiSignal(double value)
    {
        synchronized(nanoSecondsPerMidiSignalSync)
        {
            mNanoSecondsPerMidiSignal=value;
        }
    }
    private double getNextNanoSecondsPerMidiSignal()
    {
        synchronized(nextNanoSecondsPerMidiSignalSync)
        {
            return mNextNanoSecondsPerMidiSignal;
        }
    }
    private void setNextNanoSecondsPerMidiSignal(double value)
    {
        synchronized(nextNanoSecondsPerMidiSignalSync)
        {
            mNextNanoSecondsPerMidiSignal=value;
        }
    }
    private double getLastSignalTime()
    {
        synchronized(lastSignalTimeSync)
        {
            return mLastSignalTime;
        }
    }
    private void setLastSignalTime(double value)
    {
        synchronized(lastSignalTimeSync)
        {
            mLastSignalTime=value;
        }
    }
    private int getClockSignalsSent()
    {
        synchronized(clockSignalsSentSync)
        {
            return mClockSignalsSent;
        }
    }
    private int incrementClockSignalsSent()
    {
        synchronized(clockSignalsSentSync)
        {
            return ++mClockSignalsSent;
        }
    }
    private void resetClockSignalsSent()
    {
        synchronized(clockSignalsSentSync)
        {
            mClockSignalsSent=0;
        }
    }
    public void doWork()
    {
        double nanoSecondsPerMidiSignal=getNextNanoSecondsPerMidiSignal();

        // No speed set yet? Just return.
        if(nanoSecondsPerMidiSignal==0.0)
            return;

        // If we're on a 24-signal boundary, switch to the next speed.
        if (getClockSignalsSent() == 0)
            setNanoSecondsPerMidiSignal(nanoSecondsPerMidiSignal);
        long nanoTime = System.nanoTime();
        double nanoDiff = nanoTime - getLastSignalTime();
        boolean signalSent = false;
        while (nanoDiff >= getNanoSecondsPerMidiSignal()) {
            try {
                signalSent = true;
                Controller.mMIDIOutQueue.put(new ClockMessage());
                // We've hit the 24-signal boundary. Switch to the next speed.
                if (incrementClockSignalsSent() == 24) {
                    resetClockSignalsSent();
                    setNanoSecondsPerMidiSignal(getNextNanoSecondsPerMidiSignal());
                }
            } catch (Exception e) {
                Log.d(BeatPrompterApplication.TAG, "Failed to add MIDI timing clock signal to output queue.", e);
            }
            nanoDiff -= getNanoSecondsPerMidiSignal();
        }
        if (signalSent) {
            double lastSignalTime = nanoTime - nanoDiff;
            setLastSignalTime(lastSignalTime);
            double nextSignalDue = lastSignalTime + getNanoSecondsPerMidiSignal();
            long nextSignalDueNano = (long) nextSignalDue - nanoTime;
            if(nextSignalDueNano>0) {
                long nextSignalDueMilli = Utils.nanoToMilli(nextSignalDueNano);
                long nextSignalDueNanoRoundedToMilli = Utils.milliToNano(nextSignalDueMilli);
                int nextSignalDueNanoRemainder = (int) (nextSignalDueNanoRoundedToMilli > 0 ? (nextSignalDueNano % nextSignalDueNanoRoundedToMilli) : nextSignalDueNano);
                try {
                    Thread.sleep(nextSignalDueMilli, nextSignalDueNanoRemainder);
                } catch (Exception e) {
                    Log.e(BeatPrompterApplication.TAG, "Thread sleep was interrupted.", e);
                }
            }
        }
    }
    public void setBPM(double bpm)
    {
        if(bpm==0.0)
            return;
        if(getShouldStop())
            return;
        double oldNanoSecondsPerMidiSignal=getNextNanoSecondsPerMidiSignal();
        double newNanosecondsPerMidiSignal=Utils.bpmToMIDIClockNanoseconds(bpm+(mRegistered?0:(Math.random()*20)));
        if(oldNanoSecondsPerMidiSignal==0.0)
        {
            // This is the first BPM value being set.
            resetClockSignalsSent();
            setLastSignalTime(System.nanoTime());
            try
            {
                Controller.mMIDIOutQueue.put(new StartMessage());
            }
            catch(Exception e)
            {
                Log.d(BeatPrompterApplication.TAG,"Failed to add MIDI start signal to output queue.",e);
            }
            setNanoSecondsPerMidiSignal(newNanosecondsPerMidiSignal);
            setNextNanoSecondsPerMidiSignal(newNanosecondsPerMidiSignal);
        }
        else
            setNextNanoSecondsPerMidiSignal(newNanosecondsPerMidiSignal);
    }
    public void stop()
    {
        super.stop();
        setLastSignalTime(0);
        resetClockSignalsSent();
        try
        {
            Controller.mMIDIOutQueue.put(new StopMessage());
        }
        catch(Exception e)
        {
            Log.d(BeatPrompterApplication.TAG,"Failed to add MIDI stop signal to output queue.",e);
        }
    }
}



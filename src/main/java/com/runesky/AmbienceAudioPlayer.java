package com.runesky;

import java.io.File;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.BooleanControl;
import javax.sound.sampled.Clip;

public class AmbienceAudioPlayer
{
    private Clip clip;
    private String loadedAudioPath;
    private long pausedMicroseconds = 0;

    public boolean load(String audioFilePath)
    {
        if (audioFilePath == null || audioFilePath.trim().isEmpty())
        {
            close();
            return false;
        }

        if (clip != null && audioFilePath.equals(loadedAudioPath))
        {
            return true;
        }

        closeWithoutResettingPosition();

        try
        {
            File file = new File(audioFilePath);

            if (!file.exists() || !file.isFile())
            {
                clip = null;
                loadedAudioPath = null;
                return false;
            }

            try (AudioInputStream stream = AudioSystem.getAudioInputStream(file))
            {
                clip = AudioSystem.getClip();
                clip.open(stream);
                loadedAudioPath = audioFilePath;
            }

            return true;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            clip = null;
            loadedAudioPath = null;
            return false;
        }
    }

    public boolean resume(String audioFilePath, boolean muted)
    {
        boolean loaded = load(audioFilePath);

        if (!loaded || clip == null)
        {
            return false;
        }

        setMuted(muted);

        if (clip.isRunning())
        {
            return true;
        }

        if (pausedMicroseconds >= clip.getMicrosecondLength())
        {
            pausedMicroseconds = 0;
        }

        clip.setMicrosecondPosition(pausedMicroseconds);
        clip.loop(Clip.LOOP_CONTINUOUSLY);
        clip.start();

        return true;
    }

    public void pause()
    {
        if (clip != null)
        {
            pausedMicroseconds = clip.getMicrosecondPosition();
            clip.stop();
        }
    }

    public void reset()
    {
        pausedMicroseconds = 0;

        if (clip != null)
        {
            clip.stop();
            clip.setMicrosecondPosition(0);
        }
    }

    public void seekByMs(long deltaMs)
    {
        seekToMs(getPlaybackElapsedMs() + deltaMs);
    }

    public void seekToMs(long targetMs)
    {
        long targetMicroseconds = Math.max(0, targetMs * 1000L);

        if (clip == null)
        {
            pausedMicroseconds = targetMicroseconds;
            return;
        }

        boolean wasRunning = clip.isRunning();

        long length = clip.getMicrosecondLength();

        if (length > 0)
        {
            targetMicroseconds = Math.floorMod(targetMicroseconds, length);
        }

        if (wasRunning)
        {
            clip.stop();
        }

        clip.setMicrosecondPosition(targetMicroseconds);
        pausedMicroseconds = targetMicroseconds;

        if (wasRunning)
        {
            clip.loop(Clip.LOOP_CONTINUOUSLY);
            clip.start();
        }
    }

    public void setMuted(boolean muted)
    {
        if (clip == null)
        {
            return;
        }

        try
        {
            if (clip.isControlSupported(BooleanControl.Type.MUTE))
            {
                BooleanControl muteControl = (BooleanControl) clip.getControl(BooleanControl.Type.MUTE);
                muteControl.setValue(muted);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public long getPlaybackElapsedMs()
    {
        if (clip != null)
        {
            return clip.getMicrosecondPosition() / 1000L;
        }

        return pausedMicroseconds / 1000L;
    }

    public long getPlaybackLengthMs()
    {
        if (clip != null)
        {
            return clip.getMicrosecondLength() / 1000L;
        }

        return 0L;
    }

    public boolean isPlaying()
    {
        return clip != null && clip.isRunning();
    }

    public void close()
    {
        if (clip != null)
        {
            clip.stop();
            clip.close();
            clip = null;
        }

        loadedAudioPath = null;
        pausedMicroseconds = 0;
    }

    private void closeWithoutResettingPosition()
    {
        if (clip != null)
        {
            pausedMicroseconds = clip.getMicrosecondPosition();
            clip.stop();
            clip.close();
            clip = null;
        }

        loadedAudioPath = null;
    }
}
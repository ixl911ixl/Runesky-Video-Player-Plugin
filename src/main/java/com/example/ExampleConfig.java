package com.example;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("videoskyboxtest")
public interface ExampleConfig extends Config
{
	enum VideoAlignment
	{
		LEFT,
		CENTER,
		RIGHT
	}

	@ConfigItem(
			keyName = "sidebarControlsNotice",
			name = "Controls Location",
			description = "Video selection and playback are handled in the side-panel. The sidebar icon looks like a TV and should be the very bottom one on your RuneLite sidebar for convenience.",
			position = 0
	)
	default String sidebarControlsNotice()
	{
		return "Use the side-panel retro TV icon for media selection and playback controls. The sidebar icon looks like a TV and should be the very bottom one on your RuneLite sidebar for convenience!";
	}

	@ConfigItem(
			keyName = "packFolderPath",
			name = "Media Playback Folder",
			description = "Folder containing a frames folder and audio.wav file",
			position = 1,
			hidden = true
	)
	default String packFolderPath()
	{
		return "F:\\- - - - Runelite Plugin Shit idk\\VideoSkybox\\Packs\\Superjail S01E01";
	}

	@ConfigItem(
			keyName = "restartMedia",
			name = "Restart Media",
			description = "Toggle this to restart the current media from the beginning",
			position = 2,
			hidden = true
	)
	default boolean restartMedia()
	{
		return false;
	}

	@ConfigItem(
			keyName = "fullScreen",
			name = "Full Screen",
			description = "Draw the video across the full game viewport",
			position = 3,
			hidden = true
	)
	default boolean fullScreen()
	{
		return false;
	}

	@ConfigItem(
			keyName = "videoAlignment",
			name = "Alignment",
			description = "Horizontal placement of the video layer",
			position = 4,
			hidden = true
	)
	default VideoAlignment videoAlignment()
	{
		return VideoAlignment.CENTER;
	}

	@Range(min = 10, max = 150)
	@ConfigItem(
			keyName = "videoWidthPercent",
			name = "Video Width %",
			description = "How wide the video layer is",
			position = 5,
			hidden = true
	)
	default int videoWidthPercent()
	{
		return 100;
	}

	@Range(min = 10, max = 100)
	@ConfigItem(
			keyName = "videoHeightPercent",
			name = "Video Height %",
			description = "How tall the video layer is",
			position = 6,
			hidden = true
	)
	default int videoHeightPercent()
	{
		return 30;
	}

	@Range(min = 0, max = 100)
	@ConfigItem(
			keyName = "videoOpacityPercent",
			name = "Video Opacity %",
			description = "Opacity of the video layer",
			position = 7,
			hidden = true
	)
	default int videoOpacityPercent()
	{
		return 80;
	}

	@Range(min = -1000, max = 1000)
	@ConfigItem(
			keyName = "videoXOffset",
			name = "Video X Offset px",
			description = "Moves the video left or right",
			position = 8,
			hidden = true
	)
	default int videoXOffset()
	{
		return 0;
	}

	@Range(min = -1000, max = 1000)
	@ConfigItem(
			keyName = "videoYOffset",
			name = "Video Y Offset px",
			description = "Moves the video up or down",
			position = 9,
			hidden = true
	)
	default int videoYOffset()
	{
		return 0;
	}

	@Range(min = 0, max = 400)
	@ConfigItem(
			keyName = "bottomFadePixels",
			name = "Bottom Fade px",
			description = "How softly the video fades at the bottom edge",
			position = 10,
			hidden = true
	)
	default int bottomFadePixels()
	{
		return 90;
	}

	@ConfigItem(
			keyName = "muteAudio",
			name = "Mute Audio",
			description = "Mutes the audio while keeping playback timing active",
			position = 11,
			hidden = true
	)
	default boolean muteAudio()
	{
		return false;
	}

	@Range(min = -1000, max = 1000)
	@ConfigItem(
			keyName = "audioSyncOffsetMs",
			name = "Audio Sync Offset ms",
			description = "Fine-tunes video timing against audio. Positive makes video later, negative makes it earlier.",
			position = 12,
			hidden = true
	)
	default int audioSyncOffsetMs()
	{
		return 0;
	}
}
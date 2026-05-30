package com.example;

import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.swing.JFileChooser;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;

@Slf4j
@PluginDescriptor(
		name = "Runesky Video Player"
)
public class ExamplePlugin extends Plugin
{
	private static final String CONFIG_GROUP = "videoskyboxtest";

	@Inject
	private ExampleConfig config;

	@Inject
	private ConfigManager configManager;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private ExampleOverlay overlay;

	@Inject
	private ClientToolbar clientToolbar;

	private final AmbienceAudioPlayer audioPlayer = new AmbienceAudioPlayer();

	private VideoAmbiencePanel panel;
	private NavigationButton navButton;

	@Getter
	private String packStatus = "No media playback folder selected.";

	@Getter
	private boolean packValid = false;

	@Override
	protected void startUp() throws Exception
	{
		overlay.setPlaybackClockMsSupplier(audioPlayer::getPlaybackElapsedMs);
		overlay.setPackStatusSupplier(this::getPackStatus);

		panel = new VideoAmbiencePanel(this);

		navButton = NavigationButton.builder()
				.tooltip("Runesky Video Player Controls")
				.icon(createSidebarIcon())
				.priority(Integer.MIN_VALUE)
				.panel(panel)
				.build();

		clientToolbar.addNavigation(navButton);
		overlayManager.add(overlay);

		loadPackPaused();

		log.debug("Runesky Video Player started!");
	}

	@Override
	protected void shutDown() throws Exception
	{
		audioPlayer.pause();
		overlay.pausePlayback();

		overlayManager.remove(overlay);

		if (navButton != null)
		{
			clientToolbar.removeNavigation(navButton);
			navButton = null;
		}

		panel = null;

		log.debug("Runesky Video Player paused!");
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!CONFIG_GROUP.equals(event.getGroup()))
		{
			return;
		}

		switch (event.getKey())
		{
			case "packFolderPath":
				loadPackPaused();
				break;

			case "restartMedia":
				restartMedia();
				break;

			case "muteAudio":
				audioPlayer.setMuted(config.muteAudio());
				break;

			default:
				break;
		}

		updatePanelStatus();
	}

	public void choosePackFolder()
	{
		JFileChooser chooser = new JFileChooser(config.packFolderPath());
		chooser.setDialogTitle("Select Media Playback Folder");
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooser.setAcceptAllFileFilterUsed(false);

		int result = chooser.showOpenDialog(null);

		if (result != JFileChooser.APPROVE_OPTION || chooser.getSelectedFile() == null)
		{
			return;
		}

		String selectedPath = chooser.getSelectedFile().getAbsolutePath();

		configManager.setConfiguration(CONFIG_GROUP, "packFolderPath", selectedPath);

		loadPackPausedFromPath(selectedPath);
		updatePanelStatus();
	}

	public void pauseMedia()
	{
		audioPlayer.pause();
		overlay.pausePlayback();

		packStatus = "Paused.";
		updatePanelStatus();
	}

	public void resumeMedia()
	{
		if (audioPlayer.isPlaying())
		{
			packStatus = "Playing.";
			updatePanelStatus();
			return;
		}

		if (!packValid)
		{
			loadPackPaused();
			return;
		}

		boolean audioLoaded = audioPlayer.resume(getAudioFilePath(), config.muteAudio());

		if (!audioLoaded)
		{
			packValid = false;
			packStatus = "Could not load audio.wav.";
			overlay.pausePlayback();
			updatePanelStatus();
			return;
		}

		overlay.setPausedElapsedMs(audioPlayer.getPlaybackElapsedMs());
		overlay.resumePlayback();

		packStatus = "Playing.";
		updatePanelStatus();
	}

	public void restartMedia()
	{
		if (!validatePack(config.packFolderPath()))
		{
			packValid = false;
			audioPlayer.pause();
			overlay.pausePlayback();
			overlay.forceReloadFrames();
			updatePanelStatus();
			return;
		}

		audioPlayer.reset();
		overlay.resetPlayback();

		boolean audioLoaded = audioPlayer.resume(getAudioFilePath(), config.muteAudio());

		if (!audioLoaded)
		{
			packValid = false;
			packStatus = "Could not load audio.wav.";
			overlay.pausePlayback();
			updatePanelStatus();
			return;
		}

		packValid = true;
		overlay.forceReloadFrames();
		overlay.resumePlayback();

		packStatus = "Restarted media.";
		updatePanelStatus();
	}

	public void seekByMs(long deltaMs)
	{
		if (!packValid)
		{
			return;
		}

		audioPlayer.seekByMs(deltaMs);
		overlay.setPausedElapsedMs(audioPlayer.getPlaybackElapsedMs());

		packStatus = "Seeked to " + formatTime(audioPlayer.getPlaybackElapsedMs()) + ".";
		updatePanelStatus();
	}

	public void seekToMs(long targetMs)
	{
		if (!packValid)
		{
			return;
		}

		audioPlayer.seekToMs(targetMs);
		overlay.setPausedElapsedMs(audioPlayer.getPlaybackElapsedMs());

		updatePanelStatus();
	}

	public void setFullScreen(boolean value)
	{
		configManager.setConfiguration(CONFIG_GROUP, "fullScreen", value);
		updatePanelStatus();
	}

	public void setVideoAlignment(ExampleConfig.VideoAlignment value)
	{
		configManager.setConfiguration(CONFIG_GROUP, "videoAlignment", value);
		updatePanelStatus();
	}

	public void setVideoWidthPercent(int value)
	{
		configManager.setConfiguration(CONFIG_GROUP, "videoWidthPercent", clamp(value, 10, 150));
		updatePanelStatus();
	}

	public void setVideoHeightPercent(int value)
	{
		configManager.setConfiguration(CONFIG_GROUP, "videoHeightPercent", clamp(value, 10, 100));
		updatePanelStatus();
	}

	public void setVideoOpacityPercent(int value)
	{
		configManager.setConfiguration(CONFIG_GROUP, "videoOpacityPercent", clamp(value, 0, 100));
		updatePanelStatus();
	}

	public void setVideoXOffset(int value)
	{
		configManager.setConfiguration(CONFIG_GROUP, "videoXOffset", clamp(value, -1000, 1000));
		updatePanelStatus();
	}

	public void setVideoYOffset(int value)
	{
		configManager.setConfiguration(CONFIG_GROUP, "videoYOffset", clamp(value, -1000, 1000));
		updatePanelStatus();
	}

	public void setBottomFadePixels(int value)
	{
		configManager.setConfiguration(CONFIG_GROUP, "bottomFadePixels", clamp(value, 0, 400));
		updatePanelStatus();
	}

	public void setMuteAudio(boolean value)
	{
		configManager.setConfiguration(CONFIG_GROUP, "muteAudio", value);
		audioPlayer.setMuted(value);
		updatePanelStatus();
	}

	public void setAudioSyncOffsetMs(int value)
	{
		configManager.setConfiguration(CONFIG_GROUP, "audioSyncOffsetMs", clamp(value, -1000, 1000));
		updatePanelStatus();
	}

	public long getPlaybackElapsedMs()
	{
		return audioPlayer.getPlaybackElapsedMs();
	}

	public long getPlaybackLengthMs()
	{
		return audioPlayer.getPlaybackLengthMs();
	}

	public boolean isMediaPlaying()
	{
		return audioPlayer.isPlaying();
	}

	public String getCurrentPackFolderPath()
	{
		return config.packFolderPath();
	}

	public boolean isFullScreen()
	{
		return config.fullScreen();
	}

	public ExampleConfig.VideoAlignment getVideoAlignment()
	{
		return config.videoAlignment();
	}

	public int getVideoWidthPercent()
	{
		return config.videoWidthPercent();
	}

	public int getVideoHeightPercent()
	{
		return config.videoHeightPercent();
	}

	public int getVideoOpacityPercent()
	{
		return config.videoOpacityPercent();
	}

	public int getVideoXOffset()
	{
		return config.videoXOffset();
	}

	public int getVideoYOffset()
	{
		return config.videoYOffset();
	}

	public int getBottomFadePixels()
	{
		return config.bottomFadePixels();
	}

	public boolean isMuteAudio()
	{
		return config.muteAudio();
	}

	public int getAudioSyncOffsetMs()
	{
		return config.audioSyncOffsetMs();
	}

	private void loadPackPaused()
	{
		loadPackPausedFromPath(config.packFolderPath());
	}

	private void loadPackPausedFromPath(String packFolderPath)
	{
		packValid = validatePack(packFolderPath);

		audioPlayer.pause();
		overlay.pausePlayback();

		if (!packValid)
		{
			overlay.forceReloadFrames();
			log.warn("Invalid media playback folder: {}", packStatus);
			updatePanelStatus();
			return;
		}

		audioPlayer.reset();
		overlay.resetPlayback();
		overlay.forceReloadFrames();

		boolean audioLoaded = audioPlayer.load(getAudioFilePath(packFolderPath));

		if (!audioLoaded)
		{
			packValid = false;
			packStatus = "Could not load audio.wav.";
			overlay.pausePlayback();
			log.warn("Failed to load media audio: {}", getAudioFilePath(packFolderPath));
			updatePanelStatus();
			return;
		}

		audioPlayer.setMuted(config.muteAudio());
		packStatus = "Ready. Press Play.";
		updatePanelStatus();

		log.debug("Runesky Video Player media loaded paused!");
	}

	private boolean validatePack(String packFolderPath)
	{
		if (packFolderPath == null || packFolderPath.trim().isEmpty())
		{
			packStatus = "No media playback folder selected.";
			return false;
		}

		Path packFolder = Paths.get(packFolderPath.trim());

		if (!Files.exists(packFolder) || !Files.isDirectory(packFolder))
		{
			packStatus = "Media playback folder not found.";
			return false;
		}

		Path framesFolder = packFolder.resolve("frames");

		if (!Files.exists(framesFolder) || !Files.isDirectory(framesFolder))
		{
			packStatus = "Missing frames folder.";
			return false;
		}

		Path audioFile = packFolder.resolve("audio.wav");

		if (!Files.exists(audioFile) || !Files.isRegularFile(audioFile))
		{
			packStatus = "Missing audio.wav.";
			return false;
		}

		long frameCount;

		try (Stream<Path> paths = Files.list(framesFolder))
		{
			frameCount = paths
					.filter(path ->
					{
						String name = path.getFileName().toString().toLowerCase();
						return name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png");
					})
					.count();
		}
		catch (Exception e)
		{
			packStatus = "Could not read frames folder.";
			return false;
		}

		if (frameCount <= 0)
		{
			packStatus = "No image frames found.";
			return false;
		}

		packStatus = "Media folder valid: " + frameCount + " frames.";
		return true;
	}

	private String getAudioFilePath()
	{
		return getAudioFilePath(config.packFolderPath());
	}

	private String getAudioFilePath(String packFolderPath)
	{
		return Paths.get(packFolderPath, "audio.wav").toString();
	}

	private void updatePanelStatus()
	{
		if (panel != null)
		{
			panel.updateStatus();
		}
	}

	private String formatTime(long ms)
	{
		long totalSeconds = Math.max(0, ms / 1000L);
		long minutes = totalSeconds / 60L;
		long seconds = totalSeconds % 60L;

		return String.format("%d:%02d", minutes, seconds);
	}

	private int clamp(int value, int min, int max)
	{
		return Math.max(min, Math.min(max, value));
	}

	private BufferedImage createSidebarIcon()
	{
		BufferedImage icon = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = icon.createGraphics();

		g.setColor(new Color(150, 235, 255, 70));
		g.drawRoundRect(1, 3, 12, 10, 4, 4);
		g.setColor(new Color(150, 235, 255, 40));
		g.drawRoundRect(0, 2, 14, 12, 5, 5);

		g.setColor(new Color(150, 235, 255, 90));
		g.drawLine(5, 3, 3, 0);
		g.drawLine(9, 3, 11, 0);

		g.setColor(new Color(92, 62, 40));
		g.fillRoundRect(2, 4, 10, 8, 3, 3);

		g.setColor(new Color(60, 40, 24));
		g.drawRoundRect(2, 4, 10, 8, 3, 3);

		g.setColor(new Color(170, 210, 220));
		g.fillRect(4, 6, 6, 4);

		g.setColor(new Color(245, 245, 245));
		g.drawLine(4, 6, 9, 9);
		g.drawLine(5, 6, 4, 7);
		g.drawLine(7, 6, 5, 8);
		g.drawLine(9, 6, 6, 9);

		g.setColor(new Color(235, 235, 235));
		g.drawLine(5, 3, 3, 0);
		g.drawLine(9, 3, 11, 0);

		g.setColor(new Color(40, 40, 40));
		g.fillOval(11, 7, 1, 1);
		g.fillOval(11, 9, 1, 1);

		g.setColor(new Color(60, 40, 24));
		g.drawLine(4, 12, 3, 14);
		g.drawLine(10, 12, 11, 14);

		g.dispose();

		return icon;
	}

	@Provides
	ExampleConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ExampleConfig.class);
	}
}
package com.example;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

public class ExampleOverlay extends Overlay
{
    private static final int FPS = 24;

    private final Client client;
    private final ExampleConfig config;

    private List<Path> framePaths;
    private String loadedFramesFolder = "";

    private LongSupplier playbackClockMsSupplier;
    private Supplier<String> packStatusSupplier;

    private long playbackStartTime;
    private long pausedElapsedMs = 0;
    private boolean paused = true;

    private int lastFrameIndex = -1;
    private BufferedImage currentFrame;

    private String frameStatus = "No frames loaded.";

    @Inject
    private ExampleOverlay(Client client, ExampleConfig config)
    {
        this.client = client;
        this.config = config;

        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ALWAYS_ON_TOP);

        playbackStartTime = System.currentTimeMillis();
    }

    public void setPlaybackClockMsSupplier(LongSupplier playbackClockMsSupplier)
    {
        this.playbackClockMsSupplier = playbackClockMsSupplier;
    }

    public void setPackStatusSupplier(Supplier<String> packStatusSupplier)
    {
        this.packStatusSupplier = packStatusSupplier;
    }

    public void forceReloadFrames()
    {
        loadedFramesFolder = "";
        framePaths = null;
        lastFrameIndex = -1;
        currentFrame = null;
        frameStatus = "Reloading frames...";
    }

    private void loadFrameListIfNeeded()
    {
        Path framesFolder = Paths.get(config.packFolderPath(), "frames");
        String folder = framesFolder.toString();

        if (folder.equals(loadedFramesFolder) && framePaths != null)
        {
            return;
        }

        loadedFramesFolder = folder;
        lastFrameIndex = -1;
        currentFrame = null;

        if (!Files.exists(framesFolder) || !Files.isDirectory(framesFolder))
        {
            framePaths = List.of();
            frameStatus = "Missing frames folder.";
            return;
        }

        try
        {
            framePaths = Files.list(framesFolder)
                    .filter(path ->
                    {
                        String name = path.getFileName().toString().toLowerCase();
                        return name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png");
                    })
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .collect(Collectors.toList());

            if (framePaths.isEmpty())
            {
                frameStatus = "No image frames found.";
            }
            else
            {
                frameStatus = "Loaded " + framePaths.size() + " frames.";
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
            framePaths = List.of();
            frameStatus = "Could not read frames folder.";
        }
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        loadFrameListIfNeeded();

        if (framePaths == null || framePaths.isEmpty())
        {
            drawStatusIfNeeded(graphics);
            return null;
        }

        BufferedImage frame = getCurrentVideoFrame();

        if (frame == null)
        {
            drawStatusIfNeeded(graphics);
            return null;
        }

        Rectangle viewport = new Rectangle(
                client.getViewportXOffset(),
                client.getViewportYOffset(),
                client.getViewportWidth(),
                client.getViewportHeight()
        );

        if (viewport.width <= 0 || viewport.height <= 0)
        {
            return null;
        }

        int drawW;
        int drawH;

        if (config.fullScreen())
        {
            drawW = viewport.width;
            drawH = viewport.height;
        }
        else
        {
            drawW = Math.max(1, viewport.width * config.videoWidthPercent() / 100);
            drawH = Math.max(1, viewport.height * config.videoHeightPercent() / 100);
        }

        int drawX = getAlignedX(viewport, drawW) + config.videoXOffset();
        int drawY = viewport.y + config.videoYOffset();

        BufferedImage layer = new BufferedImage(drawW, drawH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = layer.createGraphics();

        g.drawImage(frame, 0, 0, drawW, drawH, null);

        int bottomFadePixels = config.bottomFadePixels();

        if (bottomFadePixels > 0)
        {
            g.setComposite(AlphaComposite.DstIn);

            int fadeStartY = Math.max(0, drawH - bottomFadePixels);

            GradientPaint fade = new GradientPaint(
                    0,
                    fadeStartY,
                    new Color(255, 255, 255, 255),
                    0,
                    drawH,
                    new Color(255, 255, 255, 0)
            );

            g.setPaint(fade);
            g.fillRect(0, fadeStartY, drawW, Math.min(bottomFadePixels, drawH));
        }

        g.dispose();

        Composite oldComposite = graphics.getComposite();

        float opacity = Math.max(0, Math.min(100, config.videoOpacityPercent())) / 100f;

        graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
        graphics.drawImage(layer, drawX, drawY, null);

        graphics.setComposite(oldComposite);

        return null;
    }

    private int getAlignedX(Rectangle viewport, int drawW)
    {
        switch (config.videoAlignment())
        {
            case LEFT:
                return viewport.x;

            case RIGHT:
                return viewport.x + viewport.width - drawW;

            case CENTER:
            default:
                return viewport.x + (viewport.width - drawW) / 2;
        }
    }

    public void resumePlayback()
    {
        if (!paused)
        {
            return;
        }

        playbackStartTime = System.currentTimeMillis() - pausedElapsedMs;
        paused = false;
    }

    public void pausePlayback()
    {
        if (paused)
        {
            return;
        }

        pausedElapsedMs = getRawPlaybackElapsedMs();
        paused = true;
    }

    public void resetPlayback()
    {
        pausedElapsedMs = 0;
        playbackStartTime = System.currentTimeMillis();
        lastFrameIndex = -1;
        currentFrame = null;
    }

    public void setPausedElapsedMs(long elapsedMs)
    {
        pausedElapsedMs = Math.max(0, elapsedMs);
        playbackStartTime = System.currentTimeMillis() - pausedElapsedMs;
        lastFrameIndex = -1;
        currentFrame = null;
    }

    private BufferedImage getCurrentVideoFrame()
    {
        long elapsedMs = getRawPlaybackElapsedMs() - config.audioSyncOffsetMs();

        if (elapsedMs < 0)
        {
            elapsedMs = 0;
        }

        long frameNumber = (elapsedMs * FPS) / 1000L;
        int frameIndex = Math.floorMod(frameNumber, framePaths.size());

        if (frameIndex != lastFrameIndex)
        {
            try
            {
                currentFrame = ImageIO.read(framePaths.get(frameIndex).toFile());
                lastFrameIndex = frameIndex;

                if (currentFrame == null)
                {
                    frameStatus = "Could not read frame: " + framePaths.get(frameIndex).getFileName();
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
                frameStatus = "Could not read frame: " + framePaths.get(frameIndex).getFileName();
                return null;
            }
        }

        return currentFrame;
    }

    private long getRawPlaybackElapsedMs()
    {
        if (!paused && playbackClockMsSupplier != null)
        {
            return playbackClockMsSupplier.getAsLong();
        }

        if (paused)
        {
            return pausedElapsedMs;
        }

        return System.currentTimeMillis() - playbackStartTime;
    }

    private void drawStatusIfNeeded(Graphics2D graphics)
    {
        Rectangle viewport = new Rectangle(
                client.getViewportXOffset(),
                client.getViewportYOffset(),
                client.getViewportWidth(),
                client.getViewportHeight()
        );

        if (viewport.width <= 0 || viewport.height <= 0)
        {
            return;
        }

        String pluginStatus = packStatusSupplier != null ? packStatusSupplier.get() : "";
        String status = pluginStatus;

        if (status == null || status.trim().isEmpty() || "Playing.".equals(status) || "Paused.".equals(status) || "Restarted media.".equals(status))
        {
            status = frameStatus;
        }

        if (status == null || status.trim().isEmpty())
        {
            return;
        }

        FontMetrics metrics = graphics.getFontMetrics();

        int padding = 8;
        int textWidth = metrics.stringWidth(status);
        int textHeight = metrics.getHeight();

        int boxWidth = textWidth + padding * 2;
        int boxHeight = textHeight + padding * 2;

        int x = viewport.x + 12;
        int y = viewport.y + 12;

        Color oldColor = graphics.getColor();

        graphics.setColor(new Color(0, 0, 0, 170));
        graphics.fillRoundRect(x, y, boxWidth, boxHeight, 10, 10);

        graphics.setColor(Color.WHITE);
        graphics.drawString(status, x + padding, y + padding + metrics.getAscent());

        graphics.setColor(oldColor);
    }
}
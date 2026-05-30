package com.example;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.event.ChangeListener;
import net.runelite.client.ui.PluginPanel;

public class VideoAmbiencePanel extends PluginPanel
{
    private static final int SLIDER_MAX = 1000;
    private static final int PANEL_WIDTH = PluginPanel.PANEL_WIDTH - 20;

    private final ExamplePlugin plugin;

    private final JLabel statusLabel = new JLabel("", SwingConstants.CENTER);
    private final JLabel folderLabel = new JLabel("", SwingConstants.CENTER);
    private final JLabel timeLabel = new JLabel("0:00 / 0:00", SwingConstants.CENTER);
    private final JSlider playbackSlider = new JSlider(0, SLIDER_MAX, 0);

    private final JCheckBox fullScreenCheckBox = new JCheckBox("Full Screen");
    private final JCheckBox muteAudioCheckBox = new JCheckBox("Mute Audio");
    private final JComboBox<ExampleConfig.VideoAlignment> alignmentComboBox = new JComboBox<>(ExampleConfig.VideoAlignment.values());

    private final JSpinner widthSpinner = new JSpinner(new SpinnerNumberModel(100, 10, 150, 1));
    private final JSpinner heightSpinner = new JSpinner(new SpinnerNumberModel(30, 10, 100, 1));
    private final JSpinner opacitySpinner = new JSpinner(new SpinnerNumberModel(80, 0, 100, 1));
    private final JSpinner xOffsetSpinner = new JSpinner(new SpinnerNumberModel(0, -1000, 1000, 1));
    private final JSpinner yOffsetSpinner = new JSpinner(new SpinnerNumberModel(0, -1000, 1000, 1));
    private final JSpinner bottomFadeSpinner = new JSpinner(new SpinnerNumberModel(90, 0, 400, 1));
    private final JSpinner audioSyncOffsetSpinner = new JSpinner(new SpinnerNumberModel(0, -1000, 1000, 1));

    private boolean updatingSlider = false;
    private boolean updatingControls = false;

    public VideoAmbiencePanel(ExamplePlugin plugin)
    {
        this.plugin = plugin;

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel title = new JLabel("Runesky Video Player", SwingConstants.CENTER);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        title.setMaximumSize(new Dimension(PANEL_WIDTH, 22));

        JButton chooseFolderButton = fullWidthButton("Select Media Playback Folder");
        chooseFolderButton.addActionListener(e -> plugin.choosePackFolder());

        JPanel transportPanel = new JPanel(new GridLayout(1, 2, 4, 0));
        transportPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        transportPanel.setMaximumSize(new Dimension(PANEL_WIDTH, 24));
        transportPanel.setPreferredSize(new Dimension(PANEL_WIDTH, 24));

        JButton playButton = halfWidthButton("Play");
        playButton.addActionListener(e -> plugin.resumeMedia());

        JButton pauseButton = halfWidthButton("Pause");
        pauseButton.addActionListener(e -> plugin.pauseMedia());

        transportPanel.add(playButton);
        transportPanel.add(pauseButton);

        JPanel seekPanel = new JPanel(new GridLayout(1, 2, 4, 0));
        seekPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        seekPanel.setMaximumSize(new Dimension(PANEL_WIDTH, 24));
        seekPanel.setPreferredSize(new Dimension(PANEL_WIDTH, 24));

        JButton rewindButton = halfWidthButton("-10s");
        rewindButton.addActionListener(e -> plugin.seekByMs(-10_000));

        JButton forwardButton = halfWidthButton("+10s");
        forwardButton.addActionListener(e -> plugin.seekByMs(10_000));

        seekPanel.add(rewindButton);
        seekPanel.add(forwardButton);

        timeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        timeLabel.setMaximumSize(new Dimension(PANEL_WIDTH, 20));

        playbackSlider.setAlignmentX(Component.LEFT_ALIGNMENT);
        playbackSlider.setMaximumSize(new Dimension(PANEL_WIDTH, 28));
        playbackSlider.setPreferredSize(new Dimension(PANEL_WIDTH, 28));
        playbackSlider.setValue(0);

        playbackSlider.addChangeListener(e ->
        {
            if (updatingSlider)
            {
                return;
            }

            if (playbackSlider.getValueIsAdjusting())
            {
                updateTimeLabelFromSlider();
                return;
            }

            long lengthMs = plugin.getPlaybackLengthMs();

            if (lengthMs <= 0)
            {
                return;
            }

            long targetMs = playbackSlider.getValue() * lengthMs / SLIDER_MAX;
            plugin.seekToMs(targetMs);
        });

        JPanel placementPanel = sectionPanel("Video Placement");
        placementPanel.add(fullScreenCheckBox);
        placementPanel.add(compactRow("Alignment", alignmentComboBox));
        placementPanel.add(compactRow("Width %", widthSpinner));
        placementPanel.add(compactRow("Height %", heightSpinner));
        placementPanel.add(compactRow("Opacity %", opacitySpinner));
        placementPanel.add(compactRow("X Offset", xOffsetSpinner));
        placementPanel.add(compactRow("Y Offset", yOffsetSpinner));
        placementPanel.add(compactRow("Bottom Fade", bottomFadeSpinner));

        JPanel audioPanel = sectionPanel("Audio");
        audioPanel.add(muteAudioCheckBox);
        audioPanel.add(compactRow("Sync Offset", audioSyncOffsetSpinner));

        fullScreenCheckBox.addActionListener(e ->
        {
            if (!updatingControls)
            {
                plugin.setFullScreen(fullScreenCheckBox.isSelected());
            }
        });

        muteAudioCheckBox.addActionListener(e ->
        {
            if (!updatingControls)
            {
                plugin.setMuteAudio(muteAudioCheckBox.isSelected());
            }
        });

        alignmentComboBox.addActionListener(e ->
        {
            if (!updatingControls && alignmentComboBox.getSelectedItem() instanceof ExampleConfig.VideoAlignment)
            {
                plugin.setVideoAlignment((ExampleConfig.VideoAlignment) alignmentComboBox.getSelectedItem());
            }
        });

        addSpinnerListener(widthSpinner, () -> plugin.setVideoWidthPercent((int) widthSpinner.getValue()));
        addSpinnerListener(heightSpinner, () -> plugin.setVideoHeightPercent((int) heightSpinner.getValue()));
        addSpinnerListener(opacitySpinner, () -> plugin.setVideoOpacityPercent((int) opacitySpinner.getValue()));
        addSpinnerListener(xOffsetSpinner, () -> plugin.setVideoXOffset((int) xOffsetSpinner.getValue()));
        addSpinnerListener(yOffsetSpinner, () -> plugin.setVideoYOffset((int) yOffsetSpinner.getValue()));
        addSpinnerListener(bottomFadeSpinner, () -> plugin.setBottomFadePixels((int) bottomFadeSpinner.getValue()));
        addSpinnerListener(audioSyncOffsetSpinner, () -> plugin.setAudioSyncOffsetMs((int) audioSyncOffsetSpinner.getValue()));

        content.add(title);
        content.add(Box.createVerticalStrut(4));
        content.add(chooseFolderButton);
        content.add(Box.createVerticalStrut(6));
        content.add(timeLabel);
        content.add(playbackSlider);
        content.add(Box.createVerticalStrut(4));
        content.add(transportPanel);
        content.add(Box.createVerticalStrut(4));
        content.add(seekPanel);
        content.add(Box.createVerticalStrut(6));
        content.add(placementPanel);
        content.add(Box.createVerticalStrut(6));
        content.add(audioPanel);
        content.add(Box.createVerticalStrut(6));
        content.add(statusLabel);
        content.add(folderLabel);

        add(content, BorderLayout.NORTH);

        updateControlValuesFromConfig();

        Timer timer = new Timer(250, e -> updateStatus());
        timer.start();

        updateStatus();
    }

    public void updateStatus()
    {
        String status = plugin.getPackStatus();
        String folder = plugin.getCurrentPackFolderPath();

        if (status == null || status.trim().isEmpty())
        {
            status = "No status.";
        }

        if (folder == null || folder.trim().isEmpty())
        {
            folder = "No media playback folder selected.";
        }

        statusLabel.setText("<html><body style='text-align:center; width:180px;'><b>Status:</b> " + escapeHtml(status) + "</body></html>");
        folderLabel.setText("<html><body style='text-align:center; width:180px;'><b>Folder:</b><br>" + escapeHtml(folder) + "</body></html>");

        updatePlaybackSliderAndTime();

        revalidate();
        repaint();
    }

    private JPanel sectionPanel(String title)
    {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder(title));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(PANEL_WIDTH, 999));
        return panel;
    }

    private JButton fullWidthButton(String text)
    {
        JButton button = new JButton(text);
        button.setFocusable(false);
        button.setMaximumSize(new Dimension(PANEL_WIDTH, 24));
        button.setPreferredSize(new Dimension(PANEL_WIDTH, 24));
        button.setMinimumSize(new Dimension(60, 24));
        return button;
    }

    private JButton halfWidthButton(String text)
    {
        JButton button = new JButton(text);
        button.setFocusable(false);
        button.setPreferredSize(new Dimension((PANEL_WIDTH - 4) / 2, 24));
        button.setMinimumSize(new Dimension(40, 24));
        return button;
    }

    private JPanel compactRow(String labelText, Component component)
    {
        JPanel panel = new JPanel(new BorderLayout(4, 0));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(PANEL_WIDTH, 24));

        JLabel label = new JLabel(labelText);
        label.setPreferredSize(new Dimension(82, 22));

        component.setPreferredSize(new Dimension(96, 22));
        component.setMaximumSize(new Dimension(120, 22));

        panel.add(label, BorderLayout.WEST);
        panel.add(component, BorderLayout.CENTER);

        return panel;
    }

    private void updateControlValuesFromConfig()
    {
        updatingControls = true;

        fullScreenCheckBox.setSelected(plugin.isFullScreen());
        muteAudioCheckBox.setSelected(plugin.isMuteAudio());
        alignmentComboBox.setSelectedItem(plugin.getVideoAlignment());

        widthSpinner.setValue(plugin.getVideoWidthPercent());
        heightSpinner.setValue(plugin.getVideoHeightPercent());
        opacitySpinner.setValue(plugin.getVideoOpacityPercent());
        xOffsetSpinner.setValue(plugin.getVideoXOffset());
        yOffsetSpinner.setValue(plugin.getVideoYOffset());
        bottomFadeSpinner.setValue(plugin.getBottomFadePixels());
        audioSyncOffsetSpinner.setValue(plugin.getAudioSyncOffsetMs());

        updatingControls = false;
    }

    private void updatePlaybackSliderAndTime()
    {
        if (playbackSlider.getValueIsAdjusting())
        {
            return;
        }

        long elapsedMs = plugin.getPlaybackElapsedMs();
        long lengthMs = plugin.getPlaybackLengthMs();

        updatingSlider = true;

        if (lengthMs <= 0)
        {
            playbackSlider.setValue(0);
            timeLabel.setText("0:00 / 0:00");
        }
        else
        {
            int sliderValue = (int) Math.max(0, Math.min(SLIDER_MAX, elapsedMs * SLIDER_MAX / lengthMs));
            playbackSlider.setValue(sliderValue);
            timeLabel.setText(formatTime(elapsedMs) + " / " + formatTime(lengthMs));
        }

        updatingSlider = false;
    }

    private void updateTimeLabelFromSlider()
    {
        long lengthMs = plugin.getPlaybackLengthMs();

        if (lengthMs <= 0)
        {
            timeLabel.setText("0:00 / 0:00");
            return;
        }

        long targetMs = playbackSlider.getValue() * lengthMs / SLIDER_MAX;
        timeLabel.setText(formatTime(targetMs) + " / " + formatTime(lengthMs));
    }

    private void addSpinnerListener(JSpinner spinner, Runnable action)
    {
        ChangeListener listener = e ->
        {
            if (!updatingControls)
            {
                action.run();
            }
        };

        spinner.addChangeListener(listener);
    }

    @Override
    public Dimension getPreferredSize()
    {
        return new Dimension(PluginPanel.PANEL_WIDTH, super.getPreferredSize().height);
    }

    private String formatTime(long ms)
    {
        long totalSeconds = Math.max(0, ms / 1000L);
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;

        if (hours > 0)
        {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        }

        return String.format("%d:%02d", minutes, seconds);
    }

    private String escapeHtml(String text)
    {
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
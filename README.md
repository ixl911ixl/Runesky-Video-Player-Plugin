# Runesky Video Player

Runesky Video Player is a RuneLite plugin that displays user-provided local media as a configurable ambience layer while playing Old School RuneScape.

**Media Disclaimer:** This plugin does not download, stream, host, or provide any media. Users are responsible for creating and supplying their own local media files.

## Features

- Displays extracted video frames as an overlay
- Optional synchronized local audio playback
- Adjustable size, position, opacity, alignment, and fade settings
- Supports full-screen or custom-positioned display modes
- Uses local files only

## Media Pack Format

The plugin expects a local media pack folder containing:

- `frames/` - extracted image frames
- `audio.wav` - optional synchronized audio file

Example:

```text
MyMediaPack/
├─ frames/
│  ├─ frame_000001.jpg
│  ├─ frame_000002.jpg
│  └─ ...
└─ audio.wav
```

## Notes

This plugin is only a local display tool. It does not include media, fetch media, recommend media sources, or provide any hosted content. All files must already exist locally on the user's computer.
I am working on a more elegant solution to getting the Media split into individual frames and audio, but for now I would highly suggest the use of the freeware FFmpeg. It takes less than a few minutes to do a full episode of TV and handles both the frames and the audio at the same time. 

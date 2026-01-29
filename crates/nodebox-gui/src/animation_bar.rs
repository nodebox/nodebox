//! Compact animation bar with playback controls.

use eframe::egui;
use std::time::{Duration, Instant};
use crate::theme;

/// Animation playback state.
#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum PlaybackState {
    Stopped,
    Playing,
    Paused,
}

/// Events that can be triggered by the animation bar.
#[derive(Clone, Copy, Debug, PartialEq)]
pub enum AnimationEvent {
    None,
    Play,
    Pause,
    Stop,
    Rewind,
    StepBack,
    StepForward,
    GoToEnd,
    FrameChanged(f64),
    FpsChanged(u32),
}

/// Compact animation bar widget.
pub struct AnimationBar {
    /// Current frame number.
    frame: u32,
    /// Start frame.
    start_frame: u32,
    /// End frame (total frames).
    end_frame: u32,
    /// Frames per second.
    fps: u32,
    /// Current playback state.
    playback_state: PlaybackState,
    /// Whether to loop the animation.
    loop_enabled: bool,
    /// Time of last frame update.
    last_frame_time: Option<Instant>,
    /// Accumulated time since last frame.
    accumulated_time: Duration,
}

impl Default for AnimationBar {
    fn default() -> Self {
        Self::new()
    }
}

impl AnimationBar {
    /// Create a new animation bar.
    pub fn new() -> Self {
        Self {
            frame: 1,
            start_frame: 1,
            end_frame: 100,
            fps: 30,
            playback_state: PlaybackState::Stopped,
            loop_enabled: true,
            last_frame_time: None,
            accumulated_time: Duration::ZERO,
        }
    }

    /// Get the current frame.
    pub fn frame(&self) -> u32 {
        self.frame
    }

    /// Get the current frame as f64.
    pub fn frame_f64(&self) -> f64 {
        self.frame as f64
    }

    /// Set the current frame.
    pub fn set_frame(&mut self, frame: u32) {
        self.frame = frame.clamp(self.start_frame, self.end_frame);
    }

    /// Get the normalized time (0.0 to 1.0).
    pub fn normalized_time(&self) -> f64 {
        let range = (self.end_frame - self.start_frame) as f64;
        if range > 0.0 {
            (self.frame - self.start_frame) as f64 / range
        } else {
            0.0
        }
    }

    /// Is the animation playing?
    pub fn is_playing(&self) -> bool {
        self.playback_state == PlaybackState::Playing
    }

    /// Play the animation.
    pub fn play(&mut self) {
        self.playback_state = PlaybackState::Playing;
        self.last_frame_time = Some(Instant::now());
        self.accumulated_time = Duration::ZERO;
    }

    /// Pause the animation.
    pub fn pause(&mut self) {
        self.playback_state = PlaybackState::Paused;
    }

    /// Stop the animation and reset to start.
    pub fn stop(&mut self) {
        self.playback_state = PlaybackState::Stopped;
        self.frame = self.start_frame;
        self.last_frame_time = None;
    }

    /// Step forward one frame.
    pub fn step_forward(&mut self) {
        if self.frame < self.end_frame {
            self.frame += 1;
        } else if self.loop_enabled {
            self.frame = self.start_frame;
        }
    }

    /// Step backward one frame.
    pub fn step_backward(&mut self) {
        if self.frame > self.start_frame {
            self.frame -= 1;
        } else if self.loop_enabled {
            self.frame = self.end_frame;
        }
    }

    /// Go to first frame.
    pub fn rewind(&mut self) {
        self.frame = self.start_frame;
    }

    /// Go to last frame.
    pub fn go_to_end(&mut self) {
        self.frame = self.end_frame;
    }

    /// Update playback (call each frame).
    /// Returns true if the frame changed.
    pub fn update(&mut self) -> bool {
        if self.playback_state != PlaybackState::Playing {
            return false;
        }

        let now = Instant::now();
        let frame_duration = Duration::from_secs_f32(1.0 / self.fps as f32);

        if let Some(last_time) = self.last_frame_time {
            self.accumulated_time += now - last_time;
            self.last_frame_time = Some(now);

            if self.accumulated_time >= frame_duration {
                self.accumulated_time -= frame_duration;
                self.step_forward();

                // Stop at end if not looping
                if !self.loop_enabled && self.frame >= self.end_frame {
                    self.playback_state = PlaybackState::Stopped;
                }

                return true;
            }
        } else {
            self.last_frame_time = Some(now);
        }

        false
    }

    /// Show the animation bar.
    pub fn show(&mut self, ui: &mut egui::Ui) -> AnimationEvent {
        let mut event = AnimationEvent::None;

        // Background
        let rect = ui.available_rect_before_wrap();
        ui.painter().rect_filled(rect, 0.0, theme::ANIMATION_BAR_BACKGROUND);

        ui.horizontal(|ui| {
            ui.add_space(8.0);

            // Rewind button
            if ui.button("⏮").on_hover_text("Rewind").clicked() {
                self.rewind();
                event = AnimationEvent::Rewind;
            }

            // Step back button
            if ui.button("⏪").on_hover_text("Step backward").clicked() {
                self.step_backward();
                event = AnimationEvent::StepBack;
            }

            // Play/Pause button
            let (play_icon, play_tooltip) = if self.is_playing() {
                ("⏸", "Pause")
            } else {
                ("▶", "Play")
            };
            if ui.button(play_icon).on_hover_text(play_tooltip).clicked() {
                if self.is_playing() {
                    self.pause();
                    event = AnimationEvent::Pause;
                } else {
                    self.play();
                    event = AnimationEvent::Play;
                }
            }

            // Step forward button
            if ui.button("⏩").on_hover_text("Step forward").clicked() {
                self.step_forward();
                event = AnimationEvent::StepForward;
            }

            // Go to end button
            if ui.button("⏭").on_hover_text("Go to end").clicked() {
                self.go_to_end();
                event = AnimationEvent::GoToEnd;
            }

            // Stop button
            if ui.button("⏹").on_hover_text("Stop").clicked() {
                self.stop();
                event = AnimationEvent::Stop;
            }

            ui.separator();

            // Frame counter
            ui.label(
                egui::RichText::new("Frame:")
                    .color(theme::TEXT_NORMAL)
                    .size(11.0),
            );
            let mut frame = self.frame as i32;
            let frame_response = ui.add(
                egui::DragValue::new(&mut frame)
                    .range(self.start_frame as i32..=self.end_frame as i32)
                    .speed(1.0),
            );
            if frame_response.changed() {
                self.frame = frame as u32;
                event = AnimationEvent::FrameChanged(self.frame as f64);
            }

            ui.label(
                egui::RichText::new(format!("/ {}", self.end_frame))
                    .color(theme::TEXT_DISABLED)
                    .size(11.0),
            );

            ui.separator();

            // FPS control
            ui.label(
                egui::RichText::new("FPS:")
                    .color(theme::TEXT_NORMAL)
                    .size(11.0),
            );
            let mut fps = self.fps as i32;
            let fps_response = ui.add(
                egui::DragValue::new(&mut fps)
                    .range(1..=120)
                    .speed(1.0),
            );
            if fps_response.changed() {
                self.fps = fps as u32;
                event = AnimationEvent::FpsChanged(self.fps);
            }

            ui.separator();

            // Loop toggle
            if ui.checkbox(&mut self.loop_enabled, "Loop").changed() {
                // Loop toggle doesn't emit a specific event
            }
        });

        event
    }
}

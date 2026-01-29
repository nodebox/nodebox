//! Animation timeline widget.

use eframe::egui::{self, Color32, Pos2, Rect, Stroke, Vec2};
use std::time::{Duration, Instant};

/// Animation playback state.
#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum PlaybackState {
    /// Animation is stopped.
    Stopped,
    /// Animation is playing forward.
    Playing,
    /// Animation is paused.
    Paused,
}

/// The animation timeline widget.
pub struct Timeline {
    /// Current frame number.
    frame: u32,
    /// Start frame.
    start_frame: u32,
    /// End frame.
    end_frame: u32,
    /// Frames per second.
    fps: f32,
    /// Current playback state.
    playback_state: PlaybackState,
    /// Whether to loop the animation.
    loop_animation: bool,
    /// Time of last frame update (for playback).
    last_frame_time: Option<Instant>,
    /// Accumulated time since last frame (for sub-frame timing).
    accumulated_time: Duration,
    /// Whether timeline is visible.
    pub visible: bool,
}

impl Default for Timeline {
    fn default() -> Self {
        Self::new()
    }
}

impl Timeline {
    /// Create a new timeline.
    pub fn new() -> Self {
        Self {
            frame: 1,
            start_frame: 1,
            end_frame: 100,
            fps: 30.0,
            playback_state: PlaybackState::Stopped,
            loop_animation: true,
            last_frame_time: None,
            accumulated_time: Duration::ZERO,
            visible: true,
        }
    }

    /// Get the current frame.
    pub fn frame(&self) -> u32 {
        self.frame
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
        } else if self.loop_animation {
            self.frame = self.start_frame;
        }
    }

    /// Step backward one frame.
    pub fn step_backward(&mut self) {
        if self.frame > self.start_frame {
            self.frame -= 1;
        } else if self.loop_animation {
            self.frame = self.end_frame;
        }
    }

    /// Go to first frame.
    pub fn go_to_start(&mut self) {
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
        let frame_duration = Duration::from_secs_f32(1.0 / self.fps);

        if let Some(last_time) = self.last_frame_time {
            self.accumulated_time += now - last_time;
            self.last_frame_time = Some(now);

            if self.accumulated_time >= frame_duration {
                self.accumulated_time -= frame_duration;
                self.step_forward();

                // Stop at end if not looping
                if !self.loop_animation && self.frame >= self.end_frame {
                    self.playback_state = PlaybackState::Stopped;
                }

                return true;
            }
        } else {
            self.last_frame_time = Some(now);
        }

        false
    }

    /// Show the timeline widget.
    pub fn show(&mut self, ui: &mut egui::Ui) {
        if !self.visible {
            return;
        }

        ui.horizontal(|ui| {
            // Playback controls
            if ui.button("⏮").on_hover_text("Go to start").clicked() {
                self.go_to_start();
            }

            if ui.button("⏪").on_hover_text("Step backward").clicked() {
                self.step_backward();
            }

            let play_button = if self.is_playing() { "⏸" } else { "▶" };
            let play_tooltip = if self.is_playing() { "Pause" } else { "Play" };
            if ui.button(play_button).on_hover_text(play_tooltip).clicked() {
                if self.is_playing() {
                    self.pause();
                } else {
                    self.play();
                }
            }

            if ui.button("⏩").on_hover_text("Step forward").clicked() {
                self.step_forward();
            }

            if ui.button("⏭").on_hover_text("Go to end").clicked() {
                self.go_to_end();
            }

            if ui.button("⏹").on_hover_text("Stop").clicked() {
                self.stop();
            }

            ui.separator();

            // Frame counter
            ui.label("Frame:");
            let mut frame = self.frame as i32;
            if ui.add(egui::DragValue::new(&mut frame).range(self.start_frame as i32..=self.end_frame as i32)).changed() {
                self.frame = frame as u32;
            }

            ui.label(format!("/ {}", self.end_frame));

            ui.separator();

            // FPS
            ui.label("FPS:");
            ui.add(egui::DragValue::new(&mut self.fps).range(1.0..=120.0).speed(1.0));

            ui.separator();

            // Loop toggle
            ui.checkbox(&mut self.loop_animation, "Loop");

            ui.separator();

            // Frame range
            ui.label("Range:");
            let mut start = self.start_frame as i32;
            if ui.add(egui::DragValue::new(&mut start).range(1..=self.end_frame as i32)).changed() {
                self.start_frame = start as u32;
                if self.frame < self.start_frame {
                    self.frame = self.start_frame;
                }
            }
            ui.label("-");
            let mut end = self.end_frame as i32;
            if ui.add(egui::DragValue::new(&mut end).range(self.start_frame as i32..=10000)).changed() {
                self.end_frame = end as u32;
                if self.frame > self.end_frame {
                    self.frame = self.end_frame;
                }
            }
        });

        // Timeline scrubber
        let available_width = ui.available_width();
        let (response, painter) = ui.allocate_painter(
            Vec2::new(available_width, 30.0),
            egui::Sense::click_and_drag(),
        );

        let rect = response.rect;

        // Draw background
        painter.rect_filled(rect, 2.0, Color32::from_rgb(30, 30, 30));

        // Draw frame markers
        let frame_width = rect.width() / (self.end_frame - self.start_frame + 1) as f32;
        let marker_color = Color32::from_rgb(60, 60, 60);

        for i in 0..=(self.end_frame - self.start_frame) {
            let x = rect.left() + i as f32 * frame_width;
            if i % 10 == 0 {
                // Major marker every 10 frames
                painter.line_segment(
                    [Pos2::new(x, rect.top()), Pos2::new(x, rect.bottom())],
                    Stroke::new(1.0, marker_color),
                );
                painter.text(
                    Pos2::new(x + 2.0, rect.top() + 2.0),
                    egui::Align2::LEFT_TOP,
                    (self.start_frame + i).to_string(),
                    egui::FontId::proportional(9.0),
                    Color32::GRAY,
                );
            }
        }

        // Draw playhead
        let playhead_x = rect.left() + (self.frame - self.start_frame) as f32 * frame_width + frame_width / 2.0;
        painter.line_segment(
            [Pos2::new(playhead_x, rect.top()), Pos2::new(playhead_x, rect.bottom())],
            Stroke::new(2.0, Color32::from_rgb(255, 100, 100)),
        );

        // Draw playhead triangle
        let triangle_size = 8.0;
        let triangle = vec![
            Pos2::new(playhead_x, rect.top() + triangle_size),
            Pos2::new(playhead_x - triangle_size / 2.0, rect.top()),
            Pos2::new(playhead_x + triangle_size / 2.0, rect.top()),
        ];
        painter.add(egui::Shape::convex_polygon(
            triangle,
            Color32::from_rgb(255, 100, 100),
            Stroke::NONE,
        ));

        // Handle scrubbing
        if response.dragged() || response.clicked() {
            if let Some(pos) = response.interact_pointer_pos() {
                let relative_x = (pos.x - rect.left()) / rect.width();
                let frame = self.start_frame + (relative_x * (self.end_frame - self.start_frame + 1) as f32) as u32;
                self.frame = frame.clamp(self.start_frame, self.end_frame);
            }
        }
    }
}

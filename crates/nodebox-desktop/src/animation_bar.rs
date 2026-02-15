//! Compact animation bar with playback controls.
//!
//! Matches the Java AnimationBar: a draggable frame number, Play/Pause, and Rewind.

use eframe::egui;
use std::time::{Duration, Instant};
use crate::theme;

/// Animation playback state.
#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum PlaybackState {
    Stopped,
    Playing,
}

/// Events that can be triggered by the animation bar.
#[derive(Clone, Copy, Debug, PartialEq)]
pub enum AnimationEvent {
    None,
    Play,
    Stop,
    Rewind,
    FrameChanged(f64),
}

/// Compact animation bar widget.
///
/// Controls: draggable frame number, Play/Pause button, Rewind button.
/// No max frame — the frame counts up indefinitely during playback.
pub struct AnimationBar {
    /// Current frame number (1-based, no upper bound).
    frame: u32,
    /// Frames per second.
    fps: u32,
    /// Current playback state.
    playback_state: PlaybackState,
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
            fps: 30,
            playback_state: PlaybackState::Stopped,
            last_frame_time: None,
            accumulated_time: Duration::ZERO,
        }
    }

    /// Get the current frame.
    pub fn frame(&self) -> u32 {
        self.frame
    }

    /// Set the current frame (clamped to >= 1).
    #[allow(dead_code)]
    pub fn set_frame(&mut self, frame: u32) {
        self.frame = frame.max(1);
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

    /// Stop the animation (keeps current frame).
    pub fn stop(&mut self) {
        self.playback_state = PlaybackState::Stopped;
        self.last_frame_time = None;
    }

    /// Rewind: stop playback and reset to frame 1.
    pub fn rewind(&mut self) {
        self.stop();
        self.frame = 1;
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
                self.frame += 1;
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

        // Clean background - seamless with panel
        let rect = ui.available_rect_before_wrap();
        ui.painter().rect_filled(rect, 0.0, theme::ANIMATION_BAR_BACKGROUND);

        // Subtle top border only
        ui.painter().line_segment(
            [
                egui::pos2(rect.min.x, rect.min.y),
                egui::pos2(rect.max.x, rect.min.y),
            ],
            egui::Stroke::new(1.0, theme::BORDER_COLOR),
        );

        ui.horizontal(|ui| {
            ui.add_space(theme::PADDING_SMALL);

            // Frame number (draggable, min 1, no max)
            let mut frame = self.frame as i32;
            let frame_response = Self::styled_drag_value(ui, &mut frame, 1..=i32::MAX, 50.0);
            if frame_response.changed() {
                self.frame = (frame.max(1)) as u32;
                event = AnimationEvent::FrameChanged(self.frame as f64);
            }

            ui.add_space(theme::PADDING_SMALL);

            // Play/Pause toggle
            let (play_icon, play_tooltip) = if self.is_playing() {
                ("\u{23F8}", "Stop")
            } else {
                ("\u{25B6}", "Play")
            };
            if self.icon_button(ui, play_icon, play_tooltip) {
                if self.is_playing() {
                    self.stop();
                    event = AnimationEvent::Stop;
                } else {
                    self.play();
                    event = AnimationEvent::Play;
                }
            }

            // Rewind
            if self.icon_button(ui, "\u{23EE}", "Rewind") {
                self.rewind();
                event = AnimationEvent::Rewind;
            }
        });

        event
    }

    /// Styled DragValue that follows the style guide.
    fn styled_drag_value(ui: &mut egui::Ui, value: &mut i32, range: std::ops::RangeInclusive<i32>, width: f32) -> egui::Response {
        let old_visuals = ui.visuals().clone();
        let old_spacing = ui.spacing().clone();

        ui.visuals_mut().widgets.inactive.bg_fill = theme::ZINC_700;
        ui.visuals_mut().widgets.inactive.weak_bg_fill = theme::ZINC_700;
        ui.visuals_mut().widgets.inactive.bg_stroke = egui::Stroke::NONE;
        ui.visuals_mut().widgets.inactive.fg_stroke = egui::Stroke::new(1.0, theme::TEXT_DEFAULT);
        ui.visuals_mut().widgets.inactive.corner_radius = egui::CornerRadius::ZERO;
        ui.visuals_mut().widgets.inactive.expansion = 0.0;

        ui.visuals_mut().widgets.hovered.bg_fill = theme::ZINC_600;
        ui.visuals_mut().widgets.hovered.weak_bg_fill = theme::ZINC_600;
        ui.visuals_mut().widgets.hovered.bg_stroke = egui::Stroke::NONE;
        ui.visuals_mut().widgets.hovered.fg_stroke = egui::Stroke::new(1.0, theme::TEXT_STRONG);
        ui.visuals_mut().widgets.hovered.corner_radius = egui::CornerRadius::ZERO;
        ui.visuals_mut().widgets.hovered.expansion = 0.0;

        ui.visuals_mut().widgets.active.bg_fill = theme::ZINC_600;
        ui.visuals_mut().widgets.active.weak_bg_fill = theme::ZINC_600;
        ui.visuals_mut().widgets.active.bg_stroke = egui::Stroke::NONE;
        ui.visuals_mut().widgets.active.fg_stroke = egui::Stroke::new(1.0, theme::TEXT_STRONG);
        ui.visuals_mut().widgets.active.corner_radius = egui::CornerRadius::ZERO;
        ui.visuals_mut().widgets.active.expansion = 0.0;

        ui.visuals_mut().widgets.noninteractive.bg_fill = theme::ZINC_700;
        ui.visuals_mut().widgets.noninteractive.weak_bg_fill = theme::ZINC_700;
        ui.visuals_mut().widgets.noninteractive.bg_stroke = egui::Stroke::NONE;
        ui.visuals_mut().widgets.noninteractive.corner_radius = egui::CornerRadius::ZERO;
        ui.visuals_mut().widgets.noninteractive.expansion = 0.0;

        ui.spacing_mut().button_padding = egui::vec2(4.0, 2.0);

        let (rect, _) = ui.allocate_exact_size(
            egui::vec2(width, theme::ANIMATION_BAR_HEIGHT),
            egui::Sense::hover(),
        );
        let response = ui.put(
            rect,
            egui::DragValue::new(value)
                .range(range)
                .speed(1.0),
        );

        *ui.visuals_mut() = old_visuals;
        *ui.spacing_mut() = old_spacing;

        response
    }

    /// Custom icon button with transparent background and hover effect.
    fn icon_button(&self, ui: &mut egui::Ui, icon: &str, tooltip: &str) -> bool {
        let button_size = egui::vec2(theme::ANIMATION_BAR_HEIGHT, theme::ANIMATION_BAR_HEIGHT);
        let (rect, response) = ui.allocate_exact_size(button_size, egui::Sense::click());

        if ui.is_rect_visible(rect) {
            let bg_color = if response.hovered() {
                theme::ZINC_700
            } else {
                egui::Color32::TRANSPARENT
            };

            ui.painter().rect_filled(rect, 0.0, bg_color);

            let text_color = if response.hovered() {
                theme::TEXT_STRONG
            } else {
                theme::TEXT_DEFAULT
            };

            ui.painter().text(
                rect.center(),
                egui::Align2::CENTER_CENTER,
                icon,
                egui::FontId::proportional(theme::FONT_SIZE_BASE),
                text_color,
            );
        }

        response.on_hover_text(tooltip).clicked()
    }
}

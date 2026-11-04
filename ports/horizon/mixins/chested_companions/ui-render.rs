fn cc_render(game: &crate::game::Game<'_>, batch: &mut HudBatch, screen: &NativeScreenView<'_>) {
    game.cc_draw_remove(batch, screen);
}

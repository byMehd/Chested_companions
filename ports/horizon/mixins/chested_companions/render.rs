fn render(game: &crate::game::Game<'_>, mob: &NativeMobRender<'_>, draw: &mut BlockEntityDraw) {
    game.cc_draw(mob, draw);
}

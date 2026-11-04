fn cc_hold(game: &mut crate::game::Game<'_>, entity: u32, kind: &str) {
    game.cc_hold_pre(entity, kind);
}

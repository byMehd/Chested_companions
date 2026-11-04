fn store(game: &mut crate::game::Game<'_>, key: (i32, i32, i32)) {
    game.cc_store(key);
}

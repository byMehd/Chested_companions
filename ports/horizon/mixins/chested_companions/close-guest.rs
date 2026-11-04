fn close(game: &mut crate::game::Game<'_>, key: (i32, i32, i32)) -> std::ops::ControlFlow<()> {
    if crate::game::chested_companions::pet_key(key).is_none() {
        return std::ops::ControlFlow::Continue(());
    }
    game.cc_store(key);
    std::ops::ControlFlow::Break(())
}

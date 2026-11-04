fn valid(
    game: &crate::game::Game<'_>,
    guest: &horizon_client::lan::guest::Guest,
) -> std::ops::ControlFlow<bool> {
    match game.cc_guest_valid(guest) {
        Some(result) => std::ops::ControlFlow::Break(result),
        None => std::ops::ControlFlow::Continue(()),
    }
}

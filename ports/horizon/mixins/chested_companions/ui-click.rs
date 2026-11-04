fn cc_click(
    game: &crate::game::Game<'_>,
    screen: &NativeScreenView<'_>,
    button: i32,
    canceled: &mut bool,
) {
    game.cc_click_remove(screen, button, canceled);
}

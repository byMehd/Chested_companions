fn cc_receive(game: &mut crate::game::Game<'_>, channel: &str, data: &[u8]) {
    game.cc_receive_menu(channel, data);
}

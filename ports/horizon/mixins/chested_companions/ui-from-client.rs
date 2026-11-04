fn cc_receive(game: &mut crate::game::Game<'_>, player: u128, channel: &str, data: &[u8]) {
    game.cc_receive_remove(player, channel, data);
}

fn open(
    game: &mut crate::game::Game<'_>,
    host: &mut LanHost,
    conn: u32,
    pos: (i32, i32, i32),
) -> std::ops::ControlFlow<bool> {
    match game.cc_open_guest(host, conn, pos) {
        Some(result) => std::ops::ControlFlow::Break(result),
        None => std::ops::ControlFlow::Continue(()),
    }
}

fn cc_tick(
    transport: &mut Transport,
    mob: &mut Mob,
    cx: &mut Bx<'_, '_>,
) -> std::ops::ControlFlow<()> {
    if chested_companions::tick(transport, mob, cx) {
        std::ops::ControlFlow::Break(())
    } else {
        std::ops::ControlFlow::Continue(())
    }
}

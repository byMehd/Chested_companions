fn cc_start(
    transport: &mut Transport,
    mob: &mut Mob,
    cx: &mut Bx<'_, '_>,
) -> std::ops::ControlFlow<bool> {
    match chested_companions::try_start(transport, mob, cx) {
        Some(value) => std::ops::ControlFlow::Break(value),
        None => std::ops::ControlFlow::Continue(()),
    }
}

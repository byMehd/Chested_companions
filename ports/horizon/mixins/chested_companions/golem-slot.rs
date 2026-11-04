fn cc_slot(
    game: &mut crate::game::Game<'_>,
    chest: [i32; 3],
    slot: usize,
    stack: &Option<ItemStack>,
) -> std::ops::ControlFlow<()> {
    if game.cc_golem_slot(chest, slot, stack.clone()) {
        std::ops::ControlFlow::Break(())
    } else {
        std::ops::ControlFlow::Continue(())
    }
}

fn interact(
    game: &mut crate::game::Game<'_>,
    entity: i32,
    kind: &str,
    stack: Option<&ItemStack>,
    claimed: &mut bool,
) {
    game.cc_interact(entity, kind, stack, claimed);
}

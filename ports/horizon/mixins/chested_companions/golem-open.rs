fn cc_open(chest: [i32; 3]) -> std::ops::ControlFlow<()> {
    if crate::game::chested_companions::pet_key((chest[0], chest[1], chest[2])).is_some() {
        std::ops::ControlFlow::Break(())
    } else {
        std::ops::ControlFlow::Continue(())
    }
}

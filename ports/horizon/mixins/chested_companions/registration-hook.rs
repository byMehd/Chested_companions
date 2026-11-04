fn cc_registration(value: &'static [Box<dyn Hooks>]) -> &'static [Box<dyn Hooks>] {
    chested_companions::registered(value)
}

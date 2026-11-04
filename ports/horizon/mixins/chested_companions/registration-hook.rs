fn cc_registration(value: &'static [Box<dyn Hooks>]) -> &'static [Box<dyn Hooks>] {
    crate::entry::chested_companions::registered(value)
}

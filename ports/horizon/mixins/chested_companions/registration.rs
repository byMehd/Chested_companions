// Source mixins still need a trait presence for Horizon's network feature
// negotiation. Existing trait mods keep their original dispatch list.
use super::Hooks;
use std::sync::OnceLock;

struct Presence;
impl Hooks for Presence {}

pub fn registered(existing: &'static [Box<dyn Hooks>]) -> &'static [Box<dyn Hooks>] {
    if !existing.is_empty() {
        return existing;
    }
    static PRESENCE: OnceLock<Vec<Box<dyn Hooks>>> = OnceLock::new();
    PRESENCE.get_or_init(|| vec![Box::new(Presence)]).as_slice()
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn a_mixin_only_install_enables_native_network_negotiation() {
        assert!(!crate::mod_api::mods().is_empty());
        assert!(!registered(&[]).is_empty());
    }

    #[test]
    fn existing_trait_mods_are_preserved_in_their_original_order() {
        let existing: &'static [Box<dyn Hooks>] = Box::leak(
            vec![Box::new(Presence) as Box<dyn Hooks>, Box::new(Presence)].into_boxed_slice(),
        );
        assert!(std::ptr::eq(registered(existing), existing));
    }
}

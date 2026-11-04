fn cc_metadata(mods: &mut Vec<NativeMod>) {
    if mods.iter().any(|m| m.id == "chested_companions") {
        return;
    }
    mods.push(NativeMod {
        id: "chested_companions".into(),
        title: "Chested Companions: Revived 1.0.1".into(),
        description: horizon_client::lang::get("mod.chestedcompanions.description"),
        keys: Vec::new(),
    });
}

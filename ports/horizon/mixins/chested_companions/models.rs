fn cc_register_models(models: &mut Vec<String>) {
    for name in [
        "chest",
        "ender",
        "copper",
        "copper_exposed",
        "copper_weathered",
        "copper_oxidized",
    ] {
        models.push(format!("chestedcompanions:entity/saddlebag/{name}"));
    }
}

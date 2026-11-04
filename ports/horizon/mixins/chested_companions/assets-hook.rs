fn cc_assets(
    vanilla: &Path,
    selected: &[String],
    server: &[Pack],
) -> std::ops::ControlFlow<std::io::Result<PathBuf>> {
    std::ops::ControlFlow::Break(crate::game::mods::chested_companions_assets::startup(
        vanilla, selected, server,
    ))
}

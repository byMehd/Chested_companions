fn cc_assets(
    base: &Path,
    enabled: &[Pack],
    cache: &Path,
    original: impl FnOnce(&Path, &[Pack], &Path) -> std::io::Result<PathBuf>,
) -> std::io::Result<PathBuf> {
    let base = crate::game::mods::chested_companions_assets::attach(base.to_owned());
    original(&base, enabled, cache)
}

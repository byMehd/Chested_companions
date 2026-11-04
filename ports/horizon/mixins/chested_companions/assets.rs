// Owned MIT assets travel inside the native source bundle and are compiled
// into the game. Load them into the base content root before resource packs.
use horizon_client::mod_content::{ContentMod, resource_root};
use horizon_mods::{Content, Package};
use sha2::{Digest, Sha256};
use std::path::{Path, PathBuf};

// Pack ordering follows Horizon's game/packs.rs::startup_root, MIT,
// Copyright (c) 2026 Ludovic Marie and the Horizon contributors. The only extra layer is our owned base assets.
pub fn startup(
    vanilla: &Path,
    selected: &[String],
    server: &[horizon_client::packs::Pack],
) -> std::io::Result<PathBuf> {
    use crate::game::packs::{cache_dir, packs_dir};
    use horizon_client::packs;
    let all = packs::discover(vanilla, &packs_dir());
    let mut enabled = packs::enabled(&all, selected);
    enabled.extend_from_slice(server);
    let base = attach(vanilla).map_err(std::io::Error::other)?;
    let root = packs::resource_root(&base, &enabled, &cache_dir())?;
    // The native quad API uses the shared GUI atlas. Alias the *selected*
    // legacy textures into its GUI folder, after resource-pack overrides.
    aliases(&root, &asset_cache()).map_err(std::io::Error::other)?;
    Ok(root)
}

const FILES: &[(&str, &[u8])] = &[
    (
        "horizon-mod.toml",
        horizon_client::native_data!("chested_companions", "horizon-mod.toml"),
    ),
    (
        "assets/chestedcompanions/lang/en_us.json",
        horizon_client::native_data!(
            "chested_companions",
            "assets/chestedcompanions/lang/en_us.json"
        ),
    ),
    (
        "assets/chestedcompanions/lang/fr_fr.json",
        horizon_client::native_data!(
            "chested_companions",
            "assets/chestedcompanions/lang/fr_fr.json"
        ),
    ),
    (
        "assets/chestedcompanions/textures/chest/chest.png",
        horizon_client::native_data!(
            "chested_companions",
            "assets/chestedcompanions/textures/chest/chest.png"
        ),
    ),
    (
        "assets/chestedcompanions/textures/chest/special/ender.png",
        horizon_client::native_data!(
            "chested_companions",
            "assets/chestedcompanions/textures/chest/special/ender.png"
        ),
    ),
    (
        "assets/chestedcompanions/textures/chest/special/copper.png",
        horizon_client::native_data!(
            "chested_companions",
            "assets/chestedcompanions/textures/chest/special/copper.png"
        ),
    ),
    (
        "assets/chestedcompanions/textures/chest/special/copper_exposed.png",
        horizon_client::native_data!(
            "chested_companions",
            "assets/chestedcompanions/textures/chest/special/copper_exposed.png"
        ),
    ),
    (
        "assets/chestedcompanions/textures/chest/special/copper_weathered.png",
        horizon_client::native_data!(
            "chested_companions",
            "assets/chestedcompanions/textures/chest/special/copper_weathered.png"
        ),
    ),
    (
        "assets/chestedcompanions/textures/chest/special/copper_oxidized.png",
        horizon_client::native_data!(
            "chested_companions",
            "assets/chestedcompanions/textures/chest/special/copper_oxidized.png"
        ),
    ),
    (
        "assets/chestedcompanions/textures/chest_icon.png",
        horizon_client::native_data!(
            "chested_companions",
            "assets/chestedcompanions/textures/chest_icon.png"
        ),
    ),
];

fn materialize(cache: &Path) -> Result<PathBuf, String> {
    let mut hash = Sha256::new();
    for (name, bytes) in FILES {
        hash.update(name.as_bytes());
        hash.update([0]);
        hash.update(bytes);
    }
    let source = cache.join(format!("assets-{:x}", hash.finalize()));
    std::fs::create_dir_all(&source).map_err(|e| e.to_string())?;
    for (name, bytes) in FILES {
        let path = source.join(name);
        atomic_asset(cache, &path, bytes)?;
    }
    Ok(source)
}

fn asset_cache() -> PathBuf {
    super::Game::game_dir().join(".horizon-cache/chested_companions")
}

fn attach(base: &Path) -> Result<PathBuf, String> {
    let cache = asset_cache();
    let source = materialize(&cache)?;
    let package = Package::read(&source)?;
    let content = Content::read(&source, &package.manifest)?;
    resource_root(
        base,
        &[ContentMod {
            id: package.manifest.id,
            content,
        }],
        &cache.join("overlays"),
    )
    .map_err(|e| e.to_string())
}

const ALIASES: &[(&str, &str)] = &[
    ("chest/chest", "chest"),
    ("chest/special/ender", "ender"),
    ("chest/special/copper", "copper"),
    ("chest/special/copper_exposed", "copper_exposed"),
    ("chest/special/copper_weathered", "copper_weathered"),
    ("chest/special/copper_oxidized", "copper_oxidized"),
    ("chest_icon", "chest_icon"),
];

fn aliases(root: &Path, cache: &Path) -> Result<(), String> {
    let textures = root.join("assets/chestedcompanions/textures");
    for (legacy, name) in ALIASES {
        let bytes =
            std::fs::read(textures.join(format!("{legacy}.png"))).map_err(|e| e.to_string())?;
        atomic_asset(
            cache,
            &textures.join(format!("gui/saddlebag/{name}.png")),
            &bytes,
        )?;
    }
    Ok(())
}

fn atomic_asset(cache: &Path, path: &Path, bytes: &[u8]) -> Result<(), String> {
    if std::fs::read(path).is_ok_and(|current| current == bytes) {
        return Ok(());
    }
    std::fs::create_dir_all(path.parent().ok_or("asset has no parent")?)
        .map_err(|e| e.to_string())?;
    std::fs::create_dir_all(cache).map_err(|e| e.to_string())?;
    let nonce = std::time::SystemTime::now()
        .duration_since(std::time::UNIX_EPOCH)
        .unwrap_or_default()
        .as_nanos();
    let temporary = cache.join(format!("write-{}-{nonce}.tmp", std::process::id()));
    let write = (|| -> std::io::Result<()> {
        use std::io::Write;
        let mut file = std::fs::OpenOptions::new()
            .write(true)
            .create_new(true)
            .open(&temporary)?;
        file.write_all(bytes)?;
        file.sync_all()?;
        drop(file);
        // Replace the link itself, never a resource pack's linked source file.
        std::fs::rename(&temporary, path)
    })();
    if let Err(error) = write {
        let _ = std::fs::remove_file(&temporary);
        return Err(error.to_string());
    }
    Ok(())
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn embedded_assets_have_the_dimensions_the_saddlebag_uvs_expect() {
        let mut bags = 0;
        for (name, bytes) in FILES {
            if !name.ends_with(".png") {
                continue;
            }
            assert_eq!(&bytes[..8], b"\x89PNG\r\n\x1a\n");
            let size = (
                u32::from_be_bytes(bytes[16..20].try_into().unwrap()),
                u32::from_be_bytes(bytes[20..24].try_into().unwrap()),
            );
            if name.ends_with("/chest_icon.png") {
                assert_eq!(size, (16, 16));
            } else {
                assert_eq!(size, (22, 11), "{name}");
                bags += 1;
            }
        }
        assert_eq!(bags, 6);
    }

    #[test]
    fn embedded_content_is_valid_and_adds_no_gameplay_registries() {
        let root = std::env::temp_dir().join(format!(
            "chested-assets-test-{}-{}",
            std::process::id(),
            std::time::SystemTime::now()
                .duration_since(std::time::UNIX_EPOCH)
                .unwrap()
                .as_nanos()
        ));
        let source = materialize(&root).unwrap();
        let package = Package::read(&source).unwrap();
        let content = Content::read(&source, &package.manifest).unwrap();
        assert_eq!(package.manifest.id, "chested_companions");
        assert!(content.blocks.is_empty());
        assert!(content.items.is_empty());
        assert!(
            content
                .files
                .contains(&"assets/chestedcompanions/textures/chest/special/ender.png".into())
        );
        aliases(&source, &root).unwrap();
        let textures = source.join("assets/chestedcompanions/textures");
        let alias = textures.join("gui/saddlebag/chest.png");
        let selected = std::fs::read(textures.join("chest/special/ender.png")).unwrap();
        assert_ne!(std::fs::read(&alias).unwrap(), selected);
        // A pack overrides the original Java path. A second reload must
        // update the atlas alias too, without changing that selected source.
        std::fs::write(textures.join("chest/chest.png"), &selected).unwrap();
        aliases(&source, &root).unwrap();
        assert_eq!(std::fs::read(&alias).unwrap(), selected);
        assert_eq!(
            std::fs::read(textures.join("chest/chest.png")).unwrap(),
            selected
        );
        assert!(
            root.starts_with(std::env::temp_dir())
                && root
                    .file_name()
                    .unwrap()
                    .to_string_lossy()
                    .starts_with("chested-assets-test-")
        );
        std::fs::remove_dir_all(root).unwrap();
    }
}

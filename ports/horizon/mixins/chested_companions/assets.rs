// Owned MIT assets travel inside the native source bundle and are compiled
// into the game. Load them into the base content root before resource packs.
use horizon_client::mod_content::{ContentMod, resource_root};
use horizon_mods::{Content, Package};
use sha2::{Digest, Sha256};
use std::path::{Path, PathBuf};

const FILES: &[(&str, &[u8])] = &[
    (
        "horizon-mod.toml",
        crate::native_data!("chested_companions", "horizon-mod.toml"),
    ),
    (
        "assets/chestedcompanions/lang/en_us.json",
        crate::native_data!(
            "chested_companions",
            "assets/chestedcompanions/lang/en_us.json"
        ),
    ),
    (
        "assets/chestedcompanions/lang/fr_fr.json",
        crate::native_data!(
            "chested_companions",
            "assets/chestedcompanions/lang/fr_fr.json"
        ),
    ),
    (
        "assets/chestedcompanions/models/entity/saddlebag/chest.json",
        crate::native_data!(
            "chested_companions",
            "assets/chestedcompanions/models/entity/saddlebag/chest.json"
        ),
    ),
    (
        "assets/chestedcompanions/models/entity/saddlebag/ender.json",
        crate::native_data!(
            "chested_companions",
            "assets/chestedcompanions/models/entity/saddlebag/ender.json"
        ),
    ),
    (
        "assets/chestedcompanions/models/entity/saddlebag/copper.json",
        crate::native_data!(
            "chested_companions",
            "assets/chestedcompanions/models/entity/saddlebag/copper.json"
        ),
    ),
    (
        "assets/chestedcompanions/models/entity/saddlebag/copper_exposed.json",
        crate::native_data!(
            "chested_companions",
            "assets/chestedcompanions/models/entity/saddlebag/copper_exposed.json"
        ),
    ),
    (
        "assets/chestedcompanions/models/entity/saddlebag/copper_weathered.json",
        crate::native_data!(
            "chested_companions",
            "assets/chestedcompanions/models/entity/saddlebag/copper_weathered.json"
        ),
    ),
    (
        "assets/chestedcompanions/models/entity/saddlebag/copper_oxidized.json",
        crate::native_data!(
            "chested_companions",
            "assets/chestedcompanions/models/entity/saddlebag/copper_oxidized.json"
        ),
    ),
    (
        "assets/chestedcompanions/models/entity/saddlebag/chest_icon.json",
        crate::native_data!(
            "chested_companions",
            "assets/chestedcompanions/models/entity/saddlebag/chest_icon.json"
        ),
    ),
    (
        "assets/chestedcompanions/textures/chest/chest.png",
        crate::native_data!(
            "chested_companions",
            "assets/chestedcompanions/textures/chest/chest.png"
        ),
    ),
    (
        "assets/chestedcompanions/textures/chest/special/ender.png",
        crate::native_data!(
            "chested_companions",
            "assets/chestedcompanions/textures/chest/special/ender.png"
        ),
    ),
    (
        "assets/chestedcompanions/textures/chest/special/copper.png",
        crate::native_data!(
            "chested_companions",
            "assets/chestedcompanions/textures/chest/special/copper.png"
        ),
    ),
    (
        "assets/chestedcompanions/textures/chest/special/copper_exposed.png",
        crate::native_data!(
            "chested_companions",
            "assets/chestedcompanions/textures/chest/special/copper_exposed.png"
        ),
    ),
    (
        "assets/chestedcompanions/textures/chest/special/copper_weathered.png",
        crate::native_data!(
            "chested_companions",
            "assets/chestedcompanions/textures/chest/special/copper_weathered.png"
        ),
    ),
    (
        "assets/chestedcompanions/textures/chest/special/copper_oxidized.png",
        crate::native_data!(
            "chested_companions",
            "assets/chestedcompanions/textures/chest/special/copper_oxidized.png"
        ),
    ),
    (
        "assets/chestedcompanions/textures/chest_icon.png",
        crate::native_data!(
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
        if std::fs::read(&path).is_ok_and(|current| current == *bytes) {
            continue;
        }
        std::fs::create_dir_all(path.parent().ok_or("asset has no parent")?)
            .map_err(|e| e.to_string())?;
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
            std::fs::rename(&temporary, &path)
        })();
        if let Err(error) = write {
            let _ = std::fs::remove_file(&temporary);
            return Err(error.to_string());
        }
    }
    Ok(source)
}

pub fn attach(base: PathBuf) -> PathBuf {
    let cache = super::Game::game_dir().join(".horizon-cache/chested_companions");
    let result = (|| -> Result<PathBuf, String> {
        let source = materialize(&cache)?;
        let package = Package::read(&source)?;
        let content = Content::read(&source, &package.manifest)?;
        resource_root(
            &base,
            &[ContentMod {
                id: package.manifest.id,
                content,
            }],
            &cache.join("overlays"),
        )
        .map_err(|e| e.to_string())
    })();
    match result {
        Ok(root) => root,
        Err(error) => {
            log::error!("Chested Companions assets: {error}");
            base
        }
    }
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

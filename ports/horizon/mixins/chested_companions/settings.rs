use super::Game;

const BOOLS: [(&str, &str, bool); 12] = [
    ("allowChestOnCats", "cats", true),
    ("allowChestOnWolves", "wolves", true),
    ("allowChestOnBabyCat", "baby_cats", false),
    ("allowChestOnBabyWolf", "baby_wolves", false),
    ("publicChest", "public", false),
    ("invertShiftToOpen", "crouch", false),
    ("feedingOverridesOpeningChest", "feeding", true),
    ("copperGolemsUsePetChests", "golems", true),
    ("hideCatChest", "hide_cats", false),
    ("hideWolfChest", "hide_wolves", false),
    ("showChestIconOnCats", "cat_icon", false),
    ("showChestIconOnWolves", "wolf_icon", false),
];
const ROWS: [(&str, &str); 2] = [
    ("catInventoryCapacity", "cat_rows"),
    ("wolfInventoryCapacity", "wolf_rows"),
];
const CAPACITIES: [&str; 3] = ["one_row", "two_rows", "three_rows"];

fn read() -> Result<
    (
        std::path::PathBuf,
        serde_json::Map<String, serde_json::Value>,
    ),
    String,
> {
    let path = Game::native_config_dir()?.join("chestedcompanions_revived.json");
    let value = match std::fs::read(&path) {
        Ok(bytes) => {
            serde_json::from_slice(&bytes).map_err(|e| format!("{}: {e}", path.display()))?
        }
        Err(error) if error.kind() == std::io::ErrorKind::NotFound => {
            serde_json::Value::Object(Default::default())
        }
        Err(error) => return Err(error.to_string()),
    };
    let serde_json::Value::Object(map) = value else {
        return Err("configuration must be a JSON object".into());
    };
    Ok((path, map))
}

impl Game<'_> {
    pub(super) fn cc_settings(&mut self, id: &str, buttons: &mut Vec<String>) {
        if id != "chested_companions" {
            return;
        }
        let Ok((_, json)) = read() else { return };
        for (name, label, default) in BOOLS {
            let enabled = json.get(name).and_then(|v| v.as_bool()).unwrap_or(default);
            buttons.push(format!(
                "{}: {}",
                horizon_client::lang::get(&format!("settings.chestedcompanions.{label}")),
                horizon_client::lang::get(if enabled { "options.on" } else { "options.off" })
            ));
        }
        for (name, label) in ROWS {
            let index = json
                .get(name)
                .and_then(|v| v.as_str())
                .and_then(|v| CAPACITIES.iter().position(|c| c.eq_ignore_ascii_case(v)))
                .unwrap_or(0);
            buttons.push(format!(
                "{}: {}",
                horizon_client::lang::get(&format!("settings.chestedcompanions.{label}")),
                (index + 1) * 9
            ));
        }
    }

    pub(super) fn cc_settings_click(&mut self, id: &str, button: usize) {
        if id != "chested_companions" || button >= BOOLS.len() + ROWS.len() {
            return;
        }
        let Ok((path, mut json)) = read() else { return };
        if let Some((name, _, default)) = BOOLS.get(button) {
            let value = json
                .get(*name)
                .and_then(|v| v.as_bool())
                .unwrap_or(*default);
            json.insert((*name).into(), (!value).into());
        } else {
            let (name, _) = ROWS[button - BOOLS.len()];
            let index = json
                .get(name)
                .and_then(|v| v.as_str())
                .and_then(|v| CAPACITIES.iter().position(|c| c.eq_ignore_ascii_case(v)))
                .unwrap_or(0);
            json.insert(name.into(), CAPACITIES[(index + 1) % 3].into());
        }
        let bytes = match serde_json::to_vec_pretty(&json) {
            Ok(bytes) => bytes,
            Err(_) => return,
        };
        let nonce = std::time::SystemTime::now()
            .duration_since(std::time::UNIX_EPOCH)
            .unwrap_or_default()
            .as_nanos();
        let temporary = path.with_extension(format!("json.{}.{nonce}.tmp", std::process::id()));
        let result = (|| -> std::io::Result<()> {
            use std::io::Write;
            let mut file = std::fs::OpenOptions::new()
                .write(true)
                .create_new(true)
                .open(&temporary)?;
            file.write_all(&bytes)?;
            file.sync_all()?;
            drop(file);
            std::fs::rename(&temporary, &path)
        })();
        if let Err(error) = result {
            log::warn!("Chested Companions configuration: {error}");
            let _ = std::fs::remove_file(&temporary);
        } else {
            super::chested_companions::reset_configuration();
        }
    }
}

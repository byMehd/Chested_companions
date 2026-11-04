// Native adapters execute inside Horizon's game module. Persistent inventories
// use Horizon attachments; ordinary menu clicks use its existing menu engine.
use super::Game;
use horizon_client::chested_companions_core::{self as runtime, Bag, Chest, Config, Pet};
use horizon_client::mod_api::{
    AttachmentCodec, AttachmentError, AttachmentType, Compound, Holder, Tag,
};
use horizon_client::{containers::BlockEntity, items::ItemStack};
use horizon_core::random::RandomSource;
use horizon_items::item_nbt::{decode_items, encode_items, item_from_nbt, item_to_nbt};

pub const KEY_X: i32 = i32::MAX - 64;
pub const ENDER_X: i32 = i32::MAX - 65;
const KEY_Y: i32 = horizon_client::lan::menu::VEHICLE_KEY_Y;

pub fn pet_key(key: (i32, i32, i32)) -> Option<u32> {
    (key.1 == KEY_Y && matches!(key.0, KEY_X | ENDER_X))
        .then(|| u32::try_from(key.2).ok())
        .flatten()
}

static BAG: AttachmentType<Option<Bag<ItemStack>>> = AttachmentType {
    namespace: "chestedcompanions",
    name: "bag",
    default: || None,
    codec: Some(AttachmentCodec {
        encode: encode_bag,
        decode: decode_bag,
    }),
    copy_on_death: false,
    sync: false,
};

static HOLD: AttachmentType<Option<[f64; 3]>> = AttachmentType {
    namespace: "chestedcompanions",
    name: "held_position",
    default: || None,
    codec: None,
    copy_on_death: false,
    sync: false,
};

#[derive(Clone, Default)]
struct Visual {
    chest: Option<Chest>,
    open: bool,
    changed: i64,
    from: f32,
}

impl Visual {
    fn openness(&self, now: f64) -> f32 {
        let delta = ((now - self.changed as f64).max(0.0) * 0.1) as f32;
        if self.open {
            (self.from + delta).min(1.0)
        } else {
            (self.from - delta).max(0.0)
        }
    }
}

static VISUAL: AttachmentType<Visual> = AttachmentType {
    namespace: "chestedcompanions",
    name: "visual",
    default: Visual::default,
    codec: Some(AttachmentCodec {
        encode: |v| {
            let mut c = Compound::new();
            if let Some(chest) = v.chest {
                c.insert("chest".into(), Tag::String(chest.item().into()));
            }
            c.insert("open".into(), Tag::Byte(i8::from(v.open)));
            c.insert("changed".into(), Tag::Long(v.changed));
            c.insert("from".into(), Tag::Float(v.from.to_bits()));
            Ok(c)
        },
        decode: |c| {
            Ok(Visual {
                chest: match c.get(&"chest".into()) {
                    Some(Tag::String(s)) => s.to_string().ok().and_then(|s| Chest::from_item(&s)),
                    _ => None,
                },
                open: matches!(c.get(&"open".into()), Some(Tag::Byte(1))),
                changed: match c.get(&"changed".into()) {
                    Some(Tag::Long(n)) => *n,
                    _ => 0,
                },
                from: match c.get(&"from".into()) {
                    Some(Tag::Float(n)) if f32::from_bits(*n).is_finite() => {
                        f32::from_bits(*n).clamp(0.0, 1.0)
                    }
                    _ => 0.0,
                },
            })
        },
    }),
    copy_on_death: false,
    sync: true,
};

fn encode_bag(bag: &Option<Bag<ItemStack>>) -> Result<Compound, AttachmentError> {
    let mut c = Compound::new();
    c.insert("schema".into(), Tag::Int(1));
    if let Some(bag) = bag {
        c.insert("ChestItem".into(), item_to_nbt(&bag.equipped));
        c.insert("size".into(), Tag::Int(bag.slots().len() as i32));
        if bag.chest != Chest::Ender {
            c.insert("CCItems".into(), encode_items(bag.slots()));
        }
    }
    Ok(c)
}

fn decode_bag(c: &Compound) -> Result<Option<Bag<ItemStack>>, AttachmentError> {
    if c.get(&"schema".into()) != Some(&Tag::Int(1)) {
        return Err(AttachmentError::Decode);
    }
    let Some(item) = c.get(&"ChestItem".into()) else {
        return Ok(None);
    };
    let item = item_from_nbt(item).ok_or(AttachmentError::Decode)?;
    let chest = Chest::from_item(&item.item).ok_or(AttachmentError::Decode)?;
    if item.count != 1 {
        return Err(AttachmentError::Decode);
    }
    let size = match c.get(&"size".into()) {
        Some(Tag::Int(n @ (0 | 9 | 18 | 27))) => *n as usize,
        _ => return Err(AttachmentError::Decode),
    };
    if (chest == Chest::Ender) != (size == 0) {
        return Err(AttachmentError::Decode);
    }
    let mut bag = Bag::new(chest, item, (size / 9) as u8);
    bag.slots_mut()
        .clone_from_slice(&decode_items(c.get(&"CCItems".into()), size));
    Ok(Some(bag))
}

fn legacy_bag(tag: &Tag, rows: u8) -> Result<Option<Bag<ItemStack>>, AttachmentError> {
    let equipped = if let Some(item) = tag.get("ChestItem") {
        item_from_nbt(item).ok_or(AttachmentError::Decode)?
    } else if matches!(tag.get("HasChest"), Some(Tag::Byte(1))) {
        ItemStack {
            item: "minecraft:chest".into(),
            count: 1,
            damage: 0,
            extra: None,
        }
    } else {
        return Ok(None);
    };
    let chest = Chest::from_item(&equipped.item).ok_or(AttachmentError::Decode)?;
    if equipped.count != 1 {
        return Err(AttachmentError::Decode);
    }
    let mut size = usize::from(rows.clamp(1, 3)) * 9;
    if chest != Chest::Ender
        && let Some(Tag::List(items)) = tag.get("CCItems")
    {
        let mut seen = std::collections::BTreeSet::new();
        for item in items {
            let slot = match item.get("Slot") {
                Some(Tag::Byte(n)) if (0..27).contains(n) => *n as usize,
                _ => return Err(AttachmentError::Decode),
            };
            if !seen.insert(slot) || item_from_nbt(item).is_none() {
                return Err(AttachmentError::Decode);
            }
            size = size.max((slot / 9 + 1) * 9);
        }
    }
    let mut bag = Bag::new(chest, equipped, (size / 9) as u8);
    let loaded = decode_items(tag.get("CCItems"), bag.slots().len());
    bag.slots_mut().clone_from_slice(&loaded);
    Ok(Some(bag))
}

fn uuid(words: [i32; 4]) -> u128 {
    words
        .into_iter()
        .fold(0, |a, word| (a << 32) | u128::from(word as u32))
}

fn load_configuration() -> Config {
    let mut config = Config::default();
    let Ok(dir) = Game::native_config_dir() else {
        return config;
    };
    let path = dir.join("chestedcompanions_revived.json");
    let json = match std::fs::read(&path) {
        Ok(bytes) => match serde_json::from_slice::<serde_json::Value>(&bytes) {
            Ok(json) => json,
            Err(error) => {
                log::warn!("Chested Companions: {}: {error}", path.display());
                return config;
            }
        },
        Err(error) if error.kind() == std::io::ErrorKind::NotFound => return config,
        Err(error) => {
            log::warn!("Chested Companions: {}: {error}", path.display());
            return config;
        }
    };
    let boolean = |name: &str, fallback| json[name].as_bool().unwrap_or(fallback);
    config.cats = boolean("allowChestOnCats", config.cats);
    config.wolves = boolean("allowChestOnWolves", config.wolves);
    config.baby_cats = boolean("allowChestOnBabyCat", config.baby_cats);
    config.baby_wolves = boolean("allowChestOnBabyWolf", config.baby_wolves);
    config.public_chest = boolean("publicChest", config.public_chest);
    config.crouch_to_open = boolean("invertShiftToOpen", config.crouch_to_open);
    config.feeding_priority = boolean("feedingOverridesOpeningChest", config.feeding_priority);
    config.copper_golems = boolean("copperGolemsUsePetChests", config.copper_golems);
    config.hide_cat_bag = boolean("hideCatChest", config.hide_cat_bag);
    config.hide_wolf_bag = boolean("hideWolfChest", config.hide_wolf_bag);
    config.cat_icon = boolean("showChestIconOnCats", config.cat_icon);
    config.wolf_icon = boolean("showChestIconOnWolves", config.wolf_icon);
    let rows = |name: &str| match json[name]
        .as_str()
        .unwrap_or("one_row")
        .to_ascii_lowercase()
        .as_str()
    {
        "two_rows" => 2,
        "three_rows" => 3,
        _ => 1,
    };
    config.cat_rows = rows("catInventoryCapacity");
    config.wolf_rows = rows("wolfInventoryCapacity");
    config
}

thread_local! {
    static CONFIG: std::cell::RefCell<Option<(std::time::Instant, Config)>> = const { std::cell::RefCell::new(None) };
}

pub fn configuration() -> Config {
    CONFIG.with(|cached| {
        let mut cached = cached.borrow_mut();
        if let Some((time, config)) = cached.as_ref()
            && time.elapsed() < std::time::Duration::from_secs(1)
        {
            return config.clone();
        }
        let config = load_configuration();
        *cached = Some((std::time::Instant::now(), config.clone()));
        config
    })
}

pub(super) fn reset_configuration() {
    CONFIG.with(|cached| *cached.borrow_mut() = None);
}

impl Game<'_> {
    fn cc_host(&mut self) -> &mut dyn horizon_client::mod_api::Host {
        self
    }

    pub(super) fn cc_prepare(&mut self) {
        if !self.native_authoritative() {
            return;
        }
        let mut pets = Vec::new();
        let config = configuration();
        let ids: Vec<_> = self
            .mobs
            .iter()
            .filter(|m| m.alive() && Pet::from_entity(&m.kind.id).is_some())
            .map(|m| m.id)
            .collect();
        if config.copper_golems && self.native_on("chested_companions") {
            for id in ids {
                self.cc_store((KEY_X, KEY_Y, id as i32));
                let Ok(Some(bag)) = self.cc_bag(id) else {
                    continue;
                };
                if !matches!(bag.chest, Chest::Copper { .. }) {
                    continue;
                }
                let uuid = uuid(self.uuid_for(id));
                let Some(mob) = self.mobs.iter().find(|m| m.id == id) else {
                    continue;
                };
                let bounds = mob.bounds();
                let (lo, hi) = (bounds.min_position(), bounds.max_position());
                let position = [mob.position.x, mob.position.y, mob.position.z];
                let key = (KEY_X, KEY_Y, id as i32);
                self.world
                    .block_entities
                    .get_or_insert_with(key, || BlockEntity::Chest(bag.slots().to_vec()));
                pets.push(
                    horizon_client::mobs::transport_items::chested_companions::PetChest {
                        id,
                        uuid,
                        position,
                        bounds: [lo.x, lo.y, lo.z, hi.x, hi.y, hi.z],
                        key: [key.0, key.1, key.2],
                    },
                );
            }
        }
        let golems: Vec<_> = self
            .mobs
            .iter()
            .filter(|m| m.alive() && &*m.kind.id == "minecraft:copper_golem")
            .map(|m| m.id)
            .collect();
        horizon_client::mobs::transport_items::chested_companions::publish(
            &self.world,
            self.dim.id(),
            self.native_lifecycle.epoch().map_or(0, |e| e.get()),
            pets,
            &golems,
        );
    }

    fn cc_players(&mut self, id: u32) -> Vec<u128> {
        let mut players: Vec<_> = self
            .lan
            .iter()
            .flat_map(|host| host.hub.guests.iter())
            .filter(|g| g.menu.as_ref().and_then(|m| m.pos).and_then(pet_key) == Some(id))
            .map(|g| g.uuid)
            .collect();
        if self.container.and_then(pet_key) == Some(id) {
            players.push(self.native_acting_id());
        }
        players
    }

    pub(super) fn cc_hold_pre(&mut self, id: u32, kind: &str) {
        if Pet::from_entity(kind).is_none() || !matches!(self.cc_bag(id), Ok(Some(_))) {
            return;
        }
        let mut openers = runtime::Openers::default();
        for player in self.cc_players(id) {
            openers.open_player(player);
        }
        for golem in horizon_client::mobs::transport_items::chested_companions::open_golems(
            &self.world,
            self.dim.id(),
            id,
        ) {
            openers.open_golem(golem);
        }
        let now = self.world.game_time;
        if horizon_client::mobs::transport_items::chested_companions::freezes(
            &self.world,
            self.dim.id(),
            id,
        ) {
            openers.freeze(now, 10);
        }
        let Some(mob) = self.mobs.iter().find(|m| m.id == id) else {
            return;
        };
        let position = openers
            .keeps_still(
                now,
                mob.hurt_time > 0,
                mob.fire_ticks > 0,
                mob.fluids.in_water || mob.fluids.in_lava,
            )
            .then_some([mob.position.x, mob.position.y, mob.position.z]);
        let _ = HOLD.set(self.cc_host(), Holder::Entity(id), position);
    }

    pub(super) fn cc_hold_post(&mut self, id: u32, kind: &str) {
        if Pet::from_entity(kind).is_none() {
            return;
        }
        let Ok(Some(Some(position))) = HOLD.get_existing(self.cc_host(), Holder::Entity(id)) else {
            return;
        };
        if let Some(mob) = self.mobs.iter_mut().find(|m| m.id == id && m.alive())
            && mob.hurt_time == 0
            && mob.fire_ticks <= 0
            && !mob.fluids.in_water
            && !mob.fluids.in_lava
        {
            // Keep effects, gravity and ageing ticking; only horizontal walking waits.
            if (mob.position.x - position[0]).powi(2) + (mob.position.z - position[2]).powi(2) < 4.0
            {
                mob.position.x = position[0];
                mob.position.z = position[2];
            }
            mob.velocity.x = 0.0;
            mob.velocity.z = 0.0;
            mob.nav_stop();
        }
        let _ = HOLD.remove(self.cc_host(), Holder::Entity(id));
    }

    pub(super) fn cc_golem_slot(
        &mut self,
        chest: [i32; 3],
        slot: usize,
        stack: Option<ItemStack>,
    ) -> bool {
        let key = (chest[0], chest[1], chest[2]);
        if pet_key(key).is_none() {
            return false;
        }
        if let Some(BlockEntity::Chest(items)) = self.world.block_entities.get_mut(&key)
            && let Some(item) = items.get_mut(slot)
        {
            *item = stack;
        }
        self.cc_store(key);
        true
    }

    pub(super) fn cc_draw(
        &self,
        render: &super::NativeMobRender<'_>,
        draw: &mut super::native_block_entity_draw::BlockEntityDraw,
    ) {
        if Pet::from_entity(render.kind).is_none() || !self.native_on("chested_companions") {
            return;
        }
        let mut attachments = match &self.remote {
            Some(remote) => remote.attachments.get(&Holder::Entity(render.id)).cloned(),
            None => self
                .persist
                .uuids
                .get(&render.id)
                .and_then(|uuid| self.entity_attachments.get(uuid))
                .cloned(),
        };
        let Some(attachments) = attachments.as_mut() else {
            return;
        };
        let Ok(Some(visual)) = attachments.get_existing(&VISUAL) else {
            return;
        };
        let Some(chest) = visual.chest else { return };
        let Some(mob) = self.mobs.iter().find(|m| m.id == render.id) else {
            return;
        };
        let config = configuration();
        let cat = render.kind == "minecraft:cat";
        if !self.ui.hide_gui
            && (if cat {
                config.cat_icon
            } else {
                config.wolf_icon
            })
            && self.pick_entity_hit().flatten() == Some(render.id as i32)
        {
            let rotation = glam::Mat4::from_rotation_y(render.body_yaw.to_radians())
                * glam::Mat4::from_quat(self.camera_orientation());
            let center = glam::Vec3::new(0.0, mob.bounds().size().y as f32 + 0.25, 0.0);
            let corners = [
                [-0.125, 0.125, 0.0],
                [-0.125, -0.125, 0.0],
                [0.125, -0.125, 0.0],
                [0.125, 0.125, 0.0],
            ]
            .map(|v| (center + rotation.transform_vector3(glam::Vec3::from_array(v))).to_array());
            self.native_draw_quad(
                draw,
                "chestedcompanions:chest_icon",
                corners,
                [0.0, 0.0, 16.0, 16.0],
                0xFFFFFF,
            );
        }
        if if cat {
            config.hide_cat_bag
        } else {
            config.hide_wolf_bag
        } {
            return;
        }
        let base = glam::Mat4::from_rotation_y(render.body_yaw.to_radians())
            * horizon_client::mobs::render::base_transform_at(
                mob,
                render.partial,
                glam::Mat4::IDENTITY,
            );
        let Some(torso) = horizon_client::mobs::render::chested_companions::torso(
            mob,
            render.partial,
            self.res.mob_data(),
            base,
        ) else {
            return;
        };
        let size = ((torso.max.y - torso.min.y)
            / if render.kind == "minecraft:cat" {
                16.0
            } else {
                9.0
            })
        .clamp(0.25, 4.0);
        let scale = size
            * if render.kind == "minecraft:cat" {
                0.65
            } else {
                0.75
            };
        let center = (torso.min + torso.max) * 0.5;
        let angle =
            runtime::lid_angle(visual.openness(draw.game_time as f64 + f64::from(draw.partial)));
        let lid_height = 3.75 * scale;
        let top = torso.max.z + scale;
        let seam = top - lid_height;
        // At every angle the complete lid slab, rather than just its hinge
        // plane, clears the visible torso. Closed bags sit against the flank.
        let spread = runtime::swept_lid_clearance(lid_height, angle, scale).unwrap_or(0.3 * scale);
        for side in [-1.0_f32, 1.0] {
            let hinge = if side < 0.0 {
                torso.min.x - spread
            } else {
                torso.max.x + spread
            };
            let outer = hinge + side * 5.0 * scale;
            let lo = glam::Vec3::new(hinge.min(outer), center.y - 5.0 * scale, top - 10.0 * scale);
            let hi = glam::Vec3::new(hinge.max(outer), center.y + 5.0 * scale, seam);
            draw_bag_box(self, draw, chest.texture(), torso.matrix, lo, hi, 6.0, 11.0);
            let lo = glam::Vec3::new(lo.x, lo.y, seam);
            let hi = glam::Vec3::new(hi.x, hi.y, top);
            let pivot = glam::Vec3::new(hinge, center.y, seam) / 16.0;
            let lid = torso.matrix
                * glam::Mat4::from_translation(pivot)
                * glam::Mat4::from_rotation_y(-side * angle)
                * glam::Mat4::from_translation(-pivot);
            draw_bag_box(self, draw, chest.texture(), lid, lo, hi, 3.0, 6.0);
        }
    }

    fn cc_bag(&mut self, id: u32) -> Result<Option<Bag<ItemStack>>, AttachmentError> {
        // A stored None is a tombstone: old Java ChestItem fields must never
        // resurrect an inventory after removal or death.
        if let Some(bag) = BAG.get_existing(self.cc_host(), Holder::Entity(id))? {
            return Ok(bag);
        }
        let Some(raw) = self.persist.raw.get(&id).cloned() else {
            return Ok(None);
        };
        let bag = legacy_bag(&raw, 1)?;
        if bag.is_some() {
            BAG.set(self.cc_host(), Holder::Entity(id), bag.clone())?;
        }
        Ok(bag)
    }

    fn cc_visual(&mut self, id: u32, chest: Option<Chest>, open: bool) {
        let holder = Holder::Entity(id);
        let now = self.world.game_time;
        let Ok(mut visual) = VISUAL.get(self.cc_host(), holder) else {
            return;
        };
        if visual.chest != chest || visual.open != open {
            if visual.open != open
                && let Some(chest) = chest.or(visual.chest)
                && let Some(mob) = self.mobs.iter().find(|m| m.id == id)
            {
                let prefix = match chest {
                    Chest::Wood => "chest",
                    Chest::Ender => "ender_chest",
                    Chest::Copper {
                        stage: runtime::Oxidation::Weathered,
                        ..
                    } => "copper_chest_weathered",
                    Chest::Copper {
                        stage: runtime::Oxidation::Oxidized,
                        ..
                    } => "copper_chest_oxidized",
                    Chest::Copper { .. } => "copper_chest",
                };
                self.audio.play(
                    &format!(
                        "minecraft:block.{prefix}.{}",
                        if open { "open" } else { "close" }
                    ),
                    Some(mob.center()),
                    0.5,
                    0.95,
                );
            }
            visual.from = visual.openness(now as f64);
            visual.chest = chest;
            visual.open = open;
            visual.changed = now;
            if let Err(error) = VISUAL.set(self.cc_host(), holder, visual) {
                log::error!("Chested Companions visual: {error:?}");
            }
        }
    }

    pub(super) fn cc_store(&mut self, key: (i32, i32, i32)) {
        let Some(id) = pet_key(key) else { return };
        if key.0 == ENDER_X {
            return;
        }
        let Some(BlockEntity::Chest(items)) = self.world.block_entities.get(&key) else {
            return;
        };
        let items = items.clone();
        let Ok(Some(mut bag)) = self.cc_bag(id) else {
            return;
        };
        if bag.chest == Chest::Ender || items.len() != bag.slots().len() {
            return;
        }
        bag.slots_mut().clone_from_slice(&items);
        if let Err(error) = BAG.set(self.cc_host(), Holder::Entity(id), Some(bag)) {
            log::error!("Chested Companions save: {error:?}");
        }
    }

    pub(super) fn cc_interact(
        &mut self,
        entity: i32,
        kind: &str,
        stack: Option<&ItemStack>,
        claimed: &mut bool,
    ) {
        if *claimed || !self.native_authoritative() || !self.native_on("chested_companions") {
            return;
        }
        let Some(pet) = Pet::from_entity(kind) else {
            return;
        };
        let Ok(id) = u32::try_from(entity) else {
            return;
        };
        let Some(index) = self.mobs.iter().position(|m| m.id == id && m.alive()) else {
            return;
        };
        let bounds = self.mobs[index].bounds();
        if self
            .native_acting_player()
            .is_some_and(|p| p.game_mode == 3)
            || !self.within_entity_interaction_range(&bounds, 4.0)
        {
            return;
        }
        let config = configuration();
        let owner = self.mobs[index]
            .extra
            .owner
            .map(uuid)
            .filter(|_| self.mobs[index].extra.tame);
        let player = self.native_acting_id();
        let crouch = self.keys.contains(&winit::keyboard::KeyCode::ShiftLeft)
            || self.keys.contains(&winit::keyboard::KeyCode::ShiftRight);
        if !config.can_access(owner, player)
            || crouch != config.crouch_to_open
            || self
                .player
                .as_ref()
                .is_none_or(|p| p.health.health() <= 0.0)
        {
            return;
        }
        if config.feeding_priority
            && self.mobs[index].health < self.mobs[index].attrs.max_health
            && stack.is_some_and(|s| {
                self.mobs[index]
                    .kind
                    .food
                    .iter()
                    .any(|item| item == s.item.as_ref())
            })
        {
            return;
        }
        let Ok(bag) = self.cc_bag(id) else { return };
        if let Some(mut bag) = bag {
            if stack.is_some_and(|s| &*s.item == "minecraft:shears") {
                if self.cc_remove_by_player(id) {
                    self.damage_held(1);
                }
            } else {
                let next = stack.and_then(|s| {
                    if &*s.item == "minecraft:honeycomb" {
                        bag.chest.wax().map(|c| (c, true))
                    } else if self.res.items().item_in_tag("minecraft:axes", &s.item) {
                        bag.chest.scrape().map(|c| (c, false))
                    } else {
                        None
                    }
                });
                if let Some((chest, consume)) = next {
                    let waxed = matches!(bag.chest, Chest::Copper { waxed: true, .. });
                    bag.chest = chest;
                    bag.equipped.item = chest.item().into();
                    if BAG
                        .set(self.cc_host(), Holder::Entity(id), Some(bag))
                        .is_err()
                    {
                        return;
                    }
                    if consume {
                        self.consume_held();
                    } else {
                        self.damage_held(1);
                    }
                    let open = VISUAL
                        .get_existing(self.cc_host(), Holder::Entity(id))
                        .ok()
                        .flatten()
                        .is_some_and(|v| v.open);
                    self.cc_visual(id, Some(chest), open);
                    let p = self.mobs[index].position;
                    self.level_event_at(
                        if consume {
                            3003
                        } else if waxed {
                            3004
                        } else {
                            3005
                        },
                        [p.x.floor() as i32, p.y.floor() as i32, p.z.floor() as i32],
                        0,
                    );
                    self.audio.play(
                        if consume {
                            "minecraft:item.honeycomb.wax_on"
                        } else if waxed {
                            "minecraft:item.axe.wax_off"
                        } else {
                            "minecraft:item.axe.scrape"
                        },
                        Some(self.mobs[index].center()),
                        1.0,
                        1.0,
                    );
                } else {
                    self.cc_open(id, bag);
                }
            }
            *claimed = true;
        } else if config.can_equip(pet, self.mobs[index].is_baby()) {
            let Some(stack) = stack else { return };
            let Some(chest) = Chest::from_item(&stack.item) else {
                return;
            };
            let mut equipped = stack.clone();
            equipped.count = 1;
            let bag = Bag::new(chest, equipped, config.rows(pet, chest));
            if BAG
                .set(self.cc_host(), Holder::Entity(id), Some(bag))
                .is_err()
            {
                return;
            }
            if !self.creative {
                self.consume_held();
            }
            self.cc_visual(id, Some(chest), false);
            self.audio.play(
                "minecraft:entity.donkey.chest",
                Some(self.mobs[index].center()),
                1.0,
                1.0,
            );
            *claimed = true;
        }
    }

    fn cc_open(&mut self, id: u32, mut bag: Bag<ItemStack>) {
        self.close_container();
        let Some(pet) = self
            .mobs
            .iter()
            .find(|m| m.id == id)
            .and_then(|m| Pet::from_entity(&m.kind.id))
        else {
            return;
        };
        let rows = configuration().rows(pet, bag.chest);
        let overflow = bag.resize(rows);
        let chest = bag.chest;
        if BAG
            .set(self.cc_host(), Holder::Entity(id), Some(bag.clone()))
            .is_err()
        {
            return;
        }
        for stack in overflow {
            self.cc_drop(id, stack);
        }
        let key = (
            if chest == Chest::Ender {
                ENDER_X
            } else {
                KEY_X
            },
            KEY_Y,
            id as i32,
        );
        if chest == Chest::Ender {
            self.open_ender_chest(key);
        } else {
            self.world
                .block_entities
                .get_or_insert_with(key, || BlockEntity::Chest(bag.slots().to_vec()));
            self.container = Some(key);
        }
        self.screen = super::Screen::Inventory;
        self.capture(false);
        self.right = false;
        self.cc_visual(id, Some(chest), true);
    }

    fn cc_drop(&mut self, id: u32, stack: ItemStack) {
        if let Some(mob) = self.mobs.iter().find(|m| m.id == id) {
            let p = mob.position;
            self.spawn_item(stack, [p.x, p.y + 0.25, p.z], [0.0, 0.1, 0.0], 10);
        }
    }

    pub(super) fn cc_remove(&mut self, id: u32) -> bool {
        self.cc_store((KEY_X, KEY_Y, id as i32));
        let Ok(mut bag) = self.cc_bag(id) else {
            return false;
        };
        if bag.is_none() {
            return false;
        }
        if BAG.set(self.cc_host(), Holder::Entity(id), None).is_err() {
            return false;
        }
        self.cc_visual(id, None, false);
        for x in [KEY_X, ENDER_X] {
            let key = (x, KEY_Y, id as i32);
            if self.container == Some(key) {
                self.close_container();
            }
            self.world.block_entities.remove(&key);
        }
        for stack in runtime::detach(&mut bag) {
            self.cc_drop(id, stack);
        }
        true
    }

    pub(super) fn cc_remove_by_player(&mut self, id: u32) -> bool {
        let at = self.mobs.iter().find(|m| m.id == id).map(|m| m.center());
        let removed = self.cc_remove(id);
        if removed {
            self.audio
                .play("minecraft:entity.donkey.chest", at, 1.0, 1.5);
        }
        removed
    }

    pub(super) fn cc_remove_local(&mut self, id: u32) {
        if !self.native_authoritative()
            || self.container.and_then(pet_key) != Some(id)
            || self
                .native_acting_player()
                .is_some_and(|p| p.game_mode == 3)
        {
            return;
        }
        let player = self.native_acting_id();
        let Some(mob) = self.mobs.iter().find(|m| m.id == id && m.alive()) else {
            return;
        };
        let bounds = mob.bounds();
        if !configuration().can_access(mob.extra.owner.map(uuid).filter(|_| mob.extra.tame), player)
            || !self.within_entity_interaction_range(&bounds, 4.0)
        {
            return;
        }
        self.cc_remove_by_player(id);
    }

    pub(super) fn cc_open_guest(
        &mut self,
        host: &mut super::lan::LanHost,
        conn: u32,
        key: (i32, i32, i32),
    ) -> Option<bool> {
        let id = pet_key(key)?;
        let Ok(Some(bag)) = self.cc_bag(id) else {
            return Some(false);
        };
        let slots = if bag.chest == Chest::Ender {
            horizon_client::lan::menu::Slots::Ender
        } else {
            horizon_client::lan::menu::Slots::Block(key)
        };
        let items = host
            .hub
            .guest(conn)
            .and_then(|g| slots.read(&self.world, g))?;
        let menu = match items.len() {
            9 => "minecraft:generic_9x1",
            18 => "minecraft:generic_9x2",
            27 => "minecraft:generic_9x3",
            _ => return Some(false),
        };
        self.guest_close_all(host, conn);
        let title = horizon_protocol::text::translatable("container.chestedcompanions.pet", &[]);
        host.hub
            .open_container(conn, menu, title, key, slots, items);
        if let Some(guest) = host.hub.guest(conn)
            && let Some(menu) = &guest.menu
        {
            self.cc_notify_menu(guest.uuid, id, menu.window);
        }
        Some(true)
    }

    pub(super) fn cc_guest_valid(&self, guest: &horizon_client::lan::guest::Guest) -> Option<bool> {
        let key = guest.menu.as_ref()?.pos?;
        let id = pet_key(key)?;
        let config = configuration();
        Some(
            !guest.dead
                && !guest.spectator()
                && guest.dimension == self.dim.id()
                && self.mobs.iter().find(|m| m.id == id).is_some_and(|m| {
                    m.alive()
                        && m.extra.tame
                        && config.can_access(m.extra.owner.map(uuid), guest.uuid)
                        && self.cc_bag_read(id).is_some()
                        && super::lan_actions::guest_within_entity_range(guest, &m.bounds(), 4.0)
                }),
        )
    }

    fn cc_bag_read(&self, id: u32) -> Option<Bag<ItemStack>> {
        let mut map = if let Some(remote) = &self.remote {
            remote.attachments.get(&Holder::Entity(id))?.clone()
        } else {
            self.entity_attachments
                .get(self.persist.uuids.get(&id)?)?
                .clone()
        };
        map.get_existing(&BAG).ok().flatten().flatten()
    }

    pub(super) fn cc_tick(&mut self) {
        if !self.native_authoritative() {
            return;
        }
        let ids: Vec<_> = self
            .mobs
            .iter()
            .filter(|m| Pet::from_entity(&m.kind.id).is_some())
            .map(|m| m.id)
            .collect();
        for id in ids {
            self.cc_store((KEY_X, KEY_Y, id as i32));
            let Ok(Some(mut bag)) = self.cc_bag(id) else {
                continue;
            };
            let Some(index) = self.mobs.iter().position(|m| m.id == id) else {
                continue;
            };
            if !self.mobs[index].alive() {
                self.cc_remove(id);
                continue;
            }
            // RandomTickSpeed is read from the authoritative world's gamerules.
            let speed = self.level.game_rule("random_tick_speed");
            let probability = bag.chest.aging_probability(speed);
            if self.native_on("chested_companions")
                && probability > 0.0
                && f64::from(self.world.random.next_float()) < probability
                && let Some(next) = bag.chest.next()
            {
                bag.chest = next;
                bag.equipped.item = next.item().into();
                let _ = BAG.set(self.cc_host(), Holder::Entity(id), Some(bag.clone()));
            }
            let open = !self.cc_players(id).is_empty()
                || !horizon_client::mobs::transport_items::chested_companions::open_golems(
                    &self.world,
                    self.dim.id(),
                    id,
                )
                .is_empty();
            self.cc_visual(id, Some(bag.chest), open);
            if !open {
                self.world.block_entities.remove(&(KEY_X, KEY_Y, id as i32));
            }
        }
    }
}

fn draw_bag_box(
    game: &Game<'_>,
    draw: &mut super::native_block_entity_draw::BlockEntityDraw,
    sprite: &str,
    matrix: glam::Mat4,
    lo: glam::Vec3,
    hi: glam::Vec3,
    v0: f32,
    v1: f32,
) {
    let [x0, y0, z0] = lo.to_array();
    let [x1, y1, z1] = hi.to_array();
    let faces = [
        (
            [[x1, y0, z1], [x1, y0, z0], [x1, y1, z0], [x1, y1, z1]],
            [3.0, v0, 11.0, v1],
        ),
        (
            [[x0, y1, z1], [x0, y1, z0], [x0, y0, z0], [x0, y0, z1]],
            [3.0, v0, 11.0, v1],
        ),
        (
            [[x0, y0, z1], [x0, y0, z0], [x1, y0, z0], [x1, y0, z1]],
            [0.0, v0, 3.0, v1],
        ),
        (
            [[x1, y1, z1], [x1, y1, z0], [x0, y1, z0], [x0, y1, z1]],
            [11.0, v0, 14.0, v1],
        ),
        (
            [[x0, y1, z1], [x0, y0, z1], [x1, y0, z1], [x1, y1, z1]],
            [3.0, 0.0, 11.0, 3.0],
        ),
        (
            [[x1, y1, z0], [x1, y0, z0], [x0, y0, z0], [x0, y1, z0]],
            [11.0, 0.0, 19.0, 3.0],
        ),
    ];
    for (corners, uv) in faces {
        let corners = corners.map(|p| {
            matrix
                .transform_point3(glam::Vec3::from_array(p) / 16.0)
                .to_array()
        });
        let uv = [
            uv[0] * 16.0 / 22.0,
            uv[1] * 16.0 / 11.0,
            uv[2] * 16.0 / 22.0,
            uv[3] * 16.0 / 11.0,
        ];
        game.native_draw_quad(draw, sprite, corners, uv, 0xFFFFFF);
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn java_save_migration_keeps_items_beyond_the_current_configured_capacity() {
        let saved = Tag::compound([
            ("HasChest", Tag::Byte(1)),
            (
                "CCItems",
                Tag::List(vec![Tag::compound([
                    ("Slot", Tag::Byte(26)),
                    ("id", Tag::String("minecraft:diamond".into())),
                    ("count", Tag::Int(37)),
                ])]),
            ),
        ]);
        let bag = legacy_bag(&saved, 1).unwrap().unwrap();
        assert_eq!(bag.slots().len(), 27);
        assert_eq!(bag.slots()[26].as_ref().unwrap().count, 37);
        assert!(bag.slots()[0].is_none());
        let tombstone = encode_bag(&None).unwrap();
        assert!(decode_bag(&tombstone).unwrap().is_none());
    }

    #[test]
    fn malformed_java_slots_and_unsupported_chests_are_preserved_as_errors() {
        let invalid = Tag::compound([
            ("HasChest", Tag::Byte(1)),
            (
                "CCItems",
                Tag::List(vec![Tag::compound([
                    ("Slot", Tag::Byte(27)),
                    ("id", Tag::String("minecraft:diamond".into())),
                ])]),
            ),
        ]);
        assert!(legacy_bag(&invalid, 1).is_err());
        let unsupported = Tag::compound([(
            "ChestItem",
            Tag::compound([("id", Tag::String("lolmcv:oak_chest".into()))]),
        )]);
        assert!(legacy_bag(&unsupported, 1).is_err());
    }

    #[test]
    fn bag_codec_keeps_slot_numbers_and_item_components() {
        let chest = item_from_nbt(&Tag::compound([
            ("id", Tag::String("minecraft:chest".into())),
            ("count", Tag::Int(1)),
            (
                "components",
                Tag::compound([(
                    "minecraft:custom_data",
                    Tag::compound([("owner_note", Tag::String("test".into()))]),
                )]),
            ),
        ]))
        .unwrap();
        let mut bag = Bag::new(Chest::Wood, chest.clone(), 3);
        let mut sword = ItemStack {
            item: "minecraft:diamond_sword".into(),
            count: 1,
            damage: 53,
            extra: None,
        };
        bag.slots_mut()[26] = Some(sword.clone());
        let saved = encode_bag(&Some(bag)).unwrap();
        let loaded = decode_bag(&saved).unwrap().unwrap();
        assert_eq!(loaded.equipped, chest);
        assert_eq!(loaded.slots().len(), 27);
        assert!(loaded.slots()[0].is_none());
        assert_eq!(loaded.slots()[26], Some(sword.clone()));
        sword.damage = 54;
        assert_ne!(loaded.slots()[26], Some(sword));
    }

    #[test]
    fn ender_codec_cannot_save_a_players_contents_on_a_pet() {
        let chest = ItemStack {
            item: "minecraft:ender_chest".into(),
            count: 1,
            damage: 0,
            extra: None,
        };
        let saved = encode_bag(&Some(Bag::new(Chest::Ender, chest, 3))).unwrap();
        assert_eq!(saved.get(&"size".into()), Some(&Tag::Int(0)));
        assert!(!saved.contains_key(&"CCItems".into()));
        assert!(decode_bag(&saved).unwrap().unwrap().slots().is_empty());
    }

    #[test]
    fn unknown_saved_schema_fails_instead_of_replacing_an_inventory() {
        let saved = [("schema".into(), Tag::Int(2))].into();
        assert!(matches!(decode_bag(&saved), Err(AttachmentError::Decode)));
    }

    #[test]
    fn visual_codec_preserves_float_bits_and_rejects_nan() {
        let codec = VISUAL.codec.as_ref().unwrap();
        let mut saved = (codec.encode)(&Visual {
            chest: Some(Chest::Ender),
            open: true,
            changed: 100,
            from: 0.375,
        })
        .unwrap();
        let loaded = (codec.decode)(&saved).unwrap();
        assert_eq!(loaded.from, 0.375);
        assert_eq!(loaded.chest, Some(Chest::Ender));
        assert_eq!(loaded.openness(102.5), 0.625);
        saved.insert("from".into(), Tag::Float(f32::NAN.to_bits()));
        assert_eq!((codec.decode)(&saved).unwrap().from, 0.0);
    }
}

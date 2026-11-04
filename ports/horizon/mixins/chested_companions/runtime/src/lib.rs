// Rust adaptation of the MIT-licensed Chested Companions by Khajiitos and
// Chested Companions: Revived by byMehd. See the repository's LICENSE.

use std::collections::BTreeSet;

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum Pet {
    Cat,
    Wolf,
}

impl Pet {
    pub fn from_entity(id: &str) -> Option<Self> {
        match id {
            "minecraft:cat" => Some(Self::Cat),
            "minecraft:wolf" => Some(Self::Wolf),
            _ => None,
        }
    }
}

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum Oxidation {
    Fresh,
    Exposed,
    Weathered,
    Oxidized,
}

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum Chest {
    Wood,
    Ender,
    Copper { stage: Oxidation, waxed: bool },
}

impl Chest {
    pub fn from_item(id: &str) -> Option<Self> {
        use Oxidation::*;
        Some(match id {
            "minecraft:chest" => Self::Wood,
            "minecraft:ender_chest" => Self::Ender,
            "minecraft:copper_chest" => Self::Copper {
                stage: Fresh,
                waxed: false,
            },
            "minecraft:exposed_copper_chest" => Self::Copper {
                stage: Exposed,
                waxed: false,
            },
            "minecraft:weathered_copper_chest" => Self::Copper {
                stage: Weathered,
                waxed: false,
            },
            "minecraft:oxidized_copper_chest" => Self::Copper {
                stage: Oxidized,
                waxed: false,
            },
            "minecraft:waxed_copper_chest" => Self::Copper {
                stage: Fresh,
                waxed: true,
            },
            "minecraft:waxed_exposed_copper_chest" => Self::Copper {
                stage: Exposed,
                waxed: true,
            },
            "minecraft:waxed_weathered_copper_chest" => Self::Copper {
                stage: Weathered,
                waxed: true,
            },
            "minecraft:waxed_oxidized_copper_chest" => Self::Copper {
                stage: Oxidized,
                waxed: true,
            },
            _ => return None,
        })
    }

    pub fn item(self) -> &'static str {
        use Oxidation::*;
        match self {
            Self::Wood => "minecraft:chest",
            Self::Ender => "minecraft:ender_chest",
            Self::Copper {
                stage: Fresh,
                waxed: false,
            } => "minecraft:copper_chest",
            Self::Copper {
                stage: Exposed,
                waxed: false,
            } => "minecraft:exposed_copper_chest",
            Self::Copper {
                stage: Weathered,
                waxed: false,
            } => "minecraft:weathered_copper_chest",
            Self::Copper {
                stage: Oxidized,
                waxed: false,
            } => "minecraft:oxidized_copper_chest",
            Self::Copper {
                stage: Fresh,
                waxed: true,
            } => "minecraft:waxed_copper_chest",
            Self::Copper {
                stage: Exposed,
                waxed: true,
            } => "minecraft:waxed_exposed_copper_chest",
            Self::Copper {
                stage: Weathered,
                waxed: true,
            } => "minecraft:waxed_weathered_copper_chest",
            Self::Copper {
                stage: Oxidized,
                waxed: true,
            } => "minecraft:waxed_oxidized_copper_chest",
        }
    }

    pub fn wax(self) -> Option<Self> {
        match self {
            Self::Copper {
                stage,
                waxed: false,
            } => Some(Self::Copper { stage, waxed: true }),
            _ => None,
        }
    }

    pub fn scrape(self) -> Option<Self> {
        use Oxidation::*;
        match self {
            Self::Copper { stage, waxed: true } => Some(Self::Copper {
                stage,
                waxed: false,
            }),
            Self::Copper {
                stage: Exposed,
                waxed: false,
            } => Some(Self::Copper {
                stage: Fresh,
                waxed: false,
            }),
            Self::Copper {
                stage: Weathered,
                waxed: false,
            } => Some(Self::Copper {
                stage: Exposed,
                waxed: false,
            }),
            Self::Copper {
                stage: Oxidized,
                waxed: false,
            } => Some(Self::Copper {
                stage: Weathered,
                waxed: false,
            }),
            _ => None,
        }
    }

    pub fn next(self) -> Option<Self> {
        use Oxidation::*;
        let stage = match self {
            Self::Copper {
                stage: Fresh,
                waxed: false,
            } => Exposed,
            Self::Copper {
                stage: Exposed,
                waxed: false,
            } => Weathered,
            Self::Copper {
                stage: Weathered,
                waxed: false,
            } => Oxidized,
            _ => return None,
        };
        Some(Self::Copper {
            stage,
            waxed: false,
        })
    }

    /// Matches Revived's isolated copper-block random-tick probability.
    pub fn aging_probability(self, random_tick_speed: i32) -> f64 {
        if random_tick_speed <= 0 || self.next().is_none() {
            return 0.0;
        }
        let modifier = if matches!(
            self,
            Self::Copper {
                stage: Oxidation::Fresh,
                ..
            }
        ) {
            0.75
        } else {
            1.0
        };
        (f64::from(random_tick_speed) / 4096.0 * 0.056_888_89 * modifier).min(1.0)
    }

    pub fn texture(self) -> &'static str {
        match self {
            Self::Wood => "chestedcompanions:gui/saddlebag/chest",
            Self::Ender => "chestedcompanions:gui/saddlebag/ender",
            Self::Copper {
                stage: Oxidation::Fresh,
                ..
            } => "chestedcompanions:gui/saddlebag/copper",
            Self::Copper {
                stage: Oxidation::Exposed,
                ..
            } => "chestedcompanions:gui/saddlebag/copper_exposed",
            Self::Copper {
                stage: Oxidation::Weathered,
                ..
            } => "chestedcompanions:gui/saddlebag/copper_weathered",
            Self::Copper {
                stage: Oxidation::Oxidized,
                ..
            } => "chestedcompanions:gui/saddlebag/copper_oxidized",
        }
    }
}

#[derive(Clone, Debug)]
pub struct Config {
    pub cats: bool,
    pub wolves: bool,
    pub baby_cats: bool,
    pub baby_wolves: bool,
    pub cat_rows: u8,
    pub wolf_rows: u8,
    pub public_chest: bool,
    pub crouch_to_open: bool,
    pub feeding_priority: bool,
    pub copper_golems: bool,
    pub hide_cat_bag: bool,
    pub hide_wolf_bag: bool,
    pub cat_icon: bool,
    pub wolf_icon: bool,
}

impl Default for Config {
    fn default() -> Self {
        Self {
            cats: true,
            wolves: true,
            baby_cats: false,
            baby_wolves: false,
            cat_rows: 1,
            wolf_rows: 1,
            public_chest: false,
            crouch_to_open: false,
            feeding_priority: true,
            copper_golems: true,
            hide_cat_bag: false,
            hide_wolf_bag: false,
            cat_icon: false,
            wolf_icon: false,
        }
    }
}

impl Config {
    pub fn can_equip(&self, pet: Pet, baby: bool) -> bool {
        match pet {
            Pet::Cat => self.cats && (!baby || self.baby_cats),
            Pet::Wolf => self.wolves && (!baby || self.baby_wolves),
        }
    }

    pub fn rows(&self, pet: Pet, chest: Chest) -> u8 {
        if chest == Chest::Ender {
            return 3;
        }
        match pet {
            Pet::Cat => self.cat_rows,
            Pet::Wolf => self.wolf_rows,
        }
        .clamp(1, 3)
    }

    pub fn can_access(&self, owner: Option<u128>, player: u128) -> bool {
        owner.is_some_and(|owner| owner == player || self.public_chest)
    }
}

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum Storage {
    Pet,
    PersonalEnder(u128),
}

pub fn storage(chest: Chest, viewer: u128) -> Storage {
    if chest == Chest::Ender {
        Storage::PersonalEnder(viewer)
    } else {
        Storage::Pet
    }
}

/// Ender inventories belong to the viewer and never become a pet's contents.
#[derive(Clone, Debug)]
pub struct Bag<S> {
    pub chest: Chest,
    pub equipped: S,
    slots: Vec<Option<S>>,
}

impl<S> Bag<S> {
    pub fn new(chest: Chest, equipped: S, rows: u8) -> Self {
        let size = if chest == Chest::Ender {
            0
        } else {
            usize::from(rows.clamp(1, 3)) * 9
        };
        Self {
            chest,
            equipped,
            slots: std::iter::repeat_with(|| None).take(size).collect(),
        }
    }

    pub fn slots(&self) -> &[Option<S>] {
        &self.slots
    }
    pub fn slots_mut(&mut self) -> &mut [Option<S>] {
        &mut self.slots
    }

    /// A smaller configured capacity cannot discard occupied old slots.
    pub fn resize(&mut self, rows: u8) -> Vec<S> {
        let size = if self.chest == Chest::Ender {
            0
        } else {
            usize::from(rows.clamp(1, 3)) * 9
        };
        if size < self.slots.len() {
            self.slots.drain(size..).flatten().collect()
        } else {
            self.slots.resize_with(size, || None);
            Vec::new()
        }
    }
}

/// Take ownership before dropping anything so reentrant removal/death cannot duplicate it.
pub fn detach<S>(bag: &mut Option<Bag<S>>) -> Vec<S> {
    let Some(bag) = bag.take() else {
        return Vec::new();
    };
    std::iter::once(bag.equipped)
        .chain(bag.slots.into_iter().flatten())
        .collect()
}

#[derive(Clone, Debug, Default)]
pub struct Openers {
    players: BTreeSet<u128>,
    golems: BTreeSet<u32>,
    frozen_until: i64,
}

impl Openers {
    pub fn is_open(&self) -> bool {
        !self.players.is_empty() || !self.golems.is_empty()
    }
    /// True only on the closed -> open edge (one sound for multiple viewers).
    pub fn open_player(&mut self, player: u128) -> bool {
        let was_open = self.is_open();
        self.players.insert(player);
        !was_open && self.is_open()
    }
    /// True only when the last opener leaves.
    pub fn close_player(&mut self, player: u128) -> bool {
        let was_open = self.is_open();
        self.players.remove(&player);
        was_open && !self.is_open()
    }
    pub fn open_golem(&mut self, golem: u32) -> bool {
        let was_open = self.is_open();
        self.golems.insert(golem);
        !was_open && self.is_open()
    }
    pub fn close_golem(&mut self, golem: u32) -> bool {
        let was_open = self.is_open();
        self.golems.remove(&golem);
        was_open && !self.is_open()
    }
    pub fn freeze(&mut self, now: i64, ticks: u32) {
        self.frozen_until = self.frozen_until.max(now.saturating_add(i64::from(ticks)));
    }
    pub fn keeps_still(&self, now: i64, hurt: bool, burning: bool, swimming: bool) -> bool {
        !hurt && !burning && !swimming && (self.is_open() || now < self.frozen_until)
    }
}

pub fn lid_angle(openness: f32) -> f32 {
    if !openness.is_finite() {
        return 0.0;
    }
    (1.0 - (1.0 - openness.clamp(0.0, 1.0)).powi(3)) * std::f32::consts::FRAC_PI_2
}

#[derive(Clone, Copy, Debug, Default, PartialEq, Eq)]
pub struct Visit {
    travel: u16,
    interaction: u8,
    done: bool,
}

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum VisitAction {
    Walk,
    Open,
    Wait,
    Sound,
    Take,
    CloseAndWalk,
    Abort,
}

impl Visit {
    pub fn is_open(&self) -> bool {
        !self.done && self.interaction > 0
    }

    pub fn step(&mut self, distance_squared: f64) -> VisitAction {
        if self.done || !distance_squared.is_finite() || distance_squared < 0.0 {
            self.done = true;
            return VisitAction::Abort;
        }
        if self.interaction > 0 && distance_squared > 4.0 {
            self.interaction = 0;
            self.travel = 0;
            return VisitAction::CloseAndWalk;
        }
        if self.interaction == 0 {
            if distance_squared > 1.0 {
                self.travel += 1;
                if self.travel > 600 {
                    self.done = true;
                    return VisitAction::Abort;
                }
                return VisitAction::Walk;
            }
            self.interaction = 1;
            return VisitAction::Open;
        }
        self.interaction += 1;
        match self.interaction {
            9 => VisitAction::Sound,
            60 => {
                self.done = true;
                VisitAction::Take
            }
            _ => VisitAction::Wait,
        }
    }
}

/// Offset in bag units needed to move even a buried hinge outside the posed torso.
pub fn lid_clearance(hinge: f32, torso_half_width: f32, scale: f32) -> Option<f32> {
    if ![hinge, torso_half_width, scale]
        .iter()
        .all(|v| v.is_finite())
        || scale <= 0.0
        || torso_half_width < 0.0
    {
        return None;
    }
    Some(((torso_half_width + 0.3 * scale - hinge.abs()) / scale).max(0.0))
}

pub fn swept_lid_clearance(height: f32, angle: f32, scale: f32) -> Option<f32> {
    if ![height, angle, scale].iter().all(|v| v.is_finite()) || height < 0.0 || scale <= 0.0 {
        return None;
    }
    Some(0.3 * scale + height * angle.clamp(0.0, std::f32::consts::FRAC_PI_2).sin())
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn the_entire_rotating_lid_slab_stays_outside_the_torso() {
        for scale in [0.125, 0.5, 0.65, 0.75, 1.0, 4.0] {
            for step in 0..=90 {
                let angle = (step as f32).to_radians();
                let spread = swept_lid_clearance(3.75 * scale, angle, scale).unwrap();
                for x in [0.0, 5.0 * scale] {
                    for z in [0.0, 3.75 * scale] {
                        let rotated_x = spread + x * angle.cos() - z * angle.sin();
                        assert!(
                            rotated_x >= 0.3 * scale - 1e-5,
                            "scale={scale}, angle={step}, x={x}, z={z}"
                        );
                    }
                }
            }
        }
        assert_eq!(swept_lid_clearance(1.0, f32::NAN, 1.0), None);
    }

    #[test]
    fn golem_takes_once_after_sixty_interaction_ticks() {
        let mut visit = Visit::default();
        assert_eq!(visit.step(0.25), VisitAction::Open);
        for _ in 2..9 {
            assert_eq!(visit.step(0.25), VisitAction::Wait);
        }
        assert_eq!(visit.step(0.25), VisitAction::Sound);
        for _ in 10..60 {
            assert_eq!(visit.step(0.25), VisitAction::Wait);
        }
        assert_eq!(visit.step(0.25), VisitAction::Take);
        assert!(!visit.is_open());
        assert_eq!(visit.step(0.25), VisitAction::Abort);
    }

    #[test]
    fn golem_abandons_an_unreachable_pet_and_reopens_after_chasing() {
        let mut visit = Visit::default();
        for _ in 0..600 {
            assert_eq!(visit.step(16.0), VisitAction::Walk);
        }
        assert_eq!(visit.step(16.0), VisitAction::Abort);
        let mut visit = Visit::default();
        assert_eq!(visit.step(0.0), VisitAction::Open);
        assert_eq!(visit.step(4.1), VisitAction::CloseAndWalk);
        assert!(!visit.is_open());
        assert_eq!(visit.step(2.0), VisitAction::Walk);
        assert_eq!(visit.step(1.0), VisitAction::Open);
        assert_eq!(visit.step(f64::NAN), VisitAction::Abort);
    }

    #[test]
    fn accepts_only_the_ten_vanilla_chests() {
        let ids = [
            "chest",
            "ender_chest",
            "copper_chest",
            "exposed_copper_chest",
            "weathered_copper_chest",
            "oxidized_copper_chest",
            "waxed_copper_chest",
            "waxed_exposed_copper_chest",
            "waxed_weathered_copper_chest",
            "waxed_oxidized_copper_chest",
        ];
        for id in ids {
            let id = format!("minecraft:{id}");
            assert_eq!(Chest::from_item(&id).unwrap().item(), id);
        }
        for id in [
            "minecraft:trapped_chest",
            "minecraft:copper_ingot",
            "morechests:chest",
            "ender_chest",
            "",
        ] {
            assert!(Chest::from_item(id).is_none());
        }
    }

    #[test]
    fn wax_and_scrape_do_not_skip_oxidation_stages() {
        let fresh = Chest::from_item("minecraft:copper_chest").unwrap();
        let exposed = fresh.next().unwrap();
        let weathered = exposed.next().unwrap();
        let oxidized = weathered.next().unwrap();
        assert_eq!(oxidized.item(), "minecraft:oxidized_copper_chest");
        assert_eq!(oxidized.scrape(), Some(weathered));
        assert_eq!(weathered.scrape(), Some(exposed));
        assert_eq!(exposed.scrape(), Some(fresh));
        assert_eq!(fresh.scrape(), None);
        assert_eq!(oxidized.next(), None);
        for chest in [fresh, exposed, weathered, oxidized] {
            let waxed = chest.wax().unwrap();
            assert_eq!(waxed.wax(), None);
            assert_eq!(waxed.next(), None);
            assert_eq!(waxed.scrape(), Some(chest));
            assert_eq!(waxed.texture(), chest.texture());
        }
    }

    #[test]
    fn oxidation_honors_disabled_random_ticks_and_wax() {
        let fresh = Chest::from_item("minecraft:copper_chest").unwrap();
        assert_eq!(fresh.aging_probability(0), 0.0);
        assert_eq!(fresh.aging_probability(-1), 0.0);
        assert_eq!(fresh.wax().unwrap().aging_probability(1000), 0.0);
        assert!((fresh.aging_probability(3) - 0.000_031_250_000_61).abs() < 1e-12);
        assert!((fresh.next().unwrap().aging_probability(3) - 0.000_041_666_667_48).abs() < 1e-12);
    }

    #[test]
    fn ender_storage_is_separate_for_each_viewer_and_never_dropped() {
        assert_eq!(storage(Chest::Ender, 12), Storage::PersonalEnder(12));
        assert_eq!(storage(Chest::Ender, 34), Storage::PersonalEnder(34));
        let mut bag = Some(Bag::new(Chest::Ender, "ender chest", 3));
        assert!(bag.as_ref().unwrap().slots().is_empty());
        assert_eq!(detach(&mut bag), ["ender chest"]);
        assert!(detach(&mut bag).is_empty());
    }

    #[test]
    fn removal_then_death_drops_contents_exactly_once() {
        let mut bag = Bag::new(Chest::Wood, "named chest", 1);
        bag.slots_mut()[0] = Some("enchanted sword");
        bag.slots_mut()[8] = Some("64 diamonds");
        let mut bag = Some(bag);
        assert_eq!(
            detach(&mut bag),
            ["named chest", "enchanted sword", "64 diamonds"]
        );
        assert!(detach(&mut bag).is_empty());
    }

    #[test]
    fn shrinking_returns_overflow_instead_of_losing_it() {
        let mut bag = Bag::new(Chest::Wood, "chest", 3);
        bag.slots_mut()[0] = Some("kept");
        bag.slots_mut()[9] = Some("returned");
        bag.slots_mut()[26] = Some("also returned");
        assert_eq!(bag.resize(1), ["returned", "also returned"]);
        assert_eq!(bag.slots().len(), 9);
        assert_eq!(bag.slots()[0], Some("kept"));
    }

    #[test]
    fn public_access_still_requires_a_tamed_pet() {
        let mut config = Config::default();
        assert!(!config.can_access(None, 1));
        assert!(!config.can_access(Some(2), 1));
        assert!(config.can_access(Some(1), 1));
        config.public_chest = true;
        assert!(config.can_access(Some(2), 1));
        assert!(!config.can_access(None, 1));
        assert!(!config.can_equip(Pet::Cat, true));
        assert!(config.can_equip(Pet::Wolf, false));
        config.wolves = false;
        assert!(!config.can_equip(Pet::Wolf, false));
    }

    #[test]
    fn opening_twice_and_multiple_viewers_play_only_edge_sounds() {
        let mut open = Openers::default();
        assert!(open.open_player(1));
        assert!(!open.open_player(1));
        assert!(!open.open_player(2));
        assert!(!open.open_golem(3));
        assert!(!open.close_player(1));
        assert!(!open.close_player(2));
        assert!(open.close_golem(3));
        assert!(!open.close_golem(3));
    }

    #[test]
    fn danger_releases_the_pet_even_when_a_menu_is_open() {
        let mut open = Openers::default();
        open.open_player(1);
        assert!(open.keeps_still(0, false, false, false));
        assert!(!open.keeps_still(0, true, false, false));
        assert!(!open.keeps_still(0, false, true, false));
        assert!(!open.keeps_still(0, false, false, true));
        open.close_player(1);
        open.freeze(50, 10);
        open.freeze(50, 1);
        assert!(open.keeps_still(59, false, false, false));
        assert!(!open.keeps_still(60, false, false, false));
    }

    #[test]
    fn open_lid_clears_small_baby_and_wide_pack_torsos() {
        for width in [0.5, 2.0, 4.0, 7.0, 16.0] {
            for scale in [0.125, 0.5, 0.8, 1.0, 2.0] {
                for hinge in [0.25, 1.0, 4.0, 12.0] {
                    let spread = lid_clearance(hinge, width, scale).unwrap();
                    assert!(hinge + spread * scale >= width + 0.3 * scale - 1e-5);
                }
            }
        }
        assert_eq!(lid_clearance(4.0, 2.0, 1.0), Some(0.0));
        assert_eq!(lid_clearance(0.0, 1.0, 0.0), None);
        assert_eq!(lid_clearance(f32::NAN, 1.0, 1.0), None);
        assert_eq!(lid_angle(0.0), 0.0);
        assert_eq!(lid_angle(1.0), std::f32::consts::FRAC_PI_2);
        assert!(lid_angle(0.5) > std::f32::consts::FRAC_PI_4);
    }
}

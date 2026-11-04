// The game publishes server-owned pet locations before each level tick. The
// existing copper-golem brain keeps control of navigation and placed chests.
use super::{
    Bx, Mob, MobEvent, Tracker, Transport, WalkTarget, cg, held, mem, mem_ref, stop_in_place,
};
use crate::{
    chested_companions_core::{Visit, VisitAction},
    mobs::Who,
    world::World,
};
use std::{
    collections::BTreeMap,
    sync::{Mutex, OnceLock},
};

#[derive(Clone)]
pub struct PetChest {
    pub id: u32,
    pub uuid: u128,
    pub position: [f64; 3],
    pub bounds: [f64; 6],
    pub key: [i32; 3],
}

#[derive(Clone, Default)]
struct Task {
    target: Option<u128>,
    visit: Visit,
    next_search: i64,
    visited: BTreeMap<u128, i64>,
    freeze: bool,
    opened_key: Option<[i32; 3]>,
    has_item: bool,
}

#[derive(Default)]
struct Level {
    pets: Vec<PetChest>,
    tasks: BTreeMap<u32, Task>,
}

static LEVELS: OnceLock<Mutex<BTreeMap<usize, Level>>> = OnceLock::new();

fn levels() -> &'static Mutex<BTreeMap<usize, Level>> {
    LEVELS.get_or_init(Default::default)
}
fn key(world: &World) -> usize {
    world as *const World as usize
}

pub fn publish(world: &World, pets: Vec<PetChest>, golems: &[u32]) {
    let mut levels = levels().lock().unwrap_or_else(|e| e.into_inner());
    // Only transient snapshots; bounded even if many worlds open in one process.
    if levels.len() > 16 {
        levels.clear();
    }
    let level = levels.entry(key(world)).or_default();
    level.pets = pets;
    level.tasks.retain(|id, _| golems.contains(id));
}

pub fn open_golems(world: &World, pet: u32) -> Vec<u32> {
    let levels = levels().lock().unwrap_or_else(|e| e.into_inner());
    let Some(level) = levels.get(&key(world)) else {
        return Vec::new();
    };
    let Some(pet) = level.pets.iter().find(|p| p.id == pet) else {
        return Vec::new();
    };
    level
        .tasks
        .iter()
        .filter(|(_, t)| t.target == Some(pet.uuid) && t.visit.is_open())
        .map(|(id, _)| *id)
        .collect()
}

pub fn freezes(world: &World, pet: u32) -> bool {
    let levels = levels().lock().unwrap_or_else(|e| e.into_inner());
    let Some(level) = levels.get(&key(world)) else {
        return false;
    };
    let Some(pet) = level.pets.iter().find(|p| p.id == pet) else {
        return false;
    };
    level
        .tasks
        .values()
        .any(|t| t.target == Some(pet.uuid) && t.freeze)
}

fn distance(mob: &Mob, pet: &PetChest) -> f64 {
    let point = super::center(mob);
    (0..3)
        .map(|i| (point[i] - point[i].clamp(pet.bounds[i], pet.bounds[i + 3])).powi(2))
        .sum()
}

fn candidate(mob: &Mob, pets: &[PetChest], task: &Task, now: i64) -> Option<PetChest> {
    pets.iter()
        .filter(|p| {
            (mob.position.y - p.position[1]).abs() <= 8.0
                && (mob.position.x - p.position[0]).hypot(mob.position.z - p.position[2]) <= 16.0
                && task.visited.get(&p.uuid).is_none_or(|until| now >= *until)
        })
        .min_by(|a, b| distance(mob, a).total_cmp(&distance(mob, b)))
        .cloned()
}

pub(super) fn try_start(
    transport: &mut Transport,
    mob: &mut Mob,
    cx: &mut Bx<'_, '_>,
) -> Option<bool> {
    if transport.target.is_some()
        || held(mob).is_some()
        || mob.extra.leash.is_some()
        || mob.extra.mount.is_some()
        || mem_ref(mob).is_panicking.present()
    {
        return None;
    }
    let levels = levels().lock().unwrap_or_else(|e| e.into_inner());
    let level = levels.get(&key(cx.w.world))?;
    let empty = Task::default();
    let task = level.tasks.get(&mob.id).unwrap_or(&empty);
    if cx.now < task.next_search || candidate(mob, &level.pets, task, cx.now).is_none() {
        return None;
    }
    transport.running = true;
    Some(true)
}

fn close(mob: &mut Mob, cx: &mut Bx<'_, '_>, task: &mut Task) {
    if let Some(chest) = task.opened_key.take() {
        cx.events.push(MobEvent::ChestOpen { chest, open: false });
    }
    mob.extra.opened_chest = None;
    mob.extra.variant3 = cg::IDLE;
}

fn walk(mob: &mut Mob, pet: &PetChest) {
    let target = Tracker::Entity {
        who: Who::Mob(pet.id),
        eye: false,
        block_eye: false,
    };
    mem(mob).walk_target.set(WalkTarget {
        target,
        speed: 1.0,
        close: 1,
    });
    mem(mob).look_target.set(target);
}

pub(super) fn tick(transport: &mut Transport, mob: &mut Mob, cx: &mut Bx<'_, '_>) -> bool {
    let world_key = key(cx.w.world);
    let (pets, mut task) = {
        let mut levels = levels().lock().unwrap_or_else(|e| e.into_inner());
        let Some(level) = levels.get_mut(&world_key) else {
            return false;
        };
        (
            level.pets.clone(),
            level.tasks.remove(&mob.id).unwrap_or_default(),
        )
    };
    let eligible = held(mob).is_none()
        && mob.extra.leash.is_none()
        && mob.extra.mount.is_none()
        && !mem_ref(mob).is_panicking.present()
        && transport.target.is_none();
    if !eligible {
        close(mob, cx, &mut task);
        task.target = None;
        task.freeze = false;
        save(world_key, mob.id, task);
        return false;
    }
    let mut pet = task
        .target
        .and_then(|id| pets.iter().find(|p| p.uuid == id).cloned());
    if task.target.is_some() && pet.is_none() {
        close(mob, cx, &mut task);
        task.target = None;
        task.freeze = false;
    }
    if pet.is_none() && cx.now >= task.next_search {
        task.visited.retain(|_, until| *until > cx.now);
        pet = candidate(mob, &pets, &task, cx.now);
        task.target = pet.as_ref().map(|p| p.uuid);
        task.visit = Visit::default();
    }
    let Some(pet) = pet else {
        task.next_search = cx.now.saturating_add(40);
        save(world_key, mob.id, task);
        return false;
    };
    let d = distance(mob, &pet);
    task.freeze = d < 6.25;
    match task.visit.step(d) {
        VisitAction::Walk => walk(mob, &pet),
        VisitAction::CloseAndWalk => {
            close(mob, cx, &mut task);
            walk(mob, &pet);
        }
        VisitAction::Open => {
            stop_in_place(mob);
            let items = super::slots(cx.w, &[pet.key]);
            task.has_item = items.iter().any(Option::is_some);
            mob.extra.variant3 = if task.has_item {
                cg::GETTING_ITEM
            } else {
                cg::GETTING_NO_ITEM
            };
            mob.extra.opened_chest = Some(pet.key);
            task.opened_key = Some(pet.key);
            cx.events.push(MobEvent::ChestOpen {
                chest: pet.key,
                open: true,
            });
        }
        VisitAction::Sound => {
            stop_in_place(mob);
            if !mob.silent {
                let sound = if task.has_item {
                    "entity.copper_golem.no_item_get"
                } else {
                    "entity.copper_golem.no_item_no_get"
                };
                cx.events.push(MobEvent::Sound(
                    sound,
                    [mob.position.x, mob.position.y, mob.position.z],
                    1.0,
                    1.0,
                ));
            }
        }
        VisitAction::Wait => stop_in_place(mob),
        VisitAction::Take => {
            close(mob, cx, &mut task);
            let items = super::slots(cx.w, &[pet.key]);
            if items.iter().any(Option::is_some) {
                transport.pick_up(mob, cx, &[pet.key], &items);
                task.visited.clear();
            } else {
                task.visited.insert(pet.uuid, cx.now.saturating_add(6000));
            }
            task.target = None;
            task.freeze = false;
            transport.running = false;
        }
        VisitAction::Abort => {
            close(mob, cx, &mut task);
            task.visited.insert(pet.uuid, cx.now.saturating_add(6000));
            task.target = None;
            task.freeze = false;
            mob.nav_stop();
            mem(mob).walk_target.clear();
            transport.running = false;
        }
    }
    if task.target.is_some() {
        mem(mob).look_target.set(Tracker::Entity {
            who: Who::Mob(pet.id),
            eye: true,
            block_eye: true,
        });
    }
    save(world_key, mob.id, task);
    true
}

fn save(world: usize, mob: u32, task: Task) {
    levels()
        .lock()
        .unwrap_or_else(|e| e.into_inner())
        .entry(world)
        .or_default()
        .tasks
        .insert(mob, task);
}

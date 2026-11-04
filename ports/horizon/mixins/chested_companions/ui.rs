use super::{Game, chested_companions::pet_key};
use horizon_client::{
    hud::{HudBatch, rgba},
    mod_api::NativeScreenView,
};
use std::{
    collections::BTreeMap,
    sync::{Mutex, OnceLock},
};

const MENU: &str = "chested_companions:menu_v1";
const REMOVE: &str = "chested_companions:remove_v1";

#[derive(Clone, Copy)]
struct Menu {
    pet: u32,
    window: i32,
    pending: bool,
}

static MENUS: OnceLock<Mutex<BTreeMap<(usize, u64), Menu>>> = OnceLock::new();
fn menus() -> &'static Mutex<BTreeMap<(usize, u64), Menu>> {
    MENUS.get_or_init(Default::default)
}

fn packet(bytes: &[u8]) -> Option<(u32, i32)> {
    if bytes.len() != 8 {
        return None;
    }
    let pet = u32::from_le_bytes(bytes[..4].try_into().ok()?);
    let window = i32::from_le_bytes(bytes[4..].try_into().ok()?);
    (pet <= i32::MAX as u32 && window > 0).then_some((pet, window))
}

fn encode(pet: u32, window: i32) -> [u8; 8] {
    let mut bytes = [0; 8];
    bytes[..4].copy_from_slice(&pet.to_le_bytes());
    bytes[4..].copy_from_slice(&window.to_le_bytes());
    bytes
}

fn button(screen: &NativeScreenView<'_>) -> [f32; 4] {
    [
        screen.rect[0] + screen.rect[2] + 4.0,
        screen.rect[1] + 4.0,
        20.0,
        20.0,
    ]
}
fn hover(screen: &NativeScreenView<'_>) -> bool {
    let [x, y, w, h] = button(screen);
    screen.mouse[0] >= x
        && screen.mouse[0] < x + w
        && screen.mouse[1] >= y
        && screen.mouse[1] < y + h
}

impl Game<'_> {
    fn cc_ui_key(&self) -> (usize, u64) {
        (
            &self.world as *const _ as usize,
            self.native_lifecycle.epoch().map_or(0, |e| e.get()),
        )
    }

    fn cc_menu(&self) -> Option<Menu> {
        if !self.native_on("chested_companions") {
            return None;
        }
        if let Some(remote) = &self.remote {
            let state = menus()
                .lock()
                .unwrap_or_else(|e| e.into_inner())
                .get(&self.cc_ui_key())
                .copied()?;
            (remote.menu.as_ref()?.0 == state.window).then_some(state)
        } else {
            Some(Menu {
                pet: pet_key(self.container?)?,
                window: 0,
                pending: false,
            })
        }
    }

    pub(super) fn cc_notify_menu(&mut self, player: u128, pet: u32, window: i32) {
        if let Err(error) = self.native_net_send_to_player(player, MENU, &encode(pet, window)) {
            log::debug!("Chested Companions menu notification: {error}");
        }
    }

    pub(super) fn cc_receive_menu(&mut self, channel: &str, data: &[u8]) {
        if channel != MENU || self.remote.is_none() {
            return;
        }
        let Some((pet, window)) = packet(data) else {
            return;
        };
        let mut menus = menus().lock().unwrap_or_else(|e| e.into_inner());
        if menus.len() > 16 {
            menus.clear();
        }
        menus.insert(
            self.cc_ui_key(),
            Menu {
                pet,
                window,
                pending: false,
            },
        );
    }

    pub(super) fn cc_draw_remove(&self, batch: &mut HudBatch, screen: &NativeScreenView<'_>) {
        if screen.id != "inventory" || self.cc_menu().is_none() {
            return;
        }
        let [x, y, w, h] = button(screen);
        let over = hover(screen);
        batch.fill(&self.gui.atlas, x, y, w, h, rgba(20, 20, 20, 255));
        batch.fill(
            &self.gui.atlas,
            x + 1.0,
            y + 1.0,
            w - 2.0,
            h - 2.0,
            if over {
                rgba(180, 180, 180, 255)
            } else {
                rgba(110, 110, 110, 255)
            },
        );
        self.native_text(batch, "X", [x + 7.0, y + 6.0], 0xFFFFFF, true);
        if over {
            self.native_text(
                batch,
                &horizon_client::lang::get("gui.chestedcompanions.remove_chest"),
                [x - 90.0, y + 24.0],
                0xFFFFFF,
                true,
            );
        }
    }

    pub(super) fn cc_click_remove(
        &self,
        screen: &NativeScreenView<'_>,
        button: i32,
        canceled: &mut bool,
    ) {
        if *canceled || screen.id != "inventory" || button != 0 || !hover(screen) {
            return;
        }
        let Some(mut menu) = self.cc_menu() else {
            return;
        };
        menu.pending = true;
        menus()
            .lock()
            .unwrap_or_else(|e| e.into_inner())
            .insert(self.cc_ui_key(), menu);
        *canceled = true;
    }

    pub(super) fn cc_send_remove(&mut self) {
        if self.remote.as_ref().is_some_and(|r| r.menu.is_none()) {
            menus()
                .lock()
                .unwrap_or_else(|e| e.into_inner())
                .remove(&self.cc_ui_key());
            return;
        }
        let pending = {
            let mut menus = menus().lock().unwrap_or_else(|e| e.into_inner());
            let Some(menu) = menus.get_mut(&self.cc_ui_key()) else {
                return;
            };
            if !menu.pending {
                return;
            }
            menu.pending = false;
            *menu
        };
        if self
            .cc_menu()
            .is_none_or(|m| m.pet != pending.pet || m.window != pending.window)
        {
            return;
        }
        if self.remote.is_some() {
            let _ = self.native_net_send_to_server(REMOVE, &encode(pending.pet, pending.window));
        } else {
            self.cc_remove_local(pending.pet);
        }
    }

    pub(super) fn cc_receive_remove(&mut self, player: u128, channel: &str, data: &[u8]) {
        if channel != REMOVE || !self.native_authoritative() {
            return;
        }
        let Some((id, window)) = packet(data) else {
            return;
        };
        let valid = self
            .lan
            .iter()
            .flat_map(|host| host.hub.guests.iter())
            .find(|g| g.uuid == player)
            .is_some_and(|guest| {
                guest
                    .menu
                    .as_ref()
                    .is_some_and(|m| m.window == window && m.pos.and_then(pet_key) == Some(id))
                    && self.cc_guest_valid(guest) == Some(true)
            });
        if valid {
            self.cc_remove_by_player(id);
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn remove_packet_rejects_wrong_lengths_negative_ids_and_invalid_windows() {
        assert_eq!(packet(&[7, 0, 0, 0, 3, 0, 0, 0]), Some((7, 3)));
        for bytes in [
            &[][..],
            &[0; 7][..],
            &[0; 9][..],
            &[0; 8][..],
            &[255, 255, 255, 255, 1, 0, 0, 0][..],
            &[1, 0, 0, 0, 255, 255, 255, 255][..],
        ] {
            assert_eq!(packet(bytes), None);
        }
    }
}

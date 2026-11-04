# Chested Companions: Revived for Horizon

A separate native Rust port for **Minecraft Java 26.3 on Horizon**. The Java
Stonecutter project at the repository root continues to build the Fabric,
Forge and NeoForge releases.

This port is under development. It is not a release and is not yet ready for
player worlds. Passing the runtime tests alone does not establish that the
complete mod builds or works in Horizon.

## Scope

Cats and wolves, wooden chests, all eight copper chest states, the player's
actual Ender inventory, ownership, safe removal and death drops, copper
golems, animated saddlebags and sounds. Texture packs and Fresh Animations
are the intended resource-pack integrations. Fat Cat and More Chest Variants
are outside the scope of this port.

The exact supported engine and transformation-tool revisions are recorded in
`compatibility.json`. The starting target is Horizon's official
Nightly 2026-11-04, commit `67dad0d1a5adca139b426c8114e74a33b376ba64`.

## Development

```sh
cargo fmt --all --check
cargo clippy --workspace --all-targets --locked -- -D warnings
cargo test --workspace --locked
```

The runtime crate contains independent gameplay rules. Small hook adapters
connect them to Horizon's menus, attachments and renderer. A source export is
transformed by the official `horizon-mixins` tool; the original Horizon
checkout and installed launcher are not modified.

Build `horizon-mixins-cli` from the revision in `compatibility.json`, then:

```sh
python3 tools/stage.py --horizon /path/to/horizon --mixins /path/to/horizon-mixins
cargo check --locked --manifest-path target/game/Cargo.toml -p horizon-client --no-default-features
```

GitHub Actions runs both the standalone tests and a check of the real modded
26.3 game. Integration and visual testing must use disposable worlds and
separate Horizon data directories. Minecraft assets and third-party packs
are not included in this repository.

## License

MIT, as in the repository's `LICENSE`. The gameplay is adapted from
Khajiitos's Chested Companions and byMehd's Chested Companions: Revived.
Horizon and its native mod tools are separate MIT-licensed projects.

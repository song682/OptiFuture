# OptiFutureOptimized

This is a mod that forked from the MCPatcher for Forge. Aiming to backport new features from OptiFine/continuity.

# License

- Original code by prupe is licensed under [MIT](LICENSE-Original).
- MCpatcher For Forge code made by mist345 is under [LGPLv3](LICENSE-MCPatcherForge).
- The textures is under [CC-BY-SA-4.0 by darkbum](LICENSE-Assets).
- And Now It Back to the [MIT](LICENSE-ModernFeature).

Known incompatibilities:
- FastCraft, causes weird chunk flickering on loading (no fix planned,hence it's a closed source mod)
- Future commands, something happens between its asm and my mixins. No fix planned as I can't find the source code.
- Optifine: Implements the same features, resulting in a crash on startup (no fix planned)
- FalseTweaks: Will need looking into

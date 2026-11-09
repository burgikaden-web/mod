package dev.kaden.oneforall.data;

import dev.kaden.oneforall.OneForAllMod;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class OFAWorldState extends PersistentState {
    private final Map<UUID, OFAPlayerProgress> players = new HashMap<>();

    public static OFAWorldState get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager().getOrCreate(
                OFAWorldState::fromNbt,
                OFAWorldState::new,
                OneForAllMod.MOD_ID + "_progress"
        );
    }

    public OFAPlayerProgress getPlayer(UUID uuid) {
        OFAPlayerProgress existing = players.get(uuid);
        if (existing != null) return existing;
        OFAPlayerProgress created = new OFAPlayerProgress();
        players.put(uuid, created);
        markDirty();
        return created;
    }

    public void markProgressDirty() {
        markDirty();
    }

    private static OFAWorldState fromNbt(NbtCompound nbt) {
        OFAWorldState state = new OFAWorldState();
        NbtList list = nbt.getList("Players", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            NbtCompound playerTag = list.getCompound(i);
            if (!playerTag.containsUuid("Uuid")) continue;
            UUID uuid = playerTag.getUuid("Uuid");
            OFAPlayerProgress progress = new OFAPlayerProgress(
                    playerTag.getInt("Vestiges"),
                    playerTag.getInt("Energy"),
                    !playerTag.contains("Active") || playerTag.getBoolean("Active"),
                    playerTag.getBoolean("DangerSense")
            );
            progress.setQuirkId(playerTag.contains("Quirk") ? playerTag.getString("Quirk") : "one_for_all");
            state.players.put(uuid, progress);
        }
        return state;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtList list = new NbtList();
        for (Map.Entry<UUID, OFAPlayerProgress> entry : players.entrySet()) {
            NbtCompound playerTag = new NbtCompound();
            OFAPlayerProgress progress = entry.getValue();
            playerTag.putUuid("Uuid", entry.getKey());
            playerTag.putInt("Vestiges", progress.getVestiges());
            playerTag.putInt("Energy", progress.getEnergy());
            playerTag.putBoolean("Active", progress.isActive());
            playerTag.putBoolean("DangerSense", progress.isDangerSenseActive());
            playerTag.putString("Quirk", progress.getQuirkId());
            list.add(playerTag);
        }
        nbt.put("Players", list);
        return nbt;
    }
}

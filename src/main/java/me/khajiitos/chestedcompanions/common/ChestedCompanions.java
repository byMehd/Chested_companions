package me.khajiitos.chestedcompanions.common;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChestedCompanions {
    public static final String MOD_ID = "chestedcompanions_revived";
    public static final Logger LOGGER = LoggerFactory.getLogger("Chested Companions");
    public static final TagKey<Item> PET_CHEST_ITEM = TagKey.create(Registries.ITEM, id("chestedcompanions", "pet_chest_item"));

    /** An identifier; code that is not processed by Stonecutter, such as the loaders' tests, goes through here instead of naming its class. */
    public static Identifier id(String namespace, String path) {
        return Identifier.fromNamespaceAndPath(namespace, path);
    }
}

package com.boaat.jazzy_cookin.registry;

import com.boaat.jazzy_cookin.JazzyCookin;
import com.boaat.jazzy_cookin.menu.KitchenStationMenu;
import com.boaat.jazzy_cookin.menu.KitchenStorageMenu;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class JazzyMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, JazzyCookin.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<KitchenStationMenu>> KITCHEN_STATION =
            MENUS.register("kitchen_station", () -> IMenuTypeExtension.create(KitchenStationMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<KitchenStorageMenu>> KITCHEN_STORAGE =
            MENUS.register("kitchen_storage", () -> IMenuTypeExtension.create(KitchenStorageMenu::new));

    private JazzyMenus() {
    }
}

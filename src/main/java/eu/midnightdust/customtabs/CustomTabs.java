package eu.midnightdust.customtabs;
;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.apache.logging.log4j.LogManager;

import java.io.InputStream;
import java.util.*;

public class CustomTabs implements ClientModInitializer {
    private boolean startup = true;

    @Override
    public void onInitializeClient() {
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public Identifier getFabricId() {
                return new Identifier("customtabs", "tabs");
            }
            @Override
            public void reload(ResourceManager manager) {
                if (startup) {
                    for (Identifier id : manager.findResources("customtabs", path -> path.contains(".properties"))) {
                        try (InputStream stream = manager.getResource(id).getInputStream()) {
                            Properties properties = new Properties();
                            properties.load(stream);
                            ItemStack icon = ItemStack.EMPTY;
                            if (properties.containsKey("icon")) {
                                Optional<Item> item = Registry.ITEM.getOrEmpty(Identifier.tryParse(properties.getProperty("icon")));
                                if (item.isPresent()) {
                                    icon = new ItemStack(item.get());
                                }

                            }
                            final ItemStack finalIcon = icon;
                            ArrayList<ItemStack> items = new ArrayList<>();
                            if (properties.containsKey("items")) {
                                Arrays.stream(properties.getProperty("items").split("\\|")).forEach(string -> {
                                    String rawItem = Arrays.stream(string.split("\\{")).findFirst().get();
                                    NbtCompound nbt = null;
                                    if (!string.matches(rawItem)) {
                                        String nbtString = string.substring(string.indexOf("{"));

                                        if (!nbtString.equals("")) {
                                            try {
                                                nbt = StringNbtReader.parse(nbtString);
                                            } catch (CommandSyntaxException e) {
                                                LogManager.getLogger("CustomTabs").info(e);
                                            }
                                        }
                                    }
                                    Optional<Item> item = Registry.ITEM.getOrEmpty(Identifier.tryParse(rawItem));
                                    NbtCompound finalNbt = nbt;
                                    if (item.isPresent()) {
                                        ItemStack stack = new ItemStack(item.get());
                                        if (nbt != null) stack.setNbt(finalNbt);
                                        items.add(stack);
                                    }
                                });
                            }
                            Identifier tab = new Identifier("customtabs",
                                    id.toString().replace(".properties", "").replace("customtabs", "")
                                            .replace("minecraft", "").replace(":", "")
                                            .replace("/", ""));
                            FabricItemGroupBuilder.create(tab).icon(() -> finalIcon).appendItems(itemStacks -> itemStacks.addAll(items)).build();
                        } catch (Exception e) {
                            LogManager.getLogger("CustomTabs").error("Error occurred while loading " + id.toString(), e);
                        }
                    }
                    startup = false;
                }
            }
        });
    }
}

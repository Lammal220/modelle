package net.me.modelle;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.me.modelle.c2s.*;
import net.me.modelle.util.*;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.common.data.LanguageProvider;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModelBlock {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Main.MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCKS_ENTITY = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Main.MODID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Main.MODID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, Main.MODID);

    public static final RegistryObject<Block> MODEL_BLOCK = BLOCKS.register("model_block", () -> new ModelBlocks(BlockBehaviour.Properties.of()
            .sound(SoundType.AMETHYST)
            .noOcclusion())
    );

    public static final RegistryObject<BlockEntityType<ModelBlockEntity>> MODEL_BLOCK_ENTITY = BLOCKS_ENTITY.register("model_block", () -> BlockEntityType.Builder.of(
            ModelBlockEntity::new,
            MODEL_BLOCK.get()
    ).build(null));

    public static final RegistryObject<MenuType<ModelBlockMenu>> MODEL_BLOCK_MENU = MENUS.register("model_block", () -> IForgeMenuType.create(ModelBlockMenu::new));

    public static final RegistryObject<Item> MODEL_BLOCK_ITEM = ITEMS.register("model_block", ()-> new BlockItem(MODEL_BLOCK.get(), new Item.Properties()));

    public static void registerBlocks(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCKS_ENTITY.register(bus);
        MENUS.register(bus);
    }

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event){
        DataGenerator gen = event.getGenerator();
        PackOutput out = gen.getPackOutput();
        ExistingFileHelper efh = event.getExistingFileHelper();
        gen.addProvider(event.includeClient(), new RegisterJsonBlocks(out,efh));
        gen.addProvider(event.includeClient(), new RegisterRuLocale(out));
        gen.addProvider(event.includeClient(), new RegisterUsLocale(out));
    }

    static class RegisterJsonBlocks extends BlockStateProvider {
        public RegisterJsonBlocks(PackOutput output, ExistingFileHelper existingFileHelper) {
            super(output, Main.MODID, existingFileHelper);
        }

        @Override
        protected void registerStatesAndModels() {
            Block block = ModelBlock.MODEL_BLOCK.get();
            ResourceLocation limeConcreteTexture = mcLoc("block/lime_concrete");
            ModelFile model = models().cubeAll(ModelBlock.MODEL_BLOCK.getId().getPath(), limeConcreteTexture);
            simpleBlock(block, model);
            simpleBlockItem(block, model);
        }
    }

    static class RegisterRuLocale extends LanguageProvider {
        public RegisterRuLocale(PackOutput output) {
            super(output, Main.MODID, "ru_ru");
        }
        @Override
        protected void addTranslations() {
            add("gui."+Main.MODID+".button.save", "Сохранить");
            add("gui."+Main.MODID+".scale", "Размер: %s");
            add("gui."+Main.MODID+".allow_copy", "Разрешить копирование");
            add("gui."+Main.MODID+".allow_edit", "Разрешить редактирование");
            add("gui."+Main.MODID+".force_render", "Всегда рисовать (Без отсечения)");
            add("gui."+Main.MODID+".button.copy_hash", "Копировать Хэш");
            add("gui."+Main.MODID+".hash_copied", "Хэш скопирован в буфер обмена!");
        }
    }

    static class RegisterUsLocale extends LanguageProvider {
        public RegisterUsLocale(PackOutput output) {
            super(output, Main.MODID, "en_us");
        }
        @Override
        protected void addTranslations() {
            add("gui."+Main.MODID+".button.save", "Save");
            add("gui."+Main.MODID+".scale", "Scale: %s");
            add("gui."+Main.MODID+".allow_copy", "Allow Copy");
            add("gui."+Main.MODID+".allow_edit", "Allow Edit");
            add("gui."+Main.MODID+".force_render", "Always Render");
            add("gui."+Main.MODID+".button.copy_hash", "Copy Hash");
            add("gui."+Main.MODID+".hash_copied", "Hash copied to clipboard!");
        }
    }

    public static class ModelBlockEntity extends BlockEntity implements MenuProvider {
        public static final ConcurrentHashMap<String, Long> REQUESTED_HASHES = new ConcurrentHashMap<>();

        public float raw = 0f;
        public float pitch = 0f;
        public float roll = 0f;
        public float scale = 1f;
        public float posX = 0f;
        public float posY = 0f;
        public float posZ = 0f;
        public String path = "";
        public volatile String modelHash = "";
        public UUID ownerUUID;
        public boolean allowCopy = false;
        public boolean allowEdit = false;
        public boolean forceRender = false;

        public volatile VertexBuffer clientVBO;
        public volatile ResourceLocation clientTextureLoc;
        public volatile String lastPath = "";

        public final AtomicBoolean loadAttempted = new AtomicBoolean(false);
        private volatile boolean isLoadingAsync = false;

        public ModelBlockEntity(BlockPos p_155229_, BlockState p_155230_) {
            super(MODEL_BLOCK_ENTITY.get(), p_155229_, p_155230_);
        }

        @ParametersAreNonnullByDefault
        @Override
        protected void saveAdditional(CompoundTag tag) {
            super.saveAdditional(tag);
            tag.putFloat("raw", raw);
            tag.putFloat("pitch", pitch);
            tag.putString("path", path);
            tag.putFloat("roll", roll);
            tag.putFloat("scale", scale);
            tag.putFloat("posx", posX);
            tag.putFloat("posy", posY);
            tag.putFloat("posz", posZ);
            tag.putString("modelHash", modelHash);
            if (ownerUUID != null) tag.putUUID("owner", ownerUUID);
            tag.putBoolean("allowCopy", allowCopy);
            tag.putBoolean("allowEdit", allowEdit);
            tag.putBoolean("forceRender", forceRender);
        }

        @ParametersAreNonnullByDefault
        @Override
        public void load(CompoundTag tag) {
            String oldHash = this.modelHash;
            super.load(tag);
            this.raw = tag.getFloat("raw");
            this.pitch = tag.getFloat("pitch");
            this.path = tag.getString("path");
            this.roll = tag.getFloat("roll");
            this.scale = tag.getFloat("scale");
            this.posX = tag.getFloat("posx");
            this.posY = tag.getFloat("posy");
            this.posZ = tag.getFloat("posz");
            this.modelHash = tag.getString("modelHash");
            if (tag.hasUUID("owner")) ownerUUID = tag.getUUID("owner");
            allowCopy = tag.getBoolean("allowCopy");
            allowEdit = tag.getBoolean("allowEdit");
            forceRender = tag.getBoolean("forceRender");

            if (!this.modelHash.equals(oldHash)) {
                this.loadAttempted.set(false);
            }
        }

        @Override
        public CompoundTag getUpdateTag() {
            CompoundTag tag = new CompoundTag();
            saveAdditional(tag);
            return tag;
        }

        @Override
        public AABB getRenderBoundingBox() {
            if (forceRender) {
                return INFINITE_EXTENT_AABB;
            }

            BlockPos pos = this.getBlockPos();
            double minX = pos.getX() + posX - scale;
            double minY = pos.getY() + posY - scale;
            double minZ = pos.getZ() + posZ - scale;
            double maxX = pos.getX() + posX + scale + 1.0;
            double maxY = pos.getY() + posY + scale + 1.0;
            double maxZ = pos.getZ() + posZ + scale + 1.0;

            return new net.minecraft.world.phys.AABB(minX, minY, minZ, maxX, maxY, maxZ);
        }

        public boolean isOwner(Player player) {
            return ownerUUID != null && ownerUUID.equals(player.getUUID());
        }

        @Nullable
        @Override
        public Packet<ClientGamePacketListener> getUpdatePacket() {
            return ClientboundBlockEntityDataPacket.create(this);
        }

        @NotNull
        @Override
        public Component getDisplayName() {
            return Component.literal("Модель");
        }

        @ParametersAreNonnullByDefault
        @Override
        public @Nullable AbstractContainerMenu createMenu(int ID, Inventory inv, Player player) {
            return new ModelBlockMenu(ID, inv, this);
        }

        @Override
        public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
            super.onDataPacket(net, pkt);
        }

        public static void cleanupRequestedHashes() {
            long now = System.currentTimeMillis();
            REQUESTED_HASHES.entrySet().removeIf(e -> now - e.getValue() > 60000);
        }

        public void loadExternalModel() {
            if (this.level == null || !this.level.isClientSide) return;
            if (this.modelHash == null || this.modelHash.isEmpty()) return;

            final String targetHash = this.modelHash;

            // Атомарная проверка: уже загружено, уже грузится, или хэш не изменился
            if (targetHash.equals(this.lastPath) && this.clientVBO != null) return;
            if (!this.loadAttempted.compareAndSet(false, true)) return; // AtomicBoolean вместо volatile boolean

            final BlockPos currentPos = this.worldPosition.immutable();
            final Level currentLevel = this.level;

            CompletableFuture.supplyAsync(() -> {
                try {
                    File fileToLoad = null;
                    File worldStorage = ModelManager.getModelStorageDir(currentLevel);
                    File worldFile = new File(worldStorage, targetHash + ".mbm");

                    if (worldFile.exists()) {
                        fileToLoad = worldFile;
                    } else {
                        File cachedFile = new File(ModelManager.CACHE_DIR, targetHash + ".mbm");
                        if (cachedFile.exists()) fileToLoad = cachedFile;
                    }

                    if (fileToLoad == null || !fileToLoad.exists()) {
                        Long lastRequest = REQUESTED_HASHES.get(targetHash);
                        if (lastRequest == null || System.currentTimeMillis() - lastRequest > 30000) {
                            REQUESTED_HASHES.put(targetHash, System.currentTimeMillis());
                            ModMessages.SIMPLE.sendToServer(new C2SRequestDownloadPacket(targetHash));
                        }
                        return null;
                    }

                    REQUESTED_HASHES.remove(targetHash);
                    MbmData data = MbmSerializer.load(fileToLoad);
                    NativeImage img = NativeImage.read(new ByteArrayInputStream(data.textureBytes()));

                    int vertexSize = DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP.getVertexSize();
                    BufferBuilder builder = new BufferBuilder(data.vertices().size() * vertexSize);
                    builder.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP);

                    for (RawData.BakedVertex bv : data.vertices()) {
                        int r = bv.rgba & 0xFF;
                        int g = (bv.rgba >> 8) & 0xFF;
                        int b = (bv.rgba >> 16) & 0xFF;
                        int a = (bv.rgba >> 24) & 0xFF;
                        builder.vertex(bv.x, bv.y, bv.z).color(r, g, b, a).uv(bv.u, bv.v).uv2(15728880).endVertex();
                    }

                    return new AsyncLoadResult(img, builder.end(), targetHash);
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }, Util.backgroundExecutor()).thenAcceptAsync(result -> {
                // Всё ещё в фоновом потоке — только подготовка
                if (result == null || this.isRemoved()) {
                    this.loadAttempted.set(false); // Разрешаем повторную попытку
                    return;
                }

                // GPU-операции — на main thread
                Minecraft.getInstance().execute(() -> {
                    if (this.isRemoved()) {
                        result.image.close();
                        return;
                    }

                    DynamicTexture newTexture = null;
                    ResourceLocation newTextureLoc = null;
                    VertexBuffer newVBO = null;
                    boolean success = false;

                    try {
                        newTexture = new DynamicTexture(result.image);
                        newTextureLoc = Minecraft.getInstance().getTextureManager().register(
                                "modelle_tex_" + currentPos.toShortString().replace(", ", "_"), newTexture);

                        newVBO = new VertexBuffer(VertexBuffer.Usage.STATIC);
                        newVBO.bind();
                        newVBO.upload(result.buffer);
                        VertexBuffer.unbind();

                        // Чистим старые ресурсы
                        if (this.clientTextureLoc != null) {
                            Minecraft.getInstance().getTextureManager().release(this.clientTextureLoc);
                        }
                        if (this.clientVBO != null) {
                            this.clientVBO.close();
                        }

                        this.clientTextureLoc = newTextureLoc;
                        this.clientVBO = newVBO;
                        this.lastPath = result.hash;
                        success = true;
                    } catch (Exception e) {
                        e.printStackTrace();
                        this.loadAttempted.set(false); // Разрешаем повторную попытку при ошибке
                    } finally {
                        if (!success) {
                            if (newTextureLoc != null) {
                                Minecraft.getInstance().getTextureManager().release(newTextureLoc);
                            } else if (newTexture != null) {
                                newTexture.close();
                            }
                            if (newVBO != null) {
                                newVBO.close();
                            }
                        }
                    }
                });
            }, Util.backgroundExecutor());
        }

        private record AsyncLoadResult(NativeImage image, BufferBuilder.RenderedBuffer buffer, String hash) {}

        @Override
        public void setRemoved() {
            super.setRemoved();
            if (this.level != null && this.level.isClientSide) {
                if (this.clientVBO != null) {
                    this.clientVBO.close();
                    this.clientVBO = null;
                }
                if (this.clientTextureLoc != null) {
                    Minecraft.getInstance().getTextureManager().release(this.clientTextureLoc);
                    this.clientTextureLoc = null;
                }
            }
        }
    }

    static class ModelBlocks extends Block implements EntityBlock {
        public ModelBlocks(Properties p_49795_) {
            super(p_49795_);
        }

        @ParametersAreNonnullByDefault
        @Override
        public @Nullable BlockEntity newBlockEntity(BlockPos p_153215_, BlockState p_153216_) {
            return new ModelBlockEntity(p_153215_, p_153216_);
        }

        @Override
        public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable net.minecraft.world.entity.LivingEntity placer, ItemStack stack) {
            if (placer instanceof Player player) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof ModelBlockEntity entity) {
                    entity.ownerUUID = player.getUUID();
                    entity.setChanged();
                }
            }
        }

        @ParametersAreNonnullByDefault
        @Override
        public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            if (!level.isClientSide) {
                ModelBlockEntity entity = (ModelBlockEntity) level.getBlockEntity(pos);
                if (entity != null) {
                    NetworkHooks.openScreen((ServerPlayer) player, entity, (buf) -> buf.writeBlockPos(pos));
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        @Override
        public RenderShape getRenderShape(BlockState p_60550_) {
            return RenderShape.INVISIBLE;
        }

        @Override
        public VoxelShape getOcclusionShape(BlockState state, BlockGetter world, BlockPos pos) {
            return Shapes.empty();
        }

        @Override
        public int getLightBlock(BlockState state, BlockGetter worldIn, BlockPos pos) {
            return 0;
        }

        @Override
        public boolean propagatesSkylightDown(BlockState state, BlockGetter reader, BlockPos pos) {
            return true;
        }

        @Override
        public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
            return Shapes.empty();
        }

        @Override
        public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
            return Shapes.block();
        }
    }

    public static class ModelBlockMenu extends AbstractContainerMenu{
        private final ModelBlockEntity blockEntity;
        public ModelBlockMenu(int containerID, Inventory inv, ModelBlockEntity entity) {
            super(MODEL_BLOCK_MENU.get(), containerID);
            blockEntity = entity;
        }
        public ModelBlockMenu(int containerID, Inventory inv, FriendlyByteBuf buf) {
            this(containerID, inv, (ModelBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos()));
        }

        @ParametersAreNonnullByDefault
        @NotNull
        @Override
        public ItemStack quickMoveStack(Player p_38941_, int p_38942_) {
            return ItemStack.EMPTY;
        }

        @ParametersAreNonnullByDefault
        @Override
        public boolean stillValid(Player player) {
            return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()), player, MODEL_BLOCK.get());
        }
    }

    public static class ModelBlockScreen extends AbstractContainerScreen<ModelBlockMenu> {
        private EditBox textBox;
        private net.minecraft.client.gui.components.Checkbox checkCopy, checkEdit, checkRender;

        public ModelBlockScreen(ModelBlockMenu menu, Inventory inv, Component component) {
            super(menu, inv, component);
        }

        private float round(float value, int places) {
            float scale = (float) Math.pow(10, places);
            return Math.round(value * scale) / scale;
        }

        @Override
        protected void init() {
            super.init();
            int centerX = this.width / 2;
            int centerY = this.height / 2;

            ModelBlockEntity entity = this.menu.blockEntity;
            boolean isOwner = entity.isOwner(this.minecraft.player);
            boolean canEdit = isOwner || entity.allowEdit;
            boolean canCopy = isOwner || entity.allowCopy;

            this.textBox = new EditBox(this.font, centerX - 100, centerY - 80, 200, 20, Component.literal("Path"));
            this.textBox.setMaxLength(500);
            this.textBox.setValue(entity.path);
            this.textBox.setEditable(canEdit);
            addRenderableWidget(this.textBox);

            addRenderableWidget(new Button.Builder(Component.literal("-"), (b) -> {
                entity.posX = round(entity.posX - (Screen.hasShiftDown() ? 0.1f : 0.01f), 2); sendUpdatePos();
            }).pos(centerX - 210, centerY - 40).size(20, 20).build()).active = canEdit;

            addRenderableWidget(new Button.Builder(Component.literal("+"), (b) -> {
                entity.posX = round(entity.posX + (Screen.hasShiftDown() ? 0.1f : 0.01f), 2); sendUpdatePos();
            }).pos(centerX - 130, centerY - 40).size(20, 20).build()).active = canEdit;

            addRenderableWidget(new Button.Builder(Component.literal("-"), (b) -> {
                entity.posY = round(entity.posY - (Screen.hasShiftDown() ? 0.1f : 0.01f), 2); sendUpdatePos();
            }).pos(centerX - 210, centerY).size(20, 20).build()).active = canEdit;

            addRenderableWidget(new Button.Builder(Component.literal("+"), (b) -> {
                entity.posY = round(entity.posY + (Screen.hasShiftDown() ? 0.1f : 0.01f), 2); sendUpdatePos();
            }).pos(centerX - 130, centerY).size(20, 20).build()).active = canEdit;

            addRenderableWidget(new Button.Builder(Component.literal("-"), (b) -> {
                entity.posZ = round(entity.posZ - (Screen.hasShiftDown() ? 0.1f : 0.01f), 2); sendUpdatePos();
            }).pos(centerX - 210, centerY + 40).size(20, 20).build()).active = canEdit;

            addRenderableWidget(new Button.Builder(Component.literal("+"), (b) -> {
                entity.posZ = round(entity.posZ + (Screen.hasShiftDown() ? 0.1f : 0.01f), 2); sendUpdatePos();
            }).pos(centerX - 130, centerY + 40).size(20, 20).build()).active = canEdit;

            addRenderableWidget(new Button.Builder(Component.literal("-"), (b) -> {
                entity.raw = round(entity.raw - (Screen.hasShiftDown() ? 5.0f : 1.0f), 1); sendUpdateValues();
            }).pos(centerX - 50, centerY - 40).size(20, 20).build()).active = canEdit;

            addRenderableWidget(new Button.Builder(Component.literal("+"), (b) -> {
                entity.raw = round(entity.raw + (Screen.hasShiftDown() ? 5.0f : 1.0f), 1); sendUpdateValues();
            }).pos(centerX + 30, centerY - 40).size(20, 20).build()).active = canEdit;

            addRenderableWidget(new Button.Builder(Component.literal("-"), (b) -> {
                entity.pitch = round(entity.pitch - (Screen.hasShiftDown() ? 5.0f : 1.0f), 1); sendUpdateValues();
            }).pos(centerX - 50, centerY).size(20, 20).build()).active = canEdit;

            addRenderableWidget(new Button.Builder(Component.literal("+"), (b) -> {
                entity.pitch = round(entity.pitch + (Screen.hasShiftDown() ? 5.0f : 1.0f), 1); sendUpdateValues();
            }).pos(centerX + 30, centerY).size(20, 20).build()).active = canEdit;

            addRenderableWidget(new Button.Builder(Component.literal("-"), (b) -> {
                entity.roll = round(entity.roll - (Screen.hasShiftDown() ? 5.0f : 1.0f), 1); sendUpdateValues();
            }).pos(centerX - 50, centerY + 40).size(20, 20).build()).active = canEdit;

            addRenderableWidget(new Button.Builder(Component.literal("+"), (b) -> {
                entity.roll = round(entity.roll + (Screen.hasShiftDown() ? 5.0f : 1.0f), 1); sendUpdateValues();
            }).pos(centerX + 30, centerY + 40).size(20, 20).build()).active = canEdit;

            addRenderableWidget(new Button.Builder(Component.literal("-"), (b) -> {
                float amount = Screen.hasControlDown() ? 5.0f : (Screen.hasShiftDown() ? 1.0f : 0.1f);
                entity.scale = Math.max(0.1f, round(entity.scale - amount, 1)); sendUpdateValues();
            }).pos(centerX + 110, centerY - 40).size(20, 20).build()).active = canEdit;

            addRenderableWidget(new Button.Builder(Component.literal("+"), (b) -> {
                float amount = Screen.hasControlDown() ? 5.0f : (Screen.hasShiftDown() ? 1.0f : 0.1f);
                entity.scale = round(entity.scale + amount, 1); sendUpdateValues();
            }).pos(centerX + 190, centerY - 40).size(20, 20).build()).active = canEdit;

            if (isOwner) {
                checkCopy = new net.minecraft.client.gui.components.Checkbox(centerX + 110, centerY, 120, 20, Component.translatable("gui." + Main.MODID + ".allow_copy"), entity.allowCopy, true);
                checkEdit = new net.minecraft.client.gui.components.Checkbox(centerX + 110, centerY + 20, 120, 20, Component.translatable("gui." + Main.MODID + ".allow_edit"), entity.allowEdit, true);
                checkRender = new net.minecraft.client.gui.components.Checkbox(centerX + 110, centerY + 40, 120, 20, Component.translatable("gui." + Main.MODID + ".force_render"), entity.forceRender, true);

                addRenderableWidget(checkCopy);
                addRenderableWidget(checkEdit);
                addRenderableWidget(checkRender);
            }

            Button btnCopyHash = new Button.Builder(Component.translatable("gui." + Main.MODID + ".button.copy_hash"), (b) -> {
                if (entity.modelHash != null && !entity.modelHash.isEmpty()) {
                    this.minecraft.keyboardHandler.setClipboard(entity.modelHash);
                    this.minecraft.player.displayClientMessage(Component.translatable("gui." + Main.MODID + ".hash_copied"), true);
                }
            }).pos(centerX - 105, centerY + 80).size(100, 20).build();
            btnCopyHash.active = canCopy && entity.modelHash != null && !entity.modelHash.isEmpty();
            addRenderableWidget(btnCopyHash);

            Button btnSave = new Button.Builder(Component.translatable("gui." + Main.MODID + ".button.save"), (b) -> {
                String input = this.textBox.getValue().trim();
                b.active = false; // Блокируем кнопку на время работы

                if (input.length() == 64 && input.matches("^[a-fA-F0-9]+$")) {
                    entity.path = "";
                    entity.modelHash = input;
                    ModMessages.SIMPLE.sendToServer(new SavePathPacket(entity.getBlockPos(), "", input));
                    System.out.println("[Modelle GUI] Использован готовый хэш: " + input);
                    b.active = true;
                } else {
                    // 🔥 Тяжёлая работа — в фоновый поток
                    CompletableFuture.supplyAsync(() -> ModelManager.convertAndSaveToWorld(input, entity.getLevel()))
                            .thenAcceptAsync(hash -> Minecraft.getInstance().execute(() -> {
                                b.active = true; // Разблокируем кнопку
                                if (!hash.isEmpty()) {
                                    entity.path = input;
                                    entity.modelHash = hash;
                                    ModMessages.SIMPLE.sendToServer(new SavePathPacket(entity.getBlockPos(), input, hash));
                                    System.out.println("[Modelle GUI] Модель сконвертирована и сохранена! Хэш: " + hash);
                                } else {
                                    System.out.println("[Modelle GUI] Ошибка: Папка '" + input + "' не найдена или не содержит model.obj");
                                }
                            }));
                }
            }).pos(centerX + 5, centerY + 80).size(100, 20).build();

            btnSave.active = canEdit;
            addRenderableWidget(btnSave);
        }

        @Override
        public void onClose() {
            ModelBlockEntity entity = this.menu.blockEntity;
            if (entity.isOwner(this.minecraft.player) && checkCopy != null) {
                ModMessages.SIMPLE.sendToServer(new UpdateModelSettingsPacket(
                        entity.getBlockPos(), checkCopy.selected(), checkEdit.selected(), checkRender.selected()
                ));
            }
            super.onClose();
        }

        private void sendUpdateValues() {
            ModMessages.SIMPLE.sendToServer(new UpdateModelValuesPacket(
                    this.menu.blockEntity.getBlockPos(), this.menu.blockEntity.raw,
                    this.menu.blockEntity.pitch, this.menu.blockEntity.roll, this.menu.blockEntity.scale
            ));
        }

        private void sendUpdatePos() {
            ModMessages.SIMPLE.sendToServer(new UpdateModelPosPacket(
                    this.menu.blockEntity.getBlockPos(), this.menu.blockEntity.posX,
                    this.menu.blockEntity.posY, this.menu.blockEntity.posZ
            ));
        }

        @Override
        public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
            this.renderBackground(guiGraphics);
            super.render(guiGraphics, mouseX, mouseY, partialTicks);

            int centerX = this.width / 2;
            int centerY = this.height / 2;
            ModelBlockEntity entity = this.menu.blockEntity;

            guiGraphics.drawCenteredString(this.font, "X: " + String.format("%.2f", entity.posX), centerX - 160, centerY - 34, 0xFFFFFF);
            guiGraphics.drawCenteredString(this.font, "Y: " + String.format("%.2f", entity.posY), centerX - 160, centerY + 6, 0xFFFFFF);
            guiGraphics.drawCenteredString(this.font, "Z: " + String.format("%.2f", entity.posZ), centerX - 160, centerY + 46, 0xFFFFFF);

            guiGraphics.drawCenteredString(this.font, "Yaw: " + String.format("%.1f", entity.raw), centerX, centerY - 34, 0xFFFFFF);
            guiGraphics.drawCenteredString(this.font, "Pitch: " + String.format("%.1f", entity.pitch), centerX, centerY + 6, 0xFFFFFF);
            guiGraphics.drawCenteredString(this.font, "Roll: " + String.format("%.1f", entity.roll), centerX, centerY + 46, 0xFFFFFF);

            Component scaleText = Component.translatable("gui." + Main.MODID + ".scale", String.format("%.1f", entity.scale));
            guiGraphics.drawCenteredString(this.font, scaleText, centerX + 160, centerY - 34, 0xFFFFFF);

            this.renderTooltip(guiGraphics, mouseX, mouseY);
        }

        @Override
        protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {}

        @ParametersAreNonnullByDefault
        @Override
        protected void renderBg(GuiGraphics p_283065_, float p_97788_, int p_97789_, int p_97790_) {}

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            InputConstants.Key key = InputConstants.getKey(keyCode, scanCode);
            if (this.textBox.isFocused() && this.minecraft.options.keyInventory.isActiveAndMatches(key)) {
                return false;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
    }

    static class ModelBlockRenderer implements BlockEntityRenderer<ModelBlockEntity>{

        @Override
        public boolean shouldRenderOffScreen(ModelBlockEntity entity) {
            return entity.forceRender;
        }

        @Override
        public int getViewDistance() {
            return 256;
        }

        @Override
        public void render(ModelBlockEntity entity, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int combinedLight, int combinedOverlay) {

            if (entity.isRemoved()) return;

            if ((entity.clientVBO == null || !entity.modelHash.equals(entity.lastPath)) && !entity.loadAttempted.get()) {
                entity.loadExternalModel();
            }

            VertexBuffer vbo = entity.clientVBO;
            if (vbo == null) {
                return;
            }

            CustomRenderTypes.MY_TRIANGLE_RENDER.setupRenderState();

            ResourceLocation texture = entity.clientTextureLoc != null ? entity.clientTextureLoc : MissingTextureAtlasSprite.getLocation();
            RenderSystem.setShaderTexture(0, texture);

            poseStack.pushPose();

            poseStack.translate(0.5f + entity.posX, 0.5f + entity.posY, 0.5f + entity.posZ);
            poseStack.scale(entity.scale, entity.scale, entity.scale);
            poseStack.mulPose(Axis.YP.rotationDegrees(entity.raw));
            poseStack.mulPose(Axis.XP.rotationDegrees(entity.pitch));
            poseStack.mulPose(Axis.ZP.rotationDegrees(entity.roll));

            Matrix4f modelViewMatrix = poseStack.last().pose();
            Matrix4f projectionMatrix = RenderSystem.getProjectionMatrix();

            vbo.bind();
            vbo.drawWithShader(modelViewMatrix, projectionMatrix, RenderSystem.getShader());
            VertexBuffer.unbind();

            poseStack.popPose();

            CustomRenderTypes.MY_TRIANGLE_RENDER.clearRenderState();
        }
    }
}
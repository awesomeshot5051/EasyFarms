package com.awesomeshot5051.easyfarms.farm;

import com.awesomeshot5051.easyfarms.EasyFarms;
import com.awesomeshot5051.easyfarms.gui.MenuOpener;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import com.awesomeshot5051.easyfarms.gui.FarmMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** One class for the three farm blocks; the kind decides which machine recipes apply. */
public class FarmBlock extends Block implements EntityBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    // Hollow glass-case outline (same collision as the old farms): six 1-pixel walls.
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(0, 0, 0, 16, 1, 16), Block.box(0, 15, 0, 16, 16, 16),
            Block.box(0, 0, 0, 1, 16, 16), Block.box(15, 0, 0, 16, 16, 16),
            Block.box(0, 0, 0, 16, 16, 1), Block.box(0, 0, 15, 16, 16, 16));

    public static final EnumProperty<FarmFloor> FLOOR = EnumProperty.create("floor", FarmFloor.class);

    private final FarmKind kind;

    public FarmBlock(FarmKind kind, Properties properties) {
        super(properties.mapColor(MapColor.METAL).strength(2.5F).sound(SoundType.METAL).noOcclusion());
        this.kind = kind;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(FLOOR, FarmFloor.defaultFor(kind)));
    }

    public FarmKind kind() {
        return kind;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FLOOR);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1F;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FarmBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != EasyFarms.FARM_BE.get()) return null;
        return (l, p, s, be) -> ((FarmBlockEntity) be).tick();
    }

    /** Upgrade card in hand: install it. Dye in hand: colour the farm (sheep wool, concrete...). Anything else: open the GUI. */
    @Override
    protected InteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos, Player player,
                                         InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof FarmBlockEntity farm)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        boolean isCard = com.awesomeshot5051.easyfarms.tier.FarmUpgrade.isCard(level.registryAccess(), held);
        if (isCard) {
            if (!level.isClientSide()) {
                if (farm.installCard(held)) {
                    if (!player.getAbilities().instabuild) held.shrink(1);
                    player.sendOverlayMessage(Component.translatable("message.easy_farms.upgrade_installed", held.getHoverName()));
                } else {
                    Component reason = farm.installProblem(held, -1);
                    player.sendOverlayMessage(reason != null
                            ? Component.translatable("message.easy_farms.upgrade_blocked", held.getHoverName(), reason)
                            : Component.translatable("message.easy_farms.upgrade_rejected", held.getHoverName()));
                }
            }
            return InteractionResult.SUCCESS;
        }
        DyeColor dye = held.get(DataComponents.DYE);
        if (dye != null) {
            if (!farm.canDye()) return InteractionResult.TRY_WITH_EMPTY_HAND;
            if (!level.isClientSide()) {
                farm.setColor(dye);
                if (!player.getAbilities().instabuild) held.shrink(1);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof FarmBlockEntity farm && player instanceof ServerPlayer server) {
            MenuOpener.openFarm(server, pos, farm, Component.translatable(getDescriptionId()));
        }
        return InteractionResult.SUCCESS;
    }

    /** Silk Touch picks the farm up with everything inside it; any other break spills the contents (see onRemove). */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (level.getBlockEntity(pos) instanceof FarmBlockEntity farm) {
            var silk = level.registryAccess().getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH);
            farm.setKeepContents(net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(silk, player.getMainHandItem()) > 0);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }
}

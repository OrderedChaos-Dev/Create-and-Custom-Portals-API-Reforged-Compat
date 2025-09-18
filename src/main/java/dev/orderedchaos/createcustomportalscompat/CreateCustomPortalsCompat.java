package dev.orderedchaos.createcustomportalscompat;

import com.mojang.logging.LogUtils;
import com.simibubi.create.api.contraption.train.PortalTrackProvider;
import com.simibubi.create.content.contraptions.glue.SuperGlueEntity;
import net.createmod.catnip.math.BlockFace;
import net.kyrptonaught.customportalapi.CustomPortalApiRegistry;
import net.kyrptonaught.customportalapi.CustomPortalBlock;
import net.kyrptonaught.customportalapi.CustomPortalsMod;
import net.kyrptonaught.customportalapi.util.CustomPortalHelper;
import net.kyrptonaught.customportalapi.util.CustomTeleporter;
import net.kyrptonaught.customportalapi.util.PortalLink;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(CreateCustomPortalsCompat.MODID)
public class CreateCustomPortalsCompat {

    public static final String MODID = "createcustomportalscompat";
    private static final Logger LOGGER = LogUtils.getLogger();

    public CreateCustomPortalsCompat(FMLJavaModLoadingContext context) {
        context.getModEventBus().addListener(this::commonSetup);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
//            CustomPortalBuilder
//              .beginPortal()
//              .frameBlock(Blocks.GLOWSTONE)
//              .lightWithItem(Items.APPLE)
//              .destDimID(new ResourceLocation("the_nether"))
//              .tintColor(100, 150, 200)
//              .registerPortal();
//
//            CustomPortalBuilder
//              .beginPortal()
//              .frameBlock(Blocks.AMETHYST_BLOCK)
//              .lightWithItem(Items.APPLE)
//              .destDimID(new ResourceLocation("blue_skies", "everbright"))
//              .tintColor(100, 150, 200)
//              .registerPortal();

            Block customPortal = CustomPortalsMod.getDefaultPortalBlock();
            PortalTrackProvider.REGISTRY.register(customPortal, CreateCustomPortalsCompat::customPortal);
        });
    }

    private static PortalTrackProvider.Exit customPortal(ServerLevel level, BlockFace inboundTrack) {
        ResourceKey<Level> overworld = Level.OVERWORLD;

        BlockPos portalPos = inboundTrack.getConnectedPos();
        Block frameBlock = CustomPortalHelper.getPortalBase(level, portalPos);
        PortalLink link = CustomPortalApiRegistry.getPortalLinkFromBase(frameBlock);
        ResourceKey<Level> destKey = CustomPortalsMod.dims.get(link.dimID);
        LOGGER.debug("Found frame block {} from portal at {} - portal goes to dimension {}", frameBlock.getName(), portalPos, destKey);

        ResourceKey<Level> resourceKey = level.dimension() == destKey ? overworld : destKey;

        MinecraftServer minecraftServer = level.getServer();
        ServerLevel otherLevel = minecraftServer.getLevel(resourceKey);

        if (otherLevel == null)
            return null;

        BlockState portalState = level.getBlockState(portalPos);

        // Modified from PortalTrackProvider.fromProbe
        SuperGlueEntity probe = new SuperGlueEntity(level, new AABB(portalPos));
        probe.setYRot(inboundTrack.getFace().toYRot());
        probe.setPortalEntrancePos();

        PortalInfo portalInfo = CustomTeleporter.customTPTarget(otherLevel, probe, portalPos, frameBlock, link.getFrameTester());
        if (portalInfo == null)
            return null;

        BlockPos otherPortalPos = BlockPos.containing(portalInfo.pos);
        BlockState otherPortalState = otherLevel.getBlockState(otherPortalPos);
        if (!otherPortalState.is(portalState.getBlock()))
            return null;

        Direction targetDirection = inboundTrack.getFace();
        if (targetDirection.getAxis() == otherPortalState.getValue(CustomPortalBlock.AXIS))
            targetDirection = targetDirection.getClockWise();
        BlockPos otherPos = otherPortalPos.relative(targetDirection);

        return new PortalTrackProvider.Exit(otherLevel, new BlockFace(otherPos, targetDirection.getOpposite()));
    }
}

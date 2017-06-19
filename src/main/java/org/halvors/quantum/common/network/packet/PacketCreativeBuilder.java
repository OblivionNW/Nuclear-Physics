package org.halvors.quantum.common.network.packet;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import org.halvors.quantum.common.block.debug.BlockCreativeBuilder;
import org.halvors.quantum.common.network.NetworkHandler;
import org.halvors.quantum.common.transform.vector.Vector3;
import org.halvors.quantum.common.utility.location.Location;
import org.halvors.quantum.lib.type.Pair;

import java.util.HashMap;
import java.util.Map;

public class PacketCreativeBuilder extends PacketLocation implements IMessage {
    public int schematicId;
    public int size;

    public PacketCreativeBuilder() {

    }

    public PacketCreativeBuilder(Location location, int schematicId, int size) {
        super(location);

        this.schematicId = schematicId;
        this.size = size;
    }

    @Override
    public void fromBytes(ByteBuf dataStream) {
        super.fromBytes(dataStream);

        schematicId = dataStream.readInt();
        size = dataStream.readInt();
    }

    @Override
    public void toBytes(ByteBuf dataStream) {
        super.toBytes(dataStream);

        dataStream.writeInt(schematicId);
        dataStream.writeInt(size);
    }

    public static class PacketCreativeBuilderMessage implements IMessageHandler<PacketCreativeBuilder, IMessage> {
        @Override
        public IMessage onMessage(PacketCreativeBuilder message, MessageContext messageContext) {
            Location location = message.getLocation();
            World world = NetworkHandler.getWorld(messageContext);

            if (!world.isRemote) {
                // TODO: Only allow operators.

                try {
                    Vector3 position = new Vector3(location.getX(), location.getY(), location.getZ());

                    if (message.size > 0) {
                        HashMap<Vector3, Pair<Block, Integer>> map = BlockCreativeBuilder.getSchematic(message.schematicId).getStructure(ForgeDirection.getOrientation(position.getBlockMetadata(world)), message.size);

                        for (Map.Entry<Vector3, Pair<Block, Integer>> entry : map.entrySet()) {
                            Vector3 placePos = entry.getKey().clone();
                            placePos.translate(position);
                            placePos.setBlock(world, entry.getValue().getLeft(), entry.getValue().getRight());
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            return null;
        }
    }
}
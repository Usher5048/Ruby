package ruby.mixin;

// ClientConnectionMixin is no longer needed for silent aim — the packet is
// fabricated in ClientPlayerEntityMixin before it ever reaches the connection.
// This file is kept as a placeholder in case you have other packet hooks here.
// If you have nothing else in this mixin, you can delete this file and remove
// the entry from your mixin config.

import net.minecraft.network.ClientConnection;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ClientConnection.class)
public class ClientConnectionMixin {
    // intentionally empty
}
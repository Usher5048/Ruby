package ruby.mixin;

import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.services.ProfileResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.gui.screens.social.PlayerSocialManager;
import net.minecraft.client.multiplayer.ProfileKeyPairManager;
import net.minecraft.client.multiplayer.chat.report.ReportingContext;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.server.Services;

@Mixin(Minecraft.class)
public interface MinecraftClientSessionAccessor {
    @Mutable
    @Accessor("user")
    void ruby$setSession(User session);

    @Mutable
    @Accessor("userApiService")
    void ruby$setUserApiService(UserApiService service);

    @Mutable
    @Accessor("playerSocialManager")
    void ruby$setSocialInteractionsManager(PlayerSocialManager manager);

    @Mutable
    @Accessor("profileKeyPairManager")
    void ruby$setProfileKeys(ProfileKeyPairManager keys);

    @Mutable
    @Accessor("reportingContext")
    void ruby$setAbuseReportContext(ReportingContext context);

    @Mutable
    @Accessor("profileFuture")
    void ruby$setGameProfileFuture(CompletableFuture<ProfileResult> future);

    @Mutable
    @Accessor("services")
    void ruby$setApiServices(Services services);

    @Mutable
    @Accessor("skinManager")
    void ruby$setSkinProvider(SkinManager manager);
}

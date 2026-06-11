package ruby.mixin;

import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.yggdrasil.ProfileResult;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.SocialInteractionsManager;
import net.minecraft.client.session.ProfileKeys;
import net.minecraft.client.session.Session;
import net.minecraft.client.session.report.AbuseReportContext;
import net.minecraft.client.texture.PlayerSkinProvider;
import net.minecraft.util.ApiServices;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.concurrent.CompletableFuture;

@Mixin(MinecraftClient.class)
public interface MinecraftClientSessionAccessor {
    @Mutable
    @Accessor("session")
    void ruby$setSession(Session session);

    @Mutable
    @Accessor("userApiService")
    void ruby$setUserApiService(UserApiService service);

    @Mutable
    @Accessor("socialInteractionsManager")
    void ruby$setSocialInteractionsManager(SocialInteractionsManager manager);

    @Mutable
    @Accessor("profileKeys")
    void ruby$setProfileKeys(ProfileKeys keys);

    @Mutable
    @Accessor("abuseReportContext")
    void ruby$setAbuseReportContext(AbuseReportContext context);

    @Mutable
    @Accessor("gameProfileFuture")
    void ruby$setGameProfileFuture(CompletableFuture<ProfileResult> future);

    @Mutable
    @Accessor("apiServices")
    void ruby$setApiServices(ApiServices services);

    @Mutable
    @Accessor("skinProvider")
    void ruby$setSkinProvider(PlayerSkinProvider provider);
}

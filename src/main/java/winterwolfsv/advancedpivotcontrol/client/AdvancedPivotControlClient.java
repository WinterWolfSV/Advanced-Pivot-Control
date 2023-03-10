package winterwolfsv.advancedpivotcontrol.client;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigManager;
import winterwolfsv.config.Config;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import org.lwjgl.glfw.GLFW;

import java.util.concurrent.atomic.AtomicBoolean;

import static winterwolfsv.advancedpivotcontrol.client.Commands.sendCommandFeedback;


@Environment(EnvType.CLIENT)
public class AdvancedPivotControlClient implements ClientModInitializer {
    public static ConfigManager configManager;
    public static final String MOD_ID = "advanced_pivot_control";
    public static float currentYaw;
    public static float currentPitch;


    private static final KeyBinding yawRight = KeyBindingHelper.registerKeyBinding(new KeyBinding("Turn right", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT, "Pitch and Yaw Control"));
    private static final KeyBinding yawLeft = KeyBindingHelper.registerKeyBinding(new KeyBinding("Turn left", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_LEFT, "Pitch and Yaw Control"));
    private static final KeyBinding pitchUp = KeyBindingHelper.registerKeyBinding(new KeyBinding("Look up", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UP, "Pitch and Yaw Control"));
    private static final KeyBinding pitchDown = KeyBindingHelper.registerKeyBinding(new KeyBinding("Look down", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_DOWN, "Pitch and Yaw Control"));
    private static final KeyBinding lockYaw = KeyBindingHelper.registerKeyBinding(new KeyBinding("Lock yaw", InputUtil.Type.KEYSYM, -1, "Pitch and Yaw Control"));
    private static final KeyBinding lockPitch = KeyBindingHelper.registerKeyBinding(new KeyBinding("Lock pitch", InputUtil.Type.KEYSYM, -1, "Pitch and Yaw Control"));

    private static int turnYaw(int direction, int degrees) {
        PlayerEntity player = MinecraftClient.getInstance().player;
        assert player != null;
        if (player.getYaw() % degrees != 0) {
            if (direction < 0) {
                player.setYaw(player.getYaw() - degrees - player.getYaw() % degrees);
            } else {
                player.setYaw(player.getYaw() - player.getYaw() % degrees);
            }
        } else {
            player.setYaw(player.getYaw() + direction * degrees);
        }

        // Some magic I wrote at 3am to make sure the yaw is between -180 and 180. I don't know how exactly it does it, but it works and I won't touch it from now on.
        float yaw = player.getYaw();
        yaw = yaw % 360;
        if (yaw > 180) {
            yaw -= 360;
        } else if (yaw < -180) {
            yaw += 360;
        }
        currentYaw = yaw;
        return (int) yaw;

    }

    private static int turnPitch(int direction, int degrees) {
        PlayerEntity player = MinecraftClient.getInstance().player;
        assert player != null;
        if (player.getPitch() % degrees != 0) {
            if (direction < 0) {
                player.setPitch(player.getPitch() - player.getPitch() % degrees);
            } else {
                player.setPitch(player.getPitch() + degrees - player.getPitch() % degrees);
            }
        } else {
            player.setPitch(player.getPitch() + direction * degrees);
        }
        currentPitch = player.getPitch();
        return (int) Math.min(90, Math.max(-90, player.getPitch()));
    }

    @Override
    public void onInitializeClient() {
        configManager = (ConfigManager) AutoConfig.register(Config.class, GsonConfigSerializer::new);
        Config config = AutoConfig.getConfigHolder(Config.class).getConfig();
        lockView();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (yawRight.wasPressed()) {
                sendCommandFeedback("Rotating yaw " + config.yawSteps + "?? to your right. New yaw: " + turnYaw(1, config.yawSteps));
            }
            while (yawLeft.wasPressed()) {
                sendCommandFeedback("Rotating yaw " + config.yawSteps + "?? to your left. New yaw: " + turnYaw(-1, config.yawSteps));
            }
            while (pitchUp.wasPressed()) {
                sendCommandFeedback("Rotating pitch " + config.pitchSteps + "?? upwards. New pitch: " + turnPitch(-1, config.pitchSteps));
            }
            while (pitchDown.wasPressed()) {
                sendCommandFeedback("Rotating pitch " + config.pitchSteps + "?? downwards. New pitch: " + turnPitch(1, config.pitchSteps));
            }
            while (lockYaw.wasPressed()) {
                config.lockYaw = !config.lockYaw;
                configManager.save();
                sendCommandFeedback("Yaw lock " + (config.lockYaw ? "enabled" : "disabled"));

            }
            while (lockPitch.wasPressed()) {
                config.lockPitch = !config.lockPitch;
                configManager.save();
                sendCommandFeedback("Pitch lock " + (config.lockPitch ? "enabled" : "disabled"));

            }
        });
    }

    private void lockView() {
        AtomicBoolean oldLockYaw = new AtomicBoolean(false);
        AtomicBoolean oldLockPitch = new AtomicBoolean(false);
        Config config = AutoConfig.getConfigHolder(Config.class).getConfig();
        ClientTickEvents.END_WORLD_TICK.register(client -> {
            PlayerEntity player = MinecraftClient.getInstance().player;
            if (oldLockYaw.get() != config.lockYaw || oldLockPitch.get() != config.lockPitch) {
                if (player != null) {
                    currentYaw = player.getYaw();
                    currentPitch = player.getPitch();
                }
            }
            oldLockYaw.set(config.lockYaw);
            oldLockPitch.set(config.lockPitch);
            if (config.lockYaw && player != null && player.getYaw() != currentYaw) {
                player.setYaw(currentYaw);
            }
            if (config.lockPitch && player != null && player.getPitch() != currentPitch) {
                player.setPitch(currentPitch);
            }
        });
    }
}

// TODO
// - Implement 4040 trick
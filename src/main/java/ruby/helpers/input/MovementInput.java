package ruby.helpers.input;

import net.minecraft.client.player.ClientInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;

public class MovementInput extends ClientInput {
    @Override
    public void tick() {
        this.moveVector = new Vec2(
                this.keyPresses.left() == this.keyPresses.right() ? 0f : (this.keyPresses.left() ? 1f : -1f),
                this.keyPresses.forward() == this.keyPresses.backward() ? 0f : (this.keyPresses.forward() ? 1f : -1f)
        );
    }

    public void stop() {
        this.keyPresses = Input.EMPTY;
    }
    @Override public boolean hasForwardImpulse() {
        return this.moveVector.y > 0.001;
    }

    public boolean forward() {
        return this.keyPresses.forward();
    }
    public void forward(boolean enabled) {
        this.keyPresses = new Input(
                enabled,
                this.backward(),
                this.left(),
                this.right(),
                this.isJump(),
                this.sneak(),
                this.sprint()
        );
    }

    public boolean backward() {
        return this.keyPresses.backward();
    }
    public void backward(boolean enabled) {
        this.keyPresses = new Input(
                this.forward(),
                enabled,
                this.left(),
                this.right(),
                this.isJump(),
                this.sneak(),
                this.sprint()
        );
    }

    public boolean left() {
        return this.keyPresses.left();
    }
    public void left(boolean enabled) {
        this.keyPresses = new Input(
                this.forward(),
                this.backward(),
                enabled,
                this.right(),
                this.isJump(),
                this.sneak(),
                this.sprint()
        );
    }

    public boolean right() {
        return this.keyPresses.right();
    }
    public void right(boolean enabled) {
        this.keyPresses = new Input(
                this.forward(),
                this.backward(),
                this.left(),
                enabled,
                this.isJump(),
                this.sneak(),
                this.sprint()
        );
    }

    public boolean isJump() {
        return this.keyPresses.jump();
    }
    public void jump(boolean enabled) {
        this.keyPresses = new Input(
                this.forward(),
                this.backward(),
                this.left(),
                this.right(),
                enabled,
                this.sneak(),
                this.sprint()
        );
    }

    public boolean sneak() {
        return this.keyPresses.shift();
    }
    public void sneak(boolean enabled) {
        this.keyPresses = new Input(
                this.forward(),
                this.backward(),
                this.left(),
                this.right(),
                this.isJump(),
                enabled,
                this.sprint()
        );
    }

    public boolean sprint() {
        return this.keyPresses.sprint();
    }
    public void sprint(boolean enabled) {
        this.keyPresses = new Input(
                this.forward(),
                this.backward(),
                this.left(),
                this.right(),
                this.isJump(),
                this.sneak(),
                enabled
        );
    }
}

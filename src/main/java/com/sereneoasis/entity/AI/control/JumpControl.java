//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.sereneoasis.entity.AI.control;


import com.sereneoasis.entity.HumanEntity;
import net.minecraft.world.entity.ai.control.Control;
import org.bukkit.event.entity.EntityExhaustionEvent;

public class JumpControl implements Control {
    private final HumanEntity mob;
    protected boolean jump;

    public JumpControl(HumanEntity entity) {
        this.mob = entity;
    }

    public void jump() {
        this.jump = true;
        if (mob.isSprinting()) {
            mob.causeFoodExhaustion(mob.level().spigotConfig.jumpSprintExhaustion, EntityExhaustionEvent.ExhaustionReason.JUMP_SPRINT);
        } else {
            mob.causeFoodExhaustion(mob.level().spigotConfig.jumpWalkExhaustion, EntityExhaustionEvent.ExhaustionReason.JUMP);
        }
    }

    public void tick() {
        this.mob.setJumping(this.jump);
        this.jump = false;
    }
}

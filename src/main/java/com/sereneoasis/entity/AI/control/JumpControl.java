//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.sereneoasis.entity.AI.control;


import com.sereneoasis.entity.HumanEntity;
import net.minecraft.world.entity.ai.control.Control;

public class JumpControl implements Control {
    private final HumanEntity mob;
    protected boolean jump;

    public JumpControl(HumanEntity entity) {
        this.mob = entity;
    }

    public void jump() {
        this.jump = true;
    }

    public void tick() {
        this.mob.setJumping(this.jump);
        this.jump = false;
    }
}

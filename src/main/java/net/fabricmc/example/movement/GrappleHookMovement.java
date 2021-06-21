package net.fabricmc.example.movement;

import net.fabricmc.example.items.GrappleHookItem;
import net.fabricmc.example.mixin.LivingEntityAcc;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class GrappleHookMovement extends Movement {

    public Vec3d target;

    public boolean hasNotJumped = false;

    public GrappleHookMovement(Vec3d hit) {
        this.target = hit;
    }

    @Override
    public Movement travel(LivingEntity entity, Vec3d input, boolean jumping) {
        if (jumping && hasNotJumped) {
            grappleJump(entity);
            entity.playSound(SoundEvents.BLOCK_CHAIN_BREAK, 1.0f, 1.0f);

            var movement = new AirStrafeMovement(0.05);
            movement.travel(entity, input, true);
            return movement;
        }
        if (!jumping) {
            hasNotJumped = true;
        }

        Vec3d delta = target.subtract(entity.getPos().add(0, entity.getHeight() / 2, 0));
//                if (true) {
//                    Vec3d dir = delta.normalize();
//
//                    Vec3d vel = dir.multiply(14f * 1/20);
//                    this.move(MovementType.SELF, vel);
//                    this.setVelocity(vel);
//
//                    ci.cancel();
//                    return;
//                }

        if (GrappleHookItem.mode == GrappleHookItem.Mode.DOT_3D) {
            if (entity instanceof PlayerEntity player) {
                player.getAbilities().flying = false;
            }
//					Vec3d dir = delta.normalize();
//
//					double damping = 0.94;
//					double gravity = -0.08 * 0.1 * 0;
//					//.multiply(damping).add(0, gravity, 0)
////                        if (world.isClient) {
////                            System.out.println("~~~~~~~~~~~~~~~~~~~~");
////                            System.out.println(getVelocity());
////                            System.out.println(getVelocity().multiply(damping));
////                            System.out.println(getVelocity().normalize().multiply(getVelocity().length() * damping));
////                        }
//
//					Vec3d velIn = getVelocity().multiply(damping).add(0, gravity, 0);
//
//					float wishSpeed = (float) Math.min(14f * 1 / 20, delta.length());
//
//					Vec3d velOut = Accelerate3D(velIn, dir, wishSpeed, 8.f * 1 / 20);
//
//					this.move(MovementType.SELF, velOut);
//
//					this.setVelocity(velOut);

            Vec3d dir = delta.normalize();
            Vec3d addVel;
//            double damping = 0.8;
            double damping = 0.8;
            if (delta.length() > 1) {
                Vec3d wishDir = inputToWishDir(input, entity.getYaw()); //TODO tilt wish dir
                dir = wishDir.add(dir.multiply(1.5)).normalize();

                addVel = dir.multiply(0.05f * 3);
//                addVel = dir.multiply(0.05f * 3); <-fun :)
//                addVel = dir.multiply(0.05f * 2);
            } else {
                addVel = delta;
                damping = 0.25;
            }

            Vec3d vel = entity.getVelocity();
            vel = vel.multiply(damping);
            vel = vel.add(addVel);

            entity.setVelocity(vel);
            entity.fallDistance = 0;
            entity.move(net.minecraft.entity.MovementType.SELF, vel);

        } else if (GrappleHookItem.mode == GrappleHookItem.Mode.DOT_2D) {
            Vec3d xzDir = delta.multiply(1, 0, 1).normalize();
            Vec3d xzVel = Accelerate3D(entity.getVelocity().multiply(1, 0, 1), xzDir, 12, 5);
            entity.setVelocity(xzVel.add(0, MathHelper.clamp(delta.y, -0.4f, 0.4f), 0));
            entity.move(net.minecraft.entity.MovementType.SELF, entity.getVelocity());
        }

        return this;
    }

    void grappleJump(LivingEntity entity) {
        float f = ((LivingEntityAcc) entity).callGetJumpVelocity() * 1.25f;
        if (entity.hasStatusEffect(StatusEffects.JUMP_BOOST)) {
            f += 0.1F * (float) (entity.getStatusEffect(StatusEffects.JUMP_BOOST).getAmplifier() + 1);
        }

        Vec3d vec3d = entity.getVelocity();
        entity.setVelocity(vec3d.x, f, vec3d.z);
    }

    private Vec3d Accelerate3D(Vec3d velocity, Vec3d wish_dir, double wish_speedIn, double accelIn) {
        double wish_speed = wish_speedIn;
        double accel = accelIn;

        double current_speed = velocity.dotProduct(wish_dir);
        double add_speed = wish_speed - current_speed;

        if (add_speed <= 0) {
            return velocity;
        }

        /*m_surfaceFriction*/
        double accel_speed = Math.min(
                wish_speed * accel,
                add_speed);

        return velocity.add(wish_dir.multiply(accel_speed));
    }

    private Vec3d Accelerate2D(Vec3d velocity, Vec3d wish_dir, double wish_speed, double accel) {
        double current_speed = velocity.dotProduct(wish_dir);
        double add_speed = wish_speed - current_speed;

        if (add_speed <= 0) {
            return velocity;
        }

        /*m_surfaceFriction*/
        double accel_speed = Math.min(
                wish_speed * (accel * 1 / 20),
                add_speed);

        return velocity.add(wish_dir.multiply(accel_speed, 0, accel_speed));
    }
}

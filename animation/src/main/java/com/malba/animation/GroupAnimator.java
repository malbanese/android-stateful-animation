package com.malba.animation;
import android.animation.Animator;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewPropertyAnimator;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;
import java.util.TreeSet;

/**
 * Created by malba on 3/7/2018.
 */

public class GroupAnimator {
    // The root ViewGroup to play the animation on.
    private ViewGroup mRootGroup;

    /**
     * Checks if the child is a descendant of the root view group.
     * @param child The child to check against the root view group.
     * @return True if the child is a descendant of the root view group.
     */
    private boolean isChildDescendant(View child) {
        if(child == mRootGroup) {
            return true;
        }

        ViewParent parent;
        while((parent = child.getParent()) != null) {
            if(parent == mRootGroup) {
                return true;
            }
        }

        return false;
    }

    static final int TRANSLATION_X  = 0x0001;
    static final int TRANSLATION_Y  = 0x0002;
    static final int TRANSLATION_Z  = 0x0004;
    static final int SCALE_X        = 0x0008;
    static final int SCALE_Y        = 0x0010;
    static final int ROTATION       = 0x0020;
    static final int ROTATION_X     = 0x0040;
    static final int ROTATION_Y     = 0x0080;
    static final int X              = 0x0100;
    static final int Y              = 0x0200;
    static final int Z              = 0x0400;
    static final int ALPHA          = 0x0800;

    // Hash map to keep track of the animation states of various views.
    private HashMap<View, TreeSet<AnimationValue>> mAnimatorMap = new HashMap<>();

    // List of active animators
    // TODO: Clear when animation is finished.
    private LinkedList<ViewPropertyAnimator> mActiveAnimators = new LinkedList<>();

    // The target view to run the next animation commands on.
    private View mTarget;

    // The default duration to be used, unless a duration is specified
    private int mDefaultDuration = 500;

    // The default delay to be used, unless a delay is specified.
    private int mDefaultDelay = 0;

    /**
     * Registers a property for animation
     * @param v The view being animated.
     * @param property The property being animated.
     * @param value The amount the property being animated to.
     * @param duration The duration being animated for.
     * @param startDelay The start delay before the animation starts.
     */
    private void animateProperty(View v, int property, float value, int duration, int startDelay) {
        TreeSet<AnimationValue> set = mAnimatorMap.get(v);

        if(set == null) {
            set = createAnimationStateSet();
            mAnimatorMap.put(v, set);
        }

        System.out.println("Adding property");
        set.add( new AnimationValue(property, value, duration, startDelay) );
    }

    /**
     * Creates the tree set to store AnimationStates with. This will keep the AnimationStates in
     * the correct order for processing later.
     * @return A new TreeSet instance.
     */
    private TreeSet<AnimationValue> createAnimationStateSet() {
        return new TreeSet<>(new Comparator<AnimationValue>() {
            @Override
            public int compare(AnimationValue s1, AnimationValue s2) {
                return s1.compareTo(s2);
            }
        });
    }

    /**
     * Starts the provided animations, given a view.
     * @param view The view to animate on.
     * @param animations The animations defined upon the view.
     */
    private void startAnimations(View view, TreeSet<AnimationValue> animations) {
        AnimationValue prevValue = null;
        ViewPropertyAnimator animator = view.animate();

        System.out.println("Animating property count " + animations.size());

        for (AnimationValue value : animations) {
            // If the value is not null, and the start delay is not the current delay OR
            // the duration is not the current duration... then start the previous animation, and
            // set up this one.
            if (prevValue != null && (prevValue.mStartDelay != value.mStartDelay || prevValue.mDuration != value.mDuration)) {
                System.out.println("Starting animation batch");
                animator.start();
            }

            System.out.println("Animating value " + value.mProperty + " " + value.mStartDelay + " " + value.mDuration);
            animateValue(animator, value);
            prevValue = value;
        }

        // Start the final piece of the animation, so long as we had something animated.
        if(prevValue != null) {
            System.out.println("Starting animation batch");
            mActiveAnimators.add(animator);
            animator.start();
        }
    }

    /**
     * Animates the correct property with a value.
     * @param animator The animator to use.
     * @param value The AnimationValue instance to animate with.
     */
    private void animateValue(ViewPropertyAnimator animator, AnimationValue value) {
        animator.setDuration(value.mDuration);
        animator.setStartDelay(value.mStartDelay);

        switch (value.mProperty) {
            case TRANSLATION_X:
                animator.translationX(value.mValue);
                break;
            case TRANSLATION_Y:
                animator.translationY(value.mValue);
                break;
            case TRANSLATION_Z:
                animator.translationZ(value.mValue);
                break;
            case SCALE_X:
                animator.scaleX(value.mValue);
                break;
            case SCALE_Y:
                animator.scaleY(value.mValue);
                break;
            case ROTATION:
                animator.rotation(value.mValue);
                break;
            case ROTATION_X:
                animator.rotationX(value.mValue);
                break;
            case ROTATION_Y:
                animator.rotationY(value.mValue);
                break;
            case X:
                animator.x(value.mValue);
                break;
            case Y:
                animator.y(value.mValue);
                break;
            case Z:
                animator.z(value.mValue);
                break;
            case ALPHA:
                animator.alpha(value.mValue);
                break;
        }
    }

    /**
     * Internal animation value class to keep track of a specific animation.
     */
    private class AnimationValue implements Comparable<AnimationValue> {
        final int mProperty;
        final int mDuration;
        final int mStartDelay;
        final float mValue;

        /**
         * @param property The property being animated.
         * @param value The value being animated to.
         * @param duration The duration the animation will last.
         * @param startDelay The start delay of the animation.--
         */
        public AnimationValue(int property, float value, int duration, int startDelay) {
            mProperty = property;
            mValue = value;
            mDuration = duration;
            mStartDelay = startDelay;
        }

        /**
         * @return Gets the total duration of this animation.
         */
        private int getTotalDuration() {
            return mStartDelay + mDuration;
        }

        @Override
        public int compareTo(@NonNull AnimationValue other) {
            // Build the arrays with the ordered properties to compare.
            int[] thisCompare = {this.getTotalDuration(), this.mStartDelay, this.mDuration, this.mProperty};
            int[] otherCompare = {other.getTotalDuration(), other.mStartDelay, other.mDuration, other.mProperty};

            // Compare against all properties.
            for(int i=0; i < thisCompare.length; i++) {
                final int compareValue = Integer.compare(thisCompare[i], otherCompare[i]);
                if (compareValue != 0) {
                    return compareValue;
                }
            }

            // Items are completely equal.
            return 0;
        }
    }

    /**
     * Starts the animation sequence.
     */
    public void start() {
        Set<View> views = mAnimatorMap.keySet();
        for(View v : views) {
            startAnimations(v, mAnimatorMap.get(v));
        }
    }

    /**
     * Cancels the currently playing animation sequence.
     */
    public void cancel() {
        for(ViewPropertyAnimator animator : mActiveAnimators) {
            animator.cancel();
        }

        mActiveAnimators.clear();
    }

    /**
     * Sets the default timings for the animation.
     * @param duration Default animation duration.
     * @param delay Default animation delay.
     * @return This AnimationState instance.
     */
    public GroupAnimator usingTiming(int duration, int delay) {
        mDefaultDuration = duration;
        mDefaultDelay = delay;
        return this;
    }

    /**
     * Sets the default animation duration, to be used is a duration is not provided.
     * @param duration Default animation duration.
     * @return This AnimationState instance.
     */
    public GroupAnimator usingDuration(int duration) {
        mDefaultDuration = duration;
        return this;
    }

    /**
     * Sets the default animation delay, to be used is a delay is not provided.
     * @param delay Default animation delay.
     * @return This AnimationState instance.
     */
    public GroupAnimator withDelay(int delay) {
        mDefaultDelay = delay;
        return this;
    }

    /**
     * Sets a view to be the target of the commands following this.
     * @param view The view to animate.
     * @return This AnimationState instance.
     */
    public GroupAnimator usingTarget(View view) {
        mTarget = view;
        return this;
    }

    /**
     * Animates a view's x translation.
     * @param value The value to animate to.
     * @param duration The duration to animate for.
     * @param startDelay The animation start delay.
     * @return This AnimationState instance.
     */
    public GroupAnimator translationX(float value, int duration, int startDelay) {
        animateProperty(mTarget, TRANSLATION_X, value, duration, startDelay);
        return this;
    }

    /**
     * Animates a view's x translation.
     * @param value The value to animate to.
     * @param duration The duration to animate for.
     * @return This AnimationState instance.
     */
    public GroupAnimator translationX(float value, int duration) {
        translationX(value, duration, mDefaultDelay);
        return this;
    }

    /**
     * Animates a view's x translation.
     * @param value The value to animate to.
     * @return This AnimationState instance.
     */
    public GroupAnimator translationX(float value) {
        translationX(value, mDefaultDuration, mDefaultDelay);
        return this;
    }

    /**
     * Animates a view's y translation.
     * @param value The value to animate to.
     * @param duration The duration to animate for.
     * @param startDelay The animation start delay.
     * @return This AnimationState instance.
     */
    public GroupAnimator translationY(float value, int duration, int startDelay) {
        animateProperty(mTarget, TRANSLATION_Y, value, duration, startDelay);
        return this;
    }

    /**
     * Animates a view's y translation.
     * @param value The value to animate to.
     * @param duration The duration to animate for.
     * @return This AnimationState instance.
     */
    public GroupAnimator translationY(float value, int duration) {
        translationY(value, duration, mDefaultDelay);
        return this;
    }

    /**
     * Animates a view's y translation.
     * @param value The value to animate to.
     * @return This AnimationState instance.
     */
    public GroupAnimator translationY(float value) {
        translationY(value, mDefaultDuration, mDefaultDelay);
        return this;
    }

    /**
     * Animates a view's z translation.
     * @param value The value to animate to.
     * @param duration The duration to animate for.
     * @param startDelay The animation start delay.
     * @return This AnimationState instance.
     */
    public GroupAnimator translationZ(float value, int duration, int startDelay) {
        animateProperty(mTarget, TRANSLATION_Z, value, duration, startDelay);
        return this;
    }

    /**
     * Animates a view's z translation.
     * @param value The value to animate to.
     * @param duration The duration to animate for.
     * @return This AnimationState instance.
     */
    public GroupAnimator translationZ(float value, int duration) {
        translationZ(value, duration, mDefaultDelay);
        return this;
    }

    /**
     * Animates a view's z translation.
     * @param value The value to animate to.
     * @return This AnimationState instance.
     */
    public GroupAnimator translationZ(float value) {
        translationZ(value, mDefaultDuration, mDefaultDelay);
        return this;
    }

    /**
     * Animates a view's x scale.
     * @param value The value to animate to.
     * @param duration The duration to animate for.
     * @param startDelay The animation start delay.
     * @return This AnimationState instance.
     */
    public GroupAnimator scaleX(float value, int duration, int startDelay) {
        animateProperty(mTarget, SCALE_X, value, duration, startDelay);
        return this;
    }

    /**
     * Animates a view's x scale.
     * @param value The value to animate to.
     * @param duration The duration to animate for.
     * @return This AnimationState instance.
     */
    public GroupAnimator scaleX(float value, int duration) {
        scaleX(value, duration, mDefaultDelay);
        return this;
    }

    /**
     * Animates a view's x scale.
     * @param value The value to animate to.
     * @return This AnimationState instance.
     */
    public GroupAnimator scaleX(float value) {
        scaleX(value, mDefaultDuration, mDefaultDelay);
        return this;
    }

    /**
     * Animates a view's y scale.
     * @param value The value to animate to.
     * @param duration The duration to animate for.
     * @param startDelay The animation start delay.
     * @return This AnimationState instance.
     */
    public GroupAnimator scaleY(float value, int duration, int startDelay) {
        animateProperty(mTarget, SCALE_Y, value, duration, startDelay);
        return this;
    }

    /**
     * Animates a view's y scale.
     * @param value The value to animate to.
     * @param duration The duration to animate for.
     * @return This AnimationState instance.
     */
    public GroupAnimator scaleY(float value, int duration) {
        scaleY(value, duration, mDefaultDelay);
        return this;
    }

    /**
     * Animates a view's y scale.
     * @param value The value to animate to.
     * @return This AnimationState instance.
     */
    public GroupAnimator scaleY(float value) {
        scaleY(value, mDefaultDuration, mDefaultDelay);
        return this;
    }

    /**
     * Animates a view's rotation.
     * @param value The value to animate to.
     * @param duration The duration to animate for.
     * @param startDelay The animation start delay.
     * @return This AnimationState instance.
     */
    public GroupAnimator rotation(float value, int duration, int startDelay) {
        animateProperty(mTarget, ROTATION, value, duration, startDelay);
        return this;
    }

    /**
     * Animates a view's rotation.
     * @param value The value to animate to.
     * @param duration The duration to animate for.
     * @return This AnimationState instance.
     */
    public GroupAnimator rotation(float value, int duration) {
        rotation(value, duration, mDefaultDelay);
        return this;
    }

    /**
     * Animates a view's rotation.
     * @param value The value to animate to.
     * @return This AnimationState instance.
     */
    public GroupAnimator rotation(float value) {
        rotation(value, mDefaultDuration, mDefaultDelay);
        return this;
    }

    /**
     * Animates a view's rotation X.
     * @param value The value to animate to.
     * @param duration The duration to animate for.
     * @param startDelay The animation start delay.
     * @return This AnimationState instance.
     */
    public GroupAnimator rotationX(float value, int duration, int startDelay) {
        animateProperty(mTarget, ROTATION_X, value, duration, startDelay);
        return this;
    }

    /**
     * Animates a view's rotation X.
     * @param value The value to animate to.
     * @param duration The duration to animate for.
     * @return This AnimationState instance.
     */
    public GroupAnimator rotationX(float value, int duration) {
        rotationX(value, duration, mDefaultDelay);
        return this;
    }

    /**
     * Animates a view's rotation X.
     * @param value The value to animate to.
     * @return This AnimationState instance.
     */
    public GroupAnimator rotationX(float value) {
        rotationX(value, mDefaultDuration, mDefaultDelay);
        return this;
    }

    /**
     * Animates a view's rotation Y.
     * @param value The value to animate to.
     * @param duration The duration to animate for.
     * @param startDelay The animation start delay.
     * @return This AnimationState instance.
     */
    public GroupAnimator rotationY(float value, int duration, int startDelay) {
        animateProperty(mTarget, ROTATION_Y, value, duration, startDelay);
        return this;
    }

    /**
     * Animates a view's rotation Y.
     * @param value The value to animate to.
     * @param duration The duration to animate for.
     * @return This AnimationState instance.
     */
    public GroupAnimator rotationY(float value, int duration) {
        rotationY(value, duration, mDefaultDelay);
        return this;
    }

    /**
     * Animates a view's rotation Y.
     * @param value The value to animate to.
     * @return This AnimationState instance.
     */
    public GroupAnimator rotationY(float value) {
        rotationY(value, mDefaultDuration, mDefaultDelay);
        return this;
    }

    /**
     * Animates a view's x translation.
     * @param value The value to animate to.
     * @param duration The duration to animate for.
     * @param startDelay The animation start delay.
     * @return This AnimationState instance.
     */
    public GroupAnimator x(float value, int duration, int startDelay) {
        animateProperty(mTarget, X, value, duration, startDelay);
        return this;
    }

    /**
     * Animates a view's x translation.
     * @param value The value to animate to.
     * @param duration The duration to animate for.
     * @return This AnimationState instance.
     */
    public GroupAnimator X(float value, int duration) {
        x(value, duration, mDefaultDelay);
        return this;
    }

    /**
     * Animates a view's x translation.
     * @param value The value to animate to.
     * @return This AnimationState instance.
     */
    public GroupAnimator X(float value) {
        x(value, mDefaultDuration, mDefaultDelay);
        return this;
    }

    /**
     * Animates a view's y coordinate.
     * @param value The value to animate to.
     * @param duration The duration to animate for.
     * @param startDelay The animation start delay.
     * @return This AnimationState instance.
     */
    public GroupAnimator y(float value, int duration, int startDelay) {
        animateProperty(mTarget, Y, value, duration, startDelay);
        return this;
    }

    /**
     * Animates a view's y coordinate.
     * @param value The value to animate to.
     * @param duration The duration to animate for.
     * @return This AnimationState instance.
     */
    public GroupAnimator y(float value, int duration) {
        y(value, duration, mDefaultDelay);
        return this;
    }

    /**
     * Animates a view's y coordinate.
     * @param value The value to animate to.
     * @return This AnimationState instance.
     */
    public GroupAnimator y(float value) {
        y(value, mDefaultDuration, mDefaultDelay);
        return this;
    }

    /**
     * Animates a view's z coordinate.
     * @param value The value to animate to.
     * @param duration The duration to animate for.
     * @param startDelay The animation start delay.
     * @return This AnimationState instance.
     */
    public GroupAnimator z(float value, int duration, int startDelay) {
        animateProperty(mTarget, Z, value, duration, startDelay);
        return this;
    }

    /**
     * Animates a view's z coordinate.
     * @param value The value to animate to.
     * @param duration The duration to animate for.
     * @return This AnimationState instance.
     */
    public GroupAnimator z(float value, int duration) {
        z(value, duration, mDefaultDelay);
        return this;
    }

    /**
     * Animates a view's z coordinate.
     * @param value The value to animate to.
     * @return This AnimationState instance.
     */
    public GroupAnimator z(float value) {
        z(value, mDefaultDuration, mDefaultDelay);
        return this;
    }

    /**
     * Animates a view's alpha.
     * @param value The value to animate to.
     * @param duration The duration to animate for.
     * @param startDelay The animation start delay.
     * @return This AnimationState instance.
     */
    public GroupAnimator alpha(float value, int duration, int startDelay) {
        animateProperty(mTarget, ALPHA, value, duration, startDelay);
        return this;
    }

    /**
     * Animates a view's alpha.
     * @param value The value to animate to.
     * @param duration The duration to animate for.
     * @return This AnimationState instance.
     */
    public GroupAnimator alpha(float value, int duration) {
        alpha(value, duration, mDefaultDelay);
        return this;
    }

    /**
     * Animates a view's alpha.
     * @param value The value to animate to.
     * @return This AnimationState instance.
     */
    public GroupAnimator alpha(float value) {
        alpha(value, mDefaultDuration, mDefaultDelay);
        return this;
    }
}

package com.malba.animation;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewPropertyAnimator;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.TreeSet;

/**
 * TBD
 */
public class GroupAnimator {
    // Static a animator definitions, so we can map the animations to Android animators.
    private static final int TRANSLATION_X  = 0x0001;
    private static final int TRANSLATION_Y  = 0x0002;
    private static final int TRANSLATION_Z  = 0x0004;
    private static final int SCALE_X        = 0x0008;
    private static final int SCALE_Y        = 0x0010;
    private static final int ROTATION       = 0x0020;
    private static final int ROTATION_X     = 0x0040;
    private static final int ROTATION_Y     = 0x0080;
    private static final int X              = 0x0100;
    private static final int Y              = 0x0200;
    private static final int Z              = 0x0400;
    private static final int ALPHA          = 0x0800;

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

    // The length of the animation that will play.
    private int mAnimationLength = 0;

    // The time when the animation was started at.
    private long mStartTime;

    /**
     * @return The estimated animation percent, after calling start().
     */
    public float getAnimationPercent() {
        final long elapsedTime = System.currentTimeMillis() - mStartTime;
        final float percent = elapsedTime / (float) mAnimationLength;

        if(percent > 1) {
            return 1;
        }

        if(percent < 0) {
            return 0;
        }

        return percent;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        super.clone();
        GroupAnimator clone  = new GroupAnimator();
        return clone;
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
            int[] thisCompare = {this.mStartDelay, this.mDuration, this.mProperty};
            int[] otherCompare = {other.mStartDelay, other.mDuration, other.mProperty};

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
        AnimationValue animationValue = new AnimationValue(property, value, duration, startDelay);

        if(animationValue.getTotalDuration() > mAnimationLength) {
            mAnimationLength = animationValue.getTotalDuration();
        }

        set.add(animationValue);
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
     * @param startTime The start time to start at.
     * @return A ViewPropertyanimator, if any animations were started.
     */
    private ViewPropertyAnimator startAnimation(View view, TreeSet<AnimationValue> animations, int startTime) {
        // Keep track of t he previous value, to know when to start an animation.
        AnimationValue prevValue = null;

        // Reset just after an animator has been started, so we know to setup the next one.
        boolean setupAnimator = false;

        // View property animator to operate on.
        ViewPropertyAnimator animator = view.animate();

        System.out.println("Animating property count " + animations.size());

        for (AnimationValue value : animations) {
            // If the value is not null, and the start delay is not the current delay OR
            // the duration is not the current duration... then start the previous animation, and
            // set up this one.
            if (prevValue != null && (prevValue.mStartDelay != value.mStartDelay || prevValue.mDuration != value.mDuration)) {
                System.out.println("Starting animation batch");
                setupAnimator = false;
                animator.start();
            }

            // Only process this animation, if the total duration is greater than the start time.
            if(value.getTotalDuration() > startTime) {
                // Set up the initial animator.
                if (!setupAnimator) {

                    final int delay, duration;
                    if(startTime > value.mStartDelay) {
                        delay = 0;
                        duration = value.getTotalDuration() - startTime;
                    } else {
                        delay = value.mStartDelay - startTime;
                        duration = value.mDuration;
                    }

                    System.out.println("Duration " + duration + " | Delay " + delay);
                    animator.setDuration(duration);
                    animator.setStartDelay(delay);
                    setupAnimator = true;
                }

                System.out.println("Animating value " + value.mProperty + " " + value.mValue);
                animateValue(animator, value);
                prevValue = value;
            }
        }

        // Start the final piece of the animation, so long as we had something animated.
        if(prevValue != null) {
            System.out.println("Starting animation batch");
            animator.start();
            return animator;
        }

        return null;
    }

    /**
     * Gets the value for a particular animatable property.
     * @param view The view to grab the value for.
     * @param propertyConstant The property being fetched.
     * @return The value of the property being fetched.
     */
    private float getValue(View view, int propertyConstant) {
        switch (propertyConstant) {
            case TRANSLATION_X:
                return view.getTranslationX();
            case TRANSLATION_Y:
                return view.getTranslationY();
            case TRANSLATION_Z:
                return view.getTranslationZ();
            case ROTATION:
                return view.getRotation();
            case ROTATION_X:
                return view.getRotationX();
            case ROTATION_Y:
                return view.getRotationY();
            case SCALE_X:
                return view.getScaleX();
            case SCALE_Y:
                return view.getScaleY();
            case X:
                return view.getLeft() + view.getTranslationX();
            case Y:
                return view.getTop() + view.getTranslationY();
            case Z:
                return view.getElevation() + view.getTranslationZ();
            case ALPHA:
                return view.getAlpha();
        }

        return 0;
    }

    /**
     * Animates the correct property with a value.
     * @param animator The animator to use.
     * @param value The AnimationValue instance to animate with.
     */
    private void animateValue(ViewPropertyAnimator animator, AnimationValue value) {
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
     * Starts the animation sequence.
     * @param startPercent The animation percent to start at.
     */
    public void start(float startPercent) {
        int startTime = (int) (startPercent * mAnimationLength);

        System.out.println("Start time " + startTime);

        Set<View> views = mAnimatorMap.keySet();
        for(View v : views) {
            ViewPropertyAnimator animator = startAnimation(v, mAnimatorMap.get(v), startTime);
            if(animator != null) {
                mActiveAnimators.add(animator);
            }
        }

        mStartTime = System.currentTimeMillis() - startTime;
    }

    /**
     * Starts the animation sequence.
     */
    public void start() {
        start(0);
    }

    /**
     * Uses the currently defined animation set, to generate a new instance of GroupAnimator,
     * which will have the animation reversed. The reverse values are captured based on the current
     * view states.
     * @return A cloned and reversed version of this GroupAnimator.
     */
    public GroupAnimator cloneReverse() {
        GroupAnimator reverseAnimator = new GroupAnimator();
        reverseAnimator.mAnimationLength = mAnimationLength;

        Set<View> views = mAnimatorMap.keySet();
        for(View v : views) {
            TreeSet<AnimationValue> animations = mAnimatorMap.get(v);
            TreeSet<AnimationValue> reverseAnimations = getReverseAnimationSet(v, animations);
            reverseAnimator.mAnimatorMap.put(v, reverseAnimations);
        }

        return reverseAnimator;
    }

    /**
     * Returns a set of animators, which will play in the reverse direction. This method takes
     * durations and start delays into account.
     * @param view The view to pull the current property states from.
     * @param animations The animation set to reverse.
     * @return The reverse animation set.
     */
    private TreeSet<AnimationValue> getReverseAnimationSet(View view, TreeSet<AnimationValue> animations) {
        if(animations != null && !animations.isEmpty()) {
            TreeSet<AnimationValue> reverseAnimations = createAnimationStateSet();
            Iterator<AnimationValue> iter = animations.descendingIterator();

            int totalAnimationTime = mAnimationLength;

            while (iter.hasNext()) {
                AnimationValue value = iter.next();
                AnimationValue reverse = new AnimationValue(
                        value.mProperty,
                        getValue(view, value.mProperty),
                        value.mDuration,
                        totalAnimationTime - value.getTotalDuration()
                );

                reverseAnimations.add(reverse);
            }

            return reverseAnimations;
        }

        return null;
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

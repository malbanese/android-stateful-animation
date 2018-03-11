package com.malba.animation;

import android.util.SparseArray;

public class StatefulGroupAnimator {
    private SparseArray<GroupAnimator> mAnimationState;

    public GroupAnimator registerAnimationState(int state) {
        return new GroupAnimator();
    }
}

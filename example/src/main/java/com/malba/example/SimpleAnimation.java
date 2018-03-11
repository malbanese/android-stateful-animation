package com.malba.example;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.malba.animation.GroupAnimator;

public class SimpleAnimation extends AppCompatActivity {
    private View mButton;
    private int mAnimationState;
    private GroupAnimator mAnimator;
    private GroupAnimator mReverseAnimator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_animation);
        mButton = findViewById(R.id.button);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                animateView(view);
            }
        });

        mAnimator = createAnimator(mButton);
        mReverseAnimator = mAnimator.cloneReverse();
    }

    private GroupAnimator createAnimator(View view) {
        return new GroupAnimator()
                .usingTarget(view)
                .usingDuration(1000)
                .rotation(360)
                .usingTiming(1000, 1000)
                .translationX(100)
                .alpha(0f)
                .usingTiming(500, 1500)
                .scaleX(0)
                .scaleY(0);
    }

    private void animateView(View view) {
        if(mAnimationState == 0) {
            mReverseAnimator.cancel();
            mAnimator.start(1 - mReverseAnimator.getAnimationPercent());
            mAnimationState = 1;
        } else {
            mAnimator.cancel();
            mReverseAnimator.start(1 - mAnimator.getAnimationPercent());
            mAnimationState = 0;
        }
    }
}

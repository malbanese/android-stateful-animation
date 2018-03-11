package com.malba.example;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.malba.animation.GroupAnimator;

public class SimpleAnimation extends AppCompatActivity {
    private View mButton;
    private int mAnimationState;
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
    }

    private void animateView(View view) {
        GroupAnimator animator = new GroupAnimator();

        if(mAnimationState == 0) {
            animator
                    .usingTarget(view)
                    .usingDuration(1000)
                    .rotation(360)
                    .usingTiming(1000, 1000)
                    .translationX(100)
                    .alpha(0f)
                    .usingTiming(500, 1500)
                    .scaleX(0)
                    .scaleY(0)
                    .start();
            mAnimationState = 1;
        } else {
            animator
                    .usingTarget(view)
                    .usingDuration(500)
                    .scaleX(1)
                    .scaleY(1)
                    .usingDuration(1000)
                    .alpha(1)
                    .translationX(0)
                    .usingTiming(1000, 1000)
                    .rotation(0)
                    .start();
            mAnimationState = 0;
        }
    }
}

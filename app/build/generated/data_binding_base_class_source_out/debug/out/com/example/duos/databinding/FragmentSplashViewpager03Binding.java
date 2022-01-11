// Generated by view binder compiler. Do not edit!
package com.example.duos.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import com.example.duos.R;
import java.lang.NullPointerException;
import java.lang.Override;
import java.lang.String;

public final class FragmentSplashViewpager03Binding implements ViewBinding {
  @NonNull
  private final ConstraintLayout rootView;

  @NonNull
  public final ImageView splashImage02Iv;

  @NonNull
  public final TextView splashText03Tv;

  @NonNull
  public final TextView splashTitle03Tv;

  private FragmentSplashViewpager03Binding(@NonNull ConstraintLayout rootView,
      @NonNull ImageView splashImage02Iv, @NonNull TextView splashText03Tv,
      @NonNull TextView splashTitle03Tv) {
    this.rootView = rootView;
    this.splashImage02Iv = splashImage02Iv;
    this.splashText03Tv = splashText03Tv;
    this.splashTitle03Tv = splashTitle03Tv;
  }

  @Override
  @NonNull
  public ConstraintLayout getRoot() {
    return rootView;
  }

  @NonNull
  public static FragmentSplashViewpager03Binding inflate(@NonNull LayoutInflater inflater) {
    return inflate(inflater, null, false);
  }

  @NonNull
  public static FragmentSplashViewpager03Binding inflate(@NonNull LayoutInflater inflater,
      @Nullable ViewGroup parent, boolean attachToParent) {
    View root = inflater.inflate(R.layout.fragment_splash_viewpager_03, parent, false);
    if (attachToParent) {
      parent.addView(root);
    }
    return bind(root);
  }

  @NonNull
  public static FragmentSplashViewpager03Binding bind(@NonNull View rootView) {
    // The body of this method is generated in a way you would not otherwise write.
    // This is done to optimize the compiled bytecode for size and performance.
    int id;
    missingId: {
      id = R.id.splash_image_02_iv;
      ImageView splashImage02Iv = ViewBindings.findChildViewById(rootView, id);
      if (splashImage02Iv == null) {
        break missingId;
      }

      id = R.id.splash_text_03_tv;
      TextView splashText03Tv = ViewBindings.findChildViewById(rootView, id);
      if (splashText03Tv == null) {
        break missingId;
      }

      id = R.id.splash_title_03_tv;
      TextView splashTitle03Tv = ViewBindings.findChildViewById(rootView, id);
      if (splashTitle03Tv == null) {
        break missingId;
      }

      return new FragmentSplashViewpager03Binding((ConstraintLayout) rootView, splashImage02Iv,
          splashText03Tv, splashTitle03Tv);
    }
    String missingId = rootView.getResources().getResourceName(id);
    throw new NullPointerException("Missing required view with ID: ".concat(missingId));
  }
}
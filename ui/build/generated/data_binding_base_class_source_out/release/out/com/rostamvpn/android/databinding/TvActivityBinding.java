// Generated by data binding compiler. Do not edit!
package com.rostamvpn.android.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.Bindable;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ObservableBoolean;
import androidx.databinding.ObservableField;
import androidx.databinding.ViewDataBinding;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.rostamvpn.android.R;
import com.rostamvpn.android.activity.TvMainActivity;
import com.rostamvpn.android.model.ObservableTunnel;
import java.lang.Deprecated;
import java.lang.Object;

public abstract class TvActivityBinding extends ViewDataBinding {
  @NonNull
  public final MaterialCardView bannerLogo;

  @NonNull
  public final MaterialButton deleteButton;

  @NonNull
  public final RecyclerView filesList;

  @NonNull
  public final TextView filesRootLabel;

  @NonNull
  public final MaterialButton importButton;

  @NonNull
  public final RecyclerView tunnelList;

  @Bindable
  protected ObservableBoolean mIsDeleting;

  @Bindable
  protected ObservableKeyedArrayList<String, TvMainActivity.KeyedFile> mFiles;

  @Bindable
  protected ObservableField<String> mFilesRoot;

  @Bindable
  protected ObservableKeyedArrayList<String, ObservableTunnel> mTunnels;

  @Bindable
  protected ObservableKeyedRecyclerViewAdapter.RowConfigurationHandler mTunnelRowConfigurationHandler;

  @Bindable
  protected ObservableKeyedRecyclerViewAdapter.RowConfigurationHandler mFilesRowConfigurationHandler;

  protected TvActivityBinding(Object _bindingComponent, View _root, int _localFieldCount,
      MaterialCardView bannerLogo, MaterialButton deleteButton, RecyclerView filesList,
      TextView filesRootLabel, MaterialButton importButton, RecyclerView tunnelList) {
    super(_bindingComponent, _root, _localFieldCount);
    this.bannerLogo = bannerLogo;
    this.deleteButton = deleteButton;
    this.filesList = filesList;
    this.filesRootLabel = filesRootLabel;
    this.importButton = importButton;
    this.tunnelList = tunnelList;
  }

  public abstract void setIsDeleting(@Nullable ObservableBoolean isDeleting);

  @Nullable
  public ObservableBoolean getIsDeleting() {
    return mIsDeleting;
  }

  public abstract void setFiles(
      @Nullable ObservableKeyedArrayList<String, TvMainActivity.KeyedFile> files);

  @Nullable
  public ObservableKeyedArrayList<String, TvMainActivity.KeyedFile> getFiles() {
    return mFiles;
  }

  public abstract void setFilesRoot(@Nullable ObservableField<String> filesRoot);

  @Nullable
  public ObservableField<String> getFilesRoot() {
    return mFilesRoot;
  }

  public abstract void setTunnels(
      @Nullable ObservableKeyedArrayList<String, ObservableTunnel> tunnels);

  @Nullable
  public ObservableKeyedArrayList<String, ObservableTunnel> getTunnels() {
    return mTunnels;
  }

  public abstract void setTunnelRowConfigurationHandler(
      @Nullable ObservableKeyedRecyclerViewAdapter.RowConfigurationHandler tunnelRowConfigurationHandler);

  @Nullable
  public ObservableKeyedRecyclerViewAdapter.RowConfigurationHandler getTunnelRowConfigurationHandler(
      ) {
    return mTunnelRowConfigurationHandler;
  }

  public abstract void setFilesRowConfigurationHandler(
      @Nullable ObservableKeyedRecyclerViewAdapter.RowConfigurationHandler filesRowConfigurationHandler);

  @Nullable
  public ObservableKeyedRecyclerViewAdapter.RowConfigurationHandler getFilesRowConfigurationHandler(
      ) {
    return mFilesRowConfigurationHandler;
  }

  @NonNull
  public static TvActivityBinding inflate(@NonNull LayoutInflater inflater,
      @Nullable ViewGroup root, boolean attachToRoot) {
    return inflate(inflater, root, attachToRoot, DataBindingUtil.getDefaultComponent());
  }

  /**
   * This method receives DataBindingComponent instance as type Object instead of
   * type DataBindingComponent to avoid causing too many compilation errors if
   * compilation fails for another reason.
   * https://issuetracker.google.com/issues/116541301
   * @Deprecated Use DataBindingUtil.inflate(inflater, R.layout.tv_activity, root, attachToRoot, component)
   */
  @NonNull
  @Deprecated
  public static TvActivityBinding inflate(@NonNull LayoutInflater inflater,
      @Nullable ViewGroup root, boolean attachToRoot, @Nullable Object component) {
    return ViewDataBinding.<TvActivityBinding>inflateInternal(inflater, R.layout.tv_activity, root, attachToRoot, component);
  }

  @NonNull
  public static TvActivityBinding inflate(@NonNull LayoutInflater inflater) {
    return inflate(inflater, DataBindingUtil.getDefaultComponent());
  }

  /**
   * This method receives DataBindingComponent instance as type Object instead of
   * type DataBindingComponent to avoid causing too many compilation errors if
   * compilation fails for another reason.
   * https://issuetracker.google.com/issues/116541301
   * @Deprecated Use DataBindingUtil.inflate(inflater, R.layout.tv_activity, null, false, component)
   */
  @NonNull
  @Deprecated
  public static TvActivityBinding inflate(@NonNull LayoutInflater inflater,
      @Nullable Object component) {
    return ViewDataBinding.<TvActivityBinding>inflateInternal(inflater, R.layout.tv_activity, null, false, component);
  }

  public static TvActivityBinding bind(@NonNull View view) {
    return bind(view, DataBindingUtil.getDefaultComponent());
  }

  /**
   * This method receives DataBindingComponent instance as type Object instead of
   * type DataBindingComponent to avoid causing too many compilation errors if
   * compilation fails for another reason.
   * https://issuetracker.google.com/issues/116541301
   * @Deprecated Use DataBindingUtil.bind(view, component)
   */
  @Deprecated
  public static TvActivityBinding bind(@NonNull View view, @Nullable Object component) {
    return (TvActivityBinding)bind(component, view, R.layout.tv_activity);
  }
}
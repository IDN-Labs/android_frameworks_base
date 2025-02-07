/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.qs;

import android.os.Bundle;

import com.android.systemui.R;
import com.android.systemui.battery.BatteryMeterViewController;
import com.android.systemui.demomode.DemoMode;
import com.android.systemui.demomode.DemoModeController;
import com.android.systemui.flags.FeatureFlags;
import com.android.systemui.qs.carrier.QSCarrierGroupController;
import com.android.systemui.qs.dagger.QSScope;
import com.android.systemui.statusbar.phone.StatusBarContentInsetsProvider;
import com.android.systemui.statusbar.phone.StatusBarIconController;
import com.android.systemui.statusbar.phone.StatusIconContainer;
import com.android.systemui.statusbar.policy.Clock;
import com.android.systemui.statusbar.policy.VariableDateViewController;
import com.android.systemui.util.ViewController;

import java.util.List;

import javax.inject.Inject;

/**
 * Controller for {@link QuickStatusBarHeader}.
 */
@QSScope
class QuickStatusBarHeaderController extends ViewController<QuickStatusBarHeader> implements
        ChipVisibilityListener {

    private final QSCarrierGroupController mQSCarrierGroupController;
    private final QuickQSPanelController mQuickQSPanelController;
    private final StatusBarIconController mStatusBarIconController;
    private final StatusIconContainer mIconContainer;
    private final StatusBarIconController.TintedIconManager mIconManager;
    private final QSExpansionPathInterpolator mQSExpansionPathInterpolator;
    private final BatteryMeterViewController mBatteryMeterViewController;
    private final FeatureFlags mFeatureFlags;
    private final StatusBarContentInsetsProvider mInsetsProvider;
    private final HeaderPrivacyIconsController mPrivacyIconsController;

    private boolean mListening;

    @Inject
    QuickStatusBarHeaderController(QuickStatusBarHeader view,
            HeaderPrivacyIconsController headerPrivacyIconsController,
            StatusBarIconController statusBarIconController,
            DemoModeController demoModeController,
            QuickQSPanelController quickQSPanelController,
            QSCarrierGroupController.Builder qsCarrierGroupControllerBuilder,
            QSExpansionPathInterpolator qsExpansionPathInterpolator,
            BatteryMeterViewController batteryMeterViewController,
            FeatureFlags featureFlags,
            VariableDateViewController.Factory variableDateViewControllerFactory,
            StatusBarContentInsetsProvider statusBarContentInsetsProvider) {
        super(view);
        mPrivacyIconsController = headerPrivacyIconsController;
        mStatusBarIconController = statusBarIconController;
        mQuickQSPanelController = quickQSPanelController;
        mQSExpansionPathInterpolator = qsExpansionPathInterpolator;
        mBatteryMeterViewController = batteryMeterViewController;
        mFeatureFlags = featureFlags;
        mInsetsProvider = statusBarContentInsetsProvider;

        mQSCarrierGroupController = qsCarrierGroupControllerBuilder
                .setQSCarrierGroup(mView.findViewById(R.id.carrier_group))
                .build();
        mIconContainer = mView.findViewById(R.id.statusIcons);

        mIconManager = new StatusBarIconController.TintedIconManager(mIconContainer, featureFlags);
        
    }

    @Override
    protected void onInit() {
        mBatteryMeterViewController.init();
    }

    @Override
    protected void onViewAttached() {
        mPrivacyIconsController.onParentVisible();
        mPrivacyIconsController.setChipVisibilityListener(this);
        mIconContainer.addIgnoredSlot(
                getResources().getString(com.android.internal.R.string.status_bar_managed_profile));
        mIconContainer.setShouldRestrictIcons(false);
        mStatusBarIconController.addIconGroup(mIconManager);

        mView.setIsSingleCarrier(mQSCarrierGroupController.isSingleCarrier());
        mQSCarrierGroupController
                .setOnSingleCarrierChangedListener(mView::setIsSingleCarrier);

        List<String> rssiIgnoredSlots;

        if (mFeatureFlags.isCombinedStatusBarSignalIconsEnabled()) {
            rssiIgnoredSlots = List.of(
                    getResources().getString(com.android.internal.R.string.status_bar_no_calling),
                    getResources().getString(com.android.internal.R.string.status_bar_call_strength)
            );
        } else {
            rssiIgnoredSlots = List.of(
                    getResources().getString(com.android.internal.R.string.status_bar_mobile)
            );
        }

        mView.onAttach(mIconManager, mQSExpansionPathInterpolator, rssiIgnoredSlots,
                mFeatureFlags.useCombinedQSHeaders(), mInsetsProvider);


    }

    @Override
    protected void onViewDetached() {
        mPrivacyIconsController.onParentInvisible();
        mStatusBarIconController.removeIconGroup(mIconManager);
        mQSCarrierGroupController.setOnSingleCarrierChangedListener(null);
        setListening(false);
    }

    public void setListening(boolean listening) {
        mQSCarrierGroupController.setListening(listening);

        if (listening == mListening) {
            return;
        }
        mListening = listening;

        mQuickQSPanelController.setListening(listening);
        if (mQuickQSPanelController.isListening()) {
            mQuickQSPanelController.refreshAllTiles();
        }

        if (mQuickQSPanelController.switchTileLayout(false)) {
            mView.updateResources();
        }

        if (listening) {
            mPrivacyIconsController.startListening();
        } else {
            mPrivacyIconsController.stopListening();
        }
    }

    @Override
    public void onChipVisibilityRefreshed(boolean visible) {
        mView.setChipVisibility(visible);
    }

    public void setContentMargins(int marginStart, int marginEnd) {
        mQuickQSPanelController.setContentMargins(marginStart, marginEnd);
    }
}
